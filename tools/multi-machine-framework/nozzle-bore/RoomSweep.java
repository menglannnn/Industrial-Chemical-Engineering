package com.wormzjl.icheme.science.equipment;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Scratch sweep (batch 2026-10-26-multi-machine-framework, final lane): one line per column design, NewSweep's 315 F3
 * designs and the ProcessUnitTest gate's columns, with the refusal or every nozzle's base and facing, then the gate's
 * failures (a neck at a pipe's largest not clear, the accepted 0.5 m and 3 m top-outlet touches left out). Run it once on
 * the branch's classes and once with a patched ColumnStructure ahead on the classpath, and diff the outputs.
 */
public final class RoomSweep {
    public static void main(String[] args) {
        int designs = 0, refused = 0, gate = 0;
        var lines = new ArrayList<String>();
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
                    designs++;
                    var misfit = ColumnDesign.misfit(s);
                    String key = "sweep d " + d + " n " + n + " " + config;
                    if (misfit.isPresent()) {
                        refused++;
                        lines.add(key + " REFUSED " + misfit.get());
                        continue;
                    }
                    int g = gate(s);
                    gate += g;
                    lines.add(key + " " + nozzles(s) + (g > 0 ? " GATE " + g : ""));
                }
        var shapes = new ArrayList<>(ProcessUnitTest.everySize());
        shapes.addAll(ShapeRasterizerTest.shapes());
        int gateShapes = 0, gateFail = 0;
        for (var s : shapes) {
            if (s.kind() != EquipmentShape.Kind.COLUMN) continue;
            gateShapes++;
            int g = gate(s);
            gateFail += g;
            lines.add("gate " + s.diameter() + " " + s.trays() + " " + s.front() + " " + s.support() + " " + nozzles(s) + (g > 0 ? " GATE " + g : ""));
        }
        lines.forEach(System.out::println);
        System.out.println("SUMMARY sweep designs " + designs + " refused " + refused + " gate " + gate + "; gate columns " + gateShapes + " failing necks " + gateFail);
    }

    /** The column's necks at a pipe's largest that are not clear, but the touches ProcessUnitTest accepts for columns. */
    static int gate(EquipmentShape s) {
        int out = 0;
        for (int k = 0; k < s.nozzles().size(); k++) {
            var role = s.nozzles().get(k).role();
            if (s.diameter() == .5 && role != EquipmentShape.Role.VAPOUR_OUTLET && role != EquipmentShape.Role.BOTTOMS || s.diameter() == 3. && role == EquipmentShape.Role.VAPOUR_OUTLET) continue;
            if (!ProcessUnitTest.NozzleRoom.clear(s, k, PipeSizes.MAX_OUTSIDE)) out++;
        }
        return out;
    }

    static String nozzles(EquipmentShape s) {
        var b = new StringBuilder();
        for (var n : s.nozzles())
            b.append(n.role()).append('@').append(String.format(Locale.ROOT, "%.2f,%.2f,%.2f", n.base()[0], n.base()[1], n.base()[2])).append('/').append(n.axis()).append(n.sign() > 0 ? '+' : '-').append(' ');
        return b.toString().trim();
    }
}
