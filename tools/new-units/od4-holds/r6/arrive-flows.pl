#!/usr/bin/perl -i
# r6 probe (654): in arrivingPhases, per junction and round 0 of a solve with dt<1e-3: its held mass, each start inflow
# (edge, donor, kg/s transported, the edge's cap at the donor end, the law flow) and m/Q. Export-only.
while(<>){
  print;
  if(/^(\s*)heat\+=delivered\*endSpecificEnthalpy\(stream,port\);\s*$/){
    my $i=$1;
    print $i.'if(R2T&&dt<1e-3&&round==0)System.out.println("R2 AFLOW j="+j+" e="+edge+" from="+donor+" q="+transported(graph,pipe,flow)+" law="+flow+" cap="+massFlowLimit(pipe,stream,port)+" heldKg="+heldMass+" dt="+dt);'."\n";
  }
}
