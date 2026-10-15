package com.wormzjl.createcheme.science.fluid.network;

import com.wormzjl.createcheme.fluid.support.FluidTestSupport;
import com.wormzjl.createcheme.science.fluid.solver.PhaseLayout;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.createcheme.science.material.MaterialCatalog;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Owner decision D59 (2), t28: one meaning of holding nothing. A vessel holds nothing when its bag holds at most the bag's
 * resolution, 1e-12 of its capacity (A311: the share at which the bag's volume is already snapped to the capacity). Below
 * it, what a vessel holds is roundoff (a residue a junction's buffer leaves in an emptied tank): an end holding it gives
 * nothing, and the rest test does not count it as stock. The residue stays booked in the inventory.
 */
class HoldsNothingTest {
    private final FluidThermodynamics model=FluidTestSupport.networkModel();

    /** A 1 m3 vessel holding nitrogen at 298.15 K and 1 atm whose bag is {@code share} of its capacity. */
    private PassiveNetwork.Reservoir vessel(double share) {
        var state=model.initialNitrogenCharge(share,298.15,101325,()->{});
        return new PassiveNetwork.Reservoir(1,0,state,PassiveNetwork.NodeKind.RESERVOIR,
                new PassiveNetwork.Inventory(1,PhaseLayout.totalAmounts(state),state.internalEnergy(),state.solids()));
    }

    @Test void aBagAtOrBelowItsResolutionHoldsNothingAndGivesNothing() {
        for(double[] c:new double[][]{{2e-12,0},{1e-12,1},{.5e-12,1}}) {
            var node=vessel(c[0]);boolean nothing=c[1]==1;
            assertEquals(c[0],PhaseLayout.bagVolume(node.state(),1),1e-6*c[0],"the bag is that share of its capacity");
            assertEquals(nothing,PassiveStepSolver.holdsNothing(node),"share "+c[0]);
            assertEquals(nothing,PassiveStepSolver.dryEnd(node,PassiveNetwork.PhasePort.BULK),"a dry end at share "+c[0]);
            // Nothing is deleted: the residue is still the vessel's inventory, and the layout still carries it.
            assertFalse(node.empty(),"the inventory keeps its moles at share "+c[0]);
        }
        var empty=new PassiveNetwork.Reservoir(2,0,model.initialNitrogenCharge(1,298.15,101325,()->{}),PassiveNetwork.NodeKind.RESERVOIR,
                new PassiveNetwork.Inventory(1,new double[model.componentCount()],0));
        assertTrue(PassiveStepSolver.holdsNothing(empty),"a vessel with no moles holds nothing");
    }
}
