package io.github.dianila68.gesturemacro.core.recording

import io.github.dianila68.gesturemacro.core.sensors.GestureDetector
import io.github.dianila68.gesturemacro.core.sensors.GestureEvent
import io.github.dianila68.gesturemacro.core.sensors.GesturePattern
import io.github.dianila68.gesturemacro.core.sensors.SensorSample
import io.github.dianila68.gesturemacro.core.sensors.SensorType
import io.github.dianila68.gesturemacro.core.sensors.SensorUtils

/**
 * Live detector that compares the incoming sensor stream against a [GestureEnvelope]
 * built from user-recorded repetitions (ticket-049).
 *
 * Detection is window-based:
 *   1. The last [windowSamples] samples are kept in a ring buffer.
 *   2. When the buffer is full, it is resampled to [EnvelopeBuilder.GRID_POINTS] buckets.
 *   3. Each resampled value is tested against the corresponding [EnvelopeBand].
 *   4. If the fraction of in-band values >= [matchThreshold] a GestureEvent is emitted.
 *
 * [pattern] is set to [GesturePattern.FALL] as a placeholder; at persistence time
 * a stable CUSTOM pattern per recorded gesture is stored in the macro model.
 */
class RecordedGestureDetector(
    private val envelope: GestureEnvelope,
    private val matchThreshold: Float = DEFAULT_MATCH_THRESHOLD,
    private val windowSamples: Int = EnvelopeBuilder.GRID_POINTS * 2,
    override val pattern: GesturePattern = GesturePattern.FALL,
    override val sensor: SensorType = SensorType.ACCELEROMETER,
) : GestureDetector {

    private val window = ArrayDeque<SensorSample>(windowSamples)
    private var lastEventT = Long.MIN_VALUE

    override fun feed(sample: SensorSample): GestureEvent? {
        if (sample.sensor != sensor) return null
        if (window.size >= windowSamples) window.removeFirst()
        window.addLast(sample)
        if (window.size < windowSamples) return null
        if (sample.t - lastEventT < MIN_COOLDOWN_MS) return null

        val resampled = resample(window.toList())
        val inBand = resampled.mapIndexed { t, values ->
            values.indices.all { axis ->
                axis < envelope.axisBands.size &&
                    t < envelope.axisBands[axis].size &&
                    envelope.axisBands[axis][t].contains(values[axis])
            }
        }.count { it }

        val score = inBand.toFloat() / EnvelopeBuilder.GRID_POINTS
        if (score < matchThreshold) return null

        lastEventT = sample.t
        return GestureEvent(pattern, sample.t, score)
    }

    override fun reset() {
        window.clear()
        lastEventT = Long.MIN_VALUE
    }

    private fun resample(samples: List<SensorSample>): List<FloatArray> {
        val axisCount = envelope.axisCount
        val tStart = samples.first().t.toFloat()
        val tEnd = samples.last().t.toFloat()
        val tRange = (tEnd - tStart).coerceAtLeast(1f)
        return (0 until EnvelopeBuilder.GRID_POINTS).map { gridIdx ->
            val targetT = tStart + tRange * gridIdx / (EnvelopeBuilder.GRID_POINTS - 1)
            val lo = samples.lastOrNull { it.t <= targetT.toLong() } ?: samples.first()
            val hi = samples.firstOrNull { it.t >= targetT.toLong() } ?: samples.last()
            val alpha = if (lo.t == hi.t) 0f else
                ((targetT - lo.t) / (hi.t - lo.t)).coerceIn(0f, 1f)
            FloatArray(axisCount) { axis ->
                val loV = if (axis < lo.v.size) lo.v[axis] else 0f
                val hiV = if (axis < hi.v.size) hi.v[axis] else 0f
                loV + alpha * (hiV - loV)
            }
        }
    }

    companion object {
        const val DEFAULT_MATCH_THRESHOLD = 0.70f
        const val MIN_COOLDOWN_MS = 1_500L
    }
}
