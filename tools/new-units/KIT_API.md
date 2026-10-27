# Kit API after the consolidation's kit batch (2026-10-18)

Package `science/equipment`. Frame: y up from grade, front -z, +x the constructor's left, metres; angles from +x towards
+z. Read each method's javadoc in the source before using it; this list is the index.

## Mesh
- `hoop(Frame f, double rIn, double rOut, double z0, double z1, int n, Material m)`: rim and both faces, no bore.
- `head(Frame f, double r, double at, double depth, int n, int bands, Material m)`: a 2:1 head with `bands` (the old
  head = bands max(3, n/4)).
- `bend(Frame f, double bend, double r, double a0, double a1, int segments, int n, Material m)`: a mitred bend round
  f.origin, square to f.axis, angles from f.ref towards axis × ref.

## PipeSizes
- `static double od(double want, double least, double most)`: a nozzle size to the nearest 0.05 m.

## Access (static)
- `PLAYER`, `MIN_DECK` (1.45), `int MAX_CLIMB = 9`, `NARROW_BAND = .13`.
- Deck parts: `BRACKETS = 1, RAILED = 2, WALL_EDGE = 4, ALL_PARTS, NARROW = 8` (narrow closed-end fences).
- `double clearance(double wall)`: the cage clearance.
- `List<Integer> climbs(int base, List<Integer> wanted, int top)`: every stop after base, top last, rests at most 9 m.
- `List<double[]> pieces(double length, double... gaps)`.
- `deck(Mesh, Run, double level, double width, int parts, double... railGaps)`: toe board only over rail pieces.
- `davit(Mesh, double cx, double cz, double t, double r, double base, double top, double reach)`,
  `double[] around(cx, cz, r, t, y)`.

## ProcessUnit
- Boxes and steel: `box(double[] lo, double[] hi, int faces, Material, boolean solid)`;
  `post(x, z, y0, y1, section)` (foot hidden, solid); `beam(lo, hi)` (top hidden, solid).
- Lines: `pipe(double r, Material, boolean solid, double[]... points)` (axis-aligned legs, long-radius elbows,
  overlapping solids); `line(double r, int sides, Material, double[]...)`;
  `double[] elbow(start, d0, d1, bend, r, n, segments, Material)` (returns its end);
  `prism(Frame, r, z0, z1, n, Material)` (cut at the texture period).
- Nozzles: `static Nozzle end(Role, double[] base, int axis, int sign, double maxOd, double defaultOd)` (`front`
  delegates); `static radialSide(Role, x, y, z, r, n, int sign, maxOd, defaultOd)` (-1 front, +1 back).
- `saddle(z, x, y, r, w, double floor)`; `static clamp(v, lo, hi)`; `static double[] point(int axis, at, p, q)`.
- Solids: `Solid.rod(c, axis, lo, hi, radius, headLo, headHi)`, `Solid.cone(c, axis, lo, hi, r, rHi, headLo, headHi)`.
- `transition(int axis, p0, p1, q0, q1, at, cp, cq, radius, to, n, Material, inside)` ((p, q) the other two axes in
  rising order).
- `brace(double[] p /*x,z*/, double[] q, low, high, width, tier)`: X braces in equal tiers of at most `tier`, ends
  hidden; `BRACE_TIER = 3`.
- `verticalLadder(x, z, wall, t, base, top, walkThrough)`.
- `enum MotorDetail {FULL, FINNED, PLAIN}`; `motor(Frame f, r, z0, z1, shaft, floor, MotorDetail)`: lying on any
  horizontal axis (feet and pedestal to `floor`) or standing (mount flange, fins, terminal box at -z).
- Stack: `record Breeching(x0, x1, z0, z1)` with `.neck(base, od)`; `static stackTop(neck, roof)` = max(neck + 6,
  2 roof); `stack(x, z, od, base, top, Material paint, Breeching | null, boolean damper)` (a band every 8 m);
  `band(Frame, r, y, n, Material)`.
- Access parts:
  - `platform(Run, level, width, int parts, closedStart, closedEnd, double... gaps)` (without RAILED: end rails only);
  - `record Ring(level, ladder, from, to)`; `List<List<Access.Arc>> ringAccess(x, z, wall, width, base, List<Ring>)`
    (a whole ring is open at its ladder only; pieces under 0.8 m left out);
  - `squareDeck(x0, x1, z0, z1, level, int axis, row, half, double[] centres, body, int sides, int parts, double[][]
    gaps)` (gaps per edge, order -z, +x, +z, -x); `fill(x, z, half, body, sides, level)`;
  - `concreteTable(x0, x1, z0, z1, top, slab, List<double[]{x0, z0, x1, z1}> holes, column, List<double[]{x, z}>
    posts)` on 0.15 m pads (`PAD`);
  - `record Tower(List<double[]> openings, double edge, double from, double to)`;
    `Tower stairTower(x, z, int axis, int sign, int side, double[] stops, boolean beams)`: flights 0.9 m on lanes
    1.0 m apart (`TOWER_FLIGHT`, `TOWER_LANE`), landings 1.2 m (`TOWER_LANDING`), flights of at most 3.6 m
    (`TOWER_RISE`); `openings` per stop {lo, hi} along the axis, cut them into the decks' rails.
- Davit and hydro parts: `davit(x, z, t, r, base, top, reach)`; `pedestal(x, z, apothem, h)`,
  `static pedestalCorner(apothem)`; `baseRing(x, z, r, y, diameter, n, double[]... openings)`;
  `skirt(x, z, r, thick, y0, y1, top, n, openings...)`; `studs(x, z, circle, low, high, p)` and
  `studs(..., int count, d)`; `heavyJoint(Frame, od, station)`; `static outward(t)`, `static tangential(t)`.
- `table` is now `steelTable`. Every old signature still works and delegates (removed at integration).

## Integration changes (2026-10-18, INTEGRATION_REPORT.md)
- `bend(..., int n, boolean ends, Material)`: a tangent polygon, its end pieces half pieces square to the tangent
  (`ends` false: the caller's legs carry them). `pipe(r, int sides, Material, solid, points...)` beside the default.
- `post(x, z, y0, y1, section, boolean plate)` (`BASE_PLATE` .03 thick, .06 margin); `stack(x, z, od, int n, ...)`.
- `TowerPlan towerPlan(double... stops)` (near, far, from, to, end(j), opening(j), static foot(at, sign, station));
  `Tower stairTower(TowerPlan, x, z, axis, sign, side, beams)`; `Access.Line.gap(double[] span)`.
- `List<Double> climb(Access.Line run, base, top, lower, upper, post)`: one caged climb, a rest past 9 m.
- `squareDeck(..., double[][] gaps, double[] hole)` ({strip side -1/+1, from, to}: a ladder's hole in that strip).
- Fittings: `valve(x, y, z, r, out)`, `thermowell(x, z, r, t, y)`, `double[] reducer(start, dir, r0, r1, length,
  sides, Material, solid)`; the hydroprocessing reactors' shared parts in `HydroprocessingParts`.
- Removed: `transition` without axis, `motor(f, x, y, ...)`, `verticalLadder(a, ...)`, `band` without paint,
  `radialSide` without sign, `saddle` without floor, `Access.deck` without parts, `PipeSizes.largestUnder`. Kept as
  defaults: `box(lo, hi, Material, solid)`, `Solid.rod(6)`, `Solid.cone(7)`, `Mesh.head(6)`, `line(3)`, `pipe(4)`,
  `front`, `platform` without parts, the 4-argument `brace` (free-standing braces with their ends drawn).
- `stair()`'s side fences are now 0.13 m bands (a 0.6 m player fits a 0.9 m flight at 1/8-block collision).
