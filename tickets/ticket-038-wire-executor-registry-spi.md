# ticket-038: Replace ActionDispatcher when-expression with ExecutorRegistrySpi map lookup

- **Milestone:** M3
- **Priority:** P1
- **Status:** Backlog
- **Dependencies:** ticket-021, ticket-036

## Description

`ActionDispatcher.run()` currently routes actions via a sealed `when`-expression that must be
edited every time a new action type is added. Replace it with a map lookup through
`ExecutorRegistrySpi` so that adding a new action type is a zero-touch change to the
dispatcher.

## Acceptance criteria

- [ ] `ActionDispatcher.run()` routes via `ExecutorRegistrySpi.executors()` map instead of a sealed `when`-expression.
- [ ] `BuiltinExecutorRegistry` is properly wired at startup in `GestureCaptureService`.
- [ ] Adding a new action type no longer requires editing `ActionDispatcher`.
- [ ] All existing executor types are covered by the map.
- [ ] All tests green; no behaviour change.

## Technical notes

- `ExecutorRegistrySpi` already exists in `core.engine.Spi.kt`.
- `BuiltinExecutorRegistry` is already implemented in `BuiltinSpiImpls.kt` — this ticket is
  primarily about wiring it into `GestureCaptureService` at startup and updating
  `ActionDispatcher` to use the map.
