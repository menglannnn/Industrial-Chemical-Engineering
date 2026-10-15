package com.wormzjl.createcheme.science.fluid.network;

import com.google.gson.JsonParser;
import com.wormzjl.createcheme.science.fluid.solver.SparseNewton;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.createcheme.science.fluid.thermo.ThermoDomainViolation;
import com.wormzjl.createcheme.science.material.MaterialCatalog;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * F4, T3 (option P1 of the F1 review): a pump's "maximum pressure rise" is the rise for water at 298.15 K and 1 atm, and
 * on what it actually withdraws its limit is that setting times the suction's bulk density over water's - a head. And
 * T2: a trial the property domain refuses is its own rejection key and the reason a failed interval gives.
 *
 * <p>Since the phase-ports batch (decisions D1, D2, D7) a pump refuses gas, so the nitrogen transfer between closed tanks is
 * a compressor's, closing at its pressure-ratio limit; the pump's density scaling is kept in the water test.
 */
class PumpRiseScalingTest {
    private static final String NETWORK="createcheme:tjl20_methane_nitrogen";
    private static final PipeResistance.Geometry LINE=new PipeResistance.Geometry(1,.05,.000045,0);
    private final FluidThermodynamics model=FluidThermodynamics.forNetwork(MaterialCatalog.bundled(),NETWORK);

    /** The compressor of the transfer: 0.1 m3/s of suction volume, ratio 1.05. */
    private static final double TARGET=.1,RATIO=1.05;
    /** Two closed 1 m3 nitrogen tanks at 298.15 K and 1 atm, a compressor from the first into the second. */
    private static PassiveNetwork transfer(FluidThermodynamics model){return transfer(model,101325);}
    private static PassiveNetwork transfer(FluidThermodynamics model,double pressure) {
        var a=model.initialNitrogenCharge(1,298.15,pressure,()->{});var b=model.initialNitrogenCharge(1,298.15,pressure,()->{});
        return new PassiveNetwork(List.of(new PassiveNetwork.Reservoir(1,0,a),new PassiveNetwork.Reservoir(2,0,b)),
                List.of(new PassiveNetwork.Pipe(10,0,1,LINE,new FlowControl.Compressor(TARGET,RATIO,1))));
    }

    @Test void theReferenceIsThisModelsOwnWaterAtRoomConditions() {
        double rho=model.waterMolecularWeight/model.waterLiquid(298.15,101325).molarVolume();
        assertEquals(rho,model.pumpReferenceDensity(),1e-12*rho);
        // IF97 Region 1 at the 2 MPa reference carried to 1 atm by the global 1e-9 1/Pa compressibility: 996.0 kg/m3.
        assertEquals(996.0,model.pumpReferenceDensity(),.1);
    }

    /**
     * Nitrogen compressed out of a closed tank into another (decision D7): the compressor reaches its ratio limit, the
     * discharge standing at {@code r_max} times the suction, and then its shutoff within a second. The suction tank is a
     * bag at the ambient (decision D54): it gives its gas at its own temperature and shrinks at the ambient pressure, the
     * atmosphere's work on it replacing the flow work of the gas that leaves, so it stays at 298.15 K and 101325 Pa; the
     * discharge, a full bag, is warmer by its compression and the shaft work booked as heat (decision D8) - and never
     * anywhere near the property domain's bounds. (Before D54 the suction tank was rigid and cooled a couple of kelvin by
     * its expansion; the strict "cooler" assertion of that time then held on a residue below 298.15 K until the semismooth
     * Jacobian of A423 left the suction 6e-12 K above it. Test name kept from the pump it was: the pump's 575 Pa
     * density-scaled shutoff became the ratio limit.)
     */
    @Test void aGasTransferBetweenClosedTanksClosesWithinASecond() {
        var graph=transfer(model);var solver=new PassiveIntervalSolver(model);double time=0,step=.05,work=0;
        PassiveIntervalSolver.Result result=null;double closedAt=Double.NaN;
        for(int k=0;k<40;k++) {
            result=solver.solve(graph,.05,PassiveIntervalSolver.Settings.defaults(),()->{},step);graph=result.graph();step=solver.nextStepEstimate();time+=.05;work+=result.pumpWorkJoule();
            if(Double.isNaN(closedAt)&&result.endpointModes().getFirst()==FlowControl.Mode.CLOSED)closedAt=time;
        }
        var suction=graph.reservoirs().get(0).state();var discharge=graph.reservoirs().get(1).state();double rise=result.endpointHeads()[0];
        System.out.printf(Locale.ROOT,"gas transfer (compressor, ratio %.2f): closed at %.2f s, suction %.6f K %.3f Pa, discharge %.6f K %.3f Pa, rise at shutoff %.4f Pa, limit %.4f Pa, work %.3f J%n",
                RATIO,closedAt,suction.temperature(),suction.pressure(),discharge.temperature(),discharge.pressure(),rise,(RATIO-1)*suction.pressure(),work);
        assertTrue(closedAt<=1,"the compressor reaches its shutoff within a second: "+closedAt);
        assertEquals(FlowControl.Mode.CLOSED,result.endpointModes().getFirst());assertEquals(0,result.averageMassFlows()[0]);
        // The pump's landing tolerance (3 Pa) on the limit; the head a closed device holds is the whole difference.
        assertEquals((RATIO-1)*suction.pressure(),rise,3,"the rise at shutoff is the ratio limit on the suction pressure");
        assertEquals(discharge.pressure()-suction.pressure(),rise,.05,"the compressor holds the whole pressure difference at shutoff");
        assertEquals(RATIO*suction.pressure(),discharge.pressure(),3,"the discharge stands at r_max times the suction");
        assertEquals(298.15,suction.temperature(),1e-6,"the suction bag gives its gas at its own temperature (D54): "+suction.temperature());
        assertEquals(101325,suction.pressure(),1e-3,"the suction bag stays at the ambient (D54): "+suction.pressure());
        assertTrue(discharge.temperature()>298.15,"the discharge is warmer: "+discharge.temperature());assertTrue(work>0);
        assertTrue(result.rejectionReasons().keySet().stream().noneMatch(k->k.startsWith(ThermoDomainViolation.REASON_PREFIX)),"never near the domain: "+result.rejectionReasons());
    }

    /** On water the limit is the setting within water's compressibility: the same shutoff as before P1. */
    @Test void onWaterTheLimitIsTheSettingWithinWatersCompressibility() {
        double[] water=new double[model.componentCount()];water[model.componentCount()-1]=1;
        for(double p:new double[]{101325,601325,2e6}) {
            var unit=model.flashTP(298.15,p,water,()->{});double rho=unit.mass()/unit.volume();
            assertEquals(1,rho/model.pumpReferenceDensity(),p*1e-9*1.01,"water at "+p+" Pa");
        }
    }

    /**
     * A trial the property domain refuses is counted under its own key, and an interval it stops fails with the
     * violation itself. Forced by narrowing the nitrogen record's fluid_domain to 298.05 K: the compressor's transfer
     * cools the suction past it (the pump's did by 0.24 K; the ratio limit lets the suction expand further).
     */
    @Test void aDomainRefusalIsItsOwnRejectionKeyAndTheReasonTheIntervalFails() {
        var resources=new HashMap<>(MaterialCatalog.bundled().resources());String key="data/createcheme/materials/properties/nitrogen.json";
        var o=JsonParser.parseString(resources.get(key)).getAsJsonObject();o.getAsJsonObject("fluid_domain").addProperty("temperature_min_kelvin",298.05);resources.put(key,o.toString());
        var narrowed=FluidThermodynamics.forNetwork(MaterialCatalog.parse(resources),NETWORK);
        var failure=assertThrows(SparseNewton.Nonconvergence.class,()->{
            // Above the ambient, so the drawn suction tank expands and cools: a slack bag at 1 atm (decision D54) shrinks at
            // the ambient pressure instead and never reaches the narrowed floor.
            var graph=transfer(narrowed,150000);var solver=new PassiveIntervalSolver(narrowed);
            for(int k=0;k<40;k++)graph=solver.solve(graph,.05,PassiveIntervalSolver.Settings.defaults(),()->{},solver.nextStepEstimate()>0?solver.nextStepEstimate():.05).graph();
        });
        var violation=failure.domainViolation();
        assertNotNull(violation,"the failed interval names the domain, not the Newton symptom: "+failure.getMessage());
        assertEquals("Nitrogen",violation.component());assertEquals(ThermoDomainViolation.Code.THERMO_DOMAIN_TEMPERATURE_BELOW,violation.code());
        assertEquals(298.05,violation.minimum());assertEquals(1,violation.node(),"the suction tank");
        assertTrue(failure.getMessage().contains("thermo-domain: Nitrogen temperature < 298.05 K="),"the rejection map counts it under its key: "+failure.getMessage());
        assertEquals(violation,SparseNewton.domainViolation(failure));
    }

    @Test void anIntervalFailsOnTheDomainWhenItsLastOrMostRejectionsWereDomainOnes() {
        var v=new ThermoDomainViolation(NETWORK,"Nitrogen",ThermoDomainViolation.Property.TEMPERATURE,60,63.151,900);
        assertSame(v,PassiveIntervalSolver.domainCause(true,v,1,20),"the last rejection");
        assertSame(v,PassiveIntervalSolver.domainCause(false,v,11,20),"most rejections");
        assertNull(PassiveIntervalSolver.domainCause(false,v,10,20),"met on the way, failed for another reason");
        assertNull(PassiveIntervalSolver.domainCause(false,null,0,20));
    }
}
