package io.github.dianila68.gesturemacro.core.sensors

import java.util.concurrent.ConcurrentHashMap

/**
 * ticket-054: Thread-safe registry mapping (deviceId, channelIndex) to
 * ExternalSensorChannel. Singleton populated at pairing time by DevicePairingService.
 */
object ExternalDeviceRegistry {
    private val channels = ConcurrentHashMap<String, List<ExternalSensorChannel>>()

    fun registerDevice(deviceId: String, deviceChannels: List<ExternalSensorChannel>) {
        channels[deviceId] = deviceChannels
    }

    fun channelFor(deviceId: String, channelIndex: Int): ExternalSensorChannel? =
        channels[deviceId]?.getOrNull(channelIndex)

    fun channelsFor(deviceId: String): List<ExternalSensorChannel> =
        channels[deviceId] ?: emptyList()

    fun removeDevice(deviceId: String) {
        channels.remove(deviceId)
    }

    fun allDevices(): Set<String> = channels.keys.toSet()

    fun clear() { channels.clear() }
}
