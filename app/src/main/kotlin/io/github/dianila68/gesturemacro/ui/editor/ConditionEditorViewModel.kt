package io.github.dianila68.gesturemacro.ui.editor

import androidx.lifecycle.ViewModel
import io.github.dianila68.gesturemacro.core.engine.Condition
import io.github.dianila68.gesturemacro.core.sensors.GesturePattern
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A single row in the condition editor: which pattern + whether it's a persistent state guard. */
data class ConditionEntry(
    val pattern: GesturePattern,
    val isStateGuard: Boolean,
    val label: String = pattern.displayLabel(),
)

enum class CombineMode { AND, OR }

class ConditionEditorViewModel : ViewModel() {

    private val _entries = MutableStateFlow<List<ConditionEntry>>(emptyList())
    val entries: StateFlow<List<ConditionEntry>> = _entries.asStateFlow()

    private val _combineMode = MutableStateFlow(CombineMode.AND)
    val combineMode: StateFlow<CombineMode> = _combineMode.asStateFlow()

    val availablePatterns: List<GesturePattern> = listOf(
        GesturePattern.IS_STATIONARY,
        GesturePattern.STEP_DETECTED,
        GesturePattern.GOING_DARK,
        GesturePattern.GOING_BRIGHT,
        GesturePattern.ALTITUDE_RISE,
        GesturePattern.ALTITUDE_FALL,
        GesturePattern.HEADING_CHANGED,
        GesturePattern.PICKED_UP,
    )

    fun loadFromCondition(condition: Condition?) {
        if (condition == null) {
            _entries.value = emptyList()
            return
        }
        when (condition) {
            is Condition.Pattern -> {
                _entries.value = listOf(ConditionEntry(condition.pattern, condition.isStateGuard))
                _combineMode.value = CombineMode.AND
            }
            is Condition.And -> {
                _combineMode.value = CombineMode.AND
                _entries.value = condition.children.filterIsInstance<Condition.Pattern>()
                    .map { ConditionEntry(it.pattern, it.isStateGuard) }
            }
            is Condition.Or -> {
                _combineMode.value = CombineMode.OR
                _entries.value = condition.children.filterIsInstance<Condition.Pattern>()
                    .map { ConditionEntry(it.pattern, it.isStateGuard) }
            }
            else -> _entries.value = emptyList()
        }
    }

    fun togglePattern(pattern: GesturePattern) {
        val current = _entries.value.toMutableList()
        val existing = current.indexOfFirst { it.pattern == pattern }
        if (existing >= 0) {
            current.removeAt(existing)
        } else {
            val isStateGuard = STATE_GUARD_PATTERNS.contains(pattern)
            current.add(ConditionEntry(pattern, isStateGuard))
        }
        _entries.value = current
    }

    fun toggleStateGuard(pattern: GesturePattern) {
        _entries.value = _entries.value.map {
            if (it.pattern == pattern) it.copy(isStateGuard = !it.isStateGuard) else it
        }
    }

    fun setCombineMode(mode: CombineMode) {
        _combineMode.value = mode
    }

    fun removeEntry(pattern: GesturePattern) {
        _entries.value = _entries.value.filter { it.pattern != pattern }
    }

    fun buildCondition(): Condition? {
        val leaves = _entries.value.map { Condition.Pattern(it.pattern, it.isStateGuard) }
        return when {
            leaves.isEmpty() -> null
            leaves.size == 1 -> leaves.first()
            _combineMode.value == CombineMode.AND -> Condition.And(leaves)
            else -> Condition.Or(leaves)
        }
    }

    companion object {
        // These patterns represent persistent state rather than instantaneous edges.
        val STATE_GUARD_PATTERNS = setOf(
            GesturePattern.IS_STATIONARY,
            GesturePattern.GOING_DARK,
            GesturePattern.GOING_BRIGHT,
        )
    }
}

fun GesturePattern.displayLabel(): String = when (this) {
    GesturePattern.IS_STATIONARY  -> "Device is stationary"
    GesturePattern.STEP_DETECTED  -> "Step detected"
    GesturePattern.GOING_DARK     -> "Screen/environment going dark"
    GesturePattern.GOING_BRIGHT   -> "Screen/environment going bright"
    GesturePattern.ALTITUDE_RISE  -> "Altitude rising"
    GesturePattern.ALTITUDE_FALL  -> "Altitude falling"
    GesturePattern.HEADING_CHANGED -> "Heading changed"
    GesturePattern.PICKED_UP      -> "Device picked up"
    else -> name.lowercase().replace('_', ' ')
}
