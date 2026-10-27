package com.wormzjl.icheme.runtime.fluid;

import com.wormzjl.icheme.science.fluid.topology.TopologyCompiler.Kind;
import java.util.*;
import org.junit.jupiter.api.Test;
import static com.wormzjl.icheme.runtime.fluid.PhysicalFluidTopology.Direction.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The pipeline tool's sneak-use ({@link PhysicalRegistry#resizeLine}): a pipe's whole line takes the tool's bore in one
 * event, from one item of equipment to the next and through its valves, open or closed; a branch, an inline pump and the
 * equipment keep theirs.
 */
class PipeLineResizeTest {
    private final EventPathWorld world=new EventPathWorld(0);
    /** No nozzle beside any pipe: each takes a pipe's largest. */
    private static final java.util.function.ToDoubleFunction<PhysicalFluidTopology.Device> ANY=d->com.wormzjl.icheme.science.equipment.PipeSizes.MAX_BORE;
    /** Every block placed, by its "x,y,z". */
    private final Map<String,Long> ids=new TreeMap<>();

    private long place(int x,int y,int z,Kind kind){long id=world.place(x,y,z,kind,EAST);ids.put(x+","+y+","+z,id);return id;}
    private long pipe(int x,int y,int z){return place(x,y,z,Kind.PIPE);}
    private long at(String cell){return ids.get(cell);}
    /** A laid pipe with {@code joints}, by its "x,y,z". */
    private long laid(int x,int y,int z,int joints){long id=world.placeLaid(x,y,z,EAST,.05,joints);ids.put(x+","+y+","+z,id);return id;}
    /** A rack lane one position wide on place {@code lane}, by its "x,y,z/lane"; its point on each joint face stands for its place. */
    private long lane(int x,int y,int z,int lane,int joints) {
        long points=0;for(int face=0;face<6;face++)if((joints>>face&1)!=0)points|=(long)(lane*8+1)<<10*face;
        long id=world.placeLane(x,y,z,lane,1,points,EAST,.05,joints);ids.put(x+","+y+","+z+"/"+lane,id);return id;
    }
    private PhysicalFluidTopology.Device device(String cell){return world.registry.registrations().get(at(cell)).device();}

    /** The cells a sneak-use on {@code cell} resizes to 0.1 m, after checking the line's length and that it came as one event. */
    private Set<String> resize(String cell,int length) {
        int events=world.registry.unapplied();
        assertEquals(length,world.registry.resizeLine(at(cell),.1,FluidWorldAuthority.LINE_LIMIT,ANY));
        assertEquals(events+1,world.registry.unapplied(),"one event");
        var resized=new TreeSet<String>();
        for(var cellAndId:ids.keySet())if(device(cellAndId).geometry().diameter()==.1)resized.add(cellAndId);
        return resized;
    }

    @Test void aLineRunsFromEquipmentToEquipmentThroughItsElbowsAndValvesOpenOrClosed() {
        // A tank, a run east, an elbow up, a riser, an elbow east, a run to a second tank.
        place(0,64,0,Kind.RESERVOIR);
        pipe(1,64,0);long shutoff=pipe(2,64,0);pipe(3,64,0);
        long control=pipe(3,65,0);
        pipe(3,66,0);long filter=pipe(4,66,0);long check=pipe(5,66,0);
        place(6,66,0,Kind.RESERVOIR);
        assertTrue(world.registry.installPipeCutoff(shutoff,EAST));world.registry.togglePipeCutoff(shutoff);
        assertTrue(world.registry.installPipePressureValve(control,UP));
        assertTrue(world.registry.installPipeFilter(filter,EAST));
        assertTrue(world.registry.installPipeValve(check,EAST,PhysicalFluidTopology.Cutoff.CHECK));
        assertEquals(PhysicalFluidTopology.Cutoff.CLOSED,device("2,64,0").cutoff());

        assertEquals(Set.of("1,64,0","2,64,0","3,64,0","3,65,0","3,66,0","4,66,0","5,66,0"),resize("4,66,0",7));
        // Each keeps its fitting, and its length and roughness.
        assertEquals(PhysicalFluidTopology.Cutoff.CLOSED,device("2,64,0").cutoff());
        assertTrue(device("4,66,0").inlineFilter());
        assertEquals(PhysicalFluidTopology.Cutoff.CHECK,device("5,66,0").cutoff());
        assertEquals(1.0,device("3,65,0").geometry().length());
    }

    @Test void aBranchIsALineOfItsOwnAndTheTeeBelongsToTheRunItContinues() {
        // A header west to east with a tee at x = 3, its branch three pipes south.
        for(int x=0;x<=6;x++)pipe(x,64,0);
        for(int z=1;z<=3;z++)pipe(3,64,z);
        var header=Set.of("0,64,0","1,64,0","2,64,0","3,64,0","4,64,0","5,64,0","6,64,0");
        assertEquals(header,resize("1,64,0",7));
        // The branch stops before the tee, which keeps the header's bore.
        world.registry.resizeLine(at("3,64,1"),.2,FluidWorldAuthority.LINE_LIMIT,ANY);
        for(int z=1;z<=3;z++)assertEquals(.2,device("3,64,"+z).geometry().diameter());
        assertEquals(.1,device("3,64,0").geometry().diameter());
    }

    @Test void fromAJunctionTheLineTakesEachStraightRunAndACornerTeeIsAlone() {
        // Two runs crossing at (3, 64, 0).
        for(int x=0;x<=6;x++)pipe(x,64,0);
        for(int z=-2;z<=2;z++)if(z!=0)pipe(3,64,z);
        assertEquals(11,resize("3,64,0",11).size());
        // A tee with no straight run: joined east, south and up.
        pipe(10,64,0);pipe(11,64,0);pipe(12,64,0);pipe(10,64,1);pipe(10,65,0);
        assertEquals(Set.of("10,64,0"),diff(resize("10,64,0",1)));
        // Coming from its east side, the line stops before it.
        world.registry.resizeLine(at("12,64,0"),.3,FluidWorldAuthority.LINE_LIMIT,ANY);
        assertEquals(.3,device("11,64,0").geometry().diameter());
        assertEquals(.1,device("10,64,0").geometry().diameter());
    }
    /** The cells resized after the cross of the junction test. */
    private static Set<String> diff(Set<String> resized) {
        var out=new TreeSet<>(resized);out.removeIf(cell->cell.startsWith("3,")||cell.matches("[0-6],64,0"));return out;
    }

    /**
     * A line through a pipe rack: laid plain pipes at x 0 and 3 meet lane 2 across the rack faces. Lane 3 beside it, its
     * ends jointed towards the same plain pipes, meets neither (a plain pipe meets one lane): it is a line of its own. A
     * lane one position wide carries no 0.3 m pipe: that line is not resized at all.
     */
    @Test void aLineRunsAlongItsRackLaneAndAParallelLaneIsALineOfItsOwn() {
        int along=1<<WEST.ordinal()|1<<EAST.ordinal();
        laid(0,64,0,along);lane(1,64,0,2,along);lane(2,64,0,2,along);laid(3,64,0,along);
        lane(1,64,0,3,along);lane(2,64,0,3,along);
        int events=world.registry.unapplied();
        assertEquals(PhysicalRegistry.LANE_TOO_NARROW,world.registry.resizeLine(at("0,64,0"),.3,FluidWorldAuthority.LINE_LIMIT,ANY));
        assertEquals(events,world.registry.unapplied(),"nothing queued");
        assertEquals(Set.of("1,64,0/3","2,64,0/3"),resize("2,64,0/3",2));
        assertEquals(Set.of("1,64,0/3","2,64,0/3","0,64,0","1,64,0/2","2,64,0/2","3,64,0"),resize("0,64,0",4));
    }

    @Test void anInlinePumpEndsALineAndBelongsToNone() {
        for(int x=0;x<=6;x++)pipe(x,64,0);
        assertTrue(world.registry.installPipeMover(at("3,64,0"),EAST,Kind.PUMP));
        assertEquals(Set.of("0,64,0","1,64,0","2,64,0"),resize("0,64,0",3));
        // Used on the pump, only its own pipe.
        assertEquals(Set.of("0,64,0","1,64,0","2,64,0","3,64,0"),resize("3,64,0",1));
    }

    @Test void aLineLongerThanTheLimitIsRefusedWhole() {
        for(int x=0;x<5;x++)pipe(x,64,0);
        int events=world.registry.unapplied();
        assertEquals(-1,world.registry.resizeLine(at("0,64,0"),.1,4,ANY));
        assertEquals(events,world.registry.unapplied(),"nothing queued");
        assertEquals(5,world.registry.resizeLine(at("0,64,0"),.1,5,ANY));
    }

    /**
     * A nozzle beside a pipe of the line takes a pipe up to the largest its place clears (owner decision 2026-10-27,
     * {@code NozzleBore}): a larger bore refuses the whole line and queues nothing; up to it, the line takes it.
     */
    @Test void aNozzleThatTakesASmallerPipeRefusesTheLine() {
        for(int x=0;x<4;x++)pipe(x,64,0);
        java.util.function.ToDoubleFunction<PhysicalFluidTopology.Device> nozzleBesideTheEnd=d->d.position().x()==3?.3:com.wormzjl.icheme.science.equipment.PipeSizes.MAX_BORE;
        int events=world.registry.unapplied();
        assertEquals(PhysicalRegistry.NOZZLE_TOO_SMALL,world.registry.resizeLine(at("0,64,0"),.4,FluidWorldAuthority.LINE_LIMIT,nozzleBesideTheEnd));
        assertEquals(events,world.registry.unapplied(),"nothing queued");
        assertEquals(4,world.registry.resizeLine(at("0,64,0"),.3,FluidWorldAuthority.LINE_LIMIT,nozzleBesideTheEnd));
        assertEquals(events+1,world.registry.unapplied(),"one event");
    }

    @Test void aLineAlreadyAtTheBoreQueuesNothingAndATankIsNoLine() {
        for(int x=0;x<3;x++)pipe(x,64,0);
        long tank=place(3,64,0,Kind.RESERVOIR);
        int events=world.registry.unapplied();
        assertEquals(3,world.registry.resizeLine(at("1,64,0"),.05,FluidWorldAuthority.LINE_LIMIT,ANY));
        assertEquals(0,world.registry.resizeLine(tank,.1,FluidWorldAuthority.LINE_LIMIT,ANY));
        assertEquals(events,world.registry.unapplied());
    }
}
