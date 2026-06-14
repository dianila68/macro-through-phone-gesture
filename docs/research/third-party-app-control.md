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

## 4. Shell / CLI command injection & privileged control (Shizuku / root)

Considered after the fact (the original passes missed it). Verdict: **not viable for a mainstream
app; only an opt-in advanced tier.**

- An unprivileged app *can* spawn `sh` via `Runtime.exec`, but the child runs under the **app's own
  UID** (`untrusted_app` SELinux domain). Self-scoped commands work; every **privileged** one fails:
  `input` (cross-app injection) needs the **`INJECT_EVENTS`** signature permission held only by system
  and the `shell` UID; `cmd media_session dispatch`, `pm grant`, app-ops similarly require shell/system.
- `am start <deep-link>` via shell adds **nothing** over our in-process `startActivity(Intent)` — same
  code path. So shell injection is only valuable for the *privileged* ops an app otherwise can't do.
- Privileged paths to a shell UID: **root** (niche, breaks Play Integrity); **ADB/wireless-debugging**
  (an app can't `adb shell` itself); **Shizuku/Sui** — a service started at the **`shell` UID** via
  wireless-debugging ADB (no root) or root, exposing a binder API so apps perform privileged ops
  *without root*; **Dhizuku/Device Owner** (survives reboot but very intrusive setup).
- In a Shizuku/root tier we *can* do **true cross-app input injection (`INJECT_EVENTS`)** and **targeted
  `media_session dispatch`** — more robust/lower-latency than accessibility — but at a **per-reboot ADB
  re-pair** cost (Shizuku, non-root). Play-distribution-risky; mostly sideload/F-Droid territory.

## 5. The assistant model (Alexa / Google Assistant)

How voice assistants integrate third parties — and why it matters here:

- **Alexa Skills** are **server-to-server, opt-in**: a voice interaction model in Amazon's console + a
  partner-hosted cloud backend (usually AWS Lambda). Alexa's cloud resolves intent and calls the
  partner endpoint; it **never touches another app's UI**. Standardized capability interfaces
  (Smart Home `PowerController`/`PlaybackController`, the Music Skill API with OAuth account-linking)
  route "play X on Spotify" to the partner cloud. **Alexa for Apps** then deep-links into a native app.
- **Google Assistant** analog on-device: **App Actions** — apps **declare** capabilities in
  `shortcuts.xml` (Built-In Intents / custom intents) that Assistant invokes; the old `actions.xml` is
  deprecated. For media, Assistant uses the app's MediaSession `onPlayFromSearch` / `MEDIA_PLAY_FROM_SEARCH`.
- **Core principle:** assistants invoke **capabilities the third party DECLARED** (cloud skill or
  on-device App Action / deep link / media session) — never UI scraping. Reliable + consensual, but it
  **requires the app to participate**, so it does not reach non-participating apps.
- **For us:** the reusable Android mechanisms are exactly our Tier 1/2 (deep links, `MEDIA_PLAY_FROM_SEARCH`,
  MediaController, per-app SDKs). We **cannot** replicate Alexa's cloud-skill graph (no cloud, no partner
  backends, no privileged routing). App Actions themselves are **Assistant-invoked, not callable by a
  third-party app** — so we reuse the *principle*, not that specific API. This reinforces the rule:
  **prefer app-exposed capabilities; UI scraping (and the Shizuku tier) are the fallbacks.**

## Implications (→ ADR-0004, tickets 035–040)

1. Model actions as a **tiered strategy** that resolves the best available mechanism per target (Tier 1 → 4), not a single executor. (ticket-036/037, extends the action catalog 016/017.)
2. Ship an **installed-app picker** (Tier-2/3 launch + targeting) via `<queries>` + `loadLabel/loadIcon`. (ticket-035.)
3. Add **targeted media control** via MediaController + Notification Listener. (ticket-036.)
4. Add **per-app deep-link/SDK providers** (Spotify first) for exact playback, opt-in, pluggable through the catalog SPI. (ticket-037.)
5. Treat **accessibility automation as a disclosed last-resort fallback**, deterministic only, per-flow. (ticket-038.)
6. Bake in **compliance & distribution posture** (disclosure/consent, no `isAccessibilityTool`, per-app ToS = user's responsibility, sideload hedge). (ticket-039.)
7. Offer a bounded **advanced privileged tier via Shizuku/root** for power users — true cross-app input injection + targeted media dispatch, opt-in, sideload-friendly. CLI command injection by an unprivileged app is rejected (UID/SELinux). (ticket-040.)
8. **Capability-first** is confirmed by the assistant model: prefer app-declared deep links / `MEDIA_PLAY_FROM_SEARCH` / MediaController / SDKs; treat accessibility and Shizuku as fallbacks. No cloud-skill graph is in scope.

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
- SELinux in Android (AOSP) — https://source.android.com/docs/security/features/selinux
- INJECT_EVENTS / targeted injection (AOSP frameworks/native) — https://android.googlesource.com/platform/frameworks/native/+/985a1b2d79
- Shizuku (RikkaApps) — https://github.com/RikkaApps/Shizuku
- Shizuku — advanced capabilities without root (Mobile Hacker, 2025) — https://www.mobile-hacker.com/2025/07/14/shizuku-unlocking-advanced-android-capabilities-without-root/
- What is the Alexa Skills Kit — https://developer.amazon.com/en-US/docs/alexa/ask-overviews/what-is-the-alexa-skills-kit.html
- Alexa Music/Radio/Podcast Skill API — https://developer.amazon.com/en-US/docs/alexa/music-skills/understand-the-music-skill-api.html
- About Alexa for Apps — https://developer.amazon.com/en-US/docs/alexa/alexa-for-apps/about-alexa-for-apps.html
- Built-in intents for App Actions (Android) — https://developer.android.com/develop/devices/assistant/intents
- Google Assistant and media apps — https://developer.android.com/media/implement/assistant
