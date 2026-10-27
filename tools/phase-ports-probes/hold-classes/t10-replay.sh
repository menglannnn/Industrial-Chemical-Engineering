#!/usr/bin/env bash
# Replays a minimized fuzz list (src-t8/T8ReplayProbeTest.java) with and without the drain-side gas column
# (src/scratch-t10-column-switch.patch applied, -Pt10column=on|off), with the balance print (src/instr-t9-balance-print.patch,
# -Pinstr=mprime) and whatever -Pinstr EXTRA adds. From the worktree root.
# Usage: t10-replay.sh <logs/NN-tag> <defaults|random> <seed> <KEEP> [slices]
set -u
p="tools/phase-ports-probes/hold-classes/$1"; d=false; [ "$2" = defaults ] && d=true; seed=$3; keep=$4; slices=${5:-120}
export JAVA_HOME="C:/Program Files/Java/jdk-21.0.11" JAVA_OPTS=-Xshare:off
for column in ${COLUMNS:-on off}; do
  ./gradlew --no-configuration-cache --init-script tools/phase-ports-probes/hold-classes/t10.init.gradle test --tests '*T8ReplayProbeTest' \
    -Pt10column=$column -Pt8seed=$seed -Pt8keep="$keep" -Pt8slices=$slices -PholdFuzzDefaults=$d -Pinstr=${INSTR:-mprime} --console=plain > "$p-$seed-$column.gradle.txt" 2>&1
  echo "== $seed column=$column"; grep -E "T8_(BLOCK|HELD|DONE)|T7_(START|FIRST)|M9_BAL|FAILED" "$p-$seed-$column.gradle.txt" | sed 's/^ *//' | cut -c1-330
done
