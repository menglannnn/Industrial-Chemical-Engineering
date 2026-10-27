# 2026-09-07-v3-trace-scaling-and-truncation

Purpose: Trace-spike singularity root cause, local-throughput row scaling, per-phase truncation (T1-T3), convergence closure knob (T5), band relaxation (T4)

Status: Implemented 2026-09-08. Index: [../INDEX.md](../INDEX.md).

## Documents

- [V3_TJL19_WET_DRAW_STALL_ROOT_CAUSE.md](V3_TJL19_WET_DRAW_STALL_ROOT_CAUSE.md)
- [V3_TRACE_BALANCE_REMEDIES_REVIEW.md](V3_TRACE_BALANCE_REMEDIES_REVIEW.md)
- [V3_TRUNCATION_EVALUATION.md](V3_TRUNCATION_EVALUATION.md)
- [V3_PHASE_SPECIFIC_TRUNCATION_PLAN.md](V3_PHASE_SPECIFIC_TRUNCATION_PLAN.md)
- [V3_PHASE_SPECIFIC_TRUNCATION_REVIEW.md](V3_PHASE_SPECIFIC_TRUNCATION_REVIEW.md)
- [V3_CLOSURE_AND_BAND_REVIEW.md](V3_CLOSURE_AND_BAND_REVIEW.md)

## Code areas

`science/column/v3/V3TruncationSupport`; config `columnV3ConvergenceClosurePercent`

## History

- Implemented 2026-09-08 (landed 2026-09-07 on the handoff branch, `main` via `f4e600a`)
