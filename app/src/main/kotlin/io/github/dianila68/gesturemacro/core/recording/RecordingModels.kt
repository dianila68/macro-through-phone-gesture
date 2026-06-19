package io.github.dianila68.gesturemacro.core.recording

import io.github.dianila68.gesturemacro.core.sensors.SensorType
import kotlinx.serialization.Serializable

/** Which sensor channels to capture during a recording session. */
enum class RecordingChannel {
    ACCELEROMETER,
    GYROSCOPE,
}

fun RecordingChannel.toSensorType(): SensorType = when (this) {
    RecordingChannel.ACCELEROMETER -> SensorType.ACCELEROMETER
    RecordingChannel.GYROSCOPE -> SensorType.GYROSCOPE
}

data class RecordingConfig(
    val requiredSamples: Int = 5,
    val minSamples: Int = 3,
    val maxWindowMs: Long = 3_000,
    val interSamplePauseMs: Long = 1_500,
    val countdownMs: Long = 3_000,
    val channels: Set<RecordingChannel> = setOf(RecordingChannel.ACCELEROMETER, RecordingChannel.GYROSCOPE),
) {
    init {
        require(requiredSamples >= 1) { "requiredSamples must be >= 1" }
        require(minSamples >= 1 && minSamples <= requiredSamples) {
            "minSamples must be in 1..$requiredSamples"
        }
        require(maxWindowMs > 0) { "maxWindowMs must be > 0" }
        require(channels.isNotEmpty()) { "channels must not be empty" }
    }
}

/** One sensor reading frame captured during a recording window. */
data class SensorFrame(
    val timestampNs: Long,
    val channel: RecordingChannel,
    /** Raw sensor values (3 floats for accel/gyro). */
    val values: FloatArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SensorFrame) return false
        return timestampNs == other.timestampNs &&
            channel == other.channel &&
            values.contentEquals(other.values)
    }

    override fun hashCode(): Int {
        var result = timestampNs.hashCode()
        result = 31 * result + channel.hashCode()
        result = 31 * result + values.contentHashCode()
        return result
    }
}

/** Frames collected during one repetition. */
class SampleWindow(val index: Int) {
    val frames: MutableList<SensorFrame> = mutableListOf()
    var startNs: Long = 0L
    var endNs: Long = 0L
    val durationMs: Long get() = if (endNs > startNs) (endNs - startNs) / 1_000_000L else 0L
}

/** Accumulates SampleWindows across repetitions. */
class SampleBuffer {
    private val _windows: MutableList<SampleWindow> = mutableListOf()
    val windows: List<SampleWindow> get() = _windows.toList()

    private var currentWindow: SampleWindow? = null

    fun openWindow(index: Int) {
        currentWindow = SampleWindow(index).also {
            it.startNs = System.nanoTime()
        }
    }

    fun appendFrame(frame: SensorFrame) {
        currentWindow?.frames?.add(frame)
    }

    fun closeWindow() {
        currentWindow?.let {
            it.endNs = System.nanoTime()
            _windows.add(it)
            currentWindow = null
        }
    }

    fun clear() {
        _windows.clear()
        currentWindow = null
    }
}

/** The derived tolerance band that describes the gesture across N repetitions. */
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
        return version == other.version &&
            sliceCount == other.sliceCount &&
            magnitudeMean.contentEquals(other.magnitudeMean) &&
            magnitudeStd.contentEquals(other.magnitudeStd) &&
            (gyroMean == null) == (other.gyroMean == null) &&
            (gyroMean?.contentEquals(other.gyroMean ?: floatArrayOf()) ?: true) &&
            durationMeanMs == other.durationMeanMs &&
            sampleCount == other.sampleCount
    }

    override fun hashCode(): Int = version * 31 + sliceCount * 31 + sampleCount
}
