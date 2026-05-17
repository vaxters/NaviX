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
package io.navix.demo.data.repository

import io.navix.demo.data.model.SettingsData
import io.navix.demo.data.model.TransitionStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory [SettingsRepository] for unit tests and Compose Previews.
 */
class FakeSettingsRepository(
    initial: SettingsData = SettingsData()
) : SettingsRepository {
    private val _settings = MutableStateFlow(initial)
    override val settings: Flow<SettingsData> = _settings.asStateFlow()

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        _settings.update { it.copy(notificationsEnabled = enabled) }
    }

    override suspend fun setAnalyticsEnabled(enabled: Boolean) {
        _settings.update { it.copy(analyticsEnabled = enabled) }
    }

    override suspend fun setTransitionStyle(style: TransitionStyle) {
        _settings.update { it.copy(transitionStyle = style) }
    }
}
