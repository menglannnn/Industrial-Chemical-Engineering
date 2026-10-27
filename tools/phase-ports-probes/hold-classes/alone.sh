#!/usr/bin/env bash
# Replays fuzz seeds alone (one worker, one Gradle invocation per distribution), classified like a campaign. From the
# worktree root. Usage: alone.sh <logs/NN-tag> <random seeds "5,17"|-> <defaults seeds "9,19"|->  (- skips a distribution)
set -u
p="tools/phase-ports-probes/hold-classes/$1"
export JAVA_HOME="C:/Program Files/Java/jdk-21.0.11" JAVA_OPTS=-Xshare:off
for dist in random defaults; do
  seeds=$2; d=false; [ $dist = defaults ] && { seeds=$3; d=true; }
  [ "$seeds" = "-" ] && continue
  ./gradlew --no-configuration-cache --init-script tools/phase-ports-probes/hold-fuzz/hold-fuzz.init.gradle test --tests '*HoldFuzzTest' \
    ${EXTRA:-} -PholdFuzzSeeds=$seeds -PholdFuzzThreads=1 -PholdFuzzCounters=true -PholdFuzzDefaults=$d -PholdFuzzOut="$PWD/$p-$dist-report.txt" --console=plain > "$p-$dist.gradle.txt" 2>&1
  bash tools/phase-ports-probes/hold-fuzz/classify.sh "$p-$dist-report.txt"
  echo "== $dist"; grep -E '^SUMMARY' "$p-$dist-report.txt"; cut -c1-150 "$p-$dist-report.txt.classes.txt"
done
