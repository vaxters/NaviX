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

import io.navix.contracts.BackstackSnapshot
import io.navix.contracts.NavEvent
import io.navix.contracts.NavTransitionKey
import io.navix.contracts.Route
import io.navix.contracts.RouteEntry
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.reflect.KClass

/**
 * The primary navigation interface. Owns the backstack state and exposes navigation actions.
 *
 * [backstack] is the single source of truth — a [StateFlow] that Compose collectors observe
 * for recomposition. Each [io.navix.contracts.RouteEntry] carries its [NavTransitionKey],
 * so `NavixHost` derives animation keys from a single StateFlow read.
 *
 * [events] is the telemetry/replay feed — one [NavEvent] per action.
 *
 * ### Thread safety
 * All navigation methods are expected to be called from the main thread.
 */
interface Navigator {
    val backstack: StateFlow<BackstackSnapshot>

    val events: SharedFlow<NavEvent>

    fun push(
        route: Route,
        transition: NavTransitionKey = NavTransitionKey.Default,
    )

    /**
     * Pops the top entry from the backstack.
     *
     * This is a fire-and-forget call: it enqueues a [BackstackAction.Pop] on the actor
     * channel and returns immediately. If the backstack has only one entry the reducer
     * treats the action as a no-op — no state change occurs. To check poppability before
     * calling, read [backstack]`.value.canPop`.
     *
     * @param transition The animation played when returning to the previous screen.
     *   Defaults to [NavTransitionKey.SlideRight] (the reverse of the standard push slide).
     *   Pass a different key to customise pop animations, e.g. [NavTransitionKey.Fade]
     *   for modal dismissal.
     */
    fun pop(transition: NavTransitionKey = NavTransitionKey.SlideRight)

    fun replace(
        route: Route,
        transition: NavTransitionKey = NavTransitionKey.Default,
    )

    fun reset(root: Route)

    fun popTo(
        routeClass: KClass<out Route>,
        inclusive: Boolean = false,
    )

    fun handleDeepLink(uri: String): Boolean

    /**
     * The entry immediately below the current active entry, or `null` when at the root.
     *
     * Useful in result-passing flows where the callee screen writes to the previous entry's
     * `SavedStateHandle` and the caller reads it.
     */
    val previousEntry: RouteEntry?
        get() {
            val entries = backstack.value.entries
            return if (entries.size >= 2) entries[entries.size - 2] else null
        }

    /**
     * Pushes [route] onto the backstack and suspends until the pushed entry is popped.
     *
     * Returns the value passed to [setResult] by the pushed screen, wrapped in
     * [NavResult.Success]. Returns [NavResult.Cancelled] if the entry is popped without
     * calling [setResult] — e.g. the user presses back, [reset] is called, or the process
     * dies and is not restored.
     *
     * **Usage:**
     * ```kotlin
     * // Caller screen
     * val result = navigator.pushForResult<Color>(ColorPickerRoute)
     * if (result is NavResult.Success) applyColor(result.value)
     *
     * // Callee screen
     * navigator.setResult(Color.Red)
     * navigator.pop()
     * ```
     *
     * This is a **suspend** function; call it from a `LaunchedEffect` or a ViewModel
     * coroutine, NOT from a click handler directly.
     *
     * @param route The destination to push.
     * @param transition The enter animation. Defaults to [NavTransitionKey.Default].
     */
    suspend fun <R : Any> pushForResult(
        route: Route,
        transition: NavTransitionKey = NavTransitionKey.Default,
    ): NavResult<R>

    /**
     * Sets the result for the current (topmost) entry.
     *
     * The result is delivered to the [pushForResult] caller when this entry is subsequently
     * popped from the stack. Calling [setResult] more than once replaces the previous value;
     * only the last value is delivered. Calling [setResult] without a following [pop] has no
     * immediate effect — the result is held in memory until the entry leaves the stack.
     *
     * @param value The value to deliver. Use a type that the caller knows to cast to [R].
     */
    fun setResult(value: Any?)
}

inline fun <reified T : Route> Navigator.popTo(inclusive: Boolean = false) {
    popTo(T::class, inclusive)
}

/**
 * Pushes [route] and suspends until the result is available, unwrapping [NavResult.Success]
 * to a nullable [R]. This is a convenience overload for callers that treat cancellation as
 * a `null` result rather than a distinct state.
 *
 * ```kotlin
 * val color: Color? = navigator.pushForResultOrNull<Color>(ColorPickerRoute)
 * color?.let { applyColor(it) }
 * ```
 */
suspend inline fun <reified R : Any> Navigator.pushForResultOrNull(
    route: Route,
    transition: NavTransitionKey = NavTransitionKey.Default,
): R? = pushForResult<R>(route, transition).getOrNull()
