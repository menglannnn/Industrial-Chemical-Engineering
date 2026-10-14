#!/usr/bin/env bash
# t19: one variant of the scratch switches (JAVA_TOOL_OPTIONS, e.g. "-Dt19fail=mono") on the suite tests the cap mode
# touched, the E6-C and quick-check seeds alone, and fuzz seeds 1-200 against ig10. From the worktree root, inside a lane.
# Usage: t19-variant.sh <logs/NN-tag> "<java tool options>"
set -u
p="tools/phase-ports-probes/hold-classes/$1"; H=tools/phase-ports-probes/hold-classes/logs
export JAVA_HOME="C:/Program Files/Java/jdk-21.0.11" JAVA_OPTS=-Xshare:off JAVA_TOOL_OPTIONS="$2"
./gradlew --no-configuration-cache test --rerun --tests '*ExtremeTopologyIslandTest' --tests '*StepStartFlowsTest' --tests '*InlineFilterLineHoldTest' \
  --tests '*LiquidFullTankOutletsTest' --tests '*TraceFlushLiquidFullTankTest' --tests '*VelocityClampTest' --console=plain --continue > "$p-tests.gradle.txt" 2>&1
echo "tests: $(grep -cE 'FAILED$' "$p-tests.gradle.txt") failed"; grep -E 'FAILED$' "$p-tests.gradle.txt" | grep -v "^> Task"
bash tools/phase-ports-probes/hold-classes/alone.sh "$1-alone" "151,173,286,336,529,594,35,47,108" "164,78,101" | grep -E "^(E|C|F|A|D)[0-9 ]" | sort -u
bash tools/phase-ports-probes/hold-classes/campaign.sh "$1-fuzz200" 1-200 > /dev/null 2>&1
for d in random defaults; do
  echo "== $d $(grep '^SUMMARY' "$p-fuzz200-$d-report.txt" | cut -d' ' -f2-3) base $(grep -c 'holds=0 classes=\[\]' $H/ca16-base200-$d-report.txt)"
  bash tools/phase-ports-probes/hold-classes/compare.sh $H/ca16-base200-$d-report.txt "$p-fuzz200-$d-report.txt"
  grep COUNTERS "$p-fuzz200-$d-report.txt" | grep -oE "newtonSolves=[0-9]+ newtonIterations=[0-9]+|equationGateRejections=[0-9]+" | tr '\n' ' '; echo
done
