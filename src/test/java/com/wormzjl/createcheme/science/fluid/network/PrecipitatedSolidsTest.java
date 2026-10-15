package com.wormzjl.createcheme.science.fluid.network;

import com.wormzjl.createcheme.fluid.support.FluidTestSupport;
import com.wormzjl.createcheme.science.fluid.solver.PhaseLayout;
import com.wormzjl.createcheme.science.fluid.state.ParticleSize;
import com.wormzjl.createcheme.science.fluid.state.SolidInventory;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.createcheme.science.fluid.thermo.NetworkCrystals;
import com.wormzjl.createcheme.science.fluid.transport.SlurryTransport;
import com.wormzjl.createcheme.science.fluid.transport.SolidMobility;
import com.wormzjl.createcheme.science.material.MaterialCatalog;
import com.wormzjl.createcheme.science.material.SolidMaterialCatalog.SolidMaterial;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Crystals as solids (milestone M3 of the unified thermo port, owner decisions D22 (5) and D23; plan
 * {@code documentation/2026-09-24-coolprop-low-temperature/PRECIPITATED_SOLIDS_PLAN.md}): the unit gates of the plan's
 * section 4 and the three gates M3 adds. A crystal is a population of main's {@code SolidInventory}; only a vessel's refresh
 * moves its mass; every stock holds one size per solid after its refresh; the outlet check reads that size; a slurry stops
 * at half solids; transport nodes never precipitate; and an island of unchanged solids rests.
 */
class PrecipitatedSolidsTest {
    static final String PILOT="createcheme:pilot_cryogenic";
    static final int NITROGEN=0,METHANE=1,CO2=3;
    private final FluidThermodynamics pilot=FluidTestSupport.model(MaterialCatalog.bundled(),PILOT);
    private final FluidThermodynamics water=FluidTestSupport.networkModel();
    private NetworkCrystals.Crystal co2(){return pilot.crystalSolids().all().getFirst();}

    /** {@code volume} m3 of {@code z} at (T, P) on {@code model}, its network TP answer (which moves no mass into a crystal). */
    private static FluidThermodynamics.State charge(FluidThermodynamics model,double t,double p,double[] z,double volume) {
        var unit=model.flashTP(t,p,z,()->{});
        double[] n=z.clone();for(int i=0;i<n.length;i++)n[i]*=volume/unit.volume();
        return model.flashTP(t,p,n,()->{});
    }
    private FluidThermodynamics.State hotWater(double p){return charge(water,350,p,unit(water,water.componentCount()-1),1);}
    private static double[] unit(FluidThermodynamics model,int component){double[] n=new double[model.componentCount()];n[component]=1;return n;}
    private SolidMaterial demo(){return water.solids.require("createcheme:demo_particle");}
    private static SolidInventory.Population grade(SolidMaterial material,String metres,double kg){return new SolidInventory.Population(material,new ParticleSize(metres),kg);}
    private static PassiveNetwork.Reservoir vessel(long id,FluidThermodynamics.State state,double capacity) {
        return new PassiveNetwork.Reservoir(id,0,state,PassiveNetwork.NodeKind.RESERVOIR,new PassiveNetwork.Inventory(capacity,PhaseLayout.totalAmounts(state),state.internalEnergy(),state.solids()));
    }
    private static double crystalKg(PassiveNetwork.Reservoir node){double m=0;for(var p:node.inventory().solids().crystals())m+=p.massKg();return m;}

    // ---- Plan section 4, gate 1: phi to size ----

    /**
     * The size crystal mass forms at (D23 (1), (2)): {@code d = 0.25 mm max(1, 4.04 (phi / (1 - phi))^0.6)}, phi the CO2's
     * volume share of the vessel's liquids as crystal, capped at 0.5, and 0 from a gas. A gas and a dilute LNG give
     * 0.25 mm; a CO2-rich liquid freezes at the slush size, 1.01 mm; 30 % CO2 in liquid ethane, phi 0.17, 0.38 mm.
     */
    @Test void theSizeOfFormingCrystalGrowsWithItsComponentsShareOfTheLiquids() {
        var crystal=co2();
        var gas=charge(pilot,250,1e5,new double[]{.9,0,0,.1,0},1);
        assertNull(gas.liquidProperties(),"a gas");
        assertEquals(new ParticleSize("0.00025"),VesselCrystals.precipitateDiameter(gas,crystal),"frost from a gas: d_dilute");
        var dilute=charge(pilot,120,3e5,new double[]{0,.995,0,.005,0},1);
        assertEquals(new ParticleSize("0.00025"),VesselCrystals.precipitateDiameter(dilute,crystal),"0.5 % CO2 in LNG: phi "+phi(dilute,crystal));
        var between=charge(pilot,200,5e6,new double[]{0,0,.7,.3,0},1);
        double phi=phi(between,crystal);
        System.out.printf(Locale.ROOT,"phi to size: dilute LNG phi %.5f; 30 %% CO2 in liquid ethane at 200 K, 5 MPa phi %.5f, d %s m%n",phi(dilute,crystal),phi,
                VesselCrystals.precipitateDiameter(between,crystal).metres());
        assertTrue(phi>.09&&phi<.5,"between the dilute and the slush ends: "+phi);
        assertEquals(size(2.5e-4*4.04*Math.pow(phi/(1-phi),.6)),VesselCrystals.precipitateDiameter(between,crystal));
        var rich=charge(pilot,230,2e6,new double[]{0,0,0,1,0},1);
        assertNotNull(rich.liquidProperties(),"liquid CO2 above its triple point");
        assertTrue(phi(rich,crystal)>.5,"pure liquid CO2: its share as crystal exceeds the cap");
        assertEquals(new ParticleSize("0.00101"),VesselCrystals.precipitateDiameter(rich,crystal),"the slush: 0.25 mm times 4.04");
    }
    /** The CO2's volume share of the state's liquids as crystal, uncapped. */
    private static double phi(FluidThermodynamics.State state,NetworkCrystals.Crystal crystal) {
        double liquids=state.liquidVolume()+state.waterVolume();
        return liquids>0?state.liquidView()[crystal.component()]*crystal.molarMass()/crystal.material().density()/liquids:0;
    }
    private static ParticleSize size(double metres){return new ParticleSize(new BigDecimal(metres).round(new MathContext(SolidInventory.SIZE_DIGITS,RoundingMode.HALF_EVEN)).toString());}

    // ---- Gate 2: one size per solid in every stock ----

    /**
     * After a refresh every stock holds one population per solid (D23, amended 2026-10-15): a tank, a junction buffer
     * and a filter cake, each holding 90 % of a solid at 0.25 mm and 10 % at 1 mm, hold it at the mass-weighted mean,
     * 0.325 mm, with its mass kept exactly and its volume and heat capacity to rounding.
     */
    @Test void aRefreshLeavesOneSizePerSolidInEveryStock() {
        var demo=demo();
        var two=new SolidInventory(List.of(grade(demo,"0.00025",9),grade(demo,"0.001",1)));
        var tankState=hotWater(160000).withSolids(two.scale(10));
        var junctionState=hotWater(155000).withSolids(two);
        var junction=new PassiveNetwork.Reservoir(2,0,junctionState,PassiveNetwork.NodeKind.JUNCTION,PassiveNetwork.mintedJunction(junctionState,5));
        var run=new PipeResistance.Geometry(10,.05,.000045,0);
        var filtered=new PassiveNetwork.Pipe(11,1,2,run).withFilter(new InlineFilter(.01,1e6,two.scale(.1),-12.5));
        var graph=new PassiveNetwork(List.of(new PassiveNetwork.Reservoir(1,0,tankState),junction,new PassiveNetwork.Reservoir(3,0,hotWater(150000))),
                List.of(new PassiveNetwork.Pipe(10,0,1,run),filtered));
        var refreshed=InventoryEquilibrium.refresh(graph,water,()->{});
        assertTrue(refreshed.converted(),"a merge is a change of the stock");
        var after=refreshed.graph();
        var stocks=List.of(graph.reservoirs().get(0).inventory().solids(),graph.reservoirs().get(1).inventory().solids(),graph.pipes().get(1).filter().captured());
        var merged=List.of(after.reservoirs().get(0).inventory().solids(),after.reservoirs().get(1).inventory().solids(),after.pipes().get(1).filter().captured());
        for(int i=0;i<3;i++) {
            var before=stocks.get(i).moments();var stock=merged.get(i);
            assertEquals(1,stock.populations().size(),"stock "+i+": one population");
            assertEquals(new ParticleSize("0.000325"),stock.populations().getFirst().size(),"stock "+i+": the mass-weighted mean 0.9 x 0.25 + 0.1 x 1 mm");
            assertEquals(before.mass(),stock.moments().mass(),0,"stock "+i+": mass");
            assertEquals(before.volume(),stock.moments().volume(),1e-15*before.volume(),"stock "+i+": volume");
            assertEquals(before.heatCapacity(),stock.moments().heatCapacity(),1e-15*before.heatCapacity(),"stock "+i+": heat capacity");
        }
        // The junction's state carries its buffer's populations at its own amount scale.
        var state=after.reservoirs().get(1).state().solids();
        assertEquals(1,state.populations().size());assertEquals(new ParticleSize("0.000325"),state.populations().getFirst().size());
        assertEquals(junctionState.solids().massKg(),state.massKg(),1e-15*state.massKg());
    }

    // ---- Gate 3: the outlet reads the per-solid mean ----

    /**
     * Main's outlet check reads each solid at its stock's size, the mass-weighted mean (D23, amended): CO2 crystals in
     * liquid methane, 99 % at 0.25 mm and 1 % at 1 mm, average 0.2575 mm and stay suspended in a 1.5 m/s line (0.25 mm
     * needs about 1.0 m/s, 1 mm about 2.8 m/s). The largest population, main's rule before, closed it.
     */
    @Test void theOutletReadsEachSolidAtItsMeanSize() {
        var crystal=co2().material();
        var lng=charge(pilot,115,3e5,new double[]{0,1,0,0,0},1);
        assertNotNull(lng.liquidProperties());assertNull(lng.vaporProperties(),"subcooled liquid methane");
        double kg=.01*lng.liquidVolume()*crystal.density();
        var pipe=new PassiveNetwork.Pipe(3,0,1,new PipeResistance.Geometry(100,.05,.000045,0));
        var mixed=lng.withSolids(new SolidInventory(List.of(grade(crystal,"0.00025",.99*kg),grade(crystal,"0.001",.01*kg))));
        var fine=lng.withSolids(new SolidInventory(List.of(grade(crystal,"0.00025",kg))));
        var coarse=lng.withSolids(new SolidInventory(List.of(grade(crystal,"0.001",kg))));
        double q=1.5*pipe.maximumArea()*mixed.mass()/(mixed.liquidVolume()+mixed.waterVolume());
        var check=SolidMobility.check(SolidMobility.donor(pilot,mixed),mixed,pipe,q,SolidMobility.Outlet.MIXED,mixed.mass());
        double fineSpeed=SolidMobility.check(SolidMobility.donor(pilot,fine),fine,pipe,0,SolidMobility.Outlet.MIXED,fine.mass()).minimumVelocity();
        double coarseSpeed=SolidMobility.check(SolidMobility.donor(pilot,coarse),coarse,pipe,0,SolidMobility.Outlet.MIXED,coarse.mass()).minimumVelocity();
        System.out.printf(Locale.ROOT,"outlet: line %.3f m/s; suspension 0.25 mm %.3f m/s, 1 mm %.3f m/s, the 99/1 stock %.3f m/s (%s)%n",check.velocity(),fineSpeed,coarseSpeed,
                check.minimumVelocity(),mixed.solids().size(crystal.id()).metres());
        assertEquals(1.5,check.velocity(),1e-12);
        assertTrue(fineSpeed<1.5&&coarseSpeed>1.5,"0.25 mm stays suspended at 1.5 m/s and 1 mm does not");
        assertEquals(SolidMobility.Reason.MOBILE,check.reason(),"the stock's mean, 0.2575 mm, keeps the line open");
        assertTrue(check.minimumVelocity()<1.1,"the mean's suspension speed: "+check.minimumVelocity());
    }

    // ---- Gate 4: slush at 0.5 ----

    /** A slurry whose solids fill half of its condensed volume is a slush and closes an outlet (D23 (3)); just below half it
     * flows. The slurry viscosity law keeps its 0.62 packing limit. */
    @Test void aSlurryStopsAtHalfSolids() {
        var demo=demo();var liquid=hotWater(160000);
        var pipe=new PassiveNetwork.Pipe(3,0,1,new PipeResistance.Geometry(100,.05,.000045,0));
        for(double share:new double[]{.5*(1+1e-9),.5*(1-1e-9)}) {
            double kg=share/(1-share)*liquid.waterVolume()*demo.density();
            var slurry=liquid.withSolids(new SolidInventory(List.of(grade(demo,"0.00001",kg))));
            double q=3*pipe.maximumArea()*slurry.mass()/slurry.waterVolume();
            var check=SolidMobility.check(SolidMobility.donor(water,slurry),slurry,pipe,q,SolidMobility.Outlet.MIXED,slurry.mass());
            assertEquals(share>.5?SolidMobility.Reason.PACKED_SLURRY:SolidMobility.Reason.MOBILE,check.reason(),"solids "+share+" of the condensed volume");
        }
        assertEquals(.5,SlurryTransport.SLUSH_FRACTION);assertEquals(.62,SlurryTransport.MAXIMUM_PACKING_FRACTION);
        assertTrue(Double.isFinite(SlurryTransport.effectiveViscosity(1e-3,.55)),"the viscosity law still states 0.55");
    }

    // ---- Gate 5: precipitation only in reservoirs ----

    /**
     * A let-down that would freeze passes (D23 (5)): liquid CO2 at 240 K and 2 MPa flows into a slack tank at the ambient,
     * where it flashes far below CO2's 194.63 K sublimation point at 1 atm. Nothing is refused and nothing freezes in the
     * transport; the stream arrives supersaturated, and the tank's next refresh makes dry ice at the sublimation point.
     */
    @Test void aLetDownThatWouldFreezePassesAndTheReceivingTankPrecipitatesAtItsRefresh() {
        var liquid=charge(pilot,240,2e6,new double[]{0,0,0,1,0},1);
        assertNotNull(liquid.liquidProperties());
        var gas=charge(pilot,250,PhaseLayout.AMBIENT_PRESSURE,new double[]{0,0,0,1,0},.05);
        var graph=new PassiveNetwork(List.of(new PassiveNetwork.Reservoir(1,0,liquid,PassiveNetwork.NodeKind.GENERATOR),vessel(2,gas,1)),
                List.of(new PassiveNetwork.Pipe(10,0,1,new PipeResistance.Geometry(10,.004,.000045,0))));
        var solver=new PassiveIntervalSolver(pilot);
        var first=solver.solve(graph,5,PassiveIntervalSolver.Settings.defaults(),()->{},.05);
        var tank=first.graph().reservoirs().get(1);
        System.out.printf(Locale.ROOT,"let-down: %.6e kg/s for 5 s; the tank at %.4f K, %.1f Pa, %.4e m3 of liquid, %.4e m3 of vapour, crystal %.3e kg, closures %d%n",
                first.averageMassFlows()[0],tank.state().temperature(),tank.state().pressure(),tank.state().liquidVolume(),tank.state().vaporVolume(),crystalKg(tank),
                first.graph().pipes().getFirst().blockedDirections());
        assertTrue(first.averageMassFlows()[0]>0,"the let-down passes");
        assertEquals(0,first.graph().pipes().getFirst().blockedDirections(),"no closure for freezing");
        assertEquals(0,crystalKg(tank),"no crystal before the tank's refresh");
        assertTrue(tank.state().temperature()<194.6,"the stream arrived supersaturated: "+tank.state().temperature());
        var refreshed=InventoryEquilibrium.refresh(first.graph(),pilot,()->{}).graph().reservoirs().get(1);
        System.out.printf(Locale.ROOT,"let-down, the tank's refresh: %.6f K, %.1f Pa, crystal %.6f kg%n",refreshed.state().temperature(),refreshed.state().pressure(),crystalKg(refreshed));
        assertTrue(crystalKg(refreshed)>0,"dry ice at the tank's refresh");
        assertEquals(co2().referenceTemperature(),refreshed.state().temperature(),1e-6,"dry ice beside its gas at 1 atm: the sublimation point");
    }

    // ---- Gate 6: a stock with one size per solid and no crystal ----

    /** A refresh changes nothing of stocks holding one size per solid and no crystal: the tank, the junction buffer and the
     * filter cake keep their every bit, and the island is not converted (it may rest). */
    @Test void aStockOfOneSizePerSolidAndNoCrystalIsLeftAsItIs() {
        var demo=demo();var one=new SolidInventory(List.of(grade(demo,"0.0001",3)));
        var junctionState=hotWater(155000).withSolids(one);
        var run=new PipeResistance.Geometry(10,.05,.000045,0);
        var graph=new PassiveNetwork(List.of(new PassiveNetwork.Reservoir(1,0,hotWater(160000).withSolids(one.scale(10))),
                new PassiveNetwork.Reservoir(2,0,junctionState,PassiveNetwork.NodeKind.JUNCTION,PassiveNetwork.mintedJunction(junctionState,5)),new PassiveNetwork.Reservoir(3,0,hotWater(150000))),
                List.of(new PassiveNetwork.Pipe(10,0,1,run),new PassiveNetwork.Pipe(11,1,2,run).withFilter(new InlineFilter(.01,1e6,one.scale(.1),-12.5))));
        var refreshed=InventoryEquilibrium.refresh(graph,water,()->{});
        assertFalse(refreshed.converted());
        for(int i=0;i<3;i++)assertSame(graph.reservoirs().get(i).inventory(),refreshed.graph().reservoirs().get(i).inventory(),"node "+i);
        assertSame(graph.reservoirs().get(1),refreshed.graph().reservoirs().get(1),"the junction");
        assertSame(graph.pipes().get(1).filter(),refreshed.graph().pipes().get(1).filter(),"the filter cake");
        assertTrue(refreshed.boundaries().isEmpty());
    }

    // ---- M3 gates ----

    /**
     * A closed tank charged supersaturated (10 % CO2 in nitrogen at 170 K and 1 MPa, below its frost point) precipitates
     * before it rests: nothing can drive a flow in it, but its refresh runs before the rest test (D23 (6)), deposits CO2
     * and warms the gas with the latent heat; the next interval is the rest interval, every bit kept.
     */
    @Test void aSupersaturatedClosedTankPrecipitatesBeforeItRests() {
        var tank=new PassiveNetwork(List.of(new PassiveNetwork.Reservoir(1,0,charge(pilot,170,1e6,new double[]{.9,0,0,.1,0},.1))),List.of());
        double co2=tank.reservoirs().getFirst().inventory().moles()[CO2];
        var solver=new PassiveIntervalSolver(pilot);
        var first=solver.solve(tank,5,PassiveIntervalSolver.Settings.defaults(),()->{},1);
        var node=first.graph().reservoirs().getFirst();
        System.out.printf(Locale.ROOT,"closed tank: %.6f K, %.1f Pa, crystal %.6f mol of %.6f mol CO2%n",node.state().temperature(),node.state().pressure(),
                crystalKg(node)/co2().molarMass(),co2);
        assertTrue(crystalKg(node)>0,"it precipitated");
        assertEquals(co2,node.inventory().moles()[CO2]+crystalKg(node)/co2().molarMass(),1e-12*co2,"CO2 conserved");
        var second=solver.solve(first.graph(),5,PassiveIntervalSolver.Settings.defaults(),()->{},1);
        assertSame(first.graph().reservoirs().getFirst().inventory(),second.graph().reservoirs().getFirst().inventory(),"then it rests");
    }

    /** An island whose solids nothing moves rests (D23 (6)): two tanks of the same slurry at one pressure, joined. Main
     * solved it every slice. */
    @Test void anIslandOfUnchangedSolidsRests() {
        var slurry=hotWater(160000).withSolids(new SolidInventory(List.of(grade(demo(),"0.0001",30))));
        var graph=new PassiveNetwork(List.of(new PassiveNetwork.Reservoir(1,0,slurry),new PassiveNetwork.Reservoir(2,0,slurry)),
                List.of(new PassiveNetwork.Pipe(10,0,1,new PipeResistance.Geometry(10,.05,.000045,0))));
        var result=new PassiveIntervalSolver(water).solve(graph,5,PassiveIntervalSolver.Settings.defaults(),()->{});
        for(int i=0;i<2;i++)assertSame(graph.reservoirs().get(i).inventory(),result.graph().reservoirs().get(i).inventory(),"node "+i+" kept to the bit: at rest");
    }

    /** The trace flush never deletes crystal mass: a vessel at its crystal equilibrium holding a 1e-25 mol methane trace
     * steps, the flush removes the trace (booked at the vessel) and the crystal population is kept to the bit. */
    @Test void theTraceFlushKeepsCrystalMass() {
        var tank=new PassiveNetwork(List.of(new PassiveNetwork.Reservoir(1,0,charge(pilot,170,1e6,new double[]{.9,0,0,.1,0},.1))),List.of());
        var settled=InventoryEquilibrium.refresh(tank,pilot,()->{}).graph().reservoirs().getFirst();
        var crystals=settled.inventory().solids();assertFalse(crystals.crystals().isEmpty());
        double[] moles=settled.inventory().moles();moles[METHANE]=1e-25;
        var traced=new PassiveNetwork(List.of(new PassiveNetwork.Reservoir(1,0,settled.state(),PassiveNetwork.NodeKind.RESERVOIR,
                new PassiveNetwork.Inventory(settled.inventory().volume(),moles,settled.inventory().internalEnergy(),crystals))),List.of());
        var result=new PassiveIntervalSolver(pilot).solve(traced,5,PassiveIntervalSolver.Settings.defaults(),()->{},1);
        var after=result.graph().reservoirs().getFirst();
        assertEquals(0,after.inventory().moles()[METHANE],"the trace is flushed");
        assertTrue(result.boundaries().stream().anyMatch(b->b.nodeId()==1&&b.moles()[METHANE]==-1e-25),"and booked at the vessel: "+result.boundaries());
        assertEquals(crystals,after.inventory().solids(),"the crystal is kept to the bit");
    }
}
