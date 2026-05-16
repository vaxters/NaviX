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
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.enableSavedStateHandles
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner

/**
 * Provides a per-backstack-entry [LifecycleOwner], [ViewModelStoreOwner], and
 * [SavedStateRegistryOwner] so that each navigation destination gets its own:
 *
 * - **Lifecycle** — `repeatOnLifecycle(STARTED)` inside a screen suspends when the
 *   screen is below the top of the stack and resumes when it comes back into view.
 * - **ViewModelStore** — each entry owns its ViewModels independently. The store is
 *   **injected** (owned by [NavixEntryViewModelStores], which is scoped to the host
 *   Activity) so entry ViewModels survive configuration changes. The store is cleared
 *   only when the entry is actually popped (see [NavixEntryViewModelStores.evict]),
 *   not when this owner instance is recreated by a configuration change.
 * - **SavedStateRegistry** — backing store for `SavedStateHandle`. The registry is
 *   restored from [restoredBundle] (the per-entry slot persisted by
 *   [NavixSavedStateHolder] inside [rememberSaveableNavigator]'s combined saved blob),
 *   and [enableSavedStateHandles] is wired so `SavedStateViewModelFactory` can build a
 *   restorable `SavedStateHandle`. [performSaveToBundle] snapshots the registry back out
 *   at save time.
 *
 * ### Initialisation ordering (load-bearing — do not reorder)
 * `performAttach()` → `performRestore(restoredBundle)` → `enableSavedStateHandles()` →
 * `Lifecycle` to **CREATED**. `SavedStateRegistry` must be restored before the lifecycle
 * reaches CREATED, and the `SavedStateHandlesProvider` must be registered (via
 * [enableSavedStateHandles]) after restore and before any `SavedStateHandle`-backed
 * `ViewModel` is requested. Owners are created eagerly by [NavixHost] before any screen
 * composable runs, so this invariant holds by construction.
 *
 * ### Lifecycle state machine
 * The host drives lifecycle states through [moveTo]:
 * - Topmost (active) entry → capped at host's lifecycle state, up to **RESUMED**.
 * - All other in-stack entries → capped at host's lifecycle state, up to **STARTED**.
 * - Entry popped from stack → [destroy] drives to **DESTROYED**. The injected
 *   [ViewModelStore] is cleared separately by [NavixEntryViewModelStores.evict] so that
 *   `ViewModel.onCleared()` fires only on a real pop, never on config-change recreation.
 *
 * @param viewModelStore Host-scoped store for this entry's ViewModels. Survives
 *   configuration changes; cleared on real pop by [NavixEntryViewModelStores].
 * @param restoredBundle Per-entry saved-state slot from a prior process, or `null` for a
 *   fresh entry.
 */
internal class NavBackStackEntryOwner(
    viewModelStore: ViewModelStore,
    restoredBundle: Bundle? = null,
) : LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner,
    HasDefaultViewModelProviderFactory {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val _viewModelStore = viewModelStore
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = _viewModelStore
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    // A SavedStateHandle is only built for an entry's ViewModel when the owner exposes a
    // saved-state-aware default factory AND the SAVED_STATE/VIEW_MODEL_STORE owner extras.
    // enableSavedStateHandles() (in init) registers the provider these read. Mirrors
    // androidx.navigation.NavBackStackEntry.
    override val defaultViewModelProviderFactory: ViewModelProvider.Factory =
        SavedStateViewModelFactory(application = null, owner = this, defaultArgs = null)

    override val defaultViewModelCreationExtras: CreationExtras
        get() = MutableCreationExtras().apply {
            set(SAVED_STATE_REGISTRY_OWNER_KEY, this@NavBackStackEntryOwner)
            set(VIEW_MODEL_STORE_OWNER_KEY, this@NavBackStackEntryOwner)
        }

    init {
        // Ordering is load-bearing — see the class KDoc.
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(restoredBundle)
        // Registers the SavedStateHandlesProvider so SavedStateViewModelFactory can build
        // a restorable SavedStateHandle from the (possibly restored) registry. Without
        // this, plumbing the bundle alone does nothing.
        enableSavedStateHandles()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    /**
     * Snapshots this entry's [SavedStateRegistry] (including every `SavedStateHandle`)
     * into a fresh [Bundle]. Called on the main thread at save time by
     * [NavixSavedStateHolder.performSave].
     */
    fun performSaveToBundle(): Bundle = Bundle().also { savedStateRegistryController.performSave(it) }

    /**
     * Moves this entry toward [targetState], capped at [hostState].
     *
     * [LifecycleRegistry.currentState] setter generates the appropriate intermediate events
     * (e.g., ON_START, ON_RESUME) so callers don't need to track the previous state.
     */
    fun moveTo(targetState: Lifecycle.State, hostState: Lifecycle.State) {
        val capped = minOf(targetState, hostState)
        // Guard against INITIALIZED — we start at CREATED and never go back below it.
        val effective = maxOf(capped, Lifecycle.State.CREATED)
        if (lifecycleRegistry.currentState != effective) {
            lifecycleRegistry.currentState = effective
        }
    }

    /**
     * Drives the lifecycle to DESTROYED. Safe to call multiple times — subsequent calls
     * are no-ops.
     *
     * Does **not** clear the injected [ViewModelStore]: a configuration change recreates
     * this owner but must retain ViewModels. The store is cleared only on a real pop, by
     * [NavixEntryViewModelStores.evict], keeping `ViewModel.onCleared()` semantics correct.
     */
    fun destroy() {
        if (lifecycleRegistry.currentState != Lifecycle.State.DESTROYED) {
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        }
    }
}
