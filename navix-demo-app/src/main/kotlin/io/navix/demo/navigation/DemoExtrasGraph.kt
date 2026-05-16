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
import io.navix.compose.dialog
import io.navix.compose.screen
import io.navix.demo.BuildConfig
import io.navix.demo.DemoApp
import io.navix.demo.routes.ColorPickerRoute
import io.navix.demo.routes.ConfirmActionDialog
import io.navix.demo.routes.NavPlayground
import io.navix.demo.routes.NavPlaygroundStep
import io.navix.demo.routes.OverlayDemo
import io.navix.demo.routes.ResultPassingDemo
import io.navix.demo.routes.TabsDemo
import io.navix.demo.routes.TelemetryViewer
import io.navix.demo.ui.overlay.ConfirmDialogContent
import io.navix.demo.ui.overlay.OverlayDemoScreen
import io.navix.demo.ui.playground.NavPlaygroundScreen
import io.navix.demo.ui.playground.NavPlaygroundStepScreen
import io.navix.demo.ui.result.ColorPickerScreen
import io.navix.demo.ui.result.ResultPassingDemoScreen
import io.navix.demo.ui.tabs.TabsDemoScreen
import io.navix.demo.ui.telemetry.TelemetryViewModel
import io.navix.demo.ui.telemetry.TelemetryViewerScreen
import io.navix.runtime.Navigator

fun NavGraphBuilder.demoExtrasGraph(
    navigator: Navigator,
    app: DemoApp,
) {
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
