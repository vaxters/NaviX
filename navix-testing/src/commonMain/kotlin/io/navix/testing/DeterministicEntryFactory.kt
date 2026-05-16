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
package io.navix.testing

import io.navix.contracts.NavLifecycleState
import io.navix.contracts.NavTransitionKey
import io.navix.contracts.Route
import io.navix.contracts.RouteEntry
import io.navix.runtime.EntryFactory

/**
 * A test [EntryFactory] that produces stable, sequential entry IDs and a fixed zero
 * timestamp so that [io.navix.contracts.BackstackSnapshot] equality checks are reliable
 * in unit tests.
 *
 * Each instance maintains its own counter. Use a single instance shared between the
 * [io.navix.runtime.DefaultReducer] and the initial snapshot to ensure IDs are globally
 * sequential across the entire test:
 *
 * ```kotlin
 * val factory = DeterministicEntryFactory()
 * val navigator = FakeNavigator(root = Home, entryFactory = factory)
 * // first entry id = "entry-0", next push id = "entry-1", …
 * ```
 *
 * This is the default factory used by [FakeNavigator].
 */
class DeterministicEntryFactory(private var seed: Int = 0) : EntryFactory {
    override fun create(route: Route, transition: NavTransitionKey): RouteEntry = RouteEntry(
        id = "entry-${seed++}",
        route = route,
        createdAt = 0L,
        lifecycleState = NavLifecycleState.RESUMED,
        transitionKey = transition,
    )
}
