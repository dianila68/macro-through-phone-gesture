# GestureMacro Remote Sensor Protocol v1

**Version:** `gesturemacro/1`  
**Status:** Stable  
**Source of truth:** This document. The Kotlin data classes in
`core.sensors.RemoteSensorProtocol` implement this spec exactly.

---

## Overview

Any device (ESP32, Raspberry Pi, USB adapter, Arduino) that implements this
protocol can be used as a pluggable sensor source without changing the
GestureMacro app. The protocol works over two transports:

- **BLE GATT** — for low-power, short-range devices (ESP32, Arduino)
- **WebSocket** — for WiFi/LAN-connected devices (ESP32-WiFi, Raspberry Pi)

The app treats remote sensor data identically to built-in phone sensors once
connected. No engine or detector changes are needed.

---

## Wire format

### Single sensor reading (JSON, human-debuggable)

```json
{"c": "humidity", "v": [65.3], "t": 1718000000000}
```

| Field | Type | Description |
|-------|------|-------------|
| `c` | string | Channel name — must match a channel declared in the handshake |
| `v` | float[] | 1–4 values; mirrors `SensorSample.v` |
| `t` | integer | Unix epoch milliseconds (device clock) |

### Compact binary alternative (MessagePack)

Same keys as JSON, encoded with MessagePack. Typical size: 14–20 bytes/sample
vs ~40 bytes JSON.

Use MessagePack when:
- Stream rate > 20 Hz, OR
- BLE MTU is 20 bytes (default negotiation failed)

Apps decode with `org.msgpack:msgpack-core`.

---

## Device capability handshake

Sent once on connect (JSON):

```json
{
  "device": "esp32-home",
  "protocol": "gesturemacro/1",
  "channels": [
    {"name": "humidity",    "unit": "%RH", "min": 0,   "max": 100,  "hz": 1},
    {"name": "temperature", "unit": "°C",  "min": -40, "max": 85,   "hz": 1},
    {"name": "co2",         "unit": "ppm", "min": 400, "max": 5000, "hz": 0.5}
  ]
}
```

| Field | Type | Description |
|-------|------|-------------|
| `device` | string | Stable unique device ID (ESP32 MAC address recommended) |
| `protocol` | string | Version string; app must reject unknown major versions |
| `channels` | array | Sensor channels this device exposes; `hz` is typical sample rate |

**Version semantics:**
- Minor versions (e.g. `gesturemacro/1.1`) add optional fields — backward compatible
- Major bumps require a new `channels[].protocol` key
- The app rejects connections where the major version ≠ 1

---

## BLE GATT mapping

**Service UUID:** `4e475552-4f4d-4143-524f-000000000001`

| Characteristic | UUID suffix | Properties | Content |
|----------------|-------------|------------|---------|
| Handshake | `...0002` | READ + NOTIFY | Capability handshake JSON (sent once on connect) |
| Samples | `...0003` | NOTIFY | Streaming samples (JSON text or MessagePack binary) |
| Format flag | `...0004` | READ + WRITE | `0x00` = JSON, `0x01` = MessagePack |

**MTU negotiation:**
- App requests MTU 185 (allows ~180-byte samples)
- Falls back to 20-byte chunked reassembly if negotiation fails

**Scanning:**
- App scans for devices advertising the GestureMacro service UUID
- Use `BluetoothLeScanner` with `ScanFilter` on the service UUID

---

## WebSocket mapping

- On connection open, the **server** sends the capability handshake as the
  first **text** message
- Subsequent messages are sensor samples:
  - Text frames → JSON samples
  - Binary frames → MessagePack samples
- Server sends a WebSocket **ping** every 30 s for keepalive; client responds
  with pong automatically (OkHttp / java.net.http handle this)

---

## Clock skew handling

Device clocks drift relative to the Android system clock. The app reconciles
this on receipt:

```
deviceClockOffsetMs = System.currentTimeMillis() - reading.t
```

Computed on the **first** reading from each device; applied to all subsequent
samples from that device:

```
correctedTimestampMs = reading.t + deviceClockOffsetMs
```

Acceptable skew: < 500 ms. If skew exceeds this, the app logs a warning but
continues operating.

---

## Reference implementations

- **ESP32 Arduino sketch:** `docs/firmware/esp32-sensor-server/` — implements
  both BLE GATT and WebSocket endpoints with this protocol (see ticket-061)
- **Kotlin data classes:** `core.sensors.RemoteSensorProtocol` —
  `CapabilityHandshake`, `ChannelSpec`, `SensorReading`, `parseHandshake()`,
  `parseReading()`
- **BLE client:** `android.sensors.DevicePairingService`
- **WebSocket client:** `android.sensors.WebSocketSensorStream`
