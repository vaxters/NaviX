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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.navix.contracts.BackstackSnapshot
import io.navix.contracts.NavLifecycleState
import io.navix.contracts.NavTransitionKey
import io.navix.demo.data.model.Product
import io.navix.demo.routes.ProductDetail
import io.navix.demo.routes.Profile
import io.navix.demo.routes.TelemetryViewer
import io.navix.runtime.Navigator

@Composable
fun HomeScreen(
    navigator: Navigator,
    viewModel: HomeViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val backstack by navigator.backstack.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.navEffect.collect { effect ->
            when (effect) {
                is HomeNavEffect.OpenProductDetail ->
                    navigator.push(ProductDetail(effect.productId), NavTransitionKey.SlideLeft)

                HomeNavEffect.OpenProfile -> navigator.push(Profile)
                HomeNavEffect.OpenTelemetry -> navigator.push(TelemetryViewer)
                is HomeNavEffect.HandleDeepLink -> navigator.handleDeepLink(effect.uri)
            }
        }
    }

    HomeContent(
        state = state,
        backstack = backstack,
        onProductClicked = viewModel::onProductClicked,
        onOpenProfile = viewModel::onOpenProfile,
        onOpenTelemetry = viewModel::onOpenTelemetry,
        onDeepLink = viewModel::onDeepLink
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    state: HomeUiState,
    backstack: BackstackSnapshot,
    onProductClicked: (String) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenTelemetry: () -> Unit,
    onDeepLink: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Home") },
                actions = {
                    IconButton(onClick = onOpenTelemetry) {
                        Icon(Icons.Default.Timeline, contentDescription = "Telemetry")
                    }
                    IconButton(onClick = onOpenProfile) {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
        ) {
            BackstackInfoBar(backstack = backstack)

            Box(modifier = Modifier.weight(1f)) {
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

                    else -> {
                        ProductList(
                            products = state.products,
                            onProductClicked = onProductClicked,
                            onDeepLink = onDeepLink,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BackstackInfoBar(backstack: BackstackSnapshot) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val activeRoute = backstack.active?.route?.let { it::class.simpleName } ?: "—"
            Text(
                text = "Depth: ${backstack.depth}  |  canPop: ${backstack.canPop}  |  Active: $activeRoute",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                backstack.entries.forEach { entry ->
                    val isResumed = entry.lifecycleState == NavLifecycleState.RESUMED
                    FilterChip(
                        selected = isResumed,
                        onClick = {},
                        label = {
                            Text(
                                text = "${entry.route::class.simpleName} • ${entry.lifecycleState}",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                        colors =
                            FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductList(
    products: List<Product>,
    onProductClicked: (String) -> Unit,
    onDeepLink: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Featured Products",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        items(products, key = { it.id }) { product ->
            ProductCard(product = product, onClick = { onProductClicked(product.id) })
        }
        item {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Deep Links",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                OutlinedButton(
                    onClick = { onDeepLink("navix://product/1") },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Deep Link → Product 1")
                }
                OutlinedButton(
                    onClick = { onDeepLink("navix://profile") },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Deep Link → Profile")
                }
            }
        }
    }
}

@Composable
private fun ProductCard(
    product: Product,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = product.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
