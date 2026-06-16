package io.github.dianila68.gesturemacro.ui

import io.github.dianila68.gesturemacro.core.engine.Condition
import io.github.dianila68.gesturemacro.core.sensors.GesturePattern
import org.junit.Assert.*
import org.junit.Test

class ConditionBuilderViewModelTest {

    private fun vm() = ConditionBuilderViewModel()

    @Test
    fun emptyChips_buildCondition_returnsNull() {
        assertNull(vm().buildCondition())
    }

    @Test
    fun singleChip_buildCondition_returnsPattern() {
        val v = vm()
        v.addChip(ConditionBuilderViewModel.ConditionChip(GesturePattern.SHAKE))
        val cond = v.buildCondition()
        assertNotNull(cond)
        assertTrue(cond is Condition.Pattern)
    }

    @Test
    fun negatedChip_buildCondition_returnsNot() {
        val v = vm()
        v.addChip(ConditionBuilderViewModel.ConditionChip(GesturePattern.SHAKE, negate = true))
        val cond = v.buildCondition()
        assertTrue(cond is Condition.Not)
    }

    @Test
    fun twoChips_allMode_returnsAnd() {
        val v = vm()
        v.addChip(ConditionBuilderViewModel.ConditionChip(GesturePattern.SHAKE))
        v.addChip(ConditionBuilderViewModel.ConditionChip(GesturePattern.FALL))
        v.setCombineMode(ConditionBuilderViewModel.CombineMode.All)
        val cond = v.buildCondition()
        assertTrue(cond is Condition.And)
    }

    @Test
    fun twoChips_anyMode_returnsOr() {
        val v = vm()
        v.addChip(ConditionBuilderViewModel.ConditionChip(GesturePattern.SHAKE))
        v.addChip(ConditionBuilderViewModel.ConditionChip(GesturePattern.FALL))
        v.setCombineMode(ConditionBuilderViewModel.CombineMode.Any)
        val cond = v.buildCondition()
        assertTrue(cond is Condition.Or)
    }

    @Test
    fun loadFromCondition_andCondition_populatesChips() {
        val v = vm()
        val cond = Condition.And(listOf(
            Condition.Pattern(GesturePattern.SHAKE),
            Condition.Pattern(GesturePattern.FALL),
        ))
        v.loadFromCondition(cond)
        assertEquals(2, v.chips.value.size)
        assertTrue(v.combineMode.value is ConditionBuilderViewModel.CombineMode.All)
    }

    @Test
    fun maxChips_doesNotExceedLimit() {
        val v = vm()
        repeat(ConditionBuilderViewModel.MAX_CHIPS + 2) {
            v.addChip(ConditionBuilderViewModel.ConditionChip(GesturePattern.SHAKE))
        }
        assertEquals(ConditionBuilderViewModel.MAX_CHIPS, v.chips.value.size)
    }
}
