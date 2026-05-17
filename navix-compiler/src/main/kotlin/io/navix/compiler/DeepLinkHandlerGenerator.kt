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

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies

/**
 * Generates a [io.navix.runtime.DeepLinkHandler] implementation for each destination
 * that declares one or more deep link URI templates.
 *
 * Output example for `ProductDetail(productId: String)` with template `myapp://product/{productId}`:
 * ```kotlin
 * // Generated — do not edit
 * package io.navix.generated
 *
 * class ProductDetailDeepLinkHandler : DeepLinkHandler {
 *     private val template0 = Regex("myapp://product/(?<productId>[^/?&]+)")
 *
 *     override fun canHandle(uri: String): Boolean =
 *         template0.matches(uri)
 *
 *     override fun resolve(uri: String): Route? {
 *         template0.find(uri)?.let { match ->
 *             return io.example.ProductDetail(
 *                 // Non-local return (let is inline): returns null from resolve() if a
 *                 // capture group is unexpectedly absent, allowing the next handler a chance.
 *                 // Positional index (1-based) avoids Matcher.group(String) which requires
 *                 // Android API 26; groups[N] works on all API levels.
 *                 productId = match.groups[1]?.value ?: return null,
 *             )
 *         }
 *         return null
 *     }
 * }
 * ```
 */
internal class DeepLinkHandlerGenerator(
    private val codeGenerator: CodeGenerator
) {
    fun generate(descriptor: RouteDestinationDescriptor) {
        val source = buildDeepLinkHandlerSource(descriptor) ?: return

        val className = "${descriptor.className}DeepLinkHandler"
        val packageName = "io.navix.generated"

        // Isolated dependency: this generated file depends only on the single source file
        // that declares the route. KSP re-runs this generator only when that file changes,
        // not when unrelated routes are added or modified elsewhere in the module.
        val sources = listOfNotNull(descriptor.containingFile).toTypedArray()
        codeGenerator
            .createNewFile(
                dependencies = Dependencies(aggregating = false, sources = sources),
                packageName = packageName,
                fileName = className
            ).use { stream ->
                stream.write(source.toByteArray())
            }
    }
}

/**
 * Builds the source code for a [io.navix.runtime.DeepLinkHandler] implementation
 * covering all deep-link templates declared on [descriptor].
 *
 * Returns `null` when the descriptor has no templates (nothing to generate).
 *
 * Extracted as a pure function (no [CodeGenerator] dependency) so it can be exercised
 * in unit tests without a KSP compilation environment.
 */
internal fun buildDeepLinkHandlerSource(descriptor: RouteDestinationDescriptor): String? {
    if (descriptor.deepLinkTemplates.isEmpty()) return null

    val className = "${descriptor.className}DeepLinkHandler"
    val packageName = "io.navix.generated"

    val templateFields =
        descriptor.deepLinkTemplates
            .mapIndexed { index, template ->
                "    private val template$index = Regex(\"\"\"${template.matchingRegex.pattern}\"\"\")"
            }.joinToString("\n")

    val canHandleChecks =
        descriptor.deepLinkTemplates.indices.joinToString(" ||\n        ") {
            "template$it.containsMatchIn(uri)"
        }

    val resolveBlocks =
        descriptor.deepLinkTemplates
            .mapIndexed { index, template ->
                // Use `?: return null` (non-local return — `let` is inline) so that a missing
                // capture group causes resolve() to return null gracefully, letting the next
                // registered DeepLinkHandler attempt the URI. This replaces the previous
                // `?: error(...)` which threw and crashed the handleDeepLink() call site.
                //
                // Positional group indices (1-based: group 0 = full match, group 1 = first capture)
                // are used instead of named group string access because Matcher.group(String) requires
                // Android API 26, while groups[N] works on all API levels.
                val paramAssignments =
                    template.params
                        .mapIndexed { index, param ->
                            "$param = match.groups[${index + 1}]?.value ?: return null,"
                        }.joinToString(",\n                ")
                buildString {
                    appendLine("        template$index.find(uri)?.let { match ->")
                    appendLine("            return ${descriptor.fullyQualifiedName}(")
                    if (paramAssignments.isNotBlank()) appendLine("                $paramAssignments")
                    appendLine("            )")
                    append("        }")
                }
            }.joinToString("\n")

    return buildString {
        appendLine("// Generated by Navix Compiler — do not edit")
        appendLine("package $packageName")
        appendLine()
        appendLine("import io.navix.contracts.Route")
        appendLine("import io.navix.runtime.DeepLinkHandler")
        appendLine()
        appendLine("class $className : DeepLinkHandler {")
        appendLine()
        appendLine(templateFields)
        appendLine()
        appendLine("    override fun canHandle(uri: String): Boolean =")
        appendLine("        $canHandleChecks")
        appendLine()
        appendLine("    override fun resolve(uri: String): Route? {")
        appendLine(resolveBlocks)
        appendLine("        return null")
        appendLine("    }")
        appendLine("}")
    }
}
