# ticket-053: Ambient temperature and humidity triggers

- **Milestone:** M5
- **Priority:** P3
- **Status:** Backlog
- **Dependencies:** ticket-031

## Description

`TYPE_AMBIENT_TEMPERATURE` (°C) and `TYPE_RELATIVE_HUMIDITY` (%) are available on some
Android devices (mostly tablets and some flagships). They enable environment-aware macros:
"alert me when room temperature rises above X" or "notify when humidity is high (rain inside)".
Battery cost is negligible (polling, not trigger-based).

## Acceptance criteria

- [ ] `SensorType.AMBIENT_TEMPERATURE`, `SensorType.RELATIVE_HUMIDITY` added.
- [ ] `GesturePattern.TEMPERATURE_HIGH`, `TEMPERATURE_LOW`, `HUMIDITY_HIGH`, `HUMIDITY_LOW` added.
- [ ] `ThresholdCrossingDetector(sensorType, direction, thresholdLow, thresholdHigh, sensitivity)`
  in `core.sensors`: generic rising/falling edge detector; fires when value crosses the
  threshold and stays crossed for `debounceSec` seconds. Reusable for both temperature and
  humidity.
- [ ] `TriggerSpec` entries marked `available = false` by default; `available` set to true only
  if `sensorMaxRange(type) != null` (device actually has the sensor).
- [ ] Sensitivity maps to hysteresis band: loose = 5 °C / 10 %, tight = 0.5 °C / 1 %.
- [ ] Default thresholds exposed as `customThresholds` in `Trigger` (keys: `threshold_low`,
  `threshold_high`) so users can set their own crossing points.

## Technical notes

- Both sensors are rare on phones (< 5 % of Android devices). The `available` gate ensures
  users without the sensor never see these triggers in the picker.
- `TYPE_AMBIENT_TEMPERATURE` should not be confused with battery temperature (read via
  `BatteryManager`) or CPU temperature. It is the room air temperature.
- Polling at 1 Hz is fine (environment changes slowly); use `maxReportLatencyUs = 10_000_000`
  for hardware batching.
