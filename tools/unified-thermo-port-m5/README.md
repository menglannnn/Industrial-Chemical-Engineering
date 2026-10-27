# unified-thermo-port-m5

Purpose: M5's measurement lane for the unified thermo port (batch `2026-09-24-coolprop-low-temperature`, branch
`claude/unified-thermo-port`). It produces the review's numbers in `MAIN_ADAPTATION_REVIEW.md`: the full test (which
serves as G1 and G3), G2 chain-100 exact, the fluid GameTests on a fresh run id, and one 1000-seed hold-fuzz campaign
(cold random, bundled random, bundled defaults, 8 workers). Nothing here was ever tracked, so nothing needs re-attaching.

- `scripts/lane.sh <head-sha> [test regression gametest campaign]`, run from the worktree root (a copy lives in the
  worktree's git-ignored `tools/`). It runs one Gradle invocation at a time through
  `tools/unified-thermo-port-m1/gradle-locked.sh`, which refuses while a dev client runs. Logs go to `build/m5-logs/`
  (`01-full-test`, `01-results`, `02-G2-regression-exact`, `03-G5-gametests`, `04-campaign-cold-*`,
  `05-campaign-bundled-*`). The campaign uses the forms harnesses (`tools/unified-thermo-port-forms/hold-fuzz-*`) and
  `tools/phase-ports-probes/hold-fuzz/classify.sh`.
- Junction lines against the batch reference: `node tools/unified-thermo-port-m2/scripts/junction-lines.js
  build/m5-logs/01-results <reference>`. Printed lines against the last full test: `printed-diff.js` and
  `printed-delta.js` in the same folder.
- Logs are archived as `logs/m5-logs.tar.gz` when the lane is done.
- `trace/` (2026-10-17): the minimizer and trace harness for the campaign's two smallest new worse worlds, bundled random
  322 (E6) and 47 (F), with its own init scripts, the main-contrast variant (`src-main/`, run in a temporary worktree at
  `ffeafe8a`, since removed) and the trace hooks as a patch (`patches/`, reverted after use). Its `README.md` explains
  each log; the report is the batch's `M5_WORSE_WORLDS_TRACE.md`.
- `logs/m5-logs.tar.gz` (2026-10-17): `build/m5-logs` at the end of M5: the lane (01 to 05), the budget holds replayed
  alone (06, 07) and the trace (08 to 15). Extract with `tar --force-local -xzf logs/m5-logs.tar.gz`.
- `d34/` (2026-10-17): the D34 fix agent's material (owner decision D34, commit `4199dbe6`): the javac-lane scripts and
  class lists it ran (`lane.sh`, `run-touching.sh`, `classes-*.txt`), the gate's rule-off proof (`rule-off-reverse.patch`
  on `4199dbe6`, gate run alone), the unit-case probe that does not reproduce the hold (`probe/StaticColumnTraceTest.java`)
  and its logs. Its `README.md` explains each; the report is the batch's `D34_STATIC_COLUMN.md`.
- `scripts/lane.sh` takes `LOGS=<dir>` and `RUNID=<prefix>` and a `quick` step (the 200-seed check); the post-D34 runs
  use `LOGS=build/m6-logs RUNID=m6`.
- `d36/` (2026-10-17): the D36 fix's material (owner decision D36, a gas column does not exist; commit `03bc70f6`): its
  lane scripts, the two gates' rule-off proof (`rule-off-reverse.patch` on `03bc70f6`, each gate run alone), four probes
  and `logs/d36-logs.tar.gz`. Its `README.md` explains each; the report is the batch's `D36_NO_GAS_COLUMN.md`.
- `simplifications/` (2026-10-17): the D37 fix's material (owner decision D37, the six approved simplifications S1-S6;
  commits `bff22e37`, `819d774b`, `9445106a`, `22b76697`, `eec02758`, `65036f2b`): its javac lane and class groups, the
  S6 and S2 edit scripts, S1's rule-off patch (`git diff -R 22b76697 eec02758 -- src/main`, the gate run alone), the S3
  and S4 probes and `logs/simp-logs.tar.gz`. Its `README.md` explains each; the report is the batch's
  `SIMPLIFICATIONS_S1_S6.md`.
- `scripts/idle.ps1`, `wait-idle.sh`, `m7-idle-checks.sh`, `bisect-seeds.sh` (2026-10-17, after D37): `idle.ps1` prints
  the JVMs not on the port's JDK 21.0.11, the CPU load and the dev clients; `wait-idle.sh [--cmd <script> ...]` waits (at
  most `MAXH` hours) until none runs and the load is under 30 % on three checks a minute apart, then runs `lane.sh` or
  the script; `m7-idle-checks.sh` replays the new cold holds alone and reruns the quick check into `build/m7-logs/idle`;
  `bisect-seeds.sh <sha> <label> <seeds>` replays seeds alone (one thread, cold harness, its pre-S6 copy before
  `9445106a`) at a detached commit and returns to the branch, refusing a dirty tracked tree. Logs: `build/m7-logs/`.
- `d38/` (2026-10-18): the D38 fix's material (owner decision D38: D36 reverted in `26090bf0`, rule C, one vanishing
  rule for every node, in `67e5f3e7`): its javac lane and class groups, rule C's rule-off patch (`rule-c-off.patch` on
  `67e5f3e7`, the gate run alone), the seed-world probe that gave the gate its blocks, the `D38V` trace hooks
  (`instr.sh`) and `logs/d38-logs.tar.gz`. Its `README.md` explains each; the report is the batch's
  `D38_REVERT_D36_AND_VANISHING_RULE.md`.
- `d39/` (2026-10-18): the D39 fixes' material (owner decision D39: a failed iterate's check only folds, in `1482c277`,
  seed 430; a proposal the next pass refutes is refused, in `00ee8385`, seed 48): its javac lane and class groups, both
  rule-off patches (`rule-1-off.patch` on `1482c277`, `rule-2-off.patch` on `00ee8385`, each gate run alone), the
  seed-world probe that gave both gates their blocks, the `D39REFUTE` trace hook (`refute-trace.pl`) and
  `logs/d39-logs.tar.gz`. Its `README.md` explains each; the report is the batch's
  `D39_FAILED_ITERATE_AND_REFUTED_PROPOSAL.md`.
- `d40/` (2026-10-18): the D40 fixes' material (owner decision D40 A, test world H1 to H4: a pump's start mode reads its
  own lines only, in `b81d3491`; one step rule in the UV pressure search, in `88817099`; a vessel's phase seed is its own
  state and a failed vessel closure proposes nothing, in `3db5e5d3`): its javac lane and class groups, the three rule-off
  patches (`rule-h2-off.patch`, `rule-h4-off.patch`, `rule-h13-off.patch`, each gate run alone), the fold-redundancy
  patches kept but not run (`fold-c-off.patch`, `fold-d39-off.patch`, `fold-d39b-off.patch`), the probe and
  `logs/d40-logs.tar.gz`. Its `README.md` explains each; the report is the batch's `D40_SEEDS_PUMP_START_UV_STEP.md`.
- `logs/m7-m9-logs.tar.gz` (2026-10-18): `build/m7-logs` (the loaded and idle quick checks at `65036f2b`, the alone replays, the bisect of the new cold holds, the M7 trace 01-13), `build/m8-logs` (the quick check, final lane and campaign at `482264fc`, the C/F holds alone, the M8 trace 01-06) and `build/m9-logs` (the quick check, final lane and campaign at `00ee8385`, bundled 430 alone). Extract with `tar --force-local -xzf logs/m7-m9-logs.tar.gz`.
- `logs/m10-logs.tar.gz` (2026-10-18): `build/m10-logs`, the final lane and campaign at `3db5e5d3` (D40; `lane-stdout.txt`, `01-results` to `05-campaign-*`) and the new budget hold replayed alone (bundled 241, `06-alone-*`). Extract with `tar --force-local -xzf logs/m10-logs.tar.gz`.

## Index record (moved from tools/INDEX.md on 2026-10-26)

Purpose: M5 of the unified thermo port at `7bd5fafc`: the measurement lane (`scripts/lane.sh`: full test, G2 chain-100 exact, fluid GameTests, the 1000-seed campaign through `unified-thermo-port-m3/fuzz.sh`), the logs archives `logs/m5-logs.tar.gz` (lane, budget holds replayed alone, trace), `logs/m7-m9-logs.tar.gz` and `logs/m10-logs.tar.gz` (the later quick checks, lanes, alone replays and traces) and `trace/` (minimizer, solve log, row-trace patch and main contrast for the new worse worlds 322 and 47), `d34/`, `d36/`, `simplifications/`, `d38/`, `d39/` and `d40/` (the D34 and D36 to D40 fixes: javac-lane scripts, the gates' rule-off patches, probes; nothing tracked), the idle and bisect scripts (`scripts/idle.ps1`, `wait-idle.sh`, `bisect-seeds.sh`) and the M7 and M8 traces in `trace/` (harness switch, pair minimizer, hooks rebased per commit)

Batch: 2026-09-24-coolprop-low-temperature

Status: Done 2026-10-18. Index: [../INDEX.md](../INDEX.md).

### Entry points

`README.md`; `scripts/lane.sh`

### History

- Done 2026-10-18 (review `MAIN_ADAPTATION_REVIEW.md`, traces `M5_WORSE_WORLDS_TRACE.md`, `M7_NEW_COLD_HOLDS_TRACE.md`, `M8_CAMPAIGN_WORSE_WORLDS_TRACE.md`, `TEST_WORLD_RIG_HOLDS_TRACE.md`, fixes `D34_STATIC_COLUMN.md`, `D36_NO_GAS_COLUMN.md`, `SIMPLIFICATIONS_S1_S6.md`, `D38_REVERT_D36_AND_VANISHING_RULE.md`, `D39_FAILED_ITERATE_AND_REFUTED_PROPOSAL.md`, `D40_SEEDS_PUMP_START_UV_STEP.md`)
- merged 2026-10-19 in 0.9.0 (main `5b49f940`, not pushed)
- `scripts/lane.sh` ran every final lane of the stack up to m17 (t49)
