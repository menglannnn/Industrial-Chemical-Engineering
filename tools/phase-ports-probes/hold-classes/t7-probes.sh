#!/usr/bin/env bash
# The t7 start/steady probe on the three worlds (two-generator world draining into a tank, the same into a void, card E4 =
# defaults seed 6), one Gradle invocation each, on whatever form the working tree holds. From the worktree root.
# Usage: t7-probes.sh <logs/NN-tag>
set -u
p="tools/phase-ports-probes/hold-classes/$1"
export JAVA_HOME="C:/Program Files/Java/jdk-21.0.11" JAVA_OPTS=-Xshare:off
run(){ ./gradlew --no-configuration-cache --init-script tools/phase-ports-probes/hold-classes/t7.init.gradle test --tests '*T7StartProbeTest' -Pt7mixprint=true "$@" --console=plain; }
run -Pt7world=tank > "$p-tank.gradle.txt" 2>&1
run -Pt7world=void > "$p-void.gradle.txt" 2>&1
run -Pt7world=widevoid > "$p-widevoid.gradle.txt" 2>&1
run -Pt7seed=6 -PholdFuzzDefaults=true > "$p-seed6.gradle.txt" 2>&1
for w in tank void widevoid seed6; do echo "== $w"; grep -E "T7_(START|FIRST|END|HELD|DONE|MIX)" "$p-$w.gradle.txt" | sed 's/^ *//' | cut -c1-260; done
