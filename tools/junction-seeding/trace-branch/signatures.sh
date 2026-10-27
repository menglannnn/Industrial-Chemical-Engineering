#!/usr/bin/env bash
# t45: replays bundled seeds on lane exports (M5SolveLogTest, one JVM per seed and export, at most 4 at a time, budgets lifted)
# and prints per seed and export the islands' end statuses and the summed solver counters.
# Usage: signatures.sh <outdir> "<export[:extra opts]> ..." seed...   e.g. signatures.sh build/t45-logs/sig "t45b t45fb t45fbr:-Dt45.rounds=8" 4 123 200 78
set -u
WT=D:/Minecraft/Modding/1.21/CreateChemE/.claude/worktrees/coolprop-multiphase-thermo-37f6b0; cd "$WT" || exit 2
out=$1; exports=$2; shift 2; mkdir -p "$out"; lane="bash tools/unified-thermo-port-m5/d40/lane.sh"
jobs=()
for s in "$@"; do for e in $exports; do jobs+=("$s|$e"); done; done
run(){ local s=${1%%|*} e=${1#*|}; local name=${e%%:*} extra=""; [ "$name" != "$e" ] && extra=${e#*:}
  JAVA_OPTS="-Dreplay.seed=$s -Dreplay.slices=${SLICES:-120} -Dreplay.budgets=600000000000:590000000000 $extra" $lane run "$name" com.wormzjl.createcheme.runtime.fluid.M5SolveLogTest#replay > "$out/$s-$name${extra:+-x}.txt" 2>&1; }
n=0; for j in "${jobs[@]}"; do run "$j" & n=$((n+1)); if [ $n -ge ${PAR:-4} ]; then wait -n; n=$((n-1)); fi; done; wait
for s in "$@"; do for e in $exports; do name=${e%%:*}; extra=""; [ "$name" != "$e" ] && extra=x; f="$out/$s-$name${extra:+-x}.txt"
  awk -v tag="$s $e" '/^SOLVE/{n++;for(i=1;i<=NF;i++){split($i,a,"=");if(a[1]=="newtonIterations")ni+=a[2];if(a[1]=="jacobianBuilds")jb+=a[2];if(a[1]=="checks")ch+=a[2]}} END{printf "SIG %s solves=%d checks=%d newtonIterations=%d jacobianBuilds=%d\n",tag,n,ch,ni,jb}' "$f"
  grep "^M5LOG END\|^M5LOG HELD" "$f" | cut -c1-170 | sed 's/^/    /'; done; done
