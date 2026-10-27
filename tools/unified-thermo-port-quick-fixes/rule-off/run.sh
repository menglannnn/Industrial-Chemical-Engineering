#!/usr/bin/env bash
# Rule-off proofs of the quick-check fixes: for each rule, exports <tree> (a commit or a tree id of the worktree; default
# HEAD) to /tmp/forms-qf-off-<rule> with the forms tool's export.sh, switches the rule off (apply.pl), compiles main and
# the gate's test class on the forms lane, and runs that one gate test, which must fail. The worktree is never touched.
#   bash run.sh [tree] [rules...]      (rules default: every rule apply.pl knows)
# A rule may have several gates (space-separated selectors); each must fail.
set -u
HERE=$(cd "$(dirname "$0")" && pwd)
FORMS=${FORMS:-D:/Minecraft/Modding/1.21/CreateChemE/tools/unified-thermo-port-forms}
tree=${1:-HEAD}; shift || true
rules=${*:-root kept crystal box dry standin layout skip melting uvfreeze watertriple triangle}
solids=com.wormzjl.createcheme.science.fluid.network.EmptyNetworkTest#aVesselHoldingOnlySolidsIsADryEndAndGivesNothing
warm=com.wormzjl.createcheme.science.fluid.network.PrecipitatedSolidsTest#aWarmWaterVesselHasNothingToConvertAndAColdOneFreezes
declare -A gate=([root]=com.wormzjl.createcheme.science.fluid.thermo.HydrocarbonModelTest#aTrialWhoseCubicLosesItsRootIsARefusedTrial
  [kept]=com.wormzjl.createcheme.science.fluid.network.ConservativeTransportTest#aVesselNothingReachesKeepsWhatItHoldsItsSolidsIncluded
  [crystal]=com.wormzjl.createcheme.science.thermo.phase.GasSolidEquilibriumTest#aCrystalBesideAVapourAndALiquidMeetsTheToleranceInEach
  [box]=com.wormzjl.createcheme.science.fluid.thermo.HydrocarbonModelTest#aPartialPressureOutsideTheBoxIsRefusedBeforeTheKernel
  [dry]=$solids [standin]=$solids [layout]=$solids [skip]=$warm [melting]=$warm
  [uvfreeze]="com.wormzjl.createcheme.science.thermo.phase.FreeWaterIceTest#aRigidVesselOfNitrogenOverWaterFreezesOnTheMeltingLine com.wormzjl.createcheme.science.thermo.phase.FreeWaterIceTest#phAndUvAcrossTheFreezeLandOnTheMeltingLine com.wormzjl.createcheme.science.fluid.network.PrecipitatedSolidsTest#aTautVesselOfNitrogenOverWaterCooledThroughItsFreezeFreezesOnTheMeltingLine"
  [watertriple]=com.wormzjl.createcheme.science.thermo.phase.FreeWaterIceTest#pureWaterWithItsOwnSteamInARigidVesselStandsAtTheTriplePoint
  [triangle]=com.wormzjl.createcheme.science.thermo.phase.FreeWaterIceTest#pureWaterJustOutsideItsTriplePointIsAnsweredOnTheNeighbouringLine)
for rule in $rules; do
  name=qf-off-$rule; g=${gate[$rule]}
  extra=$(for s in $g; do echo "$(cygpath -m /tmp/forms-$name)/src/test/java/$(echo "${s%%#*}" | tr . /).java"; done | sort -u | tr '\n' ' ')
  NAME=$name bash "$FORMS/scripts/export.sh" "$tree" | tail -1
  perl "$HERE/apply.pl" /tmp/forms-$name $rule
  EXTRA_TESTS="$extra" bash "$FORMS/scripts/index-compile.sh" $name
  JAVA_OPTS="-Duser.language=en -Duser.country=US" bash "$FORMS/scripts/index-run.sh" $name $g 2>&1 | grep -E "^M3RUN" | head -12 | cut -c1-400
done
