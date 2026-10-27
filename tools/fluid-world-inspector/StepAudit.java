package com.wormzjl.icheme.science.fluid.network;

import com.google.gson.*;
import com.wormzjl.icheme.science.material.MaterialCatalog;
import com.wormzjl.icheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.icheme.science.fluid.state.SolidInventory;
import java.nio.file.*;
import java.util.*;

/** Bounded, offline diagnostic comparison on copied numeric data; never mutates the world or runs a test suite. */
public final class StepAudit {
    public static void main(String[] args)throws Exception{
        var root=JsonParser.parseString(Files.readString(Path.of(args[0]))).getAsJsonObject();
        var model=FluidThermodynamics.forNetwork(MaterialCatalog.bundled(),"icheme:tjl20_methane_nitrogen",100,1e-6);
        JsonObject data=null,last=null;for(var e:root.getAsJsonArray("islands"))if(e.getAsJsonObject().getAsJsonObject("header").get("id").getAsLong()==99){data=e.getAsJsonObject().getAsJsonObject("graph");last=e.getAsJsonObject().getAsJsonObject("lastResult");}
        if(data==null)throw new IllegalArgumentException("No island 99");
        var nodes=new ArrayList<PassiveNetwork.Reservoir>();
        for(var e:data.getAsJsonArray("nodes")){
            var n=e.getAsJsonObject();if(!n.getAsJsonArray("solids").isEmpty())throw new IllegalArgumentException("This inspection case has no solids");
            var s=model.state(n.get("temperature").getAsDouble(),n.get("pressure").getAsDouble(),PropertyAudit.array(n.get("liquid")),PropertyAudit.array(n.get("vapor")),n.get("waterLiquid").getAsDouble(),n.get("waterVapor").getAsDouble(),n.get("hydrocarbonPressure").getAsDouble());
            var inv=new PassiveNetwork.Inventory(n.get("capacity").getAsDouble(),PropertyAudit.array(n.get("moles")),n.get("energy").getAsDouble(),SolidInventory.EMPTY,n.get("height").getAsDouble(),n.get("open").getAsBoolean());
            nodes.add(new PassiveNetwork.Reservoir(n.get("id").getAsLong(),n.get("elevation").getAsDouble(),s,PassiveNetwork.NodeKind.valueOf(n.get("kind").getAsString()),inv));
        }
        var pipes=new ArrayList<PassiveNetwork.Pipe>();
        for(var e:data.getAsJsonArray("pipes")){
            var p=e.getAsJsonObject();var sections=new ArrayList<PipeResistance.Geometry>();
            for(var sec:p.getAsJsonArray("sections")){var g=PropertyAudit.array(sec);sections.add(new PipeResistance.Geometry(g[0],g[1],g[2],g[3]));}
            var c=p.getAsJsonObject("control");FlowControl control=switch(c.get("kind").getAsString()){
                case "passive"->new FlowControl.Passive();case "valve"->new FlowControl.PressureValve(c.get("pressure").getAsDouble());default->throw new IllegalArgumentException("No mover in this case");};
            var ports=p.getAsJsonArray("ports");if(!p.get("filter").isJsonNull())throw new IllegalArgumentException("No filter in this case");
            pipes.add(new PassiveNetwork.Pipe(p.get("id").getAsLong(),p.get("first").getAsInt(),p.get("second").getAsInt(),sections,control,p.get("blocked").getAsInt(),null,PassiveNetwork.PhasePort.values()[ports.get(0).getAsInt()],PassiveNetwork.PhasePort.values()[ports.get(1).getAsInt()]));
        }
        int[] operations={0};Runnable checkpoint=()->{if(++operations[0]>500_000)throw new IllegalStateException("Offline diagnostic operation limit");};
        for(boolean equilibrated:Boolean.getBoolean("fluid.audit.carryValve")?new boolean[]{false}:new boolean[]{false,true}){
            operations[0]=0;var point=new ArrayList<>(nodes);
            if(equilibrated)for(int i=0;i<point.size();i++)if(point.get(i).id()==30){
                var node=point.get(i);var s=node.state();var method=FluidThermodynamics.class.getDeclaredMethod("equilibriumUV",double.class,double.class,double.class,double.class,double[].class,Runnable.class);method.setAccessible(true);
                var own=(FluidThermodynamics.State)method.invoke(model,s.temperature(),s.pressure(),node.inventory().internalEnergy(),s.volume(),node.inventory().moles(),checkpoint);
                if(own==null)throw new IllegalStateException("No conserved UV answer");
                point.set(i,new PassiveNetwork.Reservoir(node.id(),node.elevation(),own,node.kind(),node.inventory()));
            }
            var graph=new PassiveNetwork(point,pipes);double dt=Double.parseDouble(args[1]);
            try{
                if(args.length>2&&args[2].equals("interval")){
                    var solver=new PassiveIntervalSolver(model);solver.replayStart(graph,committed(new PassiveNetwork(nodes,pipes),last));
                    var result=solver.solve(graph,dt,new PassiveIntervalSolver.Settings(Math.min(1,dt),20,128),checkpoint);
                    System.out.println("CASE INTERVAL equilibrated="+equilibrated+" dt="+dt+" SUCCESS modes="+result.endpointModes()+" accepted="+result.acceptedSubsteps()+" rejected="+result.rejectedSubsteps()+" flows="+Arrays.toString(result.averageMassFlows()));
                    printPhases(model,result,0);
                    int following=args.length>3?Integer.parseInt(args[3]):0;
                    for(int interval=1;interval<=following;interval++){
                        operations[0]=0;graph=result.graph();solver.replayStart(graph,result);
                        result=solver.solve(graph,dt,new PassiveIntervalSolver.Settings(Math.min(1,dt),20,128),checkpoint);
                        System.out.println("FOLLOW interval="+interval+" accepted="+result.acceptedSubsteps()+" rejected="+result.rejectedSubsteps()+" modes="+result.endpointModes()+" flows="+Arrays.toString(result.averageMassFlows()));
                        printPhases(model,result,interval);
                    }
                }else{
                    var solver=new PassiveStepSolver(model);solver.replayStart(graph,committed(new PassiveNetwork(nodes,pipes),last));
                    var result=solver.solve(graph,dt,checkpoint);
                    System.out.println("CASE equilibrated="+equilibrated+" dt="+dt+" SUCCESS residual="+result.numerical().residualNorm()+" modes="+result.modes()+" flows="+Arrays.toString(result.massFlows()));
                }
            }catch(RuntimeException failure){System.out.println("CASE equilibrated="+equilibrated+" dt="+dt+" FAILURE "+failure);}
        }
    }
    static PassiveIntervalSolver.Result committed(PassiveNetwork graph,JsonObject last){
        var modes=new ArrayList<FlowControl.Mode>();for(var e:last.getAsJsonArray("modes"))modes.add(FlowControl.Mode.values()[e.getAsInt()]);
        return new PassiveIntervalSolver.Result(graph,last.get("seconds").getAsDouble(),PropertyAudit.array(last.get("massFlows")),last.get("accepted").getAsInt(),last.get("rejected").getAsInt(),last.get("pumpWork").getAsDouble(),List.of(),Map.of(),modes,PropertyAudit.array(last.get("heads")),PropertyAudit.array(last.get("endpointFlows")),List.of());
    }
    static void printPhases(FluidThermodynamics model,PassiveIntervalSolver.Result result,int interval){
        var weights=model.molecularWeights();
        for(var node:result.graph().reservoirs())if(node.id()==30){var s=node.state();System.out.printf(Locale.ROOT,"END interval=%d P=%.9g T=%.9g liquidVolume=%.9g gasVolume=%.9g endpointFlows=%s%n",interval,s.pressure(),s.temperature(),s.liquidVolume()+s.waterVolume(),s.vaporVolume(),Arrays.toString(result.endpointFlows()));}
        for(var transfer:result.pipeTransfers())if(transfer.pipeId()==7798601278550083437L){
            var phases=transfer.forward().phaseMoles();double liquid=0,gas=0;
            for(int c=0;c<weights.length;c++){liquid+=(phases[0][c]+phases[1][c])*weights[c];gas+=phases[2][c]*weights[c];}
            System.out.printf(Locale.ROOT,"BOTTOM interval=%d liquidKgPerSec=%.9g gasKgPerSec=%.9g%n",interval,liquid/result.advancedSeconds(),gas/result.advancedSeconds());
        }
    }
}
