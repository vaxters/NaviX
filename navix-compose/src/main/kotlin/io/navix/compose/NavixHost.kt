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

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.navix.contracts.RouteEntry
import io.navix.runtime.Navigator

/**
 * The primary Compose entry point for Navix navigation.
 *
 * [NavixHost] renders the currently active screen entry using [AnimatedContent] and, when the
 * topmost backstack entry is a [DestinationKind.Dialog] or [DestinationKind.BottomSheet],
 * renders it in the appropriate overlay container above the screen layer. The screen below
 * remains composed and fully interactive while an overlay is visible.
 *
 * ### Destination kinds
 * Register destinations with [NavGraphBuilder.screen], [NavGraphBuilder.dialog], or
 * [NavGraphBuilder.bottomSheet]. The host automatically determines which render path to use
 * based on the registered [DestinationKind]:
 *
 * ```kotlin
 * NavixHost(navigator = navigator) {
 *     screen<Home> { _, _ -> HomeScreen() }
 *     dialog<ConfirmDelete> { _, route -> ConfirmDeleteDialog(route.itemId) }
 *     bottomSheet<FilterOptions> { _, _ -> FilterOptionsSheet() }
 * }
 * ```
 *
 * Pressing back (or dismissing an overlay) calls [Navigator.pop], which removes the overlay
 * entry from the backstack and returns focus to the screen below.
 *
 * ### Per-entry lifecycle, ViewModels, and saved state
 * Each rendered entry receives its own [NavBackStackEntryOwner] providing:
 * - **[androidx.lifecycle.LifecycleOwner]** — the active (top) entry is RESUMED; entries
 *   below it are STARTED. Popped entries are moved to DESTROYED.
 * - **[androidx.lifecycle.ViewModelStoreOwner]** — `viewModel()` returns an instance scoped
 *   to the entry. The store is held by a host-Activity-scoped [NavixEntryViewModelStores],
 *   so entry ViewModels **survive configuration changes** and are cleared (→ `onCleared()`)
 *   only on a real pop.
 * - **[androidx.savedstate.SavedStateRegistryOwner]** — backing store for `SavedStateHandle`.
 *   When created via [rememberSaveableNavigator], each entry's `SavedStateHandle` and the
 *   screen's `rememberSaveable` state survive **process death** (entry-keyed
 *   `rememberSaveableStateHolder` + the combined blob).
 *
 * Popped entries are evicted exhaustively (including entries removed deep in the stack by
 * `popTo`/`reset` that were never composed), bounding the persisted blob to live depth.
 *
 * ### Predictive back
 * On Android 14+ [PredictiveBackHandler] intercepts the back gesture for screen-to-screen
 * navigation and drives a real-time [NavTransitionSpec.predictiveExit] transform. When the
 * active entry is a dialog or bottom sheet the system's built-in dismiss gesture is used
 * instead — [PredictiveBackHandler] is disabled in that case.
 *
 * ### Routing graph is immutable after first composition
 * The [content] DSL lambda is evaluated **exactly once** when [NavixHost] first enters
 * the composition. Subsequent recompositions do not re-run the lambda.
 *
 * @param navigator The [Navigator] that owns the backstack state.
 * @param modifier Applied to the outer container.
 * @param transitionSpec Provides enter/exit transitions per [io.navix.contracts.NavTransitionKey].
 * @param content DSL block for registering screen, dialog, and bottom-sheet composables.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavixHost(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    transitionSpec: NavTransitionSpec = NavTransitionSpec.Default,
    content: NavGraphBuilder.() -> Unit
) {
    val graphBuilder = remember { NavGraphBuilderImpl().apply(content) }
    val hostLifecycleOwner = LocalLifecycleOwner.current

    // Host-Activity-scoped ViewModel stores (survive config change) and, when the
    // navigator came from rememberSaveableNavigator, the per-entry saved-state holder.
    val ownerStore = rememberNavixOwnerStore()
    val holder = remember(navigator) { NavixHolderRegistry.holderFor(navigator) }

    // Per-entry screen-level rememberSaveable state, keyed by entry id. Itself
    // host-rememberSaveable, so screen UI state survives config change AND process death.
    val saveableStateHolder = rememberSaveableStateHolder()

    // Keyed by entry.id — owns lifecycle + SavedState; ViewModelStore is injected.
    val ownerMap = remember { HashMap<String, NavBackStackEntryOwner>() }

    fun ownerFor(id: String): NavBackStackEntryOwner =
        ownerMap.getOrPut(id) {
            NavBackStackEntryOwner(
                viewModelStore = ownerStore.storeFor(id),
                restoredBundle = holder?.consumeRestoredState(id)
            ).also { holder?.registerOwner(id, it) }
        }

    fun evict(id: String) {
        ownerMap.remove(id)?.destroy()
        ownerStore.evict(id)
        saveableStateHolder.removeState(id)
        holder?.evict(id)
    }

    val snapshot by navigator.backstack.collectAsState()

    // Eagerly create owners for every entry currently in the snapshot (so entries deep
    // in the stack have a ViewModelStore/SavedStateRegistry even when not composed).
    for (entry in snapshot.entries) {
        ownerFor(entry.id)
    }

    // After every successful composition, sync lifecycle states.
    val hostLifecycle = hostLifecycleOwner.lifecycle
    SideEffect {
        val hostState = hostLifecycle.currentState
        snapshot.entries.forEachIndexed { index, entry ->
            ownerMap[entry.id]?.moveTo(
                targetState =
                    if (index == snapshot.entries.lastIndex) {
                        Lifecycle.State.RESUMED
                    } else {
                        Lifecycle.State.STARTED
                    },
                hostState = hostState
            )
        }
    }

    // Propagate host lifecycle events to all active entry owners.
    DisposableEffect(hostLifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, _ ->
                val hostState = hostLifecycle.currentState
                snapshot.entries.forEachIndexed { index, entry ->
                    ownerMap[entry.id]?.moveTo(
                        targetState =
                            if (index == snapshot.entries.lastIndex) {
                                Lifecycle.State.RESUMED
                            } else {
                                Lifecycle.State.STARTED
                            },
                        hostState = hostState
                    )
                }
            }
        hostLifecycle.addObserver(observer)
        onDispose {
            hostLifecycle.removeObserver(observer)
            // Host leaving composition (incl. config change): drive owner lifecycles to
            // DESTROYED but do NOT clear ViewModel stores or evict saved state — those are
            // retained by the Activity-scoped NavixEntryViewModelStores so ViewModels
            // survive recreation. A real pop clears them via the eviction effect below.
            ownerMap.values.forEach { it.destroy() }
            ownerMap.clear()
        }
    }

    // ── Exhaustive eviction ─────────────────────────────────────────────────
    //
    // Drops persisted state for any entry removed from the backstack. The entry that was
    // active before the change is the one AnimatedContent / the overlay animates out — its
    // owner, ViewModel store, and saveable slot are torn down by the per-composable
    // onDispose below once the exit animation finishes (so ViewModels aren't cleared
    // mid-animation). Entries removed deeper in the stack (popTo/reset) were never composed
    // and have no onDispose, so they are torn down here. All eviction is idempotent.

    LaunchedEffect(navigator) {
        var prev = navigator.backstack.value
        navigator.backstack.collect { cur ->
            val curIds = cur.entries.mapTo(HashSet(cur.entries.size)) { it.id }
            val prevActiveId = prev.active?.id
            for (e in prev.entries) {
                if (e.id in curIds) continue
                if (e.id == prevActiveId) {
                    // The active entry's composable cleans up via onDispose after its exit
                    // animation, so only evict the holder eagerly to prevent stale restoration.
                    holder?.evict(e.id)
                } else {
                    evict(e.id)
                }
            }
            prev = cur
        }
    }

    // ── Determine screen entry vs overlay entry ─────────────────────────────
    //
    // The topmost Screen entry drives AnimatedContent. If the active entry is a
    // Dialog or BottomSheet, the screen BELOW it stays mounted and the overlay
    // is rendered in its own composable container above.

    val activeEntry = snapshot.active

    // Resolve the kind of the active entry (default to Screen for unregistered routes —
    // those produce a descriptive error inside the AnimatedContent content block).
    val activeKind =
        activeEntry?.let {
            graphBuilder.destinationKinds[it.route::class] ?: DestinationKind.Screen
        }

    val isOverlayActive =
        activeKind == DestinationKind.Dialog ||
            activeKind == DestinationKind.BottomSheet

    // Topmost Screen entry — the one AnimatedContent should show.
    // When the active entry is an overlay, this is the entry immediately below it.
    val screenEntry: RouteEntry? =
        if (isOverlayActive) {
            // Walk the stack from the top, skipping overlay entries, to find the topmost Screen.
            snapshot.entries.lastOrNull { entry ->
                val kind = graphBuilder.destinationKinds[entry.route::class] ?: DestinationKind.Screen
                kind == DestinationKind.Screen
            }
        } else {
            activeEntry
        }

    // ── Predictive back (screen-to-screen only) ─────────────────────────────
    // Disabled when an overlay is active — dialogs and bottom sheets use their own
    // system-back / swipe-to-dismiss mechanism which calls navigator.pop() via
    // onDismissRequest.

    val predictiveProgress = remember { Animatable(0f) }
    var swipeEdge by remember { mutableIntStateOf(NavTransitionSpec.SWIPE_EDGE_LEFT) }

    NavixPredictiveBackHandler(
        enabled = snapshot.canPop && !isOverlayActive,
        navigator = navigator,
        predictiveProgress = predictiveProgress,
        onSwipeEdgeChange = { swipeEdge = it }
    )

    // ── Screen layer (AnimatedContent) ──────────────────────────────────────

    Box(modifier = modifier) {
        if (screenEntry != null) {
            AnimatedContent(
                targetState = screenEntry,
                transitionSpec = {
                    val key = targetState.transitionKey
                    val enter =
                        transitionSpec.enterTransition(from = initialState, to = targetState, key = key)
                    val exit =
                        transitionSpec.exitTransition(from = initialState, to = targetState, key = key)
                    enter togetherWith exit
                },
                contentKey = { entry -> entry.id },
                label = "NavixHost"
            ) { entry ->
                val owner = ownerFor(entry.id)

                DisposableEffect(entry.id) {
                    onDispose {
                        if (navigator.backstack.value.entries
                                .none { it.id == entry.id }
                        ) {
                            evict(entry.id)
                        }
                    }
                }

                val gestureProgress = predictiveProgress.value
                val gestureModifier =
                    if (gestureProgress > 0f && entry.id == screenEntry.id) {
                        val previousEntry = snapshot.entries.getOrNull(snapshot.entries.size - 2)
                        transitionSpec.predictiveExit(
                            from = entry,
                            to = previousEntry,
                            key = entry.transitionKey,
                            progress = gestureProgress,
                            swipeEdge = swipeEdge
                        )
                    } else {
                        Modifier
                    }

                Box(modifier = gestureModifier) {
                    NavEntry(
                        owner = owner,
                        navigator = navigator,
                        entry = entry,
                        saveableStateHolder = saveableStateHolder
                    ) {
                        val screenContent =
                            graphBuilder.destinations[entry.route::class]
                                ?: error(
                                    "No screen registered for route ${entry.route::class.simpleName}. " +
                                        "Add a screen<${entry.route::class.simpleName}> { } block inside NavixHost."
                                )
                        screenContent(entry, entry.route)
                    }
                }
            }
        }

        // ── Overlay layer (Dialog / BottomSheet) ────────────────────────────
        NavixOverlayContent(
            activeEntry = activeEntry,
            activeKind = activeKind,
            navigator = navigator,
            graphBuilder = graphBuilder,
            ownerFor = ::ownerFor,
            evict = ::evict,
            saveableStateHolder = saveableStateHolder
        )
    }
}

@Composable
private fun NavixPredictiveBackHandler(
    enabled: Boolean,
    navigator: Navigator,
    predictiveProgress: Animatable<Float, androidx.compose.animation.core.AnimationVector1D>,
    onSwipeEdgeChange: (Int) -> Unit
) {
    PredictiveBackHandler(enabled = enabled) { backEvents: kotlinx.coroutines.flow.Flow<BackEventCompat> ->
        try {
            backEvents.collect { event ->
                predictiveProgress.snapTo(event.progress)
                onSwipeEdgeChange(event.swipeEdge)
            }
            // Flow completed normally → gesture committed.
            predictiveProgress.snapTo(0f)
            navigator.pop()
        } catch (e: kotlinx.coroutines.CancellationException) {
            predictiveProgress.animateTo(
                targetValue = 0f,
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            )
            throw e
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NavixOverlayContent(
    activeEntry: RouteEntry?,
    activeKind: DestinationKind?,
    navigator: Navigator,
    graphBuilder: NavGraphBuilderImpl,
    ownerFor: (String) -> NavBackStackEntryOwner,
    evict: (String) -> Unit,
    saveableStateHolder: SaveableStateHolder
) {
    val isOverlayActive = activeKind == DestinationKind.Dialog || activeKind == DestinationKind.BottomSheet
    if (!isOverlayActive) return

    // isOverlayActive is only true when activeEntry != null (activeKind is derived from activeEntry).
    val overlayEntry =
        requireNotNull(activeEntry) {
            "NavixHost: isOverlayActive is true but activeEntry is null — this is a bug in NavixHost lifecycle tracking"
        }
    val overlayOwner = ownerFor(overlayEntry.id)

    DisposableEffect(overlayEntry.id) {
        onDispose {
            if (navigator.backstack.value.entries
                    .none { it.id == overlayEntry.id }
            ) {
                evict(overlayEntry.id)
            }
        }
    }

    when (activeKind) {
        DestinationKind.Dialog -> {
            Dialog(onDismissRequest = { navigator.pop() }) {
                NavEntry(
                    owner = overlayOwner,
                    navigator = navigator,
                    entry = overlayEntry,
                    saveableStateHolder = saveableStateHolder
                ) {
                    val dialogContent =
                        graphBuilder.destinations[overlayEntry.route::class]
                            ?: error(
                                "No dialog registered for route ${overlayEntry.route::class.simpleName}. " +
                                    "Add a dialog<${overlayEntry.route::class.simpleName}> { } block inside NavixHost."
                            )
                    dialogContent(overlayEntry, overlayEntry.route)
                }
            }
        }

        DestinationKind.BottomSheet -> {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { navigator.pop() },
                sheetState = sheetState
            ) {
                NavEntry(
                    owner = overlayOwner,
                    navigator = navigator,
                    entry = overlayEntry,
                    saveableStateHolder = saveableStateHolder
                ) {
                    val routeName = overlayEntry.route::class.simpleName
                    val sheetContent =
                        graphBuilder.destinations[overlayEntry.route::class]
                            ?: error(
                                "No bottomSheet registered for route $routeName. " +
                                    "Add a bottomSheet<$routeName> { } block inside NavixHost."
                            )
                    sheetContent(overlayEntry, overlayEntry.route)
                }
            }
        }

        else -> {
            // Already handled by AnimatedContent above; unreachable here.
        }
    }
}
