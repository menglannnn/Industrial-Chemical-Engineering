package com.wormzjl.icheme.science.fluid.network;

import java.util.*;

/** Structural dependence of local regulation rows and junction mass balances, without presentation data. */
public final class ControlConstraints {
    private ControlConstraints(){}
    private record Row(double[] values,BitSet controls){}
    public static BitSet dependent(PassiveNetwork graph){
        int edges=graph.pipes().size();var result=new BitSet(edges);
        if(graph.pipes().stream().filter(p->p.control() instanceof FlowControl.Valve v&&!v.bare()).count()<2)return result;
        var basis=new Row[edges];
        for(int node=0;node<graph.reservoirs().size();node++)if(graph.reservoirs().get(node).junction()){
            double[] row=new double[edges];
            for(int e=0;e<edges;e++){var p=graph.pipes().get(e);if(p.first()==node)row[e]--;if(p.second()==node)row[e]++;}
            insert(basis,row,new BitSet(edges));
        }
        var pressure=new HashMap<Integer,List<Integer>>();
        for(int e=0;e<edges;e++){
            var p=graph.pipes().get(e);if(!(p.control() instanceof FlowControl.Valve valve)||valve.bare())continue;
            if(valve.pressureControl()){pressure.computeIfAbsent(p.first(),ignored->new ArrayList<>()).add(e);continue;}
            // Actual-volume rows also depend on local density/pressure and are not constant mass constraints.
            if(!(valve instanceof FlowControl.ControlValve v)||v.source()!=FlowControl.ValveSource.MASS_FLOW)continue;
            double[] row=new double[edges];row[e]=1;var sources=new BitSet(edges);sources.set(e);
            var dependent=insert(basis,row,sources);if(dependent!=null)result.or(dependent);
        }
        for(var group:pressure.values())if(group.size()>1)for(int e:group)result.set(e);
        return result;
    }
    private static BitSet insert(Row[] basis,double[] row,BitSet controls){
        for(int col=0;col<row.length;col++){
            if(Math.abs(row[col])<1e-10)continue;
            if(basis[col]==null){double scale=row[col];for(int j=col;j<row.length;j++)row[j]/=scale;basis[col]=new Row(row,(BitSet)controls.clone());return null;}
            double factor=row[col];for(int j=col;j<row.length;j++)row[j]-=factor*basis[col].values()[j];controls.or(basis[col].controls());
        }
        return controls;
    }
}
