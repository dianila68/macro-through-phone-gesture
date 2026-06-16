package io.github.dianila68.gesturemacro.core.sensors

/**
 * ticket-031/032: Detects altitude changes from barometric pressure (TYPE_PRESSURE in hPa).
 *
 * Uses the hypsometric approximation: Δh ≈ −8.5 m per hPa near sea level.
 * Fires [GesturePattern.ALTITUDE_RISE] or [GesturePattern.ALTITUDE_FALL] when the
 * pressure changes by more than [deltaPaThreshold] over a sliding baseline window.
 *
 * The baseline is a 30-second rolling average; readings within the first [BASELINE_MS]
 * are collected before any event is fired.
 */
class AltitudeRiseDetector(sensitivity: Float = 0.5f) : GestureDetector {
    override val pattern = GesturePattern.ALTITUDE_RISE
    override val sensor = SensorType.PRESSURE

    private val impl = AltitudeImpl(sensitivity)
    override fun onSample(sample: SensorSample): GestureEvent? = impl.onSample(sample, rising = true)
}

class AltitudeFallDetector(sensitivity: Float = 0.5f) : GestureDetector {
    override val pattern = GesturePattern.ALTITUDE_FALL
    override val sensor = SensorType.PRESSURE

    private val impl = AltitudeImpl(sensitivity)
    override fun onSample(sample: SensorSample): GestureEvent? = impl.onSample(sample, rising = false)
}

private class AltitudeImpl(sensitivity: Float) {
    // Pressure drops when altitude rises; a drop of X hPa ≈ X*8.5 m rise
    private val deltaPaThreshold: Float = lerp(DELTA_STRICT, DELTA_LENIENT, sensitivity)

    private val baseline = ArrayDeque<Pair<Long, Float>>()
    private var baselineEstablished = false

    fun onSample(sample: SensorSample, rising: Boolean): GestureEvent? {
        if (sample.sensor != SensorType.PRESSURE) return null
        val hpa = sample.v[0]
        val t = sample.t

        baseline.addLast(t to hpa)
        // Prune baseline window
        while (baseline.isNotEmpty() && t - baseline.first().first > BASELINE_MS) {
            baseline.removeFirst()
        }
        if (!baselineEstablished && t - (baseline.firstOrNull()?.first ?: t) >= BASELINE_MS) {
            baselineEstablished = true
        }
        if (!baselineEstablished) return null

        val avg = baseline.map { it.second }.average().toFloat()
        val delta = avg - hpa // positive = pressure fell = altitude rose
        return when {
            rising && delta > deltaPaThreshold ->
                GestureEvent(pattern = GesturePattern.ALTITUDE_RISE, t = t, confidence = 1f)
            !rising && delta < -deltaPaThreshold ->
                GestureEvent(pattern = GesturePattern.ALTITUDE_FALL, t = t, confidence = 1f)
            else -> null
        }
    }

    companion object {
        const val BASELINE_MS = 30_000L
        const val DELTA_STRICT = 0.5f    // ≈ 4 m change (sensitive)
        const val DELTA_LENIENT = 2.0f   // ≈ 17 m change (robust)
    }
}
