package io.github.dianila68.gesturemacro.core.actions

import io.github.dianila68.gesturemacro.core.serialization.*
import org.junit.Assert.assertEquals
import org.junit.Test

class ActionTemplatesTest {
    private val macro = GestureMacro(
        version = 1,
        id = "m1",
        name = "Test Macro",
        trigger = Trigger(sensor = SensorKind.ACCELEROMETER, pattern = PatternKind.SHAKE),
        actions = listOf(SystemToggleAction("wifi")),
    )

    @Test
    fun expandTemplate_substitutesAllVars() {
        val result = expandTemplate(
            "macro={{macro.id}} name={{macro.name}} t={{fired_at_ms}} p={{trigger.pattern}}",
            macro,
            firedAtMs = 12345L,
        )
        assertEquals("macro=m1 name=Test Macro t=12345 p=shake", result)
    }

    @Test
    fun expandTemplate_unknownVarsPassThrough() {
        val result = expandTemplate("{{unknown}}", macro, 0L)
        assertEquals("{{unknown}}", result)
    }
}
