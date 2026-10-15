package com.wormzjl.createcheme.science.fluid.network;

import com.wormzjl.createcheme.fluid.support.FluidTestSupport;
import static org.junit.jupiter.api.Assertions.*;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.createcheme.science.material.MaterialCatalog;
import java.util.*;
import org.junit.jupiter.api.Test;
import com.wormzjl.createcheme.science.fluid.solver.PhaseLayout;
import com.wormzjl.createcheme.science.column.v3.thermo.V3PengRobinsonThermo;

/**
 * The backward-Euler step and the interval controller built on it: first-order convergence against refined fixed steps,
 * the boundary ledgers of a water-appearance step and of a source-to-void step, pump work against the energy balance,
 * and a valve closure against a refined fixed-step reference. Formerly {@code TrBdf2Test}, named for the TR-BDF2
 * integrator that the mixed-gas junction batch replaced (WP1, removed at {@code 640d87b}; renamed in WP5).
 */
class BackwardEulerStepTest {
    /** The controlled interval agrees with a refined fixed-step integration, and halving a fixed step halves the error
     * (first order; measured ratio 2.03, where TR-BDF2 showed more than 3). */
    @Test void controlledIntervalAgreesWithFixedStepsAndFixedStepsShowFirstOrder() {
        var graph=graph(gas(1,180000,0),gas(2,101325,0),new FlowControl.Passive());
        var controlled=new PassiveIntervalSolver(model).solve(graph,.2,PassiveIntervalSolver.Settings.defaults(),()->{});
        double reference=integratedMass(graph,256),coarse=Math.abs(integratedMass(graph,4)-reference),fine=Math.abs(integratedMass(graph,8)-reference);
        assertEquals(reference,controlled.averageMassFlows()[0]*.2,CONTROLLED_TOLERANCE*Math.abs(reference));
        assertTrue(coarse/fine>1.8&&coarse/fine<2.5,"Observed error ratio="+coarse/fine);
    }
    /** Measured 0.88 % against 256 fixed steps. */
    private static final double CONTROLLED_TOLERANCE=.015;
    private double integratedMass(PassiveNetwork initial,int steps) {
        var graph=initial;var solver=new PassiveStepSolver(model);double mass=0;
        for(int i=0;i<steps;i++){var result=solver.solve(graph,.2/steps,()->{});graph=PassiveIntervalSolver.replace(graph,result);mass+=result.massFlows()[0]*.2/steps;}
        return mass;
    }
    private final FluidThermodynamics model=FluidTestSupport.networkModel();
    private PassiveNetwork.Reservoir gas(long id,double pressure,double elevation) {
        return new PassiveNetwork.Reservoir(id,elevation,model.initialNitrogenCharge(1,298.15,pressure,()->{}));
    }
    private PassiveNetwork graph(PassiveNetwork.Reservoir a,PassiveNetwork.Reservoir b,FlowControl control) {
        return new PassiveNetwork(List.of(a,b),List.of(new PassiveNetwork.Pipe(10,0,1,new PipeResistance.Geometry(10,.05,.000045,0),control)));
    }
    @Test void waterAppearanceConservesNitrogenAndTheIntegratedBoundaryLedger() {
        var n=new double[com.wormzjl.createcheme.science.material.MaterialTestBasis.NETWORK+1];n[com.wormzjl.createcheme.science.material.MaterialTestBasis.NETWORK]=1;
        var source=new PassiveNetwork.Reservoir(1,0,model.flashTP(298.15,200000,n,()->{}),PassiveNetwork.NodeKind.GENERATOR);
        var graph=graph(source,gas(2,101325,2),new FlowControl.Passive());
        var result=new PassiveIntervalSolver(model).solve(graph,.05,PassiveIntervalSolver.Settings.defaults(),()->{});
        var old=graph.reservoirs().get(1).inventory();var next=result.graph().reservoirs().get(1).inventory();
        double[] incoming=new double[com.wormzjl.createcheme.science.material.MaterialTestBasis.NETWORK+1];double energy=0;
        for(var entry:result.boundaries()){
            // Decision D54: the 1 atm tank is a bag; the water it takes evaporates into its dry nitrogen and cools it, so the
            // bag shrinks at the ambient pressure, and the ambient's work on it is booked at the tank, energy alone.
            if(entry.nodeId()==2){assertTrue(java.util.Arrays.stream(entry.moles()).allMatch(m->m==0),"the ambient moves no material");energy+=entry.totalEnergyJoule();continue;}
            assertEquals(1,entry.nodeId(),"a transfer at "+entry.nodeId()+": "+entry.totalEnergyJoule()+" J, "+java.util.Arrays.toString(entry.moles()));var amounts=entry.moles();for(int c=0;c<com.wormzjl.createcheme.science.material.MaterialTestBasis.NETWORK+1;c++)incoming[c]+=amounts[c];energy+=entry.totalEnergyJoule();}
        var before=old.moles();var after=next.moles();double movedMass=0;
        for(int c=0;c<com.wormzjl.createcheme.science.material.MaterialTestBasis.NETWORK+1;c++){assertEquals(before[c]+incoming[c],after[c],1e-8);movedMass+=incoming[c]*(c==com.wormzjl.createcheme.science.material.MaterialTestBasis.NETWORK?model.waterMolecularWeight:model.hydrocarbon.molecularWeight(c));}
        assertTrue(after[com.wormzjl.createcheme.science.material.MaterialTestBasis.NETWORK]>0);assertEquals(before[com.wormzjl.createcheme.science.material.MaterialTestBasis.NITROGEN],after[com.wormzjl.createcheme.science.material.MaterialTestBasis.NITROGEN],1e-9);
        assertEquals(energy,next.internalEnergy()-old.internalEnergy()+movedMass*2*PassiveStepSolver.GRAVITY,1e-3);
    }
    @Test void pumpWorkIncludesAllStagesAndElevationEnergy() {
        // Nitrogen is moved by a compressor (decision D2; test name kept): its isothermal shaft work over the efficiency is
        // booked as heat into the discharge (decision D8), and the ledger holds that work exactly.
        // Above the ambient: drawn at 1 atm, the suction tank (a bag, decision D54) would go slack and take the ambient's
        // work on its shrinking bag into the ledger this test states for the shaft work alone.
        var suction=gas(1,150000,0);
        var graph=graph(suction,gas(2,200000,10),new FlowControl.Compressor(.001,3,.8));
        var result=new PassiveStepSolver(model).solve(graph,.2,()->{});
        assertTrue(result.pumpWorkJoule()>0);double change=0;
        for(int i=0;i<2;i++){var old=graph.reservoirs().get(i).inventory();var next=result.inventories().get(i);double dm=(next.moles()[com.wormzjl.createcheme.science.material.MaterialTestBasis.NITROGEN]-old.moles()[com.wormzjl.createcheme.science.material.MaterialTestBasis.NITROGEN])*model.hydrocarbon.molecularWeight(com.wormzjl.createcheme.science.material.MaterialTestBasis.NITROGEN);change+=next.internalEnergy()-old.internalEnergy()+dm*graph.reservoirs().get(i).elevation()*PassiveStepSolver.GRAVITY;}
        assertEquals(result.pumpWorkJoule(),change,1e-5);assertTrue(result.boundaries().isEmpty());
    }
    @Test void sourceAndVoidKeepSeparateNonzeroLedgersDespiteZeroNetTransfer() {
        var source=new PassiveNetwork.Reservoir(1,0,gas(1,200000,0).state(),PassiveNetwork.NodeKind.GENERATOR);
        var sink=new PassiveNetwork.Reservoir(2,0,gas(2,101325,0).state(),PassiveNetwork.NodeKind.VOID);
        var result=new PassiveStepSolver(model).solve(graph(source,sink,new FlowControl.Passive()),2,()->{});
        double sourceMoles=0,sinkMoles=0;for(var entry:result.boundaries()){if(entry.nodeId()==1)sourceMoles+=entry.moles()[com.wormzjl.createcheme.science.material.MaterialTestBasis.NITROGEN];else sinkMoles+=entry.moles()[com.wormzjl.createcheme.science.material.MaterialTestBasis.NITROGEN];}
        assertTrue(sourceMoles>0);assertEquals(sourceMoles,-sinkMoles,1e-9);
        assertEquals(result.massFlows()[0]*2,sourceMoles*model.hydrocarbon.molecularWeight(com.wormzjl.createcheme.science.material.MaterialTestBasis.NITROGEN),1e-9);
    }
    @Test void valveTransitionMatchesRefinedBackwardEulerWithoutFlowAfterClosure() {
        var graph=graph(gas(1,180000,0),gas(2,101325,0),new FlowControl.PressureValve(175000));
        var adaptive=new PassiveIntervalSolver(model).solve(graph,1,PassiveIntervalSolver.Settings.defaults(),()->{});
        var reference=graph;double moved=0;var be=new PassiveStepSolver(model);
        for(int i=0;i<400;i++){
            PassiveStepSolver.Result step;
            try{step=be.solve(reference,.0025,()->{});}catch(RuntimeException failure){throw new AssertionError("Reference step "+i+" Pa="+reference.reservoirs().getFirst().state().pressure(),failure);}
            reference=PassiveIntervalSolver.replace(reference,step);moved+=.0025*step.massFlows()[0];
        }
        assertEquals(moved,adaptive.averageMassFlows()[0],.005*Math.abs(moved)+1e-9);
        assertEquals(reference.reservoirs().getFirst().state().pressure(),adaptive.graph().reservoirs().getFirst().state().pressure(),1);
        var closed=new PassiveIntervalSolver(model).solve(adaptive.graph(),1,PassiveIntervalSolver.Settings.defaults(),()->{});
        assertEquals(0,closed.averageMassFlows()[0],1e-8);
    }

    // ---------------- from BoundaryAndPhaseTest ----------------
    private PassiveNetwork.Pipe pipe(){return new PassiveNetwork.Pipe(3,0,1,new PipeResistance.Geometry(10,.05,.000045,0));}
    private PassiveNetwork.Reservoir source(double pressure) {
        double[] n=new double[model.hydrocarbon.componentCount()+1];n[n.length-1]=1;
        return new PassiveNetwork.Reservoir(1,0,model.flashTP(298.15,pressure,n,()->{}),PassiveNetwork.NodeKind.GENERATOR);
    }
    @Test void sourceFillsNitrogenChargedVesselAndWaterAppearsWithoutLosingTheInitialNitrogen() {
        var initial=model.initialNitrogenCharge(1,298.15,101325,()->{});
        var graph=new PassiveNetwork(List.of(source(200000),new PassiveNetwork.Reservoir(2,0,initial)),List.of(pipe()));
        var result=new PassiveStepSolver(model).solve(graph,.01,()->{});var filled=result.states().get(1);
        int nitrogen=model.hydrocarbon.components().indexOf("Nitrogen"),water=model.hydrocarbon.componentCount();
        assertEquals(initial.vapor()[nitrogen],PhaseLayout.totalAmounts(filled)[nitrogen],1e-8);
        assertTrue(filled.waterLiquid()>0&&filled.waterVapor()>0);assertTrue(result.massFlows()[0]>0);
        assertEquals(result.externalMoles()[water],filled.waterLiquid()+filled.waterVapor(),1e-8);
        assertEquals(initial.internalEnergy()+result.externalEnergyJoule(),filled.internalEnergy(),.001);
        assertEquals(graph.reservoirs().getFirst().state(),result.states().getFirst());
    }
    @Test void sourceToVoidHasSteadyThroughputAndBothBoundariesRejectReverseFlow() {
        var sink=new PassiveNetwork.Reservoir(2,0,model.initialNitrogenCharge(1,298.15,101325,()->{}),PassiveNetwork.NodeKind.VOID);
        var graph=new PassiveNetwork(List.of(source(200000),sink),List.of(pipe()));
        var result=new PassiveStepSolver(model).solve(graph,1,()->{});assertTrue(result.massFlows()[0]>0);
        assertArrayEquals(new double[com.wormzjl.createcheme.science.material.MaterialTestBasis.NETWORK+1],result.externalMoles(),1e-10);assertEquals(0,result.externalEnergyJoule(),1e-10);
        var highSink=new PassiveNetwork.Reservoir(2,0,model.initialNitrogenCharge(1,298.15,300000,()->{}),PassiveNetwork.NodeKind.VOID);
        var blocked=new PassiveStepSolver(model).solve(new PassiveNetwork(List.of(source(200000),highSink),List.of(pipe())),1,()->{});
        assertEquals(0,blocked.massFlows()[0],1e-10);
    }
    @Test void aThreePhaseWetCrudeFeedCanEnterNitrogenWithoutLosingAnyComponent() {
        String id="createcheme:tjl20_methane_nitrogen";var n=Arrays.copyOf(V3PengRobinsonThermo.fromRegisteredPackage(id).crudeFeed("createcheme:tia_juana_light_methane").moleFractions(),com.wormzjl.createcheme.science.material.MaterialTestBasis.NETWORK+1);n[com.wormzjl.createcheme.science.material.MaterialTestBasis.NETWORK]=.2;
        var source=new PassiveNetwork.Reservoir(1,0,model.flashTP(350,200000,n,()->{}),PassiveNetwork.NodeKind.GENERATOR);
        var initial=model.initialNitrogenCharge(1,298.15,101325,()->{});
        var graph=new PassiveNetwork(List.of(source,new PassiveNetwork.Reservoir(2,0,initial)),List.of(pipe()));
        var result=new PassiveStepSolver(model).solve(graph,.01,()->{});var state=result.states().get(1);
        assertTrue(source.state().liquidVolume()>0&&source.state().waterVolume()>0&&source.state().vaporVolume()>0);
        assertTrue(result.massFlows()[0]>0);assertTrue(state.liquidVolume()>0);assertTrue(state.vaporVolume()>0);
        // A small incoming free-water amount may all evaporate into the nitrogen headspace.
        assertTrue(state.waterLiquid()+state.waterVapor()>0);
        assertTrue(state.waterPartialPressure()<=model.saturationPressure(state.temperature())*(1+1e-8));
        assertArrayEquals(result.externalMoles(),difference(PhaseLayout.totalAmounts(state),PhaseLayout.totalAmounts(initial)),1e-8);
    }
    private static double[] difference(double[] a,double[] b){for(int i=0;i<a.length;i++)a[i]-=b[i];return a;}
}
