# Test review: keep what is necessary, archive the rest (2026-10-21)

Owner request: "review all the tests, only keep the ones necessary and archive the rest". Tree `main` 138f4993 (0.11.0).
Criteria: [CRITERIA.md](CRITERIA.md) (keep = the only guard of conservation, a format, an owner rule, a reference,
determinism or a core contract). Six Opus reviews, one per area: [parts/](parts/). Timings: full `test` at 138f4993.

## Baseline
- Unit `test`: 281 classes, 1 572 tests, 262 s of test time (build 4 min 26 s). GameTests: G5 31, column 2, benchmark 1.
- The time is in kept gates: HoldClassWorldsTest 53 s, RegroupedCrudeTest 32 s, ProcessUnitTest 25 s, the V3 column
  calculators ~80 s. No reviewer found a slow class that is not the only guard of something.

## Proposal
| Area | Classes kept / trimmed / archived | Tests archived |
|---|---|---|
| Column V3 (75) | 74 / 1 / 0 | 2 (V3FlashTruncationColumnTest) |
| Runtime (71) | 71 / 0 / 0 | 0 |
| Fluid science (64) | 61 / 2 / 1 | 2 + 5 trimmed |
| Thermo (37) | 31 / 2 / 4 | 20 + 8 trimmed |
| Material, equipment, client (37) | 31 / 0 / 6 | 22 (2 are benchmarks outside `test`) |
| GameTests G5 (31) | 19 kept | 12 |

- Unit archive: Cdu17TestCatalogTest, ComponentSearchTest, TextInputPriorityTest, V3PumparoundDraftTest,
  TranslatedPengRobinsonPairInteractionsTest, G3F4, G3F6, G5F5 (+ its 3 holdout TSVs), HelmholtzReferenceTest;
  benchmarks FluidNetworkBenchmarkTest, FluidPropertyBenchmarkTest (+ their Gradle tasks); helper LegacyLiquidPath.
- Trims: V3FlashTruncationColumnTest, DirectLiquidContinuityTest (retired 2 MPa path), SolidChainTransportTest (wall
  clock, counters), PengRobinson78Test (5 oracle-only methods), CrystalPhaseEvaluatorTest (3 duplicate figures).
- Effect: about 60 unit tests and under 2 s; G5 31 → 19 (part of its 21 s).
- Reversed by the orchestrator (doubt rule): ScheduledTransferTest KEEP (three kept tests take it as their energy
  input); GameTests heatedCrudePumpStartsIntoNitrogenReservoir and aPumpOnAGasTankIsRefused… KEEP (no other guard).
- Fix, not archive: PhysicalRegistryTest.placementCostIsFlatInWorldSize asserts wall-time medians (keep its count).

## Finding
Under the "only guard" bar the suite is nearly all necessary: the archive removes ~4 % of tests and ~1 % of time.
A shorter everyday run needs a different lever than archiving: a split of the slow gates into a merge-time task.

## Done (owner approval 2026-10-21: "apply as proposed"; slow gates split: "yes")
- Commit `d13e72d1` on `claude/test-review` (parent `138f4993`): 18 files removed, 16 edited (15 trimmed methods, dead
  helpers `G5Support.gao/sampson`, two `SolidCarbonDioxideReferences` uncertainties, stale class comments).
- Archive: `tools/test-archive-2026-10-21/` (32 source copies, `reattach-src.patch` checked with `git apply --check`,
  `benchmark-tasks.gradle`, README). `tools/INDEX.md` row.
- Slow gates: 18 classes of 3 s or more in `slowGateClasses` (build.gradle); `test` excludes them, `slowGates` runs them,
  `check` depends on it; `lane.sh` step `test` runs `test slowGates` and collects both result folders.
- Gates, once: everyday `test` 1 331 tests in 45 s (was 4 min 26 s for 1 572); `slowGates` 184 tests in 201 s;
  1 515 = 1 572 − 57 archived; 0 failures. Chain-100 exact 0. Fluid GameTests 19/19.
- Code guide: verification (task table, GameTest list, gate terms), client-gui, equilibrium-engine, fluid-runtime,
  fluid-science, thermo-materials no longer name archived tests; snapshots `f91c992d`. Merged as 0.11.1 (`f91c992d`,
  README fix `ef4e6581`), fast-forward, not pushed.
