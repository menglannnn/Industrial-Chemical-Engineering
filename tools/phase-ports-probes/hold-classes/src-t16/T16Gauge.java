package com.wormzjl.createcheme.science.fluid.network;

import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.createcheme.science.fluid.transport.MixtureViscosity;
import java.util.*;

/**
 * t16 probe helper (tools only, never in src/): the static drives and flow resolutions of an island's graph, read
 * with the solver's own end pressure, end density and one-way rule, as numbers (NetworkReduction.directions gives
 * only their signs). Used to find which holds are exact ties (family B) and zero-flow loops (family C).
 */
public final class T16Gauge {
    private T16Gauge() {}
    public static final double RESOLUTION=.1;
    /** Per connection {forward, reverse} allowed static drive in Pa (negative infinity when the direction is not allowed). */
    public static double[][] drives(PassiveNetwork graph,FluidThermodynamics model) {
        var out=new double[graph.pipes().size()][2];
        for(int i=0;i<out.length;i++) {
            var pipe=graph.pipes().get(i);var a=graph.reservoirs().get(pipe.first());var b=graph.reservoirs().get(pipe.second());
            double pa=a.state().pressure()+PassiveStepSolver.portHead(model,a,pipe.firstPort(),a.state());
            double pb=b.state().pressure()+PassiveStepSolver.portHead(model,b,pipe.secondPort(),b.state());
            double dz=b.elevation()-a.elevation(),difference=pa-pb;
            out[i][0]=PassiveStepSolver.boundaryAllowed(graph,pipe,1)?difference-PassiveStepSolver.endDensity(model,a.state(),pipe.firstPort())*PassiveStepSolver.GRAVITY*dz:Double.NEGATIVE_INFINITY;
            out[i][1]=PassiveStepSolver.boundaryAllowed(graph,pipe,-1)?-(difference-PassiveStepSolver.endDensity(model,b.state(),pipe.secondPort())*PassiveStepSolver.GRAVITY*dz):Double.NEGATIVE_INFINITY;
        }
        return out;
    }
    /** The flow (kg/s) that {@link #RESOLUTION} drives through the connection on its laminar branch, in the fluid end
     * {@code node} sends; infinity when the connection has no laminar resistance. */
    public static double resolutionFlow(PassiveNetwork graph,FluidThermodynamics model,int pipeIndex,int node,MixtureViscosity.Workspace scratch) {
        var pipe=graph.pipes().get(pipeIndex);var end=graph.reservoirs().get(node);var s=end.state();var port=pipe.portAt(node);
        double rho=PassiveStepSolver.endDensity(model,s,port);int phase=PhaseDraw.leading(model,s,port);double mu;
        if(phase>=0)mu=model.phaseViscosity(s,phase,scratch,null);
        else{double v=0,w=0;for(int p:new int[]{FluidThermodynamics.GAS,FluidThermodynamics.OIL,FluidThermodynamics.WATER})if(FluidThermodynamics.holdsPhase(s,p)){double x=FluidThermodynamics.phaseVolume(s,p);w+=x*model.phaseViscosity(s,p,scratch,null);v+=x;}mu=w/v;}
        double slope=0;
        for(var section:pipe.sections())slope+=128*mu*section.length()/(Math.PI*rho*Math.pow(section.diameter(),4));
        if(pipe.filter()!=null){double l=pipe.filter().loading();slope+=pipe.filter().cleanResistance()*(mu/.001)*(1+99*l*l)/rho;}
        return slope>0?RESOLUTION/slope:Double.POSITIVE_INFINITY;
    }
    /** Independent loops among the nodes that are not fixed boundaries (junctions and vessels). */
    public static int loops(PassiveNetwork graph) {
        int n=graph.reservoirs().size();int[] parent=new int[n];for(int i=0;i<n;i++)parent[i]=i;int loops=0;
        for(var pipe:graph.pipes()) {
            if(graph.reservoirs().get(pipe.first()).fixed()||graph.reservoirs().get(pipe.second()).fixed())continue;
            int a=root(parent,pipe.first()),b=root(parent,pipe.second());if(a==b)loops++;else parent[Math.max(a,b)]=Math.min(a,b);
        }
        return loops;
    }
    private static int root(int[] p,int x){while(p[x]!=x){p[x]=p[p[x]];x=p[x];}return x;}
    /** Groups of two or more generators with bit-identical pressure, temperature and inventory: "count@P/T/z-list". */
    public static String identicalGenerators(PassiveNetwork graph) {
        var groups=new LinkedHashMap<String,List<String>>();
        for(var node:graph.reservoirs()) {
            if(node.kind()!=PassiveNetwork.NodeKind.GENERATOR)continue;
            String key=Double.doubleToLongBits(node.state().pressure())+":"+Double.doubleToLongBits(node.state().temperature())+":"+Arrays.toString(node.inventory().moles());
            groups.computeIfAbsent(key,k->new ArrayList<>()).add(String.valueOf(node.elevation()));
        }
        var b=new StringBuilder();
        for(var e:groups.entrySet())if(e.getValue().size()>1)b.append(e.getValue().size()).append("x@z").append(e.getValue()).append(' ');
        return b.isEmpty()?"-":b.toString().trim();
    }
}
