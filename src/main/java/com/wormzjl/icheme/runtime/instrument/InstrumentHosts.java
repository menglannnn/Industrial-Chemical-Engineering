package com.wormzjl.icheme.runtime.instrument;

import com.wormzjl.icheme.science.equipment.EquipmentShape;
import com.wormzjl.icheme.science.equipment.PipeSizes;
import com.wormzjl.icheme.science.equipment.ShapeRasterizer;
import com.wormzjl.icheme.science.fluid.topology.TopologyCompiler;
import com.wormzjl.icheme.world.item.GaugeSettings;
import com.wormzjl.icheme.world.level.block.*;
import com.wormzjl.icheme.world.level.block.entity.*;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.*;
import java.util.*;

/** Loaded host index and shared mount selection. Does not load chunks or inspect process contents. */
public final class InstrumentHosts {
    private InstrumentHosts(){}
    private static final Map<Level,Set<BlockPos>> LOADED=new WeakHashMap<>();
    private static final Map<Level,Set<BlockPos>> INSTALLED=new WeakHashMap<>();
    public record Host(long identity,BlockEntity entity,List<InstrumentMount> mounts,boolean pipe) {
        public Host{mounts=List.copyOf(mounts);}
        public InstrumentMetadata metadata(){return ((AccessoryHost)entity).instruments();}
        public InstrumentMount mount(String id){return mounts.stream().filter(m->m.id().equals(id)).findFirst().orElse(null);}
        public boolean occupied(InstrumentMount mount){return pipe?legacy(entity.getBlockState())!=null||!metadata().installed().isEmpty():metadata().installed().stream().anyMatch(i->i.key().mount().equals(mount.id()));}
    }
    public record Selection(Host host,InstrumentMount mount,double distance) {}
    private static final Object INDEX_LOCK=new Object();
    public static void loaded(BlockEntity be){if(be.getLevel()==null||!(be instanceof FluidDeviceBlockEntity pipe&&pipe.getBlockState().getBlock() instanceof FluidDeviceBlock block&&block.kind()==TopologyCompiler.Kind.PIPE||be instanceof EquipmentShellBlockEntity shell&&shell.shape()!=null&&shell.fluidIdentity()>0))return;synchronized(INDEX_LOCK){LOADED.computeIfAbsent(be.getLevel(),l->new HashSet<>()).add(be.getBlockPos().immutable());var set=INSTALLED.computeIfAbsent(be.getLevel(),l->new HashSet<>());if(be instanceof AccessoryHost h&&!h.instruments().installed().isEmpty())set.add(be.getBlockPos().immutable());else set.remove(be.getBlockPos());}}
    private static List<BlockPos> loaded(Level level,boolean installed){synchronized(INDEX_LOCK){return List.copyOf((installed?INSTALLED:LOADED).getOrDefault(level,Set.of()));}}
    public static void unloaded(BlockEntity be){synchronized(INDEX_LOCK){var set=LOADED.get(be.getLevel());if(set!=null)set.remove(be.getBlockPos());var equipped=INSTALLED.get(be.getLevel());if(equipped!=null)equipped.remove(be.getBlockPos());}}
    public static List<Host> nearby(Level level,Vec3 centre,double radius){return nearby(level,centre,radius,false);}
    public static List<Host> nearby(Level level,Vec3 centre,double radius,boolean installedOnly){var result=new ArrayList<Host>();for(var pos:loaded(level,installedOnly)){if(pos.getCenter().distanceToSqr(centre)>(radius+20)*(radius+20)||!level.hasChunkAt(pos))continue;var h=at(level,pos);if(h!=null&&h.mounts.stream().anyMatch(m->m.centre().distanceToSqr(centre)<=radius*radius))result.add(h);}return result.stream().collect(java.util.stream.Collectors.toMap(Host::identity,h->h,(a,b)->a,LinkedHashMap::new)).values().stream().toList();}
    public static Host at(Level level,BlockPos position){if(!level.hasChunkAt(position))return null;var be=level.getBlockEntity(position);
        if(be instanceof FluidDeviceBlockEntity pipe&&pipe.getBlockState().getBlock() instanceof FluidDeviceBlock block&&block.kind()==TopologyCompiler.Kind.PIPE&&pipe.fluidIdentity()>0)return new Host(pipe.fluidIdentity(),pipe,pipeMounts(position,pipe.bore(),pipe.getBlockState()),true);
        if(be instanceof EquipmentShellBlockEntity shell&&shell.shape()!=null){var shape=shell.shape();var c=ShapeRasterizer.fluidCell(shape);if(c==null)return null;var root=shell.origin().offset(c.x(),c.y(),c.z());if(!root.equals(position))return at(level,root);if(shell.fluidIdentity()>0)return new Host(shell.fluidIdentity(),shell,reservoirMounts(shape,shell.origin()),false);}
        return null;
    }
    public static List<InstrumentMount> pipeMounts(BlockPos pos,double bore,net.minecraft.world.level.block.state.BlockState state){return pipeMounts(pos,bore,FluidDeviceBlock.connectionMask(state),state.getValue(FluidDeviceBlock.FACING));}
    public static List<InstrumentMount> pipeMounts(BlockPos pos,double bore,int mask,Direction facing){if(!com.wormzjl.icheme.science.equipment.SightGlassGeometry.supports(mask))return List.of();Direction.Axis axis=facing.getAxis();for(var d:Direction.values())if((mask&(1<<d.ordinal()))!=0){axis=d.getAxis();break;}var mounts=new ArrayList<InstrumentMount>();double radius=PipeSizes.outside(bore)/2;for(var face:Direction.Plane.HORIZONTAL){if(face.getAxis()==axis)continue;mounts.add(new InstrumentMount("pipe_"+face.getName(),pos.getCenter().add(Vec3.atLowerCornerOf(face.getNormal()).scale(radius+.13)),face,Set.of(GaugeSettings.Kind.PRESSURE,GaugeSettings.Kind.TEMPERATURE),false));}return mounts;}
    public static List<InstrumentMount> reservoirMounts(EquipmentShape shape,BlockPos origin){return com.wormzjl.icheme.science.equipment.InstrumentSockets.of(shape).stream().map(t->new InstrumentMount(t.id(),new Vec3(origin.getX()+t.x(),origin.getY()+t.y(),origin.getZ()+t.z()),Direction.valueOf(shape.front().name()),Set.of(t.pressure()?GaugeSettings.Kind.PRESSURE:GaugeSettings.Kind.TEMPERATURE),t.top())).toList();}
    public static Selection select(Collection<Host> hosts,Vec3 eye,Vec3 direction,double reach,GaugeSettings.Kind kind,boolean installedOnly){Selection best=null;Vec3 end=eye.add(direction.scale(reach));for(var h:hosts)for(var m:h.mounts){if(kind!=null&&!m.kinds().contains(kind))continue;if(installedOnly&&h.metadata().installed().stream().noneMatch(i->i.key().mount().equals(m.id())))continue;var hit=m.bounds().clip(eye,end);if(hit.isEmpty())continue;double d=hit.get().distanceTo(eye);if(best==null||d<best.distance)best=new Selection(h,m,d);}return best;}
    public static boolean reachable(net.minecraft.world.entity.player.Player player,Host host,AccessorySelection mount){var eye=player.getEyePosition();if(mount.bounds().distanceToSqr(eye)>player.blockInteractionRange()*player.blockInteractionRange())return false;var point=mount.bounds().clip(eye,mount.bounds().getCenter()).orElse(mount.bounds().getCenter());var hit=player.level().clip(new net.minecraft.world.level.ClipContext(eye,point,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,player));return hit.getType()==HitResult.Type.MISS||hit.getLocation().distanceTo(eye)+.025>=point.distanceTo(eye);}
    public static boolean clear(Level level,Host host,InstrumentMount mount){for(var pos:BlockPos.betweenClosedStream(mount.bounds()).toList()){if(!level.hasChunkAt(pos))return false;if(pos.equals(host.entity().getBlockPos())||level.getBlockState(pos).canBeReplaced())continue;var owner=at(level,pos);if(owner==null||owner.identity()!=host.identity())return false;}return true;}
    public static AccessoryStore.Type legacy(net.minecraft.world.level.block.state.BlockState state){if(FluidDeviceBlock.hasSightGlass(state))return AccessoryStore.Type.SIGHT_GLASS;if(FluidDeviceBlock.hasPressureValve(state))return AccessoryStore.Type.PRESSURE_VALVE;if(!state.hasProperty(FluidDeviceBlock.SHUTOFF))return null;return switch(FluidDeviceBlock.cutoff(state)){case NONE->null;case CHECK->AccessoryStore.Type.CHECK;default->AccessoryStore.Type.SHUTOFF;};}
}
