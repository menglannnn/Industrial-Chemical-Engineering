package com.wormzjl.createcheme.science.equipment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VesselPlacementTest {
    /** North, east, south, west as (stepX, stepZ). */
    private static final int[][] FACINGS = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};

    @Test
    void boxStandsInFrontOfTheConstructorCentredOnItsLine() {
        int x = 5, y = 64, z = -7;
        for (int[] f : FACINGS)
            for (int sizeX = 1; sizeX <= 10; sizeX++)
                for (int sizeZ = 1; sizeZ <= 10; sizeZ += 3)
                    for (int forward = 1; forward <= 3; forward++)
                        for (int right = -2; right <= 2; right++)
                            for (int up = -1; up <= 1; up++) {
                                int[] o = new VesselPlacement(forward, right, up).origin(x, y, z, f[0], f[1], sizeX, sizeZ);
                                String at = "facing " + f[0] + "," + f[1] + " size " + sizeX + "x" + sizeZ + " offsets " + forward + "," + right + "," + up;
                                assertEquals(y + up, o[1], at);
                                assertFalse(x >= o[0] && x < o[0] + sizeX && z >= o[2] && z < o[2] + sizeZ, at + ": the box covers the constructor");
                                // Distance along the facing from the constructor to the box's near and far faces.
                                int near = Integer.MAX_VALUE, far = Integer.MIN_VALUE;
                                double lateral = 0;
                                int rightX = -f[1], rightZ = f[0], depth = f[0] != 0 ? sizeX : sizeZ, across = f[0] != 0 ? sizeZ : sizeX;
                                for (int a = 0; a < sizeX; a++)
                                    for (int b = 0; b < sizeZ; b++) {
                                        int dx = o[0] + a - x, dz = o[2] + b - z;
                                        int along = dx * f[0] + dz * f[1];
                                        near = Math.min(near, along);
                                        far = Math.max(far, along);
                                        lateral += dx * rightX + dz * rightZ;
                                    }
                                assertEquals(forward, near, at);
                                assertEquals(forward + depth - 1, far, at);
                                assertEquals(right - (across % 2 == 0 ? .5 : 0), lateral / (sizeX * sizeZ), 1e-12, at);
                            }
    }

    @Test
    void formTurnsWithTheConstructor() {
        assertEquals(EquipmentShape.Orientation.VERTICAL, VesselPlacement.Form.VERTICAL.orientation());
        assertEquals(EquipmentShape.Orientation.HORIZONTAL, VesselPlacement.Form.LENGTHWISE.orientation());
        assertEquals(EquipmentShape.Orientation.HORIZONTAL, VesselPlacement.Form.CROSSWISE.orientation());
        for (int[] f : FACINGS) {
            // A standing drum's front and a lengthwise drum's front head face back to the constructor; a crosswise drum's
            // front head faces the constructor's right. A standing drum's bottom outlet leaves its skirt through the access
            // opening towards the constructor (owner report 2026-10-16); a new drum's side connection faces the right of a
            // player looking from the constructor, the facing turned clockwise seen from above.
            var back = EquipmentShape.Heading.of(-f[0], -f[1]);
            assertEquals(back, VesselPlacement.Form.VERTICAL.front(f[0], f[1]));
            assertEquals(back, VesselPlacement.Form.LENGTHWISE.front(f[0], f[1]));
            assertEquals(EquipmentShape.Heading.of(-f[1], f[0]), VesselPlacement.Form.CROSSWISE.front(f[0], f[1]));
            var standing = EquipmentShape.drum(3, 4, EquipmentShape.Orientation.VERTICAL, back, 2);
            for (var port : ShapeRasterizer.ports(standing)) {
                int[] d = {port.dx(), port.dy(), port.dz()};
                if (port.port() == EquipmentShape.PortClass.LIQUID) assertArrayEquals(new int[]{-f[0], 0, -f[1]}, d);
                if (port.port() == EquipmentShape.PortClass.BULK) assertArrayEquals(new int[]{-f[1], 0, f[0]}, d);
            }
        }
        // Facing south, a lengthwise drum's box runs away from the constructor and a crosswise one across its line.
        var lengthwise = EquipmentShape.drum(2, 6, EquipmentShape.Orientation.HORIZONTAL, VesselPlacement.Form.LENGTHWISE.front(0, 1), 1);
        var crosswise = EquipmentShape.drum(2, 6, EquipmentShape.Orientation.HORIZONTAL, VesselPlacement.Form.CROSSWISE.front(0, 1), 1);
        assertTrue(lengthwise.sizeZ() > lengthwise.sizeX() && crosswise.sizeX() > crosswise.sizeZ());
        // A column stands with its piping side (its nozzles) back to the constructor.
        assertEquals(EquipmentShape.Orientation.VERTICAL, VesselPlacement.Form.COLUMN.orientation());
        assertEquals(EquipmentShape.Heading.NORTH, VesselPlacement.Form.COLUMN.front(0, 1));
        assertThrows(IllegalArgumentException.class, () -> VesselPlacement.Form.LENGTHWISE.front(1, 1));
    }

    @Test
    void rejectsOffsetsOutOfRange() {
        for (int[] bad : new int[][]{{0, 0, 0}, {17, 0, 0}, {1, 17, 0}, {1, -17, 0}, {1, 0, 17}, {1, 0, -17}})
            assertThrows(IllegalArgumentException.class, () -> new VesselPlacement(bad[0], bad[1], bad[2]));
        assertThrows(IllegalArgumentException.class, () -> VesselPlacement.DEFAULT.origin(0, 0, 0, 1, 1, 3, 3));
        assertThrows(IllegalArgumentException.class, () -> VesselPlacement.DEFAULT.origin(0, 0, 0, 0, 0, 3, 3));
    }
}
