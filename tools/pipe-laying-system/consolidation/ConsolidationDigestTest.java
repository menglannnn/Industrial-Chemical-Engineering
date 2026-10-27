package com.wormzjl.icheme.science.equipment;

import com.wormzjl.icheme.science.equipment.PipeRoute.Cell;
import com.wormzjl.icheme.science.equipment.PipeRoute.End;
import com.wormzjl.icheme.science.equipment.PipeRoute.Room;
import com.wormzjl.icheme.science.fluid.topology.TopologyCompiler.Kind;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Scratch probe for the batch 2026-10-26-pipe-laying-system consolidation (not a gate, never committed): hashes the rack
 * geometry (frames, lanes, points, boxes, picks, aims, placement, steel, spools), the line geometry (plain pipes, devices,
 * transition pipes) and the route search (cells, options, refusal, and the server's check of each found route) over a
 * broad key set. Run at the start commit and after the last change: every digest must be bit-identical. Writes
 * tmp/consolidation-digest/last.txt.
 */
class ConsolidationDigestTest {
    private static final int DOWN = 0, UP = 1, NORTH = 2, SOUTH = 3, WEST = 4, EAST = 5;
    private static final int[] CODES = {4, 11, 23, 36, 47};
    private static final int[] STEELS = {
            RackGeometry.BENT_BACK | RackGeometry.BENT_FRONT | RackGeometry.END_BACK | RackGeometry.END_FRONT | RackGeometry.STACKED | RackGeometry.GROUNDED | RackGeometry.RISING,
            RackGeometry.BENT_BACK | RackGeometry.BENT_FRONT | RackGeometry.END_BACK | RackGeometry.END_FRONT | RackGeometry.GROUNDED,
            RackGeometry.BENT_BACK | RackGeometry.RIGHT_JOINED | RackGeometry.RISING,
            RackGeometry.LEFT_JOINED | RackGeometry.STACKED};
    private static final double[] OUTSIDES = {.0625, .171875, .359375, .508, .711, PipeSizes.MAX_OUTSIDE};

    /** A running SHA-256 with a count of what it took. */
    private static final class Digest {
        private final MessageDigest sha;
        long items;

        Digest() {
            try {
                sha = MessageDigest.getInstance("SHA-256");
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        Digest i(int v) {
            sha.update(new byte[]{(byte) (v >>> 24), (byte) (v >>> 16), (byte) (v >>> 8), (byte) v});
            return this;
        }

        Digest l(long v) {
            return i((int) (v >>> 32)).i((int) v);
        }

        Digest d(double v) {
            return l(Double.doubleToRawLongBits(v));
        }

        Digest b(boolean v) {
            return i(v ? 1 : 0);
        }

        Digest s(String v) {
            sha.update(v.getBytes(StandardCharsets.UTF_8));
            return i(v.length());
        }

        Digest ds(double[] v) {
            if (v == null) return i(-7);
            i(v.length);
            for (double x : v) d(x);
            return this;
        }

        Digest polygons(List<Mesh.Polygon> polygons) {
            i(polygons.size());
            for (var p : polygons) {
                s(p.material().name()).ds(p.positions()).ds(p.uvs()).ds(p.normals());
            }
            items++;
            return this;
        }

        String hex() {
            return HexFormat.of().formatHex(sha.digest()).substring(0, 16);
        }
    }

    private final Map<String, String> sections = new LinkedHashMap<>();

    private void section(String name, java.util.function.Consumer<Digest> body) {
        long t0 = System.nanoTime();
        var digest = new Digest();
        body.accept(digest);
        sections.put(name, digest.hex() + " items=" + digest.items + " (" + (System.nanoTime() - t0) / 1_000_000 + " ms)");
    }

    @Test
    void digest() throws Exception {
        section("rack.frames", this::frames);
        section("rack.frames.lanes", this::framesWithLanes);
        section("rack.lanes", this::lanes);
        section("rack.fits", this::fits);
        section("rack.aim", this::aims);
        section("rack.pick", this::picks);
        section("rack.placed", this::placed);
        section("rack.steel", this::steel);
        section("line.pipes", this::pipes);
        section("line.devices", this::devices);
        section("line.transitions", this::transitions);
        section("route.search", this::routes);
        var overall = new Digest();
        var out = new StringBuilder();
        for (var e : sections.entrySet()) {
            overall.s(e.getKey()).s(e.getValue().substring(0, e.getValue().indexOf(" (")));
            out.append(e.getKey()).append(' ').append(e.getValue()).append('\n');
        }
        out.append("overall ").append(overall.hex()).append('\n');
        System.out.print(out);
        Files.createDirectories(Path.of("tmp/consolidation-digest"));
        Files.writeString(Path.of("tmp/consolidation-digest/last.txt"), out.toString());
    }

    // ---- racks ---------------------------------------------------------------------------------------------------------

    private static List<RackGeometry.Rack> shapes() {
        var out = new ArrayList<RackGeometry.Rack>();
        for (int facing = 2; facing < 6; facing++)
            for (int corner = RackGeometry.STRAIGHT; corner <= RackGeometry.TEE; corner++) out.add(new RackGeometry.Rack(facing, corner));
        return out;
    }

    /** Every lane a cell takes alone at the test sizes, every option and arm pair. */
    private static List<RackGeometry.Lane> alone(RackGeometry.Rack rack) {
        var out = new ArrayList<RackGeometry.Lane>();
        for (int code : CODES) {
            int width = RackGeometry.width(PipeSizes.outsideOf(code));
            if (width > RackGeometry.MAX_WIDTH) continue;
            for (int option = 0; option < RackGeometry.OPTIONS; option++) {
                int w = RackGeometry.tier(option) ? width : 1;
                if (!RackGeometry.tier(option) && width != 1) continue;
                for (int a = 0; a < 6; a++)
                    for (int b = a + 1; b < 6; b++) {
                        int arms = 1 << a | 1 << b;
                        if (!RackGeometry.valid(rack, option, w, arms)) continue;
                        var lane = new RackGeometry.Lane(option, w, arms, 0, code);
                        if (RackGeometry.fits(rack, List.of(), lane)) out.add(lane);
                    }
            }
        }
        return out;
    }

    private void frames(Digest d) {
        for (var shape : shapes())
            for (int steel = 0; steel <= RackGeometry.STEEL; steel++)
                for (int beam : new int[]{RackGeometry.BEAM_TOP, 8, 2}) {
                    var rack = new RackGeometry.Rack(shape.facing(), shape.corner(), steel, beam);
                    d.polygons(RackGeometry.polygons(rack, List.of()));
                    var members = RackGeometry.members(rack);
                    d.i(members.size());
                    for (var box : members) d.ds(box);
                    var work = RackGeometry.steelwork(rack, List.of());
                    d.i(work.drawn().size());
                    for (var m : work.drawn()) d.ds(m.a()).ds(m.b()).d(m.width()).s(m.material().name());
                    d.i(rack.bents()).b(rack.stacked()).i(rack.sides()).b(rack.grounded()).i(rack.ends()).b(rack.rising());
                }
    }

    private void framesWithLanes(Digest d) {
        for (var shape : shapes()) {
            var lanes = alone(shape);
            for (int steel : STEELS)
                for (var lane : lanes) {
                    var rack = new RackGeometry.Rack(shape.facing(), shape.corner(), steel, RackGeometry.beam(List.of(lane)));
                    d.polygons(RackGeometry.polygons(rack, List.of(lane)));
                }
        }
    }

    private void lanes(Digest d) {
        for (var shape : shapes()) {
            var lanes = alone(shape);
            d.i(lanes.size());
            for (var alone : lanes) {
                int low = Integer.numberOfTrailingZeros(alone.arms()), high = 31 - Integer.numberOfLeadingZeros(alone.arms());
                for (int joined : new int[]{0, alone.arms(), 1 << low, 1 << high}) {
                    var lane = new RackGeometry.Lane(alone.option(), alone.width(), alone.arms(), joined, alone.outside());
                    for (int steel : STEELS) {
                        var rack = new RackGeometry.Rack(shape.facing(), shape.corner(), steel, RackGeometry.beam(List.of(lane)));
                        d.polygons(RackGeometry.lane(rack, lane));
                        if (joined == lane.arms()) {
                            d.polygons(RackGeometry.lane(rack, lane, true));
                            for (int along = 0; along < LineGeometry.SPOOL; along++) d.b(RackGeometry.spool(rack, lane, along));
                        }
                        for (var box : RackGeometry.boxes(rack, lane)) d.ds(box);
                    }
                }
                d.l(RackGeometry.points(shape, alone));
                for (int face = 0; face < 6; face++) {
                    d.i(RackGeometry.point(shape, alone.option(), alone.width(), alone.diameter(), face));
                    if ((alone.arms() >> face & 1) != 0) d.ds(RackGeometry.facePoint(shape, alone, face));
                }
                d.i(RackGeometry.taken(shape, List.of(alone))).i(RackGeometry.beam(List.of(alone)));
            }
            for (int option = 0; option < RackGeometry.OPTIONS; option++)
                for (int width = 0; width <= 5; width++) {
                    d.b(RackGeometry.takes(option, width));
                    for (int arms = 0; arms < 64; arms++) d.b(RackGeometry.valid(shape, option, width, arms));
                    for (double outside : OUTSIDES) d.b(RackGeometry.open(shape, List.of(), option, width, outside));
                }
            d.i(RackGeometry.runs(shape));
            for (int face = 0; face < 6; face++) d.b(RackGeometry.along(shape, face));
        }
        for (int code = 1; code <= 64; code++) {
            double od = PipeSizes.outsideOf(code);
            d.i(RackGeometry.width(od)).i(RackGeometry.height(od)).d(PipeSizes.drawn(code / 70.0));
            for (int other = 1; other <= 40; other += 3) d.b(RackGeometry.clears(od, PipeSizes.outsideOf(other))).b(RackGeometry.clearsUnder(od, PipeSizes.outsideOf(other)));
        }
        for (int point = 0; point < 1024; point += 7) for (int v : RackGeometry.steps(point)) d.i(v);
    }

    private void fits(Digest d) {
        for (var shape : shapes()) {
            var lanes = new ArrayList<RackGeometry.Lane>();
            for (var lane : alone(shape)) if (lane.outside() == 11 || lane.outside() == 23 || lane.outside() == 47) lanes.add(lane);
            for (var a : lanes)
                for (var b : lanes) {
                    boolean fits = RackGeometry.fits(shape, List.of(a), b);
                    d.b(fits);
                    if (fits) d.items++;
                    for (double outside : OUTSIDES) d.b(RackGeometry.open(shape, List.of(a), b.option(), b.width(), outside));
                }
        }
    }

    /** A few lane lists per shape: none, the first lane alone, and the first two that fit together. */
    private static List<List<RackGeometry.Lane>> laneLists(RackGeometry.Rack shape) {
        var out = new ArrayList<List<RackGeometry.Lane>>();
        out.add(List.of());
        var lanes = alone(shape);
        if (lanes.isEmpty()) return out;
        out.add(List.of(lanes.getFirst()));
        for (var b : lanes)
            if (b != lanes.getFirst() && RackGeometry.fits(shape, List.of(lanes.getFirst()), b)) {
                out.add(List.of(lanes.getFirst(), b));
                break;
            }
        out.add(List.of(lanes.get(lanes.size() / 2)));
        return out;
    }

    private void aims(Digest d) {
        for (var shape : shapes())
            for (var lanes : laneLists(shape))
                for (double outside : OUTSIDES)
                    for (int x = 0; x <= 16; x++)
                        for (int z = 0; z <= 16; z++) {
                            var aim = RackGeometry.aim(shape, lanes, outside, new double[]{x / 16.0, .4, z / 16.0});
                            if (aim == null) d.i(-1);
                            else {
                                d.i(aim.option()).i(aim.through()).i(aim.open()).b(aim.fits());
                                d.items++;
                            }
                        }
    }

    private void picks(Digest d) {
        for (var shape : shapes())
            for (var lanes : laneLists(shape))
                for (int steel : new int[]{0, STEELS[0]}) {
                    var rack = new RackGeometry.Rack(shape.facing(), shape.corner(), steel, RackGeometry.beam(lanes));
                    for (int x = 0; x <= 10; x++)
                        for (int y = 0; y <= 10; y++)
                            for (int z = 0; z <= 10; z++) {
                                var pick = RackGeometry.pick(rack, lanes, x / 10.0, y / 10.0, z / 10.0);
                                d.i(pick.lane()).i(pick.face());
                                if (pick.lane() >= 0) d.items++;
                            }
                }
    }

    private void placed(Digest d) {
        var choices = new ArrayList<RackGeometry.Rack>();
        choices.add(null);
        choices.addAll(shapes());
        var around = new RackGeometry.Rack[6];
        for (var n : choices)
            for (var s : choices)
                for (var w : choices)
                    for (var e : choices) {
                        around[NORTH] = n;
                        around[SOUTH] = s;
                        around[WEST] = w;
                        around[EAST] = e;
                        RackGeometry.Neighbours neighbours = face -> around[face];
                        for (int look = 2; look < 6; look++) {
                            var rack = RackGeometry.placed(neighbours, look, false);
                            d.i(rack.facing()).i(rack.corner());
                            if (rack.junction()) d.items++;
                        }
                        var sneaking = RackGeometry.placed(neighbours, EAST, true);
                        d.i(sneaking.facing()).i(sneaking.corner());
                    }
    }

    private void steel(Digest d) {
        var random = new Random(42);
        var choices = new ArrayList<RackGeometry.Rack>();
        choices.add(null);
        choices.addAll(shapes());
        for (int sample = 0; sample < 3000; sample++) {
            var around = new RackGeometry.Rack[6];
            var known = new boolean[6];
            for (int face = 0; face < 6; face++) {
                around[face] = random.nextInt(3) == 0 ? null : choices.get(random.nextInt(choices.size()));
                known[face] = random.nextInt(5) != 0;
            }
            var neighbours = new RackGeometry.Neighbours() {
                @Override
                public RackGeometry.Rack rack(int face) {
                    return around[face];
                }

                @Override
                public boolean known(int face) {
                    return known[face];
                }
            };
            for (var shape : shapes())
                for (int along = -2; along <= 5; along++)
                    for (int previous : new int[]{0, RackGeometry.STEEL, STEELS[2]}) {
                        int steel = RackGeometry.steel(neighbours, shape, along, previous);
                        d.i(steel);
                        var framed = RackGeometry.framed(shape, steel, 9);
                        d.i(framed.steel()).i(framed.beam());
                    }
            d.items++;
        }
    }

    // ---- lines ---------------------------------------------------------------------------------------------------------

    private void pipes(Digest d) {
        for (int facing = 0; facing < 6; facing++)
            for (int connections = 0; connections < 64; connections++)
                for (int own : new int[]{4, 16, 40})
                    for (int met : new int[]{own, 4, 30})
                        for (int flanged : new int[]{0, connections})
                            for (boolean spool : new boolean[]{false, true}) {
                                long faces = 0;
                                for (int f = 0; f < 6; f++) if ((connections >> f & 1) != 0) faces |= (long) met << 7 * f;
                                d.polygons(LineGeometry.polygons(new LineGeometry.Key(Kind.PIPE, facing, connections, own, faces, flanged, spool)));
                            }
        d.polygons(LineGeometry.polygons(LineGeometry.Key.item(Kind.PIPE)));
    }

    private void devices(Digest d) {
        for (var kind : new Kind[]{Kind.VALVE, Kind.PUMP, Kind.COMPRESSOR, Kind.FILTER, Kind.RESERVOIR, Kind.GENERATOR, Kind.VOID})
            for (int facing = 0; facing < 6; facing++)
                for (int connections = 0; connections < 64; connections += 1)
                    for (int met : new int[]{0, 11, 30}) {
                        long faces = 0;
                        for (int f = 0; f < 6; f++) if ((connections >> f & 1) != 0) faces |= (long) met << 7 * f;
                        try {
                            d.polygons(LineGeometry.polygons(new LineGeometry.Key(kind, facing, connections, 11, faces, connections, false)));
                        } catch (RuntimeException e) {
                            d.s("EX " + e.getClass().getName());
                        }
                    }
        for (var kind : new Kind[]{Kind.VALVE, Kind.PUMP, Kind.COMPRESSOR, Kind.FILTER, Kind.RESERVOIR, Kind.GENERATOR, Kind.VOID})
            d.polygons(LineGeometry.polygons(LineGeometry.Key.item(kind)));
    }

    private void transitions(Digest d) {
        int[][] steps = {{-6, -6}, {-6, 3}, {-3, 0}, {0, 6}, {3, -3}, {6, 6}, {0, -5}, {5, 2}, {-2, -7}};
        for (int a = 0; a < 6; a++)
            for (int b = -1; b < 6; b++) {
                if (b == a) continue;
                for (int own : CODES)
                    for (int met : new int[]{own, 4})
                        for (var step : steps)
                            for (int flanged : new int[]{0, 1 << a}) {
                                int connections = 1 << a | (b < 0 ? 0 : 1 << b);
                                long faces = (long) met << 7 * a | (b < 0 ? 0 : (long) own << 7 * b);
                                long offsets = LineGeometry.Key.offsets(a, step[0], step[1]);
                                if (b >= 0 && flanged == 0) offsets |= LineGeometry.Key.offsets(b, step[1], -step[0]);
                                var key = new LineGeometry.Key(Kind.PIPE, a ^ 1, connections, own, faces, flanged, false, offsets);
                                d.b(LineGeometry.transition(key));
                                d.polygons(LineGeometry.polygons(key));
                                if (b < 0 && met == own) d.polygons(LineGeometry.filterPolygons(key));
                            }
            }
        // A three-way pipe with an off-centre face is no transition: drawn as before.
        for (int code : CODES) {
            var key = new LineGeometry.Key(Kind.PIPE, EAST, 1 << WEST | 1 << EAST | 1 << UP, code, (long) code << 7 * WEST | (long) code << 7 * EAST | (long) code << 7 * UP, 0, false,
                    LineGeometry.Key.offsets(WEST, 3, -2));
            d.b(LineGeometry.transition(key));
            d.polygons(LineGeometry.polygons(key));
        }
    }

    // ---- routes --------------------------------------------------------------------------------------------------------

    private static Room rack(int facing, int corner, RackGeometry.Lane... lanes) {
        int faces = 0;
        for (var lane : lanes) faces |= lane.joined();
        return Room.rack(new RackGeometry.Rack(facing, corner), List.of(lanes), faces);
    }

    private static RackGeometry.Lane lane(int position, int width, double outside, int arms, int joined) {
        return new RackGeometry.Lane(RackGeometry.option(position, RackGeometry.TIER), width, arms, joined, PipeSizes.code(outside));
    }

    /** Racks facing {@code facing} along x from {@code from} to {@code to} at y {@code y}, z {@code z}, with these lanes, open at the outer ends. */
    private static void row(Map<Cell, Room> rooms, int from, int to, int y, int z, int facing, RackGeometry.Lane... lanes) {
        for (int x = from; x <= to; x++) {
            int outer = (x == from ? 1 << WEST : 0) | (x == to ? 1 << EAST : 0);
            var cell = new RackGeometry.Lane[lanes.length];
            for (int i = 0; i < lanes.length; i++)
                cell[i] = new RackGeometry.Lane(lanes[i].option(), lanes[i].width(), lanes[i].arms(), lanes[i].joined() & ~outer, lanes[i].outside());
            rooms.put(new Cell(x, y, z), rack(facing, RackGeometry.STRAIGHT, cell));
        }
    }

    private static PipeRoute.Space space(Map<Cell, Room> rooms, java.util.Set<Cell> blocked) {
        return PipeRoute.Space.of(cell -> rooms.containsKey(cell) ? rooms.get(cell) : blocked.contains(cell) ? Room.BLOCKED : Room.FREE, List.copyOf(rooms.keySet()));
    }

    private static void route(Digest d, End start, int heading, End end, double outside, PipeRoute.Space space, PipeRoute.Limits limits) {
        PipeRoute.Result route;
        try {
            route = PipeRoute.search(start, heading, end, outside, space, limits);
        } catch (IllegalArgumentException e) {
            d.s("EX");
            return;
        }
        d.s(String.valueOf(route.refusal()));
        if (route.found()) d.items++;
        d.i(route.cells().size());
        for (var c : route.cells()) d.i(c.x()).i(c.y()).i(c.z());
        for (int o : route.options()) d.i(o);
        if (!route.found()) return;
        var check = PipeRoute.check(route.cells(), route.options(), start, end, PipeRoute.Limits.DEFAULT.cells());
        d.s(String.valueOf(check));
        d.i(PipeRoute.blocked(route.cells(), route.options(), start, heading, end, outside, space));
        for (int j : PipeRoute.laidJoints(route.cells(), start, end)) d.i(j);
        for (int f : PipeRoute.facings(route.cells(), heading)) d.i(f);
        for (int a : PipeRoute.arms(route.cells(), start, heading, end)) d.i(a);
        d.i(PipeRoute.elbows(route.cells(), start, end));
    }

    /** The open and aimed ends a click on each rack cell of {@code rooms} gives, at a 3 x 3 grid on its tier. */
    private static List<End> aimedEnds(Map<Cell, Room> rooms, double outside) {
        var out = new ArrayList<End>();
        for (var e : rooms.entrySet()) {
            var room = e.getValue();
            if (room.admit() != PipeRoute.Admit.RACK) continue;
            for (double x : new double[]{.1, .5, .9})
                for (double z : new double[]{.1, .5, .9}) {
                    var aim = RackGeometry.aim(room.rack(), room.lanes(), outside, new double[]{x, .4, z});
                    if (aim != null && aim.fits()) {
                        var end = new End(e.getKey(), -1, -1, aim.option(), aim.through(), aim.open());
                        if (!out.contains(end)) out.add(end);
                    }
                }
        }
        return out;
    }

    private void routes(Digest d) {
        double[] sizes = {.0625, .171875, .359375};
        // A: aimed ends on a straight run facing east, every slot and face pair.
        {
            var rooms = new LinkedHashMap<Cell, Room>();
            row(rooms, 1, 6, 0, 0, EAST);
            var space = space(rooms, java.util.Set.of());
            for (double outside : sizes) {
                int width = RackGeometry.width(PipeSizes.outsideOf(PipeSizes.code(outside)));
                for (int a = 1; a <= 6; a++)
                    for (int b = 1; b <= 6; b++)
                        for (int from = 0; from + width <= RackGeometry.POSITIONS; from++)
                            for (int to = 0; to + width <= RackGeometry.POSITIONS; to++)
                                for (int leave : new int[]{WEST, EAST})
                                    for (int arrive : new int[]{WEST, EAST}) {
                                        var start = new End(new Cell(a, 0, 0), -1, -1, RackGeometry.option(from, RackGeometry.TIER), leave, leave ^ 1);
                                        var end = new End(new Cell(b, 0, 0), -1, -1, RackGeometry.option(to, RackGeometry.TIER), arrive, arrive ^ 1);
                                        route(d, start, leave, end, outside, space, PipeRoute.Limits.DEFAULT);
                                    }
            }
        }
        // B: plain open ends round a run with lanes, every heading.
        {
            var rooms = new LinkedHashMap<Cell, Room>();
            int along = 1 << WEST | 1 << EAST;
            row(rooms, 1, 9, 0, 1, EAST, lane(1, 1, .0625, along, along));
            var space = space(rooms, java.util.Set.of(new Cell(4, 0, 0), new Cell(5, 1, 1)));
            var starts = new ArrayList<Cell>();
            for (int x : new int[]{-2, 0, 5, 11}) for (int z : new int[]{-1, 0, 2, 3}) starts.add(new Cell(x, 0, z));
            var ends = List.of(new Cell(12, 0, 0), new Cell(10, 0, 1), new Cell(6, 2, 3), new Cell(-3, 0, 1), new Cell(3, 0, -2), new Cell(8, -1, 2));
            for (double outside : sizes)
                for (var s : starts)
                    for (int heading = 0; heading < 6; heading++)
                        for (var e : ends) route(d, new End(s, -1), heading, new End(e, -1), outside, space, PipeRoute.Limits.DEFAULT);
            // Jointed ends, and a start continuing a lane at its point.
            for (double outside : sizes)
                for (var e : ends) {
                    route(d, new End(new Cell(0, 0, 0), WEST), EAST, new End(e, NORTH), outside, space, PipeRoute.Limits.DEFAULT);
                    for (int p = 0; p < 4; p++) {
                        int point = RackGeometry.point(new RackGeometry.Rack(EAST, RackGeometry.STRAIGHT), RackGeometry.option(p, RackGeometry.TIER), 1, outside, WEST);
                        if (point >= 0) route(d, new End(new Cell(1, 0, 1), WEST, point), EAST, new End(e, -1), outside, space, PipeRoute.Limits.DEFAULT);
                    }
                }
            for (var aimed : aimedEnds(rooms, .0625)) {
                route(d, aimed, aimed.open() ^ 1, new End(new Cell(12, 0, 0), -1), .0625, space, PipeRoute.Limits.DEFAULT);
                route(d, new End(new Cell(-2, 0, 0), -1), EAST, aimed, .0625, space, PipeRoute.Limits.DEFAULT);
            }
            route(d, new End(new Cell(0, 0, 0), -1), EAST, new End(new Cell(30, 0, 0), -1), .0625, space, new PipeRoute.Limits(10, 32768, 8));
            route(d, new End(new Cell(0, 0, 0), -1), EAST, new End(new Cell(30, 0, 0), -1), .0625, space, new PipeRoute.Limits(128, 50, 8));
        }
        // C: a corner joining a north run to an east run, and D: a tee whose branch runs east, with aimed and plain ends.
        for (int scene = 0; scene < 2; scene++) {
            var rooms = new LinkedHashMap<Cell, Room>();
            if (scene == 0) {
                for (int z = 1; z <= 5; z++) rooms.put(new Cell(0, 0, z), rack(NORTH, RackGeometry.STRAIGHT));
                rooms.put(new Cell(0, 0, 0), rack(NORTH, RackGeometry.BACK_RIGHT));
                for (int x = 1; x <= 5; x++) rooms.put(new Cell(x, 0, 0), rack(EAST, RackGeometry.STRAIGHT));
            } else {
                for (int z = -3; z <= 3; z++) if (z != 0) rooms.put(new Cell(0, 0, z), rack(NORTH, RackGeometry.STRAIGHT));
                rooms.put(new Cell(0, 0, 0), rack(EAST, RackGeometry.TEE));
                for (int x = 1; x <= 3; x++) rooms.put(new Cell(x, 0, 0), rack(EAST, RackGeometry.STRAIGHT));
            }
            var space = space(rooms, java.util.Set.of());
            var plain = List.of(new Cell(0, 0, 6), new Cell(6, 0, 0), new Cell(0, 0, -4), new Cell(5, 0, 2), new Cell(-2, 0, 3), new Cell(4, 1, -2));
            for (double outside : sizes) {
                for (var s : plain)
                    for (int heading = 2; heading < 6; heading++)
                        for (var e : plain) if (!s.equals(e)) route(d, new End(s, -1), heading, new End(e, -1), outside, space, PipeRoute.Limits.DEFAULT);
                var aimed = aimedEnds(rooms, outside);
                d.i(aimed.size());
                for (var a : aimed)
                    for (var e : plain) {
                        route(d, a, a.open() ^ 1, new End(e, -1), outside, space, PipeRoute.Limits.DEFAULT);
                        route(d, new End(e, -1), EAST, a, outside, space, PipeRoute.Limits.DEFAULT);
                    }
                for (int i = 0; i < aimed.size(); i += 3)
                    for (int j = 1; j < aimed.size(); j += 4) route(d, aimed.get(i), aimed.get(i).open() ^ 1, aimed.get(j), outside, space, PipeRoute.Limits.DEFAULT);
            }
        }
        // E: two runs side by side, the north one full, and a stacked run over the south one.
        {
            var rooms = new LinkedHashMap<Cell, Room>();
            int along = 1 << WEST | 1 << EAST;
            row(rooms, 0, 5, 0, 0, EAST, lane(0, 1, .171875, along, along), lane(1, 1, .171875, along, along), lane(2, 1, .171875, along, along), lane(3, 1, .171875, along, along));
            row(rooms, 0, 5, 0, 1, EAST);
            row(rooms, 0, 8, 1, 1, EAST);
            var space = space(rooms, java.util.Set.of());
            var cells = List.of(new Cell(-1, 0, 1), new Cell(3, 0, -2), new Cell(9, 1, 1), new Cell(3, 2, 1), new Cell(-1, 1, 1), new Cell(6, 0, 0));
            for (double outside : sizes)
                for (var s : cells)
                    for (int heading = 0; heading < 6; heading++)
                        for (var e : cells) if (!s.equals(e)) route(d, new End(s, -1), heading, new End(e, -1), outside, space, PipeRoute.Limits.DEFAULT);
        }
        // F: open ground with obstacles; one-cell lines.
        {
            var random = new Random(7);
            var blocked = new java.util.LinkedHashSet<Cell>();
            for (int i = 0; i < 400; i++) blocked.add(new Cell(random.nextInt(21) - 10, random.nextInt(7) - 3, random.nextInt(21) - 10));
            var space = space(new LinkedHashMap<>(), blocked);
            for (int i = 0; i < 300; i++) {
                var s = new Cell(random.nextInt(21) - 10, random.nextInt(7) - 3, random.nextInt(21) - 10);
                var e = new Cell(random.nextInt(21) - 10, random.nextInt(7) - 3, random.nextInt(21) - 10);
                int heading = random.nextInt(6), joint = random.nextInt(7) - 1;
                route(d, new End(s, -1), heading, new End(e, joint), .0625, space, PipeRoute.Limits.DEFAULT);
            }
            var rooms = new LinkedHashMap<Cell, Room>();
            row(rooms, 1, 3, 0, 0, EAST);
            var racks = space(rooms, java.util.Set.of());
            var cell = new Cell(2, 0, 0);
            for (int p = 0; p < 4; p++)
                for (int q = 0; q < 4; q++)
                    for (int through : new int[]{WEST, EAST}) {
                        var start = new End(cell, -1, -1, RackGeometry.option(p, RackGeometry.TIER), through, through ^ 1);
                        route(d, start, through, new End(cell, -1, -1, RackGeometry.option(q, RackGeometry.TIER), WEST, EAST), .0625, racks, PipeRoute.Limits.DEFAULT);
                        for (int joint = 0; joint < 6; joint++) route(d, start, through, new End(cell, joint), .0625, racks, PipeRoute.Limits.DEFAULT);
                    }
            for (int a = 0; a < 6; a++)
                for (int b = -1; b < 6; b++) route(d, new End(new Cell(0, 5, 0), a), a ^ 1, new End(new Cell(0, 5, 0), b), .0625, space, PipeRoute.Limits.DEFAULT);
        }
    }
}
