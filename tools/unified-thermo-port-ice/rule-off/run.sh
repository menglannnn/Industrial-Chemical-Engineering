#!/usr/bin/env bash
# Rule-off proofs of the ice gates (batch 2026-09-24-coolprop-low-temperature, WP4 of RANGE_EXTRAPOLATION_PLAN.md).
# Builds a tree from BASE (a commit of claude/unified-thermo-port; default HEAD) with one rule switched off in a temporary
# index - the worktree's files are never touched - compiles it on the index lane and runs the gate tests that must fail.
#   bash run.sh a    [BASE]  free water below its melting line is not ice (FluidTpEquilibrium.frozen false):
#                            FreeWaterIceTest, all four methods (gates a and b)
#   bash run.sh b    [BASE]  the freeze step from the balance off (FluidTpEquilibrium.freezeStep false):
#                            FreeWaterIceTest#phAndUvAcrossTheFreezeLandOnTheMeltingLine (gate b)
#   bash run.sh c    [BASE]  a vessel's refresh does not test the ice (VesselCrystals wet false):
#                            WaterIntoCryogenicMethaneTest#waterDrivenIntoTheCooledVesselFreezesAtTheRefresh (gate c)
#   bash run.sh span [BASE]  the steam's difference step uncapped (PhaseLayout.logReadFloor returns the floor): gate (c)
set -u
WT=${WT:-D:/Minecraft/Modding/1.21/CreateChemE/.claude/worktrees/coolprop-multiphase-thermo-37f6b0}
S=D:/Minecraft/Modding/1.21/CreateChemE/tools/unified-thermo-port-m3/scripts
mode=${1:-a}; base=${2:-HEAD}
R=/tmp/m3/ruleoff-ice; mkdir -p $R; cd "$WT" || exit 2
F=src/main/java/com/wormzjl/createcheme/science/thermo/phase/FluidTpEquilibrium.java
V=src/main/java/com/wormzjl/createcheme/science/fluid/network/VesselCrystals.java
L=src/main/java/com/wormzjl/createcheme/science/fluid/solver/PhaseLayout.java
off() { # off <path> <perl -0 substitution>: the base's file with the rule off, as a blob in the temporary index
  git show "$base:$1" | perl -pe 's/\r\n/\n/' > $R/file.java
  perl -0pi -e "$2" $R/file.java
  if git show "$base:$1" | perl -pe 's/\r\n/\n/' | cmp -s - $R/file.java; then echo "rule edit did not apply: $1"; exit 3; fi
  GIT_INDEX_FILE=$(cygpath -m $R/index) git update-index --cacheinfo 100644,$(git hash-object -w $R/file.java),$1
}
GIT_INDEX_FILE=$(cygpath -m $R/index) git read-tree "$base"
C=com.wormzjl.createcheme
case "$mode" in
  a) off $F 's/return t < freeWater\.ice\(\)\.meltingTemperature\(p\);/return false;/'
     tests="$C.science.thermo.phase.FreeWaterIceTest";;
  b) off $F 's/return ice\.ice\(\) > 0\.0 && ice\.liquidWater\(\) == 0\.0 && liquid\.liquidWater\(\) > 0\.0 && liquid\.ice\(\) == 0\.0;/return false;/'
     tests="$C.science.thermo.phase.FreeWaterIceTest#phAndUvAcrossTheFreezeLandOnTheMeltingLine";;
  c) off $V 's/wet=totals\[totals\.length-1\]>0;/wet=false;/'
     tests="$C.science.fluid.network.WaterIntoCryogenicMethaneTest#waterDrivenIntoTheCooledVesselFreezesAtTheRefresh";;
  span) off $L 's/private double logReadFloor\(double floored\)\{return Math\.min\(floored,Math\.max\(steamSeed,LOG_READ_SPAN\*steamSeed\)\);\}/private double logReadFloor(double floored){return floored;}/'
     tests="$C.science.fluid.network.WaterIntoCryogenicMethaneTest#waterDrivenIntoTheCooledVesselFreezesAtTheRefresh";;
  *) echo "mode a, b, c or span"; exit 2;;
esac
tree=$(GIT_INDEX_FILE=$(cygpath -m $R/index) git write-tree)
export JAVA_TOOL_OPTIONS="-Duser.language=en -Duser.country=US"
bash $S/index-compile.sh "$tree"
bash $S/index-run.sh $tests 2>&1 | grep -E "M3RUN (FAILED|SUMMARY)|^M3RUN   [a-zA-Z]|^WP4 gate"
echo "Recompile the head (bash $S/index-compile.sh) before running anything else on the index lane."
