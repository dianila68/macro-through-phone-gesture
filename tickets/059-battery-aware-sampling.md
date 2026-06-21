# ticket-059: Battery-aware sampling rate adaptation

**Track:** M4 — Observability & Sensing  
**Priority:** Medium  
**Estimate:** 2 h

## Problem

The gesture engine registers sensors at `SENSOR_DELAY_GAME` (20 ms) unconditionally. When battery is low this drains power unnecessarily, especially for background operation during Doze mode.

## Acceptance criteria

1. `BatteryMonitor` listens for `ACTION_BATTERY_CHANGED` broadcasts and exposes `StateFlow<BatteryLevel>` with three tiers: `HIGH` (>50%), `MEDIUM` (20-50%), `LOW` (<20%).
2. `SamplingProfile` enum: `PERFORMANCE` (SENSOR_DELAY_GAME ~20 ms), `BALANCED` (SENSOR_DELAY_UI ~60 ms), `POWER_SAVE` (SENSOR_DELAY_NORMAL ~200 ms).
3. `GestureCaptureService` observes `BatteryMonitor` and re-registers sensors at the appropriate delay when the tier changes.
4. `SamplingProfile` is exposed as a `StateFlow<SamplingProfile>` on `GestureCaptureService` so the analytics dashboard can show current profile.
5. Unit test: `BatteryMonitor` emits correct tier for boundary battery percentages (20, 50).

## Affected files

- `core/engine/BatteryMonitor.kt` (new)
- `core/engine/SamplingProfile.kt` (new enum)
- `service/GestureCaptureService.kt` (subscribe + re-register)
- `ui/AnalyticsDashboardScreen.kt` (display current profile)

## Notes

- Re-registering sensors mid-session briefly drops samples; detectors must tolerate a gap at the reset timestamp.
- Doze white-list is already requested (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`) but not tested end-to-end.
