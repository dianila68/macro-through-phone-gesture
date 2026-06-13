# ticket-011: Trigger Library + Gesture Vocabulary Expansion

- **Milestone:** M1
- **Priority:** P1
- **Status:** Done (2026-06-13)
- **Dependencies:** ticket-003

## Description

Introduce a single source of truth for gesture triggers and expand the live
vocabulary beyond shake + flip.

`core/triggers/TriggerLibrary` describes every trigger once (display name,
description, sensor, default cooldown, sensitivity hint, and an optional detector
factory). The editor lists the `available` entries to offer the user; the capture
service builds its live detector set from `detectors()`. A `TriggerSpec` init
invariant ties `available` to the presence of a detector factory, so a trigger
can never be offered in the UI without a detector wired to fire it.

## Acceptance criteria

- [x] `TriggerLibrary` catalog with `all` / `available` / `forPattern` / `detectors`.
- [x] `GestureCaptureService` builds detectors and subscribes to sensors from the catalog (no hardcoded detector list).
- [x] `GestureDetector` declares the `SensorType` it consumes; the pipeline subscribes to exactly the distinct set and merges the streams.
- [x] `DoubleShakeDetector` (two shakes within 1.2 s) — composition over `ShakeDetector`.
- [x] `TwistDetector` (gyroscope wrist-twist: reversal flick around the long axis).
- [x] JVM trace-replay tests for both detectors; `TriggerLibraryTest` covers catalog/detector parity and the live sensor span.

## Notes / known limitations

- A vigorous single shake can satisfy the inner shake detector twice, so SHAKE
  and DOUBLE_SHAKE may both match one energetic gesture. Documented; the patterns
  are offered separately so the user picks intent.
- The gyroscope now joins whenever a twist trigger is live, regardless of whether
  any enabled macro uses it — see ticket-013 for the battery optimization.
- 5 of 6 triggers are live; `proximity_wave` remains (ticket-012).
