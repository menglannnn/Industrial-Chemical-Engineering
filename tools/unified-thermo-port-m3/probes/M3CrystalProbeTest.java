package com.wormzjl.createcheme.science.fluid.network;

import com.wormzjl.createcheme.fluid.support.FluidTestSupport;
import com.wormzjl.createcheme.science.fluid.solver.PhaseLayout;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.createcheme.science.material.MaterialCatalog;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/** M3 probe (not tracked): the crystal refresh on simple closed vessels of the pilot package, printed. */
class M3CrystalProbeTest {
    static final String PILOT="createcheme:pilot_cryogenic";
    final FluidThermodynamics model=FluidTestSupport.model(MaterialCatalog.bundled(),PILOT);

    FluidThermodynamics.State charge(double t,double p,double[] z,double volume) {
        var unit=model.flashTP(t,p,z,()->{});
        double[] n=z.clone();for(int i=0;i<n.length;i++)n[i]*=volume/unit.volume();
        return model.flashTP(t,p,n,()->{});
    }
    void print(String label,PassiveNetwork.Reservoir node) {
        var s=node.state();var inv=node.inventory();
        System.out.printf(Locale.ROOT,"M3PROBE %s: T=%.6f K P=%.3f Pa V=%.6e (cap %.6e) liq=%s vap=%s solids=%s U=%.6f n=%s%n",label,s.temperature(),s.pressure(),s.volume(),inv.volume(),
                s.liquidProperties()!=null,s.vaporProperties()!=null,inv.solids().populations(),inv.internalEnergy(),Arrays.toString(inv.moles()));
    }
    @Test void crystals() {
        var all=model.crystalSolids().all();
        for(var c:all)System.out.printf(Locale.ROOT,"M3PROBE crystal %s comp=%d M=%.7f Tref=%.6f rho=%.4f c=%.4f u0=%.4f%n",c.material().id(),c.component(),c.molarMass(),
                c.referenceTemperature(),c.material().density(),c.material().heatCapacity(),c.material().crystal().energyOffset());
    }
    @Test void closedNitrogenCo2BelowFrost() {
        var charged=charge(170,1e6,new double[]{.9,0,0,.1,0},.1);
        var graph=new PassiveNetwork(List.of(new PassiveNetwork.Reservoir(1,0,charged)),List.of());
        print("a charged",graph.reservoirs().getFirst());
        var refreshed=InventoryEquilibrium.refresh(graph,model,()->{});
        print("a refreshed",refreshed.graph().reservoirs().getFirst());
        System.out.println("M3PROBE a converted="+refreshed.converted()+" boundaries="+refreshed.boundaries());
        var again=InventoryEquilibrium.refresh(refreshed.graph(),model,()->{});
        print("a again",again.graph().reservoirs().getFirst());
        System.out.println("M3PROBE a again converted="+again.converted()+" same inventory="+(again.graph().reservoirs().getFirst().inventory()==refreshed.graph().reservoirs().getFirst().inventory()));
        var result=new PassiveIntervalSolver(model).solve(graph,5,PassiveIntervalSolver.Settings.defaults(),()->{},1);
        print("a solved",result.graph().reservoirs().getFirst());
        System.out.println("M3PROBE a solve substeps="+result.acceptedSubsteps()+" boundaries="+result.boundaries());
    }
    @Test void lngWithCo2() {
        var charged=charge(120,3e5,new double[]{0,.98,0,.02,0},.5);
        var graph=new PassiveNetwork(List.of(new PassiveNetwork.Reservoir(1,0,charged,PassiveNetwork.NodeKind.RESERVOIR,
                new PassiveNetwork.Inventory(1,PhaseLayout.totalAmounts(charged),charged.internalEnergy(),charged.solids()))),List.of());
        print("lng charged",graph.reservoirs().getFirst());
        var refreshed=InventoryEquilibrium.refresh(graph,model,()->{});
        print("lng refreshed",refreshed.graph().reservoirs().getFirst());
        System.out.println("M3PROBE lng converted="+refreshed.converted()+" boundaries="+refreshed.boundaries());
        var again=InventoryEquilibrium.refresh(refreshed.graph(),model,()->{});
        System.out.println("M3PROBE lng again converted="+again.converted());
    }
    @Test void lngPureHalfFull() {
        for(double co2:new double[]{0,.002,.02}) {
            var charged=charge(120,3e5,new double[]{0,1-co2,0,co2,0},.5);
            var graph=new PassiveNetwork(List.of(new PassiveNetwork.Reservoir(1,0,charged,PassiveNetwork.NodeKind.RESERVOIR,
                    new PassiveNetwork.Inventory(1,PhaseLayout.totalAmounts(charged),charged.internalEnergy(),charged.solids()))),List.of());
            try{var refreshed=InventoryEquilibrium.refresh(graph,model,()->{});print("half co2="+co2,refreshed.graph().reservoirs().getFirst());}
            catch(RuntimeException e){System.out.println("M3PROBE half co2="+co2+" failed: "+e);}
        }
    }
    @Test void slackDryIce() {
        // A slack bag of CO2 gas at 1 atm cooled below its sublimation point: dry ice at 194.6 K, slack at the ambient.
        var charged=charge(190,PhaseLayout.AMBIENT_PRESSURE,new double[]{0,0,0,1,0},.01);
        var graph=new PassiveNetwork(List.of(new PassiveNetwork.Reservoir(1,0,charged,PassiveNetwork.NodeKind.RESERVOIR,
                new PassiveNetwork.Inventory(.02,PhaseLayout.totalAmounts(charged),charged.internalEnergy(),charged.solids()))),List.of());
        print("dry charged",graph.reservoirs().getFirst());
        var refreshed=InventoryEquilibrium.refresh(graph,model,()->{});
        print("dry refreshed",refreshed.graph().reservoirs().getFirst());
        System.out.println("M3PROBE dry converted="+refreshed.converted()+" boundaries="+refreshed.boundaries());
    }
    @Test void phiScan() {
        var crystal=model.crystalSolids().all().getFirst();
        for(int solvent:new int[]{1,2})for(double t:new double[]{150,170,190,200,210})for(double x:new double[]{.1,.15,.2,.3}) {
            double[] z=new double[5];z[solvent]=1-x;z[3]=x;
            try{var s=charge(t,5e6,z,1);double liq=s.liquidVolume()+s.waterVolume();
                double phi=liq>0?s.liquidView()[3]*crystal.molarMass()/crystal.material().density()/liq:0;
                System.out.printf(Locale.ROOT,"M3PROBE scan solvent=%d T=%.0f x=%.2f liq=%s vap=%s phi=%.4f%n",solvent,t,x,s.liquidProperties()!=null,s.vaporProperties()!=null,phi);}
            catch(RuntimeException e){System.out.printf(Locale.ROOT,"M3PROBE scan solvent=%d T=%.0f x=%.2f %s%n",solvent,t,x,e.getClass().getSimpleName());}
        }
    }
}
