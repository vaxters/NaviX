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
package io.navix.devtools

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.navix.contracts.NavEvent
import io.navix.contracts.NavEventType
import io.navix.runtime.Navigator
import kotlinx.coroutines.flow.StateFlow

/**
 * Displays a scrolling timeline of [NavEvent]s, newest at the top.
 *
 * ### Event source priority
 * When [eventHistory] is provided (a [StateFlow] backed by e.g.
 * `NavixTelemetryPipeline.replayBuffer` or `InMemoryEventExporter.events`), the panel
 * displays the full retained history — including events emitted before this panel was
 * opened. The list is reversed so the newest event appears at the top.
 *
 * When [eventHistory] is `null`, the panel falls back to subscribing to
 * [Navigator.events] (a hot SharedFlow). In this mode events emitted before the panel
 * was first opened are not visible. Pass [eventHistory] to avoid this limitation.
 */
@Composable
internal fun EventTimelinePanel(navigator: Navigator, eventHistory: StateFlow<List<NavEvent>>? = null) {
    val listState = rememberLazyListState()

    if (eventHistory != null) {
        // Preferred path: StateFlow holds full retained history. Newest-first display
        // via reversed() — no local accumulator needed; Compose recompose on each update.
        val allEvents by eventHistory.collectAsState()
        val displayEvents = remember(allEvents) { allEvents.reversed() }

        // Scroll to top whenever a new event arrives so the newest is always visible.
        // Key on allEvents (the StateFlow value) rather than size: two batches could arrive
        // making size unchanged (one added, one removed), preventing scroll if keyed on size.
        LaunchedEffect(allEvents) {
            if (displayEvents.isNotEmpty()) listState.animateScrollToItem(0)
        }

        Column {
            EventTimelineHeader()
            LazyColumn(state = listState) {
                // Index-based key: timestampMs can collide if two events arrive in the
                // same millisecond, which would cause a duplicate-key crash in LazyColumn.
                itemsIndexed(displayEvents, key = { index, _ -> index }) { _, event ->
                    EventRow(event)
                }
            }
        }
    } else {
        // Fallback path: accumulate from hot SharedFlow. Events emitted before this
        // composable entered the composition are not captured — use eventHistory to
        // avoid this limitation.
        val events = remember { mutableStateListOf<NavEvent>() }

        LaunchedEffect(navigator) {
            navigator.events.collect { event ->
                if (events.size >= MAX_EVENTS) events.removeAt(events.lastIndex) // Remove oldest (tail) to stay within MAX_EVENTS
                events.add(0, event)
            }
        }

        // Scroll to top whenever a new event is prepended.
        LaunchedEffect(events.size) {
            if (events.isNotEmpty()) listState.animateScrollToItem(0)
        }

        Column {
            EventTimelineHeader()
            LazyColumn(state = listState) {
                items(events, key = { it.timestampMs }) { event ->
                    EventRow(event)
                }
            }
        }
    }
}

@Composable
private fun EventTimelineHeader() {
    Text(
        text = "Event Timeline",
        style =
            MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = DevToolsColors.header
            ),
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun EventRow(event: NavEvent) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
    ) {
        Text(
            text = event.type.badge(),
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = event.type.color(),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(52.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text =
                buildString {
                    event.from?.route?.let { append(it::class.simpleName) } ?: append("—")
                    append(" → ")
                    event.to?.route?.let { append(it::class.simpleName) } ?: append("—")
                },
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = DevToolsColors.text
        )
    }
}

private fun NavEventType.badge(): String {
    return when (this) {
        NavEventType.PUSH -> "PUSH"
        NavEventType.POP -> "POP "
        NavEventType.REPLACE -> "REPL"
        NavEventType.RESET -> "RST "
        NavEventType.POP_TO -> "POPT"
        NavEventType.DEEP_LINK -> "DEEP"
    }
}

private fun NavEventType.color(): Color {
    return when (this) {
        NavEventType.PUSH -> DevToolsColors.push
        NavEventType.POP -> DevToolsColors.pop
        NavEventType.REPLACE -> DevToolsColors.replace
        NavEventType.RESET -> DevToolsColors.reset
        NavEventType.POP_TO -> DevToolsColors.pop
        NavEventType.DEEP_LINK -> DevToolsColors.deepLink
    }
}

private const val MAX_EVENTS = 100
