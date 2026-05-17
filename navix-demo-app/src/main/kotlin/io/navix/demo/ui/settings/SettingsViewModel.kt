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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.navix.demo.data.model.TransitionStyle
import io.navix.demo.data.repository.SettingsRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _navEffect = Channel<SettingsNavEffect>(Channel.BUFFERED)
    val navEffect = _navEffect.receiveAsFlow()

    init {
        // Mirror persisted settings into UI state so toggles survive process death.
        viewModelScope.launch {
            settingsRepository.settings.collect { data ->
                _uiState.update {
                    it.copy(
                        notificationsEnabled = data.notificationsEnabled,
                        analyticsEnabled = data.analyticsEnabled,
                        transitionStyle = data.transitionStyle,
                    )
                }
            }
        }
    }

    fun onToggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationsEnabled(enabled)
        }
    }

    fun onToggleAnalytics(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAnalyticsEnabled(enabled)
        }
    }

    fun onTransitionStyleChange(style: TransitionStyle) {
        viewModelScope.launch {
            settingsRepository.setTransitionStyle(style)
        }
    }

    fun onBack() {
        viewModelScope.launch {
            _navEffect.send(SettingsNavEffect.NavigateBack)
        }
    }
}
