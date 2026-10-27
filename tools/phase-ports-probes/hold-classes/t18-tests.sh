#!/usr/bin/env bash
# t18: runs suite test classes under modes of src/scratch-t18-switch.patch (-Dt18mode through JAVA_TOOL_OPTIONS), one Gradle
# invocation per mode, and prints each run's pass/fail lines. From the worktree root.
# Usage: t18-tests.sh <logs/NN-tag> "<mode> <mode> ..." '<test filter>' ['<test filter>' ...]
set -u
H=tools/phase-ports-probes/hold-classes; p="$H/$1"; modes=$2; shift 2
export JAVA_HOME="C:/Program Files/Java/jdk-21.0.11" JAVA_OPTS=-Xshare:off
args=(); for t in "$@"; do args+=(--tests "$t"); done
for m in $modes; do
  JAVA_TOOL_OPTIONS="-Dt18mode=$m" ./gradlew --no-configuration-cache --no-build-cache test --rerun "${args[@]}" --console=plain --continue > "$p-$m.gradle.txt" 2>&1
  echo "== $m: $(grep -E '^[A-Za-z]+Test > .* (PASSED|FAILED)' "$p-$m.gradle.txt" | sed -E 's/\(\)//' | tr '\n' ';') $(grep -h 'BUILD' "$p-$m.gradle.txt" | tail -1)"
  cat build/test-results/test/TEST-*.xml | sed -E 's/^.*<!\[CDATA\[//' | tr -d '\r' > "$p-$m-stdout.txt"
done
