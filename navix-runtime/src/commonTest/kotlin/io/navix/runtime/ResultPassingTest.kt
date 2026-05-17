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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ResultPassingTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private fun navigator(): NavigatorImpl =
        NavigatorImpl(
            root = HomeRoute,
            scope = testScope.backgroundScope
        )

    // ── Happy path ─────────────────────────────────────────────────────────

    @Test
    fun pushForResult_calleeSetResultThenPops_returnsSuccess() =
        testScope.runTest {
            val nav = navigator()

            val resultDeferred =
                async {
                    nav.pushForResult<String>(DetailRoute("1"))
                }

            // Callee sets a result then pops
            nav.setResult("hello")
            nav.pop()

            val result = resultDeferred.await()
            assertIs<NavResult.Success<String>>(result)
            assertEquals("hello", result.value)
        }

    @Test
    fun pushForResult_calleeSetResultAndPops_backstackReturnsToCaller() =
        testScope.runTest {
            val nav = navigator()

            val resultDeferred =
                async {
                    nav.pushForResult<String>(DetailRoute("1"))
                }

            nav.setResult("done")
            nav.pop()
            resultDeferred.await()

            assertEquals(1, nav.backstack.value.depth)
            assertIs<HomeRoute>(
                nav.backstack.value.active
                    ?.route
            )
        }

    // ── Cancellation paths ─────────────────────────────────────────────────

    @Test
    fun pushForResult_calleePopWithoutResult_returnsCancelled() =
        testScope.runTest {
            val nav = navigator()

            val resultDeferred =
                async {
                    nav.pushForResult<String>(DetailRoute("2"))
                }

            // Pop without calling setResult
            nav.pop()

            assertIs<NavResult.Cancelled>(resultDeferred.await())
        }

    @Test
    fun pushForResult_resetDismissesEntry_returnsCancelled() =
        testScope.runTest {
            val nav = navigator()

            val resultDeferred =
                async {
                    nav.pushForResult<String>(DetailRoute("3"))
                }

            // Reset clears the entry without a result
            nav.reset(HomeRoute)

            assertIs<NavResult.Cancelled>(resultDeferred.await())
        }

    // ── setResult overwrite ────────────────────────────────────────────────

    @Test
    fun setResult_calledMultipleTimes_lastValueDelivered() =
        testScope.runTest {
            val nav = navigator()

            val resultDeferred =
                async {
                    nav.pushForResult<String>(DetailRoute("4"))
                }

            nav.setResult("first")
            nav.setResult("second")
            nav.setResult("third")
            nav.pop()

            val result = resultDeferred.await()
            assertIs<NavResult.Success<String>>(result)
            assertEquals("third", result.value)
        }

    // ── getOrNull / getOrDefault helpers ───────────────────────────────────

    @Test
    fun navResultSuccess_getOrNull_returnsValue() {
        val result: NavResult<Int> = NavResult.Success(42)
        assertEquals(42, result.getOrNull())
    }

    @Test
    fun navResultCancelled_getOrNull_returnsNull() {
        val result: NavResult<Int> = NavResult.Cancelled
        assertNull(result.getOrNull())
    }

    @Test
    fun navResultSuccess_getOrDefault_returnsValue() {
        val result: NavResult<Int> = NavResult.Success(7)
        assertEquals(7, result.getOrDefault(99))
    }

    @Test
    fun navResultCancelled_getOrDefault_returnsDefault() {
        val result: NavResult<Int> = NavResult.Cancelled
        assertEquals(99, result.getOrDefault(99))
    }

    // ── pushForResultOrNull convenience ────────────────────────────────────

    @Test
    fun pushForResultOrNull_calleeSetResult_returnsValue() =
        testScope.runTest {
            val nav = navigator()

            val resultDeferred =
                async {
                    nav.pushForResultOrNull<String>(DetailRoute("5"))
                }

            nav.setResult("value")
            nav.pop()

            assertEquals("value", resultDeferred.await())
        }

    @Test
    fun pushForResultOrNull_calleeCancelledWithoutResult_returnsNull() =
        testScope.runTest {
            val nav = navigator()

            val resultDeferred =
                async {
                    nav.pushForResultOrNull<String>(DetailRoute("6"))
                }

            nav.pop()

            assertNull(resultDeferred.await())
        }

    // ── previousEntry ──────────────────────────────────────────────────────

    @Test
    fun previousEntry_atRoot_returnsNull() {
        val nav = navigator()
        assertNull(nav.previousEntry)
    }

    @Test
    fun previousEntry_afterOnePush_returnsRootEntry() {
        val nav = navigator()
        nav.push(DetailRoute("7"))
        assertIs<HomeRoute>(nav.previousEntry?.route)
    }

    @Test
    fun previousEntry_afterMultiplePushes_returnsSecondToLast() {
        val nav = navigator()
        nav.push(DetailRoute("8"))
        nav.push(SettingsRoute)
        assertIs<DetailRoute>(nav.previousEntry?.route)
    }
}
