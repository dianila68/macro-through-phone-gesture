# ticket-057: BLE device pairing service

- **Milestone:** M5
- **Priority:** P2
- **Status:** Backlog
- **Dependencies:** ticket-054, ticket-055

## Description

Android-side BLE GATT client that discovers, pairs, and maintains a connection
to remote sensor devices implementing the `gesturemacro/1` wire protocol
(ticket-055). Once paired, the service feeds `SensorSample` frames into the
existing `SensorStream` pipeline — the engine and detectors remain unaware that
the data comes from BLE.

## Design

```
DevicePairingService (android.sensors)
    ├── scanForDevices(): Flow<BleDeviceInfo>   — filtered by service UUID
    ├── connect(deviceId): Boolean
    ├── disconnect(deviceId)
    └── connectedDevices(): StateFlow<List<BleDeviceInfo>>

BleSensorStream (android.sensors) : SensorStream
    — one instance per connected device
    — reads GATT characteristic 0003 NOTIFY → parses JSON or MessagePack
    → emits SensorSample(sensor=EXTERNAL, v=..., t=corrected)
```

**Service UUID** (from ticket-055):
`4e475252-4f4d-4143-524f-000000000001`

**Pairing flow**:
1. User taps "Add sensor device" in UI (ticket-058).
2. `DevicePairingService.scanForDevices()` emits nearby devices advertising
   the GestureMacro service UUID.
3. User selects device → `connect(deviceId)`.
4. On connect: read characteristic `...0002` → parse capability handshake →
   register channels in `ExternalDeviceRegistry` (ticket-054).
5. Subscribe to characteristic `...0003` NOTIFY → stream samples.
6. Read characteristic `...0004` to determine JSON vs MessagePack mode.

## Acceptance criteria

- [ ] `DevicePairingService` class in `android.sensors` with scan/connect/disconnect.
- [ ] `BleSensorStream` implements `SensorStream`; registered alongside
  `AndroidSensorStream` in `GestureCaptureService`.
- [ ] `ExternalDeviceRegistry` populated from capability handshake on connect.
- [ ] Clock skew correction: `deviceClockOffsetMs` computed on first sample,
  applied to all subsequent samples from that device.
- [ ] MTU negotiation: request 185; fall back to 20-byte chunked reassembly.
- [ ] Paired devices persisted in `SharedPreferences` (deviceId + last seen);
  auto-reconnect on service start if in range.
- [ ] Unit tests: handshake parsing, sample parsing (JSON + MessagePack),
  clock skew correction.

## Technical notes

- `BLUETOOTH_SCAN` + `BLUETOOTH_CONNECT` permissions required (API 31+);
  `ACCESS_FINE_LOCATION` for API < 31 scanning.
- Use `BluetoothLeScanner` with `ScanFilter` on service UUID to avoid battery
  drain from unfocused scanning.
- GATT operations must be serialized (Android BLE stack is not thread-safe);
  use a single-thread coroutine dispatcher or a command queue.
- MessagePack decoding: `org.msgpack:msgpack-core:0.9.x` (pure JVM, no NDK).
- `BleSensorStream.samples()` is a cold flow; collection starts GATT notify
  subscription, cancellation stops it.
