# ticket-054 — Condition UI Editor

**Track:** Sensing (ticket-033 follow-up)
**Milestone:** M3
**Status:** open
**Depends on:** ticket-033

## Problem

SensorConditions can be configured in code but there is no UI for the user to
attach a condition to a macro or combine conditions with AND/OR/NOT.

## Scope

- Add a "Condition" section to the macro editor below the Trigger picker.
- Support selecting up to one top-level condition from a predefined list
  (IS_STATIONARY, WAS_PICKED_UP, WENT_DARK, WENT_BRIGHT, ALTITUDE_ROSE,
  ALTITUDE_FELL, HEADING_CHANGED).
- Advanced mode: show a tree builder for All/Any/Not combinators.
- Persist the condition as a nullable field on the GestureMacro model (already
  present in ticket-033 groundwork).
- Show a chip on the macro list item when a condition is set.

## Acceptance
- User can add/remove a condition from the editor screen.
- Macro does not fire when condition is false (verified by unit test).
- Condition is preserved across app restarts.
