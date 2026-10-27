# fluid-solver-simplification (t38)

Purpose: the measurement and diagnosis material of thread t38, batch `documentation/2026-10-18-fluid-solver-simplification/`
(plan `SOLVER_SIMPLIFICATION_PLAN.md`, map `SOLVER_COMPLEXITY_MAP.md`), branch `claude/fluid-solver-simplification` from
`688f4cd7`. Nothing here is tracked; nothing here was committed. The forms are commits on the branch; this folder holds their
rule-off patches, the probes used to trace them, the Gradle-free quick-check runner and the logs.

## Contents

| Path | What |
|---|---|
| `quick-lane.sh` | The 200-seed quick check on the Gradle-free javac lane: `bash quick-lane.sh <export> <label> [seeds] [dists]` on an export of `tools/unified-thermo-port-m5/d40/lane.sh` (compiles the cold and bundled hold-fuzz harnesses into it, runs them with their own system properties, classifies with the main checkout's `classify.sh`). Validated: the baseline export reproduces the m11 campaign's seeds 1-200 to the seed (cold 183, bundled random 198, defaults 200) and the Gradle run s2 bitwise on bundled. Seed lists take ranges and commas, so counters can be compared on the seeds clean in every form. |
| `timeline.pl` | One line per traced pass (`perl timeline.pl <trace log>`): the reservoirs' P, T, mass, phase volumes and each edge's law flow, cap, transported flow and saturation slope. |
| `patches/form-a-off.patch` | Rule-off of form A: `saturated` returns the law's flow (the cap out of every law). Proof: VelocityClampTest x4 and NetworkRegimeTest fail with q far over the cap (`logs/faoff-gates.txt`). |
| `patches/form-a-exponent-4.patch` | The exponent-4 variant of the cap law. Rejected: cold 152/200, bundled random 169/200 (`logs/e4-*`). |
| `patches/probe-pass-trace-on-form-a.patch` | The temporary pass probe on form A's `PassiveStepSolver` (`-Dcreatecheme.fluid.trace=true`): every pass start, failure and convergence with the node states, the edges' law flow / cap / transported flow / slope, the eight largest residual rows, and `-Dcreatecheme.fluid.trace.vars=<indices>` the rows every listed unknown moves. |
| `patches/probe-pass-trace-on-688f4cd7.patch` | The same probe on the baseline's solver (flows and caps of the cap active set). |
| `patches/newton-iterations-{30,40,60}.patch` | The three iteration-limit variants measured for the owner's question 3 (`logs/it30q-*`, `it40q-*`, `it60q-*` and their `-clean` counters); 30 was committed. |
| `probe/SeedLayoutDumpTest.java`, `probe/LngBubbleProbeTest.java` | Compiled beside the cold hold-fuzz harness on a lane export: the first prints one seed's placements with exact specs and controls (`-Dseed=397 -DholdFuzz.cold=true`), the second the phases of seed 397's LNG at a few temperatures and pressures; both served the K6 gate. |
| `FORM_D_CHOKE_LAW.md`, `patches/form-d1-choke-law.patch`, `patches/form-d2-gas-choke.patch`, `patches/form-d3-gas-choke-bulk.patch`, `probe/ChokeLawProbeTest.java` | Form D (owner request of 2026-10-18, 2026-10-19): a choke law with real pressure dependence in place of the configured velocity, prototyped on exports of `6c073298` only: the law, its numbers at usual states and the cold package's extremes, three forms on the quick check, seeds 19 and 988, the suite moves, the recommendation (not to land now), the owner's questions and decisions (2026-10-19: form D dropped, the 100 m/s cap stays for gas and liquids, the patches are evidence only). Logs `d1q-*`, `d2q-*`, `d3q-*`, `*-clean-*`, `*-988-*`, `*-19-*`, `suites/d2-*`, `suites/d3-*`. |
| `patches/probe-newton-trace.patch` | The temporary Newton probe on `SparseNewton`: per iteration the norm, refresh, the accepted alpha and reduction; with `trace.vars` the listed unknowns' value, residual, direction and (fresh) Jacobian column and row. |
| `logs/t38-logs.tar.gz` | `build/t38-logs/`: the traces (`fa3-*`, `fb-*` the bubble world on form A and on the baseline; `fa3-seed19-*` cold seed 19 on form A; `fcp-*` the phase-port tests on form C's first cut), the quick checks (`c0q-*` baseline, `e4-*` the rejected exponent, `fcq-*` form C's first cut, `fc2q-*` form C, `fb2q-*` form B, `*-clean-*` the clean-set counters, `fc2-47`, `fc2-518` and `fb2-518` the replays), the rule-off proof `faoff-gates.txt`; after the owner's decisions: `it30q-*`, `it40q-*`, `it60q-*` and `*-clean-*` (the iteration limit), `k6-397-*`, `it30-397-*`, `k630-397-*`, `k6p-seed397-trace.txt` (the K6 seed), `k6g-gate.txt` and `k6off-gate.txt` (the K6 gate with and without the rule), `k6sq-*` (the final tree's quick check), `g2-declared/capture/exact.gradle.txt` and `chain-100-before.json` (the chain-100 re-recording through Gradle); after the owner's decision on seed 678: `r678-gate.txt` and `r678off-gate.txt` (the 678 gate with the rule on `32bf3fa4` and without it on `6c073298`), `suites/r678s-*` and `suites/cmp-r678s-*` (the three suites on `32bf3fa4` against `c0` and against `d38s` = `6c073298`). The Gradle quick check s2 of form A is `build/s2-logs/` in the same archive; the suite logs `fa2`, `fa4`, `fc`, `fc2`, `fb2`, `k6s` against `c0` are `/tmp/t35-logs/` copies under `suites/`. |
| `logs/m12-logs.tar.gz` | `build/m12-logs/`: thread t39's Opus final test of `573b050b` against m11 (`FINAL_TEST_REVIEW.md`): the full test and its JUnit comparison, G2, the GameTests, the 1000-seed campaign, the alone replays `06-alone-*`, the clean-in-both counter runs `07-clean-*` on the javac lane, and `scripts/` (the per-seed, moved-line and counter comparisons, the seed lists). |
| `logs/m13-logs.tar.gz` | `build/m13-logs/`: thread t41's Opus final test 2 of `6c073298` (D38 C amended, moles and volume) against m12 (`FINAL_TEST_REVIEW.md`): the full test and its JUnit comparison, G2, the GameTests, the 1000-seed campaign, `cmp-seeds.txt`, and the clean-in-both counter runs `07-clean-t41hd*` (`573b050b`) and `07-clean-t41nw` (`6c073298`) on the javac lane with their seed lists. |
| `logs/m14-logs.tar.gz` | `build/m14-logs/`: thread t43's Opus final test 3 of `32bf3fa4` (the 678 rule, a proposal at the resolution is refused) against m13 (`FINAL_TEST_REVIEW.md`): the full test and its JUnit comparison with `check-moves.txt` (every new line in t38's `r678s` suites), G2, the GameTests (`03-G5-gametests-try1-oom` the start that failed on memory, `03-G5-gametests` the rerun), the 1000-seed campaign, `cmp-seeds.txt`, the alone replays `06-alone-cold-*` (685, 698, 797), the clean-in-both counter runs `07-clean-t43hd` (`6c073298`) and `07-clean-t43nw` (`32bf3fa4`) with their seed lists, and `scripts/`. |
| `trace/` | Thread t40 (2026-10-18): the pair minimization, bisect and trace of the two smallest new bundled holds (seeds 407 and 26, E8) on the javac lane, the step and pass probes, the D38 C proposition probe and `trace/logs/t40-logs.tar.gz`; report `BUNDLED_HOLDS_TRACE.md`. See `trace/README.md` |
| `trace-d3/` | Thread t42 (2026-10-19): the trace of form D3's failing gates under the gas choke law (`documentation/.../FORM_D_GATES_TRACE.md`): the corrected form D3c `patches/form-d3c-throat-share.patch` (a stream's limit is the throat share of its phases), the mixed-gas counter probe, `probe-vlim.sh`, `compact.pl`, `perseed.pl` and `logs/t42-logs.tar.gz`. See `trace-d3/README.md` |

Rule-off proofs per form: A `patches/form-a-off.patch` (five gate tests fail); C is form A alone (`HoldClassWorldsTest aBubbleTheNextPassDrivesToNothingIsRefused` fails on `6f1a524e`, passes on `ec6114a7`); B is form C alone (cold seed 518 is an active-set cycle on `ec6114a7`, clean on the form B commit).

## How the forms were measured

Each form: javac-lane suites `science-fluid-network` and `runtime-fluid` against the baseline logs `c0`
(`tools/unified-thermo-consolidation/lane-check.sh`), then `quick-lane.sh` on the same export (4 threads, counters on).
Costs are judged on the seeds clean in every form (`*-clean-*`), because a held world's substep refinement dominates the
whole-run counters. Form A's first quick check ran through Gradle (`tools/unified-thermo-port-m5/scripts/lane.sh <sha> quick`,
`build/s2-logs/`); every later one is Gradle-free.

Probes are applied to a lane export, never to the worktree: `lane.sh export <name> "$(bash d40/tree.sh)" <patches...>`.

## Rename to IChemE (2026-10-21)

Batch `2026-10-21-rename-icheme`: the files this folder runs against the current tree use the package `com.wormzjl.icheme` and the namespace `icheme` (mod rename, `8033aed8`). Patches, diffs and probes tied to an older commit keep `createcheme`: they re-attach to that commit. The absolute folder path `D:/Minecraft/Modding/1.21/CreateChemE` is unchanged. A worktree copy of this folder (for example the lane worktree `coolprop-multiphase-thermo-37f6b0`) is refreshed from here before its next run.

## Index record (moved from tools/INDEX.md on 2026-10-26)

Purpose: Thread t38 (batch `2026-10-18-fluid-solver-simplification`, branch `claude/fluid-solver-simplification`): the Gradle-free 200-seed quick check on the javac lane (`quick-lane.sh`, validated against the m11 campaign to the seed), the rule-off and variant patches of forms A, B, C, the temporary pass and Newton trace probes, `timeline.pl`, and `logs/t38-logs.tar.gz` (traces of the bubble world on form A and the baseline, cold seed 19, the phase-port tests; every quick check and clean-set counter run; the suite logs against `c0`); `trace/` (thread t40): the javac-lane pair minimizer, step and pass probes, the D38 C proposition probe and `trace/logs/t40-logs.tar.gz`; `trace-d3/` (thread t42, 2026-10-19): the D3c corrected patch, the mixed-gas counter probe, `probe-vlim.sh`, `compact.pl`, `perseed.pl` and `trace-d3/logs/t42-logs.tar.gz`, and t42b's cold-seed pair minimizer, total-work candidates, the rejected D3d patch and `trace-d3/logs/t42b-logs.tar.gz`

Batch: `2026-10-18-fluid-solver-simplification`

Status: Implemented 2026-10-19. Index: [../INDEX.md](../INDEX.md).

### Entry points

`README.md`, `quick-lane.sh`, `patches/`; thread t39's final-test logs `logs/m12-logs.tar.gz` (lane, alone replays, clean-in-both counters, comparison scripts)

### History

- Implemented 2026-10-19 (merged 2026-10-19 in 0.9.0 (main `5b49f940`, not pushed); final tests t39, t41, t43 green, logs `logs/m12-logs.tar.gz` to `m14-logs.tar.gz`)
