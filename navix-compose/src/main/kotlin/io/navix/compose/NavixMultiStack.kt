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

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import io.navix.contracts.NavixTelemetry
import io.navix.runtime.DeepLinkHandler
import io.navix.runtime.DefaultReducer
import io.navix.runtime.Navigator
import io.navix.runtime.NavigatorSaver
import io.navix.runtime.Reducer
import io.navix.runtime.createNavigator
import io.navix.runtime.restoreNavigator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * State holder for tab-based multi-stack navigation.
 *
 * Each tab owns an independent [Navigator] with its own backstack. Switching tabs
 * preserves each tab's navigation state — the backstack, current route, and any queued
 * actions are retained for the lifetime of the composition. Composable-local state
 * (scroll position, transient text input) is not preserved across tab switches unless
 * the caller wraps tab content with [androidx.compose.runtime.saveable.SaveableStateHolder].
 *
 * Create via [rememberNavixMultiStack].
 *
 * ### Back behavior
 * [NavixMultiStackHost] does not install its own [androidx.activity.compose.BackHandler];
 * the inner [NavixHost] handles back by popping the active tab's stack. When the active
 * tab is at its root (single entry), back falls through to the parent BackHandler —
 * typically finishing the Activity or navigating up in an outer stack.
 *
 * To implement "return to first tab on back" semantics, install a [BackHandler] in the
 * call site that checks [activeTabIndex] and calls [selectTab] before yielding:
 * ```kotlin
 * val activeTabIndex by multiStack.activeTabIndex.collectAsState()
 * BackHandler(enabled = activeTabIndex != 0) { multiStack.selectTab(0) }
 * ```
 */
class NavixMultiStack internal constructor(
    /** Ordered list of tab specifications, one per tab. */
    val specs: List<NavStackSpec>,
    /** One [Navigator] per entry in [specs], in the same order. */
    val navigators: List<Navigator>,
    initialTabIndex: Int,
) {
    private val _activeTabIndex = MutableStateFlow(initialTabIndex)

    /** Index of the currently visible tab. Collect this to react to tab changes. */
    val activeTabIndex: StateFlow<Int> = _activeTabIndex.asStateFlow()

    /** The [Navigator] owned by the currently visible tab. */
    val activeNavigator: Navigator
        get() = navigators[_activeTabIndex.value]

    /** Number of tabs in this multi-stack. */
    val tabCount: Int get() = specs.size

    /**
     * Switch to the tab at [index].
     *
     * Switching to the already-active tab is a no-op — the active tab's backstack is not
     * reset. To reset on re-select, call [Navigator.reset] on the tab's navigator directly.
     *
     * @throws IllegalArgumentException if [index] is out of range.
     */
    fun selectTab(index: Int) {
        require(index in specs.indices) {
            "Tab index $index is out of range [0, ${specs.size - 1}]."
        }
        _activeTabIndex.value = index
    }

    /**
     * Pop the top entry from the active tab's backstack.
     *
     * Delegates to [activeNavigator]`.pop()`. If the active tab has only one entry, the
     * reducer treats this as a no-op — check [canPop] before calling if branching is needed.
     */
    fun pop() {
        activeNavigator.pop()
    }

    /**
     * `true` when the active tab's backstack has more than one entry.
     *
     * This is a snapshot read. For reactive updates, collect
     * `multiStack.activeNavigator.backstack`.
     */
    val canPop: Boolean
        get() = activeNavigator.backstack.value.canPop
}

/**
 * Creates and remembers a [NavixMultiStack] for the composition lifetime.
 *
 * One [Navigator] is created per entry in [specs]. All navigators are created once and
 * survive recompositions and tab switches — the backstack of each tab is retained as
 * long as the composition is alive.
 *
 * [specs] is captured on first composition; later changes are ignored (same contract as
 * [rememberNavigator]).
 *
 * @param specs Ordered list of tab specifications. Must be non-empty.
 * @param initialTabIndex Index of the tab visible on first composition. Defaults to 0.
 * @param deepLinkHandlers Deep link handlers shared across all tab navigators.
 * @param telemetry Telemetry sink receiving [io.navix.contracts.NavEvent]s from all tabs.
 * @param reducer Backstack reducer applied to all tab navigators.
 */
@Composable
fun rememberNavixMultiStack(
    specs: List<NavStackSpec>,
    initialTabIndex: Int = 0,
    deepLinkHandlers: List<DeepLinkHandler> = emptyList(),
    telemetry: NavixTelemetry = NavixTelemetry.NoOp,
    reducer: Reducer = DefaultReducer(),
): NavixMultiStack {
    require(specs.isNotEmpty()) { "rememberNavixMultiStack requires at least one NavStackSpec." }
    val scope = rememberCoroutineScope()
    return remember {
        NavixMultiStack(
            specs = specs,
            navigators =
                specs.map { spec ->
                    createNavigator(
                        root = spec.root,
                        scope = scope,
                        reducer = reducer,
                        telemetry = telemetry,
                        deepLinkHandlers = deepLinkHandlers,
                    )
                },
            initialTabIndex = initialTabIndex,
        )
    }
}

/**
 * Creates and remembers a [NavixMultiStack] whose **active tab index, every tab's
 * backstack, and per-entry state survive** configuration changes and process death.
 *
 * The process-death counterpart of [rememberNavixMultiStack]. State is persisted as one
 * combined blob (see [NavixPersistedState]): the active tab index, each tab's backstack
 * (serialised by [saver], keyed by [NavStackSpec.key]), and every entry's
 * `SavedStateRegistry` bundle (shared across tabs — entry ids are process-unique). Screen
 * `rememberSaveable` survives via each inner [NavixHost]'s entry-keyed holder.
 *
 * ```kotlin
 * val multiStack = rememberSaveableNavixMultiStack(
 *     specs = listOf(
 *         NavStackSpec(HomeRoot, key = "home"),
 *         NavStackSpec(SearchRoot, key = "search"),
 *         NavStackSpec(ProfileRoot, key = "profile"),
 *     ),
 *     saver = JsonNavigatorSaver(AppNavixSerializersModule),
 * )
 * NavixMultiStackHost(multiStack) { navigator -> NavixHost(navigator) { ... } }
 * ```
 *
 * @param specs Ordered tab specifications. Must be non-empty with **unique, stable**
 *   [NavStackSpec.key] values (deterministic per-tab restore depends on the key, not list
 *   order). `NavStackSpec.key` defaults to the root's simple class name — set explicit
 *   keys when tabs share a root type.
 * @param saver Converts each tab's backstack snapshot to/from bytes.
 * @param initialTabIndex Tab shown on first launch and the restore fallback.
 * @param deepLinkHandlers Deep link handlers shared across all tab navigators.
 * @param telemetry Telemetry sink receiving every tab's [io.navix.contracts.NavEvent]s.
 * @param reducer Backstack reducer applied to all tab navigators.
 * @throws IllegalArgumentException if [specs] is empty or has duplicate keys.
 */
@Composable
fun rememberSaveableNavixMultiStack(
    specs: List<NavStackSpec>,
    saver: NavigatorSaver,
    initialTabIndex: Int = 0,
    deepLinkHandlers: List<DeepLinkHandler> = emptyList(),
    telemetry: NavixTelemetry = NavixTelemetry.NoOp,
    reducer: Reducer = DefaultReducer(),
): NavixMultiStack {
    require(specs.isNotEmpty()) {
        "rememberSaveableNavixMultiStack requires at least one NavStackSpec."
    }
    val keys = specs.map { it.key }
    require(keys.toSet().size == keys.size) {
        "rememberSaveableNavixMultiStack requires unique NavStackSpec.key values for " +
            "deterministic per-tab restore; got $keys. Set a distinct key on each NavStackSpec."
    }
    val scope = rememberCoroutineScope()
    val sharedHolder = remember { NavixSavedStateHolder() }

    fun build(
        tabBytes: Map<String, ByteArray>,
        startTab: Int,
    ): NavixMultiStack {
        val navigators =
            specs.map { spec ->
                val bytes = tabBytes[spec.key]
                val nav =
                    if (bytes == null) {
                        createNavigator(
                            root = spec.root,
                            scope = scope,
                            reducer = reducer,
                            telemetry = telemetry,
                            deepLinkHandlers = deepLinkHandlers,
                        )
                    } else {
                        restoreNavigator(
                            root = spec.root,
                            scope = scope,
                            savedBytes = bytes,
                            saver = saver,
                            reducer = reducer,
                            telemetry = telemetry,
                            deepLinkHandlers = deepLinkHandlers,
                        )
                    }
                NavixHolderRegistry.put(nav, sharedHolder)
                nav
            }
        return NavixMultiStack(
            specs = specs,
            navigators = navigators,
            initialTabIndex = startTab.coerceIn(0, specs.lastIndex),
        )
    }

    return rememberSaveable(
        saver =
            Saver<NavixMultiStack, Any>(
                save = { ms ->
                    val tabStacks =
                        Bundle().apply {
                            ms.specs.forEachIndexed { i, spec ->
                                putByteArray(spec.key, saver.save(ms.navigators[i].backstack.value))
                            }
                        }
                    NavixPersistedState.packMultiStack(
                        activeTabIndex = ms.activeTabIndex.value,
                        tabBackstacks = tabStacks,
                        entryStates = sharedHolder.performSave(),
                    )
                },
                restore = { saved ->
                    val blob = saved as? Bundle
                    if (blob == null) {
                        build(emptyMap(), initialTabIndex)
                    } else {
                        sharedHolder.restoreFrom(NavixPersistedState.unpackEntryStates(blob))
                        val tabStacks = NavixPersistedState.unpackTabBackstacks(blob)
                        val bytesByKey =
                            specs
                                .mapNotNull { spec ->
                                    tabStacks.getByteArray(spec.key)?.let { spec.key to it }
                                }.toMap()
                        build(bytesByKey, NavixPersistedState.unpackTabIndex(blob, initialTabIndex))
                    }
                },
            ),
    ) {
        build(emptyMap(), initialTabIndex)
    }
}
