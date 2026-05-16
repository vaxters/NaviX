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
package io.navix.contracts

/**
 * Marker interface for all navigation destinations.
 *
 * Every route must be a `@Serializable` data class or object implementing this interface.
 * Routes are the sole source of truth for navigation state — there are no string paths at runtime.
 *
 * ### Serialization
 * Non-sealed interfaces are polymorphically serializable by default in kotlinx-serialization —
 * no `@Serializable` annotation is needed on `Route` itself. Fields typed as `Route` carry
 * `@Polymorphic` (see [RouteEntry.route]) so the runtime uses [PolymorphicSerializer].
 * Concrete route implementations are registered in a per-module [SerializersModule] generated
 * by the Navix KSP compiler (see `navix-compiler`).
 *
 * Example:
 * ```kotlin
 * @Serializable
 * data class ProductDetail(val productId: String) : Route
 *
 * @Serializable
 * data object Home : Route
 * ```
 */
interface Route
