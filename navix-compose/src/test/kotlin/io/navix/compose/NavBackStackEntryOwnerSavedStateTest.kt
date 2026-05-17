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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class CounterViewModel(
    val handle: SavedStateHandle,
) : ViewModel()

/**
 * Authoritative process-death proof for the per-entry [androidx.savedstate.SavedStateRegistry]
 * mechanism, isolated from Compose.
 *
 * Simulates process death precisely: a `SavedStateHandle`-backed ViewModel is built through
 * the owner's saved-state default factory, mutated, snapshotted via [NavBackStackEntryOwner.performSaveToBundle],
 * then a **brand-new owner with a brand-new [ViewModelStore]** (i.e. nothing retained) is
 * created from that bundle. The handle value must survive. A negative control without the
 * bundle proves the assertion can fail — guarding against a false pass.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NavBackStackEntryOwnerSavedStateTest {
    private fun handleOf(owner: NavBackStackEntryOwner): SavedStateHandle =
        ViewModelProvider(owner)[CounterViewModel::class.java].handle

    @Test
    fun savedStateHandle_survives_processDeath() {
        val owner1 = NavBackStackEntryOwner(viewModelStore = ViewModelStore())
        owner1.moveTo(Lifecycle.State.RESUMED, Lifecycle.State.RESUMED)
        handleOf(owner1)["key"] = "value"

        val saved: Bundle = owner1.performSaveToBundle()
        owner1.destroy()

        // Fresh owner, fresh store — models a real process restart (nothing retained).
        val owner2 = NavBackStackEntryOwner(viewModelStore = ViewModelStore(), restoredBundle = saved)
        owner2.moveTo(Lifecycle.State.RESUMED, Lifecycle.State.RESUMED)

        assertEquals("value", handleOf(owner2).get<String>("key"))
    }

    @Test
    fun savedStateHandle_negativeControl_doesNotSurviveWithoutBundle() {
        val owner1 = NavBackStackEntryOwner(viewModelStore = ViewModelStore())
        owner1.moveTo(Lifecycle.State.RESUMED, Lifecycle.State.RESUMED)
        handleOf(owner1)["key"] = "value"
        owner1.performSaveToBundle()
        owner1.destroy()

        val owner2 = NavBackStackEntryOwner(viewModelStore = ViewModelStore(), restoredBundle = null)
        owner2.moveTo(Lifecycle.State.RESUMED, Lifecycle.State.RESUMED)

        assertNull(handleOf(owner2).get<String>("key"))
    }

    @Test
    fun injectedViewModelStore_isReused_acrossOwnerRecreation() {
        // Config-change model: the SAME store is handed to a recreated owner, so the
        // ViewModel instance itself is retained (not just its SavedStateHandle).
        val store = ViewModelStore()
        val owner1 = NavBackStackEntryOwner(viewModelStore = store)
        owner1.moveTo(Lifecycle.State.RESUMED, Lifecycle.State.RESUMED)
        val vm1 = ViewModelProvider(owner1)[CounterViewModel::class.java]
        owner1.destroy()

        val owner2 = NavBackStackEntryOwner(viewModelStore = store)
        owner2.moveTo(Lifecycle.State.RESUMED, Lifecycle.State.RESUMED)
        val vm2 = ViewModelProvider(owner2)[CounterViewModel::class.java]

        assertEquals(vm1, vm2)
    }
}
