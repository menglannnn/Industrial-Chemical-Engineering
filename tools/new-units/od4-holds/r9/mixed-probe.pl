#!/usr/bin/perl -i
# r9 probe: every water-only call of BalanceNet.mixed, its inputs and its answer (R9 MIXED, under -Dr2.trace=true). Export-only.
while(<>){
  if(/^(\s*)private FluidThermodynamics\.State mixed\(double\[\] amounts,double mass,double heat,double tMix,double tLow,double tHigh,double pressure,Runnable checkpoint\) \{\s*$/){
    my $i=$1;
    print $_;
    print $i.'    var r9out=mixed0(amounts,mass,heat,tMix,tLow,tHigh,pressure,checkpoint);if(R2T){double r9h=0;for(int r9c=0;r9c<amounts.length-1;r9c++)r9h+=amounts[r9c];if(r9h==0)System.out.println("R9 MIXED p="+pressure+" tMix="+tMix+" tLow="+tLow+" tHigh="+tHigh+" hTarget="+heat/mass+" -> T="+r9out.temperature()+" wl="+r9out.waterLiquid()+" wv="+r9out.waterVapor()+" h="+r9out.enthalpy()/r9out.mass());}return r9out;}'."\n";
    print $i.'private FluidThermodynamics.State mixed0(double[] amounts,double mass,double heat,double tMix,double tLow,double tHigh,double pressure,Runnable checkpoint) {'."\n";
    next;
  }
  print;
}
