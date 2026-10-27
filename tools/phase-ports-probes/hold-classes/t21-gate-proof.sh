#!/usr/bin/env bash
# t21: the gate test FlowFormRowTest alone, with the flow form (the code) and with the loss row restored by
# src/scratch-t21-loss-row.patch (working tree only, reversed after). From the worktree root, inside a lane.
# Usage: t21-gate-proof.sh <logs/NN-tag>
set -u
H=tools/phase-ports-probes/hold-classes; p="$H/$1"
export JAVA_HOME="C:/Program Files/Java/jdk-21.0.11" JAVA_OPTS=-Xshare:off
# The incremental test compile lost main-side symbols after a main-only change twice in this worktree (2026-10-13), so
# both runs recompile from scratch first.
run(){ ./gradlew --no-configuration-cache compileJava compileTestJava --rerun-tasks --console=plain > "$p-$1-compile.gradle.txt" 2>&1
  ./gradlew --no-configuration-cache test --rerun --tests '*FlowFormRowTest' --console=plain > "$p-$1.gradle.txt" 2>&1
  cp build/test-results/test/TEST-com.wormzjl.createcheme.runtime.fluid.FlowFormRowTest.xml "$p-$1-result.xml" 2>/dev/null
  echo "$1: $(grep -cE 'FAILED$' "$p-$1.gradle.txt") failed; $(grep -h 'EVENT_PATH_HELD' "$p-$1-result.xml" 2>/dev/null | head -1 | cut -c1-260)"; }
run flowform
git apply $H/src/scratch-t21-loss-row.patch && run lossrow; git apply -R $H/src/scratch-t21-loss-row.patch
