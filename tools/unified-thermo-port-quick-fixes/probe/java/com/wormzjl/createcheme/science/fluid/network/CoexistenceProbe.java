package com.wormzjl.createcheme.science.fluid.network;

import com.wormzjl.createcheme.fluid.support.FluidTestSupport;
import com.wormzjl.createcheme.science.fluid.solver.PhaseLayout;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.createcheme.science.material.MaterialTestBasis;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/**
 * Probe (not tracked; tools/unified-thermo-port-quick-fixes/probe): a vessel of nitrogen over water a little below the
 * melting line, whose refresh freezes until ice and water coexist at T_m. Taut (capacity = the charge's volume) and slack
 * (capacity 1.2 m3) at 1 atm; the refresh the rest test reads and one 5 s interval of the island alone.
 */
class CoexistenceProbe {
    private static FluidThermodynamics.State charge(FluidThermodynamics model,double t,double p,double[] z,double volume) {
        var unit=model.flashTP(t,p,z,()->{});
        double[] n=z.clone();for(int i=0;i<n.length;i++)n[i]*=volume/unit.volume();
        return model.flashTP(t,p,n,()->{});
    }
    private static double ice(PassiveNetwork.Reservoir node){double m=0;for(var p:node.inventory().solids().crystals())m+=p.massKg();return m;}
    @Test void probe() {
        var water=FluidTestSupport.networkModel();
        for(double share:new double[]{.5,.05})for(double t:new double[]{263.15,272})for(double capacity:new double[]{1,1.2}) {
            double[] z=new double[water.componentCount()];z[MaterialTestBasis.NITROGEN]=1-share;z[z.length-1]=share;
            var state=charge(water,t,PhaseLayout.AMBIENT_PRESSURE,z,1);
            var node=new PassiveNetwork.Reservoir(1,0,state,PassiveNetwork.NodeKind.RESERVOIR,
                    new PassiveNetwork.Inventory(capacity,PhaseLayout.totalAmounts(state),state.internalEnergy(),state.solids()));
            var graph=new PassiveNetwork(List.of(node),List.of());
            String label=String.format(Locale.ROOT,"COEX water share %.2f, %.2f K, capacity %.1f m3",share,t,capacity);
            try {
                var tank=InventoryEquilibrium.conversions(graph,water,()->{}).graph().reservoirs().getFirst();
                System.out.printf(Locale.ROOT,"%s: refresh %.6f K %.1f Pa, ice %.6f kg, stand-in %.4e mol%n",label,tank.state().temperature(),tank.state().pressure(),ice(tank),tank.state().waterLiquid());
            } catch(RuntimeException e){System.out.println(label+": refresh "+e);}
            try {
                var result=new PassiveIntervalSolver(water).solve(graph,5,PassiveIntervalSolver.Settings.defaults(),()->{});
                var tank=result.graph().reservoirs().getFirst();
                System.out.printf(Locale.ROOT,"%s: interval %.6f K, ice %.6f kg%n",label,tank.state().temperature(),ice(tank));
            } catch(RuntimeException e){System.out.println(label+": interval "+e);}
        }
    }
}
