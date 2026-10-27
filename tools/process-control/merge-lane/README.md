# merge-lane

Purpose: the final lane and the comparison scripts of the process-control merge (0.14.1). Batch: `documentation/2026-10-26-process-control/` (MERGE_REVIEW.md). Branch: `claude/process-control-merge`, tip `0e697177`. Nothing here was ever in tracked code.

## Contents
- `lane.ps1`: the final lane. It runs `test slowGates --rerun-tasks`, copies the gate XML to `final-results/` before the campaign's `test` run overwrites it, then runs chain-100 exact, the fluid GameTests (run id `process-control-merge-final`) and the 1000-seed campaign through `../quick.ps1`.
  - It is `../qualify.ps1` Final with its output beside the script, and it lets gradlew's stderr through (Windows PowerShell 5.1; see `../README.md`).
- `printed.pl`: prints every captured stdout line of JUnit XML files as `SimpleClass | line`. It reproduces the codex lane's `final-printed.txt` exactly, once carriage returns are stripped.
- `maxrel.pl <Class> <diff>`: pairs the removed and added lines of one class in a diff of printed lines, and prints the largest relative move of the numbers above 1e-6.
- `devices.pl <files>`: lists every `new Device(` / `new PhysicalFluidTopology.Device(` call with its argument count. This audit found the short-constructor rebuild that reset a laid pipe's joints.

## How to run
Run it alone: no dev client, no other Gradle run.
1. Copy `lane.ps1`, `../quick.ps1`, `../cold/` and `../bundled/` into an indexed `tmp/<name>/` folder of the worktree.
2. Run `powershell -File tmp/<name>/lane.ps1 -Workspace <worktree>`.
3. Run `perl printed.pl tmp/<name>/final-results/TEST-*.xml > printed.txt`.
4. Compare the junction lines (`LIQUID_JUNCTION|MIXED_GAS_TRANSIENT|MIXED_GAS_COST`) after removing `ms=`, `bytes=` and `allocatedMB=` fields.

## History
- 2026-10-27: created by Claude (Opus 5.5) for the merge lane. Logs: `../logs/merge-lane-logs-2026-10-27.tar.gz` (lane run, gate logs and XML, chain-100, GameTests, campaign reports, printed lines and their diffs against the codex lane, focused test logs).
