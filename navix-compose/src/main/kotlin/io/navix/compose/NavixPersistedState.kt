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

/**
 * The single combined saved blob persisted by [rememberSaveableNavigator] /
 * [rememberSaveableNavixMultiStack].
 *
 * One blob (not two parallel slots) guarantees the backstack and its per-entry
 * `SavedStateRegistry` bundles are written by the same save pass and therefore reference
 * the same `RouteEntry.id` set — there is no window where a restored backstack points at
 * entry slots that failed to deserialise independently.
 *
 * It is a [Bundle] rather than JSON because per-entry payloads (`SavedStateHandle`
 * values, parcelables) are arbitrary and must not be forced through
 * `kotlinx.serialization`. The backstack itself is still serialised by the caller-supplied
 * [io.navix.runtime.NavigatorSaver] to a `ByteArray` and carried as one key.
 */
internal object NavixPersistedState {
    private const val KEY_BACKSTACK = "navix:backstack"
    private const val KEY_ENTRY_STATES = "navix:entryStates"
    private const val KEY_TAB_INDEX = "navix:activeTab"
    private const val KEY_TAB_BACKSTACKS = "navix:tabStacks"

    /** Single-navigator blob: backstack bytes + per-entry registry bundles. */
    fun pack(
        backstackBytes: ByteArray,
        entryStates: Bundle,
    ): Bundle =
        Bundle().apply {
            putByteArray(KEY_BACKSTACK, backstackBytes)
            putBundle(KEY_ENTRY_STATES, entryStates)
        }

    fun unpackBackstack(blob: Bundle): ByteArray? = blob.getByteArray(KEY_BACKSTACK)

    fun unpackEntryStates(blob: Bundle): Bundle = blob.getBundle(KEY_ENTRY_STATES) ?: Bundle()

    /**
     * Multi-stack blob: active tab index, per-tab backstack bytes keyed by
     * `NavStackSpec.key`, plus the shared per-entry registry bundles (entry ids are
     * process-unique across all tabs).
     */
    fun packMultiStack(
        activeTabIndex: Int,
        tabBackstacks: Bundle,
        entryStates: Bundle,
    ): Bundle =
        Bundle().apply {
            putInt(KEY_TAB_INDEX, activeTabIndex)
            putBundle(KEY_TAB_BACKSTACKS, tabBackstacks)
            putBundle(KEY_ENTRY_STATES, entryStates)
        }

    fun unpackTabIndex(
        blob: Bundle,
        fallback: Int,
    ): Int = blob.getInt(KEY_TAB_INDEX, fallback)

    fun unpackTabBackstacks(blob: Bundle): Bundle = blob.getBundle(KEY_TAB_BACKSTACKS) ?: Bundle()
}
