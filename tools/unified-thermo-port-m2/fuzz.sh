#!/usr/bin/env bash
# The M2 quick check: the placement-sequence hold fuzz (tools/phase-ports-probes/hold-fuzz of the main checkout, read in
# place unless HARNESS= names an adapted copy) on seeds $2 (default 1-200) in both distributions (random, world defaults),
# 8 workers, one Gradle invocation per distribution under the build/gradle.lock protocol, classified by the fuzz's
# classify.sh. Usage: fuzz.sh <label> [seeds]  ->  $OUT/<label>-{random,defaults}-report.txt(.classes.txt), Gradle logs.
set -u
label="$1"; seeds="${2:-1-200}"
here="$(cd "$(dirname "$0")" && pwd)"
fuzz="D:/Minecraft/Modding/1.21/CreateChemE/tools/phase-ports-probes/hold-fuzz"
harness="${HARNESS:-$fuzz}"
out="${OUT:-D:/Minecraft/Modding/1.21/CreateChemE/.claude/worktrees/coolprop-multiphase-thermo-37f6b0/build/m2-logs}"
mkdir -p "$out"
for dist in random defaults; do
  d=false; [ $dist = defaults ] && d=true
  bash "$here/gradle-locked.sh" "$out/$label-$dist.gradle.txt" --no-configuration-cache --init-script "$harness/hold-fuzz.init.gradle" \
    test --tests '*HoldFuzzTest' -PholdFuzzSeeds=$seeds -PholdFuzzThreads=${THREADS:-8} -PholdFuzzCounters=true -PholdFuzzDefaults=$d \
    -PholdFuzzOut="$out/$label-$dist-report.txt"
  bash "$fuzz/classify.sh" "$out/$label-$dist-report.txt" > /dev/null
  echo "== $dist"; grep -E '^(SUMMARY|COUNTERS)' "$out/$label-$dist-report.txt"; cut -c1-90 "$out/$label-$dist-report.txt.classes.txt"
done
