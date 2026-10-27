#!/usr/bin/env bash
# t285 item 2: tools/fluid-solver-simplification/trace/lane-pair-minimize.sh on this session's lane (/d/tmp_285/x/<name>).
# Usage: pairmin.sh <seed> <class> <held-export> <clean-export> <label> [base-keep]; logs in /d/tmp_285/out/min
set -u
seed=$1; target=$2; held=$3; clean=$4; label=$5; base=${6:-}; L=/d/tmp_285/out/min; mkdir -p "$L"
lane="bash /d/tmp_285/scripts/lane.sh"; B="-DholdFuzz.wallNanos=600000000000 -DholdFuzz.softNanos=590000000000 -Dminimize.threads=${THREADS:-4} ${EXTRA:-}"
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
