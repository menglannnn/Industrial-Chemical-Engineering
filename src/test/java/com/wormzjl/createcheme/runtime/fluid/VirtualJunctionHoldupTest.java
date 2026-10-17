package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.fluid.support.FluidTestSupport;
import com.wormzjl.createcheme.science.fluid.network.*;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.createcheme.science.material.MaterialCatalog;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Owner decision D46: the junction holdup is virtual. A junction's buffer (m_J, w, h: the state of its backward-Euler
 * mixing row) is not inventory. At every committed slice of three transients, the mixed-gas four-port case (methane and
 * nitrogen tanks, 50 mm, the restart by injection) and the liquid junction fed by two water tanks or two water
 * generators at 300 K and 350 K (50 mm, 20 s), at the probe's 0.1 s and the product's 5 s cycle:
 * <ul>
 * <li>the tanks' change equals the boundary transfers plus the junction term (the buffer's change since the start), per
 * component and for energy, at roundoff;</li>
 * <li>the junction term's component masses stay within the sum of m_J and its energy within the sum of m_J times the
 * span of the specific enthalpies mixed (the row's convex combination, review "the virtual junction holdup");</li>
 * <li>the buffer keeps the mass m_J it was sized with (the booked flows' mass continuity).</li>
 * </ul>
 * Each case also prints {@code VIRTUAL_HOLDUP_DIGEST}: a SHA-256 of every committed temperature, pressure, flow, head and
 * node record (tanks' inventories and junction buffers) of every slice. The proof that D46 changed no solver number is
 * the same digest with the ownership code restored in the working tree (tools/phase-ports-probes/hold-classes/src,
 * {@code scratch-t17-ownership-restored.patch}); it is printed, not asserted, so it never needs re-recording.
 */
class VirtualJunctionHoldupTest {
    private final FluidThermodynamics model=FluidTestSupport.networkModel();
    private final double[] mw=model.molecularWeights();
    private static final double LEDGER=1e-12,MASS_DRIFT=1e-12;

    @Test void theJunctionTermStaysWithinTheSumOfMjAndTheTankLedgerClosesAtEverySlice() {
        for(double interval:new double[]{.1,5}) {
            var gas=MixedGasJunctionTransientTest.island(model,.05,4,true);
            int tank=-1;for(int i=0;i<gas.reservoirs().size();i++)if(gas.reservoirs().get(i).id()==1)tank=i;
            double[] feed=new double[mw.length];feed[0]=.5;
            var feedState=model.flashTP(350,150000,feed,()->{});
            double feedEnergy=feedState.enthalpy();
            var schedule=MixedGasJunctionTransientTest.schedule(interval);var durations=new double[schedule.size()];var transfers=new ArrayList<List<ScheduledTransfer>>();
            for(int s=0;s<durations.length;s++){durations[s]=schedule.get(s)[0];transfers.add(schedule.get(s)[1]>0?List.of(new ScheduledTransfer.Injection(900,tank,feed,feedEnergy)):List.of());}
            run("mixed-gas 0.05:4:true interval="+interval,gas,durations,transfers);
            int steps=(int)Math.round(20/interval);var even=new double[steps];Arrays.fill(even,interval);
            for(boolean tanks:new boolean[]{true,false})
                run("liquid "+(tanks?"tanks":"generators")+":0.05 interval="+interval,LiquidJunctionTransientTest.island(model,tanks,.05),even,Collections.nCopies(steps,List.of()));
        }
    }
    private void run(String label,PassiveNetwork graph,double[] durations,List<List<ScheduledTransfer>> transfers) {
        var ledger=new JunctionLedger(graph,mw);var digest=sha256();
        var solver=new PassiveIntervalSolver(model);PassiveIntervalSolver.Result committed=null;
        for(int step=0;step<durations.length;step++) {
            graph=new PassiveNetwork(graph.reservoirs(),graph.pipes(),transfers.get(step));
            solver.replayStart(graph,committed);
            var result=solver.solve(graph,durations[step],PassiveIntervalSolver.Settings.defaults(),()->{});committed=result;graph=result.graph();
            ledger.add(result);digest(digest,result);
            String at=label+" slice "+step+": ";
            assertTrue(ledger.worstMoles<LEDGER,at+"the tanks' component ledger with the junction term does not close: "+ledger.worstMoles);
            assertTrue(ledger.worstEnergy<LEDGER,at+"the tanks' energy ledger with the junction term does not close: "+ledger.worstEnergy);
            assertTrue(ledger.worstShare<=1,at+"a component of the junction term exceeds the sum of m_J: "+ledger.worstShare);
            assertTrue(ledger.worstEnergyShare<=1,at+"the junction term's energy exceeds the sum of m_J times the enthalpy span: "+ledger.worstEnergyShare);
            assertTrue(ledger.worstMassDrift<MASS_DRIFT,at+"a junction's buffer left its m_J: "+ledger.worstMassDrift);
        }
        assertTrue(ledger.worstShare>0||ledger.worstEnergyShare>0,label+": the buffer moved (the term is not trivially zero)");
        System.out.println("VIRTUAL_HOLDUP "+label+" sumMJ="+ledger.holdup+" kg componentLedger="+ledger.worstMoles+" energyLedger="+ledger.worstEnergy
                +" junctionShare="+ledger.worstShare+" junctionEnergyShare="+ledger.worstEnergyShare+" massDrift="+ledger.worstMassDrift);
        System.out.println("VIRTUAL_HOLDUP_DIGEST "+label+" slices="+durations.length+" sha256="+HexFormat.of().formatHex(digest.digest()));
    }
    private static MessageDigest sha256() {
        try{return MessageDigest.getInstance("SHA-256");}catch(java.security.NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
    }
    /** Every committed number of one interval, bit for bit: flows (average and end), heads, and per node its temperature,
     * pressure and record (amounts, energy field, solids mass): a tank's inventory, a junction's buffer. */
    private static void digest(MessageDigest digest,PassiveIntervalSolver.Result result) {
        var bytes=ByteBuffer.allocate(8);
        java.util.function.DoubleConsumer put=v->{bytes.clear();bytes.putLong(Double.doubleToRawLongBits(v));digest.update(bytes.array());};
        for(double q:result.averageMassFlows())put.accept(q);
        for(double q:result.endpointFlows())put.accept(q);
        for(double h:result.endpointHeads())put.accept(h);
        for(var node:result.graph().reservoirs()) {
            put.accept(node.state().temperature());put.accept(node.state().pressure());
            for(double n:node.inventory().moles())put.accept(n);
            put.accept(node.inventory().internalEnergy());put.accept(node.inventory().solids().massKg());
        }
    }
}
