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

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import io.navix.contracts.NavEvent
import io.navix.contracts.NavixTelemetry
import io.navix.demo.data.repository.DataStoreSettingsRepository
import io.navix.demo.data.repository.SettingsRepository
import io.navix.demo.data.telemetry.InMemoryEventExporter
import io.navix.telemetry.LogcatExporter
import io.navix.telemetry.NavixTelemetryPipeline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Singleton DataStore instance scoped to the application Context.
 * Must be declared as a top-level property so that only one instance
 * is ever created for the "navix_settings" preferences file.
 */
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "navix_settings"
)

class DemoApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /**
     * In-memory event exporter shared with [DemoNavHost] so the TelemetryViewer screen
     * can display events emitted before it opened (unlike the hot [Navigator.events] SharedFlow,
     * the [InMemoryEventExporter.events] StateFlow retains history).
     */
    val inMemoryExporter: InMemoryEventExporter by lazy {
        InMemoryEventExporter(maxEvents = 200)
    }

    /** Backing telemetry pipeline — always active regardless of analytics toggle. */
    val telemetryPipeline: NavixTelemetryPipeline by lazy {
        NavixTelemetryPipeline(
            exporters =
                listOf(
                    LogcatExporter(tag = "NavixDemo"),
                    inMemoryExporter,
                )
        )
    }

    /**
     * Stable [NavixTelemetry] reference passed to [rememberNavigator].
     * Delegates to the pipeline when analytics is enabled, or to [NavixTelemetry.NoOp]
     * when the user disables analytics in Settings — demonstrating the NoOp usage.
     */
    val telemetry: NavixTelemetry =
        object : NavixTelemetry {
            override fun onEvent(event: NavEvent) {
                if (analyticsEnabled.value) telemetryPipeline.onEvent(event)
            }
        }

    /** Source-of-truth for whether telemetry is currently active. */
    val analyticsEnabled = MutableStateFlow(false)

    /**
     * Persistent settings repository backed by Jetpack DataStore.
     * Exposed here so [DemoNavHost] can inject it into [SettingsViewModel].
     */
    val settingsRepository: SettingsRepository by lazy {
        DataStoreSettingsRepository(settingsDataStore)
    }

    override fun onCreate() {
        super.onCreate()
        // Keep analyticsEnabled in sync with persisted settings.
        appScope.launch {
            settingsRepository.settings.collect { data ->
                analyticsEnabled.value = data.analyticsEnabled
            }
        }
    }
}
