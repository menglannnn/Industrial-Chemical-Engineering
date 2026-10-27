#!/usr/bin/perl -i
# r6 probe (157): in arrivingPhases, per junction whose engine answer is formed: seed, engine and own-split phase codes,
# the mixture's mass-weighted temperature and water, and what arrives. Export-only, never a tracked source.
while(<>){
  if(/^(\s*)var own=model\.junctionSplit\(n,enthalpy,molarMass\(n\),seed\.pressure\(\),engine,checkpoint\);\s*$/){
    my $i=$1; print;
    print $i.'if(R2T)System.out.println("R2 ACODE j="+j+" dt="+dt+" seed="+phaseCode(seed)+" engine="+phaseCode(model.folded(engine,true))+" own="+(own==null?-1:phaseCode(model.folded(own,true)))+" Tw="+(weighted/mass)+" P="+seed.pressure()+" heldKg="+heldMass+" massKg="+mass+" water="+n[n.length-1]+" ownWl="+(own==null?-1:own.waterLiquid())+" ownWv="+(own==null?-1:own.waterVapor())+" seedWl="+seed.waterLiquid()+" seedWv="+seed.waterVapor()+" seedT="+seed.temperature()+" ownT="+(own==null?-1:own.temperature()));'."\n";
    next;
  }
  print;
}
