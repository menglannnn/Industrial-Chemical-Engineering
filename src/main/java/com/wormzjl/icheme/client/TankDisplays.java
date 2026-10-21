package com.wormzjl.icheme.client;

import com.wormzjl.icheme.network.TankDisplayNetwork;
import com.wormzjl.icheme.runtime.fluid.FluidPresentation;
import com.wormzjl.icheme.runtime.fluid.TankDisplay;
import com.wormzjl.icheme.runtime.fluid.TankWatchers;
import com.wormzjl.icheme.world.level.block.FloatingRoofs;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/**
 * The client's open-tank displays (batch 2026-10-21-floating-roof-tanks, owner decision FR4 R-B), by node cell: what the
 * server sent for each tank this client watches, read by the tank renderer. {@link #track} keeps the watch set to the
 * tanks the renderer reports within {@link TankWatchers#RANGE}, nearest first, at most
 * {@link TankWatchers#MAXIMUM_PER_PLAYER}, and asks the server only for the difference. Plain Java on the client's main
 * thread.
 *
 * <p>A new display arrives once a presentation bucket ({@link FluidPresentation#BUCKET_TICKS}) inside the glide the server
 * gave the tank ({@link TankDisplay.Glide}, owner requests 2026-10-21: the roof moves, and a player on it rides it): the
 * roof moves from where it stood to it over that time on the game clock, here as on the server, and the level's roofs
 * ({@link FloatingRoofs}) collide by it. The server sends nothing more.</p>
 */
public final class TankDisplays {
    private TankDisplays() {}
    private static final Map<BlockPos,TankDisplay.Glide> DISPLAYS=new HashMap<>();
    private static final Set<BlockPos> WATCHED=new HashSet<>();

    /** A glide the server sent; dropped if the tank is no longer watched. */
    public static void accept(BlockPos node,TankDisplay.Glide glide) {
        if(!WATCHED.contains(node))return;
        DISPLAYS.put(node.immutable(),glide);
        var level=Minecraft.getInstance().level;if(level!=null)FloatingRoofs.of(level).glide(node,glide);
    }
    /** The display of the tank at {@code node} at game time {@code ticks} (with the frame's partial tick), or null before the first one arrives. */
    public static TankDisplay get(BlockPos node,double ticks){var glide=DISPLAYS.get(node);return glide==null?null:glide.at(ticks);}
    /**
     * Watches exactly {@code inRange} (node cells within range, nearest first; the tail beyond the maximum is not
     * watched): asks the server for the new ones and releases the others.
     */
    public static void track(List<BlockPos> inRange) {
        var wanted=new LinkedHashSet<BlockPos>();
        for(var pos:inRange){if(wanted.size()>=TankWatchers.MAXIMUM_PER_PLAYER)break;wanted.add(pos.immutable());}
        var watch=new ArrayList<BlockPos>();for(var pos:wanted)if(!WATCHED.contains(pos))watch.add(pos);
        var unwatch=new ArrayList<BlockPos>();for(var pos:WATCHED)if(!wanted.contains(pos))unwatch.add(pos);
        if(watch.isEmpty()&&unwatch.isEmpty())return;
        WATCHED.removeAll(unwatch);unwatch.forEach(DISPLAYS::remove);WATCHED.addAll(watch);
        var level=Minecraft.getInstance().level;if(level!=null)for(var pos:unwatch)FloatingRoofs.of(level).glide(pos,null);
        TankDisplayNetwork.request(watch,unwatch);
    }
    /** Forgets everything (the client left the world). */
    public static void clear(){DISPLAYS.clear();WATCHED.clear();}
}
