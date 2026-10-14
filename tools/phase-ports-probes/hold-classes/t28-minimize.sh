#!/usr/bin/env bash
# t28: minimizes a fuzz seed with one of t28's rules switched off (the working-tree scratch switch -Dt28off=<rule>, removed
# before each commit), keeping its class; then replays the minimized list with the rule on and off. From the worktree root,
# inside a lane, 8 threads. Usage: t28-minimize.sh <logs/NN-tag> <r|d> <seed> <class> <rule> [match regex]
set -u
H=tools/phase-ports-probes/hold-classes; tag=$1; d=$2; seed=$3; cls=$4; rule=$5; match=${6:-}; flag=false; [ "$d" = d ] && flag=true
export JAVA_HOME="C:/Program Files/Java/jdk-21.0.11" JAVA_OPTS=-Xshare:off
out="$H/$tag-$d$seed"; m=(); [ -n "$match" ] && m=("-PminimizeMatch=$match")
JAVA_TOOL_OPTIONS="-Dt28off=$rule" ./gradlew --no-configuration-cache --init-script $H/hold-classes.init.gradle test --tests '*HoldMinimizerTest.minimize' \
  -PminimizeSeed=$seed -PminimizeClass=$cls -PholdFuzzDefaults=$flag -PminimizeThreads=${THREADS:-8} "${m[@]}" ${EXTRA:-} --console=plain > "$out-min.gradle.txt" 2>&1
grep -h "MINIMIZE seed=.* \(RESULT\|KEEP\|LIST\)" "$out-min.gradle.txt" | cut -c1-600
keep=$(grep -h "MINIMIZE seed=.* KEEP" "$out-min.gradle.txt" | sed -E 's/.* KEEP //' | tr -d '\r')
[ -z "$keep" ] && { echo "no KEEP line"; exit 1; }
for mode in on off; do
  opt=""; [ $mode = off ] && opt="-Dt28off=$rule"
  JAVA_TOOL_OPTIONS="$opt" ./gradlew --no-configuration-cache --init-script $H/hold-classes.init.gradle test --tests '*HoldMinimizerTest.replay' \
    -PreplaySeed=$seed "-PreplayKeep=$keep" -PholdFuzzDefaults=$flag --console=plain > "$out-replay-$mode.gradle.txt" 2>&1
  echo "== rule $mode: held=$(grep -c 'REPLAY_HELD' "$out-replay-$mode.gradle.txt") $(grep -h 'REPLAY_HELD' "$out-replay-$mode.gradle.txt" | head -1 | cut -c1-300) $(grep -h 'BUILD' "$out-replay-$mode.gradle.txt")"
done
