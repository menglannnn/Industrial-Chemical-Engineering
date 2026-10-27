# 2026-08-31-v3-stage-trace-truncation

Purpose: In-column stage-trace truncation for the V3 column, and the 50-55 kPa continuation wall

Status: Implemented 2026-09-01. Index: [../INDEX.md](../INDEX.md).

## Documents

- [V3_STAGE_TRACE_TRUNCATION_PLAN.md](V3_STAGE_TRACE_TRUNCATION_PLAN.md)
- [V3_55KPA_WALL_ACTION_PLAN.md](V3_55KPA_WALL_ACTION_PLAN.md)
- [archive/v3-mask-refresh-20260831-222632/](archive/v3-mask-refresh-20260831-222632/)

## Code areas

`science/column/v3/V3TruncationSupport`, `V3TruncationFallback`; `v3LowPressureProbe` (tools/development.gradle)

## History

- Implemented 2026-09-01 (truncation, via PR #1 `bdfeff1`). 55 kPa remask route abandoned 2026-08-31 after a negative P0 probe

## Notes

- the P0 remask probe was negative (shared 3.7e-4 energy plateau) and P0b was never run; truncation work continued as the per-phase support of 2026-09-07. The earlier low-pressure plans (`V3_LOW_PRESSURE_CORRECTNESS_PLAN.md`, `..._V2.md`), the Chinese pipeline guides (`V3_THERMO_DATA_PIPELINE.md`, `V3_COLUMN_DATA_PIPELINE.md`), `NRTL_GAMMA_PHI_MATERIAL_PROPERTIES.md` and `V3_MATERIAL_PROPERTIES_PLAN.md` (2026-08-28 to 2026-08-30) are named in the agent memory but were not found in this checkout, its worktrees, the Codex worktrees or git history.
