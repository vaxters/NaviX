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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProductReviewsViewModel(
    private val productId: String,
    private val getProductReviews: GetProductReviewsUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProductReviewsUiState(productId = productId))
    val uiState: StateFlow<ProductReviewsUiState> = _uiState.asStateFlow()

    private val _navEffect = Channel<ProductReviewsNavEffect>(Channel.BUFFERED)
    val navEffect = _navEffect.receiveAsFlow()

    init {
        loadReviews()
    }

    private fun loadReviews() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            getProductReviews(productId)
                .onSuccess { reviews ->
                    _uiState.update { it.copy(reviews = reviews, isLoading = false) }
                }.onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun onBack() {
        viewModelScope.launch {
            _navEffect.send(ProductReviewsNavEffect.NavigateBack)
        }
    }

    fun onJumpToHome() {
        viewModelScope.launch {
            _navEffect.send(ProductReviewsNavEffect.JumpToHome)
        }
    }
}
