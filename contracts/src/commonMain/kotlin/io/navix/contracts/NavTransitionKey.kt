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
 * Identifies which transition animation should be applied for a navigation action.
 *
 * [NavTransitionKey] is a data class keyed by a string identifier.
 * The key is stored on each [RouteEntry] and read by `NavixHost` to select the
 * appropriate `NavTransitionSpec` enter/exit pair.
 *
 * Built-in keys are declared in the companion. Custom keys are created with any unique
 * string id and handled by a custom `NavTransitionSpec` registered in `NavixHost`.
 */
@Serializable
data class NavTransitionKey(
    val id: String,
) {
    companion object {
        val Default = NavTransitionKey("default")
        val None = NavTransitionKey("none")
        val Fade = NavTransitionKey("fade")
        val SlideLeft = NavTransitionKey("slide_left")
        val SlideRight = NavTransitionKey("slide_right")
        val Scale = NavTransitionKey("scale")
        val SharedAxisX = NavTransitionKey("shared_axis_x")
        val SharedAxisY = NavTransitionKey("shared_axis_y")
        val SharedAxisZ = NavTransitionKey("shared_axis_z")
    }
}
