package io.github.dianila68.gesturemacro.ui

import androidx.lifecycle.ViewModel
import io.github.dianila68.gesturemacro.core.engine.Condition
import io.github.dianila68.gesturemacro.core.sensors.GesturePattern
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ticket-050: ViewModel for the condition builder.
 * Manages a flat list of condition chips; builds a Condition tree on demand.
 * Depth limit 3 (AND → OR → leaf) to keep the UI simple.
 */
class ConditionBuilderViewModel : ViewModel() {

    data class ConditionChip(
        val pattern: GesturePattern,
        val isStateGuard: Boolean = false,
        val negate: Boolean = false,
    )

    sealed class CombineMode {
        data object All : CombineMode()   // AND
        data object Any : CombineMode()   // OR
    }

    private val _chips = MutableStateFlow<List<ConditionChip>>(emptyList())
    val chips: StateFlow<List<ConditionChip>> = _chips.asStateFlow()

    private val _combineMode = MutableStateFlow<CombineMode>(CombineMode.All)
    val combineMode: StateFlow<CombineMode> = _combineMode.asStateFlow()

    fun addChip(chip: ConditionChip) {
        if (_chips.value.size < MAX_CHIPS) {
            _chips.value = _chips.value + chip
        }
    }

    fun removeChip(index: Int) {
        _chips.value = _chips.value.toMutableList().also { it.removeAt(index) }
    }

    fun toggleNegate(index: Int) {
        _chips.value = _chips.value.mapIndexed { i, chip ->
            if (i == index) chip.copy(negate = !chip.negate) else chip
        }
    }

    fun setCombineMode(mode: CombineMode) { _combineMode.value = mode }

    fun clear() { _chips.value = emptyList() }

    fun buildCondition(): Condition? {
        val leaves = _chips.value.map { chip ->
            val leaf: Condition = Condition.Pattern(chip.pattern, chip.isStateGuard)
            if (chip.negate) Condition.Not(leaf) else leaf
        }
        if (leaves.isEmpty()) return null
        if (leaves.size == 1) return leaves[0]
        return when (_combineMode.value) {
            is CombineMode.All -> Condition.And(leaves)
            is CombineMode.Any -> Condition.Or(leaves)
        }
    }

    fun loadFromCondition(condition: Condition?) {
        if (condition == null) { clear(); return }
        // Flatten top-level And/Or into chips
        val (mode, children) = when (condition) {
            is Condition.And -> CombineMode.All to condition.children
            is Condition.Or -> CombineMode.Any to condition.children
            else -> CombineMode.All to listOf(condition)
        }
        _combineMode.value = mode
        _chips.value = children.mapNotNull { child ->
            when (child) {
                is Condition.Pattern -> ConditionChip(child.pattern, child.isStateGuard, false)
                is Condition.Not -> (child.child as? Condition.Pattern)?.let {
                    ConditionChip(it.pattern, it.isStateGuard, true)
                }
                else -> null
            }
        }
    }

    companion object {
        const val MAX_CHIPS = 6
    }
}
