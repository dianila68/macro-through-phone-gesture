# Plausible features — parked, not deleted

These tickets are **deliberately shelved**, not abandoned. They describe the ambitious
"control arbitrary third-party apps" direction — the *ambition layer* the project stepped back
from when it re-anchored on a tighter thesis: **private, on-device sensing → safe, local,
reversible reactions, with fall-alert as the flagship** (see [ADR-0005](../../docs/adr/0005-product-direction.md)).

## Why they're parked

Gesture detection is probabilistic (it misfires sometimes), so it pairs well only with **low-consequence, reversible** actions. Driving other apps — exact playback, accessibility UI injection, privileged shell control — is high-consequence, fragile, and policy-risky (the research showed there is no reliable universal automation path). That's the opposite end from "play a sound" or "send my location if I fall". So these are real, well-researched ideas, just not the *now* of the product.

## What's here (revive any time — the rationale is already written)

| Ticket | What |
|---|---|
| 036 | Targeted media control via Notification Listener (needs a sensitive permission for marginal gain over the global media keys we already have) |
| 037 | Per-app deep-link/SDK providers (e.g. Spotify exact playback — per-app, auth/Premium-gated) |
| 038 | Accessibility UI-automation fallback (fragile, Play-policy-constrained) |
| 039 | App-control compliance & distribution posture (gates 038/040) |
| 040 | Shizuku/root advanced privileged tier |
| 041 | Automate/minimise the privileged-tier provisioning |

**Background that stays in `docs/` (the reasoning behind this track):** [ADR-0004 — tiered third-party
app control](../../docs/adr/0004-third-party-app-control-strategy.md) and
[docs/research/third-party-app-control.md](../../docs/research/third-party-app-control.md).

## What was NOT parked (still core)

Launching an app on a gesture is a *safe* local action, so the app launch fix (**019**) and the
installed-app picker (**035**) stayed in the active backlog. Basic global media control already works.
