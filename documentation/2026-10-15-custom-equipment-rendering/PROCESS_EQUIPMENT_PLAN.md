# Drums, column structure, pipes and pipeline accessories: plan

Batch `2026-10-15-custom-equipment-rendering` · branch `claude/custom-equipment-rendering-805e7d` · 2026-10-16 · Status: In progress.

Owner request 2026-10-16: (1) rename the two vessel forms to drums and make them valid reservoirs; skirt access opening, side and top nozzles, no vents; (2) a distillation column modelling system after real images, at much higher model precision; (3) pipes modelled the same way, diameter scaled, capped at 1 m; (4) better pipeline-accessory models whose connections scale with the connected pipe; (5) only pipes and valves can be placed vertically.
Sources: `PROCESS_EQUIPMENT_PHOTO_SURVEY.md` (photos and dimension tables), code surveys of the fluid devices and the column block (this session).

## Owner decisions (2026-10-16)

- **OD1 Drum ports: fixed class by position.** Top nozzle = VAPOR port, lowest side nozzle = LIQUID port, other side nozzles = BULK. The drum's real volume and height set the liquid head (`g·m_c·H/V`); no level-dependent draw.
- **OD2 Standard nozzle set**, each nozzle ending on a block face; its size follows the connected pipe.
- **OD3 Column**: its own design in the constructor, able to import a column design; reuse the column GUI code where possible. Shape only (no fluid ports, no link to the solver).
- **OD4 Actuator hydraulics follow the line**: a pump's, compressor's or valve's half of a link takes the connected pipe's bore and roughness instead of the fixed 0.05 m.

## Physical picture

- **Drum as a reservoir.** A knock-out drum is one well-mixed vessel: vapour leaves at the top, liquid at the bottom outlet, the feed enters at the side. The pressure at the bottom outlet is the headspace pressure plus the liquid column `ρ_L g h_L`. A 3 × 9 m drum one third full of water: h_L ≈ 3.2 m → +31 kPa at the outlet. Today the solver states every tank as 1 m tall (`LEVEL_HEAD_HEIGHT`), so a drum must state its own height.
- **Hydraulics at a pump.** A 0.5 m line through a pump whose half link is 0.5 m of 0.05 m bore: at 1 m/s in the line (≈ 200 kg/s of water) the bore runs at 100 m/s and the half link alone drops about 10 bar (`f L/d · ρv²/2` = 0.2 × 5 MPa, f 0.02), against 10 Pa for the same length of line. Real pump and valve nozzles match the line (or one size below), so the line's bore is the physical choice; equal bores give exactly the old doubles.
- **Pipes.** Rack runs are welded; flanges sit at equipment, valves and spool breaks. Long-radius elbows R = 1.5 d. Flange OD ≈ 2 d at 4", 1.3 d from 24"; a flange pair is about one texel thick (survey section 0).

## Work packages

1. **Geometry kernel.** Extract the facet machinery of `ShapeRasterizer` (polygon builder, UV frames, clipping, cell pieces) into a shared `Mesh` with primitives: prism, frustum, annulus, box, torus segment, sphere. More materials (nozzle steel, flange, opening, pipe, grating, handrail, ladder steel, cladding, machine, motor, actuator), sprites from the generator. One client `ProceduralModel` bakes polygons by material for every procedural block.
2. **Placement and hydraulics.** `FluidDeviceBlock.getStateForPlacement`: pipes and valves take any of the six facings, other kinds the horizontal one (OD of item 5). `PhysicalFluidTopology`: an actuator's half link takes the pipe's bore and roughness (OD4). Tests: the pinned science tests keep their vertical actuators (they bypass the block); chain-100 and the junction lines must stay exact (default bores are equal).
3. **Pipe model.** The pipe's bore and each face's connection size become static geometry on `FluidDeviceBlockEntity`, synced by its update tag like the shell cells. The server refreshes them when a topology commit applies an edit (engine schedule) and when a neighbour changes. The model: OD = max(1/16, min(1, 1.1 d)); straight runs, LR elbows (R = 1.5 OD, at most 0.5 m), tees and crosses on a core, concentric reducers in the larger pipe, flange pairs at equipment and nozzle faces and every 6 blocks along straight runs, end caps on dead ends. Collision follows the OD (at least 3/16).
4. **Accessory models** (photo survey section 4), flanges and necks sized by the pipe on each face: in-line vertical pump (OH3, motor up), compressor skid, globe control valve with diaphragm actuator (up in horizontal lines, horizontal in vertical lines), Y-strainer (leg down), the one-block tank as a small drum with flanged nozzles, generator and void as battery-limit boxes.
5. **Drums.** Player-facing names become drums. Box width odd, so the axis lies on a cell centre. A drum has a front (towards the constructor). Standard set: vertical — top nozzle at the head centre (VAPOR), liquid outlet just above the bottom tangent facing front (LIQUID), inlet in the upper part facing right (BULK); horizontal — two top nozzles near the ends (VAPOR) and a low nozzle on the front or right head (LIQUID). Flange faces on the first block plane ≥ 0.15 m beyond the surface; unconnected nozzles show blind flanges; size = min(pipe bore, fit). Skirt access opening (0.5–0.6 m, sleeve, dark back) facing front, when the skirt clear height allows.
6. **Drums as reservoirs.** A RESERVOIR registration may carry ports (cell, face, port class). The node sits at the drum's bottom cell; volume = design volume (spec bound raised to 10,000 m³); `Inventory` gains the level height H (1 for the one-block tank: old doubles unchanged; vertical drum T/T + D/2, horizontal D). Ledger and checkpoint format 8 → 9 (fresh worlds, AGENTS.md). Registry neighbour walk, compile, presentation binding and the menu accept a drum. Build registers it, any broken cell removes it. Use opens the tank screen; sneak-use the datasheet.
7. **Column structure.** Design: diameter (0.5 m steps, up to 15 m like the calculator), trays at the solver's 0.6 m, feed tray, side draws, steam stages, pumparounds, skirt height. Height from trays (bottom space and top space added, whole metres). Parts (survey section 2): insulated shell with cladding panels and flashing rings, skirt with opening, manways with davits at the top, bottom, feed and every ≤ 15 trays, part-ring platforms (grating, handrails with mid-rail and toe board) at manways and a top ring, caged ladders between platforms alternating sides, top davit, nozzles at the feed, reflux, draws, pumparounds, steam, bottoms and vapour outlet (shape only). Finer facets than drums (≤ 0.25 m). Platforms are walkable (1/8 m decks), handrails collide like fences, ladder cells are climbable.
8. **Column constructor form.** A fourth form, Column. Import from the column preset library (bundled and the player's saved presets, `V3ClientPresets`) or from the nearest column calculator (server reply to the request); design separately with steppers and the column GUI's diagram (extracted into a shared helper).
9. **Checks and documents.** Unit tests per package; one final gates run (chain-100, junction lines, GameTests) for packages 2 and 6; dev-client shots of every part; review ≤ 50 lines; INDEX, CHANGELOG, tools README.

## Reversible decisions taken here

Front towards the constructor; inlet facing right; opening at the front. Node at the bottom cell (no per-port elevations: OD1's simple form). Nozzle stand-out ≥ 0.15 m to the next block plane. Flanges every 6 blocks on straight runs. Column trays: top space 1.2 m, bottom space max(2 m, D/2). Column nozzles are shape only until a column ↔ fluid network link exists.

## Risks

Package 6 changes the save format and the solver's head term: gated by chain-100 and the junction lines (exact) and the GameTests. Package 7 is the largest model; quad counts and the FPS pair are measured once at the end.
