#!/usr/bin/perl -i
# r6 probe (654): at each step solve's entry, every junction whose state and buffer differ by more than 1 kJ/kg in
# specific enthalpy (fluid only): id, state T, P, phase code, h, buffer h and mass. Export-only, never a tracked source.
while(<>){
  print;
  if(/^(\s*)inputGraph=PassiveNetwork\.sizeJunctionHoldups\(inputGraph,model\);\s*$/){
    my $i=$1;
    print $i.'if(R2T)for(var r2n:inputGraph.reservoirs()){if(!r2n.junction()||r2n.fixed())continue;var r2u=r2n.inventory();double r2m=0;for(int c=0;c<r2u.moles().length;c++)r2m+=r2u.moles()[c]*model.molecularWeight(c);if(!(r2m>0))continue;var r2s=r2n.state();double r2hb=(r2u.internalEnergy()-r2u.solids().moments().enthalpy(r2s.temperature(),r2s.pressure()))/r2m,r2hs=(r2s.enthalpy()-r2s.solids().moments().enthalpy(r2s.temperature(),r2s.pressure()))/r2s.mass();if(Math.abs(r2hb-r2hs)>1000)System.out.println("R2 ENTRYGAP id="+r2n.id()+" T="+r2s.temperature()+" P="+r2s.pressure()+" code="+phaseCode(r2s)+" hState="+r2hs+" hBuffer="+r2hb+" mBuffer="+r2m+" nodes="+inputGraph.reservoirs().size());}'."\n";
  }
}
