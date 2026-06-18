package io.github.dianila68.gesturemacro.core.serialization

import org.junit.Assert.assertEquals
import org.junit.Test

class WebhookActionTest {
    @Test
    fun validWebhookAction_createsSuccessfully() {
        val action = WebhookAction(url = "http://example.com/hook")
        assertEquals("webhook", action.actionType)
        assertEquals("POST", action.method)
    }

    @Test(expected = IllegalArgumentException::class)
    fun blankUrl_throws() {
        WebhookAction(url = "")
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidMethod_throws() {
        WebhookAction(url = "http://example.com", method = "CONNECT")
    }

    @Test
    fun validMqttPublishAction_createsSuccessfully() {
        val action = MqttPublishAction(brokerUrl = "tcp://192.168.1.10:1883", topic = "home/macro")
        assertEquals("mqtt_publish", action.actionType)
        assertEquals(0, action.qos)
    }

    @Test(expected = IllegalArgumentException::class)
    fun mqttAction_invalidQos_throws() {
        MqttPublishAction(brokerUrl = "tcp://broker", topic = "t", qos = 3)
    }
}
