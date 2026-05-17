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
import io.navix.compose.NavTransitionSpec
import io.navix.compose.NavixHost
import io.navix.compose.rememberNavigator
import io.navix.contracts.NavTransitionKey
import io.navix.contracts.RouteEntry
import io.navix.demo.data.deeplink.ProductDeepLinkHandler
import io.navix.demo.data.deeplink.ProfileDeepLinkHandler
import io.navix.demo.data.model.SettingsData
import io.navix.demo.data.model.TransitionStyle
import io.navix.demo.navigation.SingleTopReducer
import io.navix.demo.navigation.authGraph
import io.navix.demo.navigation.demoExtrasGraph
import io.navix.demo.navigation.productGraph
import io.navix.demo.navigation.profileGraph
import io.navix.demo.routes.Login

private fun TransitionStyle.toNavTransitionSpec(): NavTransitionSpec {
    val durationMs = 300
    return object : NavTransitionSpec {
        override fun enterTransition(
            from: RouteEntry?,
            to: RouteEntry,
            key: NavTransitionKey
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
            key: NavTransitionKey
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
            reducer = SingleTopReducer()
        )

    LaunchedEffect(deepLinkUri) {
        if (deepLinkUri != null) {
            navigator.handleDeepLink(deepLinkUri)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavixHost(navigator = navigator, transitionSpec = transitionSpec) {
            authGraph(navigator)
            productGraph(navigator)
            profileGraph(navigator, app)
            demoExtrasGraph(navigator, app)
        }

        // Debug build: real DevTools overlay. Release build: no-op (src/release source set).
        NavixDebugOverlay(
            navigator = navigator,
            eventHistory = app.telemetryPipeline.replayBuffer
        )
    }
}
