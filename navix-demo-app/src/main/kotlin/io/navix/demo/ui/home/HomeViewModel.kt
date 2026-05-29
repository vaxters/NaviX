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
package io.navix.demo.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.navix.demo.domain.usecase.GetProductsUseCase
import io.navix.demo.ui.loadUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    getProducts: GetProductsUseCase
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = viewModelScope.loadUiState(
        initial = HomeUiState.Loading,
        load = { getProducts() },
        onSuccess = { HomeUiState.Success(it) },
        onFailure = { HomeUiState.Error(it.message ?: "Failed to load products") },
    )

    private val _navEffect = Channel<HomeNavEffect>(Channel.BUFFERED)
    val navEffect = _navEffect.receiveAsFlow()

    fun onProductClicked(productId: String) {
        viewModelScope.launch { _navEffect.send(HomeNavEffect.OpenProductDetail(productId)) }
    }

    fun onOpenProfile() {
        viewModelScope.launch { _navEffect.send(HomeNavEffect.OpenProfile) }
    }

    fun onOpenTelemetry() {
        viewModelScope.launch { _navEffect.send(HomeNavEffect.OpenTelemetry) }
    }

    fun onDeepLink(uri: String) {
        viewModelScope.launch { _navEffect.send(HomeNavEffect.HandleDeepLink(uri)) }
    }
}
