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
package io.navix.demo.ui.product.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.navix.demo.domain.usecase.GetProductByIdUseCase
import io.navix.demo.ui.loadUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow

class ProductDetailViewModel(
    private val productId: String,
    getProductById: GetProductByIdUseCase
) : ViewModel() {
    val uiState: StateFlow<ProductDetailUiState> = viewModelScope.loadUiState(
        initial = ProductDetailUiState.Loading,
        load = { getProductById(productId) },
        onSuccess = { product ->
            if (product != null) {
                ProductDetailUiState.Success(product)
            } else {
                ProductDetailUiState.Error("Product not found")
            }
        },
        onFailure = { ProductDetailUiState.Error(it.message ?: "Failed to load product") },
    )

    private val _navEffect = Channel<ProductDetailNavEffect>(Channel.BUFFERED)
    val navEffect = _navEffect.receiveAsFlow()

    fun onReadReviews() {
        _navEffect.trySend(ProductDetailNavEffect.OpenReviews(productId))
    }

    fun onBack() {
        _navEffect.trySend(ProductDetailNavEffect.NavigateBack)
    }

    fun onBackToHome() {
        _navEffect.trySend(ProductDetailNavEffect.NavigateBackToHome)
    }
}
