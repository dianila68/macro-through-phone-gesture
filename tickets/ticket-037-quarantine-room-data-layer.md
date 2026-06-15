# ticket-037: Move MacroDatabase and MacroStore to android.data

- **Milestone:** M2
- **Priority:** P1
- **Status:** Backlog
- **Dependencies:** ticket-023, ticket-036

## Description

Complete the ticket-023 quarantine for the data layer: move `MacroDatabase` (Room),
`MacroStore`, and `InstalledAppRepository` into the `…gesturemacro.android.data` package.
Introduce a pure `MacroRepository` interface in `core.data` so the engine never depends on
Room types directly.

## Acceptance criteria

- [ ] `MacroDatabase` (Room) moved to `…android.data` package.
- [ ] `MacroStore` moved to `…android.data` (or `Context` dependency abstracted behind a pure `MacroRepository` interface in `core.data`).
- [ ] `InstalledAppRepository` moved to `…android.data`.
- [ ] `core.data` contains only pure model/interface code with zero `android.*` or `androidx.*` imports.
- [ ] All tests green; no behaviour change.

## Technical notes

- `MacroStore` holds a `CoroutineScope` and a DAO reference; if moving the class, keep the
  public API (`upsert`, `setEnabled`, `remove`, `recordExecution`, `macros` `StateFlow`)
  stable via a `MacroRepository` interface declared in `core.data`.
- Engine-side code should depend only on the `core.data` interface, never on the Room
  implementation.
