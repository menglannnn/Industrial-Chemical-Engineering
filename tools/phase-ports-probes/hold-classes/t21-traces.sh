#!/usr/bin/env bash
# t21: t21-trace.sh over a list of seeds. From the worktree root, inside a lane.
# Usage: t21-traces.sh <logs/NN-tag> <r594|d175 ...> (EXTRA_D forwarded as the trace's extra -D switches)
set -u
tag=$1; shift
for s in "$@"; do echo "== $s"; bash tools/phase-ports-probes/hold-classes/t21-trace.sh "$tag" "${s:0:1}" "${s:1}" "${EXTRA_D:-}"; done
