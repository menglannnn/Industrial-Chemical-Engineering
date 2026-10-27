#!/usr/bin/perl
# r3 probe, pass sequence: per converged pass the vessels' end, seed and start liquid volume; folded, proposed and refuted
# passes; a failed pass's iterate. -Dr3.trace=true. Usage: perl passes.pl <export root> (after r2/trace.pl).
use strict; my $root=shift or die "root"; my $f="$root/src/main/java/com/wormzjl/createcheme/science/fluid/network/PassiveStepSolver.java";
local @ARGV=($f); local $^I=""; my $done=0;
while(<>){ s/\x0d\x0a$/\x0a/;
  if(/^\s*static final boolean R2T=Boolean.getBoolean\("r2.trace"\);$/){ $_.='    static final boolean R3T=Boolean.getBoolean("r3.trace");'."\n".'    static String r3v(PassiveNetwork g,List<FluidThermodynamics.State> a,List<FluidThermodynamics.State> b){StringBuilder s=new StringBuilder("[");for(int q=0;q<a.size();q++)if(g.reservoirs().get(q).kind()==PassiveNetwork.NodeKind.RESERVOIR)s.append(q).append(":").append(String.format("%.4e/%.4e/%.4e ",a.get(q).liquidVolume(),b==null?Double.NaN:b.get(q).liquidVolume(),g.reservoirs().get(q).state().liquidVolume()));return s.append("]").toString();}'."\n"; }
  if(/^\s*proposal=null;$/ && !$done){ $done=1; $_.='            if(R3T){var r3s=equations.states(numerical.variables());System.out.println("R3 CONV pass="+pass+" dt="+dt+" it="+numerical.iterations()+" lv end/seed/start="+r3v(graph,r3s,seeds)+" flows="+Arrays.toString(Arrays.copyOfRange(numerical.variables(),equations.edgeOffset,equations.edgeOffset+graph.pipes().size())));}'."\n"; }
  if(/^\s*if\(recordFolds\(graph,states,projection.states\(\),foldedPhases\)\)/){ $_='            if(R3T&&recordFolds(graph,states,projection.states(),foldedPhases.clone()))System.out.println("R3 FOLD pass="+pass+" lv reconstructed/solved="+r3v(graph,projection.states(),states));'."\n".$_; }
  if(/^\s*if\(changedSeeds!=null\)\{proposal=Proposal.of\(projection.states\(\),changedSeeds\);/){ $_='            if(R3T&&changedSeeds!=null)System.out.println("R3 PROPOSE pass="+pass+" lv proposed/reconstructed="+r3v(graph,changedSeeds,projection.states()));'."\n".$_; }
  if(/^\s*if\(refuted!=0\)\{foldedPhases\[proposal.node\(\)\]\|=refuted;/){ $_='                    if(R3T)System.out.println("R3 REFUTE? pass="+pass+" node="+proposal.node()+" refuted="+refuted);'."\n".$_; }
  if(/^\s*var changedSeeds=foldedIterate\(graph,iterate\);$/){ $_.='                if(R3T)System.out.println("R3 FAILED-ITERATE pass="+pass+" foldedChange="+(changedSeeds!=null)+" lv iterate/seed="+r3v(graph,iterate,seeds));'."\n"; }
  print; }
