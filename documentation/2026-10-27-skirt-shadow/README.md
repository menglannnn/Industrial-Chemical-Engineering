# 2026-10-27-skirt-shadow

Purpose: remove the dark "shadow" on the skirts of standing drums, columns and skirted units, so a skirt shades like the shell above it (owner request 2026-10-27: "Remove the shadow in drum/vessel skirt").

Status: In progress since 2026-10-27 on branch `claude/modest-nightingale-c9932d`; not merged, not pushed. Index: [../INDEX.md](../INDEX.md).

## Documents

- [SKIRT_SHADOW_REVIEW.md](SKIRT_SHADOW_REVIEW.md): expected behaviour, the four candidate effects and what each measured, the change, the owner's options, the gates.
- `figures/skirt-before-after-low.png`: a D 6 drum and a D 4 column, low view, in-game skylight, before and after.
- `figures/skirt-before-after-iso.png`: D 3 and D 6 drums, D 4 and D 8 columns, iso view, before and after.
- `figures/unit-skirts-before-after.png`: trickle-bed reactor, ebullated-bed reactor and adsorber, before and after.

## Code areas

`science/equipment/Mesh.Material.SKIRT` (support sprite, body paint); `ShapeRasterizer.skirtFace`, `slotEdges`; `ProcessUnit.skirt`; `DelayedCokerDrums`, `AdsorberDesign` (their skirts); `Paint` (documentation); `client/model/EquipmentShellModel.SHEEN`; `models/block/equipment_shell.json` (`skirt` texture); `IChemE` config comments; `lang/en_us.json` paint page texts. Tests: `PaintTest` (channels), `ShapeRasterizerTest.partsCarryTheirMaterials`. Code guide: `equipment.md` (rows `Mesh`, `Paint`). Tools: [../../tools/skirt-shadow/](../../tools/skirt-shadow/README.md).

## History

- 2026-10-27: the shadow identified as the skirt's paint channel (structure, RAL 7031, 37 % darker than the RAL 9006 shell); light, ambient occlusion and face shade ruled out by a skylight probe (every skirt outer cell at skylight 15). Skirt plates moved to a new body-paint material `SKIRT`; base ring, chairs and sleeves stay structure. Commit `c9e827ac` (code, tests), then the documents, figures, tool folder and CHANGELOG `[Unreleased]`. Gates: `test` 1643/1643. No in-game check.

## Open items

- Owner validation (review section 4): the secondary colour no longer paints skirts; options B, C, D not taken.
- Merge: CHANGELOG entry under `[Unreleased]`; proposed version: patch (a drawing fix); the owner chooses. Set the code guide snapshot line at the merge; run `slowGates` in the merge lane.
