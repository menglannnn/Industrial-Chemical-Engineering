package com.wormzjl.icheme.science.fluid.network;

import java.lang.reflect.*;
import java.util.*;
import com.wormzjl.icheme.science.fluid.thermo.FluidThermodynamics;

/** Diagnostic output on existing values only; never evaluates an extra residual or property. */
public final class AuditTrace {
    private static int lines;
    private static boolean enabled(){return Boolean.getBoolean("fluid.audit.trace")&&lines++<4000;}
    public static void pass(PassiveNetwork graph,List<FluidThermodynamics.State> seeds,double dt,int pass,List<FlowControl.Mode> modes,byte[] segments,byte[] columns){
        if(!enabled())return;
        System.err.println("PASS dt="+dt+" pass="+pass+" modes="+modes+" segments="+Arrays.toString(segments)+" columns="+Arrays.toString(columns));
        for(int i=0;i<seeds.size();i++)if(Set.of(1L,2L,24L,40L).contains(graph.reservoirs().get(i).id())){
            var s=seeds.get(i);System.err.printf(Locale.ROOT," SEED node=%d T=%.6g P=%.8g L=%.6g G=%.6g%n",graph.reservoirs().get(i).id(),s.temperature(),s.pressure(),Arrays.stream(s.liquid()).sum(),Arrays.stream(s.vapor()).sum());
        }
    }
    public static void failed(PassiveNetwork graph,List<FluidThermodynamics.State> states,double[] flows,List<FlowControl.Mode> modes){
        if(!enabled())return;System.err.println("FAILED_POINT modes="+modes+" flows="+Arrays.toString(flows));
        for(int i=0;i<states.size();i++){var state=states.get(i);if(graph.reservoirs().get(i).junction())System.err.printf(Locale.ROOT," FAILED_STATE id=%d P=%.12g T=%.12g rho=%.12g oilV=%.12g gasV=%.12g%n",graph.reservoirs().get(i).id(),state.pressure(),state.temperature(),state.mass()/state.volume(),state.liquidVolume(),state.vaporVolume());}
    }
    public static void event(String text){if(enabled())System.err.println("EVENT "+text);}
    static Object field(Object object,String name)throws Exception{var f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
    public static void newton(Object eq,double[] x,double[] f,String stage){
        if(!eq.getClass().getName().endsWith("PassiveStepSolver$Equations")||!enabled())return;
        try{
            var offsets=(int[])field(eq,"offsets");var graph=(PassiveNetwork)field(eq,"graph");int edge=(int)field(eq,"edgeOffset");
            var controls=(int[])field(eq,"controlOffsets");
            Integer[] rows=new Integer[f.length];for(int i=0;i<f.length;i++)rows[i]=i;Arrays.sort(rows,(a,b)->Double.compare(Math.abs(f[b]),Math.abs(f[a])));
            StringBuilder s=new StringBuilder("NEWTON "+stage);
            for(int i=0;i<Math.min(5,rows.length);i++){
                int row=rows[i];String kind="?";
                if(row>=edge&&row<edge+graph.pipes().size())kind="edge:"+graph.pipes().get(row-edge).id();
                else {for(int p=0;p<controls.length;p++)if(controls[p]==row)kind="control:"+graph.pipes().get(p).id();
                    for(int n=0;n<offsets.length;n++)if(offsets[n]>=0&&row>=offsets[n]&&row<edge){int next=edge;for(int v:offsets)if(v>offsets[n])next=Math.min(next,v);if(row<next)kind="node:"+graph.reservoirs().get(n).id()+"/row:"+(row-offsets[n]);}}
                s.append(String.format(Locale.ROOT," [%d %s f=%.9g x=%.9g]",row,kind,f[row],x[row]));
            }
            System.err.println(s);
        }catch(Exception failed){System.err.println("TRACE unavailable "+failed);}
    }
}
