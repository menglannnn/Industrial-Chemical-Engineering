#!/usr/bin/env bash
# D44 (t16): minimizes random-distribution fuzz seeds keeping their first hold's class with a two-phase crude node in the
# held island (src-t16/T16CrudeCaseTest.minimize), then replays each minimized list into
# tools/phase-ports-probes/saved-cases/<seed>/case.txt (T16CrudeCaseTest.replay). One Gradle invocation per seed and step.
# From the worktree root. Usage: t16-crude.sh <logs/NN-tag> <seed> [<seed> ...]   (MINTHREADS, default 4)
set -u
H=tools/phase-ports-probes/hold-classes; p="$H/$1"; shift
export JAVA_HOME="C:/Program Files/Java/jdk-21.0.11" JAVA_OPTS=-Xshare:off
for seed in "$@"; do
  ./gradlew --no-configuration-cache --init-script $H/t16.init.gradle test --tests '*T16CrudeCaseTest.minimize' -Pt16min=$seed -Pt16threads=${MINTHREADS:-4} \
    -PholdFuzzDefaults=false --console=plain > "$p-$seed-min.gradle.txt" 2>&1
  grep -h "T16MIN seed=$seed \(start\|KEEP\|RESULT\|no two\)" "$p-$seed-min.gradle.txt" | sed 's/^ *//' | cut -c1-260
  keep=$(grep -h "T16MIN seed=$seed KEEP" "$p-$seed-min.gradle.txt" | sed 's/.*KEEP //')
  [ -z "$keep" ] && continue
  ./gradlew --no-configuration-cache --init-script $H/t16.init.gradle test --tests '*T16CrudeCaseTest.replay' -Pt16case=$seed -Pt16keep="$keep" \
    -Pt16caseOut="$PWD/tools/phase-ports-probes/saved-cases/$seed" -PholdFuzzDefaults=false --console=plain > "$p-$seed-case.gradle.txt" 2>&1
  grep -h "first hold\|^class" "tools/phase-ports-probes/saved-cases/$seed/case.txt" | cut -c1-260
done
