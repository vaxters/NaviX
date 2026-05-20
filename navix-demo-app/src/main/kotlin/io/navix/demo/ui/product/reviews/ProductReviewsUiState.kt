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
package io.navix.demo.ui.product.reviews

import io.navix.demo.data.model.Review

sealed interface ProductReviewsUiState {
    val productId: String

    data class Loading(override val productId: String) : ProductReviewsUiState
    data class Success(override val productId: String, val reviews: List<Review>) : ProductReviewsUiState
    data class Error(override val productId: String, val message: String) : ProductReviewsUiState
}

sealed interface ProductReviewsNavEffect {
    data object NavigateBack : ProductReviewsNavEffect

    data object JumpToHome : ProductReviewsNavEffect
}
