package com.wormzjl.icheme.science.fluid.network;

import com.google.gson.*;
import com.wormzjl.icheme.science.material.MaterialCatalog;
import com.wormzjl.icheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.icheme.science.fluid.state.SolidInventory;
import com.wormzjl.icheme.runtime.fluid.FluidPresetCatalog;
import java.nio.file.*;
import java.util.*;

/** Bounded replay of the frozen current-format numeric graph. No Minecraft or save writer. */
public final class RateHoldAudit {
    static double[] array(JsonElement e){var a=e.getAsJsonArray();double[] out=new double[a.size()];for(int i=0;i<out.length;i++)out[i]=a.get(i).getAsDouble();return out;}
    static PassiveNetwork graph(JsonObject data,FluidThermodynamics model,double rate) {
        var nodes=new ArrayList<PassiveNetwork.Reservoir>();
        for(var e:data.getAsJsonArray("nodes")) {
            var n=e.getAsJsonObject();if(!n.getAsJsonArray("solids").isEmpty())throw new IllegalArgumentException("This case must have no solids");
            var state=model.state(n.get("temperature").getAsDouble(),n.get("pressure").getAsDouble(),array(n.get("liquid")),array(n.get("vapor")),n.get("waterLiquid").getAsDouble(),n.get("waterVapor").getAsDouble(),n.get("hydrocarbonPressure").getAsDouble());
            var inv=new PassiveNetwork.Inventory(n.get("capacity").getAsDouble(),array(n.get("moles")),n.get("energy").getAsDouble(),SolidInventory.EMPTY,n.get("height").getAsDouble(),n.get("open").getAsBoolean());
            nodes.add(new PassiveNetwork.Reservoir(n.get("id").getAsLong(),n.get("elevation").getAsDouble(),state,PassiveNetwork.NodeKind.valueOf(n.get("kind").getAsString()),inv));
        }
        var pipes=new ArrayList<PassiveNetwork.Pipe>();
        for(var e:data.getAsJsonArray("pipes")) {
            var p=e.getAsJsonObject();var sections=new ArrayList<PipeResistance.Geometry>();for(var s:p.getAsJsonArray("sections")){var v=array(s);sections.add(new PipeResistance.Geometry(v[0],v[1],v[2],v[3]));}
            var c=p.getAsJsonObject("control");FlowControl control=switch(c.get("kind").getAsString()) {
                case "passive"->new FlowControl.Passive();case "pump"->new FlowControl.Pump(rate,c.get("limit").getAsDouble(),c.get("enabled").getAsBoolean());
                case "compressor"->new FlowControl.Compressor(c.get("volumeFlow").getAsDouble(),c.get("limit").getAsDouble(),c.get("enabled").getAsBoolean());
                case "valve"->new FlowControl.PressureValve(c.get("pressure").getAsDouble());default->throw new IllegalArgumentException("Unknown control");};
            if(!p.get("filter").isJsonNull())throw new IllegalArgumentException("This case must have no filter");var ports=p.getAsJsonArray("ports");
            pipes.add(new PassiveNetwork.Pipe(p.get("id").getAsLong(),p.get("first").getAsInt(),p.get("second").getAsInt(),sections,control,p.get("blocked").getAsInt(),null,PassiveNetwork.PhasePort.values()[ports.get(0).getAsInt()],PassiveNetwork.PhasePort.values()[ports.get(1).getAsInt()]));
        }
        return new PassiveNetwork(nodes,pipes);
    }
    public static void main(String[] args)throws Exception {
        var root=JsonParser.parseString(Files.readString(Path.of(args[0]))).getAsJsonObject();long island=args.length>3?Long.parseLong(args[3]):94;
        var entry=root.getAsJsonArray("islands").asList().stream().map(JsonElement::getAsJsonObject).filter(e->e.getAsJsonObject("header").get("id").getAsLong()==island).findFirst().orElseThrow();
        var catalog=MaterialCatalog.bundled();var model=FluidThermodynamics.forNetwork(catalog,"icheme:tjl20_methane_nitrogen",100,1e-6);
        double rate=Double.parseDouble(args[1]),dt=Double.parseDouble(args[2]);var graph=graph(entry.getAsJsonObject("graph"),model,rate);
        var preset=FluidPresetCatalog.resolve(catalog).stream().filter(p->p.name().equals("WTI Light Export")).findFirst().orElseThrow();
        for(var d:root.getAsJsonObject("world").getAsJsonArray("active")) {
            var n=d.getAsJsonObject();if(n.get("id").getAsLong()!=40)continue;var c=array(n.getAsJsonObject("spec").get("composition"));var w=preset.moleFractions();double diff=0;for(int i=0;i<c.length;i++)diff=Math.max(diff,Math.abs(c[i]-w[i]));System.out.println("WTI_PRESET_MAX_DIFFERENCE="+diff);
        }
        for(var n:graph.reservoirs()) {
            var s=n.state();System.out.printf(Locale.ROOT,"STATE id=%d kind=%s P=%.12g T=%.12g rho=%.12g volume=%.12g cap=%.12g oilV=%.12g gasV=%.12g oilMol=%.12g gasMol=%.12g%n",n.id(),n.kind(),s.pressure(),s.temperature(),s.mass()/s.volume(),s.volume(),n.inventory().volume(),s.liquidVolume(),s.vaporVolume(),Arrays.stream(s.liquid()).sum(),Arrays.stream(s.vapor()).sum());
        }
        int[] operations={0};Runnable checkpoint=()->{if(++operations[0]>2_000_000)throw new IllegalStateException("Offline diagnostic operation limit");};
        try {
            if(Boolean.getBoolean("wti.repeatPort")){
                var step=new PassiveStepSolver(model);
                for(int i=0;i<2;i++){var value=step.portRate(graph,checkpoint);System.out.println("REPEAT_PORT call="+i+" result="+(value==null?"FAILED":value.modes()+" flows="+Arrays.toString(value.massFlows())));}
                return;
            }
            if(Boolean.getBoolean("wti.rateOnly")||Boolean.getBoolean("wti.singleStep")||Boolean.getBoolean("wti.portRate")) {
                var result=Boolean.getBoolean("wti.portRate")?new PassiveStepSolver(model).portRate(graph,checkpoint):Boolean.getBoolean("wti.rateOnly")?new PassiveStepSolver(model).solveRate(graph,checkpoint):new PassiveStepSolver(model).solve(graph,dt,checkpoint);
                if(result==null){System.out.println("RESULT PORT_RATE_NULL");return;}
                System.out.println("RESULT STEP_SUCCESS modes="+result.modes()+" massFlows="+Arrays.toString(result.massFlows())+" heads="+Arrays.toString(result.devicePressureChanges()));
                for(int i=0;i<result.states().size();i++){var s=result.states().get(i);System.out.printf(Locale.ROOT,"STEP_END id=%d P=%.12g T=%.12g oilV=%.12g gasV=%.12g rho=%.12g%n",graph.reservoirs().get(i).id(),s.pressure(),s.temperature(),s.liquidVolume(),s.vaporVolume(),s.mass()/s.volume());}
            } else {
                var solver=new PassiveIntervalSolver(model);var result=solver.solve(graph,dt,PassiveIntervalSolver.Settings.defaults(),checkpoint);
                System.out.println("RESULT INTERVAL_SUCCESS rate="+rate+" modes="+result.endpointModes()+" massFlows="+Arrays.toString(result.averageMassFlows())+" heads="+Arrays.toString(result.endpointHeads())+" accepted="+result.acceptedSubsteps()+" rejected="+result.rejectedSubsteps());
                for(var n:result.graph().reservoirs())System.out.printf(Locale.ROOT,"END id=%d P=%.12g T=%.12g oilV=%.12g gasV=%.12g%n",n.id(),n.state().pressure(),n.state().temperature(),n.state().liquidVolume(),n.state().vaporVolume());
                for(int i=0;i<(args.length>4?Integer.parseInt(args[4]):0);i++){
                    graph=result.graph();solver.replayStart(graph,result);result=solver.solve(graph,dt,PassiveIntervalSolver.Settings.defaults(),checkpoint);
                    System.out.println("FOLLOW index="+i+" modes="+result.endpointModes()+" flows="+Arrays.toString(result.averageMassFlows())+" accepted="+result.acceptedSubsteps()+" rejected="+result.rejectedSubsteps());
                }
            }
        }catch(RuntimeException failure){System.out.println("RESULT FAILURE rate="+rate+" dt="+dt+" message="+failure);}
        System.out.println("OPERATIONS="+operations[0]);
    }
}
