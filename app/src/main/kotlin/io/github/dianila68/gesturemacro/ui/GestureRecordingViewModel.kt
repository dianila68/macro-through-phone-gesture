package io.github.dianila68.gesturemacro.ui

import android.app.Application
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.dianila68.gesturemacro.android.data.RecordedGestureStore
import io.github.dianila68.gesturemacro.core.recording.CoverageUpdate
import io.github.dianila68.gesturemacro.core.recording.DefaultGestureRecordingSession
import io.github.dianila68.gesturemacro.core.recording.GestureEnvelope
import io.github.dianila68.gesturemacro.core.recording.RecordingConfig
import io.github.dianila68.gesturemacro.core.recording.RecordingState
import io.github.dianila68.gesturemacro.core.recording.SensorChannel
import io.github.dianila68.gesturemacro.core.recording.SensorFrame
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ticket-050: ViewModel for the gesture recording wizard.
 * Owns the DefaultGestureRecordingSession and wires live Android sensor frames into it.
 */
class GestureRecordingViewModel(application: Application) : AndroidViewModel(application) {

    private val sensorManager = application.getSystemService(android.content.Context.SENSOR_SERVICE) as SensorManager
    private val store = RecordedGestureStore(application)

    private val session = DefaultGestureRecordingSession()

    val recordingState: StateFlow<RecordingState> = session.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, RecordingState.Idle)

    private val _coverageHistory = MutableStateFlow<List<CoverageUpdate>>(emptyList())
    val coverageHistory: StateFlow<List<CoverageUpdate>> = _coverageHistory.asStateFlow()

    private val _savedId = MutableStateFlow<String?>(null)
    val savedId: StateFlow<String?> = _savedId.asStateFlow()

    private val config = RecordingConfig()

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val channel = when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> SensorChannel.ACCELEROMETER
                Sensor.TYPE_GYROSCOPE -> SensorChannel.GYROSCOPE
                else -> return
            }
            session.appendFrame(
                SensorFrame(
                    timestampNs = event.timestamp,
                    channel = channel,
                    values = event.values.copyOf(),
                )
            )
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    init {
        viewModelScope.launch {
            session.coverageUpdates.collect { update ->
                _coverageHistory.value = _coverageHistory.value + update
            }
        }
    }

    fun startRecording() {
        _coverageHistory.value = emptyList()
        _savedId.value = null
        registerSensors()
        session.start(config, viewModelScope)
    }

    fun cancel() {
        session.cancel()
        unregisterSensors()
    }

    fun save(name: String) {
        val state = recordingState.value
        if (state !is RecordingState.Ready) return
        viewModelScope.launch {
            val id = store.upsert(name = name, envelope = state.envelope)
            _savedId.value = id
            unregisterSensors()
        }
    }

    private fun registerSensors() {
        listOf(Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GYROSCOPE).forEach { type ->
            sensorManager.getDefaultSensor(type)?.let { sensor ->
                sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
            }
        }
    }

    private fun unregisterSensors() {
        sensorManager.unregisterListener(listener)
    }

    override fun onCleared() {
        unregisterSensors()
        super.onCleared()
    }
}
