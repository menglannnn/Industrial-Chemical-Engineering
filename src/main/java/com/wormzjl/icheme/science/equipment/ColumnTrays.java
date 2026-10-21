package com.wormzjl.icheme.science.equipment;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The tray layout a distillation column's structure is built from (owner request 2026-10-16, batch
 * 2026-10-15-custom-equipment-rendering): the interior trays at the column solver's 0.6 m spacing, numbered from the top
 * as in the column calculator (tray 1 under the condenser, stage {@code trays + 1} the sump), the feed tray, side draws,
 * steam stages and pumparounds. A design imported from a column calculator keeps its numbers.
 */
public record ColumnTrays(int trays, int feed, List<Integer> draws, List<Integer> steam, List<Pumparound> pumparounds) {
    /** Tray spacing (the solver's {@code V3TrayHydraulics} spacing) and the disengagement space above the top tray, m. */
    public static final double SPACING = .6, TOP_SPACE = 1.2;
    public static final int MAX_TRAYS = 64, MAX_DRAWS = 4, MAX_STEAM = 2, MAX_PUMPAROUNDS = 4;

    /** A pumparound draws liquid from {@code draw} and returns it, cooled, to {@code ret} above it. */
    public record Pumparound(int draw, int ret) {
    }

    public ColumnTrays {
        draws = List.copyOf(draws);
        steam = List.copyOf(steam);
        pumparounds = List.copyOf(pumparounds);
        if (trays < 1 || trays > MAX_TRAYS) throw new IllegalArgumentException("A column has 1 to " + MAX_TRAYS + " trays, not " + trays);
        if (feed < 1 || feed > trays + 1) throw new IllegalArgumentException("The feed enters on tray 1 to " + (trays + 1) + ", not " + feed);
        if (draws.size() > MAX_DRAWS || steam.size() > MAX_STEAM || pumparounds.size() > MAX_PUMPAROUNDS)
            throw new IllegalArgumentException("Too many side draws, steam feeds or pumparounds");
        for (int draw : draws) if (draw < 1 || draw > trays) throw new IllegalArgumentException("Side draw outside the trays: " + draw);
        for (int stage : steam) if (stage < 1 || stage > trays + 1) throw new IllegalArgumentException("Steam stage outside the column: " + stage);
        for (var p : pumparounds)
            if (p.ret < 1 || p.ret > p.draw || p.draw > trays) throw new IllegalArgumentException("Pumparound must return at or above its draw tray: " + p);
    }

    /** A plain column: trays and the feed, nothing else. */
    public static ColumnTrays simple(int trays, int feed) {
        return new ColumnTrays(trays, feed, List.of(), List.of(), List.of());
    }

    /** Space under the bottom tray for the sump and the reboiler return, m. */
    public static double bottomSpace(double diameter) {
        return Math.max(2, diameter / 2);
    }

    /** Tangent-to-tangent length, whole metres: the trays, the space above them and the sump. */
    public int tangentLength(double diameter) {
        return (int) Math.ceil(TOP_SPACE + (trays - 1) * SPACING + bottomSpace(diameter) - 1e-9);
    }

    /** Height of tray {@code k} (1 at the top) above the bottom tangent line, m; stage {@code trays + 1} is the sump. */
    public double trayHeight(double diameter, int k) {
        return Objects.checkIndex(k - 1, trays + 1) == trays ? bottomSpace(diameter) / 2 : bottomSpace(diameter) + (trays - k) * SPACING;
    }

    /**
     * The trays of a column calculator's input: its interior trays (at least one), feed stage, side draws, steam feeds
     * and pumparounds, by the calculator's own numbers.
     */
    public static ColumnTrays of(com.wormzjl.icheme.science.column.v3.V3ColumnInput input) {
        var draws = new ArrayList<Integer>();
        for (var draw : input.sideDraws()) draws.add(draw.trayNumber());
        var steam = new ArrayList<Integer>();
        for (var feed : input.steamFeeds()) steam.add(feed.stageNumber());
        var pumparounds = new ArrayList<Pumparound>();
        for (var p : input.pumparounds()) pumparounds.add(new Pumparound(p.drawTray(), p.returnTray()));
        return new ColumnTrays(Math.max(1, input.stageCount()), input.feedStageNumber(), List.of(), List.of(), List.of())
                .refit(Math.max(1, input.stageCount()), draws, steam, pumparounds);
    }

    /** The same layout over {@code trays} trays: the feed kept within the column, draws, steam and pumparounds that no longer fit dropped. */
    public ColumnTrays withTrays(int trays) {
        return refit(trays, draws, steam, pumparounds);
    }

    /** The same layout with the feed on {@code feed}. */
    public ColumnTrays withFeed(int feed) {
        return new ColumnTrays(trays, feed, draws, steam, pumparounds);
    }

    private ColumnTrays refit(int count, List<Integer> draws, List<Integer> steam, List<Pumparound> pumparounds) {
        int n = Math.max(1, Math.min(MAX_TRAYS, count));
        return new ColumnTrays(n, Math.max(1, Math.min(n + 1, feed)), draws.stream().filter(d -> d <= n).limit(MAX_DRAWS).toList(),
                steam.stream().filter(s -> s <= n + 1).limit(MAX_STEAM).toList(),
                pumparounds.stream().filter(p -> p.draw <= n && p.ret >= 1 && p.ret <= p.draw).limit(MAX_PUMPAROUNDS).toList());
    }

    /** Trays, feed, then each list with its count first: draws, steam stages, pumparound (draw, return) pairs. */
    public int[] encode() {
        var out = new ArrayList<Integer>(List.of(trays, feed, draws.size()));
        out.addAll(draws);
        out.add(steam.size());
        out.addAll(steam);
        out.add(pumparounds.size());
        for (var p : pumparounds) {
            out.add(p.draw());
            out.add(p.ret());
        }
        return out.stream().mapToInt(Integer::intValue).toArray();
    }

    /** The longest {@link #encode()} array. */
    public static final int MAX_ENCODED = 5 + MAX_DRAWS + MAX_STEAM + 2 * MAX_PUMPAROUNDS;

    /** @throws IllegalArgumentException for a truncated, overlong or invalid layout */
    public static ColumnTrays decode(int[] data) {
        try {
            int i = 0, trays = data[i++], feed = data[i++];
            var draws = new ArrayList<Integer>();
            for (int n = data[i++]; n > 0; n--) draws.add(data[i++]);
            var steam = new ArrayList<Integer>();
            for (int n = data[i++]; n > 0; n--) steam.add(data[i++]);
            var pumparounds = new ArrayList<Pumparound>();
            for (int n = data[i++]; n > 0; n--) pumparounds.add(new Pumparound(data[i++], data[i++]));
            if (i != data.length) throw new IllegalArgumentException("Trailing tray data");
            return new ColumnTrays(trays, feed, draws, steam, pumparounds);
        } catch (ArrayIndexOutOfBoundsException truncated) {
            throw new IllegalArgumentException("Truncated tray data");
        }
    }
}
