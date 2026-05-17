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
package io.navix.demo.ui.overlay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.navix.demo.routes.ConfirmActionDialog
import io.navix.runtime.Navigator

/**
 * Demonstrates [DestinationKind.Dialog] destinations driven by the main [Navigator].
 *
 * Tapping "Show Dialog" calls `navigator.push(ConfirmActionDialog)`. The dialog is
 * registered with `dialog<ConfirmActionDialog> { }` in the main `NavixHost`, so the host
 * renders it in a `Dialog` composable while keeping this screen composed below.
 *
 * Pressing back or confirming/cancelling calls `navigator.pop()`, which removes the dialog
 * entry from the backstack and returns focus here.
 */
@Composable
fun OverlayDemoScreen(navigator: Navigator) {
    var lastAction by remember { mutableStateOf("—") }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Dialog Destinations Demo",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text =
                "Dialog destinations are first-class backstack entries registered with " +
                    "dialog<T> { } in NavixHost. The screen below stays composed. Pressing back " +
                    "or dismissing calls navigator.pop().",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))

        Button(
            onClick = { navigator.push(ConfirmActionDialog) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Show Confirmation Dialog") }

        Spacer(Modifier.height(8.dp))
        StatusRow(label = "Last action", value = lastAction)

        Spacer(Modifier.weight(1f))

        OutlinedButton(
            onClick = { navigator.pop() },
            modifier = Modifier.fillMaxWidth()
        ) { Text("← Back") }
    }
}

/**
 * Content rendered inside the `Dialog` container when `ConfirmActionDialog` is the active
 * backstack entry. The navigator is passed down via [io.navix.compose.LocalNavigator].
 *
 * The host's `dialog<ConfirmActionDialog> { entry, _ -> ConfirmDialogContent(...) }` block
 * provides `onDismissRequest = { navigator.pop() }` automatically; this composable only
 * needs to call `navigator.pop()` for its action buttons.
 */
@Composable
fun ConfirmDialogContent(
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        modifier =
            Modifier
                .fillMaxWidth(0.85f)
                .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Confirm Action",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text =
                    "This dialog is a first-class backstack entry. The screen behind it " +
                        "stays fully composed. Back press calls navigator.pop().",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) { Text("Cancel") }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                ) { Text("Confirm") }
            }
        }
    }
}

@Composable
private fun StatusRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
    }
}
