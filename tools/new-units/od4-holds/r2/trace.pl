#!/usr/bin/perl -i
# r2 probe: inserts -Dr2.trace=true prints into an exported PassiveStepSolver.java (never a tracked source).
# R2_ARRIVE: the path of arrive.java.txt (the arrivingPhases snippet), set by prep.sh.
open(my $fh,"<",$ENV{R2_ARRIVE}) or die "R2_ARRIVE"; local $/; my $arrive=<$fh>; close $fh; $/="\n";
(my $fp=$ENV{R2_ARRIVE})=~s/arrive\.java\.txt$/fail.java.txt/; open(my $gh,"<",$fp) or die "fail"; my $fail=do{local $/;<$gh>}; close $gh;
while(<>){
  if(/^\s*if\(\(phaseCode\(mixture\)&~phaseCode\(seed\)&15\)==0\)continue;/){ print $arrive; }
  if(/^\s*if\(changedSeeds==null\)throw new SparseNewton.Nonconvergence\(failure.getMessage\(\)\+"; active-set pass="/){
    print '                if(R2T)System.out.println("R2 FAIL pass="+pass+" "+failure.getMessage()+" dt="+dt+" modes="+modes+" phases="+Arrays.toString(phases));'."\n";
    print $fail;
  }
  if(/^\s*var equations=new Equations\(graph,dt,modes,boundaryClosed,seeds,reachable,supports,pipeIdentities\);/){
    print $_; $_="";
    print '            if(R2T)System.out.println("R2 PASS pass="+pass+" dt="+dt+" rate="+rateOnly+" modes="+modes+" phases="+Arrays.toString(phases)+" seedT="+seeds.stream().map(s->String.format("%.2f",s.temperature())).toList()+" seedP="+seeds.stream().map(s->String.format("%.0f",s.pressure())).toList());'."\n";
  }
  if(/^\s*public Result solve\(PassiveNetwork graph,double dt,Runnable checkpoint\)\{/){
    print '    static final boolean R2T=Boolean.getBoolean("r2.trace");'."\n";
  }
  if(/^\s*double vapourPressure\(Equations equations,int edge,FluidThermodynamics.State suction\) \{/){
    print $_; $_="";
    print '        if(R2T){var r2p=equations.graph.pipes().get(edge);var r2s=equations.seeds.get(r2p.first());if(r2p.control() instanceof FlowControl.Pump&&!FluidThermodynamics.holdsPhase(r2s,FluidThermodynamics.WATER)&&r2s.waterLiquid()+r2s.waterVapor()>0&&!(edge<suctionFloors.length&&suctionFloors[edge]<Double.POSITIVE_INFINITY))System.out.println("R2 VAPOUR_INF edge="+edge+" seedT="+r2s.temperature()+" seedP="+r2s.pressure()+" wl="+r2s.waterLiquid()+" wv="+r2s.waterVapor()+" trialT="+suction.temperature()+" mode="+equations.modes.get(edge));}'."\n";
  }
  print;
}
