# 2026-09-17-crude-regrouping

Purpose: Heavy-cut regrouping to `crude_pc01..12`, shared Dalia viscosity, regrouped initializer; data-generation review and hybrid seed-preparation study

Status: Implemented 2026-09-18. Index: [../INDEX.md](../INDEX.md).

## Documents

- [CRUDE_REGROUPING_PLAN_REVIEW.md](CRUDE_REGROUPING_PLAN_REVIEW.md)
- [CRUDE_REGROUPING_DATA_GENERATION_REVIEW.md](CRUDE_REGROUPING_DATA_GENERATION_REVIEW.md)
- [V4_HYBRID_INITIALIZER_REVIEW.md](V4_HYBRID_INITIALIZER_REVIEW.md)

## Code areas

`science/material/`, `src/main/resources/data/createcheme/materials/`, `MATERIALS.md`; research in `research/crude-regrouping/`, `research/convergence-review-2026-09-18/`

## History

- Implemented 2026-09-18 (`main` fast-forwarded `c5c8af3` -> `4e84f0e`)
- follow-ups (retraining on the corrected design, box/hull domain, seed lift rule) planned with Codex

## Notes

- owner decisions 1-4 of 2026-09-18 (Codex retrains on the corrected global-composition design; sidecar box/hull domain; solver verdict for the zero-boil-up family; labels from neural-first solves for the heat-gate class) are not implemented. The seed lift rule of `V4_HYBRID_INITIALIZER_REVIEW.md` still needs qualification on a fresh holdout.
