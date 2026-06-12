package io.github.dianila68.gesturemacro.core.data

import io.github.dianila68.gesturemacro.core.serialization.GestureMacro
import io.github.dianila68.gesturemacro.core.serialization.PatternKind
import io.github.dianila68.gesturemacro.core.serialization.SensorKind
import io.github.dianila68.gesturemacro.core.serialization.SystemToggleAction
import io.github.dianila68.gesturemacro.core.serialization.Trigger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * In-memory macro store seeding one built-in macro. Room persistence replaces
 * the backing storage in M3 (ticket-005 follow-up); the API stays.
 */
object MacroStore {
    private val state = MutableStateFlow(listOf(builtInShakeFlashlight()))

    val macros: StateFlow<List<GestureMacro>> = state

    fun upsert(macro: GestureMacro) {
        state.value = state.value.filterNot { it.id == macro.id } + macro
    }

    fun remove(id: String) {
        state.value = state.value.filterNot { it.id == id }
    }

    fun setEnabled(id: String, enabled: Boolean) {
        state.value = state.value.map { if (it.id == id) it.copy(enabled = enabled) else it }
    }

    private fun builtInShakeFlashlight() = GestureMacro(
        version = 1,
        id = "builtin-shake-flashlight",
        name = "Shake to toggle flashlight",
        enabled = true,
        trigger = Trigger(sensor = SensorKind.ACCELEROMETER, pattern = PatternKind.SHAKE),
        actions = listOf(SystemToggleAction(target = "flashlight")),
    )
}
