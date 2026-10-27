# Constructor ghost under shaders, pipe spacing: review

Batch `2026-10-15-custom-equipment-rendering` · branch `claude/custom-equipment-rendering-805e7d`, working tree on a118d219 (third pass not committed) · 2026-10-17 · owner reports after the shader client: "the structure preview was broken"; "Piping connection should have 1 block spacing between them to prevent misconnection, this issue is especially severe with vertical furnace".

## Expectation
- Ghost: a design's preview is a translucent overlay in the world; it must draw on every pipeline the mod supports (vanilla, Sodium, Iris packs). A deferred shader pack draws only geometry routed through a program it has for that kind of draw.
- Pipes: Minecraft pipes join any pipe in a block that shares a face. In a plant, neighbouring nozzles keep at least a flange and a spool of clearance; in game that is one free block between the blocks where two nozzles' pipes stand (none touching by a face, an edge or a corner).

## What the code did, what changed
- Ghost: `EquipmentGhost` drew with the chunk type `RenderType.translucent()` from `RenderLevelStageEvent.AFTER_PARTICLES`. Under Iris with Complementary Reimagined the ghost was invisible (lane shot), under Sodium alone it drew. Iris routes that type to its terrain programs, which take chunk geometry only. It now draws translucent entity geometry on the block atlas (`RenderType.entityTranslucentCull`, overlay set), as Catnip draws its outlines at the same stage.
- Pipes, measured on every unit size (shortest, middle, longest), 40 drums and 5 columns (`PortSpacing`): 110 pairs of touching pipe blocks, 94 by a face, 16 by an edge. Vertical heater 72 (all 60 shapes: convection outlet one row under the inlet; 12 also the radiant inlet one row under and beside the outlet; 2.0 m convection section on D 2.5 m), cabin 15 (convection pair), exchanger 10 (channel and shell nozzles on neighbouring rows), kettle 3 (channel outlet and feed), ejector 4 (motive and suction at an edge), columns 6 (steam and vapour return at the bottom of the crude test column; draws and pumparounds on neighbouring trays of the crowded design below). Pump, compressor, air cooler and default drums: none.
- One rule, `EquipmentShape.Nozzle.pipeBlock()` and `crowds(other)`, placed where each kind lays out its nozzles:
  - Drums: a side connection that crowds another nozzle is a clash (refused, shown on the constructor; the default placement already retries until clash-free).
  - Columns: `ColumnStructure.side` takes the nearest row whose pipe crowds none (it already moved nozzles off overlapping flanges).
  - Heaters: the convection section at least tall enough for its inlet two rows over its outlet (`yConvection + 2.2`; VC D 2.5 m: 2.7 m, the unit 1 m taller); the VC radiant inlet at most two rows under the convection outlet.
  - Exchanger and kettle: shell nozzles two rows at least behind the channel's; saddles a fifth in from the shell ends, moved off the pipe blocks under the shell (`saddleAt`, replacing the "front saddle behind the outlet" rule that could stack both saddles on short shells).
  - Ejector: the motive stub's flange two rows over the suction.
- After: 0 touching pairs on all 293 shapes. Column rows that moved: crude test column steam −1; a crowded design (draws 8, 9, 20, 21, pumparounds 11→10, 23→22) draws ±1, returns +1, steam +3; all nozzles kept.

## Gates
- `ProcessUnitTest.pipesApart(shape)` on the real port blocks, in `everyNozzleLeavesAFreeBlockForItsPipe` (every unit size; it replaces the same-block check), `ColumnStructureTest` (test columns and the crowded design, 14 nozzles kept) and `ShapeRasterizerTest.nozzlesEndOnBlockFacesWithRoomForAPipe` (drums, columns); drum clash on neighbouring rows, none two rows apart.
- Equipment tests and determinism source gate 55/55. Proofs: with `crowds` out of `clashes()` `sideConnectionsAreValidatedAndClash` fails (`drum-proof.log`); the old layouts give the 110 pairs above (same check, `pairs-before.txt`).

## In game
- Autopilot lanes (camera at a new constructor): ghost invisible under Iris before, visible after; visible under Sodium before and after. Bridge lane (vanilla, fresh world): ghost visible; VC 2.5 × 4 and 4 × 9, exchanger, kettle, ejector and cabin build; shots in `machines-check-4/shots/`.

## Owner propositions
1. A very short, crowded column (1 m, 8 trays, four draws and a pumparound) cannot place every side nozzle and drops some silently (six before this change, seven after). Proposed: the constructor's status line names the nozzles with no room, and Build refuses until the design changes. Owner 2026-10-17: "Don't do anything with column for now, I have other plans"; not done.
2. Every new unit layout passes `pipesApart`; the rule is in the tests, not a per-unit list. Owner 2026-10-17: keep the rule in mind when building new models.

## Code guide and tools
- Code guide: the equipment chapter at the merge (pipe spacing rule, `saddleAt`, ghost render type); no merge here.
- Tools: `tools/custom-equipment-rendering-poc/machines-check-4/` (scene, views, shots, `PortSpacing`, `PortCells`, `ColumnRoles`, `Supports`, ghost lane camera), `logs/machines-check-4-2026-10-17.tar.gz`.
