#!/usr/bin/perl -i
# r9 probe: each pass's junction seeds, their liquid and vapour water and their temperature and pressure (R9 JSEED, under
# -Dr2.trace=true, after the r2 trace's "R2 PASS" line). Export-only, PassiveStepSolver.java, applied after the r2 trace.
while(<>){
  print $_;
  if(/^(\s*)if\(R2T\)System\.out\.println\("R2 PASS pass="/){
    my $i=$1;
    print $i.'if(R2T)for(int r9=0;r9<seeds.size();r9++){var r9s=seeds.get(r9);if(graph.reservoirs().get(r9).junction())System.out.println("R9 JSEED pass="+pass+" dt="+dt+" node="+r9+" wl="+r9s.waterLiquid()+" wv="+r9s.waterVapor()+" T="+r9s.temperature()+" P="+r9s.pressure());}'."\n";
  }
}
