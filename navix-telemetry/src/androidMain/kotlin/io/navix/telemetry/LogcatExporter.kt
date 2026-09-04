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
package io.navix.telemetry

import android.util.Log
import io.navix.contracts.NavEvent

/**
 * A [NavEventExporter] that writes navigation events to Android Logcat.
 *
 * Each event is logged at [Log.DEBUG] priority with a formatted message that includes
 * the event type, source route, and destination route.
 *
 * [event.metadata][NavEvent.metadata] is deliberately **not** logged by default — a
 * [io.navix.runtime.Navigator.handleDeepLink] event carries the full inbound URI there,
 * which can contain session tokens or other sensitive query parameters that should not
 * land in Logcat (readable by other apps/tools on many devices). Pass
 * `includeMetadata = true` to opt back in, e.g. for local debugging of a specific
 * deep-link issue.
 *
 * Suitable for development and debug builds only. Disable or replace with a
 * [NoOpExporter] in release builds.
 */
class LogcatExporter(
    private val tag: String = "Navix",
    private val includeMetadata: Boolean = false
) : NavEventExporter {
    override fun export(event: NavEvent) {
        val from = event.from?.route?.let { it::class.qualifiedName ?: it::class.simpleName } ?: "null"
        val to = event.to?.route?.let { it::class.qualifiedName ?: it::class.simpleName } ?: "null"
        val meta =
            when {
                event.metadata.isEmpty() -> ""
                includeMetadata -> " ${event.metadata}"
                else -> " {${event.metadata.keys.joinToString()}=<redacted>}"
            }
        Log.d(tag, "[${event.type}] $from → $to$meta")
    }
}
