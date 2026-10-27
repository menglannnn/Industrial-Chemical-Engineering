# Machines and heaters, second pass: review

Batch `2026-10-15-custom-equipment-rendering` · branch `claude/custom-equipment-rendering-805e7d`, working tree on a118d219 · 2026-10-17 · owner list of 2026-10-17 (eight items). Plan: `MACHINES_HEATERS_PLAN.md` "Second pass"; research `MACHINES_SCALE_RESEARCH.md`, `METALLIC_LOOK_RESEARCH.md` (opus subagents, read-only).

## Physical expectation
- Ladder: a climber stands in the 0.7 m cage in front of the rungs from grade to the landing. Minecraft asks only the block at the feet (`isLadder`), so every cell of that zone must be a shell cell that answers yes.
- Fired heater: feed is preheated in the convection coil (in at its top, out at its bottom, against the flue gas), then crosses to the radiant coil (in under the roof, out near the floor): two services, each with an inlet and an outlet, plus fuel gas to the burners. Legs are wide-flange columns on concrete pedestals under a 0.4–0.6 m girder; braces at least 0.15 m; bays under the burners stay open (knee braces).
- Compressor (single stage): one suction header (about 0.5 b√n) feeds every suction bottle from above; one discharge header (0.7 of it) joins the discharge bottles. Frame and cylinders sit on steel rails 0.6–0.9 m deep. Power n(0.05 + 1.7 b²) MW at a 4 m/s mean piston speed: 0.45 m × 4 throws is 1.6 MW at 565 rpm, 1.2 m × 10 is 25 MW at 300 rpm, a synchronous motor about 0.3 + 1.1 (kW/rpm)^⅓ across (1.9 m and 5.1 m).
- Refinery scale: the OH2 pump stops at 0.4 m discharge (larger ones are BB1, axially split); compressor bores reach 1.2–1.25 m and 10 throws; VC heaters to D 12 m with radiant height at most 20 m; a cabin's tubes at most about 24 m per cell.
- Metallic look: every factor of a pixel (texel × tint × vertex colour × light) is at most 1, so metal reads as a sharp light–dark profile over curved faces; the sprites' 1.9 % streaks are below notice.

## What the code did, what changed
- Ladders: `EquipmentShellBlock.isLadder` answered only for columns, and the cells under a cage's first 2.2 m could be air (8 of 17 drop points at grade). `Access.Climb` is now one ladder for columns and units (rung plane, climb zone, footprint); the layout keeps every cell a climb zone reaches as a shell cell (`ShapeRasterizer.climbCells`); every shape answers.
- Heaters: one process inlet and outlet with drawn crossovers became five front nozzles (`CONVECTION_INLET/OUTLET`, `RADIANT_INLET/OUTLET`, `FUEL`); a cabin keeps its back radiant crossover. Legs: H-section on pedestal and base plate, `min(0.45, 0.28 + 0.015 size)`; VC ring girder `min(0.6, 0.3 + 0.025 D)` deep, X-braces in alternate bays, knees in the rest; cabin perimeter girder, end and every third bay braced.
- Compressor: four nozzles (each side's bottles their own header) became one suction header over the frame (inlet up) and one discharge header in front (outlet forward); the concrete block became a steel table (rails, posts, cross beams, guide beam and saddle per throw); the box motor became an octagonal stator with banded ends, top cooler, terminal box, outboard pedestal and exciter, sized by power over speed. Enclosed volumes now cover the stator, cooler hood and cylinders (a larger octagon left cells neither shell nor interior).
- Sizes: compressor bore 0.15–1.2 m, 2–10 throws, speed from piston speed (was 900 − 600 b rpm); VC D 2.5–12 m, radiant height 1.5 D to min(2.75 D, 20 m). Pump and cabin ranges unchanged (owner options 1, 2).
- Sheen: config `equipment.metallicSheen` (0–0.4, default 0) adds k(|nz|^8 + 0.5|nx|^8) to the baked shade of curved body polygons; zero per-frame cost.

## Gates and counts
- Equipment tests 53/53. Ladder gate `climbedFromShellCells` (heaters and columns); proof with the climb cells disabled: `heatersCarryWalkableDecksAndClimbableLadders` fails (`climb-proof.log`).
- Full unit suite: 1,247 tests, 1 failure: the determinism source gate on `Math.cbrt` (motor size); its re-run then caught `Math.tan` (octagon). Fixed with `DeterministicMath.cbrt` and tan(π/8) = √2 − 1; the gate with the 53 equipment tests re-run green.
- Quads (`MachineCounts`): compressor 0.8 × 6 10.9k → 13.2k, 1.2 × 10 23.2k; VC 10 × 20 29.0k, 12 × 20 33.7k; cabin 12 × 40 34.8k → 37.6k; layout 122 ms or less per unit.

## In game (bridge lane, fresh worlds)
- Ladder drop tests (survival, healed before each, a point 0.3 m out of the rungs against a control 1.5 m further out): VC 4 × 9 grade ladder 20 health against 19; VC 12 × 20 deck ladder 20 (slid to the hearth deck) against 13; cabin ladder 20 against 15.
- Datasheets: the VC lists convection inlet and outlet, radiant inlet and outlet (necks up to 0.6 m) and fuel gas (0.3 m), all facing north; the 0.8 × 6 compressor 43,429 m³/h at 400 rpm, steel table 3 m, suction facing up (1.0 m), discharge facing north (0.72 m).
- Shots (`p-*` sheen 0, `s-*` 0.2; batch copies 115–125): headers, table and motor from 0.15 to 1.2 m bore; H legs on pedestals with X-braces and knees; heater nozzles read as short stubs at their true size (0.1–0.15 m on a 6 MW heater).
- Sheen 0.2 (`ShotDiff`): the VC shell band facing the camera +25 % (123 → 151), shell mean +11 %; suction bottles +3.5 %; the octagonal motor unchanged (flat faces).

## Owner propositions
1. Pumps above 0.4 m discharge: a BB1 (axially split, between bearings, side suction and discharge, 0.3–1.2 m) as a second pump form.
2. Cabins over about 24 m: a twin-cell form (two radiant cells, one convection section) instead of one 40 m cell.
3. Sheen: choose 0 or about 0.2 (shots `s-*`); optionally stronger sprite streaks; LabPBR `_s`/`_n` maps as an optional pack for Iris users.
4. Heater legs fireproofed (concrete casing up to the floor), as refineries do over the fire zone.
5. Still open from the first pass: a compressor deck, a stack damper platform, a cabin end walkway, the VC "Radiant height" label.

## Code guide and tools
- Code guide: the equipment chapter is refreshed at the merge (roles, `Access.Climb`, sizes, sheen config); no merge here.
- Tools: `tools/custom-equipment-rendering-poc/machines-check-2/` (scene, views, drop tests, `DropPoints.java`, `Leaks.java`, `MachineCounts.java`, `ShotDiff.java`, `Pair.java`, shots, `logs/machines-check-2-2026-10-17.tar.gz`).
