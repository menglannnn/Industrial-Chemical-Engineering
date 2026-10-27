#!/usr/bin/env bash
# Batch 2026-10-21-floating-roof-tanks: the bundled 200-seed quick check on the Gradle-free javac lane, adapted from
# tools/fluid-solver-simplification/quick-lane.sh (t38). Differences: bundled distributions only, the lane worktree is
# $WT (default: the session worktree rename-ichem-mod-30e50c, for its build/moddev classpath and git archive), and the
# harness is HARNESS (default: this folder's patched copy with holdFuzz.openTanks; ORIG=1 takes the unpatched
# tools/unified-thermo-port-forms/hold-fuzz-bundled harness, the one that compiles on ef64bd06).
#   bash quick-lane.sh <export-name> <label> [seeds=1-200] [dists="bundled-random bundled-defaults"]
# Export first: WT=... bash tools/unified-thermo-port-m5/d40/lane.sh export <name> <commit>
# EXTRA_OPTS passes further system properties (-DholdFuzz.openTanks=true, -Dicheme.openTank.postStepVent=true).
# Counters on, THREADS (default 4) workers. Reports: $OUT/<label>-<dist>-report.txt (+ .classes.txt).
set -u
MAIN=D:/Minecraft/Modding/1.21/CreateChemE
export WT=${WT:-$MAIN/.claude/worktrees/rename-ichem-mod-30e50c}
HERE=$(cd "$(dirname "$0")" && pwd)
D=$MAIN/tools/unified-thermo-port-m5/d40
if [ "${ORIG:-0}" = 1 ]; then harness=$MAIN/tools/unified-thermo-port-forms/hold-fuzz-bundled/src/HoldFuzzTest.java
else harness=${HARNESS:-$HERE/hold-fuzz-bundled/src/HoldFuzzTest.java}; fi
classify=$MAIN/tools/phase-ports-probes/hold-fuzz/classify.sh
name=${1:?export name}; label=${2:?label}; seeds=${3:-1-200}; dists=${4:-"bundled-random bundled-defaults"}
OUT=${OUT:-/tmp/frt-logs}; mkdir -p "$OUT"
bash "$D/lane.sh" compile "$name" "$(cygpath -m "$harness")" > "$OUT/$label-compile.txt" 2>&1
grep -q "test=0" "$OUT/$label-compile.txt" || { cat "$OUT/$label-compile.txt"; exit 3; }
for dist in $dists; do
  case $dist in
    bundled-random) props="-DholdFuzz.defaults=false" ;;
    bundled-defaults) props="-DholdFuzz.defaults=true" ;;
    *) echo "unknown dist $dist"; exit 2 ;;
  esac
  report=$OUT/$label-$dist-report.txt
  JAVA_OPTS="-DholdFuzz.seeds=$seeds -DholdFuzz.threads=${THREADS:-4} -DholdFuzz.counters=true -DholdFuzz.out=$(cygpath -m "$report") $props ${EXTRA_OPTS:-}" \
    bash "$D/lane.sh" run "$name" com.wormzjl.icheme.runtime.fluid.HoldFuzzTest > "$OUT/$label-$dist-run.txt" 2>&1
  bash "$classify" "$report" > /dev/null 2>&1
  echo "== $dist ($label)"; grep -E '^(SUMMARY|COUNTERS)' "$report" | cut -c1-400; cut -c1-120 "$report.classes.txt"
done
