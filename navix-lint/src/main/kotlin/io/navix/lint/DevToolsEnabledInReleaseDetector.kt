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

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.ULiteralExpression

/**
 * Warns when [NavixDevToolsOverlay][io.navix.devtools.NavixDevToolsOverlay] is called with
 * `enabled = true` hardcoded, which means the overlay will appear in release builds.
 *
 * Safe patterns (not flagged):
 * - `NavixDevToolsOverlay(navigator, enabled = BuildConfig.DEBUG)`
 * - `NavixDevToolsOverlay(navigator, enabled = false)`
 * - `NavixDevToolsOverlay(navigator)` — default is `false`, safe
 *
 * Unsafe pattern (flagged):
 * - `NavixDevToolsOverlay(navigator, enabled = true)` — ships debug UI to production
 */
class DevToolsEnabledInReleaseDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(UCallExpression::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler =
        object : UElementHandler() {
            override fun visitCallExpression(node: UCallExpression) {
                if (node.methodName != COMPOSABLE_NAME) return

                // Use computeArgumentMapping so named-parameter reordering (tested by Lint's
                // REORDER_ARGUMENTS test mode) doesn't break the lookup.
                val method = node.resolve() ?: return
                val mapping = context.evaluator.computeArgumentMapping(node, method)
                val enabledArg =
                    mapping.entries
                        .find { (_, param) -> param.name == ENABLED_PARAM }
                        ?.key
                        ?: return // No 'enabled' argument supplied — default is false, safe.

                val isLiteralTrue = enabledArg is ULiteralExpression && enabledArg.value == true
                if (isLiteralTrue) {
                    context.report(
                        issue = ISSUE,
                        scope = node,
                        location = context.getLocation(enabledArg),
                        message =
                            "`NavixDevToolsOverlay` has `enabled = true` hardcoded. " +
                                "This will show the debug overlay in release builds. " +
                                "Use `enabled = BuildConfig.DEBUG` instead.",
                    )
                }
            }
        }

    companion object {
        private const val COMPOSABLE_NAME = "NavixDevToolsOverlay"
        private const val ENABLED_PARAM = "enabled"

        @JvmField
        val ISSUE: Issue =
            Issue.create(
                id = "NavixDevToolsEnabledInRelease",
                briefDescription = "NavixDevToolsOverlay enabled in release builds",
                explanation = """
                `NavixDevToolsOverlay` with `enabled = true` hardcoded will render the debug
                overlay in release builds, exposing internal navigation state to end users.

                Replace with `enabled = BuildConfig.DEBUG` to restrict the overlay to debug
                builds only, or omit the parameter entirely (the default is `false`).
            """,
                category = Category.CORRECTNESS,
                priority = 8,
                severity = Severity.WARNING,
                implementation =
                    Implementation(
                        DevToolsEnabledInReleaseDetector::class.java,
                        Scope.JAVA_FILE_SCOPE,
                    ),
            )
    }
}
