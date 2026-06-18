package io.github.dianila68.gesturemacro.core.sensors

import org.junit.Assert.assertEquals
import org.junit.Test

class RemoteSensorProtocolTest {

    @Test
    fun parseHandshake_validJson_returnsHandshake() {
        val json = """{"device":"esp32-home","protocol":"gesturemacro/1","channels":[{"name":"humidity","unit":"%RH","min":0,"max":100,"hz":1}]}"""
        val handshake = parseHandshake(json)
        assertEquals("esp32-home", handshake.device)
        assertEquals(1, handshake.channels.size)
        assertEquals("humidity", handshake.channels[0].name)
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseHandshake_wrongMajorVersion_throws() {
        val json = """{"device":"x","protocol":"gesturemacro/2","channels":[]}"""
        parseHandshake(json)
    }

    @Test
    fun parseReading_validJson_returnsReading() {
        val json = """{"c":"humidity","v":[65.3],"t":1718000000000}"""
        val reading = parseReading(json)
        assertEquals("humidity", reading.channel)
        assertEquals(65.3f, reading.values[0], 0.01f)
        assertEquals(1718000000000L, reading.timestampMs)
    }

    @Test
    fun toSensorSample_appliesClockOffset() {
        val reading = SensorReading("humidity", listOf(65.3f), 1000L)
        val sample = reading.toSensorSample(deviceClockOffsetMs = -50L)
        assertEquals(950L, sample.t)
        assertEquals(SensorType.EXTERNAL, sample.sensor)
    }

    @Test
    fun toExternalChannels_mapsAllChannels() {
        val handshake = CapabilityHandshake(
            device = "esp32-home",
            protocol = "gesturemacro/1",
            channels = listOf(
                ChannelSpec("humidity", "%RH", 0f, 100f, 1f),
                ChannelSpec("temperature", "°C", -40f, 85f, 1f),
            )
        )
        val channels = handshake.toExternalChannels()
        assertEquals(2, channels.size)
        assertEquals("humidity", channels[0].channelName)
        assertEquals("esp32-home", channels[0].deviceId)
    }
}
