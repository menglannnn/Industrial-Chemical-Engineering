# Custom equipment rendering: merge preparation (2026-10-20)
Owner request: "prepare for merge". Branch `claude/custom-equipment-rendering-805e7d` (36 commits on `ffeafe8a`, 0.8.0) onto `main` = `449d398c` (0.9.2), as its own batch with fluid checkpoint format 12 (owner decision 4 of 2026-10-19, `2026-09-24-coolprop-low-temperature/MERGE_STACK_PLAN.md`).
## Main's movement and the merge shape
- `main` moved from 0.8.0 through 0.9.0, 0.9.1 and 0.9.2 since the branch started. `origin/main` is still `5b49f940`, so local `main` was merged directly (the app's sync tool fetches from origin).
- `6cd0d020`: `main` merged into the branch. The batch then lands as a fast-forward of `main` plus one release commit, as 0.8.0 and 0.9.0 did: `main` gets the tested tree and every commit.
## Conflicts (11 files) and how they were resolved
- **Text:** CHANGELOG (both sides; the batch's lines stay under `[Unreleased]`); `build.gradle` (the branch's bridge-jar cleanup, without `project.delete`, which the configuration cache forbids); `DeterministicMath` (both sides' functions: `sin`, `asin`, `atan2` and main's `atan`, `tanh`).
- **Fluid runtime** (an Opus agent; the owner asked for Opus):
  - Checkpoint `VERSION` 11 → 12 and `UNIT_FORMAT` 4 → 5. A registration carries its vessel ports and level height; the island unit carries each node's volume and level height. Formats 11 and older are refused (fresh worlds).
  - `Inventory.levelHeight` is carried by `with(...)` through main's new rebuilds: `InventoryEquilibrium.refresh`, `VesselCrystals` melt, frozen and settled, the step solver's splits, `ConservativeTransport`, `PassiveIntervalSolver.flushTraces` and `IslandCertificate`. Without this, a drum fell back to 1 m at every crystal refresh.
  - `portHead` is h = g·m_c·H/V with the vessel's own H. A 1 m one-block tank gives main's arithmetic exactly.
  - Main wins where it deleted a mechanism: mover efficiency and m·g·z booking stay deleted. Correction (final lane): main has neither the OD4 half link nor the RWCWR rig (`449d398c` keeps RWCPR); both are the branch's own, since `d1701780` (2026-10-16), and they come in with the merge.
  - Short run: 132 tests, 0 failures. New `FluidCheckpointCodecTest.aVesselsLevelHeightRoundTrips`; `FluidCheckpointFormatTest` refuses 11.
## Sorting before the merge (AGENTS.md "Test classes and tools after a batch")
- **Detached:** `FrameTimeProbe` (`/fpsprobe` and the FPS autopilot, measurement only) leaves `src/mcpCompat` in `03be157b`. It is stored with a README and a patch in the main checkout's `tools/custom-equipment-rendering-poc/frame-time-probe/` (stored copy identical to `git show 6cd0d020:<path>`). CHANGELOG line `8f9009b3`.
- **Kept:** `UseItemMixin` and `WindowHandleMixin`, which `runMcpClient` (a documented Gradle task) loads for the GUI lane. Every gate test in `science/equipment` runs under `test`.
- **Not tracked:** the batch's probes, sweeps, renders and in-game scenes live in `tools/new-units/` and `tools/custom-equipment-rendering-poc/`. `git ls-files -i -c --exclude-standard` is empty.
## Final lane
- **NOT GREEN** (`FINAL_TEST_REVIEW.md`, m21 at `8f9009b3`): 1571 tests, 1 failure (main's gate `JunctionSeedingGateTest` on bundled 145, E7); G2 exact; GameTests 31/31; campaign cold 955 → 955 (3 newly held F, 3 newly clean), bundled 990 → 988 (145 E7, 889 E9), defaults identical.
- Every difference is OD4 (an actuator's half link takes the bore of its pipe). With OD4 off every moved test and seed equals 0.9.2, so the merge's own fluid change moves nothing in worlds without drums. OD4 also moves `LiquidCo2LetDownTest` (0.28264 → 0.23068 kg/s) and `PumpCavitationTest` (8.0132 → 7.2201), announced nowhere.
- Owner kept OD4 (option a). Fixes in `OD4_HOLDS_REVIEW.md`: `f983306b` (cycle key carries the static columns: 145), `8fd61a13` (arriving seed = PH split of the buffer's own enthalpy: 889; m22 `FINAL_TEST_M22_REVIEW.md` caught a first form that broke 583, 743, 322, 354), `9de0d85f` (owner decision E5, amending D38 C: a shrinking non-aqueous liquid vanishes on its volume share alone: the D34 gate, which 0.9.2 passed only on its exact doubles). Checks of the fix rounds: affected classes only (owner), 19 classes 135 tests 0 failures, robustness 7/7.
- **m23 at `8be1efef`: GREEN** (`FINAL_TEST_M23_REVIEW.md`; owner scope: chain-100, GameTests, campaign, no unit suite). G2 exact; GameTests 31/31; defaults counters identical to 0.9.2; clean cold 955 → 962, bundled 990 → 990. Newly held against 0.9.2: cold 285 (OD4's own world: LNG flashing through 10 mm lines, 0.802 vs 1.040 kg/s, clean with OD4 off) and cold 866 (wall time only under load: no actuator, fewer iterations than 0.9.2, clean alone). Moved references: `MOVED_REFERENCES.md`.
## Code guide
- New chapter `equipment.md` and README rows. Updated: `client-gui`, `fluid-runtime` (format 12), `fluid-science` (level height) and `thermo-materials` (the new `DeterministicMath` functions). Snapshot lines are set to the release SHA at the merge.
## Merge steps (on the owner's word)
1. Release commit on the branch tip: CHANGELOG `[Unreleased]` → `[0.10.0] - <date>` (a minor version for a merged batch) and `mod_version=0.10.0`.
2. Fast-forward `main` in the main checkout to it; set the guide's snapshot lines to the release SHA; `documentation/INDEX.md` row Implemented with the date; memory.
3. Push only on the owner's word.
## Owner items (not blocking)
1. A reader found a possible flange mismatch: a pipe wider than a nozzle's `maxOd` draws its own flange at `maxOd` (`FluidDeviceBlockEntity.computeFaces`), while the vessel side draws the pipe's size. Not checked in game.
2. Stale javadocs (kind lists in `EquipmentShape`, `ProcessUnit`, `UnitDesign`, the constructor screen; "a column's or a heater's" ladder; the debug `boot` literal under `vertical`): a comment-only commit after the final lane.
3. No test pins the 80k-quad ceiling; only the offline sweep checks it.
5. Cold 285: OD4's cost item (26 113 Newton iterations against 2 680, the island never settles since `8fd61a13`). 889 has no gate test of its own. A dead-end line takes the lighter (gas) column where water would fall in: a possible simplification proposal.
4. After the merge, one in-game look at 0.10.0 (a fresh world, format 12) when the owner wants it.
