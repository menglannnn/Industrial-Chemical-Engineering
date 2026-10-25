package com.wormzjl.icheme.runtime.instrument;

import com.wormzjl.icheme.science.equipment.*;
import com.wormzjl.icheme.world.level.block.FluidDeviceBlock;
import com.wormzjl.icheme.world.level.block.entity.FluidDeviceBlockEntity;
import net.minecraft.world.phys.AABB;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Wrench hit boxes come from the existing fittings' actual model extents, including tall actuators/handles. */
public record PipeAccessoryMount(AABB bounds) implements AccessorySelection {
    @Override public String id(){return "pipe_accessory";}
    private record Look(LineGeometry.Key key,AccessoryStore.Type type,boolean open,SightGlassGeometry.Axis window){}
    private static final Map<Look,AABB> CACHE=new ConcurrentHashMap<>();
    public static PipeAccessoryMount of(FluidDeviceBlockEntity pipe){var s=pipe.getBlockState();var type=InstrumentHosts.legacy(s);if(type==null)return null;var drawn=pipe.getModelData().get(FluidDeviceBlockEntity.DRAWN);var key=new LineGeometry.Key(pipe.kind(),s.getValue(FluidDeviceBlock.FACING).ordinal(),FluidDeviceBlock.connectionMask(s),PipeSizes.code(PipeSizes.outside(pipe.bore())),drawn==null?0:drawn.faces(),drawn==null?0:drawn.flanged(),drawn!=null&&drawn.spool());var look=new Look(key,type,FluidDeviceBlock.cutoff(s)==com.wormzjl.icheme.runtime.fluid.PhysicalFluidTopology.Cutoff.OPEN,SightGlassGeometry.Axis.valueOf(s.getValue(FluidDeviceBlock.SIGHT_GLASS_AXIS).name()));return new PipeAccessoryMount(CACHE.computeIfAbsent(look,PipeAccessoryMount::build).move(pipe.getBlockPos()));}
    private static AABB build(Look look){List<Mesh.Polygon> polygons=switch(look.type){case PRESSURE_VALVE->PressureValveGeometry.polygons(look.key,false);case SHUTOFF->ShutoffValveGeometry.polygons(look.key,look.open,false);case CHECK->CheckValveGeometry.polygons(look.key,false);case SIGHT_GLASS->{var g=SightGlassGeometry.build(new SightGlassGeometry.Key(look.key,look.window),false);var all=new ArrayList<>(g.metal());all.addAll(g.glass());yield all;}default->throw new IllegalArgumentException("Not a pipe fitting");};double x=Double.POSITIVE_INFINITY,y=x,z=x,X=Double.NEGATIVE_INFINITY,Y=X,Z=X;for(var p:polygons){var v=p.positions();for(int i=0;i<v.length;i+=3){x=Math.min(x,v[i]);y=Math.min(y,v[i+1]);z=Math.min(z,v[i+2]);X=Math.max(X,v[i]);Y=Math.max(Y,v[i+1]);Z=Math.max(Z,v[i+2]);}}return new AABB(x,y,z,X,Y,Z).inflate(.015);}
}
