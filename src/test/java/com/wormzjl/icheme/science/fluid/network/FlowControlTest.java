package com.wormzjl.icheme.science.fluid.network;

import static org.junit.jupiter.api.Assertions.*;
import com.wormzjl.icheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.icheme.science.material.MaterialCatalog;
import java.util.*;
import org.junit.jupiter.api.Test;

class FlowControlTest {
    private final FluidThermodynamics model=com.wormzjl.icheme.fluid.support.FluidTestSupport.model(MaterialCatalog.bundled(),"icheme:tjl20_methane");
    /** Two 1 m3 vessels of one component at 350 K and {@code pa}, {@code pb}, joined by 10 m of 5 cm pipe with {@code control}. */
    private PassiveNetwork line(double pa,double pb,int component,FlowControl control) {
        var nodes=new ArrayList<PassiveNetwork.Reservoir>();
        for(double p:new double[]{pa,pb}){double[] n=new double[com.wormzjl.icheme.science.material.MaterialTestBasis.CRUDE+1];n[component]=1;var unit=model.flashTP(350,p,n,()->{});n[component]/=unit.volume();nodes.add(new PassiveNetwork.Reservoir(nodes.size(),0,model.flashTP(350,p,n,()->{})));}
        return new PassiveNetwork(nodes,List.of(new PassiveNetwork.Pipe(5,0,1,new PipeResistance.Geometry(10,.05,.000045,0),control)));
    }
    private PassiveStepSolver.Result run(double pa,double pb,int component,FlowControl control,double dt) {
        return new PassiveStepSolver(model).solve(line(pa,pb,component,control),dt,()->{});
    }
    /** Now a compressor on methane (decision D2; a liquid-only pump refuses gas, decision D1; test name kept): its target is the suction volume
     * flow at inlet conditions, and its shaft work the ideal isothermal work on suction properties (decision D8). A ratio
     * of 3 lifts 1 atm suction by 203 kPa, above the 99 kPa the discharge vessel stands higher. */
    @Test void pumpMeetsItsSuctionFlowTargetAndAccountsForAddedEnergy() {
        var result=run(101325,200000,0,new FlowControl.Compressor(.001,3),.1);var suction=result.states().getFirst();
        assertEquals(.001,result.massFlows()[0]/(suction.mass()/suction.volume()),1e-10);
        assertEquals(FlowControl.Mode.PUMP_TARGET,result.modes().getFirst());assertTrue(result.devicePressureChanges()[0]>0);assertTrue(result.pumpWorkJoule()>0);
        double q=result.massFlows()[0],head=result.devicePressureChanges()[0],p=suction.pressure(),rho=suction.mass()/suction.volume();
        double expected=.1*q*(p/rho)*Math.log1p(head/p);
        assertEquals(expected,result.pumpWorkJoule(),1e-6*expected,"isothermal work on the suction's pressure and density over the step");
    }
    @Test void naturalFlowIsLimitedWithoutInventedPumpWorkAndOpposingHeadShutsThePump() {
        var natural=run(300000,101325,0,new FlowControl.Compressor(.001,3),.1);
        assertTrue(natural.devicePressureChanges()[0]<0);assertEquals(0,natural.pumpWorkJoule());assertTrue(natural.massFlows()[0]>0);
        // 8 bar against 1 atm suction is a ratio of 7.9: a 3:1 machine cannot open.
        var shutoff=run(101325,800000,0,new FlowControl.Compressor(.001,3),.1);
        assertEquals(FlowControl.Mode.CLOSED,shutoff.modes().getFirst());assertEquals(0,shutoff.massFlows()[0],1e-10);assertEquals(0,shutoff.pumpWorkJoule(),1e-6);
    }
    @Test void pressureValveClosesBelowTargetAndRegulatesAboveIt() {
        var closed=run(101325,80000,model.componentCount()-1,new FlowControl.PressureValve(175000),.1);
        assertEquals(FlowControl.Mode.CLOSED,closed.modes().getFirst());assertEquals(0,closed.massFlows()[0],1e-10);
        var regulated=run(200000,101325,model.componentCount()-1,new FlowControl.PressureValve(175000),.1);
        assertEquals(FlowControl.Mode.VALVE_REGULATING,regulated.modes().getFirst());assertEquals(175000,regulated.states().getFirst().pressure(),.01);
        assertTrue(regulated.massFlows()[0]>0);assertTrue(regulated.devicePressureChanges()[0]>0);
    }
    @Test void modularFlowValveMeetsMassAndActualVolumeTargetsWithoutWork(){
        for(var source:java.util.List.of(FlowControl.ValveSource.MASS_FLOW,FlowControl.ValveSource.VOLUME_FLOW)){
            double target=source==FlowControl.ValveSource.MASS_FLOW?.01:.001;
            var result=run(300000,101325,0,new FlowControl.ControlValve(source,target,new java.util.UUID(1,1),1),.1);
            assertEquals(FlowControl.Mode.VALVE_REGULATING,result.modes().getFirst());
            double rho=result.states().getFirst().mass()/result.states().getFirst().volume();
            assertEquals(target,source==FlowControl.ValveSource.MASS_FLOW?result.massFlows()[0]:result.massFlows()[0]/rho,1e-8*target);
            assertTrue(result.devicePressureChanges()[0]>=0);assertEquals(0,result.pumpWorkJoule(),1e-12);
        }
    }
    @Test void bareValveIsFullyOpenAndLargeFlowTargetSaturates(){
        var passive=run(300000,101325,0,new FlowControl.Passive(),.1);
        var bare=run(300000,101325,0,FlowControl.ControlValve.unconfigured(),.1);
        assertEquals(passive.massFlows()[0],bare.massFlows()[0],1e-8*Math.abs(passive.massFlows()[0]));
        assertEquals(FlowControl.Mode.VALVE_OPEN,bare.modes().getFirst());
        var saturated=run(300000,101325,0,new FlowControl.ControlValve(FlowControl.ValveSource.MASS_FLOW,100,new java.util.UUID(1,2),1),.1);
        assertEquals(FlowControl.Mode.VALVE_OPEN,saturated.modes().getFirst());
        assertTrue(saturated.massFlows()[0]<100);
    }
    @Test void removingASensorRetainsIndependentSiTargets(){
        var v=new FlowControl.ControlValve(FlowControl.ValveSource.PRESSURE,350000,2,.03,new java.util.UUID(1,1),1);
        var removed=v.withSource(v.source(),null,2);assertTrue(removed.bare());assertEquals(350000,removed.targetPressure());assertEquals(.03,removed.volumeTarget());assertEquals(2,removed.massTarget());
    }
    @Test void twoFlowValvesInSeriesDoNotCreateADependentSolve(){
        double[] n=new double[model.componentCount()];n[0]=1;
        var a=model.flashTP(350,300000,n,()->{});double[] n2=n.clone();for(int c=0;c<n.length;c++)n[c]/=a.volume();a=model.flashTP(350,300000,n,()->{});var b=model.flashTP(350,101325,n2,()->{});for(int c=0;c<n2.length;c++)n2[c]/=b.volume();b=model.flashTP(350,101325,n2,()->{});
        var nodes=java.util.List.of(new PassiveNetwork.Reservoir(1,0,a,PassiveNetwork.NodeKind.GENERATOR),new PassiveNetwork.Reservoir(2,0,b,PassiveNetwork.NodeKind.JUNCTION),new PassiveNetwork.Reservoir(3,0,b,PassiveNetwork.NodeKind.VOID));
        var line=new PipeResistance.Geometry(10,.05,.000045,0);
        for(double second:new double[]{.01,.02}){
            var graph=new PassiveNetwork(nodes,java.util.List.of(new PassiveNetwork.Pipe(5,0,1,line,new FlowControl.ControlValve(FlowControl.ValveSource.MASS_FLOW,.01,new java.util.UUID(1,1),1)),
                    new PassiveNetwork.Pipe(6,1,2,line,new FlowControl.ControlValve(FlowControl.ValveSource.MASS_FLOW,second,new java.util.UUID(1,2),1))));
            var result=new PassiveStepSolver(model).solve(graph,.1,()->{});
            assertTrue(result.massFlows()[0]>=0);assertTrue(result.massFlows()[1]>=0);
            assertEquals(List.of(FlowControl.Mode.VALVE_CONFLICT,FlowControl.Mode.VALVE_CONFLICT),result.modes());
            assertArrayEquals(new double[]{0,0},result.devicePressureChanges(),1e-5);
            var rate=new PassiveStepSolver(model).solveRate(graph,()->{});assertEquals(result.modes(),rate.modes());
            assertArrayEquals(result.massFlows(),new PassiveStepSolver(model).solve(graph,.1,()->{}).massFlows());
        }
    }
    @Test void conflictFallbackUsesAcceptedOpeningAndLeavesIndependentLoopActive(){
        double[] n=new double[model.componentCount()];n[0]=1;
        var high=model.flashTP(350,300000,n,()->{});var low=model.flashTP(350,101325,n,()->{});
        var nodes=List.of(new PassiveNetwork.Reservoir(1,0,high,PassiveNetwork.NodeKind.GENERATOR),new PassiveNetwork.Reservoir(2,0,low,PassiveNetwork.NodeKind.JUNCTION),new PassiveNetwork.Reservoir(3,0,low,PassiveNetwork.NodeKind.VOID));
        var line=new PipeResistance.Geometry(10,.05,.000045,0);
        var valve=new PassiveNetwork.Pipe(5,0,1,line,new FlowControl.ControlValve(FlowControl.ValveSource.MASS_FLOW,.01,new UUID(1,1),1)).withActuatorArea(Math.PI*.08*.08/4);
        var original=new PassiveNetwork(nodes,List.of(valve,new PassiveNetwork.Pipe(6,1,2,line,new FlowControl.Passive())));
        var solver=new PassiveStepSolver(model);var accepted=solver.solve(original,.1,()->{});solver.markBackwardEulerAccepted(original,accepted);
        double rho=accepted.states().getFirst().mass()/accepted.states().getFirst().volume(),area=valve.controlArea();assertNotEquals(area,valve.minimumArea());
        double opening=FlowControl.opening(accepted.massFlows()[0],accepted.devicePressureChanges()[0],rho,area);assertTrue(opening>0&&opening<1);
        var other=new PassiveNetwork.Pipe(6,1,2,line,new FlowControl.ControlValve(FlowControl.ValveSource.MASS_FLOW,.02,new UUID(1,2),1));
        var independent=new PassiveNetwork.Pipe(7,0,2,line,new FlowControl.ControlValve(FlowControl.ValveSource.MASS_FLOW,.005,new UUID(1,3),1));
        var conflict=new PassiveNetwork(nodes,List.of(valve,other,independent));var result=solver.solve(conflict,.1,()->{});
        assertEquals(FlowControl.Mode.VALVE_CONFLICT,result.modes().getFirst());assertEquals(FlowControl.Mode.VALVE_REGULATING,result.modes().get(2));
        rho=result.states().getFirst().mass()/result.states().getFirst().volume();
        assertEquals(opening,FlowControl.opening(result.massFlows()[0],result.devicePressureChanges()[0],rho,area),1e-8);
        assertEquals(.005,result.massFlows()[2],1e-10);assertEquals(0,result.pumpWorkJoule());
    }

    @Test void aBareValvePassesReverseFlowLikeItsPipe(){
        var plain=run(101325,300000,0,new FlowControl.Passive(),.1);var bare=run(101325,300000,0,FlowControl.ControlValve.unconfigured(),.1);
        assertTrue(bare.massFlows()[0]<0);assertEquals(plain.massFlows()[0],bare.massFlows()[0],1e-10);assertEquals(0,bare.devicePressureChanges()[0],1e-8);
    }
    /** A configured flow valve only throttles: its island rests when nothing drives through it, as the solve finds. */
    @Test void aFlowValveRestsWhenNothingDrivesThroughIt(){
        for(var source:List.of(FlowControl.ValveSource.MASS_FLOW,FlowControl.ValveSource.VOLUME_FLOW)){
            var valve=new FlowControl.ControlValve(source,source==FlowControl.ValveSource.MASS_FLOW?.01:.001,new UUID(1,1),1);
            assertTrue(NetworkReduction.atRest(line(200000,200000,0,valve),model),source+": no drive");
            assertTrue(NetworkReduction.atRest(line(101325,300000,0,valve),model),source+": a drive against it");
            assertEquals(0,run(101325,300000,0,valve,.1).massFlows()[0],1e-12,source+": it passes nothing back");
            assertFalse(NetworkReduction.atRest(line(300000,101325,0,valve),model),source+": a drive through it");
            assertTrue(NetworkReduction.atRest(line(300000,101325,0,valve.withTarget(0)),model),source+": a zero target holds it shut");
            assertEquals(0,run(300000,101325,0,valve.withTarget(0),.1).massFlows()[0],1e-12,source+": the solve passes nothing at a zero target");
        }
    }

}
