# ticket-046: Condition tree serialization (format v2)

- **Milestone:** M4
- **Priority:** P1
- **Status:** Backlog
- **Dependencies:** ticket-033, ticket-022

## Description

The `Condition` sealed class (AND / OR / NOT / Pattern) is wired into the engine but marked
`@Transient` in `GestureMacro` — conditions are not persisted yet. This ticket bumps the macro
format to v2 to serialize them, keeping full backward compatibility (v1 macros without conditions
load fine; v2 macros with conditions fail closed on old readers).

## Acceptance criteria

- [ ] `Condition` hierarchy annotated with `@Serializable` and stable `@SerialName` values:
  `"and"`, `"or"`, `"not"`, `"pattern"`.
- [ ] `GestureMacro.condition` field changed from `@Transient` to a proper serialized optional
  (`condition: Condition? = null`).
- [ ] `SUPPORTED_VERSION` bumped from 1 to 2 in `MacroCodec`; v1 documents decoded with
  `condition = null`.
- [ ] Golden fixture `condition-and-or-v2.json` added and covered by `GoldenRoundTripTest`.
- [ ] Schema file `schema/gesture-macro-v2.json` updated with `condition` property (oneOf for
  pattern/and/or/not nodes, recursive $ref).
- [ ] Fuzz harness updated to generate condition trees.

## Technical notes

- kotlinx.serialization supports `sealed class` polymorphism via `@Serializable` +
  `@SerialName` on each subclass. Register the subclasses in a `SerializersModule` attached to
  the `Json` instance in `MacroCodec`.
- `GesturePattern` enum already has `@Serializable` so the `Pattern` leaf is straightforward.
- The recursive `And`/`Or` children list needs a `@Serializable(with = ...)` or inline
  `ListSerializer`; plain `@Serializable` on the sealed class covers it automatically.
