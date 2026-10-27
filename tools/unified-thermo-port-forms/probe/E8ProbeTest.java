package com.wormzjl.createcheme.science.fluid.network;

import com.wormzjl.createcheme.fluid.support.FluidTestSupport;
import com.wormzjl.createcheme.science.fluid.solver.PhaseLayout;
import com.wormzjl.createcheme.science.fluid.state.ParticleSize;
import com.wormzjl.createcheme.science.fluid.state.SolidInventory;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.createcheme.science.material.MaterialCatalog;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/** M4c scratch probe (never committed): does a one-tank step of seed 48's E8 tank reproduce the reconstruction's refusal? It scans
 * the feed's rise (0 to 10 kPa) and the step (1e-6 to 0.05 s); on 1b15fd43 the 100 Pa rise at 1e-6 s is refused with E8's
 * message (the gate's rig) and the 10 Pa rise at 0.05 s stalls its line search (not E8; logs m4c/00).
 * Run: copy into an export's src/test/java/com/wormzjl/createcheme/science/fluid/network/, add it to EXTRA_TESTS of
 * index-compile.sh, then index-run.sh <name> com.wormzjl.createcheme.science.fluid.network.E8ProbeTest. */
class E8ProbeTest {
    final FluidThermodynamics pilot=FluidTestSupport.model(MaterialCatalog.bundled(),"createcheme:pilot_cryogenic");

    @Test void probe() {
        var crystal=pilot.crystalSolids().all().getFirst();
        var dryIce=new SolidInventory(List.of(new SolidInventory.Population(crystal.material(),new ParticleSize("0.00025"),35.09036214684682)));
        double t=153.07858326704772,p=1141313.268432868;
        double[] l={127.94664170820322,20017.721696299337,1419.2160471398552,308.7325517796933};
        double[] v={8.685458618914788E-7,2.094236045700818E-5,4.4530891574445825E-8,3.930257238466825E-8};
        var state=pilot.state(t,p,l,v,0,0,p).withSolids(dryIce);
        double nv=Arrays.stream(v).sum(),n=Arrays.stream(l).sum()+nv;
        double capacity=state.liquidVolume()+state.vaporVolume()+dryIce.volume();
        System.out.printf(Locale.ROOT,"E8 tank: %.6f m3 liquid, %.4e m3 vapour (share of moles %.6e, of volume %.4e), %.6f m3 dry ice, capacity %.9f%n",
                state.liquidVolume(),state.vaporVolume(),nv/n,state.vaporVolume()/capacity,dryIce.volume(),capacity);
        var inventory=new PassiveNetwork.Inventory(capacity,PhaseLayout.totalAmounts(state),state.internalEnergy(),dryIce);
        var tank=new PassiveNetwork.Reservoir(1,0,state,PassiveNetwork.NodeKind.RESERVOIR,inventory);
        double[] liquidOnly=l.clone();
        for(double rise:new double[]{0,1,10,100,1000,10000}) {
            for(double dt:new double[]{1e-6,1e-4,1e-3,.05}) {
                var feedState=pilot.state(t,p+rise,liquidOnly,new double[4],0,0,p+rise);
                var feed=new PassiveNetwork.Reservoir(2,0,feedState,PassiveNetwork.NodeKind.GENERATOR);
                var graph=new PassiveNetwork(List.of(tank,feed),List.of(new PassiveNetwork.Pipe(10,0,1,new PipeResistance.Geometry(10,.02,.000045,0))));
                String outcome;
                try {
                    var step=new PassiveStepSolver(pilot).solve(graph,dt,()->{});
                    var end=step.states().getFirst();
                    outcome=String.format(Locale.ROOT,"accepted: %.6f K, %.3f Pa, vapour %s, flow %.4e kg/s",end.temperature(),end.pressure(),
                            end.vaporProperties()==null?"none":String.format(Locale.ROOT,"%.4e mol",Arrays.stream(end.vaporView()).sum()),step.massFlows()[0]);
                } catch(RuntimeException refused){outcome="REFUSED "+refused.getClass().getSimpleName()+": "+refused.getMessage();}
                System.out.printf(Locale.ROOT,"E8 rise %.0f Pa dt %.0e: %s%n",rise,dt,outcome);
            }
        }
        // The tank alone at rest: the reconstruction's closure of the tank's own inventory from its own state.
        var closed=InventoryEquilibrium.close(pilot,state,inventory,null,PhaseLayout.bagVolume(state,capacity),()->{});
        System.out.printf(Locale.ROOT,"E8 close at rest: same=%s%n",closed==state);
    }
}
