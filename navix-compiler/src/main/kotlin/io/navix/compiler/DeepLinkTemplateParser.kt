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

/**
 * Parses and validates deep link URI templates of the form:
 *   `scheme://host/path/{param}?query={queryParam}`
 *
 * Extracts parameter names and builds a [Regex] for runtime URI matching.
 *
 * Validation errors are returned as [Result.failure] with a descriptive message
 * so the KSP processor can emit meaningful compile-time errors.
 */
internal object DeepLinkTemplateParser {
    private val PARAM_REGEX = Regex("\\{([^}]+)}")
    private val VALID_SCHEME = Regex("^[a-zA-Z][a-zA-Z0-9+\\-.]*://.*")

    data class ParsedTemplate(
        val original: String,
        val params: List<String>,
        val matchingRegex: Regex,
    )

    fun parse(template: String): Result<ParsedTemplate> {
        if (!VALID_SCHEME.matches(template)) {
            return Result.failure(
                IllegalArgumentException(
                    "Deep link template '$template' must start with a valid URI scheme (e.g. 'myapp://...').",
                ),
            )
        }

        val params = PARAM_REGEX.findAll(template).map { it.groupValues[1] }.toList()
        val duplicates =
            params
                .groupingBy { it }
                .eachCount()
                .filter { it.value > 1 }
                .keys
        if (duplicates.isNotEmpty()) {
            return Result.failure(
                IllegalArgumentException(
                    "Deep link template '$template' has duplicate parameter names: $duplicates",
                ),
            )
        }

        return Result.success(
            ParsedTemplate(
                original = template,
                params = params,
                matchingRegex =
                    Regex(
                        PARAM_REGEX.replace(template) { match ->
                            "(?<${match.groupValues[1]}>[^/?&]+)"
                        },
                    ),
            ),
        )
    }

    /** Extracts named group values from a URI matched against a [ParsedTemplate]. */
    fun extractParams(
        template: ParsedTemplate,
        uri: String,
    ): Map<String, String>? {
        val match = template.matchingRegex.find(uri) ?: return null
        return template.params.associateWith { param ->
            match.groups[param]?.value ?: return null
        }
    }
}
