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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import io.navix.demo.data.model.Review
import io.navix.demo.routes.Home
import io.navix.runtime.Navigator
import io.navix.runtime.popTo

@Composable
fun ProductReviewsScreen(
    navigator: Navigator,
    viewModel: ProductReviewsViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.navEffect.collect { effect ->
            when (effect) {
                ProductReviewsNavEffect.NavigateBack -> navigator.pop()
                ProductReviewsNavEffect.JumpToHome -> navigator.popTo<Home>()
            }
        }
    }

    ProductReviewsContent(
        state = state,
        onBack = viewModel::onBack,
        onJumpToHome = viewModel::onJumpToHome
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductReviewsContent(
    state: ProductReviewsUiState,
    onBack: () -> Unit,
    onJumpToHome: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reviews — ${state.productId}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
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
                                .padding(24.dp)
                    )
                }

                else -> {
                    ReviewList(
                        reviews = state.reviews,
                        onJumpToHome = onJumpToHome
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewList(
    reviews: List<Review>,
    onJumpToHome: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            OutlinedButton(
                onClick = onJumpToHome,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Jump to Home (popTo)")
            }
        }
        items(reviews, key = { it.id }) { review ->
            ReviewItem(review = review)
        }
    }
}

@Composable
private fun ReviewItem(review: Review) {
    Card {
        ListItem(
            headlineContent = { Text(review.author) },
            supportingContent = { Text(review.body) }
        )
    }
}
