package com.wormzjl.createcheme.science.fluid.network;

import com.wormzjl.createcheme.science.fluid.network.PassiveNetwork.PhasePort;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Owner decision D34 of documentation/2026-09-24-coolprop-low-temperature (M5 seed 322, minimized to 13 blocks): a port's
 * static column holds what the line holds.
 *
 * <p>The island: an upper 1 m3 tank of methane at 369 K and 430 kPa, saturated with a heavy crude cut (crude_pc08) and
 * holding 1.8e-8 m3 (16 mg) of it as liquid, its bottom port drained by 1 m of 0.05 m line (2 L) into the top of a lower
 * 1 m3 methane tank 2 m below, which stands at the gas column's balance (44 Pa above the upper tank).
 *
 * <p>Physics: the trace runs down at once and the line holds gas. Its column is the gas's plus the trace's share of the
 * line's volume, a drive of 0.16 Pa; once the trace has left, nothing drives the line. Steady state: the upper tank dry,
 * the trace in the lower one, no flow in the line, both tanks at rest. The line stood at rest before the trace condensed:
 * the job starts from a committed interval with no flow, as seed 322's held interval started from a carried flow of about
 * zero. Read from the port's leading phase, as before the rule, the line was a 17 kPa column of liquid that 16 mg cannot
 * fill; the trace, drawn alone at a vanishing start flow, had no root at any step size, and the island held.
 */
class StaticColumnTraceTest extends PortFixture {
    private final int methane=index("Methane"),heavy=index("crude_pc08");
    private static final double T=369,P=430000,DROP_HEIGHT=2,SLICE=5;
    /** The drop line: 1 m of 0.05 m pipe, 1.96e-3 m3. */
    private static final PipeResistance.Geometry DROP=new PipeResistance.Geometry(1,.05,PipeResistance.DEFAULT_ROUGHNESS_METRES,0);
    /** The trace's crude_pc08 over the vapour's saturation amount, mol: 1.8e-8 m3 of liquid at 885 kg/m3. */
    private static final double TRACE_MOLES=4.5e-5;

    /** {@code n} scaled to fill 1 m3 at {@code t} and {@code p}, flashed there. */
    private FluidThermodynamics.State vessel(double t,double p,double[] n) {
        var unit=model.flashTP(t,p,n,NOOP);double[] m=n.clone();for(int c=0;c<m.length;c++)m[c]/=unit.volume();
        return model.flashTP(t,p,m,NOOP);
    }
    /** The upper tank: methane saturated with crude_pc08 (the vapour's amount at an excess), plus the trace. */
    private FluidThermodynamics.State traceTank() {
        double[] n=new double[mw.length];n[methane]=P/(FluidThermodynamics.R*T);n[heavy]=1e-2;
        n[heavy]=model.flashTP(T,P,n,NOOP).vapor()[heavy]+TRACE_MOLES;
        return vessel(T,P,n);
    }
    private PassiveNetwork island() {
        var upper=traceTank();
        // The lower tank's pressure: the upper one's plus its own gas column over the drop.
        double[] n=new double[mw.length];double lower=P;FluidThermodynamics.State gas=null;
        for(int i=0;i<3;i++){n[methane]=lower/(FluidThermodynamics.R*T);gas=vessel(T,lower,n);lower=P+model.vaporDensity(gas)*PassiveStepSolver.GRAVITY*DROP_HEIGHT;}
        return new PassiveNetwork(List.of(new PassiveNetwork.Reservoir(1,0,upper),new PassiveNetwork.Reservoir(2,-DROP_HEIGHT,gas)),
                List.of(new PassiveNetwork.Pipe(10,0,1,List.of(DROP),new FlowControl.Passive(),0,null,PhasePort.LIQUID,PhasePort.VAPOR)));
    }

    @Test void aLiquidTraceAtABottomPortDrainsAndItsDropLineComesToRest() {
        var start=island();var graph=start;
        var upper=start.reservoirs().getFirst().state();
        double trace=upper.liquidVolume(),traceHeavy=upper.liquid()[heavy];
        assertTrue(trace>1e-8&&trace<3e-8,"a trace far below the line's 2 L: "+trace);
        double heavyHeld=start.reservoirs().get(0).inventory().moles()[heavy]+start.reservoirs().get(1).inventory().moles()[heavy];
        var solver=new PassiveIntervalSolver(model);
        solver.replayStart(start,NetworkReduction.restInterval(start,SLICE));
        int slices=0;double drained=Double.NaN;
        while(!NetworkReduction.atRest(graph,model)) {
            assertTrue(slices<12,"the island comes to rest within a minute");
            final var at=graph;
            var result=assertDoesNotThrow(()->solver.solve(at,SLICE,PassiveIntervalSolver.Settings.defaults(),NOOP),"slice "+slices+" holds");
            graph=result.graph();slices++;
            if(slices==1)drained=graph.reservoirs().get(1).inventory().moles()[heavy]-start.reservoirs().get(1).inventory().moles()[heavy];
        }
        // At rest: one more slice is the identity, nothing flows in the line.
        var rest=solver.solve(graph,SLICE,PassiveIntervalSolver.Settings.defaults(),NOOP);
        assertEquals(0,rest.endpointFlows()[0],"no flow in the drop line at rest");
        assertEquals(0,rest.rejectedSubsteps());
        var end=graph.reservoirs();
        double upperHeavy=end.get(0).inventory().moles()[heavy],lowerHeavy=end.get(1).inventory().moles()[heavy];
        System.out.println("STATIC_COLUMN trace="+trace+" m3 heavy="+traceHeavy+" mol slices="+slices+" drained in slice 1="+drained
                +" mol upper liquid="+end.get(0).state().liquidVolume()+" m3 lower heavy gain="+(lowerHeavy-start.reservoirs().get(1).inventory().moles()[heavy])
                +" mol P upper="+end.get(0).state().pressure()+" lower="+end.get(1).state().pressure());
        assertEquals(0,end.get(0).state().liquidVolume(),"the upper tank is dry: the trace ran down");
        assertTrue(lowerHeavy-start.reservoirs().get(1).inventory().moles()[heavy]>=.99*traceHeavy,"the trace reached the lower tank");
        assertEquals(heavyHeld,upperHeavy+lowerHeavy,1e-12*heavyHeld,"crude_pc08 ledger");
        // The static drive of the line at the start: the gas column's and the trace's share of the line, a fraction of a
        // pascal, not the trace's own 17 kPa column.
        double drive=NetworkReduction.drives(start,model)[0][0];
        System.out.println("STATIC_COLUMN start drive="+drive+" Pa");
        assertTrue(drive>0&&drive<1,"the line's start drive is the gas column's and the trace's share: "+drive);
    }
}
