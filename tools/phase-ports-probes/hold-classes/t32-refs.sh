#!/usr/bin/env bash
# t32: the references of a quick check or gates run against cl14 (the close's gates after A423, on d3365664), timing fields (ms=, bytes=,
# allocatedMB=) stripped: the 33 junction lines (count of differing lines), chain-100's deviation line, MIXED_GAS_COST, and
# the printed suite lines (count of differing lines, wall times masked and reason maps sorted by printed-lines.sh).
# From the worktree root. Usage: t29-refs.sh <logs/NN-tag prefix of the G1/G2 files> [-v: print the differing junction lines]
set -u
H=tools/phase-ports-probes/hold-classes; p="$H/$1"; r="$H/logs/cl14-gates"
strip(){ sed -E 's/ ms=[0-9]+//; s/, ms=[0-9]+//; s/, bytes=[0-9]+//; s/ allocatedMB=[0-9.]+//' "$1"; }
n=$(diff <(strip "$r-G1-junction-lines-raw.txt") <(strip "$p-G1-junction-lines-raw.txt") | grep -c '^>')
echo "junction lines differing from cl14 (timing stripped): $n of $(wc -l < "$p-G1-junction-lines-raw.txt")"
[ "${2:-}" = -v ] && [ "$n" -gt 0 ] && diff <(strip "$r-G1-junction-lines-raw.txt") <(strip "$p-G1-junction-lines-raw.txt") | grep '^[<>]' | cut -c1-260
grep -h "chain-100 vs reference" "$p-G2-regression-exact.gradle.txt" | head -1 | sed 's/^ */now: /' | cut -c1-140
grep -h "chain-100 vs reference" "$r-G2-regression-exact.gradle.txt" | head -1 | sed 's/^ */cl14: /' | cut -c1-140
grep -h "MIXED_GAS_COST" "$p-G1-junction-lines-raw.txt" | sed -E 's/ ms=[0-9]+//; s/ allocatedMB=[0-9.]+//'
bash $H/printed-lines.sh "$r-G1-stdout.txt" "$p-G1-stdout.txt" 2>/dev/null | tail -3
