# unified-thermo-port-m2

Purpose: the scripts, the adapted hold-fuzz harness, the probes and the archived logs of milestone M2 (fluid network model on
the equilibrium engine, with owner decisions D24 to D27) of the unified thermo port: branch `claude/unified-thermo-port`,
commits `c91e3234` (data), `0ab70bc8` (model and solver reader), `26b1511e` (deferred tests), `e59da6ee` (D24 A),
`8a06b22f` (D25), then after M3a's `cc1a39a1`: `54c1d24d` (D26 A), `33ebad06` (D27), `a0f20364` (Legacy pin, spine
revision line, chain-100), on top of M1 (`7a3ea017`). Batch `2026-09-24-coolprop-low-temperature`; report
`documentation/2026-09-24-coolprop-low-temperature/M2_FLUID_THERMO_PORT.md`; plan `MAIN_ADAPTATION_PLAN.md`.

Nothing here is a gate and nothing here was ever tracked. Paths name the M2 worktree
(`.claude/worktrees/coolprop-multiphase-thermo-37f6b0`); set `WT=` where a script reads it, or edit the first lines.

## Contents

| Path | What it is |
|---|---|
| `gradle-locked.sh` | One Gradle invocation under the `build/gradle.lock` protocol (refuses while a dev client runs, exit 4; `JAVA_OPTS=-Xshare:off`, `--offline`): `gradle-locked.sh <log> <gradle args>` |
| `scripts/wait-then-run.sh` | Waits for one minute without a dev client, runs `gradle-locked.sh`, waits again when it still refuses (6 h limit) |
| `fuzz.sh` | The quick check: the placement-sequence hold fuzz on seeds 1-200 in both distributions, 8 workers, classified by `tools/phase-ports-probes/hold-fuzz/classify.sh` (read in place): `HARNESS="$PWD/hold-fuzz" bash fuzz.sh <label> [seeds]` from this folder; reports in the worktree's `build/m2-logs` |
| `hold-fuzz/` | Copy of `tools/phase-ports-probes/hold-fuzz` (`hold-fuzz.init.gradle`, `src/`) adapted to main 0.8.0: the commit hook is gone, `committed(Set<Long> removed, Set<Long> recompiled)` |
| `scripts/edit.js` | Exact-text replacement helper: `node edit.js <file> <spec.json>`, spec `[{"from","to","all"}]`, line endings kept |
| `scripts/migrate-calls.js`, `tests-port.sh`, `tests-rewrite*.js`, `tests-place.sh`, `re-records.js`, `javac-errors.js` | The M2 call-site migration (run once at `7a3ea017`, not idempotent) and the deferred-test port of `26b1511e` from 9aaafd4 |
| `scripts/port-bubble-point-test.js` | Places the branch's `LiquidFullBubblePointTest` on main's API (D25 check; the vessel split not ported) |
| `scripts/printed-diff.js`, `printed-delta.js` | Printed lines (JUnit system-out) of two result folders: the differing lines, or per class the count and the largest relative change with timings masked |
| `scripts/junction-lines.js` | The 33 junction lines of a result folder (ms, bytes, allocatedMB masked), optionally against a reference file (M1's `13-G1-junction-lines.txt` is main's) |
| `probes/three/` | The THREE_PHASES probe of the first stop (28 states and seed 156; `RRFIX=1` is D24's option A); outputs `out-*.txt` |
| `probes/stiff/` | `M2StiffProbeTest` and its outputs: the CadenceTrajectory and McpGameplay traces with options (variants base, A, B) |
| `probes/mcp-fixture/` | `M2McpFixtureProbeTest` (D27): writes the fresh McpGameplay clamped liquid-full fixture from the 2026-09-16 capture (each node at its captured T and P keeps its minor components, its major one fills its 1 m3), reads it back as the test does and replays it; the written JSON (committed in `33ebad06`) and `out-41-mcp-fixture.txt`. Run: copy the source into `src/test/java/...runtime/fluid/`, `gradle-locked.sh <log> fluidRuntimeTest --tests '*M2McpFixtureProbeTest'` with the capture still in place (`git show 54c1d24d:src/test/resources/fluid/mcp-clamped-liquid-full-checkpoint.json`), delete the copy |
| `probes/specs/` | Temporary print edits, never committed (`git restore` afterwards): `adopt-three-print`, `pass-loop-cycle-print` (seed 116), `stack-pass-print` (STACK_PASS per slice, D24 against D25), `cycle-trace-print` + `cycle-trace-converged-print` (PassiveStepSolver: each pass's key, node states, converged flows and caps, failed iterates) and `phase-check-junction-print` (FluidThermodynamics.phaseCheck at junctions: TP and nucleus volumes, D41 fold) for seeds 35 and 86; D26: `d26-check-print` (each added phase: TP, seed and own split), `d26-split-print` (the vessel closure's exit), `d26-split-last-iterate-probe` (the closure's last iterate as the seed, LiquidFullBubblePointTest), `d25-check-print` (the same lines on D25, STACK_PASS); `water-kappa-probe` (the stiff probe's water laws) |
| `staging/` | The commit messages of `8a06b22f`, `54c1d24d`, `33ebad06` and `a0f20364`; `LegacyNetworkPathPinTest.java` (as ported, DIGEST and REVISION "PENDING") and `spine-revision.json` (the `edit.js` spec of SpineNetworkPathTest's REVISION line), both applied and pinned in `a0f20364` |
| `diffs/` | `d25-main.diff` and the three D25 main sources as committed in `8a06b22f`; `wp7b-tests-main.diff`; `d26-54c1d24d.diff`; `d27-33ebad06.diff` and `pins-a0f20364.diff` (without the fixture JSON and chain-100, see `probes/mcp-fixture/` and `logs/`) |
| `logs/m2-logs.tar.gz` | `build/m2-logs`: Gradle logs 04-25 with result folders (13 tests commit, 15 D24 families, 19 D25 suites, 20 D24-only attribution, 22 D25 rows), fuzz reports and class tables (`01` main `7a3ea017`, `08` `0ab70bc8`, `23` `8a06b22f`), probe outputs (`21`/`22` STACK_PASS D24/D25, `24`/`25` the E7 cycles of seeds 35 and 86); D26-D27 and the pins: `26`-`39` (D26 compile, units `30`, LiquidFullBubblePoint probes `28`/`29`, seeds 35 86 116 `31`, suites `32`, STACK_PASS probes `33`-`36`, quick check `37`, PhasePortPriority `38`), `41` fixture probe, `42` D27 tests and the cadence report, `43` quick check at `33ebad06`, `44` Legacy print and chain-100 capture, `45` chain-100 old and new, `46` full `test` with results, the 33 junction lines and their diff against main |

## How to run

From Git Bash in the worktree root, after a Gradle compile of the main classes:

- Quick check: `cd tools/unified-thermo-port-m2 && HARNESS="$PWD/hold-fuzz" bash fuzz.sh 43-head-<sha> 1-200`.
- A seed's cycle trace: `node tools/unified-thermo-port-m2/scripts/edit.js <PassiveStepSolver.java> probes/specs/cycle-trace-print.json`
  (then `cycle-trace-converged-print.json`, and `phase-check-junction-print.json` on `FluidThermodynamics.java`), run
  `gradle-locked.sh <log> --no-configuration-cache --init-script <abs>/hold-fuzz/hold-fuzz.init.gradle test --tests '*HoldFuzzTest'
  -PholdFuzzSeeds=35,86 -PholdFuzzThreads=1 -PholdFuzzDefaults=false -PholdFuzzOut=<report>`, read `M2PROBE` lines from the
  test XML's system-err, then `git restore` both files.
- Junction lines against main: `node scripts/junction-lines.js <results dir> <M1 logs>/13-G1-junction-lines.txt`.
- Pins at a head (log 44): `gradle-locked.sh <log> test --tests '*LegacyNetworkPathPinTest' --tests '*SpineNetworkPathTest'
  fluidSolverRegression -PfluidRegressionCapture=true --continue` prints the Legacy pin's revision and digest (test XML) and
  re-captures chain-100; compare the capture with the previous one offline (log 45).

## Index record (moved from tools/INDEX.md on 2026-10-26)

Purpose: M2 of the unified thermo port (the network model on the equilibrium engine, D24 Rachford-Rice in the minor phase's fraction, D25/D26 outer phase check): the port's scripts and diffs, the hold-fuzz harness adapted to main 0.8.0 (`hold-fuzz/`, `fuzz.sh`), the Rachford-Rice and phase-check probes, the staged Legacy pin, archived logs

Batch: 2026-09-24-coolprop-low-temperature

Status: Done 2026-10-16. Index: [../INDEX.md](../INDEX.md).

### Entry points

`README.md`; `fuzz.sh`; `gradle-locked.sh`

### History

- Done 2026-10-16 (M2 `c91e3234`..`a0f20364` on `claude/unified-thermo-port`, merged 2026-10-19 in 0.9.0 (main `5b49f940`, not pushed); STACK_PASS open as D28)
