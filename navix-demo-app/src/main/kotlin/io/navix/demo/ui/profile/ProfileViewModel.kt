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
package io.navix.demo.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.navix.demo.domain.usecase.GetCurrentUserUseCase
import io.navix.demo.ui.loadUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    getCurrentUser: GetCurrentUserUseCase
) : ViewModel() {
    val uiState: StateFlow<ProfileUiState> = viewModelScope.loadUiState(
        initial = ProfileUiState.Loading,
        load = { getCurrentUser() },
        onSuccess = { ProfileUiState.Success(it) },
        onFailure = { ProfileUiState.Error(it.message ?: "Failed to load profile") },
    )

    private val _navEffect = Channel<ProfileNavEffect>(Channel.BUFFERED)
    val navEffect = _navEffect.receiveAsFlow()

    fun onOpenSettings() {
        viewModelScope.launch { _navEffect.send(ProfileNavEffect.OpenSettings) }
    }

    fun onSignOut() {
        viewModelScope.launch { _navEffect.send(ProfileNavEffect.SignOut) }
    }

    fun onResetToHome() {
        viewModelScope.launch { _navEffect.send(ProfileNavEffect.ResetToHome) }
    }

    fun onBack() {
        viewModelScope.launch { _navEffect.send(ProfileNavEffect.NavigateBack) }
    }
}
