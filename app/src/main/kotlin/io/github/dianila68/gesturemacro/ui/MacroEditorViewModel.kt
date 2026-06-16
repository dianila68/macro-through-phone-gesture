package io.github.dianila68.gesturemacro.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.dianila68.gesturemacro.android.data.MacroStore
import io.github.dianila68.gesturemacro.core.engine.Condition
import io.github.dianila68.gesturemacro.core.serialization.Constraints
import io.github.dianila68.gesturemacro.core.serialization.GestureMacro
import io.github.dianila68.gesturemacro.core.serialization.MacroAction
import io.github.dianila68.gesturemacro.core.serialization.PatternKind
import io.github.dianila68.gesturemacro.core.serialization.SensorKind
import io.github.dianila68.gesturemacro.core.serialization.Trigger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ticket-045: ViewModel for the macro editor screen.
 * Holds draft state for a new or existing macro; persists on save.
 */
class MacroEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val store = MacroStore

    sealed class UiState {
        data object Idle : UiState()
        data object Saving : UiState()
        data object Saved : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Draft fields
    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _enabled = MutableStateFlow(true)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _sensor = MutableStateFlow(SensorKind.ACCELEROMETER)
    val sensor: StateFlow<SensorKind> = _sensor.asStateFlow()

    private val _pattern = MutableStateFlow(PatternKind.SHAKE)
    val pattern: StateFlow<PatternKind> = _pattern.asStateFlow()

    private val _sensitivity = MutableStateFlow(0.5f)
    val sensitivity: StateFlow<Float> = _sensitivity.asStateFlow()

    private val _cooldownMs = MutableStateFlow(2_000L)
    val cooldownMs: StateFlow<Long> = _cooldownMs.asStateFlow()

    private val _actions = MutableStateFlow<List<MacroAction>>(emptyList())
    val actions: StateFlow<List<MacroAction>> = _actions.asStateFlow()

    private val _constraints = MutableStateFlow(Constraints())
    val constraints: StateFlow<Constraints> = _constraints.asStateFlow()

    private val _condition = MutableStateFlow<Condition?>(null)
    val condition: StateFlow<Condition?> = _condition.asStateFlow()

    private var editingId: String? = null

    fun loadMacro(macro: GestureMacro) {
        editingId = macro.id
        _name.value = macro.name
        _enabled.value = macro.enabled
        _sensor.value = macro.trigger.sensor
        _pattern.value = macro.trigger.pattern
        _sensitivity.value = macro.trigger.sensitivity
        _cooldownMs.value = macro.trigger.cooldownMs
        _actions.value = macro.actions
        _constraints.value = macro.constraints
        _condition.value = macro.condition
    }

    fun setName(v: String) { _name.value = v }
    fun setEnabled(v: Boolean) { _enabled.value = v }
    fun setSensor(v: SensorKind) { _sensor.value = v }
    fun setPattern(v: PatternKind) { _pattern.value = v }
    fun setSensitivity(v: Float) { _sensitivity.value = v }
    fun setCooldownMs(v: Long) { _cooldownMs.value = v }
    fun setActions(v: List<MacroAction>) { _actions.value = v }
    fun setConstraints(v: Constraints) { _constraints.value = v }
    fun setCondition(v: Condition?) { _condition.value = v }

    fun addAction(action: MacroAction) { _actions.value = _actions.value + action }
    fun removeAction(index: Int) { _actions.value = _actions.value.toMutableList().also { it.removeAt(index) } }
    fun moveAction(from: Int, to: Int) {
        val list = _actions.value.toMutableList()
        val item = list.removeAt(from)
        list.add(to, item)
        _actions.value = list
    }

    fun save() {
        val nameVal = _name.value.trim()
        if (nameVal.isBlank()) {
            _uiState.value = UiState.Error("Name must not be blank")
            return
        }
        if (_actions.value.isEmpty()) {
            _uiState.value = UiState.Error("Add at least one action")
            return
        }
        viewModelScope.launch {
            _uiState.value = UiState.Saving
            runCatching {
                val macro = GestureMacro(
                    version = 2,
                    id = editingId ?: UUID.randomUUID().toString(),
                    name = nameVal,
                    enabled = _enabled.value,
                    trigger = Trigger(
                        sensor = _sensor.value,
                        pattern = _pattern.value,
                        sensitivity = _sensitivity.value,
                        cooldownMs = _cooldownMs.value,
                    ),
                    constraints = _constraints.value,
                    actions = _actions.value,
                    condition = _condition.value,
                )
                store.upsert(macro)
            }.fold(
                onSuccess = { _uiState.value = UiState.Saved },
                onFailure = { _uiState.value = UiState.Error(it.message ?: "Save failed") },
            )
        }
    }

    fun resetUiState() { _uiState.value = UiState.Idle }
}
