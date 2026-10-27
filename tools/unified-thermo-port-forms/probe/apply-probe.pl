#!/usr/bin/perl
# Applies the forms probe's edit to an exported tree (argument: the tree's root): InventoryEquilibrium.split's stalled
# closure is bucketed (FormsProbe) before it returns the restated last iterate, unchanged. One literal substring
# replacement that must match exactly once (CRLF kept). Used by scripts/export.sh with PROBE=1.
use strict; use warnings;
my $root=shift or die "usage: apply-probe.pl <exported tree>";
my $P='com.wormzjl.createcheme.science.fluid.FormsProbe';
my $base="$root/src/main/java/com/wormzjl/createcheme/science/fluid";
my @edits=(
 ["$base/network/InventoryEquilibrium.java",
  '            try{return ConservativeTransport.repartition(model,layout(model,start,null).decode(failure.lastVariables(),0),inventory.moles());}',
  "            try{var fpL=layout(model,start,null);var fpD=fpL.decode(failure.lastVariables(),0);var fpR=ConservativeTransport.repartition(model,fpD,inventory.moles());\n"
 ."                double fpS=$P.vapourShare(start),fpV=$P.vapourShare(fpD);String fpK=$P.residual(failure);\n"
 ."                $P.add(\"A start-vap \"+$P.q(fpS));$P.add(\"B last-vap \"+$P.q(fpV)+\" \"+fpK);$P.add(\"C restated-vap \"+$P.q($P.vapourShare(fpR)));\n"
 ."                if(fpV>=1e-9&&fpS>fpV){boolean fpE=com.wormzjl.createcheme.science.fluid.diagnostics.SolverDiagnostics.ENABLED;com.wormzjl.createcheme.science.fluid.diagnostics.SolverDiagnostics.ENABLED=false;\n"
 ."                    try{var fpC=SparseNewton.solve(fpL.wallClosure(inventory.moles(),inventory.internalEnergy(),inventory.volume(),workFrom),failure.lastVariables(),new SparseNewton.Settings(24,CLOSURE,1e-6,24),()->{});\n"
 ."                        $P.add(\"D +24 converged vap \"+$P.q($P.vapourShare(fpL.decode(fpC.variables(),0))));}\n"
 ."                    catch(SparseNewton.Nonconvergence fpF){$P.add(\"D +24 stalled vap \"+(fpF.lastVariables()==null?\"none\":$P.q($P.vapourShare(fpL.decode(fpF.lastVariables(),0))))+\" \"+$P.residual(fpF));}\n"
 ."                    catch(IllegalArgumentException fpX){$P.add(\"D +24 refused\");}\n"
 ."                    finally{com.wormzjl.createcheme.science.fluid.diagnostics.SolverDiagnostics.ENABLED=fpE;}}\n"
 ."                return fpR;}"],
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
