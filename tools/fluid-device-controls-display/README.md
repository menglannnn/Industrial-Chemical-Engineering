# Fluid device controls/display evidence

Batch: `2026-10-23-fluid-device-controls-display`, feature code `0f208c52`, `89168361`.
Contains compressed initial/final compilation and package logs. No probe, harness, source detachment or new Gradle task was created.
Reproduce in the feature worktree with JDK 21: `gradlew.bat jar --max-workers=4`.
Owner requested no tests/automated GUI checks; fresh-world manual checklist is in the batch review.
Archive: `logs/fluid-device-controls-display-logs-2026-10-23.tar.gz`.

Integration: owner-requested fast-forward to `claude/floating-roof-tanks-81d68b` at `232b0466`; target jar passed; 10 local files preserved. `logs/integration-logs-2026-10-23.tar.gz` includes SHA-256 before/after evidence and both launch lanes. Bridge client exited cleanly; regular runClient left running, PID 49688. No tests.

## Index record (moved from tools/INDEX.md on 2026-10-26)

Purpose: Compressed compile/package evidence only; no harness

Batch: 2026-10-23-fluid-device-controls-display

Status: Archived 2026-10-23. Index: [../INDEX.md](../INDEX.md).

### Entry points

`README.md`, `logs/*-logs-2026-10-23.tar.gz`

### History

- Archived 2026-10-23 (integrated `232b0466`; target jar passed; 10 local files preserved; regular client running; no tests)
