package com.wormzjl.icheme.client;

import com.wormzjl.icheme.network.SightGlassNetwork;
import com.wormzjl.icheme.network.SightGlassNetwork.Target;
import com.wormzjl.icheme.runtime.fluid.TankWatchers;
import com.wormzjl.icheme.world.level.block.FluidDeviceBlock;
import com.wormzjl.icheme.world.level.block.entity.FluidDeviceBlockEntity;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/** Ephemeral, identity-bound colour cache; never obtains process data through a block update tag. Client thread only. */
public final class SightGlassDisplays {
    private static final Map<Target,Integer> COLOURS=new HashMap<>();
    private static final Set<Target> WATCHED=new HashSet<>();
    private SightGlassDisplays(){}
    public static void accept(Target target,int argb) {
        var level=Minecraft.getInstance().level;
        if(level!=null&&WATCHED.contains(target)&&FluidDeviceBlock.hasSightGlass(level.getBlockState(target.position()))
                &&level.getBlockEntity(target.position()) instanceof FluidDeviceBlockEntity entity&&entity.fluidIdentity()==target.identity())COLOURS.put(target,argb);
    }
    public static int colour(FluidDeviceBlockEntity entity){long id=entity.fluidIdentity();return id<=0?0:COLOURS.getOrDefault(new Target(entity.getBlockPos(),id),0);}
    /** Nearest first, bounded independently of tanks. */
    public static void track(List<Target> near) {
        var wanted=new LinkedHashSet<Target>();for(var target:near){if(wanted.size()>=TankWatchers.MAXIMUM_PER_PLAYER)break;wanted.add(target);}
        var watch=new ArrayList<Target>();for(var target:wanted)if(!WATCHED.contains(target))watch.add(target);
        var unwatch=new ArrayList<Target>();for(var target:WATCHED)if(!wanted.contains(target))unwatch.add(target);
        WATCHED.removeAll(unwatch);unwatch.forEach(COLOURS::remove);WATCHED.addAll(watch);
        SightGlassNetwork.request(watch,unwatch);
    }
    /** A replaced/unloaded client binding must explicitly request again even if its chunk reloads within one poll. */
    public static void forget(BlockPos pos) {
        var gone=WATCHED.stream().filter(t->t.position().equals(pos)).toList();
        WATCHED.removeAll(gone);gone.forEach(COLOURS::remove);
        if(Minecraft.getInstance().getConnection()!=null)SightGlassNetwork.request(List.of(),gone);
    }
    public static void clear(){COLOURS.clear();WATCHED.clear();}
}
