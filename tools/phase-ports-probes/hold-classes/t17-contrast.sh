#!/usr/bin/env bash
# t17 (integration, 2026-10-13): replays the minimized worse worlds (the KEEP lines of logs/ig15-min-*.gradle.txt) in the
# checkout given (this worktree, or a detached worktree of another commit) with HoldMinimizerTest.replay, 120 slices, and
# prints each world's first hold (or "clean"). Usage: t17-contrast.sh <checkout dir> <out prefix (absolute)> <d|r><seed> ...
set -u
H=$(cd "$(dirname "$0")" && pwd); dir=$1; out=$2; shift 2
export JAVA_HOME="C:/Program Files/Java/jdk-21.0.11" JAVA_OPTS=-Xshare:off
for world in "$@"; do
  d=${world:0:1}; seed=${world:1}; flag=false; [ "$d" = d ] && flag=true
  keep=$(grep -h "T13MIN seed=$seed KEEP" "$H/logs/ig15-min-$world.gradle.txt" | sed 's/.* KEEP //' | tr -d '\r')
  log="$out-$world.gradle.txt"
  (cd "$dir" && ./gradlew --no-configuration-cache --init-script tools/phase-ports-probes/hold-classes/hold-classes.init.gradle test \
    --tests '*HoldMinimizerTest.replay' -PreplaySeed=$seed "-PreplayKeep=$keep" -PholdFuzzDefaults=$flag -PminimizeSlices=120 --console=plain > "$log" 2>&1)
  held=$(grep -h "REPLAY_HELD" "$log" | head -1 | cut -c1-200)
  echo "$world: ${held:-clean} $(grep -h 'BUILD' "$log" | tail -1)"
done
