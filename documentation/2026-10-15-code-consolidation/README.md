# 2026-10-15-code-consolidation

Purpose: Code consolidation (owner: "too many class files and dead checks/tests"): dead code, checks and tests removed, single-use types nested, shared test fixtures; fluid causal modules, column research options and old proof tests removed by owner decision; formats column wire 16, checkpoint 8, fluid-9. Main 245 → 192 files, tests 1260 → 1183, results bitwise identical. Detached: `tools/v3-cold-core-benchmark/detached-2026-10-15/`, `research/2026-10-15-code-consolidation/`

Status: Implemented 2026-10-15 (0.7.2). Index: [../INDEX.md](../INDEX.md).

## Documents

- [CONSOLIDATION_REVIEW.md](CONSOLIDATION_REVIEW.md)
- [CONSOLIDATION_PLAN.md](CONSOLIDATION_PLAN.md)
- [INVENTORY_COLUMN.md](INVENTORY_COLUMN.md)
- [INVENTORY_FLUID_SHARED.md](INVENTORY_FLUID_SHARED.md)

## Code areas

`science/column/v3/**`, `science/fluid/**`, `runtime/**`, `science/thermo`, `science/material`, `client/**`, `network/**`, `build.gradle`; `test`, `gates.sh`, `runColumnGameTestServer`, `runMcpClient`

## History

- Implemented 2026-10-15 (main `26ec48de`, mod 0.7.2; full test 1183 green, G1 new baseline 111/534, G2 exact 0, junction lines unchanged, fluid GameTests 31/31, column 2/2, GUI check through the bridge)
