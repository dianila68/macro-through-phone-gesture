# Research: controlling third-party apps from an Android automation app

> Spike for the "open Spotify and play a song" / app-mapping feature doubts. Cited synthesis
> of three research passes (2026-06-14). Decisions distilled in [ADR-0004](../adr/0004-third-party-app-control-strategy.md);
> tickets 035–039.

## TL;DR

There is **no universal "do anything in any app" API on Android.** Cross-app control is **tiered**:
standardized surfaces (media keys, deep links, `MEDIA_PLAY_FROM_SEARCH`) cover *launch / navigate /
transport / fuzzy-play* for many apps; **a specific action like "play THIS song" almost always needs
the target app's own deep link or SDK** (per-app, auth-gated, doesn't scale); the only universal
injection layer is the **AccessibilityService**, which is fragile, per-flow, and policy-constrained.

## 1. The control taxonomy (preference order)

| Tier | Mechanism | Can do | Cannot / cost | Permission |
|---|---|---|---|---|
| 1 | **App-specific deep link / official SDK** (e.g. `spotify:track:…` + **Spotify App Remote** `playUri`) | Reliably play a **specific** track/playlist | Per-app; App Remote needs `app-remote-control` OAuth scope + **Premium**; Web API needs full OAuth + Premium + registered app. Doesn't generalize. | App-defined / OAuth |
| 2 | **Vendor-neutral intents** — `MEDIA_PLAY_FROM_SEARCH` (`android.media.action.MEDIA_PLAY_FROM_SEARCH`), `ACTION_VIEW` deep links | "Play `<artist/song/genre>`" across compliant players; open/navigate | **Best-match search, not exact track**; app must implement `onPlayFromSearch`; coverage inconsistent | None |
| 3 | **MediaSession transport** — `AudioManager.dispatchMediaKeyEvent` (global), `MediaController`/`MediaSessionManager.getActiveSessions` (targeted) | play/pause/next/prev/seek; targeted control of a chosen app's session; `playFromSearch` | `dispatchMediaKeyEvent` hits the *current* session only (no targeting); targeting needs **Notification Listener** access (or system `MEDIA_CONTENT_CONTROL`); `playFromSearch` unreliable on Spotify | Notification Listener for targeting |
| 4 | **AccessibilityService UI injection** — node-tree traversal + `performAction(ACTION_CLICK)` / `dispatchGesture` | Universal-ish: click anything visible in any app | **Fragile** (no stable selectors, breaks on app updates, localization, WebView/Compose opacity, timing); per-flow maintenance is unbounded; **policy-risky** | User-enabled accessibility |

We already use Tier 3's `dispatchMediaKeyEvent` (`MediaControlExecutor`) and Tier 4 (`MacroAccessibilityService`, ticket-004).

**"Open Spotify and play a song" concretely needs** Tier 1: the Spotify app installed + App Remote SDK (`app-remote-control` scope) + a Premium account + `playUri("spotify:track:<id>")`. A raw `spotify:track:` `ACTION_VIEW` only reliably *opens* the track page; it does not guarantee playback.

## 2. Mapping the app list (so users don't type `com.spotify.music`)

- Enumerate **launchable** apps: `pm.queryIntentActivities(Intent(ACTION_MAIN).addCategory(CATEGORY_LAUNCHER))` → for each `ResolveInfo`: `loadLabel(pm)` ("Spotify"), `loadIcon(pm)`, `activityInfo.packageName`.
- On **Android 11+ (API 30)** this is filtered by **package visibility**; a scoped **`<queries>`** block with the MAIN/LAUNCHER intent makes every launchable app visible — exactly what we need, and the **same block ticket-019 already prescribes** to fix `getLaunchIntentForPackage`.
- **`QUERY_ALL_PACKAGES` is NOT needed** and should be avoided: it's a Play **restricted permission** whose approved uses are narrow (file managers, browsers, antivirus, banking, accessibility/device-management). "Let the user pick an app to launch" is not on the list and is fully served by scoped `<queries>`.

## 3. Accessibility injection: policy & legal reality

- **Technique** = screen-scraping an uncontrolled surface. Breaks on app-version churn, lacks stable selectors, fails on WebView/Compose, and can't be maintained per-app at scale.
- **Google Play policy (2024–2026):** a rule-based macro app *may* use the AccessibilityService **if** it (a) does **not** claim `isAccessibilityTool` (we don't qualify), (b) shows **prominent in-app disclosure + consent**, (c) documents the use in the listing, and (d) stays **strictly deterministic**. The **Oct 30 2025** clarification explicitly **bans autonomous/agentic** use ("autonomously initiate, plan, and execute actions"). Tasker/AutoInput/MacroDroid/Automate survive precisely because they're user-enabled, disclosed, and deterministic.
- **App ToS** frequently forbid automation/scraping, and there is **no programmatic way** to detect this — it's a manual, per-app legal judgement. Responsibility sits with the user; the app must disclaim it and avoid bundling presets that automate high-risk targets (LinkedIn, banking, anti-bot platforms).
- **Distribution:** a disclosed, deterministic, user-enabled tool can live on Play but carries ongoing accessibility-review risk — plan a **sideload / F-Droid** hedge.

## Implications (→ ADR-0004, tickets 035–039)

1. Model actions as a **tiered strategy** that resolves the best available mechanism per target (Tier 1 → 4), not a single executor. (ticket-036/037, extends the action catalog 016/017.)
2. Ship an **installed-app picker** (Tier-2/3 launch + targeting) via `<queries>` + `loadLabel/loadIcon`. (ticket-035.)
3. Add **targeted media control** via MediaController + Notification Listener. (ticket-036.)
4. Add **per-app deep-link/SDK providers** (Spotify first) for exact playback, opt-in, pluggable through the catalog SPI. (ticket-037.)
5. Treat **accessibility automation as a disclosed last-resort fallback**, deterministic only, per-flow. (ticket-038.)
6. Bake in **compliance & distribution posture** (disclosure/consent, no `isAccessibilityTool`, per-app ToS = user's responsibility, sideload hedge). (ticket-039.)

## Sources
- Spotify Android content linking / deep links — https://developer.spotify.com/documentation/android/tutorials/content-linking
- Android common intents (`MEDIA_PLAY_FROM_SEARCH`) — https://developer.android.com/guide/components/intents-common
- `MediaController.TransportControls` — https://developer.android.com/reference/android/media/session/MediaController.TransportControls
- `MediaSessionManager.getActiveSessions` — https://learn.microsoft.com/en-us/dotnet/api/android.media.session.mediasessionmanager.getactivesessions
- Spotify App Remote SDK — https://spotify.github.io/android-sdk/app-remote-lib/
- Spotify Web API start playback — https://developer.spotify.com/documentation/web-api/reference/start-a-users-playback
- Android package visibility — https://developer.android.com/training/package-visibility
- Declare package visibility (`<queries>`) — https://developer.android.com/training/package-visibility/declaring
- Play `QUERY_ALL_PACKAGES` policy — https://support.google.com/googleplay/android-developer/answer/10158779
- Create an accessibility service — https://developer.android.com/guide/topics/ui/accessibility/service
- Play AccessibilityService API policy — https://support.google.com/googleplay/android-developer/answer/10964491
- Play policy announcement (Oct 30 2025) — https://support.google.com/googleplay/android-developer/answer/16550159
