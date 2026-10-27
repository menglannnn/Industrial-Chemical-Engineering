# Standalone units, column nozzles at pipe size: review

Batch `2026-10-15-custom-equipment-rendering` · branch `claude/custom-equipment-rendering-805e7d` · 2026-10-16 · committed on the branch as 2f1ddb85 (code) and a2b4c2c8 (changelog), not merged. Replaces the attached column auxiliaries of 2b642fd1 (`COLUMN_AUXILIARIES_*`). Owner decisions R1–R4 in `STANDALONE_UNITS_PLAN.md`.

## Physical expectation

- One exchanger, kettle or air-cooler bay per duty, sized to it: AES 75 D² L m² (D 1.25 × 6 m ≈ 700 m², the 7 MW condenser of a D 3 column); kettle shell 1.6 D_b; air cooler about 120 kW/m², so one 3 × 6 m bay (12.2 m² face, about 1.5 MW, two 1.9 m fans over 46 % of the face, API 661 ≥ 40 %), and more duty means more bays. Ejector length is 8.3 suction diameters.
- Fluid enters and leaves where it goes by gravity or density: condensing shell side in on top, out underneath; kettle feed low, vapour on top, product low behind the weir; air cooler outlet at the header's bottom; ejector discharge down.
- Each nozzle needs one free block beyond its flange for a pipe. A column's bottoms leave at grade through the skirt.

## What was built

- `ProcessUnit` gives each unit's parts, collision, enclosed volume and nozzles in its own frame. `EquipmentShape.Kind` gains EXCHANGER, KETTLE, AIR_COOLER and EJECTOR (`unit()`), and `EquipmentShape.unit(...)` validates sizes on their grid. `ShapeRasterizer` builds a body-less geometry from the parts.
- Column (`ColumnStructure`):
  - fixed nozzle sides: feed and bottoms at the front, reflux and pumparounds on the right, draws, steam and the vapour return on the left;
  - side nozzles at the nearest row whose neck and pipe block clear the platforms, cages and davit (`Clearance`, a separating-axis test);
  - the drum's hollow skirt, with the bottoms leaving through its port (`skirtOutlet`).
- Every nozzle of every kind is a port that takes the pipe's size. Only drums are reservoirs. A cell key carries 7 bits per nozzle reaching it, at most 9 per cell, so a column's 15+ nozzles all scale.
- Constructor: seven kinds on the choice page (descriptions in hover text) and a design page per unit with size, tubes and support. The datasheet lines show surface, kettle shell, bay face or ejector length. Command `/createcheme equipment unit`; protocol `equipment-6`; no `Options` in NBT. `VesselScreen` shows a unit's datasheet and roles.
- Removed: `ColumnAuxiliaries`, `ColumnOptions`, `ColumnAuxiliariesTest`, the Units page, `columnBox`, and the rail-gap and carried-ring code.

## Found by the gates and fixed (each in its stage)

- **Kettle interior leaked**: the channel and cone had no enclosed volume, so cells inside them counted as outside. Their collision was also a 1.1 D_b cylinder under a cone flaring to 1.6 D_b. `Solid` now has a cone form, and channel, cone and shell collide and enclose as drawn.
- **Air cooler outlet pipe blocked**: the plenum's taper and the front fan ring reached 0.1 m into the block under the front header, and a beam under that header crossed the outlet neck. The plenum now leans back 0.25 m at the front, the fans fill its floor, the cross beam sits only under the return header, and the plenum collides as its mid-height section, not as a full box.
- **Ejector posts**: for suction ≤ 0.4 m the posts and base plates stood 5 cm into the suction's and the discharge's pipe blocks. The post spacing is now at least 0.5 m plus a base plate. Found in game: a small ejector's frames all fell in the suction's row and were left out, so its posts stood free. Now only the front bar and brackets give way there.
- **Default neck above its largest size**: `ColumnStructure.size` floored at 0.1 m over its maximum (motive steam of a 0.1 m ejector: 0.1 > 0.0625 m). The maximum now wins.
- `EquipmentShape.unit(DRUM, …)` built a drum; it now refuses non-units.

## Checks

- Gates: `ProcessUnitTest` (new, 3 tests) runs every size at its shortest and longest tubes plus two odd sizes. It checks ranges and grid, physics numbers, each nozzle's side, a free pipe block per nozzle (not shared, not under ground), flanges clear of each other, sealed interiors, and that the unit is on the ground filling its box.
- `ShapeRasterizerTest` adds 9 units and a D 0.5 column (30 trays, 4 pumparounds, 2 draws). Every nozzle of every shape now has a free pipe block (columns included), and a new test checks per-cell codes.
- `ColumnStructureTest` checks the fixed sides, and a free pipe block per nozzle on its four columns (D 1 to D 15 on a 16 m skirt). Equipment package: 40 tests, 0 failures.
- Test changes: the surface-area sum allows 20 slivers under `Mesh.MIN_AREA` (dropped by design; the D 0.5 column loses 0.44 mm²). Only drums must have plain body rows (the 12-tray column has none now).
- Unit suite: 1,233 tests in 216 classes, 0 failures. It ran before the ejector-frame and neck-format fixes, which the equipment package re-ran.
- In game (bridge lane, two fresh worlds, shots 71–81): a D 3 column, both exchanger sizes, a kettle, two air-cooler bays side by side and three ejectors built by command. Also the seven-kind choice page, an exchanger built from the constructor (channel towards it), and the kettle and column datasheets, with nozzle sides as in the plan.

## For the owner (reversible)

- The datasheet's neck sizes now show two decimals where needed (0.75 m, not 0.8 m).
- Moving from an ejector back to a drum keeps the ejector's length of 1 m, the range's floor.

## Open

- Column and unit nozzles are not fluid devices yet (a pipe there ends dead), as before.
- The command builds only simple columns; pumparound layouts come only by import.
- Hover text is not seen in game (the bridge's hover needs desktop input); FPS is not re-measured.
- Code guide: the equipment chapter is written with the batch.
- Tools: `tools/custom-equipment-rendering-poc/units-check/`, logs in `logs/units-check-2026-10-16.tar.gz`.
