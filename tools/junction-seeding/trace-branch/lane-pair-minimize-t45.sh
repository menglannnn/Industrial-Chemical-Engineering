#!/usr/bin/env bash
# t45 (2026-10-19): t42b's lane-pair-minimize-t42.sh with T45BranchTest (branch classes U / S / H<cls>): keeps a world in <class>
# (U) at <held> and in class ${CLEAN_CLASS:-S} at <clean>. Budgets lifted.
# total-work blow-up with -Dt42.total=K) and EXTRA_OPTS passed to both runs (the cold harness: -DholdFuzz.cold=true
# -DholdFuzz.defaults=false; -Dm7.slices; -Dt42.total). Keeps a world in <class> at the lane export <held> and "clean" at
# <clean> (both compiled with the hold-fuzz harness, ../../unified-thermo-port-m5/trace/src and trace-d3/src). Budgets lifted.
# Usage: lane-pair-minimize-t42.sh <seed> <class> <held-export> <clean-export> <label> [base-keep]; logs in $LOGS.
set -u
WT=D:/Minecraft/Modding/1.21/CreateChemE/.claude/worktrees/coolprop-multiphase-thermo-37f6b0; cd "$WT" || exit 2
seed=$1; target=$2; held=$3; clean=$4; label=$5; base=${6:-}; L=${LOGS:-build/t45-logs}; mkdir -p "$L"
lane="bash tools/unified-thermo-port-m5/d40/lane.sh"; B="-DholdFuzz.wallNanos=600000000000 -DholdFuzz.softNanos=590000000000 -Dminimize.threads=${THREADS:-4} ${EXTRA_OPTS:-}"
T=com.wormzjl.createcheme.runtime.fluid.T45BranchTest
cands(){ grep -o "M7CAND seed=[0-9]* keep=[^ ]* class=[^ ]*" "$1"; }
round=0
while true; do
  round=$((round+1)); log=$L/$label-r$round-held.txt
  JAVA_OPTS="-Dm7.seed=$seed -Dm7.drops=true -Dm7.base=$base $B" $lane run "$held" $T > "$log" 2>&1
  mapfile -t lines < <(cands "$log")
  [ ${#lines[@]} -gt 0 ] || { echo "round $round: no M7CAND lines in $log"; exit 5; }
  basecls=$(echo "${lines[0]}" | sed 's/.* class=//'); echo "round $round: base class at $held = $basecls ($(echo "${lines[0]}" | sed 's/.* keep=\([^ ]*\).*/\1/' | tr ',' '\n' | wc -l) blocks)"
  [ "$basecls" = "$target" ] || { echo "base is not $target at $held"; exit 6; }
  keeps=(); for l in "${lines[@]:1}"; do c=$(echo "$l" | sed 's/.* class=//'); [ "$c" = "$target" ] && keeps+=("$(echo "$l" | sed 's/.* keep=\([^ ]*\).*/\1/')"); done
  echo "round $round: ${#keeps[@]} single drops keep $target at $held"
  [ ${#keeps[@]} -gt 0 ] || break
  joined=$(IFS='|'; echo "${keeps[*]}"); log=$L/$label-r$round-clean.txt
  JAVA_OPTS="-Dm7.seed=$seed -Dm7.lists=$joined $B" $lane run "$clean" $T > "$log" 2>&1
  next=""; while read -r l; do c=$(echo "$l" | sed 's/.* class=//'); k=$(echo "$l" | sed 's/.* keep=\([^ ]*\).*/\1/'); [ "$c" = "${CLEAN_CLASS:-S}" ] && { next=$k; break; }; done < <(cands "$log")
  [ -n "$next" ] || { echo "round $round: none of them is ${CLEAN_CLASS:-S} at $clean"; break; }
  echo "round $round: drop -> $next"; base=$next
done
echo "PAIR KEEP ${base:-<whole seed>}"
