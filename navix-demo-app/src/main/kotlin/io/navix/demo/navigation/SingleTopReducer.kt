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

import io.navix.contracts.BackstackSnapshot
import io.navix.runtime.BackstackAction
import io.navix.runtime.DefaultEntryFactory
import io.navix.runtime.DefaultReducer
import io.navix.runtime.EntryFactory
import io.navix.runtime.Reducer

/**
 * A custom [Reducer] that wraps [DefaultReducer] and adds single-top push semantics:
 * if a [BackstackAction.Push] targets a route **type** already present anywhere in the
 * stack, the reducer pops to that entry instead of creating a duplicate.
 *
 * ### Deduplication is class-based, not instance-based
 *
 * The reducer compares `route::class` — the Kotlin class of the route — not the route
 * instance's equality. This means **all instances of the same route type** are treated
 * as the same destination:
 *
 * ```
 * Stack: [Home, ProductDetail("42"), ProductDetail("99")]
 * push(ProductDetail("123"))
 * Result: [Home, ProductDetail("42")]  ← popped to last ProductDetail, NOT navigated to "123"
 * ```
 *
 * If distinct parameter values should produce separate stack entries, use [DefaultReducer]
 * (the default).
 *
 * ### Typical use case
 * Bottom-navigation tabs where pressing an already-selected tab should bring the
 * existing instance to the top rather than layering a new one:
 *
 * ```kotlin
 * rememberNavigator(root = Home, reducer = SingleTopReducer())
 * ```
 *
 * This demonstrates the [Reducer] escape hatch for advanced stack behaviour.
 */
class SingleTopReducer(
    entryFactory: EntryFactory = DefaultEntryFactory,
    private val delegate: Reducer = DefaultReducer(entryFactory)
) : Reducer {
    override fun reduce(
        snapshot: BackstackSnapshot,
        action: BackstackAction
    ): BackstackSnapshot {
        if (action is BackstackAction.Push) {
            val existingIndex = snapshot.entries.indexOfLast { it.route::class == action.route::class }
            if (existingIndex >= 0) {
                // Route type already on the stack — pop to it instead of pushing a duplicate.
                return delegate.reduce(
                    snapshot,
                    BackstackAction.PopTo(action.route::class, inclusive = false)
                )
            }
        }
        return delegate.reduce(snapshot, action)
    }
}
