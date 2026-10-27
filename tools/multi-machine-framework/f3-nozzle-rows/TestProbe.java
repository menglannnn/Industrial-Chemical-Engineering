package com.wormzjl.icheme.science.equipment;

import com.wormzjl.icheme.science.equipment.EquipmentShape.Heading;
import com.wormzjl.icheme.science.equipment.EquipmentShape.Role;
import java.util.List;

/** What the column structure tests see under the new layout: each test column's side nozzles, unplaced ones and turns. */
public final class TestProbe {
    public static void main(String[] args) {
        var columns = List.of(
                EquipmentShape.column(3, ColumnTrays.simple(12, 6), Heading.SOUTH, 4),
                EquipmentShape.column(6, new ColumnTrays(40, 20, List.of(10, 25), List.of(41),
                        List.of(new ColumnTrays.Pumparound(12, 10), new ColumnTrays.Pumparound(30, 28))), Heading.NORTH, 5),
                EquipmentShape.column(1, ColumnTrays.simple(20, 10), Heading.EAST, 3),
                EquipmentShape.column(15, ColumnTrays.simple(30, 15), Heading.WEST, 16),
                EquipmentShape.column(4, new ColumnTrays(30, 15, List.of(8, 9, 20, 21), List.of(31),
                        List.of(new ColumnTrays.Pumparound(11, 10), new ColumnTrays.Pumparound(23, 22))), Heading.NORTH, 4),
                EquipmentShape.column(2, new ColumnTrays(4, 2, List.of(1, 2, 3, 4), List.of(4, 5), List.of()), Heading.NORTH, 3));
        for (var s : columns) {
            System.out.println(s.diameter() + " m " + s.trays().trays() + " trays, nozzles " + s.nozzles().size() + ", misfit " + ColumnDesign.misfit(s).orElse("-"));
            for (var n : ColumnDesign.sides(s)) System.out.println("   " + n.role() + " " + n.index() + " home " + n.home() + " side " + n.side() + (n.placed() ? " y " + n.nozzle().base()[1] : " UNPLACED"));
            var plan = ColumnStructure.plan(s);
            for (var p : plan.platforms()) System.out.printf("   platform %d %.0f-%.0f ring %s manway %s%n", p.level(), Math.toDegrees(p.from()), Math.toDegrees(p.to()), p.ring(), p.manway());
            for (var l : plan.ladders()) System.out.printf("   ladder %.0f %.2f-%.2f%n", Math.toDegrees(l.angle()), l.base(), l.top());
        }
        var small = columns.get(0);
        for (int side = 0; side < 4; side++) {
            var turned = EquipmentShape.column(3, ColumnTrays.simple(12, 6).withTurns(List.of(new ColumnTrays.Turn(Role.REFLUX, 1, side))), Heading.SOUTH, 4);
            var reflux = ColumnDesign.sides(turned).getFirst();
            System.out.println("reflux turned to " + side + ": side " + reflux.side() + " turned " + reflux.turned() + " placed " + reflux.placed() + " misfit " + ColumnDesign.misfit(turned).orElse("-"));
        }
        var plain = new ColumnTrays(6, 3, List.of(), List.of(7), List.of());
        for (Boolean kettle : new Boolean[]{null, true, false}) {
            var t = kettle == null ? plain : plain.withComponents(new ColumnComponents(ColumnComponents.Condenser.AIR_COOLER, ColumnComponents.Pump.STANDARD, List.of(), false, kettle));
            var s = EquipmentShape.column(2, t, Heading.NORTH, EquipmentShape.minimumSkirt(2));
            System.out.println("kettle " + kettle + ": " + s.nozzles().stream().map(n -> n.role() + "@" + n.base()[1] + "/" + n.axis() + n.sign()).toList() + " unplaced " + ColumnStructure.unplaced(s).stream().map(ColumnDesign.SideNozzle::role).toList());
        }
    }
}
