package com.wormzjl.createcheme.science.equipment;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Temporary probe: heap taken by one column geometry with its auxiliaries. */
class AuxMemoryProbeTest {
    @Test
    void probe() throws Exception {
        var out = new StringBuilder("max heap " + Runtime.getRuntime().maxMemory() / 1e6 + " MB\n");
        var four = new ColumnTrays(40, 20, List.of(10, 25), List.of(41), List.of(new ColumnTrays.Pumparound(5, 3), new ColumnTrays.Pumparound(12, 10),
                new ColumnTrays.Pumparound(22, 20), new ColumnTrays.Pumparound(33, 31)));
        for (double d : new double[]{3, 15}) {
            for (int k = 0; k < 2; k++) {
                System.gc();
                long before = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                var shape = EquipmentShape.column(d, four, new ColumnOptions(ColumnOptions.Condenser.AIR, k == 1, ColumnOptions.Reboiler.KETTLE), EquipmentShape.Heading.NORTH, 6);
                var layout = ShapeRasterizer.layout(shape);
                System.gc();
                long after = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                out.append(String.format("D %.0f: %d aux facets, %d geometry facets, %d shell cells, heap +%.1f MB%n", d, ColumnAuxiliaries.of(shape).mesh().facets().size(), geometryFacets(shape),
                        layout.shell().size(), (after - before) / 1e6));
            }
        }
        Files.writeString(Path.of("build/tmp/aux-probe/memory.txt"), out);
    }

    private static int geometryFacets(EquipmentShape shape) throws Exception {
        var cache = ShapeRasterizer.class.getDeclaredField("GEOMETRIES");
        cache.setAccessible(true);
        var g = ((java.util.Map<?, ?>) cache.get(null)).get(shape);
        var mesh = g.getClass().getDeclaredField("mesh");
        mesh.setAccessible(true);
        return ((Mesh) mesh.get(g)).facets().size();
    }
}
