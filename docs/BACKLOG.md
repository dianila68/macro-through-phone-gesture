# Backlog — dependency-ordered

> **Milestone view:** [ARCHITECTURE.md § Milestone roadmap](ARCHITECTURE.md#milestone-roadmap) phases every open ticket into M1–M5. This file is the *dependency* view.

> Re-oriented 2026-06-14 around the product thesis ([ADR-0005](adr/0005-product-direction.md)):
> *private on-device sensing → safe local reactions, fall-alert flagship.* The third-party
> app-control track is **parked** in [`tickets/plausible-features/`](../tickets/plausible-features/).
> Core/app split: [ADR-0003](adr/0003-core-app-separation.md). Source of *what*: [`tickets/`](../tickets/).

## Status snapshot

**Done:** 001, 003, 004, 006, 007, 008, 010, 011, 012, 013 (+ integration-testing emulator job).
**Effectively done / minor remainder:** 002 (on-device pass = 009), 005 (codec+YAML shipped; schema sync pending).
**Open — core (new thesis):** sensing 030–033 + **042** (fall); safe actions 016–018, **043**, **044**, 035, 019; bugs 019/020; quality 014/015; structural refactor 021–029.
**Parked → [`tickets/plausible-features/`](../tickets/plausible-features/):** 036–041 (third-party app control beyond safe launch).

## Tracks (re-oriented around ADR-0005)

**Core:**
1. **★ Flagship — Personal safety:** **042** (fall detector) → **043** (location-alert action). The headline feature; 043 pairs with a confirm-countdown.
2. **Everyday safe actions:** **044** (sound/voice — the "push → 'no'" delight), `016`→`017`→`018` (action catalog + picker, **re-scoped to safe local actions**), `035` (app picker for safe launch), `019` (launch fix). *Cross-links: 043/044 build on the catalog (016); 018←019.*
3. **Sensing (elevated to core):** `030` (research) → `031` (per-sensor utilities) → `032` (single-sensor use cases) → `033` (composed multi-sensor conditions); `042` lives here too; `034` future stub.
4. **Bug-fix (do first — cheap):** `019` (app-launch `<queries>`), `020` (proximity sensor-relative threshold).
5. **Quality (independent infra):** `014` (detekt), `015` (continuous fuzzing).
6. **Core/app refactor (structural, thesis-agnostic — ADR-0003):** `021`→`023`→`024`→`025`→`026`→`027`→`028`→`029`(gated); `022` feeds `024`; `027`←`016`.

**Parked (revive via [ADR-0004](adr/0004-third-party-app-control-strategy.md); not now):**
7. **Third-party app control:** `036`–`041` — targeted media, per-app SDK, accessibility injection, compliance, Shizuku/root. See [`tickets/plausible-features/README.md`](../tickets/plausible-features/README.md).

## Recommended order

1. **Quick wins now:** `044` (sound action — the fastest "wow", pairs with shake/flip/push) · `020` (proximity fix) · `019` (launch fix).
2. **Build the flagship:** `042` (fall detector) → `043` (location alert). Highest value; treat the safety/honesty caveats seriously.
3. **Deepen sensing:** `030` → `031` → `032` → `033`.
4. **Round out actions:** `016` → `017` → `018` (+ `035`).
5. **Structural refactor** (`021`→…→`028`) and **quality** (`014`,`015`) slot in anywhere — independent of the feature work.

## Blocked / externally gated (not code work)

- **009** — on-device M1 verification (latency, soak, Doze, restart). Needs a physical device.
- **029** — open-source carve. Gated on the monetization milestone (ADR-0003).
- **034** — user-definable composed-macro editor. Future stub; after `033` settles.
- **030** — research spike; gates the rest of the sensing track.
- **036–041** — parked (plausible-features); not blocked, deliberately deprioritised.
