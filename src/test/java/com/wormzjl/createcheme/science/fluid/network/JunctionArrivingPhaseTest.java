package com.wormzjl.createcheme.science.fluid.network;

import com.wormzjl.createcheme.fluid.support.FluidTestSupport;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.createcheme.science.material.MaterialCatalog;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Group L of the phase-ports batch (PHASE_PORTS_REVIEW.md "Group L: a junction's first step admits the phase that arrives
 * (2026-10-13)", A300-A303): a junction's first pass is laid out for the phases the step's inflow leaves in it
 * ({@code PassiveStepSolver.arrivingPhases}).
 *
 * <p>Card L's world (world defaults seed 745 under the physical start, A286): a 1 atm water generator 1 m above a fitting
 * that also feeds a void at its own height, 0.05 m pipes. The fitting holds air, the game's ambient nitrogen at 298.15 K and
 * 101 325 Pa, as a placed block does (A280); its buffer is D25's, 0.0112 kg of that gas. In real life the water falls 1 m
 * (about 9.8 kPa of drive), the air leaves within milliseconds and the fitting passes several kg/s into the void. Before
 * the rule the first pass was laid out with the air's phase alone, the junction's mixing and enthalpy rows carried the
 * residual, the trial temperatures left the water data and every step size failed. The graph is built directly, so it
 * holds air whatever junction start the world mints with.
 */
class JunctionArrivingPhaseTest {
    private static final Runnable NOOP=()->{};
    private final FluidThermodynamics model=FluidTestSupport.networkModel();
    private final int water=model.components().indexOf("Water");
    private static final PipeResistance.Geometry PIPE=new PipeResistance.Geometry(1,.05,PipeResistance.DEFAULT_ROUGHNESS_METRES,0);

    @Test void waterReachingAnAirFilledJunctionDisplacesTheAir() {
        double[] n=new double[model.molecularWeights().length];n[water]=1;
        var generator=model.flashTP(298.15,101325,n,NOOP);
        var air=model.initialNitrogenCharge(1,298.15,101325,NOOP);
        var graph=PassiveNetwork.sizeJunctionHoldups(new PassiveNetwork(List.of(new PassiveNetwork.Reservoir(1,1,generator,PassiveNetwork.NodeKind.GENERATOR),
                new PassiveNetwork.Reservoir(2,0,air,PassiveNetwork.NodeKind.JUNCTION),new PassiveNetwork.Reservoir(3,0,air,PassiveNetwork.NodeKind.VOID)),
                List.of(new PassiveNetwork.Pipe(10,0,1,PIPE),new PassiveNetwork.Pipe(11,1,2,PIPE))),model);
        assertEquals(0,graph.reservoirs().get(1).state().waterLiquid()+graph.reservoirs().get(1).state().waterVapor(),"the fitting starts as air");
        var solver=new PassiveIntervalSolver(model);PassiveIntervalSolver.Result committed=null;double toVoid=0;
        for(int slice=0;slice<20;slice++) {
            solver.replayStart(graph,committed);
            var result=solver.solve(graph,.1,PassiveIntervalSolver.Settings.defaults(),NOOP);committed=result;graph=result.graph();
            assertEquals(.1,result.advancedSeconds(),0,"slice "+slice+": the whole slice commits");
            for(var transfer:result.boundaries())if(transfer.nodeId()==3)toVoid-=transfer.moles()[water]*model.molecularWeights()[water];
        }
        var junction=graph.reservoirs().get(1).state();
        double share=junction.waterLiquid()*model.molecularWeights()[water]/junction.mass();
        System.out.println("JUNCTION_ARRIVING_PHASE waterToVoidKg="+toVoid+" junctionLiquidWaterMassShare="+share+" junctionT="+junction.temperature());
        assertTrue(share>.99,"the fitting holds water: liquid water share "+share);
        assertTrue(toVoid/2>1,"the water passes into the void at more than 1 kg/s: "+toVoid/2+" kg/s");
    }
}
