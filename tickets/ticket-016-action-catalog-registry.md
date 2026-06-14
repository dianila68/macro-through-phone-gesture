# ticket-016: Action Catalog Registry

- **Milestone:** M3
- **Priority:** P1
- **Status:** Backlog
- **Dependencies:** ticket-005, ticket-011

## Description

Introduce a single source of truth for the actions a macro can run — the
backend half of the action picker (see ticket-018 for the UI). Today the editor
makes the user type raw `target`/`command` strings (`ui/MacroEditor.kt`),
which means the set of working actions lives only in the executors' `when`
branches (`core/actions/Executors.kt`) and in the user's memory. There is no
catalog a picker could read, and nothing ties an offered action to an executor
that can run it.

Add `core/actions/ActionCatalog` (an `object` registry of `ActionSpec` entries),
mirroring `core/triggers/TriggerLibrary` exactly: a data class spec described
once for every consumer, plus `all` / `available` / lookup / grouping helpers.
Each entry maps a friendly **{category, displayName, optional appName}** to the
concrete serialized `MacroAction` (or the command fragment) it builds, so the UI
can show only friendly names while the serialized detail stays hidden from the
user — exactly the "complexity obscured, only displayed" requirement.

Entries are grouped by `ActionCategory` (Media control, System toggle, App
launch, Accessibility), each category mapping to one branch of the sealed
`MacroAction` hierarchy. An `available` flag is tied by an `init` invariant to
whether the entry can be built and run today (mirroring `TriggerSpec`), so a
catalog entry can never be offered in the picker without an executor able to
fire it. Seed it with a curated starter set drawn from what the executors
already handle: media play_pause/next/previous/stop, the flashlight toggle, app
launch (template entry; concrete package supplied at selection — ticket-017),
and the accessibility back/notifications commands.

This ticket is backend + JVM-testable only — no UI, no Compose, no Android
framework types in the catalog. The dynamic assembly of a selection into a final
`MacroAction` (binding a chosen package or media command) is ticket-017; the
picker that lists these entries is ticket-018.

## Acceptance criteria

- [ ] `ActionCategory` enum: `MEDIA_CONTROL`, `SYSTEM_TOGGLE`, `APP_LAUNCH`, `ACCESSIBILITY`, each with a display label; one category maps to one `MacroAction` subtype.
- [ ] `ActionSpec` data class with `category`, `displayName`, `appName: String?` (null for generic/system entries), an `available: Boolean`, a short `description`, and a way to produce its `MacroAction` (or command fragment) — e.g. a builder lambda. An `init` invariant ties `available` to the presence of that builder, mirroring `TriggerSpec`.
- [ ] `object ActionCatalog` with `all: List<ActionSpec>`, `available` (filter on the flag — what the picker offers), `byCategory(): Map<ActionCategory, List<ActionSpec>>` (or `forCategory`), and a lookup by stable id / (category,displayName).
- [ ] Every `ActionSpec` carries a stable identifier so a saved macro / picker selection can reference it without depending on display-string ordering.
- [ ] Curated starter set seeded for all four categories, each entry building a valid `MacroAction` that the current executors accept: media `play_pause`/`next`/`previous`/`stop` (`MediaControlAction`); flashlight toggle (`SystemToggleAction(target = "flashlight")`); app launch template (`IntentAction(command = "launch")`, package bound at selection per ticket-017); accessibility `back`/`notifications` (`AccessibilityAction`).
- [ ] Serialized command/fragment detail is internal to the spec — the public surface a consumer reads for display exposes only friendly fields (category, displayName, appName, description), never the raw command string.
- [ ] No Compose / Android dependency in the catalog package; entries build via the `kotlinx.serialization` model in `core/serialization/MacroModels.kt`.
- [ ] JVM `ActionCatalogTest`: catalog/executor parity (every `available` entry builds a `MacroAction` an executor's `when` accepts — fails if a command/target has no executor branch), `available`-vs-builder invariant holds, no duplicate ids, every category non-empty, `byCategory` partitions `all`.

## Technical notes

- Mirror `TriggerLibrary` deliberately: same `all` / `available` / lookup shape, same `init`-invariant trick binding the offered flag to the backing capability, so the two catalogs read and review identically.
- Keep the catalog pure data: the *builder* returns a `MacroAction`; running it stays in `core/actions/Executors.kt`. Catalog never imports an executor.
- `APP_LAUNCH` entries are templates — `displayName`/`appName` describe the slot, the concrete target package is filled in at selection time (ticket-017). The catalog itself does not enumerate installed apps (that needs `PackageManager`, an Android concern, and `<queries>` visibility — ticket-019).
- The executors today recognize a fixed command vocabulary (media: play_pause/next/previous/stop; system toggle: flashlight only; intent: launch only). Seed only entries those branches accept; surface any desired-but-unsupported action as a gap for ticket-017 rather than adding an unbacked entry.
- `CUSTOM`/manual entry is intentionally absent from the catalog — the typed advanced path stays the escape hatch (kept in the editor, ticket-018), exactly as `CUSTOM` is excluded from `TriggerLibrary`.
- **Re-scope ([ADR-0005](../docs/adr/0005-product-direction.md)):** the catalog now centres on **safe local actions**. Add categories for the new direction — **Sound/Voice** (ticket-044), **Location alert** (ticket-043), **Device toggle** (DND/ringer) — alongside Media/System/App-launch. The **Accessibility** category is **de-emphasised** (its UI-automation expansion is parked in `tickets/plausible-features/`); keep only the already-shipped global-action commands if any, and don't build the catalog around third-party control.
