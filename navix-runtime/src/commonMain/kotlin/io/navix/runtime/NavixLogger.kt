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
 * Receives internal NaviX diagnostic messages.
 *
 * Implement this interface to route warnings to your preferred logging backend (e.g.
 * `android.util.Log`, Timber, or a crash reporter). Provide your implementation via
 * [createNavigator] or the Compose [io.navix.compose.rememberNavigator] / [io.navix.compose.rememberSaveableNavigator] helpers.
 *
 * - [Default] — writes to [println]. Suitable for development.
 * - [NoOp] — silences all internal NaviX warnings. Use in production if you prefer zero log noise.
 */
interface NavixLogger {
    fun warn(message: String, throwable: Throwable? = null)

    companion object {
        val Default: NavixLogger = object : NavixLogger {
            override fun warn(message: String, throwable: Throwable?) {
                println("[NaviX][WARN] $message${throwable?.let { " — $it" } ?: ""}")
            }
        }
        val NoOp: NavixLogger = object : NavixLogger {
            override fun warn(message: String, throwable: Throwable?) = Unit
        }
    }
}
