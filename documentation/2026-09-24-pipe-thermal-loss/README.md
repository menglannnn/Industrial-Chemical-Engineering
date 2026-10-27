# 2026-09-24-pipe-thermal-loss

Purpose: Zero-inventory pipe heat-loss assessment and 561-case experiment

Status: Concluded 2026-09-24. Index: [../INDEX.md](../INDEX.md).

## Documents

- [PIPE_THERMAL_LOSS_REVIEW.md](PIPE_THERMAL_LOSS_REVIEW.md)
- [PIPE_THERMAL_LOSS_EXPERIMENT_PLAN.md](PIPE_THERMAL_LOSS_EXPERIMENT_PLAN.md)
- [PIPE_THERMAL_LOSS_EXPERIMENT_REVIEW.md](PIPE_THERMAL_LOSS_EXPERIMENT_REVIEW.md)

## Code areas

`codex/pipe-thermal-loss-study`; `PipeHeatExchange`; `fluidScienceTest`; `tools/pipe-thermal-loss/`

## History

- Concluded 2026-09-24 (research; unmerged experimental kernel)

## Notes

- experiment concluded: fixed 17-point enthalpy curve passed 561 thermal cases (max 7.1%); one hydraulic correction failed 84/561 against coupled axial reference. 188 science tests pass. Prototype remains uncommitted on `codex/pipe-thermal-loss-study`; no runtime integration/merge. Canonical harness and patch: `tools/pipe-thermal-loss/`; data: `research/2026-09-24-pipe-thermal-loss/`.
