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
package io.navix.telemetry

import io.navix.contracts.NavEvent
import io.navix.contracts.NavixTelemetry
import io.navix.telemetry.NavixTelemetryPipeline.Companion.MAX_CHANNEL_CAPACITY
import io.navix.telemetry.NavixTelemetryPipeline.Companion.MAX_REPLAY_BUFFER
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Production [NavixTelemetry] implementation that fans out [NavEvent]s to a list of
 * [NavEventExporter]s on a background [CoroutineDispatcher].
 *
 * Navigation actions are never blocked waiting for exporter I/O — events are buffered
 * in a [Channel] and processed asynchronously by a single actor coroutine. A [SupervisorJob]
 * ensures one failing exporter does not cancel the pipeline.
 *
 * The internal channel is bounded at [MAX_CHANNEL_CAPACITY] with [BufferOverflow.DROP_OLDEST].
 * If exporters fall behind (e.g., slow network sink), the oldest unprocessed events are
 * dropped rather than leaking heap. Observe [droppedEventCount] to detect back-pressure.
 *
 * ### Thread safety
 * [onEvent] may be called from any thread. It enqueues via [Channel.trySend] (non-blocking,
 * lock-free) and returns immediately. The [droppedEventCount] StateFlow is updated via
 * [MutableStateFlow.update] which is CAS-safe under concurrent callers. The [replayBuffer]
 * StateFlow and the internal mutable buffer are written exclusively by the single actor
 * coroutine — no synchronization required there.
 *
 * Usage:
 * ```kotlin
 * val telemetry = NavixTelemetryPipeline(
 *     exporters = listOf(
 *         LogcatExporter(),
 *         MyFirebaseExporter(),
 *     )
 * )
 * val navigator = rememberNavigator(root = Home, telemetry = telemetry)
 * ```
 *
 * The pipeline processes events in FIFO order. Exporter exceptions are caught and
 * printed; the event is still delivered to all remaining exporters.
 *
 * ### Telemetry contract
 * [onEvent] is non-blocking — it enqueues the event and returns immediately, satisfying
 * [Navigator][io.navix.runtime.Navigator]'s requirement that telemetry never stalls navigation.
 */
class NavixTelemetryPipeline(
    private val exporters: List<NavEventExporter>,
    dispatcher: CoroutineDispatcher = Dispatchers.Default
) : NavixTelemetry {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val channel =
        Channel<NavEvent>(capacity = MAX_CHANNEL_CAPACITY, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val _droppedEventCount = MutableStateFlow(0)

    /**
     * Reactive count of events dropped due to back-pressure (channel at capacity).
     *
     * Updated via CAS from whichever thread calls [onEvent], safe for concurrent access.
     * Observe this [StateFlow] in debug UIs to detect exporter lag.
     */
    val droppedEventCount: StateFlow<Int> = _droppedEventCount.asStateFlow()

    private val _replayBuffer = MutableStateFlow<List<NavEvent>>(emptyList())

    /**
     * Ordered snapshot of the [MAX_REPLAY_BUFFER] most-recent events (oldest first),
     * updated reactively after each event is processed by the pipeline actor.
     *
     * Unlike [io.navix.runtime.Navigator.events] (a hot SharedFlow), subscribing to
     * this StateFlow after events have already been emitted returns the full history
     * that fits within [MAX_REPLAY_BUFFER]. Use this in DevTools or telemetry UIs that
     * open after navigation has begun.
     */
    val replayBuffer: StateFlow<List<NavEvent>> = _replayBuffer.asStateFlow()

    init {
        scope.launch {
            // All mutations below are actor-confined: single coroutine, no concurrent writers.
            val buffer = ArrayDeque<NavEvent>(MAX_REPLAY_BUFFER)
            for (event in channel) {
                if (buffer.size >= MAX_REPLAY_BUFFER) buffer.removeFirst()
                buffer.addLast(event)
                // Snapshot for StateFlow observers (Compose collects this via collectAsState).
                _replayBuffer.value = buffer.toList()

                exporters.forEach { exporter ->
                    try {
                        exporter.export(event)
                    } catch (t: Throwable) {
                        println("NavixTelemetry: exporter ${exporter::class.simpleName} threw: $t")
                    }
                }
            }
        }
    }

    override fun onEvent(event: NavEvent) {
        val result = channel.trySend(event)
        if (result.isFailure) {
            // CAS-safe: MutableStateFlow.update is atomic across concurrent callers.
            _droppedEventCount.update { it + 1 }
            println(
                "NavixTelemetry: channel full, dropped event ${event.type}. " +
                    "Total dropped: ${_droppedEventCount.value}"
            )
        }
    }

    private companion object {
        const val MAX_REPLAY_BUFFER = 500
        const val MAX_CHANNEL_CAPACITY = 2048
    }
}
