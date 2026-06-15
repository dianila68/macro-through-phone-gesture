# ticket-051: Rotation vector / orientation triggers

- **Milestone:** M4
- **Priority:** P3
- **Status:** Backlog
- **Dependencies:** ticket-031

## Description

`TYPE_ROTATION_VECTOR` fuses accelerometer + gyroscope + magnetometer into a stable
quaternion representing absolute device orientation. It enables high-quality triggers like
"phone laid flat face-up (portrait level)", "phone held upright", or "specific tilt angle"
without the raw-sensor noise of the individual sensors.

## Acceptance criteria

- [ ] `SensorType.ROTATION_VECTOR` added; `toAndroidType()` maps to `Sensor.TYPE_ROTATION_VECTOR`.
- [ ] `GesturePattern.FACE_UP_LEVEL`, `FACE_DOWN_LEVEL`, `PORTRAIT_UPRIGHT` added (the common
  orientation snapshots useful for macros).
- [ ] `OrientationDetector(targetOrientation, thresholdDeg, sensitivity)` in `core.sensors`:
  converts quaternion → Euler angles via `SensorManager.getOrientation()`-equivalent math;
  fires when pitch/roll match the target within threshold; hysteresis prevents rapid re-fires.
- [ ] `TriggerSpec` entries for each orientation pattern.
- [ ] `MacroEngine.PatternKind.matches()` cases added.
- [ ] Trace-replay unit tests with quaternion fixtures for each orientation.

## Technical notes

- `TYPE_ROTATION_VECTOR` values[0..3] are a unit quaternion (x, y, z, w).
- Convert to rotation matrix via `SensorManager.getRotationMatrixFromVector()`, then to
  azimuth/pitch/roll via `SensorManager.getOrientation()`.
- FACE_UP_LEVEL ≈ pitch ≈ 0°, roll ≈ 0°; PORTRAIT_UPRIGHT ≈ pitch ≈ -90°.
- Battery cost: medium (fused sensor runs gyroscope continuously).
- Sensitivity maps the acceptance cone angle: loose = 30°, tight = 5°.
