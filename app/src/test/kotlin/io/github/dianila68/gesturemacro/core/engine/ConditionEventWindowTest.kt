package io.github.dianila68.gesturemacro.core.engine

import io.github.dianila68.gesturemacro.core.sensors.GestureEvent
import io.github.dianila68.gesturemacro.core.sensors.GesturePattern
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConditionEventWindowTest {

    private fun event(pattern: GesturePattern, t: Long) =
        GestureEvent(pattern, t, confidence = 1f)

    // --- empty window ---

    @Test fun emptyWindow_allConditionsFalse() {
        val window = ConditionEventWindow()
        assertFalse(window.evaluate(SensorCondition.IsStationary, nowMs = 1_000L))
        assertFalse(window.evaluate(SensorCondition.WasPickedUp, nowMs = 1_000L))
        assertFalse(window.evaluate(SensorCondition.WentDark(), nowMs = 1_000L))
    }

    // --- simple pattern conditions ---

    @Test fun isStationary_matchesWhenEventPresent() {
        val window = ConditionEventWindow()
        window.onEvent(event(GesturePattern.IS_STATIONARY, t = 500L))
        assertTrue(window.evaluate(SensorCondition.IsStationary, nowMs = 1_000L))
    }

    @Test fun wasPickedUp_matchesWhenEventPresent() {
        val window = ConditionEventWindow()
        window.onEvent(event(GesturePattern.PICKED_UP, t = 500L))
        assertTrue(window.evaluate(SensorCondition.WasPickedUp, nowMs = 1_000L))
    }

    @Test fun isStationary_falseWhenOnlyOtherEventPresent() {
        val window = ConditionEventWindow()
        window.onEvent(event(GesturePattern.SHAKE, t = 500L))
        assertFalse(window.evaluate(SensorCondition.IsStationary, nowMs = 1_000L))
    }

    // --- timed conditions ---

    @Test fun wentDark_trueWhenWithinWindow() {
        val window = ConditionEventWindow()
        window.onEvent(event(GesturePattern.GOING_DARK, t = 1_000L))
        assertTrue(window.evaluate(SensorCondition.WentDark(withinMs = 2_000L), nowMs = 2_500L))
    }

    @Test fun wentDark_falseWhenExpiredByWithinMs() {
        val window = ConditionEventWindow(windowMs = 30_000L)
        window.onEvent(event(GesturePattern.GOING_DARK, t = 0L))
        // event is 5 s old but withinMs = 3 s
        assertFalse(window.evaluate(SensorCondition.WentDark(withinMs = 3_000L), nowMs = 5_000L))
    }

    @Test fun wentBright_matchesWhenWithinWindow() {
        val window = ConditionEventWindow()
        window.onEvent(event(GesturePattern.GOING_BRIGHT, t = 2_000L))
        assertTrue(window.evaluate(SensorCondition.WentBright(withinMs = 1_500L), nowMs = 3_000L))
    }

    @Test fun altitudeRose_matchesWhenWithinWindow() {
        val window = ConditionEventWindow()
        window.onEvent(event(GesturePattern.ALTITUDE_RISE, t = 5_000L))
        assertTrue(window.evaluate(SensorCondition.AltitudeRose(withinMs = 6_000L), nowMs = 10_000L))
    }

    @Test fun altitudeFell_falseWhenOnlyRisePresent() {
        val window = ConditionEventWindow()
        window.onEvent(event(GesturePattern.ALTITUDE_RISE, t = 5_000L))
        assertFalse(window.evaluate(SensorCondition.AltitudeFell(withinMs = 6_000L), nowMs = 10_000L))
    }

    @Test fun headingChanged_matchesWhenWithinWindow() {
        val window = ConditionEventWindow()
        window.onEvent(event(GesturePattern.HEADING_CHANGED, t = 1_000L))
        assertTrue(window.evaluate(SensorCondition.HeadingChanged(withinMs = 2_000L), nowMs = 2_000L))
    }

    // --- compound conditions ---

    @Test fun all_trueWhenBothConditionsMet() {
        val window = ConditionEventWindow()
        window.onEvent(event(GesturePattern.IS_STATIONARY, t = 100L))
        window.onEvent(event(GesturePattern.GOING_DARK, t = 200L))
        assertTrue(
            window.evaluate(
                SensorCondition.All(listOf(SensorCondition.IsStationary, SensorCondition.WentDark(3_000L))),
                nowMs = 1_000L,
            )
        )
    }

    @Test fun all_falseWhenOneConditionMissing() {
        val window = ConditionEventWindow()
        window.onEvent(event(GesturePattern.IS_STATIONARY, t = 100L))
        // WentDark event missing
        assertFalse(
            window.evaluate(
                SensorCondition.All(listOf(SensorCondition.IsStationary, SensorCondition.WentDark())),
                nowMs = 1_000L,
            )
        )
    }

    @Test fun any_trueWhenOneConditionMet() {
        val window = ConditionEventWindow()
        window.onEvent(event(GesturePattern.PICKED_UP, t = 100L))
        assertTrue(
            window.evaluate(
                SensorCondition.Any(listOf(SensorCondition.IsStationary, SensorCondition.WasPickedUp)),
                nowMs = 1_000L,
            )
        )
    }

    @Test fun any_falseWhenNoConditionMet() {
        val window = ConditionEventWindow()
        assertFalse(
            window.evaluate(
                SensorCondition.Any(listOf(SensorCondition.IsStationary, SensorCondition.WasPickedUp)),
                nowMs = 1_000L,
            )
        )
    }

    @Test fun not_invertsCondition() {
        val window = ConditionEventWindow()
        // IsStationary is false (no event) → Not(IsStationary) should be true
        assertTrue(window.evaluate(SensorCondition.Not(SensorCondition.IsStationary), nowMs = 1_000L))
        window.onEvent(event(GesturePattern.IS_STATIONARY, t = 500L))
        // Now IsStationary is true → Not should be false
        assertFalse(window.evaluate(SensorCondition.Not(SensorCondition.IsStationary), nowMs = 1_000L))
    }

    // --- window expiry ---

    @Test fun oldEventsPurgedFromWindow() {
        val window = ConditionEventWindow(windowMs = 5_000L)
        window.onEvent(event(GesturePattern.IS_STATIONARY, t = 0L))
        // 6 s later: event is older than windowMs and should be purged
        assertFalse(window.evaluate(SensorCondition.IsStationary, nowMs = 6_000L))
    }

    @Test fun eventsWithinWindowNotPurged() {
        val window = ConditionEventWindow(windowMs = 5_000L)
        window.onEvent(event(GesturePattern.IS_STATIONARY, t = 2_000L))
        assertTrue(window.evaluate(SensorCondition.IsStationary, nowMs = 6_000L))
    }

    // --- reset ---

    @Test fun resetClearsAllEvents() {
        val window = ConditionEventWindow()
        window.onEvent(event(GesturePattern.IS_STATIONARY, t = 100L))
        window.onEvent(event(GesturePattern.PICKED_UP, t = 200L))
        window.reset()
        assertFalse(window.evaluate(SensorCondition.IsStationary, nowMs = 1_000L))
        assertFalse(window.evaluate(SensorCondition.WasPickedUp, nowMs = 1_000L))
    }
}
