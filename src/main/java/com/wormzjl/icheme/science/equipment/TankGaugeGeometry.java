package com.wormzjl.icheme.science.equipment;

import java.util.List;

/** Shared proportions and outward-wound faces for the tank's shell-mounted level indicator. */
public final class TankGaugeGeometry {
    public static final double CHAMBER_RADIUS = .20, BAND_HALF = .12, SCALE_HALF = .26;
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

    /** Read-only closed unit cube, 0..1 on every axis. Scale/translate copies for each band, including its underside. */
    public static List<Mesh.Polygon> bandFaces() { return BAND_FACES; }

    /** Read-only hollow collar in the same unit frame; its top/bottom leave the liquid surface clear. */
    public static List<Mesh.Polygon> markerFaces() { return MARKER_FACES; }
}
