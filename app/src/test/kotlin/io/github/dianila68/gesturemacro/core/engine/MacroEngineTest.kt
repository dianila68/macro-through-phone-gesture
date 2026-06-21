package io.github.dianila68.gesturemacro.core.engine

import io.github.dianila68.gesturemacro.core.sensors.GestureEvent
import io.github.dianila68.gesturemacro.core.sensors.GesturePattern
import io.github.dianila68.gesturemacro.core.serialization.*
import org.junit.Assert.*
import org.junit.Test

class MacroEngineTest {

    // ----- helpers -----

    private var screenOn: Boolean? = null
    private var clockMs: Long = 0L

    private fun engine(
        conditionWindowMs: Long = 2_000L,
    ) = MacroEngine(
        clock = { clockMs },
        screenOn = { screenOn },
        conditionWindowMs = conditionWindowMs,
    )

    private fun event(
        pattern: GesturePattern = GesturePattern.SHAKE,
        t: Long = clockMs,
    ) = GestureEvent(pattern, t, 1f)

    private fun macro(
        id: String = "m1",
        pattern: PatternKind = PatternKind.SHAKE,
        enabled: Boolean = true,
        cooldownMs: Long = 0L,
        screenState: ScreenState = ScreenState.ANY,
        timeWindow: TimeWindow? = null,
        condition: Condition? = null,
    ) = GestureMacro(
        version = 1,
        id = id,
        name = "Test $id",
        enabled = enabled,
        trigger = Trigger(
            sensor = SensorKind.ACCELEROMETER,
            pattern = pattern,
            cooldownMs = cooldownMs,
        ),
        constraints = Constraints(screenState = screenState, timeWindow = timeWindow),
        actions = listOf(IntentAction(target = "pkg", command = "launch")),
        condition = condition,
    )

    // ----- basic matching -----

    @Test
    fun `empty macro list returns empty`() {
        val eng = engine()
        assertTrue(eng.match(event(), emptyList()).isEmpty())
    }

    @Test
    fun `matching enabled macro is returned`() {
        val eng = engine()
        val result = eng.match(event(GesturePattern.SHAKE), listOf(macro(pattern = PatternKind.SHAKE)))
        assertEquals(1, result.size)
    }

    @Test
    fun `disabled macro is never returned`() {
        val eng = engine()
        val result = eng.match(event(GesturePattern.SHAKE), listOf(macro(enabled = false)))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `wrong pattern is not matched`() {
        val eng = engine()
        val result = eng.match(event(GesturePattern.SHAKE), listOf(macro(pattern = PatternKind.FLIP_FACE_DOWN)))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `multiple macros — only matching pattern fires`() {
        val eng = engine()
        val m1 = macro(id = "m1", pattern = PatternKind.SHAKE)
        val m2 = macro(id = "m2", pattern = PatternKind.TWIST)
        val result = eng.match(event(GesturePattern.SHAKE), listOf(m1, m2))
        assertEquals(1, result.size)
        assertEquals("m1", result[0].id)
    }

    // ----- cooldown -----

    @Test
    fun `cooldown prevents re-fire within window`() {
        val eng = engine()
        val m = macro(cooldownMs = 2_000L)
        clockMs = 0L
        eng.match(event(), listOf(m)) // first fire
        clockMs = 500L
        val second = eng.match(event(), listOf(m))
        assertTrue(second.isEmpty())
    }

    @Test
    fun `cooldown allows re-fire after window elapses`() {
        val eng = engine()
        val m = macro(cooldownMs = 2_000L)
        clockMs = 0L
        eng.match(event(), listOf(m)) // first fire
        clockMs = 3_000L
        val second = eng.match(event(), listOf(m))
        assertEquals(1, second.size)
    }

    @Test
    fun `zero cooldown allows consecutive fires`() {
        val eng = engine()
        val m = macro(cooldownMs = 0L)
        clockMs = 0L
        eng.match(event(), listOf(m))
        clockMs = 1L
        val second = eng.match(event(), listOf(m))
        assertEquals(1, second.size)
    }

    // ----- screen state -----

    @Test
    fun `screen ANY matches regardless of screen state`() {
        val eng = engine()
        screenOn = false
        assertTrue(eng.match(event(), listOf(macro(screenState = ScreenState.ANY))).isNotEmpty())
        screenOn = true
        assertTrue(eng.match(event(), listOf(macro(screenState = ScreenState.ANY))).isNotEmpty())
    }

    @Test
    fun `screen ON allows when screen is on`() {
        screenOn = true
        val eng = engine()
        assertTrue(eng.match(event(), listOf(macro(screenState = ScreenState.ON))).isNotEmpty())
    }

    @Test
    fun `screen ON blocks when screen is off`() {
        screenOn = false
        val eng = engine()
        assertTrue(eng.match(event(), listOf(macro(screenState = ScreenState.ON))).isEmpty())
    }

    @Test
    fun `screen OFF allows when screen is off`() {
        screenOn = false
        val eng = engine()
        assertTrue(eng.match(event(), listOf(macro(screenState = ScreenState.OFF))).isNotEmpty())
    }

    @Test
    fun `screen OFF blocks when screen is on`() {
        screenOn = true
        val eng = engine()
        assertTrue(eng.match(event(), listOf(macro(screenState = ScreenState.OFF))).isEmpty())
    }

    // ----- time window -----
    // clockMs = 0 → minute-of-day = 0 (00:00 UTC)

    @Test
    fun `time window includes current time`() {
        clockMs = 0L  // 00:00
        val eng = engine()
        val m = macro(timeWindow = TimeWindow("00:00", "06:00"))
        assertTrue(eng.match(event(), listOf(m)).isNotEmpty())
    }

    @Test
    fun `time window excludes current time`() {
        clockMs = 0L  // 00:00
        val eng = engine()
        val m = macro(timeWindow = TimeWindow("08:00", "22:00"))
        assertTrue(eng.match(event(), listOf(m)).isEmpty())
    }

    @Test
    fun `overnight time window includes early morning`() {
        clockMs = 0L  // 00:00 is inside 23:00..01:00
        val eng = engine()
        val m = macro(timeWindow = TimeWindow("23:00", "01:00"))
        assertTrue(eng.match(event(), listOf(m)).isNotEmpty())
    }

    // ----- condition gate -----

    @Test
    fun `condition gate returns true when no condition set`() {
        val eng = engine()
        assertTrue(eng.match(event(), listOf(macro(condition = null))).isNotEmpty())
    }

    @Test
    fun `pattern condition blocks before matching event seen`() {
        val eng = engine()
        val gate = Condition.Pattern(GesturePattern.GOING_DARK, isStateGuard = false)
        val m = macro(condition = gate)
        // No GOING_DARK event — condition should fail
        val result = eng.match(event(GesturePattern.SHAKE), listOf(m))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `pattern condition passes after matching event seen`() {
        val eng = engine(conditionWindowMs = 5_000L)
        val gate = Condition.Pattern(GesturePattern.GOING_DARK, isStateGuard = false)
        val m = macro(condition = gate)
        clockMs = 0L
        eng.match(event(GesturePattern.GOING_DARK), listOf(m)) // seeds condition state
        clockMs = 100L
        val result = eng.match(event(GesturePattern.SHAKE), listOf(m))
        assertEquals(1, result.size)
    }

    @Test
    fun `state guard condition persists beyond event window`() {
        val eng = engine(conditionWindowMs = 500L)
        val gate = Condition.Pattern(GesturePattern.GOING_DARK, isStateGuard = true)
        val m = macro(condition = gate)
        clockMs = 0L
        eng.match(event(GesturePattern.GOING_DARK), listOf(m)) // seeds active state
        clockMs = 2_000L  // event window long expired; active state persists
        val result = eng.match(event(GesturePattern.SHAKE), listOf(m))
        assertEquals(1, result.size)
    }

    // ----- reset -----

    @Test
    fun `reset clears cooldown so same macro can fire immediately`() {
        val eng = engine()
        val m = macro(cooldownMs = 60_000L)
        clockMs = 0L
        eng.match(event(), listOf(m))
        eng.reset()
        clockMs = 1L
        val result = eng.match(event(), listOf(m))
        assertEquals(1, result.size)
    }

    @Test
    fun `reset clears condition state`() {
        val eng = engine(conditionWindowMs = 60_000L)
        val gate = Condition.Pattern(GesturePattern.GOING_DARK, isStateGuard = true)
        val m = macro(condition = gate)
        clockMs = 0L
        eng.match(event(GesturePattern.GOING_DARK), listOf(m)) // seeds state
        eng.reset()
        clockMs = 1L
        val result = eng.match(event(GesturePattern.SHAKE), listOf(m))
        assertTrue(result.isEmpty())  // state guard cleared by reset
    }
}
