package io.github.dianila68.gesturemacro.core.engine

import io.github.dianila68.gesturemacro.core.sensors.GestureEvent
import io.github.dianila68.gesturemacro.core.sensors.GesturePattern
import io.github.dianila68.gesturemacro.core.serialization.Constraints
import io.github.dianila68.gesturemacro.core.serialization.GestureMacro
import io.github.dianila68.gesturemacro.core.serialization.PatternKind
import io.github.dianila68.gesturemacro.core.serialization.ScreenState
import io.github.dianila68.gesturemacro.core.serialization.SensorKind
import io.github.dianila68.gesturemacro.core.serialization.SystemToggleAction
import io.github.dianila68.gesturemacro.core.serialization.TimeWindow
import io.github.dianila68.gesturemacro.core.serialization.Trigger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MacroEngineTest {

    private var now = 0L
    private var screen: Boolean? = null
    private val engine = MacroEngine(clock = { now }, screenOn = { screen })

    private fun macro(
        id: String = "m1",
        pattern: PatternKind = PatternKind.SHAKE,
        enabled: Boolean = true,
        cooldownMs: Long = 2_000,
        constraints: Constraints = Constraints(),
    ) = GestureMacro(
        version = 1,
        id = id,
        name = "test $id",
        enabled = enabled,
        trigger = Trigger(sensor = SensorKind.ACCELEROMETER, pattern = pattern, cooldownMs = cooldownMs),
        constraints = constraints,
        actions = listOf(SystemToggleAction(target = "flashlight")),
    )

    private fun shakeAt(t: Long) = GestureEvent(GesturePattern.SHAKE, t, 1f)

    @Test
    fun `matching pattern fires the macro`() {
        val fired = engine.match(shakeAt(0), listOf(macro()))
        assertEquals(listOf("m1"), fired.map { it.id })
    }

    @Test
    fun `non matching pattern does not fire`() {
        val fired = engine.match(shakeAt(0), listOf(macro(pattern = PatternKind.FLIP_FACE_DOWN)))
        assertTrue(fired.isEmpty())
    }

    @Test
    fun `disabled macro never fires`() {
        assertTrue(engine.match(shakeAt(0), listOf(macro(enabled = false))).isEmpty())
    }

    @Test
    fun `cooldown suppresses refire until elapsed`() {
        val m = macro(cooldownMs = 2_000)
        now = 1_000
        assertEquals(1, engine.match(shakeAt(now), listOf(m)).size)
        now = 2_500
        assertTrue(engine.match(shakeAt(now), listOf(m)).isEmpty())
        now = 3_000
        assertEquals(1, engine.match(shakeAt(now), listOf(m)).size)
    }

    @Test
    fun `screen constraint fails closed when state is unknown`() {
        val m = macro(constraints = Constraints(screenState = ScreenState.OFF))
        screen = null
        assertTrue(engine.match(shakeAt(0), listOf(m)).isEmpty())
        screen = false
        assertEquals(1, engine.match(shakeAt(0), listOf(m)).size)
    }

    @Test
    fun `screen on constraint respects actual state`() {
        val m = macro(constraints = Constraints(screenState = ScreenState.ON))
        screen = true
        assertEquals(1, engine.match(shakeAt(0), listOf(m)).size)
        screen = false
        assertTrue(engine.match(shakeAt(now), listOf(m)).isEmpty())
    }

    @Test
    fun `time window allows inside and blocks outside`() {
        val m = macro(constraints = Constraints(timeWindow = TimeWindow("08:00", "17:00")))
        now = minutesToEpoch(9 * 60)
        assertEquals(1, engine.match(shakeAt(now), listOf(m)).size)
        engine.reset()
        now = minutesToEpoch(18 * 60)
        assertTrue(engine.match(shakeAt(now), listOf(m)).isEmpty())
    }

    @Test
    fun `overnight time window wraps midnight`() {
        val m = macro(constraints = Constraints(timeWindow = TimeWindow("22:00", "06:00")))
        now = minutesToEpoch(23 * 60)
        assertEquals(1, engine.match(shakeAt(now), listOf(m)).size)
        engine.reset()
        now = minutesToEpoch(3 * 60)
        assertEquals(1, engine.match(shakeAt(now), listOf(m)).size)
        engine.reset()
        now = minutesToEpoch(12 * 60)
        assertTrue(engine.match(shakeAt(now), listOf(m)).isEmpty())
    }

    @Test
    fun `independent macros both fire on one event`() {
        val fired = engine.match(shakeAt(0), listOf(macro(id = "a"), macro(id = "b")))
        assertEquals(listOf("a", "b"), fired.map { it.id })
    }

    private fun minutesToEpoch(minuteOfDay: Int): Long = minuteOfDay * 60_000L
}
