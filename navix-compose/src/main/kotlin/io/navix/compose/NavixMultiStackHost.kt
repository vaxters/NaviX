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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

/**
 * Renders the currently active tab's navigation stack.
 *
 * Delegates entirely to [NavixHost], passing the active tab's [Navigator] on each
 * recomposition triggered by [NavixMultiStack.activeTabIndex]. The [content] DSL is
 * shared across all tabs — every route registered in [content] is available in every
 * tab. Each tab's [Navigator] independently determines which screen is shown based on
 * its own backstack.
 *
 * ### Navigation state preservation
 * Each tab's [Navigator] retains its backstack across tab switches. Composable-local
 * state (scroll position, text input) is **not** preserved when switching away and
 * back. Wrap tab content with [androidx.compose.runtime.saveable.SaveableStateHolder]
 * if composable state preservation is required.
 *
 * ### Back handling
 * [NavixHost] installs a [androidx.activity.compose.BackHandler] that pops the active
 * tab's stack when it has more than one entry. When the active tab is at its root, back
 * is not intercepted and propagates to the parent — typically the system back (Activity
 * finish) or an outer [NavixHost].
 *
 * ### Routing graph
 * The [content] DSL is evaluated once on first composition and frozen (same contract as
 * [NavixHost]). Registrations for tab-internal routes — routes that are only ever pushed
 * within a specific tab — should still be included here. They are unreachable from other
 * tabs only because no other tab's [Navigator] will ever push them.
 *
 * ### Usage
 * ```kotlin
 * val multiStack = rememberNavixMultiStack(
 *     specs = listOf(
 *         NavStackSpec(root = HomeTab),
 *         NavStackSpec(root = ProfileTab),
 *         NavStackSpec(root = SettingsTab),
 *     )
 * )
 *
 * Scaffold(
 *     bottomBar = {
 *         NavigationBar {
 *             val activeIndex by multiStack.activeTabIndex.collectAsState()
 *             tabs.forEachIndexed { i, tab ->
 *                 NavigationBarItem(
 *                     selected = activeIndex == i,
 *                     onClick = { multiStack.selectTab(i) },
 *                     icon = { Icon(tab.icon, contentDescription = null) },
 *                     label = { Text(tab.label) },
 *                 )
 *             }
 *         }
 *     }
 * ) { padding ->
 *     NavixMultiStackHost(
 *         multiStack = multiStack,
 *         modifier = Modifier.padding(padding),
 *     ) {
 *         screen<HomeTab> { _, _ -> HomeTabScreen(navigator = multiStack.navigators[0]) }
 *         screen<ProfileTab> { _, _ -> ProfileTabScreen(navigator = multiStack.navigators[1]) }
 *         screen<SettingsTab> { _, _ -> SettingsTabScreen(navigator = multiStack.navigators[2]) }
 *     }
 * }
 * ```
 *
 * @param multiStack The [NavixMultiStack] created via [rememberNavixMultiStack].
 * @param modifier Applied to the active [NavixHost] container.
 * @param transitionSpec Transition animations for route changes within each tab.
 * @param content DSL for registering screen composables. Immutable after first composition.
 */
@Composable
fun NavixMultiStackHost(
    multiStack: NavixMultiStack,
    modifier: Modifier = Modifier,
    transitionSpec: NavTransitionSpec = NavTransitionSpec.Default,
    content: NavGraphBuilder.() -> Unit
) {
    val activeTabIndex by multiStack.activeTabIndex.collectAsState()

    NavixHost(
        navigator = multiStack.navigators[activeTabIndex],
        modifier = modifier,
        transitionSpec = transitionSpec,
        content = content
    )
}
