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
import io.navix.contracts.NavixTelemetry
import io.navix.contracts.Route
import kotlinx.coroutines.CoroutineScope

/**
 * Creates a fully wired [Navigator] backed by [NavigatorImpl].
 *
 * This factory is the public surface that allows `navix-compose` (and other modules)
 * to instantiate navigators without depending on the `internal` [NavigatorImpl] class.
 * All wiring — store, actor channel, telemetry forwarding — happens inside [NavigatorImpl].
 *
 * @param root Initial destination placed at the bottom of the backstack.
 * @param scope Coroutine scope for the actor that serialises navigation actions.
 * @param reducer Backstack reducer. Defaults to [DefaultReducer].
 * @param entryFactory Controls entry identity (ID + timestamp). Defaults to [DefaultEntryFactory].
 * @param telemetry Receives every [io.navix.contracts.NavEvent]. Defaults to no-op.
 * @param deepLinkHandlers Ordered list of URI handlers. Defaults to empty.
 */
fun createNavigator(
    root: Route,
    scope: CoroutineScope,
    reducer: Reducer = DefaultReducer(),
    entryFactory: EntryFactory = DefaultEntryFactory,
    telemetry: NavixTelemetry = NavixTelemetry.NoOp,
    deepLinkHandlers: List<DeepLinkHandler> = emptyList(),
): Navigator =
    NavigatorImpl(
        root = root,
        scope = scope,
        reducer = reducer,
        entryFactory = entryFactory,
        telemetry = telemetry,
        deepLinkHandlers = deepLinkHandlers,
    )

/**
 * Restores a [Navigator] from a previously serialised [BackstackSnapshot].
 *
 * This is the process-death counterpart of [createNavigator]. The [saver] deserialises the
 * [savedBytes] back into a [BackstackSnapshot]; if deserialization fails (schema mismatch,
 * corrupt bytes, etc.) the function falls back to a fresh navigator rooted at [root].
 *
 * Callers in `navix-compose` use this via [rememberSaveableNavigator], which manages the
 * save/restore lifecycle automatically. Direct use is for non-Compose contexts (e.g. iOS
 * SwiftUI wrappers, JVM tests exercising serialization round-trips).
 *
 * @param root Fallback root route used when [saver.restore] returns `null`.
 * @param scope Coroutine scope for the actor that serialises navigation actions.
 * @param savedBytes Bytes produced by a prior [NavigatorSaver.save] call.
 * @param saver The saver that produced [savedBytes] — must use a compatible schema.
 * @param reducer Backstack reducer. Defaults to [DefaultReducer].
 * @param telemetry Receives every [io.navix.contracts.NavEvent]. Defaults to no-op.
 * @param deepLinkHandlers Ordered list of URI handlers. Defaults to empty.
 */
fun restoreNavigator(
    root: Route,
    scope: CoroutineScope,
    savedBytes: ByteArray,
    saver: NavigatorSaver,
    reducer: Reducer = DefaultReducer(),
    telemetry: NavixTelemetry = NavixTelemetry.NoOp,
    deepLinkHandlers: List<DeepLinkHandler> = emptyList(),
): Navigator {
    val restoredSnapshot: BackstackSnapshot? = saver.restore(savedBytes)
    return NavigatorImpl(
        root = root,
        scope = scope,
        reducer = reducer,
        entryFactory = DefaultEntryFactory,
        telemetry = telemetry,
        deepLinkHandlers = deepLinkHandlers,
        initialSnapshot = restoredSnapshot,
    )
}
