package com.wormzjl.icheme.science.equipment;

import java.util.ArrayList;
import java.util.Locale;
import java.util.TreeMap;

/**
 * Scratch probe (option (C), 2026-10-27): every column nozzle's largest bore (NozzleBore) over the gate's columns and
 * NewSweep's buildable designs, by diameter and role where it is under a pipe's largest; whether the test's own
 * NozzleRoom finds each neck clear at that bore; and how long the search takes.
 */
public final class BoreProbe {
    public static void main(String[] args) {
        var shapes = new ArrayList<EquipmentShape>();
        for (var s : ProcessUnitTest.everySize()) if (s.kind() == EquipmentShape.Kind.COLUMN) shapes.add(s);
        for (var s : ShapeRasterizerTest.shapes()) if (s.kind() == EquipmentShape.Kind.COLUMN) shapes.add(s);
        int gateShapes = shapes.size();
        for (double d : new double[]{.5, 1, 1.5, 2, 3, 4, 6, 8, 15})
            for (int n : new int[]{1, 3, 5, 10, 15, 20, 30, 40, 64})
                for (String config : new String[]{"kettle", "kettle+steam+draw+PA", "steam+draw", "full"}) {
                    ColumnTrays t;
                    try {
                        t = NewSweep.trays(n, config);
                    } catch (IllegalArgumentException invalid) {
                        continue;
                    }
                    var s = EquipmentShape.column(d, t, EquipmentShape.Heading.NORTH, EquipmentShape.minimumSkirt(d));
                    if (ColumnDesign.misfit(s).isEmpty()) shapes.add(s);
                }
        var limited = new TreeMap<String, Integer>();
        int nozzles = 0, under = 0, notClear = 0;
        long started = System.nanoTime();
        for (int i = 0; i < shapes.size(); i++) {
            var s = shapes.get(i);
            for (int k = 0; k < s.nozzles().size(); k++) {
                nozzles++;
                double bore = NozzleBore.largest(s, k);
                var nozzle = s.nozzles().get(k);
                boolean clear = ProcessUnitTest.NozzleRoom.clear(s, k, PipeSizes.outside(bore));
                if (!clear) {
                    notClear++;
                    System.out.println("NOT CLEAR " + (i < gateShapes ? "gate " : "") + s.diameter() + " " + s.trays() + " " + nozzle.role() + " bore " + bore
                            + " design od " + nozzle.defaultOd());
                }
                if (bore < PipeSizes.MAX_BORE - 1e-9) {
                    under++;
                    limited.merge(String.format(Locale.ROOT, "%sd %4.1f %-16s bore %.3f", i < gateShapes ? "gate " : "", s.diameter(), nozzle.role(), bore), 1, Integer::sum);
                }
            }
        }
        double ms = (System.nanoTime() - started) / 1e6;
        limited.forEach((k, v) -> System.out.println(String.format("%4d  %s", v, k)));
        System.out.println(String.format(Locale.ROOT, "SUMMARY shapes %d (gate %d), nozzles %d, under 0.8 m %d, not clear at own largest %d, %.0f ms (%.2f ms per nozzle)",
                shapes.size(), gateShapes, nozzles, under, notClear, ms, ms / nozzles));
    }
}
