# Security Policy

## Reporting a vulnerability

Please report vulnerabilities privately via **GitHub Security Advisories** ("Report a vulnerability" on the repo's Security tab). Do not open public issues for exploitable bugs. You can expect an acknowledgement within 7 days.

## Scope: what to look at

This app deliberately holds high-privilege capabilities. The audited surfaces, in order of impact:

1. **AccessibilityService** (`MacroAccessibilityService`) — can act inside other apps. Any path that lets a non-user-authored macro reach it is critical.
2. **Macro import parser** — the only entry point for untrusted external data (shared JSON/YAML macro files). Strict-validation bypasses, parser resource exhaustion, or the disabled-on-import rule for `accessibility` actions being circumvented are all in scope.
3. **Foreground capture service** — availability and wakelock abuse.
4. **(M4) BLE/LAN device bridge** — authentication, replay protection, flooding.

The full analysis lives in [docs/THREAT_MODEL.md](docs/THREAT_MODEL.md) (threat IDs T1–T11); referencing a threat ID in your report helps triage.

## Hard rules the code must uphold

- Imported macros containing `accessibility` actions are persisted **disabled**, regardless of the file's `enabled` flag.
- No component is exported without signature-level protection and a threat-model update.
- Sensor data never leaves the device (pre-M4: no network permission at all).
- No analytics or tracking SDKs.

## Supported versions

Pre-1.0: only the latest release/`main` receives security fixes.
