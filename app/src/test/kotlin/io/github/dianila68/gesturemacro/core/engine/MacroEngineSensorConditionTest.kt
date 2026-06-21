package io.github.dianila68.gesturemacro.core.engine

import io.github.dianila68.gesturemacro.core.sensors.GestureEvent
import io.github.dianila68.gesturemacro.core.sensors.GesturePattern
import io.github.dianila68.gesturemacro.core.serialization.*
import org.junit.Assert.*
import org.junit.Test

class MacroEngineSensorConditionTest {

    private var clockMs: Long = 0L

    private fun engine(sensorWindowMs: Long = 10_000L) = MacroEngine(
        clock = { clockMs },
        screenOn = { null },
        sensorWindowMs = sensorWindowMs,
    )

    private fun event(pattern: GesturePattern) = GestureEvent(pattern, clockMs, 1f)

    private fun macro(sensorConditions: List<SensorCondition> = emptyList()) = GestureMacro(
        version = 1,
        id = "m1",
        name = "Test",
        trigger = Trigger(SensorKind.ACCELEROMETER, PatternKind.SHAKE, cooldownMs = 0),
        actions = listOf(IntentAction("pkg", "launch")),
        sensorConditions = sensorConditions,
    )

    private fun shakeEvent() = event(GesturePattern.SHAKE)

    // ---- backwards compatibility ----

    @Test
    fun `empty sensorConditions list always allows dispatch`() {
        val eng = engine()
        assertTrue(eng.match(shakeEvent(), listOf(macro(emptyList()))).isNotEmpty())
    }

    // ---- IsStationary ----

    @Test
    fun `IsStationary blocks before IS_STATIONARY event`() {
        val eng = engine()
        val m = macro(listOf(SensorCondition.IsStationary))
        assertTrue(eng.match(shakeEvent(), listOf(m)).isEmpty())
    }

    @Test
    fun `IsStationary allows after IS_STATIONARY event`() {
        val eng = engine()
        val m = macro(listOf(SensorCondition.IsStationary))
        eng.match(event(GesturePattern.IS_STATIONARY), listOf(m))
        clockMs = 500L
        assertTrue(eng.match(shakeEvent(), listOf(m)).isNotEmpty())
    }

    // ---- WasPickedUp ----

    @Test
    fun `WasPickedUp blocks before PICKED_UP event`() {
        val eng = engine()
        val m = macro(listOf(SensorCondition.WasPickedUp))
        assertTrue(eng.match(shakeEvent(), listOf(m)).isEmpty())
    }

    @Test
    fun `WasPickedUp allows after PICKED_UP event`() {
        val eng = engine()
        val m = macro(listOf(SensorCondition.WasPickedUp))
        eng.match(event(GesturePattern.PICKED_UP), listOf(m))
        clockMs = 500L
        assertTrue(eng.match(shakeEvent(), listOf(m)).isNotEmpty())
    }

    // ---- WentDark (time-bounded) ----

    @Test
    fun `WentDark blocks when GOING_DARK event outside withinMs`() {
        val eng = engine(sensorWindowMs = 30_000L)
        val m = macro(listOf(SensorCondition.WentDark(withinMs = 1_000L)))
        clockMs = 0L
        eng.match(event(GesturePattern.GOING_DARK), listOf(m))
        clockMs = 5_000L  // 5 s later — beyond withinMs of 1 s
        assertTrue(eng.match(shakeEvent(), listOf(m)).isEmpty())
    }

    @Test
    fun `WentDark allows when GOING_DARK event within withinMs`() {
        val eng = engine()
        val m = macro(listOf(SensorCondition.WentDark(withinMs = 3_000L)))
        clockMs = 0L
        eng.match(event(GesturePattern.GOING_DARK), listOf(m))
        clockMs = 500L
        assertTrue(eng.match(shakeEvent(), listOf(m)).isNotEmpty())
    }

    // ---- All / Any / Not ----

    @Test
    fun `All requires all conditions`() {
        val eng = engine()
        val m = macro(listOf(SensorCondition.All(listOf(
            SensorCondition.IsStationary,
            SensorCondition.WasPickedUp,
        ))))
        eng.match(event(GesturePattern.IS_STATIONARY), listOf(m))
        clockMs = 100L
        // Only IsStationary met — should still block
        assertTrue(eng.match(shakeEvent(), listOf(m)).isEmpty())
        eng.match(event(GesturePattern.PICKED_UP), listOf(m))
        clockMs = 200L
        // Both met now
        assertTrue(eng.match(shakeEvent(), listOf(m)).isNotEmpty())
    }

    @Test
    fun `Any requires at least one condition`() {
        val eng = engine()
        val m = macro(listOf(SensorCondition.Any(listOf(
            SensorCondition.IsStationary,
            SensorCondition.WasPickedUp,
        ))))
        // Meet only IsStationary
        eng.match(event(GesturePattern.IS_STATIONARY), listOf(m))
        clockMs = 100L
        assertTrue(eng.match(shakeEvent(), listOf(m)).isNotEmpty())
    }

    @Test
    fun `Not(IsStationary) blocks when device is stationary`() {
        val eng = engine()
        val m = macro(listOf(SensorCondition.Not(SensorCondition.IsStationary)))
        eng.match(event(GesturePattern.IS_STATIONARY), listOf(m))
        clockMs = 100L
        assertTrue(eng.match(shakeEvent(), listOf(m)).isEmpty())
    }

    @Test
    fun `Not(IsStationary) allows when device is moving`() {
        val eng = engine()
        val m = macro(listOf(SensorCondition.Not(SensorCondition.IsStationary)))
        // No IS_STATIONARY event — Not(IsStationary) should be true
        assertTrue(eng.match(shakeEvent(), listOf(m)).isNotEmpty())
    }

    // ---- reset ----

    @Test
    fun `reset clears sensor window so previously met condition no longer holds`() {
        val eng = engine()
        val m = macro(listOf(SensorCondition.IsStationary))
        eng.match(event(GesturePattern.IS_STATIONARY), listOf(m))
        clockMs = 100L
        eng.reset()
        clockMs = 200L
        assertTrue(eng.match(shakeEvent(), listOf(m)).isEmpty())
    }
}
