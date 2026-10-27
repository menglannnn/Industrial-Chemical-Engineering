package com.wormzjl.icheme.runtime.assembly;

import com.wormzjl.icheme.runtime.PresentationMailbox;
import com.wormzjl.icheme.science.assembly.AssemblyView;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * The laying tool's watch of the column assemblies around a player (batch 2026-10-26-multi-machine-framework, plan
 * section 9, W7; AGENTS.md "Engine schedule and player-facing updates"). Holding the tool is the request: the client says
 * when the player takes it up and puts it away. The client judges it: the selected slot is the client's, and the server
 * learns of a number key a tick after the request (the carried item is sent before the keys are read), so a server check
 * of the hand would refuse the request it answers. Taking the tool up replays the stored view of every column in range at
 * once, a cached read that walks nothing. The watch then gets, on the engine's presentation buckets, each view that
 * changed, each column that came into range and the end of each that left it or is gone. A watcher who has left is
 * dropped at the next bucket. Nothing is sent to a player who does not watch.
 *
 * @param <P> a watcher: a server player, any value in tests
 */
public final class AssemblyWatch<P> {
    /** The tool's range, blocks: a column is in range when one of its blocks, nozzles or units is this near. */
    public static final double RANGE = 48;

    /** The world a watch reads and the watchers it answers. */
    public interface Host<P> {
        /** Whether the watcher is still in the world: not disconnected, not removed. */
        boolean present(P player);

        /** The columns in range of the watcher, by master, with their stored views: cached reads that walk nothing. */
        Map<Long, AssemblyView> inRange(P player);

        /** Sends the view of the column at {@code master}, or its end ({@code null}): out of range or gone. */
        void send(P player, long master, @Nullable AssemblyView view);
    }

    private final Host<P> host;
    private final PresentationMailbox<P> mailbox = new PresentationMailbox<>();
    private final Map<P, Map<Long, AssemblyView>> sent = new HashMap<>();

    public AssemblyWatch(Host<P> host) {
        this.host = host;
    }

    /** The player took the tool up: the watch starts with every view in range, sent now. */
    public void start(P player, long now) {
        mailbox.subscribe(player, now);
        sent.remove(player);
        publish(player);
    }

    /** The player put the tool away: the watch ends. */
    public void stop(P player) {
        mailbox.remove(player);
        sent.remove(player);
    }

    /** The engine's tick: on a bucket, each watcher still present gets what changed. */
    public void tick(long now) {
        if (now < mailbox.nextTick()) return;
        mailbox.tick(now, player -> {
            if (host.present(player)) return true;
            sent.remove(player);
            return false;
        }, this::publish);
    }

    private void publish(P player) {
        var was = sent.getOrDefault(player, Map.of());
        var now = host.inRange(player);
        for (var view : now.entrySet()) if (!view.getValue().equals(was.get(view.getKey()))) host.send(player, view.getKey(), view.getValue());
        for (long master : was.keySet()) if (!now.containsKey(master)) host.send(player, master, null);
        sent.put(player, Map.copyOf(now));
    }

    /** Watchers. */
    public int watchers() {
        return mailbox.size();
    }
}
