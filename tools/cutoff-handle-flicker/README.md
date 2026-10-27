# Cutoff handle-tip geometry verification

Batch documentation/2026-10-24-cutoff-handle-flicker. Implementation 6f2f91e9, feature branch codex/cutoff-handle-flicker; integration pending.

- HandleTipAudit.java sweeps actual product polygons across OD codes 4..64, all six facings, open/closed and installed/item variants. It reproduces FLANGE/GUARD/BOLT tip faces on the control geometry and requires only BOLT faces on the repair; checks exact cap area and unchanged exterior bounds.
- FlickeringShutoffValveGeometry.java is git show 4ad6dc97:src/main/java/com/wormzjl/icheme/science/equipment/ShutoffValveGeometry.java with only its class/constructor renamed. It is a historical control, not product code or a compatibility fixture.
- HandleTipRender.java is adapted from tools/pressure-valve-accessory: actual mesh, flat materials, per-pixel depth. It renders the paired tip closeup and an 18-case orientation/diameter/open-closed sheet into the batch renders/ folder.
- Run after building the owned feature worktree: & 'D:/Minecraft/Modding/1.21/CreateChemE/tools/cutoff-handle-flicker/run.ps1'. Optional -SourceRoot chooses another compiled checkout. Pinned JDK 21, no extra libraries/Gradle task required.
- build.log: compileJava jar pass. sweep.log: 1464 control conflicts, zero repaired conflicts, exact bounds and cap area within 3.28e-15 relative error. renders.log records all 20 software scenes. No tests or game/GUI checks; the existing client remains on integration 4ad6dc97.
- logs/cutoff-handle-flicker-logs-2026-10-24.tar.gz retains logs, source diff and tools. No tracked harness/instrumentation was detached, so no removing commit/reattach patch applies.

## Index record (moved from tools/INDEX.md on 2026-10-26)

Purpose: Actual-mesh tip layers/areas/bounds sweep and before/after software renders

Batch: 2026-10-24-cutoff-handle-flicker

Status: Archived 2026-10-24. Index: [../INDEX.md](../INDEX.md).

### Entry points

`README.md`, `run.ps1`, `HandleTipAudit.java`, `HandleTipRender.java`, `logs/`

### History

- Archived 2026-10-24 (feature `6f2f91e9`; build and 1464 cases pass; no game input)
