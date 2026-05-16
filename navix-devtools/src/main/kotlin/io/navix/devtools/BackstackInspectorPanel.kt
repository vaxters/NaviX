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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.navix.contracts.NavLifecycleState
import io.navix.contracts.RouteEntry
import io.navix.runtime.Navigator

/**
 * Shows a live view of the current backstack, updated on every navigation action.
 *
 * Entries are listed from root (bottom) to active (top). The active entry is
 * highlighted and labelled with its lifecycle state.
 */
@Composable
internal fun BackstackInspectorPanel(navigator: Navigator) {
    val snapshot by navigator.backstack.collectAsState()

    Column {
        Text(
            text = "Backstack  [depth: ${snapshot.depth}]",
            style =
                MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = DevToolsColors.header,
                ),
            modifier = Modifier.padding(bottom = 4.dp),
        )

        LazyColumn {
            itemsIndexed(
                items = snapshot.entries.reversed(),
                key = { _, item ->
                    item.id
                },
            ) { index, entry ->
                val isActive = index == 0
                BackstackEntryRow(entry = entry, isActive = isActive)
            }
        }
    }
}

@Composable
private fun BackstackEntryRow(
    entry: RouteEntry,
    isActive: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    if (isActive) DevToolsColors.activeEntry else Color.Transparent,
                    shape = MaterialTheme.shapes.small,
                )
                .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        val dotColor =
            when (entry.lifecycleState) {
                NavLifecycleState.RESUMED -> DevToolsColors.resumed
                NavLifecycleState.STARTED -> DevToolsColors.started
                else -> DevToolsColors.dimmed
            }
        Spacer(
            modifier =
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = entry.route::class.simpleName ?: "Unknown",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                color = DevToolsColors.text,
            )
            Text(
                text = "id: ${entry.id.takeLast(8)}",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = DevToolsColors.dimmed,
            )
        }
    }
}
