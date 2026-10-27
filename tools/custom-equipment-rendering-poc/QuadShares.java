import com.wormzjl.createcheme.science.equipment.*;
import java.util.*;

/**
 * Per typical vessel: chunk quads by material over every shell cell, quads of the distinct cell keys (the client's baked
 * quad cache), the heap the rasterizer's geometry takes (measured), and the time to cut every cell's surface.
 */
public final class QuadShares {
    static long used() {
        var rt = Runtime.getRuntime();
        for (int i = 0; i < 4; i++) System.gc();
        return rt.totalMemory() - rt.freeMemory();
    }

    public static void main(String[] args) {
        var N = EquipmentShape.Heading.NORTH;
        var cases = new LinkedHashMap<String, EquipmentShape>();
        cases.put("drum 3x9 standing", EquipmentShape.drum(3, 9, EquipmentShape.Orientation.VERTICAL, N, 2));
        cases.put("drum 3x9 lying", EquipmentShape.drum(3, 9, EquipmentShape.Orientation.HORIZONTAL, EquipmentShape.Heading.EAST, 1));
        cases.put("column 3m 20 trays", EquipmentShape.column(3, ColumnTrays.simple(20, 10), N, 3));
        cases.put("column 6m 30 trays", EquipmentShape.column(6, ColumnTrays.simple(30, 15), N, 4));
        cases.put("AC 3x6 bank 2x2", EquipmentShape.unit(EquipmentShape.Kind.AIR_COOLER, 3, 6, N, 4, new ProcessUnit.Bank(2, 2)));
        // Warm the classes and parts on a throwaway shape so the first case's heap is its own.
        ShapeRasterizer.layout(EquipmentShape.drum(2, 4, EquipmentShape.Orientation.VERTICAL, N, 2));
        ShapeRasterizer.layout(EquipmentShape.unit(EquipmentShape.Kind.AIR_COOLER, 1.5, 3, N, 4));
        var keep = new ArrayList<Object>();
        for (var e : cases.entrySet()) {
            var shape = e.getValue();
            long before = used();
            var layout = ShapeRasterizer.layout(shape);
            long geometry = used() - before;
            keep.add(layout);
            var perMaterial = new EnumMap<Mesh.Material, long[]>(Mesh.Material.class);
            var keys = new HashMap<ShapeRasterizer.CellKey, Long>();
            long quads = 0;
            for (int pass = 0; pass < 3; pass++) {
                long t0 = System.nanoTime();
                quads = 0;
                for (var cell : layout.shell()) {
                    var key = ShapeRasterizer.key(shape, cell.x(), cell.y(), cell.z());
                    long q = 0;
                    for (var p : ShapeRasterizer.surface(key)) {
                        int n = (p.size() - 1) / 2;
                        q += n;
                        if (pass == 0) {
                            var c = perMaterial.computeIfAbsent(p.material(), m -> new long[2]);
                            c[0] += n;
                            if (p.size() == 3) c[1]++;
                        }
                    }
                    quads += q;
                    keys.put(key, q);
                }
                if (pass == 2) {
                    long distinct = keys.values().stream().mapToLong(Long::longValue).sum();
                    System.out.printf("%-19s cells %4d keys %4d | quads %6d, distinct-key quads %6d | geometry heap %5.1f MB, baked cache ~%4.1f MB, chunk buffers %4.1f MB | cut all cells %4.0f ms%n",
                            e.getKey(), layout.shell().size(), keys.size(), quads, distinct, geometry / 1e6, distinct * 180 / 1e6, quads * 128 / 1e6, (System.nanoTime() - t0) / 1e6);
                    final long total = quads;
                    var sb = new StringBuilder("    ");
                    perMaterial.entrySet().stream().sorted((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0])).limit(9)
                            .forEach(m -> sb.append(String.format("%s %d (%.0f%%%s)  ", m.getKey(), m.getValue()[0], 100.0 * m.getValue()[0] / total,
                                    m.getValue()[1] > 0 ? ", " + m.getValue()[1] + " tri" : "")));
                    System.out.println(sb);
                }
            }
        }
        System.out.println(keep.size());
    }
}
