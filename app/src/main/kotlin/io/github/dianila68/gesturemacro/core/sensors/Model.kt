package io.github.dianila68.gesturemacro.core.sensors

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class SensorType {
    @SerialName("accelerometer") ACCELEROMETER,
    @SerialName("gyroscope") GYROSCOPE,
    @SerialName("proximity") PROXIMITY,
    /** Hardware step counter (TYPE_STEP_COUNTER — cumulative total since last reboot). */
    @SerialName("step_counter") STEP_COUNTER,
    /** Ambient light level in lux (TYPE_LIGHT). */
    @SerialName("light") LIGHT,
    /** Barometric pressure in hPa (TYPE_PRESSURE). */
    @SerialName("pressure") PRESSURE,
    /** External (remote) sensor device (ticket-054). */
    @SerialName("external") EXTERNAL,
    /** Android TYPE_SIGNIFICANT_MOTION one-shot trigger (ticket-047). */
    @SerialName("significant_motion") SIGNIFICANT_MOTION,
    /** Android TYPE_ROTATION_VECTOR quaternion sensor (ticket-051). */
    @SerialName("rotation_vector") ROTATION_VECTOR,
    /** Ambient temperature in °C (TYPE_AMBIENT_TEMPERATURE) (ticket-053). */
    @SerialName("ambient_temperature") AMBIENT_TEMPERATURE,
    /** Relative humidity in % (TYPE_RELATIVE_HUMIDITY) (ticket-053). */
    @SerialName("relative_humidity") RELATIVE_HUMIDITY,
    /** Magnetic field / compass heading (TYPE_MAGNETIC_FIELD) (ticket-031). */
    @SerialName("magnetometer") MAGNETOMETER,
    /** Google Play Services Activity Recognition API (ticket-048). */
    @SerialName("activity_recognition") ACTIVITY_RECOGNITION,
}

@Serializable
enum class GesturePattern {
    @SerialName("shake") SHAKE,
    @SerialName("double_shake") DOUBLE_SHAKE,
    @SerialName("flip_face_down") FLIP_FACE_DOWN,
    @SerialName("flip_face_up") FLIP_FACE_UP,
    @SerialName("twist") TWIST,
    @SerialName("proximity_wave") PROXIMITY_WAVE,
    @SerialName("fall") FALL,
    // M4 single-sensor use cases (ticket-032)
    @SerialName("step_detected") STEP_DETECTED,
    @SerialName("is_stationary") IS_STATIONARY,
    @SerialName("picked_up") PICKED_UP,
    @SerialName("going_dark") GOING_DARK,
    @SerialName("going_bright") GOING_BRIGHT,
    @SerialName("altitude_rise") ALTITUDE_RISE,
    @SerialName("altitude_fall") ALTITUDE_FALL,
    // ticket-047
    @SerialName("significant_motion") SIGNIFICANT_MOTION,
    // ticket-051
    @SerialName("rotation_changed") ROTATION_CHANGED,
    // ticket-053
    @SerialName("temperature_high") TEMPERATURE_HIGH,
    @SerialName("temperature_low") TEMPERATURE_LOW,
    @SerialName("humidity_high") HUMIDITY_HIGH,
    @SerialName("humidity_low") HUMIDITY_LOW,
    // ticket-054
    @SerialName("external_threshold") EXTERNAL_THRESHOLD,
    // ticket-049
    @SerialName("recorded_gesture") RECORDED_GESTURE,
    // ticket-031 magnetometer heading
    @SerialName("heading_changed") HEADING_CHANGED,
    // ticket-048 activity recognition
    @SerialName("activity_walking") ACTIVITY_WALKING,
    @SerialName("activity_running") ACTIVITY_RUNNING,
    @SerialName("activity_in_vehicle") ACTIVITY_IN_VEHICLE,
    @SerialName("activity_on_bicycle") ACTIVITY_ON_BICYCLE,
    @SerialName("activity_still") ACTIVITY_STILL,
}

/** One sensor reading; [t] is milliseconds on a monotonic clock. */
class SensorSample(
    val sensor: SensorType,
    val t: Long,
    val v: FloatArray,
)

class GestureEvent(
    val pattern: GesturePattern,
    val t: Long,
    val confidence: Float,
)

const val EARTH_GRAVITY = 9.81f

/** Maps sensitivity 0..1 between a loose and a tight threshold. */
internal fun lerp(loose: Float, tight: Float, sensitivity: Float): Float =
    loose + (tight - loose) * sensitivity.coerceIn(0f, 1f)
