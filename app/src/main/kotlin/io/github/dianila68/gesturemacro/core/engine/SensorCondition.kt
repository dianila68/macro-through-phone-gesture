package io.github.dianila68.gesturemacro.core.engine

import io.github.dianila68.gesturemacro.core.sensors.GestureEvent
import io.github.dianila68.gesturemacro.core.sensors.GesturePattern
import io.github.dianila68.gesturemacro.core.sensors.SensorUtils

/**
 * Guard conditions that are evaluated against the recent event stream before
 * a macro dispatches (ticket-030 / ticket-032).
 *
 * Unlike trigger patterns (which fire on a single gesture event), conditions
 * examine rolling window state. Each condition type is a data class so they
 * can be serialized into the macro model without reflection.
 */
sealed class SensorCondition {
    /** True when the device has recently been reported as stationary (IS_STATIONARY pattern). */
    object IsStationary : SensorCondition()

    /** True when the device was recently picked up (PICKED_UP pattern fired). */
    object WasPickedUp : SensorCondition()

    /**
     * True when ambient light crossed from bright to dark within [withinMs].
     * Use case: "only fire shake macro when I cover the screen".
     */
    data class WentDark(val withinMs: Long = 3_000L) : SensorCondition()

    /**
     * True when ambient light crossed from dark to bright within [withinMs].
     */
    data class WentBright(val withinMs: Long = 3_000L) : SensorCondition()

    /**
     * True when altitude rose by at least [minRiseM] metres in [withinMs].
     * Useful for floor-change detection.
     */
    data class AltitudeRose(val withinMs: Long = 10_000L) : SensorCondition()

    /** True when altitude fell within [withinMs]. */
    data class AltitudeFell(val withinMs: Long = 10_000L) : SensorCondition()

    /** True when heading changed by >= [minDeltaDeg] within [withinMs]. */
    data class HeadingChanged(val minDeltaDeg: Float = 30f, val withinMs: Long = 5_000L) : SensorCondition()

    /** Logical AND of two conditions. */
    data class All(val conditions: List<SensorCondition>) : SensorCondition()

    /** Logical OR of two conditions. */
    data class Any(val conditions: List<SensorCondition>) : SensorCondition()

    /** Negation of a condition. */
    data class Not(val condition: SensorCondition) : SensorCondition()
}

/**
 * Stateful evaluator that maintains a rolling window of [GestureEvent]s and
 * evaluates [SensorCondition]s against it. Fed by [MacroEngine] on every event.
 */
class ConditionEventWindow(val windowMs: Long = DEFAULT_WINDOW_MS) {

    private val events = ArrayDeque<GestureEvent>()

    fun onEvent(event: GestureEvent) {
        events.addLast(event)
        purgeOld(event.t)
    }

    fun evaluate(condition: SensorCondition, nowMs: Long): Boolean {
        purgeOld(nowMs)
        return check(condition, nowMs)
    }

    fun reset() = events.clear()

    private fun purgeOld(nowMs: Long) {
        while (events.isNotEmpty() && nowMs - events.first().t > windowMs) {
            events.removeFirst()
        }
    }

    private fun hasPattern(pattern: GesturePattern) =
        events.any { it.pattern == pattern }

    private fun hasPatternWithin(pattern: GesturePattern, withinMs: Long, nowMs: Long) =
        events.any { it.pattern == pattern && nowMs - it.t <= withinMs }

    private fun check(condition: SensorCondition, nowMs: Long): Boolean = when (condition) {
        is SensorCondition.IsStationary -> hasPattern(GesturePattern.IS_STATIONARY)
        is SensorCondition.WasPickedUp -> hasPattern(GesturePattern.PICKED_UP)
        is SensorCondition.WentDark -> hasPatternWithin(GesturePattern.GOING_DARK, condition.withinMs, nowMs)
        is SensorCondition.WentBright -> hasPatternWithin(GesturePattern.GOING_BRIGHT, condition.withinMs, nowMs)
        is SensorCondition.AltitudeRose -> hasPatternWithin(GesturePattern.ALTITUDE_RISE, condition.withinMs, nowMs)
        is SensorCondition.AltitudeFell -> hasPatternWithin(GesturePattern.ALTITUDE_FALL, condition.withinMs, nowMs)
        is SensorCondition.HeadingChanged -> hasPatternWithin(GesturePattern.HEADING_CHANGED, condition.withinMs, nowMs)
        is SensorCondition.All -> condition.conditions.all { check(it, nowMs) }
        is SensorCondition.Any -> condition.conditions.any { check(it, nowMs) }
        is SensorCondition.Not -> !check(condition.condition, nowMs)
    }

    companion object {
        const val DEFAULT_WINDOW_MS = 10_000L
    }
}
