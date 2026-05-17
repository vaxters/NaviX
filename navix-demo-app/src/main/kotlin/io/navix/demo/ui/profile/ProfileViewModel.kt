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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val getCurrentUser: GetCurrentUserUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _navEffect = Channel<ProfileNavEffect>(Channel.BUFFERED)
    val navEffect = _navEffect.receiveAsFlow()

    init {
        loadUser()
    }

    private fun loadUser() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getCurrentUser()
                .onSuccess { user ->
                    _uiState.update { it.copy(user = user, isLoading = false) }
                }.onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun onOpenSettings() {
        viewModelScope.launch {
            _navEffect.send(ProfileNavEffect.OpenSettings)
        }
    }

    fun onSignOut() {
        viewModelScope.launch {
            _navEffect.send(ProfileNavEffect.SignOut)
        }
    }

    fun onResetToHome() {
        viewModelScope.launch {
            _navEffect.send(ProfileNavEffect.ResetToHome)
        }
    }

    fun onBack() {
        viewModelScope.launch {
            _navEffect.send(ProfileNavEffect.NavigateBack)
        }
    }
}
