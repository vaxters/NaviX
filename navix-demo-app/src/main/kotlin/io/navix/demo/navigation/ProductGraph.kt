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
package io.navix.demo.navigation

import androidx.lifecycle.viewmodel.compose.viewModel
import io.navix.compose.NavGraphBuilder
import io.navix.compose.screen
import io.navix.demo.data.repository.FakeProductRepository
import io.navix.demo.domain.usecase.GetProductByIdUseCase
import io.navix.demo.domain.usecase.GetProductReviewsUseCase
import io.navix.demo.domain.usecase.GetProductsUseCase
import io.navix.demo.routes.Home
import io.navix.demo.routes.ProductDetail
import io.navix.demo.routes.ProductReviews
import io.navix.demo.ui.home.HomeScreen
import io.navix.demo.ui.home.HomeViewModel
import io.navix.demo.ui.product.detail.ProductDetailScreen
import io.navix.demo.ui.product.detail.ProductDetailViewModel
import io.navix.demo.ui.product.reviews.ProductReviewsScreen
import io.navix.demo.ui.product.reviews.ProductReviewsViewModel
import io.navix.runtime.Navigator

fun NavGraphBuilder.productGraph(navigator: Navigator) {
    screen<Home> { _, _ ->
        val vm: HomeViewModel =
            viewModel {
                HomeViewModel(GetProductsUseCase(FakeProductRepository()))
            }
        HomeScreen(navigator = navigator, viewModel = vm)
    }

    screen<ProductDetail> { _, route ->
        // Keyed by productId so each distinct product gets its own ViewModel instance.
        val vm: ProductDetailViewModel =
            viewModel(key = route.productId) {
                ProductDetailViewModel(route.productId, GetProductByIdUseCase(FakeProductRepository()))
            }
        ProductDetailScreen(navigator = navigator, viewModel = vm)
    }

    screen<ProductReviews> { _, route ->
        val vm: ProductReviewsViewModel =
            viewModel(key = route.productId) {
                ProductReviewsViewModel(route.productId, GetProductReviewsUseCase(FakeProductRepository()))
            }
        ProductReviewsScreen(navigator = navigator, viewModel = vm)
    }
}
