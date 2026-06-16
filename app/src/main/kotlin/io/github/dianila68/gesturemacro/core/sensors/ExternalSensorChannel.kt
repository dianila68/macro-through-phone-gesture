package io.github.dianila68.gesturemacro.core.sensors

/**
 * ticket-054: Describes one channel of an external (remote) sensor device.
 * Populated from the device capability handshake (ticket-055).
 */
data class ExternalSensorChannel(
    val deviceId: String,
    val channelName: String,
    val unit: String,
    val valueCount: Int = 1,
    val minValue: Float = Float.MIN_VALUE,
    val maxValue: Float = Float.MAX_VALUE,
    val hz: Float = 1f,
)
