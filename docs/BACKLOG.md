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
**New — gesture recording sub-editor:** **045**→**046**→**047**→**048**→**049**→**050**→**051**→**052**→**053**.
**New — M4 platform expansion:** **054**→**055**→**056**→**057**→**058**.
**Parked → [`tickets/plausible-features/`](../tickets/plausible-features/):** 036–041 (third-party app control beyond safe launch).

## Tracks (re-oriented around ADR-0005)

**Core:**
1. **★ Flagship — Personal safety:** **042** (fall detector) → **043** (location-alert action). The headline feature; 043 pairs with a confirm-countdown.
2. **Everyday safe actions:** **044** (sound/voice — the "push → 'no'" delight), `016`→`017`→`018` (action catalog + picker, **re-scoped to safe local actions**), `035` (app picker for safe launch), `019` (launch fix). *Cross-links: 043/044 build on the catalog (016); 018←019.*
3. **Sensing (elevated to core):** `030` (research) → `031` (per-sensor utilities) → `032` (single-sensor use cases) → `033` (composed multi-sensor conditions); `042` lives here too; `034` future stub.
4. **Bug-fix (do first — cheap):** `019` (app-launch `<queries>`), `020` (proximity sensor-relative threshold).
5. **Quality (independent infra):** `014` (detekt), `015` (continuous fuzzing).
6. **Core/app refactor (structural, thesis-agnostic — ADR-0003):** `021`→`023`→`024`→`025`→`026`→`027`→`028`→`029`(gated); `022` feeds `024`; `027`←`016`.

**Gesture recording sub-editor (new track — 045–053):**

7. **Recording session & capture (pure-JVM foundation):**
   `045` (session lifecycle) → `046` (sensor capture + SampleBuffer) → `047` (per-repetition quality scoring).
8. **Envelope & live matching:**
   `048` (envelope builder) → `049` (recorded gesture trigger / live detector).
9. **UI, persistence, validation:**
   `050` (recording sub-editor UI; depends 045+047) → `051` (Room persistence, DB v3; depends 048) → `052` (replay validation / "Test it"; depends 049+050+051) → `053` (gesture library management UI; depends 051+050).

**M4 — Platform expansion (new track — 054–058):**

10. **Condition UI editor (054):** Visual editor for `SensorCondition` rules inside the macro editor. Users drag threshold sliders (AccelerationAbove, LightBelow, StepRateAbove) and preview live sensor values. Depends on `033` (multi-sensor conditions in engine). Entry point: `ui/editor/ConditionEditorSheet.kt`.
11. **Cross-device BLE bridge (055):** Bluetooth Low Energy relay so a macro triggered on the phone can be forwarded to a paired wearable (e.g. watch vibration, earpiece chime). Introduces `BleActionExecutor` + `BleGattServer` in the safe-actions catalog. Requires `BLUETOOTH_CONNECT` permission and Android 12+ APIs.
12. **Macro analytics dashboard (056):** In-app screen listing per-macro execution history: trigger count, last triggered timestamp, average gesture-to-action latency (p50/p95 from `EngineMetrics`). Data sourced from `execution_log` Room table (added by ticket-M-020). Entry point: `ui/analytics/AnalyticsDashboardScreen.kt`.
13. **Widget quick-toggle (057):** Home-screen App Widget (`AppWidgetProvider`) showing the arming state of the macro engine with a single-tap toggle. Uses `RemoteViews`; broadcasts to `GestureCaptureService`. Adds `widget/MacroToggleWidget.kt` + `res/xml/macro_widget_info.xml`.
14. **Notification action arm (058):** Persistent notification (already used by `GestureCaptureService`) gains an inline "Arm / Disarm" action button via `NotificationCompat.Action`. Tapping it toggles engine state without opening the app. Wires into the existing notification builder in `GestureCaptureService`.

**Parked (revive via [ADR-0004](adr/0004-third-party-app-control-strategy.md); not now):**
15. **Third-party app control:** `036`–`041` — targeted media, per-app SDK, accessibility injection, compliance, Shizuku/root. See [`tickets/plausible-features/README.md`](../tickets/plausible-features/README.md).

## Recommended order

1. **Quick wins now:** `044` (sound action — the fastest "wow", pairs with shake/flip/push) · `020` (proximity fix) · `019` (launch fix).
2. **Build the flagship:** `042` (fall detector) → `043` (location alert). Highest value; treat the safety/honesty caveats seriously.
3. **Gesture recording:** `045` → `046` → `047` (parallel: `048`) → `049` → `050` → `051` → `052` → `053`. Can run in parallel with flagship after `044`.
4. **Deepen sensing:** `030` → `031` → `032` → `033`.
5. **Round out actions:** `016` → `017` → `018` (+ `035`).
6. **Structural refactor** (`021`→…→`028`) and **quality** (`014`,`015`) slot in anywhere — independent of the feature work.
7. **M4 platform expansion:** `054` (condition UI; after 033) → `057` (widget) → `058` (notification action) → `056` (analytics; after engine metrics settled) → `055` (BLE; after full safe-action catalog).

## Blocked / externally gated (not code work)

- **009** — on-device M1 verification (latency, soak, Doze, restart). Needs a physical device.
- **029** — open-source carve. Gated on the monetization milestone (ADR-0003).
- **034** — user-definable composed-macro editor. Future stub; after `033` settles.
- **030** — research spike; gates the rest of the sensing track.
- **036–041** — parked (plausible-features); not blocked, deliberately deprioritised.
- **055** — BLE bridge. Gated on Android 12+ BLE APIs and hardware availability for testing.
