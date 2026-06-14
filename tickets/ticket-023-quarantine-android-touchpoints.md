# ticket-023: Quarantine Android touch-points into an `.android` package

- **Milestone:** M2
- **Priority:** P1
- **Status:** Backlog
- **Dependencies:** ticket-021

## Description

Pre-stage the module extraction (ADR-0003 step 3): move every Android-coupled class into an
`…gesturemacro.android` package while everything pure stays under `core.*`. Still one module,
no behaviour change — this makes the `:engine` / `:engine-android` split a mechanical move.

## Acceptance criteria

- [ ] Move to `…android`: `AndroidSensorStream`, `Executors` (Flashlight/Media/Intent), `AccessibilityExecutor`, `HmacSealer`/`KeystoreSealerFactory`, and Room `MacroDatabase`/`MacroStore`/`MacroIntegrity`.
- [ ] Pure code (engine, serialization, detectors, triggers, action contracts, `IntegritySealer` interface, SPI) stays under `core.*` with **zero** `android.*`/`androidx.*` imports.
- [ ] All tests green; no behaviour change.

## Technical notes

- `MacroStore` takes `Context` and calls `KeystoreSealerFactory` → it is Android, belongs in
  `.android`. Do not let a Room type leak into an engine-side signature.
