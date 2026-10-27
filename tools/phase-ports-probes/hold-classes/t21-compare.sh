#!/usr/bin/env bash
# t21: a campaign against reference campaigns: SUMMARY, counters, worse/cleared with classes (transitions.js), first-interval
# family. From the worktree root. Usage: t21-compare.sh <logs/new-prefix> <logs/ref-prefix> ...
set -u
H=tools/phase-ports-probes/hold-classes; new="$H/$1"; shift
for d in random defaults; do
  echo "==== $d: $(grep -h '^SUMMARY' "$new-$d-report.txt" | cut -d' ' -f2-6)"
  grep -h '^COUNTERS' "$new-$d-report.txt" | grep -oE 'newtonSolves=[0-9]+ newtonIterations=[0-9]+|equationGateRejections=[0-9]+' | tr '\n' ' '; echo
  for ref in "$@"; do
    echo "-- against $ref: $(grep -h '^SUMMARY' "$H/$ref-$d-report.txt" | cut -d' ' -f2-3)"
    bash $H/compare.sh "$H/$ref-$d-report.txt" "$new-$d-report.txt"
    node $H/transitions.js "$H/$ref-$d-report.txt.classes.txt" "$new-$d-report.txt.classes.txt" 2>/dev/null | head -40
  done
  node $H/first-interval.js "$new-$d-report.txt" 2>/dev/null | head -3
done
