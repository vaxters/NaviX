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

class RouteMissingSerializableDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = RouteMissingSerializableDetector()

    override fun getIssues(): List<Issue> = listOf(RouteMissingSerializableDetector.ISSUE)

    // ── Flagged cases ──────────────────────────────────────────────────────

    fun testRouteDestinationMissingSerializableIsFlagged() {
        lint()
            .files(
                *stubs(),
                kotlin(
                    """
                    package test
                    import io.navix.annotations.RouteDestination
                    import io.navix.contracts.Route
                    @RouteDestination
                    data class ProductDetail(val id: String) : Route
                    """,
                ).indented(),
            )
            .allowMissingSdk()
            .run()
            .expect(
                """
                src/test/ProductDetail.kt:5: Error: ProductDetail is annotated with @RouteDestination but is missing @Serializable. The Navix KSP processor requires both annotations to generate the route registry and SerializersModule. Add @Serializable from kotlinx.serialization. [RouteMissingSerializable]
                data class ProductDetail(val id: String) : Route
                           ~~~~~~~~~~~~~
                1 errors, 0 warnings
                """.trimIndent(),
            )
    }

    fun testRouteDestinationDataObjectMissingSerializableIsFlagged() {
        lint()
            .files(
                *stubs(),
                kotlin(
                    """
                    package test
                    import io.navix.annotations.RouteDestination
                    import io.navix.contracts.Route
                    @RouteDestination
                    data object Home : Route
                    """,
                ).indented(),
            )
            .allowMissingSdk()
            .run()
            .expect(
                """
                src/test/Home.kt:5: Error: Home is annotated with @RouteDestination but is missing @Serializable. The Navix KSP processor requires both annotations to generate the route registry and SerializersModule. Add @Serializable from kotlinx.serialization. [RouteMissingSerializable]
                data object Home : Route
                            ~~~~
                1 errors, 0 warnings
                """.trimIndent(),
            )
    }

    // ── Clean cases ────────────────────────────────────────────────────────

    fun testRouteDestinationWithSerializableIsClean() {
        lint()
            .files(
                *stubs(),
                kotlin(
                    """
                    package test
                    import io.navix.annotations.RouteDestination
                    import io.navix.contracts.Route
                    import kotlinx.serialization.Serializable
                    @Serializable
                    @RouteDestination
                    data class ProductDetail(val id: String) : Route
                    """,
                ).indented(),
            )
            .allowMissingSdk()
            .run()
            .expectClean()
    }

    fun testSerializableWithoutRouteDestinationIsClean() {
        lint()
            .files(
                *stubs(),
                kotlin(
                    """
                    package test
                    import kotlinx.serialization.Serializable
                    @Serializable
                    data class SomeDto(val value: String)
                    """,
                ).indented(),
            )
            .allowMissingSdk()
            .run()
            .expectClean()
    }

    fun testUnannotatedClassIsClean() {
        lint()
            .files(
                *stubs(),
                kotlin(
                    """
                    package test
                    data class NoAnnotation(val x: Int)
                    """,
                ).indented(),
            )
            .allowMissingSdk()
            .run()
            .expectClean()
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    // Each file can have only one package declaration — split into three separate stubs.
    private fun stubs() =
        arrayOf(
            kotlin(
                """
            package io.navix.annotations
            annotation class RouteDestination(vararg val deepLinks: String = [])
            """,
            ).indented(),
            kotlin(
                """
            package io.navix.contracts
            interface Route
            """,
            ).indented(),
            kotlin(
                """
            package kotlinx.serialization
            annotation class Serializable
            """,
            ).indented(),
        )
}
