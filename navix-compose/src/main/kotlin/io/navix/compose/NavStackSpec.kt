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
package io.navix.compose

import io.navix.contracts.Route

/**
 * Defines a single tab in a [NavixMultiStack].
 *
 * @param root The initial destination placed at the bottom of this tab's backstack.
 *   Captured once when [rememberNavixMultiStack] creates the [Navigator] for this tab;
 *   later changes have no effect (same contract as [rememberNavigator]).
 * @param key Unique string identifier for this tab. Used by [NavixMultiStack] to
 *   differentiate tabs that share the same [root] type. Defaults to the simple class
 *   name of [root].
 */
data class NavStackSpec(val root: Route, val key: String = root::class.simpleName ?: "tab") {
    init {
        require(key.isNotBlank()) { "NavStackSpec key must not be blank." }
    }
}
