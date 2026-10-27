#!/usr/bin/env bash
# t19: one fuzz seed alone with the t19 instrumentation (T19Trace, working tree only), and the passes of the solve whose
# failure the first hold reports. From the worktree root, inside a lane. Usage: t19-trace.sh <logs/NN-tag> <r|d> <seed> ["<extra -D>"]
set -u
H=tools/phase-ports-probes/hold-classes; T=C:/Users/wormz/.claude/jobs/f4ef8d38/tmp; out="$T/t19-$2$3.txt"; rm -f "$out"
if [ "$2" = r ]; then rs=$3; ds=-; d=random; else rs=-; ds=$3; d=defaults; fi
JAVA_TOOL_OPTIONS="-Dt19trace=$out ${4:-}" bash $H/alone.sh "$1" "$rs" "$ds" > /dev/null 2>&1
held=$(grep -h "HELD tick" "$H/$1-$d-report.txt" | head -1); echo "$held" | cut -c1-260
res=$(echo "$held" | grep -oE "residual [0-9.E-]+" | head -1)
n=$(grep -n "$res" "$out" | grep -E "FAIL" | head -1 | cut -d: -f1)
[ -z "$n" ] && n=$(grep -n "CYCLE" "$out" | head -1 | cut -d: -f1)
[ -z "$n" ] && { echo "no failing solve found"; exit 0; }
s=$(awk -v n=$n 'NR<=n && /PASS 0 /{l=NR} END{print l}' "$out")
awk -v s=$s -v n=$n 'NR>=s&&NR<=n' "$out" | grep -E "PASS|FAIL|CONV|CYCLE|FAILCAPS|ESCALATE" | sed -E 's/ modes=\[[^]]*\]//; s/ closed=\[[^]]*\]//; s/start: (n[^|]*)\|.*/start: \1/' | cut -c1-${WIDTH:-1600}
