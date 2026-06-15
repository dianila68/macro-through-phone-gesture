package io.github.dianila68.gesturemacro.android.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import io.github.dianila68.gesturemacro.core.sensors.SensorSample
import io.github.dianila68.gesturemacro.core.sensors.SensorStream
import io.github.dianila68.gesturemacro.core.sensors.SensorType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * ticket-023: Android implementation of [SensorStream], quarantined to the
 * `.android.sensors` package so the engine stays Android-free.
 */
class AndroidSensorStream(context: Context) : SensorStream {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    /** Returns `Sensor.getMaximumRange()` for [type], or null if the sensor is absent. */
    fun sensorMaxRange(type: SensorType): Float? =
        sensorManager.getDefaultSensor(type.toAndroidType())?.maximumRange

    override fun samples(type: SensorType, samplingPeriodUs: Int, maxReportLatencyUs: Int): Flow<SensorSample> =
        callbackFlow {
            val sensor = sensorManager.getDefaultSensor(type.toAndroidType())
            if (sensor == null) {
                close(IllegalStateException("Sensor $type not present on this device"))
                return@callbackFlow
            }
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    trySend(SensorSample(type, event.timestamp / NANOS_PER_MILLI, event.values.clone()))
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            sensorManager.registerListener(listener, sensor, samplingPeriodUs, maxReportLatencyUs)
            awaitClose { sensorManager.unregisterListener(listener) }
        }

    private fun SensorType.toAndroidType(): Int = when (this) {
        SensorType.ACCELEROMETER -> Sensor.TYPE_ACCELEROMETER
        SensorType.GYROSCOPE -> Sensor.TYPE_GYROSCOPE
        SensorType.PROXIMITY -> Sensor.TYPE_PROXIMITY
        SensorType.STEP_COUNTER -> Sensor.TYPE_STEP_COUNTER
        SensorType.LIGHT -> Sensor.TYPE_LIGHT
        SensorType.PRESSURE -> Sensor.TYPE_PRESSURE
        SensorType.MAGNETOMETER -> Sensor.TYPE_MAGNETIC_FIELD
    }

    companion object {
        private const val NANOS_PER_MILLI = 1_000_000L
    }
}
