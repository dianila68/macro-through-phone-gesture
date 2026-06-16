package io.github.dianila68.gesturemacro.core.recording

/**
 * ticket-046: Accumulates raw sensor frames during recording windows.
 */
data class SensorFrame(
    val timestampNs: Long,
    val channel: SensorChannel,
    val values: FloatArray,
) {
    /** Euclidean magnitude of the values array. */
    val magnitude: Float get() = kotlin.math.sqrt(values.map { it * it }.sum())
}

class SampleWindow(val index: Int) {
    val frames: MutableList<SensorFrame> = mutableListOf()
    var startNs: Long = 0L
    var endNs: Long = 0L
    val durationMs: Long get() = (endNs - startNs) / 1_000_000L
    var qualityScore: Float = 0f
    var qualityRating: QualityRating = QualityRating.UNKNOWN
}

enum class QualityRating { UNKNOWN, LOW_QUALITY, ACCEPTABLE, GOOD }

class SampleBuffer {
    private val _windows = mutableListOf<SampleWindow>()
    val windows: List<SampleWindow> get() = _windows.toList()

    private var currentWindow: SampleWindow? = null

    fun openWindow(index: Int) {
        currentWindow = SampleWindow(index).also {
            it.startNs = System.nanoTime()
            _windows.add(it)
        }
    }

    fun appendFrame(frame: SensorFrame) {
        currentWindow?.frames?.add(frame)
    }

    fun closeWindow() {
        currentWindow?.endNs = System.nanoTime()
        currentWindow = null
    }

    fun clear() {
        _windows.clear()
        currentWindow = null
    }
}
