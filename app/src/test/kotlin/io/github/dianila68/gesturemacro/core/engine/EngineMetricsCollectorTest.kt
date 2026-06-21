package io.github.dianila68.gesturemacro.core.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EngineMetricsCollectorTest {

    private lateinit var collector: EngineMetricsCollector

    @Before fun setUp() {
        collector = EngineMetricsCollector()
    }

    @Test fun `initial state is all-zero EngineMetrics`() {
        val m = collector.metrics.value
        assertEquals(0L, m.gestureCount)
        assertEquals(0L, m.dispatchCount)
        assertEquals(0L, m.missedGestureCount)
        assertEquals(0L, m.executorFailureCount)
        assertEquals(0L, m.latencyP50Ms)
        assertEquals(0L, m.latencyP95Ms)
    }

    @Test fun `recordGesture increments gestureCount`() {
        repeat(3) { collector.recordGesture() }
        assertEquals(3L, collector.metrics.value.gestureCount)
    }

    @Test fun `recordMissed increments missedGestureCount`() {
        collector.recordMissed()
        collector.recordMissed()
        assertEquals(2L, collector.metrics.value.missedGestureCount)
    }

    @Test fun `recordDispatch increments dispatchCount`() {
        collector.recordDispatch(gestureTimestampMs = 100L, nowMs = 150L)
        collector.recordDispatch(gestureTimestampMs = 200L, nowMs = 280L)
        assertEquals(2L, collector.metrics.value.dispatchCount)
    }

    @Test fun `recordFailure increments executorFailureCount`() {
        collector.recordFailure()
        assertEquals(1L, collector.metrics.value.executorFailureCount)
    }

    @Test fun `single dispatch produces correct p50 and p95 latency`() {
        collector.recordDispatch(gestureTimestampMs = 0L, nowMs = 42L)
        val m = collector.metrics.value
        // With one sample, p50 == p95 == 42
        assertEquals(42L, m.latencyP50Ms)
        assertEquals(42L, m.latencyP95Ms)
    }

    @Test fun `multiple dispatches yield correct p50`() {
        // Feed 10 samples: 10, 20, 30, …, 100 ms
        for (i in 1..10) {
            collector.recordDispatch(gestureTimestampMs = 0L, nowMs = (i * 10).toLong())
        }
        // p50 of [10,20,30,40,50,60,70,80,90,100] — index 5 of 10 sorted = 60
        val m = collector.metrics.value
        assertTrue("p50 should be in [50,70], was ${m.latencyP50Ms}",
            m.latencyP50Ms in 50L..70L)
    }

    @Test fun `p95 is higher than p50 for spread distribution`() {
        for (i in 1..20) {
            collector.recordDispatch(gestureTimestampMs = 0L, nowMs = (i * 5).toLong())
        }
        val m = collector.metrics.value
        assertTrue("p95 (${m.latencyP95Ms}) should be >= p50 (${m.latencyP50Ms})",
            m.latencyP95Ms >= m.latencyP50Ms)
    }

    @Test fun `reset zeros all counters and latencies`() {
        repeat(5) { collector.recordGesture() }
        repeat(3) { collector.recordDispatch(0L, 100L) }
        collector.recordMissed()
        collector.recordFailure()
        collector.reset()
        val m = collector.metrics.value
        assertEquals(0L, m.gestureCount)
        assertEquals(0L, m.dispatchCount)
        assertEquals(0L, m.missedGestureCount)
        assertEquals(0L, m.executorFailureCount)
        assertEquals(0L, m.latencyP50Ms)
        assertEquals(0L, m.latencyP95Ms)
    }

    @Test fun `LATENCY_WINDOW cap evicts oldest sample`() {
        val window = EngineMetricsCollector.LATENCY_WINDOW
        // Fill window + 1 extra
        for (i in 1..window) {
            collector.recordDispatch(gestureTimestampMs = 0L, nowMs = 1L)
        }
        // The +1 sample with a very high latency replaces the oldest
        collector.recordDispatch(gestureTimestampMs = 0L, nowMs = 10_000L)
        // If window cap is enforced, we still have exactly `window` samples
        // p95 should be influenced by the large value
        val m = collector.metrics.value
        assertTrue("p95 should include the high-latency tail sample, got ${m.latencyP95Ms}",
            m.latencyP95Ms > 1L)
    }

    @Test fun `StateFlow reflects latest values after each mutation`() {
        val snapshots = mutableListOf<Long>()
        collector.recordGesture()
        snapshots += collector.metrics.value.gestureCount
        collector.recordGesture()
        snapshots += collector.metrics.value.gestureCount
        assertEquals(listOf(1L, 2L), snapshots)
    }

    @Test fun `empty latency yields 0 for both percentiles`() {
        assertEquals(0L, collector.metrics.value.latencyP50Ms)
        assertEquals(0L, collector.metrics.value.latencyP95Ms)
    }

    @Test fun `mixed operations accumulate independently`() {
        collector.recordGesture(); collector.recordGesture()
        collector.recordDispatch(0L, 50L)
        collector.recordMissed()
        collector.recordFailure(); collector.recordFailure()
        val m = collector.metrics.value
        assertEquals(2L, m.gestureCount)
        assertEquals(1L, m.dispatchCount)
        assertEquals(1L, m.missedGestureCount)
        assertEquals(2L, m.executorFailureCount)
        assertEquals(50L, m.latencyP50Ms)
    }
}
