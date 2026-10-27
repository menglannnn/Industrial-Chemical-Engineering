package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.equipment.EquipmentShape.Heading;
import com.wormzjl.createcheme.science.equipment.EquipmentShape.Kind;
import com.wormzjl.createcheme.science.equipment.EquipmentShape.Orientation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The cells where pipes meet a shape's nozzles (the cell beyond each flange face): pairs that coincide, share a face
 * (their pipes join), or touch at an edge or corner. Every unit kind at every size (shortest and longest), drums with
 * their default connections. Args: none; prints a table per kind and the touching pairs.
 */
public class PortSpacing {
    public static void main(String[] args) {
        var shapes = new ArrayList<EquipmentShape>();
        for (var kind : List.of(Kind.EXCHANGER, Kind.KETTLE, Kind.AIR_COOLER, Kind.EJECTOR, Kind.PUMP, Kind.COMPRESSOR, Kind.VERTICAL_HEATER, Kind.CABIN_HEATER))
            for (double d = ProcessUnit.minDiameter(kind); d <= ProcessUnit.maxDiameter(kind) + 1e-9; d += ProcessUnit.diameterStep(kind)) {
                double size = ProcessUnit.snap(kind, d);
                int lo = ProcessUnit.minLength(kind, size), hi = ProcessUnit.maxLength(kind, size);
                for (int length : lo == hi ? new int[]{lo} : new int[]{lo, (lo + hi) / 2, hi})
                    try {
                        shapes.add(EquipmentShape.unit(kind, size, length, Heading.NORTH, ProcessUnit.defaultSupport(kind, size, length)));
                    } catch (IllegalArgumentException e) {
                        // a size the constructor does not offer
                    }
            }
        for (double d = .5; d <= 10; d += .5)
            for (var o : List.of(Orientation.VERTICAL, Orientation.HORIZONTAL))
                try {
                    shapes.add(EquipmentShape.drum(d, Math.max(1, Math.round(3 * d)), o, Heading.NORTH, EquipmentShape.defaultSupport(d, o)));
                } catch (IllegalArgumentException e) {
                    // not offered
                }
        shapes.add(EquipmentShape.column(3, ColumnTrays.simple(12, 6), Heading.SOUTH, 2));
        shapes.add(EquipmentShape.column(6, new ColumnTrays(40, 20, List.of(10, 25), List.of(41), List.of(new ColumnTrays.Pumparound(12, 10), new ColumnTrays.Pumparound(30, 28))), Heading.NORTH, 4));
        shapes.add(EquipmentShape.column(1, ColumnTrays.simple(20, 10), Heading.EAST, 1));
        shapes.add(EquipmentShape.column(15, ColumnTrays.simple(30, 15), Heading.WEST, 16));
        shapes.add(EquipmentShape.column(4, new ColumnTrays(30, 15, List.of(8, 9, 20, 21), List.of(31), List.of(new ColumnTrays.Pumparound(11, 10), new ColumnTrays.Pumparound(23, 22))), Heading.NORTH, 3));
        Map<String, int[]> table = new TreeMap<>();
        var lines = new ArrayList<String>();
        for (var shape : shapes) {
            var ports = ShapeRasterizer.ports(shape);
            var nozzles = shape.nozzles();
            String key = shape.kind() + (shape.kind() == Kind.DRUM ? " " + shape.orientation() : "");
            int[] row = table.computeIfAbsent(key, k -> new int[4]);
            row[0]++;
            boolean any = false;
            for (int i = 0; i < ports.size(); i++)
                for (int j = i + 1; j < ports.size(); j++) {
                    var a = ports.get(i);
                    var b = ports.get(j);
                    int dx = Math.abs(a.cell().x() + a.dx() - b.cell().x() - b.dx()), dy = Math.abs(a.cell().y() + a.dy() - b.cell().y() - b.dy()), dz = Math.abs(a.cell().z() + a.dz() - b.cell().z() - b.dz());
                    int cheb = Math.max(dx, Math.max(dy, dz)), man = dx + dy + dz;
                    if (cheb >= 2) continue;
                    String what = man == 0 ? "same" : man == 1 ? "face" : "touch";
                    row[man == 0 ? 1 : man == 1 ? 2 : 3]++;
                    any = true;
                    lines.add(String.format("%s %.2f x %.0f: %s %s and %s %s (%s)", shape.kind(), shape.diameter(), shape.tangentLength(), nozzles.get(i).role(),
                            side(a), nozzles.get(j).role(), side(b), what));
                }
        }
        System.out.println("| kind | shapes | same cell | face | edge or corner |");
        table.forEach((k, r) -> System.out.println("| " + k + " | " + r[0] + " | " + r[1] + " | " + r[2] + " | " + r[3] + " |"));
        lines.stream().forEach(System.out::println);
        System.out.println(lines.size() + " pairs");
    }

    private static String side(ShapeRasterizer.Port p) {
        return "[" + (p.cell().x() + p.dx()) + "," + (p.cell().y() + p.dy()) + "," + (p.cell().z() + p.dz()) + "]";
    }
}
