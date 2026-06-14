# Architecture

Native Android app that captures hardware gestures in the background and executes user-defined macros: system actions (flashlight, media control) and third-party app control via the Accessibility API.

## Tech stack

| Layer | Choice | Rationale |
|---|---|---|
| Language | **Kotlin** (JDK 17 toolchain) | First-class Android support, coroutines |
| Concurrency | **Coroutines + Flow** | Sensor streams and macro pipelines are naturally reactive |
| UI | **Jetpack Compose** (Material 3) | Declarative UI for the macro editor; minimal UI surface overall |
| Persistence | **Room** | Local storage of macros, trigger configs, execution logs |
| Serialization | **kotlinx.serialization** | Canonical JSON macro format; YAML supported at the import/export boundary |
| DI | **Hilt** (planned) | Service/ViewModel graph wiring |
| Background | **Foreground Service** + strategic WakeLocks | See below |
| Action execution | **AccessibilityService**, system Intents, `CameraManager`, media key dispatch | OS-level control surface |

### Module layout (target)

```
app/                  Compose UI, navigation, DI entry points
core/engine/          Macro engine: trigger evaluation → constraints → action dispatch
core/sensors/         SensorManager abstraction, sampling, gesture pattern detection
core/actions/         Action executors (intent, system_toggle, media_control, accessibility)
core/data/            Room entities/DAOs, macro repository
core/serialization/   JSON/YAML schema models, import/export, validation, migrations
service/              Foreground Service + AccessibilityService implementations
```

A single `app` module is acceptable until M2; the package structure above must be respected from day one so the later module split is mechanical.

## Background execution strategy

The hard requirement: **gesture capture must survive screen-off, Doze, and OEM task killers.**

1. **Persistent Foreground Service** (`GestureCaptureService`)
   - Foreground service type: `specialUse` (`FOREGROUND_SERVICE_SPECIAL_USE`, API 34+) with a declared `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` explaining gesture capture; `dataSync`/`health` types do not fit this use case.
   - `START_STICKY` so the system restarts the service after process death.
   - Low-priority, non-dismissible notification (mandatory for FGS) doubling as a quick macro on/off toggle.
2. **Strategic WakeLocks**
   - A `PARTIAL_WAKE_LOCK` is held **only while a gesture window is open** (i.e. a candidate gesture's first phase was detected and the detector is waiting for the completing phase), never indefinitely. Target: lock held for seconds, not minutes.
   - Sensor batching (`maxReportLatencyUs`) is preferred over wakelocks wherever trigger latency tolerances allow.
3. **Doze & battery optimization**
   - Onboarding flow requests `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` exemption (with clear user explanation — this is also a Play policy disclosure point).
   - Detect and surface OEM-specific killers (e.g. via vendor settings deep links) rather than silently dying.
4. **Process resilience**
   - Service self-monitors via a heartbeat persisted to Room; the app surfaces "engine was killed at HH:MM" diagnostics instead of failing silently.
5. **AccessibilityService** (`MacroAccessibilityService`)
   - Separate service, enabled explicitly by the user in system settings; it is the **action-execution arm** (dispatching gestures/actions into third-party apps), not the sensing arm.
   - Scope declared minimally in `accessibility-service` XML (`canPerformGestures`, no broad event flooding: `accessibilityEventTypes` limited to what executors need).

### Permission & policy ledger

| Capability | Permission / declaration | Notes |
|---|---|---|
| Foreground capture | `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `POST_NOTIFICATIONS` | API 34+ subtype justification required |
| WakeLocks | `WAKE_LOCK` | Held only during gesture windows |
| Flashlight | `CAMERA` (flash unit access via `CameraManager.setTorchMode`) | No image capture |
| App control | `BIND_ACCESSIBILITY_SERVICE` (service declaration) | Play Store requires prominent disclosure & justification |
| Battery exemption | `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Policy-sensitive; user-initiated flow only |

## Gesture-Macro data structure

Canonical format is **JSON**; YAML is accepted at import and offered at export as an alternate serialization of the same schema. Every document carries a `version` for forward migrations.

### JSON Schema (v1)

The normative schema lives at [`schema/gesture-macro-v1.json`](../schema/gesture-macro-v1.json) — the single source of truth for tooling, tests, and the kotlinx.serialization models (ticket-005). Highlights: required `version`/`id`/`name`/`trigger`/`actions`; trigger = sensor + pattern + sensitivity + `cooldown_ms`; typed actions (`system_toggle`, `media_control`, `intent`, `accessibility`); optional `constraints` (screen state, time window).

### Example macro

```json
{
  "version": 1,
  "id": "3f1a2b6c-9d4e-4f0a-8b7c-1e2d3c4b5a69",
  "name": "Flip to pause Spotify",
  "enabled": true,
  "trigger": { "sensor": "accelerometer", "pattern": "flip_face_down", "sensitivity": 0.6, "cooldown_ms": 3000 },
  "constraints": { "screen_state": "any" },
  "actions": [
    { "type": "media_control", "target": "com.spotify.music", "command": "play_pause" }
  ]
}
```

Import is **strict**: documents failing schema validation are rejected with field-level errors, never partially applied. Imported macros containing `accessibility` actions are imported **disabled** and require explicit user re-enable (defense against malicious shared macro files).

## Milestone roadmap

> Milestone view of the work; the **dependency order & parallel tracks** live in
> [BACKLOG.md](BACKLOG.md). The **core/app modular refactor** (ADR-0003, tickets 021–029) is a
> cross-cutting *structural* effort that runs alongside M2–M3 and should land before the M4 advanced
> work; it is not a feature milestone.

### M1 — Core Background Engine & Sensor Data Parsing — ✅ largely done
Foreground Service pipeline, SensorManager, the full gesture vocabulary (shake, double-shake, flip up/down, twist, proximity-wave) via the trigger library, demand-driven sensor subscription, engine (trigger → constraints → dispatch). Exit: screen-off gesture logged within 500 ms, 24 h background.
*Tickets: 001, 002, 003, 011, 012, 013 (done); **009** on-device verification (needs a device), **020** proximity sensor-relative threshold (correctness).*

### M2 — Action Execution & Capability-First App Control — in progress
Executor framework (done: flashlight, media keys, intent, accessibility); fix + harden the action surface and lay the capability-first app-control foundation (ADR-0004 Tiers 1–3). Exit: "flip to pause Spotify" + reliable app launch + pick apps by friendly name.
*Tickets: 004 (done); **019** app-launch `<queries>` fix; **035** installed-app picker; **016/017** action-catalog backend + assembly; **036** targeted media control; **039** app-control compliance posture.*

### M3 — Macro UX, Persistence & Per-App Providers — in progress
Compose editor (done, 010), Room library + JSON/YAML import/export (done, 005/007/008), the action-picker UX, and per-app exact-action providers (Spotify deep-link/SDK). Plus quality gates. Exit: a macro survives export→import across devices; users build macros by picking actions, not typing.
*Tickets: 005/007/008/010 (done); **018** editor action picker; **037** per-app deep-link/SDK providers; quality: **014** detekt, **015** fuzz harness, **022** format-spec lock.*

### M4 — Advanced Sensing, Fallback Automation & Cross-Boundary
- **Advanced sensing:** **030** research → **031** per-sensor utilities → **032** single-sensor use cases → **033** composed multi-sensor conditions (sensitivity-weighted).
- **Fallback app control (ADR-0004 Tiers 4–5):** **038** accessibility UI-automation (last resort), **040** Shizuku/root privileged tier, **041** privileged-provisioning UX.
- **Cross-device (original M4):** BLE/LAN bridge exposing **secondary-device sensors** (smartwatch, spare phone) as `external` triggers — pairing, authenticated/replay-protected transport, `source_device` routing. Exit: a watch gesture fires a phone macro over an authenticated BLE link.

### M5 / future-gated
**034** user-definable composed-macro editor (after 033 settles); **029** open-source carve of `:engine` (gated on the monetization milestone, ADR-0003).

## Cross-cutting concerns

- **Security:** the AccessibilityService and the macro import parser are the privilege/attack surface; both get dedicated review (see ../CONTRIBUTING.md) and the M4 bridge requires mutual authentication before any remote trigger is honored.
- **Battery budget:** every detector must declare its sampling rate and expected wakelock duty cycle in its ticket; regressions are release blockers.
- **Testing:** detectors are tested against recorded sensor traces (deterministic replay), not live hardware, in CI.
