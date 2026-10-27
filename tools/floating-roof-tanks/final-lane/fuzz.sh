#!/usr/bin/env bash
# The M3 quick check and the cold-package fuzz: the placement-sequence hold fuzz on seeds $2 (default 1-200), 8 workers,
# one Gradle invocation per distribution through tools/unified-thermo-port-m1/gradle-locked.sh (the build/gradle.lock
# protocol), classified by the main checkout's tools/phase-ports-probes/hold-fuzz/classify.sh (read in place).
#   bundled: HARNESS=<M2 hold-fuzz> bash fuzz.sh <label> [seeds]                  (DISTS="random defaults", the default)
#   cold:    HARNESS=<this folder>/hold-fuzz DISTS=random EXTRA=-PholdFuzzCold=true bash fuzz.sh <label> [seeds]
# Reports: $OUT/<label>-<dist>-report.txt and .classes.txt, Gradle logs beside them (OUT default build/m3-logs).
set -u
label="$1"; seeds="${2:-1-200}"
WT=D:/Minecraft/Modding/1.21/CreateChemE/.claude/worktrees/rename-ichem-mod-30e50c
classify="D:/Minecraft/Modding/1.21/CreateChemE/tools/phase-ports-probes/hold-fuzz/classify.sh"
harness="${HARNESS:-$WT/tools/unified-thermo-port-m2/hold-fuzz}"
out="${OUT:-$WT/build/m3-logs}"
mkdir -p "$out"
for dist in ${DISTS:-random defaults}; do
  d=false; [ $dist = defaults ] && d=true
  bash D:/Minecraft/Modding/1.21/CreateChemE/.claude/worktrees/rename-ichem-mod-30e50c/build/tmp/lane/gradle-locked.sh "$out/$label-$dist.gradle.txt" --no-configuration-cache --init-script "$harness/hold-fuzz.init.gradle" \
    test --tests '*HoldFuzzTest' -PholdFuzzSeeds=$seeds -PholdFuzzThreads=${THREADS:-8} -PholdFuzzCounters=true -PholdFuzzDefaults=$d \
    -PholdFuzzOut="$out/$label-$dist-report.txt" ${EXTRA:-}
  bash "$classify" "$out/$label-$dist-report.txt" > /dev/null
  echo "== $dist"; grep -E '^(SUMMARY|COUNTERS)' "$out/$label-$dist-report.txt"; cut -c1-90 "$out/$label-$dist-report.txt.classes.txt"
done
