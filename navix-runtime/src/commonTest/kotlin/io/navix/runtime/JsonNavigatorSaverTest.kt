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
import io.navix.contracts.NavLifecycleState
import io.navix.contracts.NavTransitionKey
import io.navix.contracts.Route
import io.navix.contracts.RouteEntry
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** Routes module that mirrors what KSP would generate for the test route set. */
private val testRoutesModule =
    SerializersModule {
        polymorphic(Route::class) {
            subclass(HomeRoute::class)
            subclass(DetailRoute::class)
            subclass(SettingsRoute::class)
            subclass(ProfileRoute::class)
        }
    }

class JsonNavigatorSaverTest {
    private val saver = JsonNavigatorSaver(testRoutesModule)

    // ── Round-trip correctness ─────────────────────────────────────────────

    @Test
    fun save_thenRestore_singleEntry_producesIdenticalSnapshot() {
        val original =
            BackstackSnapshot(
                entries =
                    listOf(
                        RouteEntry(
                            id = "entry-1",
                            route = HomeRoute,
                            createdAt = 1_000L,
                            lifecycleState = NavLifecycleState.RESUMED,
                            transitionKey = NavTransitionKey.Default,
                        ),
                    ),
            )

        val restored = saver.restore(saver.save(original))

        assertNotNull(restored)
        assertEquals(original, restored)
    }

    @Test
    fun save_thenRestore_multipleEntries_preservesOrderAndParams() {
        val original =
            BackstackSnapshot(
                entries =
                    listOf(
                        RouteEntry(id = "e1", route = HomeRoute, createdAt = 1_000L),
                        RouteEntry(id = "e2", route = DetailRoute("42"), createdAt = 2_000L),
                        RouteEntry(id = "e3", route = SettingsRoute, createdAt = 3_000L),
                    ),
            )

        val restored = saver.restore(saver.save(original))

        assertNotNull(restored)
        assertEquals(3, restored.entries.size)
        assertEquals("e1", restored.entries[0].id)
        assertEquals(HomeRoute, restored.entries[0].route)
        assertEquals(DetailRoute("42"), restored.entries[1].route)
        assertEquals(SettingsRoute, restored.entries[2].route)
    }

    @Test
    fun save_thenRestore_dataClassRoute_preservesArguments() {
        val snapshot =
            BackstackSnapshot(
                entries =
                    listOf(
                        RouteEntry(id = "e1", route = DetailRoute(id = "product-99"), createdAt = 0L),
                    ),
            )

        val restored = saver.restore(saver.save(snapshot))

        assertNotNull(restored)
        val route = restored.entries.first().route as DetailRoute
        assertEquals("product-99", route.id)
    }

    @Test
    fun save_thenRestore_transitionKey_preserved() {
        val snapshot =
            BackstackSnapshot(
                entries =
                    listOf(
                        RouteEntry(
                            id = "e1",
                            route = HomeRoute,
                            createdAt = 0L,
                            transitionKey = NavTransitionKey.SlideLeft,
                        ),
                    ),
            )

        val restored = saver.restore(saver.save(snapshot))

        assertNotNull(restored)
        assertEquals(NavTransitionKey.SlideLeft, restored.entries.first().transitionKey)
    }

    // ── Failure / degradation ──────────────────────────────────────────────

    @Test
    fun restore_corruptBytes_returnsNull() {
        val restored = saver.restore("not valid json".encodeToByteArray())
        assertNull(restored)
    }

    @Test
    fun restore_emptyBytes_returnsNull() {
        val restored = saver.restore(ByteArray(0))
        assertNull(restored)
    }

    @Test
    fun restore_schemaIncompatibleJson_returnsNull() {
        // A JSON object that has no resemblance to BackstackSnapshot.
        val bytes = """{"foo":"bar"}""".encodeToByteArray()
        val restored = saver.restore(bytes)
        // Either null (parse error) or an empty snapshot — either is acceptable
        // as long as we don't throw.
        if (restored != null) assertEquals(emptyList(), restored.entries)
    }

    // ── prettyPrint variant ────────────────────────────────────────────────

    @Test
    fun save_prettyPrint_thenRestore_producesIdenticalSnapshot() {
        val prettySaver = JsonNavigatorSaver(testRoutesModule, prettyPrint = true)
        val original =
            BackstackSnapshot(
                entries =
                    listOf(
                        RouteEntry(id = "e1", route = HomeRoute, createdAt = 0L),
                    ),
            )

        val restored = prettySaver.restore(prettySaver.save(original))

        assertNotNull(restored)
        assertEquals(original, restored)
    }
}
