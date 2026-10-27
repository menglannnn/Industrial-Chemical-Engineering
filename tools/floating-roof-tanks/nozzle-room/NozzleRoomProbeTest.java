package com.wormzjl.icheme.science.equipment;

import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * Probe (batch 2026-10-21-floating-roof-tanks, nozzle roots; detached before the merge): per kind and role, the least
 * room a nozzle's neck has over every gated size against its current largest size. Prints only.
 */
class NozzleRoomProbeTest {
    @Test
    void printRoom() {
        var shapes = new ArrayList<>(ProcessUnitTest.everySize());
        shapes.addAll(ShapeRasterizerTest.shapes());
        var rows = new TreeMap<String, double[]>();
        for (var shape : shapes)
            for (int k = 0; k < shape.nozzles().size(); k++) {
                var nozzle = shape.nozzles().get(k);
                String key = shape.kind() + (shape.drum() ? "/" + shape.orientation() : "") + " " + nozzle.role();
                double room = NozzleRoom.room(shape, k);
                var row = rows.computeIfAbsent(key, x -> new double[]{9, 9, 0, 0});
                row[0] = Math.min(row[0], room);
                row[1] = Math.min(row[1], nozzle.maxOd());
                row[2] = Math.max(row[2], nozzle.maxOd());
                if (!NozzleRoom.clear(shape, k, nozzle.maxOd())) row[3]++;
            }
        System.out.println("NOZZLE-ROOM kind role | least room | maxOd least..most | sizes whose maxOd neck is not clear");
        rows.forEach((key, r) -> System.out.printf(Locale.ROOT, "NOZZLE-ROOM %-40s %.2f  %.2f..%.2f  %d%n", key, r[0], r[1], r[2], (int) r[3]));
    }
}
