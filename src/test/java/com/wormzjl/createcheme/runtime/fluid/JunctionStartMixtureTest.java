package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.network.FlowControl;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import com.wormzjl.createcheme.science.material.MaterialTestBasis;
import org.junit.jupiter.api.Test;
import static com.wormzjl.createcheme.runtime.fluid.PhysicalFluidTopology.Direction.*;

/**
 * The junction start of form (m') (phase-ports batch, PHASE_PORTS_REVIEW.md "Junction start (m'): the pressure-balanced
 * mixture (2026-10-12)", owner decision D39): a junction that several suppliers drive into and that drains somewhere starts
 * as what is still flowing in once its pressure balances its suppliers against its drains (NetworkReduction.junctionStarts):
 * the flow-weighted mixture where the drain is open, the strongest supplier alone where it backs up. Under the earlier rule
 * it started as the nearest supplier's stream, and under form (c2) as the strongest's, whatever the drain did. It starts at
 * that balance pressure, a lone supplier's stream throttled to it as well (2026-10-13). The start chain is retired by owner
 * decision D54 (a minted junction starts from the island's fixed point, PassiveNetwork.mintJunctions); its worlds stay as
 * regressions: each keeps up.
 */
class JunctionStartMixtureTest {
    private static final FluidDeviceSpec NITROGEN=EventPathWorld.pure(MaterialTestBasis.NITROGEN,298.15,101325);

    /**
     * The smallest world of random fuzz seed 30 in which the three start rules differ (8 blocks; minimized so that the
     * nearest-supplier rule holds in class E6, the strongest-supplier rule holds, and (m') runs clean;
     * tools/phase-ports-probes/hold-classes logs m09, m10). A 361 kPa water generator at 356 K and a 366 kPa nitrogen
     * generator at 333 K feed a junction pipe level with them (directly and through a 0.1 m loop), which vents up into a
     * 1 atm nitrogen void; at tick 756 a second void beside the loop's pipe makes that a second junction. Both generators
     * stand far above the void, so both keep feeding: the junction holds a water-rich mixture with about 2.4 % nitrogen
     * (mole) at 332 kPa at rest, both generators flowing. (m') starts it as 2.7 % nitrogen at 355 kPa, and after the
     * second void at 2.2 % and 335 kPa, beside a second junction at 1.0 % and 102 kPa (its steady state 1.5 % at 101.5
     * kPa); the world runs 120 slices. Started as the nearest supplier's water at 361 kPa, the second island held at tick
     * 758 on "Newton iteration limit at residual 0.0041" at every step size; started as the strongest supplier's nitrogen, the
     * first island held at tick 104 on the water-vapour range.
     */
    @Test void twoGeneratorsVentingIntoAVoidStartTheirJunctionAsTheirMixture() {
        var w=new EventPathWorld();var passive=new FlowControl.Passive();
        var water=EventPathWorld.pure(MaterialTestBasis.NETWORK,355.6753347596607,361077.84297592135);
        var nitrogen=EventPathWorld.pure(MaterialTestBasis.NITROGEN,333.2954119245145,366182.8050784859);
        w.runTo(103);
        w.place(0,0,1,Kind.PIPE,NORTH,.02);w.place(-1,0,0,Kind.PIPE,NORTH,.01);w.place(0,0,2,Kind.PIPE,NORTH,.02);
        w.place(0,0,0,Kind.GENERATOR,EAST,passive,water,.01);w.place(0,1,1,Kind.VOID,NORTH,passive,NITROGEN,.1);
        w.place(-1,0,1,Kind.PIPE,NORTH,.1);w.place(0,0,3,Kind.GENERATOR,NORTH,passive,nitrogen,.02);
        w.runTo(756);
        w.place(-2,0,1,Kind.VOID,NORTH,passive,NITROGEN,.1);
        w.runTo(756+120*100);w.assertKeptUp("two generators venting into a void through one junction");
    }

    /**
     * The start pressure of a junction one supplier reaches (PHASE_PORTS_REVIEW.md "Junction start at the balance pressure
     * (2026-10-13)"): the smallest world of random fuzz seed 17 in which the two starts differ (9 blocks; minimized so that
     * the start at the supplier's own state holds and the start at the balance pressure runs clean; tools/phase-ports-probes/
     * hold-classes logs n09, n10). A 195 kPa nitrogen generator at 288 K feeds, through two 0.01 m pipe blocks, three
     * junctions that drain into two 1 atm nitrogen tanks. The narrow supply runs at its velocity cap (0.018 kg/s), so the
     * junctions stand near the tanks: about 109 kPa at the first, 101.37 kPa at the other two after the first interval,
     * and the tanks fill slowly. Started at the balance pressure (109.27, 101.33, 101.33 kPa) the world runs 120 slices;
     * started at the generator's own 195 kPa, it held at tick 101 on "Newton iteration limit at residual 2.9e-7" at every
     * step size.
     */
    @Test void aLoneSupplierThroughANarrowPipeStartsItsJunctionsAtTheirBalancePressure() {
        var w=new EventPathWorld();var passive=new FlowControl.Passive();
        var nitrogen=EventPathWorld.pure(MaterialTestBasis.NITROGEN,288.29426437198714,194962.1520197138);
        w.runTo(100);
        w.place(4,1,0,Kind.PIPE,NORTH,.01);w.place(3,0,0,Kind.RESERVOIR,NORTH,.05);w.place(2,0,-1,Kind.RESERVOIR,NORTH,.1);
        w.place(3,1,0,Kind.PIPE,NORTH,.05);w.place(5,1,0,Kind.PIPE,NORTH,.01);w.place(2,0,0,Kind.PIPE,NORTH,.05);
        w.place(4,0,0,Kind.PIPE,NORTH,.05);w.place(2,1,0,Kind.PIPE,NORTH,.05);w.place(6,1,0,Kind.GENERATOR,NORTH,passive,nitrogen,.01);
        w.runTo(100+120*100);w.assertKeptUp("a lone generator feeding junctions through a narrow pipe");
    }

    /**
     * The drain-side gas column (PHASE_PORTS_REVIEW.md "Junction start: the drain-side gas column (2026-10-13)"): the
     * smallest world of world-default fuzz seed 992 in which the start with and without that column differ (11 blocks;
     * minimized so that the start without it holds and the start with it runs clean; tools/phase-ports-probes/hold-classes
     * logs o07, o08). Two 1 atm nitrogen tanks at y 0 and a 1 atm water generator at y -1 meet at junctions at y 0 and
     * y -1; at tick 165 a pipe joins a tank's branch and the island is re-formed. Every boundary stands at 1 atm, so the
     * junction at y -1 stands under 1 m of the tanks' nitrogen at 101 336.2 Pa, 11 Pa above the generator's head: the
     * generator stays shut and nothing flows. Counting the gas its drain path is full of, the junction's drain bound is
     * 101 336.2 Pa, the generator does not reach it, and it starts as the tanks' nitrogen; the world runs 120 slices.
     * Without the column the bound is the tanks' pressure, a few 1e-11 Pa below the generator's head by tick 167, so the
     * re-formed junction started as the generator's water and held at tick 168 on "Newton iteration limit at residual
     * 0.0012" at every step size.
     */
    @Test void aJunctionUnderTheTanksNitrogenStandsAboveALevelWaterGenerator() {
        var w=new EventPathWorld();
        var water=EventPathWorld.pure(MaterialTestBasis.NETWORK,298.15,101325);
        w.runTo(132);
        w.place(-3,-1,0,Kind.PIPE,NORTH,.05);w.place(-3,0,0,Kind.PIPE,NORTH,.05);w.place(-2,-1,-1,Kind.PIPE,NORTH,.05);
        w.place(-1,0,0,Kind.PIPE,NORTH,.05);w.place(0,0,0,Kind.RESERVOIR,NORTH,.05);w.place(-1,0,-2,Kind.RESERVOIR,NORTH,.05);
        w.place(-2,0,0,Kind.PIPE,NORTH,.05);w.place(-2,-1,0,Kind.PIPE,NORTH,.05);w.place(-2,-1,-2,Kind.GENERATOR,NORTH,new FlowControl.Passive(),water,.05);
        w.place(-1,0,-1,Kind.PIPE,NORTH,.05);
        w.runTo(165);
        w.place(-1,0,-3,Kind.PIPE,NORTH,.05);
        w.runTo(165+120*100);w.assertKeptUp("a junction under the tanks' nitrogen beside a level water generator");
    }
}
