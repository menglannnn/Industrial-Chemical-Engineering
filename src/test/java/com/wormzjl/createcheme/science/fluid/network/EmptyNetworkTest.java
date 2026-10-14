package com.wormzjl.createcheme.science.fluid.network;

import com.wormzjl.createcheme.science.fluid.solver.PhaseLayout;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.createcheme.science.material.MaterialCatalog;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class EmptyNetworkTest {
    private final FluidThermodynamics model=FluidThermodynamics.forNetwork(MaterialCatalog.bundled(),"createcheme:tjl20_methane_nitrogen",1e-9);
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
}
