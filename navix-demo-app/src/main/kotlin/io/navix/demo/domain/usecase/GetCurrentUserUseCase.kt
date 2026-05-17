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
package io.navix.demo.domain.usecase

import io.navix.demo.data.model.User
import io.navix.demo.data.repository.UserRepository

/**
 * Returns the currently signed-in [User] wrapped in a [Result].
 */
class GetCurrentUserUseCase(
    private val repository: UserRepository
) {
    suspend operator fun invoke(): Result<User> = runCatching { repository.getCurrentUser() }
}
