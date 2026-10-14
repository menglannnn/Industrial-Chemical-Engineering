#!/usr/bin/env bash
# t14 (2026-10-13): the G1 fluid suites of gates.sh under one mode of the scratch switch src/scratch-t14-carry-switch.patch
# (-Dt14carry=off|trace|on, passed through JAVA_TOOL_OPTIONS), summarized like gates.sh. From the worktree root.
# Usage: t14-g1mode.sh <logs/NN-tag> <mode>
set -u
p="tools/phase-ports-probes/hold-classes/$1"
export JAVA_HOME="C:/Program Files/Java/jdk-21.0.11" JAVA_OPTS=-Xshare:off JAVA_TOOL_OPTIONS="-Dt14carry=$2"
./gradlew --no-configuration-cache --no-build-cache test --rerun --tests 'com.wormzjl.createcheme.science.fluid.*' --tests 'com.wormzjl.createcheme.runtime.fluid.*' --console=plain --continue > "$p-G1-fluid-suites.gradle.txt" 2>&1
bash tools/phase-ports-probes/holds3/summarize.sh "$p-G1"
cat build/test-results/test/TEST-*.xml | sed -E 's/^.*<!\[CDATA\[//' | tr -d '\r' > "$p-G1-stdout.txt"
