package io.github.dianila68.gesturemacro.core.recording

import kotlinx.serialization.Serializable
import kotlin.math.sqrt

/**
 * ticket-048: Parameterised tolerance band describing a recorded gesture.
 * Serializable for Room persistence (ticket-051).
 */
@Serializable
data class GestureEnvelope(
    val version: Int = 1,
    val sliceCount: Int,
    val magnitudeMean: FloatArray,
    val magnitudeStd: FloatArray,
    val gyroMean: FloatArray? = null,
    val gyroStd: FloatArray? = null,
    val durationMeanMs: Float,
    val durationStdMs: Float,
    val sampleCount: Int,
    val confidence: Float,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GestureEnvelope) return false
        return sliceCount == other.sliceCount &&
            magnitudeMean.contentEquals(other.magnitudeMean) &&
            magnitudeStd.contentEquals(other.magnitudeStd) &&
            sampleCount == other.sampleCount
    }

    override fun hashCode(): Int = 31 * sliceCount + magnitudeMean.contentHashCode()
}

object GestureEnvelopeBuilder {

    private const val DEFAULT_SLICE_COUNT = 30

    fun build(
        windows: List<SampleWindow>,
        config: RecordingConfig,
        sliceCount: Int = DEFAULT_SLICE_COUNT,
    ): GestureEnvelope {
        require(sliceCount in 10..60) { "sliceCount must be 10–60" }
        require(windows.isNotEmpty()) { "Need at least one window" }

        val lowCount = windows.count { it.qualityRating == QualityRating.LOW_QUALITY }
        val usable = if (windows.size - lowCount >= config.minSamples) {
            windows.filter { it.qualityRating != QualityRating.LOW_QUALITY }
        } else windows

        val resampled = usable.map { window ->
            val magnitudes = window.frames
                .filter { it.channel == SensorChannel.ACCELEROMETER }
                .map { it.magnitude }
            resample(magnitudes, sliceCount)
        }

        val mean = FloatArray(sliceCount) { i -> resampled.map { it[i] }.average().toFloat() }
        val std = FloatArray(sliceCount) { i ->
            val vals = resampled.map { it[i] }
            val m = vals.average().toFloat()
            sqrt(vals.map { (it - m) * (it - m) }.average().toFloat())
        }

        val hasGyro = config.sensors.contains(SensorChannel.GYROSCOPE)
        val gyroResampled = if (hasGyro) {
            usable.map { window ->
                val mags = window.frames
                    .filter { it.channel == SensorChannel.GYROSCOPE }
                    .map { it.magnitude }
                resample(mags, sliceCount)
            }
        } else null

        val gyroMean = gyroResampled?.let { gr ->
            FloatArray(sliceCount) { i -> gr.map { it[i] }.average().toFloat() }
        }
        val gyroStd = gyroResampled?.let { gr ->
            FloatArray(sliceCount) { i ->
                val vals = gr.map { it[i] }
                val m = vals.average().toFloat()
                sqrt(vals.map { (it - m) * (it - m) }.average().toFloat())
            }
        }

        val durations = usable.map { it.durationMs.toFloat() }
        val durationMean = durations.average().toFloat()
        val durationStd = sqrt(durations.map { (it - durationMean) * (it - durationMean) }.average().toFloat())

        val cvs = (0 until sliceCount).map { i ->
            if (mean[i] > 0.001f) std[i] / mean[i] else 1f
        }
        val confidence = (1f - cvs.average().toFloat().coerceIn(0f, 1f))

        return GestureEnvelope(
            sliceCount = sliceCount,
            magnitudeMean = mean,
            magnitudeStd = std,
            gyroMean = gyroMean,
            gyroStd = gyroStd,
            durationMeanMs = durationMean,
            durationStdMs = durationStd,
            sampleCount = usable.size,
            confidence = confidence,
        )
    }

    fun resample(values: List<Float>, targetSize: Int): FloatArray {
        if (values.isEmpty()) return FloatArray(targetSize)
        if (values.size == 1) return FloatArray(targetSize) { values[0] }
        return FloatArray(targetSize) { i ->
            val srcPos = i.toFloat() / (targetSize - 1) * (values.size - 1)
            val lo = srcPos.toInt().coerceIn(0, values.size - 2)
            val hi = lo + 1
            val frac = srcPos - lo
            values[lo] * (1f - frac) + values[hi] * frac
        }
    }
}
