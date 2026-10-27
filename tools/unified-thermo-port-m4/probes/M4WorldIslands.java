package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.createcheme.science.material.PilotCryogenicTestCatalog;
import java.nio.file.Path;

/**
 * M4 GUI-check probe (never committed): reads a dev world's fluid checkpoint (its data folder: createcheme_fluid_core.dat
 * and createcheme_fluid/) with the pilot model and prints every saved island (status, clock, certificate, nodes with
 * pressure, temperature and solids, connections with their solid closure mask) and the ledger's active devices.
 * Usage: java ... com.wormzjl.createcheme.runtime.fluid.M4WorldIslands <data folder>
 */
public final class M4WorldIslands {
    public static void main(String[] args) throws Exception {
        var model=FluidThermodynamics.forNetwork(PilotCryogenicTestCatalog.catalog(),PilotCryogenicTestCatalog.PACKAGE_ID);
        var loaded=FluidCheckpointStore.directory(Path.of(args[0]),Runnable::run,false).read(key->model).orElseThrow();
        System.out.println("islands="+loaded.checkpoint().islands().size());
        for(var entry:loaded.checkpoint().islands()) {
            var s=entry.snapshot();
            System.out.println("island "+s.id()+" rev="+s.revision()+" committed="+s.clock().committedTick()+" certified="+s.certificate().isPresent()+" status="+s.status());
            for(var r:s.graph().reservoirs()) {
                double solids=0;for(var p:r.inventory().solids().populations())solids+=p.massKg();
                System.out.printf("  node %d %s p=%.1f Pa T=%.3f K solids=%.6g kg pops=%d%n",r.id(),r.kind(),r.state().pressure(),r.state().temperature(),solids,r.inventory().solids().populations().size());
            }
            for(var p:s.graph().pipes())System.out.println("  pipe "+p.id()+" "+s.graph().reservoirs().get(p.first()).id()+"->"+s.graph().reservoirs().get(p.second()).id()+" blocked="+p.blockedDirections()+" control="+p.control().getClass().getSimpleName());
        }
        loaded.world().ifPresent(w->{
            System.out.println("ledger onlineTick="+w.onlineTick()+" active="+w.active().size());
            w.active().values().stream().sorted(java.util.Comparator.comparingLong(r->r.device().id())).forEach(r->System.out.println("  device "+r.device().id()+" "+r.device().kind()+" at "+r.device().position()));
        });
    }
}
