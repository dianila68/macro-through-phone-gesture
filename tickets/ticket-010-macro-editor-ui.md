# ticket-010: Macro Editor UI

- **Milestone:** M3
- **Priority:** P1
- **Status:** Backlog
- **Dependencies:** ticket-007

## Description

Complete FR-6: in-app creation and editing of macros (the manager section currently
covers list/toggle/delete/import/export only).

## Acceptance criteria

- [ ] Editor screen: name field, trigger picker (sensor, pattern, sensitivity slider, cooldown), constraints (screen state, time window), ordered action list builder for all four action types.
- [ ] Validation mirrors the model init-block invariants with inline field errors.
- [ ] Creating a macro with an `accessibility` action warns about the capability (T11) and requires the service to be enabled before the macro can be enabled (NFR-7).
- [ ] Navigation: list row tap opens the editor; new-macro FAB or button.
- [ ] ViewModel-hosted state per DESIGN.md (no logic in composables).
