#!/usr/bin/env bash
# Cross-runtime output hashes of the pow kernel (batch 2026-09-27-jdk-determinism, pow performance study): compiles
# DeterministicMath, FdlibmPow, DeterministicPow (src/main) and FastDetMath, FastDetCheck (perf/src) with --release 17
# and runs `FastDetCheck accuracy <n>` under every runtime and mode below; the hash column must be identical on every
# line of the same input class.   kernel-sweep.sh <log> [n]
set -uo pipefail
HERE=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
ROOT=$(cd "$HERE/../../.." && pwd)
LOG=${1:?log}; N=${2:-1000000}
OUT=$ROOT/build/perf/kernel17; rm -rf "$OUT"; mkdir -p "$OUT"
M=$ROOT/src/main/java/com/wormzjl/createcheme/science/math
J21="C:/Program Files/Java/jdk-21.0.11/bin"
"$J21/javac" --release 17 -nowarn -d "$(cygpath -w "$OUT")" "$(cygpath -w "$M/DeterministicMath.java")" "$(cygpath -w "$M/FdlibmPow.java")" \
  "$(cygpath -w "$M/DeterministicPow.java")" "$(cygpath -w "$HERE/src/FastDetMath.java")" "$(cygpath -w "$HERE/src/FastDetCheck.java")" || exit 2
: > "$LOG"
for rt in "$J21/java|" "$J21/java|-Xint" "$J21/java|-XX:TieredStopAtLevel=1" "C:/Program Files/Zulu/zulu-25/bin/java|" \
          "C:/Program Files/Zulu/zulu-25/bin/java|-Xint" "$HOME/.gradle/jdks/eclipse_adoptium-17-amd64-windows/jdk-17.0.20+8/bin/java|"; do
  java=${rt%%|*}; opt=${rt#*|}
  "$java" $opt -cp "$(cygpath -w "$OUT")" FastDetCheck accuracy "$N" 2>&1 | grep -v "true error" >> "$LOG"
done
# one hash per input class across all runtimes and modes
awk '/^#/{next} {match($0,/hash=[0-9a-f]+/); h=substr($0,RSTART+5,RLENGTH-5); name=substr($0,1,34); seen[name][h]=1} END {bad=0; for (n in seen) {c=0; for (h in seen[n]) c++; if (c!=1) {bad=1; print "DIFFERENT HASHES: " n}} print bad ? "kernel-sweep: hashes differ" : "kernel-sweep: every input class has one hash on every runtime and mode"}' "$LOG" | tee -a "$LOG"
