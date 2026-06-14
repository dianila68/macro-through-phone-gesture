# ticket-017: Catalog Action Assembly + Executor Coverage

- **Milestone:** M3
- **Priority:** P1
- **Status:** Backlog
- **Dependencies:** ticket-016

## Description

Turn a catalog selection into a concrete, runnable `MacroAction`, and guarantee
the executors cover every action the catalog can produce.

ticket-016 gives a registry of `ActionSpec` templates. Some entries are complete
on their own (flashlight toggle, a fixed media command); others are templates
with a slot to fill — the clearest case is **App launch**, where the catalog
entry describes "launch an app" but the concrete target package is chosen at
selection time. This ticket defines that dynamic assembly: taking an
`ActionSpec` plus any selection parameters (a chosen package, a specific media
command, an accessibility command) and producing the final serialized
`MacroAction` the editor saves and the runtime runs.

It also closes the loop with the executors. Every `available` catalog entry must
map to an executor branch that accepts it; this ticket adds a parity check and
fills (or explicitly documents) any gap. The manual/advanced typed path from
`ui/MacroEditor.kt` must keep working unchanged — assembly is an *additional*
route to a `MacroAction`, not a replacement, so imported and hand-typed macros
are unaffected.

## Acceptance criteria

- [ ] An `assemble`/`build` function (backend, JVM-testable) takes an `ActionSpec` + a typed parameter bundle and returns a concrete `MacroAction`, applying the model `init` invariants so an invalid assembly can never be produced.
- [ ] App-launch assembly binds a chosen package into `IntentAction(target = <package>, command = "launch")`; assembly rejects a blank/missing package with a clear message (the catalog template alone is not directly runnable).
- [ ] Media / accessibility assembly binds the selected command into `MediaControlAction` / `AccessibilityAction`; system-toggle assembly needs no extra parameter (flashlight target is fixed).
- [ ] Round-trip: assembling a spec selection then mapping the result back to a draft (as `MacroEditor.toDraft` does) preserves category, friendly name, and parameters — editing a catalog-built action re-opens it correctly.
- [ ] Executor coverage parity: a JVM test asserts every `available` catalog entry, once assembled, is accepted by some executor's dispatch (`when` branch) — no offered action can produce an `ExecResult.Failure("Unknown …")`.
- [ ] The manual/advanced typed path in `ui/MacroEditor.kt` (`DraftAction.toAction`) still produces the same `MacroAction`s as before — covered by an existing/added regression assertion; catalog assembly does not change or gate it.
- [ ] Any catalog action with no executor branch is documented as a gap in **Notes / known limitations** below, not silently shipped as a selectable entry.

## Technical notes

- Assembly is the bridge between ticket-016's pure data catalog and the editor (ticket-018): the picker hands back a spec + parameters, this layer produces the `MacroAction`, the editor saves it through the existing `buildMacro` path.
- Keep assembly backend-only (no Compose). The parameter bundle is plain data (e.g. selected package string, command string); the *list of installed apps* the user picks from is an Android/UI concern (ticket-018) gated on package visibility (ticket-019).
- **Known executor gap — targeted media:** `MediaControlAction` carries an optional `target` package, but `MediaControlExecutor` ignores it and dispatches a global media key to whatever owns the active `MediaSession` (`core/actions/Executors.kt`). So a catalog entry like "Pause Spotify specifically" cannot be honored today. Either (a) seed media entries as untargeted only, or (b) extend the executor to route the key to a named session — track the chosen option here; do not offer a targeted-media entry the runtime can't fulfill.
- **Known executor gap — app launch visibility:** assembled `IntentAction` launches resolve through `PackageManager.getLaunchIntentForPackage`, which returns null under Android 11+ scoped package visibility unless declared in `<queries>`. Assembly succeeds but the launch fails at runtime until ticket-019 lands; note this so app-launch parity tests stay JVM-level (no real `PackageManager`).
- System toggle and intent executors recognize a single command each (`flashlight`, `launch`); accessibility/media have small fixed vocabularies. Keep assembly within those vocabularies — surfacing a new command means an executor change in the same change set, with its own dispatch + test.
