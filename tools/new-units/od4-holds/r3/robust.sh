#!/usr/bin/env bash
# r3 robustness proof: the D34 gate and bundled fuzz world 322 on a tree-ish with the fix (and probes) applied, then under the
# arriving-seed perturbations of perturb.pl. Scratch exports under D:/tmp_od4r3/<label>-<variant>; logs in $OUT.
#   bash robust.sh <label> <tree-ish> [variants...]   variants: ph:0 tp:0 tp:1e-12 tp:1e-9 tp:-1e-9 ph:1e-9 ph:-1e-9 (default all)
set -u
D=$(cd "$(dirname "$0")/.." && pwd); label=${1:?label}; tree=${2:?tree}; shift 2
variants=${*:-"ph:0 tp:0 tp:1e-12 tp:1e-9 tp:-1e-9 ph:1e-9 ph:-1e-9"}
OUT=${OUT:-D:/tmp_od4r3/logs}; mkdir -p "$OUT"
for v in $variants; do
  mode=${v%%:*}; eps=${v#*:}; name="$label-$mode${eps}"
  bash "$D/r3/lane.sh" export "$name" "$tree" > /dev/null || exit 3
  for p in ${FIX:-}; do perl "$p" "/d/tmp_od4r3/$name" 2>/dev/null || exit 5; done
  perl "$D/r3/perturb.pl" "/d/tmp_od4r3/$name" "$mode" "$eps" 2>/dev/null || exit 4
  bash "$D/r3/lane.sh" compile "$name" com/wormzjl/createcheme/runtime/fluid/HoldClassWorldsTest.java | grep -q "test=0" || { echo "$name compile failed"; continue; }
  bash "$D/r3/lane.sh" run "$name" "com.wormzjl.createcheme.runtime.fluid.HoldClassWorldsTest#aLiquidTraceAtATanksBottomPortLeavesItsDropLineAGasColumn" > "$OUT/$name-d34.txt" 2>&1
  gate=$(grep -o "succeeded=[0-9]*" "$OUT/$name-d34.txt"); held=$(grep -o "Held\[tick=[0-9]*" "$OUT/$name-d34.txt" | head -1)
  THREADS=1 OUT=$OUT bash "$D/r3/quick-lane.sh" "$name" "$name" 322 bundled-random > "$OUT/$name-q322.txt" 2>&1
  fuzz=$(grep "^SUMMARY" "$OUT/$name-bundled-random-report.txt" | cut -c1-160)
  echo "$name gate:$gate $held | 322: $fuzz | $(cut -c1-80 "$OUT/$name-bundled-random-report.txt.classes.txt" 2>/dev/null | tr '\n' ' ')"
done
