package com.wormzjl.icheme.science.equipment;

import java.util.List;

/** Q9 (a): the W1 grid of short designs, unplaced nozzles before (no component design) and after (kettle only where needed). */
public final class Q9Probe {
    public static void main(String[] args) {
        int before = 0, after = 0, cases = 0;
        var sb = new StringBuilder();
        for (double d : new double[]{1, 2, 3, 4, 6, 8})
            for (int trays : new int[]{1, 2, 4, 6, 10})
                for (int variant = 0; variant < 3; variant++) {
                    ColumnTrays t = switch (variant) {
                        case 0 -> ColumnTrays.simple(trays, Math.max(1, trays / 2));
                        case 1 -> new ColumnTrays(trays, Math.max(1, trays / 2), List.of(), List.of(trays + 1), List.of());
                        default -> new ColumnTrays(trays, Math.max(1, trays / 2), List.of(Math.max(1, trays - 1)), List.of(trays + 1), List.of());
                    };
                    // Plain: a kettle (no steam to strip the bottom); with sump steam: stripped, no kettle.
                    var c = new ColumnComponents(ColumnComponents.Condenser.AIR_COOLER, ColumnComponents.Pump.STANDARD, List.of(), false, variant == 0);
                    var old = ColumnStructure.unplaced(EquipmentShape.column(d, t, EquipmentShape.Heading.NORTH, EquipmentShape.minimumSkirt(d)));
                    var now = ColumnStructure.unplaced(EquipmentShape.column(d, t.withComponents(c), EquipmentShape.Heading.NORTH, EquipmentShape.minimumSkirt(d)));
                    cases++;
                    if (!old.isEmpty()) before++;
                    if (!now.isEmpty()) after++;
                    if (!now.isEmpty() || !old.isEmpty())
                        sb.append("d=").append(d).append(" trays=").append(trays).append(" v=").append(variant).append(" before=").append(roles(old)).append(" after=").append(roles(now)).append('\n');
                }
        System.out.print(sb);
        System.out.println("cases " + cases + " before " + before + " after " + after);
    }

    private static List<String> roles(List<ColumnStructure.Unplaced> u) {
        return u.stream().map(x -> x.role().name()).toList();
    }
}
