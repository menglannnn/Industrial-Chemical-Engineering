# 2026-09-01-v3-holland-benchmark

Purpose: Holland Example 3-2 accuracy benchmark and in-game preset

Status: Implemented 2026-09-01. Index: [../INDEX.md](../INDEX.md).

## Documents

- [V3_HOLLAND_BENCHMARK_CASE.md](V3_HOLLAND_BENCHMARK_CASE.md)
- [sources/holland-example-3-2-ocr-excerpts.txt](sources/holland-example-3-2-ocr-excerpts.txt)

## Code areas

`science/column/v3/V3HollandExample32`, `IndependentHollandMeshOracle`; `v3HollandBenchmark`

## History

- Implemented 2026-09-01 (via PR #1 `bdfeff1`)

## Notes

- the strict published-number gate fails on source-table errata (pinned as `strictPublishedAccuracyAccepted=false`); a cold start at reflux 1e4 still fails (open probe).
