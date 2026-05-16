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
package io.navix.compose

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.ui.Modifier
import io.navix.contracts.NavTransitionKey
import io.navix.contracts.RouteEntry
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame

/**
 * Unit tests for [NavTransitionSpec] predictive-back additions.
 *
 * These tests verify the contract and constants of the predictive-back API without
 * requiring a real Compose UI environment. Behaviour of the applied graphicsLayer
 * transform is covered by the instrumented [NavixHostPredictiveBackTest].
 */
class NavTransitionSpecPredictiveBackTest {
    // ── Edge constants ─────────────────────────────────────────────────────

    @Test
    fun swipeEdgeLeft_matchesBackEventCompatEdgeLeft() {
        // BackEventCompat.EDGE_LEFT == 0
        assertEquals(0, NavTransitionSpec.SWIPE_EDGE_LEFT)
    }

    @Test
    fun swipeEdgeRight_matchesBackEventCompatEdgeRight() {
        // BackEventCompat.EDGE_RIGHT == 1
        assertEquals(1, NavTransitionSpec.SWIPE_EDGE_RIGHT)
    }

    // ── Default implementation exists and is non-null ──────────────────────

    @Test
    fun predictiveExit_defaultImpl_returnsNonNullModifier() {
        val spec = NavTransitionSpec.Default
        val result: Modifier =
            spec.predictiveExit(
                from = fakeEntry("e1"),
                to = fakeEntry("e2"),
                key = NavTransitionKey.Default,
                progress = 0.5f,
                swipeEdge = NavTransitionSpec.SWIPE_EDGE_LEFT,
            )
        assertNotNull(result)
    }

    @Test
    fun predictiveExit_progressZero_returnsModifier() {
        val result =
            NavTransitionSpec.Default.predictiveExit(
                from = fakeEntry("e1"),
                to = null,
                key = NavTransitionKey.SlideLeft,
                progress = 0f,
                swipeEdge = NavTransitionSpec.SWIPE_EDGE_LEFT,
            )
        assertNotNull(result)
    }

    @Test
    fun predictiveExit_progressOne_returnsModifier() {
        val result =
            NavTransitionSpec.Default.predictiveExit(
                from = fakeEntry("e1"),
                to = null,
                key = NavTransitionKey.SlideRight,
                progress = 1f,
                swipeEdge = NavTransitionSpec.SWIPE_EDGE_RIGHT,
            )
        assertNotNull(result)
    }

    // ── NoneSpec inherits default ──────────────────────────────────────────

    @Test
    fun predictiveExit_noneSpec_returnsNonNullModifier() {
        val result =
            NavTransitionSpec.None.predictiveExit(
                from = fakeEntry("e1"),
                to = null,
                key = NavTransitionKey.None,
                progress = 0.5f,
                swipeEdge = NavTransitionSpec.SWIPE_EDGE_RIGHT,
            )
        assertNotNull(result)
    }

    // ── Custom spec can override ───────────────────────────────────────────

    @Test
    fun predictiveExit_customOverride_calledWithCorrectArguments() {
        var capturedProgress = -1f
        var capturedEdge = -1

        val customSpec =
            object : NavTransitionSpec {
                override fun enterTransition(
                    from: RouteEntry?,
                    to: RouteEntry,
                    key: NavTransitionKey,
                ) = EnterTransition.None

                override fun exitTransition(
                    from: RouteEntry,
                    to: RouteEntry?,
                    key: NavTransitionKey,
                ) = ExitTransition.None

                override fun predictiveExit(
                    from: RouteEntry,
                    to: RouteEntry?,
                    key: NavTransitionKey,
                    progress: Float,
                    swipeEdge: Int,
                ): Modifier {
                    capturedProgress = progress
                    capturedEdge = swipeEdge
                    return Modifier
                }
            }

        customSpec.predictiveExit(
            from = fakeEntry("e1"),
            to = null,
            key = NavTransitionKey.Default,
            progress = 0.75f,
            swipeEdge = NavTransitionSpec.SWIPE_EDGE_RIGHT,
        )

        assertEquals(0.75f, capturedProgress, absoluteTolerance = 0.001f)
        assertEquals(NavTransitionSpec.SWIPE_EDGE_RIGHT, capturedEdge)
    }

    @Test
    fun predictiveExit_customOverrideReturningModifierIdentity_returnsSameInstance() {
        val sentinel = Modifier

        val customSpec =
            object : NavTransitionSpec {
                override fun enterTransition(
                    from: RouteEntry?,
                    to: RouteEntry,
                    key: NavTransitionKey,
                ) = EnterTransition.None

                override fun exitTransition(
                    from: RouteEntry,
                    to: RouteEntry?,
                    key: NavTransitionKey,
                ) = ExitTransition.None

                override fun predictiveExit(
                    from: RouteEntry,
                    to: RouteEntry?,
                    key: NavTransitionKey,
                    progress: Float,
                    swipeEdge: Int,
                ): Modifier = sentinel
            }

        val result =
            customSpec.predictiveExit(
                from = fakeEntry("e1"),
                to = null,
                key = NavTransitionKey.Default,
                progress = 0.5f,
                swipeEdge = NavTransitionSpec.SWIPE_EDGE_LEFT,
            )

        assertSame(sentinel, result)
    }

    // ── Both edges accepted ────────────────────────────────────────────────

    @Test
    fun predictiveExit_leftEdge_doesNotThrow() {
        NavTransitionSpec.Default.predictiveExit(
            from = fakeEntry("e1"),
            to = null,
            key = NavTransitionKey.Default,
            progress = 0.3f,
            swipeEdge = NavTransitionSpec.SWIPE_EDGE_LEFT,
        )
    }

    @Test
    fun predictiveExit_rightEdge_doesNotThrow() {
        NavTransitionSpec.Default.predictiveExit(
            from = fakeEntry("e1"),
            to = null,
            key = NavTransitionKey.Default,
            progress = 0.3f,
            swipeEdge = NavTransitionSpec.SWIPE_EDGE_RIGHT,
        )
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun fakeEntry(id: String) =
        RouteEntry(
            id = id,
            route = FakeRoute,
            createdAt = 0L,
        )

    private object FakeRoute : io.navix.contracts.Route
}
