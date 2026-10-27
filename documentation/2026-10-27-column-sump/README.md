# 2026-10-27-column-sump

Purpose: a distillation column's sump (bottom tangent to bottom tray) sized as real columns are, from engineering references, so it looks right (owner request 2026-10-27: "Enlarge the bottom sump of the distillation column so it looks more real, find some engineering drawings for reference").

Status: In progress since 2026-10-27 on branch `claude/distillation-sump-enlargement-59e8e7`; not merged. Index: [../INDEX.md](../INDEX.md).

## Documents

- [COLUMN_SUMP_SURVEY.md](COLUMN_SUMP_SURVEY.md): the references: the stack of clearances and levels under the bottom tray, dimensioned drawings (a 2.44 m stripper with a 10.9 m sump; Kister's 4.72 m deethanizer; Walas and Perry's allowances), wider and narrower bottoms, what shows outside, and the game's rule from them.
- [COLUMN_SUMP_REVIEW.md](COLUMN_SUMP_REVIEW.md): expected behaviour, what the code did, the change, the sweeps, the nozzle moves on placed columns, the owner's options, the gates.
- `figures/sump-before-after.png`: five columns from the front at one scale: before, after, and the alternative not taken; `sump-before-iso.png`, `sump-after-iso.png`.

## Code areas

`science/equipment/ColumnTrays` (`bottomSpace` as the sump's stack: `LOW_LEVEL`, `LEVEL_SPAN`, `UNDER_RETURN`, `OVER_RETURN`), `ColumnStructure.returnOd` (the vapour return's size, shared by the stack and `place`). Layout table `src/test/resources/compat/layouts/layouts.txt` (the 0.5 m column's row). Tests: `EquipmentShapeCompatTest` (a column's length from the current rule), `ColumnStructureTest.theVapourReturnComesWithAKettle` (the sump steam a row over the return). Code guide: `equipment.md`. Tools: [../../tools/column-sump/](../../tools/column-sump/README.md).

## History

- 2026-10-27: references found (research subagent on opus; the stripper drawing, the Walas rule and Perry's 3 m checked here), three forms compared on NewSweep's 315 designs, the stack chosen (option A of the review). First at half metres: the full suite failed 4 tests, one of them a crowded 4 m design losing a draw because a half-metre sump moves the tray grid half a block against the cell rows; the stack now rounds to whole metres. Sump 3 m to D 1.5, 4 m for D 2 to 8, D/2 beyond (was max(2, D/2)). Refused designs 103 → 93, none newly refused; every tray nozzle moves by exactly the sump's growth. Two tests changed (review section 7). Commit `a0ab6b50` (code, tests, layout table), then the documents, tool folder and CHANGELOG `[Unreleased]`. Gates on `a0ab6b50`'s tree: `test` 1643/1643, `slowGates` 188/188, fluid GameTests 34/34 (fresh run id `sump1`). No in-game check.
- 2026-10-27: owner decisions: "Keep option A (3 to 4 m sump, wide columns unchanged). The nozzle moves on placed columns are approved." Option B dropped.

## Open items

- Merge: CHANGELOG entry is under `[Unreleased]`; proposed version 0.15.1 (patch: a layout correction); the owner chooses.
- Not looked at: a level bridle on the sump (survey section 4), a narrower bottom boot for hot residue service.
