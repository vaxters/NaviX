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
package io.navix.demo

import app.cash.turbine.test
import io.navix.demo.data.repository.FakeProductRepository
import io.navix.demo.domain.usecase.GetProductsUseCase
import io.navix.demo.routes.Home
import io.navix.demo.routes.ProductDetail
import io.navix.demo.ui.home.HomeNavEffect
import io.navix.demo.ui.home.HomeViewModel
import io.navix.testing.FakeNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Demonstrates using [FakeNavigator] and Turbine to test ViewModel navigation logic.
 *
 * The ViewModel emits nav effects via a Channel; the Screen collects those effects
 * and calls the corresponding Navigator method. Tests verify the effect, not the
 * Navigator call — keeping the ViewModel decoupled from the navigation layer.
 */
class HomeViewModelTest {
    // HomeViewModel uses viewModelScope which dispatches on Main. In JVM unit tests
    // there is no Main dispatcher, so we install a test dispatcher before each test
    // and restore the real dispatcher afterwards.
    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onProductClicked_productId_emitsOpenProductDetailEffectWithCorrectId() =
        runTest {
            val vm = HomeViewModel(GetProductsUseCase(FakeProductRepository()))

            vm.navEffect.test {
                vm.onProductClicked("42")
                val effect = awaitItem()
                assertTrue(effect is HomeNavEffect.OpenProductDetail)
                assertEquals("42", (effect as HomeNavEffect.OpenProductDetail).productId)
            }
        }

    @Test
    fun push_newRoute_recordsPushAndAssertsPasses() {
        val nav = FakeNavigator(root = Home)

        nav.push(ProductDetail("99"))

        nav.assertCurrentRoute(ProductDetail("99"))
        nav.assertBackstackSize(2)
        nav.assertLastPushed(ProductDetail("99"))
        nav.assertCanPop(true)
        nav.assertPushCount(1)
    }

    @Test
    fun reset_afterMultiplePushes_clearsToNewRoot() {
        val nav = FakeNavigator(root = Home)

        nav.push(ProductDetail("1"))
        nav.push(ProductDetail("2"))
        nav.assertBackstackSize(3)

        nav.reset(Home)

        nav.assertBackstackSize(1)
        nav.assertCurrentRoute(Home)
        nav.assertCanPop(false)
    }

    @Test
    fun popTo_inclusive_removesTopMatchAndLeavesDeeper() {
        val nav = FakeNavigator(root = Home)

        nav.push(ProductDetail("1"))
        nav.push(ProductDetail("2"))
        nav.assertBackstackSize(3)

        // popTo finds the most-recent (topmost) ProductDetail — PD("2") at index 2.
        // inclusive = true: PD("2") is itself removed. PD("1") remains.
        nav.popTo(ProductDetail::class, inclusive = true)

        nav.assertBackstackSize(2)
        nav.assertCurrentRoute(ProductDetail("1"))
    }
}
