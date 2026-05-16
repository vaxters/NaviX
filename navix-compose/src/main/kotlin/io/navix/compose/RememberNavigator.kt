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
package io.navix.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import io.navix.contracts.NavixTelemetry
import io.navix.contracts.Route
import io.navix.runtime.DeepLinkHandler
import io.navix.runtime.DefaultReducer
import io.navix.runtime.Navigator
import io.navix.runtime.Reducer
import io.navix.runtime.createNavigator

/**
 * Creates and remembers a [Navigator] for the composition lifetime.
 *
 * The navigator is created **once** per composition site and survives recompositions.
 * [root] is the initial destination placed at the bottom of the backstack — it is captured
 * at creation time and cannot be changed afterwards. If you need to navigate to a new root
 * after creation, call [Navigator.reset] instead.
 *
 * ### Important: root changes are ignored
 * This function uses `remember` without a key. If [root] changes identity after the first
 * composition (e.g., it is a non-stable computed value), the navigator is **not** recreated
 * and the change is silently ignored. This is intentional — recreating the navigator would
 * destroy the entire backstack. Use [Navigator.reset] to programmatically swap the root.
 *
 * To integrate telemetry, pass a [NavixTelemetry] implementation — e.g. a
 * [io.navix.telemetry.NavixTelemetryPipeline] with your chosen exporters.
 *
 * @param root Initial destination placed at the bottom of the backstack. Captured once at
 *   creation; later changes to this parameter have no effect.
 * @param deepLinkHandlers Ordered list of handlers for deep link URI resolution.
 * @param telemetry Telemetry sink that receives every [io.navix.contracts.NavEvent].
 * @param reducer Custom backstack reducer. Defaults to [DefaultReducer].
 */
@Composable
fun rememberNavigator(
    root: Route,
    deepLinkHandlers: List<DeepLinkHandler> = emptyList(),
    telemetry: NavixTelemetry = NavixTelemetry.NoOp,
    reducer: Reducer = DefaultReducer(),
): Navigator {
    val scope = rememberCoroutineScope()
    // No key — root changes after first composition are ignored intentionally.
    // Use Navigator.reset() to swap the root programmatically.
    return remember {
        createNavigator(
            root = root,
            scope = scope,
            reducer = reducer,
            telemetry = telemetry,
            deepLinkHandlers = deepLinkHandlers,
        )
    }
}
