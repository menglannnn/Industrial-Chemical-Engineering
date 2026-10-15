package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.network.*;
import com.wormzjl.createcheme.science.fluid.state.SolidInventory;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler;
import java.util.*;

/** Display-only reduction of accepted graph/history. No thermo calls, solver work or state mutation. */
public final class PipePresentation {
    private PipePresentation(){}
    public static FluidView.PipeInfo inspect(PhysicalFluidTopology.Device device,PassiveNetwork graph,
            List<PhysicalFluidTopology.View> mappings,List<PipeTransfer> history,double seconds,int components){
        if(!Double.isFinite(seconds)||seconds<0)throw new IllegalArgumentException("Invalid history interval");
        var pipes=new HashMap<Long,PassiveNetwork.Pipe>();for(var p:graph.pipes())pipes.put(p.id(),p);
        var transfers=new HashMap<Long,PipeTransfer>();for(var t:history)transfers.put(t.pipeId(),t);
        int node=-1;for(int i=0;i<graph.reservoirs().size();i++)if(graph.reservoirs().get(i).id()==device.id())node=i;
        boolean junction=device.kind()==TopologyCompiler.Kind.PIPE&&node>=0&&graph.reservoirs().get(node).junction();
        var ids=new LinkedHashSet<Long>();for(var mapping:mappings)if(pipes.containsKey(mapping.pipeId()))ids.add(mapping.pipeId());
        double divisor=junction?1:Math.max(1,ids.size());
        var total=new Sum(components);var metrics=new ArrayList<FluidView.PipeConnection>();boolean reversal=false;
        for(long id:ids){
            var pipe=pipes.get(id);var t=transfers.get(id);
            double mass=0,volume=0;boolean reverse=false;
            if(t!=null){
                mass=t.forward().massKg()+t.reverse().massKg();
                volume=volume(t.forward())+volume(t.reverse());reverse=t.reverse().massKg()>t.forward().massKg();
                reversal|=t.forward().massKg()>0&&t.reverse().massKg()>0;
                if(junction){
                    if(pipe.first()==node)total.add(t.forward(),1);
                    else if(pipe.second()==node)total.add(t.reverse(),1);
                }else{total.add(t.forward(),1/divisor);total.add(t.reverse(),1/divisor);}
            }
            double length=pipe.sections().stream().mapToDouble(PipeResistance.Geometry::length).sum();
            double difference=Math.abs(graph.reservoirs().get(pipe.first()).state().pressure()-graph.reservoirs().get(pipe.second()).state().pressure());
            metrics.add(new FluidView.PipeConnection(id,seconds>0?mass/seconds:0,
                seconds>0?volume/seconds/device.geometry().area():0,difference/length,reverse));
        }
        return new FluidView.PipeInfo(total.finish(),metrics,junction,reversal);
    }
    /** Replace the single-interval speed with the server's previous-five-seconds bulk volume rate. */
    static FluidView.PipeInfo withBulkSpeed(FluidView.PipeInfo info,Map<Long,Double> volumeRates,double area) {
        var connections=info.connections().stream().map(c->new FluidView.PipeConnection(c.pipeId(),
            c.massRateKgPerSecond(),volumeRates.getOrDefault(c.pipeId(),0.0)/area,
            c.pressureDropPascalPerMetre(),c.reverse())).toList();
        return new FluidView.PipeInfo(info.contents(),connections,info.junction(),info.changedDirection());
    }
    private static double volume(PipeTransfer.Stream stream){return Arrays.stream(stream.phaseVolumes()).sum()+stream.solids().volume();}
    private static final class Sum {
        private double mass;
        private final double[][] moles;
        private final double[] volumes=new double[3];
        private final SolidInventory.Accumulator solids=new SolidInventory.Accumulator();
        Sum(int count){moles=new double[3][count];}
        void add(PipeTransfer.Stream stream,double weight){
            mass+=stream.massKg()*weight;var n=stream.phaseMoles();var v=stream.phaseVolumes();
            for(int p=0;p<3;p++){volumes[p]+=v[p]*weight;for(int c=0;c<moles[p].length;c++)moles[p][c]+=n[p][c]*weight;}
            solids.add(stream.solids(),weight);
        }
        PipeTransfer.Stream finish(){return new PipeTransfer.Stream(mass,moles,volumes,solids.finish());}
    }
    /** Ephemeral, server-thread-owned transport history. Never advances process state or survives topology changes. */
    static final class SpeedWindow {
        private static final long TICKS = 100;
        private static final double SECONDS = 5.0;
        private record Span(long start, long end, Map<Long, Double> volumePerTick) {}
        private final ArrayDeque<Span> spans = new ArrayDeque<>();
        private long cachedAt = Long.MIN_VALUE;
        private Map<Long, Double> cached = Map.of();

        void accepted(long start, long end, List<PipeTransfer> transfers) {
            if (end <= start) throw new IllegalArgumentException("Empty transport interval");
            if (!spans.isEmpty() && start < spans.getLast().end())
                throw new IllegalArgumentException("Overlapping transport intervals");
            var rates = new HashMap<Long, Double>();
            for (var transfer : transfers) {
                double volume = volume(transfer.forward()) + volume(transfer.reverse());
                rates.put(transfer.pipeId(), volume / (end - start));
            }
            spans.addLast(new Span(start, end, Map.copyOf(rates)));
            prune(end);
            cachedAt = Long.MIN_VALUE;
        }

        /** Accepted gross volume within the previous 100 online ticks, divided by five seconds.
         * Partial intervals use their recorded interval-average rate; missing/startup history contributes zero. */
        Map<Long, Double> volumeRates(long onlineTick) {
            if (cachedAt == onlineTick) return cached;
            prune(onlineTick);
            var result = new HashMap<Long, Double>();
            for (var span : spans) {
                long overlap = Math.min(onlineTick, span.end()) - Math.max(onlineTick - TICKS, span.start());
                if (overlap > 0) span.volumePerTick().forEach((pipe, rate) ->
                    result.merge(pipe, rate * overlap / SECONDS, Double::sum));
            }
            cached = Map.copyOf(result);
            cachedAt = onlineTick;
            return cached;
        }

        private void prune(long end) {
            while (!spans.isEmpty() && spans.getFirst().end() <= end - TICKS) spans.removeFirst();
        }
    }
}
