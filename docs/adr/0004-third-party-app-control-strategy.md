# ADR-0004: Tiered strategy for controlling third-party apps

- **Status:** Accepted (2026-06-14)
- **Context:** The product must control other apps ("open Spotify and play a song", "next track", "launch X"). Research ([docs/research/third-party-app-control.md](../research/third-party-app-control.md)) confirms there is **no universal cross-app automation API** on Android. Mechanisms differ sharply in reliability, scope, permission cost, and Google Play policy risk. We need a decision framework so action execution is principled, not ad-hoc.

## Decision

Model third-party app control as a **tiered strategy** that resolves the *best available* mechanism per action/target, preferring higher reliability and lower policy risk:

1. **Tier 1 — App-specific deep link / official SDK** (e.g. Spotify App Remote `playUri`). The only reliable way to perform a *specific* in-app action ("play THIS song"). Per-app, opt-in, often auth/account-gated; pluggable through the catalog SPI (ADR-0003).
2. **Tier 2 — Vendor-neutral intents** (`MEDIA_PLAY_FROM_SEARCH`, `ACTION_VIEW` deep links). App-agnostic launch/navigate/fuzzy-play; best-match, no exact-track guarantee.
3. **Tier 3 — MediaSession transport.** `dispatchMediaKeyEvent` (global play/pause/next — already shipped) and `MediaController` via Notification Listener for *targeted* control of a chosen app's session.
4. **Tier 4 — AccessibilityService UI injection** — universal but fragile, per-flow, **last resort only**.

### Compliance posture (binding)
- Do **not** set `isAccessibilityTool="true"` — we do not qualify; it would violate Play policy.
- Accessibility use requires **prominent in-app disclosure + explicit consent**, user-enabled in system Settings, documented in the store listing.
- Stay **strictly deterministic** — only "trigger → user-defined action". **No autonomous/agentic** operation (explicitly banned, Play policy Oct 30 2025). Never market "AI agent that operates any app" or "works on every app".
- **Per-app ToS is the user's responsibility.** There is no programmatic way to detect whether an app forbids automation; disclaim it and do not ship presets that automate high-risk targets (banking, LinkedIn, anti-bot platforms).
- **Distribution hedge:** plan a sideload / F-Droid path alongside Play, given ongoing accessibility-review risk.

### App-list mapping
Resolve installed **launchable** apps to friendly name + icon via `queryIntentActivities(MAIN/LAUNCHER)` + `loadLabel`/`loadIcon`, backed by the scoped `<queries>` block from ticket-019. **No `QUERY_ALL_PACKAGES`** (restricted Play permission; not an approved use for an app picker).

## Consequences
- Action execution becomes a **resolver over tiers**, generalising today's single `ActionExecutor`; it slots behind the `ActionCatalog` SPI (ADR-0003 / tickets 016–017).
- "Play a specific song" is a **Tier-1, per-app** capability (Spotify first) — not a generic guarantee; the catalog advertises only what a given mechanism can actually deliver.
- Accessibility automation is scoped as an explicitly-disclosed, deterministic, best-effort fallback — never promised as reliable on arbitrary apps.
- Implemented incrementally by tickets 035 (app picker), 036 (targeted media), 037 (per-app deep-link/SDK providers), 038 (accessibility fallback), 039 (compliance & distribution posture).
