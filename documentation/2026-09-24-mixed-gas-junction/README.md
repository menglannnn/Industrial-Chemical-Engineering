# 2026-09-24-mixed-gas-junction

Purpose: Mixed-gas junction startup and transient investigation, then the productized fix: backward-Euler transient basis with owned junction holdup, merged REST/STEADY certificate (checkpoint format 5), suction-density pump rule

Status: Implemented 2026-09-26 (0.6.0). Index: [../INDEX.md](../INDEX.md).

## Documents

- [HANDOFF.md](HANDOFF.md)
- [INVESTIGATION_PLAN.md](INVESTIGATION_PLAN.md)
- [JUNCTION_REVIEW.md](JUNCTION_REVIEW.md)
- [JUNCTION_FIX_PLAN.md](JUNCTION_FIX_PLAN.md)
- [BE_INTEGRATOR_PLAN.md](BE_INTEGRATOR_PLAN.md)
- [DECISION_LOG.md](DECISION_LOG.md)
- [HANDOFF_REVIEW.md](HANDOFF_REVIEW.md) (sections 6-9)

## Code areas

`PassiveIntervalSolver`, `PassiveStepSolver`, `PassiveNetwork`, `ConservativeTransport`, `IslandCertificate`, `IslandCoordinator`, `FluidCheckpointCodec`; tools/junction-holdup-prototype/, tools/pipe-junction-probe/; `fluidSolverRegression`, `runFluidGameTestServer`

## History

- Implemented 2026-09-26 (0.6.0; investigation concluded 2026-09-25; WP0-WP5 on `claude/charming-elion-1170cc`: `3dba4b0`, `640d87b`, `b6422e3`, `618ea63`, `5b708fa`, `daa1ccc`, `c7a7dc2`, `b5ed407`; gates after cleanup: fluid suites 406/406, exact regression 0.000e+00, 38/38 adjacent, mixed-gas 32/32 static and 12/12 transients at 286 Newton solves; open owner decisions D14 liquid holdup sizing and D16 roundoff-flow status; in-game mixed-gas check partial)
