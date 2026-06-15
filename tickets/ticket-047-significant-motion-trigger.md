# ticket-047: Significant-motion trigger (TYPE_SIGNIFICANT_MOTION)

- **Milestone:** M4
- **Priority:** P2
- **Status:** Backlog
- **Dependencies:** ticket-031

## Description

Android's `TYPE_SIGNIFICANT_MOTION` is a one-shot hardware trigger-sensor: it fires once when
the device moves significantly (e.g. picked up, carried) and then disarms until re-registered.
It runs on the sensor coprocessor with zero CPU wake — the lowest-power activity signal on any
Android device. Ideal for "fire once when the user starts moving" use-cases without a wake lock.

## Acceptance criteria

- [ ] `SensorType.SIGNIFICANT_MOTION` added to the model.
- [ ] `GesturePattern.SIGNIFICANT_MOTION` and `PatternKind.significant_motion` added.
- [ ] `SignificantMotionDetector` in `core.sensors` (pure interface); Android-side implementation
  in `android.sensors` uses `SensorManager.requestTriggerSensor()` and re-registers after each
  fire (one-shot semantics preserved).
- [ ] `TriggerSpec` entry in `TriggerLibrary` (available = true, no sensitivity knob needed —
  hardware-determined threshold, so sensitivity field should be hidden in UI).
- [ ] `MacroEngine.PatternKind.matches()` case added.
- [ ] Unit test: mock trigger → engine fires macro exactly once per registration.

## Technical notes

- **Critical**: `TYPE_SIGNIFICANT_MOTION` is a trigger-sensor. Do NOT use `SensorEventListener`.
  Use `TriggerEventListener` + `SensorManager.requestTriggerSensor()`. The sensor fires exactly
  once then auto-cancels; must call `requestTriggerSensor()` again to re-arm.
- `values[0]` is always `1.0f` in the trigger event — no data payload.
- Available on API 18+; `SensorManager.getDefaultSensor(TYPE_SIGNIFICANT_MOTION)` returns
  null on devices without hardware support — mark `available` conditionally via
  `sensorMaxRange()` null-check pattern already in `AndroidSensorStream`.
- This is a wake-up sensor: it wakes the device from deep sleep (no WakeLock needed).
- No cooldown needed — hardware re-arming provides natural debounce.
- The `AndroidSensorStream` `callbackFlow` pattern does not apply; needs a custom
  `TriggerEventListener` implementation in `android.sensors`.
