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
import io.navix.contracts.RouteEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BackstackReducerTest {
    // Shared reducer instance — a single DefaultReducer with a deterministic factory
    // so that entry IDs are predictable and snapshot equality is reliable across calls.
    private var entryCounter = 0

    private val reducer =
        DefaultReducer(
            entryFactory =
                EntryFactory { route, transition ->
                    RouteEntry(
                        id = "id-${entryCounter++}",
                        route = route,
                        createdAt = 0L,
                        lifecycleState = NavLifecycleState.RESUMED,
                        transitionKey = transition
                    )
                }
        )

    private fun reduce(snapshot: BackstackSnapshot, action: BackstackAction) = reducer.reduce(snapshot, action)

    private fun snapshotOf(vararg routes: io.navix.contracts.Route): BackstackSnapshot {
        val entries =
            routes.mapIndexed { index, route ->
                RouteEntry(
                    id = "id-${entryCounter++}",
                    route = route,
                    createdAt = 0L,
                    lifecycleState =
                        if (index == routes.size - 1) {
                            NavLifecycleState.RESUMED
                        } else {
                            NavLifecycleState.STARTED
                        }
                )
            }
        return BackstackSnapshot(entries)
    }

    // ── Push ────────────────────────────────────────────────────────────────

    @Test
    fun push_singleEntryStack_addsRouteToTop() {
        val snapshot = snapshotOf(HomeRoute)
        val result = reduce(snapshot, BackstackAction.Push(DetailRoute("1")))
        assertEquals(2, result.depth)
        assertTrue(result.active?.route is DetailRoute)
    }

    @Test
    fun push_singleEntryStack_setsNewTopResumedAndPreviousStarted() {
        val snapshot = snapshotOf(HomeRoute)
        val result = reduce(snapshot, BackstackAction.Push(DetailRoute("1")))
        assertEquals(NavLifecycleState.RESUMED, result.active?.lifecycleState)
        assertEquals(NavLifecycleState.STARTED, result.entries[0].lifecycleState)
    }

    @Test
    fun push_sameRouteTypeTwice_createsTwoDistinctEntries() {
        val snapshot = snapshotOf(HomeRoute)
        val r1 = reduce(snapshot, BackstackAction.Push(DetailRoute("a")))
        val r2 = reduce(r1, BackstackAction.Push(DetailRoute("b")))
        assertEquals(3, r2.depth)
        assertTrue(r2.entries[1].route is DetailRoute)
        assertTrue(r2.entries[2].route is DetailRoute)
        val ids = r2.entries.map { it.id }
        assertEquals(ids.distinct().size, ids.size)
    }

    // ── Pop ─────────────────────────────────────────────────────────────────

    @Test
    fun pop_multiEntryStack_removesTopmostEntry() {
        val snapshot = snapshotOf(HomeRoute, DetailRoute("1"))
        val result = reduce(snapshot, BackstackAction.Pop())
        assertEquals(1, result.depth)
        assertTrue(result.active?.route is HomeRoute)
    }

    @Test
    fun pop_singleEntryStack_isNoOp() {
        val snapshot = snapshotOf(HomeRoute)
        val result = reduce(snapshot, BackstackAction.Pop())
        assertEquals(snapshot, result)
    }

    @Test
    fun pop_multiEntryStack_restoresResumedOnNewTop() {
        val snapshot = snapshotOf(HomeRoute, DetailRoute("1"))
        val result = reduce(snapshot, BackstackAction.Pop())
        assertEquals(NavLifecycleState.RESUMED, result.active?.lifecycleState)
    }

    // ── Replace ──────────────────────────────────────────────────────────────

    @Test
    fun replace_multiEntryStack_swapsTopmostRoute() {
        val snapshot = snapshotOf(HomeRoute, DetailRoute("1"))
        val result = reduce(snapshot, BackstackAction.Replace(SettingsRoute))
        assertEquals(2, result.depth)
        assertTrue(result.active?.route is SettingsRoute)
        assertTrue(result.entries[0].route is HomeRoute)
    }

    @Test
    fun replace_emptySnapshot_addsSingleEntry() {
        val result = reduce(BackstackSnapshot.Empty, BackstackAction.Replace(HomeRoute))
        assertEquals(1, result.depth)
        assertTrue(result.active?.route is HomeRoute)
    }

    // ── Reset ────────────────────────────────────────────────────────────────

    @Test
    fun reset_deepStack_clearsToSingleRoot() {
        val snapshot = snapshotOf(HomeRoute, DetailRoute("1"), SettingsRoute)
        val result = reduce(snapshot, BackstackAction.Reset(ProfileRoute))
        assertEquals(1, result.depth)
        assertTrue(result.active?.route is ProfileRoute)
        assertEquals(NavLifecycleState.RESUMED, result.active?.lifecycleState)
    }

    // ── PopTo ────────────────────────────────────────────────────────────────

    @Test
    fun popTo_exclusiveMatch_stopsBeforeMatchingEntry() {
        val snapshot = snapshotOf(HomeRoute, DetailRoute("1"), SettingsRoute, ProfileRoute)
        val result = reduce(snapshot, BackstackAction.PopTo(DetailRoute::class, inclusive = false))
        assertEquals(2, result.depth)
        assertTrue(result.active?.route is DetailRoute)
    }

    @Test
    fun popTo_inclusiveMatch_removesMatchingEntryToo() {
        val snapshot = snapshotOf(HomeRoute, DetailRoute("1"), SettingsRoute)
        val result = reduce(snapshot, BackstackAction.PopTo(DetailRoute::class, inclusive = true))
        assertEquals(1, result.depth)
        assertTrue(result.active?.route is HomeRoute)
    }

    @Test
    fun popTo_noMatchingEntry_isNoOp() {
        val snapshot = snapshotOf(HomeRoute, SettingsRoute)
        val result = reduce(snapshot, BackstackAction.PopTo(DetailRoute::class, inclusive = false))
        assertEquals(snapshot, result)
    }

    @Test
    fun popTo_inclusiveOnOnlyMatch_preservesSingleEntry() {
        val snapshot = snapshotOf(HomeRoute, DetailRoute("1"))
        val result = reduce(snapshot, BackstackAction.PopTo(HomeRoute::class, inclusive = true))
        // Inclusive pop of the only remaining entry would leave empty — implementation guards against this
        assertTrue(result.entries.isNotEmpty())
    }

    // ── Invariants ───────────────────────────────────────────────────────────

    @Test
    fun snapshot_multiEntryStack_exactlyOneEntryIsResumed() {
        val snapshot = snapshotOf(HomeRoute, DetailRoute("1"), SettingsRoute)
        val resumedCount = snapshot.entries.count { it.lifecycleState == NavLifecycleState.RESUMED }
        assertEquals(1, resumedCount)
    }

    @Test
    fun canPop_singleEntry_returnsFalse() {
        val single = snapshotOf(HomeRoute)
        assertFalse(single.canPop)
    }

    @Test
    fun canPop_multipleEntries_returnsTrue() {
        val multiple = snapshotOf(HomeRoute, DetailRoute("1"))
        assertTrue(multiple.canPop)
    }

    // ── Purity: same inputs → same outputs ──────────────────────────────────

    @Test
    fun reduce_deterministicFactoryIdenticalActionSequences_producesIdenticalSnapshots() {
        fun makeReducer(): Reducer {
            var seed = 0
            return DefaultReducer(
                entryFactory =
                    EntryFactory { route, transition ->
                        RouteEntry(
                            id = "e-${seed++}",
                            route = route,
                            createdAt = 42L,
                            lifecycleState = NavLifecycleState.RESUMED,
                            transitionKey = transition
                        )
                    }
            )
        }

        val initial =
            BackstackSnapshot(
                listOf(RouteEntry("e-init", HomeRoute, 42L, NavLifecycleState.RESUMED))
            )

        fun applySequence(r: Reducer): BackstackSnapshot {
            var s = initial
            s = r.reduce(s, BackstackAction.Push(DetailRoute("1")))
            s = r.reduce(s, BackstackAction.Push(SettingsRoute))
            s = r.reduce(s, BackstackAction.Pop())
            return s
        }

        assertEquals(applySequence(makeReducer()), applySequence(makeReducer()))
    }
}
