# ticket-036: Move SoundExecutor and LocationAlertExecutor to android.actions

- **Milestone:** M2
- **Priority:** P1
- **Status:** Backlog
- **Dependencies:** ticket-023

## Description

Complete the ticket-023 quarantine by moving the remaining Android-coupled executors —
`SoundExecutor` and `LocationAlertExecutor` — into the `…gesturemacro.android.actions` package.
The `core.actions` package must then contain only pure action contracts with zero `android.*`
imports.

## Acceptance criteria

- [ ] `SoundExecutor` moved to `…android.actions` package.
- [ ] `LocationAlertExecutor` moved to `…android.actions` package.
- [ ] `core.actions` versions of both classes replaced with `@Deprecated` typealias shims pointing to the new locations.
- [ ] Zero `android.*` or `androidx.*` imports remain in any file under `core.actions`.
- [ ] All tests green; no behaviour change.

## Technical notes

- Follow the same pattern used for `FlashlightExecutor`, `MediaControlExecutor`, and
  `IntentExecutor` in ticket-023: move the class, add a `@Deprecated` typealias in the old
  location so call-sites compile without immediate changes.
- Remove the shims once all call-sites are updated (can be a follow-up or done in this ticket).
