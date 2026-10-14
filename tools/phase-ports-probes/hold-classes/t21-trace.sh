#!/usr/bin/env bash
# t21: one fuzz seed alone with src/instr-t21-trace.patch applied (T19Trace plus a line per Newton iteration, working tree
# only), and the passes of the solve whose failure the first hold reports, with their Newton iterations. From the worktree
# root, inside a lane. Usage: t21-trace.sh <logs/NN-tag> <r|d> <seed> ["<extra -D>"]
# Writes <logs/NN-tag>-<r|d><seed>-passes.txt (the failing solve) beside the alone reports; the full trace stays in $T.
set -u
H=tools/phase-ports-probes/hold-classes; T=C:/Users/wormz/.claude/jobs/20526532/tmp; out="$T/t21-$(basename $1)-$2$3.txt"; rm -f "$out"
if [ "$2" = r ]; then rs=$3; ds=-; d=random; else rs=-; ds=$3; d=defaults; fi
JAVA_TOOL_OPTIONS="-Dt19trace=$out ${4:-}" bash $H/alone.sh "$1-$2$3" "$rs" "$ds" > /dev/null 2>&1
held=$(grep -h "HELD tick" "$H/$1-$2$3-$d-report.txt" | head -1); echo "$held" | cut -c1-260
res=$(echo "$held" | grep -oE "residual [0-9.E-]+" | head -1)
n=""; [ -n "$res" ] && n=$(grep -n "$res" "$out" | grep -E "FAIL" | head -1 | cut -d: -f1)
[ -z "$n" ] && n=$(grep -n "CYCLE" "$out" | head -1 | cut -d: -f1)
[ -z "$n" ] && { echo "no failing solve found"; exit 0; }
s=$(awk -v n=$n 'NR<=n && /PASS 0 /{l=NR} END{print l}' "$out")
awk -v s=$s -v n=$n 'NR>=s&&NR<=n' "$out" | grep -E "PASS|FAIL|CONV|CYCLE|FAILCAPS|NEWTON" | sed -E 's/ modes=\[[^]]*\]//; s/ closed=\[[^]]*\]//' > "$H/$1-$2$3-passes.txt"
grep -cE "NEWTON" "$H/$1-$2$3-passes.txt"
