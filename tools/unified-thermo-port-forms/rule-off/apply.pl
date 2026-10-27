#!/usr/bin/perl
# Switches one owner rule off in an exported tree (arguments: the tree's root, the rule), for the rule-off proof of its gate:
# the gate is then run alone and must fail. One literal substring replacement per edit, each matching exactly once
# (CRLF-insensitive). The rules match the branch's final code (the F2 commit) unless a commit is named.
#   d26      the D26 amendment: the own split refuses nothing                     gate (a) aLiquidFullLngTankBesideDryIceAtItsBubblePointHoldsNoVapour
#   f4ii     form 4 (ii): a phase the closure drives to nothing is not refused     aLiquidFullLngTankWhoseClosureStopsShortOfNothingHoldsNoVapour
#   f4i      form 4 (i): the stand-in is not proposed (D31 B's lacking)            gate (b) aFrostedGasTanksStandInIsDecidedByItsOwnSplitAndRefused,
#                                                                                  M3's aLetDownThatWouldFreezePassesAndTheReceivingTankPrecipitatesAtItsRefresh (F1)
#   d33g     D33, the gas half: a bulk end always draws the bulk                   gate (c) aFrostingTanksSideOutletPassesGasAndKeepsTheFrost, first half
#   d33r     D33, the rest half: closures read open only while a node holds solids gate (c), second half
#   e9       an unresolved product's stability is a plain refusal again            NetworkPhaseEngineTest#aSplitWhoseProductStabilityIsUnresolvedIsHeldTyped
#   water    a failed closure carrying a hold is not named                         WaterIntoCryogenicMethaneTest#waterDrivenIntoTheCooledVesselHoldsOnTheWaterDomain
#   f4after  phi read after the freeze (the head's rule)                           aRefreshFreezesCrystalMassAtTheSizeOfTheLiquidItFrozeFrom (the slush)
#   f4always phi read before the freeze even where no liquid is left               aRefreshFreezesCrystalMassAtTheSizeOfTheLiquidItFrozeFrom (the frost)
#   f2seed   F2 fix A, the stream seed: a mover's suction line is seeded as before   SlurryTankPumpTest (both gates)
#   f2inlet  F2 fix A, the slice's inlet decision reads the closures as they stood  SlurryTankPumpTest#aSuctionLineTheSlowTargetClosedOpensUnderTheRaisedTarget
#   e8       E8 (M4c): the folded vessel is only checked, not closed again             PrecipitatedSolidsTest#aLiquidFullTankWhoseClosureFoldsItsLastBubbleIsClosedAgainWithoutIt
#   d33i     D33 (M4c), the interval start: every path but the rest test reads the closures as they stood (the head b46b4a81)
#                                                                                  gate (c) aFrostingTanksSideOutletPassesGasAndKeepsTheFrost, the thawed outlet
# d33r and f2inlet match b46b4a81: the D33 completion moved both readings to the interval start (d33i).
# f4i is also the proof of LiquidCo2LetDownTest (D23-1 at 4 mm and 5 cm).
# D32's proof (LoneBoundaryJoinTest) ran on an export of the fix commit with the fix reversed (logs 25-*).
use strict; use warnings;
my ($root,$rule)=@ARGV; die "usage: apply.pl <exported tree> <rule>" unless $rule;
my $f="$root/src/main/java/com/wormzjl/createcheme/science/fluid";
my $refusal='                if(own!=null)refused=proposed&(~lacking(kept,seed)|(junction?0:vanishing(start,own)));';
my %rules=(
 d26=>[["$f/thermo/FluidThermodynamics.java",$refusal,'                // rule off (D26 amendment): no refusal']],
 f4ii=>[["$f/thermo/FluidThermodynamics.java",$refusal,'                if(own!=null)refused=proposed&~lacking(kept,seed); // rule off (form 4, ii)']],
 f4i=>[["$f/thermo/FluidThermodynamics.java",'    private int lacking(State node,State answer){return ownPhaseBits(answer)&~ownPhaseBits(node)|phaseBits(answer)&~phaseBits(node);}',
        '    private int lacking(State node,State answer){return ownPhaseBits(answer)&~ownPhaseBits(node);} // rule off (form 4, i)']],
 d33g=>[["$f/thermo/FluidThermodynamics.java",'        return !(state.liquidVolume()>0)&&!(state.waterVolume()>0)&&state.vaporVolume()>0&&!state.solids().empty();',
         '        return false; // rule off (D33, the gas half)']],
 d33r=>[["$f/network/NetworkReduction.java",'        var drives=drives(flows==null?reopened(graph):graph,model);',
         '        var drives=drives(graph.reservoirs().stream().anyMatch(r->!r.inventory().solids().empty())&&flows==null?reopened(graph):graph,model); // rule off (D33, the rest half)']],
 e9=>[["$f/thermo/FluidThermodynamics.java",'if(unstable||result.detail().startsWith(EquilibriumResult.UNRESOLVED_PRODUCT))throw','if(unstable)throw']],
 water=>[["$f/network/ConservativeTransport.java",'            catch(SparseNewton.Nonconvergence failure){throw failure.at(reservoir.id());}','            // rule off (water)'],
         ["$f/network/InventoryEquilibrium.java",'            catch(SparseNewton.Nonconvergence failure){throw failure.at(node.id());}','            // rule off (water)']],
 f4after=>[["$f/network/VesselCrystals.java",'stock(inventory.solids(),inert,freezing(closed,beside),all,held,mass)','stock(inventory.solids(),inert,beside,all,held,mass)']],
 f4always=>[["$f/network/VesselCrystals.java",'return beside!=null&&beside.liquidVolume()+beside.waterVolume()>0?closed:null;','return closed;']],
 f2seed=>[["$f/network/PassiveStepSolver.java",'var seed=supplies?endStream(supplier.state(),last.portAt(walk.node())):null;','FluidThermodynamics.State seed=null; // rule off (F2 fix A, the stream seed)']],
 f2inlet=>[["$f/network/PassiveIntervalSolver.java",'InletPhase.decide(model,NetworkReduction.reopened(initial),modes,duration)','InletPhase.decide(model,initial,modes,duration)']],
 d33i=>[["$f/network/PassiveIntervalSolver.java",'        initial=NetworkReduction.reopened(committed);','        initial=committed; // rule off (D33, the interval start)'],
       ["$f/network/PassiveIntervalSolver.java",'NetworkReduction.atRest(initial,model,null,refused)','NetworkReduction.atRest(NetworkReduction.reopened(initial),model,null,refused)'],
       ["$f/network/PassiveIntervalSolver.java",'InletPhase.decide(model,initial,modes,duration)','InletPhase.decide(model,NetworkReduction.reopened(initial),modes,duration)'],
       ["$f/network/PassiveIntervalSolver.java",'        initial=NetworkReduction.reopened(PassiveNetwork.sizeJunctionHoldups(initial,model));','        initial=PassiveNetwork.sizeJunctionHoldups(initial,model);']],
 e8=>[["$f/network/InventoryEquilibrium.java",'        if(!regime(closed).equals(regime(state))) {','        if(false) { // rule off (E8): the folded state is only checked, in its own layout'],
     ["$f/network/InventoryEquilibrium.java",'        x=layout.encode(closed);residual=new double[layout.size()];',
      '        var check=regime(closed).equals(regime(state))?layout:layout(model,closed,null);x=check.encode(closed);residual=new double[check.size()];'],
     ["$f/network/InventoryEquilibrium.java",'        layout.residual(closed,n,energy,volume,workFrom,residual,0,x);','        check.residual(closed,n,energy,volume,workFrom,residual,0,x);']],
);
my $edits=$rules{$rule} or die "unknown rule $rule";
for my $e (@$edits) {
  my ($file,$from,$to)=@$e;
  open my $in,'<:raw',$file or die "$file: $!"; local $/; my $text=<$in>; close $in;
  my $crlf=$text=~/\r\n/;$text=~s/\r\n/\n/g;
  my $count=()=$text=~/\Q$from\E/g;
  die "$file: expected one match, found $count for: $from\n" unless $count==1;
  $text=~s/\Q$from\E/$to/;$text=~s/\n/\r\n/g if $crlf;
  open my $out,'>:raw',$file or die "$file: $!"; print $out $text; close $out;
}
print "rule-off $rule: ".scalar(@$edits)." edit(s) under $root\n";
