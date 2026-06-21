package io.github.dianila68.gesturemacro.ui.recording

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.dianila68.gesturemacro.core.recording.DefaultGestureRecordingSession
import io.github.dianila68.gesturemacro.core.recording.GestureEnvelope
import io.github.dianila68.gesturemacro.core.recording.RecordingChannel
import io.github.dianila68.gesturemacro.core.recording.RecordingConfig
import io.github.dianila68.gesturemacro.core.recording.RecordingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecordingViewModel : ViewModel() {

    companion object {
        private const val WAVEFORM_BUFFER_SIZE = 60
    }

    private val session = DefaultGestureRecordingSession()

    val recordingState: StateFlow<RecordingState> = session.state
    val coverageUpdates = session.coverageUpdates

    private val _requiredSamples = MutableStateFlow(5)
    val requiredSamples: StateFlow<Int> = _requiredSamples.asStateFlow()

    private val _useGyro = MutableStateFlow(true)
    val useGyro: StateFlow<Boolean> = _useGyro.asStateFlow()

    private val _sensitivity = MutableStateFlow(0.5f)
    val sensitivity: StateFlow<Float> = _sensitivity.asStateFlow()

    // Live waveform magnitude values for UI (ring-buffer style, last N points)
    private val _waveformPoints = MutableStateFlow<List<Float>>(emptyList())
    val waveformPoints: StateFlow<List<Float>> = _waveformPoints.asStateFlow()

    private val _validationResult = MutableStateFlow<ValidationOutcome?>(null)
    val validationResult: StateFlow<ValidationOutcome?> = _validationResult.asStateFlow()

    fun setRequiredSamples(n: Int) { _requiredSamples.value = n.coerceIn(3, 8) }
    fun setUseGyro(v: Boolean) { _useGyro.value = v }
    fun setSensitivity(v: Float) { _sensitivity.value = v.coerceIn(0f, 1f) }

    fun startRecording() {
        _waveformPoints.value = emptyList()
        _validationResult.value = null
        val channels = buildSet {
            add(RecordingChannel.ACCELEROMETER)
            if (_useGyro.value) add(RecordingChannel.GYROSCOPE)
        }
        val config = RecordingConfig(
            requiredSamples = _requiredSamples.value,
            minSamples = (_requiredSamples.value - 2).coerceAtLeast(1),
            channels = channels,
        )
        session.start(config, viewModelScope)

        // Collect frames from the session buffer for waveform display
        viewModelScope.launch {
            session.state.collect { state ->
                if (state is RecordingState.Recording) {
                    val recentFrames = session.buffer.windows.lastOrNull()?.frames
                        ?.filter { it.channel == RecordingChannel.ACCELEROMETER }
                        ?.takeLast(WAVEFORM_BUFFER_SIZE)
                        ?.map { f ->
                            kotlin.math.sqrt(
                                f.values[0] * f.values[0] +
                                    f.values[1] * f.values[1] +
                                    f.values[2] * f.values[2],
                            )
                        } ?: emptyList()
                    _waveformPoints.value = recentFrames
                }
            }
        }
    }

    fun cancel() {
        session.cancel()
    }

    fun reset() {
        session.cancel()
        _waveformPoints.value = emptyList()
        _validationResult.value = null
    }

    fun onValidationComplete(result: ValidationOutcome) {
        _validationResult.value = result
    }

    /** Returns the ready envelope if state is [RecordingState.Ready], else null. */
    fun getReadyEnvelope(): GestureEnvelope? =
        (recordingState.value as? RecordingState.Ready)?.envelope
}

enum class ValidationOutcome { MATCHED, PARTIAL, TIMED_OUT }
