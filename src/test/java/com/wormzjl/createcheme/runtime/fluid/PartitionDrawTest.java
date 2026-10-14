package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.column.v3.thermo.V3PengRobinsonThermo;
import com.wormzjl.createcheme.science.fluid.network.FlowControl;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import com.wormzjl.createcheme.science.material.MaterialTestBasis;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static com.wormzjl.createcheme.runtime.fluid.PhysicalFluidTopology.Direction.*;

/**
 * Nothing is drawn that is not there (PHASE_PORTS_REVIEW.md "Active-set cycles and the flush family (2026-10-14)", B3):
 * a phase stream carries a component in proportion to what its vessel holds of it. Where the reconstruction's explicit
 * booking of a frozen phase stream leaves a component negative, that component's streams are booked at the candidate's
 * partition against the donor's own end fraction ({@code ConservativeTransport.reconstruct0}).
 */
class PartitionDrawTest {
    static final int METHANE=EventPathWorld.CATALOG.requirePackage("createcheme:tjl20_methane_nitrogen").components().indexOf("Methane");
    static FluidDeviceSpec crude(double temperature,double pressure) {
        double[] n=Arrays.copyOf(V3PengRobinsonThermo.fromRegisteredPackage("createcheme:tjl20_methane_nitrogen").crudeFeed("createcheme:tia_juana_light_methane").moleFractions(),
                MaterialTestBasis.NETWORK+1);
        return new FluidDeviceSpec(1,temperature,pressure,n);
    }
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
}
