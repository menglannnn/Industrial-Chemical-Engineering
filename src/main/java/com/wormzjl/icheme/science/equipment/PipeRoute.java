package com.wormzjl.icheme.science.equipment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;

/**
 * The cells a laid pipe takes between its two ends (batch 2026-10-26-pipe-laying-system, plan section 4.3). The client
 * searches the route with a weighted A* and sends it; the server checks the cells it is sent ({@link #check}) and never
 * searches, so the two cannot disagree on the route.
 *
 * <p>Faces follow {@link LineGeometry#STEPS} (Minecraft's direction order): face {@code f}'s opposite is {@code f ^ 1}. A
 * route's heading in a cell is the face it travels towards there, so it entered through {@code heading ^ 1}. Each cell of a
 * route has a joint towards the cells before and after it; the first and last cells also have one towards what the line
 * starts or ends at, unless that end is open.
 */
public final class PipeRoute {
    /** A step through a free cell; a change of heading (an elbow); the extra for a vertical step. */
    public static final double STEP = 1, ELBOW = 1.5, VERTICAL = .3;
    /**
     * The heuristic per block of Manhattan distance, and its weight. 0.4 is a step along a rack (the cheapest step a
     * route may take once racks exist), so the unweighted heuristic never overestimates.
     */
    public static final double HEURISTIC = .4, WEIGHT = 2;

    public record Cell(int x, int y, int z) {
        public Cell step(int face) {
            var s = LineGeometry.STEPS[face];
            return new Cell(x + s[0], y + s[1], z + s[2]);
        }

        public int distance(Cell other) {
            return Math.abs(x - other.x) + Math.abs(y - other.y) + Math.abs(z - other.z);
        }

        /** The face of this cell beyond which {@code next} lies, or -1 when it is not a neighbour. */
        public int faceTowards(Cell next) {
            for (int face = 0; face < 6; face++) if (step(face).equals(next)) return face;
            return -1;
        }
    }

    /** What a cell offers a route. */
    public enum Admit {FREE, BLOCKED}

    @FunctionalInterface
    public interface Space {
        Admit admit(Cell cell);
    }

    /** An end of a line: its cell, and the face of that cell joined to what the line meets there (-1: an open end). */
    public record End(Cell cell, int joint) {
        public End {
            if (cell == null || joint < -1 || joint > 5) throw new IllegalArgumentException("Invalid route end");
        }
    }

    /** The search bounds: the cells of a route, the nodes expanded, and the blocks the search box extends past its ends. */
    public record Limits(int cells, int expansions, int margin) {
        public static final Limits DEFAULT = new Limits(128, 32768, 8);
    }

    /** Why a route cannot be laid; each has a player message. */
    public enum Refusal {START_OCCUPIED, END_OCCUPIED, SAME_FACE, BLOCKED, TOO_LONG, BROKEN, JOINTS}

    /** A route's cells, or why there is none. */
    public record Result(List<Cell> cells, Refusal refusal) {
        public Result {
            cells = List.copyOf(cells);
        }

        public boolean found() {
            return refusal == null;
        }

        static Result refused(Refusal refusal) {
            return new Result(List.of(), refusal);
        }
    }

    private PipeRoute() {
    }

    /**
     * The cheapest route from {@code start} to {@code end} by the costs above, entering the start cell along
     * {@code heading} (the face the player clicked: it steers the first leg). Deterministic: ties go to the lower f, then
     * the lower h, then the earlier push, and pushes follow the face order.
     */
    public static Result search(End start, int heading, End end, Space space, Limits limits) {
        if (heading < 0 || heading > 5 || start.joint >= 0 && start.joint != (heading ^ 1)) throw new IllegalArgumentException("Heading does not leave the start's joint");
        if (space.admit(start.cell) != Admit.FREE) return Result.refused(Refusal.START_OCCUPIED);
        if (space.admit(end.cell) != Admit.FREE) return Result.refused(Refusal.END_OCCUPIED);
        if (start.cell.equals(end.cell))
            return start.joint >= 0 && start.joint == end.joint ? Result.refused(Refusal.SAME_FACE) : new Result(List.of(start.cell), null);
        int lowX = Math.min(start.cell.x, end.cell.x) - limits.margin, highX = Math.max(start.cell.x, end.cell.x) + limits.margin;
        int lowY = Math.min(start.cell.y, end.cell.y) - limits.margin, highY = Math.max(start.cell.y, end.cell.y) + limits.margin;
        int lowZ = Math.min(start.cell.z, end.cell.z) - limits.margin, highZ = Math.max(start.cell.z, end.cell.z) + limits.margin;
        var open = new PriorityQueue<Node>();
        var best = new HashMap<State, Double>();
        var admitted = new HashMap<Cell, Admit>();
        long pushes = 0;
        var first = new Node(start.cell, heading, 0, h(start.cell, end.cell), 1, null, pushes++);
        open.add(first);
        best.put(new State(start.cell, heading), 0.0);
        int expansions = 0;
        boolean capped = false;
        while (!open.isEmpty()) {
            var node = open.poll();
            if (node.g > best.getOrDefault(new State(node.cell, node.heading), Double.MAX_VALUE)) continue;
            if (node.cell.equals(end.cell)) return new Result(path(node), null);
            if (++expansions > limits.expansions) {
                capped = true;
                break;
            }
            for (int face = 0; face < 6; face++) {
                if (face == (node.heading ^ 1)) continue;
                var next = node.cell.step(face);
                if (next.x < lowX || next.x > highX || next.y < lowY || next.y > highY || next.z < lowZ || next.z > highZ) continue;
                if (node.length + 1 > limits.cells) {
                    capped = true;
                    continue;
                }
                if (admitted.computeIfAbsent(next, space::admit) != Admit.FREE) continue;
                double g = node.g + STEP + (face != node.heading ? ELBOW : 0) + (face < 2 ? VERTICAL : 0);
                if (next.equals(end.cell) && end.joint >= 0) {
                    // The end cell joins what it meets through end.joint: entering through that face is impossible, any other but the far one is an elbow.
                    if (end.joint == (face ^ 1)) continue;
                    if (end.joint != face) g += ELBOW;
                }
                var state = new State(next, face);
                if (g >= best.getOrDefault(state, Double.MAX_VALUE)) continue;
                best.put(state, g);
                open.add(new Node(next, face, g, h(next, end.cell), node.length + 1, node, pushes++));
            }
        }
        return Result.refused(capped ? Refusal.TOO_LONG : Refusal.BLOCKED);
    }

    private static double h(Cell from, Cell to) {
        return HEURISTIC * from.distance(to);
    }

    private static List<Cell> path(Node node) {
        var cells = new ArrayList<Cell>();
        for (var at = node; at != null; at = at.parent) cells.add(at.cell);
        Collections.reverse(cells);
        return cells;
    }

    /**
     * Each cell's joints: towards the cells before and after it, and at the ends towards what the line meets. Null when
     * the cells do not form a route between these ends (see {@link #check}).
     */
    public static int[] joints(List<Cell> cells, End start, End end) {
        if (check(cells, start, end, Integer.MAX_VALUE) != null) return null;
        return masks(cells, start, end);
    }

    private static int[] masks(List<Cell> cells, End start, End end) {
        int n = cells.size();
        var joints = new int[n];
        for (int i = 0; i < n; i++) {
            if (i > 0) joints[i] |= 1 << cells.get(i).faceTowards(cells.get(i - 1));
            if (i + 1 < n) joints[i] |= 1 << cells.get(i).faceTowards(cells.get(i + 1));
        }
        return joints;
    }

    /**
     * The facing each cell is placed with: the face towards the next cell, the last cell keeping the heading it was
     * entered along ({@code heading} for a one-cell line). A straight cell faces along its run, as an axial fitting needs.
     */
    public static int[] facings(List<Cell> cells, int heading) {
        int n = cells.size();
        var facings = new int[n];
        for (int i = 0; i < n; i++)
            facings[i] = i + 1 < n ? cells.get(i).faceTowards(cells.get(i + 1)) : n > 1 ? cells.get(i - 1).faceTowards(cells.get(i)) : heading;
        return facings;
    }

    /** The changes of direction along the line, the ends' joints included. */
    public static int elbows(List<Cell> cells, End start, End end) {
        var joints = joints(cells, start, end);
        if (joints == null) return -1;
        int elbows = 0;
        for (int i = 0; i < cells.size(); i++) {
            int mask = joints[i] | (i == 0 && start.joint >= 0 ? 1 << start.joint : 0) | (i == cells.size() - 1 && end.joint >= 0 ? 1 << end.joint : 0);
            if (Integer.bitCount(mask) == 2 && !opposite(mask)) elbows++;
        }
        return elbows;
    }

    private static boolean opposite(int mask) {
        int low = Integer.numberOfTrailingZeros(mask);
        return mask == (1 << low | 1 << (low ^ 1));
    }

    /**
     * Server: why {@code cells} cannot be laid as a line from {@code start} to {@code end}, or null: at most {@code maxCells}
     * cells, each a neighbour of the one before and none twice, from the start's cell to the end's, and no end joint on a
     * face the line itself uses. The joints of the cells are then {@link #joints} plus the ends'.
     */
    public static Refusal check(List<Cell> cells, End start, End end, int maxCells) {
        if (cells.isEmpty() || cells.size() > maxCells) return Refusal.TOO_LONG;
        if (!cells.getFirst().equals(start.cell) || !cells.getLast().equals(end.cell)) return Refusal.BROKEN;
        var seen = new java.util.HashSet<Cell>();
        for (int i = 0; i < cells.size(); i++) {
            if (!seen.add(cells.get(i))) return Refusal.BROKEN;
            if (i > 0 && cells.get(i - 1).faceTowards(cells.get(i)) < 0) return Refusal.BROKEN;
        }
        var masks = masks(cells, start, end);
        int last = cells.size() - 1;
        if (start.joint >= 0 && (masks[0] >> start.joint & 1) != 0) return Refusal.JOINTS;
        if (end.joint >= 0 && (masks[last] >> end.joint & 1) != 0) return Refusal.JOINTS;
        if (last == 0 && start.joint >= 0 && start.joint == end.joint) return Refusal.SAME_FACE;
        return null;
    }

    /** All joints of each cell, the ends' included: what each laid pipe is registered with. */
    public static int[] laidJoints(List<Cell> cells, End start, End end) {
        var joints = joints(cells, start, end);
        if (joints == null) return null;
        if (start.joint >= 0) joints[0] |= 1 << start.joint;
        if (end.joint >= 0) joints[cells.size() - 1] |= 1 << end.joint;
        return joints;
    }

    private record State(Cell cell, int heading) {
    }

    private record Node(Cell cell, int heading, double g, double h, int length, Node parent, long order) implements Comparable<Node> {
        double f() {
            return g + WEIGHT * h;
        }

        @Override
        public int compareTo(Node other) {
            int byF = Double.compare(f(), other.f());
            if (byF != 0) return byF;
            int byH = Double.compare(h, other.h);
            return byH != 0 ? byH : Long.compare(order, other.order);
        }
    }
}
