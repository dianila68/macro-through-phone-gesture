# ticket-058 — Notification Action Arm

**Track:** Everyday safe actions
**Milestone:** M3
**Status:** open
**Depends on:** ticket-016 (action catalog expansion)

## Problem

Gesture macros can only execute local actions (flashlight, media, sound, launch).
Adding a "post a notification" action would let users build silent reminders or
status indicators triggered by gesture.

## Scope

- Add `NotificationAction` to the action catalog with fields: `channel_id`,
  `title`, `body`, `icon` (resource name), `auto_cancel: Boolean`.
- Implement `NotificationExecutor` in `android.actions`:
  - Creates or reuses a notification channel.
  - Posts via `NotificationManagerCompat`.
  - Returns `ExecResult.Success` on post, `ExecResult.Failure` if permission absent.
- Add `POST_NOTIFICATIONS` permission check (runtime on API 33+).
- Add `NotificationAction` to the macro editor action builder UI.

## Acceptance
- Shake → notification appears within 500 ms on a locked screen.
- Repeat-fire does not stack duplicate notifications (use a stable notification ID).
- Action is documented in the JSON schema (v3 bump).
