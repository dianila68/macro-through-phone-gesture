package io.github.dianila68.gesturemacro.core.recording

import kotlin.math.sqrt

object GestureEnvelopeBuilder {

    private const val DEFAULT_SLICE_COUNT = 30
    private const val SLICE_COUNT_MIN = 10
    private const val SLICE_COUNT_MAX = 60

    /**
     * Builds a [GestureEnvelope] from the collected [windows].
     * Returns null if there are no usable windows after filtering.
     *
     * [sliceCount] must be in 10..60.
     */
    fun build(
        windows: List<SampleWindow>,
        config: RecordingConfig,
        sliceCount: Int = DEFAULT_SLICE_COUNT,
    ): GestureEnvelope? {
        require(sliceCount in SLICE_COUNT_MIN..SLICE_COUNT_MAX) { "sliceCount must be in $SLICE_COUNT_MIN..$SLICE_COUNT_MAX" }

        val scorer = RepetitionQualityScorer()

        // Filter: drop LOW_QUALITY only if enough remain
        val lowQuality = windows.filter { scorer.score(it, config) < RepetitionQualityScorer.LOW_QUALITY_THRESHOLD }
        val usable = if (windows.size - lowQuality.size >= config.minSamples) {
            windows.filter { scorer.score(it, config) >= RepetitionQualityScorer.LOW_QUALITY_THRESHOLD }
        } else {
            windows // keep all if filtering leaves too few
        }

        if (usable.isEmpty()) return null

        // Time-normalise each window's accelerometer magnitude to [sliceCount] points
        val accelTraces = usable.map { w -> timeNormalise(accelMagnitudes(w), sliceCount) }

        val magnitudeMean = FloatArray(sliceCount) { i -> accelTraces.map { it[i] }.average().toFloat() }
        val magnitudeStd = FloatArray(sliceCount) { i ->
            std(accelTraces.map { it[i] })
        }

        // Optional gyroscope
        val hasGyro = config.channels.contains(RecordingChannel.GYROSCOPE) &&
            usable.any { w -> w.frames.any { it.channel == RecordingChannel.GYROSCOPE } }

        val gyroMean: FloatArray?
        val gyroStd: FloatArray?
        if (hasGyro) {
            val gyroTraces = usable.map { w -> timeNormalise(gyroMagnitudes(w), sliceCount) }
            gyroMean = FloatArray(sliceCount) { i -> gyroTraces.map { it[i] }.average().toFloat() }
            gyroStd = FloatArray(sliceCount) { i -> std(gyroTraces.map { it[i] }) }
        } else {
            gyroMean = null
            gyroStd = null
        }

        // Duration stats
        val durations = usable.map { it.durationMs.toFloat() }
        val durationMean = durations.average().toFloat()
        val durationStd = std(durations)

        // Confidence from coverage
        val coverageTracker = CoverageTracker()
        val report = coverageTracker.update(usable, config)

        return GestureEnvelope(
            sliceCount = sliceCount,
            magnitudeMean = magnitudeMean,
            magnitudeStd = magnitudeStd,
            gyroMean = gyroMean,
            gyroStd = gyroStd,
            durationMeanMs = durationMean,
            durationStdMs = durationStd,
            sampleCount = usable.size,
            confidence = report.coverageScore,
        )
    }

    private fun accelMagnitudes(window: SampleWindow): List<Float> =
        window.frames
            .filter { it.channel == RecordingChannel.ACCELEROMETER }
            .map { f -> sqrt(f.values[0] * f.values[0] + f.values[1] * f.values[1] + f.values[2] * f.values[2]) }

    private fun gyroMagnitudes(window: SampleWindow): List<Float> =
        window.frames
            .filter { it.channel == RecordingChannel.GYROSCOPE }
            .map { f -> sqrt(f.values[0] * f.values[0] + f.values[1] * f.values[1] + f.values[2] * f.values[2]) }

    /** Linear-interpolation resample to [points] points. */
    fun timeNormalise(src: List<Float>, points: Int): FloatArray {
        if (src.isEmpty()) return FloatArray(points)
        if (src.size == 1) return FloatArray(points) { src[0] }
        return FloatArray(points) { i ->
            val pos = i.toFloat() / (points - 1) * (src.size - 1)
            val lo = pos.toInt().coerceIn(0, src.size - 2)
            val hi = lo + 1
            val frac = pos - lo
            src[lo] * (1f - frac) + src[hi] * frac
        }
    }

    private fun std(values: List<Float>): Float {
        if (values.size < 2) return 0f
        val mean = values.average().toFloat()
        val variance = values.map { (it - mean) * (it - mean) }.average().toFloat()
        return sqrt(variance)
    }
}
