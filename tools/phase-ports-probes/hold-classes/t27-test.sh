#!/usr/bin/env bash
# t27: runs one test class alone (a gate proof, with or without a working-tree switch from JAVA_TOOL_OPTIONS, t27-lane.sh
# XOPTS=...) and keeps its Gradle output and result XML. From the worktree root, inside a lane.
# Usage: t27-test.sh <logs/NN-tag> <test class pattern, e.g. '*TurbulentWalkTest'>
set -u
H=tools/phase-ports-probes/hold-classes; p="$H/$1"
export JAVA_HOME="C:/Program Files/Java/jdk-21.0.11" JAVA_OPTS=-Xshare:off
./gradlew --no-configuration-cache test --rerun --tests "$2" --console=plain > "$p.gradle.txt" 2>&1
grep -E "BUILD|tests completed|FAILED" "$p.gradle.txt" | head -4
for f in build/test-results/test/TEST-*.xml; do case "$f" in *"${2#\*}"*) cp "$f" "$p-result.xml";; esac; done
