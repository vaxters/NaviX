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
import io.navix.contracts.Route
import io.navix.runtime.DeepLinkHandler
import io.navix.runtime.DefaultEntryFactory
import io.navix.runtime.DefaultReducer
import io.navix.runtime.EntryFactory
import io.navix.runtime.Navigator
import io.navix.runtime.NavigatorSaver
import io.navix.runtime.Reducer
import io.navix.runtime.createNavigator
import io.navix.runtime.restoreNavigator

/**
 * Creates and remembers a [Navigator] whose backstack **and per-entry state** survive
 * Activity recreation and process death.
 *
 * On first composition the navigator is initialised with a single entry at [root].
 * On recreation (configuration change, process death, etc.) the navigator is restored
 * from the combined blob that Compose's saved-state mechanism persisted on the previous
 * run.
 *
 * ### What survives
 * - **Backstack** — every [io.navix.contracts.RouteEntry] (route arguments), serialised by
 *   [saver].
 * - **`SavedStateHandle`** — each entry's `ViewModel` `SavedStateHandle`, via its per-entry
 *   `SavedStateRegistry` (see [NavBackStackEntryOwner]).
 * - **Screen `rememberSaveable`** — handled by [NavixHost]'s per-entry
 *   `rememberSaveableStateHolder`.
 *
 * The backstack bytes and the per-entry registry bundles are written as **one** blob
 * (see [NavixPersistedState]) so they always reference the same entry-id set.
 *
 * ### Serialization
 * [saver] converts the [io.navix.contracts.BackstackSnapshot] to/from bytes. Use
 * [io.navix.runtime.JsonNavigatorSaver] with the KSP-generated
 * `${Module}NavixSerializersModule` as the routes module:
 *
 * ```kotlin
 * val nav = rememberSaveableNavigator(
 *     root = Home,
 *     saver = JsonNavigatorSaver(AppNavixSerializersModule),
 * )
 * NavixHost(navigator = nav) { ... }
 * ```
 *
 * ### Process-death guarantee
 * The combined [Bundle] is natively supported by Android's instance state, so it survives
 * both configuration changes and process death. A blob written by an older Navix version
 * (raw `ByteArray` format) is not readable here; restore degrades gracefully to a fresh
 * navigator at [root] — the same fallback as a schema mismatch.
 *
 * @param root Initial root destination (used on first launch and as fallback on restore failure).
 * @param saver Converts the backstack snapshot to/from bytes.
 * @param reducer Backstack reducer. Defaults to [DefaultReducer].
 * @param entryFactory Controls entry identity (ID + timestamp). Defaults to [DefaultEntryFactory].
 * @param telemetry Receives every [io.navix.contracts.NavEvent]. Defaults to no-op.
 * @param deepLinkHandlers Ordered list of URI handlers. Defaults to empty.
 */
@Composable
fun rememberSaveableNavigator(
    root: Route,
    saver: NavigatorSaver,
    reducer: Reducer = DefaultReducer(),
    entryFactory: EntryFactory = DefaultEntryFactory,
    telemetry: NavixTelemetry = NavixTelemetry.NoOp,
    deepLinkHandlers: List<DeepLinkHandler> = emptyList(),
): Navigator {
    val scope = rememberCoroutineScope()
    val savedStateHolder = remember { NavixSavedStateHolder() }

    val navigator =
        rememberSaveable(
            saver =
                Saver<Navigator, Any>(
                    save = { nav ->
                        NavixPersistedState.pack(
                            backstackBytes = saver.save(nav.backstack.value),
                            entryStates = savedStateHolder.performSave(),
                        )
                    },
                    restore = { saved ->
                        val blob = saved as? Bundle
                        val bytes = blob?.let { NavixPersistedState.unpackBackstack(it) }
                        if (blob == null || bytes == null) {
                            createNavigator(root, scope, reducer, entryFactory, telemetry, deepLinkHandlers)
                        } else {
                            savedStateHolder.restoreFrom(NavixPersistedState.unpackEntryStates(blob))
                            restoreNavigator(
                                root = root,
                                scope = scope,
                                savedBytes = bytes,
                                saver = saver,
                                reducer = reducer,
                                telemetry = telemetry,
                                deepLinkHandlers = deepLinkHandlers,
                            )
                        }
                    },
                ),
        ) {
            createNavigator(
                root = root,
                scope = scope,
                reducer = reducer,
                entryFactory = entryFactory,
                telemetry = telemetry,
                deepLinkHandlers = deepLinkHandlers,
            )
        }

    // Associate the holder with this navigator so NavixHost can wire per-entry
    // persistence. Idempotent; runs before NavixHost composes.
    remember(navigator) {
        NavixHolderRegistry.put(navigator, savedStateHolder)
        navigator
    }
    return navigator
}
