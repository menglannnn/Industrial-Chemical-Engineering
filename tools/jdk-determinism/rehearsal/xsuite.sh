#!/usr/bin/env bash
# Fluid suites (the selection of perf/timing.sh "suite": science.fluid, runtime.fluid, runtime.Fluid*Test, without
# PackagedSparseLuTest) plus chain-100 exact, for one build under each runtime given, Gradle-free (xrt.sh).
#   xsuite.sh <tag> <build: merged|base> "<runtimes>"
# Logs: logs/xrt/<tag>-<build>-<runtime>-suite.txt, -regression.txt, and -printed.txt = every stdout line of the suite with
# wall times, allocation and byte counts masked (the lines that must be identical across runtimes).
set -uo pipefail
HERE=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
ROOT=$(cd "$HERE/../../.." && pwd)
tag=$1; build=$2; runtimes=$3
case $build in merged) out="$ROOT/build/rehearsal/out" ;; base) out="$ROOT/build/rehearsal/out-base" ;; esac
fluidclasses=$(cd "$out/test" && ls com/wormzjl/createcheme/runtime/Fluid*Test.class 2>/dev/null | sed -E 's#/#.#g; s#\.class$##; s#^#class:#')
mkdir -p "$HERE/logs/xrt"
for r in $runtimes; do
  p="$HERE/logs/xrt/$tag-$build-$r"
  echo "== $tag $build $r"
  # shellcheck disable=SC2086
  bash "$HERE/xrt.sh" "$build" "$r" "$p-suite.txt" package:com.wormzjl.createcheme.science.fluid package:com.wormzjl.createcheme.runtime.fluid \
    $fluidclasses 'exclude:.*PackagedSparseLuTest'
  JVM_OPTS="-Xmx2g -Dfluid.regression.mode=exact -Dfluid.regression.capture=false" bash "$HERE/xrt.sh" "$build" "$r" "$p-regression.txt" \
    class:com.wormzjl.createcheme.fluid.benchmark.FluidSolverRegressionTest
  grep -hE "chain-100 vs reference" "$p-regression.txt" | cut -c1-200
  grep -vE '^# xrt|^MINILAUNCHER|wallMs|^\s+(at |org\.junit|java\.base)' "$p-suite.txt" | tr -d '\r' \
    | sed -E 's/ ms=[0-9.]+/ ms=*/g; s/ allocatedMB=[0-9.]+/ allocatedMB=*/g; s/ bytes=[0-9]+/ bytes=*/g; s/[0-9.]+ ?ms\b/*ms/g; s/wall(Ms|Nanos)?=[0-9.]+/wall=*/g' \
    > "$p-printed.txt"
  echo "printed lines: $(wc -l < "$p-printed.txt")"
done
