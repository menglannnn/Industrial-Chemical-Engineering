# 2026-09-24-column-gui

Purpose: Editable scale-3 column GUI, independent JSON presets, total tray numbering/pots; shared generator composition, C/K, tank/pipe tables, linked flow units and cursor recovery; fixed pipe roughness; focused text input and shared fuzzy component dropdown; requested cached fluid snapshot on opening; automatic pipe operating panel and all junction connections

Status: Implemented 2026-09-24 (0.5.0). Index: [../INDEX.md](../INDEX.md).

## Documents

- [COLUMN_GUI_REVIEW.md](COLUMN_GUI_REVIEW.md)
- [COLUMN_GUI_PLAN.md](COLUMN_GUI_PLAN.md)
- [COLUMN_GUI_IMPLEMENTATION_REVIEW.md](COLUMN_GUI_IMPLEMENTATION_REVIEW.md)
- [COLUMN_GUI_INTERACTIVE_REVIEW.md](COLUMN_GUI_INTERACTIVE_REVIEW.md)
- [COLUMN_GUI_SCALE3_REVIEW.md](COLUMN_GUI_SCALE3_REVIEW.md)
- [SHARED_PROCESS_GUI_PLAN.md](SHARED_PROCESS_GUI_PLAN.md)
- [SHARED_PROCESS_GUI_REVIEW.md](SHARED_PROCESS_GUI_REVIEW.md)
- [CLIENT_PRESET_FORMAT.md](CLIENT_PRESET_FORMAT.md)
- [COMPONENT_DROPDOWN_PLAN.md](COMPONENT_DROPDOWN_PLAN.md)
- [COMPONENT_DROPDOWN_REVIEW.md](COMPONENT_DROPDOWN_REVIEW.md)
- [OPENING_SNAPSHOT_PLAN.md](OPENING_SNAPSHOT_PLAN.md)
- [OPENING_SNAPSHOT_REVIEW.md](OPENING_SNAPSHOT_REVIEW.md)
- [PIPE_INSPECTION_PLAN.md](PIPE_INSPECTION_PLAN.md)
- [PIPE_INSPECTION_REVIEW.md](PIPE_INSPECTION_REVIEW.md)
- [MERGE_PREPARATION_PLAN.md](MERGE_PREPARATION_PLAN.md)
- [MERGE_READINESS_REVIEW.md](MERGE_READINESS_REVIEW.md)
- [BULK_SPEED_REVIEW.md](BULK_SPEED_REVIEW.md)
- [screenshots/](screenshots/)

## Code areas

ColumnCalculatorV3Screen, FluidDeviceScreen, client/gui/common, V3ClientPresets, FluidNetwork; test, runMcpClient

## History

- Implemented 2026-09-24 (main 9674bf1, version 0.5.0; 1,093 tests passed before final five-second speed follow-up 9817ecc, which owner requested merging without verification; client left open, restart needed)
