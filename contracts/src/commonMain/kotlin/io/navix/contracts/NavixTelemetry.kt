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
package io.navix.contracts

/**
 * Minimal telemetry interface kept in [contracts] so that [io.navix.runtime.Navigator]
 * can accept it without creating a dependency on [navix-telemetry].
 *
 * The full pipeline implementation ([io.navix.telemetry.NavixTelemetryPipeline]) lives
 * in the `navix-telemetry` module and implements this interface.
 */
interface NavixTelemetry {
    fun onEvent(event: NavEvent)

    companion object {
        val NoOp: NavixTelemetry = object : NavixTelemetry {
            override fun onEvent(event: NavEvent) = Unit
        }
    }
}
