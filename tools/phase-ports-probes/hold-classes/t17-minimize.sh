#!/usr/bin/env bash
# t17 (integration, 2026-10-13): minimizes the worse worlds of the joint campaign, each keeping its first hold's signature
# (class, row and ends, first/later, flags) with t13's minimizer (src-t13/T13TraceProbeTest.minimize; needs
# src/instr-t13-failing-rows-and-levers.patch applied in the working tree, never committed). One Gradle invocation per
# world, 8 threads (40 slices after the last placement; EXTRA=-PminimizeSlices=120 for a later hold). From the worktree
# root. Usage: t17-minimize.sh <logs/NN-tag> <d|r> <seed> '<signature regex>' [...]
set -u
H=tools/phase-ports-probes/hold-classes; tag=$1; shift
export JAVA_HOME="C:/Program Files/Java/jdk-21.0.11" JAVA_OPTS=-Xshare:off
while [ $# -ge 3 ]; do
  d=$1; seed=$2; sig=$3; shift 3; flag=false; [ "$d" = d ] && flag=true
  out="$H/$tag-$d$seed.gradle.txt"
  ./gradlew --no-configuration-cache --init-script $H/t13.init.gradle test --tests '*T13TraceProbeTest.minimize' -Pt13min=$seed "-Pt13sig=$sig" \
    -PholdFuzzDefaults=$flag -PminimizeThreads=8 ${EXTRA:-} --console=plain > "$out" 2>&1
  echo "$d $seed: $(grep -h 'T13MIN seed=.* RESULT' "$out" | cut -c1-200) $(grep -h 'does not match' "$out" | cut -c1-120)"
done
