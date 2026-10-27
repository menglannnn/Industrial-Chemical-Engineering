#!/usr/bin/env bash
# One distribution of the fuzz, seeds 1-1000, ONE worker, with a COUNTERS_SEED line per seed (Newton solves and
# iterations, gate refusals, step attempts; ../hold-fuzz/src/HoldFuzzTest.java attributes the counters per seed only with
# one worker). MODE is forwarded as -Dt12mode (src/scratch-t12-switch.patch applied). From the worktree root.
# Usage: perseed.sh <logs/NN-tag> <random|defaults> [seeds]
set -u
p="tools/phase-ports-probes/hold-classes/$1"; d=false; [ "$2" = defaults ] && d=true; seeds="${3:-1-1000}"
export JAVA_HOME="C:/Program Files/Java/jdk-21.0.11" JAVA_OPTS=-Xshare:off
JAVA_TOOL_OPTIONS="-Dt12mode=${MODE:-on}" ./gradlew --no-configuration-cache --init-script tools/phase-ports-probes/hold-fuzz/hold-fuzz.init.gradle test --tests '*HoldFuzzTest' \
  -PholdFuzzSeeds=$seeds -PholdFuzzThreads=1 -PholdFuzzCounters=true -PholdFuzzDefaults=$d -PholdFuzzOut="$PWD/$p-$2-report.txt" --console=plain > "$p-$2.gradle.txt" 2>&1
grep -E '^(SUMMARY|COUNTERS)' "$p-$2-report.txt"
