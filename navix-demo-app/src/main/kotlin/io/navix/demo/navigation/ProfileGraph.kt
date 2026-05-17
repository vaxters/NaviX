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
package io.navix.demo.navigation

import androidx.lifecycle.viewmodel.compose.viewModel
import io.navix.compose.NavGraphBuilder
import io.navix.compose.screen
import io.navix.demo.DemoApp
import io.navix.demo.data.repository.FakeUserRepository
import io.navix.demo.domain.usecase.GetCurrentUserUseCase
import io.navix.demo.routes.Profile
import io.navix.demo.routes.Settings
import io.navix.demo.ui.profile.ProfileScreen
import io.navix.demo.ui.profile.ProfileViewModel
import io.navix.demo.ui.settings.SettingsScreen
import io.navix.demo.ui.settings.SettingsViewModel
import io.navix.runtime.Navigator

fun NavGraphBuilder.profileGraph(
    navigator: Navigator,
    app: DemoApp
) {
    screen<Profile> { _, _ ->
        val vm: ProfileViewModel =
            viewModel {
                ProfileViewModel(GetCurrentUserUseCase(FakeUserRepository()))
            }
        ProfileScreen(navigator = navigator, viewModel = vm)
    }

    screen<Settings> { _, _ ->
        val vm: SettingsViewModel =
            viewModel {
                SettingsViewModel(app.settingsRepository)
            }
        SettingsScreen(navigator = navigator, viewModel = vm)
    }
}
