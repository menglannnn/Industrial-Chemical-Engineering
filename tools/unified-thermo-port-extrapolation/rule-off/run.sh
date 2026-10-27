#!/usr/bin/env bash
# Rule-off proofs of the range-extrapolation gates (batch 2026-09-24-coolprop-low-temperature, WP1-WP3). Builds a tree
# from BASE (a commit or tree of claude/unified-thermo-port; default HEAD) with one rule switched off in a temporary
# index - the worktree's files are never touched - compiles it on the index lane and runs the gate tests that must fail.
#   bash run.sh all  [BASE]  continuation (Andrade slope, ideal-gas and crystal-segment enthalpy), the numerical box and
#                            the notice, all off in one tree: RangeContinuationGateTest (3 methods fail),
#                            CrystalReferenceTest#segments..., DataRangeContinuationTest#aPressureBelow..., FluidDataRangeIslandTest
#   bash run.sh cap  [BASE]  only the viscosity cap off: RangeContinuationGateTest#aContinuedViscosityIsCappedAtTheGlassValue
set -u
WT=${WT:-D:/Minecraft/Modding/1.21/CreateChemE/.claude/worktrees/coolprop-multiphase-thermo-37f6b0}
S=D:/Minecraft/Modding/1.21/CreateChemE/tools/unified-thermo-port-m3/scripts
mode=${1:-all}; base=${2:-HEAD}
R=/tmp/m3/ruleoff; mkdir -p $R; cd "$WT" || exit 2
V=src/main/java/com/wormzjl/createcheme/science/material/ViscosityCorrelation.java
N=src/main/java/com/wormzjl/createcheme/science/fluid/thermo/NetworkPhaseEngine.java
K=src/main/java/com/wormzjl/createcheme/science/material/CrystalReference.java
F=src/main/java/com/wormzjl/createcheme/science/fluid/thermo/FluidThermodynamics.java
P=src/main/java/com/wormzjl/createcheme/runtime/ProcessSolveServices.java
off() { # off <path> <perl -0 substitution>: the base's file with the rule off, as a blob in the temporary index
  git show "$base:$1" | perl -pe 's/\r\n/\n/' > $R/file.java
  perl -0pi -e "$2" $R/file.java
  if git show "$base:$1" | perl -pe 's/\r\n/\n/' | cmp -s - $R/file.java; then echo "rule edit did not apply: $1"; exit 3; fi
  GIT_INDEX_FILE=$(cygpath -m $R/index) git update-index --cacheinfo 100644,$(git hash-object -w $R/file.java),$1
}
GIT_INDEX_FILE=$(cygpath -m $R/index) git read-tree "$base"
C=com.wormzjl.createcheme
if [ "$mode" = cap ]; then
  off $V 's/return log >= LOG_MAXIMUM_VISCOSITY \? MAXIMUM_VISCOSITY : DeterministicMath\.exp\(log\);/return DeterministicMath.exp(log);/; s/        if \(!\(value <= MAXIMUM_VISCOSITY\)\) value = MAXIMUM_VISCOSITY;\n//'
  tests="$C.science.fluid.thermo.RangeContinuationGateTest#aContinuedViscosityIsCappedAtTheGlassValue"
else
  off $V 's/if \(phase == Phase\.LIQUID && t > te\) return endValue;/if (phase == Phase.LIQUID) return endValue;/; s/return log >= LOG_MAXIMUM_VISCOSITY \? MAXIMUM_VISCOSITY : DeterministicMath\.exp\(log\);/return DeterministicMath.exp(log);/; s/        if \(!\(value <= MAXIMUM_VISCOSITY\)\) value = MAXIMUM_VISCOSITY;\n//'
  off $N 's/return e==t\?h:h\+fitHeatCapacity\(e\)\*\(t-e\);/return h;/'
  off $K 's/return e == t \? h : h \+ cp\[k\]\.heatCapacity\(e\) \* \(t - e\);/return h;/'
  off $F 's/(only the numerical box refuses a trial, a plain IllegalArgumentException\.\n)        NumericalBox\.require\(t,p\);\n/$1/'
  off $P 's/Optional\.empty\(\),notices\(model,candidate\)\);/Optional.empty(),List.of());/g'
  tests="$C.science.fluid.thermo.RangeContinuationGateTest $C.science.material.CrystalReferenceTest#segmentsContinuePastTheirRangeWithValueAndSlope $C.science.fluid.thermo.DataRangeContinuationTest#aPressureBelowTheEnvelopeIsCarriedAndOnlyTheNumericalBoxRefusesAState $C.runtime.FluidDataRangeIslandTest"
fi
tree=$(GIT_INDEX_FILE=$(cygpath -m $R/index) git write-tree)
export JAVA_TOOL_OPTIONS="-Duser.language=en -Duser.country=US"
bash $S/index-compile.sh "$tree"
bash $S/index-run.sh $tests 2>&1 | grep -E "M3RUN (FAILED|SUMMARY)|^M3RUN   [a-z]"
echo "Recompile the head (bash $S/index-compile.sh) before running anything else on the index lane."
