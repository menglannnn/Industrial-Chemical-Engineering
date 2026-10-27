package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.equipment.EquipmentShape.Heading;
import com.wormzjl.createcheme.science.equipment.EquipmentShape.Kind;
import java.util.HashSet;

/**
 * Collision at the ends of a heater's walkways, as the game builds it: 1/8-block voxels whose centres lie in the solid.
 * For each deck and end: the filled voxel centres in the end fence slab (0.1 m at the end, between the deck's inner edge
 * and the side rail, from the walking surface to fence height) and in the floor's last 0.3 m. Args: kind size length support heading.
 */
public final class WalkwayEnds {
    public static void main(String[] args) {
        var shape = EquipmentShape.unit(Kind.valueOf(args[0]), Double.parseDouble(args[1]), Double.parseDouble(args[2]), Heading.valueOf(args[4]), Integer.parseInt(args[3]));
        var shell = new HashSet<>(ShapeRasterizer.layout(shape).shell());
        int k = 0;
        for (var d : ProcessUnit.of(shape).decks()) {
            double len = d.run().length(), deck = d.level() + Access.DECK;
            for (int end = 0; end < 2; end++) {
                double s0 = end == 0 ? 0 : len - .1, s1 = end == 0 ? .1 : len, f0 = end == 0 ? 0 : len - .3, f1 = end == 0 ? .3 : len;
                int[] fence = count(shape, shell, d.run().at(s0, Access.GAP + .01, deck + .05), d.run().at(s1, d.width() - Access.RAIL_BAND - .01, deck + Access.FENCE));
                int[] floor = count(shape, shell, d.run().at(f0, Access.GAP + .01, d.level()), d.run().at(f1, d.width() - .01, deck));
                System.out.printf("deck %d level %.2f len %.2f end %d: fence %d/%d filled (%d outside shell), floor %d/%d%n", k, d.level(), len, end, fence[0], fence[1], fence[2], floor[0], floor[1]);
            }
            k++;
        }
    }

    /** Filled, total and filled-but-not-in-a-shell-cell voxel centres in the frame box between two corners. */
    static int[] count(EquipmentShape shape, HashSet<ShapeRasterizer.Cell> shell, double[] a, double[] b) {
        var p = shape.transform().point(a);
        var q = shape.transform().point(b);
        int filled = 0, total = 0, stray = 0;
        for (int x = (int) Math.ceil(Math.min(p[0], q[0]) * 8 - .5); (x + .5) / 8 <= Math.max(p[0], q[0]); x++)
            for (int y = (int) Math.ceil(Math.min(p[1], q[1]) * 8 - .5); (y + .5) / 8 <= Math.max(p[1], q[1]); y++)
                for (int z = (int) Math.ceil(Math.min(p[2], q[2]) * 8 - .5); (z + .5) / 8 <= Math.max(p[2], q[2]); z++) {
                    total++;
                    double cx = (x + .5) / 8, cy = (y + .5) / 8, cz = (z + .5) / 8;
                    if (ShapeRasterizer.inside(shape, cx, cy, cz)) {
                        filled++;
                        if (!shell.contains(new ShapeRasterizer.Cell((int) Math.floor(cx), (int) Math.floor(cy), (int) Math.floor(cz)))) stray++;
                    }
                }
        return new int[]{filled, total, stray};
    }
}
