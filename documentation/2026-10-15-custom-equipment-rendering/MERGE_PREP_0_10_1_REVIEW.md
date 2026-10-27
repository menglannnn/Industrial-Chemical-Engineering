# Merge prep 0.10.1 (Opus, 2026-10-21), `144321e7` on `claude/stoic-bell-ae6019`, not merged or pushed
Owner request 2026-10-21: "Option (a): merge prep on c7b883cb (code guide, CHANGELOG version heading, mod_version bump, tooling sort, gates). Do not merge or push without asking." Follows `FINAL_LANE_C7B883CB_REVIEW.md`. Logs: `tools/new-units/logs/merge-prep-0.10.1-2026-10-21.tar.gz`.

## 1. Base
- `main` is still `2e28a20a` (0.10.0), and the branch is 15 commits ahead with nothing to merge in. The merge can be a fast-forward of `main` to `144321e7`, so the final lane on `c7b883cb` (fl8) is the lane of the merged tree.
## 2. Tooling sort (AGENTS.md "Test classes and tools after a batch")
- The branch changes 8 main classes and 2 test classes. The only test addition is the gate `PumpCavitationTest.waterArrivingAtACavitatingSuctionStaysLiquid`. The `HoldClassWorldsTest` change is javadoc only.
- No probe, trace switch, system property or print is in tracked main code. The one `System.out` is the gate's `PUMP_CAVITATION_ARRIVAL` line. `git ls-files -i -c --exclude-standard` is empty, and no log is tracked.
- Nothing tracked was detached, so the gate suite is unchanged.
- The batch's measurement material is in the main checkout's `tools/new-units/od4-holds/`:
  - rounds 1 to 12 (`probe/`, `r2/` to `r9/`), with probes, form patches and per-round logs;
  - the lane scripts as used, new in this prep: `scripts-2026-10-21/final-lane/` and `scripts-2026-10-21/javac-lane/`, each with a README section.
## 3. Release files (commit `144321e7`)
- `CHANGELOG.md`: `[Unreleased]` becomes `[0.10.1] - 2026-10-21`. It has a batch line with the commits, the reviews, the tools folder and the final lane, then the nine fix lines. A's line left with its revert. If the merge lands on another day, the date moves with it.
- `gradle.properties`: `mod_version` 0.10.0 → 0.10.1, a patch bump because the batch only contains fixes (version rule; owner feedback on version pace).
- No format changed: fluid checkpoint 12, unit format 5.
## 4. Code guide (main checkout, `documentation/reference/code-guide/`)
- `fluid-science.md`:
  - class map: `PhaseLayout.junctionClosure`, `FluidThermodynamics.junctionSplit`, the step solver's seeds, flash seeds and suction floor, `PassiveIntervalSolver.carriedFlows`, `SolidEventIntegrator.still`, `JunctionCrystals.split`;
  - the suction floor (`BalanceNet.suctionFloor`, `waterFloor`);
  - the balanced start (`runFlow`, 1e-9 slopes, `mixed`, closures off a converged balance only);
  - the arriving seeds (stock alone, `laidOutFor`, `junctionSplit`, the 889 gate);
  - a new "Flash seeds" paragraph, and the still pipes from `carriedFlows`;
  - the routing rows, the tests, and the balance defaults row.
- `README.md`: a section "OD4 holds after 0.10.0 (release 0.10.1)" and open item 14 (430, 788, 475, 855, bundled Newton +14.7 %, the taut-vessel rate solve, the hydrocarbon water floor, the stored sweep-first form).
- Both snapshot lines read `c7b883cb` at merge prep, to be set to the merge commit when it lands. The other chapters are unchanged, because their code is untouched.
## 5. Gates on `144321e7` (alone, the m23 lane script)
- Full test: 1 572 tests, 0 failures, 0 errors. Chain-100 exact (deviation 0). Fluid GameTests 31/31.
- The campaign was not re-run: no code changed after fl8 (cold 975, bundled 994, defaults 1000).
## 6. For the owner
- Ready to merge as 0.10.1, a fast-forward of `main` to `144321e7`. At the merge: set the guide's snapshot lines to the merge commit, and mark the batch rows in `documentation/INDEX.md` as Implemented. Not merged or pushed.
- Merged 2026-10-21 (owner: "Merge ... into main as 0.10.1 by fast-forward ... Do not push"): `main` fast-forwarded from `2e28a20a` to `144321e7`; code guide snapshot lines set to `144321e7`; INDEX rows marked Implemented (reviews) and Concluded (final lanes). Not pushed.
