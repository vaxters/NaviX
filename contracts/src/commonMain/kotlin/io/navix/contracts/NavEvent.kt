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

/**
 * A discrete navigation event emitted by the runtime for every state transition.
 *
 * Events are the telemetry and replay backbone. Every push, pop, replace, reset,
 * popTo, and deep-link resolution produces exactly one [NavEvent].
 *
 * [metadata] carries event-specific key/value pairs (e.g. "uri" for deep links).
 */
data class NavEvent(
    val type: NavEventType,
    val from: RouteEntry?,
    val to: RouteEntry?,
    val timestampMs: Long,
    val metadata: Map<String, String> = emptyMap(),
)

enum class NavEventType {
    PUSH,
    POP,
    REPLACE,
    RESET,
    POP_TO,
    DEEP_LINK,
}
