package com.wormzjl.createcheme.science.equipment;

/**
 * Where an equipment constructor builds its vessel, relative to itself and the way it faces. With the default offsets
 * the vessel's box stands on the constructor's level, directly in front of it and centred on its line. {@code forward}
 * is the distance from the constructor to the near face of the box (1 = adjacent), {@code right} shifts the box to the
 * constructor's right, {@code up} raises it. For an even size across the line the box centre lies half a block to the
 * left. The vessel's {@link Form} is relative to the constructor too.
 */
public record VesselPlacement(int forward, int right, int up) {
    public static final int MAX_FORWARD = 16, MAX_SHIFT = 16;
    public static final VesselPlacement DEFAULT = new VesselPlacement(1, 0, 0);

    /** Standing, or lying with its axis along the constructor's facing (lengthwise) or across it (crosswise). */
    public enum Form {
        VERTICAL, LENGTHWISE, CROSSWISE;

        public Form next() {
            return values()[(ordinal() + 1) % values().length];
        }

        /** The vessel's orientation in the world for a constructor facing the horizontal unit step (stepX, stepZ). */
        public EquipmentShape.Orientation orientation(int stepX, int stepZ) {
            if (Math.abs(stepX) + Math.abs(stepZ) != 1) throw new IllegalArgumentException("Facing must be a horizontal unit step");
            boolean facingX = stepX != 0;
            return switch (this) {
                case VERTICAL -> EquipmentShape.Orientation.VERTICAL;
                case LENGTHWISE -> facingX ? EquipmentShape.Orientation.ALONG_X : EquipmentShape.Orientation.ALONG_Z;
                case CROSSWISE -> facingX ? EquipmentShape.Orientation.ALONG_Z : EquipmentShape.Orientation.ALONG_X;
            };
        }
    }

    public VesselPlacement {
        if (forward < 1 || forward > MAX_FORWARD || Math.abs(right) > MAX_SHIFT || Math.abs(up) > MAX_SHIFT)
            throw new IllegalArgumentException("Offsets must be forward 1 to " + MAX_FORWARD + ", right and up within ±" + MAX_SHIFT);
    }

    /**
     * Lower north-west corner of a box of sizeX × sizeZ cells for a constructor at (x, y, z) facing the horizontal unit
     * step (stepX, stepZ).
     */
    public int[] origin(int x, int y, int z, int stepX, int stepZ, int sizeX, int sizeZ) {
        if (Math.abs(stepX) + Math.abs(stepZ) != 1) throw new IllegalArgumentException("Facing must be a horizontal unit step");
        // The constructor's right is its facing turned clockwise seen from above (north -> east). On each axis the box
        // extends either along the facing (from the near face) or across it (centred), never both.
        int rightX = -stepZ, rightZ = stepX;
        return new int[]{x + stepX * forward + rightX * right + Math.min(0, stepX * (sizeX - 1)) + across(rightX, sizeX), y + up,
                z + stepZ * forward + rightZ * right + Math.min(0, stepZ * (sizeZ - 1)) + across(rightZ, sizeZ)};
    }

    /** Offset of the box's lower corner from the line through the constructor, on an axis the line crosses ({@code right} ≠ 0). */
    private static int across(int right, int size) {
        int low = -(size / 2), high = size - 1 - size / 2;
        return Math.min(right * low, right * high);
    }
}
