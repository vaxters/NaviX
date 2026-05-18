/*
 * Copyright 2026 Navix Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.navix.runtime

import io.navix.contracts.BackstackSnapshot
import io.navix.contracts.NavEvent
import io.navix.contracts.NavEventType
import io.navix.contracts.NavTransitionKey
import io.navix.contracts.NavixTelemetry
import io.navix.contracts.Route
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.reflect.KClass

/**
 * Default [Navigator] implementation.
 *
 * Wires [BackstackStore] (pure state machine), [NavixTelemetry] (event pipeline),
 * and [DeepLinkHandler] list (URI resolution) into a single coordinator.
 *
 * ### Thread safety
 * All navigation actions are dispatched through an internal [Channel] actor processed
 * by a single coroutine launched on [scope]. This serializes concurrent calls regardless
 * of which thread or coroutine they originate from. The public API is synchronous and
 * fire-and-forget — callers never suspend.
 *
 * ### Action sequence (per action processed by the actor)
 * 1. Action dispatched to [BackstackStore] — pure, deterministic, synchronous.
 * 2. [NavEvent] emitted on [events] SharedFlow.
 * 3. [NavEvent] forwarded to [NavixTelemetry] (non-blocking when using
 *    [io.navix.telemetry.NavixTelemetryPipeline]).
 *
 * ### Telemetry contract
 * [NavixTelemetry.onEvent] is called from the actor coroutine. Implementations MUST be
 * non-blocking. [io.navix.telemetry.NavixTelemetryPipeline] satisfies this via an
 * internal Channel. A throwing implementation will NOT crash or stall the navigator —
 * the exception is caught, logged to stdout, and navigation continues normally. This
 * matches the defensive pattern used in [io.navix.telemetry.NavixTelemetryPipeline].
 */
internal class NavigatorImpl(
    root: Route,
    private val store: BackstackStore,
    private val telemetry: NavixTelemetry,
    private val deepLinkHandlers: List<DeepLinkHandler>,
    scope: CoroutineScope
) : Navigator {
    internal constructor(
        root: Route,
        scope: CoroutineScope,
        reducer: Reducer = DefaultReducer(),
        entryFactory: EntryFactory = DefaultEntryFactory,
        telemetry: NavixTelemetry = NavixTelemetry.NoOp,
        deepLinkHandlers: List<DeepLinkHandler> = emptyList(),
        /** When non-null, the navigator resumes from this snapshot instead of a fresh root entry. */
        initialSnapshot: BackstackSnapshot? = null
    ) : this(
        root = root,
        store =
            BackstackStoreImpl(
                initial =
                    initialSnapshot ?: BackstackSnapshot(
                        entries = listOf(entryFactory.create(root, NavTransitionKey.Default))
                    ),
                reducer = reducer
            ),
        telemetry = telemetry,
        deepLinkHandlers = deepLinkHandlers,
        scope = scope
    )

    override val backstack: StateFlow<BackstackSnapshot> = store.state

    private val _events = MutableSharedFlow<NavEvent>(extraBufferCapacity = 64)
    override val events: SharedFlow<NavEvent> = _events.asSharedFlow()

    // Actor channel: UNLIMITED capacity so navigation calls never block or drop.
    // A single consumer coroutine processes messages serially, preventing concurrent
    // backstack mutations regardless of which thread the caller is on.
    private val actionChannel = Channel<ActorMessage>(Channel.UNLIMITED)

    // Pending result deferreds keyed by the entry ID of the pushed destination, plus the
    // value the callee set via setResult(). Every read/write of both maps happens on the
    // single actor coroutine — registration (pushForResult's onApplied), delivery
    // (processAction's removal drain), and setResult's Task all run there — so the maps
    // need no synchronization and there is no caller/actor data race.
    private val pendingResults = HashMap<String, CompletableDeferred<Any?>>()
    private val pendingResultValues = HashMap<String, Any?>()

    init {
        scope.launch {
            for (msg in actionChannel) {
                when (msg) {
                    is ActorMessage.Navigate -> processAction(msg)
                    is ActorMessage.Task -> msg.block()
                }
            }
        }
        // Close the channel when the scope's Job completes so the actor coroutine can
        // terminate cleanly. Without this, the actor's `for (msg in channel)` loop suspends
        // forever after the scope is cancelled, leaking a coroutine in tests and on Activity
        // finish.
        scope.coroutineContext[Job]?.invokeOnCompletion { actionChannel.close() }
    }

    override fun push(route: Route, transition: NavTransitionKey) {
        actionChannel.trySend(
            ActorMessage.Navigate(
                action = BackstackAction.Push(route = route, transition = transition),
                eventType = NavEventType.PUSH
            )
        )
    }

    override fun pop(transition: NavTransitionKey) {
        actionChannel.trySend(
            ActorMessage.Navigate(action = BackstackAction.Pop(transition), eventType = NavEventType.POP)
        )
    }

    override fun replace(route: Route, transition: NavTransitionKey) {
        actionChannel.trySend(
            ActorMessage.Navigate(
                action = BackstackAction.Replace(route = route, transition = transition),
                eventType = NavEventType.REPLACE
            )
        )
    }

    override fun reset(root: Route) {
        actionChannel.trySend(
            ActorMessage.Navigate(
                action = BackstackAction.Reset(root = root, transition = NavTransitionKey.Fade),
                eventType = NavEventType.RESET
            )
        )
    }

    override fun popTo(routeClass: KClass<out Route>, inclusive: Boolean) {
        actionChannel.trySend(
            ActorMessage.Navigate(
                action =
                    BackstackAction.PopTo(
                        routeClass = routeClass,
                        inclusive = inclusive,
                        transition = NavTransitionKey.SlideRight
                    ),
                eventType = NavEventType.POP_TO
            )
        )
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <R : Any> pushForResult(route: Route, transition: NavTransitionKey): NavResult<R> {
        val deferred = CompletableDeferred<Any?>()
        // Register the pending deferred ON THE ACTOR via onApplied, keyed by the entry the
        // Push creates. Routing registration through the actor (instead of writing
        // pendingResults from the caller thread, as a previous version did) confines every
        // mutation of the result maps to the single actor coroutine — eliminating the
        // caller/actor data race on these non-concurrent HashMaps.
        actionChannel.trySend(
            ActorMessage.Navigate(
                action = BackstackAction.Push(route = route, transition = transition),
                eventType = NavEventType.PUSH,
                onApplied = { before, after ->
                    val pushed = after.entries.lastOrNull { e -> before.entries.none { it.id == e.id } }
                    if (pushed != null) {
                        pendingResults[pushed.id] = deferred
                    } else {
                        // Reducer produced no new entry (e.g. a custom single-top reducer):
                        // nothing to await on — resolve as Cancelled rather than hang.
                        deferred.complete(null)
                    }
                }
            )
        )

        // await() only completes via deferred.complete(value) (never exceptionally), so a
        // throw here means the caller's coroutine was cancelled. Let CancellationException
        // propagate — swallowing it would break structured concurrency by resuming a
        // cancelled caller. A failed `as R` cast is a real programming error, not Cancelled.
        val raw = deferred.await()
        return if (raw == null) NavResult.Cancelled else NavResult.Success(raw as R)
    }

    override fun setResult(value: Any?) {
        // Routed through the actor so pendingResultValues is only mutated on the actor
        // coroutine. Reads the active entry id at processing time; ordering relative to a
        // following pop()/reset() is preserved by the channel's FIFO single consumer.
        actionChannel.trySend(
            ActorMessage.Task {
                val currentId = store.state.value.active?.id ?: return@Task
                pendingResultValues[currentId] = value
            }
        )
    }

    override fun handleDeepLink(uri: String): Boolean {
        // Resolution is synchronous — only the navigation action is queued.
        for (handler in deepLinkHandlers) {
            if (handler.canHandle(uri)) {
                val route = handler.resolve(uri) ?: continue
                actionChannel.trySend(
                    ActorMessage.Navigate(
                        action = BackstackAction.Push(route = route, transition = NavTransitionKey.Fade),
                        eventType = NavEventType.DEEP_LINK,
                        metadata = mapOf("uri" to uri)
                    )
                )
                return true
            }
        }
        return false
    }

    private fun processAction(msg: ActorMessage.Navigate) {
        val before = store.state.value
        store.dispatch(msg.action)
        val after = store.state.value

        // Register any pending-result deferred for the entry this action just created,
        // before draining removals — a Push never removes the entry it added, so the
        // registration cannot be wiped by this same action's removal pass.
        msg.onApplied?.invoke(before, after)

        // Detect entries that were removed by this action and drain their pending results.
        // This runs on the actor coroutine (single-threaded) so no synchronization is needed.
        val removedIds = before.entries.map { it.id } - after.entries.map { it.id }.toSet()
        for (id in removedIds) {
            val deferred = pendingResults.remove(id)
            if (deferred != null) {
                // Deliver any value the callee set via setResult(), or null (→ Cancelled).
                val value = pendingResultValues.remove(id)
                deferred.complete(value)
            }
            pendingResultValues.remove(id) // clean up even without a pending deferred
        }

        val from = before.active
        val to = after.active
        val event = NavEvent(msg.eventType, from, to, nowMs(), msg.metadata)
        _events.tryEmit(event)
        // Guard: a misbehaving telemetry implementation must never crash or stall
        // the actor coroutine — that would permanently halt all navigation. The catch
        // mirrors the defensive pattern in NavixTelemetryPipeline's exporter loop.
        try {
            telemetry.onEvent(event)
        } catch (t: Throwable) {
            println("[NaviX][WARN] telemetry.onEvent threw — navigation continues. $t")
        }
    }

    private fun nowMs() = Clock.System.now().toEpochMilliseconds()

    private sealed interface ActorMessage {
        /** A backstack action plus optional post-apply hook, run on the actor coroutine. */
        data class Navigate(
            val action: BackstackAction,
            val eventType: NavEventType,
            val metadata: Map<String, String> = emptyMap(),
            val onApplied: ((before: BackstackSnapshot, after: BackstackSnapshot) -> Unit)? = null
        ) : ActorMessage

        /** A side-effect-only unit of work serialized onto the actor coroutine. */
        data class Task(val block: () -> Unit) : ActorMessage
    }
}
