package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.network.FlowControl;
import com.wormzjl.createcheme.science.fluid.network.NetworkReduction;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import com.wormzjl.createcheme.science.material.MaterialTestBasis;
import java.util.*;
import org.junit.jupiter.api.Test;
import static com.wormzjl.createcheme.runtime.fluid.PhysicalFluidTopology.Direction.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Owner decision D19 (phase-ports batch, PHASE_PORTS_REVIEW.md "Boundary split, dead ends and rest (2026-09-28)"): parts of
 * a network that nothing can drive are at rest and are not solved ({@code NetworkReduction.atRest}): the interval is the
 * identity and the island certifies through the existing certificate path. The world is the smallest one of fuzz seed 30
 * with the world's placement defaults (hold class E5): two 1 atm water generators at the height of their junctions, one
 * void of 1 atm nitrogen level with them and one a block above, a pump facing a dead end and, placed 129 ticks later, a
 * filter at the end of a dead-end pipe. The generators cannot push into a void at their own pressure and height, nor lift
 * water a block, so nothing flows. It held on a line-search stall when roundoff flows of 1e-14 kg/s out of a void brought
 * nitrogen into the water junctions; at rest it is never solved.
 */
class RestIslandTest {
    private static final FlowControl PASSIVE=new FlowControl.Passive();
    private static long seed30(EventPathWorld w,double firstGenerator) {
        w.runTo(103);var water=EventPathWorld.pure(MaterialTestBasis.NETWORK,298.15,101325);var nitrogen=EventPathWorld.pure(MaterialTestBasis.NITROGEN,298.15,101325);
        w.place(0,0,1,Kind.PIPE,NORTH,.05);w.place(0,0,2,Kind.PIPE,NORTH,.05);
        long generator=w.place(0,0,0,Kind.GENERATOR,NORTH,PASSIVE,EventPathWorld.pure(MaterialTestBasis.NETWORK,298.15,firstGenerator),.05);
        w.place(0,1,1,Kind.VOID,NORTH,PASSIVE,nitrogen,.05);w.place(-1,0,1,Kind.PIPE,NORTH,.05);w.place(0,0,3,Kind.GENERATOR,NORTH,PASSIVE,water,.05);
        w.place(0,1,3,Kind.PUMP,NORTH,new FlowControl.Pump(.01,500000,1),null,.05);w.place(-2,0,0,Kind.PIPE,NORTH,.05);w.place(0,1,2,Kind.PIPE,NORTH,.05);
        w.place(-2,0,1,Kind.VOID,NORTH,PASSIVE,nitrogen,.05);
        w.runTo(232);w.place(-3,0,0,Kind.FILTER,EAST,.05);
        return generator;
    }

    @Test void generatorsThatCannotPushIntoTheirVoidsAreAtRestAndNeverSolved() {
        var w=new EventPathWorld();long generator=seed30(w,101325);w.runTo(106);
        long island=w.registry.owner(generator);var formed=w.coordinator.observe(island).graph();
        assertTrue(NetworkReduction.atRest(formed,w.model),"nothing in the island can drive a flow");
        w.runTo(4300);w.assertKeptUp("seed 30 at rest");
        var end=w.coordinator.snapshot(w.registry.owner(generator));
        assertTrue(end.status().startsWith("STEADY: no flow"),end.status());
        for(double q:end.lastResult().orElseThrow().averageMassFlows())assertEquals(0.0,q,"every flow exactly zero");
        for(int i=0;i<formed.reservoirs().size();i++) {
            var a=formed.reservoirs().get(i);var b=end.graph().reservoirs().get(i);
            assertEquals(a.inventory(),b.inventory(),"node "+a.id()+" holds what it was formed with");
            assertEquals(a.state().pressure(),b.state().pressure(),0.0,"node "+a.id()+" is not re-stated by a solve");
        }
    }

    @Test void aGeneratorAboveItsVoidsPressureIsSolved() {
        var w=new EventPathWorld();long generator=seed30(w,150000);w.runTo(106);
        assertFalse(NetworkReduction.atRest(w.coordinator.observe(w.registry.owner(generator)).graph(),w.model),"a 150 kPa generator pushes into the level void");
        w.runTo(1400);
        var end=w.coordinator.snapshot(w.registry.owner(generator));
        assertTrue(Arrays.stream(end.lastResult().orElseThrow().averageMassFlows()).anyMatch(q->Math.abs(q)>1e-3),"the generator's water flows: "+end.status());
    }

    /** Owner decision D22 (P3), restated by decision D54: a 150 kPa water generator piped to a nitrogen tank beside it. A
     * placed tank holds nothing whatever pressure it is set to (FluidDeviceSpec.initialize), so a tank set to the source's
     * pressure and one set below it are the same world: it fills to the source's pressure and rests. */
    @Test void aPlacedTankFillsToThePressureItsSourceReachesAndRests() {
        for(double tankPressure:new double[]{150000}) {
            var w=new EventPathWorld();w.runTo(100);var passive=new FlowControl.Passive();
            w.place(0,0,0,Kind.GENERATOR,NORTH,passive,EventPathWorld.pure(MaterialTestBasis.NETWORK,298.15,150000),.05);w.place(1,0,0,Kind.PIPE,NORTH,.05);
            long tank=w.place(2,0,0,Kind.RESERVOIR,NORTH,passive,EventPathWorld.pure(MaterialTestBasis.NITROGEN,298.15,tankPressure),.05);w.runTo(102);
            // Decision D54: a placed tank holds nothing, whatever its settings, so it is not at rest beside its source: it
            // fills, as a bag at the ambient, until it is full and stands at the source's pressure, and then rests.
            var formed=w.coordinator.observe(w.registry.owner(tank)).graph();
            assertFalse(NetworkReduction.atRest(formed,w.model),"tank set to "+tankPressure+" Pa, placed empty");
            w.runTo(3000);w.assertKeptUp("a tank set to "+tankPressure+" Pa");
            var end=w.coordinator.snapshot(w.registry.owner(tank));
            var filled=end.graph().reservoirs().stream().filter(n->n.id()==tank).findFirst().orElseThrow();
            assertTrue(!filled.empty()&&filled.state().mass()>1,"below the source it fills");
            assertEquals(150000,filled.state().pressure(),1,"it stands at the source's pressure");
            assertTrue(end.status().startsWith("STEADY: no flow"),end.status());
        }
    }

    @Test void aRunBetweenTwoVoidsIsAtRest() {
        var w=new EventPathWorld();w.runTo(100);var nitrogen=EventPathWorld.pure(MaterialTestBasis.NITROGEN,298.15,101325);
        long first=w.place(0,0,0,Kind.VOID,NORTH,PASSIVE,nitrogen,.05);w.place(0,1,0,Kind.PIPE,NORTH,.05);w.place(0,2,0,Kind.PIPE,NORTH,.05);
        w.place(0,3,0,Kind.VOID,NORTH,PASSIVE,EventPathWorld.pure(MaterialTestBasis.NITROGEN,298.15,300000),.05);w.runTo(102);
        assertTrue(NetworkReduction.atRest(w.coordinator.observe(w.registry.owner(first)).graph(),w.model),"voids only receive: the run between them is at rest at any pressures");
        w.runTo(1000);w.assertKeptUp("two voids");
        assertTrue(w.coordinator.snapshot(w.registry.owner(first)).status().startsWith("STEADY: no flow"));
    }
}
