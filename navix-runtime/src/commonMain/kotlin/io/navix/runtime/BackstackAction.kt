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
package io.navix.runtime

import io.navix.contracts.NavTransitionKey
import io.navix.contracts.Route
import kotlin.reflect.KClass

/**
 * All possible mutations to the navigation backstack.
 *
 * Actions are consumed by [Reducer], a pure function. Each action produces exactly one
 * new [io.navix.contracts.BackstackSnapshot] — no action has side effects inside the reducer.
 *
 * Entry identity ([io.navix.contracts.RouteEntry.id], [io.navix.contracts.RouteEntry.createdAt])
 * is fully determined by the [EntryFactory] injected into the [Reducer] — not by the action
 * itself. This keeps actions simple value objects and the reducer referentially transparent:
 * given the same snapshot, action, and factory output, the result is always identical.
 *
 * Transition-affecting actions carry a [NavTransitionKey] that is stored on the resulting
 * [io.navix.contracts.RouteEntry]. `NavixHost` reads the key from the entry, eliminating
 * the need for a separate transition-key StateFlow.
 */
sealed class BackstackAction {
    /**
     * Push [route] onto the top of the stack with the given [transition] animation.
     */
    data class Push(
        val route: Route,
        val transition: NavTransitionKey = NavTransitionKey.Default,
    ) : BackstackAction()

    /**
     * Remove the topmost entry using the given [transition] animation (applied to the
     * entry being revealed). No-op if the stack has only one entry.
     */
    data class Pop(
        val transition: NavTransitionKey = NavTransitionKey.SlideRight,
    ) : BackstackAction()

    /**
     * Replace the topmost entry with [route], preserving the rest of the stack.
     */
    data class Replace(
        val route: Route,
        val transition: NavTransitionKey = NavTransitionKey.Default,
    ) : BackstackAction()

    /**
     * Clear the entire stack and set [root] as the sole entry.
     */
    data class Reset(
        val root: Route,
        val transition: NavTransitionKey = NavTransitionKey.Fade,
    ) : BackstackAction()

    /**
     * Pop entries until the first entry whose route matches [routeClass].
     * If [inclusive] is true, that entry is also popped.
     * No-op if no matching entry exists.
     */
    data class PopTo(
        val routeClass: KClass<out Route>,
        val inclusive: Boolean = false,
        val transition: NavTransitionKey = NavTransitionKey.SlideRight,
    ) : BackstackAction()
}
