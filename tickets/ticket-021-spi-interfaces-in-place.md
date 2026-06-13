# ticket-021: Introduce engine SPI interfaces in place

- **Milestone:** M2
- **Priority:** P1
- **Status:** Backlog
- **Dependencies:** — (foundation for the core/app split, see [ADR-0003](../docs/adr/0003-core-app-separation.md))

## Description

First step of the core/app separation (ADR-0003): introduce the SPI seam **inside the
current `:app` module**, with today's concrete classes as the implementations. No module
boundary and no behaviour change yet — this is a pure refactor that makes the seam explicit
so later module extraction is mechanical.

## Acceptance criteria

- [ ] Define `TriggerCatalog { fun detectors(): List<GestureDetector> }`; back it with a `BuiltinTriggerCatalog` delegating to today's `TriggerLibrary`.
- [ ] Define `ActionCatalog { fun executors(): Map<ActionKind, ActionExecutor> }` (kind = action `@SerialName`); register the current executors through it.
- [ ] Define `SealerProvider { fun create(): IntegritySealer? }`; back it with the existing `KeystoreSealerFactory`.
- [ ] Define an `EngineConfig`/builder that takes `sensorStream`, `actionCatalog`, `triggerCatalog`, `sealerProvider`; route `GestureCaptureService`/`MacroStore` wiring through it.
- [ ] Seam types `public`; everything else `internal`. No behaviour change; all existing tests green.

## Technical notes

- This is the converged ADR-0003 step 1. Keeping the dependency arrow one-way (engine ← catalog)
  is the whole point; the SPI is what later lets `:engine` be open-sourced without referencing
  closed catalog data.
