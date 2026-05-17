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
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import io.navix.contracts.NavTransitionKey
import io.navix.contracts.RouteEntry

/**
 * Defines enter and exit transitions for navigation between [RouteEntry]s.
 *
 * Implement this interface to customize animation behavior per [NavTransitionKey].
 * The [from] and [to] parameters provide access to the route being animated from/to,
 * enabling route-specific transition logic.
 *
 * Register a custom spec via [NavixHost]'s `transitionSpec` parameter.
 *
 * Annotated [@Stable][Stable] so the Compose compiler skips unnecessary recompositions
 * of [NavixHost] when the same spec instance is reused across recompositions. Implementations
 * must honour the Compose stability contract: equal inputs produce equal outputs.
 *
 * ### Predictive back
 * Override [predictiveExit] to customise the graphicsLayer transform applied to the [from]
 * entry frame-by-frame during a predictive back gesture. The default implementation produces
 * a standard scale-and-fade that matches the Android 14 system back gesture aesthetic.
 * Existing custom specs that do **not** override [predictiveExit] compile unchanged.
 */
@Stable
interface NavTransitionSpec {
    fun enterTransition(
        from: RouteEntry?,
        to: RouteEntry,
        key: NavTransitionKey,
    ): EnterTransition

    fun exitTransition(
        from: RouteEntry,
        to: RouteEntry?,
        key: NavTransitionKey,
    ): ExitTransition

    /**
     * Returns a [Modifier] applied to the [from] entry while a predictive back gesture is
     * in flight. Called on every frame during the gesture with [progress] in `[0f, 1f]`.
     *
     * - `progress = 0f` → gesture just started; content should look fully normal.
     * - `progress = 1f` → gesture fully committed; content should look about to leave.
     *
     * On **commit** [NavixHost] calls [io.navix.runtime.Navigator.pop] and plays the
     * standard [exitTransition]. On **cancel** [NavixHost] animates [progress] back to `0f`.
     *
     * [swipeEdge] matches `BackEventCompat.EDGE_LEFT` (`0`) or `EDGE_RIGHT` (`1`).
     *
     * The default implementation ignores [key] and applies a uniform scale-down + slight
     * horizontal drift + alpha fade, matching the Material Design predictive back aesthetic.
     * Override for key-aware or richer gesture animations.
     */
    fun predictiveExit(
        from: RouteEntry,
        to: RouteEntry?,
        key: NavTransitionKey,
        progress: Float,
        swipeEdge: Int,
    ): Modifier =
        Modifier.graphicsLayer {
            // Scale down slightly as the user drags (max 8 % reduction at full progress).
            val scale = 1f - progress * 0.08f
            scaleX = scale
            scaleY = scale
            // Drift toward the swipe edge: left edge → drift right (+X), right edge → drift left (−X).
            translationX = if (swipeEdge == SWIPE_EDGE_LEFT) progress * 72f else -progress * 72f
            // Fade slightly to hint that the screen is leaving.
            alpha = 1f - progress * 0.25f
        }

    companion object {
        /** Matches `BackEventCompat.EDGE_LEFT`. Passed as [predictiveExit]'s [swipeEdge]. */
        const val SWIPE_EDGE_LEFT = 0

        /** Matches `BackEventCompat.EDGE_RIGHT`. Passed as [predictiveExit]'s [swipeEdge]. */
        const val SWIPE_EDGE_RIGHT = 1

        /** Default spec: key-driven slide/scale/fade transitions + predictive back support. */
        val Default: NavTransitionSpec = NavTransitions.DefaultSpec

        /** No animation — instant content swap. */
        val None: NavTransitionSpec = NavTransitions.NoneSpec
    }
}
