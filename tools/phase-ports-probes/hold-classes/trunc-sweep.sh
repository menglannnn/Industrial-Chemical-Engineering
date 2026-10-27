#!/usr/bin/env bash
# Group C (trace truncation in the flash, decision D34): the phase threshold swept on the committed fold. Applies
# src/scratch-phase-threshold.patch (never commit: -Dtruncphase=<share> overrides FluidThermodynamics.PHASE_TRACE_FRACTION,
# 0 disables the fold; -Dtruncprint=true prints every fold as TRUNC_FOLD phase= share= kg=), then for each threshold runs
# the fluid suites with every fold printed (summary, junction lines against ../holds/12-junction-lines.txt), the
# exact regression and the 1000-seed campaign in both distributions; reverses the patch at the end. From the worktree
# root, one Gradle invocation at a time. Usage: trunc-sweep.sh <logs/NN-tag> <threshold> [<threshold> ...]
set -u
tag="$1"; shift
export JAVA_HOME="C:/Program Files/Java/jdk-21.0.11" JAVA_OPTS=-Xshare:off
git apply tools/phase-ports-probes/hold-classes/src/scratch-phase-threshold.patch || exit 1
for t in "$@"; do
  p="tools/phase-ports-probes/hold-classes/$tag-$t"
  rm -f build/test-results/test/*.xml
  JAVA_TOOL_OPTIONS="-Dtruncphase=$t -Dtruncprint=true" ./gradlew --no-configuration-cache --no-build-cache test --rerun --tests 'com.wormzjl.createcheme.science.fluid.*' --tests 'com.wormzjl.createcheme.runtime.fluid.*' --console=plain --continue > "$p-G1-fluid-suites.gradle.txt" 2>&1
  bash tools/phase-ports-probes/holds3/summarize.sh "$p-G1" > /dev/null
  cat build/test-results/test/TEST-*.xml | sed -E 's/^.*<!\[CDATA\[//' | tr -d '\r' > "$p-G1-stdout.txt"
  JAVA_TOOL_OPTIONS="-Dtruncphase=$t" ./gradlew --no-configuration-cache fluidSolverRegression -PfluidRegressionMode=exact --console=plain > "$p-G2-regression-exact.gradle.txt" 2>&1
  JAVA_TOOL_OPTIONS="-Dtruncphase=$t" bash tools/phase-ports-probes/hold-classes/campaign.sh "$tag-$t-campaign" > /dev/null 2>&1
  echo "== $t"; head -4 "$p-G1-summary.txt"; echo "junction diff lines: $(wc -l < "$p-G1-junction-diff.txt")"
  grep -iE "max deviation" "$p-G2-regression-exact.gradle.txt" | tail -1
  grep -h SUMMARY "$p-campaign-random-report.txt" "$p-campaign-defaults-report.txt"
done
git apply -R tools/phase-ports-probes/hold-classes/src/scratch-phase-threshold.patch
