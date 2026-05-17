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
package io.navix.demo.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.navix.demo.data.model.TransitionStyle
import io.navix.runtime.Navigator

@Composable
fun SettingsScreen(
    navigator: Navigator,
    viewModel: SettingsViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.navEffect.collect { effect ->
            when (effect) {
                SettingsNavEffect.NavigateBack -> navigator.pop()
            }
        }
    }

    SettingsContent(
        state = state,
        onToggleNotifications = viewModel::onToggleNotifications,
        onToggleAnalytics = viewModel::onToggleAnalytics,
        onTransitionStyleChange = viewModel::onTransitionStyleChange,
        onBack = viewModel::onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(
    state: SettingsUiState,
    onToggleNotifications: (Boolean) -> Unit,
    onToggleAnalytics: (Boolean) -> Unit,
    onTransitionStyleChange: (TransitionStyle) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
        ) {
            item {
                SettingsToggleItem(
                    title = "Notifications",
                    subtitle = "Enable push notifications",
                    checked = state.notificationsEnabled,
                    onCheckedChange = onToggleNotifications
                )
                HorizontalDivider()
            }
            item {
                SettingsToggleItem(
                    title = "Analytics",
                    subtitle = "Off = NavixTelemetry.NoOp — no events emitted",
                    checked = state.analyticsEnabled,
                    onCheckedChange = onToggleAnalytics
                )
                HorizontalDivider()
            }
            item {
                TransitionStyleItem(
                    selected = state.transitionStyle,
                    onSelect = onTransitionStyleChange,
                )
                HorizontalDivider()
            }
            item {
                ListItem(
                    headlineContent = { Text("Version") },
                    trailingContent = { Text(state.appVersion) },
                )
            }
        }
    }
}

@Composable
private fun SettingsToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    )
}

@Composable
private fun TransitionStyleItem(
    selected: TransitionStyle,
    onSelect: (TransitionStyle) -> Unit
) {
    ListItem(
        headlineContent = { Text("Transition Style") },
        supportingContent = {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TransitionStyle.entries.forEach { style ->
                    FilterChip(
                        selected = style == selected,
                        onClick = { onSelect(style) },
                        label = {
                            Text(
                                text = style.name,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                }
            }
        }
    )
}
