# Pipeline tool merge review

2026-10-23 · Merge `b3bb813ec1e61259bc86c5ffc915138d4926f51a`.

- Owner requested merging the pipeline tool work into `claude/floating-roof-tanks-81d68b`.
- Parents: target `1a806141` and source `6025f4d3` (`codex/pipeline-tool`).
- Prepared the conflict-free merge on `codex/pipeline-tool-integration` in the owned worktree.
- Automatic merges retained tank-display registration, watchers, publication and open-vessel registration alongside the pipeline tool and placement inheritance.
- Ran `gradlew.bat test slowGates --max-workers=4` on the committed integrated tree, with no dev client running.
- Quick suite: 1,357 tests, zero failures/errors/skips.
- Slow suite: 185 tests, one failure: `ProcessUnitTest.everyNozzleNeckAtItsLargestStandsClear` (line 314).
- Replayed only that failing gate on the original target commit `1a806141`; it failed there too. The assertion messages match exactly.
- The merge introduces no new test failure. The existing nozzle-clearance failure remains the target branch's work; no equipment geometry was changed for this merge.
- Fast-forwarded the target checkout to the prepared merge. Nine modified equipment sources and untracked `TmpDiagTest.java` remained byte-for-byte identical (SHA-256 checked before/after).
- Verified `codex/pipeline-tool` is an ancestor of the target, the owned checkout is clean, and no tracked ignored files exist.
- Tests cover the committed integrated tree; pre-existing uncommitted target edits were excluded from that test tree and preserved separately.
- No in-game checks, solver campaigns, version bump or main merge were performed. The owner handles in-game verification.
- Feature code-guide addenda are updated to `b3bb813e` in this batch and the owned guide draft; canonical main chapters await the main merge.
- Tooling: `tools/pipeline-tool/`; no one-off tracked harness or instrumentation was introduced.
- Evidence: `logs/merge-gates-logs-2026-10-23.tar.gz`, `logs/merge-baseline-logs-2026-10-23.tar.gz`, `logs/merge-preservation-logs-2026-10-23.tar.gz` beneath that tool folder.
- Status: In progress since 2026-10-23; integrated into the requested development branch, pending main integration.
- Owner-requested runClient 2026-10-23, 10:53 SGT: launched from the target worktree at b3bb813e, including its existing local equipment edits. IChemE loaded and rendering/JEI initialized; left running for owner testing, with no automated in-game checks.
