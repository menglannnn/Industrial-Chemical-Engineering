#!/usr/bin/env bash
# Seeds 1-1000 of the placement-sequence fuzz in both distributions (random, then world defaults), $THREADS workers (default 8), one Gradle
# invocation each, classified by ../hold-fuzz/classify.sh. From the worktree root. Usage: campaign.sh <logs/NN-tag> [seeds]
set -u
p="tools/phase-ports-probes/hold-classes/$1"; seeds="${2:-1-1000}"
export JAVA_HOME="C:/Program Files/Java/jdk-21.0.11" JAVA_OPTS=-Xshare:off
for dist in random defaults; do
  d=false; [ $dist = defaults ] && d=true
  ./gradlew --no-configuration-cache --init-script tools/phase-ports-probes/hold-fuzz/hold-fuzz.init.gradle test --tests '*HoldFuzzTest' \
    ${EXTRA:-} -PholdFuzzSeeds=$seeds -PholdFuzzThreads=${THREADS:-8} -PholdFuzzCounters=true -PholdFuzzDefaults=$d -PholdFuzzOut="$PWD/$p-$dist-report.txt" --console=plain > "$p-$dist.gradle.txt" 2>&1
  bash tools/phase-ports-probes/hold-fuzz/classify.sh "$p-$dist-report.txt"
  echo "== $dist"; grep -E '^(SUMMARY|COUNTERS)' "$p-$dist-report.txt"; cat "$p-$dist-report.txt.classes.txt" | cut -c1-80
done
