# 2026-09-10-v4-neural-initializer

Purpose: Neural (Transformer) initializer for V3: survey, contract, review, R1-R6 follow-ups, promotion of F0, benchmark cleanup, LNN-only gap, training guide

Status: Implemented 2026-09-14. Index: [../INDEX.md](../INDEX.md).

## Documents

- [V4_TRANSFORMER_INITIALIZER_REVIEW.md](V4_TRANSFORMER_INITIALIZER_REVIEW.md)
- [V4_IMPLEMENTATION_RESULTS.md](V4_IMPLEMENTATION_RESULTS.md)
- [V4_LNN_ONLY_GAP_ANALYSIS.md](V4_LNN_ONLY_GAP_ANALYSIS.md)
- [V4_BENCHMARK_POPULATION_REVIEW.md](V4_BENCHMARK_POPULATION_REVIEW.md)
- [V4_TRANSFORMER_TRAINING_GUIDE.md](V4_TRANSFORMER_TRAINING_GUIDE.md)

## Code areas

`science/column/v3/V3AnchorTransformerInitializer`, `V3NeuralRegistry`, `V3LiquidSupplyScreen`; `src/main/resources/data/createcheme/neural/`; tools/development.gradle neural tasks

## History

- Implemented 2026-09-14 (`main` fast-forwarded `f01a28e` -> `e66b374`)

## Notes

- open owner decisions: enable `Recovery.RAMP_HANDOFF` at 2,500 ms and/or `CandidateRule.DECODE_VARIANTS`; in-game GUI check not done. The long handoff `BRANCH_HANDOFF.md` stays in the repository root because `tools/capacity-followup/build_branch_handoff.py` writes it there.
