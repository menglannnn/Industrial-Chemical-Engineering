#!/usr/bin/perl -i
# r5 probe (729): on an r2-traced PassiveStepSolver export: topology print, gate-rejection rows, variable-free failures,
# solid arrivals during the gate's residual. Export-only, never a tracked source.
while(<>){
  if(/^(\s*)static final boolean R2T=Boolean\.getBoolean\("r2\.trace"\);\s*$/){chomp;$_.=" static long R2TOPO=-1; static boolean R2DBG=false;\n";}
  if(/^(\s*)if\(R2T\)System\.out\.println\("R2 PASS pass="/){
    print '            {long r2h=graph.pipes().size();for(var r2x:graph.reservoirs())r2h=r2h*31+r2x.id()+r2x.kind().ordinal();for(var r2y:graph.pipes())r2h=r2h*31+r2y.first()*7+r2y.second();if(R2T&&r2h!=R2TOPO){R2TOPO=r2h;StringBuilder r2b=new StringBuilder("R2 TOPO");for(int r2i=0;r2i<graph.reservoirs().size();r2i++){var r2r=graph.reservoirs().get(r2i);r2b.append(" n"+r2i+"="+r2r.kind()+"#"+r2r.id());}for(int r2e=0;r2e<graph.pipes().size();r2e++){var r2p=graph.pipes().get(r2e);r2b.append(" e"+r2e+"="+r2p.first()+">"+r2p.second()+":"+r2p.control().getClass().getSimpleName()+(r2p.filter()!=null?"+F":"")+":"+r2p.sections().size());}System.out.println(r2b);}}'."\n";
  }
  s/^(\s*)if\(failure\.lastVariables\(\)==null\)throw failure;/$1if(failure.lastVariables()==null){if(R2T)System.out.println("R2 NOVARS pass="+pass+" dt="+dt+" "+failure.getMessage());throw failure;}/;
  if(/^(\s*)if\(maximumResidual>gate\)\{SolverDiagnostics\.count\(SolverDiagnostics\.equationGateRejections\);/){
    print '            if(R2T&&maximumResidual>gate){R2DBG=true;double[] r2f=equations.residual(reconstructed);R2DBG=false;Integer[] r2i=new Integer[r2f.length];for(int q=0;q<r2i.length;q++)r2i[q]=q;Arrays.sort(r2i,(u,v)->Double.compare(Math.abs(r2f[v]),Math.abs(r2f[u])));StringBuilder r2b=new StringBuilder("R2 GATE pass="+pass+" dt="+dt+" max="+maximumResidual);for(int q=0;q<Math.min(5,r2i.length);q++){int row=r2i[q];String where;if(row>=equations.edgeOffset)where="edge"+(row-equations.edgeOffset);else{int nd=0;while(nd+1<equations.offsets.length&&equations.offsets[nd+1]<=row)nd++;where="node"+nd+"+"+(row-equations.offsets[nd]);}r2b.append(" ").append(where).append("=").append(r2f[row]);}var r2ps=projection.states();for(int q=0;q<r2ps.size();q++){var s=r2ps.get(q);r2b.append(" | n"+q+" T="+String.format("%.3f",s.temperature())+" P="+String.format("%.0f",s.pressure())+" code="+phaseCode(s)+" solidKg="+s.solids().massKg());}r2b.append(" | flows="+Arrays.toString(flows));System.out.println(r2b);}'."\n";
  }
  if(/^\s*var sourceMoments=carried\.solids\.values\(\);\s*$/){
    $_.='                    if(R2DBG&&carried.solids.mass()>0)System.out.println("R2 SOLIDIN node="+node+" donor="+mixed+" edge="+edge+" flow="+flow+" carriedSolidPerKg="+carried.solids.mass()/carried.mass+" meltingAny="+(melting!=null&&melting.any(node))+" sourceListKg="+source.solids().massKg()+" sourceT="+source.temperature()+" filter="+(pipe.filter()!=null));'."\n";
  }
  print;
}
