import com.wormzjl.createcheme.science.equipment.EquipmentShape;
import com.wormzjl.createcheme.science.equipment.EquipmentShape.Heading;
import com.wormzjl.createcheme.science.equipment.EquipmentShape.Kind;
import com.wormzjl.createcheme.science.equipment.ShapeRasterizer;
import java.util.TreeMap;

/** Chunk quads by material of a few machines and heaters. */
public final class MaterialCounts {
    public static void main(String[] args) {
        var cases = java.util.List.of(EquipmentShape.unit(Kind.PUMP, .05, 1, Heading.NORTH, 0), EquipmentShape.unit(Kind.COMPRESSOR, .8, 6, Heading.NORTH, 3),
                EquipmentShape.unit(Kind.VERTICAL_HEATER, 10, 27, Heading.NORTH, 3), EquipmentShape.unit(Kind.CABIN_HEATER, 12, 40, Heading.NORTH, 3));
        for (var shape : cases) {
            var counts = new TreeMap<String, Long>();
            var layout = ShapeRasterizer.layout(shape);
            for (var cell : layout.shell())
                for (var p : ShapeRasterizer.surface(ShapeRasterizer.key(shape, cell.x(), cell.y(), cell.z()))) counts.merge(p.material().name(), (long) (p.size() - 1) / 2, Long::sum);
            System.out.println(shape.kind() + " " + shape.diameter() + " x " + shape.tangentLength() + ": " + counts);
        }
    }
}
