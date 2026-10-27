# Pump inlet adapter review

**Date:** 2026-10-24  
**Branch:** `claude/floating-roof-tanks-81d68b` (worktree `rename-ichem-mod-30e50c`)  
**Status:** implemented locally; compilation passes, in-game visual check pending.

## Expected behaviour

Both OH2 pump ports should form closed castings from the connected pipe's neck and flange into the default-size pump branches. A smaller connected pipe needs a visible reducer or closing shoulder, not a narrow neck entering an open large-diameter casting.

## What the code did instead

Pipe-size synchronization works: `EquipmentStructure.refreshNozzles` reads the adjacent pipe bore and supplies its outside diameter to the shell cells. `ShapeRasterizer.Geometry.nozzle` then draws the detachable neck at `od / 2` and its flange at the connected pipe's size.

The fixed OH2 castings do not receive that connected diameter. `ProcessUnit.pump` draws its suction bell from the default `s = pumpSuction(d)`, with front radius `.5 * s + .04`. The shared nozzle starts at `zf + ts + .01`, so a smaller connected pipe produces a narrow neck against that larger fixed bell. `Mesh.frustum` supplies the bell's side faces but no front annulus around the neck, leaving the abrupt, poorly closed transition in the screenshot.

The discharge has the same defect. Its rectangular-to-round transition ends at `neck` with fixed radius `.5 * d + .025`; the pipe-sized discharge nozzle begins at `neck - .01`. The 0.01 m axial overlap does not close the radial gap when the connected OD is smaller, and `transition` draws only side faces. A larger connected neck instead intersects the fixed casting.

Commit `7cb42268` removed the dedicated suction flange and moved the shared nozzle base to the bell front, exposing this independent-sizing defect. No hydraulic or nozzle-synchronization fault was found.

## Implemented correction

`ProcessUnit.pump` records the fixed suction and discharge casting radii against their nozzles. `ShapeRasterizer` blends each root into the connected pipe's neck over at most 0.12 m and adds a closing annulus where the casting is larger. The nozzle's solid shape follows the same reducer. Both connection planes, pipe-sized flanges and outer necks are unchanged; hydraulics and nozzle synchronization are untouched.

## Verification

- `compileJava`: **pass** with Java 21.
- `git diff --check`: **pass**.
- `runClient`: **pass** on committed `b52732c9`; mod loading and resource initialization completed, client left running.
- No test suite: the development client is running. In-game visual verification remains pending.

Existing unrelated equipment edits and `TmpDiagTest` remain untouched. Code-guide update remains a merge-time task.
