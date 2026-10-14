#!/usr/bin/env bash
# t16: contrast minimizations (src-t16/T16ContrastMinimizerTest.java, needs src/scratch-t16-switch.patch applied in the
# working tree): a smallest placement list that holds in its class under BAD (default exact, the rest test before D42) and
# runs clean under GOOD (default on). From the worktree root. One Gradle invocation per seed.
# Usage: t16-contrast.sh <logs/NN-tag> <defaults|random> <seed> [<seed> ...]   (BAD, GOOD, THREADS default 4, SLICES 120)
set -u
H=tools/phase-ports-probes/hold-classes; p="$H/$1"; d=false; [ "$2" = defaults ] && d=true; shift 2
export JAVA_HOME="C:/Program Files/Java/jdk-21.0.11" JAVA_OPTS=-Xshare:off
for seed in "$@"; do
  ./gradlew --no-configuration-cache --init-script $H/t16.init.gradle test --tests '*T16ContrastMinimizerTest' -Pt16switch=true \
    -Pt16bad=${BAD:-exact} -Pt16good=${GOOD:-on} -PminimizeSeed=$seed -PminimizeThreads=${THREADS:-4} -PminimizeSlices=${SLICES:-120} \
    -PholdFuzzDefaults=$d --console=plain > "$p-$seed.gradle.txt" 2>&1
  grep -h "CONTRAST seed=$seed \(start\|KEEP\|RESULT\|LIST\)" "$p-$seed.gradle.txt" | sed 's/^ *//' | cut -c1-400
done
