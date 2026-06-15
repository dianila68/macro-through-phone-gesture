package io.github.dianila68.gesturemacro.core.sensors

enum class SensorType {
    ACCELEROMETER,
    GYROSCOPE,
    PROXIMITY,
    /** Hardware step counter (TYPE_STEP_COUNTER — cumulative total since last reboot). */
    STEP_COUNTER,
    /** Ambient light level in lux (TYPE_LIGHT). */
    LIGHT,
    /** Barometric pressure in hPa (TYPE_PRESSURE). */
    PRESSURE,
}

enum class GesturePattern {
    SHAKE,
    DOUBLE_SHAKE,
    FLIP_FACE_DOWN,
    FLIP_FACE_UP,
    TWIST,
    PROXIMITY_WAVE,
    FALL,
    // M4 single-sensor use cases (ticket-032)
    STEP_DETECTED,
    IS_STATIONARY,
    PICKED_UP,
    GOING_DARK,
    GOING_BRIGHT,
    ALTITUDE_RISE,
    ALTITUDE_FALL,
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
