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
package io.navix.demo.ui.playground

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.navix.contracts.NavTransitionKey
import io.navix.demo.routes.NavPlayground
import io.navix.demo.routes.NavPlaygroundStep
import io.navix.demo.routes.OverlayDemo
import io.navix.demo.routes.ResultPassingDemo
import io.navix.demo.routes.TabsDemo
import io.navix.runtime.Navigator
import io.navix.runtime.popTo

/**
 * Interactive playground that demonstrates every [Navigator] operation with live backstack
 * visualization. Each section maps to one part of the Navix API:
 *
 * - **Push / Pop** — basic stack manipulation
 * - **Replace** — replace top entry without growing the stack
 * - **Reset** — clear stack to a single root
 * - **popTo** — pop until a specific route type (inclusive vs exclusive)
 * - **Deep link** — simulated deep link dispatch
 * - **Transitions** — named [NavTransitionKey] showcase
 */
@Composable
fun NavPlaygroundScreen(navigator: Navigator) {
    val snapshot by navigator.backstack.collectAsState()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
    ) {
        Text(
            text = "Navigation Playground",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        Spacer(Modifier.height(4.dp))

        // Live backstack visualization
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Backstack  [depth: ${snapshot.depth}]",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                snapshot.entries.reversed().forEachIndexed { index, entry ->
                    val isActive = index == 0
                    Text(
                        text =
                            buildString {
                                append(if (isActive) "▶ " else "  ")
                                append(entry.route::class.simpleName)
                                if (entry.route is NavPlaygroundStep) {
                                    append(" #${(entry.route as NavPlaygroundStep).stepNumber}")
                                }
                                append("  [${entry.lifecycleState.name.lowercase()}]")
                            },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color =
                            if (isActive) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader("Push / Pop")

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val nextStep =
                (
                    snapshot.entries.mapNotNull { (it.route as? NavPlaygroundStep)?.stepNumber }
                        .maxOrNull() ?: 0
                ) + 1
            Button(
                onClick = { navigator.push(NavPlaygroundStep(nextStep), NavTransitionKey.SlideLeft) },
                modifier = Modifier.weight(1f),
            ) { Text("Push Step $nextStep") }
            OutlinedButton(
                onClick = { navigator.pop() },
                enabled = snapshot.canPop,
                modifier = Modifier.weight(1f),
            ) { Text("Pop") }
        }

        Spacer(Modifier.height(12.dp))
        SectionHeader("Replace (no stack growth)")

        Button(
            onClick = {
                val nextStep =
                    (
                        snapshot.entries.mapNotNull { (it.route as? NavPlaygroundStep)?.stepNumber }
                            .maxOrNull() ?: 0
                    ) + 1
                navigator.replace(NavPlaygroundStep(nextStep), NavTransitionKey.Fade)
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Replace top with next Step") }

        Spacer(Modifier.height(12.dp))
        SectionHeader("Reset (clear stack)")

        Button(
            onClick = { navigator.reset(NavPlayground) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        ) { Text("Reset → Playground") }

        Spacer(Modifier.height(12.dp))
        SectionHeader("popTo — exclusive (keep target)")

        Button(
            onClick = { navigator.popTo<NavPlaygroundStep>(inclusive = false) },
            enabled = snapshot.entries.any { it.route is NavPlaygroundStep },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("popTo<Step>(inclusive=false)") }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Pops until the last Step entry is on top (Step is kept).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(8.dp))
        SectionHeader("popTo — inclusive (remove target)")

        Button(
            onClick = { navigator.popTo<NavPlaygroundStep>(inclusive = true) },
            enabled = snapshot.entries.any { it.route is NavPlaygroundStep },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("popTo<Step>(inclusive=true)") }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Pops through and including the last Step entry.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(12.dp))
        SectionHeader("Deep Link (simulated)")

        Button(
            onClick = { navigator.handleDeepLink("navix://product/playground-42") },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("handleDeepLink(\"navix://product/playground-42\")") }

        Spacer(Modifier.height(12.dp))
        SectionHeader("Transitions")

        val transitions =
            listOf(
                "SlideLeft" to NavTransitionKey.SlideLeft,
                "SlideRight" to NavTransitionKey.SlideRight,
                "Fade" to NavTransitionKey.Fade,
                "Scale" to NavTransitionKey.Scale,
                "SharedAxisX" to NavTransitionKey.SharedAxisX,
                "None" to NavTransitionKey.None,
            )
        transitions.chunked(3).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                row.forEach { (label, key) ->
                    OutlinedButton(
                        onClick = {
                            val nextStep =
                                (
                                    snapshot.entries.mapNotNull { (it.route as? NavPlaygroundStep)?.stepNumber }
                                        .maxOrNull() ?: 0
                                ) + 1
                            navigator.push(NavPlaygroundStep(nextStep), key)
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text(label, fontSize = 10.sp, maxLines = 1) }
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        Spacer(Modifier.height(12.dp))
        SectionHeader("Phase 6+ — Additional Capabilities")

        Button(
            onClick = { navigator.push(TabsDemo) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Multi-Stack Navigation Demo →") }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Independent per-tab backstacks with NavixMultiStack.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = { navigator.push(OverlayDemo) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Dialog Destinations Demo →") }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "First-class dialog/bottom-sheet entries via dialog<T> { } in NavixHost.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = { navigator.push(ResultPassingDemo) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Result Passing Demo →") }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "pushForResult<R> + setResult: coroutine-await result passing between screens.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(24.dp))
    }
}

/**
 * A single numbered step within the playground backstack.
 */
@Composable
fun NavPlaygroundStepScreen(
    navigator: Navigator,
    stepNumber: Int,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Step #$stepNumber",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "You pushed this step. Use the playground controls to navigate.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(24.dp))

        Button(onClick = { navigator.pop() }) {
            Text("← Pop")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { navigator.reset(NavPlayground) }) {
            Text("Reset to Playground")
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(6.dp))
}
