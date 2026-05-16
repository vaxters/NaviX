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
 * the event type, source route, destination route, and any metadata.
 *
 * Suitable for development and debug builds only. Disable or replace with a
 * [NoOpExporter] in release builds.
 */
class LogcatExporter(private val tag: String = "Navix") : NavEventExporter {

    override fun export(event: NavEvent) {
        val from = event.from?.route?.let { it::class.qualifiedName ?: it::class.simpleName } ?: "null"
        val to = event.to?.route?.let { it::class.qualifiedName ?: it::class.simpleName } ?: "null"
        val meta = if (event.metadata.isEmpty()) "" else " ${event.metadata}"
        Log.d(tag, "[${event.type}] $from → $to$meta")
    }
}
