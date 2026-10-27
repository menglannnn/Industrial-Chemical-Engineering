package com.wormzjl.createcheme.science.fluid.network;

import com.wormzjl.createcheme.science.fluid.solver.PhaseLayout;
import com.wormzjl.createcheme.science.fluid.solver.SparseNewton;
import com.wormzjl.createcheme.science.fluid.state.ParticleSize;
import com.wormzjl.createcheme.science.fluid.state.SolidInventory;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.createcheme.science.fluid.thermo.NetworkCrystals;
import com.wormzjl.createcheme.science.material.MaterialRuntime;
import com.wormzjl.createcheme.science.material.PilotCryogenicTestCatalog;
import com.wormzjl.createcheme.science.thermo.PhaseSupport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/**
 * M3 probe (not a gate; run on the javac lane): the cold fuzz's E1 holds (seeds 35, 72, 148). Each is a vessel's crystal
 * refresh whose closure (VesselCrystals.Joint) stalls: a liquid-full 1 m3 tank of LNG holding CO2-I. The vessel's
 * inventory and the state the refresh started from are the M3TRACE lines of logs 15 and 16; the probe states the vessel
 * at that temperature and pressure, runs the refresh, and prints the closure's pieces: the fluid-only closure, the drive,
 * the engine's crystal answer and a copy of the joint closure with its rows at the start and at its last iterate.
 */
class M3ColdRefreshProbeTest {
    static final String PILOT="createcheme:pilot_cryogenic";

    @Test void seed148(){run("148",135.31344863949792,646769.5572012014,1.0,-3.4029249587803435E8,new double[]{29.797759173414118,22312.312863511135,1670.92313085922,82.27928302487227,0.0},19.752811751259323);}
    @Test void seed35(){run("35",143.76662813072838,1062109.4374840155,1.0,-3.1544940535936993E8,new double[]{160.74057717872068,21038.55534624571,1592.0604241703604,148.0408861708337,0.0},37.27616586858761);}
    @Test void seed72(){run("72",121.36081450452541,783227.4174694573,1.0,-3.669055719656025E8,new double[]{179.42086098265187,24848.353001361684,274.7251617975553,23.466720918258286,0.0},39.338696332694674);}

    static void p(String format,Object... args){System.out.println("M3PROBE "+String.format(Locale.ROOT,format,args));}
    static String phases(FluidThermodynamics.State s) {
        return String.format(Locale.ROOT,"T=%.6f K P=%.1f Pa liq=%.6e vap=%.6e water=%.3e V=%.9f U=%.6e",s.temperature(),s.pressure(),s.liquidVolume(),s.vaporVolume(),s.waterVolume(),s.volume(),s.internalEnergy());
    }

    void run(String label,double t,double p,double capacity,double u,double[] n,double crystalKg) {
        var catalog=PilotCryogenicTestCatalog.catalog();
        MaterialRuntime.with(catalog,PILOT,()->{
            var model=FluidThermodynamics.forNetwork(catalog,PILOT);
            var crystal=model.crystalSolids().all().getFirst();
            var solids=new SolidInventory(List.of(new SolidInventory.Population(crystal.material(),new ParticleSize("0.00025"),crystalKg)));
            var state=model.flashTP(t,p,n,()->{}).withSolids(solids);
            p("[%s] seed state %s; crystal %.6f kg = %.6f mol, %.6e m3 at the record's density",label,phases(state),crystalKg,crystal.moles(crystalKg),crystalKg/crystal.material().density());
            var inventory=new PassiveNetwork.Inventory(capacity,n,u,solids);
            var node=new PassiveNetwork.Reservoir(2,0,state,PassiveNetwork.NodeKind.RESERVOIR,inventory);
            try {
                var refreshed=InventoryEquilibrium.refresh(new PassiveNetwork(List.of(node),List.of()),model,()->{});
                var after=refreshed.graph().reservoirs().getFirst();double kg=0;for(var q:after.inventory().solids().crystals())kg+=q.massKg();
                p("[%s] refresh passed: %s crystal %.6f kg",label,phases(after.state()),kg);
            } catch(RuntimeException failure) {
                p("[%s] refresh failed: %s",label,failure);
            }
            // The pieces.
            var closed=InventoryEquilibrium.solve(model,state,inventory,Double.NaN,()->{});
            double drive=VesselCrystals.drive(closed,crystal,closed.temperature(),closed.pressure());
            p("[%s] fluid-only closure (crystal inert): %s taut=%s; drive D=%.6e",label,phases(closed),PhaseLayout.taut(closed,capacity),drive);
            double[] totals=n.clone();totals[crystal.component()]+=crystal.moles(crystalKg);
            double trueEnergy=u+crystalKg*crystal.material().crystal().energyOffset();
            var answer=model.crystalsAtVolume(totals,trueEnergy,capacity,closed.temperature(),closed.pressure(),()->{});
            p("[%s] engine answer: T=%.6f K P=%.1f Pa crystal %s mol, volume %.9f m3, fluid %s",label,answer.temperature(),answer.pressure(),Arrays.toString(answer.crystalMoles()),
                    answer.volume(),answer.fluid()==null?"none":phases(answer.fluid()));
            double engineCrystalVolume=answer.volume()-(answer.fluid()==null?0:answer.fluid().volume());
            double recordCrystalVolume=answer.crystalMoles()[0]*crystal.molarMass()/crystal.material().density();
            p("[%s] crystal volume at the answer's amount: engine %.6e m3 (%.2f kg/m3), record %.6e m3 (%.2f kg/m3): the record's crystal takes %.3e m3 more",label,
                    engineCrystalVolume,answer.crystalMoles()[0]*crystal.molarMass()/engineCrystalVolume,recordCrystalVolume,crystal.material().density(),recordCrystalVolume-engineCrystalVolume);
            // The joint closure, copied with its rows printed.
            var joint=new Joint(model,answer.fluid(),List.of(crystal),List.of(answer.crystalMoles()[0]),totals,trueEnergy,capacity,PhaseLayout.bagVolume(closed,capacity),SolidInventory.EMPTY);
            double[] x=joint.start(answer.fluid(),List.of(answer.crystalMoles()[0]));
            p("[%s] joint layout: %s; start rows %s",label,joint.describe(),Arrays.toString(joint.residual(x)));
            try {
                var solved=SparseNewton.solve(joint,x,new SparseNewton.Settings(24,InventoryEquilibrium.CLOSURE,1e-6,24),()->{});
                p("[%s] joint closure converged: %s crystal %.6f mol",label,phases(joint.fluid(solved.variables())),joint.crystalMoles(solved.variables(),0));
            } catch(SparseNewton.Nonconvergence failure) {
                double[] last=failure.lastVariables();
                p("[%s] joint closure failed: %s",label,failure.getMessage());
                if(last!=null){var f=joint.fluid(last);p("[%s] last iterate: %s crystal %.6f mol wall=%s; rows %s",label,phases(f),joint.crystalMoles(last,0),PhaseLayout.wall(joint.trial(f,last),capacity),Arrays.toString(joint.residual(last)));}
            }
            return null;
        });
    }

    /** VesselCrystals.Joint, copied (M3 head 1eeb8673) with describe(), for the probe. */
    static final class Joint implements SparseNewton.Equations {
        final FluidThermodynamics model;final PhaseLayout layout;final List<NetworkCrystals.Crystal> crystals;
        final double[] totals,crystalScale;final boolean[] vapourRow;final double trueEnergy,capacity,workFrom;
        final SolidInventory inert;final SolidInventory.Moments inertMoments;final int fluidSize;
        volatile FluidThermodynamics.Prepared prepared;PhaseLayout.Wall sweeping,built;
        Joint(FluidThermodynamics model,FluidThermodynamics.State seed,List<NetworkCrystals.Crystal> crystals,List<Double> seeds,double[] totals,
              double trueEnergy,double capacity,double workFrom,SolidInventory inert) {
            this.model=model;this.crystals=List.copyOf(crystals);this.totals=totals;this.trueEnergy=trueEnergy;this.capacity=capacity;this.workFrom=workFrom;
            this.inert=inert;inertMoments=inert.moments();
            layout=new PhaseLayout(model,seed,null,null,PhaseLayout.support(seed,null,model.traceTruncation(),null),false);
            fluidSize=layout.size();crystalScale=new double[crystals.size()];vapourRow=new boolean[crystals.size()];
            for(int j=0;j<crystals.size();j++){int c=crystals.get(j).component();var support=layout.support(c);vapourRow[j]=support.vapor();crystalScale[j]=Math.max(1e-6,seeds.get(j)/layout.amountScale());}
        }
        String describe() {
            var b=new StringBuilder("size "+size()+", support");for(int c=0;c<totals.length-1;c++)b.append(' ').append(layout.support(c));
            return b.append(", vapour row ").append(Arrays.toString(vapourRow)).append(", amount scale ").append(layout.amountScale()).toString();
        }
        double[] start(FluidThermodynamics.State seed,List<Double> seeds){double[] x=new double[size()];System.arraycopy(layout.encode(seed),0,x,0,fluidSize);for(int j=0;j<seeds.size();j++)x[fluidSize+j]=seeds.get(j)/layout.amountScale();return x;}
        double crystalMoles(double[] x,int j){return x[fluidSize+j]*layout.amountScale();}
        FluidThermodynamics.State fluid(double[] x){return layout.decode(x,0,prepare(x));}
        @Override public int size(){return fluidSize+crystals.size();}
        @Override public double differenceScale(int column,double value){return column<fluidSize?layout.differenceStepScale(column,value):Math.max(Math.abs(value),crystalScale[column-fluidSize]);}
        @Override public double maximumStep(double[] x,double[] direction){double alpha=1;for(int c=0;c<fluidSize;c++)if(layout.totalAmountVariable(c)&&x[c]>0&&direction[c]<0)alpha=Math.min(alpha,.99*x[c]/-direction[c]);return alpha;}
        @Override public int[][] columnRows(){int[][] rows=new int[size()][size()];for(int[] column:rows)for(int i=0;i<column.length;i++)column[i]=i;return rows;}
        @Override public void linearising(double[] x){built=sweeping=PhaseLayout.wall(trial(fluid(x),x),capacity);}
        @Override public void linearised(){sweeping=null;}
        @Override public boolean linearisationHolds(double[] builtAt,double[] now){return built==null||built.equals(PhaseLayout.wall(trial(fluid(now),now),capacity));}
        @Override public double[] residual(double[] x) {
            var local=prepare(x);var fluid=layout.decode(x,0,local);var state=trial(fluid,x);
            double[] targets=totals.clone();double energy=trueEnergy;
            for(int j=0;j<crystals.size();j++){var crystal=crystals.get(j);double moles=crystalMoles(x,j);targets[crystal.component()]-=moles;energy-=moles*crystal.molarMass()*crystal.material().crystal().energyOffset();}
            double[] f=new double[size()];layout.residual(state,targets,energy,capacity,workFrom,f,0,x,local,sweeping);
            double t=fluid.temperature(),p=fluid.pressure();
            for(int j=0;j<crystals.size();j++)f[fluidSize+j]=VesselCrystals.drive(fluid,crystals.get(j),t,p);
            return f;
        }
        FluidThermodynamics.State trial(FluidThermodynamics.State fluid,double[] x) {
            var base=inert.empty()?fluid:fluid.withSolidState(inert,inertMoments);double t=fluid.temperature(),p=fluid.pressure(),volume=0,energy=0,mass=0;
            for(int j=0;j<crystals.size();j++){var material=crystals.get(j).material();double m=crystalMoles(x,j)*crystals.get(j).molarMass();volume+=m/material.density();energy+=m*material.specificInternalEnergy(t);mass+=m;}
            return new FluidThermodynamics.State(t,p,base.liquidView(),base.vaporView(),base.waterLiquid(),base.waterVapor(),base.hydrocarbonPartialPressure(),
                    base.waterPartialPressure(),base.volume()+volume,base.enthalpy()+energy+p*volume,base.internalEnergy()+energy,base.mass()+mass,
                    base.liquidVolume(),base.waterVolume(),base.vaporVolume(),base.liquidProperties(),base.vaporProperties(),base.solids(),base.solidMoments(),base.waterCompressibility());
        }
        FluidThermodynamics.Prepared prepare(double[] x){double temperature=layout.temperature(x,0);var local=prepared;if(local==null||local.temperature()!=temperature)prepared=local=model.prepare(temperature);return local;}
    }
}
