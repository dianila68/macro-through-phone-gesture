package io.github.dianila68.gesturemacro.core.sensors

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Deterministic trace replay for detector tests (ticket-003, NFR-2): traces are
 * JSON fixtures in test resources, one timestamped sample per entry.
 */
@Serializable
data class TraceSample(val t: Long, val x: Float, val y: Float, val z: Float)

object TraceReplay {
    private val json = Json

    fun load(resource: String): List<SensorSample> {
        val stream = checkNotNull(javaClass.getResourceAsStream(resource)) { "Missing fixture $resource" }
        val text = stream.bufferedReader().use { it.readText() }
        return json.decodeFromString<List<TraceSample>>(text).map {
            SensorSample(SensorType.ACCELEROMETER, it.t, floatArrayOf(it.x, it.y, it.z))
        }
    }

    fun run(detector: GestureDetector, samples: List<SensorSample>): List<GestureEvent> =
        samples.mapNotNull { detector.feed(it) }
}
