package com.wormzjl.icheme.runtime.assembly;

import com.wormzjl.icheme.runtime.PresentationMailbox;
import com.wormzjl.icheme.science.assembly.AssemblyReport;
import com.wormzjl.icheme.science.assembly.AssemblyReport.SlotReport;
import com.wormzjl.icheme.science.assembly.AssemblyReport.State;
import com.wormzjl.icheme.science.assembly.AssemblyView;
import com.wormzjl.icheme.science.assembly.ColumnBlueprint;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The laying tool's watch of the column assemblies around a player (batch 2026-10-26-multi-machine-framework, plan
 * section 9, W7; AGENTS.md "Engine schedule and player-facing updates"): taking the tool up replays the stored views in
 * range at once; changes, arrivals and departures come with the buckets; putting the tool away, or no longer holding it,
 * ends the watch; nothing is sent to a player who does not watch.
 */
class AssemblyToolWatchTest {
    /** The views in range of each player, who holds the tool, and what was sent: (player, master, view or null). */
    private static final class Host implements AssemblyWatch.Host<String> {
        final Map<String, Map<Long, AssemblyView>> inRange = new HashMap<>();
        final Set<String> holding = new HashSet<>();
        final List<Sent> sent = new ArrayList<>();

        @Override
        public boolean holding(String player) {
            return holding.contains(player);
        }

        @Override
        public Map<Long, AssemblyView> inRange(String player) {
            return new TreeMap<>(inRange.getOrDefault(player, Map.of()));
        }

        @Override
        public void send(String player, long master, @Nullable AssemblyView view) {
            sent.add(new Sent(player, master, view));
        }
    }

    private record Sent(String player, long master, @Nullable AssemblyView view) {
    }

    private static final long COLUMN = 10, OTHER = 20, BUCKET = PresentationMailbox.PERIOD;

    /** A checked view whose detail tells reports apart. */
    private static AssemblyView checked(int n) {
        var report = new AssemblyReport(List.of(new SlotReport(ColumnBlueprint.COLUMN, State.OK, COLUMN, AssemblyReport.NONE, ""),
                new SlotReport(ColumnBlueprint.CONDENSER, State.MISSING, AssemblyReport.NONE, AssemblyReport.NONE, "run " + n)), List.of(), Set.of(), List.of(COLUMN));
        return new AssemblyView(AssemblyView.Status.CHECKED, report, AssemblyReport.NONE);
    }

    private static Host watching(String player, Map<Long, AssemblyView> views) {
        var host = new Host();
        host.holding.add(player);
        host.inRange.put(player, new HashMap<>(views));
        return host;
    }

    @Test
    void takingTheToolUpReplaysEveryStoredViewInRangeAtOnce() {
        var host = watching("a", Map.of(COLUMN, checked(1), OTHER, AssemblyView.NOT_VALIDATED));
        var watch = new AssemblyWatch<>(host);
        watch.start("a", 5);
        assertEquals(List.of(new Sent("a", COLUMN, checked(1)), new Sent("a", OTHER, AssemblyView.NOT_VALIDATED)), host.sent);
        assertEquals(1, watch.watchers());
        // Nothing changed: the bucket sends nothing again.
        watch.tick(BUCKET);
        assertEquals(2, host.sent.size());
    }

    @Test
    void changesArrivalsAndDeparturesComeWithTheBucket() {
        var host = watching("a", Map.of(COLUMN, checked(1), OTHER, AssemblyView.NOT_VALIDATED));
        var watch = new AssemblyWatch<>(host);
        watch.start("a", 5);
        host.sent.clear();
        // A validation changed the column's view, the other column left the range, a third came into it.
        host.inRange.get("a").put(COLUMN, checked(2));
        host.inRange.get("a").remove(OTHER);
        host.inRange.get("a").put(30L, checked(3));
        watch.tick(BUCKET - 1);
        assertEquals(List.of(), host.sent, "nothing before the bucket");
        watch.tick(BUCKET);
        assertEquals(List.of(new Sent("a", COLUMN, checked(2)), new Sent("a", 30L, checked(3)), new Sent("a", OTHER, null)), host.sent);
    }

    @Test
    void puttingTheToolAwayOrNoLongerHoldingItEndsTheWatch() {
        var host = watching("a", Map.of(COLUMN, checked(1)));
        host.holding.add("b");
        host.inRange.put("b", new HashMap<>(Map.of(COLUMN, checked(1))));
        var watch = new AssemblyWatch<>(host);
        watch.start("a", 5);
        watch.start("b", 5);
        watch.stop("a");
        // b put the tool down without a word: the next bucket drops b.
        host.holding.remove("b");
        host.sent.clear();
        host.inRange.get("a").put(COLUMN, checked(2));
        host.inRange.get("b").put(COLUMN, checked(2));
        watch.tick(BUCKET);
        assertEquals(List.of(), host.sent, "nothing to a player who does not watch");
        assertEquals(0, watch.watchers());
        watch.tick(2 * BUCKET);
        assertEquals(List.of(), host.sent);
    }

    @Test
    void takingTheToolUpAgainReplaysTheWholeRange() {
        var host = watching("a", Map.of(COLUMN, checked(1)));
        var watch = new AssemblyWatch<>(host);
        watch.start("a", 5);
        watch.stop("a");
        watch.start("a", 50);
        assertEquals(List.of(new Sent("a", COLUMN, checked(1)), new Sent("a", COLUMN, checked(1))), host.sent);
    }
}
