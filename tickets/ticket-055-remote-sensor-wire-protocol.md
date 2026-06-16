# ticket-055: Remote sensor wire protocol specification

- **Milestone:** M5
- **Priority:** P1
- **Status:** Backlog
- **Dependencies:** ticket-054

## Description

Define the canonical wire format for GestureMacro external sensor devices. Any device
(ESP32, Raspberry Pi, USB adapter, Arduino) that implements this protocol can be used as
a pluggable sensor source without app changes. The protocol must work over both BLE and
WebSocket transports.

## Wire format

**Single sensor reading (JSON, human-debuggable)**:
```json
{"c": "humidity", "v": [65.3], "t": 1718000000000}
```
- `c` — channel name (string, matches a capability declared in the handshake)
- `v` — float array (1–4 values; mirrors SensorSample.v)
- `t` — Unix epoch milliseconds (device clock; app reconciles skew on receipt)

**Compact binary alternative (MessagePack)**:
- Same keys, 14–20 bytes/sample vs ~40 bytes JSON
- Use MessagePack when stream rate > 20 Hz or BLE MTU is 20 bytes
- Apps decode with `org.msgpack:msgpack-core`

**Device capability handshake** (sent once on connect, JSON):
```json
{
  "device": "esp32-home",
  "protocol": "gesturemacro/1",
  "channels": [
    {"name": "humidity",    "unit": "%RH",  "min": 0,   "max": 100, "hz": 1},
    {"name": "temperature", "unit": "°C",   "min": -40, "max": 85,  "hz": 1},
    {"name": "co2",         "unit": "ppm",  "min": 400, "max": 5000,"hz": 0.5}
  ]
}
```
- `device` — stable unique device ID (ESP32 MAC address recommended)
- `protocol` — version string; app must reject unknown major versions
- `channels` — list of sensor channels this device exposes; `hz` is typical sample rate

**BLE GATT mapping**:
- Service UUID: `4e475552-4f4d-4143-524f-000000000001` ("GESTUREMACRO\0\0\0\0\0\0\1")
- Characteristic `...0002`: READ + NOTIFY — capability handshake JSON
- Characteristic `...0003`: NOTIFY only — streaming samples (JSON or MessagePack)
- Format flag: characteristic `...0004` value `0x00` = JSON, `0x01` = MessagePack

**WebSocket mapping**:
- On connection open, server sends capability handshake as first text message
- Subsequent messages are sensor samples (text for JSON, binary frames for MessagePack)
- Ping/pong every 30 s for keepalive

## Acceptance criteria

- [ ] `docs/protocol/remote-sensor-v1.md` documents the above (single source of truth).
- [ ] `RemoteSensorProtocol.kt` in `core.sensors`: data classes `CapabilityHandshake`,
  `SensorReading`, `ChannelSpec`; `parseHandshake(json)`, `parseReading(json)` functions.
- [ ] Unit tests: parse capability + reading JSON; round-trip MessagePack encoding.
- [ ] Reference ESP32 Arduino sketch in `docs/firmware/esp32-sensor-server/` implementing
  both BLE GATT and WebSocket endpoints with the protocol above (see ticket-061).

## Technical notes

- Clock skew: store `deviceClockOffsetMs = System.currentTimeMillis() - reading.t` on first
  reading; correct subsequent sample timestamps. Acceptable skew < 500 ms.
- MTU negotiation: on BLE, request MTU 185 (allows ~180-byte samples). Fall back to 20-byte
  chunks if negotiation fails.
- `gesturemacro/1` is the initial protocol version. Minor versions add optional fields;
  major bumps require a new `channels[].protocol` key.
