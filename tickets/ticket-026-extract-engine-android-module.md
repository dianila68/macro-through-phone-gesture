# ticket-026: Extract the closed `:engine-android` bindings module

- **Milestone:** M2
- **Priority:** P1
- **Status:** Backlog
- **Dependencies:** ticket-024, ticket-025

## Description

Create `:engine-android` (`com.android.library`, CLOSED) and move the Android bindings — the
thin impls of `:engine` interfaces (ADR-0003 step 6). The T5 sealing implementation lives here
and stays closed.

## Acceptance criteria

- [ ] New `:engine-android` module depending on `:engine`.
- [ ] Move the quarantined impls: `AndroidSensorStream`, `Executors`, `AccessibilityExecutor`, `HmacSealer`/`KeystoreSealerFactory` (T5), Room `MacroDatabase`/`MacroStore`/`MacroIntegrity`.
- [ ] Move `androidTest` Room/migration + DAO tests here.
- [ ] `:app` depends on `:engine` + `:engine-android`; build + instrumented tests green.

## Technical notes

- T5 (`HmacSealer`/`KeystoreSealerFactory`) is **closed forever** — never promote it to `:engine`.
  Open-sourcing the sealing algorithm would hand forgers the bypass spec.
