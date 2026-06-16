package io.github.dianila68.gesturemacro.core.triggers

import io.github.dianila68.gesturemacro.core.recording.GestureEnvelope
import io.github.dianila68.gesturemacro.core.recording.GestureEnvelopeBuilder
import io.github.dianila68.gesturemacro.core.sensors.GestureDetector
import io.github.dianila68.gesturemacro.core.sensors.GestureEvent
import io.github.dianila68.gesturemacro.core.sensors.GesturePattern
import io.github.dianila68.gesturemacro.core.sensors.SensorSample
import io.github.dianila68.gesturemacro.core.sensors.SensorType
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * ticket-049: Live envelope matcher. Maintains a sliding buffer of sensor frames
 * and fires GesturePattern.RECORDED_GESTURE when the current window matches the
 * stored GestureEnvelope within the sensitivity tolerance band.
 *
 * sensitivity=0 (loose): k=3.0, matchThreshold=0.65
 * sensitivity=1 (tight): k=1.2, matchThreshold=0.85
 */
class RecordedGestureDetector(
    private val envelope: GestureEnvelope,
    @Suppress("unused") private val envelopeId: String,
    sensitivity: Float = 0.5f,
) : GestureDetector {

    override val pattern: GesturePattern = GesturePattern.RECORDED_GESTURE
    override val sensor: SensorType = SensorType.ACCELEROMETER

    private val kMultiplier: Float = lerp(3.0f, 1.2f, sensitivity)
    private val matchThreshold: Float = lerp(0.65f, 0.85f, sensitivity)

    private val bufferMs: Long = (envelope.durationMeanMs + 2 * envelope.durationStdMs).toLong().coerceAtLeast(500L)
    private val minDurationMs: Long = (envelope.durationMeanMs - envelope.durationStdMs).toLong().coerceAtLeast(200L)

    private val buffer = ArrayDeque<Pair<Long, Float>>()
    private var lastFiredMs = 0L
    private val cooldownMs = 2_000L

    override fun feed(sample: SensorSample): GestureEvent? {
        if (sample.sensor != SensorType.ACCELEROMETER) return null
        val mag = magnitude(sample.v)
        val nowMs = sample.t

        buffer.addLast(nowMs to mag)
        while (buffer.isNotEmpty() && nowMs - buffer.first().first > bufferMs) {
            buffer.removeFirst()
        }

        val spanMs = if (buffer.size > 1) buffer.last().first - buffer.first().first else 0L
        if (spanMs < minDurationMs) return null
        if (nowMs - lastFiredMs < cooldownMs) return null

        val cutoff = nowMs - envelope.durationMeanMs.toLong()
        val window = buffer.filter { it.first >= cutoff }.map { it.second }
        if (window.isEmpty()) return null

        val resampled = GestureEnvelopeBuilder.resample(window, envelope.sliceCount)
        var matchCount = 0
        for (i in 0 until envelope.sliceCount) {
            val tolerance = kMultiplier * envelope.magnitudeStd[i]
            if (abs(resampled[i] - envelope.magnitudeMean[i]) <= tolerance) matchCount++
        }
        val matchFraction = matchCount.toFloat() / envelope.sliceCount

        if (matchFraction >= matchThreshold) {
            lastFiredMs = nowMs
            return GestureEvent(pattern = GesturePattern.RECORDED_GESTURE, t = nowMs, confidence = matchFraction)
        }
        return null
    }

    override fun reset() {
        buffer.clear()
        lastFiredMs = 0L
    }

    private fun magnitude(v: FloatArray): Float = sqrt(v.sumOf { (it * it).toDouble() }.toFloat())
    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t.coerceIn(0f, 1f)
}
