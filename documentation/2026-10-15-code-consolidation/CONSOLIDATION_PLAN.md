# Code consolidation: plan (2026-10-15)

Owner request: "there are too many class files and dead checks/tests". Base: `009cdf5c` (branch `claude/code-consolidation`). Three read-only inventories (column, fluid, shared) found the items below; ids are theirs (C/K/D/M = column, F/C/T/M = fluid, S/G = shared). This file is the brief for the implementation threads.

## Rules for every item

1. Results stay bitwise identical. No change to arithmetic order. Proof: fluid = `fluidSolverRegression -PfluidRegressionMode=exact` (chain-100, max deviation 0) and the 33 junction lines; column = a throw-away digest probe (raw bits of the results of a fixed set of solves, recorded on the base and after the change; the probe is not committed).
2. Formats change only where this plan says so. Nesting or renaming a class must not change a codec key, NBT key, Gson field name or enum ordinal. Keep `FlowControl.Mode` and `V3PumparoundSpec.Split` order. Keep `ProcessSolveServices$RequestContext` (reached by `Class.forName` from a GameTest). Keep the private codec method names that tests reach by reflection, or change the tests to direct calls in the same step.
3. No compatibility (AGENTS.md): compatibility shims, absent-field defaults and legacy readers are dead. Validation of data read from disk, network or datapack stays.
4. A test is removed only when it is dead: it tests test-only code, it repeats another test exactly, it pins behaviour that no longer exists, or no documented Gradle task runs it. A merge keeps the union of the assertions. Keep the G3 class names: PassiveStepSolverTest, PumpJunctionStartupTest, FilterBlockLineIslandTest, DeadHeadedLineIslandTest, NetworkRegimeTest, PhysicalFluidTopologyTest, PipePresentationTest. Keep FluidSolverRegressionTest, MixedGasJunctionTransientTest, VirtualJunctionHoldupTest, McpGameplayRegressionTest.
5. Detached harnesses go to the main checkout's `tools/<folder>/` with a README section: purpose, how to run, the commit that removed them, and a re-attach patch (AGENTS.md "Test classes and tools after a batch").
6. One commit per item group, message `consolidate: <ids> <what>`. Commit on your branch. No push, no merge, no version bump, no CHANGELOG, INDEX or code-guide edit (the orchestrator does those).

## Ownership (two threads run at the same time; a thread edits only its own files)

| Thread | Owns |
|---|---|
| A (column) | `science/column/v3/**`; `world/level/block/entity/ColumnCalculatorV3*`, `ColumnPresentation`, `ColumnInputPreset`, `V3*Codec`; `network/ColumnV3Network`, `network/ProcessSolveCoordinator`; `client/**` (both screens, drafts, `gui/common`); `assets/createcheme/lang/en_us.json`; the record `ProcessSolveServices.V3ColumnCommand` only; tests under `science/column/**`, `client/**`, `network/**`; column GameTests |
| B (fluid, shared science, build) | `science/fluid/**`, `science/thermo/**`, `science/material/**`, `science/math/**`, `material/**`; `runtime/**` except the record `V3ColumnCommand`; `network/FluidNetwork*`; `world/inventory/**`, `world/item/**`, `world/level/block/**` fluid devices; `registry/**`; `CreateChemE.java`; `build.gradle`; tests under `science/fluid/**`, `science/thermo/**`, `science/material/**`, `runtime/**`, `fluid/**`; fluid GameTests; test resources outside `v3-benchmarks/` |

B does not rename `PengRobinsonKernel.Root`, `science.thermo.PhaseSupport`, `TjlReferenceExtension` or `Cdu17FixtureExtension`; A uses them. An item that needs an edit in the other thread's files is skipped and listed in the report under "cross items".

## Thread A (column) — wave 1

- Dead production: C1/S3 delete `V3ResultProvenance` + test. C2 `calculateOnSuppliedProfile`. C3, C4, S8 (column and client members). C11 move CDU fixture factories to the test tree. C7 move the finite-difference Jacobian oracle to the test tree.
- C5 + D2 + K7: replace the preconditioner framework by one private calculator method; delete `V3SequentialPreconditioner`, `V3BubblePointPreconditioner`, test-tree `V3SumRatesPreconditioner`, `V3HybridPreconditioner` and their tests; adapt the tests named in the inventory.
- Never-produced values and constant parameters: K3 `DECODE_VARIANTS`; K4 `V3ColumnInitializer.Mode` and `PHASE_AWARE_TRAFFIC`; K5/S12 four failure codes and `V3Status.IDLE`; K9 `serializedName` → `name()`/`valueOf`.
- Duplicate checks: K6 + D7 (one check per invariant, in the resolver), K8, K10, S19.
- Dead packet: S2 server preset packet and `V3State.presets`; bump the column wire schema 15 → 16.
- Shorthand constructors and overloads: C8, C10, then C6 + C9 + D8 with one shared test fixture (`V3TestInputs`) and one codec-reflection helper. Do this group last.
- Harness: D1/S20 detach `V3ColdCoreBenchmarkWorker`, its test and `v3-cold-core-v1.json` to `tools/v3-cold-core-benchmark/`.
- Tests: D3 delete; M9 one `V3ColdStartTest` (includes the real-crude sweep); M10, M11, M12/S22, M13; D9.
- Merges (production): M1, M2, M3, M4, M5, M6, M7; M8/S29/S31 drafts nested in their single user; S30 small UI types into `ProcessUi`; S35 shared screen helpers into `ProcessUi` (keep per-screen differences); S36 `V3Phase` → `PengRobinsonKernel.Root` and `V3FlashPhaseSupport` → `PhaseSupport` only if no codec writes `V3Phase` by name.
- S11: delete the 37 unused lang keys.
- Not now (owner): K1 RAMP_HANDOFF, K2 decoder lift, K11 WALLS, D5 banded-solver frozen copy, D6 catalog parity tables, M14.

## Thread B (fluid, shared, build) — wave 1

- Dead production: F3 move the format-3 reader to a test fixture loader; F4 move codec test helpers to the test tree; F5; F6; F7 drop `FluidView.hydraulicOwner`/`intervalQuality` and bump the fluid protocol fluid-8 → fluid-9; F8 rewrite tests on production entry points, then delete the test-only overloads (not `V3ColumnCommand`).
- Shared: S1(a) unused PR78 methods; S1(b) move `PengRobinson78` + `PhaseProperties` to the test tree with a private pure-component Z in `HydrocarbonModel` (copy the PR78 expressions; G2 exact must stay 0); S5; S6 (skip any accessor whose users are in thread A's tests: cross item); S7; S9; S13; S14 is thread A's; S15; S32 `PhaseRoot` → `PengRobinsonKernel.Root`; S33; S34 one JSON field reader (keep error texts that tests pin); S37 one registry class (optional, last).
- Checks: C1, C2, C3, C4, C5, C7, C8 (one source of the solid defaults, via M6), C9/S18; C6 after M10 only.
- Tests: T1 shared rigs (`CertifyingRig`, `BlockLineHarness`, extend `EventPathWorld` and `FluidTestSupport`); T2; T3 with its Gradle switches and the `FLUID_REGRESSION_SNAPSHOT` alias; T4/S23; T5; T6/S27 drop unread report writes; S24; S25; S28; M13 one `HoldClassWorldsTest`; M14; M15.
- Merges (production): M1-M10; M12 optional, last.
- Build: G1 dead `gameTestServer` run and the `mod_id` namespace lines; G2 dead `runData` and generated-resources lines; G4 stale comments; G3 limit the jar dependency to the tasks that read `fluid.modJar`.
- Not now (owner): F1 causal modules, F2/S4 analytic derivatives, S10 appearance data, T7/S26 reference TSV files and examples scripts.

## Checks

- Per step: compile the four source sets, then the unit tests of the touched packages.
- Thread A at the end: full column, client and network tests, `v3HollandBenchmark`, the digest probe against the base, `runColumnGameTestServer`.
- Thread B at the end: G1 suites, G2 exact once, G3, `runFluidGameTestServer` once.
- After both merge: one full `test`, one `gates.sh` run, column GameTests (orchestrator lane). G1 class and test counts get a new baseline; the 33 junction lines and chain-100 stay byte-identical.

## Owner decisions (2026-10-15, after wave 1 started)

- Remove: F1 fluid causal modules (checkpoint format 7 → 8, fluid GameTests 33 → 31, benchmark profile "module" goes), K1 RAMP_HANDOFF + K2 decoder lift + K11 WALLS correction, F2 analytic derivatives.
- Keep: S10 material appearance (still required in the datapack).
- Remove the old proofs: D5 (banded-solver frozen copy, with S17), D6 (Java package tables and V3CatalogParityTest).
- Move T7/S26: the 16 TSV files in `src/test/resources/fluid/reference` go to `research/2026-10-15-code-consolidation/fluid-reference-tsv/` with a README that names the `examples/` scripts that made them. The scripts stay in `examples/`.
- G1 class and test counts get a new baseline. The 33 junction lines and chain-100 stay identical.
- 009cdf5c merges to main together with the consolidation, after the gates.

## Wave 2 (after the thread's wave-1 list)

- Thread A: K1, K2, K11 (with V3LearnedRecoveryTest and V3DecoderPresenceFloorTest parts that pin them), D5 + S17, D6.
- Thread B: F2 (rewrite FluidNitrogenCryogenicTest:67-105 without the derivatives), T7/S26 move, then F1 last (bump the checkpoint format to 8; remove FluidModuleGameTests and the module part of FluidPropertyReloadGameTests; remove the benchmark "module" profile; keep G5 at the remaining 31).
