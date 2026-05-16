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
package io.navix.compose

import io.navix.contracts.BackstackSnapshot
import io.navix.contracts.Route
import io.navix.contracts.RouteEntry
import io.navix.runtime.NavigatorSaver

// Test destinations. data objects so the trivial saver below can round-trip them by
// their simple class name without needing the kotlinx.serialization plugin in this module.
internal data object HomeR : Route
internal data object DetailR : Route
internal data object Detail2R : Route
internal data object TabHomeR : Route
internal data object TabSearchR : Route
internal data object TabDetailR : Route

/**
 * Minimal [NavigatorSaver] for tests. Encodes `entryId|RouteSimpleName` per line so the
 * **entry id is preserved** across restore (per-entry saved-state slots are keyed by id).
 */
internal object TestSaver : NavigatorSaver {

    private val routes: Map<String, Route> =
        listOf(HomeR, DetailR, Detail2R, TabHomeR, TabSearchR, TabDetailR)
            .associateBy { it::class.simpleName!! }

    override fun save(snapshot: BackstackSnapshot): ByteArray =
        snapshot.entries.joinToString("\n") { "${it.id}|${it.route::class.simpleName}" }
            .encodeToByteArray()

    override fun restore(bytes: ByteArray): BackstackSnapshot? = runCatching {
        BackstackSnapshot(
            bytes.decodeToString()
                .split("\n")
                .filter { it.isNotBlank() }
                .map { line ->
                    val sep = line.indexOf('|')
                    RouteEntry(
                        id = line.substring(0, sep),
                        route = routes.getValue(line.substring(sep + 1)),
                        createdAt = 0L,
                    )
                },
        )
    }.getOrNull()
}
