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
package io.navix.demo.ui.result

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.navix.demo.routes.ColorPickerRoute
import io.navix.runtime.NavResult
import io.navix.runtime.Navigator

/**
 * Demonstrates the `pushForResult<R>` / `setResult` coroutine-await result-passing API.
 *
 * Tapping "Pick a Color" launches [ColorPickerRoute] via `navigator.pushForResult<Int>()`.
 * The call suspends inside a [LaunchedEffect] until the color picker calls
 * `navigator.setResult(color.toArgb())` then `navigator.pop()`. The chosen color is then
 * shown back here.
 *
 * If the user presses back without picking a color, `pushForResult` returns
 * [NavResult.Cancelled] and the placeholder text is shown unchanged.
 */
@Composable
fun ResultPassingDemoScreen(navigator: Navigator) {
    var pickedColor by remember { mutableStateOf<Color?>(null) }
    var lastOutcome by remember { mutableStateOf("—") }

    // LaunchedEffect is restarted each time "Pick a Color" is tapped (key changes).
    // The coroutine suspends at pushForResult until the picker pops.
    var pickTrigger by remember { mutableIntStateOf(0) }
    LaunchedEffect(pickTrigger) {
        if (pickTrigger == 0) return@LaunchedEffect
        when (val result = navigator.pushForResult<Int>(ColorPickerRoute)) {
            is NavResult.Success -> {
                pickedColor = Color(result.value)
                lastOutcome = "Success — color selected"
            }

            NavResult.Cancelled -> {
                lastOutcome = "Cancelled — no color selected"
            }
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Result Passing Demo",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text =
                "Demonstrates pushForResult<R> / setResult. The caller suspends in a " +
                    "LaunchedEffect until the callee calls setResult(value) then pop(). " +
                    "Pressing back without picking yields NavResult.Cancelled.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(8.dp))

        // Color preview swatch
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(pickedColor ?: MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape),
                )
                Text(
                    text =
                        pickedColor?.let { "#%06X".format(it.toArgb() and 0xFFFFFF) }
                            ?: "No color picked yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Text(
            text = "Last outcome: $lastOutcome",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(
            onClick = { pickTrigger++ },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Pick a Color") }

        Spacer(Modifier.weight(1f))

        OutlinedButton(
            onClick = { navigator.pop() },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("← Back") }
    }
}

private val DemoColors =
    listOf(
        // Red
        Color(0xFFE53935),
        // Green
        Color(0xFF43A047),
        // Blue
        Color(0xFF1E88E5),
        // Orange
        Color(0xFFFB8C00),
        // Purple
        Color(0xFF8E24AA),
        // Cyan
        Color(0xFF00ACC1),
        // Yellow
        Color(0xFFFFD600),
        // Brown
        Color(0xFF6D4C41),
    )

/**
 * Color picker screen. Tapping a color calls `navigator.setResult(color.toArgb())` then
 * `navigator.pop()`, delivering the chosen color to the `pushForResult` caller.
 *
 * Pressing back without tapping a color leaves the result unset, causing the caller to
 * receive [NavResult.Cancelled].
 */
@Composable
fun ColorPickerScreen(navigator: Navigator) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            text = "Pick a Color",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text =
                "Tap a color to return it to the caller via setResult + pop. " +
                    "Press back without picking to return NavResult.Cancelled.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // 4-column color grid
        DemoColors.chunked(4).forEach { rowColors ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                rowColors.forEach { color ->
                    Box(
                        modifier =
                            Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .clickable {
                                    navigator.setResult(color.toArgb())
                                    navigator.pop()
                                },
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        OutlinedButton(
            onClick = { navigator.pop() },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("← Back (cancel)") }
    }
}
