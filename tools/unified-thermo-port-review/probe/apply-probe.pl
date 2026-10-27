#!/usr/bin/perl
# Applies the review probe's call-site edits to an exported tree (argument: the tree's root), each a literal one-line
# substring replacement that must match exactly once (CRLF kept). Used by scripts/export.sh with PROBE=1 on 76e87740.
use strict; use warnings;
my $root=shift or die "usage: apply-probe.pl <exported tree>";
my $P='com.wormzjl.createcheme.science.fluid.ReviewProbe';
my $base="$root/src/main/java/com/wormzjl/createcheme/science/fluid";
my @edits=(
 ["$base/thermo/FluidThermodynamics.java",
  '        if(hydrocarbonComplete&&waterComplete)return kept;',
  "        if(hydrocarbonComplete&&waterComplete){$P.complete(junction);return kept;}"],
 ["$base/thermo/FluidThermodynamics.java",
  '        if((phaseBits(equilibrium)&~phaseBits(kept)&~excluded)==0)return kept;',
  "        if((phaseBits(equilibrium)&~phaseBits(kept)&~excluded)==0){$P.tpNothing(junction);return kept;}"],
 ["$base/thermo/FluidThermodynamics.java",
  '        State own=junction?equilibriumPH(',
  "        long rpIt=$P.iterations();State own=junction?equilibriumPH("],
 ["$base/thermo/FluidThermodynamics.java",
  'SolverDiagnostics.count(own==null?SolverDiagnostics.phaseSeedFallbacks:SolverDiagnostics.phaseSeeds);',
  "SolverDiagnostics.count(own==null?SolverDiagnostics.phaseSeedFallbacks:SolverDiagnostics.phaseSeeds);$P.own(junction,own==null,rpIt);"],
 ["$base/thermo/FluidThermodynamics.java",
  '        return (phaseBits(seed)&~phaseBits(kept)&~excluded)==0?kept:seed;',
  "        boolean rpAdopt=(phaseBits(seed)&~phaseBits(kept)&~excluded)!=0;$P.outcome(junction,rpAdopt);return rpAdopt?seed:kept;"],
 ["$base/network/InventoryEquilibrium.java",
  '                var checked=model.phaseCheck(trial,false,excluded,null,own,checkpoint);',
  "                FluidThermodynamics.State checked;$P.push(\"closure-fail\");try{checked=model.phaseCheck(trial,false,excluded,null,own,checkpoint);}finally{$P.pop();}"],
 ["$base/network/InventoryEquilibrium.java",
  '            var checked=model.phaseCheck(candidate,false,excluded,null,own,checkpoint);',
  "            FluidThermodynamics.State checked;$P.push(\"closure-conv\");try{checked=model.phaseCheck(candidate,false,excluded,null,own,checkpoint);}finally{$P.pop();}"],
 ["$base/network/PassiveStepSolver.java",
  'var changedSeeds=phaseCorrection(graph,equations.states(failure.lastVariables()),checkpoint,null,foldedPhases.clone());',
  "List<FluidThermodynamics.State> changedSeeds;$P.push(\"step-fail\");try{changedSeeds=phaseCorrection(graph,equations.states(failure.lastVariables()),checkpoint,null,foldedPhases.clone());}finally{$P.pop();}"],
 ["$base/network/PassiveStepSolver.java",
  'var changedSeeds=phaseCorrection(graph,projection.states(),checkpoint,equations.cachedPrepared,foldedPhases);if(changedSeeds!=null){seeds=changedSeeds;continue;}',
  "List<FluidThermodynamics.State> changedSeeds;$P.push(\"step-conv\");try{changedSeeds=phaseCorrection(graph,projection.states(),checkpoint,equations.cachedPrepared,foldedPhases);}finally{$P.pop();}if(changedSeeds!=null){seeds=changedSeeds;continue;}"],
 ["$base/network/PassiveStepSolver.java",
  '||phaseCorrection(graph,projection.states(),checkpoint,equations.cachedPrepared,foldedPhases.clone())!=null)return null;',
  "||$P.in(\"step-polish\",()->phaseCorrection(graph,projection.states(),checkpoint,equations.cachedPrepared,foldedPhases.clone()))!=null)return null;"],
 ["$base/network/PassiveStepSolver.java",
  'if(phaseCorrection(equations.graph,corrected,checkpoint)!=null)throw new ApproximationAnchor.Rejected',
  "if($P.in(\"step-probe\",()->phaseCorrection(equations.graph,corrected,checkpoint))!=null)throw new ApproximationAnchor.Rejected"],
 ["$base/network/PassiveIntervalSolver.java",
  'var refreshed=model.crystalSolids().isEmpty()?null:InventoryEquilibrium.refresh(initial,model,checkpoint);',
  "InventoryEquilibrium.Refreshed refreshed=null;if(!model.crystalSolids().isEmpty()){$P.push(\"refresh-solve\");try{refreshed=InventoryEquilibrium.refresh(initial,model,checkpoint);}finally{$P.pop();}}"],
 ["$base/network/PassiveIntervalSolver.java",
  'var refreshed=acceptance==PassiveStepSolver.Acceptance.FULL?InventoryEquilibrium.refresh(initial,model,checkpoint):null;',
  "InventoryEquilibrium.Refreshed refreshed=null;if(acceptance==PassiveStepSolver.Acceptance.FULL){$P.push(\"refresh-integrate\");try{refreshed=InventoryEquilibrium.refresh(initial,model,checkpoint);}finally{$P.pop();}}"],
 ["$base/network/SolidEventIntegrator.java",
  'var refreshed=InventoryEquilibrium.refresh(initial,model,checkpoint);var graph=refreshed.graph();',
  "InventoryEquilibrium.Refreshed refreshed;$P.push(\"refresh-solid\");try{refreshed=InventoryEquilibrium.refresh(initial,model,checkpoint);}finally{$P.pop();}var graph=refreshed.graph();"],
);
for my $e (@edits) {
  my ($file,$from,$to)=@$e;
  open my $in,'<:raw',$file or die "$file: $!"; local $/; my $text=<$in>; close $in;
  my $count=()=$text=~/\Q$from\E/g;
  die "$file: expected one match, found $count for: $from\n" unless $count==1;
  $text=~s/\Q$from\E/$to/;
  open my $out,'>:raw',$file or die "$file: $!"; print $out $text; close $out;
}
print "apply-probe: ".scalar(@edits)." edits applied under $root\n";
