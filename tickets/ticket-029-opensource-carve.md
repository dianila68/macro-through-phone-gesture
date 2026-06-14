# ticket-029: Open-source carve of `:engine` (deferred — monetization-gated)

- **Milestone:** M4
- **Priority:** P2
- **Status:** Backlog (blocked on monetization milestone)
- **Dependencies:** ticket-028

## Description

The deferred, deliberate open-sourcing of `:engine` (ADR-0003 step 9). **Not** part of the
structural refactor batch — gated on monetization being real, per the converged decision to
avoid freezing an API or adding release plumbing before revenue.

## Acceptance criteria

- [ ] Apply **Apache-2.0** headers/license to `:engine` (and the format spec) only.
- [ ] History-hygiene scan: confirm no secrets/closed code ever landed under `engine/` before splitting.
- [ ] `git subtree split --prefix=engine` → push to a new public repo; document the sync workflow.
- [ ] (Optional, only when an external consumer needs it) publish `:engine` to Maven; the closed app then consumes it as a versioned artifact to force API discipline.
- [ ] Publish the macro format **spec** as the public interop contract.

## Technical notes

- The carve is a non-event by construction: `:engine` already depends on nothing internal and is
  compile-guaranteed Android-free (ticket-025). The open artifact is an engine that does nothing
  useful alone (no curated catalog, no precompiled macros, no working sealer, no UI) — which is
  exactly what prevents whole-app copy-paste.
- Do **not** open the T5 sealer or the catalog/macros/UI under any circumstance.
