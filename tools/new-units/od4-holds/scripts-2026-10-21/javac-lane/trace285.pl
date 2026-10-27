#!/usr/bin/perl -i
# t285 probe: phaseCheck prints (-Dt285.trace=true) in an exported FluidThermodynamics.java; never a tracked source.
while(<>){
  if(/^\s*if\(refused!=0\)SolverDiagnostics.count\(SolverDiagnostics.phaseRefusals\);/){
    print;
    print '                if(Boolean.getBoolean("t285.trace"))System.out.println("T285 CHECK junction="+junction+" T="+kept.temperature()+" P="+kept.pressure()+" keptBits="+phaseBits(kept)+" eqBits="+phaseBits(equilibrium)+" proposed="+proposed+" excluded="+excluded+" refused="+refused+" checked="+(checked!=kept)+" ownNull="+(own==null)+" ownBits="+(own==null?-1:phaseBits(own))+" ownNothing="+(own==null?-1:nothing(own))+" keptAm="+java.util.Arrays.toString(phaseAmounts(kept))+" keptVol="+java.util.Arrays.toString(phaseVolumes(kept))+" eqAm="+java.util.Arrays.toString(phaseAmounts(equilibrium))+" eqVol="+java.util.Arrays.toString(phaseVolumes(equilibrium))+" ownT="+(own==null?Double.NaN:own.temperature())+" ownAm="+(own==null?"-":java.util.Arrays.toString(phaseAmounts(own)))+" ownVol="+(own==null?"-":java.util.Arrays.toString(phaseVolumes(own)))+" eqT="+equilibrium.temperature()+" hState="+state.enthalpy()+" hKept="+kept.enthalpy()+" hEq="+equilibrium.enthalpy()+" hOwn="+(own==null?Double.NaN:own.enthalpy())+" fromBits="+(from==null?-1:phaseBits(from))+" shrinking="+shrinking);'."\n";
    next;
  }
  print;
}
