#!/usr/bin/env bash
# r3: exports <name> of the worktree tree-ish into D:/tmp_od4r3/<name>, with the r2 trace and the r3 probe.
# bash prep.sh <name> <tree-ish> [notrace]
D=$(cd "$(dirname "$0")/.." && pwd); S=src/main/java/com/wormzjl/createcheme/science/fluid/network/PassiveStepSolver.java
bash "$D/r3/lane.sh" export "$1" "$2" | tail -1
[ "${3:-}" = notrace ] && exit 0
R2_ARRIVE="$D/r2/arrive.java.txt" perl -i "$D/r2/trace.pl" /d/tmp_od4r3/$1/$S
perl "$D/r3/probe.pl" /d/tmp_od4r3/$1; perl "$D/r3/passes.pl" /d/tmp_od4r3/$1
echo "trace lines: R2T $(grep -c R2T /d/tmp_od4r3/$1/$S) R3 $(grep -c 'R3\|r3describe' /d/tmp_od4r3/$1/src/main/java/com/wormzjl/createcheme/science/fluid/solver/*.java /d/tmp_od4r3/$1/$S | tr '\n' ' ')"
