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

import app.cash.turbine.test
import io.navix.contracts.NavEvent
import io.navix.contracts.NavEventType
import io.navix.contracts.NavTransitionKey
import io.navix.contracts.NavixTelemetry
import io.navix.contracts.Route
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NavigatorImplTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private fun navigator(deepLinkHandlers: List<DeepLinkHandler> = emptyList()): NavigatorImpl =
        NavigatorImpl(
            root = HomeRoute,
            // backgroundScope is silently cancelled at the end of runTest without triggering
            // UncompletedCoroutinesError. This is correct for the actor coroutine — it's a
            // long-lived "infrastructure" coroutine, not a test assertion coroutine.
            scope = testScope.backgroundScope,
            telemetry = NavixTelemetry.NoOp,
            deepLinkHandlers = deepLinkHandlers
        )

    // ── Backstack initial state ────────────────────────────────────────────

    @Test
    fun backstack_afterInit_containsRoot() {
        val nav = navigator()
        assertEquals(1, nav.backstack.value.depth)
        assertIs<HomeRoute>(
            nav.backstack.value.active
                ?.route
        )
    }

    // ── Push ──────────────────────────────────────────────────────────────

    @Test
    fun push_newRoute_updatesBackstack() {
        val nav = navigator()
        nav.push(DetailRoute("42"))
        assertEquals(2, nav.backstack.value.depth)
        assertIs<DetailRoute>(
            nav.backstack.value.active
                ?.route
        )
    }

    @Test
    fun push_newRoute_emitsPushEvent() =
        testScope.runTest {
            val nav = navigator()
            nav.events.test {
                nav.push(DetailRoute("1"))
                val event = awaitItem()
                assertEquals(NavEventType.PUSH, event.type)
                assertIs<HomeRoute>(event.from?.route)
                assertIs<DetailRoute>(event.to?.route)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun push_withTransitionKey_stampsActiveEntryWithKey() {
        val nav = navigator()
        nav.push(DetailRoute("1"), NavTransitionKey.SlideLeft)
        assertEquals(
            NavTransitionKey.SlideLeft,
            nav.backstack.value.active
                ?.transitionKey
        )
    }

    // ── Pop ───────────────────────────────────────────────────────────────

    @Test
    fun pop_singleEntryStack_isNoOp() {
        val nav = navigator()
        nav.pop()
        assertEquals(1, nav.backstack.value.depth)
        assertIs<HomeRoute>(
            nav.backstack.value.active
                ?.route
        )
    }

    @Test
    fun pop_multiEntryStack_removesTopEntry() {
        val nav = navigator()
        nav.push(DetailRoute("1"))
        nav.pop()
        assertEquals(1, nav.backstack.value.depth)
        assertIs<HomeRoute>(
            nav.backstack.value.active
                ?.route
        )
    }

    @Test
    fun pop_multiEntryStack_emitsPopEvent() =
        testScope.runTest {
            val nav = navigator()
            nav.push(DetailRoute("1"))
            nav.events.test {
                nav.pop()
                val event = awaitItem()
                assertEquals(NavEventType.POP, event.type)
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ── Replace ───────────────────────────────────────────────────────────

    @Test
    fun replace_activeRoute_swapsActiveRoute() {
        val nav = navigator()
        nav.replace(SettingsRoute)
        assertEquals(1, nav.backstack.value.depth)
        assertIs<SettingsRoute>(
            nav.backstack.value.active
                ?.route
        )
    }

    @Test
    fun replace_activeRoute_emitsReplaceEvent() =
        testScope.runTest {
            val nav = navigator()
            nav.events.test {
                nav.replace(SettingsRoute)
                val event = awaitItem()
                assertEquals(NavEventType.REPLACE, event.type)
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ── Reset ─────────────────────────────────────────────────────────────

    @Test
    fun reset_deepStack_clearsHistory() {
        val nav = navigator()
        nav.push(DetailRoute("1"))
        nav.push(SettingsRoute)
        nav.reset(ProfileRoute)
        assertEquals(1, nav.backstack.value.depth)
        assertIs<ProfileRoute>(
            nav.backstack.value.active
                ?.route
        )
    }

    @Test
    fun reset_anyStack_emitsResetEvent() =
        testScope.runTest {
            val nav = navigator()
            nav.push(DetailRoute("1"))
            nav.events.test {
                nav.reset(HomeRoute)
                val event = awaitItem()
                assertEquals(NavEventType.RESET, event.type)
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ── PopTo ─────────────────────────────────────────────────────────────

    @Test
    fun popTo_existingTarget_navigatesBackToTarget() {
        val nav = navigator()
        nav.push(DetailRoute("1"))
        nav.push(SettingsRoute)
        nav.push(ProfileRoute)
        nav.popTo<DetailRoute>()
        assertEquals(2, nav.backstack.value.depth)
        assertIs<DetailRoute>(
            nav.backstack.value.active
                ?.route
        )
    }

    // ── Deep link ─────────────────────────────────────────────────────────

    @Test
    fun handleDeepLink_noHandlerMatches_returnsFalse() {
        val nav = navigator()
        assertFalse(nav.handleDeepLink("myapp://unknown"))
    }

    @Test
    fun handleDeepLink_handlerMatches_pushesResolvedRoute() {
        val handler =
            object : DeepLinkHandler {
                override fun canHandle(uri: String) = uri.startsWith("myapp://detail/")

                override fun resolve(uri: String) = DetailRoute(uri.substringAfterLast("/"))
            }
        val nav = navigator(deepLinkHandlers = listOf(handler))
        assertTrue(nav.handleDeepLink("myapp://detail/99"))
        assertIs<DetailRoute>(
            nav.backstack.value.active
                ?.route
        )
        assertEquals(
            "99",
            (
                nav.backstack.value.active
                    ?.route as DetailRoute
            ).id
        )
    }

    @Test
    fun handleDeepLink_handlerMatches_emitsDeepLinkEventWithUriMetadata() =
        testScope.runTest {
            val handler =
                object : DeepLinkHandler {
                    override fun canHandle(uri: String) = true

                    override fun resolve(uri: String) = DetailRoute("x")
                }
            val nav = navigator(deepLinkHandlers = listOf(handler))
            nav.events.test {
                nav.handleDeepLink("myapp://detail/x")
                val event = awaitItem()
                assertEquals(NavEventType.DEEP_LINK, event.type)
                assertEquals("myapp://detail/x", event.metadata["uri"])
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ── Telemetry forwarding ───────────────────────────────────────────────

    @Test
    fun telemetry_multipleActions_receivesAllEvents() {
        val received = mutableListOf<NavEvent>()
        val telemetry = NavixTelemetry { received.add(it) }
        val nav = NavigatorImpl(root = HomeRoute, scope = testScope.backgroundScope, telemetry = telemetry)
        nav.push(DetailRoute("1"))
        nav.pop()
        assertEquals(2, received.size)
        assertEquals(NavEventType.PUSH, received[0].type)
        assertEquals(NavEventType.POP, received[1].type)
    }

    // R2.1 regression: a throwing telemetry implementation must NOT stall the actor.
    // Before the fix, an exception from onEvent() crashed the actor coroutine and all
    // subsequent navigation actions were silently dropped.
    @Test
    fun telemetry_throws_doesNotStallActorAndSubsequentActionsAreProcessed() {
        val received = mutableListOf<NavEvent>()
        val throwingTelemetry =
            NavixTelemetry { event ->
                if (event.type == NavEventType.PUSH) throw RuntimeException("simulated bad telemetry")
                received.add(event)
            }
        val nav = NavigatorImpl(root = HomeRoute, scope = testScope.backgroundScope, telemetry = throwingTelemetry)
        nav.push(DetailRoute("1")) // telemetry throws — must not stall the actor
        nav.pop() // must still be processed after the throw

        // Backstack reflects both operations: root entry only after push + pop
        assertEquals(1, nav.backstack.value.depth)
        assertIs<HomeRoute>(
            nav.backstack.value.active
                ?.route
        )

        // POP telemetry callback did NOT throw, so it was delivered
        assertEquals(1, received.size)
        assertEquals(NavEventType.POP, received[0].type)
    }

    // ── Deep-link null fallthrough ─────────────────────────────────────────

    // R2.2 integration: when a handler's resolve() returns null (the correct behaviour
    // after the generator fix — previously it threw), handleDeepLink must fall through
    // to the next registered handler rather than returning false prematurely.
    @Test
    fun handleDeepLink_firstHandlerResolveNullSecondHandlerMatches_usesSecondHandler() {
        val nullResolvingHandler =
            object : DeepLinkHandler {
                override fun canHandle(uri: String) = true // claims it can handle it

                override fun resolve(uri: String): Route? = null // but resolve returns null
            }
        val fallbackHandler =
            object : DeepLinkHandler {
                override fun canHandle(uri: String) = uri.startsWith("myapp://detail/")

                override fun resolve(uri: String) = DetailRoute(uri.substringAfterLast("/"))
            }
        val nav = navigator(deepLinkHandlers = listOf(nullResolvingHandler, fallbackHandler))
        assertTrue(nav.handleDeepLink("myapp://detail/42"))
        assertIs<DetailRoute>(
            nav.backstack.value.active
                ?.route
        )
        assertEquals(
            "42",
            (
                nav.backstack.value.active
                    ?.route as DetailRoute
            ).id
        )
    }

    // ── State restoration ─────────────────────────────────────────────────

    @Test
    fun restoreNavigator_withSavedSnapshot_resumesBackstackFromSnapshot() {
        val saver = JsonNavigatorSaver(testRoutesModule)
        val original = navigator()
        original.push(DetailRoute("1"))
        original.push(SettingsRoute)
        val bytes = saver.save(original.backstack.value)

        val restored =
            restoreNavigator(root = HomeRoute, scope = testScope.backgroundScope, savedBytes = bytes, saver = saver)

        assertEquals(3, restored.backstack.value.depth)
        assertIs<SettingsRoute>(
            restored.backstack.value.active
                ?.route
        )
    }

    @Test
    fun restoreNavigator_withCorruptBytes_fallsBackToFreshRoot() {
        val saver = JsonNavigatorSaver(testRoutesModule)

        val restored =
            restoreNavigator(
                root = HomeRoute,
                scope = testScope.backgroundScope,
                savedBytes = "corrupt".encodeToByteArray(),
                saver = saver
            )

        assertEquals(1, restored.backstack.value.depth)
        assertIs<HomeRoute>(
            restored.backstack.value.active
                ?.route
        )
    }

    @Test
    fun restoreNavigator_preservesRouteArguments_afterRoundTrip() {
        val saver = JsonNavigatorSaver(testRoutesModule)
        val original = navigator()
        original.push(DetailRoute("product-99"))
        val bytes = saver.save(original.backstack.value)

        val restored =
            restoreNavigator(root = HomeRoute, scope = testScope.backgroundScope, savedBytes = bytes, saver = saver)

        val restoredRoute =
            restored.backstack.value.active
                ?.route
        assertNotNull(restoredRoute)
        assertIs<DetailRoute>(restoredRoute)
        assertEquals("product-99", restoredRoute.id)
    }

    // ── Actor serialization ───────────────────────────────────────────────

    @Test
    fun push_concurrentCalls_areSerializedInArrivalOrder() {
        val nav = navigator()
        // Simulate rapid-fire pushes from the same call site (e.g. double-tap).
        repeat(5) { i -> nav.push(DetailRoute("$i")) }
        // With UnconfinedTestDispatcher the actor processes all 5 immediately.
        assertEquals(6, nav.backstack.value.depth) // root + 5 pushes
        assertEquals(
            "4",
            (
                nav.backstack.value.active
                    ?.route as DetailRoute
            ).id
        )
    }

    // Unlike the rapid-fire test above (single call site), this exercises the documented
    // "callable from any coroutine — the actor serializes" contract from many distinct
    // concurrent coroutines: every enqueued push must land, none dropped or duplicated.
    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun push_fromManyConcurrentCoroutines_actorSerializesWithoutLoss() =
        testScope.runTest {
            val nav = navigator()
            val count = 50
            val jobs =
                (0 until count).map { i ->
                    launch { nav.push(DetailRoute("$i")) }
                }
            jobs.forEach { it.join() }
            advanceUntilIdle()

            assertEquals(count + 1, nav.backstack.value.depth) // root + count pushes
        }
}

private fun NavixTelemetry(block: (NavEvent) -> Unit): NavixTelemetry =
    object : NavixTelemetry {
        override fun onEvent(event: NavEvent) = block(event)
    }

/** Mirrors what the KSP-generated SerializersModule produces for the test route set. */
private val testRoutesModule =
    SerializersModule {
        polymorphic(Route::class) {
            subclass(HomeRoute::class)
            subclass(DetailRoute::class)
            subclass(SettingsRoute::class)
            subclass(ProfileRoute::class)
        }
    }
