package io.github.dianila68.gesturemacro.core.engine

import io.github.dianila68.gesturemacro.core.sensors.GestureEvent
import io.github.dianila68.gesturemacro.core.sensors.GesturePattern
import io.github.dianila68.gesturemacro.core.serialization.GestureMacro
import io.github.dianila68.gesturemacro.core.serialization.PatternKind
import io.github.dianila68.gesturemacro.core.serialization.ScreenState

/**
 * Pure decision core: gesture event in, list of macros to dispatch out
 * (trigger match → enabled → constraints → condition → sensor-conditions → cooldown).
 *
 * ticket-033: A shared [ConditionEvaluator] is maintained per engine instance. Every event
 * is fed to it before trigger matching so state guards (IS_STATIONARY, GOING_DARK, etc.)
 * reflect the latest sensor picture by the time conditions are checked.
 *
 * ticket-032: A [ConditionEventWindow] is also maintained for [SensorCondition] lists.
 * Macros may carry zero or more SensorConditions that must ALL hold before dispatch.
 */
class MacroEngine(
    private val clock: () -> Long = System::currentTimeMillis,
    private val screenOn: () -> Boolean? = { null },
    conditionWindowMs: Long = ConditionEvaluator.DEFAULT_EVENT_WINDOW_MS,
    sensorWindowMs: Long = ConditionEventWindow.DEFAULT_WINDOW_MS,
) {
    private val lastFiredAt = mutableMapOf<String, Long>()
    private val conditionEvaluator = ConditionEvaluator(conditionWindowMs)
    private val sensorConditionWindow = ConditionEventWindow(sensorWindowMs)

    /**
     * Feeds the event to both condition systems, then returns every macro whose trigger
     * matches and whose condition, sensor-conditions, constraints, and cooldown pass.
     */
    fun match(event: GestureEvent, macros: List<GestureMacro>): List<GestureMacro> {
        conditionEvaluator.onEvent(event)
        sensorConditionWindow.onEvent(event)
        val now = clock()
        val minuteOfDay = minuteOfDay(now)
        return macros.filter { macro ->
            macro.enabled &&
                macro.trigger.pattern.matches(event.pattern) &&
                screenStateAllows(macro.constraints.screenState) &&
                timeWindowAllows(macro, minuteOfDay) &&
                conditionHolds(macro, now) &&
                sensorConditionsHold(macro, now) &&
                cooldownElapsed(macro, now)
        }.onEach { lastFiredAt[it.id] = now }
    }

    fun reset() {
        lastFiredAt.clear()
        conditionEvaluator.reset()
        sensorConditionWindow.reset()
    }

    private fun conditionHolds(macro: GestureMacro, nowMs: Long): Boolean {
        val condition = macro.condition ?: return true
        return conditionEvaluator.evaluate(condition, nowMs)
    }

    private fun sensorConditionsHold(macro: GestureMacro, nowMs: Long): Boolean {
        if (macro.sensorConditions.isEmpty()) return true
        return macro.sensorConditions.all { sensorConditionWindow.evaluate(it, nowMs) }
    }

    private fun cooldownElapsed(macro: GestureMacro, now: Long): Boolean {
        val last = lastFiredAt[macro.id] ?: return true
        return now - last >= macro.trigger.cooldownMs
    }

    private fun screenStateAllows(required: ScreenState): Boolean = when (required) {
        ScreenState.ANY -> true
        // Fail closed on unknown screen state: a constrained macro must not fire blind.
        ScreenState.ON -> screenOn() == true
        ScreenState.OFF -> screenOn() == false
    }

    private fun timeWindowAllows(macro: GestureMacro, minuteOfDay: Int): Boolean {
        val window = macro.constraints.timeWindow ?: return true
        val start = window.start.toMinutes()
        val end = window.end.toMinutes()
        return if (start <= end) {
            minuteOfDay in start..end
        } else {
            // Overnight window, e.g. 22:00–06:00
            minuteOfDay >= start || minuteOfDay <= end
        }
    }

    private fun minuteOfDay(epochMillis: Long): Int {
        val minutes = (epochMillis / 60_000L) % (24 * 60)
        return minutes.toInt()
    }

    private fun String.toMinutes(): Int {
        val (h, m) = split(":")
        return h.toInt() * 60 + m.toInt()
    }

    companion object {
        fun PatternKind.matches(pattern: GesturePattern): Boolean = when (this) {
            PatternKind.SHAKE -> pattern == GesturePattern.SHAKE
            PatternKind.DOUBLE_SHAKE -> pattern == GesturePattern.DOUBLE_SHAKE
            PatternKind.FLIP_FACE_DOWN -> pattern == GesturePattern.FLIP_FACE_DOWN
            PatternKind.FLIP_FACE_UP -> pattern == GesturePattern.FLIP_FACE_UP
            PatternKind.TWIST -> pattern == GesturePattern.TWIST
            PatternKind.PROXIMITY_WAVE -> pattern == GesturePattern.PROXIMITY_WAVE
            PatternKind.FALL -> pattern == GesturePattern.FALL
            PatternKind.CUSTOM -> false
            PatternKind.STEP_DETECTED -> pattern == GesturePattern.STEP_DETECTED
            PatternKind.IS_STATIONARY -> pattern == GesturePattern.IS_STATIONARY
            PatternKind.PICKED_UP -> pattern == GesturePattern.PICKED_UP
            PatternKind.GOING_DARK -> pattern == GesturePattern.GOING_DARK
            PatternKind.GOING_BRIGHT -> pattern == GesturePattern.GOING_BRIGHT
            PatternKind.ALTITUDE_RISE -> pattern == GesturePattern.ALTITUDE_RISE
            PatternKind.ALTITUDE_FALL -> pattern == GesturePattern.ALTITUDE_FALL
            PatternKind.HEADING_CHANGED -> pattern == GesturePattern.HEADING_CHANGED
        }
    }
}
