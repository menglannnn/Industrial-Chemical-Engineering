#!/usr/bin/env bash
# M7 (2026-10-17): a greedy minimization that keeps a world held in class <class> at commit <held> and clean at commit
# <clean>: each round lists, at <held>, the class of every world that drops one placement of the current one
# (M7CandidatesTest, -Dm7.drops), then checks at <clean> those that keep <class>, and drops the first placement whose
# removal keeps both. The harness follows each commit's mover API (cold from 9445106a on, cold-pre-s6 before).
# Usage (worktree root, clean tracked tree): m7-pair-minimize.sh <seed> <class> <held-sha> <clean-sha> <label> [base-keep]
# Logs: ${LOGS:-build/m7-logs/trace}/<label>-r<round>-{held,clean}.gradle.txt (M8: LOGS=build/m8-logs/trace); the result is printed as "PAIR KEEP <list>".
set -u
WT=D:/Minecraft/Modding/1.21/CreateChemE/.claude/worktrees/coolprop-multiphase-thermo-37f6b0; cd "$WT" || exit 2
seed=$1; target=$2; held=$3; clean=$4; label=$5; base=${6:-}; L=${LOGS:-build/m7-logs/trace}; mkdir -p "$L"
G="bash tools/unified-thermo-port-m1/gradle-locked.sh"; I="--no-configuration-cache --init-script tools/unified-thermo-port-m5/trace/m5-trace.init.gradle"
harness(){ if git merge-base --is-ancestor 9445106a "$1"; then echo cold; else echo cold-pre-s6; fi; }
at(){ [ -z "$(git status --porcelain --untracked-files=no)" ] || { echo "tracked tree not clean"; exit 3; }; git checkout -q --detach "$1" || exit 4; }
back(){ git checkout -q claude/unified-thermo-port; }
round=0
while true; do
  round=$((round+1)); log=$L/$label-r$round-held.gradle.txt
  at "$held"; $G "$log" $I test --tests '*M7CandidatesTest' -Pm5.harness=$(harness "$held") -Pm7.seed=$seed -Pm7.drops=true "-Pm7.base=$base" -Pminimize.threads=4 >/dev/null; back
  mapfile -t lines < <(grep -o "M7CAND seed=[0-9]* keep=[^ ]* class=[^ ]*" "$log")
  [ ${#lines[@]} -gt 0 ] || { echo "round $round: no M7CAND lines in $log"; exit 5; }
  basecls=$(echo "${lines[0]}" | sed 's/.* class=//'); echo "round $round: base class at $held = $basecls ($(echo "${lines[0]}" | sed 's/.* keep=\([^ ]*\).*/\1/' | tr ',' '\n' | wc -l) blocks)"
  [ "$basecls" = "$target" ] || { echo "base is not $target at $held"; exit 6; }
  keeps=(); for l in "${lines[@]:1}"; do c=$(echo "$l" | sed 's/.* class=//'); [ "$c" = "$target" ] && keeps+=("$(echo "$l" | sed 's/.* keep=\([^ ]*\).*/\1/')"); done
  echo "round $round: ${#keeps[@]} single drops keep $target at $held"
  [ ${#keeps[@]} -gt 0 ] || break
  joined=$(IFS='|'; echo "${keeps[*]}"); log=$L/$label-r$round-clean.gradle.txt
  at "$clean"; $G "$log" $I test --tests '*M7CandidatesTest' -Pm5.harness=$(harness "$clean") -Pm7.seed=$seed "-Pm7.lists=$joined" -Pminimize.threads=4 >/dev/null; back
  next=""; while read -r l; do c=$(echo "$l" | sed 's/.* class=//'); k=$(echo "$l" | sed 's/.* keep=\([^ ]*\).*/\1/'); [ "$c" = clean ] && { next=$k; break; }; done < <(grep -o "M7CAND seed=[0-9]* keep=[^ ]* class=[^ ]*" "$log")
  [ -n "$next" ] || { echo "round $round: none of them is clean at $clean"; break; }
  echo "round $round: drop -> $next"; base=$next
done
echo "PAIR KEEP ${base:-<whole seed>}"
