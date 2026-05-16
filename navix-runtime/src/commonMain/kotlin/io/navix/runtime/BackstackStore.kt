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
package io.navix.runtime

import io.navix.contracts.BackstackSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Owns the authoritative [BackstackSnapshot] state and applies [BackstackAction]s via
 * a [Reducer].
 *
 * This interface is internal to the navix-runtime module. External code interacts with
 * the backstack exclusively through [Navigator] — dispatching actions directly would
 * bypass telemetry and event emission.
 *
 * Thread safety: [MutableStateFlow.update] is atomic. Callers from any thread are safe.
 */
internal interface BackstackStore {
    val state: StateFlow<BackstackSnapshot>

    fun dispatch(action: BackstackAction)
}

internal class BackstackStoreImpl(
    initial: BackstackSnapshot,
    private val reducer: Reducer,
) : BackstackStore {
    private val _state = MutableStateFlow(initial)
    override val state: StateFlow<BackstackSnapshot> = _state.asStateFlow()

    override fun dispatch(action: BackstackAction) {
        _state.update { current -> reducer.reduce(current, action) }
    }
}
