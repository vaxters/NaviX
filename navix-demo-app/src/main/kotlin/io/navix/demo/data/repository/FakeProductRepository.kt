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
package io.navix.demo.data.repository

import io.navix.demo.data.model.Product
import io.navix.demo.data.model.Review
import kotlinx.coroutines.delay

class FakeProductRepository : ProductRepository {
    private val products =
        listOf(
            Product(id = "p-001", name = "Kotlin Multiplatform Guide", rating = 4.8f, reviewCount = 128),
            Product(id = "p-002", name = "Compose Animation Cookbook", rating = 4.6f, reviewCount = 95),
            Product(id = "p-003", name = "Architecture Deep Dive", rating = 4.9f, reviewCount = 214),
            Product(id = "p-004", name = "KSP Internals", rating = 4.5f, reviewCount = 63),
            Product(id = "p-005", name = "State Management Patterns", rating = 4.7f, reviewCount = 177)
        )

    override suspend fun getProducts(): List<Product> {
        delay(300) // Simulate network latency
        return products
    }

    override suspend fun getProduct(id: String): Product? {
        delay(200)
        return products.find { it.id == id }
    }

    override suspend fun getReviews(productId: String): List<Review> {
        delay(250)
        return List(10) { index ->
            Review(
                id = "$productId-review-${index + 1}",
                author = "User ${index + 1}",
                body = "Great product! Navix navigation feels very clean.",
                rating = if (index % 5 == 0) 4 else 5
            )
        }
    }
}
