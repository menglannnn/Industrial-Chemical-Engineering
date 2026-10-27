#!/usr/bin/env bash
# The batch's gates, one Gradle invocation at a time, from the worktree root. Usage: gates.sh <logs/NN-tag> [gametest-run-id]
#   G1 fluid suites (+ summary, junction lines against ../holds/12-junction-lines.txt), G2 exact regression,
#   G3 adjacent selection, and G5 fluid GameTests when a run id is given.
set -u
p="tools/phase-ports-probes/hold-classes/$1"
export JAVA_HOME="C:/Program Files/Java/jdk-21.0.11" JAVA_OPTS=-Xshare:off
./gradlew --no-configuration-cache --no-build-cache test --rerun --tests 'com.wormzjl.createcheme.science.fluid.*' --tests 'com.wormzjl.createcheme.runtime.fluid.*' --tests 'com.wormzjl.createcheme.runtime.Fluid*' --console=plain --continue > "$p-G1-fluid-suites.gradle.txt" 2>&1
bash tools/phase-ports-probes/holds3/summarize.sh "$p-G1"
cat build/test-results/test/TEST-*.xml | sed -E 's/^.*<!\[CDATA\[//' | tr -d '\r' > "$p-G1-stdout.txt"
./gradlew --no-configuration-cache fluidSolverRegression -PfluidRegressionMode=exact --console=plain > "$p-G2-regression-exact.gradle.txt" 2>&1
grep -iE "max|0\.000e|differ|BUILD" "$p-G2-regression-exact.gradle.txt" | tail -8
./gradlew --no-configuration-cache --no-build-cache test --rerun --tests '*PassiveStepSolverTest' --tests '*PumpJunctionStartupTest' --tests '*FilterBlockLineIslandTest' --tests '*DeadHeadedLineIslandTest' --tests '*NetworkRegimeTest' --tests '*PhysicalFluidTopologyTest' --tests '*PipePresentationTest' --console=plain --continue > "$p-G3-adjacent.gradle.txt" 2>&1
bash tools/phase-ports-probes/holds3/summarize.sh "$p-G3" | head -1
if [ $# -ge 2 ]; then
  ./gradlew --no-configuration-cache runFluidGameTestServer -PfluidGameTestRunId="$2" --console=plain > "$p-G5-gametests.gradle.txt" 2>&1
  grep -E "required tests|BUILD" "$p-G5-gametests.gradle.txt" | tail -3
fi
