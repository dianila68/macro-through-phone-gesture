# ticket-025: CI guard — `:engine` stays Android-free

- **Milestone:** M2
- **Priority:** P1
- **Status:** Backlog
- **Dependencies:** ticket-024

## Description

Make the engine's Android-freeness a build invariant, not a hope (ADR-0003 step 5). Two layers
of defence, wired into `check` and run on every PR.

## Acceptance criteria

- [ ] **Primary (structural):** `:engine` applies only `kotlin("jvm")` — no Android plugin — so `import android.*`/`androidx.*` fails to compile.
- [ ] **Secondary (explicit):** a `verifyEngineAndroidFree` Gradle task scanning `engine/src/**/*.kt`, failing on any line matching `^import\s+(android|androidx)\.`, `kotlinx-coroutines-android`, or Keystore/`javax.crypto` symbols, with the message "`:engine` must stay Android-free — move this to `:engine-android`."
- [ ] `tasks.named("check").dependsOn("verifyEngineAndroidFree")`; CI runs it.
- [ ] A deliberately-introduced `android.*` import in `engine/` fails CI (verified once, then reverted).

## Technical notes

- The denylist must include `kotlinx-coroutines-android` — it compiles on JVM but quietly
  re-Android-izes the engine via `Dispatchers.Main`/Looper.
