# ticket-031: Per-sensor utility/feature functions (single-sensor signals)

- **Milestone:** M4
- **Priority:** P2
- **Status:** Backlog
- **Dependencies:** ticket-030, ticket-003

## Description

For each usable sensor, provide a small **toolkit of derived-signal functions** — the building
blocks that turn raw sensor frames into meaningful, named states/events (e.g. "is walking",
"step detected", "now dark", "altitude rose"). These are the per-sensor primitives that use
cases (032) and composed conditions (033) consume. Pure-JVM and trace-replay testable, in the
spirit of the existing detectors (`ShakeDetector`, `TwistDetector`, …).

## Acceptance criteria

- [ ] A per-sensor module of feature extractors (one cohesive set per sensor), each: pure Kotlin, stateful-over-a-stream where needed, JVM-testable by trace replay, parameterised by a **sensitivity** value (reuse the `lerp(loose, tight, sensitivity)` convention).
- [ ] Coverage for the shortlist from ticket-030, e.g.: accelerometer → step/cadence, picked-up, stationary; activity (still/walking/running) — prefer the hardware step counter / Activity Recognition where available; light → dark/bright transition + level; barometer → altitude-change / stairs; magnetometer → heading change; gyroscope → rotation/orientation utilities.
- [ ] Each extractor declares the `SensorType` it consumes (mirror ticket-011's detector `sensor` property) so the demand-driven pipeline (ticket-013) can subscribe minimally.
- [ ] Trace-replay test fixtures per extractor (positive + negative + boundary), matching `core/sensors` test conventions.

## Technical notes

- These belong in the **pure-JVM engine** (`:engine` after ADR-0003) — keep them Android-free;
  hardware-backed signals (step counter, Activity Recognition) are fed in through a
  `SensorStream`-style interface implemented in `:engine-android`.
- Distinguish **events** (instantaneous: "step", "shake") from **states** (continuous:
  "is walking", "is dark") — ticket-033's composition depends on that distinction.
