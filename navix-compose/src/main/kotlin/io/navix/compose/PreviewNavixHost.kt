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

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.navix.contracts.NavLifecycleState
import io.navix.contracts.NavTransitionKey
import io.navix.contracts.Route
import io.navix.contracts.RouteEntry

/**
 * A lightweight Compose Preview-friendly host that renders a single screen without a
 * live [io.navix.runtime.Navigator] or backstack state machine.
 *
 * Use this in `@Preview` functions to render screen composables in Android Studio's
 * design pane. It creates a synthetic [RouteEntry] for [route] and invokes [content]
 * directly — no animation, no back-press handling.
 *
 * Pair with [io.navix.testing.FakeNavigator.Companion.preview] to satisfy composables
 * that require a [io.navix.runtime.Navigator] reference:
 *
 * ```kotlin
 * @Preview
 * @Composable
 * fun HomeScreenPreview() {
 *     PreviewNavixHost(route = Home) { entry, route ->
 *         HomeScreen(
 *             navigator = FakeNavigator.preview(Home),
 *             viewModel = HomeViewModel.forPreview(),
 *         )
 *     }
 * }
 * ```
 *
 * @param route The route to render. Used to construct the synthetic [RouteEntry].
 * @param modifier Applied to the content slot.
 * @param content The screen composable. Receives the synthetic [RouteEntry] and the
 *   typed [Route], matching the signature expected by [NavGraphBuilder.screen].
 */
@Composable
fun <T : Route> PreviewNavixHost(
    route: T,
    modifier: Modifier = Modifier,
    content: @Composable (entry: RouteEntry, route: T) -> Unit,
) {
    val previewEntry = RouteEntry(
        id = "preview",
        route = route,
        createdAt = 0L,
        lifecycleState = NavLifecycleState.RESUMED,
        transitionKey = NavTransitionKey.Default,
    )
    Box(modifier = modifier) {
        content(previewEntry, route)
    }
}
