# Session Progress

## Plan
- [ ] Task 1: Design dynamic `jobs.json` schema and validation model [dependency: none]
- [ ] Task 2: Replace enum-backed jobs catalog/state/store with string-keyed runtime model and migration path [dependency: Task 1]
- [ ] Task 3: Decouple jobs from economy-only events and add configurable boost resolution [dependency: Task 2]
- [ ] Task 4: Add dynamic commands plus server-authoritative catalog/status sync [dependency: Task 3]
- [ ] Task 5: Refactor client HUD and `/otter` jobs UI to render live job metadata [dependency: Task 4]
- [ ] Task 6: Update docs/status surfaces and verify full flow [dependency: Task 5]

## Current Status
Last updated: 2026-05-14 12:49
Working on: Task 1 - designing dynamic jobs schema and service refactor path
Next: Rewrite `JobsConfig`/`JobsConfigLoader`, then move player state/store off enum keys

## Failed Attempts
- None yet.

## Completed Work
- 2026-05-14 12:45: Gathered plan details, current jobs architecture, client assumptions, and docs parity surfaces.
