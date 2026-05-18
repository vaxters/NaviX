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
package io.navix.demo.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Loads a one-shot [Result] into a hot [StateFlow] of UI state.
 *
 * Every data-backed screen ViewModel in this demo repeated the same
 * `flow { emit(...) }.map { fold }.stateIn(...)` plumbing — incidental ceremony that
 * obscured what each screen actually expresses: which use case it calls and how success
 * and failure map to UI state. Those decisions stay at each call site; only the generic
 * loading boilerplate lives here. The navigation patterns this demo exists to teach are
 * deliberately left inline in the ViewModels.
 */
fun <T, R> CoroutineScope.loadUiState(
    initial: R,
    load: suspend () -> Result<T>,
    onSuccess: (T) -> R,
    onFailure: (Throwable) -> R,
): StateFlow<R> =
    flow { emit(load()) }
        .map { it.fold(onSuccess, onFailure) }
        .stateIn(this, SharingStarted.WhileSubscribed(5_000), initial)
