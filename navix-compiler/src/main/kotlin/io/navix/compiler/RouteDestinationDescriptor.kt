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

import com.google.devtools.ksp.symbol.KSFile

/**
 * Intermediate representation of a discovered [@RouteDestination] annotated class,
 * built by [RouteDestinationVisitor] and consumed by the code generators.
 *
 * [containingFile] is the [KSFile] that declares this class. It is used by
 * [DeepLinkHandlerGenerator] to register the generated file as an *isolated* output
 * (only regenerated when this specific source file changes) rather than an aggregating
 * output (regenerated on any source change). Null when constructed in unit tests that
 * don't have access to a real KSP compilation unit.
 */
internal data class RouteDestinationDescriptor(
    val packageName: String,
    val className: String,
    val canonicalRoute: String,
    val deepLinkTemplates: List<DeepLinkTemplateParser.ParsedTemplate>,
    val isSerializable: Boolean,
    /** Primary constructor parameters, empty for `data object` routes. */
    val constructorParams: List<ConstructorParam> = emptyList(),
    /**
     * The KSP source file containing this class, used for incremental dependency tracking.
     * Null in unit-test contexts that construct descriptors directly.
     */
    val containingFile: KSFile? = null
) {
    val fullyQualifiedName: String get() = "$packageName.$className"
}

/** A single primary-constructor parameter extracted from a route class. */
internal data class ConstructorParam(
    val name: String,
    /** Kotlin type name (e.g. `String`, `Int`, `kotlin.collections.List<String>`). */
    val typeName: String
)
