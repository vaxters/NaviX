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
package io.navix.devtools

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.navix.contracts.NavEvent
import io.navix.runtime.Navigator
import kotlinx.coroutines.flow.StateFlow

/**
 * An in-process debug overlay that renders over app content when [enabled] is true.
 *
 * Provides two inspection panels:
 * - **Stack**: Live backstack view from [Navigator.backstack]
 * - **Events**: Navigation event timeline
 *
 * A floating action button toggles visibility. The overlay is transparent to touch
 * events when collapsed, and only captures touches on its own surface when expanded.
 *
 * ### Event history
 * By default the Events tab subscribes to [Navigator.events] (a hot SharedFlow) and
 * only shows events emitted after the overlay panel is first opened. Pass [eventHistory]
 * to supply a full event history observable that retains events from before the panel
 * was opened — the recommended source is `NavixTelemetryPipeline.replayBuffer`:
 *
 * ```kotlin
 * NavixDevToolsOverlay(
 *     navigator = navigator,
 *     enabled = BuildConfig.DEBUG,
 *     eventHistory = telemetryPipeline.replayBuffer,  // StateFlow<List<NavEvent>>
 * )
 * ```
 *
 * **IMPORTANT:** [enabled] defaults to `false`. Pass `enabled = BuildConfig.DEBUG`
 * to activate in debug builds only and prevent accidental shipping to production.
 *
 * Usage:
 * ```kotlin
 * Box(Modifier.fillMaxSize()) {
 *     NavixHost(navigator = navigator) { /* screens */ }
 *     NavixDevToolsOverlay(
 *         navigator = navigator,
 *         enabled = BuildConfig.DEBUG,
 *         eventHistory = telemetry.replayBuffer,
 *     )
 * }
 * ```
 */
@Composable
fun NavixDevToolsOverlay(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    enabled: Boolean = false,
    eventHistory: StateFlow<List<NavEvent>>? = null,
) {
    if (!enabled) return

    var panelVisible by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    Box(modifier = modifier.fillMaxSize()) {
        FloatingActionButton(
            onClick = { panelVisible = !panelVisible },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(40.dp)
                .zIndex(10f),
            containerColor = DevToolsColors.header,
            contentColor = DevToolsColors.background,
        ) {
            Icon(
                imageVector = if (panelVisible) Icons.Default.Close else Icons.Default.BugReport,
                contentDescription = if (panelVisible) "Close DevTools" else "Open DevTools",
                modifier = Modifier.size(20.dp),
            )
        }

        // Sliding panel from bottom
        AnimatedVisibility(
            visible = panelVisible,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .zIndex(9f),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(DevToolsColors.background)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent(pass = PointerEventPass.Final).changes.forEach { it.consume() }
                            }
                        }
                    }
                    .padding(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Navix DevTools",
                        fontSize = 12.sp,
                        color = DevToolsColors.header,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = { panelVisible = false },
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = DevToolsColors.dimmed,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = DevToolsColors.background,
                    contentColor = DevToolsColors.header,
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Stack", fontSize = 11.sp) },
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = {
                            Icon(
                                Icons.Default.Timeline,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        text = { Text("Events", fontSize = 11.sp) },
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                when (selectedTab) {
                    0 -> BackstackInspectorPanel(navigator = navigator)
                    1 -> EventTimelinePanel(navigator = navigator, eventHistory = eventHistory)
                }
            }
        }
    }
}
