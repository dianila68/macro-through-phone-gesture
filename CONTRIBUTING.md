# Contributing

This project uses a **file-based ticket system** (the [`tickets/`](tickets/) directory) as the single source of truth for planned work. Every change must be traceable to a ticket.

## Branch naming convention (enforced)

Branches **must** follow this pattern:

```
<type>/ticket-<NNN>-<short-kebab-description>
```

| Type | Use for |
|---|---|
| `feat/` | New functionality |
| `fix/` | Bug fixes |
| `refactor/` | Code restructuring without behavior change |
| `chore/` | Build, tooling, dependency updates |
| `docs/` | Documentation only |

Examples:

```
feat/ticket-002-foreground-service
fix/ticket-003-sensor-listener-module
docs/ticket-005-json-macro-schema
```

`<NNN>` is the zero-padded ID of an existing ticket file in `tickets/` (e.g. `ticket-002-foreground-service.md`). If no ticket covers your change, create one first (see below).

## Ticket workflow

1. **Create or claim a ticket** in `tickets/` using the next free ID: `tickets/ticket-NNN-short-name.md`. Follow the structure of the existing tickets (Milestone, Priority, Status, Description, Acceptance Criteria, Technical Notes, Dependencies).
2. **Set its Status** to `In Progress` in the same PR that starts the work.
3. **Branch** off `main` using the convention above.
4. **Open a PR** that references the ticket file. The ticket's Status moves to `Done` in the PR that completes it.

## Commit messages

Reference the ticket ID in the commit subject:

```
ticket-002: implement notification channel for foreground service
```

Keep subjects imperative and under ~72 characters; use the body for the "why".

## Pull request rules

- The PR description **must** link the ticket file it implements.
- The branch name **must** match the convention — PRs from non-conforming branches will be asked to rebranch.
- CI (`Android CI` workflow) must pass: Gradle build + lint.
- No Kotlin source without an associated ticket in an active milestone (see [ARCHITECTURE.md](docs/ARCHITECTURE.md#milestone-roadmap)).

## Code style

- Kotlin official code style (`kotlin.code.style=official`).
- ktlint will be wired into CI with the Gradle scaffolding (ticket-001); until then, format with Android Studio defaults.
- Compose: stateless composables by default, state hoisted to ViewModels.

## Static analysis

Two tools run in CI and must pass before a PR can merge:

- **ktlint** (style): `./gradlew ktlintCheck`
- **detekt** (code smells / complexity): `./gradlew detekt`

Configuration lives in `config/detekt/detekt.yml`. The baseline (`config/detekt/detekt-baseline.xml`) absorbs pre-existing findings; **never add new entries** — the baseline is a ratchet, not a permanent exemption list.

If your change introduces a finding that you believe is a false positive, suppress it inline with `@Suppress("DetektRuleName")` and add a comment explaining why. If it affects many existing call sites, update `detekt.yml` instead and get it reviewed.

**Regenerating the baseline** (only after fixing a batch of existing findings):

```bash
./gradlew detektBaseline
git add config/detekt/detekt-baseline.xml
```

## Security-sensitive areas

Changes touching the **AccessibilityService**, **Foreground Service**, or **macro import parsing** require extra scrutiny in review (they are the app's privilege and attack surface). Call this out explicitly in the PR description.
