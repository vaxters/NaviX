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
package io.navix.demo.domain.usecase

import io.navix.demo.data.model.Product
import io.navix.demo.data.repository.ProductRepository

/**
 * Fetches a single [Product] by its [id], returning `null` inside [Result] when
 * the product does not exist and a [Failure][Result.isFailure] when the data source
 * throws.
 */
class GetProductByIdUseCase(
    private val repository: ProductRepository,
) {
    suspend operator fun invoke(id: String): Result<Product?> = runCatching { repository.getProduct(id) }
}
