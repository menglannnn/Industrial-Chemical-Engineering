package com.wormzjl.icheme.science.equipment;

import com.wormzjl.icheme.science.equipment.EquipmentShape.Heading;
import com.wormzjl.icheme.science.equipment.EquipmentShape.Kind;

public class CellCount {
    public static void main(String[] args) {
        int want = Integer.parseInt(args[0]);
        for (double d = 2; d <= 40; d += .5)
            for (int len = 2; len <= 30; len++) {
                EquipmentShape shape;
                try {
                    shape = EquipmentShape.storage(Kind.TANK, d, len, Heading.NORTH, 0);
                } catch (RuntimeException e) {
                    continue;
                }
                var layout = ShapeRasterizer.layout(shape);
                int n = layout.shell().size() + layout.interior().size();
                if (n == want) System.out.println("d=" + d + " len=" + len + " shell=" + layout.shell().size() + " interior=" + layout.interior().size() + " box=" + shape.sizeX() + "x" + shape.sizeY() + "x" + shape.sizeZ());
            }
    }
}
