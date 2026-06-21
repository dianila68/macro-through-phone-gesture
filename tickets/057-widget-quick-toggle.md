# ticket-057: Widget quick-toggle

**Track:** M4 — Platform expansion  
**Depends on:** `GestureCaptureService` arm/disarm intent (already exists)

## Context

Users currently arm or disarm the macro engine by opening the app or using the
persistent notification. A home-screen widget gives instant one-tap access without
opening the app.

## Goal

A 1×1 `AppWidgetProvider` showing the current arm state (coloured icon + label)
with a single tap that toggles arm/disarm.

## Acceptance criteria

- [ ] Widget declared in `AndroidManifest.xml` and `res/xml/macro_widget_info.xml` (1×1 cells, `updatePeriodMillis=0`, `resizeMode=none`).
- [ ] Shows “● Armed” (green) or “○ Disarmed” (grey) using `RemoteViews`.
- [ ] Tap sends `ACTION_TOGGLE_ARM` broadcast to `GestureCaptureService`, which updates the notification and arm state.
- [ ] Widget updates immediately on state change via explicit `AppWidgetManager.updateAppWidget` call from the service.
- [ ] Survives device restart (declares `RECEIVE_BOOT_COMPLETED` to restore arm state from `SharedPreferences`).
- [ ] Unit test: `MacroToggleWidgetTest` — verifies `RemoteViews` text and pending-intent action for both states.

## Implementation notes

- New files: `widget/MacroToggleWidget.kt`, `res/xml/macro_widget_info.xml`, `res/layout/widget_macro_toggle.xml`.
- `RemoteViews` layout: `ImageView` (arm icon) + `TextView` (status label), centred, dark background rounded rect.
- `ACTION_TOGGLE_ARM` already declared in `GestureCaptureService.Companion`; just add the broadcast receive path.
- Widget background: use `@drawable/widget_bg` (9-patch or shape drawable with corner radius 12 dp).
