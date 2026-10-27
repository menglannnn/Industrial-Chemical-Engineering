import com.wormzjl.createcheme.science.equipment.*;
import java.util.*;

/** The heap a vessel's geometry keeps on a dedicated server ({@link ShapeRasterizer#serverOnly}): layout, keys and solids. */
public final class ServerHeap {
    static long used() {
        var rt = Runtime.getRuntime();
        for (int i = 0; i < 4; i++) System.gc();
        return rt.totalMemory() - rt.freeMemory();
    }

    public static void main(String[] args) {
        if (args.length > 0) ShapeRasterizer.serverOnly();
        var N = EquipmentShape.Heading.NORTH;
        var cases = new LinkedHashMap<String, EquipmentShape>();
        cases.put("drum 3x9 standing", EquipmentShape.drum(3, 9, EquipmentShape.Orientation.VERTICAL, N, 2));
        cases.put("column 6m 30 trays", EquipmentShape.column(6, ColumnTrays.simple(30, 15), N, 4));
        cases.put("AC 3x6 bank 2x2", EquipmentShape.unit(EquipmentShape.Kind.AIR_COOLER, 3, 6, N, 4, new ProcessUnit.Bank(2, 2)));
        ShapeRasterizer.layout(EquipmentShape.drum(2, 4, EquipmentShape.Orientation.VERTICAL, N, 2));
        ShapeRasterizer.layout(EquipmentShape.unit(EquipmentShape.Kind.AIR_COOLER, 1.5, 3, N, 4));
        var keep = new ArrayList<Object>();
        for (var e : cases.entrySet()) {
            long before = used(), t0 = System.nanoTime();
            var layout = ShapeRasterizer.layout(e.getValue());
            int boxes = 0;
            for (var c : layout.shell()) boxes += ShapeRasterizer.solid(ShapeRasterizer.key(e.getValue(), c.x(), c.y(), c.z())).size();
            long ms = (System.nanoTime() - t0) / 1_000_000;
            keep.add(layout);
            System.out.printf("%s %-19s geometry heap %5.2f MB, layout + solids %d ms (%d boxes)%n", args.length > 0 ? "server" : "client", e.getKey(), (used() - before) / 1e6, ms, boxes);
        }
        System.out.println(keep.size());
    }
}
