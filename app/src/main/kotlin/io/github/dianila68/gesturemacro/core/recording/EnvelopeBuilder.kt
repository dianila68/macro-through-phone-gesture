package io.github.dianila68.gesturemacro.core.recording

import io.github.dianila68.gesturemacro.core.sensors.SensorSample
import kotlin.math.abs

data class EnvelopeBand(val mean: Float, val tolerance: Float) {
    fun contains(value: Float) = abs(value - mean) <= tolerance
}

data class GestureEnvelope(
    /** Per-axis (index 0–2) list of [GRID_POINTS] bands from start to end of gesture. */
    val axisBands: List<List<EnvelopeBand>>,
    val axisCount: Int,
    val gridPoints: Int,
)

/**
 * Builds a [GestureEnvelope] from multiple repetitions (ticket-048).
 *
 * Each repetition is resampled to [GRID_POINTS] evenly-spaced time buckets via
 * linear interpolation so all repetitions share a common normalised time axis.
 * The per-bucket mean and tolerance (1.5 × std dev, clamped to [MIN_TOLERANCE])
 * are then computed independently for each sensor axis.
 */
object EnvelopeBuilder {

    const val GRID_POINTS = 50
    const val TOLERANCE_SIGMA = 1.5f
    const val MIN_TOLERANCE = 0.5f

    fun build(repetitions: List<List<SensorSample>>): GestureEnvelope? {
        val nonEmpty = repetitions.filter { it.size >= 2 }
        if (nonEmpty.isEmpty()) return null
        val axisCount = nonEmpty.first().firstOrNull()?.v?.size ?: return null

        val resampled: List<Array<FloatArray>> = nonEmpty.map { resample(it, axisCount) }

        val axisBands = (0 until axisCount).map { axis ->
            (0 until GRID_POINTS).map { t ->
                val values = resampled.map { rep -> rep[axis][t] }
                val mean = values.average().toFloat()
                val variance = values.map { v -> val d = v - mean; d * d }.average().toFloat()
                val sigma = kotlin.math.sqrt(variance)
                EnvelopeBand(mean, maxOf(MIN_TOLERANCE, sigma * TOLERANCE_SIGMA))
            }
        }
        return GestureEnvelope(axisBands, axisCount, GRID_POINTS)
    }

    /** Resamples [samples] into [GRID_POINTS] × [axisCount] grid via linear interpolation. */
    private fun resample(samples: List<SensorSample>, axisCount: Int): Array<FloatArray> {
        val result = Array(axisCount) { FloatArray(GRID_POINTS) }
        val tStart = samples.first().t.toFloat()
        val tEnd = samples.last().t.toFloat()
        val tRange = (tEnd - tStart).coerceAtLeast(1f)

        for (gridIdx in 0 until GRID_POINTS) {
            val targetT = tStart + tRange * gridIdx / (GRID_POINTS - 1)
            val (lo, hi) = findNeighbours(samples, targetT)
            val alpha = if (lo == hi) 0f else
                ((targetT - lo.t) / (hi.t - lo.t)).toFloat().coerceIn(0f, 1f)
            for (axis in 0 until axisCount) {
                result[axis][gridIdx] = lo.v[axis] + alpha * (hi.v[axis] - lo.v[axis])
            }
        }
        return result
    }

    private fun findNeighbours(
        samples: List<SensorSample>,
        targetT: Float,
    ): Pair<SensorSample, SensorSample> {
        var lo = samples.first()
        var hi = samples.last()
        for (i in samples.indices) {
            if (samples[i].t <= targetT) lo = samples[i]
            if (samples[i].t >= targetT) { hi = samples[i]; break }
        }
        return lo to hi
    }
}
