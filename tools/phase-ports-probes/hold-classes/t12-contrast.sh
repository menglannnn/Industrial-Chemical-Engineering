#!/usr/bin/env bash
# t12 contrast minimizations (src-t12/T12ContrastMinimizerTest.java), one Gradle invocation per seed, with
# src/scratch-t12-switch.patch applied. From the worktree root. BAD (default k) must hold in the class, GOOD (default on) run clean.
# Usage: t12-contrast.sh <logs/NN-tag> <defaults|random> seed...
set -u
p="tools/phase-ports-probes/hold-classes/$1"; d=false; [ "$2" = defaults ] && d=true; shift 2
export JAVA_HOME="C:/Program Files/Java/jdk-21.0.11" JAVA_OPTS=-Xshare:off
for s in "$@"; do
  ./gradlew --no-configuration-cache --init-script tools/phase-ports-probes/hold-classes/t12.init.gradle test --tests '*T12ContrastMinimizerTest' \
    -Pt12switch=true -Pt12bad=${BAD:-k} -Pt12good=${GOOD:-on} -PminimizeSeed=$s -PholdFuzzDefaults=$d -PminimizeThreads=${THREADS:-8} ${EXTRA:-} --console=plain > "$p-$s.gradle.txt" 2>&1
  echo "== $s"; grep -E "CONTRAST seed=$s (bad|start|KEEP|RESULT|LIST)|FAILED|error:" "$p-$s.gradle.txt" | sed 's/^ *//' | cut -c1-400
done
