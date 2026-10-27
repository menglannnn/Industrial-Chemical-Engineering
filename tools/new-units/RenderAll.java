package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.equipment.EquipmentShape.Kind;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * RenderAll <out dir> <views> [KIND...]: renders each unit kind (all when none named) at its smallest, middle and largest
 * size, each at its default second size and support, with {@link UnitRender} to {@code <out>/<kind>-<size>-<view>.png}:
 * the before and after pictures of a change to the kit (kit consolidation 2026-10-18).
 */
public class RenderAll {
    public static void main(String[] args) throws Exception {
        String out = args[0], views = args[1];
        List<Kind> kinds = new ArrayList<>();
        for (int i = 2; i < args.length; i++) kinds.add(Kind.valueOf(args[i]));
        if (kinds.isEmpty()) for (var kind : Kind.values()) if (kind.unit()) kinds.add(kind);
        for (var kind : kinds) {
            double lo = ProcessUnit.minDiameter(kind), hi = ProcessUnit.maxDiameter(kind);
            for (double size : new double[]{lo, ProcessUnit.snap(kind, (lo + hi) / 2), hi}) {
                int length = ProcessUnit.defaultLength(kind, size), support = ProcessUnit.defaultSupport(kind, size, length);
                String name = kind.name().toLowerCase(Locale.ROOT) + "-" + UnitDesign.size(size);
                UnitRender.main(new String[]{kind.name(), Double.toString(size), Integer.toString(length), Integer.toString(support), out + "/" + name, views});
            }
        }
    }
}
