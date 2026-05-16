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
import io.navix.demo.routes.Login
import io.navix.demo.routes.Register
import io.navix.demo.ui.auth.AuthScreen
import io.navix.demo.ui.auth.AuthViewModel
import io.navix.demo.ui.register.RegisterScreen
import io.navix.demo.ui.register.RegisterViewModel
import io.navix.runtime.Navigator

fun NavGraphBuilder.authGraph(navigator: Navigator) {
    screen<Login> { _, _ ->
        val vm: AuthViewModel = viewModel()
        AuthScreen(navigator = navigator, viewModel = vm)
    }

    screen<Register> { _, _ ->
        val vm: RegisterViewModel = viewModel()
        RegisterScreen(navigator = navigator, viewModel = vm)
    }
}
