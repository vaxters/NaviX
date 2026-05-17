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
package io.navix.testing

import io.navix.contracts.BackstackSnapshot
import io.navix.contracts.NavEvent
import io.navix.contracts.NavEventType
import io.navix.contracts.NavTransitionKey
import io.navix.contracts.Route
import io.navix.runtime.BackstackAction
import io.navix.runtime.DefaultReducer
import io.navix.runtime.EntryFactory
import io.navix.runtime.NavResult
import io.navix.runtime.Navigator
import io.navix.runtime.Reducer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * A test double for [Navigator] that records all navigation calls for assertion.
 *
 * [FakeNavigator] updates its [backstack] state using the same [Reducer] as the real
 * implementation, so composables render correctly in UI tests and reducer-level behaviour
 * (single-top, guards, etc.) is exercised end-to-end.
 *
 * Assertion helpers are provided for common test patterns:
 *
 * ```kotlin
 * val nav = FakeNavigator(root = Home)
 * nav.push(ProductDetail("123"))
 * nav.assertCurrentRoute(ProductDetail("123"))
 * nav.assertBackstackSize(2)
 * nav.assertPushCount(1)
 * ```
 *
 * ### Custom reducers
 * Pass a custom [Reducer] to test backstack policies end-to-end:
 * ```kotlin
 * val nav = FakeNavigator(root = Home, reducer = SingleTopReducer())
 * nav.push(Home)       // no-op in single-top mode
 * nav.assertBackstackSize(1)
 * ```
 *
 * ### Deep link testing
 * Configure [deepLinkResult] before exercising code that calls [handleDeepLink].
 * Inspect [handledDeepLinks] afterwards to assert which URIs were dispatched:
 *
 * ```kotlin
 * nav.deepLinkResult = true
 * nav.handleDeepLink("myapp://product/42")
 * nav.assertDeepLinkHandled("myapp://product/42")
 * ```
 *
 * ### Compose Preview
 * Use [FakeNavigator.preview] to obtain a no-op navigator suitable for `@Preview`
 * composables that require a [Navigator] reference.
 */
class FakeNavigator(
    root: Route,
    entryFactory: EntryFactory = DeterministicEntryFactory(),
    private val reducer: Reducer = DefaultReducer(entryFactory)
) : Navigator {
    companion object {
        /**
         * Returns a [FakeNavigator] suitable for use in Compose `@Preview` functions.
         *
         * The returned navigator starts with [root] on the backstack. Navigation calls
         * update the internal state so composables recompose correctly; no real UI
         * transitions occur.
         *
         * ```kotlin
         * @Preview
         * @Composable
         * fun HomeScreenPreview() {
         *     PreviewNavixHost(route = Home) { _, _ ->
         *         HomeScreen(
         *             navigator = FakeNavigator.preview(Home),
         *             viewModel = HomeViewModel.forPreview(),
         *         )
         *     }
         * }
         * ```
         */
        fun preview(root: Route): FakeNavigator = FakeNavigator(root)
    }

    private val _backstack =
        MutableStateFlow(
            BackstackSnapshot(listOf(entryFactory.create(root, NavTransitionKey.Default)))
        )
    override val backstack: StateFlow<BackstackSnapshot> = _backstack.asStateFlow()

    private val _events = MutableSharedFlow<NavEvent>(extraBufferCapacity = 64)
    override val events: SharedFlow<NavEvent> = _events.asSharedFlow()

    val pushedRoutes = mutableListOf<Route>()
    val replacedRoutes = mutableListOf<Route>()
    var poppedCount = 0
        private set
    var resetCount = 0
        private set

    private val _emittedEventTypes = mutableListOf<NavEventType>()

    /** All [NavEventType]s emitted, in the order they occurred. */
    val emittedEventTypes: List<NavEventType> get() = _emittedEventTypes.toList()

    // ── Navigation actions ───────────────────────────────────────────────────

    override fun push(route: Route, transition: NavTransitionKey) {
        pushedRoutes.add(route)
        val prev = _backstack.value.active
        _backstack.value = reducer.reduce(_backstack.value, BackstackAction.Push(route, transition))
        _emittedEventTypes.add(NavEventType.PUSH)
        _events.tryEmit(NavEvent(NavEventType.PUSH, prev, _backstack.value.active, 0L))
    }

    override fun pop(transition: NavTransitionKey) {
        if (!_backstack.value.canPop) return
        poppedCount++
        val prev = _backstack.value.active
        val prevId = prev?.id
        _backstack.value = reducer.reduce(_backstack.value, BackstackAction.Pop(transition))
        _emittedEventTypes.add(NavEventType.POP)
        _events.tryEmit(NavEvent(NavEventType.POP, prev, _backstack.value.active, 0L))
        // Complete any pending result deferred for the popped entry.
        if (prevId != null) {
            val deferred = pendingResults.remove(prevId)
            deferred?.complete(pendingResultValues.remove(prevId))
        }
    }

    override fun replace(route: Route, transition: NavTransitionKey) {
        replacedRoutes.add(route)
        val prev = _backstack.value.active
        _backstack.value = reducer.reduce(_backstack.value, BackstackAction.Replace(route, transition))
        _emittedEventTypes.add(NavEventType.REPLACE)
        _events.tryEmit(NavEvent(NavEventType.REPLACE, prev, _backstack.value.active, 0L))
    }

    override fun reset(root: Route) {
        resetCount++
        val before = _backstack.value
        val prev = before.active
        _backstack.value = reducer.reduce(before, BackstackAction.Reset(root))
        _emittedEventTypes.add(NavEventType.RESET)
        _events.tryEmit(NavEvent(NavEventType.RESET, prev, _backstack.value.active, 0L))
        // Cancel all pending results for entries removed by the reset.
        val removedIds =
            before.entries.map { it.id } -
                _backstack.value.entries
                    .map { it.id }
                    .toSet()
        for (id in removedIds) {
            pendingResults.remove(id)?.complete(pendingResultValues.remove(id))
            pendingResultValues.remove(id)
        }
    }

    override fun popTo(routeClass: KClass<out Route>, inclusive: Boolean) {
        val prev = _backstack.value.active
        _backstack.value =
            reducer.reduce(
                _backstack.value,
                BackstackAction.PopTo(routeClass, inclusive)
            )
        _emittedEventTypes.add(NavEventType.POP_TO)
        _events.tryEmit(NavEvent(NavEventType.POP_TO, prev, _backstack.value.active, 0L))
    }

    // ── Result passing ───────────────────────────────────────────────────────

    /** Pending result deferreds keyed by the entry ID of the pushed destination. */
    private val pendingResults = HashMap<String, CompletableDeferred<Any?>>()

    /** Result values staged by [setResult], keyed by entry ID. */
    private val pendingResultValues = HashMap<String, Any?>()

    @Suppress("UNCHECKED_CAST")
    override suspend fun <R : Any> pushForResult(route: Route, transition: NavTransitionKey): NavResult<R> {
        val snapshotBefore = _backstack.value
        push(route, transition)
        val pushedEntry =
            _backstack.value.entries
                .first { e -> snapshotBefore.entries.none { it.id == e.id } }

        val deferred = CompletableDeferred<Any?>()
        pendingResults[pushedEntry.id] = deferred

        return try {
            val raw = deferred.await()
            if (raw == null) NavResult.Cancelled else NavResult.Success(raw as R)
        } catch (_: Exception) {
            NavResult.Cancelled
        }
    }

    override fun setResult(value: Any?) {
        val currentId = _backstack.value.active?.id ?: return
        pendingResultValues[currentId] = value
    }

    /**
     * Delivers the pending result for the current top entry, then pops it.
     * Use this in tests to simulate a callee screen completing with a result:
     *
     * ```kotlin
     * val nav = FakeNavigator(root = Home)
     * val resultDeferred = async { nav.pushForResult<Color>(PickerRoute) }
     * nav.popWithResult(Color.Red)
     * assertEquals(NavResult.Success(Color.Red), resultDeferred.await())
     * ```
     */
    fun popWithResult(value: Any?) {
        val currentId = _backstack.value.active?.id ?: return
        pendingResultValues[currentId] = value
        pop()
        // Complete the deferred after pop so the entry ID is no longer in the stack.
        val deferred = pendingResults.remove(currentId)
        deferred?.complete(pendingResultValues.remove(currentId))
    }

    /**
     * Pops the current entry without setting a result, completing any pending [pushForResult]
     * with [NavResult.Cancelled].
     */
    fun popCancelled() {
        val currentId = _backstack.value.active?.id
        pop()
        if (currentId != null) {
            pendingResults.remove(currentId)?.complete(null)
            pendingResultValues.remove(currentId)
        }
    }

    // ── Deep link ────────────────────────────────────────────────────────────

    private val _handledDeepLinks = mutableListOf<String>()

    /** All URIs that were passed to [handleDeepLink], in order. */
    val handledDeepLinks: List<String> get() = _handledDeepLinks.toList()

    /**
     * The value returned by [handleDeepLink]. Set this to `true` to simulate a
     * successful deep link resolution in tests.
     */
    var deepLinkResult: Boolean = false

    override fun handleDeepLink(uri: String): Boolean {
        _handledDeepLinks.add(uri)
        if (deepLinkResult) _emittedEventTypes.add(NavEventType.DEEP_LINK)
        return deepLinkResult
    }

    // ── Assertion helpers ────────────────────────────────────────────────────

    fun assertCurrentRoute(expected: Route) {
        assertEquals(
            expected,
            _backstack.value.active?.route,
            "Expected current route to be $expected but was ${_backstack.value.active?.route}"
        )
    }

    fun assertBackstackSize(expected: Int) {
        assertEquals(
            expected,
            _backstack.value.depth,
            "Expected backstack size $expected but was ${_backstack.value.depth}"
        )
    }

    fun assertLastPushed(expected: Route) {
        assertEquals(
            expected,
            pushedRoutes.lastOrNull(),
            "Expected last pushed route to be $expected but was ${pushedRoutes.lastOrNull()}"
        )
    }

    fun assertCanPop(expected: Boolean) {
        if (expected) {
            assertTrue(
                _backstack.value.canPop,
                "Expected canPop=true but stack has only ${_backstack.value.depth} entry"
            )
        } else {
            assertFalse(
                _backstack.value.canPop,
                "Expected canPop=false but stack has ${_backstack.value.depth} entries"
            )
        }
    }

    fun assertNoPushes() {
        assertTrue(pushedRoutes.isEmpty(), "Expected no pushes but got: $pushedRoutes")
    }

    fun assertPushCount(expected: Int) {
        assertEquals(
            expected,
            pushedRoutes.size,
            "Expected $expected pushes but got ${pushedRoutes.size}"
        )
    }

    fun assertNoReplaces() {
        assertTrue(replacedRoutes.isEmpty(), "Expected no replaces but got: $replacedRoutes")
    }

    fun assertReplaceCount(expected: Int) {
        assertEquals(
            expected,
            replacedRoutes.size,
            "Expected $expected replaces but got ${replacedRoutes.size}"
        )
    }

    fun assertNoPops() {
        assertEquals(0, poppedCount, "Expected no pops but got $poppedCount")
    }

    fun assertPopCount(expected: Int) {
        assertEquals(expected, poppedCount, "Expected $expected pops but got $poppedCount")
    }

    fun assertNoResets() {
        assertEquals(0, resetCount, "Expected no resets but got $resetCount")
    }

    fun assertResetCount(expected: Int) {
        assertEquals(expected, resetCount, "Expected $expected resets but got $resetCount")
    }

    fun assertEventEmitted(type: NavEventType) {
        assertTrue(
            _emittedEventTypes.contains(type),
            "Expected event of type $type to be emitted but only saw: $_emittedEventTypes"
        )
    }

    fun assertNoEventEmitted(type: NavEventType) {
        assertFalse(
            _emittedEventTypes.contains(type),
            "Expected no event of type $type but it was emitted. All events: $_emittedEventTypes"
        )
    }

    fun assertDeepLinkHandled(uri: String) {
        assertTrue(
            _handledDeepLinks.contains(uri),
            "Expected deep link '$uri' to be handled but handledDeepLinks=$_handledDeepLinks"
        )
    }

    fun assertNoDeepLinks() {
        assertTrue(
            _handledDeepLinks.isEmpty(),
            "Expected no deep links to be handled but got: $_handledDeepLinks"
        )
    }
}
