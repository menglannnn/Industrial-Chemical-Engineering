package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.fluid.support.FluidTestSupport;
import com.wormzjl.createcheme.science.fluid.diagnostics.SolverDiagnostics;
import com.wormzjl.createcheme.science.fluid.network.*;
import com.wormzjl.createcheme.science.fluid.solver.PhaseLayout;
import com.wormzjl.createcheme.science.fluid.solver.SparseNewton;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import com.wormzjl.createcheme.science.material.*;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * G2 (2026-10-12): the ExtremeTopologyIslandTest grid per interval, each tank's stated state against its booked contents.
 * Not a gate: compiled only with g2.init.gradle. -Pg2grid=true; -Pg2spread (1), -Pg2slice (5), -Pg2count (40),
 * -Pg2uncapped (false).
 * Per interval: the highest tank's stated pressure against the highest generator, the largest volume-row and energy-row
 * lag of a tank's stated state against its inventory (V, n, U), the pressure that inventory implies (its energy-volume
 * closure solved to 1e-14 from the stated state), the top generator's flow and mode, and the interval's Newton solves
 * and iterations.
 */
class G2GridProbeTest {
    private static final PipeResistance.Geometry BLOCK=new PipeResistance.Geometry(1,.05,.000045,0);
    @Test void gridLag() {
        if(!Boolean.getBoolean("g2grid"))return;
        double spread=Double.parseDouble(System.getProperty("g2spread","1")),slice=Double.parseDouble(System.getProperty("g2slice","5"));
        int count=Integer.parseInt(System.getProperty("g2count","40"));
        var model=Boolean.getBoolean("g2uncapped")?FluidThermodynamics.forNetwork(MaterialCatalog.bundled(),"createcheme:tjl20_methane_nitrogen",1e-9,100000):FluidTestSupport.networkModel();
        var devices=new ArrayList<PhysicalFluidTopology.Device>();var pressures=new LinkedHashMap<Long,Double>();
        for(int x=0;x<10;x++){devices.add(device(1+x,x,0,Kind.GENERATOR));pressures.put(1L+x,ExtremeTopologyIslandTest.GENERATOR_CENTRE+spread*(ExtremeTopologyIslandTest.GENERATORS[x]-ExtremeTopologyIslandTest.GENERATOR_CENTRE));}
        for(int x=0;x<10;x++)devices.add(device(11+x,x,1,Kind.PIPE));
        for(int x=0;x<10;x++)devices.add(device(21+x,x,2,Kind.PIPE));
        for(int x=0;x<10;x++){devices.add(device(31+x,x,3,Kind.RESERVOIR));pressures.put(31L+x,ExtremeTopologyIslandTest.TANK_CENTRE+spread*(ExtremeTopologyIslandTest.TANKS[x]-ExtremeTopologyIslandTest.TANK_CENTRE));}
        var boundaries=new LinkedHashMap<Long,PassiveNetwork.Reservoir>();
        double[] n2=new double[model.components().size()];n2[MaterialTestBasis.NITROGEN]=1;
        for(var d:devices)if(pressures.containsKey(d.id()))boundaries.put(d.id(),new FluidDeviceSpec(1,350,pressures.get(d.id()),n2).initialize(d,model,()->{}));
        var graph=PassiveNetwork.sizeJunctionHoldups(PhysicalFluidTopology.compile(devices,boundaries).islands().getFirst().graph(),model);
        var nodes=graph.reservoirs();double pMax=0;int top=-1;
        for(int i=0;i<nodes.size();i++)if(nodes.get(i).kind()==PassiveNetwork.NodeKind.GENERATOR&&nodes.get(i).state().pressure()>pMax){pMax=nodes.get(i).state().pressure();top=i;}
        int topEdge=-1;for(int e=0;e<graph.pipes().size();e++){var p=graph.pipes().get(e);if(p.first()==top||p.second()==top)topEdge=e;}
        var solver=new PassiveIntervalSolver(model);PassiveIntervalSolver.Result committed=null;
        boolean enabled=SolverDiagnostics.ENABLED;SolverDiagnostics.ENABLED=true;
        System.out.println("G2_GRID spread="+spread+" slice="+slice+" uncapped="+Boolean.getBoolean("g2uncapped")+" pMax="+pMax);
        try {
            for(int i=0;i<count;i++) {
                SolverDiagnostics.reset();long closures0=closureSolves();
                solver.replayStart(graph,committed==null?null:committed.graph(),committed==null?null:committed.endpointModes());
                var result=solver.solve(graph,slice,PassiveIntervalSolver.Settings.defaults(),()->{});committed=result;graph=result.graph();
                var sample=SolverDiagnostics.sample();
                double tankMax=Double.NEGATIVE_INFINITY,uvMax=Double.NEGATIVE_INFINITY,volumeLag=0,energyLag=0,uvResidual=0;
                for(var node:graph.reservoirs()) {
                    if(node.kind()!=PassiveNetwork.NodeKind.RESERVOIR)continue;
                    var state=node.state();var inventory=node.inventory();tankMax=Math.max(tankMax,state.pressure());
                    var layout=new PhaseLayout(model,state);int balance=layout.componentBalanceCount();
                    double[] x=layout.encode(state),f=new double[layout.size()];
                    layout.residual(state,inventory.moles(),inventory.internalEnergy(),inventory.volume(),f,0,x);
                    energyLag=Math.max(energyLag,Math.abs(f[balance]));volumeLag=Math.max(volumeLag,Math.abs(f[balance+1]));
                    var equations=layout.fixedInventory(inventory.moles(),inventory.internalEnergy(),inventory.volume());double[] closed;double reached;
                    try{var solved=SparseNewton.solve(equations,x,new SparseNewton.Settings(30,1e-14,1e-6,24),()->{});closed=solved.variables();reached=solved.residualNorm();}
                    catch(SparseNewton.Nonconvergence stalled){closed=stalled.lastVariables();reached=Double.NaN;}
                    uvResidual=Math.max(uvResidual,Double.isNaN(reached)?1:reached);
                    uvMax=Math.max(uvMax,layout.decode(closed,0).pressure());
                }
                System.out.println(String.format(Locale.ROOT,"G2_GRID interval=%d tankMax-pMax=%.6e uvMax-pMax=%.6e volumeLag=%.3e energyLag=%.3e uvClosure=%.1e topFlow=%.6e topMode=%s accepted=%d rejected=%d newtonSolves=%d closureSolves=%d newtonIterations=%d",
                        i+1,tankMax-pMax,uvMax-pMax,volumeLag,energyLag,uvResidual,result.averageMassFlows()[topEdge],result.endpointModes().get(topEdge),result.acceptedSubsteps(),result.rejectedSubsteps(),
                        sample.value("newtonSolves"),closureSolves()-closures0,sample.value("newtonIterations")));
            }
        } finally {SolverDiagnostics.ENABLED=enabled;SolverDiagnostics.reset();}
    }
    /** The closure counter of instr-g2-gate-polish-closure-counters.patch, or -1 without it. */
    static long closureSolves(){try{return ((java.util.concurrent.atomic.AtomicLong)InventoryEquilibrium.class.getField("G2_CLOSURE_SOLVES").get(null)).get();}catch(ReflectiveOperationException absent){return -1;}}
    private static PhysicalFluidTopology.Device device(long id,int x,int z,Kind kind){
        return new PhysicalFluidTopology.Device(id,new PhysicalFluidTopology.Position("minecraft:overworld",x,64,z),kind,PhysicalFluidTopology.Direction.EAST,BLOCK,new FlowControl.Passive());
    }
}
