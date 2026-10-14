#!/usr/bin/env bash
# t21: fuzz seeds 1-200 in both distributions (campaign.sh, $THREADS workers), against the same seeds of a reference campaign
# (default ca44). From the worktree root, inside a lane. Usage: t21-quick.sh <logs/NN-tag> [reference prefix, default logs/ca44-campaign-final]
set -u
H=tools/phase-ports-probes/hold-classes; ref="$H/${2:-logs/ca44-campaign-final}"
bash $H/campaign.sh "$1" 1-200 > /dev/null 2>&1
clean(){ awk '/^SEED /{if($2<=200 && $0 ~ /holds=0 classes=\[\]/) print $2}' "$1" | LC_ALL=C sort; }
for d in random defaults; do
  new="$H/$1-$d-report.txt"
  echo "== $d clean $(clean "$new" | wc -l) (reference $(clean "$ref-$d-report.txt" | wc -l))"
  echo "worse: $(LC_ALL=C comm -23 <(clean "$ref-$d-report.txt") <(clean "$new") | sort -n | tr '\n' ' ')"
  echo "cleared: $(LC_ALL=C comm -13 <(clean "$ref-$d-report.txt") <(clean "$new") | sort -n | tr '\n' ' ')"
  grep COUNTERS "$new" | grep -oE "newtonSolves=[0-9]+ newtonIterations=[0-9]+|equationGateRejections=[0-9]+" | tr '\n' ' '; echo
done
