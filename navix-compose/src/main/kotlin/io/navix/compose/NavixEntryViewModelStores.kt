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

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Holds one [ViewModelStore] per backstack entry, keyed by `RouteEntry.id`.
 *
 * This is itself a [ViewModel] scoped to the **host** [androidx.lifecycle.ViewModelStoreOwner]
 * (the Activity, via [rememberNavixOwnerStore]). Because the host store survives
 * configuration changes, every per-entry [ViewModelStore] handed out by [storeFor] also
 * survives configuration changes — so a screen's `viewModel()` returns the *same* instance
 * across rotation. The store is cleared only when its entry is genuinely popped
 * ([evict]) or when the host is destroyed ([onCleared]), preserving correct
 * `ViewModel.onCleared()` timing.
 *
 * Entry ids are process-unique (see `DefaultEntryFactory`), so a single shared instance
 * is safe even when multiple [NavixHost]s (e.g. multi-stack tabs) run under one Activity.
 */
internal class NavixEntryViewModelStores : ViewModel() {
    private val stores = mutableMapOf<String, ViewModelStore>()

    fun storeFor(id: String): ViewModelStore = stores.getOrPut(id) { ViewModelStore() }

    /** Clears and drops the store for [id]. Call only on a real pop (fires `onCleared`). */
    fun evict(id: String) {
        stores.remove(id)?.clear()
    }

    override fun onCleared() {
        stores.values.forEach { it.clear() }
        stores.clear()
    }
}

/**
 * Returns the host-scoped [NavixEntryViewModelStores], creating it in the nearest
 * [androidx.lifecycle.ViewModelStoreOwner] (normally the Activity, so it survives
 * configuration changes).
 *
 * If no host owner is present (e.g. a `@Preview` or a bare test harness), this degrades
 * to a per-composition instance — entry ViewModels then do **not** survive configuration
 * change, but nothing crashes.
 */
@Composable
internal fun rememberNavixOwnerStore(): NavixEntryViewModelStores {
    val hostOwner = LocalViewModelStoreOwner.current
    return if (hostOwner != null) {
        viewModel(viewModelStoreOwner = hostOwner)
    } else {
        Log.w(
            "Navix",
            "NavixHost is not hosted under a ViewModelStoreOwner; entry-scoped ViewModels " +
                "will not survive configuration changes. Host NavixHost inside a ComponentActivity.",
        )
        remember { NavixEntryViewModelStores() }
    }
}
