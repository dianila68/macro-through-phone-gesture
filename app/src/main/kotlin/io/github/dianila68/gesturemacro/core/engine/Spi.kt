package io.github.dianila68.gesturemacro.core.engine

import io.github.dianila68.gesturemacro.core.actions.ActionExecutor
import io.github.dianila68.gesturemacro.core.security.IntegritySealer
import io.github.dianila68.gesturemacro.core.sensors.GestureDetector
import io.github.dianila68.gesturemacro.core.sensors.SensorStream

/**
 * ticket-021: Engine SPI seam (ADR-0003 step 1).
 *
 * These interfaces define the dependency boundary between a pure-JVM engine core
 * (`:engine`, eventually open-sourced) and platform-specific implementations in
 * `:engine-android` and `:app`. No behaviour change; existing classes back these
 * interfaces so the split is mechanical when it happens.
 *
 * All SPI types are `public`; everything that is not part of the seam stays `internal`.
 */

/**
 * Provides the set of gesture detectors the capture service should run.
 * The engine consults this at startup so new detectors become available by
 * registering here, not by modifying the service.
 */
interface TriggerCatalogSpi {
    fun detectors(sensitivity: Float): List<GestureDetector>
}

/**
 * Maps action type tags (the `@SerialName` of each [MacroAction] subclass)
 * to the executor that can run them. The engine dispatches via this map so
 * adding a new action type only requires registering a new executor here.
 */
interface ExecutorRegistrySpi {
    fun executors(): Map<String, ActionExecutor>
}

/**
 * Provides (or withholds) an [IntegritySealer] instance. Returns null when the
 * platform keystore is unavailable (emulator cold-start, first boot).
 */
interface SealerProviderSpi {
    fun create(): IntegritySealer?
}

/**
 * Configuration bundle assembled by `:app` and passed into engine entry points.
 * Acts as a dependency-injection root for the pure-JVM engine layer so the engine
 * never reaches up into Android framework types.
 */
data class EngineConfig(
    val sensorStream: SensorStream,
    val executorRegistry: ExecutorRegistrySpi,
    val triggerCatalog: TriggerCatalogSpi,
    val sealerProvider: SealerProviderSpi,
)
