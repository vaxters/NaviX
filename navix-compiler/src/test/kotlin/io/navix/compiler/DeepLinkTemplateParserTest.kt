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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeepLinkTemplateParserTest {
    @Test
    fun parse_singlePathParam_succeeds() {
        val result = DeepLinkTemplateParser.parse("myapp://product/{productId}")
        assertTrue(result.isSuccess)
        val parsed = result.getOrThrow()
        assertEquals(listOf("productId"), parsed.params)
    }

    @Test
    fun parse_multipleParams_extractsAllParams() {
        val result = DeepLinkTemplateParser.parse("myapp://shop/{category}/{itemId}")
        assertTrue(result.isSuccess)
        val parsed = result.getOrThrow()
        assertEquals(listOf("category", "itemId"), parsed.params)
    }

    @Test
    fun parse_noParams_returnsEmptyParamList() {
        val result = DeepLinkTemplateParser.parse("myapp://home")
        assertTrue(result.isSuccess)
        assertEquals(emptyList(), result.getOrThrow().params)
    }

    @Test
    fun parse_missingScheme_returnsFailure() {
        val result = DeepLinkTemplateParser.parse("not-a-uri")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("valid URI scheme"))
    }

    @Test
    fun parse_duplicateParams_returnsFailure() {
        val result = DeepLinkTemplateParser.parse("myapp://item/{id}/detail/{id}")
        assertTrue(result.isFailure)
        assertEquals(result.exceptionOrNull()?.message?.contains("duplicate"), true)
    }

    @Test
    fun extractParams_matchingUri_returnsCorrectValues() {
        val parsed = DeepLinkTemplateParser.parse("myapp://product/{productId}").getOrThrow()
        val params = DeepLinkTemplateParser.extractParams(parsed, "myapp://product/42")
        assertNotNull(params)
        assertEquals("42", params["productId"])
    }

    @Test
    fun extractParams_nonMatchingUri_returnsNull() {
        val parsed = DeepLinkTemplateParser.parse("myapp://product/{productId}").getOrThrow()
        val params = DeepLinkTemplateParser.extractParams(parsed, "myapp://other/42")
        assertNull(params)
    }

    @Test
    fun extractParams_multipleParams_extractsAllCorrectly() {
        val parsed = DeepLinkTemplateParser.parse("myapp://shop/{category}/{itemId}").getOrThrow()
        val params = DeepLinkTemplateParser.extractParams(parsed, "myapp://shop/electronics/999")
        assertNotNull(params)
        assertEquals("electronics", params["category"])
        assertEquals("999", params["itemId"])
    }
}
