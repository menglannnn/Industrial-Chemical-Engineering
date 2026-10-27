#!/usr/bin/env bash
# t18: two campaigns side by side, both distributions: SUMMARY and COUNTERS lines, worse/cleared seeds (compare.sh), class
# transitions (transitions.js) and the first-interval family (first-interval.js). Usage, from this folder:
#   t18-compare.sh <logs/A-prefix> <logs/B-prefix>   (prefixes as given to campaign.sh, e.g. logs/ig10-campaign-final)
set -u
for dist in random defaults; do
  a="$1-$dist-report.txt"; b="$2-$dist-report.txt"
  echo "== $dist: $1 -> $2"
  grep -hE '^(SUMMARY|COUNTERS)' "$a" | cut -c1-400; grep -hE '^(SUMMARY|COUNTERS)' "$b" | cut -c1-400
  bash compare.sh "$a" "$b"
  node transitions.js "$a.classes.txt" "$b.classes.txt"
  node first-interval.js "$a" "$b" | tail -4
done
