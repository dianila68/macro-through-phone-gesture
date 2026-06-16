package io.github.dianila68.gesturemacro.core.actions

import io.github.dianila68.gesturemacro.core.serialization.GestureMacro

/**
 * ticket-056: Simple template variable substitution for webhook/MQTT payloads.
 */
fun expandTemplate(template: String, macro: GestureMacro, firedAtMs: Long): String =
    template
        .replace("{{macro.id}}", macro.id)
        .replace("{{macro.name}}", macro.name)
        .replace("{{fired_at_ms}}", firedAtMs.toString())
        .replace("{{trigger.pattern}}", macro.trigger.pattern.name.lowercase())
