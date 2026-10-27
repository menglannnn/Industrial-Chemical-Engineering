# 2026-09-15-fluid-network

Purpose: Simulation engine and fluid network: design, work order, Codex implementation, reviews, solver optimization WP1-WP7, cleanup

Status: Implemented 2026-09-17. Index: [../INDEX.md](../INDEX.md).

## Documents

- [SIMULATION_ENGINE_AND_FLUID_NETWORK_DESIGN.md](SIMULATION_ENGINE_AND_FLUID_NETWORK_DESIGN.md)
- `..._WORK_ORDER.md`
- `..._DESIGN_REVIEW.md`
- [FLUID_NETWORK_IMPLEMENTATION_REVIEW.md](FLUID_NETWORK_IMPLEMENTATION_REVIEW.md)
- [FLUID_SOLVER_OPTIMIZATION_REVIEW.md](FLUID_SOLVER_OPTIMIZATION_REVIEW.md)
- [FLUID_NETWORK_ACCEPTANCE.md](FLUID_NETWORK_ACCEPTANCE.md)

## Code areas

`science/fluid/`, `runtime/fluid/`, `science/thermo/PengRobinsonKernel`; `fluidSolverRegression`, `fluidServerBenchmark`, `fluidStressProfile`, `fluidModuleProfile`

## History

- Implemented 2026-09-17 (`main` fast-forwarded `e660097` -> `af65243`, cleanup to `6deb609`)

## Notes

- design residuals R1-R9 are recorded in the design review; the remaining cost is transients only (ratio-keyed refresh rule, block LU on the cold path, analytic node interface). The Luna/Sol review and acceptance files are Codex working documents untracked on 2026-09-17 (`5e47076`).
