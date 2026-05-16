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
package io.navix.demo.data.deeplink

import io.navix.contracts.Route
import io.navix.demo.routes.Profile
import io.navix.runtime.DeepLinkHandler

/**
 * Resolves `navix://profile` to the [Profile] route.
 *
 * Registered alongside [ProductDeepLinkHandler] to demonstrate that multiple handlers
 * are evaluated in order — the first match wins.
 */
class ProfileDeepLinkHandler : DeepLinkHandler {
    override fun canHandle(uri: String): Boolean = uri == "navix://profile"

    override fun resolve(uri: String): Route = Profile
}
