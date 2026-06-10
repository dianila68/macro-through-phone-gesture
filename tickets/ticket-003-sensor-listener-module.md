# ticket-003: Sensor Listener Module & Gesture Pattern Detection

- **Milestone:** M1
- **Priority:** P1
- **Status:** Backlog
- **Dependencies:** ticket-001, ticket-002

## Description

Build the `core/sensors` module: a `SensorManager` abstraction that exposes sensor streams as `Flow`s and a pattern-detection pipeline that turns raw samples into discrete gesture events (`shake`, `flip_face_down`, `flip_face_up` first).

## Acceptance criteria

- [ ] `SensorStream` abstraction: cold `Flow<SensorSample>` per sensor type with configurable sampling rate and batching (`maxReportLatencyUs`).
- [ ] Detectors for `shake` and `flip_face_down`/`flip_face_up` with a `sensitivity` parameter (0.0–1.0) mapped to internal thresholds, plus per-trigger `cooldown_ms` debouncing.
- [ ] Detection emits `GestureEvent(pattern, timestamp, confidence)` into the engine; engine logs it (action execution is M2).
- [ ] Screen-off detection works: flip gesture detected with screen off within 500 ms (M1 exit criterion).
- [ ] Wakelock duty cycle: partial wakelock held only during open gesture windows; measured duty cycle documented in the PR.
- [ ] Unit tests replay **recorded sensor traces** (JSON fixtures of timestamped samples) through detectors deterministically — no hardware needed in CI. Include at least: positive trace, near-miss trace, and a noisy-walk false-positive trace per pattern.

## Technical notes

- Keep detectors pure functions/classes over sample windows (no Android deps) so they live in a JVM-testable package.
- Record fixture traces with a small debug screen (can be part of this ticket's debug build only).
- Battery budget: declare sampling rates chosen and expected mA impact in the ticket PR per ARCHITECTURE.md cross-cutting rules.
