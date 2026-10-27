# 2026-10-27-skirt-shadow

Purpose: remove the shadow on the floor inside a vessel's skirt, seen through its slots and port, which grew with the diameter (owner request 2026-10-27: "Remove the shadow in drum/vessel skirt"; clarified: "the bottom plane looked through the windows, drums with very large diameter makes this worse").

Status: In progress since 2026-10-27 on branch `claude/modest-nightingale-c9932d`; not merged, not pushed. Index: [../INDEX.md](../INDEX.md).

## Documents

- [SKIRT_SHADOW_REVIEW.md](SKIRT_SHADOW_REVIEW.md): expected behaviour, the light the floor got (by diameter), the change, what the owner validates, the gates.
- `figures/floor-before-after.png`: D 3, D 6 and D 10 drums and a D 8 column cut at 0.3 m, in-game skylight at noon, before and after.

## Code areas

`science/equipment/ShapeRasterizer.skylight`, `ProcessUnit.openSkirt` (set by `skirt`); `world/level/block/EquipmentInteriorBlock.SKYLIGHT`; `EquipmentStructure` (`interior`, `restateLater`, `restate`, `restateCells`, the tick queue, `refresh`); `EquipmentShellBlockEntity.onLoad` (the controller queues the restate); `blockstates/equipment_interior.json`. Tests: `ShapeRasterizerTest.aSkirtShowingItsInsideLetsSkylightThrough`; GameTest `EquipmentCompatGameTests.aSkirtsFloorTakesTheSkylightAboveTheDrum`. Code guide: `equipment.md`. Tools: [../../tools/skirt-shadow/](../../tools/skirt-shadow/README.md).

## History

- 2026-10-27: first reading: the skirt's darker paint (structure channel). Skirts moved to the body paint (`c9e827ac`, docs `0711471d`). The owner corrected the reading; reverted (`f2b15b31`).
- 2026-10-27: the shadow is the floor's light: opaque interior cells leave it lit only sideways from the skirt ring (skylight 9 at a 10 m drum's centre). Owner choice "Skylight through" (over baking light into the inside faces). Code `d02d221f`: `EquipmentInteriorBlock.SKYLIGHT` on the interior cells of a vessel whose skirt shows its inside; placed vessels restated at load. `test` 1644/1644, fluid GameTests 35/35; floor skylight 15 at every size (probe).

## Open items

- Owner validation (review section 4); Iris shadow map and Distant Horizons not checked in game.
- Merge: CHANGELOG entry under `[Unreleased]`; proposed version: patch (a lighting fix); the owner chooses. Set the code guide snapshot line at the merge; run `slowGates` in the merge lane.
