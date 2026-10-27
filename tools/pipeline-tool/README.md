# Pipeline tool verification evidence

Purpose: preserve the single component-test result for batch `2026-10-23-pipeline-tool`.
No investigation harness or detached production code was created.

From the pipeline-tool worktree, with no dev client running:

```powershell
.\gradlew.bat test --tests com.wormzjl.icheme.world.item.PipelineToolSettingsTest --max-workers=4
```

`logs/component-test-logs-2026-10-23.tar.gz` contains the Gradle XML and HTML test results:
one test, zero failures/errors/skips, code commit `d30694ed`.
Inspect with `tar -tzf logs/component-test-logs-2026-10-23.tar.gz` or extract into a temporary folder.
The owner performs the in-game verification.

Placement follow-up `459eda31`: `logs/placement-build-logs-2026-10-23.tar.gz` records a successful `gradlew.bat jar --max-workers=4` build. No additional tests or automated in-game checks were run. `logs/client-startup-logs-2026-10-23.tar.gz` is the earlier owner-requested client launch, before the placement follow-up.

Target integration `b3bb813e`: `logs/merge-gates-logs-2026-10-23.tar.gz` records `gradlew.bat test slowGates --max-workers=4` (1,357 quick tests pass; 184/185 slow tests pass). `logs/merge-baseline-logs-2026-10-23.tar.gz` records the sole failure reproduced with `slowGates --tests com.wormzjl.icheme.science.equipment.ProcessUnitTest.everyNozzleNeckAtItsLargestStandsClear --max-workers=4` at the target's pre-merge commit `1a806141`. `logs/merge-preservation-logs-2026-10-23.tar.gz` records the hashes of the ten target files left uncommitted and byte-for-byte unchanged during the fast-forward.
`logs/merged-client-startup-logs-2026-10-23.tar.gz` records the owner-requested runClient launch from the floating-roof-tanks target worktree after the merge, including its local equipment edits; no automated in-game checks.

## Index record (moved from tools/INDEX.md on 2026-10-26)

Purpose: Pipeline tool component, placement, client-startup and target-integration verification evidence; no standalone harness or detached code

Batch: 2026-10-23-pipeline-tool

Status: Archived 2026-10-23. Index: [../INDEX.md](../INDEX.md).

### Entry points

`README.md`, `logs/*-logs-2026-10-23.tar.gz`

### History

- Archived 2026-10-23 (merge `b3bb813e`; one slow gate fails identically on target baseline)
