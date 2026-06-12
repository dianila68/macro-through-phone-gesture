# ticket-006: Repository Refactoring & Reorganization

- **Milestone:** M1 (pre-code hygiene)
- **Priority:** P0
- **Status:** Done (2026-06-11)
- **Dependencies:** bootstrap branch merged into `main`

## Description

Execute [docs/REFACTORING_PLAN.md](../docs/REFACTORING_PLAN.md): clear structural dead weight, give every document a single home, extract the macro schema as a tooling-readable contract, and lock the modular directory layout before feature code lands (ticket-001).

## Acceptance criteria

- [x] Phase 0 actions done: stale `main` resolved (plan step 0.1), branch naming convention in force (0.2).
- [x] JSON Schema extracted to `schema/gesture-macro-v1.json`; `ARCHITECTURE.md` references the file and keeps only the example macro (0.3).
- [x] `ARCHITECTURE.md` moved to `docs/`; **all** relative links across README, CONTRIBUTING, and tickets updated and verified.
- [x] `SECURITY.md` added at root (reporting channel + privilege-surface statement).
- [x] ADRs `docs/adr/0001-native-kotlin-pivot.md` and `docs/adr/0002-json-canonical-format.md` written.
- [x] No empty directories or placeholder files committed (plan rule 0.6).
- [x] README gains a docs index pointing at `docs/`.

## Resolution note

Executed during the SDLC Architecture stage on the session branch `claude/repo-criticality-flaws-review-jxpodr` (pre-dates enforcement of the branch convention for agent sessions).

## Technical notes

- Branch: `chore/ticket-006-repo-refactoring`.
- Link check: run a markdown link checker (or manual pass) before merging — the ARCHITECTURE.md move touches every document in the repo.
- The CI gradlew-guard removal is **not** in scope here; it belongs to ticket-001 (plan item 0.4).
