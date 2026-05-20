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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.navix.demo.domain.usecase.GetProductReviewsUseCase
import io.navix.demo.ui.loadUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow

class ProductReviewsViewModel(
    private val productId: String,
    getProductReviews: GetProductReviewsUseCase
) : ViewModel() {
    val uiState: StateFlow<ProductReviewsUiState> = viewModelScope.loadUiState(
        initial = ProductReviewsUiState.Loading(productId),
        load = { getProductReviews(productId) },
        onSuccess = { ProductReviewsUiState.Success(productId, it) },
        onFailure = { ProductReviewsUiState.Error(productId, it.message ?: "Failed to load reviews") },
    )

    private val _navEffect = Channel<ProductReviewsNavEffect>(Channel.BUFFERED)
    val navEffect = _navEffect.receiveAsFlow()

    fun onBack() {
        _navEffect.trySend(ProductReviewsNavEffect.NavigateBack)
    }

    fun onJumpToHome() {
        _navEffect.trySend(ProductReviewsNavEffect.JumpToHome)
    }
}
