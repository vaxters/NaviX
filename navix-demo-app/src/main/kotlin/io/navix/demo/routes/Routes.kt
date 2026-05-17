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
package io.navix.demo.routes

import io.navix.annotations.RouteDestination
import io.navix.contracts.Route
import kotlinx.serialization.Serializable

// ── Auth graph ────────────────────────────────────────────────────────────────

@Serializable
@RouteDestination
data object Login : Route

@Serializable
@RouteDestination
data object Register : Route

// ── Home graph ────────────────────────────────────────────────────────────────

@Serializable
@RouteDestination
data object Home : Route

// ── Product graph ─────────────────────────────────────────────────────────────

@Serializable
@RouteDestination(deepLinks = ["navix://product/{productId}"])
data class ProductDetail(
    val productId: String
) : Route

@Serializable
@RouteDestination
data class ProductReviews(
    val productId: String
) : Route

// ── Profile graph ─────────────────────────────────────────────────────────────

@Serializable
@RouteDestination
data object Profile : Route

@Serializable
@RouteDestination
data object Settings : Route

// ── Telemetry viewer (demo-only) ──────────────────────────────────────────────

@Serializable
@RouteDestination
data object TelemetryViewer : Route

// ── Navigation Playground (demo-only) ────────────────────────────────────────

/** Landing screen for the interactive navigation playground. */
@Serializable
@RouteDestination
data object NavPlayground : Route

/** A numbered step pushed onto the backstack by the playground. */
@Serializable
@RouteDestination
data class NavPlaygroundStep(
    val stepNumber: Int
) : Route

// ── Multi-stack demo (Phase 6A) ───────────────────────────────────────────────

/** Entry point for the multi-stack / tab-navigation demo. */
@Serializable
@RouteDestination
data object TabsDemo : Route

// Tab-internal routes — no @RouteDestination because they are only reachable
// within the NavixMultiStackHost, not through the outer NavixHost graph.
@Serializable
data object CounterTab : Route

@Serializable
data object ListTab : Route

@Serializable
data object InfoTab : Route

@Serializable
data class ListTabDetail(
    val item: String
) : Route

// ── Result passing demo (Phase 9) ────────────────────────────────────────────

/** Entry point for the result-passing demo. */
@Serializable
@RouteDestination
data object ResultPassingDemo : Route

/** Color picker pushed by [ResultPassingDemo]; calls setResult(Color) then pop(). */
@Serializable
@RouteDestination
data object ColorPickerRoute : Route

// ── Dialog / BottomSheet demo (Phase 8) ──────────────────────────────────────

/** Entry point for the dialog-destination demo. */
@Serializable
@RouteDestination
data object OverlayDemo : Route

/**
 * Confirmation dialog shown as a [DestinationKind.Dialog] backstack entry.
 * Registered in the main NavixHost graph via `dialog<ConfirmActionDialog> { }`.
 */
@Serializable
@RouteDestination
data object ConfirmActionDialog : Route
