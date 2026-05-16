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

import io.navix.contracts.Route
import io.navix.contracts.RouteEntry
import io.navix.contracts.NavTransitionKey
import io.navix.contracts.NavLifecycleState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Minimal route stubs for testing
private data object HomeRoute : Route
private data object DetailRoute : Route
private data object ConfirmDialog : Route
private data object FilterSheet : Route

/**
 * Unit tests for [NavGraphBuilder] / [NavGraphBuilderImpl] covering:
 * - [DestinationKind] assignment per registration method
 * - Inline reified overloads ([screen], [dialog], [bottomSheet])
 * - Last-registration-wins for duplicate keys
 * - Missing route returns null (no crash)
 */
class NavGraphBuilderTest {

    private fun builder(block: NavGraphBuilder.() -> Unit): NavGraphBuilderImpl =
        NavGraphBuilderImpl().apply(block)

    // ── screen registrations ───────────────────────────────────────────────

    @Test
    fun screen_registersContentAndKind() {
        val b = builder { screen<HomeRoute> { _, _ -> } }
        assertNotNull(b.destinations[HomeRoute::class], "destinations should contain HomeRoute")
        assertEquals(DestinationKind.Screen, b.destinationKinds[HomeRoute::class])
    }

    @Test
    fun screen_multipleDistinctRoutes_allRegistered() {
        val b = builder {
            screen<HomeRoute> { _, _ -> }
            screen<DetailRoute> { _, _ -> }
        }
        assertEquals(2, b.destinations.size)
        assertEquals(DestinationKind.Screen, b.destinationKinds[HomeRoute::class])
        assertEquals(DestinationKind.Screen, b.destinationKinds[DetailRoute::class])
    }

    // ── dialog registrations ───────────────────────────────────────────────

    @Test
    fun dialog_registersContentAndKind() {
        val b = builder { dialog<ConfirmDialog> { _, _ -> } }
        assertNotNull(b.destinations[ConfirmDialog::class])
        assertEquals(DestinationKind.Dialog, b.destinationKinds[ConfirmDialog::class])
    }

    @Test
    fun dialog_doesNotOverwriteScreenRegistrations() {
        val b = builder {
            screen<HomeRoute> { _, _ -> }
            dialog<ConfirmDialog> { _, _ -> }
        }
        assertEquals(DestinationKind.Screen, b.destinationKinds[HomeRoute::class])
        assertEquals(DestinationKind.Dialog, b.destinationKinds[ConfirmDialog::class])
        assertEquals(2, b.destinations.size)
    }

    // ── bottomSheet registrations ──────────────────────────────────────────

    @Test
    fun bottomSheet_registersContentAndKind() {
        val b = builder { bottomSheet<FilterSheet> { _, _ -> } }
        assertNotNull(b.destinations[FilterSheet::class])
        assertEquals(DestinationKind.BottomSheet, b.destinationKinds[FilterSheet::class])
    }

    @Test
    fun allThreeKinds_registeredTogether() {
        val b = builder {
            screen<HomeRoute> { _, _ -> }
            dialog<ConfirmDialog> { _, _ -> }
            bottomSheet<FilterSheet> { _, _ -> }
        }
        assertEquals(3, b.destinations.size)
        assertEquals(3, b.destinationKinds.size)
        assertEquals(DestinationKind.Screen, b.destinationKinds[HomeRoute::class])
        assertEquals(DestinationKind.Dialog, b.destinationKinds[ConfirmDialog::class])
        assertEquals(DestinationKind.BottomSheet, b.destinationKinds[FilterSheet::class])
    }

    // ── last-registration-wins ─────────────────────────────────────────────

    @Test
    fun registerSameRouteAsScreenThenDialog_lastKindWins() {
        val b = builder {
            screen<ConfirmDialog> { _, _ -> }   // registered first as Screen
            dialog<ConfirmDialog> { _, _ -> }   // overwritten as Dialog
        }
        assertEquals(DestinationKind.Dialog, b.destinationKinds[ConfirmDialog::class],
            "Last registration should win — Dialog should overwrite Screen")
        assertEquals(1, b.destinations.size)
    }

    @Test
    fun registerSameRouteAsDialogThenBottomSheet_lastKindWins() {
        val b = builder {
            dialog<FilterSheet> { _, _ -> }
            bottomSheet<FilterSheet> { _, _ -> }
        }
        assertEquals(DestinationKind.BottomSheet, b.destinationKinds[FilterSheet::class])
    }

    // ── missing routes ─────────────────────────────────────────────────────

    @Test
    fun unregisteredRoute_returnsNullFromDestinations() {
        val b = builder { screen<HomeRoute> { _, _ -> } }
        assertNull(b.destinations[DetailRoute::class])
    }

    @Test
    fun unregisteredRoute_returnsNullFromDestinationKinds() {
        val b = builder { screen<HomeRoute> { _, _ -> } }
        assertNull(b.destinationKinds[DetailRoute::class])
    }

    // ── DestinationKind enum ───────────────────────────────────────────────

    @Test
    fun destinationKind_hasThreeValues() {
        assertEquals(3, DestinationKind.entries.size)
        assertTrue(DestinationKind.entries.contains(DestinationKind.Screen))
        assertTrue(DestinationKind.entries.contains(DestinationKind.Dialog))
        assertTrue(DestinationKind.entries.contains(DestinationKind.BottomSheet))
    }
}
