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
package io.navix.demo

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.navix.compose.NavTransitionSpec
import io.navix.compose.NavixHost
import io.navix.compose.dialog
import io.navix.compose.rememberNavigator
import io.navix.compose.screen
import io.navix.contracts.NavTransitionKey
import io.navix.contracts.RouteEntry
import io.navix.demo.data.deeplink.ProductDeepLinkHandler
import io.navix.demo.data.deeplink.ProfileDeepLinkHandler
import io.navix.demo.data.model.SettingsData
import io.navix.demo.data.model.TransitionStyle
import io.navix.demo.data.repository.FakeProductRepository
import io.navix.demo.data.repository.FakeUserRepository
import io.navix.demo.domain.usecase.GetCurrentUserUseCase
import io.navix.demo.domain.usecase.GetProductByIdUseCase
import io.navix.demo.domain.usecase.GetProductReviewsUseCase
import io.navix.demo.domain.usecase.GetProductsUseCase
import io.navix.demo.navigation.SingleTopReducer
import io.navix.demo.routes.ColorPickerRoute
import io.navix.demo.routes.ConfirmActionDialog
import io.navix.demo.routes.Home
import io.navix.demo.routes.Login
import io.navix.demo.routes.NavPlayground
import io.navix.demo.routes.NavPlaygroundStep
import io.navix.demo.routes.OverlayDemo
import io.navix.demo.routes.ProductDetail
import io.navix.demo.routes.ProductReviews
import io.navix.demo.routes.Profile
import io.navix.demo.routes.Register
import io.navix.demo.routes.ResultPassingDemo
import io.navix.demo.routes.Settings
import io.navix.demo.routes.TabsDemo
import io.navix.demo.routes.TelemetryViewer
import io.navix.demo.ui.auth.AuthScreen
import io.navix.demo.ui.auth.AuthViewModel
import io.navix.demo.ui.home.HomeScreen
import io.navix.demo.ui.home.HomeViewModel
import io.navix.demo.ui.overlay.ConfirmDialogContent
import io.navix.demo.ui.overlay.OverlayDemoScreen
import io.navix.demo.ui.playground.NavPlaygroundScreen
import io.navix.demo.ui.playground.NavPlaygroundStepScreen
import io.navix.demo.ui.product.detail.ProductDetailScreen
import io.navix.demo.ui.product.detail.ProductDetailViewModel
import io.navix.demo.ui.product.reviews.ProductReviewsScreen
import io.navix.demo.ui.product.reviews.ProductReviewsViewModel
import io.navix.demo.ui.profile.ProfileScreen
import io.navix.demo.ui.profile.ProfileViewModel
import io.navix.demo.ui.register.RegisterScreen
import io.navix.demo.ui.register.RegisterViewModel
import io.navix.demo.ui.result.ColorPickerScreen
import io.navix.demo.ui.result.ResultPassingDemoScreen
import io.navix.demo.ui.settings.SettingsScreen
import io.navix.demo.ui.settings.SettingsViewModel
import io.navix.demo.ui.tabs.TabsDemoScreen
import io.navix.demo.ui.telemetry.TelemetryViewModel
import io.navix.demo.ui.telemetry.TelemetryViewerScreen

private fun TransitionStyle.toNavTransitionSpec(): NavTransitionSpec {
    val durationMs = 300
    return object : NavTransitionSpec {
        override fun enterTransition(
            from: RouteEntry?,
            to: RouteEntry,
            key: NavTransitionKey,
        ): EnterTransition =
            when (if (key == NavTransitionKey.Default) styleKey else key) {
                NavTransitionKey.SlideLeft -> slideInHorizontally(tween(durationMs)) { it }
                NavTransitionKey.SlideRight -> slideInHorizontally(tween(durationMs)) { -it }
                NavTransitionKey.Scale -> scaleIn(tween(durationMs), initialScale = 0.9f) + fadeIn(tween(durationMs))
                NavTransitionKey.None -> EnterTransition.None
                else -> fadeIn(tween(durationMs))
            }

        override fun exitTransition(
            from: RouteEntry,
            to: RouteEntry?,
            key: NavTransitionKey,
        ): ExitTransition =
            when (if (key == NavTransitionKey.Default) styleKey else key) {
                NavTransitionKey.SlideLeft -> slideOutHorizontally(tween(durationMs)) { -it }
                NavTransitionKey.SlideRight -> slideOutHorizontally(tween(durationMs)) { it }
                NavTransitionKey.Scale -> scaleOut(tween(durationMs), targetScale = 1.1f) + fadeOut(tween(durationMs))
                NavTransitionKey.None -> ExitTransition.None
                else -> fadeOut(tween(durationMs))
            }

        private val styleKey =
            when (this@toNavTransitionSpec) {
                TransitionStyle.Default -> NavTransitionKey.Default
                TransitionStyle.Slide -> NavTransitionKey.SlideLeft
                TransitionStyle.Scale -> NavTransitionKey.Scale
                TransitionStyle.Fade -> NavTransitionKey.Fade
            }
    }
}

@Composable
fun DemoNavHost(deepLinkUri: String? = null) {
    val context = LocalContext.current
    val app = context.applicationContext as DemoApp

    // Multiple handlers evaluated in order — first match wins.
    val deepLinkHandlers = remember { listOf(ProductDeepLinkHandler(), ProfileDeepLinkHandler()) }

    val settings by app.settingsRepository.settings.collectAsStateWithLifecycle(SettingsData())

    // Custom NavTransitionSpec — remaps the Default key to the user-chosen animation style.
    val transitionSpec =
        remember(settings.transitionStyle) {
            settings.transitionStyle.toNavTransitionSpec()
        }

    // Custom reducer: single-top push behavior — navigating to an already-stacked route
    // pops back to it instead of creating a duplicate.
    val navigator =
        rememberNavigator(
            root = Login,
            deepLinkHandlers = deepLinkHandlers,
            telemetry = app.telemetry,
            reducer = SingleTopReducer(),
        )

    LaunchedEffect(deepLinkUri) {
        if (deepLinkUri != null) {
            navigator.handleDeepLink(deepLinkUri)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavixHost(navigator = navigator, transitionSpec = transitionSpec) {
            screen<Login> { _, _ ->
                val vm: AuthViewModel = viewModel()
                AuthScreen(navigator = navigator, viewModel = vm)
            }

            screen<Register> { _, _ ->
                val vm: RegisterViewModel = viewModel()
                RegisterScreen(navigator = navigator, viewModel = vm)
            }

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

            screen<Profile> { _, _ ->
                val vm: ProfileViewModel =
                    viewModel {
                        ProfileViewModel(GetCurrentUserUseCase(FakeUserRepository()))
                    }
                ProfileScreen(navigator = navigator, viewModel = vm)
            }

            screen<Settings> { _, _ ->
                val vm: SettingsViewModel =
                    viewModel {
                        SettingsViewModel(app.settingsRepository)
                    }
                SettingsScreen(navigator = navigator, viewModel = vm)
            }

            screen<NavPlayground> { _, _ ->
                NavPlaygroundScreen(navigator = navigator)
            }

            screen<NavPlaygroundStep> { _, route ->
                NavPlaygroundStepScreen(navigator = navigator, stepNumber = route.stepNumber)
            }

            screen<TabsDemo> { _, _ ->
                TabsDemoScreen(outerNavigator = navigator)
            }

            screen<ResultPassingDemo> { _, _ ->
                ResultPassingDemoScreen(navigator = navigator)
            }

            screen<ColorPickerRoute> { _, _ ->
                ColorPickerScreen(navigator = navigator)
            }

            screen<OverlayDemo> { _, _ ->
                OverlayDemoScreen(navigator = navigator)
            }

            // ConfirmActionDialog is a first-class dialog destination — registered with
            // dialog<> so NavixHost renders it in a Dialog composable above OverlayDemoScreen.
            // Back press / onDismissRequest automatically calls navigator.pop().
            dialog<ConfirmActionDialog> { _, _ ->
                ConfirmDialogContent(
                    onConfirm = { navigator.pop() },
                    onCancel = { navigator.pop() },
                )
            }

            if (BuildConfig.DEBUG) {
                screen<TelemetryViewer> { _, _ ->
                    // Pass the InMemoryEventExporter's StateFlow — preserves events emitted
                    // before this screen opens, unlike the hot SharedFlow on navigator.events.
                    val vm: TelemetryViewModel =
                        viewModel {
                            TelemetryViewModel(app.inMemoryExporter.events)
                        }
                    TelemetryViewerScreen(navigator = navigator, viewModel = vm)
                }
            }
        }

        // Debug build: real DevTools overlay. Release build: no-op (src/release source set).
        NavixDebugOverlay(
            navigator = navigator,
            eventHistory = app.telemetryPipeline.replayBuffer,
        )
    }
}
