# ticket-019: App-Launch Macros Never Fire (Android 11+ Package Visibility)

- **Milestone:** M2
- **Priority:** P1
- **Status:** Backlog
- **Dependencies:** ticket-003

## Description

Field report: *"any kind of movement seems to not trigger the launch of any
app."* The triggers are not at fault — shake → flashlight and twist → play/pause
both fire correctly on the same device — so the failure is in the **action**, not
the gesture. Every app-launch macro silently does nothing.

### Confirmed root cause

`IntentExecutor` in
`app/src/main/kotlin/io/github/dianila68/gesturemacro/core/actions/Executors.kt`
(line 71) resolves the target app with:

```kotlin
val launch = context.packageManager.getLaunchIntentForPackage(spec.target)
    ?: return ExecResult.Failure("No launchable app for package ${spec.target}")
```

On Android 11+ (API 30+), package-visibility filtering makes
`getLaunchIntentForPackage` return `null` for any package the app has **not**
declared visibility to. `app/src/main/AndroidManifest.xml` has **no `<queries>`
element**, so for an installed-but-invisible target the launch intent is always
`null` and the executor returns `ExecResult.Failure("No launchable app for
package …")`. The launch never happens. (On API < 30 the same call succeeds,
which is why the feature looks fine in older-device testing.)

The failure is also **silent**. `GestureCaptureService` (line 137) routes the
results only to `MacroStore.recordExecution(macro, results)`, which writes
`ExecResult.Failure` rows into the Room audit log. Nothing in the UI consumes
that signal, so the user sees no error, no toast, nothing — the gesture appears
to do nothing at all. This invisibility is a second defect that compounds the
first: it turns a fixable resolution bug into "the app is broken."

This is a broken advertised feature (launch-app-by-gesture is a headline
capability), hence P1.

## Acceptance criteria

- [ ] `AndroidManifest.xml` declares a `<queries>` block granting visibility to
      launchable apps, scoped via an `<intent>` filter for action
      `android.intent.action.MAIN` + category
      `android.intent.category.LAUNCHER` (the minimum needed for
      `getLaunchIntentForPackage` to resolve any launcher-visible app).
- [ ] `IntentExecutor.execute` resolves and launches a previously-failing target
      app on an API 30+ device; an integration/instrumented check (or a
      documented manual repro) confirms the launch now succeeds.
- [ ] Decision recorded for **scoped `<queries>` vs `QUERY_ALL_PACKAGES`**: the
      scoped `<queries>` approach is preferred. `QUERY_ALL_PACKAGES` is a
      Google Play **policy-sensitive** permission (requires a declared, approved
      use case at submission and risks rejection for a gesture launcher that only
      needs launchable-app visibility). If a future feature genuinely needs full
      enumeration, that justification is captured here before adding it.
- [ ] When an action fails, the failure is **surfaced to the user** rather than
      only logged: a transient message (toast/snackbar) on the failing
      execution, and/or a visible recent-activity / diagnostics view backed by
      the existing `MacroStore` execution records. The specific
      `"No launchable app for package …"` reason is shown verbatim or mapped to a
      user-readable hint (e.g. "App not found or not visible to GestureMacro").
- [ ] The surfaced-failure path is exercised for at least one failure case
      (unknown package) so silent failures cannot regress unnoticed.

## Notes / known limitations

- The scoped `<queries>` MAIN/LAUNCHER intent makes only **launchable** apps
  visible. Targets without a launcher entry (rare for user-pickable apps) would
  still resolve `null`; that is acceptable for v1 since the macro UI offers
  launchable apps.
- Surfacing failures touches the service → UI boundary; reuse the
  `MacroStore.recordExecution` records already written (see line 61–62) rather
  than adding a parallel channel.
- Re-test specifically on an API 30+ device; the bug is invisible on API < 30,
  which is the most likely reason it shipped.
