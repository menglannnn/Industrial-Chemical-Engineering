package com.wormzjl.createcheme.runtime.fluid;

import com.google.gson.*;
import com.wormzjl.createcheme.fluid.support.FluidTestSupport;
import com.wormzjl.createcheme.science.fluid.network.*;
import com.wormzjl.createcheme.science.fluid.solver.PhaseLayout;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.createcheme.science.material.MaterialTestBasis;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * M2PROBE (D27), never committed: writes the fresh McpGameplay clamped liquid-full fixture under the current model. Each
 * node of the 2026-09-16 capture's graph is restated at its captured temperature and pressure: it keeps its minor
 * components, and its major component fills its inventory volume (flashTP, secant on the major amount). The inventory is
 * the flash state's (amounts, internal energy) at the node's volume; the phase is that state. Pipes are copied as captured.
 * Output: build/m2-logs/41-mcp-fixture/mcp-clamped-liquid-full-checkpoint.json and 41-mcp-fixture.txt.
 */
class M2McpFixtureProbeTest {
    private final StringBuilder out=new StringBuilder();
    private void line(String s){out.append(s).append(System.lineSeparator());System.out.println(s);}
    private static JsonArray array(double[] values){var a=new JsonArray();for(double v:values)a.add(v);return a;}
    private static double[] doubles(JsonArray a){var v=new double[a.size()];for(int i=0;i<v.length;i++)v[i]=a.get(i).getAsDouble();return v;}
    private static String describe(String label,FluidThermodynamics.State s) {
        return String.format(Locale.ROOT,"%s: P %.4f Pa, T %.6f K, V %.12f m3, vapour %.6g m3, water %.9f m3, mass %.6f kg, water liquid %.6f mol, water vapour %.6g mol, N2 vapour %.10g mol, pHC %.4f Pa, kappa %.6g 1/Pa",
                label,s.pressure(),s.temperature(),s.volume(),s.vaporVolume(),s.waterVolume(),s.mass(),s.waterLiquid(),s.waterVapor(),s.vaporView()[MaterialTestBasis.NITROGEN],s.hydrocarbonPartialPressure(),s.waterCompressibility());
    }

    @Test void writeFreshFixture() throws Exception {
        var outDir=Path.of("build/m2-logs/41-mcp-fixture");Files.createDirectories(outDir);
        try {
            var model=FluidTestSupport.networkModel();
            JsonObject old;
            try(var input=Objects.requireNonNull(getClass().getResourceAsStream("/fluid/mcp-clamped-liquid-full-checkpoint.json"))) {
                old=JsonParser.parseString(new String(input.readAllBytes(),StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("islands").get(0).getAsJsonObject().getAsJsonObject("graph");
            }
            var nodes=new JsonArray();
            for(var element:old.getAsJsonArray("nodes")) {
                var node=element.getAsJsonObject();var inventory=node.getAsJsonObject("inventory");var phase=node.getAsJsonObject("phase");
                double t=phase.get("temperature").getAsDouble(),p=phase.get("pressure").getAsDouble(),capacity=inventory.get("volume").getAsDouble();
                double[] n=doubles(inventory.getAsJsonArray("moles"));double[] archived=n.clone();
                int major=0;for(int c=1;c<n.length;c++)if(n[c]>n[major])major=c;
                var oldState=model.flashTP(t,p,n,()->{});
                line(describe("node "+node.get("id")+" "+node.get("kind").getAsString()+" archived amounts at its T and P",oldState)+", capacity "+capacity);
                // Secant on the major amount: at fixed T and P the volume is linear in it while no phase appears or vanishes.
                double x0=n[major],v0=oldState.volume(),x1=x0*capacity/v0;
                FluidThermodynamics.State state=null;
                for(int k=0;k<20;k++) {
                    n[major]=x1;state=model.flashTP(t,p,n,()->{});double v1=state.volume();
                    line(String.format(Locale.ROOT,"  iteration %d: major %.12f mol, V %.15f m3",k,x1,v1));
                    if(Math.abs(v1-capacity)<=1e-14*capacity||v1==v0)break;
                    double x2=x1+(capacity-v1)*(x1-x0)/(v1-v0);x0=x1;v0=v1;x1=x2;
                }
                double[] moles=PhaseLayout.totalAmounts(state);
                for(int c=0;c<moles.length;c++)if(c!=major&&moles[c]!=archived[c])line("  minor component "+c+" moved: "+archived[c]+" -> "+moles[c]);
                line(describe("  restated",state)+String.format(Locale.ROOT,", major %d: %.9f -> %.9f mol (%+.6f %%), U %.6f J (archived %.6f J)",
                        major,archived[major],moles[major],100*(moles[major]/archived[major]-1),state.internalEnergy(),inventory.get("internalEnergy").getAsDouble()));
                var fresh=new JsonObject();
                fresh.add("id",node.get("id"));fresh.add("elevation",node.get("elevation"));fresh.add("kind",node.get("kind"));
                var freshInventory=new JsonObject();freshInventory.addProperty("volume",capacity);freshInventory.add("moles",array(moles));freshInventory.addProperty("internalEnergy",state.internalEnergy());
                var freshPhase=new JsonObject();
                freshPhase.addProperty("temperature",state.temperature());freshPhase.addProperty("pressure",state.pressure());
                freshPhase.add("liquid",array(state.liquid()));freshPhase.add("vapor",array(state.vapor()));
                freshPhase.addProperty("waterLiquid",state.waterLiquid());freshPhase.addProperty("waterVapor",state.waterVapor());
                freshPhase.addProperty("hydrocarbonPressure",state.hydrocarbonPartialPressure());
                fresh.add("inventory",freshInventory);fresh.add("phase",freshPhase);nodes.add(fresh);
            }
            var graph=new JsonObject();graph.add("nodes",nodes);graph.add("pipes",old.getAsJsonArray("pipes").deepCopy());
            var island=new JsonObject();island.add("graph",graph);
            var islands=new JsonArray();islands.add(island);
            var root=new JsonObject();root.add("islands",islands);
            String json=new GsonBuilder().setPrettyPrinting().create().toJson(root)+"\n";
            Files.writeString(outDir.resolve("mcp-clamped-liquid-full-checkpoint.json"),json);
            line("written "+json.length()+" characters");

            // Read back exactly as McpGameplayRegressionTest#archivedGraph does, and close the tank from its inventory.
            var saved=JsonParser.parseString(json).getAsJsonObject().getAsJsonArray("islands").get(0).getAsJsonObject().getAsJsonObject("graph");
            var list=new ArrayList<PassiveNetwork.Reservoir>();
            for(var element:saved.getAsJsonArray("nodes")) {
                var node=element.getAsJsonObject();var inventory=node.getAsJsonObject("inventory");var phase=node.getAsJsonObject("phase");
                var inv=new PassiveNetwork.Inventory(inventory.get("volume").getAsDouble(),doubles(inventory.getAsJsonArray("moles")),inventory.get("internalEnergy").getAsDouble(),com.wormzjl.createcheme.science.fluid.state.SolidInventory.EMPTY);
                var state=model.state(phase.get("temperature").getAsDouble(),phase.get("pressure").getAsDouble(),doubles(phase.getAsJsonArray("liquid")),doubles(phase.getAsJsonArray("vapor")),
                        phase.get("waterLiquid").getAsDouble(),phase.get("waterVapor").getAsDouble(),phase.get("hydrocarbonPressure").getAsDouble()).withSolids(inv.solids());
                list.add(new PassiveNetwork.Reservoir(node.get("id").getAsLong(),node.get("elevation").getAsDouble(),state,PassiveNetwork.NodeKind.valueOf(node.get("kind").getAsString()),inv));
            }
            var pipes=new ArrayList<PassiveNetwork.Pipe>();
            for(var element:saved.getAsJsonArray("pipes")) {
                var pipe=element.getAsJsonObject();var sections=new ArrayList<PipeResistance.Geometry>();
                for(var s:pipe.getAsJsonArray("sections")){var o=s.getAsJsonObject();sections.add(new PipeResistance.Geometry(o.get("length").getAsDouble(),o.get("diameter").getAsDouble(),o.get("roughness").getAsDouble(),o.get("minorLoss").getAsDouble()));}
                pipes.add(new PassiveNetwork.Pipe(pipe.get("id").getAsLong(),pipe.get("first").getAsInt(),pipe.get("second").getAsInt(),sections,new FlowControl.Passive(),0,null,PassiveNetwork.PhasePort.BULK,PassiveNetwork.PhasePort.BULK));
                line("pipe "+pipe.get("id").getAsLong()+" "+pipe.get("first")+"->"+pipe.get("second"));
            }
            var network=new PassiveNetwork(list,pipes);
            var refreshed=InventoryEquilibrium.refresh(PassiveNetwork.sizeJunctionHoldups(network,model),model,()->{});
            line(describe("tank as read",network.reservoirs().getFirst().state()));
            line(describe("tank refreshed from its inventory",refreshed.reservoirs().getFirst().state()));
            var result=new PassiveIntervalSolver(model).solve(network,1,PassiveIntervalSolver.Settings.defaults(),()->{});
            var refined=new PassiveIntervalSolver(model).solve(network,1,new PassiveIntervalSolver.Settings(.005,.02,4096),()->{});
            for(int i=0;i<result.averageMassFlows().length;i++)line(String.format(Locale.ROOT,"pipe %d mean flow: default %.9f, refined %.9f kg/s, %.6f %% (bound 0.2 %%)",
                    i,result.averageMassFlows()[i],refined.averageMassFlows()[i],100*(result.averageMassFlows()[i]-refined.averageMassFlows()[i])/refined.averageMassFlows()[i]));
            line(describe("default end tank",result.graph().reservoirs().getFirst().state())+", accepted "+result.acceptedSubsteps()+", rejected "+result.rejectedSubsteps());
            line(describe("refined end tank",refined.graph().reservoirs().getFirst().state())+", accepted "+refined.acceptedSubsteps()+", rejected "+refined.rejectedSubsteps());
            var continuing=result.graph();
            for(int i=1;i<=200;i++) {
                continuing=new PassiveIntervalSolver(model).solve(continuing,5,PassiveIntervalSolver.Settings.defaults(),()->{}).graph();
                if(i%40==0)line(String.format(Locale.ROOT,"continuing %d x 5 s: tank nitrogen %.6g mol, ",i,continuing.reservoirs().getFirst().inventory().moles()[MaterialTestBasis.NITROGEN])+describe("tank",continuing.reservoirs().getFirst().state()));
            }
        } finally {
            Files.writeString(outDir.resolve("41-mcp-fixture.txt"),out.toString());
        }
    }
}
