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
import io.navix.contracts.NavLifecycleState
import io.navix.contracts.NavTransitionKey
import io.navix.contracts.Route
import io.navix.contracts.RouteEntry
import kotlinx.datetime.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Produces the next [BackstackSnapshot] from the current snapshot and a [BackstackAction].
 *
 * Implementations must be **pure**: no side effects, no coroutines, no I/O. Given the
 * same [BackstackSnapshot] and [BackstackAction], a [Reducer] must always return an
 * identical result. Referential transparency is guaranteed because all entry identity
 * ([RouteEntry.id], [RouteEntry.createdAt]) is controlled by the injected [EntryFactory],
 * not by the reducer itself.
 *
 * ### Custom reducers
 * Implement this interface to add cross-cutting backstack policies:
 * ```kotlin
 * class SingleTopReducer(
 *     private val delegate: Reducer = DefaultReducer(),
 * ) : Reducer {
 *     override fun reduce(snapshot: BackstackSnapshot, action: BackstackAction): BackstackSnapshot {
 *         if (action is BackstackAction.Push) {
 *             val existing = snapshot.entries.indexOfLast { it.route::class == action.route::class }
 *             if (existing >= 0) return delegate.reduce(snapshot, BackstackAction.PopTo(action.route::class))
 *         }
 *         return delegate.reduce(snapshot, action)
 *     }
 * }
 * ```
 */
fun interface Reducer {
    fun reduce(
        snapshot: BackstackSnapshot,
        action: BackstackAction,
    ): BackstackSnapshot
}

/**
 * Creates a new [RouteEntry] for entry-producing actions (Push, Replace, Reset).
 *
 * Injecting the factory into a [Reducer] keeps the reducer referentially pure — the
 * reducer itself makes no clock reads or random calls.
 *
 * - [DefaultEntryFactory] — production; uses [Uuid.random] and [Clock.System.now].
 * - [io.navix.testing.DeterministicEntryFactory] — tests; produces stable sequential
 *   IDs and zero timestamps so snapshot equality checks are reliable.
 */
fun interface EntryFactory {
    fun create(
        route: Route,
        transition: NavTransitionKey,
    ): RouteEntry
}

/**
 * Production [EntryFactory]. Generates a random UUID entry ID and stamps the current
 * wall-clock time as [RouteEntry.createdAt].
 */
object DefaultEntryFactory : EntryFactory {
    @OptIn(ExperimentalUuidApi::class)
    override fun create(
        route: Route,
        transition: NavTransitionKey,
    ): RouteEntry =
        RouteEntry(
            id = Uuid.random().toString(),
            route = route,
            createdAt = Clock.System.now().toEpochMilliseconds(),
            lifecycleState = NavLifecycleState.RESUMED,
            transitionKey = transition,
        )
}

/**
 * The default [Reducer] shipped with Navix.
 *
 * Pass a custom [EntryFactory] to control entry identity — useful for deterministic
 * tests ([io.navix.testing.DeterministicEntryFactory]) or for stamping extra metadata.
 *
 * ### Invariants maintained
 * - The stack is never empty after any action (Pop on a single-entry stack is a no-op).
 * - The topmost entry always has [NavLifecycleState.RESUMED].
 * - All other entries have [NavLifecycleState.STARTED].
 * - Each entry's [RouteEntry.transitionKey] records the animation used when it became active.
 */
class DefaultReducer(
    private val entryFactory: EntryFactory = DefaultEntryFactory,
) : Reducer {
    override fun reduce(
        snapshot: BackstackSnapshot,
        action: BackstackAction,
    ): BackstackSnapshot =
        when (action) {
            is BackstackAction.Push ->
                snapshot.withEntries(
                    snapshot.entries.map { it.copy(lifecycleState = NavLifecycleState.STARTED) } +
                        entryFactory.create(action.route, action.transition),
                )

            is BackstackAction.Pop -> {
                if (snapshot.entries.size <= 1) {
                    snapshot
                } else {
                    snapshot.withEntries(
                        snapshot.entries.dropLast(1).mapIndexed { index, entry ->
                            if (index == snapshot.entries.size - 2) {
                                entry.copy(
                                    lifecycleState = NavLifecycleState.RESUMED,
                                    transitionKey = action.transition,
                                )
                            } else {
                                entry
                            }
                        },
                    )
                }
            }

            is BackstackAction.Replace -> {
                val base =
                    if (snapshot.entries.isEmpty()) {
                        emptyList()
                    } else {
                        snapshot.entries.dropLast(1)
                    }
                snapshot.withEntries(base + entryFactory.create(action.route, action.transition))
            }

            is BackstackAction.Reset ->
                snapshot.withEntries(
                    listOf(entryFactory.create(action.root, action.transition)),
                )

            is BackstackAction.PopTo -> {
                val targetIndex =
                    snapshot.entries.indexOfLast { action.routeClass.isInstance(it.route) }
                if (targetIndex < 0) {
                    snapshot
                } else {
                    val cutIndex = if (action.inclusive) targetIndex else targetIndex + 1
                    if (cutIndex >= snapshot.entries.size) {
                        snapshot
                    } else {
                        val remaining = snapshot.entries.subList(0, cutIndex)
                        if (remaining.isEmpty()) {
                            snapshot
                        } else {
                            snapshot.withEntries(
                                remaining.mapIndexed { index, entry ->
                                    if (index == remaining.size - 1) {
                                        entry.copy(
                                            lifecycleState = NavLifecycleState.RESUMED,
                                            transitionKey = action.transition,
                                        )
                                    } else {
                                        entry
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
}

private fun BackstackSnapshot.withEntries(entries: List<RouteEntry>): BackstackSnapshot = copy(entries = entries)
