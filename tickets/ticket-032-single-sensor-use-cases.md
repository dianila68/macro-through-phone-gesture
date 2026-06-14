# ticket-032: Single-sensor macro use cases

- **Milestone:** M4
- **Priority:** P2
- **Status:** Backlog
- **Dependencies:** ticket-031

## Description

Turn the per-sensor primitives (031) into a curated set of **single-sensor use-case macros** —
ready-made triggers a user can pick, each driven by one derived signal. These both validate the
031 toolkit end-to-end and seed the curated catalog (ties to the action-catalog feature 016–018).

## Acceptance criteria

- [ ] A concrete shortlist of single-sensor use cases wired as selectable triggers, e.g.: "while walking", "when I start running", "on entering darkness", "when picked up", "on going up stairs", "on heading change".
- [ ] Each is expressible in the macro format (trigger pattern + sensitivity) and runs through the existing engine → action pipeline.
- [ ] Each appears in the trigger catalog (mirror `TriggerLibrary`) with a friendly name + description + sensitivity hint; the demand-driven pipeline subscribes only the needed sensor.
- [ ] JVM trace-replay coverage proving each fires (and doesn't false-fire) on representative traces.

## Technical notes

- This is the "events/states by themselves" deliverable. The format may need a way to bind a
  trigger to a **state** ("while walking") vs an **event** ("step detected") — a small extension
  to `Trigger`/`PatternKind` (governed by ADR-0002 + the spec lock, ticket-022). Keep it minimal
  here; the full condition algebra is ticket-033.
