#!/usr/bin/env bash
# Replays a minimized fuzz list (src-t8/T8ReplayProbeTest.java) with option K and with the re-mint
# (src/scratch-t11-keep-switch.patch applied, -Pt11keep=on|remint|strict); prints (src/instr-t9-balance-print.patch -Pinstr=mprime, others)
# only with INSTR set. From the worktree root.
# Usage: t11-replay.sh <logs/NN-tag> <defaults|random> <seed> <KEEP> [slices]
set -u
p="tools/phase-ports-probes/hold-classes/$1"; d=false; [ "$2" = defaults ] && d=true; seed=$3; keep=$4; slices=${5:-120}
export JAVA_HOME="C:/Program Files/Java/jdk-21.0.11" JAVA_OPTS=-Xshare:off
for mode in ${MODES:-on remint}; do
  ./gradlew --no-configuration-cache --init-script tools/phase-ports-probes/hold-classes/t11.init.gradle test --tests '*T8ReplayProbeTest' \
    -Pt11keep=$mode -Pt8seed=$seed -Pt8keep="$keep" -Pt8slices=$slices -PholdFuzzDefaults=$d -Pinstr=${INSTR:-none} --console=plain > "$p-$seed-$mode.gradle.txt" 2>&1
  echo "== $seed keep=$mode"; grep -E "T8_(BLOCK|HELD|DONE)|T7_(START|FIRST)|M9_BAL|FAILED" "$p-$seed-$mode.gradle.txt" | sed 's/^ *//' | cut -c1-330
done
