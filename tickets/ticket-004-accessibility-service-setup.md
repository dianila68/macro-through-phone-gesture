# ticket-004: AccessibilityService Setup (MacroAccessibilityService)

- **Milestone:** M2
- **Priority:** P1
- **Status:** In Progress (service declared with minimal scope, connection-state flow, global-action executor; onboarding UI pending)
- **Dependencies:** ticket-001, ticket-002

## Description

Declare and scaffold the `MacroAccessibilityService` — the action-execution arm that lets macros drive third-party apps. This ticket covers service declaration, minimal capability scoping, and the user onboarding flow; concrete accessibility executors are follow-up tickets.

## Acceptance criteria

- [ ] `MacroAccessibilityService` declared with `accessibility-service` XML config: `canPerformGestures="true"`, `accessibilityEventTypes` restricted to the minimum the executors need (start with `typeWindowStateChanged` only), no `canRetrieveWindowContent` until an executor requires it.
- [ ] Service binds/unbinds cleanly and exposes a connection-state `Flow` the engine can observe (macros with `accessibility` actions are inert while disconnected).
- [ ] Onboarding flow: in-app explanation of **why** the permission is needed and **what the service will/won't do**, deep link to the system Accessibility settings screen, and detection of enabled/disabled state on return.
- [ ] Graceful degradation: engine refuses to enable macros containing `accessibility` actions when the service is off, with actionable UI messaging.
- [ ] A smoke "global action" (e.g. `performGlobalAction(GLOBAL_ACTION_BACK)`) executable from a debug screen to prove the dispatch path.

## Technical notes

- Keep the service a thin dispatcher; executor logic lives in `core/actions`.

## Security / policy

- This is the app's largest privilege surface. Play requires prominent disclosure + AccessibilityService usage justification — onboarding copy is part of this ticket's review.
- Per ARCHITECTURE.md: imported macros with `accessibility` actions arrive disabled; this ticket must not introduce any path that bypasses that rule.
