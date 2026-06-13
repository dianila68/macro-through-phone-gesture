# ticket-027: Carve the proprietary `catalog` package + precompiled macros

- **Milestone:** M3
- **Priority:** P1
- **Status:** Backlog
- **Dependencies:** ticket-024, ticket-026, ticket-016

## Description

Create the closed `…gesturemacro.catalog` package inside `:app` (a package behind the SPI, not
its own module — ADR-0003 step 7). This is the moat: the curated `ActionCatalog`, curated
triggers, and the precompiled macros register into the engine through the SPI at startup.

## Acceptance criteria

- [ ] `…catalog` package with `CuratedActionCatalog` (the ticket-016 catalog content) + any curated `TriggerCatalog`, registered via the `EngineConfig` builder in `GestureMacroApp`.
- [ ] Precompiled/seeded macros move to `app/src/main/assets/macros/*` and load through `MacroCodec`.
- [ ] Verify the dependency arrow is **catalog → engine only** (engine never references catalog).
- [ ] Behaviour unchanged: seeded macros + curated actions still available at runtime.

## Technical notes

- Keeping the catalog a *package* (not a `:catalog` module) means the one-way arrow is enforced
  by the SPI + review, acceptable while everything here is closed. Promote to a module only if
  we ever ship catalog variants.
