package com.wormzjl.icheme.science.equipment;

import java.util.List;

/** Shared proportions and outward-wound faces for the tank's shell-mounted level indicator. */
public final class TankGaugeGeometry {
    public static final double CHAMBER_RADIUS = .20, BAND_HALF = .12, SCALE_HALF = .26;
    public static final double MARKER_HALF = .16, MARKER_HEIGHT = .07;
    private static final List<Mesh.Polygon> BAND_FACES;

    static {
        var mesh = new Mesh();
        mesh.box(new double[]{0, 0, 0}, new double[]{1, 1, 1}, Mesh.ALL_FACES, Mesh.Material.OPENING);
        BAND_FACES = List.copyOf(mesh.cell(0, 0, 0));
    }

    private TankGaugeGeometry() {}

    /** Read-only closed unit cube, 0..1 on every axis. Scale/translate copies for each band, including its underside. */
    public static List<Mesh.Polygon> bandFaces() { return BAND_FACES; }
}
