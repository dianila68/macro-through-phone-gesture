# ticket-048: Activity Recognition triggers (walking / running / still / vehicle)

- **Milestone:** M4
- **Priority:** P2
- **Status:** Backlog
- **Dependencies:** ticket-031, ticket-003

## Description

Android's Activity Recognition API (via `ActivityRecognitionClient` from Google Play Services,
or the hardware step counter as a fallback) identifies coarse human activity: WALKING, RUNNING,
ON_BICYCLE, IN_VEHICLE, STILL, TILTING. These are richer state guards than step counting alone
and are the recommended way to implement "fire only when walking" constraints.

Preferred path (no Google Play Services): The hardware step detector + step rate heuristic
(ticket-031) can classify WALKING vs STILL without Play Services. The Play Services path
is an enhancement where available.

## Acceptance criteria

- [ ] `GesturePattern.IS_WALKING`, `IS_RUNNING`, `IS_IN_VEHICLE`, `IS_ON_BICYCLE` added.
- [ ] `ActivityStateDetector` in `core.sensors`: classifies activity from step-rate and
  accelerometer energy; pure JVM, trace-testable. Fires state-change events when the
  classification changes (hysteresis to avoid flapping).
- [ ] Optional Play Services path in `android.sensors`: `ActivityRecognitionClient`
  implementation of the same interface, registered/deregistered with the foreground service.
  Graceful fallback to the pure-JVM path when Play Services unavailable.
- [ ] `TriggerSpec` entries for each pattern (available=true for accelerometer path;
  availability of Play Services path detected at runtime).
- [ ] `ConditionEvaluator` state-transition table updated for activity states (IS_WALKING ↔
  IS_STILL, etc.) so conditions like `And(Pattern(IS_WALKING), Pattern(GOING_DARK))` work.
- [ ] Unit tests: step-rate trace → correct activity classification.

## Technical notes

- Step rate ≥ 80 steps/min ≈ walking; ≥ 150 ≈ running. Use 10-second sliding window.
- `ActivityRecognitionClient.requestActivityUpdates(intervalMs, pendingIntent)` — minimum
  reliable interval ≈ 10_000 ms; battery cost is very low (coprocessor).
- Requires `com.google.android.gms:play-services-location` dependency if Play Services path
  is included; gate behind `BuildConfig` or runtime check.
- `android.permission.ACTIVITY_RECOGNITION` required on API 29+.
