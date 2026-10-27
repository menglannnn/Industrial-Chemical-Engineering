#!/usr/bin/env bash
# Windows cross-runtime determinism sweep (batch 2026-09-27-jdk-determinism, owner decision D8). Git Bash. At most one
# Gradle invocation to build (the fuzz runner's build: main + test classes, mod jar, the test task's JVM arguments and
# class path), then every check runs Gradle-free under each JDK home given. The first JDK is the reference; every other
# runtime is compared with it.
#
#   winsweep.sh <out-dir> <reference-jdk-home> [<jdk-home>...]
#
# Checks per runtime (README.md): identity (vendor, VM, JIT flags), MathSweep (which Math functions differ from StrictMath
# on this runtime, and whether they equal the reference's), the fluid suites (printed lines, failing tests), chain-100
# exact twice (tiered, and with every science method compiled before its first call), and the placement-sequence fuzz in
# both distributions (first hold per seed). compare.js prints the tables to <out-dir>/table.md and exits 1 on any real
# difference; this script exits with its status (2 on a setup error).
#
# Env: ROOT (worktree to test; default the one holding this script), SEEDS (default 1-200), THREADS (fuzz workers,
#      default 4), FUZZ=0 (skip the fuzz), BUILD=auto|always|never (default auto: build when missing or stale),
#      FUZZ_RUNNER (folder of the Gradle-free fuzz runner; default <ROOT>/tools/phase-ports-probes/fuzz-runner, else a
#      copy of branch claude/fuzz-runner extracted into <ROOT>/build/winsweep/fr), JAVAC_HOME (compiles MiniLauncher and
#      MathSweep; default C:/Program Files/Java/jdk-21.0.11).
set -uo pipefail
unset JAVA_TOOL_OPTIONS
HERE=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
ROOT=$(cd "${ROOT:-$HERE/../../..}" && pwd)
[ $# -ge 2 ] || { sed -n 2,20p "$0"; exit 2; }
OUTDIR=$1; shift
mkdir -p "$OUTDIR"; OUTDIR=$(cd "$OUTDIR" && pwd)
SEEDS=${SEEDS:-1-200}; THREADS=${THREADS:-4}; FUZZ=${FUZZ:-1}; BUILD=${BUILD:-auto}
JAVAC_HOME=${JAVAC_HOME:-"C:/Program Files/Java/jdk-21.0.11"}
FB="$ROOT/build/fuzz-runner"; WB="$ROOT/build/winsweep"
PROBES="$ROOT/tools/phase-ports-probes"
PKG=com.wormzjl.createcheme
win() { cygpath -w "$1"; }

# ---- build (at most one Gradle invocation) -----------------------------------------------------------------------------
fuzz_runner() {  # the runner's folder: given, in the tree under test, or extracted read-only from its branch
  if [ -n "${FUZZ_RUNNER:-}" ]; then echo "$FUZZ_RUNNER"; return; fi
  if [ -f "$PROBES/fuzz-runner/build.sh" ]; then echo "$PROBES/fuzz-runner"; return; fi
  local x="$WB/fr/tools/phase-ports-probes/fuzz-runner"
  if [ ! -f "$x/build.sh" ]; then
    mkdir -p "$WB/fr" && git -C "$ROOT" archive claude/fuzz-runner tools/phase-ports-probes/fuzz-runner | tar -x -C "$WB/fr" || return 1
  fi
  echo "$x"
}
stale=0
[ -f "$FB/stamp" ] || stale=1
[ $stale = 0 ] && [ -n "$(find "$ROOT/src" "$PROBES/hold-fuzz/src" "$PROBES/hold-classes/src" -type f -newer "$FB/stamp" 2>/dev/null | head -1)" ] && stale=1
if [ "$BUILD" = always ] || { [ "$BUILD" = auto ] && [ $stale = 1 ]; }; then
  fr=$(fuzz_runner) || { echo "winsweep: no fuzz runner (set FUZZ_RUNNER)"; exit 2; }
  echo "winsweep: building $ROOT (one Gradle invocation, $fr/build.sh)"
  bash "$fr/build.sh" "$ROOT" || { echo "winsweep: build failed"; exit 2; }
elif [ $stale = 1 ]; then echo "winsweep: build missing or stale and BUILD=never"; exit 2; fi
mkdir -p "$WB/launcher" "$WB/mathsweep"
junit=$(node "$HERE/argfile.js" "$FB/classpath.args" "$(win "$WB/launcher")" "$WB/classpath.args") || exit 2
"$JAVAC_HOME/bin/javac" --release 21 -nowarn -d "$(win "$WB/launcher")" -cp "$junit" "$(win "$ROOT/tools/jdk-determinism/perf/src/MiniLauncher.java")" || exit 2
"$JAVAC_HOME/bin/javac" --release 17 -nowarn -d "$(win "$WB/mathsweep")" "$(win "$ROOT/tools/jdk-determinism/src/MathSweep.java")" || exit 2
WORKDIR=$(tr -d '\r\n' < "$FB/workdir.txt")
# Probe classes the fuzz runner build compiles into the test output (not part of any suite): excluded by simple name.
excl=$(find "$PROBES/hold-fuzz/src" "$PROBES/hold-classes/src" -name '*.java' -printf '%f\n' 2>/dev/null | sed -E 's/\.java$//' | sort -u | sed -E 's/^/exclude:.*\\./; s/$/$/' | tr '\n' ' ')
fluidcls=$(cd "$ROOT/build/classes/java/test" && ls com/wormzjl/createcheme/runtime/Fluid*Test.class 2>/dev/null | sed -E 's#/#.#g; s#\.class$##; s#^#class:#' | tr '\n' ' ')
HEAD=$(git -C "$ROOT" rev-parse --short HEAD)$(git -C "$ROOT" diff --quiet HEAD -- src || echo '+dirty')
echo "root=$ROOT head=$HEAD seeds=$SEEDS threads=$THREADS fuzz=$FUZZ build-stamp=$(tr -d '\r\n' < "$FB/stamp")" > "$OUTDIR/sweep.txt"
echo "winsweep: $(cat "$OUTDIR/sweep.txt")"

# ---- per runtime -------------------------------------------------------------------------------------------------------
launch() {  # launch <java> <log> <extra JVM options...> -- <main class and args...>  (the test task's JVM, only java changes)
  local j=$1 log=$2; shift 2; local opts=()
  while [ $# -gt 0 ] && [ "$1" != -- ]; do opts+=("$1"); shift; done; shift
  (cd "$WORKDIR" && "$j" @"$(win "$FB/jvm.args")" "${opts[@]}" @"$(win "$WB/classpath.args")" "$@") > "$log" 2>&1
}
REGRESSION=(-Dfluid.regression.mode=exact -Dfluid.regression.capture=false -- MiniLauncher "class:$PKG.fluid.benchmark.FluidSolverRegressionTest")
i=0
for home in "$@"; do
  i=$((i+1)); j="$home/bin/java"
  [ -x "$j" ] || [ -x "$j.exe" ] || { echo "winsweep: no java in $home"; exit 2; }
  d="$OUTDIR/r$i"; mkdir -p "$d"; rm -f "$d"/*.log
  echo "$home" > "$d/home.txt"
  "$j" -XshowSettings:properties -version > "$d/identity.txt" 2>&1
  openj9=0; grep -q 'OpenJ9' "$d/identity.txt" && openj9=1
  [ $openj9 = 0 ] && "$j" -XX:+PrintFlagsFinal -version 2>/dev/null | grep -E ' (UseJVMCICompiler|EnableJVMCI|UseJVMCINativeLibrary|TieredCompilation|UseFMA) ' >> "$d/identity.txt"
  graal=0; grep -qE 'UseJVMCICompiler +:?= true' "$d/identity.txt" && graal=1
  echo "== r$i $(grep -m1 -E 'java.vm.name' "$d/identity.txt" | sed 's/.*= //') $(grep -m1 -E 'java.runtime.version' "$d/identity.txt" | sed 's/.*= //') graal=$graal openj9=$openj9"
  "$j" -cp "$(win "$WB/mathsweep")" MathSweep > "$d/mathsweep.txt" 2>&1
  # JIT evidence for the suite run: every HotSpot-family compile with tier and JVM uptime; on GraalVM also Graal's own log.
  jopts=(); forced=()
  if [ $openj9 = 1 ]; then
    forced=(-Xjit:count=0)   # every method compiled at its first call
  else
    jopts=("-Xlog:jit+compilation=debug:file=\"$(win "$d/jit.log")\":uptime,level:filecount=0")
    [ $graal = 1 ] && jopts+=(-Djdk.graal.PrintCompilation=true "-Djdk.graal.LogFile=$(win "$d/graal-compilation.log")")
    # every science method compiled by the top-tier compiler (C2, or Graal on GraalVM) before its first call
    forced=(-Xcomp -XX:-TieredCompilation -XX:CompileCommand=quiet "-XX:CompileCommand=compileonly,$PKG.science.*::*")
  fi
  # shellcheck disable=SC2086
  launch "$j" "$d/suite.txt" "${jopts[@]}" -- MiniLauncher package:$PKG.science.fluid package:$PKG.runtime.fluid $fluidcls $excl
  grep -m1 MINILAUNCHER "$d/suite.txt"
  launch "$j" "$d/regression.txt" "${REGRESSION[@]}"
  grep -m1 -oE 'chain-100 vs reference: max deviation: state/moles [^ ]+' "$d/regression.txt"
  launch "$j" "$d/regression-forced.txt" "${forced[@]}" "${REGRESSION[@]}"
  grep -m1 -oE 'chain-100 vs reference: max deviation: state/moles [^ ]+' "$d/regression-forced.txt" | sed 's/^/forced: /'
  if [ "$FUZZ" = 1 ]; then
    for dist in random defaults; do
      dflag=false; [ $dist = defaults ] && dflag=true
      launch "$j" "$d/fuzz-$dist.java.txt" -DholdFuzz.seeds="$SEEDS" -DholdFuzz.threads="$THREADS" -DholdFuzz.slices=120 \
        -DholdFuzz.verbose=false -DholdFuzz.strict=false -DholdFuzz.defaults=$dflag -DholdFuzz.out="$(win "$d/fuzz-$dist-report.txt")" \
        -- FuzzRunnerLauncher "method:$PKG.runtime.fluid.HoldFuzzTest#placementSequenceFuzz"
      bash "$PROBES/hold-fuzz/classify.sh" "$d/fuzz-$dist-report.txt" > /dev/null
      grep -h '^SUMMARY' "$d/fuzz-$dist-report.txt" | cut -d' ' -f2-3
    done
  fi
done
node "$HERE/compare.js" "$OUTDIR" $i
