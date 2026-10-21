package com.wormzjl.icheme.client;

import com.wormzjl.icheme.network.TankDisplayNetwork;
import com.wormzjl.icheme.runtime.fluid.TankDisplay;
import com.wormzjl.icheme.runtime.fluid.TankWatchers;
import java.util.*;
import net.minecraft.core.BlockPos;

/**
 * The client's open-tank displays (batch 2026-10-21-floating-roof-tanks, owner decision FR4 R-B), by node cell: what the
 * server sent for each tank this client watches, read by the tank renderer. {@link #track} keeps the watch set to the
 * tanks the renderer reports within {@link TankWatchers#RANGE}, nearest first, at most
 * {@link TankWatchers#MAXIMUM_PER_PLAYER}, and asks the server only for the difference. Plain Java on the client's main
 * thread.
 */
public final class TankDisplays {
    private TankDisplays() {}
    private static final Map<BlockPos,TankDisplay> DISPLAYS=new HashMap<>();
    private static final Set<BlockPos> WATCHED=new HashSet<>();

    /** A display the server sent; dropped if the tank is no longer watched. */
    public static void accept(BlockPos node,TankDisplay display){if(WATCHED.contains(node))DISPLAYS.put(node.immutable(),display);}
    /** The last display of the tank at {@code node}, or null before the first one arrives. */
    public static TankDisplay get(BlockPos node){return DISPLAYS.get(node);}
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
        TankDisplayNetwork.request(watch,unwatch);
    }
    /** Forgets everything (the client left the world). */
    public static void clear(){DISPLAYS.clear();WATCHED.clear();}
}
