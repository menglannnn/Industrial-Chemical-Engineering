package com.wormzjl.icheme.science.equipment;

import java.util.List;

/** Box sizes and nozzle pipe blocks of the review plant's parts. */
public final class PortProbe {
    public static void main(String[] args) {
        var c = new ColumnComponents(ColumnComponents.Condenser.AIR_COOLER, ColumnComponents.Pump.STANDARD, List.of(ColumnComponents.Pump.STANDARD), true, true);
        var trays = new ColumnTrays(20, 12, List.of(9), List.of(21), List.of(new ColumnTrays.Pumparound(7, 5)), c);
        var column = EquipmentShape.column(2, trays, EquipmentShape.Heading.NORTH, EquipmentShape.minimumSkirt(2));
        System.out.println("misfit " + ColumnDesign.misfit(column));
        show("COLUMN", column);
        for (int support : new int[]{2, 4, 5})
            show("DRUM s" + support, EquipmentShape.drum(1.5, 4, EquipmentShape.Orientation.HORIZONTAL, EquipmentShape.Heading.NORTH, support,
                    EquipmentShape.defaultConnections(1.5, 4, EquipmentShape.Orientation.HORIZONTAL, support, true), true));
        for (var kind : List.of(EquipmentShape.Kind.EJECTOR, EquipmentShape.Kind.AIR_COOLER, EquipmentShape.Kind.KETTLE, EquipmentShape.Kind.EXCHANGER, EquipmentShape.Kind.PUMP)) {
            var d = UnitDesign.of(kind);
            double size = d.minSize();
            int length = d.defaultLength(size);
            show(kind.name(), EquipmentShape.designed(kind, size, length, EquipmentShape.Heading.NORTH, d.defaultSupport(size, length), ProcessUnit.Bank.ONE));
        }
    }

    static void show(String name, EquipmentShape s) {
        System.out.println(name + " box " + s.sizeX() + "x" + s.sizeY() + "x" + s.sizeZ());
        for (var p : ShapeRasterizer.ports(s)) {
            var n = s.nozzles().get(p.nozzle());
            System.out.println("  " + n.role() + " pipe " + (p.cell().x() + p.dx()) + "," + (p.cell().y() + p.dy()) + "," + (p.cell().z() + p.dz()) + " dir " + p.dx() + "," + p.dy() + "," + p.dz());
        }
    }
}
