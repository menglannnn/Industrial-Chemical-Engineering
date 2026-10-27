#!/usr/bin/perl -i
# t285 probe b: PH consistency prints in phaseCheck (-Dt285.ph=true); export only.
while(<>){
  if(/^\s*if\(refused!=0\)SolverDiagnostics.count\(SolverDiagnostics.phaseRefusals\);/){
    print;
    print <<'J';
                if(Boolean.getBoolean("t285.ph")&&junction){
                    double hh=state.enthalpy()-state.solidMoments().enthalpy(state.temperature(),state.pressure());
                    var sp=hydrocarbon.spine();double tg=hh;
                    if(sp.isPresent()){var fr=sp.get().formationReference();tg=EnergyReference.sensible(fr.components()).rebase(hh,Arrays.copyOf(totals,hydrocarbon.componentCount()),fr);}
                    var rc=phaseEngine().ph(kept.pressure(),tg,totals,true);var rf=phaseEngine().ph(kept.pressure(),tg,totals,false);
                    var tk=phaseEngine().tp(kept.temperature(),kept.pressure(),totals,true);
                    StringBuilder b=new StringBuilder("T285 PH h="+hh+" target="+tg+" spine="+sp.isPresent()+" compete="+crystalsCompete(kept.temperature(),totals));
                    for(var r:new EquilibriumResult[]{rc,rf,tk}){b.append(" | ").append(r);for(var ph:r.phases())b.append(" {").append(ph.kind()).append(" T=").append(ph.temperature()).append(" n=").append(ph.total()).append("}");}
                    State ownF=null;try{ownF=equilibriumPH(kept.temperature(),kept.pressure(),hh,totals,false,checkpoint);}catch(RuntimeException e){}
                    b.append(" | ownFluid T=").append(ownF==null?Double.NaN:ownF.temperature()).append(" H=").append(ownF==null?Double.NaN:ownF.enthalpy()).append(" am=").append(ownF==null?"-":Arrays.toString(phaseAmounts(ownF)));
                    if(own!=null){State back=null;try{back=equilibriumTP(own.temperature(),own.pressure(),totals,checkpoint,null);}catch(RuntimeException e){}
                        b.append(" | TPatOwnT H=").append(back==null?Double.NaN:back.enthalpy()).append(" am=").append(back==null?"-":Arrays.toString(phaseAmounts(back)));}
                    b.append(" | totals=").append(Arrays.toString(totals));
                    System.out.println(b);
                }
J
    next;
  }
  print;
}
