package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.fluid.support.FluidTestSupport;
import com.wormzjl.createcheme.runtime.ProcessSolveServices;
import com.wormzjl.createcheme.science.fluid.diagnostics.SolverDiagnostics;
import com.wormzjl.createcheme.science.fluid.network.*;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.createcheme.science.fluid.thermo.WaterRegion1;
import com.wormzjl.createcheme.science.material.MaterialTestBasis;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;

/**
 * M2PROBE, never committed: CadenceTrajectoryQualificationTest's rows and McpGameplayRegressionTest's clamped drain under
 * three laws of the network's liquid water (WaterRegion1.M2PROBE_KAPPA from tools/unified-thermo-port-m2/probes/specs/
 * water-kappa-probe.json): base (Region 1 at the state pressure), A (the compressibility alone back at 1e-9 1/Pa, the
 * density level Region 1's at 1 atm) and B (the retired law exactly: Region 1 at 2 MPa with 1e-9 1/Pa). Rows print, they do
 * not assert; each ratio is the error over the test's own bound.
 */
class M2StiffProbeTest {
    private static final double REFERENCE_TOLERANCE=.0025,CADENCE_TOLERANCE=.02;
    private record Variant(String name,double kappa,double p0){}
    private static final List<Variant> VARIANTS=List.of(new Variant("base: Region 1 at the state pressure",0,101325),
            new Variant("A: kappa 1e-9 1/Pa about 1 atm (Region 1's density at 1 atm)",1e-9,101325),
            new Variant("B: the retired law, Region 1 at 2 MPa with kappa 1e-9 1/Pa",1e-9,2e6));
    private FluidThermodynamics model;private double[] molecularWeights;
    private final StringBuilder out=new StringBuilder();
    private void line(String s){out.append(s).append('\n');System.out.println(s);}
    private record Frame(int tick,PassiveNetwork graph,double massTransferred) {}
    private record Trajectory(List<Frame> frames,Set<Integer> cadences,int intervals) {}

    @Test void probe() throws Exception {
        try {
            for(var v:VARIANTS) {
                WaterRegion1.M2PROBE_KAPPA=v.kappa();WaterRegion1.M2PROBE_P0=v.p0();
                model=FluidTestSupport.networkModel();molecularWeights=FluidTestSupport.molecularWeights(model);
                line("=== variant "+v.name());
                for(String fluid:List.of("water","wet-crude"))try{cadence(fluid);}catch(Throwable t){line("cadence "+fluid+" threw "+t);}
                try{mcp();}catch(Throwable t){line("mcp threw "+t);}
            }
        } finally {
            WaterRegion1.M2PROBE_KAPPA=0;
            var path=Path.of("build/m2-logs/10-stiff-probe.txt");Files.createDirectories(path.getParent());Files.writeString(path,out.toString());
        }
    }

    // ---- CadenceTrajectoryQualificationTest, rows printed ----
    private void cadence(String fluid) {
        var initial=initial(fluid);
        var s0=initial.reservoirs().get(0).state();var s1=initial.reservoirs().get(1).state();
        line(String.format("cadence %s initial: P %.3f / %.3f Pa, T %.4f K, mass %.6f / %.6f kg, water volume %.9f / %.9f m3, vapour %.6g / %.6g m3, water kappa %.6g / %.6g 1/Pa",
                fluid,s0.pressure(),s1.pressure(),s0.temperature(),s0.mass(),s1.mass(),s0.waterVolume(),s1.waterVolume(),s0.vaporVolume(),s1.vaporVolume(),s0.waterCompressibility(),s1.waterCompressibility()));
        var coarse=reference(initial,.5);var fine=reference(initial,.25);
        double previous=s0.pressure()-s1.pressure();int previousTick=0;
        for(var f:fine.frames()) {
            var a=f.graph().reservoirs().get(0).state();var b=f.graph().reservoirs().get(1).state();double dp=a.pressure()-b.pressure();
            line(String.format("  fine 0.25 s t=%d s: P %.3f / %.3f Pa, dP %.3f Pa, moved %.9f kg, tau over the frame %.3f s",
                    f.tick()/20,a.pressure(),b.pressure(),dp,f.massTransferred(),(f.tick()-previousTick)/20.0/Math.log(previous/dp)));
            previous=dp;previousTick=f.tick();
        }
        for(var f:coarse.frames())line(String.format("  coarse 0.5 s t=%d s: moved %.9f kg",f.tick()/20,f.massTransferred()));
        compare(fluid,"reference-refinement",coarse,fine,REFERENCE_TOLERANCE);
        if(WaterRegion1.M2PROBE_KAPPA==0&&fluid.equals("water")) {
            var finer=reference(initial,.125);compare(fluid,"option-b refinement 0.25 against 0.125 s",fine,finer,REFERENCE_TOLERANCE);
            for(int cadence:new int[]{1,5,20})compare(fluid,"option-b fixed-"+cadence+" against 0.125 s",runFixed(initial,cadence),finer,CADENCE_TOLERANCE);
            compare(fluid,"option-b adaptive against 0.125 s",runAdaptive(initial),finer,CADENCE_TOLERANCE);
        }
        for(int cadence:new int[]{1,5,20})try{compare(fluid,"fixed-"+cadence,runFixed(initial,cadence),fine,CADENCE_TOLERANCE);}catch(Throwable t){line("  "+fluid+" fixed-"+cadence+" threw "+t);}
        try{compare(fluid,"adaptive-controlled-cpu",runAdaptive(initial),fine,CADENCE_TOLERANCE);}catch(Throwable t){line("  "+fluid+" adaptive threw "+t);}
    }
    private PassiveNetwork initial(String fluid) {
        var mixture=fluid.equals("water")?FluidTestSupport.Mixture.WATER:FluidTestSupport.Mixture.WET_CRUDE;
        double upper=fluid.equals("water")?500000:200000;
        var nodes=new ArrayList<PassiveNetwork.Reservoir>();
        for(double pressure:new double[]{upper,150000})nodes.add(new PassiveNetwork.Reservoir(nodes.size()+1,0,FluidTestSupport.oneCubicMetreState(model,mixture,350,pressure)));
        var geometry=fluid.equals("water")?new PipeResistance.Geometry(10000,.006,.000045,0):new PipeResistance.Geometry(200,.01,.000045,0);
        return new PassiveNetwork(nodes,List.of(new PassiveNetwork.Pipe(3,0,1,geometry)));
    }
    private Trajectory reference(PassiveNetwork initial,double maximumStep) {
        var graph=initial;double moved=0;var frames=new ArrayList<Frame>();
        var settings=new PassiveIntervalSolver.Settings(maximumStep,maximumStep,8192);
        for(int tick=400;tick<=1600;tick+=400) {
            var result=new PassiveIntervalSolver(model).solve(graph,20,settings,()->{});
            graph=result.graph();moved+=20*result.averageMassFlows()[0];frames.add(new Frame(tick,graph,moved));
        }
        return new Trajectory(frames,Set.of(),4);
    }
    private Trajectory runFixed(PassiveNetwork initial,int cadenceSeconds) {
        var graph=initial;double moved=0;var frames=new ArrayList<Frame>();int intervals=0;
        for(int tick=20*cadenceSeconds;tick<=1600;tick+=20*cadenceSeconds) {
            var result=new PassiveIntervalSolver(model).solve(graph,cadenceSeconds,PassiveIntervalSolver.Settings.defaults(),()->{});
            graph=result.graph();moved+=cadenceSeconds*result.averageMassFlows()[0];intervals++;
            if(tick%400==0)frames.add(new Frame(tick,graph,moved));
        }
        return new Trajectory(frames,Set.of(20*cadenceSeconds),intervals);
    }
    private Trajectory runAdaptive(PassiveNetwork initial) {
        class Dispatch implements IslandCoordinator.Dispatcher {
            long sequence;IslandCoordinator.Attempt attempt;ProcessSolveServices.FluidIslandCommand command;
            public int availableWorkers(){return attempt==null?1:0;}
            public long nextRequestId(){return ++sequence;}
            public boolean submit(IslandCoordinator.Attempt a,ProcessSolveServices.FluidIslandCommand c){attempt=a;command=c;return true;}
            public void cancel(long request){throw new AssertionError("Controlled cadence fixture must not time out");}
        }
        var dispatch=new Dispatch();double[] moved={0};var frames=new ArrayList<Frame>();var cadences=new TreeSet<Integer>();int[] intervals={0};
        var coordinator=new IslandCoordinator(dispatch,changes->{
            for(var state:changes) {
                var result=state.lastResult().orElseThrow();
                moved[0]+=result.advancedSeconds()*result.averageMassFlows()[0];cadences.add(state.clock().cadenceTicks());intervals[0]++;
                if(state.clock().committedTick()%400==0)frames.add(new Frame((int)state.clock().committedTick(),state.graph(),moved[0]));
            }
        },()->0,new IslandCoordinator.Settings(2_000_000_000L,1_500_000_000L,64,true,100,CertificatePolicy.disabled()));
        coordinator.register(new IslandCoordinator.Snapshot(1,0,initial,new IslandClock.Snapshot(0,0,0,100),FallbackAllowance.NONE,Optional.empty(),Optional.empty(),"READY"),model);
        UUID boundary=UUID.randomUUID();coordinator.fence(boundary,400,List.of(1L));
        for(int tick=1;tick<=1600;tick++) {
            coordinator.tick();
            while(dispatch.attempt!=null) {
                var attempt=dispatch.attempt;var command=dispatch.command;dispatch.attempt=null;dispatch.command=null;
                var result=new PassiveIntervalSolver(model).solve(command.snapshot(),command.durationSeconds(),command.settings(),()->{});
                long cpu=intervals[0]<3?1_600_000_000L:10_000_000L;
                coordinator.completed(attempt,Optional.of(new ProcessSolveServices.FluidIslandSolveResult(Optional.of(result),"FULL",cpu,cpu,FallbackAllowance.NONE,Optional.of(ApproximationAnchor.fromFull(model,result)))));
                coordinator.pump();
            }
            if(tick%400==0&&tick<1600){coordinator.releaseFence(boundary,List.of(1L));boundary=UUID.randomUUID();coordinator.fence(boundary,tick+400,List.of(1L));}
        }
        coordinator.stop();return new Trajectory(frames,cadences,intervals[0]);
    }
    private void compare(String fluid,String cadence,Trajectory candidate,Trajectory reference,double tolerance) {
        double worst=0;
        for(int frame=0;frame<reference.frames().size();frame++) {
            var expected=reference.frames().get(frame);var actual=candidate.frames().get(frame);
            double massError=Math.abs(actual.massTransferred()-expected.massTransferred());
            double massRatio=massError/(1e-8+tolerance*Math.abs(expected.massTransferred()));
            double pressureRatio=0,componentRatio=0,phaseRatio=0,temperatureRatio=0;
            for(int node=0;node<2;node++) {
                var a=actual.graph().reservoirs().get(node);var b=expected.graph().reservoirs().get(node);
                pressureRatio=Math.max(pressureRatio,Math.abs(a.state().pressure()-b.state().pressure())/(1+tolerance*Math.abs(b.state().pressure())));
                temperatureRatio=Math.max(temperatureRatio,Math.abs(a.state().temperature()-b.state().temperature())/.5);
                double[] phaseA={a.state().vaporVolume(),a.state().liquidVolume(),a.state().waterVolume()};
                double[] phaseB={b.state().vaporVolume(),b.state().liquidVolume(),b.state().waterVolume()};
                for(int phase=0;phase<3;phase++)phaseRatio=Math.max(phaseRatio,Math.abs(phaseA[phase]/a.state().volume()-phaseB[phase]/b.state().volume())/tolerance);
                var n=a.inventory().moles();var t=b.inventory().moles();
                for(int c=0;c<n.length;c++)componentRatio=Math.max(componentRatio,Math.abs(n[c]-t[c])/(1e-10+tolerance*Math.abs(t[c])));
            }
            line(String.format("  %s %s t=%d s: cumulative flow %.4f %% off (ratio %.3f), pressure ratio %.3f, component ratio %.3f, phase ratio %.3f, temperature ratio %.3f, intervals %d",
                    fluid,cadence,expected.tick()/20,100*massError/Math.abs(expected.massTransferred()),massRatio,pressureRatio,componentRatio,phaseRatio,temperatureRatio,candidate.intervals()));
            worst=Math.max(worst,Math.max(Math.max(massRatio,pressureRatio),Math.max(Math.max(componentRatio,phaseRatio),temperatureRatio)));
        }
        line(String.format("  %s %s worst ratio to the bound %.3f: %s",fluid,cadence,worst,worst<=1?"PASS":"FAIL"));
    }

    // ---- McpGameplayRegressionTest.clampedDrainContinuesAfterTheTankBecomesAlmostLiquidFull, rows printed ----
    private com.google.gson.JsonObject archivedIsland(String resource) throws java.io.IOException {
        try(var input=Objects.requireNonNull(getClass().getResourceAsStream(resource))) {
            return com.google.gson.JsonParser.parseString(new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("islands").get(0).getAsJsonObject();
        }
    }
    private record Phase(double temperature,double pressure,double[] liquid,double[] vapor,double waterLiquid,double waterVapor,double hydrocarbonPressure) {}
    private record Inventory(double volume,double[] moles,double internalEnergy) {}
    private record Node(long id,double elevation,String kind,Inventory inventory,Phase phase) {}
    private record Control(String kind,double target,double limit,double efficiency) {}
    private record Pipe(long id,int first,int second,List<PipeResistance.Geometry> sections,Control control) {}
    private record Graph(List<Node> nodes,List<Pipe> pipes) {}
    private static PassiveNetwork archivedGraph(com.google.gson.JsonObject island,FluidThermodynamics model) {
        var saved=new com.google.gson.Gson().fromJson(island.getAsJsonObject("graph"),Graph.class);
        var nodes=new ArrayList<PassiveNetwork.Reservoir>();
        for(var n:saved.nodes()) {
            var p=n.phase();var inventory=new PassiveNetwork.Inventory(n.inventory().volume(),n.inventory().moles(),n.inventory().internalEnergy(),com.wormzjl.createcheme.science.fluid.state.SolidInventory.EMPTY);
            nodes.add(new PassiveNetwork.Reservoir(n.id(),n.elevation(),model.state(p.temperature(),p.pressure(),p.liquid(),p.vapor(),p.waterLiquid(),p.waterVapor(),p.hydrocarbonPressure()).withSolids(inventory.solids()),
                    PassiveNetwork.NodeKind.valueOf(n.kind()),inventory));
        }
        return new PassiveNetwork(nodes,saved.pipes().stream().map(p->new PassiveNetwork.Pipe(p.id(),p.first(),p.second(),p.sections(),new FlowControl.Passive(),0,null,PassiveNetwork.PhasePort.BULK,PassiveNetwork.PhasePort.BULK)).toList());
    }
    private static String describe(String label,FluidThermodynamics.State s) {
        return String.format("%s: P %.4f Pa, T %.6f K, V %.9f m3, vapour %.6g m3, water %.9f m3, mass %.6f kg, water vapour %.6g mol, water kappa %.6g 1/Pa",
                label,s.pressure(),s.temperature(),s.volume(),s.vaporVolume(),s.waterVolume(),s.mass(),s.waterVapor(),s.waterCompressibility());
    }
    private void mcp() throws Exception {
        var graph=archivedGraph(archivedIsland("/fluid/mcp-clamped-liquid-full-checkpoint.json"),model);
        for(var node:graph.reservoirs())line(describe("mcp archived node "+node.id()+" "+node.kind(),node.state())+", inventory volume "+node.inventory().volume());
        var refreshed=InventoryEquilibrium.refresh(PassiveNetwork.sizeJunctionHoldups(graph,model),model,()->{});
        line(describe("mcp tank refreshed from its inventory",refreshed.reservoirs().getFirst().state()));
        SolverDiagnostics.reset();SolverDiagnostics.ENABLED=true;
        PassiveIntervalSolver.Result result;
        try{result=new PassiveIntervalSolver(model).solve(graph,1,PassiveIntervalSolver.Settings.defaults(),()->{});}
        finally{SolverDiagnostics.ENABLED=false;}
        for(var a:SolverDiagnostics.attempts())line("  default attempt "+a);
        SolverDiagnostics.reset();
        var refined=new PassiveIntervalSolver(model).solve(graph,1,new PassiveIntervalSolver.Settings(.005,.02,4096),()->{});
        for(int i=0;i<result.averageMassFlows().length;i++)line(String.format("  pipe %d mean flow: default %.6f, refined %.6f kg/s, %.4f %% (bound 0.2 %%); end flow default %.6f, refined %.6f kg/s",
                i,result.averageMassFlows()[i],refined.averageMassFlows()[i],100*(result.averageMassFlows()[i]-refined.averageMassFlows()[i])/refined.averageMassFlows()[i],result.endpointFlows()[i],refined.endpointFlows()[i]));
        line(describe("  default end tank",result.graph().reservoirs().getFirst().state())+", accepted "+result.acceptedSubsteps()+", rejected "+result.rejectedSubsteps());
        line(describe("  refined end tank",refined.graph().reservoirs().getFirst().state())+", accepted "+refined.acceptedSubsteps());
        var after=result.graph();
        var second=new PassiveIntervalSolver(model).solve(after,1,PassiveIntervalSolver.Settings.defaults(),()->{});
        var secondRefined=new PassiveIntervalSolver(model).solve(after,1,new PassiveIntervalSolver.Settings(.005,.02,4096),()->{});
        for(int i=0;i<second.averageMassFlows().length;i++)line(String.format("  option-b second interval pipe %d mean flow: default %.6f, refined %.6f kg/s, %.5f %% (bound 0.2 %%)",i,second.averageMassFlows()[i],secondRefined.averageMassFlows()[i],100*(second.averageMassFlows()[i]-secondRefined.averageMassFlows()[i])/secondRefined.averageMassFlows()[i]));
        var g=graph;
        for(int k=1;k<=12;k++) {
            var r=new PassiveIntervalSolver(model).solve(g,.005,new PassiveIntervalSolver.Settings(.005,.005,4096),()->{});g=r.graph();
            line(String.format("  0.005 s steps, t=%.3f s: ",k*.005)+describe("tank",g.reservoirs().getFirst().state())+", flows "+Arrays.toString(r.averageMassFlows()));
        }
        var continuing=result.graph();for(int i=0;i<200;i++)continuing=new PassiveIntervalSolver(model).solve(continuing,5,PassiveIntervalSolver.Settings.defaults(),()->{}).graph();
        line("  continuing 200 x 5 s: tank nitrogen "+continuing.reservoirs().getFirst().inventory().moles()[MaterialTestBasis.NITROGEN]+" mol");
    }
}
