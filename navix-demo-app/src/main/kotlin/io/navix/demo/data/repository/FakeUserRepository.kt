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

import io.navix.demo.data.model.User
import kotlinx.coroutines.delay

class FakeUserRepository : UserRepository {
    override suspend fun getCurrentUser(): User {
        delay(200) // Simulate network latency
        return User(name = "Demo User", email = "demo@navix.io")
    }
}
