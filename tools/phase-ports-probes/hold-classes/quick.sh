#!/usr/bin/env bash
# The between-commits check of the reduction stage (orchestrator's cadence, 2026-09-28), from the worktree root, one Gradle
# invocation at a time: G1 fluid suites (+ summary and junction lines against ../holds/12-junction-lines.txt), G2 exact
# regression, then the fuzz on seeds 1-200 in both distributions compared with the same seeds of a full campaign.
# Not a gate: the full gates.sh (with GameTests) and campaign.sh run at the end of each group.
# Usage: quick.sh <logs/NN-tag> <full-campaign prefix, e.g. logs/b18-campaign-rule3>
set -u
p="tools/phase-ports-probes/hold-classes/$1"; base="tools/phase-ports-probes/hold-classes/$2"
export JAVA_HOME="C:/Program Files/Java/jdk-21.0.11" JAVA_OPTS=-Xshare:off
rm -f build/test-results/test/*.xml
./gradlew --no-configuration-cache --no-build-cache test --rerun --tests 'com.wormzjl.createcheme.science.fluid.*' --tests 'com.wormzjl.createcheme.runtime.fluid.*' --console=plain --continue > "$p-G1-fluid-suites.gradle.txt" 2>&1
bash tools/phase-ports-probes/holds3/summarize.sh "$p-G1"
cat build/test-results/test/TEST-*.xml | sed -E 's/^.*<!\[CDATA\[//' | tr -d '\r' > "$p-G1-stdout.txt"
./gradlew --no-configuration-cache fluidSolverRegression -PfluidRegressionMode=exact --console=plain > "$p-G2-regression-exact.gradle.txt" 2>&1
grep -iE "max deviation|BUILD" "$p-G2-regression-exact.gradle.txt" | tail -2
bash tools/phase-ports-probes/hold-classes/campaign.sh "$1-fuzz200" 1-200 > /dev/null
for dist in random defaults; do
  awk '/^SEED /{keep=($2<=200)} keep' "$base-$dist-report.txt" > "$p-base200-$dist-report.txt"
  echo "== $dist: $(grep '^SUMMARY' "$p-fuzz200-$dist-report.txt" | cut -d' ' -f2-3) (base $(grep -c 'holds=0 classes=\[\]' "$p-base200-$dist-report.txt") clean of 200)"
  bash tools/phase-ports-probes/hold-classes/compare.sh "$p-base200-$dist-report.txt" "$p-fuzz200-$dist-report.txt"
done
