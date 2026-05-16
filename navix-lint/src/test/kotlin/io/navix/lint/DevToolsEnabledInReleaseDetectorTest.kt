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
package io.navix.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue

class DevToolsEnabledInReleaseDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = DevToolsEnabledInReleaseDetector()

    override fun getIssues(): List<Issue> = listOf(DevToolsEnabledInReleaseDetector.ISSUE)

    fun testOverlayEnabledTrueIsFlagged() {
        lint()
            .files(
                kotlin(
                    """
                    package test
                    import io.navix.devtools.NavixDevToolsOverlay
                    fun Screen(navigator: Any) {
                        NavixDevToolsOverlay(navigator = navigator, enabled = true)
                    }
                    """,
                ).indented(),
                navixDevToolsStub(),
            )
            .allowMissingSdk()
            .run()
            .expect(
                """
                src/test/test.kt:4: Warning: NavixDevToolsOverlay has enabled = true hardcoded. This will show the debug overlay in release builds. Use enabled = BuildConfig.DEBUG instead. [NavixDevToolsEnabledInRelease]
                    NavixDevToolsOverlay(navigator = navigator, enabled = true)
                                                                          ~~~~
                0 errors, 1 warnings
                """.trimIndent(),
            )
    }

    fun testOverlayEnabledBuildConfigDebugIsNotFlagged() {
        lint()
            .files(
                kotlin(
                    """
                    package test
                    import io.navix.devtools.NavixDevToolsOverlay
                    object BuildConfig { const val DEBUG = false }
                    fun Screen(navigator: Any) {
                        NavixDevToolsOverlay(navigator = navigator, enabled = BuildConfig.DEBUG)
                    }
                    """,
                ).indented(),
                navixDevToolsStub(),
            )
            .allowMissingSdk()
            .run()
            .expectClean()
    }

    fun testOverlayEnabledFalseIsNotFlagged() {
        lint()
            .files(
                kotlin(
                    """
                    package test
                    import io.navix.devtools.NavixDevToolsOverlay
                    fun Screen(navigator: Any) {
                        NavixDevToolsOverlay(navigator = navigator, enabled = false)
                    }
                    """,
                ).indented(),
                navixDevToolsStub(),
            )
            .allowMissingSdk()
            .run()
            .expectClean()
    }

    fun testOverlayEnabledOmittedIsNotFlagged() {
        lint()
            .files(
                kotlin(
                    """
                    package test
                    import io.navix.devtools.NavixDevToolsOverlay
                    fun Screen(navigator: Any) {
                        NavixDevToolsOverlay(navigator = navigator)
                    }
                    """,
                ).indented(),
                navixDevToolsStub(),
            )
            .allowMissingSdk()
            .run()
            .expectClean()
    }

    private fun navixDevToolsStub() =
        kotlin(
            """
        package io.navix.devtools
        fun NavixDevToolsOverlay(navigator: Any, enabled: Boolean = false) {}
        """,
        ).indented()
}
