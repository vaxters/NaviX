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
package io.navix.demo.ui.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.navix.compose.NavStackSpec
import io.navix.compose.NavixMultiStackHost
import io.navix.compose.rememberNavixMultiStack
import io.navix.compose.screen
import io.navix.demo.routes.CounterTab
import io.navix.demo.routes.InfoTab
import io.navix.demo.routes.ListTab
import io.navix.demo.routes.ListTabDetail
import io.navix.runtime.Navigator

private val TABS =
    listOf(
        Triple("Counter", CounterTab, 0),
        Triple("List", ListTab, 1),
        Triple("Info", InfoTab, 2),
    )

private val SAMPLE_ITEMS = listOf("Alpha", "Beta", "Gamma", "Delta", "Epsilon")

/**
 * Demonstrates [NavixMultiStack] with three independent navigation stacks.
 *
 * Each tab maintains its own backstack — navigating to a detail screen in one tab and
 * switching to another tab preserves the first tab's navigation state. Switching back
 * restores the exact backstack, including depth and active route.
 *
 * The outer [Navigator] (the one that pushed [TabsDemo]) is used only for the top-level
 * back press (popping out of this screen entirely). Each tab's sub-navigation is handled
 * by the per-tab navigators inside [NavixMultiStack].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabsDemoScreen(outerNavigator: Navigator) {
    val multiStack =
        rememberNavixMultiStack(
            specs =
                listOf(
                    NavStackSpec(root = CounterTab, key = "counter"),
                    NavStackSpec(root = ListTab, key = "list"),
                    NavStackSpec(root = InfoTab, key = "info"),
                ),
        )

    val activeTabIndex by multiStack.activeTabIndex.collectAsState()

    // No explicit BackHandler needed here. The inner NavixHost installs its own
    // BackHandler that pops the active tab's navigator while the tab has entries.
    // When the active tab is at its root (canPop = false), that inner BackHandler is
    // disabled and back propagates naturally to the outer NavixHost, which pops the
    // TabsDemo screen from the outer navigator.

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Multi-Stack Demo") },
                navigationIcon = {
                    OutlinedButton(
                        onClick = { outerNavigator.pop() },
                        modifier = Modifier.padding(start = 8.dp),
                    ) { Text("← Back") }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                TABS.forEach { (label, _, index) ->
                    NavigationBarItem(
                        selected = activeTabIndex == index,
                        onClick = { multiStack.selectTab(index) },
                        icon = {
                            Text(
                                text = label.first().toString(),
                                fontWeight = FontWeight.Bold,
                            )
                        },
                        label = { Text(label) },
                    )
                }
            }
        },
    ) { padding ->
        NavixMultiStackHost(
            multiStack = multiStack,
            modifier = Modifier.padding(padding),
        ) {
            screen<CounterTab> { _, _ ->
                CounterTabScreen(navigator = multiStack.navigators[0])
            }
            screen<ListTab> { _, _ ->
                ListTabScreen(navigator = multiStack.navigators[1])
            }
            screen<ListTabDetail> { _, route ->
                ListTabDetailScreen(navigator = multiStack.navigators[1], item = route.item)
            }
            screen<InfoTab> { _, _ ->
                InfoTabScreen()
            }
        }
    }
}

// ── Tab screen implementations ────────────────────────────────────────────────

@Composable
private fun CounterTabScreen(navigator: Navigator) {
    var count by remember { mutableIntStateOf(0) }
    val snapshot by navigator.backstack.collectAsState()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Counter Tab",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "$count",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = { count++ }, modifier = Modifier.fillMaxWidth()) {
            Text("Increment")
        }
        OutlinedButton(
            onClick = { count = 0 },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Reset") }
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Stack depth: ${snapshot.depth}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text =
                "Note: this counter resets when you switch tabs and return.\n" +
                    "Composable-local state (remember { }) is not preserved.\n" +
                    "See the List tab to observe navigation state preservation.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ListTabScreen(navigator: Navigator) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "List Tab",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        Text(
            text = "Tap an item — each tab has its own independent backstack.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(SAMPLE_ITEMS) { item ->
                Card(
                    onClick = { navigator.push(ListTabDetail(item)) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = item,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun ListTabDetailScreen(
    navigator: Navigator,
    item: String,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Detail: $item",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Switch to another tab and come back — this detail screen is still here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = { navigator.pop() }) {
            Text("← Back to List")
        }
    }
}

@Composable
private fun InfoTabScreen() {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Info Tab",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text =
                    "NavixMultiStack\n\n" +
                        "• Each tab has its own Navigator\n" +
                        "• Backtracks are independent\n" +
                        "• Switching tabs preserves nav state\n" +
                        "• Back pops the active tab's stack\n" +
                        "• When the active tab is at root,\n" +
                        "  back propagates to the outer stack",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
