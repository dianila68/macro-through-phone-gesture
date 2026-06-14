# ticket-035: Installed-app picker (package → friendly name + icon)

- **Milestone:** M3
- **Priority:** P1
- **Status:** Backlog
- **Dependencies:** ticket-019, ticket-018

## Description

Stop making users type `com.spotify.music`. Resolve installed **launchable** apps to friendly
name + icon and present a searchable picker wherever a macro needs an app (launch action, media
target). Grounded in [ADR-0004](../docs/adr/0004-third-party-app-control-strategy.md) §App-list mapping.

## Acceptance criteria

- [ ] Enumerate launchable apps via `pm.queryIntentActivities(Intent(ACTION_MAIN).addCategory(CATEGORY_LAUNCHER))`; per result expose `loadLabel(pm)`, `loadIcon(pm)`, `activityInfo.packageName`.
- [ ] Backed by the scoped `<queries>` (MAIN/LAUNCHER) block from ticket-019 — **no `QUERY_ALL_PACKAGES`** (Play restricted permission; not an approved use here).
- [ ] A searchable app-picker UI used by the editor's "Launch app" action (and later media-target selection), showing label + icon, storing the package internally.
- [ ] Cache the list; refresh on resume; handle an app being uninstalled (the chosen package no longer resolves → user-readable hint, ties to ticket-019 failure surfacing).

## Technical notes

- Reads cleanly on top of ticket-018's action picker — this is the "App launch" category's data source.
- Keep enumeration on a background dispatcher; `loadIcon` is not free for hundreds of apps.
