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

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSVisitorVoid
import io.navix.annotations.RouteDestination

/**
 * KSP visitor that collects [RouteDestinationDescriptor]s from all classes
 * annotated with [RouteDestination].
 *
 * Emits KSP errors for:
 * - Routes not annotated with @Serializable (detected heuristically via annotation presence)
 * - Invalid deep link URI templates
 * - Duplicate canonical route identifiers (checked in [NavixSymbolProcessor])
 */
internal class RouteDestinationVisitor(
    private val logger: KSPLogger,
    private val descriptors: MutableList<RouteDestinationDescriptor>,
) : KSVisitorVoid() {
    override fun visitClassDeclaration(
        classDeclaration: KSClassDeclaration,
        data: Unit,
    ) {
        val annotation =
            classDeclaration.annotations.firstOrNull { it.shortName.asString() == RouteDestination::class.simpleName }
                ?: return

        val packageName = classDeclaration.packageName.asString()
        val className = classDeclaration.simpleName.asString()

        val isSerializable =
            classDeclaration.annotations.any {
                it.shortName.asString() == "Serializable"
            }
        if (!isSerializable) {
            logger.error(
                "@RouteDestination class '$className' must also be annotated with @Serializable.",
                classDeclaration,
            )
            return
        }

        val routeValue = annotation.arguments.firstOrNull { it.name?.asString() == "route" }?.value as? String ?: ""
        val canonicalRoute = routeValue.ifBlank { "$packageName.$className" }

        val deepLinks = annotation.arguments.firstOrNull { it.name?.asString() == "deepLinks" }?.value as? List<*>
        val deepLinkValues = deepLinks?.filterIsInstance<String>() ?: emptyList()

        val parsedTemplates =
            deepLinkValues.mapNotNull { template ->
                DeepLinkTemplateParser.parse(template).fold(
                    onSuccess = { it },
                    onFailure = { err ->
                        logger.error(
                            "Invalid deep link template '$template' on '$className': ${err.message}",
                            classDeclaration,
                        )
                        null
                    },
                )
            }

        val constructorParams =
            classDeclaration.primaryConstructor?.parameters?.map { param ->
                ConstructorParam(
                    name = param.name?.asString() ?: "_",
                    typeName = param.type.resolve().toKotlinTypeName(),
                )
            } ?: emptyList()

        descriptors.add(
            RouteDestinationDescriptor(
                packageName = packageName,
                className = className,
                canonicalRoute = canonicalRoute,
                deepLinkTemplates = parsedTemplates,
                isSerializable = true,
                constructorParams = constructorParams,
                // Thread the source file so isolated generators can declare a per-file
                // dependency, ensuring KSP only re-runs them when this specific file changes.
                containingFile = classDeclaration.containingFile,
            ),
        )
    }
}

/**
 * Converts a KSP [KSType] to its Kotlin source-code type name, stripping the `kotlin.`
 * prefix from built-in types so the generated code reads naturally (e.g. `String`, not
 * `kotlin.String`).
 */
private fun KSType.toKotlinTypeName(): String {
    val raw = declaration.qualifiedName?.asString() ?: declaration.simpleName.asString()
    val simplified = raw.removePrefix("kotlin.")
    val args = arguments
    return if (args.isEmpty()) {
        simplified
    } else {
        "$simplified<${args.joinToString { it.type?.resolve()?.toKotlinTypeName() ?: "*" }}>"
    }
}
