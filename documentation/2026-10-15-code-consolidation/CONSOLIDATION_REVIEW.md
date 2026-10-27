# Code consolidation: review (2026-10-15)

Owner request: "there are too many class files and dead checks/tests". Branch `claude/code-consolidation` on `009cdf5c`, 39 commits to `c083a302`, not merged. Plan and evidence: `CONSOLIDATION_PLAN.md`, `INVENTORY_COLUMN.md`, `INVENTORY_FLUID_SHARED.md`. Work: three read-only inventory agents, then Opus threads A (column, own worktree), B (fluid, shared, build), C (cross items, final checks), D (code guide).

## Result

| | 009cdf5c | c083a302 |
|---|---|---|
| Main Java files / lines | 245 / 48,679 | 192 / 45,497 |
| Test Java files / lines | 314 / 40,084 | 235 / 35,757 |
| fluidGameTest files / required GameTests | 19 / 33 | 18 / 31 |
| Full `test` | 1260 tests | 1183 tests, 0 failures |

- Removed (dead): unused members and classes, never-produced status and failure values, constant parameters, checks that repeat an upstream check, absent-field shorthand constructors, tests of test-only code, the dead `gameTestServer` and `runData` runs.
- Removed (owner decisions): fluid causal modules, RAMP_HANDOFF, decoder lift, WALLS, the analytic PR derivatives, the frozen banded-solver copy, the Java package tables (their data is now test JSON).
- Merged: single-use types nested in their owners; small tests merged into the test of the same class (union of assertions); shared fixtures `V3TestInputs`, `ColumnCodecs`, `CertifyingRig`, `HoldClassWorldsTest` (20 hold worlds).
- Formats (no compatibility, AGENTS.md): column wire schema 16, fluid checkpoint format 8 (format 7 refused), fluid protocol fluid-9.
- Kept on purpose: material appearance (owner), `ScheduledTransfer` (the junction-line test feeds through it), the 12-argument `V3ColumnInput` (Holland example).

## Checks (one final run each, thread C, on 7e677d49; c083a302 changes comments only)

| Check | Result |
|---|---|
| Compile, four source sets | pass |
| Full `test` | 209 classes, 1183 tests, 0 failures |
| G1 (`gates.sh`) | 111 classes, 534 tests (new baseline, owner accepted), 0 failures; 33 junction lines, 0 diff; MIXED_GAS_COST newtonSolves 339 |
| G2 exact chain-100 | 0.000e+00 on moles, temperature, phase fraction, flow |
| G3 | 7 classes, 48 tests, 0 failures |
| G5 fluid GameTests | 31/31 |
| Column GameTests, Holland | 2/2, 2/2 |
| Column digest (raw bits of 14-15 solves) | equal to the base after every result-touching group |
| GUI (runMcpClient, bridge, no desktop input) | all labels show text; tray dP and presets round-trip 0.7; solve fills all tabs; fluid reservoir STEADY, pump CLOSED; `screenshots/` |

## Detached material

- `tools/v3-cold-core-benchmark/detached-2026-10-15/`: the cold-core worker, its test and fixture, re-attach patch.
- `research/2026-10-15-code-consolidation/fluid-reference-tsv/`: 16 steam and nitrogen TSV files; the `examples/` scripts that made them stay.

## Open for the owner

1. About 21 offline tools in the main checkout's `tools/` (neural, dwsim, neural-budget, lnn-gap, hold-fuzz, cloud-science-harness stubs and others) call removed APIs. They run on older commits; they do not compile on this branch.
2. `ScheduledTransfer` has no production producer after the module removal. Remove it (the junction-line test must then feed another way)?
3. `ColumnV3Network.PROTOCOL_VERSION` stays "11" while the wire schema is 16. `tryLoadPreset` is called only by a GameTest.
4. Still open from the findings batch: the three authoring defaults (`liquid_mixture`, positional assay form, aliases).
5. The Python tests in `examples/` were edited (profile `module` removed) but not run: no Python on the machine.

Code guide: updated by thread D (all seven files); snapshot lines are set at the merge.
