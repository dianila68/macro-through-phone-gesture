# ADR-0005: Product direction — private on-device sensing → safe local reactions

- **Status:** Accepted (2026-06-14)
- **Context:** First on-device use revealed where the product is strong and where it is weak. The
  delightful, trustworthy uses ("push the phone forward → play a 'no' sound", flip → flashlight, twist →
  pause music) all share a shape: a sensor/gesture drives a **local, reversible, low-consequence**
  reaction. The ambitious uses ("order a pizza", drive arbitrary third-party apps) feel risky — because
  gesture detection is probabilistic and a misfire there is costly, and because the research
  ([ADR-0004](0004-third-party-app-control-strategy.md)) showed those need fragile, policy-risky paths.

## Decision

Re-anchor the product on a tighter thesis:

> **A private, on-device tool that turns reliable background sensing into safe, local, personal
> reactions — with personal-safety (fall → location alert) as the flagship, and quick delight
> macros (sounds, flashlight, media, app launch) as the everyday surface.**

Concretely:

1. **Lead with the sweet spot: "sensor → safe local reaction."** The action affects *your own
   device / your own awareness*, never the outside world or other people's systems. Misfires are
   cheap and reversible.
2. **Flagship: fall detection → send-my-location-to-a-contact** (tickets 042 + 043). High value,
   privacy-sensitive in a way our local-only, no-telemetry, fail-closed posture is *built for*.
   Treated as a serious, honestly-caveated feature (not medical-grade; best when the phone is on the
   body — robustness improves with the cross-device/watch bridge).
3. **Everyday delight: "play a sound / voice" and the existing quick actions** (flashlight, media,
   app launch) as the friendly, low-stakes surface (ticket 044).
4. **Elevate the sensing track** (030→031→032→033): per-sensor utilities and human-state inference
   are now *core*, because they power fall/activity/context detection.
5. **Park the "ambition layer."** Third-party app *control* beyond safe app launch — targeted media,
   per-app SDKs, accessibility injection, Shizuku/root — is moved to
   [`tickets/plausible-features/`](../../tickets/plausible-features/), revivable but not the now.
   ADR-0004 stands as the recorded rationale for if/when it returns.
6. **Privacy is a feature, not overhead.** Local-only, no telemetry, fail-closed — these become
   selling points for a safety/location tool, not just security hygiene.

## What stays in core

Engine + sensors + trigger library (done), the macro editor + persistence (done), the bug fixes
(019 app launch, 020 proximity), quality (014 detekt, 015 fuzz), the core/app modular refactor
(021–029, good engineering regardless of thesis), the action catalog (016–018, **re-scoped to safe
local actions**), the app picker (035), and the sensing track (030–033).

## Candidate safe-local actions (for the fresh backlog to pick from)

Sound/TTS (044), location alert (043), flashlight + SOS, media transport, app launch, **device
toggles** (Do-Not-Disturb / ringer / silent), a discreet **panic gesture** → location alert.
"Left behind" detection is noted but deferred to the cross-device (watch) bridge, since a phone
can't easily tell *you* walked away.

## Consequences

- The backlog re-orients around two core feature tracks (sensing, safe-actions) + a flagship
  (fall-alert); the third-party-control track is parked. See [BACKLOG.md](../BACKLOG.md) and the
  [milestone roadmap](../ARCHITECTURE.md#milestone-roadmap).
- New tickets 042 (fall detector), 043 (location-alert action), 044 (sound/voice action).
- No code is invalidated — the engine/sensor architecture is exactly what this thesis needs.
