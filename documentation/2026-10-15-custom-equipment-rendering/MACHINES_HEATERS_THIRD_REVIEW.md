# Machines and heaters, third pass: review

Batch `2026-10-15-custom-equipment-rendering` · branch `claude/custom-equipment-rendering-805e7d`, working tree on a118d219 · 2026-10-17 · owner answers to the second review (BB1 ok, twin cell ok, sheen 0.2 after shader and optimisation-mod checks, fireproofing yes) and two reports (max compressor build error, box furnace walkway end hitbox). Plan: `MACHINES_HEATERS_PLAN.md` "Third pass".

## Physical expectation
- Walkway end: a closed end is a fence a walker cannot pass. Minecraft's collision is the 1/8-block voxels whose centres lie in the solid, so any barrier thinner than 0.125 m can vanish; the end fence must be one voxel thick at least.
- Fireproofing: refinery practice cases heater legs in about 50 mm of concrete over the H-section from the pedestal to the radiant floor (fire zone).
- Twin cell: past about 24 m a cabin's tubes are too long; two radiant cells side by side, each half the length, share one convection section over an aisle; their coils are fed and joined by front manifolds; duty is the sum of both cells.
- BB1: above 0.4 m discharge, pumps are between bearings and axially split, double suction, nozzles side by side under the shaft; 0.75 m discharge has a 0.95 m suction, the largest the mod's 1 m pipes connect.

## What the code did, what changed
- Walkway ends: the closed-end fence was a 0.1 m slab and fell between voxel centres (box furnace hearth deck: 0 voxels), so it rendered but did not collide. Closed ends now use the rails' 0.2 m band (`Access.RAIL_BAND`) on line and arc decks; the arc fence sits inside the band.
- Max compressor: not a code fault. The owner's world held a first-pass compressor; the constructor refuses occupied cells (no save compatibility). The 1.2 m × 10 build succeeds on a fresh world.
- Fireproofing: `casing(depth) = max(0.45, depth + 0.1)` concrete casing on a 0.3 m pedestal, VC and cabin legs.
- Twin cell: `MAX_CABIN_CELL` 24 m, max length 48 m, `AISLE` 1.5 m; throat walls straight to the convection section, eave walls sloped, walkways on the outer walls, manifolds across the aisle front; `heaterDuty` counts both cells; datasheet line "Twin cell: two radiant sections …".
- BB1: `splitPump()` for discharge 0.45–0.75 m (`OH2_MAX` 0.4); `largeMotor` shared with the compressor (induction: no pedestal or exciter).
- Sheen default 0.2 (`equipment.metallicSheen`).
- Wording: the BB1 doc and the cabin hint named sides from the unit's own view; seen from the constructor +x is left (as the columns' "reflux to your right" at −x). Texts corrected; no nozzle moved.

## Gates and counts
- Equipment tests and the determinism source gate: 55 tests green. New gate `stoppedAtClosedEnds` (every closed end of every heater deck fills voxels); with 0.1 m fences it fails on the 4 × 6 cabin hearth deck (`fence-proof.log`).
- Full unit suite: 1,247 tests, 1 failure, `RegroupedCrudeTest` (crude column, wall-clock deadline 45 s; 55 s alone). The CPU was at 100 % from 16 external Java jobs (`D:/tmp_scripts/snow`) and another worktree's campaign; this branch changes no column code, and the test passed in the second pass's suite. Re-run on a quiet machine is open.
- Quads (`MachineCounts`): BB1 0.45 2.8k, 0.75 3.5k; twin 4 × 25 13.7k, 12 × 48 42.6k (6,056 cells); 12 × 40 as a twin 36.4k (single cell 37.6k); VC 12 × 20 33.2k.

## In game (bridge lane, fresh world, sheen 0.2)
- Fence drops, survival, 7 closed ends (cabin 6 × 12 hearth, middle and convection decks, twin 4 × 25 hearth and convection): all land on the fence top (−55.5, −51.5, −46.5, −49.5), 1.375 m above the decks they fell to before.
- Constructor, 1.2 m × 10 throws: "Built a Reciprocating compressor: 1027 cells". Datasheets: BB1 0.75 "Axially split … (API 610 BB1)", 4,771 m³/h, 1,857 kW, suction west; twin "two radiant sections 4.0 m wide, 12.5 m long", 18.8 MW.
- Shots: overview, pumps, BB1, VC and cabin legs, twin small, big, top and aisle, max compressor.

## Shaders and optimisation mods (autopilot lanes, sheen 0.2)
- Sodium 0.8.12 + Sodium Extra + EntityCulling + FerriteCore + ModernFix; Iris 1.8.14 with Complementary Reimagined and Unbound r5.7.1, Bliss, MakeUp UltraFast 9.4b, Photon: all opened the save, rendered and quit cleanly; equipment shading, sheen band, rails and shadows correct in every pack. The owner then said not every shader needs testing; the batch had one lane left and finished.
- The bridge (0.4.7-osguard) crashed every Sodium lane: it guesses the GLFW handle as the first long-valued window method, which a Sodium mixin answers. `src/mcpCompat` `WindowHandleMixin` hands it the real handle. `prepareMcpControlMod` no longer calls `project.delete` at execution; the lanes ran with `--no-configuration-cache` after an unexplained cache report (a forced run with the cache then passed).
- FPS pair not run: the machine never went quiet (external jobs). The sheen is a vertex colour baked once, so quads, format and draw calls are unchanged per frame.
- Rule slip: the gate run overlapped the last lane's client start (a stopped batch's child kept running); the gates are correctness tests and were up to date on the re-check.

## Owner propositions
1. Under shader packs our baked directional shade multiplies with the pack's own lighting (MakeUp's north faces read dark). Option: bake without the directional shade (sheen kept) while an Iris pack is active; one lane with a vanilla reference wall would show the size first.
2. `RegroupedCrudeTest` measures a wall-clock deadline; a solver-counter budget would not fail under machine load (not this batch's code).
3. Still open: compressor deck, stack damper platform, cabin end walkway, the VC "Radiant height" label.

## Code guide and tools
- Code guide: the equipment chapter is refreshed at the merge (BB1, twin cell, fireproofing, rail band, sheen default); no merge here.
- Tools: `tools/custom-equipment-rendering-poc/machines-check-3/` (scene, views, fence drops, `WalkwayEnds`, `EndDebug`, `BoxDebug`, `FenceDrops`, `MachineCounts`, `shader-lanes/`, shots), `logs/machines-check-3-2026-10-17.tar.gz`.
