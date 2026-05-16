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
package io.navix.telemetry

import io.navix.contracts.NavEvent

/**
 * Receives [NavEvent]s from [NavixTelemetryPipeline] for export to an external system.
 *
 * Implement this interface to forward navigation events to Firebase Analytics, Amplitude,
 * OpenTelemetry collectors, a custom logging backend, or any other sink.
 *
 * [export] is called on the background dispatcher specified in [NavixTelemetryPipeline].
 * Implementations may perform blocking I/O safely inside this call.
 *
 * The pipeline guarantees that each event is delivered to every registered exporter,
 * in registration order, on the pipeline's dispatcher. Exporter exceptions are caught
 * and logged to prevent one failing exporter from blocking others.
 */
interface NavEventExporter {
    fun export(event: NavEvent)
}
