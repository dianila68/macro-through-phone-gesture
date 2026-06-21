# ticket-059 — Detekt Ruleset Expansion

**Track:** Quality (follow-up to ticket-014)
**Milestone:** Maintenance
**Status:** open
**Depends on:** ticket-014

## Problem

ticket-014 addressed the initial detekt baseline but the ruleset only covers
`complexity` and `style`. Several high-value rule sets are disabled:
- `coroutines` — catches incorrect coroutine usage (GlobalScope, blocking calls)
- `naming` — enforces Kotlin naming conventions uniformly
- `performance` — flags mutable-collection-as-return-type, unnecessary object creation

## Scope

- Enable `coroutines`, `naming`, `performance` rule sets in `detekt.yml`.
- Fix or suppress all new violations with justification comments.
- Update baseline file.
- Add a CI step that fails on net-new detekt violations (ratchet pattern).

## Acceptance

- `./gradlew detekt` passes with the expanded ruleset.
- No new suppressions added without a code comment explaining why.
- CI ratchet is in place and documented in CONTRIBUTING.md.
