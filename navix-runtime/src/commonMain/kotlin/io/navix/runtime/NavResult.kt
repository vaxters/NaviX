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

/**
 * The outcome of a [Navigator.pushForResult] call.
 *
 * Use [NavResult] when callers need to distinguish between a result that was explicitly
 * set by the target screen and one where the screen was dismissed without providing a
 * value (e.g. back-press or stack reset):
 *
 * ```kotlin
 * when (val result = navigator.pushForResult<Color>(PickerRoute)) {
 *     is NavResult.Success -> applyColor(result.value)
 *     is NavResult.Cancelled -> { /* user dismissed without picking */ }
 * }
 * ```
 *
 * When the caller doesn't care about cancellation, use the non-sealed
 * [Navigator.pushForResult] return value directly — it returns `null` on cancellation:
 *
 * ```kotlin
 * val color: Color? = navigator.pushForResult<Color>(PickerRoute)
 * color?.let { applyColor(it) }
 * ```
 */
sealed class NavResult<out R> {
    /** The target screen called [Navigator.setResult] with a value of type [R]. */
    data class Success<R>(
        val value: R,
    ) : NavResult<R>()

    /**
     * The target screen was popped/reset without calling [Navigator.setResult].
     *
     * This happens when the user presses back, when [Navigator.reset] is called while
     * the target entry is in the stack, or when the process dies and is not restored.
     */
    data object Cancelled : NavResult<Nothing>()
}

/**
 * Returns the contained [value][NavResult.Success.value] if this is [NavResult.Success],
 * or `null` if this is [NavResult.Cancelled].
 */
fun <R> NavResult<R>.getOrNull(): R? =
    when (this) {
        is NavResult.Success -> value
        NavResult.Cancelled -> null
    }

/**
 * Returns the contained [value][NavResult.Success.value] if this is [NavResult.Success],
 * or [default] if this is [NavResult.Cancelled].
 */
fun <R> NavResult<R>.getOrDefault(default: R): R =
    when (this) {
        is NavResult.Success -> value
        NavResult.Cancelled -> default
    }
