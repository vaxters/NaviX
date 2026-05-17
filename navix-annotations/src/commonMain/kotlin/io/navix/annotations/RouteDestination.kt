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
package io.navix.annotations

/**
 * Marks a [io.navix.contracts.Route] implementation as a navigable destination.
 *
 * The KSP processor in `navix-compiler` discovers all classes annotated with
 * [RouteDestination] and generates:
 *  - A `NavixRouteRegistry` entry for each destination
 *  - [DeepLinkHandler] implementations for each entry in [deepLinks]
 *
 * Usage:
 * ```kotlin
 * @Serializable
 * @RouteDestination(deepLinks = ["myapp://product/{productId}"])
 * data class ProductDetail(val productId: String) : Route
 * ```
 *
 * [route] is the canonical identifier for this destination. Defaults to the
 * fully qualified class name when left empty.
 *
 * [deepLinks] is an ordered list of URI templates this destination responds to.
 * Template syntax: `scheme://host/path/{param}?query={queryParam}`.
 *
 * **Multi-module scoping:** In multi-module projects, set the `navix.moduleName`
 * KSP argument in each subproject's `build.gradle.kts` to give the generated
 * registry a unique name (e.g. `CheckoutNavixRouteRegistry`):
 * ```kotlin
 * ksp { arg("navix.moduleName", "Checkout") }
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class RouteDestination(
    val route: String = "",
    val deepLinks: Array<String> = [],
)
