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
package io.navix.demo.ui.telemetry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.navix.contracts.NavEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Exposes navigation event history from [InMemoryEventExporter] to the TelemetryViewer.
 *
 * Using [StateFlow] instead of the raw [Navigator.events] SharedFlow means events emitted
 * before the screen opens are preserved — the UI sees full history, not just live events.
 */
class TelemetryViewModel(
    val navEvents: StateFlow<List<NavEvent>>
) : ViewModel() {
    val uiState: StateFlow<List<NavEvent>> = navEvents

    private val _navEffect = Channel<TelemetryNavEffect>(Channel.BUFFERED)
    val navEffect = _navEffect.receiveAsFlow()

    fun onBack() {
        viewModelScope.launch {
            _navEffect.send(TelemetryNavEffect.NavigateBack)
        }
    }
}
