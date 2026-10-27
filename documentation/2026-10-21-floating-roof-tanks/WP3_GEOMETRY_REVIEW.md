# WP3 geometry and placement review (2026-10-21)

Plan sections 3.3 (tank) and 3.5 (sphere). Commit `a16977ab` on branch `claude/wonderful-banach-1f1152` (parent
`ef64bd06`), not merged or pushed. No science/fluid or runtime/fluid code was touched.

## What landed
- `Kind.TANK` and `Kind.SPHERE` (`designed()`, `reservoir()`) are in the Vessels category, on protocol `equipment-10`.
  New roles `BOTTOM` and `OVERFLOW`; new port class `OVERFLOW`.
- `TankDesign`: D 4-40 m, shell 4-24 m (default 0.6 D), V = πD²/4·(shell - 1). It has a ringwall, 2 m courses as body
  rows, a top angle, a wind-girder walkway 1.2 m below the top (pieces of at least 2 m), a gauger's platform, a sight
  glass, a wound stair, bottom nozzles at ±k and an overflow 0.5 m below the top. The roof and rolling ladder are drawn
  by the client through `ShapeRasterizer.floating(shape)`.
- `SphereDesign`: D 4-24 m on 2-8 m legs, V = πD³/6. It has n = max(4, 2·round(πD/9)) equator legs, crossed tie rods, a
  crown ring deck with a landing, two relief valves with stacks and a manway. N1 (vapour) leaves the crown upward; N2
  (liquid) runs from the bottom pole to the front edge. The stair keeps at least 0.45 m from the shell and clears the
  legs.
- `Access.WoundStair` is one kit piece, 1.4 m wide and about 40°. A common factor found by bisection ends the last tread
  at the top, and each rise is set at its own tread's height. `RADIAL_SHARE` went from 0.8 to 0.35 after the walk test
  found a 53° turn on the sphere's stair.
- `ProcessUnit` now tests solids with a bounding box over 256 cells directly instead of indexing them per cell. This
  fixes the out-of-memory failure on the 40 m shell. A ring of a full turn is drawn whole.
- World: `EquipmentInteriorBlock.OPEN` gives the tank an interior that passes light. Reservoirs register as drums do,
  and the find walk reaches across a 40 m tank. The datasheet, lang and `EquipmentRefresh` stamps cover both kinds.

## Tests
- `./gradlew test`: 1338 tests, 0 failures. `slowGates --tests ProcessUnitTest`: 19/19.
- New `ShapeRasterizerTest` checks per size: grid, held volume, ports, walk with pipes apart, decks and stairs in the
  box, the roof and ladder API; `theLargestTankLaysOut` gives the timing.
- `ProcessUnitTest.woundStairsWalked` checks tread, headroom, fences and a free 0.6 m player box at 1/8 block.
  `UnitTextTest` and `EquipmentRefreshTest` cover both kinds.

## Timing (largest tank)
The D 40 × 24 tank (box 45 × 26 × 44, 4,646 shell cells and 28,248 interior cells) builds in 152 ms on a cold JVM, run
alone. That is under the 0.5 s bound, so the grid keeps D 40 and needs no stop at 32 m.

## Renders, for the owner's approval before any in-game check
Main checkout `tools/storage-vessel-renders/renders/`: 24 PNGs (iso, front, top, iso3) of tanks 4×4, 20×12 and 40×24
(roof at 60 % of fill) and spheres 4 on 8, 14 on 2 and 24 on 2. Tool: `StorageRender.java`, `render.sh`, `README.md`.

## Open items for the owner
1. Tall narrow tank (shell > D): at a low level the rolling ladder (length H) passes through the shell. Options: limit
   the shell to D, or draw a telescoping ladder.
2. Large spheres: the top of the stair flattens to a near-level ramp below the crown. It passes the walk test, but it
   looks shallow in `sphere-24-on-2`.
3. The D 4 sphere's crown deck is wide (rp 1.75 m on a 2 m radius) because of the minimum landing.
4. Wound stairs are 1.4 m wide, against 0.9 m for straight stairs, because the walk box is world-aligned at the bends.
5. Placeholders: `OVERFLOW` maps to the LIQUID phase port (`TODO(WP1, 3.1)`); volumes above
   `FluidDeviceSpec.MAX_VOLUME` stay unregistered (`TODO(WP2, 3.2)`: MAX_VOLUME 30,000 and the open flag).

Code guide: update `equipment.md` at the merge (kinds, `WoundStair`, large solids, `floating`, `OPEN`, `equipment-10`).
