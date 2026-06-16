# ticket-054: External / pluggable sensor type model

- **Milestone:** M5
- **Priority:** P1
- **Status:** Backlog
- **Dependencies:** ticket-003, ticket-021

## Description

The codebase already has `SensorKind.EXTERNAL` and `Trigger.sourceDevice` in the macro model,
anticipating external sensors. This ticket formalises the runtime model so the rest of the
pipeline (detectors, engine, GestureCaptureService) can treat a remote sensor exactly like a
built-in one.

The key invariant: **detectors and the engine must not know or care whether a `SensorSample`
came from the Android SensorManager or from an ESP32 over WiFi.** The `SensorStream` interface
already provides this seam.

## Design

```
SensorType (enum, existing)         ExternalSensorChannel (NEW, data class)
    ACCELEROMETER                       deviceId: String   e.g. "esp32-home"
    GYROSCOPE                           channelName: String e.g. "humidity"
    ...                                 unit: String        e.g. "%RH"
    EXTERNAL  ←── virtual entry         valueCount: Int     e.g. 1

SensorSample (existing)
    sensor: SensorType              ExternalSample (NEW)
    t: Long                 OR          channel: ExternalSensorChannel
    v: FloatArray                       t: Long
                                        v: FloatArray

SensorSample.toExternal(): ExternalSensorChannel?    ← lookup in ExternalDeviceRegistry
```

**Implementation choices:**
- `SensorType.EXTERNAL` is the single enum entry for all remote sensors (already present in
  serialization model). The `SensorSample` carries `sensor = EXTERNAL`; the actual
  channel (humidity, CO2, etc.) is resolved via `ExternalDeviceRegistry.channelFor(deviceId, idx)`.
- Alternatively (cleaner but more invasive): change `SensorSample.sensor` to a sealed class.
  **This ticket chooses the additive path** — add `ExternalSensorChannel` alongside the existing
  enum without changing `SensorSample`; the channel is resolved in the detector factory.

## Acceptance criteria

- [ ] `ExternalSensorChannel(deviceId, channelName, unit, valueCount)` data class in `core.sensors`.
- [ ] `ExternalDeviceRegistry` in `core.sensors`: thread-safe registry mapping
  `(deviceId, channelIndex)` → `ExternalSensorChannel`. Singleton, populated at pairing time.
- [ ] `ExternalSensorType` sealed entry: when a trigger has `sensor = EXTERNAL` and
  `sourceDevice = "esp32-home"`, the engine looks up the registered channels for that device.
- [ ] `TriggerLibrary` updated to support dynamic external specs: `forDevice(deviceId): List<TriggerSpec>`.
- [ ] Unit tests: register a device with 2 channels, resolve trigger specs, verify samples route.

## Technical notes

- `ExternalDeviceRegistry` is process-singleton; populated by the `DevicePairingService`
  (ticket-057) when a device connects and its capability manifest is received.
- The capability manifest format is part of the wire protocol spec (ticket-055).
- No migration needed: existing macros with `sourceDevice = null` are unaffected.
