#!/usr/bin/env bash
# t18 (E9 (a), 2026-10-13): replays minimized worlds in this worktree with HoldMinimizerTest.replay, 120 slices, one Gradle
# invocation per world, and prints each world's first hold (or "clean"). A world is <d|r><seed>; its KEEP line is read from
# logs/ig15-min-<world>.gradle.txt, or from the file named by KEEPFILE_<world> (a T13MIN/MINIMIZE line with " KEEP ").
# JAVA_TOOL_OPTIONS is passed through (for example -Dt18print=true with the print patch applied).
# Usage, from the worktree root: t18-replay.sh <logs/NN-tag> <d|r><seed> ...
set -u
H=tools/phase-ports-probes/hold-classes; out="$H/$1"; shift
export JAVA_HOME="C:/Program Files/Java/jdk-21.0.11" JAVA_OPTS=-Xshare:off
for world in "$@"; do
  d=${world:0:1}; seed=${world:1}; flag=false; [ "$d" = d ] && flag=true
  var="KEEPFILE_$world"; src="${!var:-$H/logs/ig15-min-$world.gradle.txt}"
  keep=$(grep -h -E "seed=$seed KEEP|KEEP " "$src" | tail -1 | sed 's/.* KEEP //' | tr -d '\r')
  log="$out-$world.gradle.txt"
  ./gradlew --no-configuration-cache --init-script $H/hold-classes.init.gradle test --rerun \
    --tests '*HoldMinimizerTest.replay' -PreplaySeed=$seed "-PreplayKeep=$keep" -PholdFuzzDefaults=$flag -PminimizeSlices=120 --console=plain > "$log" 2>&1
  held=$(grep -h "REPLAY_HELD" "$log" | head -1 | cut -c1-220)
  echo "$world: ${held:-clean} $(grep -h 'BUILD' "$log" | tail -1)"
done
