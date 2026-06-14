package io.github.dianila68.gesturemacro.core.sensors

import kotlin.math.sqrt

/**
 * Detects a likely fall from accelerometer data by recognising a three-phase
 * signature:
 *
 * 1. **Free-fall** — acceleration magnitude drops close to 0 g for at least
 *    [MIN_FREEFALL_MS]. Normal gravity is absent; the device (and ideally the
 *    person wearing it) is in free flight.
 * 2. **Impact** — a sharp spike well above 1 g occurs within [IMPACT_WINDOW_MS]
 *    after the free-fall ends. This is the body or phone hitting the ground.
 * 3. **Post-impact stillness** — magnitude variance stays below
 *    [stillnessVarThreshold] for [STILLNESS_DURATION_MS]. A person who has fallen
 *    and not immediately recovered remains still; a phone set down hard or dropped
 *    on a couch does not show the same stillness tail.
 *
 * **Not medical-grade.** Both false positives and false negatives exist — never
 * imply guaranteed detection. The phone must be on the body to detect a *person's*
 * fall. Pair with a confirm-countdown action (ticket-043) so users can cancel a
 * false alarm before anything is sent. Detection improves with a wrist or
 * secondary-device sensor (M4 bridge).
 */
class FallDetector(sensitivity: Float = 0.5f) : GestureDetector {
    override val pattern = GesturePattern.FALL
    override val sensor = SensorType.ACCELEROMETER

    // Higher sensitivity: wider free-fall band, lower impact threshold, higher stillness tolerance.
    private val freefallMaxMag =
        lerp(FREEFALL_MAX_G_STRICT, FREEFALL_MAX_G_LENIENT, sensitivity) * EARTH_GRAVITY
    private val impactMinMag =
        lerp(IMPACT_G_STRICT, IMPACT_G_LENIENT, sensitivity) * EARTH_GRAVITY
    private val stillnessVarThreshold =
        lerp(STILLNESS_VAR_STRICT, STILLNESS_VAR_LENIENT, sensitivity)

    private var state = State.IDLE
    private var freefallStart = Long.MIN_VALUE
    private var impactTime = Long.MIN_VALUE
    private val stillnessMags = ArrayDeque<Pair<Long, Float>>()

    private enum class State { IDLE, FREEFALL, IMPACT_WAIT, STILLNESS_WATCH }

    override fun feed(sample: SensorSample): GestureEvent? {
        if (sample.sensor != SensorType.ACCELEROMETER || sample.v.size < 3) return null
        val mag = magnitude(sample.v)
        val t = sample.t
        return when (state) {
            State.IDLE -> {
                if (mag < freefallMaxMag) {
                    state = State.FREEFALL
                    freefallStart = t
                }
                null
            }
            State.FREEFALL -> {
                if (mag < freefallMaxMag) return null
                if (t - freefallStart >= MIN_FREEFALL_MS) {
                    state = State.IMPACT_WAIT
                } else {
                    // Free-fall too brief — spurious noise; reset.
                    state = State.IDLE
                    freefallStart = Long.MIN_VALUE
                }
                null
            }
            State.IMPACT_WAIT -> {
                if (t - freefallStart > IMPACT_WINDOW_MS) {
                    state = State.IDLE
                    return null
                }
                if (mag >= impactMinMag) {
                    state = State.STILLNESS_WATCH
                    impactTime = t
                    stillnessMags.clear()
                }
                null
            }
            State.STILLNESS_WATCH -> {
                // A large secondary motion spike means the person is moving — not a fall.
                if (mag > impactMinMag * MOTION_ABORT_FRACTION) {
                    state = State.IDLE
                    stillnessMags.clear()
                    return null
                }
                stillnessMags.addLast(t to mag)
                if (t - impactTime >= STILLNESS_DURATION_MS) {
                    val varResult = variance(stillnessMags.map { it.second })
                    state = State.IDLE
                    stillnessMags.clear()
                    if (varResult <= stillnessVarThreshold) {
                        val confidence = (1f - varResult / stillnessVarThreshold).coerceIn(0f, 1f)
                        GestureEvent(pattern, t, confidence)
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        }
    }

    override fun reset() {
        state = State.IDLE
        freefallStart = Long.MIN_VALUE
        impactTime = Long.MIN_VALUE
        stillnessMags.clear()
    }

    companion object {
        /** Fraction of g below which = free-fall (strict/lenient). */
        const val FREEFALL_MAX_G_STRICT = 0.3f
        const val FREEFALL_MAX_G_LENIENT = 0.6f

        /** Free-fall must last at least this long; shorter bursts are sensor noise. */
        const val MIN_FREEFALL_MS = 60L

        /** Multiple of g above which = impact spike (strict requires harder hit). */
        const val IMPACT_G_STRICT = 3.0f
        const val IMPACT_G_LENIENT = 2.0f

        /** Impact must arrive within this window after free-fall ends. */
        const val IMPACT_WINDOW_MS = 2_000L

        /** Observation window for post-impact stillness. */
        const val STILLNESS_DURATION_MS = 2_500L

        /** Variance of magnitudes (m/s²)² below which = still (strict/lenient). */
        const val STILLNESS_VAR_STRICT = 0.5f
        const val STILLNESS_VAR_LENIENT = 3.0f

        /** Fraction of impact threshold above which aborts the stillness watch. */
        const val MOTION_ABORT_FRACTION = 0.5f
    }
}

private fun magnitude(v: FloatArray): Float =
    sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])

private fun variance(values: List<Float>): Float {
    if (values.size < 2) return Float.MAX_VALUE
    val mean = values.average().toFloat()
    return values.map { d -> val diff = d - mean; diff * diff }.average().toFloat()
}
