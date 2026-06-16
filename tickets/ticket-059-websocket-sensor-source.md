# ticket-059: WebSocket sensor source

- **Milestone:** M5
- **Priority:** P3
- **Status:** Backlog
- **Dependencies:** ticket-054, ticket-055

## Description

Alternative transport to BLE (ticket-057): connect to an ESP32 or Raspberry Pi
sensor server over WebSocket (WiFi / LAN). Implements `SensorStream` so the
engine pipeline is transport-agnostic.

## Design

```kotlin
class WebSocketSensorStream(
    private val url: String,   // e.g. "ws://192.168.1.42:8765"
    private val registry: ExternalDeviceRegistry,
) : SensorStream {
    override fun samples(type: SensorType, ...): Flow<SensorSample>
}
```

- First text message on connect = capability handshake → populate registry.
- Subsequent text frames = JSON samples; binary frames = MessagePack samples.
- Ping every 30 s (server-side keepalive per ticket-055 spec).
- Auto-reconnect with exponential backoff (1 s → 2 s → 4 s → max 30 s).

## Acceptance criteria

- [ ] `WebSocketSensorStream` in `android.sensors` using OkHttp `WebSocket`.
- [ ] Registered in `GestureCaptureService` alongside `BleSensorStream`.
- [ ] User can add a WebSocket source by entering URL in the pairing UI
  (ticket-058 extended with a "Connect via IP" option).
- [ ] Unit tests: handshake parsing, sample parsing, reconnect backoff logic.

## Technical notes

- OkHttp is the preferred WebSocket client (already in the dependency graph).
- `Flow` produced via `callbackFlow`; `awaitClose` stops the socket.
- No mDNS/Bonjour discovery in this ticket — manual IP entry only (ticket-060
  covers mDNS auto-discovery as a nice-to-have).
