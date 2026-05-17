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
package io.navix.contracts

import kotlinx.serialization.Serializable

/**
 * Immutable snapshot of the full navigation backstack at a point in time.
 *
 * Wrapping [List<RouteEntry>] as a named type provides a stable ABI boundary:
 * derived properties (canPop, depth, active) can be added here without
 * breaking collectors of the [io.navix.runtime.Navigator.backstack] StateFlow.
 */
@Serializable
data class BackstackSnapshot(val entries: List<RouteEntry>) {
    /** The currently visible destination, or null when the stack is empty. */
    val active: RouteEntry?
        get() = entries.lastOrNull()

    /** True when there is more than one entry — i.e. [io.navix.runtime.Navigator.pop] is meaningful. */
    val canPop: Boolean
        get() = entries.size > 1

    /** Number of entries on the stack. */
    val depth: Int
        get() = entries.size

    companion object {
        val Empty: BackstackSnapshot = BackstackSnapshot(emptyList())
    }
}
