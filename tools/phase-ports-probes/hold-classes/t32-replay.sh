#!/usr/bin/env bash
# t32: replays a minimized list (HoldMinimizerTest.replay) once, with the switches of JAVA_TOOL_OPTIONS (t32-lane.sh
# XOPTS=...), and keeps the Gradle output (the trace prints of src/instr-close-trace-on-d15661b8.patch land there). From the
# worktree root, inside a lane. Usage: t32-replay.sh <logs/NN-tag> <r|d> <seed> <keep list "2@360,3@360">
set -u
H=tools/phase-ports-probes/hold-classes; flag=false; [ "$2" = d ] && flag=true
export JAVA_HOME="C:/Program Files/Java/jdk-21.0.11" JAVA_OPTS=-Xshare:off
./gradlew --no-configuration-cache --init-script $H/hold-classes.init.gradle test --tests '*HoldMinimizerTest.replay' \
  -PreplaySeed=$3 "-PreplayKeep=$4" -PholdFuzzDefaults=$flag ${EXTRA:-} --console=plain > "$H/$1.gradle.txt" 2>&1
echo "held=$(grep -c 'REPLAY_HELD' "$H/$1.gradle.txt") $(grep -h 'REPLAY_HELD' "$H/$1.gradle.txt" | head -1 | cut -c1-250) $(grep -h 'BUILD' "$H/$1.gradle.txt")"
