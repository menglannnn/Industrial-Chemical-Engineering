#!/usr/bin/env bash
# t20: the worse seeds (against ig10) and budget holds of each form's campaign replayed alone (one worker, alone.sh), one
# batch, run under the alone lock: t20-lane.sh alone bash t20-alone-batch.sh <logs/NN-tag> <listdir>. Each form's lists are
# <listdir>/<campaign>-<random|defaults>-alone.txt (comma-separated); the form is passed as -Dcreatecheme.junctionStart.
set -u
H=tools/phase-ports-probes/hold-classes; tag=$1; dir=$2
for f in ps01-campaign-air:air ps02-campaign-empty:empty ps05-campaign-fixedpoint:fixedpoint; do
  IFS=: read c s <<< "$f"
  export JAVA_TOOL_OPTIONS="-Dcreatecheme.junctionStart=$s"
  bash $H/alone.sh "$tag-$s" "$(cat $dir/$c-random-alone.txt)" "$(cat $dir/$c-defaults-alone.txt)" > "$H/$tag-$s-summary.txt" 2>&1
  echo "$s done"
done
