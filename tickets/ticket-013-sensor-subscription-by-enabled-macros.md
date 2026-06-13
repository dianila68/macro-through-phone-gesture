# ticket-013: Subscribe Only to Sensors Needed by Enabled Macros

- **Milestone:** M1
- **Priority:** P1
- **Status:** Backlog
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

- [ ] Derive the active `PatternKind` set from `MacroStore.macros` filtered to `enabled`.
- [ ] Build detectors only for patterns in that set (via `TriggerLibrary.forPattern`).
- [ ] Subscribe to the distinct `SensorType`s of those detectors only.
- [ ] React to macro changes: when the enabled set changes (toggle/add/remove), the
      pipeline re-subscribes without dropping in-flight gestures unnecessarily. A
      simple restart of the pipeline job on a debounced macros change is acceptable.
- [ ] With zero enabled macros, no sensor listeners are registered.

## Technical notes

- `MacroStore.macros` is a hot `StateFlow`; collect it and `flatMapLatest`/restart
  the sensor collection on change. Keep the engine-cooldown state if feasible, or
  document the reset on re-subscribe.
- Watch the cost of frequent re-subscription if a user toggles rapidly — debounce.
