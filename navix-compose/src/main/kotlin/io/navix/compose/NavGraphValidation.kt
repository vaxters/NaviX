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

import io.navix.contracts.Route
import io.navix.runtime.NavixRouteRegistry

/**
 * Validates that every route registered with `@RouteDestination` in [registries] has a
 * corresponding [screen] block registered in this [NavGraphBuilder].
 *
 * Call this inside the [NavixHost] content lambda — ideally gated on `BuildConfig.DEBUG`
 * so it runs only in debug builds and has zero production overhead. The check runs at
 * first composition when the graph is built, catching missing registrations before the
 * user navigates to the route.
 *
 * Routes that are intentionally **not** in the main NavixHost (e.g., tab-internal routes
 * in [NavixMultiStackHost]) should not carry `@RouteDestination`. Dialog and bottom-sheet
 * destinations registered with [NavGraphBuilder.dialog] or [NavGraphBuilder.bottomSheet]
 * **are** counted as registered — they appear in [NavGraphBuilderImpl.destinations] and
 * will pass validation without any extra exclusions.
 *
 * ### Usage
 * ```kotlin
 * // In DemoNavHost.kt (or similar)
 * NavixHost(navigator = navigator) {
 *     screen<Home> { _, _ -> HomeScreen() }
 *     screen<ProductDetail> { _, route -> ProductDetailScreen(route.productId) }
 *     // ... all other screens
 *
 *     // Must be last — NavGraphBuilderImpl.apply(content) runs sequentially, so
 *     // calling this before screen { } registrations would see an empty destinations
 *     // map and report every route as missing.
 *     if (BuildConfig.DEBUG) {
 *         validateAgainst(DemoNavixRouteRegistry)
 *     }
 * }
 * ```
 *
 * @param registries One or more [NavixRouteRegistry] instances to validate against.
 *   Multi-module apps typically pass one registry per feature module.
 * @throws IllegalStateException if any route FQN in [registries] has no registered
 *   screen and its class can be loaded on the current classpath.
 */
fun NavGraphBuilder.validateAgainst(vararg registries: NavixRouteRegistry) {
    val impl = this as? NavGraphBuilderImpl ?: return
    val registeredClasses = impl.destinations.keys

    for (registry in registries) {
        val missing = mutableListOf<String>()
        for (fqn in registry.destinations) {
            val klass = runCatching {
                @Suppress("UNCHECKED_CAST")
                Class.forName(fqn).kotlin as kotlin.reflect.KClass<out Route>
            }.getOrNull() ?: continue  // skip if class can't be loaded (e.g., stripped by R8)

            if (klass !in registeredClasses) {
                missing += fqn
            }
        }
        check(missing.isEmpty()) {
            buildString {
                appendLine("NavixHost is missing screen registrations for the following @RouteDestination routes:")
                missing.forEach { appendLine("  • $it") }
                appendLine()
                append(
                    "Add a screen<RouteName> { }, dialog<RouteName> { }, or bottomSheet<RouteName> { } " +
                        "block for each missing route, or remove @RouteDestination if the route " +
                        "is intentionally excluded from this NavixHost (e.g., tab-internal routes)."
                )
            }
        }
    }
}
