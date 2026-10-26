package com.wormzjl.icheme.runtime.fluid;

import com.wormzjl.icheme.fluid.support.FluidTestSupport;
import com.wormzjl.icheme.science.fluid.network.*;
import com.wormzjl.icheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.icheme.science.fluid.topology.TopologyCompiler.Kind;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.wormzjl.icheme.runtime.fluid.PhysicalFluidTopology.Direction.*;

/**
 * Explicit pipe joints (batch 2026-10-26-pipe-laying-system): a laid pipe connects only through the faces its path uses,
 * so two lines laid side by side stay two lines, while hand-placed pipes (every face) join as they always did.
 */
class PipeJointsTest {
    private static final int ALONG_X=1<<WEST.ordinal()|1<<EAST.ordinal();
    private final FluidThermodynamics model=FluidTestSupport.networkModel();
    private PhysicalFluidTopology.Device pipe(long id,int x,int z,int joints) {
        return new PhysicalFluidTopology.Device(id,new PhysicalFluidTopology.Position("minecraft:overworld",x,0,z),Kind.PIPE,EAST,new PipeResistance.Geometry(1,.05,.000045,0),
                new FlowControl.Passive(),List.of(),true,PhysicalFluidTopology.Cutoff.NONE,false,joints);
    }
    private PhysicalFluidTopology.Device tank(long id,int x,int z) {
        return new PhysicalFluidTopology.Device(id,new PhysicalFluidTopology.Position("minecraft:overworld",x,0,z),Kind.RESERVOIR,EAST,new PipeResistance.Geometry(1,.05,.000045,0),new FlowControl.Passive());
    }
    private PassiveNetwork.Reservoir reservoir(long id){return new PassiveNetwork.Reservoir(id,0,model.initialNitrogenCharge(1,298.15,101325,()->{}));}
    /** Two lines of four pipes, at z 0 and z 1, each between two tanks; {@code joints} is every pipe's joint set. */
    private PhysicalFluidTopology.Compiled twoLines(int joints,int teeAt) {
        var devices=new ArrayList<PhysicalFluidTopology.Device>(List.of(tank(1,0,0),tank(2,5,0),tank(3,0,1),tank(4,5,1)));
        for(int x=1;x<=4;x++) {
            devices.add(pipe(10+x,x,0,x==teeAt?joints|1<<SOUTH.ordinal():joints));
            devices.add(pipe(20+x,x,1,x==teeAt?joints|1<<NORTH.ordinal():joints));
        }
        return PhysicalFluidTopology.compile(devices,Map.of(1L,reservoir(1),2L,reservoir(2),3L,reservoir(3),4L,reservoir(4)));
    }

    @Test void twoLinesLaidSideBySideStayTwoLines() {
        var laid=twoLines(ALONG_X,0);
        assertEquals(2,laid.islands().size(),"each laid line is its own island");
        for(var island:laid.islands()) {
            assertEquals(1,island.graph().pipes().size(),"one run between its two tanks");
            assertEquals(4,island.graph().pipes().getFirst().sections().stream().mapToDouble(PipeResistance.Geometry::length).sum());
        }
        var placed=twoLines(PhysicalFluidTopology.ALL_FACES,0);
        assertEquals(1,placed.islands().size(),"hand-placed pipes join every neighbour, as before");
    }

    @Test void aTeeLaidBetweenTheLinesJoinsThemOnlyWhereBothHaveTheJoint() {
        assertEquals(1,twoLines(ALONG_X,2).islands().size(),"both pipes at x 2 carry the joint across: the lines meet there");
        var oneSided=new ArrayList<PhysicalFluidTopology.Device>(List.of(tank(1,0,0),tank(2,5,0),tank(3,0,1),tank(4,5,1)));
        for(int x=1;x<=4;x++){oneSided.add(pipe(10+x,x,0,x==2?ALONG_X|1<<SOUTH.ordinal():ALONG_X));oneSided.add(pipe(20+x,x,1,ALONG_X));}
        assertEquals(2,PhysicalFluidTopology.compile(oneSided,Map.of(1L,reservoir(1),2L,reservoir(2),3L,reservoir(3),4L,reservoir(4))).islands().size(),
                "a joint facing a pipe without one is an open end");
    }

    @Test void aLaidPipeBesideAHandPlacedOneDoesNotJoinIt() {
        var devices=List.of(tank(1,0,0),pipe(11,1,0,PhysicalFluidTopology.ALL_FACES),tank(2,2,0),pipe(21,1,1,ALONG_X));
        var compiled=PhysicalFluidTopology.compile(devices,Map.of(1L,reservoir(1),2L,reservoir(2)));
        assertEquals(1,compiled.islands().size());assertFalse(compiled.islands().getFirst().physicalIds().contains(21L));
        assertEquals("NO FLOW: unconnected pipe",compiled.diagnostics().get(21L));
    }

    @Test void onlyALaidPipeHasExplicitJointsAndEveryCopyKeepsThem() {
        assertThrows(IllegalArgumentException.class,()->pipe(1,0,0,64),"past the six faces");
        assertThrows(IllegalArgumentException.class,()->new PhysicalFluidTopology.Device(1,new PhysicalFluidTopology.Position("minecraft:overworld",0,0,0),Kind.RESERVOIR,EAST,
                new PipeResistance.Geometry(1,.05,.000045,0),new FlowControl.Passive(),List.of(),true,PhysicalFluidTopology.Cutoff.NONE,false,ALONG_X),"a tank has every face");
        var laid=pipe(1,0,0,ALONG_X);
        assertEquals(ALONG_X,laid.withGeometry(new PipeResistance.Geometry(1,.1,.000045,0)).joints());
        assertEquals(ALONG_X,laid.withCutoff(EAST,PhysicalFluidTopology.Cutoff.OPEN).joints());
        assertEquals(ALONG_X,laid.withInlineFilter(EAST,true).joints());
        assertEquals(ALONG_X|1<<NORTH.ordinal(),laid.withJoints(ALONG_X|1<<NORTH.ordinal()).joints());
        assertFalse(laid.mechanicallyConnects(NORTH));assertTrue(laid.mechanicallyConnects(EAST));
    }

    private PhysicalFluidTopology.Device lane(long id,int x,int z,int lane,int slots,int joints) {
        return new PhysicalFluidTopology.Device(id,new PhysicalFluidTopology.Position("minecraft:overworld",x,0,z,lane),Kind.PIPE,EAST,new PipeResistance.Geometry(1,.05,.000045,0),
                new FlowControl.Passive(),List.of(),true,PhysicalFluidTopology.Cutoff.NONE,false,joints,slots);
    }
    private int islands(List<PhysicalFluidTopology.Device> devices,long... tanks) {
        var boundaries=new HashMap<Long,PassiveNetwork.Reservoir>();for(long tank:tanks)boundaries.put(tank,reservoir(tank));
        return PhysicalFluidTopology.compile(devices,boundaries).islands().size();
    }

    /** A line through two rack cells: plain laid pipes at x 0 and 3 meet the lanes across the rack faces (plan 5.2). */
    @Test void aLaneJoinsTheSameLaneNextDoorAndAPlainPipeAcrossARackFace() {
        var line=List.of(tank(1,-1,0),pipe(10,0,0,ALONG_X),lane(11,1,0,2,1,ALONG_X),lane(12,2,0,2,1,ALONG_X),pipe(13,3,0,ALONG_X),tank(2,4,0));
        assertEquals(1,islands(line,1,2),"one line from tank to tank");
        var compiled=PhysicalFluidTopology.compile(line,Map.of(1L,reservoir(1),2L,reservoir(2)));
        assertEquals(1,compiled.islands().getFirst().graph().pipes().size());
        assertEquals(4,compiled.islands().getFirst().graph().pipes().getFirst().sections().stream().mapToDouble(PipeResistance.Geometry::length).sum());
        assertEquals(2,islands(List.of(tank(1,-1,0),pipe(10,0,0,ALONG_X),lane(11,1,0,2,1,ALONG_X),lane(12,2,0,3,1,ALONG_X),pipe(13,3,0,ALONG_X),tank(2,4,0)),1,2),
                "lanes on other slots do not meet");
        assertEquals(2,islands(List.of(tank(1,-1,0),pipe(10,0,0,ALONG_X),lane(11,1,0,2,1,ALONG_X),lane(12,2,0,2,2,ALONG_X),pipe(13,3,0,ALONG_X),tank(2,4,0)),1,2),
                "lanes of other widths do not meet");
        assertEquals(2,islands(List.of(tank(1,0,0),lane(11,1,0,2,1,ALONG_X),lane(12,2,0,2,1,ALONG_X),pipe(13,3,0,ALONG_X),tank(2,4,0)),1,2),
                "a lane meets a tank only through a plain pipe");
    }

    @Test void parallelLanesInOneRackStayApart() {
        var devices=new ArrayList<PhysicalFluidTopology.Device>(List.of(tank(1,-1,0),pipe(10,0,0,ALONG_X),pipe(13,3,0,ALONG_X),tank(2,4,0)));
        // A second line on another slot of the same rack cells, open at both ends.
        for(int x=1;x<=2;x++){devices.add(lane(10+x,x,0,1,1,ALONG_X));devices.add(lane(20+x,x,0,4,2,ALONG_X));}
        var compiled=PhysicalFluidTopology.compile(devices,Map.of(1L,reservoir(1),2L,reservoir(2)));
        assertEquals(1,compiled.islands().size(),"the open lane pair reaches no tank: "+compiled.diagnostics());
        assertTrue(compiled.islands().getFirst().physicalIds().containsAll(List.of(1L,10L,11L,12L,13L,2L)));
        assertFalse(compiled.islands().getFirst().physicalIds().contains(21L));
        assertTrue(compiled.diagnostics().get(21L).startsWith("NO FLOW"),compiled.diagnostics().toString());
    }

    /**
     * A lane meets a plain cell only through a transition pipe (plan 6): a laid pipe of two joints at most and no fitting,
     * whose run can bend to the lane's off-centre point. A hand-placed pipe, a tee and a valve beside the rack meet none.
     */
    @Test void aLaneMeetsOnlyATransitionPipe() {
        var valve=new PhysicalFluidTopology.Device(10,new PhysicalFluidTopology.Position("minecraft:overworld",0,0,0),Kind.PIPE,EAST,new PipeResistance.Geometry(1,.05,.000045,0),
                new FlowControl.Passive(),List.of(),true,PhysicalFluidTopology.Cutoff.OPEN,false,ALONG_X);
        for(var beside:List.of(pipe(10,0,0,PhysicalFluidTopology.ALL_FACES),pipe(10,0,0,ALONG_X|1<<UP.ordinal()),valve))
            assertEquals(2,islands(List.of(tank(1,-1,0),beside,lane(11,1,0,2,1,ALONG_X),pipe(12,2,0,ALONG_X),tank(2,3,0)),1,2),"beside the rack: "+beside);
        assertEquals(1,islands(List.of(tank(1,0,1),pipe(10,0,0,1<<SOUTH.ordinal()|1<<EAST.ordinal()),lane(11,1,0,2,1,ALONG_X),pipe(12,2,0,ALONG_X),tank(2,3,0)),1,2),
                "an elbow into the rack meets the lane");
    }

    @Test void aLaneIsAPlainPipeOfAtMostTwoJointsWithinTheRack() {
        assertThrows(IllegalArgumentException.class,()->lane(1,0,0,2,1,ALONG_X|1<<UP.ordinal()),"a junction in a rack");
        assertThrows(IllegalArgumentException.class,()->lane(1,0,0,5,2,ALONG_X),"past the fifth slot");
        assertThrows(IllegalArgumentException.class,()->lane(1,0,0,0,1,ALONG_X),"a width without a lane");
        assertThrows(IllegalArgumentException.class,()->lane(1,0,0,2,0,ALONG_X),"a lane without a width");
        assertThrows(IllegalArgumentException.class,()->lane(1,0,0,2,1,PhysicalFluidTopology.ALL_FACES),"automatic joints");
        assertThrows(IllegalArgumentException.class,()->new PhysicalFluidTopology.Position("minecraft:overworld",0,0,0,6));
        var copied=lane(1,0,0,2,1,ALONG_X).withGeometry(new PipeResistance.Geometry(1,.1,.000045,0));assertEquals(1,copied.slots());assertEquals(2,copied.position().lane());
    }

    /** Facts schema 2 writes the joints after the revision; the schema-1 corpus image (no joints field) reads every face. */
    @Test void jointsRoundTripThroughTheFacts() {
        for(var device:List.of(pipe(7,3,0,ALONG_X|1<<UP.ordinal()),pipe(7,3,0,PhysicalFluidTopology.ALL_FACES),pipe(7,3,0,0),lane(7,3,0,4,2,ALONG_X))) {
            var laid=new WorldTopologyLedger.Registration(device,FluidDeviceSpec.nitrogen(EventPathWorld.CATALOG),4);
            var bytes=FluidFacts.events(List.of(new WorldTopologyLedger.Event(UUID.randomUUID(),12,List.of(new WorldTopologyLedger.Edit(7,laid)),Set.of(7L))));
            assertEquals(laid,FluidFacts.events(bytes,0,bytes.length).getFirst().edits().getFirst().replacement());
        }
    }
}
