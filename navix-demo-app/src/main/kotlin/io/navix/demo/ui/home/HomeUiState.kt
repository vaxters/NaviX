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

import io.navix.demo.data.model.Product

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val products: List<Product>) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

sealed interface HomeNavEffect {
    data class OpenProductDetail(
        val productId: String
    ) : HomeNavEffect

    data object OpenProfile : HomeNavEffect

    data object OpenTelemetry : HomeNavEffect

    data class HandleDeepLink(
        val uri: String
    ) : HomeNavEffect
}
