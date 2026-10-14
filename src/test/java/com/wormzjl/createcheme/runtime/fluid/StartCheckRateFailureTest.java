package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.network.FlowControl;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import com.wormzjl.createcheme.science.material.MaterialCatalog;
import com.wormzjl.createcheme.science.material.MaterialTestBasis;
import org.junit.jupiter.api.Test;
import static com.wormzjl.createcheme.runtime.fluid.PhysicalFluidTopology.Direction.*;

/**
 * Hold class E1 of the placement-sequence fuzz (phase-ports batch, PHASE_PORTS_REVIEW.md "Hold class E1, R1 on the E6 rule
 * (2026-10-12)", owner decision D37): an island with an inline filter checks at the start of every interval, by one rate solve
 * of its port graph, whether a filter or a connection must close. That check only pre-empts a closure the steps would find,
 * and on the island's first interval the junctions it holds fixed are the mint, so a rate solve that fails there is no
 * evidence of anything. It used to hold the island; it now declares no closure and the steps decide (option R1).
 */
class StartCheckRateFailureTest {
    private static final int METHANE=MaterialCatalog.bundled().requirePackage("createcheme:tjl20_methane_nitrogen").components().indexOf("Methane");

    /**
     * The smallest world of random fuzz seed 866 with R1 reverted (13 blocks, all placed at tick 289; minimized in
     * tools/phase-ports-probes/hold-classes logs i07). A 348 kPa methane generator two blocks up feeds a junction. Its only
     * ways out are a 120 kPa back-pressure valve, whose upstream side is joined to a 1 atm void and so stays shut, and a line
     * through an inline filter to an 88 kPa methane generator, which only supplies. Nothing can flow anywhere: the junctions
     * fill to the upper generator's 348 kPa, the lower generator stays shut, and the island rests. Without R1 the filter's
     * start-of-interval check stopped at its iteration limit (residual 0.0231 on pass 0) and held the island at tick 290; with
     * it the first interval commits and the island keeps up for 120 slices. This world fails its check from the old junction
     * start (the island's first boundary); under the start rule (D37) it passes without R1, and the next test guards R1.
     */
    @Test void aFailedStartCheckOfAFilterIslandLetsTheStepsDecide() {
        var w=new EventPathWorld();var passive=new FlowControl.Passive();
        var nitrogen=EventPathWorld.pure(MaterialTestBasis.NITROGEN,298.15,101325.0);
        w.runTo(289);
        w.place(2,0,-2,Kind.PIPE,NORTH,passive,null,.01);
        w.place(1,2,0,Kind.GENERATOR,NORTH,passive,EventPathWorld.pure(METHANE,359.6449449966632,348152.3443461481),.02);
        w.place(0,0,-2,Kind.PIPE,NORTH,passive,null,.05);
        w.place(2,0,-1,Kind.PIPE,NORTH,passive,null,.01);
        w.place(-2,0,-2,Kind.GENERATOR,NORTH,passive,EventPathWorld.pure(METHANE,366.5658834936904,88458.07705694108),.01);
        w.place(-1,0,-2,Kind.PIPE,NORTH,passive,null,.05);
        w.place(0,0,0,Kind.VALVE,EAST,new FlowControl.PressureValve(120000.0),null,.05);
        w.place(1,0,0,Kind.PIPE,NORTH,passive,null,.05);
        w.place(2,0,0,Kind.PIPE,NORTH,passive,null,.05);
        w.place(1,1,0,Kind.PIPE,NORTH,passive,null,.02);
        w.place(-2,0,0,Kind.VOID,NORTH,passive,nitrogen,.01);
        w.place(1,0,-2,Kind.FILTER,EAST,passive,null,.05);
        w.place(-1,0,0,Kind.PIPE,NORTH,passive,null,.01);
        w.runTo(289+120*100);w.assertKeptUp("a filter island whose start check fails at the mint");
    }

    /**
     * The same guard with the junction start rule in place (decision D37; the world above starts its junctions differently
     * under it and no longer fails its check). The smallest world of random fuzz seed 122 with R1 reverted and the start
     * rule on (9 blocks, all placed at tick 217; minimized in tools/phase-ports-probes/hold-classes
     * logs i34). A 273 kPa nitrogen generator feeds, through a 0.01 m pipe, a junction that drains through a pipe into a
     * 1 atm void; a branch from that junction rises through an inline filter to a 91 kPa nitrogen generator, which only
     * supplies and so stays shut. Nitrogen flows to the void at the small pipe's velocity cap (0.023 kg/s) and the filter
     * branch rests. Without R1 the filter's start-of-interval check stopped at its iteration limit (residual 2.51 on pass 0)
     * and held the island at tick 218; with it the island keeps up for 120 slices.
     */
    @Test void aFailedStartCheckUnderTheJunctionStartRuleLetsTheStepsDecide() {
        var w=new EventPathWorld();var passive=new FlowControl.Passive();
        var nitrogen=EventPathWorld.pure(MaterialTestBasis.NITROGEN,298.15,101325.0);
        w.runTo(217);
        w.place(-1,-1,0,Kind.PIPE,NORTH,passive,null,.1);
        w.place(-4,0,0,Kind.GENERATOR,NORTH,passive,EventPathWorld.pure(MaterialTestBasis.NITROGEN,291.0013701419287,90886.84532252274),.05);
        w.place(-1,-1,1,Kind.PIPE,NORTH,passive,null,.01);
        w.place(1,-1,0,Kind.VOID,NORTH,passive,nitrogen,.05);
        w.place(-1,-1,2,Kind.GENERATOR,NORTH,passive,EventPathWorld.pure(MaterialTestBasis.NITROGEN,311.53760283311857,272641.22182337544),.01);
        w.place(-2,0,0,Kind.FILTER,WEST,passive,null,.05);
        w.place(-3,0,0,Kind.PIPE,NORTH,passive,null,.05);
        w.place(0,-1,0,Kind.PIPE,NORTH,passive,null,.05);
        w.place(-1,0,0,Kind.PIPE,NORTH,passive,null,.05);
        w.runTo(217+120*100);w.assertKeptUp("a filter island whose start check fails under the start rule");
    }
}
