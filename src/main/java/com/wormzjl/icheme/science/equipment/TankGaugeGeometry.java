package com.wormzjl.icheme.science.equipment;

import java.util.List;

/** Shared proportions and outward-wound faces for the tank's shell-mounted level indicator. */
public final class TankGaugeGeometry {
    public static final double CHAMBER_RADIUS = .20, BAND_HALF = .12, SCALE_HALF = .26;
    /** Clear sleeve between the liquid and the external collar: 3 mm liquid gap, 2 mm collar gap. */
    public static final double GLASS_HALF = .123, END_HEIGHT = .12;
    public static final double MARKER_HALF = .16, MARKER_HEIGHT = .07;
    /** The collar slips around the liquid/glass column with 5 mm clearance. */
    public static final double MARKER_INNER_HALF = BAND_HALF + .005;
    private static final List<Mesh.Polygon> BAND_FACES;
    private static final List<Mesh.Polygon> MARKER_FACES;

    static {
        var mesh = new Mesh();
        mesh.box(new double[]{0, 0, 0}, new double[]{1, 1, 1}, Mesh.ALL_FACES, Mesh.Material.OPENING);
        BAND_FACES = List.copyOf(mesh.cell(0, 0, 0));
        // A collar has a hole: its end faces must never cover the liquid cap, even at a clipped gauge limit.
        var collar = new Mesh();
        double inset = (1 - MARKER_INNER_HALF / MARKER_HALF) / 2;
        collar.box(new double[]{0, 0, 0}, new double[]{1, 1, inset}, Mesh.ALL_FACES, Mesh.Material.OPENING);
        collar.box(new double[]{0, 0, 1 - inset}, new double[]{1, 1, 1}, Mesh.ALL_FACES, Mesh.Material.OPENING);
        collar.box(new double[]{0, 0, inset}, new double[]{inset, 1, 1 - inset}, Mesh.ALL_FACES, Mesh.Material.OPENING);
        collar.box(new double[]{1 - inset, 0, inset}, new double[]{1, 1, 1 - inset}, Mesh.ALL_FACES, Mesh.Material.OPENING);
        MARKER_FACES = List.copyOf(collar.cell(0, 0, 0));
    }

    private TankGaugeGeometry() {}

    /** Four continuous panes, with one full sprite per face rather than metre-tiled glass borders. */
    static void sleeve(Mesh mesh, double x, double z, double low, double high) {
        double[] lo = {x - GLASS_HALF, low, z - GLASS_HALF}, hi = {x + GLASS_HALF, high, z + GLASS_HALF};
        var vertical = new Mesh.Uv(lo, new double[]{0, 1, 0}, 0, high - low);
        for (int axis : new int[]{0, 2}) for (int sign : new int[]{0, 1}) {
            int across = 2 - axis;
            var corners = new java.util.ArrayList<double[]>();
            for (int[] corner : new int[][]{{0, 0}, {1, 0}, {1, 1}, {0, 1}}) {
                double[] p = lo.clone();p[axis] = sign == 0 ? lo[axis] : hi[axis];
                p[across] = corner[0] == 0 ? lo[across] : hi[across];p[1] = corner[1] == 0 ? low : high;
                corners.add(p);
            }
            double[] horizontal = new double[3];horizontal[across] = 1;
            mesh.add(corners, new double[]{x, (low + high) / 2, z}, new Mesh.Flat(), Mesh.Material.GLASS,
                    new Mesh.Uv(lo, horizontal, 0, 2 * GLASS_HALF), vertical);
        }
    }

    /** Read-only closed unit cube, 0..1 on every axis. Scale/translate copies for each band, including its underside. */
    public static List<Mesh.Polygon> bandFaces() { return BAND_FACES; }

    /** Read-only hollow collar in the same unit frame; its top/bottom leave the liquid surface clear. */
    public static List<Mesh.Polygon> markerFaces() { return MARKER_FACES; }
}
