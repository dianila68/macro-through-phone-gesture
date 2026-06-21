# ticket-058: Notification action arm button

**Track:** M4 — Platform expansion  
**Depends on:** `GestureCaptureService` persistent notification (already exists)

## Context

`GestureCaptureService` shows a persistent foreground notification while running.
Currently it has no interactive actions. Adding an inline Arm / Disarm button
lets users toggle the engine state without opening the app or home screen.

## Goal

Extend the existing `NotificationCompat.Builder` in `GestureCaptureService` with a
`NotificationCompat.Action` button that toggles arm/disarm.

## Acceptance criteria

- [ ] Notification shows “Disarm” action when armed; “Arm” action when disarmed.
- [ ] Tapping the action sends `ACTION_TOGGLE_ARM` `PendingIntent` to the service; service calls `arm()` / `disarm()` and re-issues the notification with the updated action label.
- [ ] `PendingIntent` uses `FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE`.
- [ ] Notification channel remains `CHANNEL_ID_ENGINE` (already declared); no duplicate channel creation.
- [ ] On Android 12+ (API 31): `PendingIntent.FLAG_MUTABLE` is **not** used; confirm with `if (Build.VERSION.SDK_INT >= 31)` guard on flags.
- [ ] Unit test: `GestureCaptureServiceNotificationTest` — verifies action title string and pending-intent action for both arm states (using `ShadowNotificationManager`).

## Implementation notes

- Change is confined to the `buildNotification(armed: Boolean)` method in `GestureCaptureService`.
- Add `addAction(R.drawable.ic_arm, actionLabel, pendingIntent)` to the builder.
- Re-issue with `NotificationManagerCompat.from(this).notify(NOTIF_ID, buildNotification(newState))`.
- Keep existing title / content-text / small-icon unchanged to avoid jarring UI shifts.
