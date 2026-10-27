#!/usr/bin/env perl
# Switches one quick-check fix off in an exported tree (/tmp/forms-<name>, the forms tool's export.sh), restoring the code
# before it, for its gate's rule-off proof; the worktree is never touched. Usage: perl apply.pl /tmp/forms-<name> <rule>
#   root     HydrocarbonModel.phase lets the kernel's NoPhysicalRoot escape again instead of refusing the trial with
#            RootAbsent. Gate: HydrocarbonModelTest#aTrialWhoseCubicLosesItsRootIsARefusedTrial
#   kept     ConservativeTransport states a kept vessel (holds nothing, nothing reaches it) with the Newton candidate's
#            solids again, which its inventory then books. Gate: ConservativeTransportTest#aVesselNothingReachesKeepsWhatItHoldsItsSolidsIncluded
#   crystal  FluidTpEquilibrium's general crystal solve stops on its first fluid phase's |mu_i - mu_s|/RT again, and
#            refuses the answer after the loop when another phase is outside the tolerance.
#            Gate: GasSolidEquilibriumTest#aCrystalBesideAVapourAndALiquidMeetsTheToleranceInEach
#   (follow-up, decisions 1 and 2)
#   box      FluidThermodynamics.state reads a vapour's partial pressure without the numerical box, its floor alone
#            guarding it. Gate: HydrocarbonModelTest#aPartialPressureOutsideTheBoxIsRefusedBeforeTheKernel
#   dry      PassiveStepSolver.dryEnd is dry only for a vessel holding nothing, not for one holding only solids.
#   standin  a state holding no fluid has no stream properties: its bulk viscosity is 0/0, its acoustic bound refused.
#   layout   a step lays out dry only a vessel holding nothing; one holding only solids is laid out with unknowns.
#            Gate of dry, standin and layout: EmptyNetworkTest#aVesselHoldingOnlySolidsIsADryEndAndGivesNothing
#   (option (a) of the fallback regression)
#   skip     the refresh the rest test reads (InventoryEquilibrium.conversions) visits every node again, as refresh does.
#   melting  VesselCrystals.competes lets water compete at any temperature again, not only at or below its melting line.
#            Gate of skip and melting: PrecipitatedSolidsTest#aWarmWaterVesselHasNothingToConvertAndAColdOneFreezes
#   (the fixed-volume freeze step)
#   uvfreeze FluidTpEquilibrium.freezingAtVolume answers nothing: a UV step across the melting line is left to the
#            searches again. Gates: FreeWaterIceTest#aRigidVesselOfNitrogenOverWaterFreezesOnTheMeltingLine and
#            #phAndUvAcrossTheFreezeLandOnTheMeltingLine (its TP calls), and
#            PrecipitatedSolidsTest#aTautVesselOfNitrogenOverWaterCooledThroughItsFreezeFreezesOnTheMeltingLine
#   (pure water's triple point)
#   watertriple  FluidTpEquilibrium.waterTriplePoint answers nothing: pure water with only its own steam is left to the
#            UV search again. Gate: FreeWaterIceTest#pureWaterWithItsOwnSteamInARigidVesselStandsAtTheTriplePoint
#   triangle insideTriangle takes an amount within 1e-6 of the moles below zero again, not only where the answer still
#            meets the energy and volume. Gate: FreeWaterIceTest#pureWaterJustOutsideItsTriplePointIsAnsweredOnTheNeighbouringLine
use strict; use warnings;
my ($d, $rule) = @ARGV; die "usage: apply.pl <tree> <rule>" unless $d && $rule;
sub edit { my ($f, $from, $to) = @_; $f = "$d/$f"; local $/; open my $h, '<:raw', $f or die "$f: $!"; my $s = <$h>; close $h;
  my $n = index($s, $from); die "pattern not found in $f: $from" if $n < 0;
  substr($s, $n, length($from)) = $to; open $h, '>:raw', $f or die; print $h $s; close $h; print "rule $rule off in $f\n"; }
my $base = 'src/main/java/com/wormzjl/createcheme/science';
my $step = "$base/fluid/network/PassiveStepSolver.java";
if ($rule eq 'root') {
  edit("$base/fluid/thermo/HydrocarbonModel.java",
    'try{values=translated.evaluateValues(t,p,amounts,root,terms);}',
    'values=translated.evaluateValues(t,p,amounts,root,terms);if(false)try{throw new PengRobinsonKernel.NoPhysicalRoot(root);}');
} elsif ($rule eq 'kept') {
  edit("$base/fluid/network/ConservativeTransport.java",
    'states.add(kept[node]&&!state.solids().equals(held)?state.withSolids(held):state);',
    'states.add(state);');
} elsif ($rule eq 'crystal') {
  my $f = "$base/thermo/phase/FluidTpEquilibrium.java";
  edit($f, 'if (reduced != null && iterations > 0 && worst <= DEPOSITION_TOLERANCE) break;',
    'if (reduced != null && iterations > 0 && Math.abs(f) <= DEPOSITION_TOLERANCE) break;');
  edit($f, 'double residual = complete ? Double.NaN : worst;',
    'double residual = Double.NaN; if (!complete) { residual = 0.0; for (PhaseAmounts phase : reduced.phases()) {'
    .' residual = Math.max(residual, Math.abs((phase.state().chemicalPotential(i) - solidMu) / rt)); }'
    .' if (!(residual <= DEPOSITION_TOLERANCE)) { return EquilibriumResult.notConverged(Specification.TP, contract.identity(),'
    .' competition, "the fluid left by " + c.id() + " disagrees with the crystal (|mu_i - mu_s|/RT = " + residual'
    .' + " in a fluid phase)", diagnostics(w, w.feedResult, reduced.diagnostics().criticalBand())); } }');
} elsif ($rule eq 'box') {
  edit("$base/fluid/thermo/FluidThermodynamics.java", 'if(nv>0)NumericalBox.requirePartialPressure(hydrocarbonPressure);',
    'if(nv>0&&!(hydrocarbonPressure>=NumericalBox.MINIMUM_PARTIAL_PRESSURE))throw new IllegalArgumentException("Hydrocarbon vapour partial pressure below the numerical floor");');
} elsif ($rule eq 'dry') {
  edit($step, 'if(holdsNoFluid(node))return true;', 'if(node.inventory().solids().empty()&&holdsNoFluid(node))return true;');
} elsif ($rule eq 'standin') {
  edit($step, 'if(!FluidThermodynamics.holdsFluid(s))return viscosity(ambient(),null);', '');
  edit($step, 'if(!FluidThermodynamics.holdsFluid(s))return carrierViscosity(ambient());', '');
  edit("$base/fluid/network/NetworkReduction.java", 'if(!FluidThermodynamics.holdsFluid(s))return viscosity(model,ambient(model),scratch);', '');
  edit("$base/fluid/thermo/FluidThermodynamics.java", 'if(compliance==0&&!holdsFluid(state))return Double.POSITIVE_INFINITY;', '');
} elsif ($rule eq 'layout') {
  edit($step, 'return node.kind()==PassiveNetwork.NodeKind.RESERVOIR&&holdsNoFluid(node);',
    'return node.kind()==PassiveNetwork.NodeKind.RESERVOIR&&node.inventory().solids().empty()&&holdsNoFluid(node);');
} elsif ($rule eq q{skip}) {
  edit("$base/fluid/network/InventoryEquilibrium.java", q{return refresh(graph,model,checkpoint,node->mayConvert(node,model));},
    q{return refresh(graph,model,checkpoint,node->true);});
} elsif ($rule eq q{melting}) {
  edit("$base/fluid/network/VesselCrystals.java", q{totals[totals.length-1]>0&&t<=model.freeWaterIce().meltingTemperature(state.pressure())},
    q{totals[totals.length-1]>0});
} elsif ($rule eq q{uvfreeze}) {
  edit("$base/thermo/phase/FluidTpEquilibrium.java", q{if (w.freezeAttempts >= FREEZE_ATTEMPTS) return null;}, q{if (true) return null;});
} elsif ($rule eq q{watertriple}) {
  edit("$base/thermo/phase/FluidTpEquilibrium.java", q{        if (waterTriangle == null) return null;}, q{        if (true) return null;});
} elsif ($rule eq q{triangle}) {
  edit("$base/thermo/phase/FluidTpEquilibrium.java", q{        if (solved[0] >= 0.0 && solved[2] >= 0.0 && nl >= 0.0) return new double[] {nv, nl, ns};},
    q{        double allowance = SPECIFICATION_TOLERANCE * n; for (double amount : solved) if (!(amount >= -allowance)) return null;}
    . q{ if (!(nl >= -allowance)) return null; if (true) return new double[] {nv, Math.max(nl, 0.0), ns};});
} else { die "unknown rule $rule"; }
