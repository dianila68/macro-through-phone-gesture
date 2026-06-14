# ticket-022: Lock the macro format as a versioned spec (golden fixtures)

- **Milestone:** M2
- **Priority:** P1
- **Status:** Backlog
- **Dependencies:** ticket-005, ticket-008

## Description

The macro codec/format becomes the **published, versioned spec** (ADR-0003) — the interop
contract third parties can target without getting the catalog. Freeze it with golden
round-trip fixtures before it is frozen by a document, so format drift is caught by CI.

## Acceptance criteria

- [ ] Golden fixtures: canonical YAML/JSON ⇄ model round-trip cases under `…/resources/golden/`, asserting byte-stable canonical output and exact model equality.
- [ ] Confirm/define an explicit `formatVersion` (today's `version`) and document its bump policy (mirror [ADR-0002](../docs/adr/0002-json-canonical-format.md)).
- [ ] Carry `FuzzCorpusTest` + `YamlCodecTest` forward as the spec's fail-closed guard.
- [ ] A short `schema/`-adjacent note stating the spec is the public interop surface.

## Technical notes

- Independent of the module split; can land in parallel with ticket-021. Travels with `:engine`
  at extraction (ticket-024).
