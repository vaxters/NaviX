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
package io.navix.compiler

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [buildSerializersModuleSource] — the pure string-building core of
 * [SerializersModuleGenerator].
 *
 * Tests cover the generated val name, required imports, polymorphic block structure,
 * and subclass declarations. The KSP [com.google.devtools.ksp.processing.CodeGenerator]
 * is not involved; these run as plain JVM unit tests.
 */
class SerializersModuleGeneratorTest {
    // ── File name (val name) ───────────────────────────────────────────────

    @Test
    fun buildSource_moduleName_producesCorrectValName() {
        val (valName, _) =
            buildSerializersModuleSource(
                descriptors = listOf(descriptor("com.example.HomeRoute")),
                moduleName = "app",
            )
        assertEquals("AppNavixSerializersModule", valName)
    }

    @Test
    fun buildSource_hyphenatedModuleName_sanitizesCorrectly() {
        val (valName, _) =
            buildSerializersModuleSource(
                descriptors = listOf(descriptor("com.example.HomeRoute")),
                moduleName = "my-feature",
            )
        // "my-feature" → replaceFirstChar uppercases 'm', then strip non-alphanumeric → "Myfeature"
        assertEquals("MyfeatureNavixSerializersModule", valName)
    }

    @Test
    fun buildSource_lowercaseModuleName_uppercasesFirstChar() {
        val (valName, _) =
            buildSerializersModuleSource(
                descriptors = listOf(descriptor("com.example.HomeRoute")),
                moduleName = "checkout",
            )
        assertEquals("CheckoutNavixSerializersModule", valName)
    }

    // ── Package declaration ────────────────────────────────────────────────

    @Test
    fun buildSource_generatedSource_containsCorrectPackage() {
        val (_, source) =
            buildSerializersModuleSource(
                descriptors = listOf(descriptor("com.example.HomeRoute")),
                moduleName = "app",
            )
        assertTrue(source.contains("package io.navix.generated"))
    }

    // ── Required imports ───────────────────────────────────────────────────

    @Test
    fun buildSource_generatedSource_containsAllRequiredImports() {
        val (_, source) =
            buildSerializersModuleSource(
                descriptors = listOf(descriptor("com.example.HomeRoute")),
                moduleName = "app",
            )
        assertTrue(source.contains("import io.navix.contracts.Route"))
        assertTrue(source.contains("import kotlinx.serialization.modules.SerializersModule"))
        assertTrue(source.contains("import kotlinx.serialization.modules.polymorphic"))
        assertTrue(source.contains("import kotlinx.serialization.modules.subclass"))
    }

    // ── Polymorphic block ──────────────────────────────────────────────────

    @Test
    fun buildSource_singleRoute_containsPolymorphicBlock() {
        val (_, source) =
            buildSerializersModuleSource(
                descriptors = listOf(descriptor("com.example.HomeRoute")),
                moduleName = "app",
            )
        assertTrue(source.contains("polymorphic(Route::class)"))
    }

    @Test
    fun buildSource_singleRoute_containsSubclassDeclaration() {
        val (_, source) =
            buildSerializersModuleSource(
                descriptors = listOf(descriptor("com.example.HomeRoute")),
                moduleName = "app",
            )
        assertTrue(source.contains("subclass(com.example.HomeRoute::class)"))
    }

    @Test
    fun buildSource_multipleRoutes_allSubclassesPresent() {
        val (_, source) =
            buildSerializersModuleSource(
                descriptors =
                    listOf(
                        descriptor("com.example.HomeRoute"),
                        descriptor("com.example.DetailRoute"),
                        descriptor("com.example.SettingsRoute"),
                    ),
                moduleName = "app",
            )
        assertTrue(source.contains("subclass(com.example.HomeRoute::class)"))
        assertTrue(source.contains("subclass(com.example.DetailRoute::class)"))
        assertTrue(source.contains("subclass(com.example.SettingsRoute::class)"))
    }

    @Test
    fun buildSource_multipleRoutes_sortedAlphabetically() {
        val (_, source) =
            buildSerializersModuleSource(
                descriptors =
                    listOf(
                        descriptor("com.example.ZetaRoute"),
                        descriptor("com.example.AlphaRoute"),
                        descriptor("com.example.BetaRoute"),
                    ),
                moduleName = "app",
            )
        val alphaIdx = source.indexOf("AlphaRoute")
        val betaIdx = source.indexOf("BetaRoute")
        val zetaIdx = source.indexOf("ZetaRoute")
        assertTrue(alphaIdx < betaIdx && betaIdx < zetaIdx, "Routes should be sorted alphabetically")
    }

    // ── SerializersModule val declaration ──────────────────────────────────

    @Test
    fun buildSource_generatedSource_declaresValWithCorrectType() {
        val (valName, source) =
            buildSerializersModuleSource(
                descriptors = listOf(descriptor("com.example.HomeRoute")),
                moduleName = "app",
            )
        assertTrue(source.contains("val $valName: SerializersModule = SerializersModule {"))
    }

    // ── Header comment ─────────────────────────────────────────────────────

    @Test
    fun buildSource_generatedSource_startsWithDoNotEditComment() {
        val (_, source) =
            buildSerializersModuleSource(
                descriptors = listOf(descriptor("com.example.HomeRoute")),
                moduleName = "app",
            )
        assertTrue(source.trimStart().startsWith("// Generated by Navix Compiler"))
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /**
     * Creates a minimal [RouteDestinationDescriptor] from a fully-qualified class name.
     * Only [packageName], [className], and [canonicalRoute] are needed by the generator.
     */
    private fun descriptor(fqn: String): RouteDestinationDescriptor {
        val lastDot = fqn.lastIndexOf('.')
        val packageName = if (lastDot >= 0) fqn.substring(0, lastDot) else ""
        val className = if (lastDot >= 0) fqn.substring(lastDot + 1) else fqn
        return RouteDestinationDescriptor(
            packageName = packageName,
            className = className,
            canonicalRoute = fqn,
            deepLinkTemplates = emptyList(),
            isSerializable = true,
        )
    }
}
