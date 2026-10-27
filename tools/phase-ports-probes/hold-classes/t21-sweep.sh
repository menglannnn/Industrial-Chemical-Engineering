#!/usr/bin/env bash
# t21: the flow-form weight's Reynolds thresholds (scratch switches -Dt21re1, -Dt21re2, working tree only), each pair on the
# E6-S seeds alone and on fuzz seeds 1-200 against ca44. From the worktree root, inside a lane.
# Usage: t21-sweep.sh <logs/NN-tag> <re1:re2> ...
set -u
H=tools/phase-ports-probes/hold-classes; tag=$1; shift
R="19,65,92,98,107,118,151,244,556,594,600,633,660,768,794,824,842,886,915,916,937"
D="17,29,145,173,175,183,214,220,241,246,298,336,352,357,364,397,401,424,529,547,606,681,720,758,768,786,833,886,915,932,961,967,981"
for pair in "$@"; do
  r1=${pair%%:*}; r2=${pair##*:}; t="$tag-re$r1-$r2"
  export JAVA_TOOL_OPTIONS="-Dt21re1=$r1 -Dt21re2=$r2 ${EXTRA_OPTS:-}"
  echo "#### $pair"
  bash $H/alone.sh "$t-e6s" "$R" "$D" | grep -E "^(E|C|F)[0-9 ]" | sort -u
  for d in random defaults; do echo "e6s $d $(grep -h SUMMARY $H/$t-e6s-$d-report.txt | grep -oE 'clean=[0-9]+|wallMs=[0-9]+' | tr '\n' ' ') $(grep -h '^COUNTERS' $H/$t-e6s-$d-report.txt | grep -oE 'newtonIterations=[0-9]+')"; done
  bash $H/t21-quick.sh "$t-quick"
done
