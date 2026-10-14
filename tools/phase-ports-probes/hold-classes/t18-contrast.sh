#!/usr/bin/env bash
# t18: contrast minimizations (src-t18/T18ContrastMinimizerTest.java, needs src/scratch-t18-switch.patch applied in the
# working tree): a smallest placement list that holds in its class under BAD (default on, the code) and runs clean under
# GOOD (default nocolumn). From the worktree root. One Gradle invocation per seed.
# Usage: t18-contrast.sh <logs/NN-tag> <defaults|random> <seed> [<seed> ...]   (BAD, GOOD, THREADS default 8, SLICES 120)
set -u
H=tools/phase-ports-probes/hold-classes; p="$H/$1"; d=false; [ "$2" = defaults ] && d=true; shift 2
export JAVA_HOME="C:/Program Files/Java/jdk-21.0.11" JAVA_OPTS=-Xshare:off
for seed in "$@"; do
  ./gradlew --no-configuration-cache --init-script $H/t18.init.gradle test --tests '*T18ContrastMinimizerTest' \
    -Pt18bad=${BAD:-on} -Pt18good=${GOOD:-nocolumn} -PminimizeSeed=$seed -PminimizeThreads=${THREADS:-8} -PminimizeSlices=${SLICES:-120} \
    -PholdFuzzDefaults=$d --console=plain > "$p-$seed.gradle.txt" 2>&1
  grep -h "CONTRAST seed=$seed \(bad\|KEEP\|RESULT\|LIST\)" "$p-$seed.gradle.txt" | sed 's/^ *//' | cut -c1-400
  grep -h -E "error:|FAILED" "$p-$seed.gradle.txt" | head -5
done
