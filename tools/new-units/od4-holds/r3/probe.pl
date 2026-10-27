#!/usr/bin/perl
# r3 probe: inserts -Dr3.trace=true prints into an exported tree (never a tracked source). Usage: perl probe.pl <export root>
# SparseNewton: per-iteration history (norm, alpha, backtracks, fresh) printed when a solve hits its iteration limit or stalls.
# PhaseLayout: describe(): per component of a two-phase vessel layout its support, l, v, split unknown, implied split, row.
# PassiveStepSolver: after R2 FAILROWS, describe() of the node holding the worst row (needs r2/trace.pl applied first).
use strict; my $root=shift or die "root"; our $linear=do{ (my $p=$0)=~s/probe\.pl$/linear.java.txt/; open(my $h,"<",$p) or die "linear"; local $/; <$h> };
sub edit { my($f,$code)=@_; local @ARGV=("$root/$f"); local $^I=""; while(<>){ s/\x0d\x0a$/\x0a/; $code->(); print; } }
edit("src/main/java/com/wormzjl/createcheme/science/fluid/solver/SparseNewton.java", sub {
  if(/^\s*ThermoHold hold=null;$/){ $_.="        final boolean R3=Boolean.getBoolean(\"r3.trace\");StringBuilder r3h=R3?new StringBuilder():null;\n"; }
  if(/^\s*x=candidate;f=next;norm=nextNorm;accepted=true;break;$/){ $_="                        if(R3)r3h.append(String.format(\" [%d %.3e a=%.3g bt=%d %s]\",iteration,nextNorm,alpha,backtrack,fresh?\"F\":\"c\"));\n".$_; }
  if(/^\s*throw new Nonconvergence\("Newton iteration limit at residual "\+norm,x,hold\);$/){ $_="        if(R3)System.out.println(\"R3 NEWTON limit n=\"+n+r3h);\n".$main::linear.$_; }
  if(/^\s*workspace.invalidate\(\);throw new Nonconvergence\("Newton line search stalled at residual "\+norm,x,hold\);$/){ $_="                if(R3)System.out.println(\"R3 NEWTON stalled n=\"+n+r3h);\n".$_; }
});
edit("src/main/java/com/wormzjl/createcheme/science/fluid/solver/PhaseLayout.java", sub {
  if(/^\s*public int size\(\)\{return size;\}$/){ $_.=<<'J'; }
    public String r3describe(FluidThermodynamics.State state,double[] variables,int offset,double[] f) {
        var b=new StringBuilder("R3 LAYOUT size="+size+" comps="+components.length+" liq="+liquidActive+" vap="+vaporActive+" wl="+waterLiquidIndex+" wv="+waterVaporIndex+" T="+state.temperature()+" P="+state.pressure()+" Pc="+state.hydrocarbonPartialPressure()+" lv="+state.liquidVolume()+" amountScale="+amountScale);
        double[] l=state.liquidView(),v=state.vaporView();double nl=sum(l),nv=sum(v);b.append(" nl="+nl+" nv="+nv);
        int row=offset+componentBalanceCount()+2+(solidIndex>=0?3:0);
        double[] fl=state.liquidProperties()==null?null:state.liquidProperties().logFugacityView(),fv=state.vaporProperties()==null?null:state.vaporProperties().logFugacityView();
        for(int i:components){b.append("\n   c"+i+" "+model.hydrocarbon.components().get(i)+" "+support[i]+" l="+l[i]+" v="+v[i]+" x/scale="+variables[offset+amountIndex[i]]);
            if(support[i]==PhaseSupport.BOTH){b.append(" split="+variables[offset+vaporIndex[i]]+" lnK="+(fl==null?"na":String.valueOf(fl[i]-fv[i]))+" row"+(row-offset)+"="+f[row]);row++;}}
        return b.toString();
    }
J
});
edit("src/main/java/com/wormzjl/createcheme/science/fluid/network/PassiveStepSolver.java", sub {
  if(/StringBuilder r2b=new StringBuilder\("R2 FAILROWS"\);/){ s/StringBuilder r2b=/int r2w=r2i[0];StringBuilder r2b=/; }
  if(/System.out.println\(r2b\);\}catch\(RuntimeException r2e\)/){ s/System.out.println\(r2b\);/System.out.println(r2b);if(r2w<equations.edgeOffset){int nd=0;while(nd+1<equations.offsets.length&&equations.offsets[nd+1]<=r2w)nd++;if(equations.layout[nd]!=null)System.out.println(equations.layout[nd].r3describe(r2st.get(nd),failure.lastVariables(),equations.offsets[nd],r2f));}/; }
});
edit("src/main/java/com/wormzjl/createcheme/science/fluid/solver/PhaseLayout.java", sub {
  if(/^    public int size\(\)\{return size;\}$/){ $_.="    public int[] r3splits(){int k=0;for(int i:components)if(support[i]==PhaseSupport.BOTH)k++;int[] r=new int[k];k=0;for(int i:components)if(support[i]==PhaseSupport.BOTH)r[k++]=vaporIndex[i];return r;}\n    public int[] r3singles(){int k=0;for(int i:components)if(support[i]!=PhaseSupport.BOTH)k++;int[] r=new int[k];k=0;for(int i:components)if(support[i]!=PhaseSupport.BOTH)r[k++]=amountIndex[i];return r;}\n"; }
});
edit("src/main/java/com/wormzjl/createcheme/science/fluid/network/PassiveStepSolver.java", sub {
  if(/System.out.println\(equations.layout\[nd\].r3describe/){ s/if\(equations.layout\[nd\]!=null\)System.out.println\(equations.layout\[nd\].r3describe\(r2st.get\(nd\),failure.lastVariables\(\),equations.offsets\[nd\],r2f\)\);/if(equations.layout[nd]!=null){System.out.println(equations.layout[nd].r3describe(r2st.get(nd),failure.lastVariables(),equations.offsets[nd],r2f));double[] r3x=failure.lastVariables();{double[] r3z=equations.residual(r3x.clone());double r3d=0;int r3w2=-1;for(int q=0;q<r3z.length;q++)if(Math.abs(r3z[q]-r2f[q])>r3d){r3d=Math.abs(r3z[q]-r2f[q]);r3w2=q;}System.out.println("R3 REEVAL maxdiff="+r3d+" at row "+r3w2);}for(int r3m=0;r3m<2;r3m++){double[] r3y=r3x.clone();double r3hh=1e-6;int[] r3c=r3m==0?equations.layout[nd].r3splits():equations.layout[nd].r3singles();for(int c:r3c)r3y[equations.offsets[nd]+c]+=r3m==0?r3hh:r3hh*r3x[equations.offsets[nd]+c];double[] r3g=equations.residual(r3y);Integer[] r3o=new Integer[r3g.length];for(int q=0;q<r3o.length;q++)r3o[q]=q;final double[] gg=r3g;Arrays.sort(r3o,(u,v)->Double.compare(Math.abs(gg[v]-r2f[v]),Math.abs(gg[u]-r2f[u])));StringBuilder r3b=new StringBuilder(r3m==0?"R3 JE uniform split shift:":"R3 JE uniform single-phase scaling:");for(int q=0;q<10;q++)r3b.append(String.format(" r%d=%.4g",r3o[q],(r3g[r3o[q]]-r2f[r3o[q]])\/r3hh));System.out.println(r3b);}}/; }
});
