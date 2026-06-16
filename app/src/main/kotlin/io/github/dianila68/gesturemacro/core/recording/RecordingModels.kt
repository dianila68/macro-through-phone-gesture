package io.github.dianila68.gesturemacro.core.recording

import io.github.dianila68.gesturemacro.core.sensors.SensorType

/**
 * ticket-045: Core types for the gesture recording system.
 */

enum class SensorChannel(val sensorType: SensorType) {
    ACCELEROMETER(SensorType.ACCELEROMETER),
    GYROSCOPE(SensorType.GYROSCOPE),
    ROTATION_VECTOR(SensorType.ROTATION_VECTOR),
    GRAVITY(SensorType.ACCELEROMETER), // derived channel, same sensor
}

data class RecordingConfig(
    val requiredSamples: Int = 5,
    val minSamples: Int = 3,
    val maxWindowMs: Long = 3_000,
    val interSamplePauseMs: Long = 1_500,
    val countdownMs: Long = 3_000,
    val sensors: Set<SensorChannel> = setOf(SensorChannel.ACCELEROMETER, SensorChannel.GYROSCOPE),
)

sealed interface RecordingState {
    data object Idle : RecordingState
    data class Countdown(val remainingMs: Long) : RecordingState
    data class Recording(val sampleIndex: Int, val elapsedMs: Long) : RecordingState
    data class InterSamplePause(val nextSampleIndex: Int, val remainingMs: Long) : RecordingState
    data object Analysing : RecordingState
    data class Ready(val envelope: GestureEnvelope) : RecordingState
    data object InsufficientData : RecordingState
    data object Cancelled : RecordingState
    data object TimedOut : RecordingState
}

data class CoverageUpdate(
    val windowIndex: Int,
    val qualityScore: Float,
    val coverageScore: Float,
)
