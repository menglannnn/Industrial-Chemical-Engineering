# Skirt shadow: review

Batch `2026-10-27-skirt-shadow` · branch `claude/modest-nightingale-c9932d` · 2026-10-27 · owner request (verbatim): "Remove the shadow in drum/vessel skirt".

## 1. Expected behaviour

A skirt is a steel cylinder of the shell's diameter under the bottom head. Outdoors, its outside faces the same sky as the shell above it, so it is lit the same way and darkens only with the angle to the light. Real skirts are usually painted with the vessel (or fireproofed in concrete grey). Only the inside of the skirt is dark, and it shows only through the access opening and the slots.

## 2. What the code did

Four candidate effects were checked, on a D 3 and a D 6 standing drum and a D 4 and a D 8 column (renders in `figures/`, probe in `tools/skirt-shadow/`):

| Candidate | Finding |
|---|---|
| Paint channel | **The cause.** The skirt plate was `Mesh.Material.SUPPORT`, tinted by the structure colour: default RAL 7031 `#5B686D` (mean 101/255) under a RAL 9006 `#A1A1A0` shell (mean 161/255). On the same sprite (painted sprites all average about 236), the skirt drew 37 % darker than the shell: a dark band under every skirted vessel. The 7031 default was an agent choice in the 2026-10-16 polish build ("it keeps today's darker skirt", `EQUIPMENT_POLISH_PLAN.md`), not an owner decision. |
| Shadow band under the head | None drawn. The 0.1 m bare-steel weld band at the bottom tangent line (owner request 2026-10-16) is a thin line. Kept. |
| Light, ambient occlusion, face shade | None. The shell model asks for no ambient occlusion and bakes the same smooth-normal shade into skirt and shell. A skylight probe (Minecraft's propagation over the layout: interior cells opaque, shell cells pass skylight down) puts every cell that holds a skirt's outer face at skylight 15 on all four vessels. |
| Inside through the port and slots | Shell cells inside the skirt sit at skylight 12 to 14 (10 to 14 under the D 8 column). Correct for the inside of a skirt; seen only through one-facet slots and the port. Unchanged. |

## 3. Change

- `Mesh.Material.SKIRT` (period 2, `Paint.BODY`): the support sprite in the body's paint. The shell model gives it `painted_support` and the metallic sheen, as `SUPPORT` had.
- `SKIRT` draws the skirt plate, its inner face, its slot sills, heads and jambs, and the plate pieces round the port: `ShapeRasterizer.skirtFace`, `slotEdges`; `ProcessUnit.skirt` (trickle-bed and ebullated-bed reactors); the coker drums' skirt (`DelayedCokerDrums`); the adsorber beds' skirt (`AdsorberDesign`).
- The base ring, the anchor chairs, the floor plate, the port sleeve and the vent sleeves stay `SUPPORT`, in the structure colour, with saddles and steelwork.
- `Paint` documentation, the config comments and the constructor's paint texts name the skirt under the primary colour and the base ring under the secondary.
- Cells, collision, nozzles and layouts do not change (`EquipmentLayoutGoldenTest` untouched). No saved field changes.

## 4. For the owner to validate (reversible)

- **A placed vessel's skirt redraws in its primary colour at the next load.** The saved paint keeps both colours; the secondary colour now paints the base ring, chairs, saddles and steelwork, but no longer the skirt. A player who painted a skirt differently from the shell loses that contrast.
- Options not taken:
  - B: a lighter default secondary colour (RAL 7035 or 9006). Only new designs change; placed vessels keep their saved 7031 skirts, and saddles and steelwork turn light too.
  - C: base ring and anchor chairs in the primary colour as well.
  - D: a third paint channel for skirts: one more saved paint field, page row and tint index.

## 5. Gates

- `./gradlew test` on `c9e827ac`'s tree (second Gradle lane, `--max-workers=4`): 1643 tests, 0 failures, 0 errors. Two tests follow the new material: `PaintTest` (`SKIRT` is a body material), `ShapeRasterizerTest.partsCarryTheirMaterials` (the skirt rows hold `SKIRT` beside `SUPPORT`); no tolerance widened.
- `slowGates` and the fluid GameTests not run: no solver, fluid or layout code changed. Run them in the merge lane.

No in-game check: the change only redraws (judged by the software renders, which use the game's polygons, sprites, tint and shade, plus the skylight probe), and the sump session held a Gradle lane.

Code guide: `equipment.md` rows `Mesh` and `Paint` updated; the snapshot line is left for the merge (the sump branch rewrites the same line). Tool folder: `tools/skirt-shadow/`.
