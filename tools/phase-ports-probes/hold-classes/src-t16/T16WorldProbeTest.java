package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.network.NetworkReduction;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import com.wormzjl.createcheme.science.material.MaterialTestBasis;
import java.util.*;
import org.junit.jupiter.api.Test;
import static com.wormzjl.createcheme.runtime.fluid.PhysicalFluidTopology.Direction.*;

/**
 * t16 probe (tools only; needs src/scratch-t16-switch.patch): candidate gate worlds run in the modes exact (the rest test
 * of D19) and on (the gauge), printing the first hold or the end status and the tank states. -Dt16.worlds=a,b,c.
 */
class T16WorldProbeTest {
    static final FluidDeviceSpec WATER=EventPathWorld.pure(MaterialTestBasis.NETWORK,298.15,101325);
    static long gen(EventPathWorld w,int x,int y,int z){return w.place(x,y,z,Kind.GENERATOR,NORTH,EventPathWorld.defaultControl(Kind.GENERATOR),WATER,.05);}
    static void pipe(EventPathWorld w,int x,int y,int z){w.place(x,y,z,Kind.PIPE,NORTH,.05);}
    /** Defaults seed 138 minimized (8 blocks): a generator 2 m up feeds a junction between two tanks on equal legs. With
     * {@code second}, a mirror generator on the junction's other side (two identical generators on one junction). */
    static long world138(EventPathWorld w,boolean second) {
        w.runTo(236);
        pipe(w,-2,1,-1);pipe(w,-1,0,-1);pipe(w,0,0,-1);long g=gen(w,-2,2,-1);w.place(0,0,0,Kind.RESERVOIR,NORTH,.05);pipe(w,0,0,-2);
        w.place(0,0,-3,Kind.RESERVOIR,NORTH,.05);pipe(w,-2,0,-1);
        if(second){pipe(w,2,1,-1);pipe(w,2,0,-1);pipe(w,1,0,-1);gen(w,2,2,-1);}
        return g;
    }
    static void run(String name) {
        for(String mode:List.of("exact","on")) {
            NetworkReduction.SCRATCH_T16.set(mode);
            try {
                var w=new EventPathWorld();long g=switch(name){case "138"->world138(w,false);case "138two"->world138(w,true);default->throw new IllegalArgumentException(name);};
                long end=w.ledger.onlineTick()+12000;while(w.ledger.onlineTick()<end&&w.held.isEmpty())w.tick();
                var s=w.coordinator.snapshot(w.registry.owner(g));var b=new StringBuilder();
                for(var n:s.graph().reservoirs())if(n.kind()==com.wormzjl.createcheme.science.fluid.network.PassiveNetwork.NodeKind.RESERVOIR)b.append(" tank ").append(n.id()).append(" P=").append(n.state().pressure()).append(" water=").append(n.state().waterVolume());
                System.out.println("T16WORLD "+name+" mode="+mode+" "+(w.held.isEmpty()?"clean at "+w.ledger.onlineTick()+" status="+s.status():"HELD "+w.held.getFirst())+b);
            } finally{NetworkReduction.SCRATCH_T16.remove();}
        }
    }
    @Test void worlds(){for(var name:System.getProperty("t16.worlds","138,138two").split(","))run(name);}
}
