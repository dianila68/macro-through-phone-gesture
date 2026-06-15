package io.github.dianila68.gesturemacro.core.engine

import io.github.dianila68.gesturemacro.core.sensors.GestureEvent
import io.github.dianila68.gesturemacro.core.sensors.GesturePattern
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ticket-033: Tests for the [Condition] model and [ConditionEvaluator].
 *
 * Covers the motivating use case "light turns off while walking → flashlight"
 * plus basic combinator algebra and state management.
 */
class ConditionTest {

    private fun event(pattern: GesturePattern, t: Long = 0L) =
        GestureEvent(pattern = pattern, t = t, confidence = 1f)

    // ── Leaf conditions ───────────────────────────────────────────────────────

    @Test
    fun `Pattern event leaf fires when pattern in recent events`() {
        val eval = ConditionEvaluator(eventWindowMs = 2_000)
        eval.onEvent(event(GesturePattern.GOING_DARK, t = 1000))
        assertTrue(eval.evaluate(Condition.Pattern(GesturePattern.GOING_DARK), nowMs = 1500))
    }

    @Test
    fun `Pattern event leaf does not fire after window expires`() {
        val eval = ConditionEvaluator(eventWindowMs = 2_000)
        eval.onEvent(event(GesturePattern.GOING_DARK, t = 0))
        assertFalse(eval.evaluate(Condition.Pattern(GesturePattern.GOING_DARK), nowMs = 3_000))
    }

    @Test
    fun `Pattern state guard fires when pattern in active states`() {
        val eval = ConditionEvaluator()
        eval.onEvent(event(GesturePattern.IS_STATIONARY, t = 0))
        val stateGuard = Condition.Pattern(GesturePattern.IS_STATIONARY, isStateGuard = true)
        assertTrue(eval.evaluate(stateGuard, nowMs = 60_000)) // still active 60s later
    }

    // ── Combinator algebra ────────────────────────────────────────────────────

    @Test
    fun `And fires only when all children hold`() {
        val eval = ConditionEvaluator(eventWindowMs = 5_000)
        eval.onEvent(event(GesturePattern.GOING_DARK, t = 1000))

        val condition = Condition.And(
            Condition.Pattern(GesturePattern.GOING_DARK),
            Condition.Pattern(GesturePattern.IS_STATIONARY, isStateGuard = true),
        )
        // Only GOING_DARK fired; IS_STATIONARY state not set — should not fire
        assertFalse(eval.evaluate(condition, nowMs = 2000))

        eval.onEvent(event(GesturePattern.IS_STATIONARY, t = 1500))
        assertTrue(eval.evaluate(condition, nowMs = 2000))
    }

    @Test
    fun `Or fires when any child holds`() {
        val eval = ConditionEvaluator()
        eval.onEvent(event(GesturePattern.SHAKE, t = 0))
        val condition = Condition.Or(
            Condition.Pattern(GesturePattern.SHAKE),
            Condition.Pattern(GesturePattern.FALL),
        )
        assertTrue(eval.evaluate(condition, nowMs = 500))
    }

    @Test
    fun `Not inverts its child`() {
        val eval = ConditionEvaluator()
        val notShake = Condition.Not(Condition.Pattern(GesturePattern.SHAKE))
        assertTrue(eval.evaluate(notShake, nowMs = 0)) // no events → not shake is true
        eval.onEvent(event(GesturePattern.SHAKE, t = 0))
        assertFalse(eval.evaluate(notShake, nowMs = 500))
    }

    // ── Motivating use case: "dark while moving → flashlight" ─────────────────

    @Test
    fun `dark-while-walking composite fires`() {
        // Condition: GOING_DARK event AND (NOT IS_STATIONARY state)
        val condition = Condition.And(
            Condition.Pattern(GesturePattern.GOING_DARK),
            Condition.Not(Condition.Pattern(GesturePattern.IS_STATIONARY, isStateGuard = true)),
        )
        val eval = ConditionEvaluator(eventWindowMs = 3_000)

        // User is walking — steps are incrementing (IS_STATIONARY state cleared)
        eval.onEvent(event(GesturePattern.STEP_DETECTED, t = 500))
        eval.onEvent(event(GesturePattern.STEP_DETECTED, t = 1000))
        // Light turns off
        eval.onEvent(event(GesturePattern.GOING_DARK, t = 1500))

        assertTrue("Should fire: dark + not stationary", eval.evaluate(condition, nowMs = 2000))
    }

    @Test
    fun `dark-while-walking does not fire when stationary`() {
        val condition = Condition.And(
            Condition.Pattern(GesturePattern.GOING_DARK),
            Condition.Not(Condition.Pattern(GesturePattern.IS_STATIONARY, isStateGuard = true)),
        )
        val eval = ConditionEvaluator(eventWindowMs = 3_000)

        // User became stationary
        eval.onEvent(event(GesturePattern.IS_STATIONARY, t = 0))
        // Light turns off (maybe entering pocket in a parked car)
        eval.onEvent(event(GesturePattern.GOING_DARK, t = 1000))

        assertFalse("Should not fire: dark but user is stationary", eval.evaluate(condition, nowMs = 1500))
    }

    // ── State management ──────────────────────────────────────────────────────

    @Test
    fun `GOING_BRIGHT clears GOING_DARK active state`() {
        val eval = ConditionEvaluator()
        eval.onEvent(event(GesturePattern.GOING_DARK, t = 0))
        eval.onEvent(event(GesturePattern.GOING_BRIGHT, t = 1000))

        val dark = Condition.Pattern(GesturePattern.GOING_DARK, isStateGuard = true)
        val bright = Condition.Pattern(GesturePattern.GOING_BRIGHT, isStateGuard = true)
        assertFalse("GOING_DARK state should be cleared", eval.evaluate(dark, nowMs = 5000))
        assertTrue("GOING_BRIGHT state should be active", eval.evaluate(bright, nowMs = 5000))
    }

    @Test
    fun `reset clears all state and events`() {
        val eval = ConditionEvaluator()
        eval.onEvent(event(GesturePattern.SHAKE, t = 0))
        eval.onEvent(event(GesturePattern.IS_STATIONARY, t = 100))
        eval.reset()
        assertFalse(eval.evaluate(Condition.Pattern(GesturePattern.SHAKE), nowMs = 500))
        assertFalse(
            eval.evaluate(
                Condition.Pattern(GesturePattern.IS_STATIONARY, isStateGuard = true),
                nowMs = 500,
            ),
        )
    }
}
