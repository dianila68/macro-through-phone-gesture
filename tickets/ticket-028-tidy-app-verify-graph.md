# ticket-028: Tidy `:app` and verify the dependency graph

- **Milestone:** M3
- **Priority:** P2
- **Status:** Backlog
- **Dependencies:** ticket-026, ticket-027

## Description

Final structural step (ADR-0003 step 8): `:app` now holds only `ui/*`, `service/*`, the
`catalog` package, and app wiring. Lock in the module graph.

## Acceptance criteria

- [ ] `:app` contains only UI, services, catalog, and composition root; no stray pure-logic or bindings.
- [ ] Verified graph: `:app → :engine, :engine-android`; `:engine-android → :engine`; `:engine →` (no project deps).
- [ ] A module-graph assertion (or documented `./gradlew :engine:dependencies` check) guards the arrows.
- [ ] Full build + unit + instrumented tests green.

## Technical notes

- Good point to update `docs/ARCHITECTURE.md` with the four-module diagram and re-run the
  REFACTORING_PLAN checkpoint.
