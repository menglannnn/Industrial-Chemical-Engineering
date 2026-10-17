package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.column.v3.thermo.V3PengRobinsonThermo;
import com.wormzjl.createcheme.science.fluid.network.FlowControl;
import com.wormzjl.createcheme.science.fluid.network.PassiveStepSolver;
import com.wormzjl.createcheme.science.fluid.thermo.FluidMaterialCatalog;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import com.wormzjl.createcheme.science.material.MaterialCatalog;
import com.wormzjl.createcheme.science.material.MaterialRuntime;
import com.wormzjl.createcheme.science.material.MaterialTestBasis;
import com.wormzjl.createcheme.science.material.PilotCryogenicTestCatalog;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static com.wormzjl.createcheme.runtime.fluid.PhysicalFluidTopology.Direction.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The hold-class worlds of the phase-ports batch (PHASE_PORTS_REVIEW.md, "Hold class ..." sections) and the later
 * junction-start and closure batches: each world is the smallest the placement-sequence fuzz or an in-game run found for
 * one way an island held (tools/phase-ports-probes/hold-fuzz and hold-classes). Every world is placed on the event path
 * ({@link EventPathWorld}), run to its end tick and must keep up: no hold, every placement applied, every island within
 * one cadence of the online tick. Each section below keeps the original test class's account of its class.
 */
class HoldClassWorldsTest {
    private static final FluidDeviceSpec NITROGEN=EventPathWorld.pure(MaterialTestBasis.NITROGEN,298.15,101325),WATER=EventPathWorld.pure(MaterialTestBasis.NETWORK,298.15,101325);
    private static final int METHANE=EventPathWorld.CATALOG.requirePackage("createcheme:tjl20_methane_nitrogen").components().indexOf("Methane");
    /** The Tia Juana light crude with methane, on the network basis, as a 1 m3 generator's spec. */
    private static FluidDeviceSpec crude(double temperature,double pressure) {
        double[] n=Arrays.copyOf(V3PengRobinsonThermo.fromRegisteredPackage("createcheme:tjl20_methane_nitrogen").crudeFeed("createcheme:tia_juana_light_methane").moleFractions(),
                MaterialTestBasis.NETWORK+1);
        return new FluidDeviceSpec(1,temperature,pressure,n);
    }

    // ---------------- ActuatedIslandGeneratorClosure ----------------
    /*
     * Hold class E2 of the placement-sequence fuzz (phase-ports batch, PHASE_PORTS_REVIEW.md "Hold class E2: a singular step
     * at every step size (2026-09-28)"): the cold-start closure of receiving generators (decision D12) left islands with an
     * actuator out, so a generator below its junctions' pressure stayed a two-way boundary in the cold rate seed, the steps
     * started from that seed's failure, and the island ended on a singular step at every step size (a two-phase crude
     * junction whose phase split the Newton could not determine). The smallest world of fuzz seed 4: a 472 kPa methane
     * generator and a 261 kPa two-phase crude generator feeding a junction network with two pressure control valves (120 and
     * 200 kPa) and a void, every block in one tick. It must keep up. Rebuilt for decisions D18-D20 (2026-09-28): a pipe block
     * between the crude generator and the 200 kPa valve (they touched), and a pipe and a void below the 120 kPa valve (it
     * faced a dead end), so both valves are on valid runs; without the closure in actuated islands (a3d3a789) it holds.
     */
    @Test void aLowCrudeGeneratorInAValveIslandDoesNotHoldTheSteps() {
        var catalog=EventPathWorld.CATALOG;int components=FluidDeviceSpec.nitrogen(catalog).composition().length;
        double[] crude=Arrays.copyOf(V3PengRobinsonThermo.fromRegisteredPackage("createcheme:tjl20_methane_nitrogen").crudeFeed("createcheme:tia_juana_light_methane").moleFractions(),components);
        int methane=catalog.requirePackage("createcheme:tjl20_methane_nitrogen").components().indexOf("Methane");
        var w=new EventPathWorld();w.runTo(105);var passive=new FlowControl.Passive();
        w.place(-1,-2,1,Kind.PIPE,NORTH,.05);w.place(2,0,0,Kind.PIPE,NORTH,.05);w.place(-1,-1,1,Kind.PIPE,NORTH,.02);w.place(-1,-2,3,Kind.PIPE,NORTH,.05);
        w.place(0,-2,3,Kind.PIPE,NORTH,.02);w.place(1,-2,3,Kind.GENERATOR,NORTH,passive,EventPathWorld.pure(methane,352.1261664904419,472036.05064761586),.05);
        w.place(-1,-3,0,Kind.VALVE,DOWN,new FlowControl.PressureValve(120000),null,.02);w.place(-1,-2,0,Kind.PIPE,NORTH,.02);
        w.place(-1,-4,0,Kind.PIPE,NORTH,.02);w.place(-1,-5,0,Kind.VOID,NORTH,passive,EventPathWorld.pure(MaterialTestBasis.NITROGEN,298.15,101325),.02);
        w.place(0,0,0,Kind.PIPE,NORTH,.05);w.place(1,0,0,Kind.VALVE,EAST,new FlowControl.PressureValve(200000),null,.05);
        w.place(-1,0,0,Kind.GENERATOR,NORTH,passive,new FluidDeviceSpec(1,368.8747936542612,261354.27900741925,crude),.02);
        w.place(-1,-2,2,Kind.PIPE,NORTH,.05);w.place(3,0,0,Kind.PIPE,NORTH,.05);
        w.place(4,0,0,Kind.VOID,NORTH,passive,EventPathWorld.pure(MaterialTestBasis.NITROGEN,298.15,101325),.05);
        w.place(-1,-1,0,Kind.PIPE,NORTH,.02);w.place(-1,0,1,Kind.PIPE,NORTH,.02);
        w.runTo(4200);w.assertKeptUp("a low crude generator in a valve island");
    }

    // ---------------- FilterIslandGeneratorClosure ----------------
    /*
     * Hold class E1 of the placement-sequence fuzz (phase-ports batch, PHASE_PORTS_REVIEW.md "Hold class E1: the
     * interval-start rate pass (2026-09-28)"): an island with an inline filter runs a rate solve of its port graph at every
     * interval's start, and its failure holds the island. The cold-start closure of receiving generators (decision D12) left
     * filter islands out, so a generator that can only receive stayed a two-way boundary in that rate solve. The smallest
     * world of fuzz seed 6 with the world's placement defaults: a 1 atm water generator three blocks below a junction pipe
     * that joins two nitrogen voids at 1 atm and an inline filter facing down with nothing past it, every block in one tick.
     * It held at its first slice on "Newton line search stalled"; it must keep up. Rebuilt for decisions D18-D20 (2026-09-28): the
     * filter led nowhere and is now given a pipe and a void past it, and a 130 kPa water generator feeds the junction (without
     * it the island is at rest and never solved, decision D19); without the filter in the cold closure (bbb79790) it holds on
     * the interval-start rate pass again.
     */
    @Test void aWaterGeneratorBelowAFilterIslandsJunctionDoesNotHoldTheRatePass() {
        var w=new EventPathWorld();w.runTo(662);var passive=new FlowControl.Passive();
        var nitrogen=EventPathWorld.pure(MaterialTestBasis.NITROGEN,298.15,101325);
        w.place(0,0,-2,Kind.VOID,NORTH,passive,nitrogen,.05);w.place(0,0,-1,Kind.PIPE,NORTH,.05);w.place(0,1,1,Kind.PIPE,NORTH,.05);
        w.place(-1,-3,1,Kind.GENERATOR,NORTH,passive,EventPathWorld.pure(MaterialTestBasis.NETWORK,298.15,101325),.05);
        w.place(-1,0,1,Kind.PIPE,NORTH,.05);w.place(-1,-1,1,Kind.PIPE,NORTH,.05);w.place(0,2,1,Kind.FILTER,DOWN,.05);
        w.place(0,0,0,Kind.VOID,NORTH,passive,nitrogen,.05);w.place(-1,-2,1,Kind.PIPE,NORTH,.05);w.place(0,0,1,Kind.PIPE,NORTH,.05);
        w.place(-1,0,0,Kind.PIPE,NORTH,.05);
        // Rebuilt for decisions D18-D20: the filter leads on to a void (a pipe and a void above it), and a 130 kPa water
        // generator feeds the junction through one pipe block, so the island flows and is solved.
        w.place(0,3,1,Kind.PIPE,NORTH,.05);w.place(0,4,1,Kind.VOID,NORTH,passive,nitrogen,.05);
        w.place(1,0,1,Kind.PIPE,NORTH,.05);w.place(2,0,1,Kind.GENERATOR,NORTH,passive,EventPathWorld.pure(MaterialTestBasis.NETWORK,298.15,130000),.05);
        w.runTo(4700);w.assertKeptUp("a water generator below a filter island's junction");
        long filter=w.registry.at(new PhysicalFluidTopology.Position(EventPathWorld.DIM,0,2,1)).orElseThrow().device().id();
        org.junit.jupiter.api.Assertions.assertTrue(w.registry.members(w.registry.owner(filter)).size()>1,"the filter is on a solved run, not dormant");
    }

    // ---------------- CappedJunction ----------------
    /*
     * Owner decision D59 (5), the capped gas junctions (A276, A341), t28: a junction holds nothing, so its mass balance is what
     * places its pressure. Where every line of a junction (or of a group of junctions joined by lines on their law) is choked
     * and every cap enters it, nothing places that pressure; where every cap leaves it and nothing enters, its balance has no
     * root. In real life its pressure moves until the line whose drive exceeds its cap's pressure drop by the least leaves its
     * choke: that line is on its law for the pass.
     */
    /**
     * The smallest world of random fuzz seed 529 (7 blocks; minimized in tools/phase-ports-probes/hold-classes logs cc34): a
     * 496 kPa methane generator feeds pipes around a placed tank, and a pipe added at tick 162 closes a loop. The new
     * island's start puts the lines of a junction group on their caps with no way for the group to balance: without the rule
     * the first Jacobian is singular at every step size (held at tick 164); with it the island keeps up for 120 slices.
     */
    @Test void aChokedJunctionThatCannotBalanceKeepsOneLineOnItsLaw() {
        var w=new EventPathWorld();var passive=new FlowControl.Passive();
        w.runTo(100);
        w.place(1,-1,-2,Kind.GENERATOR,NORTH,passive,EventPathWorld.pure(0,311.67397547863067,495884.91340665857),.1);
        w.place(0,-1,-2,Kind.PIPE,NORTH,passive,null,.05);
        w.place(0,-1,-1,Kind.PIPE,NORTH,passive,null,.05);
        w.place(0,0,-1,Kind.PIPE,NORTH,passive,null,.05);
        w.place(0,0,-2,Kind.RESERVOIR,NORTH,.05);
        w.place(0,0,0,Kind.PIPE,NORTH,passive,null,.1);
        w.runTo(162);
        w.place(0,-1,0,Kind.PIPE,NORTH,passive,null,.05);
        w.runTo(162+120*100);w.assertKeptUp("a junction whose caps all enter it");
    }

    // ---------------- ChordPressureSlope ----------------
    /*
     * Thread t29: a chord step reuses the Jacobian only while it still closes at least half of every row's gap, the rows'
     * slopes in their ends' pressures included (a row's slope in {@code ln P} is the pressure itself).
     */
    /**
     * The smallest world of random fuzz seed 886 (18 blocks, all placed at tick 239; minimized in
     * tools/phase-ports-probes/hold-classes logs fx16): a 490 kPa methane generator feeding two placed tanks and, through a
     * filter, a void, every run 10 mm at its narrowest. The step's active set is the real one (the line after the filter
     * choked, the filter on its law at 4.7e-4 below its own cap, 105 Pa across it), but the filter's two junctions started
     * 373 kPa apart and walked from about 143 kPa, where the last Jacobian was built, to 222 kPa: the chord steps stated the
     * filter row's pressure columns at 1/1.55 of their own, overshot by 0.55 per step, and the Newton ran out of iterations
     * at 1.9e-3 at every step size (held at tick 240). With the pressure slopes in the chord test the island keeps up for
     * 120 slices.
     */
    @Test void aFilterBetweenTwoWanderingJunctionsConverges() {
        var w=new EventPathWorld();var passive=new FlowControl.Passive();
        double[] water=new double[21];water[19]=1;double[] methane=new double[21];methane[0]=1;
        w.runTo(239);
        w.place(0,0,0,Kind.FILTER,SOUTH,passive,null,.01);
        w.place(0,0,4,Kind.PIPE,NORTH,passive,null,.01);
        w.place(0,1,4,Kind.PIPE,NORTH,passive,null,.01);
        w.place(0,0,-3,Kind.VOID,NORTH,passive,new FluidDeviceSpec(1,298.15,101325,water),.01);
        w.place(0,-3,-2,Kind.RESERVOIR,NORTH,passive,new FluidDeviceSpec(1,298.15,101325,water),.02);
        w.place(0,1,5,Kind.PIPE,NORTH,passive,null,.05);
        w.place(0,0,-2,Kind.PIPE,NORTH,passive,null,.01);
        w.place(0,0,1,Kind.PIPE,NORTH,passive,null,.01);
        w.place(0,0,2,Kind.PIPE,NORTH,passive,null,.01);
        w.place(-3,0,3,Kind.GENERATOR,NORTH,passive,new FluidDeviceSpec(1,372.0615276045455,490111.61641710874,methane),.01);
        w.place(-2,0,3,Kind.PIPE,NORTH,passive,null,.01);
        w.place(0,0,-1,Kind.PIPE,NORTH,passive,null,.01);
        w.place(0,-2,-2,Kind.PIPE,NORTH,passive,null,.02);
        w.place(0,1,6,Kind.PIPE,NORTH,passive,null,.05);
        w.place(-1,0,3,Kind.PIPE,NORTH,passive,null,.01);
        w.place(0,1,7,Kind.RESERVOIR,NORTH,passive,new FluidDeviceSpec(1,298.15,101325,water),.05);
        w.place(0,-1,-2,Kind.PIPE,NORTH,passive,null,.02);
        w.place(0,0,3,Kind.PIPE,NORTH,passive,null,.01);
        w.runTo(239+120*100);w.assertKeptUp("a filter between two wandering junctions");
    }

    // ---------------- ConnectionRowScale ----------------
    /*
     * Hold class E6 of the placement-sequence fuzz (phase-ports batch, PHASE_PORTS_REVIEW.md "Hold class E6, option (a)
     * implemented (2026-10-12)", owner decision D35): a plain passive connection's hydraulic row is divided by a scale taken from
     * the velocity-cap drops of its ends, and the scale must not depend on which end the fluid would come from, since at a
     * zero-flow root neither end does. Taken from the donor picked by the driving pressure's sign, the scale jumped by orders at
     * that root wherever the two ends held different fluids, every differenced Jacobian straddled the jump and the step Newton
     * stopped at its iteration limit at every step size.
     *
     * <p>Both worlds were rebuilt on the code with the junction start rule (decision D37; review "Integration of R1 and the
     * junction start rule (2026-10-12)"), which removed the nitrogen-against-water starts the first two worlds were built on:
     * world-default fuzz seeds that hold in E6 with the donor's scale restored and run clean with the scale from both ends,
     * minimized with the donor's scale (tools/phase-ports-probes/hold-classes logs k05). Both are world-default blocks (0.05 m
     * pipes, 1 atm water generators at 298.15 K, 1 atm nitrogen voids) placed through the event path as the fuzz placed them.
     */
    /**
     * The smallest world of world-default fuzz seed 724 (4 blocks, all placed at tick 110): two 1 atm water generators level
     * with a pipe between them, and a 1 atm nitrogen void one block below that pipe, which becomes their junction. Water
     * falls through the junction into the void, driven by its 1 m column (9.8 kPa); the two generators feed it equally and
     * the island rests at 13.94 kg/s (6.97 kg/s each), the junction at 99.32 kPa. With the donor's scale the first interval
     * held at tick 113 on "Newton iteration limit at residual 1.13e-8" at every step size (the full seed held the same way).
     */
    @Test void twoLevelWaterGeneratorsDrainingIntoAVoidBelowKeepUp() {
        var w=new EventPathWorld();var passive=new FlowControl.Passive();
        w.runTo(110);
        w.place(1,0,0,Kind.PIPE,NORTH,.05);w.place(2,0,0,Kind.GENERATOR,NORTH,passive,WATER,.05);
        w.place(1,-1,0,Kind.VOID,NORTH,passive,NITROGEN,.05);w.place(0,0,0,Kind.GENERATOR,EAST,passive,WATER,.05);
        w.runTo(110+120*100);w.assertKeptUp("two level water generators draining into a void below");
    }

    /**
     * The smallest world of world-default fuzz seed 247 (5 blocks, all placed at tick 140): a 1 atm water generator two
     * pipe blocks from a junction that has a 1 atm nitrogen void one block above and one block below. Water drains into the
     * lower void (9.12 kg/s at rest, the junction at 94.0 kPa, pulled below the generator by the drain's column); the upper
     * void only receives (decision D21) and stands above the junction, so its connection closes. With the donor's scale the
     * island held at tick 203 on "Newton iteration limit at residual 0.357" at every step size (the full seed's class).
     */
    @Test void aWaterGeneratorDrainingBetweenAVoidAboveAndAVoidBelowKeepsUp() {
        var w=new EventPathWorld();var passive=new FlowControl.Passive();
        w.runTo(140);
        w.place(2,1,0,Kind.PIPE,NORTH,.05);w.place(2,1,-1,Kind.PIPE,NORTH,.05);w.place(2,1,-2,Kind.GENERATOR,NORTH,passive,WATER,.05);
        w.place(2,2,0,Kind.VOID,NORTH,passive,NITROGEN,.05);w.place(2,0,0,Kind.VOID,NORTH,passive,NITROGEN,.05);
        w.runTo(140+120*100);w.assertKeptUp("a water generator draining between a void above and a void below");
    }

    // ---------------- DryEndOutflow ----------------
    /*
     * Thread t29: a vessel that starts a step holding nothing gives nothing in that step, whether the pass laid it out dry or
     * laid it out from what reached it (decision D54's dry end, read by every booking of a converged or polished point).
     */
    /**
     * The smallest world of random fuzz seed 708 (8 blocks, all placed at tick 360; minimized in
     * tools/phase-ports-probes/hold-classes logs fx03): a 366 K crude generator at 172 kPa feeding a 200 kPa relief valve,
     * an empty placed tank looped by two lines to one junction, and a void. The pass's flows reached the empty tank with
     * 1.6e-22 kg/s, so it was laid out from that; the next pass converged with the same 1.6e-22 kg/s in by one line and out
     * by the other, and the booking, which starts the tank from nothing, ended it at exactly zero: "Candidate overdraws a
     * reservoir" at every step size (held at tick 361). With an outflow from a dry end within the forbidden-direction
     * tolerance read as no flow, the tank keeps the roundoff it received and the island keeps up for 120 slices.
     */
    @Test void anEmptyTankLoopedToOneJunctionGivesNothing() {
        var w=new EventPathWorld();var passive=new FlowControl.Passive();
        double[] water=new double[21];water[19]=1;
        double[] crude={0.005000000000000001,0.0011908995509034748,0.010591918105298616,0.006510980717557732,0.0222592523684086,0.01663176428669243,
                0.024555314957131154,0.16629362769970432,0.12314847499525976,0.1177490756868832,0.10469103210175804,0.08701659237073239,0.07336455325619681,
                0.06154184102834706,0.06057002423058549,0.06607465545777229,0.02967631275527244,0.015001342402485261,0.008132338029011052,0.0,0.0};
        w.runTo(360);
        w.place(0,0,4,Kind.PIPE,NORTH,passive,null,.1);
        w.place(0,0,6,Kind.VALVE,NORTH,new FlowControl.PressureValve(200000),null,.01);
        w.place(-1,0,5,Kind.RESERVOIR,NORTH,passive,new FluidDeviceSpec(1,298.15,101325,water),.05);
        w.place(-1,0,7,Kind.VOID,NORTH,passive,new FluidDeviceSpec(1,298.15,101325,water),.1);
        w.place(0,0,5,Kind.PIPE,NORTH,passive,null,.1);
        w.place(1,0,7,Kind.GENERATOR,NORTH,passive,new FluidDeviceSpec(1,365.5491586603084,172266.18504004818,crude),.05);
        w.place(0,0,7,Kind.PIPE,NORTH,passive,null,.05);
        w.place(-1,0,4,Kind.PIPE,NORTH,passive,null,.05);
        w.runTo(360+120*100);w.assertKeptUp("an empty tank looped to one junction");
    }

    // ---------------- EmptyingVessel ----------------
    /*
     * A vessel that runs dry within a step (decision D54, A313) is solved as a junction holding its stock, from the step's
     * own start: the same carried flows as every other pass of the step (PHASE_PORTS_REVIEW.md "Active-set cycles and the
     * flush family (2026-10-14)", B1).
     */
    /**
     * Random fuzz seed 257 minimized to 7 blocks (tools/phase-ports-probes/hold-classes logs cf10). A 384 kPa, 350 K water
     * generator pushes through a 1 cm pipe into a junction that feeds an empty 1 m3 tank's top and, round a loop, a
     * junction at the tank's side, which drains 1 m down into a 1 atm void. Physics: the 1 cm pipe passes about 1.4 kg/s;
     * the tank's side outlet, on a 1 m drain to the ambient, can carry ten times what reaches the tank, so the tank holds
     * nothing: water passes through it into the void. Each time it has drained its last grams it runs dry within the step.
     * The emptying pass stands it as a junction; its connections' ports turn to the bulk, which changed their identity, so
     * the pass started from the static estimate instead of the step's carried flows, read the void's air as the drain's
     * column, converged with the tank above the ambient and released it; the vessel form has no root, and the pass sequence
     * cycled at every step size: with that start it held at tick 154 on "Phase/device active-set cycle" (logs cf10). From
     * the carried flows it runs 120 slices.
     */
    @Test void aTankThatWaterPassesThroughRunsDryAtEachStep() {
        var w=new EventPathWorld();var passive=new FlowControl.Passive();
        w.runTo(151);
        w.place(0,-1,-1,Kind.PIPE,NORTH,.01);
        w.place(0,0,-1,Kind.PIPE,NORTH,.05);
        w.place(0,0,0,Kind.GENERATOR,SOUTH,passive,EventPathWorld.pure(MaterialTestBasis.NETWORK,350.1538205969658,384088.6831723288),.02);
        w.place(0,-3,-2,Kind.VOID,NORTH,.05);
        w.place(0,-2,-1,Kind.RESERVOIR,NORTH,.01);
        w.place(0,-2,-2,Kind.PIPE,NORTH,.05);
        w.place(0,-1,-2,Kind.PIPE,NORTH,.05);
        w.runTo(151+120*100);w.assertKeptUp("a tank that water passes through");
    }

    // ---------------- HoldingNothingBooking ----------------
    /*
     * Thread t32, owner decision D59 (2): a vessel that holds nothing at the bag's resolution (1e-12 of its capacity) after a
     * step's booking keeps its residue in its inventory and is stated at its flash; no closure is solved for it.
     */
    /**
     * The smallest world of random fuzz seed 707 on the close's code (12 blocks; minimized in
     * tools/phase-ports-probes/hold-classes logs cw06): a 327 K crude generator at 371 kPa and a 311 K water generator at
     * 363 kPa filling three empty tanks, the water line joined at tick 165. The booking reached a tank holding 1e-42 mol of
     * crude and 1e-84 mol of water vapour and closed its state by a Newton whose water column found no finite difference
     * trial at any step size (held at tick 167). With the residue stated at its flash the island keeps up for 120 slices.
     */
    @Test void aResidueIsBookedWithoutAClosure() {
        var w=new EventPathWorld();var passive=new FlowControl.Passive();
        double[] water=new double[21];water[20]=1;
        double[] air=new double[21];air[19]=1;
        double[] crude={0.005000000000000001,0.0011908995509034748,0.010591918105298616,0.006510980717557732,0.0222592523684086,0.01663176428669243,
                0.024555314957131154,0.16629362769970432,0.12314847499525976,0.1177490756868832,0.10469103210175804,0.08701659237073239,0.07336455325619681,
                0.06154184102834706,0.06057002423058549,0.06607465545777229,0.02967631275527244,0.015001342402485261,0.008132338029011052,0.0,0.0};
        w.runTo(142);
        w.place(0,-3,0,Kind.GENERATOR,NORTH,passive,new FluidDeviceSpec(1,326.6101199519573,371434.1948684311,crude),.02);
        w.place(0,0,2,Kind.PIPE,NORTH,passive,null,.05);
        w.place(-1,-2,-2,Kind.GENERATOR,NORTH,passive,new FluidDeviceSpec(1,311.4239803319713,362954.9489303839,water),.05);
        w.place(0,-2,0,Kind.PIPE,NORTH,passive,null,.02);
        w.place(0,-2,-2,Kind.PIPE,NORTH,passive,null,.05);
        w.place(0,0,1,Kind.PIPE,NORTH,passive,null,.05);
        w.place(-2,0,2,Kind.RESERVOIR,NORTH,passive,new FluidDeviceSpec(1,298.15,101325,air),.01);
        w.place(0,1,1,Kind.RESERVOIR,NORTH,passive,new FluidDeviceSpec(1,298.15,101325,air),.05);
        w.place(-1,0,2,Kind.PIPE,NORTH,passive,null,.01);
        w.place(0,0,0,Kind.RESERVOIR,WEST,passive,new FluidDeviceSpec(1,298.15,101325,air),.05);
        w.place(0,-1,0,Kind.PIPE,NORTH,passive,null,.02);
        w.runTo(165);
        w.place(0,-2,-1,Kind.PIPE,NORTH,passive,null,.05);
        w.runTo(165+120*100);w.assertKeptUp("a residue booked into an empty tank");
    }

    // ---------------- HoldingNothingLayout ----------------
    /*
     * Thread t29, owner decision D59 (2): a vessel that holds nothing at the bag's resolution (1e-12 of its capacity) is laid
     * out dry, and a roundoff that reaches it is not an arrival: it stays booked in the inventory and the vessel stays dry.
     */
    /**
     * The smallest world of random fuzz seed 708 after its first hold was cured (17 blocks, all placed at tick 332;
     * minimized in tools/phase-ports-probes/hold-classes logs fx30): a 366 K crude generator at 172 kPa, a 200 kPa relief
     * valve, two placed tanks and two voids. A pass's flows reached an empty tank with 2e-29 kg of crude; the pass loop laid
     * the tank out from it, as two phases, and its fugacity rows, stated for that roundoff, stalled the line search at every
     * step size (held at tick 333). With the roundoff read as nothing the tank stays dry and the island keeps up for 120
     * slices.
     */
    @Test void aRoundoffArrivalLeavesAnEmptyTankDry() {
        var w=new EventPathWorld();var passive=new FlowControl.Passive();
        double[] water=new double[21];water[19]=1;
        double[] crude={0.005000000000000001,0.0011908995509034748,0.010591918105298616,0.006510980717557732,0.0222592523684086,0.01663176428669243,
                0.024555314957131154,0.16629362769970432,0.12314847499525976,0.1177490756868832,0.10469103210175804,0.08701659237073239,0.07336455325619681,
                0.06154184102834706,0.06057002423058549,0.06607465545777229,0.02967631275527244,0.015001342402485261,0.008132338029011052,0.0,0.0};
        w.runTo(332);
        w.place(0,0,0,Kind.PIPE,NORTH,passive,null,.01);
        w.place(0,0,4,Kind.PIPE,NORTH,passive,null,.1);
        w.place(0,0,6,Kind.VALVE,NORTH,new FlowControl.PressureValve(200000),null,.01);
        w.place(0,-1,1,Kind.RESERVOIR,NORTH,passive,new FluidDeviceSpec(1,298.15,101325,water),.05);
        w.place(0,-1,0,Kind.PIPE,NORTH,passive,null,.01);
        w.place(0,0,2,Kind.PIPE,NORTH,passive,null,.1);
        w.place(-1,0,5,Kind.RESERVOIR,NORTH,passive,new FluidDeviceSpec(1,298.15,101325,water),.05);
        w.place(2,0,2,Kind.PIPE,NORTH,passive,null,.02);
        w.place(3,0,2,Kind.VOID,NORTH,passive,new FluidDeviceSpec(1,298.15,101325,water),.02);
        w.place(-1,0,7,Kind.VOID,NORTH,passive,new FluidDeviceSpec(1,298.15,101325,water),.1);
        w.place(1,0,2,Kind.PIPE,NORTH,passive,null,.02);
        w.place(0,0,1,Kind.PIPE,NORTH,passive,null,.1);
        w.place(0,0,5,Kind.PIPE,NORTH,passive,null,.1);
        w.place(1,0,7,Kind.GENERATOR,NORTH,passive,new FluidDeviceSpec(1,365.5491586603084,172266.18504004818,crude),.05);
        w.place(0,0,7,Kind.PIPE,NORTH,passive,null,.05);
        w.place(-1,0,4,Kind.PIPE,NORTH,passive,null,.05);
        w.place(0,0,3,Kind.PIPE,NORTH,passive,null,.1);
        w.runTo(332+120*100);w.assertKeptUp("a roundoff arrival in an empty tank");
    }

    // ---------------- JunctionStartMixture ----------------
    /*
     * The junction start of form (m') (phase-ports batch, PHASE_PORTS_REVIEW.md "Junction start (m'): the pressure-balanced
     * mixture (2026-10-12)", owner decision D39): a junction that several suppliers drive into and that drains somewhere starts
     * as what is still flowing in once its pressure balances its suppliers against its drains (NetworkReduction.junctionStarts):
     * the flow-weighted mixture where the drain is open, the strongest supplier alone where it backs up. Under the earlier rule
     * it started as the nearest supplier's stream, and under form (c2) as the strongest's, whatever the drain did. It starts at
     * that balance pressure, a lone supplier's stream throttled to it as well (2026-10-13). The start chain is retired by owner
     * decision D54 (a minted junction starts from the island's fixed point, PassiveNetwork.mintJunctions); its worlds stay as
     * regressions: each keeps up.
     */
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

    // ---------------- JunctionStartState ----------------
    /*
     * Hold class E4 of the placement-sequence fuzz (phase-ports batch, PHASE_PORTS_REVIEW.md "Hold class E4, the junction start
     * from reachability (2026-10-12)", owner decision D37): a junction of an island formed from placed blocks starts as the stream
     * of the nearest boundary that can drive fluid into it, or, when none can, as the fluid standing highest among the boundaries
     * in contact with it (the start chain, retired by owner decision D54: a minted junction now starts from the island's fixed
     * point, PassiveNetwork.mintJunctions; the world stays as a regression). It used to start as the island's first boundary by identity, which
     * could be a void's nitrogen at a junction only water ever reaches; the step's material bookkeeping then failed its equation
     * gate at every step size.
     */
    /**
     * The smallest world of world-default fuzz seed 438 with the start rule reverted (6 blocks, all placed at tick 510;
     * minimized in tools/phase-ports-probes/hold-classes logs i17). A 1 atm water generator feeds, through two pipe blocks
     * level with it, a junction with one void a block below it and another beside it at its own height. Water falls the 1 m
     * into the lower void and the island settles at a steady spill of about 6.3 kg/s. Nothing is driven into the junction by
     * the static test at the start (the generator stands exactly at the voids' pressure at its height), so it holds the
     * fluid standing highest in contact with it: the generator's water and the side void's nitrogen stand equal there, and the
     * denser water settles in. Started as the island's first boundary, the lower void's nitrogen, the island held at tick 573
     * on "Conservative reconstruction fails equation gate: 1.16e-4" at every step size; with the rule it keeps up for 120
     * slices.
     */
    @Test void aJunctionOnlyWaterReachesStartsAsWater() {
        var w=new EventPathWorld();var passive=new FlowControl.Passive();
        w.runTo(510);
        w.place(0,1,0,Kind.PIPE,NORTH,.05);w.place(0,0,0,Kind.VOID,NORTH,passive,NITROGEN,.05);w.place(0,1,2,Kind.PIPE,NORTH,.05);
        w.place(0,1,3,Kind.GENERATOR,NORTH,passive,WATER,.05);w.place(0,1,1,Kind.PIPE,NORTH,.05);w.place(1,1,0,Kind.VOID,NORTH,passive,NITROGEN,.05);
        w.runTo(510+120*100);w.assertKeptUp("a junction only water reaches");
    }

    // ---------------- PartitionDraw ----------------
    /*
     * Nothing is drawn that is not there (PHASE_PORTS_REVIEW.md "Active-set cycles and the flush family (2026-10-14)", B3):
     * a phase stream carries a component in proportion to what its vessel holds of it. Where the reconstruction's explicit
     * booking of a frozen phase stream leaves a component negative, that component's streams are booked at the candidate's
     * partition against the donor's own end fraction ({@code ConservativeTransport.reconstruct0}).
     */
    /**
     * Random fuzz seed 322 minimized to 10 blocks (tools/phase-ports-probes/hold-classes logs cf21). A 276 kPa crude
     * generator and a 431 kPa methane generator feed a tank and a riser to a 1 atm void; the tank drains through its bottom
     * into a second tank below. The upper tank's gas stream drew the Newton seed's entry trace of a component the tank never
     * held and its bulk inflow did not carry (the unbacked rule spares a donor that receives anything), 1e-34 to 5e-24 mol
     * per step, and the explicit booking held at tick 513 on "Negative transport reconstruction" at every step size.
     */
    @Test void aGasStreamDrawsNoComponentItsTankNeverHeld() {
        var w=new EventPathWorld();var passive=new FlowControl.Passive();
        w.runTo(498);
        w.place(1,0,-1,Kind.RESERVOIR,NORTH,.1);
        w.place(1,2,-2,Kind.PIPE,NORTH,.1);
        w.place(2,0,-1,Kind.PIPE,NORTH,.1);
        w.place(0,0,-2,Kind.GENERATOR,NORTH,passive,crude(362.6583839483169,276473.9715734725),.02);
        w.place(2,0,-2,Kind.GENERATOR,NORTH,passive,EventPathWorld.pure(METHANE,357.0575469354789,431096.16156985326),.1);
        w.place(1,3,-2,Kind.VOID,NORTH,.1);
        w.place(1,1,-2,Kind.PIPE,NORTH,.1);
        w.place(1,-1,-1,Kind.PIPE,NORTH,.05);
        w.place(1,0,-2,Kind.PIPE,NORTH,.1);
        w.place(1,-2,-1,Kind.RESERVOIR,NORTH,.05);
        w.runTo(498+120*100);w.assertKeptUp("a gas stream from a tank fed with crude and methane");
    }
    /**
     * Random fuzz seed 720 minimized to 10 blocks (logs cf21). A 316.2 kPa methane generator fills two tanks stacked one
     * above the other, the upper one's top joined through 2 cm pipes to a 315.9 kPa nitrogen generator, which cannot
     * receive. The upper tank's nitrogen washes out until it stands at 4.8e-24 of its amount, just above the flush fraction
     * (D16); its gas stream, frozen at the candidate's composition, then booked 5.8e-21 mol of nitrogen out over a step,
     * twelve times the 4.9e-22 mol it held, and the explicit booking held at tick 145 on "Negative transport
     * reconstruction". At the partition the stream carries the nitrogen in proportion to what the tank holds.
     */
    @Test void aTraceJustAboveTheFlushFractionIsDrawnInProportionToWhatIsHeld() {
        var w=new EventPathWorld();var passive=new FlowControl.Passive();
        w.runTo(142);
        w.place(1,3,-2,Kind.PIPE,NORTH,.02);
        w.place(0,2,0,Kind.RESERVOIR,NORTH,.05);
        w.place(0,3,0,Kind.PIPE,NORTH,.05);
        w.place(1,3,0,Kind.PIPE,NORTH,.05);
        w.place(1,3,-1,Kind.PIPE,NORTH,.02);
        w.place(0,0,0,Kind.RESERVOIR,WEST,.05);
        w.place(0,1,0,Kind.PIPE,NORTH,.05);
        w.place(0,0,2,Kind.GENERATOR,NORTH,passive,EventPathWorld.pure(METHANE,364.6040996772146,316227.12347029406),.05);
        w.place(1,3,-3,Kind.GENERATOR,NORTH,passive,EventPathWorld.pure(MaterialTestBasis.NITROGEN,299.0689920722667,315871.77173495316),.02);
        w.place(0,0,1,Kind.PIPE,NORTH,.05);
        w.runTo(142+120*100);w.assertKeptUp("a nitrogen trace washed out of a methane tank");
    }

    // ---------------- PolishedClosure ----------------
    /*
     * Thread t32: a point the D13 polish re-solves from the reconstruction may show a drive the converged point did not, a
     * one-way end that would receive; the pass loop closes that boundary and solves the pass again, as it does for any
     * converged point, instead of refusing the step.
     */
    /**
     * The smallest world of random fuzz seed 71 on the close's code (9 blocks, all placed at tick 207; minimized in
     * tools/phase-ports-probes/hold-classes logs cw05): a 317 K water generator at 334 kPa fills an empty 1 m3 tank 3 m
     * below it, beside a 146 kPa generator its lines may not feed. Full, the tank stands at 363.4 kPa (the generator and
     * the column) and nothing flows. The step's Newton accepted its start, the tank 1e-11 of its volume over its capacity
     * (within the small headspace's tolerance); the booking stated the tank at its contents, 0.01 Pa above what the
     * generator holds (water's stiffness times 1e-11), and the line into it read that as 2.8e-8 against the 1e-8 equation
     * gate. The polish converged (1.2e-14) with that 0.01 Pa pushing water back into the generator, a forbidden direction,
     * and the step was refused at every step size (held at tick 734). With the generator's line closed the island keeps up
     * for 120 slices.
     */
    @Test void aFullTankAboveItsGeneratorClosesTheGeneratorsLine() {
        var w=new EventPathWorld();var passive=new FlowControl.Passive();
        double[] water=new double[21];water[20]=1;
        double[] air=new double[21];air[19]=1;
        w.runTo(207);
        w.place(1,-2,-1,Kind.PIPE,NORTH,passive,null,.05);
        w.place(1,0,-1,Kind.PIPE,NORTH,passive,null,.05);
        w.place(1,1,-1,Kind.PIPE,NORTH,passive,null,.1);
        w.place(1,-1,-1,Kind.PIPE,NORTH,passive,null,.05);
        w.place(0,0,-1,Kind.PIPE,NORTH,passive,null,.01);
        w.place(2,1,-1,Kind.PIPE,NORTH,passive,null,.1);
        w.place(2,0,-1,Kind.GENERATOR,NORTH,passive,new FluidDeviceSpec(1,297.6508444710189,146277.4155068182,water),.1);
        w.place(0,1,-1,Kind.GENERATOR,NORTH,passive,new FluidDeviceSpec(1,317.097467877553,334274.03661019984,water),.02);
        w.place(0,-2,-1,Kind.RESERVOIR,NORTH,passive,new FluidDeviceSpec(1,298.15,101325,air),.01);
        w.runTo(207+120*100);w.assertKeptUp("a tank filled full of water by a generator");
    }

    // ---------------- ResidueTank ----------------
    /*
     * Owner decision D59 (2), t28: a residue a junction's buffer leaves in an emptied tank is nothing (the bag's resolution,
     * 1e-12 of its capacity), so a tank holding it gives nothing.
     */
    /**
     * The smallest world of world-defaults fuzz seed 322 (13 blocks, all placed at tick 498; minimized in
     * tools/phase-ports-probes/hold-classes logs cc40): two 1 atm water generators, two voids, pipes, and two placed tanks,
     * one 2 m above the other. The upper tank is left holding about 1e-71 kg of water by the junction buffers, so it was no
     * dry end: the line from it to the empty tank below stated a 2 m water column of that residue (19.5 kPa) with nothing to
     * move, and the step stalled at 4.2106 at every step size (held at tick 501). With the residue read as nothing the line
     * is a dry end's, closed for the solve, and the island keeps up for 120 slices.
     */
    @Test void aTankHoldingAResidueGivesNothing() {
        var w=new EventPathWorld();var passive=new FlowControl.Passive();
        var water=EventPathWorld.pure(MaterialTestBasis.NETWORK,298.15,101325);
        w.runTo(498);
        w.place(1,0,-1,Kind.RESERVOIR,NORTH,.05);
        w.place(0,1,-2,Kind.PIPE,NORTH,passive,null,.05);
        w.place(1,2,-2,Kind.PIPE,NORTH,passive,null,.05);
        w.place(2,0,-1,Kind.PIPE,NORTH,passive,null,.05);
        w.place(1,0,0,Kind.VOID,NORTH,.05);
        w.place(0,0,-2,Kind.GENERATOR,NORTH,passive,water,.05);
        w.place(2,0,-2,Kind.GENERATOR,NORTH,passive,water,.05);
        w.place(1,3,-2,Kind.VOID,NORTH,.05);
        w.place(1,1,-2,Kind.PIPE,NORTH,passive,null,.05);
        w.place(1,-1,-1,Kind.PIPE,NORTH,passive,null,.05);
        w.place(1,0,-2,Kind.PIPE,NORTH,passive,null,.05);
        w.place(1,-2,-1,Kind.RESERVOIR,NORTH,.05);
        w.place(2,0,0,Kind.PIPE,NORTH,passive,null,.05);
        w.runTo(498+120*100);w.assertKeptUp("a tank holding a residue");
    }

    // ---------------- StartCheckRateFailure ----------------
    /*
     * Hold class E1 of the placement-sequence fuzz (phase-ports batch, PHASE_PORTS_REVIEW.md "Hold class E1, R1 on the E6 rule
     * (2026-10-12)", owner decision D37): an island with an inline filter checks at the start of every interval, by one rate solve
     * of its port graph, whether a filter or a connection must close. That check only pre-empts a closure the steps would find,
     * and on the island's first interval the junctions it holds fixed are the mint, so a rate solve that fails there is no
     * evidence of anything. It used to hold the island; it now declares no closure and the steps decide (option R1).
     */
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

    // ---------------- StaticColumn ----------------
    /*
     * Owner decision D34 of the coolprop-low-temperature batch (M5_WORSE_WORLDS_TRACE.md "Seed 322 (E6): a trace of liquid at a
     * tank's bottom port", 2026-10-17): a phase port's static column is filled with the port's phases in priority order by
     * volume over the connection, so a trace in a long line stands as a gas column and a liquid filling the line as a liquid
     * column. The draw still reads the port's leading phase: the trace leaves first, at its capacity.
     */
    /**
     * The smallest world of random fuzz seed 322 (13 blocks; minimized in tools/unified-thermo-port-m5/trace logs 09). A 431 kPa
     * methane generator vents to two 1 atm voids, each line at its velocity cap; the junctions and two 1 m3 tanks stand at
     * 430 kPa. The upper tank's bottom port drains through 1 m of 0.05 m line (2 L) into the top of the lower tank, 2 m down,
     * which has no other connection. Heavy crude vapour that the methane carries in condenses in the upper tank (0.9 mL at
     * tick 785). It runs down and the line holds the upper tank's gas: the lower tank stands at the upper one's pressure plus
     * that gas column, 2.29 kg/m3 over 2 m, 44.9 Pa. Steady state: the upper tank dry, and no flow in the drop line but the
     * lower tank following the upper tank's gas as it cools towards the methane's temperature (0.08 K a slice, 7e-9 kg/s).
     * Read from the port's leading phase, the line was the condensate's column, 880 kg/m3 over 2 m, 17.3 kPa, which a few
     * milligrams could not fill: the condensate stood in the tank (1.8e-8 m3 at tick 997), drawn alone at a carried flow of
     * about zero it had no root, and the island held at tick 1097 at every step size.
     */
    @Test void aLiquidTraceAtATanksBottomPortLeavesItsDropLineAGasColumn() {
        var w=new EventPathWorld();var passive=new FlowControl.Passive();
        w.runTo(498);
        long upper=w.place(1,0,-1,Kind.RESERVOIR,NORTH,passive,NITROGEN,.1);
        w.place(0,1,-2,Kind.PIPE,NORTH,passive,null,.05);
        w.place(1,2,-2,Kind.PIPE,NORTH,passive,null,.1);
        w.place(2,0,-1,Kind.PIPE,NORTH,passive,null,.1);
        w.place(1,0,0,Kind.VOID,NORTH,passive,NITROGEN,.05);
        w.place(0,0,-2,Kind.GENERATOR,NORTH,passive,crude(362.6583839483169,276473.9715734725),.02);
        w.place(2,0,-2,Kind.GENERATOR,NORTH,passive,EventPathWorld.pure(METHANE,357.0575469354789,431096.16156985326),.1);
        w.place(1,3,-2,Kind.VOID,NORTH,passive,NITROGEN,.1);
        w.place(1,1,-2,Kind.PIPE,NORTH,passive,null,.1);
        w.place(1,-1,-1,Kind.PIPE,NORTH,passive,null,.05);
        w.runTo(680);w.place(1,0,-2,Kind.PIPE,NORTH,passive,null,.1);
        w.runTo(729);long lower=w.place(1,-2,-1,Kind.RESERVOIR,NORTH,passive,NITROGEN,.05);
        w.runTo(770);w.place(2,0,0,Kind.PIPE,NORTH,passive,null,.1);
        w.runTo(770+40*100);w.assertKeptUp("a liquid trace at a tank's bottom port");
        var island=w.coordinator.snapshots().stream().filter(s->s.graph().reservoirs().stream().anyMatch(n->n.id()==upper)).findFirst().orElseThrow();
        var nodes=island.graph().reservoirs();int a=-1,b=-1,line=-1;
        for(int i=0;i<nodes.size();i++){if(nodes.get(i).id()==upper)a=i;if(nodes.get(i).id()==lower)b=i;}
        for(int i=0;i<island.graph().pipes().size();i++){var p=island.graph().pipes().get(i);if(p.first()==a&&p.second()==b||p.first()==b&&p.second()==a)line=i;}
        double flow=island.lastResult().orElseThrow().averageMassFlows()[line];
        var top=nodes.get(a).state();var bottom=nodes.get(b).state();
        double column=w.model.vaporDensity(top)*PassiveStepSolver.GRAVITY*(nodes.get(a).elevation()-nodes.get(b).elevation());
        System.out.println("STATIC_COLUMN_WORLD upper P="+top.pressure()+" liquid="+top.liquidVolume()+" lower P="+bottom.pressure()+" liquid="+bottom.liquidVolume()
                +" gas column="+column+" Pa drop line="+flow+" kg/s");
        assertEquals(0,top.liquidVolume(),"the upper tank is dry: the condensate ran down");
        assertEquals(column,bottom.pressure()-top.pressure(),.1,"the drop line stands as the upper tank's gas column");
        assertTrue(Math.abs(flow)<1e-6,"no flow in the drop line but the lower tank following the upper one's cooling gas: "+flow);
    }

    // ---------------- StepStartFlows ----------------
    /*
     * The step starts from the committed flows (phase-ports batch, PHASE_PORTS_REVIEW.md "The step starts from the committed
     * flows; forbidden directions closed on first solves (2026-10-13)", card E6-S): flows are continuous in time, so an island
     * job's first step starts from the flows its committed interval ended with (PassiveIntervalSolver.Result.endpointFlows),
     * as every later step starts from the step before, and the start estimate states its column as the flow row does.
     */
    /**
     * Card E6-S's world (world-default fuzz seed 786, minimized to 8 blocks keeping the group, tools/phase-ports-probes/
     * hold-classes logs y18 and sf09). A 1 atm water generator feeds a junction beside it that drains 1 m down into a void;
     * both rise 1 m to a junction above, which the last pipe joins to a nitrogen tank. The tank vents through the lower
     * junction into the void and stands near 90.9 kPa, so the generator lifts water up its riser (10.4 kPa of drive against
     * 9.8 kPa of column) and it falls back down the other: about 4.8 kg/s circulating, 15.9 kg/s into the void, the tank
     * taking a trickle. With the committed flows carried into every job the world runs 120 slices. With the old job start
     * (the static per-connection estimate, its own column rule) it held at tick 3210 on "Newton iteration limit at
     * residual 0.0789" at every step size: every job restarted the circulation from the estimate.
     */
    @Test void aCirculationBetweenTwoRisersContinuesFromItsCommittedFlows() {
        var w=new EventPathWorld();var passive=new FlowControl.Passive();
        w.runTo(158);w.place(0,0,2,Kind.PIPE,NORTH,.05);
        w.runTo(473);w.place(-3,0,2,Kind.PIPE,NORTH,.05);
        w.runTo(504);w.place(-1,-1,2,Kind.GENERATOR,NORTH,passive,WATER,.05);
        w.runTo(553);w.place(0,-1,2,Kind.PIPE,NORTH,.05);
        w.runTo(625);w.place(-1,0,2,Kind.PIPE,NORTH,.05);
        w.runTo(643);w.place(0,-2,2,Kind.VOID,NORTH,passive,NITROGEN,.05);
        w.runTo(694);w.place(-3,0,1,Kind.RESERVOIR,NORTH,passive,NITROGEN,.05);
        w.runTo(1283);w.place(-2,0,2,Kind.PIPE,NORTH,.05);
        w.runTo(1283+120*100);w.assertKeptUp("a circulation between two risers");
    }

    // ---------------- TopologyEventFence ----------------
    /*
     * Hold class X of the placement-sequence fuzz (phase-ports batch, PHASE_PORTS_REVIEW.md "Hold class X: topology event
     * outside its fence (2026-09-27)"): {@code PhysicalRegistry.apply} threw "Topology event reached boundary/filter ... outside
     * the islands it fenced" on the server tick. Two ways an event's fence missed an island its component walk reaches, each
     * the smallest world of its fuzz seeds:
     * <ul>
     * <li>seed 28: a pipe placed beside a lone void while the void's island is mid-slice, the next pipe beside it a tick later.
     * The second event's walk stops at the first pipe (a queued event places it); when the first event applies, that pipe is a
     * dead end no island owns, so the second event was never fenced on the void's new island.</li>
     * <li>seed 234: two inline filters facing each other with no boundary (each in a dormant island of its own), then a tank
     * beside one of them. The tank's walk stopped at the first filter, whose island does not hold the second. Since only pipes
     * connect (decision D20) a pipe block stands between the filters and between the filter and the tank.</li>
     * </ul>
     */
    @Test void anEventBehindADeadEndPipeAQueuedEventPlacedIsFencedOnTheIslandBeyond() {
        var w=new EventPathWorld();
        // A tank a generator keeps filling, not the fuzz's lone void: since decision D17 a void stops an event's walk and is
        // fenced for no island, so it no longer holds the second event back; the flowing tank's island is mid-slice at 272.
        w.runTo(105);w.place(0,0,-3,Kind.GENERATOR,NORTH,new com.wormzjl.createcheme.science.fluid.network.FlowControl.Passive(),EventPathWorld.pure(com.wormzjl.createcheme.science.material.MaterialTestBasis.NETWORK,298.15,150000),.05);
        w.place(0,0,-2,Kind.PIPE,NORTH,.05);w.place(0,0,-1,Kind.RESERVOIR,NORTH,.05);
        w.runTo(272);w.place(1,0,-1,Kind.PIPE,NORTH,.1);
        w.runTo(273);w.place(2,0,-1,Kind.PIPE,NORTH,.1);
        w.runTo(1000);w.assertKeptUp("pipes beside a tank a tick apart");
    }
    @Test void aWalkGoesOnThroughAFilterInADormantIslandOfItsOwn() {
        var w=new EventPathWorld();
        // A pipe and a void past the second filter (decisions D18-D20): the chain is a dead end, each filter dormant, until
        // the tank gives it its other end; the tank's event must reach the second filter through the first.
        w.runTo(517);w.place(4,0,0,Kind.VOID,NORTH,.05);w.place(3,0,0,Kind.PIPE,NORTH,.05);w.place(2,0,0,Kind.FILTER,WEST,.05);w.place(1,0,0,Kind.PIPE,NORTH,.05);
        w.runTo(718);w.place(0,0,0,Kind.FILTER,EAST,.05);w.place(-1,0,0,Kind.PIPE,NORTH,.05);
        w.runTo(776);w.place(-2,0,0,Kind.RESERVOIR,NORTH,.05);
        w.runTo(1200);w.assertKeptUp("a tank beside a chain of two dormant filters");
    }

    // ---------------- TurbulentWalk ----------------
    /*
     * The turbulent walk on gas lines (B2 of the phase-ports batch, decision A371; PHASE_PORTS_REVIEW.md "The turbulent walk
     * on gas lines (2026-10-14)"): a chord step reuses a Jacobian only while every connection's loss slope stays within a
     * factor two of the slope that Jacobian states.
     */
    /**
     * Random fuzz seed 498 minimized to 9 blocks (tools/phase-ports-probes/hold-classes logs tw08), all placed at tick 100.
     * A 480 kPa nitrogen generator feeds, through two 1 cm pipes, a dead-end ladder of 1, 2 and 5 cm pipes: two loops of
     * junctions below it and nothing else. Physics: the ladder fills to the generator's pressure plus its gas column and
     * then nothing moves. The first solve starts its flows from the static estimate on the junctions' old pressures (about
     * 125 kPa against 480 kPa), so its lines start turbulent (0.01 to 0.03 kg/s, Re up to 2e5) and the root is near rest
     * (1e-4 kg/s, laminar). Newton halves a turbulent flow per fresh step; the chord steps between them kept the resistance
     * of flows up to twice as large and closed only a part of each line's gap (residual contractions 0.75 to 0.8, which the
     * solver read as progress), and the first step ran out of its 20 iterations at every step size: held at tick 101 on
     * "Newton iteration limit at residual 1.5497028030015203E-5" (logs tw09). With the Jacobian refreshed once a line's
     * slope has moved by more than a factor two it runs 120 slices.
     */
    @Test void aGeneratorFillingADeadEndLadderOfGasLinesSettlesWithinTheNewton() {
        var w=new EventPathWorld();var passive=new FlowControl.Passive();
        w.runTo(100);
        w.place(0,3,-3,Kind.GENERATOR,NORTH,passive,EventPathWorld.pure(MaterialTestBasis.NITROGEN,272.72073259613035,480371.272983223),.01);
        w.place(0,3,-2,Kind.PIPE,NORTH,.01);
        w.place(0,1,0,Kind.PIPE,NORTH,.01);
        w.place(0,2,-1,Kind.PIPE,NORTH,.02);
        w.place(0,0,-1,Kind.PIPE,NORTH,.02);
        w.place(0,1,-1,Kind.PIPE,NORTH,.02);
        w.place(0,3,-1,Kind.PIPE,NORTH,.01);
        w.place(0,0,0,Kind.PIPE,NORTH,.05);
        w.place(0,2,0,Kind.PIPE,NORTH,.01);
        w.runTo(100+120*100);w.assertKeptUp("a generator filling a dead-end ladder of gas lines");
    }

    // ---------------- VanishingPhase ----------------
    /*
     * One vanishing rule for every node (owner decision D38 C, 2026-10-17; D38_REVERT_D36_AND_VANISHING_RULE.md of batch
     * 2026-09-24-coolprop-low-temperature): a phase holding less than FluidThermodynamics.VANISHING_SHARE (1e-6) of its
     * node's fluid moles, and a smaller share than where the solve judging it started, leaves at a converged pass and at a
     * failed iterate, in a pipe junction as in a vessel, whether the node holds the phase or is offered it.
     */
    /**
     * Cold fuzz seed 30, pair-minimized to 11 blocks (M7_NEW_COLD_HOLDS_TRACE.md; tools/unified-thermo-port-m5/trace), on
     * the cold pilot package. The world is flat but for an LNG generator (6 MPa, 169.7 K) one block up, which pours about
     * 72 kg/s down its 5 cm port, at the velocity cap, into a junction (n5); from n5, 70 kg/s goes to a 1 atm void and
     * about 2 kg/s through 1 cm lines to a 1 m3 tank. At tick 1593 a 5 cm pipe joins a CO2-gas generator (2.03 MPa, 292 K,
     * 92.6 % CO2) to the tank and to the line between n5 and the tank. Physics: the gas pours into the tank; the
     * generator's line into n5 closes, as its flow would enter the generator; n5 goes back to about 3.49 MPa, all liquid,
     * 1.3 MPa above the pressure at which the line beside it boils. The re-minted start states n5 flashed at the
     * generator's 2.03 MPa with 4.5 % of its 56 mol as vapour, which the solve must remove: a bubble in a subcooled
     * liquid collapses. The rate solve's failed iterate left it at 1.0e-8 of n5's moles, above the trace share, so before
     * the rule the failed iterate's trial layout kept it and the island held at tick 1595 (class E5, a line-search stall at
     * residual 1.47e-9).
     */
    @Test void aJunctionsVapourCollapsingInASubcooledLiquidLeavesAtAFailedIterate() {
        var catalog=PilotCryogenicTestCatalog.catalog();var network=FluidMaterialCatalog.networkPackage(catalog);
        MaterialRuntime.with(catalog,network,()->{
            var w=new EventPathWorld(catalog,FluidThermodynamics.forNetwork(catalog,network));
            w.runTo(100);w.place(2,0,-1,Kind.PIPE,NORTH,.1);
            w.runTo(466);w.place(-2,0,0,Kind.RESERVOIR,NORTH,.05);
            w.runTo(470);w.place(0,0,0,Kind.GENERATOR,EAST,new FluidDeviceSpec(1,291.9601168397745,2031523.2199300588,
                    new double[]{0.009971399772847691,0.06435484220406093,0,0.9256737580230914,0}),.01);
            w.place(0,0,-2,Kind.PIPE,NORTH,.05);w.place(-1,0,-1,Kind.PIPE,NORTH,.01);
            w.runTo(599);w.place(-2,0,-1,Kind.PIPE,NORTH,.01);
            w.runTo(647);w.place(1,0,-1,Kind.PIPE,NORTH,.1);
            w.runTo(696);w.place(2,1,-1,Kind.GENERATOR,NORTH,new FluidDeviceSpec(1,169.6783671876938,6000000.0,
                    new double[]{0.005965999241527276,0.9300951052162594,0.027900495572427266,0.03603839996978608,0}),.05);
            w.runTo(971);w.place(0,0,-3,Kind.VOID,NORTH,.05);
            w.runTo(1283);w.place(0,0,-1,Kind.PIPE,NORTH,.05);
            w.runTo(1593);w.place(-1,0,0,Kind.PIPE,NORTH,.05);
            w.runTo(1593+5*100);
            for(var s:w.coordinator.snapshots())for(var node:s.graph().reservoirs())
                System.out.println("VANISHING_RULE island "+s.id()+" node "+node.id()+" "+node.kind()+" z="+node.elevation()+" P="+node.state().pressure()+" T="+node.state().temperature()
                        +" m="+node.state().mass()+" liq="+node.state().liquidVolume()+" vap="+node.state().vaporVolume()+" solidsKg="+node.state().solids().massKg());
            w.assertKeptUp("a junction's vapour collapsing in a subcooled liquid");
            // The junctions carry the LNG at 2.4 to 3.8 MPa and 168.5 to 169.1 K, above its bubble pressure: liquid only.
            for(var s:w.coordinator.snapshots())for(var node:s.graph().reservoirs())if(node.junction())
                assertEquals(0,node.state().vaporVolume(),"junction "+node.id()+" at "+node.state().pressure()+" Pa, "+node.state().temperature()+" K holds liquid only");
            return null;
        });
    }

    // ---------------- VelocityCapMode ----------------
    /*
     * The velocity cap is an active-set mode of the connection (phase-ports batch, PHASE_PORTS_REVIEW.md "The velocity cap as
     * an active-set mode (2026-10-13)", card E6-C): a line at its cap states the cap flow of the end it leaves, as a pump at its
     * target states its flow, and the pass loop decides when a line enters and leaves the mode.
     */
    /**
     * Card E6-C's world (random fuzz seed 529, minimized to 7 blocks keeping the group, tools/phase-ports-probes/hold-classes
     * logs y19 and y25). A 496 kPa methane generator fills tank B (nitrogen, 441 kPa by tick 1157) through a junction and
     * two pipes; the last block joins tank B to tank A (nitrogen, 1 atm) through a 2 cm pipe. The generator drives 55 kPa
     * into tank B: the inflow runs at its cap (about 0.6 kg/s, a cap drop of about 3 kPa), the junction sits just above
     * tank B and its two outlets below their caps; tank B drives 340 kPa into tank A through the 2 cm pipe, capped at about
     * 0.09 kg/s. With the cap as a mode the world runs 120 slices. With the cap as a branch of the flow row it held at tick
     * 1159 on "Newton iteration limit at residual 0.65" at every step size: the junction's pressure jumped about 2e5 Pa
     * per iteration between its capped inflow and its outlets, which went on and off their caps.
     */
    @Test void aJunctionBetweenALineAtItsCapAndItsOutletsFindsItsPressure() {
        var w=new EventPathWorld();var passive=new FlowControl.Passive();
        w.runTo(100);w.place(1,-1,-2,Kind.GENERATOR,NORTH,passive,EventPathWorld.pure(0,311.67397547863067,495884.91340665857),.1);
        w.runTo(113);w.place(0,-1,-2,Kind.PIPE,NORTH,.05);
        w.runTo(458);w.place(-2,0,-2,Kind.RESERVOIR,NORTH,.02);
        w.runTo(869);w.place(0,-1,-1,Kind.PIPE,NORTH,.05);w.place(0,0,-1,Kind.PIPE,NORTH,.05);
        w.runTo(1104);w.place(0,0,-2,Kind.RESERVOIR,NORTH,.05);
        w.runTo(1157);w.place(-1,0,-2,Kind.PIPE,NORTH,.02);
        w.runTo(1157+120*100);w.assertKeptUp("a junction between a capped inflow and its outlets");
    }

    // ---------------- VesselClosureEquilibrium ----------------
    /*
     * G2 follow-up (phase-ports batch, PHASE_PORTS_REVIEW.md "G2 follow-up: cushion vessels, budget seeds, closure cost
     * (2026-10-12)"): every reconstruction closes each vessel to its booked inventory (decision D36, InventoryEquilibrium.close),
     * and it must close it to the equilibrium the step solved it with. The step holds a trace component of a two-phase vessel in
     * one phase (the trace truncation's frozen phase support); closed in full equilibrium instead, every component in both
     * phases, a crude tank at rest stood 0.71 Pa (3.2e-6) above the step's own solution, the equation gate refused the
     * reconstruction (6.8e-6 against 1e-8) whenever the closure ran, and the island held on "Interval substep limit" after about
     * 520 refusals. Both worlds are the smallest worlds of the random fuzz seeds that held this way (minimized in
     * tools/phase-ports-probes/hold-classes logs j01, j04), with the seeds' exact doubles, placed through the event path.
     */
    /**
     * Random seed 400, 3 blocks: a light crude generator (339.33 K, 220.17 kPa, liquid at that state) feeds a 1 m3 nitrogen
     * tank at 1 atm through one 50 mm pipe, all at one height. The tank fills until it stands at the generator's pressure and
     * then rests: 0.222 m3 of crude liquid under 0.778 m3 of nitrogen and methane-rich gas at 338.33 K (traced, log j03).
     * Before the fix the closure moved the resting tank 0.71 Pa up and the island held at tick 1289.
     */
    @Test void aCrudeTankAtRestIsClosedInTheStepsEquilibrium() {
        var w=new EventPathWorld();var passive=new FlowControl.Passive();
        w.runTo(762);
        w.place(0,2,0,Kind.RESERVOIR,NORTH,passive,NITROGEN,.1);w.place(0,2,1,Kind.PIPE,NORTH,.05);
        w.place(0,2,2,Kind.GENERATOR,NORTH,passive,crude(339.3308315223712,220170.99007955682),.05);
        w.runTo(762+40*100);w.assertKeptUp("a crude tank at rest");
    }

    /**
     * Random seed 737, 5 blocks: a junction pipe joins a liquid crude generator (299.34 K, 326.9 kPa, 10 mm), a two-phase
     * one (391.7 K, 248.8 kPa, 20 mm, which closes: it stands below the junction and a generator only supplies) and, through a
     * 100 mm pipe one block up, a 1 m3 nitrogen tank. The tank fills to the generator's pressure less one metre of crude
     * (860 kg/m3, 8.4 kPa: 318.48 kPa) and rests 71 % liquid. Before the fix the closure moved it 0.032 Pa up, no polish
     * passed the gate (520 of 520 refused) and the island held at tick 978.
     */
    @Test void aCrudeTankFedThroughAJunctionIsClosedInTheStepsEquilibrium() {
        var w=new EventPathWorld();var passive=new FlowControl.Passive();
        w.runTo(151);
        w.place(1,0,0,Kind.PIPE,NORTH,.05);
        w.place(1,0,1,Kind.GENERATOR,NORTH,passive,crude(391.7410790736319,248773.00966064972),.02);
        w.place(0,0,0,Kind.GENERATOR,NORTH,passive,crude(299.34412272252007,326916.0769202787),.01);
        w.place(1,1,0,Kind.PIPE,NORTH,.1);w.place(0,1,0,Kind.RESERVOIR,NORTH,passive,NITROGEN,.01);
        w.runTo(151+40*100);w.assertKeptUp("a crude tank fed through a junction");
    }
}
