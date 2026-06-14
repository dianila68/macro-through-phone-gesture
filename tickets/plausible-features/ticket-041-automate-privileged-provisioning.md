# ticket-041: Minimize/automate privileged-tier provisioning (within the security boundary)

- **Milestone:** M4+
- **Priority:** P3
- **Status:** Backlog (advanced; refines ticket-040)
- **Dependencies:** ticket-040

## Assessment (the hard boundary, stated up front)

**The initial elevation to a `shell`-UID cannot be self-automated by an unprivileged app.** That is a
deliberate Android security boundary — an app granting itself shell/system privilege is, by definition,
a privilege-escalation exploit. The root-of-trust for the privileged tier therefore *must* come from
**outside the sandbox**: root, or ADB (USB or on-device wireless debugging). We will **not** attempt to
bypass this (it would be malware behaviour and an instant Play ban / security hole).

**What we CAN automate / smooth:**
- **One-time setup without a PC:** an in-app guided **wireless-debugging pairing** flow (Android 11+) —
  the user enables wireless debugging and enters the pairing code in-app; we then connect to the local
  ADB daemon and start the shell-UID service. No computer required.
- **Scripted grants once the channel exists:** through the shell-UID service we can `pm grant` the
  dangerous/secure-settings permissions and set the app-ops our app needs — so after setup it "just works".
- **Proxying, not self-granting, the truly privileged ops:** `INJECT_EVENTS` is a signature permission
  we can **never** `pm grant` to ourselves; instead the cross-app input injection / `media_session
  dispatch` runs **through** the persistent shell-UID service. That's the correct, non-exploit design.
- **Boot persistence:** **free with root** (auto-start on boot). **Device Owner** (`dpm set-device-owner`,
  one ADB command + remove other accounts) survives reboot but is intrusive. On plain non-root Shizuku,
  the service dies on reboot and must be re-triggered via the wireless-debugging flow — this part is
  **inherent and cannot be silently automated**.

## Acceptance criteria

- [ ] In-app guided wireless-debugging pairing UX (no PC) that starts the shell-UID service on Android 11+.
- [ ] After the channel is up, scripted self-provisioning of the exact permissions/app-ops the app needs.
- [ ] Cross-app privileged actions proxied through the shell-UID service (never self-granting `INJECT_EVENTS`).
- [ ] Boot-persistence: auto-start when root is available; document the Device-Owner option; for non-root,
      a clear "re-arm after reboot" flow (honest about the limitation).
- [ ] Explicit copy stating we do not and cannot bypass the elevation boundary.

## Technical notes

- **Reboot re-arm cognitive cost (non-root):** a low-frequency but recurring ritual (unlock → re-enable Wireless debugging if off → open app → "Start"; ~3–6 taps, 15–40 s, more if re-pairing). The real cost is **silent failure** — macros stop until the user remembers. Reduce it: a **Quick Settings tile** / **persistent notification action** that launches the guided start (~1–2 taps), proactive "advanced mode is down — re-arm?" detection instead of silent failure, and on **Android 13+** lean on Shizuku's *trusted-network auto-start*. Acceptable for the power-user audience; a dealbreaker as a mainstream default.
- **No "ephemeral root" shortcut:** rootedness is a persistent device state, so per-trigger start/stop neither helps banking detection nor fits the latency budget (service start = seconds). Keep the channel up, invoke on demand (see ticket-040).

- Sideload/F-Droid-oriented (ties to ticket-039 distribution hedge); Play is hostile to this tier.
- Strictly an enhancement of ticket-040; same `ActionExecutor`/catalog SPI seam (ADR-0003).
