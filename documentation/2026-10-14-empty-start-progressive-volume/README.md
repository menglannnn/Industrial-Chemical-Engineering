# 2026-10-14-empty-start-progressive-volume

Purpose: A placed reservoir or pipe holds nothing; a vessel's volume grows with its contents until taut (owner direction D52 of the phase-ports batch): the investigation of the form and the solver changes it needs, physics first (a vessel that holds nothing is a boundary until it is taut: SLACK/TAUT modes in the active-set loop, closure at fixed pressure, the crossing as a step cut; empty junction buffers with the fixed point as the cold start; runs massless first, fill length later)

Status: Implemented 2026-10-14 (0.7.0, inside the phase-ports merge). Index: [../INDEX.md](../INDEX.md).

## Documents

- [EMPTY_START_PLAN.md](EMPTY_START_PLAN.md)
- [EMPTY_START_VS_CHAIN_REVIEW.md](EMPTY_START_VS_CHAIN_REVIEW.md)

## Code areas

`PassiveStepSolver` (vessel modes, cold start), `InventoryEquilibrium` / `PhaseLayout` (fixed-pressure closure), `PassiveIntervalSolver` (the crossing as a transition), `NetworkReduction` (chain and switch retired), `PhysicalRegistry` (no initial charge), checkpoint and snapshot codecs, tank GUI

## History

- In progress since 2026-10-14 (owner decision D54: the plan taken
- thread t24 on `claude/empty-start`: `cc001df3` `7e8b26fb` the bag with a stiff wall, `8e3d1fbf` `a5d9c91e` nothing at the mint
- campaign 882 / 943, gates not green, stop points S1-S6 in the batch review
- S1-S3 taken, S4 decided as D55: `340c782a` `ef987556` the junction's start from its suppliers, an island holding no stock at rest, GameTests restated
- campaign 933 / 970, gates green
- review section (its section 10), A310-A333 and the holds table in `2026-09-26-phase-ports-and-compressor/`
- the evaluation against the chain EMPTY_START_VS_CHAIN_REVIEW.md written 2026-10-14, D56
- merged into `claude/phase-ports-compressor` 2026-10-14, D57)

## Notes

- 2026-10-26: status corrected from "In progress": merged into the phase-ports branch (D57) and reached `main` in 0.7.0 with the squash `1dba69b3` (CHANGELOG 0.7.0, D55).
