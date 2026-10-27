# Ladders and the rolling ladder review (2026-10-25)

Batch `2026-10-21-floating-roof-tanks`, branch `claude/floating-roof-tanks-81d68b`. Commits `e2619287`, `68af29c2`, `d8746d00`.

## Owner reports
- "Floating tank top model is broken, I cannot reach the platform using the moving ladder."
- "I cannot move to side platforms when climbing the ladder on the furnace as well, review other ladders."
- Later the same day: "These big structures will need a major rework later, leave them for now" (the fluidized-bed gasifier).

## Expected behaviour
A player is a 0.6 x 1.8 m box aligned with the world. Equipment collides in 1/8-block voxels that are solid at their centres, so a fence stands up to half a voxel (1/16 m) past its drawn end.

- **Walk-through ladder:** the handrail gap needs 0.6 m plus a voxel either side, 0.85 m.
- **Rolling ladder:** a box on a ramp rests on its uphill edge, and its feet reach the deck's height as that edge reaches the deck's edge.
- **Inside an open tank:** only the wall plate and the floating roof stand.

## What the code did
1. Walk-through gaps were 0.3 m each side of the ladder's middle, so a 0.6 m box met a voxel.
   - Cabin heater: grade and convection walkways (the furnace report).
   - Also the adsorber, stirred reactor, cyclone, gasifier feed floor and the kit's rest platforms.
2. A square deck's fill collided as a ring out to its corners. It blocked the ladder holes beside it (trickle bed, ebullated bed).
3. The cyclone's rest platform ended 0.22 m beside its ladder's middle, and its climb landed off its walkway's clear width.
4. A small sphere's bridge (1.1 m) left 0.625 m between its rails' bands. A player off the ladder's middle by 0.05 m could not walk on.
5. The rolling ladder held the feet on the treads at the body's middle. At the deck's edge the feet were 0.3–0.42 m of run lower, and the box ran into the edge at every pitch except level. It could not jump or step up: the ladder sets the vertical speed each tick.
6. The tank's shell collided as its whole inside (`Solid.rod`), in every cell its wall crosses or a part reaches. Under the platform the hinge's cell was an invisible solid block, and a climber's head met it at every roof level.

## Fixes
| # | Fix | Where |
|---|---|---|
| 1 | `Access.WALK_THROUGH` = (0.6 + 2/8)/2 = 0.425 m replaces every 0.3 m gap | `Access`, `ProcessUnit` (cabin heater, `climb`), adsorber, stirred reactor, gasifier |
| 2 | Fill collides out to the square only (`Solid.clip`) | `ProcessUnit.fill` |
| 3 | The rest runs past a section by its opening and a fence band; the cyclone's climb lands on the clear middle | `ProcessUnit.climb`, `CycloneDesign` |
| 4 | `BRIDGE` = GAP + 2 (RAIL_BAND + WALK_THROUGH) = 1.3 m | `SphereDesign` |
| 5 | `FloatingRoofs.tread`: the line at the box's uphill edge (`lead` = box reach along the ladder + 1/16). The climb holds until that edge leaves the foot, so the box lands on the roof without a drop | `FloatingRoofs` |
| 6 | The shell collides as a wall `NARROW_BAND` thick. In its cells the roof collides inside its radius (`FloatingRoofs.rim`, joined in `EquipmentShellBlock.getCollisionShape`, tank shells only) | `TankDesign`, `FloatingRoofs`, `EquipmentShellBlock` |
| 7 | The rolling ladder's stringers and treads lie under the walking line; the first tread is at the deck's edge (drawing only) | `TankDesign.floating` |

## Gates (new)
- `ProcessUnitTest.aPlayerClimbsEveryLadderAndGetsOffAtItsLanding` (slow gates), using `PlayerWalk`, which moves a player as vanilla 1.21 does against the voxel collision.
  - Scope: every unit size except the gasifier, 28 tanks and 12 spheres; about 10 s.
  - With the old 0.3 m gap it fails on the cabin heater.
- `FloatingRoofsTest.aPlayerWalksTheRollingLadderBetweenTheRoofAndThePlatform`: from the roof onto the platform and back, at five roof levels (landed to full).
  - With the old middle rule it stops at the deck slab's voxel.
  - With the old rod solid it stops at the hinge's cell.
- Run: equipment and block packages 71/71, `ProcessUnitTest` 22/22 (93 in total).

## Probe results (646 unit ladders, 52 column, tank and sphere ladders)
- The early probe counts mixed real defects with probe artefacts (a start inside a floor voxel, a climb past the landing), so no before count is given.
- After the fixes: 0 stuck, except:
  - gasifier 1 (a stack ladder 1.5 x 1, beside its 1 m ring deck; the kit needs `MIN_DECK` 1.45 m there);
  - the 15 m column 5 (a climber drifts off its 45° ladders that start from decks).
- Both are left to the big-structure rework.

## Not changed
- Gasifier ring decks stay 1 m wide.
- At `MIN_DECK` all its ladders passed, but the frame and bay gap would grow. That is left for the rework.

## Tools
`tools/floating-roof-tanks/ladder-probe/` (main checkout):
- the probe test with its trace helpers;
- the storage render with a pivot focus;
- before and after renders;
- `logs/ladder-probe-logs-2026-10-25.tar.gz`;
- `CellCount.java`, which finds the tank sizes that lay out a given number of cells (the "Built ..." chat line).

Code guide at merge (equipment chapter):
- `Access.WALK_THROUGH`;
- the rest platform's extension;
- the tank's wall collision and `FloatingRoofs.rim`;
- the tread rule;
- `PlayerWalk` and both gates.

## Second report: the platform at the ladder's head (commit `d8746d00`)
Owner, later the same day, with screenshots of a 7 x 4 m tank: "The broken 3d model of the floating roof tank is not fixed".
- The tank's size came from the client log ("Built a Floating-roof tank: 347 cells"); only 7 x 4 m lays out 347 cells.
- **Expected:** along its inner edge, a gauger's platform carries the ladder's opening and a rail piece with its posts on each side. Every handrail run ends at a post.
- **What the code did:** the platform's angle gave it 2 m along the stair (or the small tank's deck), and it narrows to its inner edge over the tank.
  - At 7 m the inner edge was 1.05 m, shorter than the 1.1 m opening. `Access.pieces` left the inner rail out, with its posts.
  - The rails across the stair's open end start at the inner edge with no post of their own, so they stopped in the air.
  - The 20 m tank of the first renders had 0.23 m inner pieces and looked right.
  - Affected: every stair tank under about 17 m and every ladder-access tank (4 and 5 m).
- **Fix:** the platform's angle also gives the inner edge the opening and a rail band either side, `(LADDER_OPENING + 2 RAIL_BAND) / (r - OVERHANG)` = 1.5 m (`TankDesign.span`).
  - The layout of the smaller tanks changes; placed tanks are rebuilt on load (`EquipmentRefresh`).
- **Gate:** `ShapeRasterizerTest.everyStorageSizeIsWalkedAndKeepsItsPipesApart` checks the inner rail's band either side of the opening on all 777 tank sizes. With the old angle it fails on the 4 x 4 m tank.
- **Run:**
  - `ShapeRasterizerTest` 37/37, `FloatingRoofsTest` 5/5, `EquipmentRefreshTest` 5/5, `UnitTextTest` 1/1;
  - `ProcessUnitTest` 22/22 (the player-walk ladder gates).
- Renders before and after (7 m tank) in the tool folder.

## Open for the owner
1. In-game check of the rolling ladder, the 7 m tank's platform and a furnace's walk-through.
2. The gasifier and the 15 m column ladders, in their rework.
