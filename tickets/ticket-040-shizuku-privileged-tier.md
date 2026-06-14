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

## Technical notes

- Distribution: this tier is **sideload/F-Droid-oriented** (Play is hostile to privileged-automation
  apps) — coordinate with the ticket-039 distribution hedge.
- Dhizuku/Device-Owner is a heavier alternative that survives reboot but has very intrusive setup;
  note as a possible future, not v1.
- Implement behind the `ActionExecutor`/catalog SPI (ADR-0003) so it's just another provider tier.
