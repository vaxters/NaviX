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
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement

/**
 * Flags any class or object annotated with `@RouteDestination` that is missing
 * `@Serializable`.
 *
 * The Navix KSP processor requires `@Serializable` on every `@RouteDestination` class
 * to generate the route registry and the kotlinx-serialization `SerializersModule`. Without
 * it the KSP processor emits a compile error — this lint rule surfaces the problem earlier,
 * at the IDE inspection and `./gradlew lint` level, with a quick-fix-friendly message.
 *
 * ### Cases detected
 * ```kotlin
 * // ❌ Missing @Serializable — flagged
 * @RouteDestination
 * data class ProductDetail(val id: String) : Route
 *
 * // ✅ Both annotations present — clean
 * @Serializable
 * @RouteDestination
 * data class ProductDetail(val id: String) : Route
 * ```
 */
class RouteMissingSerializableDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(UClass::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler =
        object : UElementHandler() {
            override fun visitClass(node: UClass) {
                val hasRouteDestination =
                    node.uAnnotations.any { annotation ->
                        val name =
                            annotation.qualifiedName ?: annotation.attributeValues
                                .firstOrNull()?.name
                        name == ROUTE_DESTINATION_FQN || annotation.qualifiedName?.endsWith(
                            ".RouteDestination",
                        ) == true
                    }
                if (!hasRouteDestination) return

                val hasSerializable =
                    node.uAnnotations.any { annotation ->
                        annotation.qualifiedName?.endsWith(".Serializable") == true
                    }
                if (hasSerializable) return

                // Cast to UElement explicitly to resolve overload ambiguity: UClass implements
                // both UElement and PsiElement, making the scope parameter ambiguous without
                // the cast.
                context.report(
                    issue = ISSUE,
                    scope = node as UElement,
                    location = context.getNameLocation(node),
                    message =
                        "`${node.name}` is annotated with `@RouteDestination` but is " +
                            "missing `@Serializable`. The Navix KSP processor requires both " +
                            "annotations to generate the route registry and SerializersModule. " +
                            "Add `@Serializable` from `kotlinx.serialization`.",
                )
            }
        }

    companion object {
        private const val ROUTE_DESTINATION_FQN = "io.navix.annotations.RouteDestination"

        @JvmField
        val ISSUE: Issue =
            Issue.create(
                id = "RouteMissingSerializable",
                briefDescription = "@RouteDestination without @Serializable",
                explanation = """
                Every class or object annotated with `@RouteDestination` must also carry
                `@Serializable` (from `kotlinx.serialization`).

                Navix uses kotlinx-serialization to:
                - Generate a `SerializersModule` so the `BackstackSnapshot` can be saved
                  and restored across process death.
                - Validate deep link argument types at compile time.

                The KSP processor will emit a compile error if `@Serializable` is absent,
                but this lint rule catches the problem earlier — at IDE inspection time.

                **Fix:** Add `@Serializable` above `@RouteDestination`:
                ```kotlin
                @Serializable
                @RouteDestination
                data class ProductDetail(val id: String) : Route
                ```
            """,
                category = Category.CORRECTNESS,
                priority = 9,
                severity = Severity.ERROR,
                implementation =
                    Implementation(
                        RouteMissingSerializableDetector::class.java,
                        Scope.JAVA_FILE_SCOPE,
                    ),
            )
    }
}
