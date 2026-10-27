package com.wormzjl.icheme.science.equipment;

import java.util.*;

/** W3 scratch: what the band rule draws on every flow-marked nozzle of the layout table's shapes, at the design size. */
public final class BandReport {
    public static void main(String[] args) {
        int marked = 0, none = 0, partial = 0;
        var lengths = new ArrayList<Double>();
        var notes = new ArrayList<String>();
        long t0 = System.nanoTime();
        for (var shape : EquipmentLayoutGoldenTest.shapes()) {
            var nozzles = shape.nozzles();
            for (int k = 0; k < nozzles.size(); k++) {
                var n = nozzles.get(k);
                var flow = PortMarkings.flow(shape.kind(), n.role());
                if (flow == PortMarkings.Flow.NONE) continue;
                marked++;
                var mesh = new Mesh();
                PortMarkings.band(mesh, shape, k, 0, flow);
                int heads = 0; double low = 1e9, high = -1e9;
                for (var f : mesh.facets()) {
                    if (f.positions().size() == 3) heads++;
                    for (var p : f.positions()) { double s = (p[n.axis()] - n.base()[n.axis()]) * n.sign(); low = Math.min(low, s); high = Math.max(high, s); }
                }
                String id = String.format(Locale.ROOT, "%s d=%.2f %s od=%.3f", shape.kind(), shape.diameter(), n.role(), n.defaultOd());
                if (heads == 0) { none++; notes.add("none    " + id); continue; }
                lengths.add(high - low);
                if (heads < 4) { partial++; notes.add(heads + " sides " + id + String.format(Locale.ROOT, " len %.3f", high - low)); }
            }
        }
        long ms = (System.nanoTime() - t0) / 1_000_000;
        Collections.sort(lengths);
        int m = lengths.size();
        System.out.printf(Locale.ROOT, "marked %d, banded %d, none %d, fewer than 4 sides %d; band length p10 %.3f p50 %.3f p90 %.3f min %.3f; %d ms%n",
                marked, m, none, partial, lengths.get(m / 10), lengths.get(m / 2), lengths.get(9 * m / 10), lengths.get(0), ms);
        long short20 = lengths.stream().filter(l -> l < .2).count();
        System.out.println("bands shorter than 0.20 m: " + short20);
        notes.forEach(System.out::println);
    }
}
