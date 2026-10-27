#!/usr/bin/env bash
# t40 (2026-10-18): m7-pair-minimize.sh on the Gradle-free javac lane: a greedy minimization that keeps a world held in
# <class> at the lane export <held> and clean at the lane export <clean> (both compiled with the bundled harness and
# ../../unified-thermo-port-m5/trace/src). Budgets out of the way, so the class is deterministic. No checkout is touched.
# Usage: lane-pair-minimize.sh <seed> <class> <held-export> <clean-export> <label> [base-keep]; logs in $LOGS (build/t40-logs).
set -u
WT=D:/Minecraft/Modding/1.21/CreateChemE/.claude/worktrees/coolprop-multiphase-thermo-37f6b0; cd "$WT" || exit 2
seed=$1; target=$2; held=$3; clean=$4; label=$5; base=${6:-}; L=${LOGS:-build/t40-logs}; mkdir -p "$L"
lane="bash tools/unified-thermo-port-m5/d40/lane.sh"; B="-DholdFuzz.wallNanos=600000000000 -DholdFuzz.softNanos=590000000000 -Dminimize.threads=${THREADS:-4}"
cands(){ grep -o "M7CAND seed=[0-9]* keep=[^ ]* class=[^ ]*" "$1"; }
round=0
while true; do
  round=$((round+1)); log=$L/$label-r$round-held.txt
  JAVA_OPTS="-Dm7.seed=$seed -Dm7.drops=true -Dm7.base=$base $B" $lane run "$held" com.wormzjl.createcheme.runtime.fluid.M7CandidatesTest > "$log" 2>&1
  mapfile -t lines < <(cands "$log")
  [ ${#lines[@]} -gt 0 ] || { echo "round $round: no M7CAND lines in $log"; exit 5; }
  basecls=$(echo "${lines[0]}" | sed 's/.* class=//'); echo "round $round: base class at $held = $basecls ($(echo "${lines[0]}" | sed 's/.* keep=\([^ ]*\).*/\1/' | tr ',' '\n' | wc -l) blocks)"
  [ "$basecls" = "$target" ] || { echo "base is not $target at $held"; exit 6; }
  keeps=(); for l in "${lines[@]:1}"; do c=$(echo "$l" | sed 's/.* class=//'); [ "$c" = "$target" ] && keeps+=("$(echo "$l" | sed 's/.* keep=\([^ ]*\).*/\1/')"); done
  echo "round $round: ${#keeps[@]} single drops keep $target at $held"
  [ ${#keeps[@]} -gt 0 ] || break
  joined=$(IFS='|'; echo "${keeps[*]}"); log=$L/$label-r$round-clean.txt
  JAVA_OPTS="-Dm7.seed=$seed -Dm7.lists=$joined $B" $lane run "$clean" com.wormzjl.createcheme.runtime.fluid.M7CandidatesTest > "$log" 2>&1
  next=""; while read -r l; do c=$(echo "$l" | sed 's/.* class=//'); k=$(echo "$l" | sed 's/.* keep=\([^ ]*\).*/\1/'); [ "$c" = clean ] && { next=$k; break; }; done < <(cands "$log")
  [ -n "$next" ] || { echo "round $round: none of them is clean at $clean"; break; }
  echo "round $round: drop -> $next"; base=$next
done
echo "PAIR KEEP ${base:-<whole seed>}"
