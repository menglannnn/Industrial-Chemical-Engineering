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

    /**
     * A drum standing, or lying with its axis along the constructor's facing (lengthwise) or across it (crosswise); or a
     * distillation column (owner request 2026-10-16).
     */
    public enum Form {
        VERTICAL, LENGTHWISE, CROSSWISE, COLUMN;

        public EquipmentShape.Orientation orientation() {
            return this == LENGTHWISE || this == CROSSWISE ? EquipmentShape.Orientation.HORIZONTAL : EquipmentShape.Orientation.VERTICAL;
        }

        public boolean column() {
            return this == COLUMN;
        }

        /**
         * The vessel's front for a constructor facing the horizontal unit step (stepX, stepZ): back towards the constructor
         * (a standing drum's liquid outlet, a lengthwise drum's front head, a column's piping side), or a crosswise drum's
         * front head to the constructor's right.
         */
        public EquipmentShape.Heading front(int stepX, int stepZ) {
            if (Math.abs(stepX) + Math.abs(stepZ) != 1) throw new IllegalArgumentException("Facing must be a horizontal unit step");
            return this == CROSSWISE ? EquipmentShape.Heading.of(-stepZ, stepX) : EquipmentShape.Heading.of(-stepX, -stepZ);
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
        return origin(x, y, z, stepX, stepZ, sizeX, sizeZ, new int[]{0, 0, sizeX, sizeZ});
    }

    /**
     * The same for a box whose part {@code body} = {x0, z0, x1, z1} (box cells, upper bounds exclusive) is what the
     * constructor builds in front of itself: across its line it centres that part (a column's own square, its auxiliaries
     * standing beside it), along its line the whole box starts at the near face.
     */
    public int[] origin(int x, int y, int z, int stepX, int stepZ, int sizeX, int sizeZ, int[] body) {
        if (Math.abs(stepX) + Math.abs(stepZ) != 1) throw new IllegalArgumentException("Facing must be a horizontal unit step");
        // The constructor's right is its facing turned clockwise seen from above (north -> east). On each axis the box
        // extends either along the facing (from the near face) or across it (centred on the body), never both.
        int rightX = -stepZ, rightZ = stepX;
        int acrossX = stepX == 0 ? across(rightX, body[2] - body[0]) - body[0] : 0, acrossZ = stepZ == 0 ? across(rightZ, body[3] - body[1]) - body[1] : 0;
        return new int[]{x + stepX * forward + rightX * right + Math.min(0, stepX * (sizeX - 1)) + acrossX, y + up,
                z + stepZ * forward + rightZ * right + Math.min(0, stepZ * (sizeZ - 1)) + acrossZ};
    }

    /** Offset of the box's lower corner from the line through the constructor, on an axis the line crosses ({@code right} ≠ 0). */
    private static int across(int right, int size) {
        int low = -(size / 2), high = size - 1 - size / 2;
        return Math.min(right * low, right * high);
    }
}
