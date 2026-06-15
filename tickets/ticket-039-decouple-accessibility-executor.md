# ticket-039: Decouple AccessibilityExecutor from MacroAccessibilityService singleton

- **Milestone:** M3
- **Priority:** P1
- **Status:** Backlog
- **Dependencies:** ticket-038

## Description

`AccessibilityExecutor` currently reaches into `MacroAccessibilityService.instance.value`
directly (line 20), creating a hard coupling to the service singleton and making unit testing
impossible without a real service instance. Introduce a narrow interface so the executor
depends only on an abstraction.

## Acceptance criteria

- [ ] `AccessibilityExecutor` does not import anything from the service package.
- [ ] A pure interface (e.g. `AccessibilityServiceGate { val instance: AccessibilityService? }`) is introduced and injected into `AccessibilityExecutor`.
- [ ] `MacroAccessibilityService` sets the gate implementation at `onCreate` and clears it at `onUnbind`.
- [ ] `AccessibilityExecutor` is unit-testable without a real service instance.
- [ ] All tests green; no behaviour change.

## Technical notes

- `AccessibilityExecutor` currently calls `MacroAccessibilityService.instance.value` directly
  at line 20 — that is the only coupling to remove.
- The gate interface can live in `core.engine` or `core.actions`; the concrete wiring lives in
  the service class.
