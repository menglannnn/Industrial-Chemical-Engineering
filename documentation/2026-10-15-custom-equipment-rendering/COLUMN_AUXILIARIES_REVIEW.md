# Column auxiliaries (condenser and reflux drum, ejector, PA coolers, kettle): review

Batch `2026-10-15-custom-equipment-rendering` · branch `claude/custom-equipment-rendering-805e7d` · 2026-10-16 · committed on the branch as 2b642fd1 (code) and abec84a6 (changelog), not merged.

Owner input: condenser (water or air, required), steam ejector before it, PA coolers, reboiler (optional), "put on its side", sized with the column, "purely visual"; the condenser "needs to come with reflux drum"; the seven decisions of `COLUMN_AUXILIARIES_PLAN.md`.

## Physical expectation

- D 3: about 7 MW to condense. Water: a 1.25 m shell with 6 m tubes. Air: 60 m² of face (8.5 × the cross-section), two 3.5 × 9 m bays at least 8 m up, so the condensate falls to the drum.
- Liquid moves by gravity or by a pump: condenser over drum, the reflux pump lifts to tray 1, each PA pump lifts its cooled liquid back over its draw, and the sump feeds the kettle.
- Access (EN ISO 14122-2): walkways at least 0.6 m (0.8 m preferred) and 2.1 m of headroom. A line crosses a handrail through a gap in it.

## What was built

- `ColumnOptions` (condenser, ejector, reboiler; 3 bits) in `EquipmentShape`; NBT `Options` on the constructor and the shell; protocol `equipment-5`; `/createcheme equipment column … <front> water|air ejector|no_ejector none|kettle`.
- `ColumnAuxiliaries` (layout, mesh and collision in the column's frame):
  - a pipe bay with lanes, and a structure with posts, beams, fenced grating floors and a caged ladder;
  - the drum, with a water boot when there is an ejector; a TEMA AES condenser on the upper floor, or a forced-draft bank on its own stand 1 m beyond;
  - PA coolers and pumps in lanes at grade, the reflux pump, the ejector in a frame on the down-leg, and the kettle on the left;
  - utility ends with blind flanges.
- Nozzles: reflux, PA draws and returns on the right (structure side); side draws, steam and the kettle's vapour return on the left. A kettle raises the least skirt (D 6: 3 m instead of 2).
- The column's own square stays on the constructor's line, with the units aside (`columnBox()`). The constructor has a Units page (Condenser, Steam ejector, Reboiler; footprint, PA coolers, least skirt), reached by a fourth bottom button.
- Sizes with two PAs: D 3 water, box 21 × 24 × 7 blocks, structure 9.25 × 4.9 m, floors at 4 and 8 m (the plan said 9 × 4 m, 4 and 8 m). D 3 air + ejector + kettle: 34 × 25 × 10 blocks. 6–11 k unit facets.

## Found and fixed during the step (each in its stage)

- **Heap**: a column geometry takes 15–40 MB (about 750 B per facet), and the units' cache kept a second mesh, so the clash sweep ran out of heap. The cache now keeps collision only and redraws facets on demand. The geometry cache is also bounded at 300 k facets; before, only its entry count was bounded.
- **Lines through rails**: where a line crosses, the column's handrail is cut, and its fence collision now is too, so collision follows what is drawn. A part ring that ends at a line's heading (D ≤ 0.5) is carried on past the line. The clash check tests drawn parts only (`insideDrawn`; the 1.4 m fence band above the top rail is collision only).
- **Walkways** (found in game): at the drum's head 0.3 m was free, and 0.6 m at the sides. Every unit now keeps `WALKWAY` 0.8 m. `HEADROOM` 2.1 m raises the second floor and the overhead run when needed.
- **Determinism**: `Math.hypot` removed from `science/`.

## Checks

- **Gate** `ColumnAuxiliariesTest` (5 tests, about 3 s): options round-trip, kettle skirt, column on the line, and no line through another line, a unit, the structure or the shell (2 tray sets × 8 options × 3 sizes). A player fits on every floor's walkway. Before the fixes the clash check found 156 crossings; the walkway test fails at WALKWAY 0.3 / HEADROOM 1.5. `ShapeRasterizerTest` gains a column with every unit.
- **Unit suite**: 1,234 tests in 216 classes, 0 failures (counted without the four probe classes).
- **Probes** (detached): a 192-shape clash sweep finds none; also software renders, sizes and heap.
- **In game** (bridge lane, fresh worlds, shots 62–70):
  - four columns placed by command;
  - the Units page and a build from the constructor;
  - survival drops land on both floors' walkways (−56.875 and −52.875). Shots 63–69 predate the walkway fix.

## Deviations from the plan, for the owner (reversible)

- Reflux rises in the pipe bay on the structure side (plan: up the front), and side draws moved to the left.
- PA coolers lie in lanes at grade under the first floor (plan: stacked two high).
- The Reboiler button reads None / Kettle.

## Open

- PA layouts only by import (the command has none); Units-page hover text not seen (needs desktop input); FPS not re-measured. Code guide: the equipment chapter is to be written with the batch. Tools: `tools/custom-equipment-rendering-poc/aux-layout/` (probes, renders), `aux-check/`; logs `logs/aux-check-2026-10-16.tar.gz`.
