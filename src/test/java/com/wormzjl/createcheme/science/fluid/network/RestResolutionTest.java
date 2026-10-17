package com.wormzjl.createcheme.science.fluid.network;

import com.wormzjl.createcheme.fluid.support.FluidTestSupport;
import static org.junit.jupiter.api.Assertions.*;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.createcheme.science.fluid.transport.MixtureViscosity;
import com.wormzjl.createcheme.science.material.MaterialCatalog;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * Owner decisions D42 and D43 (PHASE_PORTS_REVIEW.md "The certificate is the gauge: rest at 0.1 Pa, zero-flow loops, saved
 * crude cases (2026-10-13)"): the rest step reads drives and flows against a gauge of 0.1 Pa, and a loop of connections
 * is at rest only when its net drive is below that too. The rule on graphs stated directly; the world gate tests are
 * {@code runtime.fluid.RestResolutionWorldTest}.
 */
class RestResolutionTest {
    private final FluidThermodynamics model=FluidTestSupport.networkModel();
    private final PipeResistance.Geometry run=new PipeResistance.Geometry(1,.05,PipeResistance.DEFAULT_ROUGHNESS_METRES,0);
    private FluidThermodynamics.State water(double pressure) {
        double[] n=new double[model.hydrocarbon.componentCount()+1];n[n.length-1]=1;
        return model.flashTP(298.15,pressure,n,()->{});
    }
    /** A water generator {@code excess} Pa above a 1 atm nitrogen void, through a water junction {@code junction} Pa above the
     * void, all level, 1 m runs. */
    private PassiveNetwork line(double excess,double junction) {
        return new PassiveNetwork(List.of(new PassiveNetwork.Reservoir(1,0,water(101325+excess),PassiveNetwork.NodeKind.GENERATOR),
                new PassiveNetwork.Reservoir(2,0,water(101325+junction),PassiveNetwork.NodeKind.JUNCTION),
                new PassiveNetwork.Reservoir(3,0,model.initialNitrogenCharge(1,298.15,101325,()->{}),PassiveNetwork.NodeKind.VOID)),
                List.of(new PassiveNetwork.Pipe(10,0,1,run),new PassiveNetwork.Pipe(11,1,2,run)));
    }
    /** {@link #line(double, double)} with the junction standing where the generator does. */
    private PassiveNetwork line(double excess){return line(excess,excess);}

    /** Restated under owner decision D58: the gauge stands where stock is (the generator and the void), and a junction's own
     * pressure is its relaxation row's state, no gauge; a drive is read from stock to stock along the junctions. */
    @Test void aDriveAtOrBelowTheResolutionIsNoDriveAndOneAboveItIsSolved() {
        assertTrue(NetworkReduction.atRest(line(.05),model),"the generator 0.05 Pa above its void: a gauge reads no difference");
        assertFalse(NetworkReduction.atRest(line(.2),model),"0.2 Pa drives the generator's water into the void");
        assertFalse(NetworkReduction.atRest(line(.2,0),model),"0.2 Pa drives it whatever the junction's own pressure");
        assertTrue(NetworkReduction.atRest(line(0,.2),model),"a junction 0.2 Pa above both 1 atm ends is no drive: it holds no stock");
        assertTrue(NetworkReduction.atRest(line(0,-.2),model),"nor 0.2 Pa below them");
        // The state is never rounded: the rest interval returns the graph to the last bit.
        var graph=line(.05);var rest=NetworkReduction.restInterval(graph,5);
        assertSame(graph,rest.graph());assertEquals(101325+.05,rest.graph().reservoirs().get(1).state().pressure(),0.0);
    }

    @Test void aFlowAboveWhatTheResolutionDrivesIsAFlow() {
        var graph=line(0);
        // Poiseuille on 1 m of 0.05 m: 0.1 Pa / (128 mu L / (pi rho d^4)), 0.017 kg/s in 298 K water (the generator's run,
        // water at both ends) and 0.001 kg/s in 1 atm nitrogen (the run into the void: the smaller of its two ends).
        double water=NetworkReduction.resolutionFlow(graph,model,0,new MixtureViscosity.Workspace());
        double q=NetworkReduction.resolutionFlow(graph,model,1,new MixtureViscosity.Workspace());
        assertEquals(.0172,water,.0172*.05,"water run's flow resolution");
        assertEquals(.00099,q,.00099*.05,"the run into the nitrogen void reads the gas end");
        assertTrue(NetworkReduction.atRest(graph,model,new double[]{.5*water,.5*q}),"half the resolution flow: no flow");
        assertFalse(NetworkReduction.atRest(graph,model,new double[]{0,2*q}),"twice the resolution flow: a flow, whatever the drive reads");
    }

    /** Four nitrogen junctions in a ring (0-1-2-3-0); node {@code fixed}, when not negative, is a generator instead. */
    private PassiveNetwork ring(int fixed) {
        var nodes=new ArrayList<PassiveNetwork.Reservoir>();var gas=model.initialNitrogenCharge(1,298.15,101325,()->{});
        for(int i=0;i<4;i++)nodes.add(new PassiveNetwork.Reservoir(i+1,0,gas,i==fixed?PassiveNetwork.NodeKind.GENERATOR:PassiveNetwork.NodeKind.JUNCTION));
        var pipes=new ArrayList<PassiveNetwork.Pipe>();for(int i=0;i<4;i++)pipes.add(new PassiveNetwork.Pipe(10+i,i,(i+1)%4,run));
        return new PassiveNetwork(nodes,pipes);
    }
    private static double[][] drives(double... forward){var d=new double[forward.length][];for(int i=0;i<d.length;i++)d[i]=new double[]{forward[i],-forward[i]};return d;}

    /** A 1 atm water generator, three junctions in a row and a 1 atm nitrogen void: a chain of four connections. */
    private PassiveNetwork chain() {
        var nodes=new ArrayList<PassiveNetwork.Reservoir>();
        nodes.add(new PassiveNetwork.Reservoir(1,0,water(101325),PassiveNetwork.NodeKind.GENERATOR));
        for(int i=0;i<3;i++)nodes.add(new PassiveNetwork.Reservoir(2+i,0,water(101325),PassiveNetwork.NodeKind.JUNCTION));
        nodes.add(new PassiveNetwork.Reservoir(5,0,model.initialNitrogenCharge(1,298.15,101325,()->{}),PassiveNetwork.NodeKind.VOID));
        var pipes=new ArrayList<PassiveNetwork.Pipe>();for(int i=0;i<4;i++)pipes.add(new PassiveNetwork.Pipe(10+i,i,i+1,run));
        return new PassiveNetwork(nodes,pipes);
    }

    @Test void aPathOfJunctionsIsDrivenByTheDifferenceBetweenTheNodesAtItsEnds() {
        var chain=chain();
        assertTrue(NetworkReduction.pathDriven(chain,drives(.05,.05,.05,.05)),"0.05 Pa on each tee: 0.2 Pa between the generator and the void");
        assertFalse(NetworkReduction.pathDriven(chain,drives(.02,.02,.02,.02)),"0.08 Pa end to end: below the gauge");
        assertFalse(NetworkReduction.pathDriven(chain,drives(.05,-.05,.05,-.05)),"no drive between the ends");
    }

    @Test void aLoopIsDrivenByItsNetDriveNotByEachConnection() {
        var ring=ring(-1);
        assertTrue(NetworkReduction.loopDriven(ring,drives(.05,.05,.05,.05)),"0.05 Pa on each of four connections: 0.2 Pa net around the loop circulates");
        assertFalse(NetworkReduction.loopDriven(ring,drives(.02,.02,.02,.02)),"0.08 Pa net: below the gauge, no circulation");
        assertFalse(NetworkReduction.loopDriven(ring,drives(.05,-.05,.05,-.05)),"no net drive around the loop");
        var blocked=drives(.05,.05,.05,.05);blocked[2][0]=Double.NEGATIVE_INFINITY;
        assertFalse(NetworkReduction.loopDriven(ring,blocked),"a direction one connection forbids carries no circulation");
        assertFalse(NetworkReduction.loopDriven(ring(2),drives(.05,.05,.05,.05)),"nothing circulates through a generator");
    }

    // ---- Owner decision D36: a gas column does not exist (documentation/2026-09-24-coolprop-low-temperature) ----
    private final PipeResistance.Geometry riser=new PipeResistance.Geometry(2,.05,PipeResistance.DEFAULT_ROUGHNESS_METRES,0);
    /** A 1 m3 nitrogen tank at random seed 498's generator pressure, 480 kPa, and {@code temperature}. */
    private FluidThermodynamics.State nitrogen(double temperature){return model.initialNitrogenCharge(1,temperature,480371.272983223,()->{});}
    private static final double COLD=272.72073259613035,HOT=COLD+100;

    /**
     * D36, gate (a): two 1 m3 nitrogen tanks at 480 kPa and 272.7 K, one 2 m above the other, joined by a 2 m riser of
     * 0.05 m pipe, rest at one pressure. The gas weighs nothing, so the riser has no drive either way, the gauge reads the
     * tanks at rest, and a 5 s step carries nothing and moves neither pressure. Weighed (5.94 kg/m3, 58 Pa per metre), the
     * riser drove the upper tank's gas down with 116 Pa until the lower tank stood that much higher.
     */
    @Test void twoGasTanksJoinedByARiserRestAtOnePressure() {
        var graph=new PassiveNetwork(List.of(new PassiveNetwork.Reservoir(1,0,nitrogen(COLD)),new PassiveNetwork.Reservoir(2,2,nitrogen(COLD))),
                List.of(new PassiveNetwork.Pipe(10,0,1,riser)));
        var drive=NetworkReduction.drives(graph,model);
        assertEquals(0,drive[0][0],0.0,"the riser's gas weighs nothing: no drive up");
        assertEquals(0,drive[0][1],0.0,"and none down");
        assertTrue(NetworkReduction.atRest(graph,model),"the gauge reads the two tanks at rest");
        var step=new PassiveStepSolver(model).solve(graph,5,()->{});
        System.out.println("D36_RISER flow="+step.massFlows()[0]+" lower P="+step.states().get(0).pressure()+" upper P="+step.states().get(1).pressure());
        assertEquals(0,step.massFlows()[0],1e-12,"a 5 s step carries nothing through the riser");
        for(int i=0;i<2;i++)assertEquals(480371.272983223,step.states().get(i).pressure(),1e-6,"tank "+i+" keeps its pressure");
    }

    /**
     * D36, gate (b): a loop of gas risers carries no circulation. Four 1 m3 nitrogen tanks at 480 kPa stand at the corners of
     * a vertical square, joined by 2 m risers of 0.05 m pipe up its two sides and 1 m level runs across its top and bottom;
     * the two tanks up one side are at 272.7 K, the two up the other at 372.7 K. The gas weighs nothing, so the loop's net
     * drive is zero whatever its temperatures: the gauge reads no circulation and a 5 s step carries nothing in any
     * connection. Weighed, the cold riser (5.94 kg/m3) outweighed the hot one (4.34 kg/m3) by 31 Pa round the loop, a
     * thermosiphon the gauge read as a circulation.
     */
    @Test void aLoopOfGasRisersCarriesNoCirculation() {
        var nodes=List.of(new PassiveNetwork.Reservoir(1,0,nitrogen(COLD)),new PassiveNetwork.Reservoir(2,0,nitrogen(HOT)),
                new PassiveNetwork.Reservoir(3,2,nitrogen(HOT)),new PassiveNetwork.Reservoir(4,2,nitrogen(COLD)));
        var loop=new PassiveNetwork(nodes,List.of(new PassiveNetwork.Pipe(10,0,1,run),new PassiveNetwork.Pipe(11,1,2,riser),
                new PassiveNetwork.Pipe(12,2,3,run),new PassiveNetwork.Pipe(13,3,0,riser)));
        var drives=NetworkReduction.drives(loop,model);double net=0;for(var drive:drives)net+=drive[0];
        assertEquals(0,net,0.0,"no net drive round the loop");
        assertFalse(NetworkReduction.loopDriven(loop,drives),"the gauge reads no circulation");
        var step=new PassiveStepSolver(model).solve(loop,5,()->{});
        System.out.println("D36_LOOP net="+net+" flows="+Arrays.toString(step.massFlows()));
        for(int i=0;i<4;i++)assertEquals(0,step.massFlows()[i],1e-12,"a 5 s step carries nothing in connection "+i);
    }
}
