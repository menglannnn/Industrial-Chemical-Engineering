# Test archive 2026-10-21

Tests and test helpers removed from the tracked tree by the test review (batch `documentation/2026-10-21-test-review/`,
`TEST_REVIEW.md`, criteria in `CRITERIA.md`, per-area tables in `parts/`). Each item failed the bar "the only guard of
conservation, a format, an owner rule, a reference, determinism or a core contract": a probe or report, a duplicate of a
kept test, an implementation-detail pin, a timing assertion, or a legacy path.

Removing commit: `d13e72d1` (branch `claude/test-review`, parent `138f4993` = 0.11.0).

## Contents

- `sources/`: every file the commit deleted or changed under `src/`, as it was at `d13e72d1^` (32 files: 17 deleted
  test classes, helpers and resources; 15 files with methods trimmed or helpers dropped).
- `reattach-src.patch`: `git diff d13e72d1 d13e72d1^ -- src`; restores all of it.
- `benchmark-tasks.gradle`: the `fluidPropertyBenchmark` / `fluidNetworkBenchmark` task block removed from
  `build.gradle` (it sat before `def fluidBenchmarkRunId`).

## Re-attach

From a worktree root at `d13e72d1` or a descendant that has not touched these files:

    git apply --check <this folder>/reattach-src.patch && git apply <this folder>/reattach-src.patch

To bring back one class only, copy it from `sources/` (or `git show d13e72d1^:<path>`); the two agree. A benchmark also
needs its block from `benchmark-tasks.gradle` pasted back into `build.gradle`. A re-attached GameTest class registers
itself through `@GameTestHolder("icheme_fluid_test")` and raises the G5 count above 19.

## What was archived

- Unit classes: `Cdu17TestCatalogTest` (a test fixture), `ComponentSearchTest`, `TextInputPriorityTest` (GUI-only),
  `V3PumparoundDraftTest` (duplicate), `TranslatedPengRobinsonPairInteractionsTest` (duplicate of
  `PengRobinsonKernelPairInteractionsTest` and G3F8), G3F4 (duplicate of `SpineNetworkPathTest` /
  `DirectLiquidContinuityTest`), G3F6 and G5F5 (probe and report only; with G5F5 its Gao 2012, Sampson 2023 and Kurata
  1971 tables and `G5Support.gao()` / `sampson()`), `HelmholtzReferenceTest` (duplicate of the parity test).
- Benchmarks: `FluidNetworkBenchmarkTest`, `FluidPropertyBenchmarkTest` and their Gradle tasks (timing, not gates).
- Trimmed methods: `V3FlashTruncationColumnTest` (2), `DirectLiquidContinuityTest` (2, the retired 2 MPa liquid path,
  with the helper `LegacyLiquidPath`), `SolidChainTransportTest` (3: wall clock, substep and workspace counters),
  `PengRobinson78Test` (5 oracle-only), `CrystalPhaseEvaluatorTest` (3 oracle figures, held by
  `JaegerSpanGibbsFunctionTest`); `SolidCarbonDioxideReferences` lost its two then unused uncertainty functions.
- GameTests (12 of 31): `FluidHarnessGameTests` (5), `FluidPacketGameTests` (1), `FluidPhasePortGameTests` (1, the
  compressor), `FluidPropertyReloadGameTests` (1), `FluidPumpStartupGameTests` (1, default water),
  `FluidPlacementGameTests`, `FluidPumpedFillGameTests`, `FluidTopologyGameTests` (whole); each names the unit test that
  holds its contract in `parts/gametests.md`.
- Edited, not archived: `PhysicalRegistryTest.placementCostIsFlatInWorldSize` keeps its count and batch checks without
  its wall-time medians (the old version is in `sources/`).

## Index record (moved from tools/INDEX.md on 2026-10-26)

Purpose: Tests and helpers archived by the test review: 9 unit classes, 15 trimmed methods, the two benchmark tests and their Gradle tasks, 12 fluid GameTests; source copies at the removing commit and a re-attach patch

Batch: 2026-10-21-test-review

Status: Archived 2026-10-21 (`d13e72d1`). Index: [../INDEX.md](../INDEX.md).

### Entry points

`README.md`, `reattach-src.patch`, `sources/`, `benchmark-tasks.gradle`

### History

- Archived (removed by `d13e72d1`)
