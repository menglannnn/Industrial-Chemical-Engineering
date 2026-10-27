package com.wormzjl.createcheme.science.equipment;

/** Prints where the check drum's fluid node cell lies against its bottom head and how the layout classes the cells on its axis. */
public class NodeCell {
    public static void main(String[] args) {
        var shape = EquipmentShape.drum(3, 6, EquipmentShape.Orientation.VERTICAL, EquipmentShape.Heading.NORTH, 2);
        System.out.println("yB " + shape.bottomTangent() + " head " + shape.headDepth() + " pole " + (shape.bottomTangent() - shape.headDepth()) + " node " + ShapeRasterizer.fluidCell(shape));
        var layout = ShapeRasterizer.layout(shape);
        for (int y = 0; y < 4; y++) {
            var c = new ShapeRasterizer.Cell(2, y, 2);
            System.out.println(c + " shell " + layout.shell().contains(c) + " interior " + layout.interior().contains(c));
        }
    }
}
