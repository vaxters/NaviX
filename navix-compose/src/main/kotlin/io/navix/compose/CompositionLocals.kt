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

import androidx.compose.runtime.staticCompositionLocalOf
import io.navix.contracts.RouteEntry
import io.navix.runtime.Navigator

/**
 * Provides the [Navigator] to any composable inside a [NavixHost] or [NavixMultiStackHost].
 *
 * Throws by default — using this local outside a Navix host is a programming error caught
 * at composition time rather than silently producing a no-op.
 *
 * ### Usage
 * ```kotlin
 * @Composable
 * fun HomeScreen() {
 *     val navigator = LocalNavigator.current
 *     Button(onClick = { navigator.push(Detail(id = "1")) }) { Text("Open") }
 * }
 * ```
 */
val LocalNavigator =
    staticCompositionLocalOf<Navigator> {
        error(
            "No Navigator found in the composition. " +
                "Ensure this composable is called inside NavixHost { }.",
        )
    }

/**
 * Provides the [RouteEntry] for the currently rendered screen to any composable inside
 * a [NavixHost] content block.
 *
 * Throws by default — using this local outside a Navix host is a programming error caught
 * at composition time rather than silently producing a no-op.
 *
 * ### Usage
 * ```kotlin
 * @Composable
 * fun ProductDetailScreen() {
 *     val entry = LocalNavEntry.current
 *     Text("Entry id: ${entry.id}")
 * }
 * ```
 */
val LocalNavEntry =
    staticCompositionLocalOf<RouteEntry> {
        error(
            "No NavEntry found in the composition. " +
                "Ensure this composable is called inside NavixHost { }.",
        )
    }
