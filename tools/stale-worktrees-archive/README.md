# Stale worktrees and branches archive (2026-10-21)

Purpose: keep what the stale-branch cleanup of 2026-10-21 removed, so that any of it can be restored. Nothing here is attached to a gate or a Gradle task.

Batch: housekeeping (no documentation batch). Code guide: no change.

## What was removed

- 33 worktrees (Claude, Codex and agent worktrees) whose HEAD was already in `main` or whose branch was deleted below.
- About 70 local branches already merged into `main` (deleted with `git branch -d`; their commits are in `main`).
- 36 unmerged branches (deleted with `git branch -D`), listed with their tip sha in `deleted-branches.txt`:
  - `lineage`: the 0.7.0-era phase-ports / jdk-determinism branches. 0.7.0 landed as the squash `1dba69b3`; their unique content was mostly force-added logs and captures under `tools/phase-ports-probes/` and `tools/jdk-determinism/`.
  - `picked`: small pre-rename branches the owner chose to drop (wall-probes, v4-r1/r6, fluid-regression-rerecord, minecraft-mcp-skill-c3f0f0, coolprop-multiphase-thermo-37f6b0).
- Kept: `claude/competent-franklin-43848a`, `claude/minecraft-mcp-mouse-issue-828c9a`, `codex/matlab-v3-tool`, `codex/reward-initializer-training`, `codex/v3-literature-cdu` and their worktrees. The remote branch `origin/claude/phase-ports-compressor` (tip `a0c09c9f`) was deleted from GitHub on 2026-10-21 with the owner's approval.

## The archive

`logs/stale-worktrees-logs-2026-10-21.tar.gz` holds one folder per worktree that had uncommitted work (11):

- `HEAD.txt`: path, branch, HEAD sha.
- `changes.patch`: `git diff --binary HEAD` (tracked edits, staged and unstaged).
- `untracked/`: the untracked, non-ignored files.

The code edits target the pre-rename package `com.wormzjl.createcheme` (b88f column V3 network work, pipe-thermal-loss-study `PipeHeatExchange`, agent-ad8e7b stall probe, fluid-perf-probe instrumentation).

## How to restore

- A deleted branch: `git branch <name> <sha>` with the sha from `deleted-branches.txt` (works while the objects exist; run no `git gc --prune=now`).
- A worktree's work: `git worktree add <dir> <head-sha>`, then `git apply changes.patch` and copy `untracked/` over it.

## Index record (moved from tools/INDEX.md on 2026-10-26)

Purpose: Restore kit of the 2026-10-21 stale branch and worktree cleanup: tip shas of the 36 force-deleted branches, and the diffs and untracked files of 11 dirty worktrees

Batch: housekeeping 2026-10-21

Status: Archived 2026-10-21. Index: [../INDEX.md](../INDEX.md).

### Entry points

`README.md`, `deleted-branches.txt`, `logs/stale-worktrees-logs-2026-10-21.tar.gz`

### History

- Archived 2026-10-21
