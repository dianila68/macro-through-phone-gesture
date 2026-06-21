# ticket-055: Cross-device BLE bridge

**Track:** M4 — Platform expansion  
**Depends on:** ticket-016/017 (safe action catalog), Android 12+ BLE APIs  
**Blocked by:** hardware availability for end-to-end testing

## Context

The macro engine currently dispatches actions only on the local device. A natural
extension is forwarding actions to a paired wearable (watch vibration, earpiece
chime) over Bluetooth Low Energy without requiring a full companion app.

## Goal

A `BleActionExecutor` that opens a GATT connection to a pre-paired device and
writes to a known service/characteristic UUID to signal the action.

## Acceptance criteria

- [ ] `BleActionExecutor` implements the existing `ActionExecutor` interface.
- [ ] Pairs with one configured remote device (persisted in `SharedPreferences` by MAC address).
- [ ] Writes a 1-byte opcode to `SERVICE_UUID` / `CHAR_UUID` (constants configurable via `build.gradle` `buildConfigField`).
- [ ] Handles `BLUETOOTH_CONNECT` permission denial with `ExecResult.Failure(missingPermission)`.
- [ ] Falls back gracefully if BLE is not available (`PackageManager.FEATURE_BLUETOOTH_LE` absent).
- [ ] Retries once on `GATT_FAILURE` with 500 ms delay before returning failure.
- [ ] Unit test (JVM, with mock `BluetoothGatt`): connect→write→disconnect happy path; permission-denied path.

## Implementation notes

- New files: `core/actions/BleActionExecutor.kt`, `core/actions/BleConfig.kt`.
- Uses `BluetoothManager.openGattServer` (Android 12 API 31+); guard with `Build.VERSION.SDK_INT >= 31`.
- Characteristic write uses `WRITE_TYPE_DEFAULT` (requires response); timeout after 3 s via `coroutineScope.withTimeout`.
- Do **not** scan for devices here — expect MAC address pre-configured; scanning is a separate UX flow.
