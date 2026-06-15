package io.github.dianila68.gesturemacro.core.sensors

/**
 * ticket-031/032: Detects ambient light transitions.
 *
 * [GoingDarkDetector] fires [GesturePattern.GOING_DARK] when the light level
 * drops below a threshold and stays there for [minDarkMs].
 * [GoingBrightDetector] fires [GesturePattern.GOING_BRIGHT] when the light level
 * rises above a threshold.
 *
 * Sensitivity controls the threshold: higher sensitivity means a smaller change
 * in light level triggers the event.
 */
class GoingDarkDetector(sensitivity: Float = 0.5f) : GestureDetector {
    override val pattern = GesturePattern.GOING_DARK
    override val sensor = SensorType.LIGHT

    private val threshold: Float = lerp(DARK_THRESHOLD_STRICT, DARK_THRESHOLD_LENIENT, sensitivity)
    private val minDarkMs: Long = lerp(MIN_DARK_MS_STRICT, MIN_DARK_MS_LENIENT, sensitivity).toLong()

    private var darkSince = Long.MIN_VALUE
    private var firedForCurrentDark = false

    override fun onSample(sample: SensorSample): GestureEvent? {
        if (sample.sensor != SensorType.LIGHT) return null
        val lux = sample.v[0]
        when {
            lux <= threshold && darkSince == Long.MIN_VALUE -> {
                darkSince = sample.t
                firedForCurrentDark = false
            }
            lux > threshold -> {
                darkSince = Long.MIN_VALUE
                firedForCurrentDark = false
            }
            lux <= threshold && !firedForCurrentDark && (sample.t - darkSince) >= minDarkMs -> {
                firedForCurrentDark = true
                return GestureEvent(pattern = GesturePattern.GOING_DARK, t = sample.t, confidence = 1f)
            }
        }
        return null
    }

    companion object {
        const val DARK_THRESHOLD_STRICT = 5f     // < 5 lux (dim room) at sensitivity 0
        const val DARK_THRESHOLD_LENIENT = 50f   // < 50 lux (indoor light) at sensitivity 1
        const val MIN_DARK_MS_STRICT = 2_000f
        const val MIN_DARK_MS_LENIENT = 500f
    }
}

class GoingBrightDetector(sensitivity: Float = 0.5f) : GestureDetector {
    override val pattern = GesturePattern.GOING_BRIGHT
    override val sensor = SensorType.LIGHT

    private val threshold: Float = lerp(BRIGHT_THRESHOLD_LENIENT, BRIGHT_THRESHOLD_STRICT, sensitivity)
    private val minBrightMs: Long = lerp(MIN_BRIGHT_MS_STRICT, MIN_BRIGHT_MS_LENIENT, sensitivity).toLong()

    private var brightSince = Long.MIN_VALUE
    private var firedForCurrentBright = false

    override fun onSample(sample: SensorSample): GestureEvent? {
        if (sample.sensor != SensorType.LIGHT) return null
        val lux = sample.v[0]
        when {
            lux >= threshold && brightSince == Long.MIN_VALUE -> {
                brightSince = sample.t
                firedForCurrentBright = false
            }
            lux < threshold -> {
                brightSince = Long.MIN_VALUE
                firedForCurrentBright = false
            }
            lux >= threshold && !firedForCurrentBright && (sample.t - brightSince) >= minBrightMs -> {
                firedForCurrentBright = true
                return GestureEvent(pattern = GesturePattern.GOING_BRIGHT, t = sample.t, confidence = 1f)
            }
        }
        return null
    }

    companion object {
        const val BRIGHT_THRESHOLD_LENIENT = 100f   // > 100 lux at sensitivity 0
        const val BRIGHT_THRESHOLD_STRICT = 500f    // > 500 lux (outdoor) at sensitivity 1
        const val MIN_BRIGHT_MS_STRICT = 2_000f
        const val MIN_BRIGHT_MS_LENIENT = 500f
    }
}
