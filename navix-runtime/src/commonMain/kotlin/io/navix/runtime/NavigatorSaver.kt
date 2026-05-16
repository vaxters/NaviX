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
package io.navix.runtime

import io.navix.contracts.BackstackSnapshot

/**
 * Serialises and deserialises a [BackstackSnapshot] so the navigation state can survive
 * process death and Activity recreation.
 *
 * Navix ships [JsonNavigatorSaver] as its default implementation. Custom savers can be
 * supplied when the application already uses an alternative serialization format or when
 * additional per-entry data must be included in the saved state.
 *
 * ### Contract
 * - [save] must produce a non-empty [ByteArray] representing [snapshot].
 * - [restore] receives bytes previously produced by [save] and returns the recovered
 *   [BackstackSnapshot], or `null` if the bytes are invalid or incompatible (e.g. after
 *   a schema-breaking migration). A `null` return causes the caller to fall back to a
 *   fresh navigator rooted at the original `root` route.
 */
interface NavigatorSaver {
    /**
     * Serialises [snapshot] to bytes.
     */
    fun save(snapshot: BackstackSnapshot): ByteArray

    /**
     * Deserialises [bytes] back to a [BackstackSnapshot], or `null` on failure.
     */
    fun restore(bytes: ByteArray): BackstackSnapshot?
}
