#!/usr/bin/env bash
# r2: exports <name> of the worktree tree-ish (default HEAD), optionally with fix 2 off, then the trace.
# bash prep.sh <name> <tree-ish> [fix2off]
D=$(cd "$(dirname "$0")" && pwd); S=src/main/java/com/wormzjl/createcheme/science/fluid/network/PassiveStepSolver.java
bash "$D/../lane.sh" export "$1" "$2" | tail -1
[ "${3:-}" = fix2off ] && perl -i "$D/fix2off.pl" /tmp/od4-$1/$S
R2_ARRIVE="$D/arrive.java.txt" perl -i "$D/trace.pl" /tmp/od4-$1/$S; echo "R2T lines: $(grep -c R2T /tmp/od4-$1/$S)"
