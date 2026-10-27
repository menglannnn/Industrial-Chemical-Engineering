# 2026-10-15-fluid-tank-column-gui-fixes

Purpose: Owner list 2026-10-15: tank rail phase ratio and status/errors, organic/aqueous phase names, kmol and kg side by side, column four side draws (`MAX_STREAMS` follows), nitrogen column feed (preset `tia_juana_nitrogen`, bundled mixture), generator mixture solids, recompiled devices re-presented after a topology commit; a held island accepts a queued topology event at its committed tick and events fence only joined neighbours (owner decision 2026-10-15)

Status: Implemented 2026-10-15 (0.8.0). Index: [../INDEX.md](../INDEX.md).

## Documents

- [FLUID_TANK_COLUMN_GUI_FIXES_REVIEW.md](FLUID_TANK_COLUMN_GUI_FIXES_REVIEW.md)

## Code areas

`FluidDeviceScreen`, `ColumnCalculatorV3Screen`, `V3ClientPresets`, `V3EditorDraft`, `V3ColumnInput`, `V3ColumnStreamProperties`, `PhysicalRegistry`, `IslandCoordinator`, `FluidWorldAuthority`, materials presets, mixture presets, `lang/en_us.json`; `test`, `runMcpClient`

## History

- Implemented 2026-10-15 (main `ffeafe8a`, mod 0.8.0, fast-forward of `claude/fluid-tank-column-gui-fixes-3f0fc3`, not pushed; final gates: full `test` 1190/0, 33 junction lines 0 diff, chain-100 exact 0, fluid GameTests 31/31, column 2/2; detached: `tools/fluid-tank-column-gui-fixes/`; branch gates G1 536/0 with 33 junction lines 0 diff, G2 exact 0, G3 48/48, GameTests 31/31; in-game check through the bridge: tank, slurry, block break, N2 and 4th draw PASS)
