# ticket-008: YAML Import/Export at the Boundary

- **Milestone:** M3
- **Priority:** P2
- **Status:** Backlog
- **Dependencies:** ticket-005

## Description

Complete the ADR-0002 contract: accept YAML at import and offer it at export as an alternate serialization of the identical Kotlin models (JSON stays canonical).

## Acceptance criteria

- [ ] `kaml` (or equivalent) added via the version catalog; anchors/aliases disabled, strict mode on (threat T2).
- [ ] `MacroCodec.decode` detects format (or explicit format parameter) and routes; same size cap and import policy as JSON (accessibility ⇒ disabled).
- [ ] Round-trip property tests: model → YAML → model identity; YAML(export) → JSON(import) equivalence.
- [ ] Malformed-YAML fuzz corpus seeds checked in (FuzzTesting stage feed).
