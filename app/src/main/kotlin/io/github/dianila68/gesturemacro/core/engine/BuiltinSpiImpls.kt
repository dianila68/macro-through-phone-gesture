package io.github.dianila68.gesturemacro.core.engine

import io.github.dianila68.gesturemacro.core.actions.AccessibilityExecutor
import io.github.dianila68.gesturemacro.core.actions.ActionExecutor
import io.github.dianila68.gesturemacro.core.actions.FlashlightExecutor
import io.github.dianila68.gesturemacro.core.actions.IntentExecutor
import io.github.dianila68.gesturemacro.core.actions.LocationAlertExecutor
import io.github.dianila68.gesturemacro.core.actions.MediaControlExecutor
import io.github.dianila68.gesturemacro.core.actions.SoundExecutor
import io.github.dianila68.gesturemacro.core.security.IntegritySealer
import io.github.dianila68.gesturemacro.core.security.KeystoreSealerFactory
import io.github.dianila68.gesturemacro.core.sensors.GestureDetector
import io.github.dianila68.gesturemacro.core.triggers.TriggerLibrary

/**
 * ticket-021: Built-in implementations of the engine SPI, delegating to the
 * existing concrete classes. No behaviour change — these back the seam so the
 * module split (ticket-024) is mechanical.
 */
class BuiltinTriggerCatalog : TriggerCatalogSpi {
    override fun detectors(sensitivity: Float): List<GestureDetector> =
        TriggerLibrary.detectors(sensitivity)
}

/** Executor wiring: maps each action [type] @SerialName to its executor instance. */
class BuiltinExecutorRegistry(
    private val flashlight: FlashlightExecutor,
    private val mediaControl: MediaControlExecutor,
    private val intent: IntentExecutor,
    private val accessibility: AccessibilityExecutor,
    private val sound: SoundExecutor,
    private val locationAlert: LocationAlertExecutor,
) : ExecutorRegistrySpi {
    override fun executors(): Map<String, ActionExecutor> = mapOf(
        "system_toggle" to flashlight,
        "media_control" to mediaControl,
        "intent" to intent,
        "accessibility" to accessibility,
        "play_sound" to sound,
        "location_alert" to locationAlert,
    )
}

class KeystoreSealerProvider : SealerProviderSpi {
    override fun create(): IntegritySealer? = KeystoreSealerFactory.create()
}
