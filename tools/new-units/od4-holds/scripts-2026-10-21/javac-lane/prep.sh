#!/usr/bin/env bash
# t285: export <name> of <tree-ish> into /d/tmp_285/x/<name>, r2 trace (PassiveStepSolver) + t285 phaseCheck trace.
set -u
T=/d/Minecraft/Modding/1.21/CreateChemE/tools/new-units/od4-holds; S=src/main/java/com/wormzjl/createcheme/science/fluid
bash /d/tmp_285/scripts/lane.sh export "$1" "$2" "${@:3}" | tail -1
R2_ARRIVE="$T/r2/arrive.java.txt" perl -i "$T/r2/trace.pl" /d/tmp_285/x/$1/$S/network/PassiveStepSolver.java
perl -i /d/tmp_285/scripts/trace285.pl /d/tmp_285/x/$1/$S/thermo/FluidThermodynamics.java
echo "trace lines: R2 $(grep -c R2T /d/tmp_285/x/$1/$S/network/PassiveStepSolver.java) T285 $(grep -c T285 /d/tmp_285/x/$1/$S/thermo/FluidThermodynamics.java)"
