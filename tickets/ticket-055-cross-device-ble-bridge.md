# ticket-055 — Cross-device BLE bridge (M4)

**Track:** Sensing – cross-device (ticket-034 stub)
**Milestone:** M4
**Status:** open
**Depends on:** ticket-033

## Problem

A phone in a pocket has limited accelerometer signal for fall detection compared
to a wearable on the wrist. A BLE-connected watch or secondary phone could provide
higher-quality sensor data to the engine running on the primary device.

## Scope

- Define a `RemoteSensorSource` interface in the core package.
- Implement `BleSensorBridge` in the android package using BLE GATT scanning.
- Advertise sensor data from a companion app (or the same app on a second device)
  using a custom GATT service UUID.
- Feed BLE-sourced samples into the existing `GestureCaptureService` sensor stream.
- Pair UI: scan for devices, confirm pairing, show connection status chip in the
  main screen header.

## Acceptance
- A sample from a paired BLE device triggers the same detector pipeline.
- BLE disconnect is handled gracefully (falls back to local sensors).
- Battery impact of BLE scanning is bounded (scan window: 10 s on / 30 s off).
