# Drum bottom outlet, round-shell shading, constructor descriptions, drum datasheet tab: review

Batch `2026-10-15-custom-equipment-rendering` · branch `claude/custom-equipment-rendering-805e7d` · 2026-10-16 · committed on the branch as 91fcb448 (code) and d43509f5 (changelog), not merged · owner report (constructor rows, drum outlet "inside the skirt", column side shadows) and request ("right clicking the drum should open the reservoir GUI, its information can be shown in a tab").

## Physical expectation

- A drum on a skirt drains from the low point of its bottom head. The line drops inside the skirt, turns, and leaves through a sleeve in the skirt wall about 0.5 m above grade, towards the pump suction. A D3 × 4 drum on a 2 m skirt: head pole at 1.25 m, outlet line at 0.5 m.
- A nozzle on the shell 0.5 m above the bottom tangent would leave the head and that 0.5 m of shell undrained: 3.5 + 3.5 = 7.1 m³ of 35.3 m³. Its line would also start 2 m higher, about 20 kPa of water head less at the pump.
- A round shell in daylight darkens continuously with the angle between its surface and the light. It has no edges where the brightness steps.

## What the code did

1. **Outlet.** A skirted drum's liquid outlet was a side nozzle at the lowest cell centre at least 0.25 m above the bottom tangent, facing the front. That is on the shell just above the skirt; the owner read it as "inside the skirt". The reservoir is lumped, so the model still drained it all through the LIQUID port.
2. **Bands.** Each quad asked vanilla for its shade. Vanilla picks one of six values from the nearest axis (up 1.0, down 0.5, N/S 0.8, E/W 0.6), so a cylinder steps between 0.8 and 0.6 at every 45°: four bands around a column.
3. **Constructor.** Rows had a label and a value only. The − / + buttons said "Shift: steps of 5".
4. **Drum use.** Use opened the tank and sneak-use the datasheet: two screens for one vessel.

## Fixes (in their stage)

- `EquipmentShape.nozzles`: on a skirt with room under the head, the LIQUID nozzle is the bottom outlet. It leaves the skirt at `SKIRT_OUTLET` = 0.5 m, facing frame +x: opposite the inlet, on the left of a player looking from the constructor. The access opening stays at the front. Its size is capped so the flange clears the base plate (`PipeSizes.largestUnder`, the inverse of `flange`) and the line fits under the head. Without that room (for example D3 on a 1 m skirt) the shell outlet above the tangent remains.
- `ShapeRasterizer`: a sleeve where the line passes the skirt. A drum's anchor chairs move to the diagonals, clear of the opening and the sleeve; one `chairFacet` serves both drawing and collision. Columns are unchanged.
- `EquipmentShellModel.quad` (shared by `FluidDeviceModel`): the quad asks for no shade. Each vertex colour carries NeoForge's normal-based shade (`QuadLighter.calculateShade`: x²·0.6 + y²·(3+y)/4 + z²·0.8), which is continuous from 0.6 to 0.8 around a shell. An axis-facing face keeps exactly its block-face value. Vanilla and Sodium both multiply the vertex colour.
- `EquipmentConstructorScreen`: one `rows()` list names each row. Hovering a label or value shows what the row sets, as do its − and + buttons (with the shift hint). The datasheet lines and the status line have their own descriptions. The vertical drum's choice text now names the outlet and inlet sides.
- `VesselScreen.datasheet` builds the datasheet lines for both screens; a column's first line now names it as a column. `FluidDeviceScreen` page 5 "Datasheet" is a drum's tab beside "Connections by face", scrolled like the other tables. `EquipmentShellBlock.use` serves shell and interior cells: a drum opens its tank, the fluid debugger opens it with diagnostics, and a column opens its datasheet. Sneak-use no longer differs.

## Checks

- Unit suite: 1,226 tests in 215 classes, 0 failures. Four equipment assertions follow the new rule, with no tolerance widened:
  - `ShapeRasterizerTest.partsCarryTheirMaterials`: skirt row 0 now holds PIPE and FLANGE;
  - `drumNozzlesArePortsByPosition`: a standing drum's LIQUID faces opposite the inlet;
  - `supportsCarryTheVessel`: the skirt-wall probes moved to the inlet side;
  - `VesselPlacementTest.formTurnsWithTheConstructor`: LIQUID on the constructor-viewer's left.
- GameTests and chain-100 not re-run: no GameTest builds a drum, and solver code is unchanged.
- In game (bridge lane, fresh world), shots 48–51:
  - the outlet leaves the skirt on the east of a north-fronted drum, with sleeve, flange and chairs;
  - the column cladding shades smoothly;
  - plain use opens the tank, and the Datasheet tab lists the drum.
- Not seen in game:
  - hover texts (the bridge hovers only through desktop input);
  - the datasheet's nozzle lines below the fold (the bridge's `scroll_at` does not reach the 1.21 four-argument `mouseScrolled`);
  - the column datasheet and the new choice text (the window was minimized mid-check, so those shots were blank).

## For the owner to validate (reversible)

- The bottom outlet faces left, seen from the constructor, opposite the inlet.
- A drum's chairs sit on the diagonals.
- A skirt too short for the line keeps the shell outlet.
- A column's bottoms nozzle is unchanged (still above the tangent); the same treatment is possible as a follow-up.
- The baked smooth shade: under an Iris shader pack the pack may shade again (not checked), and the Nether's flat ambient is not modelled.

Code guide: still to be written with the batch (unchanged item). Tool folder: `tools/custom-equipment-rendering-poc/drum-check/` (scene, views, aims, shots); logs `logs/drum-check-2026-10-16.tar.gz` (bridge client, unit suite).
