package com.wormzjl.icheme.science.fluid.network;

import com.wormzjl.icheme.science.fluid.solver.PhaseLayout;
import com.wormzjl.icheme.science.fluid.solver.SparseNewton;
import com.wormzjl.icheme.science.fluid.thermo.FluidThermodynamics;
import java.lang.reflect.Field;
import java.util.*;

/** Seed-236 probe: labels the Newton unknowns and rows of a failed pass and prints the failing point. Scratch only. */
public final class Probe236 implements PassiveStepSolver.FailureProbe {
    public FluidThermodynamics model;final List<String> names;int fails,oks,detailed;final int detailLimit;public boolean active;
    public Probe236(List<String> names,int detailLimit){this.names=names;this.detailLimit=detailLimit;}
    public static Probe236 install(List<String> names,int detailLimit){var p=new Probe236(names,detailLimit);PassiveStepSolver.failureProbe=p;return p;}
    public static void uninstall(){PassiveStepSolver.failureProbe=null;}
    static Object get(Object o,String name){try{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
    String comp(int c){return c<names.size()?names.get(c):c==names.size()?"water":"c"+c;}
    public void converged(PassiveStepSolver.Equations e,double[] x,int pass){
        if(!active)return;oks++;
        {var sz=new StringBuilder();for(int i=0;i<e.layout.length;i++)sz.append(e.layout[i]==null?"-":String.valueOf(e.layout[i].size())).append(" ");System.out.println("  OK pass="+pass+" dt="+e.dt+" flows="+flows(e,x)+" layoutSizes="+sz);}
    }
    int stacks;
    public void rejected(double step,Throwable failure){
        if(!active)return;
        System.out.println("  REJECT step="+step+" "+failure);
        if(String.valueOf(failure.getMessage()).contains("line search")&&stacks++<2){var trace=failure.getStackTrace();for(int i=0;i<Math.min(14,trace.length);i++)System.out.println("      at "+trace[i]);}
    }
    int gates;
    public void gateFailed(PassiveStepSolver.Equations e,double[] x,List<FluidThermodynamics.State> states){
        if(!active||gates++>=3)return;
        String[] vl=new String[e.size],rl=new String[e.size];label(e,vl,rl);
        double[] r=e.residual(x);Integer[] order=new Integer[r.length];for(int i=0;i<r.length;i++)order[i]=i;
        Arrays.sort(order,(a,b)->Double.compare(Math.abs(r[b]),Math.abs(r[a])));
        var b=new StringBuilder("  GATE FAIL dt="+e.dt+" top rows:");
        for(int k=0;k<Math.min(8,r.length);k++)b.append(String.format(Locale.ROOT," %s=%.4g",rl[order[k]],r[order[k]]));
        System.out.println(b);System.out.println("    flows="+flows(e,x)+" seedsPhases="+phases(e));
        for(int i=0;i<states.size();i++){var s=states.get(i);if(e.layout[i]==null)continue;double[] l=s.liquidView(),v=s.vaporView();double nh=0;for(int c=0;c<l.length;c++)nh+=l[c]+v[c];
            System.out.println(String.format(Locale.ROOT,"    reconstructed node%d %s junction=%b layoutSize=%d P=%.6f T=%.6f Vliq=%.4g Vvap=%.4g wL=%.10g HCmol=%.4g V=%.6g omitted(v)=%b omitted(j)=%b",
                i,e.graph.reservoirs().get(i).kind(),e.graph.reservoirs().get(i).junction(),e.layout[i].size(),s.pressure(),s.temperature(),s.liquidVolume(),s.vaporVolume(),s.waterLiquid(),nh,s.volume(),
                FluidThermodynamics.omittedTrace(s,false),FluidThermodynamics.omittedTrace(s,true)));
            var seed=e.seeds.get(i);double sh=0;for(int c=0;c<l.length;c++)sh+=seed.liquidView()[c]+seed.vaporView()[c];
            System.out.println(String.format(Locale.ROOT,"      seed P=%.6f Vliq=%.4g Vvap=%.4g HCmol=%.4g",seed.pressure(),seed.liquidVolume(),seed.vaporVolume(),sh));}
    }
    int closures;
    public void closureFailed(PhaseLayout layout,FluidThermodynamics.State state,double[] n,double energy,double volume,double workFrom,SparseNewton.Nonconvergence f){
        if(!active||closures++>=3)return;
        System.out.println("  CLOSURE FAIL "+f.getMessage()+" energy="+energy+" volume="+volume+" workFrom="+workFrom);
        int[] comps=(int[])get(layout,"components"),li=(int[])get(layout,"liquidIndex"),vi=(int[])get(layout,"vaporIndex");
        int wl=(int)get(layout,"waterLiquidIndex"),wv=(int)get(layout,"waterVaporIndex");double[] scales=(double[])get(layout,"componentScales"),diff=(double[])get(layout,"differenceScales");
        String[] rl=new String[layout.size()],vl=new String[layout.size()];Arrays.fill(rl,"?");Arrays.fill(vl,"?");int row=0;
        for(int c:comps)rl[row++]="bal["+comp(c)+"]";if(wl>=0||wv>=0)rl[row++]="bal[water]";rl[row++]="U";rl[row++]="wall";for(int k=0;row<rl.length;k++)rl[row++]="eq"+k;
        for(int c:comps){if(li[c]>=0)vl[li[c]]="L["+comp(c)+"]";if(vi[c]>=0)vl[vi[c]]=(li[c]>=0?"split[":"V[")+comp(c)+"]";}
        System.out.println("    support="+Arrays.toString(Arrays.stream(comps).mapToObj(c->comp(c)+":"+layout.support(c)).toArray()));
        double[] l=state.liquidView(),v=state.vaporView();var b=new StringBuilder("    start state vs inventory (mol, scale):");
        for(int c=0;c<l.length;c++)if(l[c]+v[c]!=0||n[c]!=0)b.append(String.format(Locale.ROOT," %s=%.4g+%.4g/%.4g(%.3g)",comp(c),l[c],v[c],n[c],scales[c]));
        b.append(String.format(Locale.ROOT," water=%.10g/%.10g Vliq=%.4g Vvap=%.4g P=%.3f T=%.6f",state.waterLiquid()+state.waterVapor(),n[l.length],state.liquidVolume(),state.vaporVolume(),state.pressure(),state.temperature()));
        System.out.println(b);
        for(var point:new double[][]{layout.encode(state),f.lastVariables()}){
            if(point==null)continue;
            try{var s=layout.decode(point,0);double[] r=new double[layout.size()];layout.residual(s,n,energy,volume,workFrom,r,0,point);
                var bb=new StringBuilder(point==f.lastVariables()?"    at failure:":"    at start:");
                for(int i=0;i<r.length;i++)if(Math.abs(r[i])>1e-12)bb.append(String.format(Locale.ROOT," %s=%.4g",rl[i],r[i]));
                double[] sl=s.liquidView(),sv=s.vaporView();bb.append("\n      amounts l/v:");for(int c:comps)bb.append(String.format(Locale.ROOT," %s=%.4g/%.4g",comp(c),sl[c],sv[c]));
                bb.append(String.format(Locale.ROOT,"\n      P=%.6f T=%.8f Vliq=%.4g Vvap=%.4g",s.pressure(),s.temperature(),s.liquidVolume(),s.vaporVolume()));
                System.out.println(bb);
            }catch(RuntimeException failure){System.out.println("    point failed: "+failure.getMessage());}
        }
        if(model!=null)try{double[] hc=n.clone();hc[hc.length-1]=0;
            var flash=model.flashTP(state.temperature(),state.pressure(),hc,()->{});double nl=0,nv=0;for(double x:flash.liquidView())nl+=x;for(double x:flash.vaporView())nv+=x;
            double[] scaled=hc.clone();double total=0;for(double x:hc)total+=x;for(int i=0;i<scaled.length;i++)scaled[i]=scaled[i]/total*100;
            var big=model.flashTP(state.temperature(),state.pressure(),scaled,()->{});double bl=0,bv=0;for(double x:big.liquidView())bl+=x;for(double x:big.vaporView())bv+=x;
            System.out.println(String.format(Locale.ROOT,"    TP flash of the inventory's HC alone at T,P: liquid=%.4g vapour=%.4g mol (vapour fraction %.4g); scaled to 100 mol: vapour fraction %.6g",nl,nv,nv/(nl+nv),bv/(bl+bv)));
            for(double p:new double[]{3.74e5,3.0e5,2.5e5,2.2e5,2.0e5,1.5e5}){var s=model.flashTP(state.temperature(),p,scaled,()->{});double a=0,c=0;for(double x:s.liquidView())a+=x;for(double x:s.vaporView())c+=x;System.out.println(String.format(Locale.ROOT,"      P=%.0f vapour fraction %.6g",p,c/(a+c)));}
        }catch(RuntimeException failure){System.out.println("    flash failed: "+failure);}
        var bd=new StringBuilder("    diffScales:");for(int i=0;i<diff.length;i++)bd.append(String.format(Locale.ROOT," %s=%.3g",vl[i],diff[i]));System.out.println(bd);
    }
    public void failed(PassiveStepSolver.Equations e,SparseNewton.Nonconvergence f,int pass,double tolerance){
        if(!active)return;fails++;
        System.out.println("  FAIL#"+fails+" pass="+pass+" dt="+e.dt+" tol="+tolerance+" msg="+f.getMessage());
        double[] x=f.lastVariables();if(x==null)return;
        String[] vl=new String[e.size],rl=new String[e.size];label(e,vl,rl);
        System.out.println("    flows="+flows(e,x)+" seedsPhases="+phases(e));
        boolean full=detailed<detailLimit;if(full)detailed++;
        try{
            double[] r=e.residual(x);Integer[] order=new Integer[r.length];for(int i=0;i<r.length;i++)order[i]=i;
            Arrays.sort(order,(a,b)->Double.compare(Math.abs(r[b]),Math.abs(r[a])));
            var b=new StringBuilder("    top rows:");
            for(int k=0;k<Math.min(full?10:4,r.length);k++)b.append(String.format(Locale.ROOT," %s=%.4g",rl[order[k]],r[order[k]]));
            System.out.println(b);
        }catch(RuntimeException failure){System.out.println("    residual at last point failed: "+failure);}
        var m=java.util.regex.Pattern.compile("variable (\\d+)").matcher(f.getMessage());
        if(m.find()){int column=Integer.parseInt(m.group(1));double scale=e.differenceScale(column,x[column]),step=1e-6*scale;
            System.out.println("    jacobian column "+column+"="+vl[column]+" x="+x[column]+" diffScale="+scale);
            for(double s:new double[]{1,-1,1e-3,-1e-3,1e-6,-1e-6}){double[] t=x.clone();t[column]+=s*step;
                try{e.residual(t);System.out.println("      trial "+s+": ok");}catch(RuntimeException failure){System.out.println("      trial "+s+": "+failure.getMessage());}}
            try{e.residual(x);System.out.println("      base point ok");}catch(RuntimeException failure){System.out.println("      base point: "+failure.getMessage());}
        }
        if(!full)return;
        var b=new StringBuilder("    variables:");for(int i=0;i<x.length;i++)b.append(String.format(Locale.ROOT," %s=%.6g",vl[i],x[i]));System.out.println(b);
        try{var states=e.states(x);
            for(int i=0;i<states.size();i++){var s=states.get(i);if(e.layout[i]==null)continue;var bb=new StringBuilder();double[] l=s.liquidView(),v=s.vaporView();
                for(int c=0;c<l.length;c++)if(l[c]+v[c]!=0)bb.append(String.format(Locale.ROOT,"%s=%.4g/%.4g ",comp(c),l[c],v[c]));
                System.out.println(String.format(Locale.ROOT,"    node%d %s P=%.6f T=%.6f Vliq=%.4g Vw=%.4g Vvap=%.4g wL=%.8g wV=%.4g HC[l/v]: %s",i,e.graph.reservoirs().get(i).kind(),s.pressure(),s.temperature(),s.liquidVolume(),s.waterVolume(),s.vaporVolume(),s.waterLiquid(),s.waterVapor(),bb));}
        }catch(RuntimeException failure){System.out.println("    states failed: "+failure);}
    }
    String flows(PassiveStepSolver.Equations e,double[] x){
        var b=new StringBuilder("[");for(int i=0;i<e.graph.pipes().size();i++)b.append(i>0?", ":"").append(String.format(Locale.ROOT,"%.6g",x[e.edgeOffset+i]));return b.append(']').toString();
    }
    String phases(PassiveStepSolver.Equations e){
        var b=new StringBuilder("[");for(int i=0;i<e.seeds.size();i++){var s=e.seeds.get(i);b.append(i>0?", ":"").append(e.layout[i]==null?"fixed":String.format(Locale.ROOT,"liq=%.3g w=%.3g vap=%.3g",s.liquidVolume(),s.waterVolume(),s.vaporVolume()));}return b.append(']').toString();
    }
    void label(PassiveStepSolver.Equations e,String[] vl,String[] rl){
        Arrays.fill(vl,"?");Arrays.fill(rl,"?");
        for(int node=0;node<e.layout.length;node++){var L=e.layout[node];if(L==null)continue;int o=e.offsets[node];
            int[] comps=(int[])get(L,"components"),li=(int[])get(L,"liquidIndex"),vi=(int[])get(L,"vaporIndex"),basis=(int[])get(L,"junctionBasis");
            int wl=(int)get(L,"waterLiquidIndex"),wv=(int)get(L,"waterVaporIndex"),ti=(int)get(L,"temperatureIndex"),pi=(int)get(L,"pressureIndex"),ppi=(int)get(L,"partialPressureIndex"),si=(int)get(L,"solidIndex");
            String n="n"+node;
            for(int c:comps){if(li[c]>=0)vl[o+li[c]]=n+".L["+comp(c)+"]";if(vi[c]>=0)vl[o+vi[c]]=n+(li[c]>=0?".split[":".V[")+comp(c)+"]";}
            if(wl>=0)vl[o+wl]=n+".wL";if(wv>=0)vl[o+wv]=n+".wV";vl[o+ti]=n+".lnT";vl[o+pi]=n+".lnP";if(ppi>=0)vl[o+ppi]=n+".lnPc";
            if(si>=0)for(int k=0;k<3;k++)vl[o+si+k]=n+".solid"+k;
            int row=o;boolean junction=e.graph.reservoirs().get(node).junction();
            if(junction){for(int k=0;k<basis.length-1;k++)rl[row++]=n+".w["+comp(basis[k])+"]";rl[row++]=n+".net";rl[row++]=n+".h";rl[row++]=n+".norm";}
            else{for(int c:comps)rl[row++]=n+".bal["+comp(c)+"]";if(wl>=0||wv>=0)rl[row++]=n+".bal[water]";rl[row++]=n+".U";rl[row++]=n+".wall";}
            if(si>=0)for(int k=0;k<3;k++)rl[row++]=n+".solid"+k;
            for(int k=0;row<o+L.size();k++)rl[row++]=n+".eq"+k;
        }
        for(int i=0;i<e.graph.pipes().size();i++){vl[e.edgeOffset+i]="e"+i+".flow";rl[e.edgeOffset+i]="e"+i+".hyd";
            if(e.controlOffsets[i]>=0){vl[e.controlOffsets[i]]="e"+i+".ctl";rl[e.controlOffsets[i]]="e"+i+".ctl";}}
        if(e.weirOffsets!=null)for(int i=0;i<e.weirOffsets.length;i++)if(e.weirOffsets[i]!=null)for(int end=0;end<2;end++)if(e.weirOffsets[i][end]>=0){vl[e.weirOffsets[i][end]]="e"+i+".weir"+end;rl[e.weirOffsets[i][end]]="e"+i+".weir"+end;}
    }
}
