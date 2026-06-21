# ticket-056 — Macro Analytics Dashboard

**Track:** Quality / Observability
**Milestone:** M3+
**Status:** open
**Depends on:** ticket-014 (metrics hooks)

## Problem

Users have no visibility into whether gestures are being detected, how often
macros fire, or how long dispatch takes. This makes debugging false positives
or missed gestures opaque.

## Scope

- Add a Diagnostics screen accessible from the app settings.
- Display live `EngineMetrics` from `EngineMetricsCollector.metrics` StateFlow:
  - Gestures detected (count + rate/min)
  - Macros dispatched
  - Missed gestures (no macro matched)
  - Executor failure count
  - Gesture-to-action latency p50 / p95
- Add a 30-second rolling chart of gesture events (Canvas or Vico).
- Show last execution log entries from Room.

## Acceptance
- Dashboard updates in real time while GestureCaptureService runs.
- Latency percentiles reflect actual measured timings.
- No additional permissions required.
