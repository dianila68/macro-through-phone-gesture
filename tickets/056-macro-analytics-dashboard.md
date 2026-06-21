# ticket-056: Macro analytics dashboard

**Track:** M4 — Platform expansion  
**Depends on:** `EngineMetrics` + `execution_log` Room table (both done)

## Context

`EngineMetrics` (added in engine maintenance session) tracks p50/p95 gesture-to-action
latency, missed gesture count, and executor failure rate via a `StateFlow`. The data
is transiently held in memory and also logged to the `execution_log` Room table.
There is no UI to surface this to the user.

## Goal

An `AnalyticsDashboardScreen` that shows per-macro execution history in a
`LazyColumn` with summary chips for latency and reliability.

## Acceptance criteria

- [ ] Accessible from the main screen via a “Stats” toolbar icon.
- [ ] Lists every macro by name; each row shows: trigger count (all time), last triggered timestamp (human-readable), p50 / p95 latency.
- [ ] Tapping a row expands to a spark-line chart of the last 20 executions (latency over time), drawn on `Canvas`.
- [ ] “Clear history” button truncates the `execution_log` table (confirmation dialog).
- [ ] Summary banner at the top: total triggers today, overall miss rate, worst-latency macro.
- [ ] Refreshes automatically via `StateFlow` collection; no manual pull-to-refresh needed.

## Implementation notes

- New file: `ui/analytics/AnalyticsDashboardScreen.kt` (Compose).
- New file: `ui/analytics/AnalyticsViewModel.kt` — queries Room `execution_log` as `Flow<List<ExecutionLogEntry>>`; combines with `GestureCaptureService.metrics`.
- Spark-line chart: 20-point polyline on a 80 × 30 dp `Canvas`; colour-coded by p95 threshold breach.
- Timestamp formatting: `DateUtils.getRelativeTimeSpanString` (API 1, no extra dep).
