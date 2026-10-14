# jdk-determinism/winsweep

Windows cross-runtime sweep of the deterministic math. It runs the agreed check set under every JDK home given,
compares each runtime with the first, prints one table, and exits non-zero on any real difference. Batch
`2026-09-27-jdk-determinism`, owner decision D8 (2026-09-28): "do GraalVM on win, rewrite tools if needed. Linux testing
will be a separate work." Written on branch `claude/determinism-rehearsal`. First run (five runtimes, 2026-10-12):
`documentation/2026-09-27-jdk-determinism/INTEGRATION_REHEARSAL.md` section 14, logs in
`../rehearsal/logs/winsweep/w01/`.

**Linux is out of scope.** `../sweep.sh` (Linux, container JDKs from `../fetch-jdks.sh`) is left unchanged. By the
owner's decision the Linux sweep of the final code is a separate, later piece of work.

## How to run (Git Bash, from the worktree root)

```
bash tools/jdk-determinism/winsweep/winsweep.sh <out-dir> <reference-jdk-home> [<jdk-home>...]
# the five runtimes of this machine, HotSpot 21 as the reference:
bash tools/jdk-determinism/winsweep/winsweep.sh build/winsweep/w01 \
  "C:/Program Files/Java/jdk-21.0.11" "C:/Program Files/Zulu/zulu-25" \
  "C:/Program Files/JetBrains/IntelliJ IDEA 2026.2.1/jbr" \
  "D:/Minecraft/Modding/jdks/jdk-21.0.12.15" "D:/Minecraft/Modding/jdks/graalvm-community-25.4.4.1.1+1.1"
```

Exit status: 0 all runtimes identical (budget holds excepted), 1 a real difference (named in `details.txt`), 2 a
setup error (no `java` in a home, build failed).

Env: `ROOT` (worktree under test, default the one holding this script), `SEEDS` (default `1-200`), `THREADS` (fuzz
workers, default 4: the machine rule while another thread runs a campaign), `FUZZ=0` (skip the fuzz),
`BUILD=auto|always|never`, `FUZZ_RUNNER` (the Gradle-free fuzz runner folder; default
`<ROOT>/tools/phase-ports-probes/fuzz-runner`, else a read-only copy of branch `claude/fuzz-runner` extracted with
`git archive` into `<ROOT>/build/winsweep/fr`), `JAVAC_HOME` (compiles the two helper classes, default Oracle JDK
21.0.11).

Output in `<out-dir>`: `table.md` (also printed), `details.txt` (every differing line, chain-100 line or fuzz seed, and
the lines whose wall times were masked), `sweep.txt` (tree, head, settings), and per runtime `r<i>/`: `home.txt`,
`identity.txt` (`-XshowSettings:properties`; on HotSpot-family VMs also `UseJVMCICompiler`, `EnableJVMCI`,
`UseJVMCINativeLibrary`, `TieredCompilation`, `UseFMA` from `-XX:+PrintFlagsFinal`), `mathsweep.txt`, `suite.txt`,
`jit.log` (HotSpot family), `graal-compilation.log` (GraalVM), `regression.txt`, `regression-forced.txt`,
`fuzz-<dist>-report.txt` (+ `.classes.txt`) and `fuzz-<dist>.java.txt`.

## What it does

1. **Build, at most one Gradle invocation**, only when `<ROOT>/build/fuzz-runner/stamp` is missing or older than a file
   under `src/` or the probe sources. The fuzz runner's `build.sh` compiles main and test classes and the probe sources,
   builds the mod jar, and writes the test task's `java`, JVM arguments, class path and working directory into
   `<ROOT>/build/fuzz-runner/`. Then `javac` compiles `../perf/src/MiniLauncher.java` (JUnit Platform launcher with
   package selectors) and `../src/MathSweep.java` into `<ROOT>/build/winsweep/`. Everything after this is Gradle-free.
2. **Per runtime.** Every JVM gets the test task's JVM arguments and class path; only the `java` executable changes.
   - Identity and family: OpenJ9 by VM name, GraalVM by `UseJVMCICompiler = true`, otherwise HotSpot by major version.
   - `MathSweep`: which `java.lang.Math` functions differ from `StrictMath` on this runtime, whether any is
     tier-dependent, and which give other results than the reference's `Math` (the libm family the product must not
     depend on).
   - Fluid suites: `package:science.fluid`, `package:runtime.fluid` and `runtime.Fluid*Test`, without the probe classes
     of `phase-ports-probes/hold-fuzz/src` and `hold-classes/src`. HotSpot-family VMs log every compilation with its tier
     and JVM uptime (`-Xlog:jit+compilation`); GraalVM also writes Graal's own compilation log
     (`-Djdk.graal.PrintCompilation`). The evidence table shows the top tier (C2, or Graal on GraalVM), how many science
     methods it compiled, and the share of the run after which `DeterministicMath.log/exp/cbrt/pow` and
     `DeterministicPow.pow` were running at the top tier.
   - chain-100 exact (`FluidSolverRegressionTest`, `-Dfluid.regression.mode=exact`, never capture), twice: tiered as
     shipped, and forced, with every science method compiled before its first call (HotSpot family:
     `-Xcomp -XX:-TieredCompilation` restricted to `com.wormzjl.createcheme.science.*`, so C2 or Graal compiles them;
     OpenJ9: `-Xjit:count=0`).
   - Placement-sequence fuzz, random and world-default distributions, `SEEDS`, `THREADS` workers, 120 slices.
3. **Compare** (`compare.js`) against the first runtime.
   - Printed suite lines, in order, after these normalisations: Log4j's time of day masked; the fields `ms=`,
     `allocatedMB=`, `bytes=`, `wall...=` and every number followed by `ms` masked (the line is still compared); a median
     table of times dropped; JDK-internal stack frames (`java.*`, `jdk.*`, `sun.*`) and `... N more` lines left out, since
     their count and line numbers belong to the JDK build; the JVM's own `WARNING:` lines left out (JDK 24+ prints four
     when Netty calls a terminally deprecated `sun.misc.Unsafe` method; the reference prints none); Log4j's `~` before a
     frame's jar removed (it marks a guessed class location, which JDK 25 resolves exactly). The first five-runtime run
     found the last two (`INTEGRATION_REHEARSAL.md` 15). Lines that differ only in the order of a `reasons={...}` map
     count as identical (`Map.copyOf`, randomised per JVM start; `INTEGRATION_REHEARSAL.md` decision 2). The set of
     failing tests must be the same.
   - chain-100: the deviation line against the in-tree record, the list of differing values, and the step, solve,
     iteration and residual counts must be the same text as the reference's tiered run, for both the tiered and the
     forced run.
   - Fuzz: per seed the first hold (class and full status text) or clean. A seed that differs with a budget hold (class
     F: wall deadline or soft budget) on either side is "budget-only" (timing-dependent by design); any other difference
     is real.

## Adding a runtime

Extract the JDK anywhere (not on PATH, no system setting changed) and pass its home directory; nothing else changes. A
runtime must run class files of release 21 (the build's target) and accept the test task's JVM arguments (`-Xmx8g`,
`-ea`, `-D...`, file encoding and locale). HotSpot 21 and 25, OpenJ9 21 and GraalVM 25 all do. Family detection covers
HotSpot, OpenJ9, and GraalVM with the Graal JIT; a GraalVM run with `-XX:-UseJVMCICompiler` would be reported as
HotSpot. A VM that is neither OpenJ9 nor HotSpot-based would need its own JIT log and forced-compile switch in
`winsweep.sh` (the two `if` branches next to `forced=`).

## Cost in counted work

Per runtime:

| item | JVM starts | counted work |
|---|---|---|
| identity | 2 (OpenJ9: 1) | `-version` only |
| `MathSweep` | 1 | 11 functions x 1,048,576 inputs x 3 repetitions, each through `Math` and `StrictMath`: about 69 million calls |
| fluid suites | 1 | 528 tests (on `446fddf0`); the mixed-gas cost case alone makes 286 Newton solves |
| chain-100, tiered and forced | 2 | 1 test each: 3 accepted steps, 4 Newton solves, 29 Newton iterations, 33 residual evaluations |
| fuzz | 2 | 2 x `SEEDS` seed runs (default 400) of 120 five-second slices each, on `THREADS` workers |

Once per sweep: at most one Gradle invocation (none when the build is current) and two `javac` runs. A five-runtime
sweep is therefore 5 x 8 JVM starts (39 with OpenJ9), 5 x 528 tests, 10 chain-100 runs and 2,000 fuzz seed runs. The
fuzz reports print their own `seedMsSum` and `wallMs`; those depend on machine load and are not a claim of this tool.

## Files

- `winsweep.sh`: build check, per-runtime runs, then `compare.js`.
- `compare.js`: the comparison and the two tables (verdict, evidence).
- `argfile.js`: copies the build's class-path argfile with the launcher folder prepended, and prints the JUnit Platform
  jars for compiling `MiniLauncher`.
- `fuzz-runner-e6-exclude-on-c2d124d6.patch`: the fuzz runner (branch `claude/fuzz-runner`, `c2d124d6`) compiles all of `hold-classes/src`; since the E6 probe test of 2026-10-12 (`E6RowScaleProbeTest`, stale against `replayStart`) that build fails. The patch adds the exclusion `hold-classes.init.gradle` already has (its line 11). The close's sweep of 2026-10-14 (`w02`) ran with it applied to the extracted copy under `build/winsweep/fr`; apply it to the branch before the next sweep.
