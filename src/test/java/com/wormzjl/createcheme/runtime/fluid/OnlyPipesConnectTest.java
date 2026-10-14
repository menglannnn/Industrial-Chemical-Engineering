package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.fluid.support.FluidTestSupport;
import com.wormzjl.createcheme.science.fluid.network.*;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import java.util.*;
import org.junit.jupiter.api.Test;
import static com.wormzjl.createcheme.runtime.fluid.PhysicalFluidTopology.Direction.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Owner decision D20 (phase-ports batch, PHASE_PORTS_REVIEW.md "Follow-up rules"): only pipes connect. Two pieces of
 * equipment side by side - a tank, generator, void, pump, compressor, valve or filter against another - exchange nothing;
 * with a pipe block between them they are one line. Step 1 of the network reduction stage, in the compile and in the
 * registry's walks.
 */
class OnlyPipesConnectTest {
    private final FluidThermodynamics model=FluidTestSupport.networkModel();
    private static PhysicalFluidTopology.Device at(long id,int x,Kind kind,PhysicalFluidTopology.Direction facing) {
        var control=switch(kind){case PUMP->new FlowControl.Pump(.01,500000,1);case COMPRESSOR->new FlowControl.Compressor(.05,3,1);case VALVE->new FlowControl.PressureValve(200000);default->new FlowControl.Passive();};
        return new PhysicalFluidTopology.Device(id,new PhysicalFluidTopology.Position("minecraft:overworld",x,0,0),kind,facing,new PipeResistance.Geometry(1,.05,.000045,0),control);
    }
    private PhysicalFluidTopology.Compiled compile(String layout) {
        var devices=new ArrayList<PhysicalFluidTopology.Device>();var boundaries=new LinkedHashMap<Long,PassiveNetwork.Reservoir>();
        for(int i=0;i<layout.length();i++) {
            var kind=switch(layout.charAt(i)){case 'G'->Kind.GENERATOR;case 'V'->Kind.VOID;case 'R'->Kind.RESERVOIR;case 'U'->Kind.PUMP;case 'C'->Kind.COMPRESSOR;case 'Q'->Kind.VALVE;case 'F'->Kind.FILTER;default->Kind.PIPE;};
            var device=at(i+1,i,kind,EAST);devices.add(device);
            if(kind==Kind.GENERATOR)boundaries.put(device.id(),new PassiveNetwork.Reservoir(device.id(),0,model.initialNitrogenCharge(1,298.15,200000,()->{}),PassiveNetwork.NodeKind.GENERATOR));
            if(kind==Kind.VOID)boundaries.put(device.id(),new PassiveNetwork.Reservoir(device.id(),0,model.initialNitrogenCharge(1,298.15,101325,()->{}),PassiveNetwork.NodeKind.VOID));
            if(kind==Kind.RESERVOIR)boundaries.put(device.id(),new PassiveNetwork.Reservoir(device.id(),0,model.initialNitrogenCharge(1,298.15,101325,()->{})));
        }
        var filters=new HashMap<Long,InlineFilter>();for(var d:devices)if(d.kind()==Kind.FILTER)filters.put(d.id(),InlineFilter.empty());
        return PhysicalFluidTopology.compile(devices,boundaries,filters,model.initialNitrogenCharge(1,298.15,101325,()->{}));
    }
    private static Set<Long> islandOf(PhysicalFluidTopology.Compiled compiled,long device) {
        return compiled.islands().stream().filter(i->i.physicalIds().contains(device)&&!i.graph().pipes().isEmpty()&&i.graph().pipes().stream().noneMatch(p->p.blockedDirections()==3)).findFirst().map(PhysicalFluidTopology.Island::physicalIds).orElse(Set.of());
    }

    @Test void equipmentSideBySideExchangesNothing() {
        // A generator against a pump, the pump's discharge piped to a tank: the pump has one port, the generator touches nothing.
        var pumped=compile("GUPR");
        assertFalse(islandOf(pumped,4).contains(1L),"the generator is not joined to the pump: "+pumped.diagnostics());
        assertTrue(pumped.diagnostics().get(2L).startsWith("ERROR"),"the pump has one port: "+pumped.diagnostics());
        // A valve, a compressor or a filter against a tank, piped on to a void: nothing reaches the tank.
        for(String layout:List.of("RQPV","RCPV","RFPV"))assertTrue(islandOf(compile(layout),1).isEmpty(),layout+": the tank is joined to nothing");
        // Two filters and a pump in a row between pipes: no connection across a device-device face.
        assertTrue(islandOf(compile("GPFFPR"),6).isEmpty(),"two filters side by side do not pass fluid");
    }

    @Test void aPipeBlockBetweenTheEquipmentJoinsThem() {
        for(String layout:List.of("GPUPR","RPQPV","RPCPV","RPFPV","GPFPFPR")) {
            var compiled=compile(layout);var island=islandOf(compiled,layout.length());
            assertEquals(layout.length(),island.size(),layout+": one line: "+compiled.diagnostics());
        }
    }

    @Test void theRegistryWalksFollowTheSameRule() {
        var w=new EventPathWorld();w.runTo(100);
        long tank=w.place(0,0,0,Kind.RESERVOIR,NORTH,.05);long pump=w.place(1,0,0,Kind.PUMP,EAST,.05);w.place(2,0,0,Kind.PIPE,NORTH,.05);
        w.place(3,0,0,Kind.VOID,NORTH,.05);w.runTo(102);
        assertNull(w.registry.owner(pump),"a pump against a tank is in no island");
        assertEquals(List.of(tank),w.registry.members(w.registry.owner(tank)),"the tank is an island of its own");
        // A pipe between them: the placement's walk reaches both, and one island holds the line.
        w.runTo(300);w.place(0,0,1,Kind.PIPE,NORTH,.05);w.runTo(302);
        assertNull(w.registry.owner(pump),"a pipe beside the tank but not between them changes nothing");
        w.runTo(4000);w.assertKeptUp("a pump against a tank");
    }
}
