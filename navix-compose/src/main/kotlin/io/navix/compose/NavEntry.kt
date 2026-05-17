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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import io.navix.contracts.RouteEntry
import io.navix.runtime.Navigator

/**
 * Wraps a single navigation destination's composable content and provides the
 * per-entry [NavBackStackEntryOwner] through four CompositionLocals:
 *
 * - [LocalLifecycleOwner] — entry-scoped lifecycle for `repeatOnLifecycle`, `collectAsStateWithLifecycle`, etc.
 * - [LocalViewModelStoreOwner] — entry-scoped store; `viewModel()` calls return an instance
 *   tied to this entry (and a restorable `SavedStateHandle`, since the owner exposes a
 *   saved-state-aware default factory). Survives configuration changes; cleared on real pop.
 * - [LocalNavEntry] — the [RouteEntry] for the currently rendered screen.
 * - [LocalNavigator] — the [Navigator] that owns this screen's backstack.
 *
 * ### Screen `rememberSaveable`
 * Content is wrapped in [saveableStateHolder]`.SaveableStateProvider(entry.id)` so a
 * screen's `rememberSaveable { }` state is scoped to this entry and survives both
 * configuration change and process death (the holder is itself host-`rememberSaveable`).
 * The slot is dropped when the entry is popped (see [NavixHost] eviction).
 *
 * Note: `LocalSavedStateRegistryOwner` is intentionally not overridden here — the
 * [NavBackStackEntryOwner] is reachable via `(LocalLifecycleOwner.current as
 * SavedStateRegistryOwner)`, and `viewModel()` resolves the owner's saved-state default
 * factory directly. Providing `LocalSavedStateRegistryOwner` requires
 * `savedstate-compose:1.3.0+` and is unnecessary for `SavedStateHandle` to work.
 *
 * [NavixHost] renders each destination inside a [NavEntry], so screen composables can
 * access all of these without any boilerplate.
 */
@Composable
internal fun NavEntry(
    owner: NavBackStackEntryOwner,
    navigator: Navigator,
    entry: RouteEntry,
    saveableStateHolder: SaveableStateHolder,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalLifecycleOwner provides owner,
        LocalViewModelStoreOwner provides owner,
        LocalNavEntry provides entry,
        LocalNavigator provides navigator
    ) {
        saveableStateHolder.SaveableStateProvider(entry.id) {
            content()
        }
    }
}
