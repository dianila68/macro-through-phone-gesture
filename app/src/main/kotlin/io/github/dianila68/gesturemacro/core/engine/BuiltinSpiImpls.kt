package io.github.dianila68.gesturemacro.core.engine

import io.github.dianila68.gesturemacro.core.actions.ActionExecutor
import io.github.dianila68.gesturemacro.core.security.IntegritySealer
import io.github.dianila68.gesturemacro.core.security.KeystoreSealerFactory
import io.github.dianila68.gesturemacro.core.sensors.GestureDetector
import io.github.dianila68.gesturemacro.core.triggers.TriggerLibrary

/**
 * ticket-021/038: Built-in SPI implementations backing the engine seam.
 * All executor parameters are typed as [ActionExecutor] — callers supply
 * the concrete android.actions implementations; the registry is agnostic.
 */
class BuiltinTriggerCatalog : TriggerCatalogSpi {
    override fun detectors(sensitivity: Float): List<GestureDetector> =
        TriggerLibrary.detectors(sensitivity)
}

/**
 * ticket-038: Executor map wired at service startup; [ActionDispatcher] looks up
 * executors by [MacroAction.actionType] key — no when-expression in the dispatcher.
 */
class BuiltinExecutorRegistry(
    private val flashlight: ActionExecutor,
    private val mediaControl: ActionExecutor,
    private val intent: ActionExecutor,
    private val accessibility: ActionExecutor,
    private val sound: ActionExecutor,
    private val locationAlert: ActionExecutor,
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
