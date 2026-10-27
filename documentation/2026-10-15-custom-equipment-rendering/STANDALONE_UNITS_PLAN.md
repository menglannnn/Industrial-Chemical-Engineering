# Standalone units, column nozzles at pipe size: plan

Batch `2026-10-15-custom-equipment-rendering` · branch `claude/custom-equipment-rendering-805e7d` · 2026-10-16 · replaces the attached auxiliaries of `COLUMN_AUXILIARIES_PLAN.md` (2b642fd1).

## Owner decisions (2026-10-16)

- R1: "AC, heat exchanger, reboiler will be standalone structures, which HX and reboiler can be scaled. AC appears to be too large, define a largest size, and use scale out/numbers approach."
- R2: "On the column leave their specific connections, and let users arrange them": nozzles at fixed sides (feed and bottoms at the front; reflux and pumparounds on the right; side draws, steam and the reboiler return on the left).
- R3: "distillation column output on the skirt like the drum, with pipe connections scalable".
- R4: the ejector is a standalone unit on its own frame, sized by its suction.

## Physical expectation (sizes)

- Shell-and-tube (TEMA AES): 3/4" tubes on a 1" triangular pitch, two passes, give about 1,260 D² tubes, so about 75 D² L m² of surface. A D 1.25 m shell with 6 m tubes gives 700 m², which condenses the 7 MW of a D 3 column at U 0.5 kW/m²K and ΔT 20 K. Bundles are 3 to 8 shell diameters long, so tubes are at least 3 D.
- Kettle: the bundle (D_b) sits in a shell of about 1.6 D_b, with vapour space over the boiling pool. Feed enters low at the front, vapour leaves on top and product leaves low behind the weir at the back.
- Air cooler (forced draft): about 120 kW/m² of face at a 35 K air rise. The largest bay is 3 × 6 m (12 m² of bundle face between the headers, about 1.5 MW), with two 1.9 m fans (46 % of the face; API 661 asks at least 40 %). More duty means more bays, each built as its own unit, side by side.
- Steam ejector: proportions in suction diameters (chest 0.8, chamber 1.5 × 1.2 wide, inlet cone 2.2, throat 0.8 × 0.35, diffuser 3.0 to 0.75 wide), 8.3 s long overall, standing with its discharge down.
- Liquid moves by gravity or by pump; the player places drums, pumps and pipes. Column and unit nozzles take the connected pipe's size; they are not fluid devices yet (a pipe there ends dead), as the column already was.

## Reversible decisions (mine)

- New constructor kinds: Heat exchanger (shell 0.5 to 2.5 m in 0.25 m steps; tubes 3 D to 12 m in whole metres), Kettle reboiler (bundle 0.5 to 2 m in 0.25 m steps; tubes 3 D_b to 10 m), Air cooler (one fixed 3 × 6 m bay; bundle height 5 to 16 m), Ejector (suction 0.1 to 1 m in 0.1 m steps; discharge 1 to 16 m up).
- The exchanger and kettle stand on saddles with the shell bottom 2 to 16 m up (room for a pipe under the bottom nozzles). The units lie lengthwise, the channel (the air cooler's inlet header, the ejector's suction) facing the constructor.
- Exchanger nozzles: tube side in at the bottom and out at the top of the channel; shell in on top near the back, out at the bottom near the front. Kettle: steam in on top and condensate out at the bottom of the channel, feed low at the front of the kettle, vapour on top, product low at the back. Air cooler: inlet on top and outlet at the bottom of the split front header. Ejector: motive steam on top, suction to the front, discharge down.
- The reflux drum is the horizontal drum and the pumps are the pump block. The column always has its vapour-return nozzle on the left.
- The column's skirt becomes the drum's: hollow with three slots, its bottom head (bare, under the cladding) drawn inside. The bottoms leave through the skirt port at the front, where the access opening was (a short skirt keeps the shell nozzle).
- Side nozzles avoid platform rows: every nozzle leaves a free block beyond its flange for a pipe, so rails need no gaps. The rail-gap and carried-ring code of the auxiliaries step goes.
- A cell key carries the codes of the nozzles that reach that cell, so a column with more than nine nozzles scales every one.

## Steps

1. Remove `ColumnAuxiliaries`, `ColumnOptions`, the Units page and `columnBox`. Restore protocol and NBT without options.
2. Column: bottoms through the skirt, a hollow skirt, nozzles off platform rows, ports for sizing, per-cell nozzle codes.
3. `ProcessUnit` gives each unit's parts, collision, interior and nozzles in its own frame. `EquipmentShape` gains the four kinds, and `ShapeRasterizer` builds a body-less geometry from the parts.
4. Constructor: seven kinds on a compact choice page, a design page per unit, datasheets, a command `/createcheme equipment unit`.
5. Gates: every port's pipe block is free; unit nozzles clear saddles and each other; per-cell codes round-trip. Then an in-game check on the bridge lane, on a fresh world.
