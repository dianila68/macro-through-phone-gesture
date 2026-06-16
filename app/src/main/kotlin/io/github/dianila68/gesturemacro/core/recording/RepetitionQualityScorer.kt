package io.github.dianila68.gesturemacro.core.recording

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * ticket-047: Scores a single recording window [0.0, 1.0] across three factors.
 */
data class WindowQuality(
    val score: Float,
    val rating: QualityRating,
    val factors: Map<String, Float>,
)

object RepetitionQualityScorer {

    private const val LOW_QUALITY_THRESHOLD = 0.4f
    private const val GOOD_THRESHOLD = 0.7f

    /** maxWindowMs from RecordingConfig used to compute duration ratio. */
    fun score(window: SampleWindow, maxWindowMs: Long): WindowQuality {
        val accelFrames = window.frames.filter { it.channel == SensorChannel.ACCELEROMETER }
        if (accelFrames.isEmpty()) {
            return WindowQuality(0f, QualityRating.LOW_QUALITY, mapOf("reason" to 0f))
        }

        // Factor 1: duration ratio
        val ratio = window.durationMs.toFloat() / maxWindowMs.toFloat()
        val durationScore = when {
            ratio < 0.1f -> 0f
            ratio > 0.9f -> 0.3f
            ratio in 0.2f..0.7f -> 1f
            else -> 0.6f
        }

        // Factor 2: peak magnitude (subtract gravity ~9.81 m/s²)
        val magnitudes = accelFrames.map { it.magnitude }
        val peakMag = magnitudes.maxOrNull() ?: 0f
        val netPeak = (peakMag - 9.81f).coerceAtLeast(0f)
        val magnitudeScore = when {
            netPeak < 1.5f -> 0f
            netPeak > 4f -> 1f
            else -> (netPeak - 1.5f) / 2.5f
        }

        // Factor 3: spectral spread (frequency bin dominance via simple power ratio)
        val spreadScore = computeSpreadScore(magnitudes)

        val composite = (durationScore + magnitudeScore + spreadScore) / 3f
        val rating = when {
            composite < LOW_QUALITY_THRESHOLD -> QualityRating.LOW_QUALITY
            composite < GOOD_THRESHOLD -> QualityRating.ACCEPTABLE
            else -> QualityRating.GOOD
        }
        return WindowQuality(
            score = composite,
            rating = rating,
            factors = mapOf(
                "duration" to durationScore,
                "magnitude" to magnitudeScore,
                "spread" to spreadScore,
            ),
        )
    }

    private fun computeSpreadScore(magnitudes: List<Float>): Float {
        if (magnitudes.size < 4) return 0.5f
        val diffs = magnitudes.zipWithNext { a, b -> b - a }
        val mean = diffs.average().toFloat()
        val variance = diffs.map { (it - mean) * (it - mean) }.average().toFloat()
        val totalPower = magnitudes.map { it * it }.average().toFloat()
        if (totalPower < 0.001f) return 0f
        val dominanceFraction = 1f - (variance / totalPower).coerceIn(0f, 1f)
        return when {
            dominanceFraction > 0.8f -> 0f
            dominanceFraction < 0.5f -> 1f
            else -> (0.8f - dominanceFraction) / 0.3f
        }
    }
}

/**
 * ticket-047: Tracks coverage across all scored windows.
 */
class CoverageTracker {

    data class CoverageReport(
        val coverageScore: Float,
        val lowQualityCount: Int,
        val isEarlyAbortRecommended: Boolean,
        val requiredSamples: Int,
    )

    fun compute(windows: List<SampleWindow>, requiredSamples: Int): CoverageReport {
        val lowCount = windows.count { it.qualityRating == QualityRating.LOW_QUALITY }
        val earlyAbort = lowCount > kotlin.math.ceil(requiredSamples / 2.0)

        if (windows.size < 2) {
            return CoverageReport(0f, lowCount, earlyAbort, requiredSamples)
        }

        val traces = windows.map { downsample(accelMagnitudes(it), 20) }
        val distances = mutableListOf<Float>()
        for (i in traces.indices) {
            for (j in i + 1 until traces.size) {
                distances.add(euclideanDistance(traces[i], traces[j]))
            }
        }
        val mean = distances.average().toFloat()
        val std = if (distances.size > 1) {
            sqrt(distances.map { (it - mean) * (it - mean) }.average().toFloat())
        } else 0f

        val coverageScore = if (mean < 0.001f) 0f else (1f - (std / mean)).coerceIn(0f, 1f)
        return CoverageReport(coverageScore, lowCount, earlyAbort, requiredSamples)
    }

    private fun accelMagnitudes(window: SampleWindow): List<Float> =
        window.frames.filter { it.channel == SensorChannel.ACCELEROMETER }.map { it.magnitude }

    private fun downsample(values: List<Float>, targetSize: Int): FloatArray {
        if (values.isEmpty()) return FloatArray(targetSize)
        val result = FloatArray(targetSize)
        for (i in 0 until targetSize) {
            val srcIdx = (i.toFloat() / targetSize * values.size).toInt().coerceIn(0, values.size - 1)
            result[i] = values[srcIdx]
        }
        return result
    }

    private fun euclideanDistance(a: FloatArray, b: FloatArray): Float {
        return sqrt(a.zip(b.toTypedArray()).sumOf { (x, y) -> ((x - y) * (x - y)).toDouble() }.toFloat())
    }
}
