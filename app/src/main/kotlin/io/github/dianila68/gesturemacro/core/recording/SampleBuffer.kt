package io.github.dianila68.gesturemacro.core.recording

import io.github.dianila68.gesturemacro.core.sensors.SensorSample

/**
 * Fixed-capacity ring buffer that stores [SensorSample]s during a recording window (ticket-046).
 *
 * - [add] drops the oldest sample when the buffer is full (overwrite mode).
 * - [snapshot] returns an immutable ordered copy (oldest to newest).
 * - Thread-safe: a sensor callback and the UI thread may call concurrently.
 */
class SampleBuffer(val maxSamples: Int = DEFAULT_CAPACITY) {

    private val buffer = ArrayDeque<SensorSample>(maxSamples)

    @Synchronized
    fun add(sample: SensorSample) {
        if (buffer.size >= maxSamples) buffer.removeFirst()
        buffer.addLast(sample)
    }

    @Synchronized
    fun snapshot(): List<SensorSample> = buffer.toList()

    @Synchronized
    fun clear() = buffer.clear()

    @Synchronized
    fun size(): Int = buffer.size

    @Synchronized
    fun isEmpty(): Boolean = buffer.isEmpty()

    /**
     * Returns samples whose [SensorSample.t] falls within the half-open interval
     * [fromMs, toMs). Useful for extracting a single repetition window.
     */
    @Synchronized
    fun snapshotWindow(fromMs: Long, toMs: Long): List<SensorSample> =
        buffer.filter { it.t in fromMs until toMs }

    companion object {
        /** 5 s × 100 Hz = 500 samples. */
        const val DEFAULT_CAPACITY = 500
    }
}
