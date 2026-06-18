package io.github.dianila68.gesturemacro.core.sensors

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalDeviceRegistryTest {

    @After fun tearDown() { ExternalDeviceRegistry.clear() }

    @Test
    fun registerAndResolve() {
        val channels = listOf(
            ExternalSensorChannel("esp32", "humidity", "%RH"),
            ExternalSensorChannel("esp32", "temperature", "°C"),
        )
        ExternalDeviceRegistry.registerDevice("esp32", channels)
        assertEquals("humidity", ExternalDeviceRegistry.channelFor("esp32", 0)?.channelName)
        assertEquals("temperature", ExternalDeviceRegistry.channelFor("esp32", 1)?.channelName)
        assertNull(ExternalDeviceRegistry.channelFor("esp32", 2))
    }

    @Test
    fun removeDevice_clearsChannels() {
        ExternalDeviceRegistry.registerDevice("esp32", listOf(ExternalSensorChannel("esp32", "co2", "ppm")))
        ExternalDeviceRegistry.removeDevice("esp32")
        assertTrue(ExternalDeviceRegistry.channelsFor("esp32").isEmpty())
    }
}
