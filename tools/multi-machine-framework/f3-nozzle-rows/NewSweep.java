package com.wormzjl.icheme.science.equipment;

import com.wormzjl.icheme.science.math.DeterministicMath;
import java.util.*;

/**
 * The new layout (finding F3) over RowSweep's 126 designs: refused designs, nozzles off their stage band, turned
 * nozzles, ladders and davits moved, and any ladder cage, deck or mast still in a nozzle's room.
 */
public final class NewSweep {
    public static void main(String[] args) {
        int designs = 0, refused = 0, off = 0, turned = 0, nozzles = 0, laddersMoved = 0, ladders = 0, davitMoved = 0, cageClash = 0, deckClash = 0, mastClash = 0, emptyTop = 0;
        var turnedByRole = new TreeMap<String, Integer>();
        var refusedWhy = new TreeMap<String, Integer>();
        var examples = new ArrayList<String>();
        var clashByD = new TreeMap<Double, int[]>();
        for (double d : new double[]{.5, 1, 1.5, 2, 3, 4, 6, 8, 15})
            for (int n : new int[]{1, 3, 5, 10, 15, 20, 30, 40, 64})
                for (String config : new String[]{"kettle", "kettle+steam+draw+PA", "steam+draw", "full"}) {
                    ColumnTrays t;
                    try {
                        t = trays(n, config);
                    } catch (IllegalArgumentException invalid) {
                        continue;
                    }
                    var s = EquipmentShape.column(d, t, EquipmentShape.Heading.NORTH, EquipmentShape.minimumSkirt(d));
                    designs++;
                    var misfit = ColumnDesign.misfit(s);
                    if (misfit.isPresent()) {
                        refused++;
                        refusedWhy.merge(misfit.get().replaceAll("[0-9]+-tray", "N-tray"), 1, Integer::sum);
                        if (d <= 1.5) SLIM.merge("d " + d + ": " + misfit.get().replaceAll(" has no free row.*", ""), 1, Integer::sum);
                        continue;
                    }
                    double yB = s.bottomTangent();
                    var homes = Map.of(EquipmentShape.Role.REFLUX, 3, EquipmentShape.Role.FEED, 0, EquipmentShape.Role.PUMPAROUND_DRAW, 3, EquipmentShape.Role.RETURN, 3,
                            EquipmentShape.Role.VAPOUR_RETURN, 1, EquipmentShape.Role.DRAW, 1, EquipmentShape.Role.STEAM, 1, EquipmentShape.Role.BOTTOMS, 0);
                    var wants = RowSweep.wants(t, d, yB, ColumnTrays.bottomSpace(d));
                    var used = new HashMap<EquipmentShape.Role, Integer>();
                    var line = new StringBuilder();
                    for (var nozzle : s.nozzles()) {
                        if (nozzle.axis() == 1) continue;
                        var list = wants.get(nozzle.role());
                        if (list == null) continue;
                        nozzles++;
                        var want = list.get(used.merge(nozzle.role(), 1, Integer::sum) - 1);
                        int side = side(nozzle.axis(), nozzle.sign());
                        if (side != homes.get(nozzle.role())) {
                            turned++;
                            turnedByRole.merge(nozzle.role().name(), 1, Integer::sum);
                            line.append(' ').append(nozzle.role()).append("->").append(new String[]{"front", "left", "back", "right"}[side]);
                        }
                        // Off: more than a row from the wanted height.
                        if (Math.abs(nozzle.base()[1] - (Math.floor(want.y()) + .5)) > 1.01) {
                            off++;
                            line.append(String.format(" OFF %s %.1f->%.1f", nozzle.role(), want.y(), nozzle.base()[1]));
                            OFFS.add(String.format("d %.1f n %d %s: %s wanted %.2f at %.2f, side %d", d, n, config, nozzle.role(), want.y(), nozzle.base()[1], side));
                        }
                    }
                    var plan = ColumnStructure.plan(s);
                    for (int k = 0; k < plan.ladders().size(); k++) {
                        ladders++;
                        double preferred = k % 2 == 0 ? ColumnStructure.LADDER_A : ColumnStructure.LADDER_B;
                        if (Math.abs(plan.ladders().get(k).angle() - preferred) > 1e-9) laddersMoved++;
                    }
                    int levels = plan.ladders().size();
                    if (Math.abs(plan.davitAngle() - (levels % 2 == 1 ? Math.PI / 6 : 5 * Math.PI / 6)) > 1e-9) davitMoved++;
                    int top = plan.platforms().getLast().level();
                    boolean topPieces = plan.platforms().stream().anyMatch(p -> p.level() == top);
                    if (!topPieces) emptyTop++;
                    int[] clash = clashes(s, plan);
                    var layout = ShapeRasterizer.layout(s);var occupied = new HashSet<ShapeRasterizer.Cell>(layout.shell());occupied.addAll(layout.interior());
                    for (var port : ShapeRasterizer.ports(s)) { var pipe = new ShapeRasterizer.Cell(port.cell().x() + port.dx(), port.cell().y() + port.dy(), port.cell().z() + port.dz()); if (occupied.contains(pipe)) { PIPES[0]++; if (PIPE_CASES.size() < 6) PIPE_CASES.add(String.format("d %.1f n %d %s: %s at %s", d, n, config, s.nozzles().get(port.nozzle()).role(), pipe)); } }
                    String walk = broken(s, plan);
                    if (walk != null) { BROKEN[0]++; if (WALKS.size() < 12) WALKS.add(String.format("d %.1f n %d %s: %s", d, n, config, walk)); }
                    cageClash += clash[0];
                    deckClash += clash[1];
                    mastClash += clash[2];
                    var byD = clashByD.computeIfAbsent(d, k -> new int[4]);byD[0] += clash[0];byD[1] += clash[1];byD[2] += clash[2];byD[3]++;
                    if ((clash[0] + clash[1] + clash[2] > 0 || line.indexOf("OFF") >= 0) && examples.size() < 14)
                        examples.add(String.format("d %.1f n %d %s:%s clash %s", d, n, config, line, Arrays.toString(clash)));
                }
        System.out.printf("designs %d, refused %d, side nozzles %d, off their band %d, turned %d %s%n", designs, refused, nozzles, off, turned, turnedByRole);
        System.out.printf("ladders %d, moved %d; davits moved %d; top platform gone %d; clashes: cage %d, deck %d, mast %d%n", ladders, laddersMoved, davitMoved, emptyTop,
                cageClash, deckClash, mastClash);
        System.out.println("cage clashes into a pipe block cell: " + BLOCK[0]);
        System.out.println("designs with a deck walk cut between a ladder and the next (or the top manway): " + BROKEN[0]);
        WALKS.forEach(w -> System.out.println("  walk: " + w));
        System.out.println("pipe cells taken by the structure (rasterized): " + PIPES[0]);
        PIPE_CASES.forEach(w -> System.out.println("  pipe: " + w));
        OFFS.forEach(o -> System.out.println("  off: " + o));
        clashByD.forEach((k, v) -> System.out.println("  d " + k + ": designs " + v[3] + " cage " + v[0] + " deck " + v[1] + " mast " + v[2]));
        refusedWhy.forEach((k, v) -> System.out.println("  refused " + v + ": " + k));
        examples.forEach(System.out::println);
        SLIM.forEach((k, v) -> System.out.println("  slim refused " + v + ": " + k));
        System.out.printf("clearance d 0.5 %.0f, d 1 %.0f, d 1.5 %.0f, d 2 %.0f deg%n", Math.toDegrees(ColumnStructure.clearance(EquipmentShape.column(.5, ColumnTrays.simple(10, 5), EquipmentShape.Heading.NORTH, 3))), Math.toDegrees(ColumnStructure.clearance(EquipmentShape.column(1, ColumnTrays.simple(10, 5), EquipmentShape.Heading.NORTH, 3))), Math.toDegrees(ColumnStructure.clearance(EquipmentShape.column(1.5, ColumnTrays.simple(10, 5), EquipmentShape.Heading.NORTH, 3))), Math.toDegrees(ColumnStructure.clearance(EquipmentShape.column(2, ColumnTrays.simple(10, 5), EquipmentShape.Heading.NORTH, 3))));
    }

    static int side(int axis, int sign) {
        return axis == 2 ? (sign < 0 ? 0 : 2) : (sign > 0 ? 1 : 3);
    }

    static ColumnTrays trays(int n, String config) {
        if (config.equals("full")) {
            var c = new ColumnComponents(ColumnComponents.Condenser.AIR_COOLER, ColumnComponents.Pump.STANDARD,
                    List.of(ColumnComponents.Pump.STANDARD, ColumnComponents.Pump.STANDARD), false, true);
            return new ColumnTrays(n, n / 2 + 1, List.of(Math.max(1, n / 4), Math.max(1, n / 2), Math.max(1, 3 * n / 4)), List.of(n + 1),
                    List.of(new ColumnTrays.Pumparound(Math.max(1, n / 3), Math.max(1, n / 3 - 2)), new ColumnTrays.Pumparound(Math.max(1, 2 * n / 3), Math.max(1, 2 * n / 3 - 2))), c);
        }
        return RowSweep.trays(n, config);
    }

    /** Cages, decks and the mast in a nozzle's room: counted where they overlap it over its row. */
    static int[] clashes(EquipmentShape s, ColumnStructure.Plan plan) {
        double outer = s.outerRadius(), face = Math.ceil(s.axis() + outer + EquipmentShape.STAND_OUT) - s.axis(), reach = .55;
        int[] out = new int[3];
        for (var nozzle : s.nozzles()) {
            if (nozzle.axis() == 1) continue;
            double row = Math.floor(nozzle.base()[1]), h = nozzle.axis() == 0 ? (nozzle.sign() > 0 ? 0 : Math.PI) : (nozzle.sign() > 0 ? Math.PI / 2 : 3 * Math.PI / 2);
            double neck = Math.min(reach, PipeSizes.flange(nozzle.defaultOd()) / 2 + .05);
            var neckPart = new double[][]{p(outer, h, -neck), p(face, h, -neck), p(face, h, neck), p(outer, h, neck)};
            var blockPart = new double[][]{p(face, h, -reach), p(face + 1, h, -reach), p(face + 1, h, reach), p(face, h, reach)};
            for (var l : plan.ladders()) {
                if (!(row < l.top() && row + 1 > l.base())) continue;
                double r = outer + Access.STANDOFF;
                var cage = new double[][]{p(outer, l.angle(), -Access.CAGE_HALF), p(r + 2 * Access.CAGE_HALF, l.angle(), -Access.CAGE_HALF),
                        p(r + 2 * Access.CAGE_HALF, l.angle(), Access.CAGE_HALF), p(outer, l.angle(), Access.CAGE_HALF)};
                if (overlap(cage, neckPart) || overlap(cage, blockPart)) {
                    out[0]++;
                    var block = new double[][]{p(face, h, -.5), p(face + 1, h, -.5), p(face + 1, h, .5), p(face, h, .5)};
                    if (overlap(cage, block)) BLOCK[0]++;
                }
            }
            for (var pl : plan.platforms()) {
                if (!(row < pl.level() + Access.DECK + Access.RAIL + Access.BAR && row + 1 > pl.level() - .65)) continue;
                int pieces = Math.max(1, (int) Math.ceil((pl.to() - pl.from()) / (Math.PI / 12)));
                for (int k = 0; k < pieces; k++) {
                    double a0 = pl.from() + (pl.to() - pl.from()) * k / pieces, a1 = pl.from() + (pl.to() - pl.from()) * (k + 1) / pieces;
                    double edge = plan.deckRadius();
                    double in = outer + Access.GAP;
                    var piece = new double[][]{p(in, a0, 0), p(edge, a0, 0), p(edge, (a0 + a1) / 2, 0), p(edge, a1, 0), p(in, a1, 0)};
                    if (overlap(neckPart, piece) || overlap(blockPart, piece)) {
                        out[1]++;
                        break;
                    }
                }
            }
            int topLevel = plan.platforms().getLast().level();
            if (row + 1 > topLevel && row < plan.davitTop()) {
                var c = p(outer + .35, plan.davitAngle(), 0);
                var sq = new double[][]{{c[0] - .1, c[1] - .1}, {c[0] + .1, c[1] - .1}, {c[0] + .1, c[1] + .1}, {c[0] - .1, c[1] + .1}};
                if (overlap(sq, neckPart) || overlap(sq, blockPart)) out[2]++;
            }
        }
        return out;
    }

    static final int[] BLOCK = new int[1], BROKEN = new int[1], PIPES = new int[1];
    static final List<String> PIPE_CASES = new ArrayList<>();
    static final List<String> WALKS = new ArrayList<>();
    static final Map<String, Integer> SLIM = new TreeMap<>();

    /** The first level whose deck does not carry a player from the arriving ladder to the next ladder (the top: to the manway), or null. */
    static String broken(EquipmentShape s, ColumnStructure.Plan plan) {
        double clear = ColumnStructure.clearance(s);
        var ladders = plan.ladders();
        int levels = ladders.size();
        var byLevel = new java.util.TreeMap<Integer, List<ColumnStructure.Platform>>();
        for (var p : plan.platforms()) byLevel.computeIfAbsent(p.level(), k -> new ArrayList<>()).add(p);
        var list = new ArrayList<>(byLevel.keySet());
        if (list.size() != levels) return "levels " + list + " ladders " + levels;
        for (int k = 0; k < levels; k++) {
            double in = ladders.get(k).angle();
            var pieces = byLevel.get(list.get(k));
            boolean ok = false;
            if (k + 1 < levels) {
                double out = ladders.get(k + 1).angle(), a = in > out ? in - clear : in + clear;
                for (var p : pieces) ok |= p.from() <= Math.min(a, out) + 1e-9 && p.to() >= Math.max(a, out) - 1e-9;
            } else for (var p : pieces) ok |= p.manway() && (Math.abs(p.to() - (in + 2 * Math.PI - clear)) < 1e-9 || Math.abs(p.from() - (in + clear)) < 1e-9);
            if (!ok) return "level " + list.get(k) + " pieces " + pieces.stream().map(p -> String.format("%.0f-%.0f%s", Math.toDegrees(p.from()), Math.toDegrees(p.to()), p.manway() ? "M" : "")).toList() + String.format(" ladder %.0f", Math.toDegrees(in));
        }
        return null;
    }
    static final List<String> OFFS = new ArrayList<>();

    static double[] p(double r, double a, double t) {
        double c = DeterministicMath.cos(a), s = DeterministicMath.sin(a);
        return new double[]{r * c - t * s, r * s + t * c};
    }

    static boolean overlap(double[][] a, double[][] b) {
        return !separates(a, b) && !separates(b, a);
    }

    static boolean separates(double[][] edges, double[][] other) {
        for (int i = 0; i < edges.length; i++) {
            double[] p = edges[i], q = edges[(i + 1) % edges.length];
            double nx = q[1] - p[1], nz = p[0] - q[0];
            double minA = Double.MAX_VALUE, maxA = -Double.MAX_VALUE, minB = Double.MAX_VALUE, maxB = -Double.MAX_VALUE;
            for (var v : edges) {
                double dd = v[0] * nx + v[1] * nz;
                minA = Math.min(minA, dd);
                maxA = Math.max(maxA, dd);
            }
            for (var v : other) {
                double dd = v[0] * nx + v[1] * nz;
                minB = Math.min(minB, dd);
                maxB = Math.max(maxB, dd);
            }
            if (maxA <= minB + 1e-9 || maxB <= minA + 1e-9) return true;
        }
        return false;
    }
}
