package com.wormzjl.icheme.runtime.fluid;

import com.wormzjl.icheme.science.fluid.state.SolidInventory;
import com.wormzjl.icheme.science.fluid.thermo.FluidMaterialCatalog;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * An open tank's display and its watchers (batch 2026-10-21-floating-roof-tanks, WP4, owner decisions FR4 and FR7): the
 * liquid's colour is the subtractive (Beer-Lambert) mix of its components by mass fraction, the level reads the condensed
 * volume over the footprint, and a player watches at most {@link TankWatchers#MAXIMUM_PER_PLAYER} tanks, each until it
 * unwatches, leaves or the tank goes.
 */
class TankDisplayTest {
    private static final String PACKAGE=FluidMaterialCatalog.networkPackage(EventPathWorld.CATALOG);
    private static final List<String> AXIS=new EventPathWorld().model.components();
    private static final double[] WEIGHTS=new EventPathWorld().model.molecularWeights();

    @Test void aPureLiquidTakesItsComponentsColourAndTwoMixByTransmittance() {
        int a=0,b=1;var lookA=EventPathWorld.CATALOG.componentAppearance(PACKAGE,AXIS.get(a));var lookB=EventPathWorld.CATALOG.componentAppearance(PACKAGE,AXIS.get(b));
        double[] n=new double[AXIS.size()];n[a]=1;
        int pure=TankDisplay.mix(n,AXIS,WEIGHTS,EventPathWorld.CATALOG,PACKAGE);
        assertEquals(lookA.rgb()&0xFFFFFF,pure&0xFFFFFF,1,"a single component keeps its own colour (within the floor)");
        // Equal masses: each channel is the geometric mean of the two transmittances.
        n=new double[AXIS.size()];n[a]=1/WEIGHTS[a];n[b]=1/WEIGHTS[b];
        int mixed=TankDisplay.mix(n,AXIS,WEIGHTS,EventPathWorld.CATALOG,PACKAGE);
        for(int shift:new int[]{16,8,0}) {
            double ta=Math.max(lookA.rgb()>>shift&255,.5)/255,tb=Math.max(lookB.rgb()>>shift&255,.5)/255;
            assertEquals(Math.round(Math.sqrt(ta*tb)*255),mixed>>shift&255,1,"channel "+shift);
        }
        double clear=Math.sqrt(Math.max(lookA.transparency(),.5/255)*Math.max(lookB.transparency(),.5/255));
        assertEquals(Math.round((1-clear)*255),mixed>>>24,1,"the opacity mixes the same way");
        assertEquals(0,TankDisplay.mix(new double[AXIS.size()],AXIS,WEIGHTS,EventPathWorld.CATALOG,PACKAGE),"nothing to colour");
    }

    @Test void theDisplayReadsTheLevelAndTheFreeWaterUnderIt() {
        // A 100 m3 tank 10 m high (10 m2 footprint) holding 30 m3 of hydrocarbon over 20 m3 of free water.
        int c=AXIS.size();double[][] n=new double[3][c];n[0][0]=1000;n[1][c-1]=1000;
        var state=new FluidView.State(101325,298.15,50,40000,new double[]{30,20,0},n,SolidInventory.EMPTY,100);
        var view=new FluidView(1,0,0,0,"STEADY",state,0,List.of()).withTank(new FluidView.Tank(10,true,0,0,100,101325.0,140551.6));
        var display=TankDisplay.of(view,AXIS,WEIGHTS,EventPathWorld.CATALOG,PACKAGE);
        assertEquals(5,display.level(),1e-6);assertEquals(2,display.waterLevel(),1e-6);assertEquals(10,display.height(),1e-6);
        assertEquals(EventPathWorld.CATALOG.componentAppearance(PACKAGE,"Water").argb(),display.waterArgb());
        assertNull(TankDisplay.of(new FluidView(1,0,0,0,"STEADY",state,0,List.of()),AXIS,WEIGHTS,EventPathWorld.CATALOG,PACKAGE),"no tank, no display");
        assertEquals(TankDisplay.empty(10),TankDisplay.of(new FluidView(1,0,0,0,"EMPTY",null,0,List.of()).withTank(new FluidView.Tank(10,true,0,0,100,null,null)),AXIS,WEIGHTS,EventPathWorld.CATALOG,PACKAGE));
    }

    @Test void watchersAreBoundedAndEndWithThePlayerOrTheTank() {
        var watchers=new TankWatchers();var alice=UUID.randomUUID();var bob=UUID.randomUUID();
        for(long d=0;d<TankWatchers.MAXIMUM_PER_PLAYER;d++)assertTrue(watchers.watch(alice,d));
        assertFalse(watchers.watch(alice,1000),"over the maximum");assertFalse(watchers.watch(alice,3),"already watched");
        assertTrue(watchers.watch(bob,3));assertEquals(Set.of(alice,bob),watchers.watchers(3));
        watchers.unwatch(alice,3);assertEquals(Set.of(bob),watchers.watchers(3));assertTrue(watchers.watch(alice,1000),"a freed slot");
        watchers.remove(3);assertEquals(Set.of(),watchers.watchers(3));assertEquals(0,watchers.watched(bob));
        watchers.forget(alice);assertEquals(0,watchers.watched(alice));assertEquals(Set.of(),watchers.watchers(0));
    }
}
