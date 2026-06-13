# ticket-013: Subscribe Only to Sensors Needed by Enabled Macros

- **Milestone:** M1
- **Priority:** P1
- **Status:** Done (2026-06-13)
- **Dependencies:** ticket-011

## Description

Today the capture service builds **every** available detector and subscribes to
**every** sensor those detectors need, regardless of which macros are enabled.
With the gyroscope twist trigger live, that means an always-on gyroscope even
when no enabled macro uses a twist — a real NFR-1 (battery) cost.

Make the active detector/sensor set a function of the enabled macros: only run a
detector if at least one enabled macro's trigger pattern maps to it, and only
subscribe to the sensors those detectors consume.

## Acceptance criteria

- [x] Derive the active `PatternKind` set from `MacroStore.macros` filtered to `enabled`.
- [x] Build detectors only for patterns in that set (via `TriggerLibrary.forPattern`).
- [x] Subscribe to the distinct `SensorType`s of those detectors only.
- [x] React to macro changes: `distinctUntilChanged` + `debounce(300ms)` + `flatMapLatest`
      rebuilds the detector/sensor set when the enabled pattern set changes.
- [x] With zero enabled macros, `detectorStream` returns `emptyFlow()` → no sensor
      listeners are registered.

## Technical notes / outcome

- Implemented in `GestureCaptureService.startPipeline`: `MacroStore.macros` →
  enabled-pattern set → `distinctUntilChanged` → `debounce` → `flatMapLatest` →
  merged sensor streams. `flatMapLatest` cancels the previous sensor collection on
  change, so detectors are rebuilt fresh (engine cooldown state resets on
  re-subscribe — acceptable, the cooldown is per-macro and short).
- **Behavior change:** the UI "last gesture" readout now only updates for patterns
  that an enabled macro uses (previously any detected gesture). This is arguably
  more correct and was accepted.
- 300 ms debounce absorbs the empty→seeded startup transition and rapid toggles.
