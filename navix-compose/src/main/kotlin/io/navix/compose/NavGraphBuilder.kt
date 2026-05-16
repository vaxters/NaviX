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
import androidx.compose.runtime.Stable
import io.navix.contracts.Route
import io.navix.contracts.RouteEntry
import kotlin.reflect.KClass

/**
 * Classifies how a registered destination is rendered by [NavixHost].
 *
 * - [Screen] — rendered inside [AnimatedContent] as a standard full-screen (or arbitrary-size)
 *   destination. The previous screen stays mounted below it in the Z-order.
 * - [Dialog] — rendered in a [androidx.compose.ui.window.Dialog] container above the topmost
 *   screen. The screen below remains composed and fully interactive.
 * - [BottomSheet] — rendered in a [androidx.compose.material3.ModalBottomSheet] above the
 *   topmost screen. The screen below remains composed and fully interactive.
 *
 * Register destinations with the correct kind via [NavGraphBuilder.screen],
 * [NavGraphBuilder.dialog], or [NavGraphBuilder.bottomSheet].
 */
enum class DestinationKind { Screen, Dialog, BottomSheet }

/**
 * DSL receiver for registering screen composables in [NavixHost].
 *
 * Each registration call maps a [Route] type to a composable content lambda and a
 * [DestinationKind] that controls how [NavixHost] renders the destination:
 *
 * - [screen] — full-screen/arbitrary-size destination inside [AnimatedContent].
 * - [dialog] — rendered as a floating [androidx.compose.ui.window.Dialog] above the current screen.
 * - [bottomSheet] — rendered as a [androidx.compose.material3.ModalBottomSheet] above the
 *   current screen.
 *
 * Registering the same type twice overwrites the previous registration without error — the last
 * registration wins.
 *
 * The routing graph is **immutable after first composition** — the DSL lambda passed to
 * [NavixHost] is evaluated exactly once. Conditional registrations (e.g.,
 * `if (BuildConfig.DEBUG) { screen<Debug> { ... } }`) work because the condition is evaluated
 * at that first composition time.
 *
 * ### Example
 * ```kotlin
 * NavixHost(navigator = navigator) {
 *     screen<Home> { _, _ -> HomeScreen() }
 *     screen<ProductDetail> { _, route -> ProductDetailScreen(route.id) }
 *     dialog<ConfirmDelete> { _, route -> ConfirmDeleteDialog(route.itemId) }
 *     bottomSheet<FilterOptions> { _, route -> FilterOptionsSheet(route.category) }
 * }
 * ```
 */
@Stable
interface NavGraphBuilder {
    /**
     * Registers [klass] as a standard screen destination rendered by [AnimatedContent].
     */
    fun <T : Route> screen(
        klass: KClass<T>,
        content: @Composable (entry: RouteEntry, route: T) -> Unit,
    )

    /**
     * Registers [klass] as a dialog destination rendered in a
     * [androidx.compose.ui.window.Dialog] above the current screen.
     *
     * Dismissing the dialog (via system back or the dialog's `onDismissRequest` callback)
     * calls [io.navix.runtime.Navigator.pop], returning to the screen below.
     */
    fun <T : Route> dialog(
        klass: KClass<T>,
        content: @Composable (entry: RouteEntry, route: T) -> Unit,
    )

    /**
     * Registers [klass] as a bottom-sheet destination rendered in a
     * [androidx.compose.material3.ModalBottomSheet] above the current screen.
     *
     * Dismissing the sheet (via swipe or system back) calls [io.navix.runtime.Navigator.pop],
     * returning to the screen below.
     */
    fun <T : Route> bottomSheet(
        klass: KClass<T>,
        content: @Composable (entry: RouteEntry, route: T) -> Unit,
    )
}

inline fun <reified T : Route> NavGraphBuilder.screen(
    noinline content: @Composable (entry: RouteEntry, route: T) -> Unit,
) = screen(T::class, content)

inline fun <reified T : Route> NavGraphBuilder.dialog(
    noinline content: @Composable (entry: RouteEntry, route: T) -> Unit,
) = dialog(T::class, content)

inline fun <reified T : Route> NavGraphBuilder.bottomSheet(
    noinline content: @Composable (entry: RouteEntry, route: T) -> Unit,
) = bottomSheet(T::class, content)

internal class NavGraphBuilderImpl : NavGraphBuilder {
    val destinations = mutableMapOf<KClass<out Route>, @Composable (RouteEntry, Route) -> Unit>()

    /** Tracks the [DestinationKind] for each registered route class. */
    val destinationKinds = mutableMapOf<KClass<out Route>, DestinationKind>()

    override fun <T : Route> screen(
        klass: KClass<T>,
        content: @Composable (entry: RouteEntry, route: T) -> Unit,
    ) = register(klass, DestinationKind.Screen, content)

    override fun <T : Route> dialog(
        klass: KClass<T>,
        content: @Composable (entry: RouteEntry, route: T) -> Unit,
    ) = register(klass, DestinationKind.Dialog, content)

    override fun <T : Route> bottomSheet(
        klass: KClass<T>,
        content: @Composable (entry: RouteEntry, route: T) -> Unit,
    ) = register(klass, DestinationKind.BottomSheet, content)

    private fun <T : Route> register(
        klass: KClass<T>,
        kind: DestinationKind,
        content: @Composable (entry: RouteEntry, route: T) -> Unit,
    ) {
        destinations[klass] = { entry, route ->
            @Suppress("UNCHECKED_CAST")
            content(entry, route as T)
        }
        destinationKinds[klass] = kind
    }
}
