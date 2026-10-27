# 2026-09-05-v3-solver-core-review

Purpose: Review of the V3 numerical core, 64-case cold-core benchmark, first core fixes, redesign proposals

Status: Implemented 2026-09-05. Index: [../INDEX.md](../INDEX.md).

## Documents

- [V3_SOLVER_DESIGN_REVIEW.md](V3_SOLVER_DESIGN_REVIEW.md)
- [V3_MAIN_SOLVER_REVIEW.md](V3_MAIN_SOLVER_REVIEW.md)
- [V3_CORE_FIXES_RESULTS.md](V3_CORE_FIXES_RESULTS.md)
- [V3_COLD_CORE_BENCHMARK_PLAN.md](V3_COLD_CORE_BENCHMARK_PLAN.md)
- [V3_COLD_CORE_BENCHMARK_RESULTS.md](V3_COLD_CORE_BENCHMARK_RESULTS.md)
- [V3_FUNDAMENTAL_SOLVER_REDESIGN.md](V3_FUNDAMENTAL_SOLVER_REDESIGN.md)

## Code areas

`science/column/v3/V3SimultaneousColumnSolver`, `linalg/V3BandedPivotedSolver`; `tools/v3-cold-core-benchmark/`

## History

- Implemented 2026-09-05 (core fixes `0f94ffe`, benchmark tooling `6c7d446`)
- replacement-core redesign proposed and not pursued

## Notes

- the redesign notes recommend replacing the numerical core; later work stayed incremental (2026-09-06 to 2026-09-09), so treat the redesign as not pursued.
