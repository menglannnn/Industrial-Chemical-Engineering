# 2026-10-26-air-cooler-chunk-rendering

Purpose: Owner report 2026-10-26: changing an air cooler's working status reloads the chunk rendering. Investigate and fix.

Status: Implemented 2026-10-26 (0.12.3). Index: [../INDEX.md](../INDEX.md).

## Documents

- [AIR_COOLER_CHUNK_RELOAD_REVIEW.md](AIR_COOLER_CHUNK_RELOAD_REVIEW.md)

## Code areas

`client/MachineAnimation.java` (`running`, `toggle`, `render`), `client/model/EquipmentShellModel.java` (`getQuads`, `Layers`), `client/gui/screens/VesselScreen.java` (motion switch), `science/equipment/ShapeRasterizer.java` (`fixedSurface`; `moves(CellKey)` deleted), `lang/en_us.json` (`motion.hint`)

## History

- 2026-10-26: investigation on branch `claude/air-cooler-chunk-rendering-5d0aba` at `a8ce7558` (0.12.1). Cause: the datasheet's "Moving parts" switch called `LevelRenderer.allChanged()` (the F3+A reload, about 15,000 sections at render distance 12) because the shell model read the switch while it meshed.
- 2026-10-26: owner direction: the switch behaves like a pump's or compressor's on/off. Fix: the chunk mesh never holds moving parts; off, `MachineAnimation` draws them at rest; the `still` layer and `ShapeRasterizer.moves(CellKey)` are deleted. `ShapeRasterizerTest` 37/0, `ProcessUnitTest` 22/0. Commit `1d57c010`.
- 2026-10-26: rebased onto `bc7a4167` (0.12.2, documentation only): fix `c91af09a`, release prep `2fbb80f2` (0.12.3); merged to `main` by fast-forward as 0.12.3 (`2fbb80f2`), not pushed. Test run waived by the owner. Code guide `client-gui.md`, `equipment.md`, `README.md` at `2fbb80f2`. Tool folders: none.

## Open items

- One in-game check: switch the button and watch the terrain (the client is the only judge).
