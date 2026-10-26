package com.wormzjl.icheme.science.equipment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.function.Function;

/**
 * The cells a laid pipe takes between its two ends (batch 2026-10-26-pipe-laying-system, plan section 4.3). The client
 * searches the route with a weighted A* and sends it; the server checks the cells it is sent ({@link #check}) and never
 * searches, so the two cannot disagree on the route.
 *
 * <p>Faces follow {@link LineGeometry#STEPS} (Minecraft's direction order): face {@code f}'s opposite is {@code f ^ 1}. A
 * route's heading in a cell is the face it travels towards there, so it entered through {@code heading ^ 1}. Each cell of a
 * route has a joint towards the cells before and after it; the first and last cells also have one towards what the line
 * starts or ends at, unless that end is open.</p>
 *
 * <p>In a rack cell the line is a lane on a slot range ({@link RackGeometry}), which it keeps along a run of racks: a
 * route's slot in each cell is the lane's first slot, or -1 in a plain cell. Steps along a rack are cheap, so routes follow
 * racks as plant piping does.</p>
 */
public final class PipeRoute {
    /** A step through a free cell; a change of heading (an elbow); the extra for a vertical step. */
    public static final double STEP = 1, ELBOW = 1.5, VERTICAL = .3;
    /** A step through a rack cell along the rack's axis; the extra for passing between a lane and a plain cell. */
    public static final double RACK_STEP = .4, TRANSITION = .5;

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
    public enum Admit {FREE, RACK, BLOCKED}

    /**
     * A cell as the route sees it: free, blocked, or a rack cell with the slots its lanes use (a bit per slot), the faces
     * they have joints on (a bit per face), and its axis (0 x, 1 y, 2 z).
     */
    public record Room(Admit admit, int used, int faces, int axis) {
        public static final Room FREE = new Room(Admit.FREE, 0, 0, -1), BLOCKED = new Room(Admit.BLOCKED, 0, 0, -1);

        public static Room rack(int used, int faces, int axis) {
            return new Room(Admit.RACK, used, faces, axis);
        }
    }

    /** The world as the route sees it. */
    public interface Space {
        Room room(Cell cell);

        /**
         * Every rack cell from {@code low} to {@code high}, inclusive. The search's estimate counts on knowing them all: a
         * rack it does not know of, it may pass by.
         */
        Collection<Cell> racks(Cell low, Cell high);

        static Space of(Function<Cell, Room> rooms, Collection<Cell> racks) {
            return new Space() {
                @Override
                public Room room(Cell cell) {
                    return rooms.apply(cell);
                }

                @Override
                public Collection<Cell> racks(Cell low, Cell high) {
                    return racks;
                }
            };
        }
    }

    /**
     * An end of a line: its cell, the face of that cell joined to what the line meets there (-1: an open end), and in a
     * rack cell the slot its lane must take (-1: any free one; an extended lane keeps its own).
     */
    public record End(Cell cell, int joint, int slot) {
        public End(Cell cell, int joint) {
            this(cell, joint, -1);
        }

        public End {
            if (cell == null || joint < -1 || joint > 5 || slot < -1 || slot >= RackGeometry.SLOTS) throw new IllegalArgumentException("Invalid route end");
        }
    }

    /** The search bounds: the cells of a route, the nodes expanded, and the blocks the search box extends past its ends. */
    public record Limits(int cells, int expansions, int margin) {
        public static final Limits DEFAULT = new Limits(128, 32768, 8);
    }

    /** Why a route cannot be laid; each has a player message. */
    public enum Refusal {START_OCCUPIED, END_OCCUPIED, SAME_FACE, BLOCKED, TOO_LONG, BROKEN, JOINTS}

    /** A route's cells and each cell's slot (-1 in a plain cell), or why there is none. */
    public record Result(List<Cell> cells, List<Integer> slots, Refusal refusal) {
        public Result {
            cells = List.copyOf(cells);
            slots = List.copyOf(slots);
        }

        public boolean found() {
            return refusal == null;
        }

        static Result refused(Refusal refusal) {
            return new Result(List.of(), List.of(), refusal);
        }
    }

    private PipeRoute() {
    }

    /**
     * The cheapest route from {@code start} to {@code end} for a line {@code width} slots wide in a rack, by the costs
     * above, entering the start cell along {@code heading} (the face the player clicked: it steers the first leg). An A*
     * search with {@link Estimate}, which never overestimates, so the route found is a cheapest one. Deterministic: ties
     * go to the lower f, then the lower h, then the earlier push; pushes follow the face order, and in a rack the order of
     * {@link RackGeometry#free}.
     */
    public static Result search(End start, int heading, End end, int width, Space space, Limits limits) {
        if (heading < 0 || heading > 5 || start.joint >= 0 && start.joint != (heading ^ 1)) throw new IllegalArgumentException("Heading does not leave the start's joint");
        var rooms = new HashMap<Cell, Room>();
        Function<Cell, Room> room = cell -> rooms.computeIfAbsent(cell, space::room);
        var startSlots = slots(room.apply(start.cell), start.slot, width);
        if (startSlots.isEmpty()) return Result.refused(Refusal.START_OCCUPIED);
        var endSlots = slots(room.apply(end.cell), end.slot, width);
        if (endSlots.isEmpty()) return Result.refused(Refusal.END_OCCUPIED);
        if (start.cell.equals(end.cell)) {
            if (start.joint >= 0 && start.joint == end.joint) return Result.refused(Refusal.SAME_FACE);
            for (int slot : startSlots) if (endSlots.contains(slot)) return new Result(List.of(start.cell), List.of(slot), null);
            return Result.refused(Refusal.END_OCCUPIED);
        }
        int lowX = Math.min(start.cell.x, end.cell.x) - limits.margin, highX = Math.max(start.cell.x, end.cell.x) + limits.margin;
        int lowY = Math.min(start.cell.y, end.cell.y) - limits.margin, highY = Math.max(start.cell.y, end.cell.y) + limits.margin;
        int lowZ = Math.min(start.cell.z, end.cell.z) - limits.margin, highZ = Math.max(start.cell.z, end.cell.z) + limits.margin;
        var estimate = Estimate.of(end.cell, space.racks(new Cell(lowX, lowY, lowZ), new Cell(highX, highY, highZ)), room, width);
        var open = new PriorityQueue<Node>();
        var best = new HashMap<State, Double>();
        long pushes = 0;
        for (int slot : startSlots) {
            open.add(new Node(start.cell, heading, slot, 0, estimate.at(start.cell, heading), 1, null, pushes++));
            best.put(new State(start.cell, heading, slot), 0.0);
        }
        int expansions = 0;
        boolean capped = false;
        while (!open.isEmpty()) {
            var node = open.poll();
            if (node.g > best.getOrDefault(new State(node.cell, node.heading, node.slot), Double.MAX_VALUE)) continue;
            if (node.cell.equals(end.cell)) return new Result(path(node), slots(node), null);
            if (++expansions > limits.expansions) {
                capped = true;
                break;
            }
            var here = room.apply(node.cell);
            for (int face = 0; face < 6; face++) {
                if (face == (node.heading ^ 1)) continue;
                var next = node.cell.step(face);
                if (next.x < lowX || next.x > highX || next.y < lowY || next.y > highY || next.z < lowZ || next.z > highZ) continue;
                if (node.length + 1 > limits.cells) {
                    capped = true;
                    continue;
                }
                var there = room.apply(next);
                if (there.admit == Admit.BLOCKED) continue;
                boolean lane = node.slot >= 0, rack = there.admit == Admit.RACK;
                // A rack face towards a plain cell carries at most one lane (plan 5.2): no lane of the rack may point there already.
                if (lane && !rack && (here.faces >> face & 1) != 0) continue;
                if (!lane && rack && (there.faces >> (face ^ 1) & 1) != 0) continue;
                double g = node.g + (rack && axis(face) == there.axis ? RACK_STEP : STEP) + (face != node.heading ? ELBOW : 0) + (face < 2 ? VERTICAL : 0)
                        + (lane != rack ? TRANSITION : 0);
                boolean last = next.equals(end.cell);
                if (last && end.joint >= 0) {
                    // The end cell joins what it meets through end.joint: entering through that face is impossible, any other but the far one is an elbow.
                    if (end.joint == (face ^ 1)) continue;
                    if (end.joint != face) g += ELBOW;
                }
                List<Integer> candidates = !rack ? List.of(-1) : lane ? fits(there, node.slot, width) ? List.of(node.slot) : List.of() : RackGeometry.free(there.used, width);
                for (int slot : candidates) {
                    if (last && !endSlots.contains(slot)) continue;
                    var state = new State(next, face, slot);
                    if (g >= best.getOrDefault(state, Double.MAX_VALUE)) continue;
                    best.put(state, g);
                    open.add(new Node(next, face, slot, g, estimate.at(next, face), node.length + 1, node, pushes++));
                }
            }
        }
        return Result.refused(capped ? Refusal.TOO_LONG : Refusal.BLOCKED);
    }

    /** The slots a line may take in {@code room}: -1 in a free cell; in a rack the free ranges, or only {@code fixed}; none when blocked. */
    private static List<Integer> slots(Room room, int fixed, int width) {
        return switch (room.admit) {
            case FREE -> List.of(-1);
            case BLOCKED -> List.of();
            case RACK -> fixed >= 0 ? fits(room, fixed, width) ? List.of(fixed) : List.of() : RackGeometry.free(room.used, width);
        };
    }

    /** The axis a step through {@code face} runs along, 0 x, 1 y, 2 z: faces pair up as y, z, x in {@link LineGeometry#STEPS} order. */
    static int axis(int face) {
        return ((face >> 1) + 1) % 3;
    }

    private static boolean fits(Room room, int slot, int width) {
        return slot + width <= RackGeometry.SLOTS && (room.used & ((1 << width) - 1) << slot) == 0;
    }

    /**
     * The search's estimate of the cost left from a cell to the goal; it never overestimates. Each block of a route costs
     * at least {@link #STEP}, except a step into a rack cell along its axis ({@link #RACK_STEP}). A route that uses no
     * rack therefore costs at least the Manhattan distance d; one that uses racks takes at least (r - 1) plain steps to
     * reach the first rack cell, r its distance from the nearest rack run, and at least r_goal plain steps from the last
     * one, so it costs at least 0.4 d + 0.6 (max(0, r - 1) + r_goal). The estimate is the smaller of the two: the full
     * distance far from racks, so open-ground routes go straight to the goal, and less only near a rack run. Every route
     * also pays {@link #VERTICAL} per block it still has to climb or drop, and an {@link #ELBOW} per turn it still needs
     * ({@link #turns}); without these, every path with as few turns would tie with the cheapest, and the search would
     * expand them all.
     *
     * <p>Rack runs are boxes of rack cells in a row along their axis; a cell's distance to a box is at most its distance
     * to any rack cell in it. Racks with no room for the line are left out: the line cannot enter them.</p>
     */
    private record Estimate(Cell goal, List<int[]> runs, int goalToRack) {
        static Estimate of(Cell goal, Collection<Cell> racks, Function<Cell, Room> room, int width) {
            var rows = new HashMap<List<Integer>, List<Integer>>();
            for (var cell : racks) {
                var r = room.apply(cell);
                if (r.admit != Admit.RACK || RackGeometry.free(r.used, width).isEmpty()) continue;
                int[] c = {cell.x, cell.y, cell.z};
                int a = r.axis;
                rows.computeIfAbsent(List.of(a, c[(a + 1) % 3], c[(a + 2) % 3]), k -> new ArrayList<>()).add(c[a]);
            }
            var runs = new ArrayList<int[]>();
            for (var row : rows.entrySet()) {
                int a = row.getKey().get(0);
                var along = row.getValue();
                Collections.sort(along);
                for (int i = 0; i < along.size(); ) {
                    int j = i;
                    while (j + 1 < along.size() && along.get(j + 1) <= along.get(j) + 1) j++;
                    var box = new int[6];
                    box[a] = along.get(i);
                    box[a + 3] = along.get(j);
                    box[(a + 1) % 3] = box[(a + 1) % 3 + 3] = row.getKey().get(1);
                    box[(a + 2) % 3] = box[(a + 2) % 3 + 3] = row.getKey().get(2);
                    runs.add(box);
                    i = j + 1;
                }
            }
            var estimate = new Estimate(goal, runs, 0);
            return new Estimate(goal, runs, estimate.toRack(goal));
        }

        /** The estimate from {@code cell}, entered along {@code heading}. */
        double at(Cell cell, int heading) {
            int d = cell.distance(goal);
            double blocks = runs.isEmpty() ? STEP * d : Math.min(STEP * d, RACK_STEP * d + (STEP - RACK_STEP) * (Math.max(0, toRack(cell) - 1) + goalToRack));
            return blocks + VERTICAL * Math.abs(goal.y - cell.y) + ELBOW * turns(cell, heading);
        }

        /**
         * The fewest elbows left: a leg along each axis the goal still lies off, each a change of heading, but for a first
         * leg along the heading the cell was entered on.
         */
        private int turns(Cell cell, int heading) {
            int[] apart = {goal.x - cell.x, goal.y - cell.y, goal.z - cell.z};
            var step = LineGeometry.STEPS[heading];
            int axes = 0;
            boolean along = false;
            for (int a = 0; a < 3; a++)
                if (apart[a] != 0) {
                    axes++;
                    if (Integer.signum(apart[a]) == step[a]) along = true;
                }
            return along ? axes - 1 : axes;
        }

        private int toRack(Cell cell) {
            int nearest = Integer.MAX_VALUE;
            for (var b : runs) {
                int d = Math.max(0, Math.max(b[0] - cell.x, cell.x - b[3])) + Math.max(0, Math.max(b[1] - cell.y, cell.y - b[4])) + Math.max(0, Math.max(b[2] - cell.z, cell.z - b[5]));
                if (d < nearest) nearest = d;
            }
            return nearest;
        }
    }

    private static List<Cell> path(Node node) {
        var cells = new ArrayList<Cell>();
        for (var at = node; at != null; at = at.parent) cells.add(at.cell);
        Collections.reverse(cells);
        return cells;
    }

    private static List<Integer> slots(Node node) {
        var slots = new ArrayList<Integer>();
        for (var at = node; at != null; at = at.parent) slots.add(at.slot);
        Collections.reverse(slots);
        return slots;
    }

    private static int[] masks(List<Cell> cells) {
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

    /** The changes of direction along a checked line, the ends' joints included. */
    public static int elbows(List<Cell> cells, End start, End end) {
        var joints = laidJoints(cells, start, end);
        int elbows = 0;
        for (int mask : joints) if (Integer.bitCount(mask) == 2 && !opposite(mask)) elbows++;
        return elbows;
    }

    private static boolean opposite(int mask) {
        int low = Integer.numberOfTrailingZeros(mask);
        return mask == (1 << low | 1 << (low ^ 1));
    }

    /**
     * Server: why {@code cells} with {@code slots} cannot be laid as a line {@code width} slots wide from {@code start} to
     * {@code end}, or null: at most {@code maxCells} cells, each a neighbour of the one before and none twice, from the
     * start's cell to the end's; a slot per cell, -1 or a range that fits, kept from lane to lane and as the ends require;
     * and no end joint on a face the line itself uses. Whether each cell is free, or a rack cell with that range free, is
     * the world's to say.
     */
    public static Refusal check(List<Cell> cells, List<Integer> slots, End start, End end, int width, int maxCells) {
        if (cells.isEmpty() || cells.size() > maxCells) return Refusal.TOO_LONG;
        if (slots.size() != cells.size() || !cells.getFirst().equals(start.cell) || !cells.getLast().equals(end.cell)) return Refusal.BROKEN;
        var seen = new HashSet<Cell>();
        for (int i = 0; i < cells.size(); i++) {
            int slot = slots.get(i);
            if (!seen.add(cells.get(i)) || slot < -1 || slot + width > RackGeometry.SLOTS) return Refusal.BROKEN;
            if (i > 0 && (cells.get(i - 1).faceTowards(cells.get(i)) < 0 || slot >= 0 && slots.get(i - 1) >= 0 && slots.get(i - 1) != slot)) return Refusal.BROKEN;
        }
        if (start.slot >= 0 && slots.getFirst() != start.slot || end.slot >= 0 && slots.getLast() != end.slot) return Refusal.BROKEN;
        var masks = masks(cells);
        int last = cells.size() - 1;
        if (start.joint >= 0 && (masks[0] >> start.joint & 1) != 0) return Refusal.JOINTS;
        if (end.joint >= 0 && (masks[last] >> end.joint & 1) != 0) return Refusal.JOINTS;
        if (last == 0 && start.joint >= 0 && start.joint == end.joint) return Refusal.SAME_FACE;
        return null;
    }

    /**
     * Server: the first cell of a checked line that {@code space} does not admit as the search would, or -1: a plain cell
     * free, a lane's range free in its rack, and no step between a lane and a plain cell through a rack face that another
     * lane already has a joint on. The ends' own joints are the anchors' to check.
     */
    public static int blocked(List<Cell> cells, List<Integer> slots, int width, Space space) {
        Room before = null;
        for (int i = 0; i < cells.size(); i++) {
            var room = space.room(cells.get(i));
            int slot = slots.get(i);
            boolean lane = slot >= 0;
            if (room.admit == Admit.BLOCKED || lane != (room.admit == Admit.RACK) || lane && !fits(room, slot, width)) return i;
            if (i > 0) {
                int face = cells.get(i - 1).faceTowards(cells.get(i));
                boolean wasLane = slots.get(i - 1) >= 0;
                if (wasLane && !lane && (before.faces >> face & 1) != 0 || !wasLane && lane && (room.faces >> (face ^ 1) & 1) != 0) return i;
            }
            before = room;
        }
        return -1;
    }

    /** All joints of each cell of a checked line, the ends' included: what each laid pipe or lane is registered with. */
    public static int[] laidJoints(List<Cell> cells, End start, End end) {
        var joints = masks(cells);
        if (start.joint >= 0) joints[0] |= 1 << start.joint;
        if (end.joint >= 0) joints[cells.size() - 1] |= 1 << end.joint;
        return joints;
    }

    private record State(Cell cell, int heading, int slot) {
    }

    private record Node(Cell cell, int heading, int slot, double g, double h, int length, Node parent, long order) implements Comparable<Node> {
        double f() {
            return g + h;
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
