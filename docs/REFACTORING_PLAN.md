# Refactoring & Reorganization Plan

> Owner: dianila68 · Created: 2026-06-11 · Status: **Executed** (Phases 0–1 done 2026-06-11; Phase 2 code tree pending ticket-001)
> Execution tracked by [ticket-006](../tickets/ticket-006-repo-refactoring.md). Check items off in the PRs that implement them.

## Goal

Before feature code lands (ticket-001 onward), put the repository in its final shape: remove everything that is dead weight or duplicated, give every document a single home, lock in the modular directory layout, and pin an ordered execution sequence so the next steps are unambiguous.

## Phase 0 — Audit: what is useless and gets cleared

An honest audit of a 2-commit repo finds little dead code — the waste here is **structural**:

| # | Finding | Why it's a problem | Action |
|---|---|---|---|
| 0.1 | `main` is stale: it still contains only the original one-line README, while all real content lives on `claude/repo-criticality-flaws-review-jxpodr` | The default branch misrepresents the project; CI never runs on real content | Merge the bootstrap branch into `main` (PR), then delete the merged work branch |
| 0.2 | Current work branch name violates our own convention in [CONTRIBUTING.md](../CONTRIBUTING.md) (`<type>/ticket-NNN-…`) | Process rule is dead-on-arrival if the repo itself ignores it | After 0.1, all new branches follow the convention — starting with `chore/ticket-006-repo-refactoring` |
| 0.3 | The Gesture-Macro JSON Schema is **embedded in prose** (ARCHITECTURE.md) | A schema living inside a markdown code block can't be referenced by tooling/tests and will drift from the models | Extract to `schema/gesture-macro-v1.json` as the single source of truth; ARCHITECTURE.md links to it and keeps only the example |
| 0.4 | CI's "skip if no `gradlew`" guard in `.github/workflows/android.yml` | Useful now, useless (and masking failures) the moment Gradle lands | Flagged for **removal in ticket-001** — already noted in the workflow comment; this plan makes it an explicit acceptance criterion |
| 0.5 | Old GitHub repo name (`…-gesture-`, trailing hyphen) | Already fixed by rename; the local remote still points at the redirecting URL | Update `origin` to `https://github.com/dianila68/macro-through-phone-gesture.git` on each clone |
| 0.6 | No placeholder junk to delete (no `.gitkeep` farms, no generated files) | — | Keep it that way: **empty directories are never committed**; structure appears only when content does |

## Phase 1 — Documentation: one home per document

Target layout (root keeps only what GitHub surfaces automatically):

```
README.md            entry point — pitch, status, links (stays at root)
CONTRIBUTING.md      process rules (stays at root — GitHub renders it in PR UI)
LICENSE              (root, unchanged)
SECURITY.md          NEW — vulnerability reporting + statement on the app's privilege surface
docs/
  ARCHITECTURE.md    moved from root; schema section slimmed to link + example (see 0.3)
  REFACTORING_PLAN.md  this file
  adr/
    0001-native-kotlin-pivot.md       NEW — records why cross-platform was rejected
    0002-json-canonical-format.md     NEW — records JSON-canonical / YAML-at-boundary decision
```

Steps:

- [x] Create `SECURITY.md` (reporting channel; note that AccessibilityService + macro import are the audited surfaces, per CONTRIBUTING).
- [x] Move `ARCHITECTURE.md` → `docs/ARCHITECTURE.md`; fix every relative link (README, CONTRIBUTING, all 5 tickets reference it).
- [x] Extract the JSON Schema to `schema/gesture-macro-v1.json` (see 0.3); ARCHITECTURE keeps the example macro only.
- [x] Write the two ADRs above (short, one page each — decisions are already documented in prose, ADRs make them citable).
- [x] Add a docs index section to `README.md` pointing at `docs/`.

Rule going forward: **README/CONTRIBUTING/SECURITY at root, everything else under `docs/`, decisions as ADRs** — no new top-level markdown files.

## Phase 2 — Modular directory layout (locked)

Final repository shape, aligned with the module plan in ARCHITECTURE.md. Code directories are **created by ticket-001**, not before (per 0.6):

```
.github/workflows/    CI (exists)
docs/                 documentation (Phase 1)
schema/               versioned macro format contracts: gesture-macro-v1.json, v2, …
tickets/              file-based backlog (exists)
app/                  ┐ Compose UI, DI entry points
core/
  engine/             │ trigger evaluation → constraints → action dispatch
  sensors/            │ SensorManager abstraction + gesture detectors (JVM-testable)
  actions/            │ action executors                      (created by ticket-001,
  data/               │ Room entities/DAOs, repositories       single Gradle module
  serialization/      │ schema models, import/export           with these packages
service/              ┘ GestureCaptureService, MacroAccessibilityService   until M2)
```

Module-split trigger: the single `app` module splits into Gradle modules matching the tree above **at the start of M2** (when `core/actions` and `service/` gain real depth). Until then the boundary is enforced by package structure + review.

- [x] `schema/` created in Phase 1 (schema extraction).
- [x] Code tree created by ticket-001 (single `app` module; packages emerge with code); ticket-001 acceptance criteria already require this layout.
- [ ] M2 kickoff includes a `chore/` ticket for the module split.

## Phase 3 — Next steps, pinned (ordered)

| Order | Step | Branch / Ticket | Done when |
|---|---|---|---|
| 1 | Merge bootstrap branch into `main` | PR from `claude/repo-criticality-flaws-review-jxpodr` | `main` shows full infrastructure; work branch deleted |
| 2 | Execute Phases 0–2 of this plan | `chore/ticket-006-repo-refactoring` | All checkboxes above ticked; links green |
| 3 | Gradle scaffolding | `feat/ticket-001-gradle-scaffolding` | CI runs real `build` + `lint`; gradlew guard **removed** (0.4) |
| 4 | Foreground service backbone | `feat/ticket-002-foreground-service` | Service survives 24 h background (M1 exit criterion groundwork) |
| 5 | Sensor module + detectors | `feat/ticket-003-sensor-listener-module` | Flip detected screen-off < 500 ms; trace-replay tests in CI → **M1 done** |
| 6 | AccessibilityService setup | `feat/ticket-004-accessibility-service-setup` | Smoke global action dispatches → M2 underway |
| 7 | Schema models & import/export | `feat/ticket-005-json-macro-schema` | Round-trip tests pass against `schema/gesture-macro-v1.json` |

Re-plan checkpoint: at M1 completion (step 5), review this plan, file M2 executor tickets, and update ticket statuses — the backlog beyond step 7 is intentionally not pinned yet.

## Out of scope

- No Kotlin/Gradle code is written by this plan itself (steps 3+ own that).
- No history rewriting: the audit clears structure, not commits.
- M4 (BLE/LAN device bridge) planning stays in ARCHITECTURE.md until M3 nears completion.
