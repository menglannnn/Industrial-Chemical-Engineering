package com.wormzjl.icheme.client;

import com.wormzjl.icheme.network.TankDisplayNetwork;
import com.wormzjl.icheme.runtime.fluid.FluidPresentation;
import com.wormzjl.icheme.runtime.fluid.TankDisplay;
import com.wormzjl.icheme.runtime.fluid.TankWatchers;
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
 * <p>A new display arrives once a presentation bucket ({@link FluidPresentation#BUCKET_TICKS}); the roof glides to it
 * from where it stood over that time (owner request 2026-10-21, "make the top lid model actually moving"), its levels
 * interpolated on the client's game time. Drawing only: the server sends nothing more.</p>
 */
public final class TankDisplays {
    private TankDisplays() {}
    /** What a tank showed when its latest display arrived, that display, and the game tick it arrived at. */
    private record Shown(TankDisplay from,TankDisplay to,long tick) {
        /** The display {@code ticks} after it arrived: its levels moved from {@code from}'s towards {@code to}'s, linearly. */
        TankDisplay at(double ticks) {
            double t=Math.max(0,Math.min(1,(ticks-tick)/FluidPresentation.BUCKET_TICKS));
            if(t>=1)return to;
            return new TankDisplay((float)(from.level()+(to.level()-from.level())*t),(float)(from.waterLevel()+(to.waterLevel()-from.waterLevel())*t),
                    to.height(),to.oilArgb(),to.waterArgb());
        }
    }
    private static final Map<BlockPos,Shown> DISPLAYS=new HashMap<>();
    private static final Set<BlockPos> WATCHED=new HashSet<>();

    /** A display the server sent; dropped if the tank is no longer watched. The first one is shown at once. */
    public static void accept(BlockPos node,TankDisplay display) {
        if(!WATCHED.contains(node))return;
        long now=now();var old=DISPLAYS.get(node);
        DISPLAYS.put(node.immutable(),new Shown(old==null?display:old.at(now),display,now));
    }
    /** The display of the tank at {@code node} at game time {@code ticks} (with the frame's partial tick), or null before the first one arrives. */
    public static TankDisplay get(BlockPos node,double ticks){var shown=DISPLAYS.get(node);return shown==null?null:shown.at(ticks);}
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
    private static long now(){var level=Minecraft.getInstance().level;return level==null?0:level.getGameTime();}
}
