# ticket-042: Fall-detection trigger (flagship)

- **Milestone:** M1 (sensing) — flagship per [ADR-0005](../docs/adr/0005-product-direction.md)
- **Priority:** P1
- **Status:** Backlog
- **Dependencies:** ticket-003, ticket-011

## Description

Detect a likely fall from the accelerometer and expose it as a trigger, so it can drive the
flagship "send my location to a contact" action (ticket-043). New `GesturePattern.FALL` +
`FallDetector` in the pure-JVM trigger library, trace-replay testable like the other detectors.

## The signature

A fall has a recognisable accelerometer pattern: **free-fall** (magnitude drops toward ~0 g for a
short window) → **impact** (a sharp spike well above 1 g) → **post-impact stillness** (low variance
for a few seconds, i.e. the person/phone doesn't get up immediately). Requiring all three stages —
especially the stillness tail — is what separates a fall from setting the phone down hard or a normal
bump.

## Acceptance criteria

- [ ] `FallDetector` (pure JVM, `sensor = ACCELEROMETER`, sensitivity-parameterised): free-fall window → impact spike → stillness; emits `GesturePattern.FALL` with a confidence.
- [ ] Promote a `TriggerLibrary` entry (available) with an honest description.
- [ ] Trace-replay tests: positive (free-fall+impact+still), and negatives — set-down-hard, drop-on-couch (impact but no free-fall / no stillness), walking/running, phone-in-pocket sit-down.
- [ ] A meaningful **cooldown** and a confidence threshold tunable by sensitivity.

## Honest caveats (must be reflected in UX copy)

- **Not medical-grade.** Best-effort; both false positives (annoying) and false negatives (dangerous)
  exist — never imply guaranteed detection.
- **Phone must be on the body** to detect a *person's* fall; a phone on a table can't. Robustness
  improves materially with a wrist/cross-device sensor (M4 bridge) — note as the future hardening path.
- Pair with a **confirm-countdown** in the action (043) so the user can cancel a false alarm before
  anything is sent.
