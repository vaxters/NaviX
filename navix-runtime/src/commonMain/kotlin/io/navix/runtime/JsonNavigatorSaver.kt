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
import io.navix.contracts.Route
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/**
 * [NavigatorSaver] backed by [kotlinx.serialization] JSON.
 *
 * Concrete [Route] subclasses must be registered in [routesModule] so that
 * [kotlinx.serialization.PolymorphicSerializer] can map the JSON `"type"` discriminator field
 * back to the correct class. Use the KSP-generated `${Module}NavixSerializersModule` as the
 * [routesModule] — the Navix compiler generates this from every class annotated with
 * `@RouteDestination`.
 *
 * ### Usage
 * ```kotlin
 * val saver = JsonNavigatorSaver(AppNavixSerializersModule)
 * val navigator = rememberSaveableNavigator(root = Home, saver = saver) { ... }
 * ```
 *
 * ### Failure handling
 * [restore] returns `null` on any [kotlinx.serialization.SerializationException] or
 * [IllegalArgumentException]. The caller falls back to a fresh navigator rooted at the
 * original `root` route.
 *
 * @param routesModule A [SerializersModule] that registers all concrete [Route] subclasses
 *   under `polymorphic(Route::class)`. Typically the KSP-generated module.
 * @param prettyPrint When `true`, the saved JSON is human-readable (useful during development).
 *   Defaults to `false` for compact on-disk size.
 */
class JsonNavigatorSaver(
    routesModule: SerializersModule,
    prettyPrint: Boolean = false,
) : NavigatorSaver {
    private val json =
        Json {
            this.serializersModule =
                SerializersModule {
                    include(routesModule)
                    // Re-declare the polymorphic base so that Route subclasses registered in
                    // routesModule are discoverable when decoding RouteEntry.route.
                    polymorphic(Route::class) {}
                }
            this.prettyPrint = prettyPrint
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    override fun save(snapshot: BackstackSnapshot): ByteArray =
        json.encodeToString(BackstackSnapshot.serializer(), snapshot).encodeToByteArray()

    override fun restore(bytes: ByteArray): BackstackSnapshot? =
        runCatching {
            json.decodeFromString(BackstackSnapshot.serializer(), bytes.decodeToString())
        }.getOrNull()
}
