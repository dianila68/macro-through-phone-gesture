# ticket-014: detekt Static Analysis

- **Milestone:** M2
- **Priority:** P2
- **Status:** Backlog
- **Dependencies:** ticket-001

## Description

Complete the StaticAnalysis stage: ktlint (style) is wired; detekt (code-smell /
complexity / potential-bug rules) is the missing half. Add detekt to the build
and CI with a committed baseline so it lands green and then ratchets.

## Acceptance criteria

- [ ] detekt Gradle plugin in the version catalog and applied to `:app`.
- [ ] A committed `config/detekt/detekt.yml` (start from the default, tune obvious
      false positives) and a `detekt-baseline.xml` so existing code passes.
- [ ] CI runs `gradlew detekt`; failures block the build.
- [ ] A short note in CONTRIBUTING on regenerating the baseline.

## Technical notes

- **Risk:** the build cannot be run locally in the sandbox (`dl.google.com`
  blocked), so a fresh detekt config will surface violations only in CI. Land the
  plugin + baseline in one PR (baseline absorbs current findings), then tighten
  rules incrementally so each CI round-trip is small.
- Prefer `buildUponDefaultConfig = true`; enable `formatting` only if it does not
  duplicate ktlint.
