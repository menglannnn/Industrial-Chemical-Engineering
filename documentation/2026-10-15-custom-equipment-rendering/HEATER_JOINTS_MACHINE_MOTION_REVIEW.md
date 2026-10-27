# Heater joints, compressor pistons, machine motion: review

Batch `2026-10-15-custom-equipment-rendering` · branch `claude/custom-equipment-rendering-805e7d`, working tree on 9cadc501 · 2026-10-18 · owner notes: "Red dots on the side of furnace convection section is weird"; "Furnaces are assembled from sections and have junction lines, check actual photo"; "Add Piston model to compressor"; "Give compressor and pump working animations". Plan and photo survey: `HEATER_JOINTS_MACHINE_MOTION_PLAN.md`.

## Expectation
- The red dots were soot-blower gearboxes. Soot blowers clean oil-fired convection tubes; our heaters burn fuel gas and keep their tubes clean, so a real one has none.
- Heaters arrive as road-legal shop modules bolted together on site. A vertical heater's curved radiant panels, and a cabin's wall panels, meet on vertical flanged seams at the casing columns. Convection modules of 2 to 3 m stack on bolted perimeter flanges, with corner columns; stack sections have flanged joints. Each joint is a ridge about 0.1 m proud with a row of nuts.
- A reciprocating compressor's pistons never show. Its piston rod does, in the open distance piece between the crosshead guide and the cylinder's packing case, with an oil deflector collar riding on it. Balanced-opposed throws move outward together on a slider-crank (connecting rod 5 cranks long).
- A pump's shaft shows in its bracket windows; on a TEFC motor the same shaft turns the cooling fan behind the cowl's grille.
- Real speeds alias at frame rate (crank 5 to 14 rev/s, pump about 49 rev/s), so the display runs at 1 rev/s for the crank, 2 for the pump and 1.5 for the air cooler fans. 40 ticks hold whole turns of all three.

## What changed
- Soot blowers removed (`convection()` and the heater docs).
- Joints in `ProcessUnit`: `seam`, two flange bars either side of a split with nuts, at every vertical heater casing column and cabin wall column; `joint`, a flange frame round the convection box at its foot, between its modules (equal, 3 m at most, header boxes split with them) and at its top, with corner columns; `stackJoint`, a flanged split every 8 m of stack.
- Compressor: each distance piece is open (plates, side walls round two windows, partition), with packing and wiper cases. Its piston rod and collar are a moving part, long enough that both ends stay hidden over the whole stroke.
- One moving-parts path replaces the air cooler's fan special case:
  - `Mesh` facets carry a part index. `ProcessUnit.Motion` either turns or rides a slider-crank (pivot, axis, speed, phase, crank, rod).
  - `ShapeRasterizer.surface`/`fixedSurface`/`moves`/`moving` cut the parts at the cell grid, relative to their pivots. `ProcessUnit.rotors`/`rotor` and the `AC_FAN` filter are gone.
  - `MachineAnimation` (was `FanAnimation`) draws each tracked part's VBO with its transform. The block entity tracks the parts whose pivots it holds, and the shell model bakes the cell without them while they move.
  - The datasheet switch "Moving parts: running/still" is on pumps, compressors and air coolers; on by default, not saved.
- Pump: a seal collar with set screws on the shaft in each bracket window. The motor's cowl end is a spoked, ringed grille over a dark bore, with a six-blade fan behind it (OH2 motor and BB1 cooler cowl). The shaft and the fan turn.

## Measured
- Quads (`MachineCounts`), before → after, +10 to +25 %: pump 0.05 1702 → 1976, 0.75 3543 → 3937; compressor 0.15 × 2 3635 → 4270, 1.2 × 10 23225 → 25501; vertical heater 2.5 × 4 5392 → 6016, 12 × 20 33166 → 39307; cabin 6 × 12 11600 → 13506, 12 × 48 42603 → 51715.
- The first form (nuts every 0.5 m, buried bar faces drawn) gave +41 % on the 12 × 48 cabin; leaving hidden faces out and spacing nuts every 0.75 m brought it to +21 %. Removing the soot blowers drops the shell cells they stuck into (vertical heater 12 × 20: 2526 → 2496).
- Gates in `ProcessUnitTest`, replacing `fanRotorsTurnAboutTheirHubs`:
  - `movingPartsAreTheStillOnesSetGoing`: part counts per kind, pivots inside the box, polygons within reach, unpainted materials, and moving area equal to surface minus fixed surface.
  - `pistonRodsHideTheirEnds`: stroke equal to two cranks, both rod ends inside the solid over a whole turn, one rod per throw.
- Equipment tests with the determinism source gate: 56/56 on the final code (`ProcessUnitTest` 8, `ShapeRasterizerTest` 25).

## In game
- Bridge lane (vanilla, fresh world, `machines-check-5`): the rods slide in the open windows (three shots, the collar at three places); the pump collars, both motor fans and the air cooler fans turn; vertical heater seams with nut rows, two convection modules on D 6 m with their joint, cabin wall seams, no soot blowers.
- Iris lane (Complementary Reimagined): the rods draw shaded and move (`iris-rods-stack.png`).
- Capture note: the background client refreshes its frames slowly, so captures within about 2 s come out identical; motion shots were taken 5 to 6 s apart.

## Owner propositions
1. Nuts every 0.75 m instead of 0.5 m keep the quad increase to +10 to +25 %. Keep?
2. Cabin roof bay seams are not drawn because they would cross the convection walkway deck. Keep the roof plain?
3. The stack damper's red actuator stays: one box on the stack, not on the convection side. Keep it, or paint it in the Structure colour?

## Code guide and tools
- Code guide: the equipment chapter is updated at the merge (moving-parts path, `MachineAnimation`, joints); no merge here.
- Tools: `tools/custom-equipment-rendering-poc/machines-check-5/` (scenes, views, shots, `MovingParts`, `Stack`) and `logs/machines-check-5-2026-10-18.tar.gz`.
