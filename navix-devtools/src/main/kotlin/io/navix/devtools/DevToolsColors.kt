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

import androidx.compose.ui.graphics.Color

internal object DevToolsColors {
    val background = Color(0xDD000000)
    val header = Color(0xFF64FFDA)
    val text = Color(0xFFE0E0E0)
    val dimmed = Color(0xFF757575)
    val activeEntry = Color(0x2264FFDA)
    val resumed = Color(0xFF69F0AE)
    val started = Color(0xFFFFD740)
    val push = Color(0xFF1DE9B6)    // teal — distinct from resumed green
    val pop = Color(0xFFFF5252)
    val replace = Color(0xFFE040FB) // purple — distinct from started amber
    val reset = Color(0xFFFF6D00)
    val deepLink = Color(0xFF40C4FF)
}
