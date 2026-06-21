# ticket-057 — Home Screen Widget: Engine Toggle

**Track:** UX / Everyday surface
**Milestone:** M3+
**Status:** open
**Depends on:** ticket-010 (Compose UI stable)

## Problem

Starting/stopping the macro engine requires opening the app. A home screen widget
would let power users toggle the service without navigating.

## Scope

- Implement a 1×1 `AppWidgetProvider` with a Start/Stop toggle button.
- Button sends a `PendingIntent` to `GestureCaptureService`.
- Widget state reflects service running status via a `BroadcastReceiver`.
- Support Android 12+ widget preview and resizeable widget manifest entries.

## Acceptance
- Widget shows correct state (running/stopped) after screen rotation and reboot.
- Tapping widget starts or stops the service without opening the app.
- Widget respects battery-optimization exemption state.
