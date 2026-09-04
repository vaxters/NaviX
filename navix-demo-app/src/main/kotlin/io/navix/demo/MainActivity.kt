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
package io.navix.demo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * launchMode="singleTop" (see AndroidManifest.xml) keeps a single MainActivity instance
 * alive across repeated VIEW deep-link intents, routing each one through [onNewIntent]
 * instead of creating (and navigating away from) a new Activity/backstack every time.
 */
class MainActivity : ComponentActivity() {
    private var deepLinkUri by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        deepLinkUri = intent?.data?.toString()
        setContent {
            NavixDemoTheme {
                // Reading the mutableStateOf here (not a one-shot constructor arg) means
                // a later onNewIntent() update recomposes DemoNavHost with the new URI,
                // whose LaunchedEffect(deepLinkUri) re-runs handleDeepLink for it.
                DemoNavHost(deepLinkUri = deepLinkUri)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkUri = intent.data?.toString()
    }
}
