import com.wormzjl.createcheme.science.equipment.EquipmentShape;
import com.wormzjl.createcheme.science.equipment.EquipmentShape.Heading;
import com.wormzjl.createcheme.science.equipment.EquipmentShape.Kind;
import com.wormzjl.createcheme.science.equipment.ShapeRasterizer;
import java.util.HashSet;

/** Blocks, shell cells, chunk quads and layout time of the machines and heaters at their smallest and largest. */
public final class MachineCounts {
    public static void main(String[] args) {
        var cases = java.util.List.of(EquipmentShape.unit(Kind.PUMP, .05, 1, Heading.NORTH, 0), EquipmentShape.unit(Kind.PUMP, .2, 1, Heading.NORTH, 0),
                EquipmentShape.unit(Kind.PUMP, .4, 1, Heading.NORTH, 0), EquipmentShape.unit(Kind.COMPRESSOR, .15, 2, Heading.NORTH, 1),
                EquipmentShape.unit(Kind.COMPRESSOR, .45, 4, Heading.NORTH, 2), EquipmentShape.unit(Kind.COMPRESSOR, .8, 6, Heading.NORTH, 3),
                EquipmentShape.unit(Kind.VERTICAL_HEATER, 2.5, 4, Heading.NORTH, 3), EquipmentShape.unit(Kind.VERTICAL_HEATER, 4, 8, Heading.NORTH, 3),
                EquipmentShape.unit(Kind.VERTICAL_HEATER, 10, 27, Heading.NORTH, 3), EquipmentShape.unit(Kind.CABIN_HEATER, 4, 6, Heading.NORTH, 3),
                EquipmentShape.unit(Kind.CABIN_HEATER, 6, 12, Heading.NORTH, 3), EquipmentShape.unit(Kind.CABIN_HEATER, 12, 40, Heading.NORTH, 3));
        System.out.println("| unit | box | shell | quads | layout ms |");
        for (var shape : cases) {
            long t0 = System.nanoTime();
            var layout = ShapeRasterizer.layout(shape);
            long quads = 0;
            for (var cell : layout.shell())
                for (var p : ShapeRasterizer.surface(ShapeRasterizer.key(shape, cell.x(), cell.y(), cell.z()))) quads += (p.size() - 1) / 2;
            long t1 = System.nanoTime();
            System.out.printf("| %s %.2f x %.0f | %dx%dx%d | %d | %d | %.0f |%n", shape.kind(), shape.diameter(), shape.tangentLength(), shape.sizeX(), shape.sizeY(), shape.sizeZ(),
                    layout.shell().size(), quads, (t1 - t0) / 1e6);
        }
    }
}
