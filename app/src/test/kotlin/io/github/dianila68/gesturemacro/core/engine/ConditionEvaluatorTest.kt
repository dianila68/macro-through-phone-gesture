package io.github.dianila68.gesturemacro.core.engine

import io.github.dianila68.gesturemacro.core.sensors.GestureEvent
import io.github.dianila68.gesturemacro.core.sensors.GesturePattern
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConditionEvaluatorTest {

    private fun event(pattern: GesturePattern, t: Long) =
        GestureEvent(pattern, t, confidence = 1f)

    // --- Empty evaluator ---

    @Test fun empty_noPatternMatches() {
        val ev = ConditionEvaluator()
        assertFalse(ev.evaluate(Condition.Pattern(GesturePattern.SHAKE), nowMs = 1_000L))
        assertFalse(ev.evaluate(Condition.Pattern(GesturePattern.IS_STATIONARY, isStateGuard = true), nowMs = 1_000L))
    }

    // --- Pattern (event, not state guard) ---

    @Test fun pattern_event_trueWhenRecentEventPresent() {
        val ev = ConditionEvaluator(eventWindowMs = 3_000L)
        ev.onEvent(event(GesturePattern.SHAKE, t = 0L))
        assertTrue(ev.evaluate(Condition.Pattern(GesturePattern.SHAKE), nowMs = 2_000L))
    }

    @Test fun pattern_event_falseWhenEventExpired() {
        val ev = ConditionEvaluator(eventWindowMs = 1_000L)
        ev.onEvent(event(GesturePattern.SHAKE, t = 0L))
        // 2 s later, event outside the 1 s window
        assertFalse(ev.evaluate(Condition.Pattern(GesturePattern.SHAKE), nowMs = 2_000L))
    }

    @Test fun pattern_event_falseWhenDifferentEventPresent() {
        val ev = ConditionEvaluator()
        ev.onEvent(event(GesturePattern.FLIP_FACE_DOWN, t = 0L))
        assertFalse(ev.evaluate(Condition.Pattern(GesturePattern.SHAKE), nowMs = 1_000L))
    }

    // --- Pattern (state guard) ---

    @Test fun pattern_stateGuard_trueWhenActiveStateSet() {
        val ev = ConditionEvaluator()
        ev.onEvent(event(GesturePattern.IS_STATIONARY, t = 0L))
        assertTrue(
            ev.evaluate(Condition.Pattern(GesturePattern.IS_STATIONARY, isStateGuard = true), nowMs = 5_000L)
        )
    }

    @Test fun pattern_stateGuard_persists_beyondEventWindow() {
        val ev = ConditionEvaluator(eventWindowMs = 500L)
        ev.onEvent(event(GesturePattern.GOING_DARK, t = 0L))
        // 2 s later — event has expired but active state remains
        assertTrue(
            ev.evaluate(Condition.Pattern(GesturePattern.GOING_DARK, isStateGuard = true), nowMs = 2_000L)
        )
    }

    @Test fun pattern_stateGuard_clearedByOpposite() {
        val ev = ConditionEvaluator()
        ev.onEvent(event(GesturePattern.GOING_DARK, t = 0L))
        ev.onEvent(event(GesturePattern.GOING_BRIGHT, t = 100L))
        // GOING_DARK state should have been cleared by GOING_BRIGHT
        assertFalse(
            ev.evaluate(Condition.Pattern(GesturePattern.GOING_DARK, isStateGuard = true), nowMs = 1_000L)
        )
        assertTrue(
            ev.evaluate(Condition.Pattern(GesturePattern.GOING_BRIGHT, isStateGuard = true), nowMs = 1_000L)
        )
    }

    @Test fun stationarySteps_toggleActiveState() {
        val ev = ConditionEvaluator()
        ev.onEvent(event(GesturePattern.IS_STATIONARY, t = 0L))
        assertTrue(ev.evaluate(Condition.Pattern(GesturePattern.IS_STATIONARY, isStateGuard = true), nowMs = 100L))

        ev.onEvent(event(GesturePattern.STEP_DETECTED, t = 200L))
        // IS_STATIONARY active state should be cleared
        assertFalse(ev.evaluate(Condition.Pattern(GesturePattern.IS_STATIONARY, isStateGuard = true), nowMs = 300L))
        assertTrue(ev.evaluate(Condition.Pattern(GesturePattern.STEP_DETECTED, isStateGuard = true), nowMs = 300L))
    }

    // --- And combinator ---

    @Test fun and_trueWhenAllChildrenMet() {
        val ev = ConditionEvaluator(eventWindowMs = 5_000L)
        ev.onEvent(event(GesturePattern.SHAKE, t = 0L))
        ev.onEvent(event(GesturePattern.GOING_DARK, t = 100L))
        assertTrue(
            ev.evaluate(
                Condition.And(
                    Condition.Pattern(GesturePattern.SHAKE),
                    Condition.Pattern(GesturePattern.GOING_DARK),
                ),
                nowMs = 1_000L,
            )
        )
    }

    @Test fun and_falseWhenOneChildMissing() {
        val ev = ConditionEvaluator(eventWindowMs = 5_000L)
        ev.onEvent(event(GesturePattern.SHAKE, t = 0L))
        assertFalse(
            ev.evaluate(
                Condition.And(
                    Condition.Pattern(GesturePattern.SHAKE),
                    Condition.Pattern(GesturePattern.GOING_DARK),
                ),
                nowMs = 1_000L,
            )
        )
    }

    // --- Or combinator ---

    @Test fun or_trueWhenOneChildMet() {
        val ev = ConditionEvaluator(eventWindowMs = 5_000L)
        ev.onEvent(event(GesturePattern.FLIP_FACE_UP, t = 0L))
        assertTrue(
            ev.evaluate(
                Condition.Or(
                    Condition.Pattern(GesturePattern.SHAKE),
                    Condition.Pattern(GesturePattern.FLIP_FACE_UP),
                ),
                nowMs = 1_000L,
            )
        )
    }

    @Test fun or_falseWhenNoChildMet() {
        val ev = ConditionEvaluator()
        assertFalse(
            ev.evaluate(
                Condition.Or(
                    Condition.Pattern(GesturePattern.SHAKE),
                    Condition.Pattern(GesturePattern.TWIST),
                ),
                nowMs = 1_000L,
            )
        )
    }

    // --- Not combinator ---

    @Test fun not_invertsEventCondition() {
        val ev = ConditionEvaluator(eventWindowMs = 5_000L)
        // No SHAKE event → Not(SHAKE) should be true
        assertTrue(ev.evaluate(Condition.Not(Condition.Pattern(GesturePattern.SHAKE)), nowMs = 1_000L))
        ev.onEvent(event(GesturePattern.SHAKE, t = 100L))
        // After SHAKE fires, Not(SHAKE) should be false
        assertFalse(ev.evaluate(Condition.Not(Condition.Pattern(GesturePattern.SHAKE)), nowMs = 1_000L))
    }

    // --- Nested combinators ---

    @Test fun nestedAndNot_composedCorrectly() {
        val ev = ConditionEvaluator(eventWindowMs = 5_000L)
        ev.onEvent(event(GesturePattern.SHAKE, t = 0L))
        ev.onEvent(event(GesturePattern.IS_STATIONARY, t = 50L))
        // SHAKE AND NOT(GOING_DARK) — GOING_DARK has not fired
        assertTrue(
            ev.evaluate(
                Condition.And(
                    Condition.Pattern(GesturePattern.SHAKE),
                    Condition.Not(Condition.Pattern(GesturePattern.GOING_DARK)),
                ),
                nowMs = 1_000L,
            )
        )
    }

    // --- reset ---

    @Test fun reset_clearsEventsAndActiveStates() {
        val ev = ConditionEvaluator(eventWindowMs = 60_000L)
        ev.onEvent(event(GesturePattern.SHAKE, t = 0L))
        ev.onEvent(event(GesturePattern.IS_STATIONARY, t = 100L))
        ev.reset()
        assertFalse(ev.evaluate(Condition.Pattern(GesturePattern.SHAKE), nowMs = 1_000L))
        assertFalse(
            ev.evaluate(Condition.Pattern(GesturePattern.IS_STATIONARY, isStateGuard = true), nowMs = 1_000L)
        )
    }
}
