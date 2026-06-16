package io.github.dianila68.gesturemacro.core.sensors

/**
 * ticket-031/032: Detects a single step event from the hardware step counter sensor
 * (TYPE_STEP_COUNTER, which reports a cumulative total since last reboot).
 *
 * Fires a [GesturePattern.STEP_DETECTED] event on each increment of the counter.
 * Sensitivity is not applicable to a discrete hardware counter; the field is accepted
 * for API compatibility but ignored.
 */
class StepDetector(
    @Suppress("UNUSED_PARAMETER") sensitivity: Float = 0.5f,
) : GestureDetector {
    override val pattern = GesturePattern.STEP_DETECTED
    override val sensor = SensorType.STEP_COUNTER

    private var lastCount = -1L

    override fun onSample(sample: SensorSample): GestureEvent? {
        if (sample.sensor != SensorType.STEP_COUNTER) return null
        val count = sample.v[0].toLong()
        val prev = lastCount
        lastCount = count
        // Ignore the first sample (baseline establishment) and counter resets.
        if (prev < 0 || count <= prev) return null
        return GestureEvent(pattern = GesturePattern.STEP_DETECTED, t = sample.t, confidence = 1f)
    }
}

/**
 * ticket-031/032: Reports [GesturePattern.IS_STATIONARY] when the device has been
 * still (step count not incrementing) for at least [minStillMs].
 *
 * Fires once when the device transitions from moving to stationary; does not re-fire
 * while it stays stationary (edge-trigger semantics consistent with fall detector).
 * Sensitivity controls how long the device must be still before the event fires.
 */
class StationaryDetector(sensitivity: Float = 0.5f) : GestureDetector {
    override val pattern = GesturePattern.IS_STATIONARY
    override val sensor = SensorType.STEP_COUNTER

    private val minStillMs: Long = lerp(
        STILL_MIN_MS_LENIENT,
        STILL_MIN_MS_STRICT,
        sensitivity,
    ).toLong()

    private var lastCount = -1L
    private var lastStepTime = Long.MIN_VALUE
    private var firedForCurrentStillPeriod = false

    override fun onSample(sample: SensorSample): GestureEvent? {
        if (sample.sensor != SensorType.STEP_COUNTER) return null
        val count = sample.v[0].toLong()
        if (lastCount < 0) {
            lastCount = count
            lastStepTime = sample.t
            return null
        }
        if (count > lastCount) {
            lastCount = count
            lastStepTime = sample.t
            firedForCurrentStillPeriod = false
            return null
        }
        if (firedForCurrentStillPeriod) return null
        val stillFor = sample.t - lastStepTime
        if (stillFor >= minStillMs) {
            firedForCurrentStillPeriod = true
            return GestureEvent(pattern = GesturePattern.IS_STATIONARY, t = sample.t, confidence = 1f)
        }
        return null
    }

    companion object {
        const val STILL_MIN_MS_LENIENT = 5_000f   // 5 s at sensitivity 0
        const val STILL_MIN_MS_STRICT = 30_000f   // 30 s at sensitivity 1
    }
}

/**
 * ticket-031/032: Fires [GesturePattern.PICKED_UP] when accelerometer magnitude
 * leaves the gravity band (device lifted off a surface).
 *
 * A device resting flat registers ≈ 9.81 m/s² (1 g). When picked up, the Z-axis
 * component changes and the total magnitude deviates from 1 g. This detector fires
 * when the magnitude departs from the gravity band for at least [minDepartMs].
 */
class PickedUpDetector(sensitivity: Float = 0.5f) : GestureDetector {
    override val pattern = GesturePattern.PICKED_UP
    override val sensor = SensorType.ACCELEROMETER

    private val departThreshold: Float = lerp(DEPART_STRICT, DEPART_LENIENT, sensitivity)
    private val minDepartMs: Long = lerp(MIN_DEPART_MS_STRICT, MIN_DEPART_MS_LENIENT, sensitivity).toLong()

    private var departStart = Long.MIN_VALUE
    private var fired = false

    override fun onSample(sample: SensorSample): GestureEvent? {
        if (sample.sensor != SensorType.ACCELEROMETER) return null
        val mag = sample.v.magnitude()
        val departed = kotlin.math.abs(mag - EARTH_GRAVITY) > departThreshold
        when {
            departed && departStart == Long.MIN_VALUE -> {
                departStart = sample.t
                fired = false
            }
            !departed -> {
                departStart = Long.MIN_VALUE
                fired = false
            }
            departed && !fired && (sample.t - departStart) >= minDepartMs -> {
                fired = true
                return GestureEvent(pattern = GesturePattern.PICKED_UP, t = sample.t, confidence = 1f)
            }
        }
        return null
    }

    companion object {
        const val DEPART_STRICT = 1.5f   // must differ from 1g by 1.5 m/s² (high sensitivity 0)
        const val DEPART_LENIENT = 3.0f  // must differ by 3.0 m/s² (low sensitivity 1)
        const val MIN_DEPART_MS_STRICT = 300f
        const val MIN_DEPART_MS_LENIENT = 100f
    }
}

private fun FloatArray.magnitude(): Float {
    var sum = 0f
    for (v in this) sum += v * v
    return kotlin.math.sqrt(sum)
}
