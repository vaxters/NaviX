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
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import io.navix.annotations.RouteDestination

/**
 * KSP entry point for Navix route discovery.
 *
 * Scans the current compilation unit for all classes annotated with [@RouteDestination],
 * validates them, and delegates code generation to [RouteRegistryGenerator] and
 * [DeepLinkHandlerGenerator].
 *
 * Compile-time errors are emitted for:
 * - Missing @Serializable annotation
 * - Invalid or malformed deep link URI templates
 * - Duplicate canonical route identifiers within the same module
 */
internal class NavixSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>,
) : SymbolProcessor {
    private val registryGenerator = RouteRegistryGenerator(codeGenerator)
    private val deepLinkGenerator = DeepLinkHandlerGenerator(codeGenerator)
    private val extensionsGenerator = NavixExtensionsGenerator(codeGenerator)
    private val serializersModuleGenerator = SerializersModuleGenerator(codeGenerator)

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val annotatedSymbols =
            resolver
                .getSymbolsWithAnnotation(
                    requireNotNull(RouteDestination::class.qualifiedName) {
                        "RouteDestination annotation must have a qualified name"
                    },
                ).filterIsInstance<KSClassDeclaration>()
                .toList()

        if (annotatedSymbols.isEmpty()) return emptyList()

        val descriptors = mutableListOf<RouteDestinationDescriptor>()
        val visitor = RouteDestinationVisitor(logger, descriptors)
        annotatedSymbols.forEach { it.accept(visitor, Unit) }

        validateUniqueness(descriptors)

        val moduleName = options["navix.moduleName"] ?: "app"
        registryGenerator.generate(descriptors, moduleName)
        extensionsGenerator.generate(descriptors, moduleName)
        serializersModuleGenerator.generate(descriptors, moduleName)
        descriptors.forEach { deepLinkGenerator.generate(it) }

        return emptyList()
    }

    private fun validateUniqueness(descriptors: List<RouteDestinationDescriptor>) {
        descriptors
            .groupBy { it.canonicalRoute }
            .filter { it.value.size > 1 }
            .forEach { (route, dupes) ->
                logger.error(
                    "Duplicate @RouteDestination canonical route '$route' found in: " +
                        dupes.joinToString { it.fullyQualifiedName },
                )
            }
    }
}

/** Registered via META-INF/services — KSP uses this to discover the processor. */
class NavixSymbolProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        NavixSymbolProcessor(
            codeGenerator = environment.codeGenerator,
            logger = environment.logger,
            options = environment.options,
        )
}
