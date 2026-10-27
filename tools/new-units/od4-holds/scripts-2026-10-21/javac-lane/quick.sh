#!/usr/bin/env bash
# t38: the 200-seed quick check on the Gradle-free javac lane (tools/unified-thermo-port-m5/d40/lane.sh exports),
# for a tree already exported and compiled there: the cold and the bundled hold-fuzz harnesses are compiled into the
# export (they share the class name, so one at a time), run with the harness's own system properties, and classified
# by the main checkout's classify.sh. Counters on, 4 threads. Reports: $OUT/<label>-<dist>-report.txt (+ .classes.txt).
#   bash quick-lane.sh <export-name> <label> [seeds=1-200] [dists="cold-random bundled-random bundled-defaults"]
set -u
WT=D:/Minecraft/Modding/1.21/CreateChemE/.claude/worktrees/custom-equipment-rendering-805e7d
D=/d/tmp_285/scripts; F=D:/Minecraft/Modding/1.21/CreateChemE/tools/unified-thermo-port-forms
classify=D:/Minecraft/Modding/1.21/CreateChemE/tools/phase-ports-probes/hold-fuzz/classify.sh
name=${1:?export name}; label=${2:?label}; seeds=${3:-1-200}; dists=${4:-"cold-random bundled-random bundled-defaults"}
OUT=${OUT:-/d/tmp_285/out/q}; mkdir -p "$OUT"
for dist in $dists; do
  case $dist in
    cold-random) harness=$F/hold-fuzz-cold/src/HoldFuzzTest.java; props="-DholdFuzz.cold=true -DholdFuzz.defaults=false" ;;
    bundled-random) harness=$F/hold-fuzz-bundled/src/HoldFuzzTest.java; props="-DholdFuzz.defaults=false" ;;
    bundled-defaults) harness=$F/hold-fuzz-bundled/src/HoldFuzzTest.java; props="-DholdFuzz.defaults=true" ;;
    *) echo "unknown dist $dist"; exit 2 ;;
  esac
  bash "$D/lane.sh" compile "$name" "$(cygpath -m "$harness")" > "$OUT/$label-$dist-compile.txt" 2>&1
  grep -q "test=0" "$OUT/$label-$dist-compile.txt" || { cat "$OUT/$label-$dist-compile.txt"; exit 3; }
  report=$OUT/$label-$dist-report.txt
  JAVA_OPTS="-DholdFuzz.seeds=$seeds -DholdFuzz.threads=${THREADS:-4} -DholdFuzz.counters=true -DholdFuzz.out=$(cygpath -m "$report") $props ${EXTRA_OPTS:-}" \
    bash "$D/lane.sh" run "$name" com.wormzjl.createcheme.runtime.fluid.HoldFuzzTest > "$OUT/$label-$dist-run.txt" 2>&1
  bash "$classify" "$report" > /dev/null 2>&1
  echo "== $dist ($label)"; grep -E '^(SUMMARY|COUNTERS)' "$report" | cut -c1-400; cut -c1-120 "$report.classes.txt"
done
