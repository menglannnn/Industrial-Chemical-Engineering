package com.wormzjl.icheme.science.fluid.network;

import com.wormzjl.icheme.science.fluid.network.ConservativeTransport.BoundaryTransfer.Kind;
import com.wormzjl.icheme.science.fluid.network.PassiveNetwork.PhasePort;
import com.wormzjl.icheme.science.fluid.solver.PhaseLayout;
import com.wormzjl.icheme.science.fluid.state.SolidInventory;
import com.wormzjl.icheme.science.fluid.thermo.FluidThermodynamics;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** t895 probe (tools/floating-roof-tanks/trace-895, never tracked): a gas-only open tank under the owner's rule of 2026-10-21. */
class OpenTankGasProbeTest extends PortFixture {
    static final double P0=PhaseLayout.AMBIENT_PRESSURE;
    static PassiveNetwork.Reservoir openTank(long id,FluidThermodynamics.State s,double volume) {
        return new PassiveNetwork.Reservoir(id,0,s,PassiveNetwork.NodeKind.RESERVOIR,new PassiveNetwork.Inventory(volume,PhaseLayout.totalAmounts(s),s.internalEnergy(),SolidInventory.EMPTY,1,true));
    }
    static PassiveNetwork.Pipe pipe(long id,int a,int b){return new PassiveNetwork.Pipe(id,a,b,List.of(new PipeResistance.Geometry(5,.025,PipeResistance.DEFAULT_ROUGHNESS_METRES,0)),new FlowControl.Passive(),0,null,PhasePort.BULK,PhasePort.BULK);}
    void drive(String label,PassiveNetwork graph,double interval,int count) {
        var solver=new PassiveIntervalSolver(model);PassiveIntervalSolver.Result committed=null;
        double[] initial=owned(graph),external=new double[mw.length];double e0=ownedEnergy(graph),ext=0;
        for(int slice=0;slice<count;slice++) {
            solver.replayStart(graph,committed);
            var r=solver.solve(graph,interval,PassiveIntervalSolver.Settings.defaults(),NOOP);committed=r;graph=r.graph();
            double vent=0;for(var b:r.boundaries()){var n=b.moles();for(int c=0;c<n.length;c++)external[c]+=n[c];ext+=b.totalEnergyJoule();if(b.kind()==Kind.VENT)vent-=n[nitrogen];}
            ext+=r.pumpWorkJoule();
            var now=owned(graph);double worst=0;for(int c=0;c<now.length;c++)worst=Math.max(worst,Math.abs(now[c]-initial[c]-external[c])/Math.max(1,initial[c]));
            double eerr=Math.abs(ownedEnergy(graph)-e0-ext)/Math.max(1,Math.abs(e0));
            var t=graph.reservoirs().getFirst();
            System.out.println("PROBE "+label+" slice="+slice+" advanced="+r.advancedSeconds()+" acc="+r.acceptedSubsteps()+" rej="+r.rejectedSubsteps()+" tankN2="+t.inventory().moles()[nitrogen]+" empty="+t.empty()+" P="+t.state().pressure()+" T="+t.state().temperature()+" vent="+vent+" ledger="+worst+" eledger="+eerr);
            assertEquals(interval,r.advancedSeconds(),0,label+" slice "+slice+" commits");
            assertTrue(worst<1e-10&&eerr<1e-9,label+" ledgers");
        }
    }
    @Test void aGasOnlyOpenTankWithNothingArrivingEndsEmpty() {
        drive("alone",new PassiveNetwork(List.of(openTank(1,nitrogenAt(P0),1)),List.of()),5,3);
    }
    @Test void aGasOnlyOpenTankBesideAVoidAtTheAmbient() {
        var nodes=List.of(openTank(1,nitrogenAt(P0),1),new PassiveNetwork.Reservoir(2,0,nitrogenAt(P0),PassiveNetwork.NodeKind.VOID));
        drive("void",new PassiveNetwork(nodes,List.of(pipe(90,0,1))),5,3);
    }
    @Test void aGasOnlyOpenTankFedFromAGenerator() {
        var nodes=List.of(PassiveNetwork.emptyVessel(1,0,1,1,true,model),new PassiveNetwork.Reservoir(2,0,nitrogenAt(1.5e5),PassiveNetwork.NodeKind.GENERATOR));
        drive("fed",new PassiveNetwork(nodes,List.of(pipe(90,1,0))),5,3);
    }
}
