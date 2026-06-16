package io.github.dianila68.gesturemacro.core.sensors

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * ticket-055: Data classes and parsers for the gesturemacro/1 wire protocol.
 * Pure JVM — no android.* imports. Works over BLE and WebSocket transports.
 */

@Serializable
data class CapabilityHandshake(
    val device: String,
    val protocol: String,
    val channels: List<ChannelSpec>,
) {
    init {
        require(protocol.startsWith("gesturemacro/")) {
            "Unknown protocol: $protocol"
        }
        val major = protocol.substringAfter("gesturemacro/").substringBefore(".").toIntOrNull() ?: -1
        require(major == 1) { "Unsupported protocol major version: $major" }
    }
}

@Serializable
data class ChannelSpec(
    val name: String,
    val unit: String,
    val min: Float = Float.MIN_VALUE,
    val max: Float = Float.MAX_VALUE,
    val hz: Float = 1f,
)

@Serializable
data class SensorReading(
    @SerialName("c") val channel: String,
    @SerialName("v") val values: List<Float>,
    @SerialName("t") val timestampMs: Long,
)

private val json = Json { ignoreUnknownKeys = true }

fun parseHandshake(jsonString: String): CapabilityHandshake =
    json.decodeFromString(CapabilityHandshake.serializer(), jsonString)

fun parseReading(jsonString: String): SensorReading =
    json.decodeFromString(SensorReading.serializer(), jsonString)

fun CapabilityHandshake.toExternalChannels(): List<ExternalSensorChannel> =
    channels.map { spec ->
        ExternalSensorChannel(
            deviceId = device,
            channelName = spec.name,
            unit = spec.unit,
            valueCount = 1,
            minValue = spec.min,
            maxValue = spec.max,
            hz = spec.hz,
        )
    }

fun SensorReading.toSensorSample(deviceClockOffsetMs: Long = 0L): SensorSample =
    SensorSample(
        sensor = SensorType.EXTERNAL,
        t = timestampMs + deviceClockOffsetMs,
        v = values.toFloatArray(),
    )
