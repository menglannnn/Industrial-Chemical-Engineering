#!/usr/bin/env bash
# t28: the references of a quick check or gates run against tw19 (t27's gates on A371) and mh02 (the merged-head baseline),
# timing fields (ms=, bytes=, allocatedMB=) stripped: the 33 junction lines (count of differing lines), chain-100's deviation
# line, MIXED_GAS_COST's Newton solves. From the worktree root.
# Usage: t28-refs.sh <logs/NN-tag prefix of the G1/G2 files, e.g. logs/cc19-quick-item1> [-v: print the differing lines]
set -u
H=tools/phase-ports-probes/hold-classes; p="$H/$1"
strip(){ sed -E 's/ ms=[0-9]+//; s/, ms=[0-9]+//; s/, bytes=[0-9]+//; s/ allocatedMB=[0-9.]+//' "$1"; }
for ref in tw19-gates mh02-gates; do
  r="$H/logs/$ref"
  n=$(diff <(strip "$r-G1-junction-lines-raw.txt") <(strip "$p-G1-junction-lines-raw.txt") | grep -c '^>')
  echo "junction lines differing from ${ref%-gates} (timing stripped): $n of $(wc -l < "$p-G1-junction-lines-raw.txt")"
  [ "${2:-}" = -v ] && [ "$n" -gt 0 ] && diff <(strip "$r-G1-junction-lines-raw.txt") <(strip "$p-G1-junction-lines-raw.txt") | grep '^[<>]' | cut -c1-260
done
grep -h "chain-100 vs reference" "$p-G2-regression-exact.gradle.txt" | head -1
grep -h "chain-100 vs reference" "$H/logs/tw19-gates-G2-regression-exact.gradle.txt" | head -1 | sed 's/^ */tw19: /'
grep -h "MIXED_GAS_COST" "$p-G1-junction-lines-raw.txt" | sed -E 's/ ms=[0-9]+//; s/ allocatedMB=[0-9.]+//'
