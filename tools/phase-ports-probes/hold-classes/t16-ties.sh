#!/usr/bin/env bash
# The t16 gauge probe on fuzz seeds (src-t16/T16TieProbeTest.java): at every hold, the held island's largest allowed
# passive drive, its last flows against the flow 0.1 Pa drives, its loops and identical generators, and its last four
# committed intervals. From the worktree root. Usage: t16-ties.sh <logs/NN-tag> <random|defaults> <seeds "47,19"> [threads]
# DESCRIBE=true adds each held island's nodes and connections.
set -u
p="tools/phase-ports-probes/hold-classes/$1"; d=false; [ "$2" = defaults ] && d=true
export JAVA_HOME="C:/Program Files/Java/jdk-21.0.11" JAVA_OPTS=-Xshare:off
./gradlew --no-configuration-cache --init-script tools/phase-ports-probes/hold-classes/t16.init.gradle test --tests '*T16TieProbeTest' \
  ${EXTRA:-} -Pt16seeds="$3" -Pt16threads=${4:-1} -Pt16describe=${DESCRIBE:-false} -PholdFuzzDefaults=$d -Pt16out="$PWD/$p-$2.txt" --console=plain > "$p-$2.gradle.txt" 2>&1
grep -c T16_HOLD "$p-$2.txt"; grep -E "BUILD|FAILED|error:" "$p-$2.gradle.txt" | head -5
