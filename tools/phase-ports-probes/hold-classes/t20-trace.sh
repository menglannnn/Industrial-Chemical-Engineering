#!/usr/bin/env bash
# t20 (physical start, 2026-10-13): replays fuzz seeds alone to their first hold with t13's failing-row trace
# (src-t13/T13TraceProbeTest.trace; needs src/instr-t18-t13-trace-on-47618332.patch applied in the working tree with
# git apply --ignore-whitespace, never committed), one Gradle invocation per distribution, then classifies the traces with
# t13's group rule (t13-classify.js). The start form comes from JAVA_TOOL_OPTIONS (t20-lane.sh START=...). From the
# worktree root. Usage: t20-trace.sh <logs/NN-tag> <defaults seeds "1,19"|-> <random seeds|->
set -u
H=tools/phase-ports-probes/hold-classes; p="$H/$1"; mkdir -p "$p"
export JAVA_HOME="C:/Program Files/Java/jdk-21.0.11" JAVA_OPTS=-Xshare:off
for dist in defaults random; do
  seeds=$2; flag=true; [ $dist = random ] && { seeds=$3; flag=false; }
  [ "$seeds" = "-" ] && continue
  ./gradlew --no-configuration-cache --init-script $H/t13.init.gradle test --tests '*T13TraceProbeTest.trace' -Pt13seeds=$seeds -PholdFuzzDefaults=$flag \
    -Pt13out="$PWD/$p" ${EXTRA:-} --console=plain > "$p-$dist.gradle.txt" 2>&1
  grep -h "T13_DONE" "$p-$dist.gradle.txt" | cut -c1-220 | sed "s/^/$dist /"
done | tee "$p-summary.txt" | wc -l
node $H/t13-classify.js "$p" > "$p-classification.txt"
node $H/t13-classify.js "$p" --md > "$p-classification.md"
cut -d'|' -f1 "$p-classification.txt" | sort | uniq -c
