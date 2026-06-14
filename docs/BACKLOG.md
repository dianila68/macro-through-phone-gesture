# Backlog — dependency-ordered

> Generated 2026-06-13. Orders every **open** ticket by dependency, and groups the
> independent tracks that can run in parallel. Source of truth for *what* each ticket is:
> [`tickets/`](../tickets/). Architecture of the core/app split: [ADR-0003](adr/0003-core-app-separation.md).

## Status snapshot

**Done:** 001, 003, 004, 006, 007, 008, 010, 011, 012, 013 (+ integration-testing emulator job).
**Effectively done / minor remainder:** 002 (code-complete; on-device pass = 009), 005 (codec+YAML shipped; schema-file sync pending).
**Open:** 009, 014, 015, 016–020 (features/bugs), 021–029 (core/app refactor).

## Dependency tiers (topological — a tier depends only on earlier tiers)

| Tier | Tickets (ready when the tier opens) | Note |
|---|---|---|
| **0 — ready now** | **019**, **020**, **014**, **015**, **016**, **021**, **022** | No open dependencies. Mutually independent — any order / parallel. |
| 1 | **017** (←016), **023** (←021) | |
| 2 | **024** (←021,022,023), **018** (←016,017,019) | |
| 3 | **025** (←024) | |
| 4 | **026** (←024,025) | |
| 5 | **027** (←024,026,**016**) | cross-track: needs the catalog backend |
| 6 | **028** (←026,027) | |
| 7 — gated | **029** (←028 + monetization milestone) | deferred deliberately |

## Independent parallel tracks

These four tracks have **no dependencies on each other** except the two cross-links noted, so they can be worked concurrently:

1. **Bug-fix track (do first — cheap, fixes broken advertised features):**
   `019` (app-launch package visibility) · `020` (proximity-wave sensor-relative threshold). Each standalone.
2. **Quality track (independent infra):**
   `014` (detekt → completes StaticAnalysis) · `015` (continuous fuzzing → completes FuzzTesting).
3. **Action-catalog feature track:**
   `016` (catalog backend) → `017` (assembly) → `018` (editor picker UI).
   *Cross-link:* `018` also needs `019` (so app-launch actions are pickable).
4. **Core/app refactor track (ADR-0003):**
   `021` (SPI in place) → `023` (quarantine Android) → `024` (extract `:engine`) → `025` (CI guard) → `026` (extract `:engine-android`) → `027` (carve catalog) → `028` (tidy/verify) → `029` (open-source carve, gated).
   `022` (lock format spec) feeds `024`.
   *Cross-link:* `027` needs `016` from the feature track (the catalog package hosts the `ActionCatalog`).
5. **Sensor-expansion track (M4, mostly independent — research-gated):**
   `030` (deep-research spike) → `031` (per-sensor utility functions) → `032` (single-sensor use cases) → `033` (composed multi-sensor conditions, sensitivity-weighted) → `034` (user-definable composed-macro editor — **future, plan-carefully stub, do not start**).
   *Cross-links:* `031`/`033` are pure-engine work that lands cleanest **after** the refactor track puts logic in `:engine`; `033` extends the macro format (bump governed by `022`/ADR-0002); `032` feeds the curated catalog (`016`). `030` is a `deep-research` deliverable, not code.

## Critical path & recommended order

The longest chain is the refactor track: **021 → 023 → 024 → 025 → 026 → 027 → 028 → 029** (8 deep), with `027` also waiting on `016`. That chain governs the timeline.

Recommended execution:
1. **Now, in parallel:** `019`, `020` (bug fixes) and `021` + `022` (kick off the refactor) and `016` (catalog backend — it's on the refactor's critical path via `027`).
2. **Then:** `017`, `023`; then `024` (the pivotal extraction) + `018`.
3. **Then the refactor tail:** `025 → 026 → 027 → 028`.
4. **Quality track** (`014`, `015`) slots in anywhere — it's fully independent.

## Blocked / externally gated (not code work)

- **009** — on-device M1 verification (screen-off latency, 24 h soak, Doze, restart). Needs a physical Android device; closes M1 but blocks no code.
- **029** — open-source carve. Deliberately gated on the monetization milestone (ADR-0003); do **not** start early.
- **034** — user-definable composed-sensor macro editor. Future stub; do **not** start before `033` settles and ADR-0003 lands.
- **030** — research spike; gates the rest of the sensor-expansion track.
