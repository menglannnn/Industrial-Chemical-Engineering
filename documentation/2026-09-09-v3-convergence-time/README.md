# 2026-09-09-v3-convergence-time

Purpose: Measured V3 cost structure and the optimizations (flat band LU, decode-free probes, doubling steam ramp, analytic PR78 derivatives, trace-seed cap)

Status: Implemented 2026-09-09. Index: [../INDEX.md](../INDEX.md).

## Documents

- [V3_CONVERGENCE_TIME_OPTIMIZATION_PROPOSALS.md](V3_CONVERGENCE_TIME_OPTIMIZATION_PROPOSALS.md)
- [V3_OPTIMIZED_SOLVER_ROBUSTNESS_REVIEW.md](V3_OPTIMIZED_SOLVER_ROBUSTNESS_REVIEW.md)
- [V3_OPTIMIZED_SOLVER_REGRESSION_FIX_REVIEW.md](V3_OPTIMIZED_SOLVER_REGRESSION_FIX_REVIEW.md)

## Code areas

`science/column/v3/linalg/`, `V3MeshResidualEvaluator`, `thermo/` derivatives

## History

- Implemented 2026-09-09 (`6a5ab33`..`db46e88`)

## Notes

- `convergence-time-optimization-2026-09-09.md` is an identical copy of `V3_CONVERGENCE_TIME_OPTIMIZATION_PROPOSALS.md` kept under its former `research/` name; it can be deleted. Open: certificate from the analytic block Jacobian, the six plateau iterations on the 30-stage rung, the `capOversizedPoint` Javadoc.
