# ticket-040: Advanced privileged tier via Shizuku/root (opt-in)

- **Milestone:** M4+
- **Priority:** P3
- **Status:** Backlog (advanced / power-user; sideload-oriented)
- **Dependencies:** ticket-039, ticket-038

## Description

ADR-0004 **Tier 5**: an opt-in advanced mode that, when the user has set up **Shizuku** (a service
running at the `shell` UID via wireless-debugging ADB, no root) or has root, performs **privileged**
control that an ordinary app cannot — most importantly **true cross-app input injection**
(`INJECT_EVENTS`) and **targeted `media_session dispatch`** — more robustly and at lower latency than
the accessibility fallback (ticket-038). Plain unprivileged "CLI command injection" is **out of scope**:
it's blocked by the app's UID/SELinux domain, and `am start <deep-link>` adds nothing over our
in-process `startActivity`.

## Acceptance criteria

- [ ] Detect Shizuku availability and bind its API only when the user has explicitly set it up; degrade silently to the normal tiers when absent.
- [ ] Provide privileged actions where they beat the alternatives: cross-app input/gesture injection, targeted media-session dispatch, (optionally) `pm grant`/app-ops for self-setup.
- [ ] An explicit, honest onboarding explaining the Shizuku/root setup, the per-reboot ADB re-pair cost (non-root), and the risks.
- [ ] Keep this behind a clearly-labelled "advanced" gate; the default app never depends on it.
- [ ] Make **minimal/no persistent system-wide changes** (proxy through the shell-UID service) so uninstall leaves nothing to clean; never use Device Owner for v1 (uninstall trap).
- [ ] Onboarding states the security tradeoffs plainly: Shizuku is non-root and bounded (can't read other apps' data); some banking apps may still detect it; root is total-trust and breaks many banking/payment apps; non-root requires re-arming after reboot.

## Security model & user tradeoffs (assessment)

- **Is granting this secure?** It's a real trust expansion, at two levels. **Shizuku (shell UID, non-root)** is *bounded*: it can inject input, change secure settings, grant permissions, manage packages — but **cannot read other apps' private data** (that needs root). **Root** is *unbounded* (a bug/compromise in our app becomes full-device compromise). → Prefer Shizuku; never require root; request the narrowest scope.
- **Exploitable by other apps?** No, not by our grant: Shizuku authorises **per app** via a runtime permission the user approves individually — our access does not leak to other apps. The residual risk is the user being tricked into granting a *malicious* app, plus the temporary ADB surface while wireless debugging is on → minimise that window (turn wireless debugging off once the service is up). Security vendors treat Shizuku's presence as a root-like risk signal.
- **Does it break banking apps?** **Shizuku (non-root) does not root the device** and keeps Play Integrity/SafetyNet intact — most banking apps keep working. BUT some banking/UPI/payment apps independently detect Shizuku (or developer-mode/ADB) and refuse to run (open Shizuku feature request for a stealth mode); reversible by disabling/uninstalling Shizuku. **Actual root commonly breaks** banking/payment/DRM apps (Play Integrity device/strong-integrity fails). → Default to Shizuku; warn explicitly about root's banking cost.
- **Auto-clean on uninstall?** Android auto-removes our app's data and our package's self-granted permissions on uninstall. **But there is no reliable on-uninstall hook**, and any *system-wide* setting we changed persists. → Design for **minimal/no persistent system changes** (proxy privileged actions through the shell-UID service rather than mutating global state) so there is nothing left to clean. Shizuku is a separate, user-managed app. **Device Owner is a trap here**: a device-owner app can't be uninstalled until device-owner is removed (`dpm remove-active-admin` / factory reset) — another reason to avoid it for v1.
- **Can the app restart the service after reboot itself?** **Non-root: no** — re-establishing a shell-UID process needs the ADB/wireless-debugging channel; the app can't silently spawn it, so the user must re-trigger the connect flow. **Root: yes** — auto-start on boot. (Device Owner sidesteps it: its privileges are persistent across reboot by nature, at the uninstall/intrusiveness cost above.)

## Technical notes

- Distribution: this tier is **sideload/F-Droid-oriented** (Play is hostile to privileged-automation
  apps) — coordinate with the ticket-039 distribution hedge.
- Dhizuku/Device-Owner is a heavier alternative that survives reboot but has very intrusive setup;
  note as a possible future, not v1.
- Implement behind the `ActionExecutor`/catalog SPI (ADR-0003) so it's just another provider tier.
- **On-demand invocation, persistent channel:** invoke the privilege *per action* (cheap — `su -c`/binder call is sub-second), but **keep the shell-UID channel/service running**; (re)starting it costs *seconds* (process spawn + binder handshake), so it must NOT be torn down and rebuilt per trigger — that would blow the gesture→action latency budget. An idle binder endpoint costs ≈ no battery.
- **No per-action "root toggle":** rootedness (unlocked bootloader + su daemon) is a *persistent device state*, not a runtime-toggleable capability, so time-slicing privilege per trigger does **not** make root-detecting banking apps happy — they see the device as rooted regardless. Magisk DenyList/Zygisk hiding is the (imperfect, increasingly-defeated) tool for that, not our concern to implement.
