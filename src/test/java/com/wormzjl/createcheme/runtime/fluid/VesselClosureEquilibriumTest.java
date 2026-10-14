package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.column.v3.thermo.V3PengRobinsonThermo;
import com.wormzjl.createcheme.science.fluid.network.FlowControl;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import com.wormzjl.createcheme.science.material.MaterialTestBasis;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static com.wormzjl.createcheme.runtime.fluid.PhysicalFluidTopology.Direction.*;

/**
 * G2 follow-up (phase-ports batch, PHASE_PORTS_REVIEW.md "G2 follow-up: cushion vessels, budget seeds, closure cost
 * (2026-10-12)"): every reconstruction closes each vessel to its booked inventory (decision D36, InventoryEquilibrium.close),
 * and it must close it to the equilibrium the step solved it with. The step holds a trace component of a two-phase vessel in
 * one phase (the trace truncation's frozen phase support); closed in full equilibrium instead, every component in both
 * phases, a crude tank at rest stood 0.71 Pa (3.2e-6) above the step's own solution, the equation gate refused the
 * reconstruction (6.8e-6 against 1e-8) whenever the closure ran, and the island held on "Interval substep limit" after about
 * 520 refusals. Both worlds are the smallest worlds of the random fuzz seeds that held this way (minimized in
 * tools/phase-ports-probes/hold-classes logs j01, j04), with the seeds' exact doubles, placed through the event path.
 */
class VesselClosureEquilibriumTest {
    private static final FluidDeviceSpec NITROGEN=EventPathWorld.pure(MaterialTestBasis.NITROGEN,298.15,101325);
    private static FluidDeviceSpec crude(double temperature,double pressure) {
        int components=MaterialTestBasis.NETWORK+1;
        double[] n=Arrays.copyOf(V3PengRobinsonThermo.fromRegisteredPackage("createcheme:tjl20_methane_nitrogen").crudeFeed("createcheme:tia_juana_light_methane").moleFractions(),components);
        return new FluidDeviceSpec(1,temperature,pressure,n);
    }

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
