#!/usr/bin/env bash
# t29: prints a minimized list as EventPathWorld placement code (HoldMinimizerTest.code). From the worktree root, inside a lane.
# Usage: t29-code.sh <logs/NN-tag> <r|d> <seed> <keep list "2@360,3@360">
set -u
H=tools/phase-ports-probes/hold-classes; flag=false; [ "$2" = d ] && flag=true
export JAVA_HOME="C:/Program Files/Java/jdk-21.0.11" JAVA_OPTS=-Xshare:off
./gradlew --no-configuration-cache --init-script $H/hold-classes.init.gradle test --tests '*HoldMinimizerTest.code' \
  -PreplaySeed=$3 "-PreplayKeep=$4" -PholdFuzzDefaults=$flag --console=plain > "$H/$1-code.gradle.txt" 2>&1
grep -h "CODE" "$H/$1-code.gradle.txt" | sed 's/^ *//'
