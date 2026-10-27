#!/usr/bin/env bash
# t21: HoldMinimizerTest on one fuzz seed (greedy block removal keeping its first hold's class), or its replay of a KEEP list.
# From the worktree root, inside a lane; the row form is whatever the working tree holds (src/scratch-t21-loss-row.patch
# restores the loss row). Usage: t21-minimize.sh <logs/NN-tag> min <r|d> <seed> <class> | replay <r|d> <seed> '<KEEP>'
set -u
H=tools/phase-ports-probes/hold-classes; tag=$1; mode=$2; d=$3; seed=$4; flag=false; [ "$d" = d ] && flag=true
export JAVA_HOME="C:/Program Files/Java/jdk-21.0.11" JAVA_OPTS=-Xshare:off
out="$H/$tag-$mode-$d$seed.gradle.txt"
if [ "$mode" = min ]; then
  ./gradlew --no-configuration-cache --init-script $H/hold-classes.init.gradle test --tests '*HoldMinimizerTest.minimize' -PminimizeSeed=$seed \
    -PholdFuzzDefaults=$flag -PminimizeClass=$5 -PminimizeThreads=${THREADS:-4} -PminimizeSlices=${SLICES:-40} --console=plain > "$out" 2>&1
  grep -h "MINIMIZE seed" "$out" | cut -c1-400
else
  ./gradlew --no-configuration-cache --init-script $H/hold-classes.init.gradle test --tests '*HoldMinimizerTest.replay' -PreplaySeed=$seed \
    -PholdFuzzDefaults=$flag "-PreplayKeep=$5" -PminimizeSlices=${SLICES:-40} --console=plain > "$out" 2>&1
  grep -hE "HELD|held|CLEAN|clean|REPLAY" "$out" | head -5 | cut -c1-300
fi
