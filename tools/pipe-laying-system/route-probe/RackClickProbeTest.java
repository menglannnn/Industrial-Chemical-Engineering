package com.wormzjl.icheme.science.equipment;

import com.wormzjl.icheme.science.equipment.PipeRoute.Cell;
import com.wormzjl.icheme.science.equipment.PipeRoute.End;
import com.wormzjl.icheme.science.equipment.PipeRoute.Room;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

/**
 * Off-tree probe (batch 2026-10-26-pipe-laying-system, RACK_FLANGES_AIM_PLAN sections 5 and 6), not a gate: the client's
 * route search against the server's check and blocked, for every aimed pair of clicks on a 10-cell rack run (2", 6",
 * 14" lines). Prints per outcome the count (TALLY), the routes that pass a cell twice by click layout (KINDS), the total
 * and worst search time (TIME, WORST, microseconds) and the searches over 2 ms by layout (SLOW). Run from
 * {@code src/test/java/com/wormzjl/icheme/science/equipment/} as the folder README says.
 */
class RackClickProbeTest {
    private static final int WEST = 4, EAST = 5;

    @Test
    void probe() {
        var rooms = new HashMap<Cell, Room>();
        for (int x = 1; x <= 10; x++) rooms.put(new Cell(x, 0, 0), Room.rack(new RackGeometry.Rack(EAST, RackGeometry.STRAIGHT), List.of(), 0));
        var space = PipeRoute.Space.of(cell -> rooms.getOrDefault(cell, Room.FREE), rooms.keySet());
        var tally = new TreeMap<String, Integer>();
        var kinds = new TreeMap<String, Integer>();
        var slow = new TreeMap<String, Integer>();
        var time = new TreeMap<String, Long>();
        var worst = new TreeMap<String, Long>();
        for (double outside : new double[]{.0625, .171875, .359375})
            for (int a = 1; a <= 10; a++)
                for (int b = 1; b <= 10; b++)
                    for (int sp = 0; sp < 4; sp++)
                        for (int ep = 0; ep < 4; ep++)
                            for (int st : new int[]{WEST, EAST})
                                for (int et : new int[]{WEST, EAST}) {
                                    int width = RackGeometry.width(PipeSizes.outsideOf(PipeSizes.code(outside)));
                                    if (sp + width > 4 || ep + width > 4) continue;
                                    var start = new End(new Cell(a, 0, 0), -1, -1, RackGeometry.option(sp, RackGeometry.TIER), st, st ^ 1);
                                    var end = new End(new Cell(b, 0, 0), -1, -1, RackGeometry.option(ep, RackGeometry.TIER), et, et ^ 1);
                                    int heading = start.open() ^ 1;
                                    long began = System.nanoTime();
                                    var route = PipeRoute.search(start, heading, end, outside, space, PipeRoute.Limits.DEFAULT);
                                    long micros = (System.nanoTime() - began) / 1000;
                                    String key;
                                    if (!route.found()) key = "client refuses " + route.refusal();
                                    else {
                                        var check = PipeRoute.check(route.cells(), route.options(), start, end, 128);
                                        int blocked = check != null ? -2 : PipeRoute.blocked(route.cells(), route.options(), start, heading, end, outside, space);
                                        var seen = new HashSet<Cell>();
                                        Cell twice = null;
                                        for (var c : route.cells()) if (!seen.add(c) && twice == null) twice = c;
                                        key = check != null ? "SERVER check " + check + (twice != null ? " (cell twice)" : "") : blocked >= 0 ? "SERVER blocked" : "laid";
                                        if (twice != null) {
                                            String where = twice.equals(start.cell()) ? "start" : twice.equals(end.cell()) ? "end" : "other";
                                            kinds.merge(layout(a, b, st, et) + " gap" + Math.min(Math.abs(b - a), 3) + " " + where, 1, Integer::sum);
                                        }
                                    }
                                    tally.merge(key, 1, Integer::sum);
                                    time.merge(key, micros, Long::sum);
                                    worst.merge(key, micros, Math::max);
                                    if (micros > 2000)
                                        slow.merge(key + " | " + layout(a, b, st, et) + (sp == ep ? " same-pos" : " other-pos") + " gap" + Math.min(Math.abs(b - a), 3), 1, Integer::sum);
                                }
        System.out.println("TALLY " + tally);
        System.out.println("KINDS " + kinds);
        System.out.println("TIME us " + time);
        System.out.println("WORST us " + worst);
        slow.forEach((k, v) -> System.out.println("SLOW " + k + " = " + v));
    }

    /** Where each click aims relative to the other cell: the start towards or away from the end, the end facing the start or its back to it. */
    private static String layout(int a, int b, int st, int et) {
        int d = Integer.signum(b - a);
        if (d == 0) return "same";
        return (st == (d > 0 ? EAST : WEST) ? "s-toward" : "s-away") + "/" + (et == (d > 0 ? WEST : EAST) ? "e-facing" : "e-back");
    }
}
