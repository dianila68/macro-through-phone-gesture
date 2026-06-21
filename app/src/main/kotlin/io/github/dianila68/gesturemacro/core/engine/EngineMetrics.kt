package io.github.dianila68.gesturemacro.core.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EngineMetrics(
    val gestureCount: Long = 0L,
    val dispatchCount: Long = 0L,
    val missedGestureCount: Long = 0L,
    val executorFailureCount: Long = 0L,
    val latencyP50Ms: Long = 0L,
    val latencyP95Ms: Long = 0L,
)

/**
 * Rolling metrics collector for the gesture capture → action dispatch pipeline.
 *
 * Latency is measured from [GestureEvent.t] (sensor timestamp) to the moment
 * [recordDispatch] is called. Keeps the last [LATENCY_WINDOW] samples for
 * percentile computation without unbounded growth.
 */
class EngineMetricsCollector {

    private val _metrics = MutableStateFlow(EngineMetrics())
    val metrics: StateFlow<EngineMetrics> = _metrics.asStateFlow()

    private val latencySamples = ArrayDeque<Long>(LATENCY_WINDOW)
    private var gestureCount = 0L
    private var dispatchCount = 0L
    private var missedCount = 0L
    private var failureCount = 0L

    fun recordGesture() {
        gestureCount++
        publish()
    }

    fun recordMissed() {
        missedCount++
        publish()
    }

    fun recordDispatch(gestureTimestampMs: Long, nowMs: Long = System.currentTimeMillis()) {
        dispatchCount++
        val latency = nowMs - gestureTimestampMs
        if (latencySamples.size >= LATENCY_WINDOW) latencySamples.removeFirst()
        latencySamples.addLast(latency)
        publish()
    }

    fun recordFailure() {
        failureCount++
        publish()
    }

    fun reset() {
        gestureCount = 0L; dispatchCount = 0L; missedCount = 0L; failureCount = 0L
        latencySamples.clear()
        publish()
    }

    private fun publish() {
        val sorted = latencySamples.sorted()
        _metrics.value = EngineMetrics(
            gestureCount = gestureCount,
            dispatchCount = dispatchCount,
            missedGestureCount = missedCount,
            executorFailureCount = failureCount,
            latencyP50Ms = sorted.percentile(50),
            latencyP95Ms = sorted.percentile(95),
        )
    }

    private fun List<Long>.percentile(p: Int): Long {
        if (isEmpty()) return 0L
        val idx = ((p / 100.0) * (size - 1)).toInt().coerceIn(0, size - 1)
        return sorted()[idx]
    }

    companion object {
        const val LATENCY_WINDOW = 100
    }
}
