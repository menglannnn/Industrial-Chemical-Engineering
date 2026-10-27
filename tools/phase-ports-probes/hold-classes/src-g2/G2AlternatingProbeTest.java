package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.fluid.support.FluidTestSupport;
import com.wormzjl.createcheme.science.fluid.network.*;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import com.wormzjl.createcheme.science.material.*;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * The one-way scale refinement (A91) on ExtremeTopologyIslandTest's ALTERNATING_BOTH_SIDES at the default cap, 0.1 s
 * slices (2026-10-12). Not a gate: compiled only with g2.init.gradle; -Pg2alt=true, -Pg2count (400). For every interval in
 * which a generator's booked transfer is negative (it received), the interval, the net amount, and per connection of
 * that generator the other end and the interval's average solved flow (positive out of the generator).
 */
class G2AlternatingProbeTest {
    private static final PipeResistance.Geometry BLOCK=new PipeResistance.Geometry(1,.05,.000045,0);
    @Test void generatorsReceiving() {
        if(!Boolean.getBoolean("g2alt"))return;
        int count=Integer.parseInt(System.getProperty("g2count","400"));double slice=.1;
        var model=FluidTestSupport.networkModel();int n=10;
        var devices=new ArrayList<PhysicalFluidTopology.Device>();var pressures=new LinkedHashMap<Long,Double>();
        for(int x=0;x<2*n;x++){boolean generator=x%2==0;devices.add(device(1+x,x,0,generator?Kind.GENERATOR:Kind.RESERVOIR));
            pressures.put(1L+x,generator?ExtremeTopologyIslandTest.GENERATORS[x/2]:ExtremeTopologyIslandTest.TANKS[x/2]);}
        for(int x=0;x<2*n;x++)devices.add(device(21+x,x,1,Kind.PIPE));
        for(int x=0;x<2*n;x++)devices.add(device(41+x,x,-1,Kind.PIPE));
        var boundaries=new LinkedHashMap<Long,PassiveNetwork.Reservoir>();
        double[] n2=new double[model.components().size()];n2[MaterialTestBasis.NITROGEN]=1;
        for(var d:devices)if(pressures.containsKey(d.id()))boundaries.put(d.id(),new FluidDeviceSpec(1,350,pressures.get(d.id()),n2).initialize(d,model,()->{}));
        var graph=PassiveNetwork.sizeJunctionHoldups(PhysicalFluidTopology.compile(devices,boundaries).islands().getFirst().graph(),model);
        double[] mw=model.molecularWeights();
        var solver=new PassiveIntervalSolver(model);PassiveIntervalSolver.Result committed=null;int receiving=0;double worst=0;
        for(int i=0;i<count;i++) {
            solver.replayStart(graph,committed==null?null:committed.graph(),committed==null?null:committed.endpointModes());
            var result=solver.solve(graph,slice,PassiveIntervalSolver.Settings.defaults(),()->{});committed=result;graph=result.graph();
            var net=new TreeMap<Long,Double>();
            for(var t:result.boundaries()){double m=0;for(int c=0;c<t.moles().length;c++)m+=t.moles()[c]*mw[c];net.merge(t.nodeId(),m,Double::sum);}
            for(var e:net.entrySet()) {
                boolean at=(","+System.getProperty("g2altat","")+",").contains(","+(i+1)+",")&&e.getKey()==7;
                if(!at&&e.getValue()>=-ExtremeTopologyIslandTest.ZERO_FLOW*slice)continue;
                if(e.getValue()<-ExtremeTopologyIslandTest.ZERO_FLOW*slice){
                receiving++;worst=Math.max(worst,-e.getValue()/slice);}
                var line=new StringBuilder(String.format(Locale.ROOT,"G2_ALT interval=%d generator=%d received=%.4e kg (%.3e kg/s)",i+1,e.getKey(),-e.getValue(),-e.getValue()/slice));
                for(int k=0;k<graph.pipes().size();k++){var p=graph.pipes().get(k);var a=graph.reservoirs().get(p.first());var b=graph.reservoirs().get(p.second());
                    if(a.id()!=e.getKey()&&b.id()!=e.getKey())continue;
                    double out=(a.id()==e.getKey()?1:-1)*result.averageMassFlows()[k];var other=a.id()==e.getKey()?b:a;
                    line.append(String.format(Locale.ROOT," | to %s %d: %.4e kg/s, end mode %s",other.kind(),other.id(),out,result.endpointModes().get(k)));}
                System.out.println(line);
            }
        }
        System.out.println(String.format(Locale.ROOT,"G2_ALT summary intervals=%d receiving=%d worst=%.3e kg/s",count,receiving,worst));
    }
    private static PhysicalFluidTopology.Device device(long id,int x,int z,Kind kind){
        return new PhysicalFluidTopology.Device(id,new PhysicalFluidTopology.Position("minecraft:overworld",x,64,z),kind,PhysicalFluidTopology.Direction.EAST,BLOCK,new FlowControl.Passive());
    }
}
