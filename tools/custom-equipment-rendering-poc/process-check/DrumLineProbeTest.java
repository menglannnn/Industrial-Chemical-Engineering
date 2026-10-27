package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.network.PassiveNetwork;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import java.util.*;
import org.junit.jupiter.api.Test;
import static com.wormzjl.createcheme.runtime.fluid.PhysicalFluidTopology.Direction.*;

/** Temporary probe (in-game check 2026-10-16): the check scene's drum and its north line through a pump and two valves. */
class DrumLineProbeTest {
    private static PhysicalFluidTopology.Position at(int x,int y,int z){return new PhysicalFluidTopology.Position(EventPathWorld.DIM,x,y,z);}

    @Test void probe() {
        var w=new EventPathWorld();w.runTo(100);
        long drum=w.placeVessel(at(200,-59,12),List.of(
                new PhysicalFluidTopology.Port(at(200,-52,12),UP,PassiveNetwork.PhasePort.VAPOR),
                new PhysicalFluidTopology.Port(at(200,-58,10),NORTH,PassiveNetwork.PhasePort.LIQUID),
                new PhysicalFluidTopology.Port(at(198,-54,12),WEST,PassiveNetwork.PhasePort.BULK)),50,6);
        w.runTo(110);
        var line=new LinkedHashMap<String,Long>();
        line.put("pipe 9",w.place(200,-58,9,Kind.PIPE,NORTH,.05));w.runTo(112);
        line.put("pump 8",w.place(200,-58,8,Kind.PUMP,NORTH,.05));w.runTo(114);
        line.put("valve 7",w.place(200,-58,7,Kind.VALVE,NORTH,.05));w.runTo(116);
        line.put("pipe 6",w.place(200,-58,6,Kind.PIPE,NORTH,.05));w.runTo(118);
        line.put("pipe up",w.place(200,-57,6,Kind.PIPE,UP,.05));w.runTo(120);
        line.put("valve up",w.place(200,-56,6,Kind.VALVE,UP,.05));w.runTo(122);
        line.put("pipe top",w.place(200,-55,6,Kind.PIPE,UP,.05));w.runTo(124);
        line.put("void",w.place(200,-54,6,Kind.VOID,NORTH,.05));w.runTo(400);
        var island=w.registry.owner(drum);
        System.out.println("drum island "+island+" members "+(island==null?null:w.registry.members(island)));
        for(var e:line.entrySet()){var owner=w.registry.owner(e.getValue());System.out.println(e.getKey()+" id "+e.getValue()+" island "+owner+" members "+(owner==null?null:w.registry.members(owner)));}
        System.out.println("tank faces "+w.registry.tankFaces(drum));
    }

    /** The same line on a one-block tank, and the drum line without its actuators. */
    @Test void controls() {
        for(int variant=0;variant<2;variant++) {
            var w=new EventPathWorld();w.runTo(100);
            long tank=variant==0?w.place(200,-58,10,Kind.RESERVOIR,NORTH,.05)
                    :w.placeVessel(at(200,-59,12),List.of(new PhysicalFluidTopology.Port(at(200,-58,10),NORTH,PassiveNetwork.PhasePort.LIQUID)),50,6);
            w.runTo(110);
            var ids=new ArrayList<Long>();
            ids.add(w.place(200,-58,9,Kind.PIPE,NORTH,.05));
            ids.add(w.place(200,-58,8,variant==0?Kind.PUMP:Kind.PIPE,NORTH,.05));
            ids.add(w.place(200,-58,7,variant==0?Kind.VALVE:Kind.PIPE,NORTH,.05));
            ids.add(w.place(200,-58,6,Kind.VOID,NORTH,.05));w.runTo(400);
            System.out.println((variant==0?"one-block tank with pump and valve: ":"drum, pipes only: ")+"tank island "+w.registry.owner(tank)+" "+(w.registry.owner(tank)==null?null:w.registry.members(w.registry.owner(tank))));
            for(long id:ids)System.out.println("  "+id+" island "+w.registry.owner(id));
        }
    }
}
