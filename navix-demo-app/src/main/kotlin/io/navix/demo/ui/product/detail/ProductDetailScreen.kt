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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.navix.contracts.NavTransitionKey
import io.navix.demo.data.model.Product
import io.navix.demo.routes.Home
import io.navix.demo.routes.ProductReviews
import io.navix.runtime.Navigator
import io.navix.runtime.popTo

@Composable
fun ProductDetailScreen(
    navigator: Navigator,
    viewModel: ProductDetailViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.navEffect.collect { effect ->
            when (effect) {
                is ProductDetailNavEffect.OpenReviews ->
                    navigator.push(ProductReviews(effect.productId), NavTransitionKey.SlideLeft)

                ProductDetailNavEffect.NavigateBack -> navigator.pop()
                ProductDetailNavEffect.NavigateBackToHome -> navigator.popTo<Home>()
            }
        }
    }

    ProductDetailContent(
        state = state,
        onReadReviews = viewModel::onReadReviews,
        onBack = viewModel::onBack,
        onBackToHome = viewModel::onBackToHome
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductDetailContent(
    state: ProductDetailUiState,
    onReadReviews: () -> Unit,
    onBack: () -> Unit,
    onBackToHome: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.product?.name ?: "Product Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        }
    ) { padding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                state.error != null -> {
                    Text(
                        text = "Error: ${state.error}",
                        color = MaterialTheme.colorScheme.error,
                        modifier =
                            Modifier
                                .align(Alignment.Center)
                                .padding(24.dp),
                    )
                }

                state.product != null -> {
                    ProductDetailBody(
                        product = state.product,
                        onReadReviews = onReadReviews,
                        onBackToHome = onBackToHome,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductDetailBody(
    product: Product,
    onReadReviews: () -> Unit,
    onBackToHome: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = product.name,
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Deep link: navix://product/${product.id}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            repeat(product.rating.toInt()) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                )
            }
            Text(
                text = "  ${product.rating} (${product.reviewCount} reviews)",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onReadReviews,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Read Reviews")
        }

        OutlinedButton(
            onClick = onBackToHome,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Home (popTo)")
        }
    }
}
