package com.wormzjl.createcheme.science.fluid.network;

import com.wormzjl.createcheme.science.fluid.solver.PhaseLayout;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import java.util.*;

/** t45 probe (tools/junction-seeding/trace-branch, export only, never tracked): one line per node of a graph. -Dt45.trace=true. */
public final class T45Probe {
    public static final boolean ON=Boolean.getBoolean("t45.trace");
    private T45Probe(){}
    public static double inventoryMass(PassiveNetwork.Reservoir node,FluidThermodynamics model) {
        double m=0;var n=node.inventory().moles();for(int c=0;c<n.length;c++)m+=n[c]*model.molecularWeight(c);return m;
    }
    public static String node(PassiveNetwork.Reservoir node,FluidThermodynamics model) {
        var s=node.state();double[] a=PhaseLayout.totalAmounts(s);int n2=model.hydrocarbon.components().indexOf("Nitrogen");
        double total=0,nitrogen=0,methane=0;int ch4=model.hydrocarbon.components().indexOf("Methane");
        for(int c=0;c<a.length;c++){double m=a[c]*model.molecularWeight(c);total+=m;if(c==n2)nitrogen+=m;if(c==ch4)methane+=m;}
        return String.format(Locale.ROOT,"[%d %s P=%.2fkPa T=%.2fK rho=%.4g liqShare=%.3g N2=%.4g CH4=%.3g HC=%.3g%s]",node.id(),node.kind().name().substring(0,3),
                s.pressure()/1000,s.temperature(),s.mass()/s.volume(),s.volume()>0?(s.liquidVolume()+s.waterVolume())/s.volume():0,
                total>0?nitrogen/total:0,total>0?methane/total:0,total>0?1-nitrogen/total:0,node.junction()?String.format(Locale.ROOT," mJ=%.4gkg",inventoryMass(node,model)):"");
    }
    public static void nodes(String label,PassiveNetwork graph,FluidThermodynamics model) {
        if(!ON)return;var b=new StringBuilder(label);for(var n:graph.reservoirs())if(n.junction())b.append(' ').append(node(n,model));System.out.println(b);
    }
    public static void states(String label,PassiveNetwork graph,List<FluidThermodynamics.State> states,FluidThermodynamics model) {
        if(!ON)return;var b=new StringBuilder(label);
        for(int i=0;i<graph.reservoirs().size();i++){var n=graph.reservoirs().get(i);if(!n.junction()||states.get(i)==null)continue;
            b.append(' ').append(node(new PassiveNetwork.Reservoir(n.id(),n.elevation(),states.get(i),n.kind(),n.inventory()),model));}
        System.out.println(b);
    }
    public static void flows(String label,PassiveNetwork graph,double[] q) {
        if(!ON)return;var b=new StringBuilder(label);
        for(int e=0;e<q.length;e++){var p=graph.pipes().get(e);b.append(String.format(Locale.ROOT," e%d[%d>%d %s]=%.5g",e,graph.reservoirs().get(p.first()).id(),graph.reservoirs().get(p.second()).id(),p.control().getClass().getSimpleName(),q[e]));}
        System.out.println(b);
    }
}
