package io.github.dianila68.gesturemacro.android.sensors

import io.github.dianila68.gesturemacro.core.sensors.ExternalDeviceRegistry
import io.github.dianila68.gesturemacro.core.sensors.SensorSample
import io.github.dianila68.gesturemacro.core.sensors.SensorStream
import io.github.dianila68.gesturemacro.core.sensors.SensorType
import io.github.dianila68.gesturemacro.core.sensors.parseHandshake
import io.github.dianila68.gesturemacro.core.sensors.parseReading
import io.github.dianila68.gesturemacro.core.sensors.toExternalChannels
import io.github.dianila68.gesturemacro.core.sensors.toSensorSample
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import java.net.URI
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.nio.ByteBuffer
import java.util.concurrent.CompletionStage

/**
 * ticket-059: SensorStream implementation that connects to an ESP32 or Raspberry Pi
 * sensor server via WebSocket. Implements the gesturemacro/1 wire protocol (ticket-055).
 *
 * First text message = capability handshake → populates ExternalDeviceRegistry.
 * Subsequent text frames = JSON samples; binary frames = MessagePack (not yet decoded).
 * Auto-reconnect with exponential backoff (1s → 2s → 4s → max 30s).
 *
 * Uses java.net.http.WebSocket (Java 11+; project targets Java 17).
 *
 * Usage: collect samples(SensorType.EXTERNAL, ...) after registering this stream
 * in GestureCaptureService alongside AndroidSensorStream.
 */
class WebSocketSensorStream(
    private val url: String,
    private val deviceId: String,
    private val registry: ExternalDeviceRegistry = ExternalDeviceRegistry,
) : SensorStream {

    private val httpClient = HttpClient.newHttpClient()

    private var deviceClockOffsetMs = 0L

    override fun samples(type: SensorType, samplingPeriodUs: Int, maxReportLatencyUs: Int): Flow<SensorSample> {
        if (type != SensorType.EXTERNAL) return emptyFlow()
        return callbackFlow {
            var handshakeReceived = false
            var webSocket: WebSocket? = null
            var backoffMs = INITIAL_BACKOFF_MS
            var cancelled = false

            fun connect() {
                handshakeReceived = false
                val messageBuffer = StringBuilder()

                val listener = object : WebSocket.Listener {
                    override fun onOpen(ws: WebSocket) {
                        backoffMs = INITIAL_BACKOFF_MS
                        ws.request(1)
                    }

                    override fun onText(ws: WebSocket, data: CharSequence, last: Boolean): CompletionStage<*>? {
                        messageBuffer.append(data)
                        if (last) {
                            val text = messageBuffer.toString()
                            messageBuffer.clear()
                            if (!handshakeReceived) {
                                runCatching {
                                    val handshake = parseHandshake(text)
                                    registry.registerDevice(handshake.device, handshake.toExternalChannels())
                                    handshakeReceived = true
                                }
                            } else {
                                runCatching {
                                    val reading = parseReading(text)
                                    if (deviceClockOffsetMs == 0L) {
                                        deviceClockOffsetMs = System.currentTimeMillis() - reading.timestampMs
                                    }
                                    trySend(reading.toSensorSample(deviceClockOffsetMs))
                                }
                            }
                        }
                        ws.request(1)
                        return null
                    }

                    override fun onBinary(ws: WebSocket, data: ByteBuffer, last: Boolean): CompletionStage<*>? {
                        // MessagePack binary frames — decode tracked in ticket-059 full impl
                        ws.request(1)
                        return null
                    }

                    override fun onError(ws: WebSocket, error: Throwable) {
                        handshakeReceived = false
                        // Reconnect handled below via backoff loop
                    }

                    override fun onClose(ws: WebSocket, statusCode: Int, reason: String): CompletionStage<*>? {
                        handshakeReceived = false
                        return null
                    }
                }

                runCatching {
                    webSocket = httpClient.newWebSocketBuilder()
                        .buildAsync(URI.create(url), listener)
                        .get()
                }.onFailure {
                    // Connection failed; backoff handled by caller loop
                }
            }

            // Initial connection
            connect()

            // Reconnect loop with exponential backoff
            val reconnectJob = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                while (!cancelled) {
                    delay(backoffMs)
                    if (!cancelled && (webSocket == null || webSocket?.isInputClosed == true)) {
                        connect()
                        backoffMs = minOf(backoffMs * 2, MAX_BACKOFF_MS)
                    }
                }
            }

            awaitClose {
                cancelled = true
                reconnectJob.cancel()
                webSocket?.sendClose(WebSocket.NORMAL_CLOSURE, "Stream cancelled")
                httpClient.executor().ifPresent { /* shared executor; do not shut down */ }
            }
        }
    }

    companion object {
        private const val INITIAL_BACKOFF_MS = 1_000L
        private const val MAX_BACKOFF_MS = 30_000L
    }
}
