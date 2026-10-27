#!/usr/bin/env bash
# t52: the D46 probe on the 200-seed quick check, three distributions, one after another, 4 workers (t51 runs beside).
#   bash run-d46.sh <export name> <tree-ish> <log dir> [seeds=1-200] [patch...]
# Exports the tree under /tmp/d40-<name> (lane.sh), applies the probe (apply-probe.pl), compiles each harness copy and
# runs it; summarize.pl sums the D46SEED lines.
set -u
WT=D:/Minecraft/Modding/1.21/CreateChemE/.claude/worktrees/coolprop-multiphase-thermo-37f6b0
D=$WT/tools/unified-thermo-port-m5/d40; P=$(cd "$(dirname "$0")" && pwd)
name=${1:?export name}; tree=${2:?tree-ish}; L=${3:?log dir}; seeds=${4:-1-200}; shift 4 2>/dev/null || shift $#
mkdir -p "$L"
bash "$D/lane.sh" export "$name" "$tree" "$@" > "$L/00-export.txt" 2>&1 || { cat "$L/00-export.txt"; exit 2; }
perl "$P/probe/apply-probe.pl" "/tmp/d40-$name" "$WT" "$L/harness" >> "$L/00-export.txt" 2>&1 || { cat "$L/00-export.txt"; exit 2; }
for dist in cold-random bundled-random bundled-defaults; do
  case $dist in
    cold-random) h=$L/harness/cold/HoldFuzzTest.java; props="-DholdFuzz.cold=true -DholdFuzz.defaults=false" ;;
    bundled-random) h=$L/harness/bundled/HoldFuzzTest.java; props="-DholdFuzz.defaults=false" ;;
    bundled-defaults) h=$L/harness/bundled/HoldFuzzTest.java; props="-DholdFuzz.defaults=true" ;;
  esac
  bash "$D/lane.sh" compile "$name" "$(cygpath -m "$h")" > "$L/$dist-compile.txt" 2>&1
  grep -q "main=0 test=0" "$L/$dist-compile.txt" || { cat "$L/$dist-compile.txt"; exit 3; }
  JAVA_OPTS="-DholdFuzz.seeds=$seeds -DholdFuzz.threads=4 -DholdFuzz.counters=true -DholdFuzz.out=$(cygpath -m "$L/$dist-report.txt") $props" \
    bash "$D/lane.sh" run "$name" com.wormzjl.createcheme.runtime.fluid.HoldFuzzTest > "$L/$dist-run.txt" 2>&1
  grep -E "^(SUMMARY|COUNTERS)" "$L/$dist-report.txt" | cut -c1-400
  perl "$P/summarize.pl" "$L/$dist-run.txt" "$L/$dist-report.txt" "$dist"
done
echo "done $(date '+%H:%M:%S')"
