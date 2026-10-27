#!/usr/bin/env bash
# Compares the lane's 1000-seed campaign of two log folders (t36 final test), per distribution: the classifier's
# .classes.txt exactly, the COUNTERS line of the .gradle.txt exactly, and every SEED line of the report with its wall
# time (ms=) blanked. Usage: bash compare-campaign.sh <baseline logs> <new logs>; prints differing lines and a verdict.
set -u
b=${1:?baseline}; n=${2:?new}; bad=0
for d in 04-campaign-cold-random 05-campaign-bundled-random 05-campaign-bundled-defaults; do
  sb=$(grep -o 'clean=[0-9]*' "$b/$d.gradle.txt" | head -1); sn=$(grep -o 'clean=[0-9]*' "$n/$d.gradle.txt" | head -1)
  c=$(diff <(cat "$b/$d-report.txt.classes.txt") <(cat "$n/$d-report.txt.classes.txt")) || true
  k=$(diff <(grep -o 'COUNTERS.*' "$b/$d.gradle.txt") <(grep -o 'COUNTERS.*' "$n/$d.gradle.txt")) || true
  s=$(diff <(grep '^SEED' "$b/$d-report.txt" | sed 's/ ms=[0-9]*//') <(grep '^SEED' "$n/$d-report.txt" | sed 's/ ms=[0-9]*//')) || true
  seeds=$(grep -c '^SEED' "$n/$d-report.txt")
  echo "$d: baseline $sb, new $sn, seeds $seeds; classes $([ -z "$c" ] && echo same || echo DIFFER), counters $([ -z "$k" ] && echo same || echo DIFFER), SEED lines $([ -z "$s" ] && echo same || echo DIFFER)"
  for x in "$c" "$k" "$s"; do [ -n "$x" ] && { echo "$x" | cut -c1-400; bad=1; }; done
  [ "$sb" = "$sn" ] && [ "$seeds" = 1000 ] || bad=1
done
[ $bad = 0 ] && echo "campaign: identical" || echo "campaign: DIFFERS"
