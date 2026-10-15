package com.wormzjl.createcheme.science.fluid.network;

import com.wormzjl.createcheme.fluid.support.FluidTestSupport;
import com.wormzjl.createcheme.science.fluid.solver.PhaseLayout;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.createcheme.science.material.MaterialCatalog;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class EmptyNetworkTest {
    private final FluidThermodynamics model=FluidTestSupport.networkModel();
    private PassiveNetwork.Reservoir empty() {
        // The retained guess cannot become inventory or be used to infer a vacuum temperature.
        var guess=model.initialNitrogenCharge(1,298.15,101325,()->{});
        return new PassiveNetwork.Reservoir(1,0,guess,PassiveNetwork.NodeKind.RESERVOIR,new PassiveNetwork.Inventory(1,new double[com.wormzjl.createcheme.science.material.MaterialTestBasis.NETWORK+1],0));
    }
    @Test void isolatedEvacuatedReservoirAdvancesWithoutEosEvaluationOrInventedGas() {
        var initial=new PassiveNetwork(List.of(empty()),List.of());assertTrue(initial.reservoirs().getFirst().empty());
        var result=new PassiveIntervalSolver(model).solve(initial,20,PassiveIntervalSolver.Settings.defaults(),()->{});
        assertEquals(20,result.advancedSeconds());assertTrue(result.graph().reservoirs().getFirst().empty());
        assertArrayEquals(new double[com.wormzjl.createcheme.science.material.MaterialTestBasis.NETWORK+1],result.graph().reservoirs().getFirst().inventory().moles());assertEquals(0,result.graph().reservoirs().getFirst().inventory().internalEnergy());
        assertEquals(0,result.averageMassFlows().length);assertTrue(result.boundaries().isEmpty());assertEquals(initial,result.graph());
    }
    /**
     * Owner decision D54: a vessel that holds nothing is a slack bag at the ambient. A 2 bar nitrogen generator fills it
     * (it was refused before: an evacuated vessel had no state to fill from); it gives nothing back, and the step's ledger
     * books what arrived, less the wall's work on the ambient, in the vessel.
     */
    @Test void anEmptyVesselFillsFromWhatArrivesAndGivesNothingBack() {
        var empty=empty();var source=new PassiveNetwork.Reservoir(2,0,model.initialNitrogenCharge(1,298.15,200000,()->{}),PassiveNetwork.NodeKind.GENERATOR);
        var graph=new PassiveNetwork(List.of(empty,source),List.of(new PassiveNetwork.Pipe(3,1,0,new PipeResistance.Geometry(1,.05,0,0))));
        var result=new PassiveStepSolver(model).solve(graph,.01,()->{});
        assertTrue(result.massFlows()[0]>0,"the generator fills the vessel: "+result.massFlows()[0]);
        var filled=result.inventories().get(0);double held=0;for(double n:filled.moles())held+=n;
        assertTrue(held>0,"the vessel holds what arrived");
        var state=result.states().get(0);assertTrue(state.pressure()>=PhaseLayout.AMBIENT_PRESSURE*(1-1e-9),"no vacuum: "+state.pressure());
        assertArrayEquals(new double[com.wormzjl.createcheme.science.material.MaterialTestBasis.NETWORK+1],empty.inventory().moles());assertEquals(0,empty.inventory().internalEnergy());
    }

    // ---------------- from HoldsNothingTest ----------------
    /*
     * Owner decision D59 (2), t28: one meaning of holding nothing. A vessel holds nothing when its bag holds at most the bag's
     * resolution, 1e-12 of its capacity (A311: the share at which the bag's volume is already snapped to the capacity). Below
     * it, what a vessel holds is roundoff (a residue a junction's buffer leaves in an emptied tank): an end holding it gives
     * nothing, and the rest test does not count it as stock. The residue stays booked in the inventory.
     */
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
