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
package io.navix.runtime

import io.navix.contracts.Route

/**
 * Resolves a URI string to a [Route] for deep link navigation.
 *
 * Handlers are evaluated in registration order by [Navigator.handleDeepLink].
 * The first handler whose [canHandle] returns true wins — subsequent handlers
 * are not consulted. There is no fallback magic.
 *
 * KSP-generated handlers implement this interface automatically for each
 * [io.navix.annotations.RouteDestination] that declares deep link patterns.
 * Manual implementations are also fully supported.
 *
 * Example:
 * ```kotlin
 * class ProductDeepLinkHandler : DeepLinkHandler {
 *     private val pattern = Regex("myapp://product/(?<id>[^/]+)")
 *     override fun canHandle(uri: String) = pattern.matches(uri)
 *     override fun resolve(uri: String): Route? {
 *         val id = pattern.find(uri)?.groups?.get("id")?.value ?: return null
 *         return ProductDetail(productId = id)
 *     }
 * }
 * ```
 */
interface DeepLinkHandler {
    fun canHandle(uri: String): Boolean

    fun resolve(uri: String): Route?
}
