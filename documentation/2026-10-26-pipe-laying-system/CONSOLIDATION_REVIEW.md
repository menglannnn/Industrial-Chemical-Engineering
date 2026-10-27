# Code consolidation review (batch 2026-10-26-pipe-laying-system)

Date 2026-10-27. Branch `claude/pipe-laying-system-092ac8`, on `9b5f1fcb` (base `main` `600549d9`). Owner request: "consolidate and prepare for merge". Scope: dead code, duplicated helpers, the anchor and placement logic client and server both repeated, history comments, small test classes merged; no change to formats, protocols, ids, physics, route results, GUI layout or drawn geometry. Tooling cleanup, code guide, CHANGELOG and final gates: `MERGE_REVIEW.md`.

## Counts

| | `9b5f1fcb` | `71ad53ef` |
|---|---|---|
| `src/main/java` files | 333 | 333 |
| `src/test/java` files | 331 | 327 (-4) |
| `src/fluidGameTest` files | 18 | 18 |
| batch diff against `600549d9` (`src/`) | 64 files, +7029 / -174 | 61 files, +6985 / -177 |
| this consolidation (`src/`) | | 22 files, +709 / -756 (`src/main` +194 / -184) |
| `test`: classes / tests / failures | 274 / 1520 / 0 | 270 / 1520 / 0 |
| fluid GameTests | 26 / 26 | 26 / 26 |

## Commits

1. `32de136e` Dead code: `PipeRoute.axis(int)` (a copy of `LineGeometry.axis`), the `valid` wrapper (`finishing` uses `leaving`), `PipeAnchor.resolve(Level, BlockPos, Direction, double)`, the one-paint `PipeRackBlockEntity.Lane` constructor, `FluidDeviceBlockEntity.SPOOL` (alias of `LineGeometry.SPOOL`), `EquipmentShellModel.paintedLine` (folded into `paintedLines`), a second size standardization in `PipeAnchor.aimed`, two unused imports. One `painted(paint, secondary)` mask replaces the copies in `FluidDeviceModel` and `PipeRackModel`; `PipeRoute.ends` and `LineGeometry.flange` private.
2. `945cbf51` Client and server share `PipeAnchor.refuses(start, end, outside)`, `PipeRoute.Placement`/`placement()` (joints, facings, arms), `PipeLaying.refusal(...)` and `describe(anchor)` (the client's `name()` and `describe()` go) and `PipeAnchor.block(Cell)`; the client's `Plan` holds one `Placement`, not three arrays.
3. `cd672ac4` Rack lane paints read and written by `FluidDeviceBlockEntity.readPaint`/`readSecondary` (log-label overloads) and the new `writePaints`, the rack's copies gone; `PipeRackBlockEntity.redraw()` replaces the refresh-and-send written out in `PipeRackBlock` and `FluidDeviceBlockEntity`; `refreshDrawn`, `faces`, `position` private.
4. `f1b3e408` Comments: the `rack-3` and schema 2 docs drop the RD9/`rack-2` history (format facts kept); `PhysicalFluidTopology.across` no longer names what it replaced; an orphaned test javadoc moved.
5. `71ad53ef` Tests: `RackAimTest` (4) and `RackGroupTest` (14, its own `Cell` now `PipeRoute.Cell`) into `PipeRackGeometryTest` (12, now 30); `PipeJointsTest` (9) into `PhysicalFluidTopologyTest` (20, now 29); `PipeRackCompatTest` (6) into `PipeLineCompatTest` (2, now 8). Method names 1629 before and after; renamed: the two `everyCorpusImageReadsToWhatItWasSavedWith` to `everyPipeCorpusImageReadsToWhatItWasSavedWith` and `everyRackCorpusImageReadsToWhatItWasSavedWith`.

## Formats and protocols

No change: rack copy `rack-3` (`Lanes`, `Identity`, `Option`, `Width`, `Arms`, `Joints`, `Bore`, `Paint`, `Secondary`; update tag `Joined`, `Frame`), pipe line facts (`Joints`, `Paint`, `Secondary`), facts `SCHEMA=2`, `fluid-14`, `pipeline-tool-2` (`network/`, `src/main/resources`, `src/test/resources` untouched). Only behaviour-visible change: a damaged rack lane paint is logged once (`pipe_rack repair=paint`), not twice.

## Geometry and route digest

Probe `ConsolidationDigestTest.java` (run from `src/test`, never committed): SHA-256 over rack frames, lanes, steelwork, spools, `fits`, `aim`, `pick`, `placed`, `steel`; `LineGeometry.polygons` (pipes, devices, transitions); `PipeRoute.search` with `check`, `blocked`, `laidJoints`, `facings`, `arms`, `elbows` on aimed runs, corners, tees, stacked runs, obstacles and one-cell lines: 12 sections, 1,348,749 items, 13,424 routes. All 12 sections bit-identical at `9b5f1fcb` and `71ad53ef` (only the timings differ); overall `bf89be369ae600a5`, route.search `dda2826346b6a2ef`. Tool folder: `tools/pipe-laying-system/consolidation/` (the probe, `before.txt`, `after.txt`), logs in its `logs/`; nothing detached from a tracked path.

## Gates

Between commits the affected classes only, all green. After the last: `./gradlew test` 1 min 15 s, 270 classes, 1520/1520; `runFluidGameTestServer` (run id `consolidation1`, folder deleted) 26/26. `git ls-files -i -c --exclude-standard` empty. The merge gates (`test slowGates`, GameTests): `MERGE_REVIEW.md`. A dev client of worktree `distillation-column-framework-922ec9` ran during these runs.

## Found, not fixed

- `PipeRackBlockEntity.refresh` sends drawn-only changes with `setChanged()` (`sync()`); `redraw()` sends without it, as before. Drawn state is not saved. Kept: removing it changes when chunks save.
- `EventPathWorld` (older than the batch) imports the unused `BoundedCpuSolveService`.

## Owner proposals (not done)

1. Used only by tests or the harness `tools/pipe-laying-renders/`, kept: `RackGeometry.option(position, level)` (tests, harness), `RackGeometry.largest(width)`, `PipeRoute.Space.of`, `PipeRoute.End(Cell, int)`, `PipeAnchor.Click(BlockPos, Direction)` (GameTests), `PipeRackBlockEntity.Lane.painted(int)`. Read outside `RackGeometry` only by the harness: `setback`, `spoolAt`; by the harness and tests: `steelwork`, `first`, `path`.
2. Make `RackGeometry.NONE` and `PICK_TOLERANCE` private (used only inside `RackGeometry`).
3. `PipelineToolScreen.colourName`, `EquipmentConstructorScreen.colourName` and `PipeLayingToolItem.paintName` name a colour nearly alike; one helper would remove one or two. GUI text code, left.
4. Rename `RackGeometry.Rack.corner` (it holds every shape) to `shape`, `PipeRackBlockEntity.BARE` to `BARE_STEEL`.
5. Fold `FluidNetwork.answer` (one use, dismantle path): wire code, left.
6. Drop the `setChanged()` named under Found.
