# ticket-058: Sensor device pairing UI

- **Milestone:** M5
- **Priority:** P3
- **Status:** Backlog
- **Dependencies:** ticket-057

## Description

Simple "Add sensor device" screen that lets users scan for, pair, and manage
BLE sensor devices implementing the gesturemacro/1 protocol. Must feel like
"plug device" — scan → tap → done.

## Acceptance criteria

- [ ] `DevicePairingScreen` (Compose): shows scanning spinner, list of nearby
  devices (name + RSSI), tap to connect.
- [ ] `DevicePairingViewModel` wraps `DevicePairingService` flows.
- [ ] Connected device list screen: device name, channels it exposes, last-seen
  timestamp, disconnect button.
- [ ] Connected device channels appear as available trigger sources in macro
  editor trigger picker.
- [ ] Proper permission request flow for BLUETOOTH_SCAN / BLUETOOTH_CONNECT /
  ACCESS_FINE_LOCATION (pre-31).

## Technical notes

- Use `rememberLauncherForActivityResult` for runtime permission requests.
- Show clear error states: "Bluetooth off", "Permission denied", "No devices found".
- Paired device persisted via `DevicePairingService`; screen re-reads on resume.
