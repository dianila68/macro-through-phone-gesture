package io.github.dianila68.gesturemacro.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.dianila68.gesturemacro.android.data.RecordedGestureStore
import io.github.dianila68.gesturemacro.core.recording.GestureEnvelope
import io.github.dianila68.gesturemacro.core.triggers.RecordedGestureDetector
import io.github.dianila68.gesturemacro.core.sensors.SensorSample
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ticket-052: Live replay-validation — after recording finishes, streams live accel frames
 * through a RecordedGestureDetector against the freshly built envelope, so the user can
 * verify the gesture is recognisable before saving.
 */
class GestureReplayValidationViewModel(application: Application) : AndroidViewModel(application) {

    private val store = RecordedGestureStore(application)

    sealed class ValidationState {
        data object Idle : ValidationState()
        data object Waiting : ValidationState()
        data class Matched(val confidence: Float) : ValidationState()
        data object NoMatch : ValidationState()
        data object Saved : ValidationState()
    }

    private val _state = MutableStateFlow<ValidationState>(ValidationState.Idle)
    val state: StateFlow<ValidationState> = _state.asStateFlow()

    private var pendingEnvelope: GestureEnvelope? = null
    private var pendingName: String = ""
    private var detector: RecordedGestureDetector? = null

    fun beginValidation(envelope: GestureEnvelope, name: String) {
        pendingEnvelope = envelope
        pendingName = name
        detector = RecordedGestureDetector(
            envelope = envelope,
            envelopeId = "preview",
            sensitivity = 0.5f,
        )
        _state.value = ValidationState.Waiting
    }

    /** Feed a live accelerometer sample; updates state when a match fires. */
    fun feed(sample: SensorSample) {
        if (_state.value !is ValidationState.Waiting) return
        val event = detector?.feed(sample) ?: return
        _state.value = ValidationState.Matched(event.confidence)
    }

    fun resetValidation() {
        detector?.reset()
        _state.value = ValidationState.Waiting
    }

    fun save(name: String = pendingName) {
        val envelope = pendingEnvelope ?: return
        viewModelScope.launch {
            store.upsert(name = name.ifBlank { pendingName }, envelope = envelope)
            _state.value = ValidationState.Saved
        }
    }

    fun discard() {
        pendingEnvelope = null
        detector = null
        _state.value = ValidationState.Idle
    }
}
