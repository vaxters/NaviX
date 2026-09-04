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
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import io.navix.contracts.NavTransitionKey
import io.navix.contracts.RouteEntry

/**
 * Built-in [NavTransitionSpec] implementations and factory functions.
 *
 * Use [NavTransitionKey] constants to select a built-in transition when calling
 * [io.navix.runtime.Navigator.push] or [io.navix.runtime.Navigator.replace].
 * Combine with a custom [NavTransitionSpec] registered in [NavixHost] for fully
 * bespoke animations.
 */
object NavTransitions {
    private const val DURATION_MS = 300

    // Shared-axis transitions travel a fraction of the full slide distance (Material's
    // "shared axis" pattern is a smaller, fade-heavy translation, not a full off-screen
    // slide like SlideLeft/SlideRight).
    private const val SHARED_AXIS_FRACTION = 4

    internal val DefaultSpec: NavTransitionSpec =
        object : NavTransitionSpec {
            override fun enterTransition(from: RouteEntry?, to: RouteEntry, key: NavTransitionKey): EnterTransition {
                return when (key) {
                    NavTransitionKey.SlideLeft -> slideInHorizontally(tween(DURATION_MS)) { it }
                    NavTransitionKey.SlideRight -> slideInHorizontally(tween(DURATION_MS)) { -it }
                    NavTransitionKey.Scale ->
                        scaleIn(tween(DURATION_MS), initialScale = 0.9f) +
                            fadeIn(tween(DURATION_MS))

                    NavTransitionKey.SharedAxisX ->
                        slideInHorizontally(tween(DURATION_MS)) { it / SHARED_AXIS_FRACTION } +
                            fadeIn(tween(DURATION_MS))

                    NavTransitionKey.SharedAxisY ->
                        slideInVertically(tween(DURATION_MS)) { it / SHARED_AXIS_FRACTION } +
                            fadeIn(tween(DURATION_MS))

                    NavTransitionKey.SharedAxisZ ->
                        scaleIn(tween(DURATION_MS), initialScale = 0.92f) +
                            fadeIn(tween(DURATION_MS))

                    NavTransitionKey.None -> EnterTransition.None
                    else -> fadeIn(tween(DURATION_MS))
                }
            }

            override fun exitTransition(from: RouteEntry, to: RouteEntry?, key: NavTransitionKey): ExitTransition {
                return when (key) {
                    NavTransitionKey.SlideLeft -> slideOutHorizontally(tween(DURATION_MS)) { -it }
                    NavTransitionKey.SlideRight -> slideOutHorizontally(tween(DURATION_MS)) { it }
                    NavTransitionKey.Scale ->
                        scaleOut(tween(DURATION_MS), targetScale = 1.1f) +
                            fadeOut(tween(DURATION_MS))

                    NavTransitionKey.SharedAxisX ->
                        slideOutHorizontally(tween(DURATION_MS)) { -it / SHARED_AXIS_FRACTION } +
                            fadeOut(tween(DURATION_MS))

                    NavTransitionKey.SharedAxisY ->
                        slideOutVertically(tween(DURATION_MS)) { -it / SHARED_AXIS_FRACTION } +
                            fadeOut(tween(DURATION_MS))

                    NavTransitionKey.SharedAxisZ ->
                        scaleOut(tween(DURATION_MS), targetScale = 1.08f) +
                            fadeOut(tween(DURATION_MS))

                    NavTransitionKey.None -> ExitTransition.None
                    else -> fadeOut(tween(DURATION_MS))
                }
            }
        }

    internal val NoneSpec: NavTransitionSpec =
        object : NavTransitionSpec {
            override fun enterTransition(from: RouteEntry?, to: RouteEntry, key: NavTransitionKey) = EnterTransition.None

            override fun exitTransition(from: RouteEntry, to: RouteEntry?, key: NavTransitionKey) = ExitTransition.None
        }
}
