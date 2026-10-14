#!/usr/bin/env bash
# Replays a minimized fuzz list (src-t8/T8ReplayProbeTest.java) under modes of src/scratch-t12-switch.patch (applied):
# MODES (default "on k"), each of on|replace|nodisplace|k|remint; INSTR forwards -Pinstr; PRINT=true prints every kept
# junction's displacement test (T12_DISPLACE). From the worktree root.
# Usage: t12-replay.sh <logs/NN-tag> <defaults|random> <seed> <KEEP> [slices]
set -u
p="tools/phase-ports-probes/hold-classes/$1"; d=false; [ "$2" = defaults ] && d=true; seed=$3; keep=$4; slices=${5:-120}
export JAVA_HOME="C:/Program Files/Java/jdk-21.0.11" JAVA_OPTS=-Xshare:off
for mode in ${MODES:-on k}; do
  JAVA_TOOL_OPTIONS="-Dt12print=${PRINT:-false}" ./gradlew --no-configuration-cache --init-script tools/phase-ports-probes/hold-classes/t12.init.gradle test --tests '*T8ReplayProbeTest' \
    -Pt12mode=$mode -Pt8seed=$seed -Pt8keep="$keep" -Pt8slices=$slices -PholdFuzzDefaults=$d -Pinstr=${INSTR:-none} --console=plain > "$p-$seed-$mode.gradle.txt" 2>&1
  echo "== $seed mode=$mode"; grep -E "T8_(HELD|DONE)|T12_DISPLACE.*displaced=true|FAILED" "$p-$seed-$mode.gradle.txt" | sed 's/^ *//' | cut -c1-330 | head -${LINES_MAX:-12}
done
