package io.github.dianila68.gesturemacro.core.sensors

import kotlinx.coroutines.flow.Flow

/**
 * ticket-023: Pure SPI for sensor data, extracted from AndroidSensorStream so
 * the engine layer has no android.* imports. The Android implementation lives
 * in the `.android.sensors` package.
 */
interface SensorStream {
    /**
     * Cold flow of samples; registering/unregistering follows collection (DESIGN.md).
     * [maxReportLatencyUs] > 0 enables hardware batching to spare wakeups (NFR-1).
     */
    fun samples(type: SensorType, samplingPeriodUs: Int, maxReportLatencyUs: Int = 0): Flow<SensorSample>
}
