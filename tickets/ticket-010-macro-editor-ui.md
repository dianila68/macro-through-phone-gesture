# ticket-010: Macro Editor UI

- **Milestone:** M3
- **Priority:** P1
- **Status:** Done (2026-06-13 — full editor shipped; two deliberate deviations noted below)
- **Dependencies:** ticket-007

## Description

Complete FR-6: in-app creation and editing of macros (the manager section currently
covers list/toggle/delete/import/export only).

Backed by a new **trigger library** (`core/triggers/TriggerLibrary`): a single
source of truth the editor reads to offer triggers and the capture service reads
to build its live detector set. The catalog's `available` flag is tied by an init
invariant to the presence of a detector factory, so a trigger can never be offered
in the UI without a detector wired to fire it (or vice versa). `CUSTOM` is excluded
(import-only escape hatch).

## Acceptance criteria

- [x] Editor screen (`ui/MacroEditor.kt`): name field, trigger picker (pattern + description from the library, sensitivity slider, cooldown), constraints (screen state chips, optional HH:MM time window), ordered action list builder (add/remove/move) for all four action types.
- [x] Validation mirrors the model init-block invariants — the editor builds the `GestureMacro` through its own `require` checks, so an invalid macro can never be saved; failures surface inline. **Deviation:** one consolidated error line rather than per-field errors.
- [x] Creating a macro with an `accessibility` action warns about the capability (T11). Runtime still fails closed when the service is disconnected (`AccessibilityExecutor`, NFR-7); the editor does not separately pre-gate the enable toggle.
- [x] Navigation: per-row **Edit** opens the editor prefilled; **New macro (full editor)** button opens an empty one (in-place view swap, no nav dependency added).
- [ ] **Deferred:** ViewModel-hosted state per DESIGN.md. The editor currently holds Compose-local state (`remember`), consistent with the "keep utilitarian" steer. Lift into a ViewModel at the M2 module split, when the `:app` UI layer is separated.

## Follow-ups

- Per-field inline validation (vs the single error line) once a ViewModel hosts field state.
- Optional: gate enabling an accessibility macro on the service being connected, in addition to the runtime fail-closed.
