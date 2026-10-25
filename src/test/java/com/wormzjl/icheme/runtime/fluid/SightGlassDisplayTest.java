package com.wormzjl.icheme.runtime.fluid;

import com.wormzjl.icheme.network.SightGlassNetwork;
import com.wormzjl.icheme.science.equipment.*;
import com.wormzjl.icheme.science.fluid.network.PipeTransfer;
import com.wormzjl.icheme.science.fluid.state.*;
import com.wormzjl.icheme.science.fluid.topology.TopologyCompiler.Kind;
import com.wormzjl.icheme.science.material.MaterialCatalog;
import io.netty.buffer.Unpooled;
import net.minecraft.core.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SightGlassDisplayTest {
    private static final MaterialCatalog CATALOG=MaterialCatalog.bundled();
    private static final String PACKAGE="icheme:tjl20_methane";
    private static final List<String> COMPONENTS=List.of("crude_pc01","crude_pc12","Water");
    private static final double[] WEIGHTS={.1,.6,.018};
    private static final SightGlassBubbles.Motion UP=new SightGlassBubbles.Motion(0,1);

    @Test void gasLiquidRatioControlsBubbleDensityAndOtherPhasesRetainHaze(){
        assertEquals(0,display(new double[]{1,0,0},SolidInventory.EMPTY,1).bubbles(),"two components in one liquid stay homogeneous");
        for(double[] volumes:List.of(new double[]{.5,0,.5},new double[]{.4,.4,.2})){
            var sample=display(volumes,SolidInventory.EMPTY,1);assertTrue(sample.bubbles()>0);assertTrue(sample.argb()>>>24>=200);
        }
        var solids=new SolidInventory(List.of(new SolidInventory.Population(CATALOG.solids().require("icheme:demo_particle"),ParticleSize.micrometres("100"),.25)));
        assertEquals(0,display(new double[]{1,0,0},solids,1).bubbles(),"suspended solids cause haze, not gas bubbles");
        assertEquals(0,display(new double[]{.5,.5,0},SolidInventory.EMPTY,1).bubbles(),"two liquids retain haze without gas bubbles");
        int previous=0,previousCount=0;
        for(double gas:new double[]{.01,.1,1,9,99}){
            var sample=display(new double[]{.4,.6,gas},SolidInventory.EMPTY,1);
            assertTrue(sample.bubbles()>previous,"gas-rich flow must not lose bubbles as haze declines");previous=sample.bubbles();
            int count=SightGlassBubbles.at(100,9,sample.bubbles(),UP).size();assertTrue(count>=previousCount);previousCount=count;
            assertEquals(sample.bubbles(),display(new double[]{4,6,10*gas},SolidInventory.EMPTY,1).bubbles(),"ratio is independent of total volume");
            assertEquals(sample.bubbles(),display(new double[]{.4,.6,gas},solids,1).bubbles(),"solids do not dilute the gas/liquid ratio");
        }
        assertEquals(4,SightGlassBubbles.at(100,9,display(new double[]{1,0,1},SolidInventory.EMPTY,1).bubbles(),UP).size());
        assertEquals(8,SightGlassBubbles.at(100,9,display(new double[]{1,0,99},SolidInventory.EMPTY,1).bubbles(),UP).size());
        assertEquals(SightGlassDisplay.Sample.CLEAR,display(new double[]{.5,0,.5},SolidInventory.EMPTY,0),"old stream with stopped speed clears");
        assertEquals(SightGlassDisplay.Sample.CLEAR,display(new double[]{0,0,1},SolidInventory.EMPTY,1));
        assertEquals(SightGlassDisplay.Sample.CLEAR,display(new double[3],SolidInventory.EMPTY,1));
        assertEquals(SightGlassDisplay.Sample.CLEAR,SightGlassDisplay.of(base(),COMPONENTS,WEIGHTS,CATALOG,PACKAGE,List.of()));
    }

    @Test void displayWireRoundTripsAndRejectsMotionWithoutLiquid(){
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        var target=new SightGlassNetwork.Target(new BlockPos(-20,64,44),9);
        try{
            var samples=new ArrayList<SightGlassDisplay.Sample>();
            samples.add(SightGlassDisplay.Sample.CLEAR);samples.add(new SightGlassDisplay.Sample(0xC8A17A40,0,null));
            for(var direction:PhysicalFluidTopology.Direction.values())samples.add(new SightGlassDisplay.Sample(0xFF9E9987,255,direction));
            for(var sample:samples){
                var payload=new SightGlassNetwork.DisplayPayload(target,sample);SightGlassNetwork.DisplayPayload.STREAM_CODEC.encode(buffer,payload);
                assertEquals(payload,SightGlassNetwork.DisplayPayload.STREAM_CODEC.decode(buffer));
            }
            buffer.clear();buffer.writeBlockPos(target.position());buffer.writeLong(target.identity());buffer.writeInt(0);buffer.writeByte(1);buffer.writeByte(0);
            assertThrows(IllegalArgumentException.class,()->SightGlassNetwork.DisplayPayload.STREAM_CODEC.decode(buffer));
            for(int code:new int[]{7,255}){
                buffer.clear();buffer.writeBlockPos(target.position());buffer.writeLong(target.identity());buffer.writeInt(0xFF123456);buffer.writeByte(1);buffer.writeByte(code);
                assertThrows(IllegalArgumentException.class,()->SightGlassNetwork.DisplayPayload.STREAM_CODEC.decode(buffer));
            }
            assertThrows(IllegalArgumentException.class,()->new SightGlassDisplay.Sample(0xFF123456,-1,null));
            assertThrows(IllegalArgumentException.class,()->new SightGlassDisplay.Sample(0xFF123456,256,PhysicalFluidTopology.Direction.EAST));
            assertThrows(IllegalArgumentException.class,()->new SightGlassDisplay.Sample(0xFF123456,1,null));
        }finally{buffer.release();}
    }

    @Test void motionIsStableContainedAndBetweenTheLiquidAndGlass(){
        assertEquals(List.of(),SightGlassBubbles.at(100,9,0,UP));
        assertEquals(SightGlassBubbles.at(100,9,255,UP),SightGlassBubbles.at(100,9,255,UP));
        assertNotEquals(SightGlassBubbles.at(100,9,255,UP),SightGlassBubbles.at(110,9,255,UP));
        for(int strength:new int[]{1,100,255})for(long seed:new long[]{0,9,Long.MAX_VALUE})for(int tick=0;tick<400;tick+=13)
            for(var bubble:SightGlassBubbles.at(tick,seed,strength,UP)){
                assertTrue(bubble.radius()>=0&&Double.isFinite(bubble.radius()));
                assertTrue(Math.hypot(bubble.x(),bubble.y())+bubble.radius()<=.971,"entire ring fits inside the aperture");
            }
        for(var axis:SightGlassGeometry.Axis.values())for(double bore:new double[]{.001,.05,1}){
            int face=axis==SightGlassGeometry.Axis.X?2:5;
            var key=new SightGlassGeometry.Key(new LineGeometry.Key(Kind.PIPE,face,3<<(face&~1),PipeSizes.code(PipeSizes.outside(bore)),0,0,false),axis);
            var window=SightGlassGeometry.window(key);var geometry=SightGlassGeometry.build(key,false);int c=axis.ordinal();
            double liquid=0,glass=1;
            for(var p:geometry.liquid())for(int i=c;i<p.positions().length;i+=3)liquid=Math.max(liquid,Math.abs(p.positions()[i]-.5));
            for(var p:geometry.glass())for(int i=c;i<p.positions().length;i+=3)glass=Math.min(glass,Math.abs(p.positions()[i]-.5));
            assertTrue(window.plane()>liquid&&window.plane()<glass,"rings have distinct depth behind the window");
        }
    }

    @Test void acceptedDirectionReversesAndMissingLocalMappingsDoNotInventMotion(){
        for(var direction:PhysicalFluidTopology.Direction.values())for(boolean reverse:new boolean[]{false,true}){
            var info=new FluidView.PipeInfo(new PipeTransfer.Stream(0,new double[3][3],new double[3]),List.of(new FluidView.PipeConnection(7,1,1,0,reverse)),false,false);
            var mapping=new PhysicalFluidTopology.View(0,7,true,.5,direction);
            assertEquals(reverse?PhysicalFluidTopology.opposite(direction):direction,SightGlassDisplay.flowDirection(info,List.of(mapping)));
            assertNull(SightGlassDisplay.flowDirection(info,List.of(new PhysicalFluidTopology.View(0,7,true,Double.NaN,direction))));
            assertNull(SightGlassDisplay.flowDirection(info,List.of(new PhysicalFluidTopology.View(0,7,true))));
        }
    }

    @Test void bubbleTravelFollowsEveryRunAndAgreesThroughBothWindows(){
        for(var window:SightGlassGeometry.Axis.values())for(var run:SightGlassGeometry.Axis.values())if(run!=window)
            for(boolean positive:new boolean[]{false,true}){
                var motion=SightGlassBubbles.Motion.along(window,run,positive);
                var before=SightGlassBubbles.at(50,9,255,motion);var after=SightGlassBubbles.at(50.001,9,255,motion);
                for(int i=0;i<before.size();i++){
                    double[] front=world(window,before.get(i),false),back=world(window,before.get(i),true),next=world(window,after.get(i),false);
                    assertArrayEquals(front,back,1e-12,"paired windows show the same physical bubble path");
                    assertTrue((next[run.ordinal()]-front[run.ordinal()])*(positive?1:-1)>0,"bubble advances in accepted flow direction");
                    assertTrue(Math.hypot(after.get(i).x(),after.get(i).y())+after.get(i).radius()<=.971);
                }
            }
    }
    private static double[] world(SightGlassGeometry.Axis window,SightGlassBubbles.Bubble bubble,boolean back){
        double x=back?-bubble.x():bubble.x(),y=bubble.y();
        // The renderer turns the back face 180 degrees about local Y and mirrors its bubble centre.
        if(back)x=-x;
        return switch(window){case X->new double[]{0,y,-x};case Y->new double[]{x,0,-y};case Z->new double[]{x,y,0};};
    }

    private static SightGlassDisplay.Sample display(double[] volumes,SolidInventory solids,double speed){
        double[][] n=new double[3][3];n[0][0]=volumes[0];n[0][1]=volumes[0];n[1][2]=volumes[1];n[2][0]=volumes[2];
        var stream=new PipeTransfer.Stream(Arrays.stream(volumes).sum(),n,volumes,solids);
        var info=new FluidView.PipeInfo(stream,List.of(new FluidView.PipeConnection(7,1,speed,0,false)),false,false);
        return SightGlassDisplay.of(base().withPipeInfo(info,1),COMPONENTS,WEIGHTS,CATALOG,PACKAGE,List.of(new PhysicalFluidTopology.View(0,7,true,.5,PhysicalFluidTopology.Direction.EAST)));
    }
    private static FluidView base(){return new FluidView(1,0,0,0,"FULL",null,0,List.of(),null,1,List.of(),null);}
}
