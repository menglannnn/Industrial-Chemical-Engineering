package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.thermo.FluidMaterialCatalog;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import java.util.*;
import org.junit.jupiter.api.Test;
import static com.wormzjl.createcheme.runtime.fluid.PhysicalFluidTopology.Direction.*;

/** Temporary probe (gate run 2026-10-16): the persistence GameTest's tank, pipe and generator saved and reloaded. */
class SavedLedgerProbeTest {
    @Test void probe() {
        var w=new EventPathWorld();w.runTo(100);
        int x=-12139802,z=-9858452;
        long tank=w.place(x,-59,z,Kind.RESERVOIR,NORTH,.05);long pipe=w.place(x+1,-59,z,Kind.PIPE,NORTH,.05);
        long generator=w.place(x+2,-59,z,Kind.GENERATOR,NORTH,EventPathWorld.defaultControl(Kind.GENERATOR),FluidDeviceSpec.water(EventPathWorld.CATALOG),.05);
        w.runTo(200);
        var registration=w.ledger.snapshot().active().get(generator);var spec=registration.spec();
        w.registry.submit(List.of(new WorldTopologyLedger.Edit(generator,new WorldTopologyLedger.Registration(registration.device(),new FluidDeviceSpec(spec.volume(),spec.temperature(),200000,spec.composition()),registration.revision()+1))),w.ledger.nextIdentity(),null);
        w.runTo(600);
        var world=w.ledger.snapshot();
        System.out.println("active "+world.active().keySet()+" events "+world.events().size());
        try {
            var bytes=FluidCheckpointCodec.encodeTopology(world);var decoded=FluidCheckpointCodec.decodeTopology(bytes,FluidCheckpointCodec.VERSION);
            System.out.println("codec round trip active "+decoded.active().keySet());
        }catch(RuntimeException failure){System.out.println("codec round trip failed: "+failure);}
        var islands=new ArrayList<FluidCheckpointCodec.IslandEntry>();
        for(var s:w.coordinator.snapshots())islands.add(new FluidCheckpointCodec.IslandEntry(EventPathWorld.DIM,FluidMaterialCatalog.networkPackage(EventPathWorld.CATALOG),1e-9,s));
        try {
            var data=new FluidSavedData(new FluidCheckpointCodec.Checkpoint(islands),world,key->w.model);
            var tag=data.save(new net.minecraft.nbt.CompoundTag(),null);
            var restored=FluidSavedData.load(tag,key->w.model,data.store().reopen());
            System.out.println("saved data round trip active "+restored.world().map(s->s.active().keySet()));
        }catch(RuntimeException failure){System.out.println("saved data round trip failed: "+failure);failure.printStackTrace(System.out);}
    }
}
