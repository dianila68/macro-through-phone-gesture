package io.github.dianila68.gesturemacro.core.recording

import io.github.dianila68.gesturemacro.core.sensors.SensorSample
import io.github.dianila68.gesturemacro.core.sensors.SensorUtils

data class RepetitionScore(
    val overall: Float,
    val feedback: String,
    val durationScore: Float,
    val amplitudeScore: Float,
    val peakScore: Float,
)

/**
 * Scores one captured repetition window (ticket-047).
 *
 * Scoring is heuristic and intentionally generous — the goal is to give
 * the user actionable feedback, not to reject ambiguous gestures.
 */
object RepetitionScorer {

    fun score(samples: List<SensorSample>): RepetitionScore {
        if (samples.size < MIN_SAMPLES) {
            return RepetitionScore(0f, "Too short — hold the gesture longer", 0f, 0f, 0f)
        }

        val durationMs = samples.last().t - samples.first().t
        val durationScore = when {
            durationMs < MIN_DURATION_MS -> 0f
            durationMs > MAX_DURATION_MS -> 0.4f
            else -> 1f
        }

        val magnitudes = samples.map { SensorUtils.magnitude(it.v) }
        val rms = SensorUtils.rollingRms(magnitudes.toFloatArray())
        val amplitudeScore = (rms / TARGET_RMS_G).coerceIn(0f, 1f)

        val peaks = countPeaks(magnitudes, PEAK_THRESHOLD_G)
        val peakScore = when {
            peaks < MIN_EXPECTED_PEAKS -> peaks.toFloat() / MIN_EXPECTED_PEAKS
            peaks > MAX_EXPECTED_PEAKS -> MAX_EXPECTED_PEAKS.toFloat() / peaks
            else -> 1f
        }

        val overall = (durationScore * 0.3f + amplitudeScore * 0.4f + peakScore * 0.3f)
            .coerceIn(0f, 1f)

        val feedback = when {
            overall >= 0.8f -> "Great gesture — clean and consistent"
            durationScore < 0.5f && durationMs < MIN_DURATION_MS -> "Too brief — slow down the motion"
            amplitudeScore < 0.4f -> "Too gentle — make the gesture more pronounced"
            peakScore < 0.4f -> "Missing motion peaks — make sure the gesture is complete"
            else -> "Acceptable — try to be more deliberate"
        }

        return RepetitionScore(overall, feedback, durationScore, amplitudeScore, peakScore)
    }

    private fun countPeaks(magnitudes: List<Float>, threshold: Float): Int {
        var peaks = 0
        for (i in 1 until magnitudes.size - 1) {
            if (magnitudes[i] > threshold &&
                magnitudes[i] > magnitudes[i - 1] &&
                magnitudes[i] >= magnitudes[i + 1]
            ) peaks++
        }
        return peaks
    }

    private val MIN_SAMPLES = 10
    private val MIN_DURATION_MS = 150L
    private val MAX_DURATION_MS = 3_000L
    private const val TARGET_RMS_G = 15f
    private const val PEAK_THRESHOLD_G = 12f
    private const val MIN_EXPECTED_PEAKS = 1
    private const val MAX_EXPECTED_PEAKS = 8
}
