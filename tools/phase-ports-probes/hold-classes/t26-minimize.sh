#!/usr/bin/env bash
# t26: minimizes a fuzz seed with one of t26's rules switched off (needs src/instr-t26-pass-trace.patch applied: the
# scratch switch -Dt26off=<rule>), keeping its class; then replays the minimized list with the rule on. From the worktree
# root, inside a lane, 4 threads. Usage: t26-minimize.sh <logs/NN-tag> <r|d> <seed> <class> <rule>
set -u
H=tools/phase-ports-probes/hold-classes; tag=$1; d=$2; seed=$3; cls=$4; rule=$5; flag=false; [ "$d" = d ] && flag=true
export JAVA_HOME="C:/Program Files/Java/jdk-21.0.11" JAVA_OPTS=-Xshare:off
out="$H/$tag-$d$seed"
JAVA_TOOL_OPTIONS="-Dt26off=$rule" ./gradlew --no-configuration-cache --init-script $H/hold-classes.init.gradle test --tests '*HoldMinimizerTest.minimize' \
  -PminimizeSeed=$seed -PminimizeClass=$cls -PholdFuzzDefaults=$flag -PminimizeThreads=4 ${EXTRA:-} --console=plain > "$out-min.gradle.txt" 2>&1
grep -h "MINIMIZE seed=.* \(RESULT\|KEEP\|LIST\)" "$out-min.gradle.txt" | cut -c1-600
keep=$(grep -h "MINIMIZE seed=.* KEEP" "$out-min.gradle.txt" | sed -E 's/.* KEEP //' | tr -d '\r')
[ -z "$keep" ] && { echo "no KEEP line"; exit 1; }
for mode in on off; do
  opt=""; [ $mode = off ] && opt="-Dt26off=$rule"
  JAVA_TOOL_OPTIONS="$opt" ./gradlew --no-configuration-cache --init-script $H/hold-classes.init.gradle test --tests '*HoldMinimizerTest.replay' \
    -PreplaySeed=$seed "-PreplayKeep=$keep" -PholdFuzzDefaults=$flag --console=plain > "$out-replay-$mode.gradle.txt" 2>&1
  echo "== rule $mode: held=$(grep -c 'REPLAY_HELD' "$out-replay-$mode.gradle.txt") $(grep -h 'REPLAY_HELD' "$out-replay-$mode.gradle.txt" | head -1 | cut -c1-300) $(grep -h 'BUILD' "$out-replay-$mode.gradle.txt")"
done
