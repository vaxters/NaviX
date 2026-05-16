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
package io.navix.testing

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import io.navix.compose.NavGraphBuilder
import io.navix.compose.NavixHost
import io.navix.contracts.Route

/**
 * Sets [NavixHost] as the test content, driving it with the provided [FakeNavigator].
 *
 * Use this when you want to create the navigator yourself (e.g. to pre-configure routes
 * or check its state after interactions):
 *
 * ```kotlin
 * @get:Rule val composeRule = createComposeRule()
 *
 * @Test
 * fun navigatesToDetail() {
 *     val navigator = FakeNavigator(root = Home)
 *     composeRule.setNavixContent(navigator) {
 *         screen<Home> { _, _ -> HomeScreen(navigator) }
 *         screen<Detail> { _, route -> DetailScreen(route.id, navigator) }
 *     }
 *     composeRule.onNodeWithText("Open Detail").performClick()
 *     navigator.assertCurrentRoute(Detail("1"))
 * }
 * ```
 *
 * @param navigator The [FakeNavigator] that drives the test navigation.
 * @param content DSL for registering screen composables. Evaluated once at first composition.
 */
fun ComposeContentTestRule.setNavixContent(
    navigator: FakeNavigator,
    content: NavGraphBuilder.() -> Unit,
) {
    setContent {
        NavixHost(navigator = navigator, content = content)
    }
}

/**
 * Sets [NavixHost] as the test content with a freshly created [FakeNavigator], returning the
 * navigator for state assertions.
 *
 * ```kotlin
 * @get:Rule val composeRule = createComposeRule()
 *
 * @Test
 * fun navigatesToDetail() {
 *     val navigator = composeRule.setNavixContent(root = Home) {
 *         screen<Home> { _, _ -> HomeScreen() }
 *         screen<Detail> { _, _ -> DetailScreen() }
 *     }
 *     composeRule.onNodeWithText("Open Detail").performClick()
 *     navigator.assertCurrentRoute(Detail("1"))
 * }
 * ```
 *
 * @param root The root route the navigator starts on.
 * @param content DSL for registering screen composables. Evaluated once at first composition.
 * @return The [FakeNavigator] created for this test — use it to assert navigation calls.
 */
fun ComposeContentTestRule.setNavixContent(
    root: Route,
    content: NavGraphBuilder.() -> Unit,
): FakeNavigator {
    val navigator = FakeNavigator(root)
    setContent {
        NavixHost(navigator = navigator, content = content)
    }
    return navigator
}
