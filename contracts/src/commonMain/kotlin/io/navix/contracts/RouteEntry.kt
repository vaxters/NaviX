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

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable

/**
 * A single entry in the navigation backstack.
 *
 * [id] is a unique identifier generated at entry creation time. It is stable across
 * recompositions and allows multiple instances of the same [Route] type to coexist
 * on the stack without identity collision (e.g. A → B → A deep-drill).
 *
 * [transitionKey] records the animation key used when this entry became active. For
 * entries created by Push or Replace it is the key passed by the caller. For entries
 * revealed by Pop or PopTo it is the reverse-animation key set by the runtime.
 * `NavixHost` reads this field as the single source of truth for animation — there is
 * no separate transition-key StateFlow to keep in sync.
 *
 * Per-entry state that must survive process death belongs on the per-entry
 * `SavedStateHandle` provided by the Compose host (see `navix-compose`). Route arguments
 * already survive process death because [Route] is `@Serializable`.
 */
@Serializable
data class RouteEntry(
    val id: String,
    @Polymorphic val route: Route,
    val createdAt: Long,
    val lifecycleState: NavLifecycleState = NavLifecycleState.CREATED,
    val transitionKey: NavTransitionKey = NavTransitionKey.Default,
)
