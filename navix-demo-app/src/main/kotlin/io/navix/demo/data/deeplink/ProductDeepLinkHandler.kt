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
package io.navix.demo.data.deeplink

import io.navix.contracts.Route
import io.navix.demo.routes.ProductDetail
import io.navix.runtime.DeepLinkHandler

/**
 * Resolves product deep links of the form `navix://product/{productId}` to a [ProductDetail] route.
 */
class ProductDeepLinkHandler : DeepLinkHandler {
    private val pattern = Regex("navix://product/(?<productId>[^/?&]+)")

    override fun canHandle(uri: String): Boolean = pattern.containsMatchIn(uri)

    override fun resolve(uri: String): Route? {
        val productId = pattern.find(uri)?.groups[1]?.value ?: return null
        return ProductDetail(productId = productId)
    }
}
