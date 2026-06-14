# ticket-033: Composed multi-sensor conditions (modular, sensitivity-weighted)

- **Milestone:** M4
- **Priority:** P2
- **Status:** Backlog
- **Dependencies:** ticket-031, ticket-032

## Description

The headline of the sensor expansion: macros whose trigger is a **composition of multiple
sensor conditions**, each condition **modular** and driven by its own **sensitivity multiplier**.

Motivating example: *"the light turns off **while** I'm walking → turn on the flashlight."*
That is a composite: an **event** (ambient light → dark) gated by a **state** (is-walking).
Each condition is swappable/modular — e.g. replace the "walking" state with a "temperature
above X" state without touching the rest — and each carries its own sensitivity.

## Acceptance criteria

- [ ] A **condition model**: a predicate tree over the 031 primitives with boolean combinators (AND / OR / NOT) and a clear distinction between **events** (instantaneous, the firing edge) and **states** (continuous guards). A composite fires when an event edge occurs *and* all guard states hold.
- [ ] Each leaf condition is **modular** (independently addable/removable/swappable) and carries its own **sensitivity multiplier** (reuse the `sensitivity` + `lerp` convention per condition).
- [ ] Pure-JVM evaluator in the engine (`:engine`), trace-replay testable with multi-sensor fixtures; deterministic, side-effect-free (mirrors `MacroEngine`).
- [ ] The macro **format** is extended to serialize a condition tree (new `formatVersion`, governed by ADR-0002 + the spec lock ticket-022; provide a migration from the single-trigger v1).
- [ ] The demand-driven pipeline (ticket-013) subscribes to the **union** of sensors referenced by all conditions of enabled macros.
- [ ] At least the motivating use case ("dark-while-walking → flashlight") implemented end-to-end and tested.

## Technical notes

- This is a **significant model change**: today `Trigger` is a single `{sensor, pattern,
  sensitivity, cooldown}`. The condition tree generalises it. Land it behind the format-version
  bump and keep v1 single-trigger macros working via migration.
- Belongs in the pure-JVM engine; hardware-backed states (activity, step counter) arrive through
  the `SensorStream`/SPI seam (ADR-0003), so the evaluator stays Android-free.
- Watch evaluation cost & debounce: continuous state guards re-evaluate on every relevant frame —
  keep the hot path allocation-free like the current engine.
