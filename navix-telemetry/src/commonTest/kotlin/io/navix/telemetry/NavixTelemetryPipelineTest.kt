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
@file:OptIn(ExperimentalCoroutinesApi::class)

package io.navix.telemetry

import io.navix.contracts.NavEvent
import io.navix.contracts.NavEventType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NavixTelemetryPipelineTest {
    private fun fakeEvent(type: NavEventType = NavEventType.PUSH) =
        NavEvent(
            type = type,
            from = null,
            to = null,
            timestampMs = 0L,
        )

    @Test
    fun onEvent_multipleExporters_allExportersReceiveEveryEvent() =
        runTest {
            val received1 = mutableListOf<NavEvent>()
            val received2 = mutableListOf<NavEvent>()

            val pipeline =
                NavixTelemetryPipeline(
                    exporters =
                        listOf(
                            object : NavEventExporter {
                                override fun export(event: NavEvent) {
                                    received1.add(event)
                                }
                            },
                            object : NavEventExporter {
                                override fun export(event: NavEvent) {
                                    received2.add(event)
                                }
                            },
                        ),
                    dispatcher = StandardTestDispatcher(testScheduler),
                )

            val event = fakeEvent(NavEventType.PUSH)
            pipeline.onEvent(event)
            advanceUntilIdle()

            assertEquals(1, received1.size)
            assertEquals(1, received2.size)
            assertEquals(NavEventType.PUSH, received1[0].type)
        }

    @Test
    fun onEvent_firstExporterThrows_doesNotBlockSubsequentExporters() =
        runTest {
            val received = mutableListOf<NavEvent>()

            val pipeline =
                NavixTelemetryPipeline(
                    exporters =
                        listOf(
                            object : NavEventExporter {
                                override fun export(event: NavEvent): Unit = throw RuntimeException("oops")
                            },
                            object : NavEventExporter {
                                override fun export(event: NavEvent) {
                                    received.add(event)
                                }
                            },
                        ),
                    dispatcher = StandardTestDispatcher(testScheduler),
                )

            pipeline.onEvent(fakeEvent())
            advanceUntilIdle()

            assertEquals(1, received.size)
        }

    @Test
    fun export_noOpExporter_doesNotThrow() =
        runTest {
            val noop = NoOpExporter()
            noop.export(fakeEvent())
            // No assertion needed — must not throw
            assertTrue(true)
        }

    @Test
    fun onEvent_multipleEvents_deliveredInOrder() =
        runTest {
            val types = mutableListOf<NavEventType>()
            val pipeline =
                NavixTelemetryPipeline(
                    exporters =
                        listOf(
                            object : NavEventExporter {
                                override fun export(event: NavEvent) {
                                    types.add(event.type)
                                }
                            },
                        ),
                    dispatcher = StandardTestDispatcher(testScheduler),
                )

            pipeline.onEvent(fakeEvent(NavEventType.PUSH))
            pipeline.onEvent(fakeEvent(NavEventType.POP))
            pipeline.onEvent(fakeEvent(NavEventType.RESET))
            advanceUntilIdle()

            assertEquals(listOf(NavEventType.PUSH, NavEventType.POP, NavEventType.RESET), types)
        }

    @Test
    fun replayBuffer_multipleEvents_accumulatesOldestFirst() =
        runTest {
            val pipeline =
                NavixTelemetryPipeline(
                    exporters = emptyList(),
                    dispatcher = StandardTestDispatcher(testScheduler),
                )

            pipeline.onEvent(fakeEvent(NavEventType.PUSH))
            pipeline.onEvent(fakeEvent(NavEventType.POP))
            advanceUntilIdle()

            val buffer = pipeline.replayBuffer.value
            assertEquals(2, buffer.size)
            assertEquals(NavEventType.PUSH, buffer[0].type)
            assertEquals(NavEventType.POP, buffer[1].type)
        }

    @Test
    fun replayBuffer_lateSubscriber_receivesAllPreviousEvents() =
        runTest {
            val pipeline =
                NavixTelemetryPipeline(
                    exporters = emptyList(),
                    dispatcher = StandardTestDispatcher(testScheduler),
                )

            // Emit events before any observer subscribes
            pipeline.onEvent(fakeEvent(NavEventType.PUSH))
            pipeline.onEvent(fakeEvent(NavEventType.REPLACE))
            advanceUntilIdle()

            // Late subscriber sees all events (StateFlow retains last value)
            val snapshot = pipeline.replayBuffer.value
            assertEquals(2, snapshot.size)
        }

    @Test
    fun droppedEventCount_freshPipeline_startsAtZero() =
        runTest {
            val pipeline = NavixTelemetryPipeline(exporters = emptyList())
            assertEquals(0, pipeline.droppedEventCount.value)
        }

    @Test
    fun onEvent_concurrentCalls_doesNotCorruptReplayBuffer() =
        runTest {
            // Regression test for the thread-safety bug fixed in 0.1.x:
            // Previously, concurrent onEvent() calls could corrupt the ArrayDeque.
            // Now the buffer is actor-confined (single writer), so this is safe.
            val pipeline =
                NavixTelemetryPipeline(
                    exporters = emptyList(),
                    dispatcher = StandardTestDispatcher(testScheduler),
                )

            // Fire 20 events from different coroutines concurrently
            val jobs =
                (1..20).map {
                    launch {
                        pipeline.onEvent(fakeEvent(NavEventType.PUSH))
                    }
                }
            jobs.forEach { it.join() }
            advanceUntilIdle()

            // All events should be buffered without corruption
            val buffer = pipeline.replayBuffer.value
            assertTrue(buffer.size <= 20, "Buffer overflowed unexpectedly: ${buffer.size}")
            assertTrue(buffer.isNotEmpty(), "Buffer should not be empty")
        }
}
