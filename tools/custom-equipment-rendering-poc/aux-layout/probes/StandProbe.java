package com.wormzjl.createcheme.science.equipment;

/** Temporary probe: collision boxes of the cells under a player at a world point near column A's structure. */
public class StandProbe {
    public static void main(String[] args) {
        var shape = EquipmentShape.column(3, ColumnTrays.simple(20, 10), ColumnOptions.DEFAULT, EquipmentShape.Heading.NORTH, 2);
        int ox = 321, oy = -60, oz = -3;
        double px = Double.parseDouble(args[0]), pz = Double.parseDouble(args[1]);
        var t = shape.transform();
        for (int cx = (int) Math.floor(px - .3 - ox); cx <= (int) Math.floor(px + .3 - ox); cx++)
            for (int cz = (int) Math.floor(pz - .3 - oz); cz <= (int) Math.floor(pz + .3 - oz); cz++)
                for (int cy = 2; cy <= 5; cy++) {
                    if (cx < 0 || cz < 0 || cx >= shape.sizeX() || cz >= shape.sizeZ()) continue;
                    var key = ShapeRasterizer.key(shape, cx, cy, cz);
                    for (var b : ShapeRasterizer.solid(key)) {
                        double x0 = ox + cx + b.x0() / 8.0, x1 = ox + cx + b.x1() / 8.0, z0 = oz + cz + b.z0() / 8.0, z1 = oz + cz + b.z1() / 8.0;
                        if (x1 <= px - .3 || x0 >= px + .3 || z1 <= pz - .3 || z0 >= pz + .3) continue;
                        double[] f = t.local(new double[]{cx + (b.x0() / 8.0 + b.x1() / 8.0) / 2, cy + (b.y0() / 8.0 + b.y1() / 8.0) / 2, cz + (b.z0() / 8.0 + b.z1() / 8.0) / 2});
                        System.out.printf("cell %d %d %d box x %.3f-%.3f y %.3f-%.3f z %.3f-%.3f (frame centre %.2f %.2f %.2f)%n", cx, cy, cz, x0, x1,
                                oy + cy + b.y0() / 8.0, oy + cy + b.y1() / 8.0, z0, z1, f[0], f[1], f[2]);
                    }
                }
    }
}
