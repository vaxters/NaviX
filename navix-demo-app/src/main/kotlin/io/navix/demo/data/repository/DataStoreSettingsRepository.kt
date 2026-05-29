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

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import io.navix.demo.data.model.SettingsData
import io.navix.demo.data.model.TransitionStyle
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * Production [SettingsRepository] backed by Jetpack DataStore (Preferences).
 *
 * Settings survive process death and are written asynchronously off the main thread.
 * IOException from the underlying store is caught and replaced with default values so
 * the UI never hangs on a corrupted preferences file.
 */
class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {
    private object Keys {
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        val ANALYTICS = booleanPreferencesKey("analytics_enabled")
        val TRANSITION_STYLE = stringPreferencesKey("transition_style")
    }

    override val settings: Flow<SettingsData> =
        dataStore.data
            .catch { cause ->
                // Emit defaults rather than crashing if the file is unreadable.
                if (cause is IOException) emit(emptyPreferences()) else throw cause
            }.map { prefs ->
                SettingsData(
                    notificationsEnabled = prefs[Keys.NOTIFICATIONS] ?: true,
                    analyticsEnabled = prefs[Keys.ANALYTICS] ?: false,
                    transitionStyle =
                        prefs[Keys.TRANSITION_STYLE]
                            ?.let { value ->
                                runCatching { TransitionStyle.valueOf(value) }
                                    .onFailure { Log.w("NaviX", "TransitionStyle '$value' not recognized, using default") }
                                    .getOrNull()
                            }
                            ?: TransitionStyle.Default
                )
            }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.NOTIFICATIONS] = enabled }
    }

    override suspend fun setAnalyticsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.ANALYTICS] = enabled }
    }

    override suspend fun setTransitionStyle(style: TransitionStyle) {
        dataStore.edit { it[Keys.TRANSITION_STYLE] = style.name }
    }
}
