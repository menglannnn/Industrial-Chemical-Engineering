package com.wormzjl.icheme.runtime.fluid;

import java.util.*;

/**
 * Which players watch which open tanks (batch 2026-10-21-floating-roof-tanks, plan section 3.4, owner decision FR4, the
 * request path R-B of the engine-owned presentation rule): a client asks for each tank whose node cell it has loaded within
 * {@link #RANGE}; the server replays the tank's last published display at once and sends it again at each of its island's
 * presentation buckets ({@link FluidPresentation}) until the client unwatches it, leaves its range or disconnects. Nothing
 * is sent to a player who did not ask. At most {@link #MAXIMUM_PER_PLAYER} tanks per player.
 */
public final class TankWatchers {
    /** The farthest a watched tank's node cell may stand from its watcher, blocks. */
    public static final double RANGE=128;
    public static final int MAXIMUM_PER_PLAYER=64;
    private final Map<Long,Set<UUID>> byDevice=new HashMap<>();
    private final Map<UUID,Set<Long>> byPlayer=new HashMap<>();

    /** Starts {@code player} watching {@code device}; false when it already does or watches its maximum. */
    public boolean watch(UUID player,long device) {
        var devices=byPlayer.computeIfAbsent(player,k->new HashSet<>());
        if(devices.contains(device)||devices.size()>=MAXIMUM_PER_PLAYER)return false;
        devices.add(device);byDevice.computeIfAbsent(device,k->new HashSet<>()).add(player);
        return true;
    }
    public void unwatch(UUID player,long device) {
        var devices=byPlayer.get(player);if(devices!=null&&devices.remove(device)&&devices.isEmpty())byPlayer.remove(player);
        var players=byDevice.get(device);if(players!=null&&players.remove(player)&&players.isEmpty())byDevice.remove(device);
    }
    /** Ends every subscription of {@code player} (it disconnected). */
    public void forget(UUID player) {
        var devices=byPlayer.remove(player);if(devices==null)return;
        for(long device:devices){var players=byDevice.get(device);if(players!=null&&players.remove(player)&&players.isEmpty())byDevice.remove(device);}
    }
    /** Ends every subscription to {@code device} (it is gone). */
    public void remove(long device) {
        var players=byDevice.remove(device);if(players==null)return;
        for(var player:players){var devices=byPlayer.get(player);if(devices!=null&&devices.remove(device)&&devices.isEmpty())byPlayer.remove(player);}
    }
    /** The players watching {@code device}, a copy (empty for none). */
    public Set<UUID> watchers(long device){var players=byDevice.get(device);return players==null?Set.of():Set.copyOf(players);}
    /** The devices {@code player} watches, a copy (empty for none). */
    public Set<Long> devices(UUID player){var devices=byPlayer.get(player);return devices==null?Set.of():Set.copyOf(devices);}
    public int watched(UUID player){var devices=byPlayer.get(player);return devices==null?0:devices.size();}
}
