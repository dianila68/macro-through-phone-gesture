# ticket-060 — Migrate Detector Private Helpers to SensorUtils

**Track:** Maintenance / Code quality
**Milestone:** Maintenance
**Status:** open
**Depends on:** ticket-031 (SensorUtils module, already shipped)

## Problem

After introducing `SensorUtils` (ticket-031), several detectors still carry
private copies of the same helpers:
- `FallDetector` has private `magnitude()` and `variance()`
- `StepDetector` has a private RMS helper
- `MagnetometerDetector` has a private heading calculation

This is duplication that will drift over time.

## Scope

- Replace all private detector helpers with calls to `SensorUtils`.
- Delete the duplicated private functions.
- Run tests to confirm no behaviour change.

## Acceptance

- No private functions in detectors that duplicate SensorUtils behaviour.
- All existing tests pass.
- Detekt reports no new violations.
