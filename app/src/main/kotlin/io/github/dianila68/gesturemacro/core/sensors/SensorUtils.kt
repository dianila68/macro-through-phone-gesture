package io.github.dianila68.gesturemacro.core.sensors

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Pure-Kotlin signal-processing utilities shared across gesture detectors (ticket-031).
 * No Android dependency — fully testable on the JVM.
 */
object SensorUtils {

    fun magnitude(v: FloatArray): Float =
        sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])

    /**
     * Root-mean-square of [window] values; 0 for empty input.
     * Useful for estimating sustained acceleration energy over a rolling buffer.
     */
    fun rollingRms(window: FloatArray): Float {
        if (window.isEmpty()) return 0f
        val sumSq = window.fold(0.0) { acc, x -> acc + x * x }
        return sqrt((sumSq / window.size).toFloat())
    }

    /**
     * Exponential low-pass filter: blends [input] into [previous] with smoothing
     * factor [alpha] ∈ (0, 1). alpha = 0 → output tracks input instantly;
     * alpha = 1 → output never changes.
     */
    fun lowPass(input: Float, previous: Float, alpha: Float): Float =
        alpha * previous + (1f - alpha) * input

    /** Sample variance; returns Float.MAX_VALUE for fewer than 2 samples. */
    fun variance(values: List<Float>): Float {
        if (values.size < 2) return Float.MAX_VALUE
        val mean = values.average().toFloat()
        return values.map { d -> val diff = d - mean; diff * diff }.average().toFloat()
    }

    /**
     * Compass heading in degrees [0, 360) from a magnetometer XY reading.
     * Assumes the device is lying flat (no tilt compensation).
     */
    fun headingDegrees(v: FloatArray): Float {
        val heading = Math.toDegrees(atan2(v[1].toDouble(), v[0].toDouble())).toFloat()
        return (heading + 360f) % 360f
    }

    /**
     * Smallest angular difference between headings [a] and [b] in degrees,
     * always in [0, 180].
     */
    fun angleDifferenceDeg(a: Float, b: Float): Float {
        val diff = ((b - a + 540f) % 360f) - 180f
        return abs(diff)
    }

    /**
     * Step rate from a monotonically increasing step counter.
     * Returns steps per minute given [deltaSteps] observed over [deltaMs] milliseconds.
     */
    fun stepsPerMinute(deltaSteps: Int, deltaMs: Long): Float {
        if (deltaMs <= 0) return 0f
        return deltaSteps * 60_000f / deltaMs
    }
}
