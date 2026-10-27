# 2026-09-18-tray-pressure-drop

Purpose: Flow-dependent sieve-tray pressure drop with a player column diameter; bounded solve path fix

Status: Implemented 2026-09-22. Index: [../INDEX.md](../INDEX.md).

## Documents

- [TRAY_PRESSURE_METHOD_REVIEW.md](TRAY_PRESSURE_METHOD_REVIEW.md)
- [TRAY_PRESSURE_IMPLEMENTATION_NOTES.md](TRAY_PRESSURE_IMPLEMENTATION_NOTES.md)
- [handoff-bounded-solve-path/](handoff-bounded-solve-path/)

## Code areas

`science/column/v3/V3TrayHydraulics`, `V3TrayHydraulicsSummary`; research in `research/tray-pressure-method/`

## History

- Implemented 2026-09-22 (`bae4737` + `51442f7`, merge `3c27271`)

## Notes

- in-game GUI check and accepted-profile persistence across re-solves are still open. `handoff-bounded-solve-path/` is the patch and test that became `51442f7`.
