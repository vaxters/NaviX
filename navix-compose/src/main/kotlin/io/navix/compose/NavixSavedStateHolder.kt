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
import androidx.annotation.VisibleForTesting
import io.navix.runtime.Navigator
import java.util.WeakHashMap

/**
 * Aggregates every live entry's per-entry [android.os.SavedStateRegistry] bundle into a
 * single [Bundle] keyed by `RouteEntry.id`, and replays restored slots back into recreated
 * [NavBackStackEntryOwner]s.
 *
 * This is the bridge between [NavBackStackEntryOwner] (which owns a per-entry
 * `SavedStateRegistryController`) and [rememberSaveableNavigator] (which persists one
 * combined blob through Compose's saved-state machinery — see [NavixPersistedState]).
 *
 * ### Threading
 * Main-thread confined. Every caller — [NavixHost] owner creation, the saved-state
 * `Saver.save`/`restore` lambdas, and eviction — runs on the composition / main thread.
 * No synchronization is required and none is added.
 */
internal class NavixSavedStateHolder {

    // Slots restored from a prior process, not yet consumed by a recreated owner.
    private val restoredStates = mutableMapOf<String, Bundle>()

    // Owners currently on the backstack; each contributes its registry at save time.
    private val liveOwners = mutableMapOf<String, NavBackStackEntryOwner>()

    /**
     * Returns and removes the restored slot for [id] (applied exactly once, when the
     * owner is first recreated). `null` for a fresh entry with no prior state.
     */
    fun consumeRestoredState(id: String): Bundle? = restoredStates.remove(id)

    fun registerOwner(id: String, owner: NavBackStackEntryOwner) {
        liveOwners[id] = owner
    }

    /** Drops every trace of [id] so a popped entry never re-enters the saved blob. */
    fun evict(id: String) {
        liveOwners.remove(id)
        restoredStates.remove(id)
    }

    /**
     * Snapshots every live owner plus any restored-but-not-yet-recreated slot into one
     * [Bundle] (`id` -> per-entry registry bundle). Carrying restored slots forward keeps
     * state for an entry whose owner has not been instantiated this run.
     */
    fun performSave(): Bundle = Bundle().apply {
        for ((id, bundle) in restoredStates) putBundle(id, bundle)
        for ((id, owner) in liveOwners) putBundle(id, owner.performSaveToBundle())
    }

    /** Loads the per-entry slots from a [Bundle] produced by a prior [performSave]. */
    fun restoreFrom(root: Bundle) {
        restoredStates.clear()
        for (key in root.keySet()) {
            root.getBundle(key)?.let { restoredStates[key] = it }
        }
    }

    @VisibleForTesting
    fun debugTrackedIds(): Set<String> = restoredStates.keys + liveOwners.keys
}

/**
 * Process-wide association from a [Navigator] created by [rememberSaveableNavigator] to
 * its [NavixSavedStateHolder].
 *
 * [rememberSaveableNavigator] populates this; [NavixHost] reads it to wire per-entry
 * persistence. A [WeakHashMap] keyed by navigator identity self-prunes once the navigator
 * is collected. A plain [io.navix.compose.rememberNavigator] navigator is absent here, so
 * [NavixHost] simply falls back to in-memory (non-persisted) owners — exactly the
 * pre-existing behaviour.
 */
internal object NavixHolderRegistry {

    private val holders = WeakHashMap<Navigator, NavixSavedStateHolder>()

    fun put(navigator: Navigator, holder: NavixSavedStateHolder) {
        holders[navigator] = holder
    }

    fun holderFor(navigator: Navigator): NavixSavedStateHolder? = holders[navigator]
}
