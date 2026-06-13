# ticket-018: Editor Action Picker UI

- **Milestone:** M3
- **Priority:** P1
- **Status:** Backlog
- **Dependencies:** ticket-010, ticket-016, ticket-017, ticket-019

## Description

The UI half of FR-6's action authoring: replace raw `target`/`command` typing as
the *primary* path with a categorized, scrollable/searchable action picker
sourced from `ActionCatalog` (ticket-016), assembled via ticket-017. The user
should select an action by scrolling through a menu of all possible actions
grouped by category, seeing **only friendly names** (category, app/display name)
— the serialized command the macro actually runs stays hidden, only displayed.
Deliberate manual entry stays available as an **advanced** fallback for power
users and for commands not yet in the catalog.

Today `ui/MacroEditor.kt` adds an action by picking an `ActionType` and typing
free-form `target`/`command` strings (`ActionEditor`, `ActionField`,
`defaultDraft`). This ticket adds a picker in front of that: choosing a category,
scrolling/searching its catalog entries by friendly name, selecting one, and (for
templated entries like App launch) filling the one required slot. The selection
flows through ticket-017 assembly into a `DraftAction`/`MacroAction`, so the rest
of the editor (ordering, delays, validation, save) is unchanged.

## Acceptance criteria

- [ ] An action picker entered from "Add action" that lists `ActionCatalog.available` grouped by `ActionCategory`, scrollable, showing only friendly fields (category, displayName, appName) — never the raw serialized command.
- [ ] Search/filter field narrows entries by friendly name / category as the user types; categories with no match collapse or hide.
- [ ] Selecting an entry adds a configured action row to the list; for templated entries (App launch) the picker collects the one required parameter (chosen app) before the row is added, via ticket-017 assembly.
- [ ] An **Advanced / manual** affordance preserves today's typed `target`/`command` flow for all four action types, including commands not present in the catalog — the existing `ActionEditor` path stays reachable and functional.
- [ ] A catalog-built action row remains editable: re-opening shows the friendly summary and (for templated entries) the chosen parameter; switching a row to Advanced reveals the underlying fields.
- [ ] The accessibility-capability warning (ticket-010 / T11) still fires whenever the action list contains an accessibility action, whether added via picker or manually.
- [ ] No serialized command string is shown in the picker or the row summary in non-advanced mode; the "complexity obscured, only displayed" requirement is verifiable from the UI.

## Technical notes

- Build on ticket-010's editor (`ui/MacroEditor.kt`); reuse `DraftAction`, `ActionsSection`, and `buildMacro`. The picker produces a `DraftAction` (via ticket-017 assembly) and appends it exactly like `defaultDraft` does today, so save/validation is untouched.
- Picker reads `ActionCatalog` only (display fields); it must not reconstruct command strings itself — assembly is ticket-017's job. This keeps the UI free of serialized detail and keeps the catalog the single source of truth.
- **App launch depends on ticket-019.** The "App launch" category, to let the user pick an *installed* app from a list, needs `PackageManager` queries that return nothing under Android 11+ scoped package visibility until `<queries>` is declared (ticket-019). Until then either gate the App-launch category behind ticket-019 or fall back to manual package entry for that category; the other three categories (Media, System toggle, Accessibility) do not depend on ticket-019 and can ship first.
- Consider a Compose `ModalBottomSheet`/dialog with a `LazyColumn` of category headers + entries and a sticky search field; keep it utilitarian, consistent with the editor's current styling (Cards, Material3), per the project's "keep utilitarian" steer.
- State stays Compose-local (`remember`) for now, consistent with ticket-010's deferred ViewModel decision; lift into a ViewModel at the M2 module split alongside the rest of the editor state.
