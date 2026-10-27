# Pipe end hitbox review

**Date:** 2026-10-24  
**Branch:** `claude/floating-roof-tanks-81d68b` (worktree `rename-ichem-mod-30e50c`)  
**Status:** implemented in `9ab5f6cf`; development client loaded a world.

## Expected behaviour

Every pipe block's hitbox follows its drawn run. A pipe with one connected face is a straight dead-end section: it crosses the whole block and ends in a blind flange at the opposite face. This applies to both ends of a horizontal run of two or more blocks.

## What the code did instead

`LineGeometry.pipe` draws a one-connection pipe through both axial faces. The voxel builder added only the connected arm and the centre cube. The opposite half had no hitbox. The same mismatch existed on vertical dead ends.

## Implemented correction

`FluidDeviceBlock.getShape` adds the opposite axial arm when the connection mask has exactly one face, matching `LineGeometry.pipe`. The existing private `pipeShape` builder remains in place. Empty, straight, elbow, tee and cross masks retain their previous shapes. Diameter handling and the shape cache are unchanged. No helper class or test scaffolding remains.

## Verification

- `compileJava`: **pass** on the final direct change.
- `git diff --check`: **pass**.
- No dev-client check: no rendering or GUI change.

Existing unrelated equipment edits and `TmpDiagTest` were left untouched. No save, protocol, physics or tooling change. Code-guide update remains a merge-time task.
