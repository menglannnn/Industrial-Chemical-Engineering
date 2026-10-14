#!/usr/bin/env bash
# t10 contrast minimizations (src-t10/T10ContrastMinimizerTest.java), one Gradle invocation per seed, with
# src/scratch-t10-column-switch.patch applied. From the worktree root.
# Usage: t10-contrast.sh <logs/NN-tag> <defaults|random> <worse|better> seed...
set -u
p="tools/phase-ports-probes/hold-classes/$1"; d=false; [ "$2" = defaults ] && d=true; want=$3; shift 3
export JAVA_HOME="C:/Program Files/Java/jdk-21.0.11" JAVA_OPTS=-Xshare:off
for s in "$@"; do
  ./gradlew --no-configuration-cache --init-script tools/phase-ports-probes/hold-classes/t10.init.gradle test --tests '*T10ContrastMinimizerTest' \
    -Pt10switch=true -Pt10want=$want -PminimizeSeed=$s -PholdFuzzDefaults=$d -PminimizeThreads=${THREADS:-8} ${EXTRA:-} --console=plain > "$p-$s.gradle.txt" 2>&1
  echo "== $s"; grep -E "CONTRAST seed=$s (want|start|KEEP|RESULT|LIST)|FAILED|error:" "$p-$s.gradle.txt" | sed 's/^ *//' | cut -c1-400
done
