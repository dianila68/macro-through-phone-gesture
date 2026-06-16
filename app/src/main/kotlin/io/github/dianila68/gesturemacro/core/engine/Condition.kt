package io.github.dianila68.gesturemacro.core.engine

import io.github.dianila68.gesturemacro.core.sensors.GestureEvent
import io.github.dianila68.gesturemacro.core.sensors.GesturePattern
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * ticket-033: Condition model for composed multi-sensor triggers.
 *
 * A [Condition] is either a **leaf** (wraps a single sensor signal) or a
 * **combinator** (AND / OR / NOT). A composite trigger fires when its root
 * condition evaluates to true given the current sensor state.
 *
 * Distinction between events and states (per ticket-033 spec):
 *  - **Event** (e.g. STEP_DETECTED, FALL): instantaneous; the firing *edge*.
 *  - **State** (e.g. IS_STATIONARY, GOING_DARK): continuous; a guard that holds.
 *
 * The motivating example is: "light turns off *while* I'm walking → flashlight".
 * That's an event (GOING_DARK) gated by a state guard (NOT IS_STATIONARY), which
 * cannot be expressed with today's single-trigger v1 format. This model supports it.
 *
 * **Format note:** Serializing condition trees requires a format v2 bump
 * (governed by ADR-0002 / ticket-022). That migration is tracked separately;
 * this file is the pure-JVM evaluator the engine will consume.
 */
@Serializable
sealed class Condition {
    /** Evaluates the condition against [state]; returns true when it holds. */
    abstract fun evaluate(state: ConditionState): Boolean

    /** Leaf: true when a specific [pattern] has fired (event or active state). */
    @Serializable
    @SerialName("pattern")
    data class Pattern(
        val pattern: GesturePattern,
        /** When true this is a *state* guard (continuously active); when false an event edge. */
        val isStateGuard: Boolean = false,
    ) : Condition() {
        override fun evaluate(state: ConditionState): Boolean =
            if (isStateGuard) state.activeStates.contains(pattern) else state.recentEvents.contains(pattern)
    }

    /** Combinator: true when ALL [children] hold. */
    @Serializable
    @SerialName("and")
    data class And(val children: List<Condition>) : Condition() {
        constructor(vararg children: Condition) : this(children.toList())

        override fun evaluate(state: ConditionState): Boolean = children.all { it.evaluate(state) }
    }

    /** Combinator: true when ANY [children] holds. */
    @Serializable
    @SerialName("or")
    data class Or(val children: List<Condition>) : Condition() {
        constructor(vararg children: Condition) : this(children.toList())

        override fun evaluate(state: ConditionState): Boolean = children.any { it.evaluate(state) }
    }

    /** Combinator: inverts its single [child]. */
    @Serializable
    @SerialName("not")
    data class Not(val child: Condition) : Condition() {
        override fun evaluate(state: ConditionState): Boolean = !child.evaluate(state)
    }
}

/**
 * Snapshot of sensor state that [Condition.evaluate] reads.
 *
 * [recentEvents] — patterns that fired within the last [eventWindowMs].
 * [activeStates] — patterns whose detectors have signalled and that haven't been
 *                  cleared by an opposing event (e.g. GOING_DARK active until GOING_BRIGHT).
 */
@Serializable
data class ConditionState(
    val recentEvents: Set<GesturePattern> = emptySet(),
    val activeStates: Set<GesturePattern> = emptySet(),
    val timestampMs: Long = 0L,
)

/**
 * Maintains rolling [ConditionState] and evaluates [Condition] trees against it.
 *
 * Thread-safety: not required — the engine processes samples on a single thread.
 *
 * Usage:
 * ```
 * val evaluator = ConditionEvaluator(eventWindowMs = 2_000)
 * evaluator.onEvent(GestureEvent(GesturePattern.GOING_DARK, t = now, confidence = 1f))
 * val fires = evaluator.evaluate(myCompositeCondition)
 * ```
 */
class ConditionEvaluator(
    private val eventWindowMs: Long = DEFAULT_EVENT_WINDOW_MS,
) {
    private val recentEvents: ArrayDeque<Pair<Long, GesturePattern>> = ArrayDeque()
    private val activeStates: MutableSet<GesturePattern> = mutableSetOf()

    fun onEvent(event: GestureEvent) {
        val t = event.t
        recentEvents.addLast(t to event.pattern)
        // Prune events outside the window
        while (recentEvents.isNotEmpty() && t - recentEvents.first().first > eventWindowMs) {
            recentEvents.removeFirst()
        }
        // Update active-state tracking based on semantic opposites
        when (event.pattern) {
            GesturePattern.GOING_DARK -> {
                activeStates.add(GesturePattern.GOING_DARK)
                activeStates.remove(GesturePattern.GOING_BRIGHT)
            }
            GesturePattern.GOING_BRIGHT -> {
                activeStates.add(GesturePattern.GOING_BRIGHT)
                activeStates.remove(GesturePattern.GOING_DARK)
            }
            GesturePattern.IS_STATIONARY -> {
                activeStates.add(GesturePattern.IS_STATIONARY)
                activeStates.remove(GesturePattern.STEP_DETECTED)
            }
            GesturePattern.STEP_DETECTED -> {
                activeStates.remove(GesturePattern.IS_STATIONARY)
                activeStates.add(GesturePattern.STEP_DETECTED)
            }
            GesturePattern.ALTITUDE_RISE -> activeStates.add(GesturePattern.ALTITUDE_RISE)
            GesturePattern.ALTITUDE_FALL -> activeStates.add(GesturePattern.ALTITUDE_FALL)
            else -> { /* instantaneous event only */ }
        }
    }

    fun currentState(nowMs: Long): ConditionState {
        val window = recentEvents
            .filter { nowMs - it.first <= eventWindowMs }
            .map { it.second }
            .toSet()
        return ConditionState(
            recentEvents = window,
            activeStates = activeStates.toSet(),
            timestampMs = nowMs,
        )
    }

    fun evaluate(condition: Condition, nowMs: Long): Boolean =
        condition.evaluate(currentState(nowMs))

    fun reset() {
        recentEvents.clear()
        activeStates.clear()
    }

    companion object {
        const val DEFAULT_EVENT_WINDOW_MS = 2_000L
    }
}
