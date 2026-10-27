package com.wormzjl.createcheme.science.equipment;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/** Temporary probe: sizes of columns with their auxiliaries (not a gate; removed before the batch closes). */
class AuxProbeTest {
    @Test
    void probe() throws Exception {
        var out = new StringBuilder();
        var crude = new ColumnTrays(25, 13, List.of(8), List.of(26), List.of(new ColumnTrays.Pumparound(6, 4), new ColumnTrays.Pumparound(15, 13)));
        for (var options : List.of(ColumnOptions.DEFAULT, new ColumnOptions(ColumnOptions.Condenser.AIR, true, ColumnOptions.Reboiler.KETTLE),
                new ColumnOptions(ColumnOptions.Condenser.WATER, true, ColumnOptions.Reboiler.KETTLE)))
            for (double d : new double[]{.5, 1, 3, 6, 10, 15}) {
                int skirt = Math.max(EquipmentShape.defaultSupport(d, EquipmentShape.Orientation.VERTICAL), EquipmentShape.minimumSkirt(d, options));
                long t0 = System.nanoTime();
                var shape = EquipmentShape.column(d, crude, options, EquipmentShape.Heading.NORTH, skirt);
                var aux = ColumnAuxiliaries.of(shape);
                long t1 = System.nanoTime();
                var layout = ShapeRasterizer.layout(shape);
                long t2 = System.nanoTime();
                out.append(String.format("D %.1f %s skirt %d: box %d x %d x %d, column %s, aux %s, facets %d, shell %d, interior %d, aux %.0f ms, layout %.0f ms%n", d, options, skirt,
                        shape.sizeX(), shape.sizeY(), shape.sizeZ(), Arrays.toString(shape.columnBox()), Arrays.toString(round(aux.bounds())), aux.mesh().facets().size(),
                        layout.shell().size(), layout.interior().size(), (t1 - t0) / 1e6, (t2 - t1) / 1e6));
            }
        Files.createDirectories(Path.of("build/tmp/aux-probe"));
        Files.writeString(Path.of("build/tmp/aux-probe/sizes.txt"), out);
    }

    private static double[] round(double[] v) {
        var r = new double[v.length];
        for (int i = 0; i < v.length; i++) r[i] = Math.round(v[i] * 100) / 100.0;
        return r;
    }
}
