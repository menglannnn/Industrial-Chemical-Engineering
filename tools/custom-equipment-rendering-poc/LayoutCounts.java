import com.wormzjl.createcheme.science.equipment.ColumnTrays;
import com.wormzjl.createcheme.science.equipment.EquipmentShape;
import com.wormzjl.createcheme.science.equipment.ShapeRasterizer;
import java.util.HashSet;

/** Prints blocks, shell cells, unique keys, chunk quads and collision boxes of the production rasterizer for the budget cases. */
public final class LayoutCounts {
    public static void main(String[] args) {
        var vertical = EquipmentShape.Orientation.VERTICAL;
        var horizontal = EquipmentShape.Orientation.HORIZONTAL;
        var north = EquipmentShape.Heading.NORTH;
        // The feasibility review's bare budget cases, supported drums, then columns (batch step of 2026-10-16: access
        // structure, cladding, nozzles): the check scene's 3 m and 6 m columns and the largest, 15 m on its tallest skirt.
        var cases = java.util.List.of(new EquipmentShape(2, 3), new EquipmentShape(3, 9), new EquipmentShape(4, 5), new EquipmentShape(2, 30),
                new EquipmentShape(6, 40), new EquipmentShape(10, 60), EquipmentShape.drum(3, 9, vertical, north, 2), EquipmentShape.drum(6, 40, vertical, north, 3),
                EquipmentShape.drum(10, 60, vertical, north, 4), EquipmentShape.drum(3, 9, horizontal, EquipmentShape.Heading.EAST, 1),
                EquipmentShape.drum(4, 12, horizontal, north, 2),
                EquipmentShape.column(3, ColumnTrays.simple(20, 10), north, 3), EquipmentShape.column(6, ColumnTrays.simple(30, 15), north, 4),
                EquipmentShape.column(8, new ColumnTrays(40, 37, java.util.List.of(10, 20, 30), java.util.List.of(41),
                        java.util.List.of(new ColumnTrays.Pumparound(12, 10), new ColumnTrays.Pumparound(22, 20), new ColumnTrays.Pumparound(32, 30))), north, 2),
                EquipmentShape.column(15, ColumnTrays.simple(64, 32), north, 16));
        System.out.println("| D x T/T, support | blocks | shell | keys | quads | boxes/cell mean, max | layout ms |");
        for (var shape : cases) {
            long t0 = System.nanoTime();
            var layout = ShapeRasterizer.layout(shape);
            long t1 = System.nanoTime();
            var keys = new HashSet<ShapeRasterizer.CellKey>();
            long quads = 0, boxes = 0, maxBoxes = 0;
            for (var cell : layout.shell()) {
                var key = ShapeRasterizer.key(shape, cell.x(), cell.y(), cell.z());
                keys.add(key);
                for (var p : ShapeRasterizer.surface(key)) quads += (p.size() - 1) / 2;
                int b = ShapeRasterizer.solid(key).size();
                boxes += b;
                maxBoxes = Math.max(maxBoxes, b);
            }
            System.out.printf("| %.1f x %.0f, %s %d | %d | %d | %d | %d | %.1f, %d | %.1f |%n", shape.diameter(), shape.tangentLength(),
                    shape.column() ? "column" : shape.orientation().name().toLowerCase(), shape.support(), layout.shell().size() + layout.interior().size(),
                    layout.shell().size(), keys.size(), quads, boxes / (double) layout.shell().size(), maxBoxes, (t1 - t0) / 1e6);
        }
    }
}
