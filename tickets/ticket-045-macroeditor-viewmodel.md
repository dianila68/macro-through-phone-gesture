# ticket-045: Add ViewModel and SavedStateHandle to MacroEditor

- **Milestone:** M4
- **Priority:** P2
- **Status:** Backlog
- **Dependencies:** ticket-010

## Description

`MacroEditor.kt` currently holds 20+ local `mutableStateOf` values directly inside the
composable, meaning all editor state is lost on configuration changes (rotation, multi-window
resize). Introduce `MacroEditorViewModel` backed by `SavedStateHandle` so state survives
configuration changes and the composable becomes a thin UI layer.

## Acceptance criteria

- [ ] `MacroEditorViewModel` holds all editor state: name, triggers, actions, and sensitivity.
- [ ] State survives configuration changes via `SavedStateHandle`.
- [ ] `MacroEditor` composable receives the ViewModel via `hiltViewModel()` or `viewModel()` and no longer holds 20+ local `remember` states.
- [ ] Save and cancel actions go through `MacroEditorViewModel`, which calls `MacroStore` (or the `MacroRepository` interface from ticket-037).
- [ ] All tests green; no behaviour change visible to the user.

## Technical notes

- `MacroEditor.kt` currently has 20+ local `mutableStateOf` declarations and no ViewModel or
  `SavedStateHandle` — see `ui/MacroEditor.kt`.
- Wire the ViewModel with Hilt if Hilt is already used elsewhere in the UI layer; otherwise
  plain `viewModel()` is acceptable.
