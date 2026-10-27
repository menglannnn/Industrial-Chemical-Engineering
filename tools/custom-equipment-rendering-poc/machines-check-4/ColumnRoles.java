package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.equipment.EquipmentShape.Heading;
import java.util.List;

/** Each test column's nozzle roles and pipe rows, to check no side nozzle was dropped for want of a free row. */
public class ColumnRoles {
    public static void main(String[] args) {
        var columns = List.of(EquipmentShape.column(3, ColumnTrays.simple(12, 6), Heading.SOUTH, 2),
                EquipmentShape.column(6, new ColumnTrays(40, 20, List.of(10, 25), List.of(41), List.of(new ColumnTrays.Pumparound(12, 10), new ColumnTrays.Pumparound(30, 28))), Heading.NORTH, 4),
                EquipmentShape.column(1, ColumnTrays.simple(20, 10), Heading.EAST, 1),
                EquipmentShape.column(15, ColumnTrays.simple(30, 15), Heading.WEST, 16),
                EquipmentShape.column(4, new ColumnTrays(30, 15, List.of(8, 9, 20, 21), List.of(31), List.of(new ColumnTrays.Pumparound(11, 10), new ColumnTrays.Pumparound(23, 22))), Heading.NORTH, 3),
                EquipmentShape.column(1, new ColumnTrays(8, 4, List.of(2, 3, 5, 6), List.of(9), List.of(new ColumnTrays.Pumparound(4, 3))), Heading.NORTH, 1));
        for (var c : columns) {
            var sb = new StringBuilder(String.format("D %.0f, %d trays: %d nozzles:", c.diameter(), c.trays().trays(), c.nozzles().size()));
            for (var n : c.nozzles()) sb.append(" ").append(n.role()).append("@").append(n.pipeBlock()[1]).append(n.axis() == 1 ? "" : (n.sign() > 0 ? "+" : "-") + n.axis());
            System.out.println(sb);
        }
    }
}
