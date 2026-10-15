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
        for (int[] f : FACINGS) {
            var along = f[0] != 0 ? EquipmentShape.Orientation.ALONG_X : EquipmentShape.Orientation.ALONG_Z;
            var across = f[0] != 0 ? EquipmentShape.Orientation.ALONG_Z : EquipmentShape.Orientation.ALONG_X;
            assertEquals(EquipmentShape.Orientation.VERTICAL, VesselPlacement.Form.VERTICAL.orientation(f[0], f[1]));
            assertEquals(along, VesselPlacement.Form.LENGTHWISE.orientation(f[0], f[1]));
            assertEquals(across, VesselPlacement.Form.CROSSWISE.orientation(f[0], f[1]));
        }
        // Facing south, a lengthwise drum's box runs away from the constructor and a crosswise one across its line.
        var lengthwise = new EquipmentShape(2, 6, VesselPlacement.Form.LENGTHWISE.orientation(0, 1), 1);
        var crosswise = new EquipmentShape(2, 6, VesselPlacement.Form.CROSSWISE.orientation(0, 1), 1);
        assertTrue(lengthwise.sizeZ() > lengthwise.sizeX() && crosswise.sizeX() > crosswise.sizeZ());
        assertEquals(VesselPlacement.Form.VERTICAL, VesselPlacement.Form.CROSSWISE.next());
        assertThrows(IllegalArgumentException.class, () -> VesselPlacement.Form.LENGTHWISE.orientation(1, 1));
    }

    @Test
    void rejectsOffsetsOutOfRange() {
        for (int[] bad : new int[][]{{0, 0, 0}, {17, 0, 0}, {1, 17, 0}, {1, -17, 0}, {1, 0, 17}, {1, 0, -17}})
            assertThrows(IllegalArgumentException.class, () -> new VesselPlacement(bad[0], bad[1], bad[2]));
        assertThrows(IllegalArgumentException.class, () -> VesselPlacement.DEFAULT.origin(0, 0, 0, 1, 1, 3, 3));
        assertThrows(IllegalArgumentException.class, () -> VesselPlacement.DEFAULT.origin(0, 0, 0, 0, 0, 3, 3));
    }
}
