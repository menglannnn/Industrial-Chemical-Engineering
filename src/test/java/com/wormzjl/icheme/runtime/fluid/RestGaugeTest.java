package com.wormzjl.icheme.runtime.fluid;

import com.wormzjl.icheme.science.fluid.network.FlowControl;
import com.wormzjl.icheme.science.fluid.topology.TopologyCompiler.Kind;
import com.wormzjl.icheme.science.material.MaterialTestBasis;
import org.junit.jupiter.api.Test;
import static com.wormzjl.icheme.runtime.fluid.PhysicalFluidTopology.Direction.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Owner decision D58, case A3 of the phase-ports batch (REMAINING_HOLDS.md, "Remaining issues sorted for the owner"): an
 * island where nothing can move is at rest before anything is solved. A gauge stands where stock is; a junction holds no
 * stock and no gauge (D42), and on an island's first interval its pressure is only its start guess (D55). So the rest test
 * reads drives between nodes that hold stock, directly or along paths of junctions, never between two junctions.
 */
class RestGaugeTest {
    static final FluidDeviceSpec WATER=EventPathWorld.pure(MaterialTestBasis.NETWORK,298.15,101325);

    static void assertNothingMoves(EventPathWorld w,String label) {
        w.assertKeptUp(label);
        for(var s:w.coordinator.snapshots())for(double q:s.lastResult().orElseThrow().averageMassFlows())assertEquals(0,q,label+": nothing moves in island "+s.id());
    }

    /**
     * The smallest world of world-defaults fuzz seed 619 (11 blocks, all placed at tick 100; minimized in
     * tools/phase-ports-probes/hold-classes logs rc10). A 1 atm water generator three blocks below two 1 atm voids, joined by
     * pipes and junctions. Water cannot rise 3 m against the same pressure (29 kPa short), and a void only receives:
     * nothing moves. The junctions' start guesses differ by kPa between neighbours, so the rest test read a drive between
     * two junctions and solved the island; its junctions between a generator they cannot draw from and voids that cannot
     * supply them have no pressure level, and the step was singular at every size (held at tick 101).
     */
    @Test void aGeneratorBelowTwoVoidsRests() {
        var w=new EventPathWorld();var passive=new FlowControl.Passive();
        w.runTo(100);
        w.place(-1,0,2,Kind.PIPE,NORTH,passive,null,.05);
        w.place(0,-3,2,Kind.GENERATOR,NORTH,passive,WATER,.05);
        w.place(0,0,2,Kind.PIPE,NORTH,passive,null,.05);
        w.place(0,0,1,Kind.PIPE,NORTH,passive,null,.05);
        w.place(0,0,0,Kind.VOID,NORTH,.05);
        w.place(-2,0,2,Kind.PIPE,NORTH,passive,null,.05);
        w.place(-2,-1,2,Kind.PIPE,NORTH,passive,null,.05);
        w.place(0,-2,2,Kind.PIPE,NORTH,passive,null,.05);
        w.place(0,-1,2,Kind.PIPE,NORTH,passive,null,.05);
        w.place(0,0,3,Kind.VOID,NORTH,.05);
        w.place(-1,-1,2,Kind.PIPE,NORTH,passive,null,.05);
        w.runTo(100+120*100);assertNothingMoves(w,"a generator below two voids");
    }

    /**
     * The smallest world of world-defaults fuzz seed 354 (11 blocks, all placed at tick 117; rc10). Two 1 atm water
     * generators one and two blocks below two 1 atm voids and a placed tank. A generator only supplies, so neither feeds
     * the other, and neither can lift water to the voids or the tank: nothing moves. The rest test read drives between
     * junction start guesses, and the step stopped at its iteration limit at 5.4e-9 (held at tick 118).
     */
    @Test void twoGeneratorsBelowVoidsAndATankRest() {
        var w=new EventPathWorld();var passive=new FlowControl.Passive();
        w.runTo(117);
        w.place(0,1,-2,Kind.PIPE,NORTH,passive,null,.05);
        w.place(0,1,-1,Kind.PIPE,NORTH,passive,null,.05);
        w.place(0,2,-1,Kind.PIPE,NORTH,passive,null,.05);
        w.place(0,1,0,Kind.PIPE,NORTH,passive,null,.05);
        w.place(0,0,0,Kind.GENERATOR,NORTH,passive,WATER,.05);
        w.place(0,0,-3,Kind.PIPE,NORTH,passive,null,.05);
        w.place(0,2,-2,Kind.RESERVOIR,NORTH,.05);
        w.place(0,2,0,Kind.VOID,NORTH,.05);
        w.place(0,-1,-2,Kind.GENERATOR,NORTH,passive,WATER,.05);
        w.place(0,0,-2,Kind.PIPE,NORTH,passive,null,.05);
        w.place(0,1,-3,Kind.VOID,NORTH,.05);
        w.runTo(117+120*100);assertNothingMoves(w,"two generators below voids and a tank");
    }
}
