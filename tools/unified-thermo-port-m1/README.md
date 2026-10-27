# unified-thermo-port-m1

Purpose: the scripts, the Gradle-free lane, the probes and the archived logs of milestone M1 (engine and data,
bundled-neutral) of the unified thermo port: branch `claude/unified-thermo-port` (from main `ffeafe8a`, 0.8.0), porting
`claude/coolprop-multiphase-thermo-37f6b0` at `9aaafd4`, plus D21 (near-pure crystal steps). Batch
`2026-09-24-coolprop-low-temperature`; report `documentation/2026-09-24-coolprop-low-temperature/M1_ENGINE_PORT.md`;
plan `MAIN_ADAPTATION_PLAN.md`.

Nothing here is a gate and nothing here was ever tracked: the port's tests and code are in its commits
(`348d06e1`, `2b31cb90`, `49ad6968`, `efb222ae`, `de34347c`, D21 `7a3ea017`). Paths in the scripts name the M1 worktree
(`.claude/worktrees/coolprop-multiphase-thermo-37f6b0`); set `WT=` where a script reads it, or edit the first lines.

## Contents

| Path | What it is |
|---|---|
| `gradle-locked.sh` | One Gradle invocation under the `build/gradle.lock` protocol (refuses while a dev client runs; `JAVA_OPTS=-Xshare:off`, `--offline`): `gradle-locked.sh <log> <gradle args>` |
| `scripts/` | The port's deterministic rewrites, in order: `regen-step1.sh` (data, catalog, engine copies from 9aaafd4), `catalog.js`, `convert.js` (Math to DeterministicMath, JSON readers), `index.js`, `step1-extras.js`, `kernel.js` (kij(T) on main's kernel), `pair-interactions.js`, `tpr.js` and `cubic-evaluator.js` (OD-P2: derivatives on `evaluateDerivatives`, nothing restored), `spine-package-data.js`, `rename-root.js` (PhaseRoot to `PengRobinsonKernel.Root`), `engine-trim.js`; the test steps `tests-step1/2/3*.sh|js` (production entry points, F8; pre-WP7 domain rows; the oracle's stop rule); `audit.js` (test-only member audit) |
| `lane/` | Gradle-free javac + JUnit 5 lane: `lane.sh build <src-root> <out> <tests>` and `lane.sh run <classpath> <classes>` (each class's printed output under `### <class>`), `cmp.js a b` (printed lines only in one run), `grid.js` |
| `probes/facade/` | The determinism proof: `DeterministicMath.java` delegating to `java.lang.Math` (the branch's calls), compiled ahead of the port's classes, gives the branch's printed numbers; `Pins.java` (atan/tanh StrictMath pins), `CatalogProbe.java`, `MeltProbe.java` |
| `probes/d21/` | D21: `D21Probe.java` (row A's UV with N2/CH4 traces, PH across the sublimation line at 101.325 kPa, deep deposition, traces above theta, targets in the triple point's triangle), `variant.sh <Y or off> <mode>` (rebuilds the engine with another fluid-trace bound), `run1.sh` + `trace-inner.js` (nested-UV trace) |
| `diffs/` | `wp7-data.diff` (the branch's 10 MPa network data, M2), `wp7b-tests.diff` (its test rows), and the review diffs of catalog, kernel and TranslatedPengRobinson against the branch |
| `logs/m1-logs.tar.gz` | Gradle logs 01-16 (steps, full suite, G1 fluid suites and junction lines, G2 chain-100 exact, D21 scope), lane outputs (branch baseline at 9aaafd4, port, facade, D21), probe outputs |

## How to run

From Git Bash in the worktree root, after a Gradle compile:

- Engine families, port against a recorded run: `lane/lane.sh run "<WT>/build/classes/java/main;<WT>/build/resources/main;<WT>/build/classes/java/test;<WT>/build/resources/test;<launch dir>" <classes> > out.txt`, then `node lane/cmp.js before.txt out.txt` (compile `Launch.java` into `<launch dir>` first).
- D21: `probes/d21/variant.sh 1.0e-4 uv` (the committed bound); `off` disables the near-pure rule.
- Gates: `gradle-locked.sh <log> test --tests '...'`; one Gradle run at a time, no timing.

## Index record (moved from tools/INDEX.md on 2026-10-26)

Purpose: M1 of the unified thermo port to main 0.8.0: the port's deterministic rewrite scripts (catalog, Math to DeterministicMath, kij(T) on main's kernel, OD-P2 derivatives, PhaseRoot rename), a Gradle-free javac + JUnit lane with a printed-line comparer, the determinism facade proof, the D21 near-pure crystal probes and fluid-trace-bound variants, the WP7 10 MPa data diffs for M2, archived logs

Batch: 2026-09-24-coolprop-low-temperature

Status: Done 2026-10-15. Index: [../INDEX.md](../INDEX.md).

### Entry points

`README.md`; `gradle-locked.sh`; `lane/lane.sh`; `probes/d21/variant.sh`

### History

- Done 2026-10-15 (M1 `348d06e1`..`7a3ea017` on `claude/unified-thermo-port`, merged 2026-10-19 in 0.9.0 (main `5b49f940`, not pushed))
