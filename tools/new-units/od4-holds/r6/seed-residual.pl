#!/usr/bin/perl -i
# r6 probe (994): on an r2-traced + probe.pl export, before each pass's Newton at dt < 1e-3: the residual at the pass's
# seed point (top rows, labelled as R2 GATE labels them) and, per junction, the seed against its stock (the buffer the
# rows mix). Export-only, never a tracked source.
while(<>){
  if(/^(\s*)try\{numerical=SparseNewton\.solve\(equations,equations\.initial\(\),/){
    my $i=$1;
    print $i.'if(R2T&&dt<1e-3){try{double[] r2x=equations.initial();double[] r2f=equations.residual(r2x);Integer[] r2i=new Integer[r2f.length];for(int q=0;q<r2i.length;q++)r2i[q]=q;Arrays.sort(r2i,(u,v)->Double.compare(Math.abs(r2f[v]),Math.abs(r2f[u])));StringBuilder r2b=new StringBuilder("R2 SEEDRES pass="+pass+" dt="+dt);for(int q=0;q<Math.min(8,r2i.length);q++){int row=r2i[q];String where;if(row>=equations.edgeOffset)where="edge"+(row-equations.edgeOffset);else{int nd=0;while(nd+1<equations.offsets.length&&equations.offsets[nd+1]<=row)nd++;where="node"+nd+"+"+(row-equations.offsets[nd]);}r2b.append(" ").append(where).append("=").append(r2f[row]);}System.out.println(r2b);}catch(RuntimeException r2e){System.out.println("R2 SEEDRES failed "+r2e);}}'."\n";
    print $i.'if(R2T&&dt<1e-3)for(int r2j=0;r2j<seeds.size();r2j++){var r2n=graph.reservoirs().get(r2j);if(!r2n.junction()||r2n.fixed())continue;var r2s=seeds.get(r2j);var r2u=r2n.inventory();double r2m=molarMass(r2u.moles());double r2h=(r2u.internalEnergy()-r2u.solids().moments().enthalpy(r2n.state().temperature(),r2n.state().pressure()))/r2m;System.out.println("R2 JSEED j="+r2j+" T="+r2s.temperature()+" P="+r2s.pressure()+" code="+phaseCode(r2s)+" liqMol="+Arrays.stream(r2s.liquid()).sum()+" vapMol="+Arrays.stream(r2s.vapor()).sum()+" seedKg="+r2s.mass()+" stockKg="+r2m+" seedH="+r2s.enthalpy()/r2s.mass()+" stockH="+r2h+" stateT="+r2n.state().temperature()+" stateCode="+phaseCode(r2n.state())+" solidKg="+r2s.solids().massKg());}'."\n";
  }
  print;
}
