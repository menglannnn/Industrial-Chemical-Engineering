#!/usr/bin/env bash
# t26: one fuzz seed alone with the pass trace (src/instr-t26-pass-trace.patch applied: T19Trace, -Dt19trace=<file>), then
# the passes of the solve the first hold reports (the first cycle, else the failing solve). From the worktree root, inside a
# lane. Usage: t26-trace.sh <logs/NN-tag> <r|d> <seed> ["<extra -D>"]   (TRACE_DIR: where the raw trace goes)
set -u
H=tools/phase-ports-probes/hold-classes; T=${TRACE_DIR:-C:/Users/wormz/.claude/jobs/0dddee1e/tmp}; out="$T/t26-$2$3.txt"; rm -f "$out"
if [ "$2" = r ]; then rs=$3; ds=-; d=random; else rs=-; ds=$3; d=defaults; fi
JAVA_TOOL_OPTIONS="-Dt19trace=$out ${4:-}" bash $H/alone.sh "$1-$2$3" "$rs" "$ds" > /dev/null 2>&1
held=$(grep -h "HELD tick" "$H/$1-$2$3-$d-report.txt" | head -1); echo "$held" | cut -c1-400
n=$(grep -n "CYCLE" "$out" | head -1 | cut -d: -f1)
if [ -z "$n" ]; then res=$(echo "$held" | grep -oE "residual [0-9.E-]+" | head -1); [ -n "$res" ] && n=$(grep -n "$res" "$out" | grep -E "FAIL" | head -1 | cut -d: -f1); fi
[ -z "$n" ] && { echo "no failing solve found"; exit 0; }
s=$(awk -v n=$n 'NR<=n && / PASS 0 /{l=NR} END{print l}' "$out")
awk -v s=$s -v n=$n 'NR>=s&&NR<=n' "$out" > "$H/$1-$2$3-passes.txt"
wc -l "$H/$1-$2$3-passes.txt"
