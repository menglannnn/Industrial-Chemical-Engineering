#!/usr/bin/env bash
# Quick solver-effect check of one compiled tree (batch 2026-09-27-jdk-determinism, pow performance study):
# the 33 junction ledger lines (MixedGasJunctionTransientTest + LiquidJunctionTransientTest) against a reference file
# (default: the harness reference = the prototype's values), and chain-100 exact against src/test/resources.
#   SRC=<tree> RES=<worktree> OUT=<compiled> check.sh <log-dir> [reference-lines]
set -uo pipefail
HERE=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
LOG=${1:?log dir}; REF=${2:-$HERE/../../cloud-science-harness/reference/junction-lines.txt}
mkdir -p "$LOG"
bash "$HERE/wbuild.sh" run class:com.wormzjl.createcheme.runtime.fluid.MixedGasJunctionTransientTest \
  class:com.wormzjl.createcheme.runtime.fluid.LiquidJunctionTransientTest > "$LOG/junction.txt" 2>&1
grep -E "MINILAUNCHER" "$LOG/junction.txt"
norm() { grep -v "^#" "$1" | grep -oE "(MIXED_GAS_(STATIC|TRANSIENT|COST)|LIQUID_JUNCTION) .*" | sed -E 's/\r$//; s/ ms=[0-9]+//; s/, bytes=[0-9]+//; s/ allocatedMB=[0-9.]+//' | sort; }
norm "$LOG/junction.txt" > "$LOG/junction-lines.txt"
if diff <(norm "$REF") "$LOG/junction-lines.txt" > "$LOG/junction-lines.diff"; then
  echo "junction: $(wc -l < "$LOG/junction-lines.txt") lines identical to $(basename "$REF")"
else
  echo "junction: $(grep -c '^>' "$LOG/junction-lines.diff") of $(wc -l < "$LOG/junction-lines.txt") lines differ from $(basename "$REF") (diff in $LOG/junction-lines.diff)"
fi
grep -oE "MIXED_GAS_COST .*" "$LOG/junction.txt"
JVM_OPTS="-Xmx2g -Dfluid.regression.mode=exact -Dfluid.regression.capture=false" bash "$HERE/wbuild.sh" run \
  class:com.wormzjl.createcheme.fluid.benchmark.FluidSolverRegressionTest > "$LOG/regression.txt" 2>&1
grep -E "MINILAUNCHER|chain-100 vs reference" "$LOG/regression.txt"
# accepted, rejected, newtonSolves, newtonIterations, residualEvaluations of the chain-100 table row
awk -F'|' '/^\| 0 \|/{print "chain-100: accepted="$5" rejected="$6" newtonSolves="$9" newtonIterations="$10" residualEvaluations="$12; exit}' "$LOG/regression.txt" | tr -s ' '
