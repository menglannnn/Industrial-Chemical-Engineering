# jdk-determinism

Cross-JDK determinism sweep of the fluid solver: one compiled build of the science code run under many Java runtimes
(Linux x64: HotSpot vendors 21 to 27, GraalVM with the Graal JIT, IBM Semeru/OpenJ9), every output that must be
bitwise identical captured and compared, and a `java.lang.Math` vs `StrictMath` micro-sweep per function. Built for the
owner's requirement of 2026-09-27: "I need the result to be identical across various JDKs on the market. Perform a
sweep, find out diff, propose fix."

Batch: `2026-09-27-jdk-determinism` (review `documentation/2026-09-27-jdk-determinism/JDK_DETERMINISM_REVIEW.md`).
Branch `claude/jdk-determinism-wip` from `efd123a` (phase-ports batch close). Status: in use since 2026-09-27.

## Files

- `fetch-jdks.sh` - downloads the runtimes from the vendors' official endpoints (Adoptium API, Azul metadata API,
  corretto.aws, aka.ms, BellSoft API, jdk.java.net, download.oracle.com, GitHub releases of GraalVM CE and IBM Semeru;
  SapMachine, Dragonwell, Kona and a few Oracle/GraalVM builds resolved through the foojay disco index to the vendor's own
  URL), checks the vendor checksum where one is published, extracts to `$JDKS/<id>/`, deletes the tarball, and appends
  `id, vendor, jvm, version, vm, url, sha256` to `$JDKS/manifest.tsv`. `jmods/`, `lib/src.zip`, `man/`, `demo/`,
  `legal/` were deleted afterwards to save disk (not needed to run `java`).
- `sweep.sh <capture-dir> [ids]` - per runtime: `tools/cloud-science-harness/harness.sh runtime` (the 33 junction
  lines, normalised; its reference check is not allowed to stop the sweep), `harness.sh regression` (chain-100 exact),
  the BitwiseProbe (default and, on HotSpot, with `-XX:-UseLibmIntrinsic`), `MathSweep` (default JIT, `-Xint`,
  `-XX:-UseLibmIntrinsic`). The harness got a `JAVA=` override for this (javac stays the container JDK's, so every
  runtime runs the same class files).
- `compare.py <capture-dir> --manifest <jdks>/manifest.tsv` - groups runtimes by byte-identical output and prints the
  Markdown tables of the review (junction lines, probe files with differing-value counts, MathSweep per function and
  runtime, tier consistency).
- `src/MathSweep.java` - 1,048,576 deterministic inputs per function (SplitMix64 and exact bit operations only, so the
  inputs are identical everywhere; their hash is printed) over the ranges the thermodynamics uses, for `log, exp, pow,
  cbrt, log1p, expm1, cos, log10, acos, sqrt, fma`: count of inputs where `Math.f != StrictMath.f`, max ulp, hashes of
  all Math and all StrictMath outputs, repetition stability. `java MathSweep bench` prints indicative ns/call of Math vs
  StrictMath (warmed loop, volatile black hole, not JMH); `tier` compares cold and warm results in one JVM; `dump <fn>
  <reps>` prints every input and Math output in hex (diff a JIT run against an `-Xint` run).
- `src/BitwiseProbe.java` - copy of `tools/phase-ports-probes/d9/src/BitwiseProbe.java` whose chain fixture uses
  `StrictMath.cos` like the prototype's `SolverRegressionHarness` (used for the after sweep; the before sweep used the
  d9 original).
- `src/CosChain.java` - shows that the chain-100 fixture pressures `150000 + 1000 cos(0.7 i)` are bitwise the same with
  `Math.cos` and `StrictMath.cos` (6 of the 100 cosines differ by 1 ulp, none of the pressures).
- `src/PowEquivalence.java` - bitwise comparison of `DeterministicMath.pow` (the allocation-free fdlibm port of the
  prototype) with `StrictMath.pow`, 6 input classes x N (default 50 million); `bench` prints ns/call of the three `pow`s.
  Compile against the prototype's main classes (`javac -cp <out>/main`).
- `xmlcases.py <test-results dir> <prefix>` - Gradle JUnit XML to a case list (`<prefix>-cases.tsv`) and per-class
  stdout with wall times masked; `stdoutdiff.py <before> <after> [noise]` - classifies stdout differences per class
  (max relative float change, integer tokens changed, text changes), ignoring line positions that differ between the
  before run and a second before run (`noise`).
- `full-science-routing.patch` - on top of the prototype commit: the other 17 column files and the column test's frozen
  flash reference through `DeterministicMath`, guard exemption removed (decision 12.2 (a) of the review;
  `git apply -p1`).
- `pow-strength-reduction.patch` - optional: 10 lines in `PipeResistance` and `TranslatedPengRobinson` replacing
  integer-exponent `pow` calls by multiplications (cost back to base; decision 12.4).
- `column-retune.patch` - test-only re-tune of the 4 column tests that depend on the libm (zero-duty reboiler fixtures
  moved to 50 kW / 100 kW; `documentation/2026-09-27-jdk-determinism/COLUMN_TESTS_REVIEW.md`), not applied; logs in
  `perf/logs/column-retune/`.
- `captures/before/<id>/`, `captures/after/<id>/` - per runtime `meta.txt`, `junction-lines.txt`, `runtime-summary.txt`,
  `regression.txt`, `probe-sha256.txt`, `probe-nolibm-sha256.txt`, `mathsweep-{jit,xint,nolibm}.txt`; `comparison.md`
  = `compare.py` output. `captures/probe-variants-before/` holds one `chain-100.json` per distinct before output
  (A HotSpot 21, B JDK 25, C OpenJ9 = HotSpot without libm intrinsics, D GraalVM without libm intrinsics);
  `captures/jdks-manifest.tsv` the runtimes; `captures/tier/` the GraalVM CE 25 interpreter-vs-JIT `pow` diff;
  `captures/gradle/` the Gradle case lists and junction lines; `captures/gradle-stdout-base-vs-prototype.md` the
  classification of moved test output.
- `site-counts/` - the call-site census: every non-correctly-rounded `Math` call site of `src/main/java` rewritten to
  a counter (`Count.java`; one-off `perl` rewrite of a scratch copy of the tree, not kept) and run through the harness
  science, runtime, adjacent and regression gates and the probe; `executed-sites.txt`, `site-map.md`.
- `logs/` - Gradle gate logs of the base (`00-*`, two runs), the prototype (`01-06`), the whole-science variant
  (`07`), timing (`09`), micro-benchmarks (`10`), pow equivalence (`11`), harness equivalence base-OpenJ9 vs
  prototype-HotSpot (`12`), harness `all` (`13`), `column-base/` (column tests on base code under three runtimes),
  `strictmath-direct/` (the same gates on the first, direct-`StrictMath` prototype).

## How to run

```
tools/jdk-determinism/fetch-jdks.sh                      # all runtimes (or name ids); JDKS= to relocate
SP=<scratch>; H=tools/cloud-science-harness/harness.sh
REPO=$PWD OUT=$SP/out LIB=$SP/lib bash $H compile       # once, with the container JDK
javac --release 21 -d $SP/probe -cp $SP/out/main:<gson, ejml jars> tools/jdk-determinism/src/BitwiseProbe.java
javac --release 21 -d $SP/mathsweep tools/jdk-determinism/src/MathSweep.java
OUT=$SP/out PROBE=$SP/probe MSWEEP=$SP/mathsweep tools/jdk-determinism/sweep.sh $SP/cap      # about 4 min per runtime
python3 tools/jdk-determinism/compare.py $SP/cap --manifest $SP/jdks/manifest.tsv > comparison.md
```

Identical output across runtimes = every file of `<cap>/<id>/` except `meta.txt`, `runtime-summary.txt` (walls) and
the Math columns of `mathsweep-*.txt` byte-identical, and `probe-sha256.txt` identical.

## prototype/

`0001-WIP-jdk-determinism-StrictMath-routing-prototype.patch` is `git format-patch` of the prototype commit `4956d2e` (branch `claude/jdk-determinism-wip`, base `efd123a`), kept here because that branch was never pushed. Re-apply with `git am tools/jdk-determinism/prototype/*.patch`; then `full-science-routing.patch` and `pow-strength-reduction.patch` are the two options of `documentation/2026-09-27-jdk-determinism/HANDOFF.md` section 5.

## perf/ (pow performance study, 2026-09-27)

Tools of `documentation/2026-09-27-jdk-determinism/POW_PERFORMANCE_REVIEW.md` (owner decision D4: mitigate the cost of
the deterministic math). Built on the Windows machine without Gradle and without downloads (Git Bash, Oracle JDK
21.0.11; the scripts ignore `JAVA_HOME`, which the machine points at Zulu 25, and use `PERF_JAVA_HOME`).

- `wbuild.sh compile fluid|column|all` / `run <selectors>` / `java <class>` - javac build of `SRC` (a tree) into `OUT`
  with the `cloud-science-harness` stand-ins (plus `perf/stubs/`, which adds the literature preset accessor), JUnit
  jars from the Gradle cache and `src/MiniLauncher.java` instead of the console launcher (`package:`, `class:`,
  `method:`, `exclude:` selectors). `RES` = the tree holding the resources and the tests' working directory.
- `check.sh <log-dir> [reference]` - the 33 junction lines (`MixedGasJunctionTransientTest`, `LiquidJunctionTransientTest`)
  against the harness reference, and chain-100 exact with its step and Newton counts.
- `timing.sh <log> <reps> suite|mixed|column|colsuite <builds...>` - alternated cost runs; waits before every run while
  another Gradle build, Gradle test JVM or NeoForge dev client is alive.
- `census/census.sh <scratch> [patch...]` + `census/DeterministicMath.java` - call-site census: copies the tree, tags every
  `DeterministicMath.f(` call with `"File.java:line[#k]"` and swaps in a counting facade (calls, argument range, repeats,
  `pow` exponents); run with `-Dcensus.out=<file>`. Results in `census/results/` (prototype and `after-*` = the stack).
- `src/ColumnProbe.java` - three column solves: `digest` (outcome, counters, result hash) or `time <reps>`.
- `src/FastDetMath.java` - prototype kernels (candidates C and D: log, exp, pow) with tables built from BigDecimal at
  class initialisation; `src/FastDetCheck.java` - `accuracy [n]` (per input class: differ count, max/mean ulp against
  StrictMath, output hash, exact-error sample), `specials`, `bench` (ns/call); `src/PowTablesPrinter.java` - prints the
  tables as the hex literals of `science/math/DeterministicPow.java`; `kernel-sweep.sh <log> [n]` - the accuracy hashes
  under HotSpot 21 (JIT, `-Xint`, C1), Zulu 25 (JIT, `-Xint`) and Temurin 17 (Gradle-provisioned), which must agree.
- `logs/` - `10-*` column suites per build, `20-*` kernel bench, `21-*`/`22-*` fluid-suite and column-probe timing of
  every candidate, `30-*`/`31-*` kernel sweep and 10^7 accuracy, `32-*` fluid suites on the stack, `40-42-*` final
  confirmation runs, `50-*` junction lines (and their diff against the prototype reference) and chain-100 deviations of
  B, B+A and B+A+C.

## rehearsal/ (integration rehearsal, 2026-09-28)

Tools and logs of `documentation/2026-09-27-jdk-determinism/INTEGRATION_REHEARSAL.md` (branch
`claude/determinism-rehearsal`: `claude/jdk-determinism-perf-wip` + phase-ports `db3ac1b9` + `column-retune.patch`; not
for merging). `xrt.sh` / `matrix.sh` / `xsuite.sh` run javac builds of the merged tree (`build/rehearsal/out`) and of
`git archive db3ac1b9` (`build/rehearsal/tree-base`, `out-base`) under HotSpot 21.0.11, Zulu 25.0.4, JBR 25.0.3, Semeru
21.0.12.15 (`openj9`) or GraalVM CE 25.4.4.1.1 (`graal25`) through `perf/wbuild.sh`; `printeddiff.js` compares printed
lines across runtimes (reason-map order normalised); `linediff.js` / `linetable.js` compare junction lines per token;
`fuzzxrt.sh` runs the Gradle-free fuzz runner of `claude/fuzz-runner` under another runtime (since that branch's
worktree was removed, from a `git archive` copy in `build/winsweep/fr`); `fuzzcmp.js` compares two fuzz reports per seed
(first hold, classes of `phase-ports-probes/hold-fuzz/classify.sh`). Logs: `g01-*` gates, `g02-*` full test, `g03-*`
GameTests, `fuzz/` (`f01` merged, `f02` control on `db3ac1b9`, `f03` merged under Zulu 25, `f04` OpenJ9, `f05` GraalVM),
`xrt/` (`s1-*` HotSpot, OpenJ9; `s2-*` GraalVM with its compilation logs and the forced-compiled chain-100 runs),
`winsweep/w01/` the first five-runtime `winsweep` run; files above 300 kB gzipped (`gunzip -k` before `fuzzcmp.js`).
`logs/` needs `git add -f`. `wip-winsweep-interrupted.patch`: the uncommitted state an interrupted session left
(2026-09-28), saved before the work continued.

## winsweep/ (Windows cross-runtime sweep, 2026-10-12)

`winsweep.sh <out-dir> <reference-jdk-home> <jdk-home>...` runs the agreed check set (fluid suites printed lines,
chain-100 exact tiered and forced-compiled, the fuzz in both distributions, MathSweep, JIT evidence) under every JDK home
given and prints one table; exit 1 on a real difference. Owner decision D8: Windows now, the Linux sweep (`sweep.sh`,
unchanged) later as separate work. See `winsweep/README.md`.

Build dirs (`build/perf/`) are git-ignored scratch: `tree-base` = `git archive a0c09c9f`, `tree-proto` = `27f7b53c`,
`tree-routed` = `0f3a94b6`, candidate trees = the branch commits, `tree-BAC(D)` = copies with `FastDetMath` routed.
`src/PowEquivalence.java` now checks `DeterministicMath.strictPow` (the bitwise fdlibm port), since `DeterministicMath.pow`
is the table-driven kernel.
