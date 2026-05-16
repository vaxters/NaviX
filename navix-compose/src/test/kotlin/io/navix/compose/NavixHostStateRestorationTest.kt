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

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import io.navix.runtime.Navigator
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/**
 * End-to-end Compose wiring: screen `rememberSaveable` and the backstack survive a
 * process-death-equivalent state restoration, and popped entries are evicted from the
 * combined saved blob (bounding its size).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NavixHostStateRestorationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun screenRememberSaveable_and_backstack_surviveProcessDeath() {
        val tester = StateRestorationTester(composeRule)
        lateinit var nav: Navigator
        tester.setContent {
            nav = rememberSaveableNavigator(root = HomeR, saver = TestSaver)
            NavixHost(nav) {
                screen<HomeR> { _, _ ->
                    Button(
                        onClick = { nav.push(DetailR) },
                        modifier = Modifier.testTag("toDetail"),
                    ) { Text("toDetail") }
                }
                screen<DetailR> { _, _ ->
                    var n by rememberSaveable { mutableIntStateOf(0) }
                    Column {
                        Text("n=$n", modifier = Modifier.testTag("n"))
                        Button(
                            onClick = { n++ },
                            modifier = Modifier.testTag("inc"),
                        ) { Text("inc") }
                    }
                }
            }
        }

        composeRule.onNodeWithTag("toDetail").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("inc").performClick()
        composeRule.onNodeWithTag("inc").performClick()
        composeRule.onNodeWithTag("n").assertTextEquals("n=2")

        tester.emulateSavedInstanceStateRestore()

        // Still on Detail (backstack restored) and the rememberSaveable counter survived.
        composeRule.onNodeWithTag("n").assertTextEquals("n=2")
    }

    @Test
    fun poppedEntries_areEvictedFromSavedBlob() {
        lateinit var nav: Navigator
        composeRule.setContent {
            nav = rememberSaveableNavigator(root = HomeR, saver = TestSaver)
            NavixHost(nav) {
                screen<HomeR> { _, _ -> Text("home") }
                screen<DetailR> { _, _ -> Text("detail") }
                screen<Detail2R> { _, _ -> Text("detail2") }
            }
        }

        composeRule.runOnIdle { nav.push(DetailR) }
        composeRule.waitForIdle()
        composeRule.runOnIdle { nav.push(Detail2R) }
        composeRule.waitForIdle()
        assertEquals(3, NavixHolderRegistry.holderFor(nav)!!.debugTrackedIds().size)

        composeRule.runOnIdle { nav.popTo(HomeR::class, inclusive = false) }
        composeRule.waitForIdle()

        // Only the surviving Home entry remains in the persisted blob.
        assertEquals(1, NavixHolderRegistry.holderFor(nav)!!.debugTrackedIds().size)
    }
}
