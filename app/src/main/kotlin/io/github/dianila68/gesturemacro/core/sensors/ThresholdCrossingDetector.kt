package io.github.dianila68.gesturemacro.core.sensors

/**
 * ticket-053: Generic rising/falling edge detector for slow scalar sensors
 * (ambient temperature, relative humidity).
 *
 * Fires [patternHigh] when the value crosses [thresholdHigh] upward and stays
 * above for [debounceSamples] consecutive samples. Fires [patternLow] on the
 * downward crossing of [thresholdLow]. The hysteresis band ([thresholdLow]..[thresholdHigh])
 * prevents chattering near the crossing point.
 *
 * sensitivity maps to hysteresis:
 *   loose (0.0) = wide band (5°C / 10%)
 *   tight (1.0) = narrow band (0.5°C / 1%)
 */
class ThresholdCrossingDetector(
    override val sensor: SensorType,
    private val patternHigh: GesturePattern,
    private val patternLow: GesturePattern,
    private val thresholdLow: Float,
    private val thresholdHigh: Float,
    private val debounceSamples: Int = 3,
) : GestureDetector {
    // GestureDetector.pattern is the "primary" pattern this detector reports;
    // ThresholdCrossing can fire both high and low, so we use patternHigh as primary.
    override val pattern: GesturePattern get() = patternHigh

    private var aboveHigh = false
    private var belowLow = false
    private var aboveCount = 0
    private var belowCount = 0

    override fun feed(sample: SensorSample): GestureEvent? {
        if (sample.sensor != sensor || sample.v.isEmpty()) return null
        val v = sample.v[0]
        return when {
            v >= thresholdHigh -> {
                belowCount = 0
                if (!aboveHigh) {
                    aboveCount++
                    if (aboveCount >= debounceSamples) {
                        aboveHigh = true; belowLow = false; aboveCount = 0
                        GestureEvent(patternHigh, sample.t, confidence = 1f)
                    } else null
                } else null
            }
            v <= thresholdLow -> {
                aboveCount = 0
                if (!belowLow) {
                    belowCount++
                    if (belowCount >= debounceSamples) {
                        belowLow = true; aboveHigh = false; belowCount = 0
                        GestureEvent(patternLow, sample.t, confidence = 1f)
                    } else null
                } else null
            }
            else -> { aboveCount = 0; belowCount = 0; null }
        }
    }

    override fun reset() {
        aboveHigh = false
        belowLow = false
        aboveCount = 0
        belowCount = 0
    }
}
