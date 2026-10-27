# 2026-09-18-solid-phase-fluid-system

Purpose: Solid phases, slurry transport and inline filtration in the fluid network, with fixes A-H, GUI pass, filter/row-scale/pump/tank/junction/elevation/dead-head fixes

Status: Implemented 2026-09-23 (0.2.0). Index: [../INDEX.md](../INDEX.md).

## Documents

- [solid-phase-fluid-system-plan.md](solid-phase-fluid-system-plan.md)
- [SOLID_PHASE_FLUID_SYSTEM_PLAN_REVIEW.md](SOLID_PHASE_FLUID_SYSTEM_PLAN_REVIEW.md)
- [SOLID_PHASE_FLUID_SYSTEM_IMPLEMENTATION_REVIEW.md](SOLID_PHASE_FLUID_SYSTEM_IMPLEMENTATION_REVIEW.md)
- [SOLID_PHASE_GUI_PASS.md](SOLID_PHASE_GUI_PASS.md)
- [DEAD_HEADED_LINE.md](DEAD_HEADED_LINE.md)
- [screenshots/](screenshots/)

## Code areas

`science/fluid/network/` (`InlineFilter`, `SolidEventIntegrator`, `PassiveStepSolver`, `ConservativeTransport`), `science/fluid/transport/`, `runtime/fluid/`; `fluidSolverRegression`

## History

- Implemented 2026-09-23 (`440a754`..`1403eeb`, version 0.2.0)

## Notes

- Codex implementation `440a754` was reviewed as not mergeable (2026-09-22); eight diagnosis layers and fixes A-H landed on `claude/dead-headed-line` and merged with `2a7bfdd` (AGENTS.md). `FLUID_SOLVER_REGRESSION_RERECORD.md` (regression harness on the 21-component basis) landed as `fca6a15` in the same merge. Open: owner decision on HELD islands taking no events, latent velocity-clamp donor, stale island regression references (`quiet-11312`, `quiet-11324`, `cold-11312`), GUI attribution, reject-to-source for trace transfers.
