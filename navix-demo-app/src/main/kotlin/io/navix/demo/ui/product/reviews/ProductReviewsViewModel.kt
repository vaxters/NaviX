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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn

class ProductReviewsViewModel(
    private val productId: String,
    getProductReviews: GetProductReviewsUseCase
) : ViewModel() {
    val uiState: StateFlow<ProductReviewsUiState> = flow { emit(getProductReviews(productId)) }
        .map { result ->
            result.fold(
                onSuccess = { reviews -> ProductReviewsUiState.Success(productId, reviews) },
                onFailure = { error -> ProductReviewsUiState.Error(productId, error.message ?: "Failed to load reviews") }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ProductReviewsUiState.Loading(productId),
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
