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

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/**
 * The active tab index, every tab's backstack, and inactive tabs all survive a
 * process-death-equivalent state restoration via [rememberSaveableNavixMultiStack].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NavixMultiStackProcessDeathTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tabIndex_and_perTabBackstacks_surviveProcessDeath() {
        val tester = StateRestorationTester(composeRule)
        lateinit var ms: NavixMultiStack
        tester.setContent {
            ms =
                rememberSaveableNavixMultiStack(
                    specs =
                        listOf(
                            NavStackSpec(TabHomeR, key = "home"),
                            NavStackSpec(TabSearchR, key = "search")
                        ),
                    saver = TestSaver
                )
            NavixMultiStackHost(ms) {
                screen<TabHomeR> { _, _ -> Text("home") }
                screen<TabSearchR> { _, _ -> Text("search") }
                screen<TabDetailR> { _, _ -> Text("detail") }
            }
        }

        composeRule.runOnIdle { ms.selectTab(1) }
        composeRule.waitForIdle()
        composeRule.runOnIdle { ms.navigators[1].push(TabDetailR) }
        composeRule.waitForIdle()
        assertEquals(1, ms.activeTabIndex.value)
        assertEquals(2, ms.navigators[1].backstack.value.depth)

        tester.emulateSavedInstanceStateRestore()

        assertEquals(1, ms.activeTabIndex.value)
        assertEquals(2, ms.navigators[1].backstack.value.depth)
        assertEquals(1, ms.navigators[0].backstack.value.depth)
    }
}
