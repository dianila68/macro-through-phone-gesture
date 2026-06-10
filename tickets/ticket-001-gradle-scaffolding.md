# ticket-001: Gradle & Project Scaffolding

- **Milestone:** M1
- **Priority:** P0
- **Status:** Backlog
- **Dependencies:** none

## Description

Create the Android Gradle project skeleton so every subsequent ticket has a buildable base and CI runs real builds (the `Android CI` workflow currently skips its Gradle steps until `./gradlew` exists).

## Acceptance criteria

- [ ] Gradle wrapper committed; `./gradlew build` succeeds locally and in CI.
- [ ] Kotlin DSL build scripts (`build.gradle.kts`, `settings.gradle.kts`) with a **version catalog** (`gradle/libs.versions.toml`).
- [ ] Single `app` module; `minSdk 26`, `targetSdk`/`compileSdk` 35, JDK 17 toolchain, `kotlin.code.style=official`.
- [ ] Jetpack Compose enabled (Material 3, BOM-managed) with a placeholder `MainActivity` showing an empty screen — no feature code.
- [ ] kotlinx.serialization and Room dependencies declared in the catalog (not yet used).
- [ ] ktlint Gradle plugin wired in and added to the CI lint step.
- [ ] Package structure created per ARCHITECTURE.md module layout (empty packages are fine).

## Technical notes

- Latest stable AGP + Kotlin pairing; pin everything through the version catalog.
- Keep release signing config out of VCS (`keystore.properties` is already gitignored).
