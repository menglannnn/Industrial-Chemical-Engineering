#!/usr/bin/env bash
# Exports <tree-ish> as /tmp/d38-<name> with the cold harness (hold-fuzz-cold), the M7 trace world and the D38 probe added
# to its runtime.fluid tests, and compiles them (lane.sh). Usage (worktree root): probe-export.sh <name> <tree-ish>
set -u
D=$(cd "$(dirname "$0")" && pwd); WT=${WT:-D:/Minecraft/Modding/1.21/CreateChemE/.claude/worktrees/coolprop-multiphase-thermo-37f6b0}
name=${1:?name}; tree=${2:?tree-ish}
bash "$D/lane.sh" export "$name" "$tree" || exit 3
T=/tmp/d38-$name/src/test/java/com/wormzjl/createcheme/runtime/fluid
cp "$WT/tools/unified-thermo-port-forms/hold-fuzz-cold/src/HoldFuzzTest.java" "$WT/tools/unified-thermo-port-m5/trace/src/M5TraceWorld.java" \
   "$WT/tools/unified-thermo-port-m5/trace/src/M5MinimizerTest.java" "$WT/tools/unified-thermo-port-m5/trace/src/M5SolveLogTest.java" "$D"/probe/*.java "$T/"
P=com/wormzjl/createcheme/runtime/fluid; files=""; for f in "$D"/probe/*.java; do files="$files $P/$(basename "$f")"; done
bash "$D/lane.sh" compile "$name" $files $P/HoldFuzzTest.java $P/M5TraceWorld.java $P/M5MinimizerTest.java $P/M5SolveLogTest.java
