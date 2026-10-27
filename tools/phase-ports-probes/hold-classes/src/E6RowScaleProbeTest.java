package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.fluid.support.FluidTestSupport;
import com.wormzjl.createcheme.science.fluid.diagnostics.SolverDiagnostics;
import com.wormzjl.createcheme.science.fluid.network.*;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import com.wormzjl.createcheme.science.material.*;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * E6 option (a), the connection row scale from both ends (2026-10-12). Not a gate: compiled only with
 * hold-classes.init.gradle, and read with src/scratch-e6-donor-tol-counters.patch applied (-De6donor=true restores the
 * donor's scale, -De6tol scales the pass tolerance, cap counters).
 * <ul>
 * <li>{@link #mixedGasStates}: the twelve MixedGasJunctionTransientTest cases at 0.1 s (or -De6interval), the physical
 * state at 10, 13 and 16 s (every tank's pressure, temperature and moles, the junction's pressure, what each void took),
 * printed with every digit, and the ledger figures the junction lines print.</li>
 * <li>{@link #capCounters}: the twelve cases at 5 s, the cap-drop calls and recomputes against the residual evaluations.</li>
 * <li>{@link #gridTrace}: the ExtremeTopologyIslandTest grid (-De6spread, -De6slice, -De6count, -De6uncapped) per interval:
 * the highest tank against the highest generator, the top generator's flow and mode, the junctions.</li>
 * </ul>
 */
class E6RowScaleProbeTest {
    private final FluidThermodynamics model=FluidThermodynamics.forNetwork(MaterialCatalog.bundled(),"createcheme:tjl20_methane_nitrogen",1e-9);
    private final double[] mw=model.molecularWeights();

    @Test void mixedGasStates() {
        if(!Boolean.getBoolean("e6states"))return;
        double interval=Double.parseDouble(System.getProperty("e6interval","0.1"));
        for(double bore:new double[]{.05,.02})for(int ports=4;ports<=6;ports++)for(boolean unequal:new boolean[]{false,true}) {
            String key=bore+":"+ports+":"+unequal;
            try{for(var line:runCase(bore,ports,unequal,interval))System.out.println("E6_STATE key="+key+" "+line);}
            catch(Exception|AssertionError failure){System.out.println("E6_STATE key="+key+" FAILED "+failure);}
        }
    }
    @Test void capCounters() {
        if(!Boolean.getBoolean("e6counters"))return;
        boolean enabled=SolverDiagnostics.ENABLED;SolverDiagnostics.reset();SolverDiagnostics.ENABLED=true;
        PassiveStepSolver.SCRATCH_CAP_CALLS.set(0);PassiveStepSolver.SCRATCH_CAP_COMPUTES.set(0);PassiveStepSolver.SCRATCH_CAP_NANOS.set(0);long start=System.nanoTime();
        for(double bore:new double[]{.05,.02})for(int ports=4;ports<=6;ports++)for(boolean unequal:new boolean[]{false,true})runCase(bore,ports,unequal,5);
        var sample=SolverDiagnostics.sample();SolverDiagnostics.ENABLED=enabled;SolverDiagnostics.reset();
        System.out.println("E6_COUNTERS donor="+Boolean.getBoolean("e6donor")+" capCalls="+PassiveStepSolver.SCRATCH_CAP_CALLS.get()+" capComputes="+PassiveStepSolver.SCRATCH_CAP_COMPUTES.get()
                +" capNanos="+PassiveStepSolver.SCRATCH_CAP_NANOS.get()+" residualEvaluations="+sample.value("residualEvaluations")+" residualEvaluationsInJacobian="+sample.value("residualEvaluationsInJacobian")
                +" jacobianBuilds="+sample.value("jacobianBuilds")+" newtonSolves="+sample.value("newtonSolves")+" newtonIterations="+sample.value("newtonIterations")+" ms="+(System.nanoTime()-start)/1000000);
    }

    /** One MixedGasJunctionTransientTest case; the state at 10, 13 and 16 s and the ledger figures. */
    private List<String> runCase(double bore,int ports,boolean unequal,double interval){
        int[][] positions={{-1,0,0},{1,0,0},{0,0,-1},{0,0,1},{0,1,0},{0,-1,0}};
        var geometry=new PipeResistance.Geometry(1,bore,PipeResistance.DEFAULT_ROUGHNESS_METRES,0);
        var devices=new ArrayList<PhysicalFluidTopology.Device>();
        devices.add(new PhysicalFluidTopology.Device(100,new PhysicalFluidTopology.Position("minecraft:overworld",0,0,0),Kind.PIPE,PhysicalFluidTopology.Direction.EAST,geometry,new FlowControl.Passive()));
        var boundaries=new HashMap<Long,PassiveNetwork.Reservoir>();
        for(int i=0;i<ports;i++){
            int[] at=positions[i];var kind=i<2?Kind.RESERVOIR:Kind.VOID;
            var d=new PhysicalFluidTopology.Device(i+1,new PhysicalFluidTopology.Position("minecraft:overworld",at[0],at[1],at[2]),kind,PhysicalFluidTopology.Direction.EAST,geometry,new FlowControl.Passive());devices.add(d);
            double pressure=i==0?150000:i==1?(unequal?90000:150000):101325;
            double temperature=i==1&&unequal?400:350;
            double[] n=new double[mw.length];n[i==0?0:MaterialTestBasis.NITROGEN]=1;
            var unit=model.flashTP(temperature,pressure,n,()->{});for(int c=0;c<n.length;c++)n[c]/=unit.volume();
            var state=model.flashTP(temperature,pressure,n,()->{});
            boundaries.put(d.id(),new PassiveNetwork.Reservoir(d.id(),at[1],state,i<2?PassiveNetwork.NodeKind.RESERVOIR:PassiveNetwork.NodeKind.VOID));
        }
        var graph=PassiveNetwork.sizeJunctionHoldups(PhysicalFluidTopology.compile(devices,boundaries).islands().getFirst().graph(),model);
        double[] initial=totals(graph),external=new double[mw.length];
        double initialEnergy=energy(graph),externalEnergy=0,worstMoles=0,worstEnergy=0;
        int tank=-1;for(int i=0;i<graph.reservoirs().size();i++)if(graph.reservoirs().get(i).id()==1)tank=i;
        double[] feed=new double[mw.length];feed[0]=.5;
        var feedState=model.flashTP(350,150000,feed,()->{});
        double feedEnergy=feedState.enthalpy()+.5*mw[0]*PassiveStepSolver.GRAVITY*graph.reservoirs().get(tank).elevation();
        var solver=new PassiveIntervalSolver(model);
        var durations=new ArrayList<Double>();var injecting=new ArrayList<Boolean>();
        for(double[] segment:new double[][]{{10,0},{3,1},{3,0}}){
            double length=segment[0];long whole=Math.round(length/interval);
            if(Math.abs(whole*interval-length)<=1e-9*length){for(long k=0;k<whole;k++){durations.add(interval);injecting.add(segment[1]>0);}}
            else{long full=(long)Math.floor(length/interval);for(long k=0;k<full;k++){durations.add(interval);injecting.add(segment[1]>0);}durations.add(length-full*interval);injecting.add(segment[1]>0);}
        }
        var lines=new ArrayList<String>();var voidTaken=new TreeMap<Long,double[]>();double time=0;long accepted=0,rejected=0;
        String fixed=System.getProperty("e6step");var settings=fixed==null?PassiveIntervalSolver.Settings.defaults():new PassiveIntervalSolver.Settings(Double.parseDouble(fixed),Double.parseDouble(fixed),100000);
        PassiveIntervalSolver.Result committed=null;
        for(int step=0;step<durations.size();step++){
            List<ScheduledTransfer> transfers=injecting.get(step)?List.of(new ScheduledTransfer.Injection(900,tank,feed,feedEnergy)):List.of();
            graph=new PassiveNetwork(graph.reservoirs(),graph.pipes(),transfers);
            solver.replayStart(graph,committed==null?null:committed.graph(),committed==null?null:committed.endpointModes());
            var result=solver.solve(graph,durations.get(step),settings,()->{});committed=result;accepted+=result.acceptedSubsteps();rejected+=result.rejectedSubsteps();
            graph=result.graph();time+=durations.get(step);
            for(var transfer:result.boundaries()){var n=transfer.moles();for(int c=0;c<n.length;c++)external[c]+=n[c];externalEnergy+=transfer.totalEnergyJoule();
                if(transfer.nodeId()!=900){var v=voidTaken.computeIfAbsent(transfer.nodeId(),k->new double[mw.length]);for(int c=0;c<n.length;c++)v[c]-=n[c];}}
            var sum=totals(graph);
            for(int c=0;c<sum.length;c++)worstMoles=Math.max(worstMoles,Math.abs(sum[c]-initial[c]-external[c])/Math.max(1,initial[c]));
            worstEnergy=Math.max(worstEnergy,Math.abs(energy(graph)-initialEnergy-externalEnergy)/Math.max(1,Math.abs(initialEnergy)));
            for(double at:new double[]{10,13,16})if(Math.abs(time-at)<1e-6) {
                var b=new StringBuilder(String.format(Locale.ROOT,"t=%.1f",at));
                for(var node:graph.reservoirs()) {
                    if(node.kind()==PassiveNetwork.NodeKind.RESERVOIR||node.junction()) {
                        var n=node.inventory().moles();
                        b.append(String.format(Locale.ROOT," %s%d:P=%.17g,T=%.17g,n0=%.17g,nN2=%.17g",node.junction()?"J":"T",node.id(),node.state().pressure(),node.state().temperature(),n[0],n[MaterialTestBasis.NITROGEN]));
                    }
                }
                for(var e:voidTaken.entrySet())b.append(String.format(Locale.ROOT," V%d:n0=%.17g,nN2=%.17g",e.getKey(),e.getValue()[0],e.getValue()[MaterialTestBasis.NITROGEN]));
                lines.add(b.toString());
            }
        }
        lines.add("ledger worstMoles="+worstMoles+" worstEnergy="+worstEnergy+" accepted="+accepted+" rejected="+rejected);
        return lines;
    }
    private double[] totals(PassiveNetwork graph){
        var totals=new double[mw.length];
        for(var node:graph.reservoirs())if(node.kind()==PassiveNetwork.NodeKind.RESERVOIR||node.junction()){var n=node.inventory().moles();for(int c=0;c<n.length;c++)totals[c]+=n[c];}
        return totals;
    }
    private double energy(PassiveNetwork graph){
        double energy=0;
        for(var node:graph.reservoirs())if(node.kind()==PassiveNetwork.NodeKind.RESERVOIR||node.junction()){
            double mass=0;var n=node.inventory().moles();for(int c=0;c<n.length;c++)mass+=mw[c]*n[c];
            energy+=node.inventory().internalEnergy()+mass*PassiveStepSolver.GRAVITY*node.elevation();
        }
        return energy;
    }

    /* ---------------- the extreme grid ---------------- */

    private static final PipeResistance.Geometry BLOCK=new PipeResistance.Geometry(1,.05,.000045,0);
    @Test void gridTrace() {
        if(!Boolean.getBoolean("e6grid"))return;
        double spread=Double.parseDouble(System.getProperty("e6spread","1")),slice=Double.parseDouble(System.getProperty("e6slice","5"));
        int count=Integer.getInteger("e6count",40);
        var gridModel=Boolean.getBoolean("e6uncapped")?FluidThermodynamics.forNetwork(MaterialCatalog.bundled(),"createcheme:tjl20_methane_nitrogen",1e-9,100000):FluidTestSupport.networkModel();
        var devices=new ArrayList<PhysicalFluidTopology.Device>();var pressures=new LinkedHashMap<Long,Double>();
        for(int x=0;x<10;x++){devices.add(device(1+x,x,0,Kind.GENERATOR));pressures.put(1L+x,ExtremeTopologyIslandTest.GENERATOR_CENTRE+spread*(ExtremeTopologyIslandTest.GENERATORS[x]-ExtremeTopologyIslandTest.GENERATOR_CENTRE));}
        for(int x=0;x<10;x++)devices.add(device(11+x,x,1,Kind.PIPE));
        for(int x=0;x<10;x++)devices.add(device(21+x,x,2,Kind.PIPE));
        for(int x=0;x<10;x++){devices.add(device(31+x,x,3,Kind.RESERVOIR));pressures.put(31L+x,ExtremeTopologyIslandTest.TANK_CENTRE+spread*(ExtremeTopologyIslandTest.TANKS[x]-ExtremeTopologyIslandTest.TANK_CENTRE));}
        var boundaries=new LinkedHashMap<Long,PassiveNetwork.Reservoir>();
        double[] n2=new double[gridModel.components().size()];n2[MaterialTestBasis.NITROGEN]=1;
        for(var d:devices)if(pressures.containsKey(d.id()))boundaries.put(d.id(),new FluidDeviceSpec(1,350,pressures.get(d.id()),n2).initialize(d,gridModel,()->{}));
        var graph=PassiveNetwork.sizeJunctionHoldups(PhysicalFluidTopology.compile(devices,boundaries).islands().getFirst().graph(),gridModel);
        var nodes=graph.reservoirs();double pMax=0;int top=-1;
        for(int i=0;i<nodes.size();i++)if(nodes.get(i).kind()==PassiveNetwork.NodeKind.GENERATOR&&nodes.get(i).state().pressure()>pMax){pMax=nodes.get(i).state().pressure();top=i;}
        int topEdge=-1;for(int e=0;e<graph.pipes().size();e++){var p=graph.pipes().get(e);if(p.first()==top||p.second()==top)topEdge=e;}
        var solver=new PassiveIntervalSolver(gridModel);PassiveIntervalSolver.Result committed=null;
        System.out.println("E6_GRID donor="+Boolean.getBoolean("e6donor")+" spread="+spread+" slice="+slice+" pMax="+pMax+" topNode="+top+" topEdge="+topEdge);
        for(int i=0;i<count;i++) {
            solver.replayStart(graph,committed==null?null:committed.graph(),committed==null?null:committed.endpointModes());
            var result=solver.solve(graph,slice,PassiveIntervalSolver.Settings.defaults(),()->{});committed=result;graph=result.graph();
            double supplied=0,held=0;for(var t:result.boundaries())for(int c=0;c<t.moles().length;c++)supplied+=t.moles()[c]*gridModel.molecularWeights()[c];
            for(var node:graph.reservoirs())if(node.kind()==PassiveNetwork.NodeKind.RESERVOIR||node.junction())for(int c=0;c<node.inventory().moles().length;c++)held+=node.inventory().moles()[c]*gridModel.molecularWeights()[c];
            double tankMax=Double.NEGATIVE_INFINITY,tankMin=Double.POSITIVE_INFINITY,junctionMax=Double.NEGATIVE_INFINITY;int open=0;
            for(var node:graph.reservoirs()){double p=node.state().pressure();
                if(node.kind()==PassiveNetwork.NodeKind.RESERVOIR){tankMax=Math.max(tankMax,p);tankMin=Math.min(tankMin,p);}
                if(node.junction())junctionMax=Math.max(junctionMax,p);}
            var modes=result.endpointModes();for(int e=0;e<modes.size();e++){var p=graph.pipes().get(e);
                boolean gen=graph.reservoirs().get(p.first()).kind()==PassiveNetwork.NodeKind.GENERATOR||graph.reservoirs().get(p.second()).kind()==PassiveNetwork.NodeKind.GENERATOR;
                if(gen&&modes.get(e)!=FlowControl.Mode.CLOSED)open++;}
            System.out.println(String.format(Locale.ROOT,"E6_GRID interval=%d tankMax-pMax=%.6e tankMin-pMax=%.6e junctionMax-pMax=%.6e topFlow=%.6e topMode=%s openGenerators=%d accepted=%d rejected=%d supplied=%.6e kg held=%.17g kg",
                    i+1,tankMax-pMax,tankMin-pMax,junctionMax-pMax,result.averageMassFlows()[topEdge],modes.get(topEdge),open,result.acceptedSubsteps(),result.rejectedSubsteps(),supplied,held));
        }
    }
    private static PhysicalFluidTopology.Device device(long id,int x,int z,Kind kind){
        return new PhysicalFluidTopology.Device(id,new PhysicalFluidTopology.Position("minecraft:overworld",x,64,z),kind,PhysicalFluidTopology.Direction.EAST,BLOCK,new FlowControl.Passive());
    }
}
