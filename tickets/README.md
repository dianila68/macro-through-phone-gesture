# Ticket Backlog

File-based ticket system — the single source of truth for planned work. Branch names and commits are bound to ticket IDs (see [CONTRIBUTING.md](../CONTRIBUTING.md)).

- **Naming:** `ticket-NNN-short-kebab-name.md`, IDs are zero-padded and never reused.
- **Status values:** `Backlog` → `In Progress` → `Done` (or `Blocked` / `Won't Do`).
- **Priority:** `P0` (blocker) / `P1` (milestone-critical) / `P2` (nice to have).
- Each ticket lists its **Milestone** (M1–M4, see [ARCHITECTURE.md](../docs/ARCHITECTURE.md#milestone-roadmap)) and **Dependencies** (ticket IDs).
