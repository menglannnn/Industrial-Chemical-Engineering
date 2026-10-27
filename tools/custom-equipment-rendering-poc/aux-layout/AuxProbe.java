package com.wormzjl.createcheme.science.equipment;

import java.util.List;

public class AuxProbe {
    public static void main(String[] args) {
        for (double d : new double[]{1.5, 3, 6}) {
            int trays = d == 1.5 ? 15 : d == 3 ? 25 : 35;
            var t = new ColumnTrays(trays, trays / 2, List.of(), d == 3 ? List.of() : List.of(),
                    d == 1.5 ? List.of() : List.of(new ColumnTrays.Pumparound(6, 4), new ColumnTrays.Pumparound(trays / 2 + 4, trays / 2 + 2)));
            var s = EquipmentShape.column(d, t, EquipmentShape.Heading.NORTH, EquipmentShape.defaultSupport(d, EquipmentShape.Orientation.VERTICAL));
            var plan = ColumnStructure.plan(s);
            System.out.printf("D %.1f trays %d skirt %d T/T %.0f bT %.2f tT %.2f top %.2f outerR %.2f width %d box %d x %d x %d deckR %.2f davitTop %.2f%n",
                    d, trays, EquipmentShape.defaultSupport(d, EquipmentShape.Orientation.VERTICAL), s.tangentLength(), s.bottomTangent(), s.topTangent(), s.top(), s.outerRadius(), s.width(), s.sizeX(), s.sizeY(), s.sizeZ(), plan.deckRadius(), plan.davitTop());
            for (var p : plan.platforms()) System.out.printf("  deck %d ring %b from %.0f to %.0f manway %b%n", p.level(), p.ring(), Math.toDegrees(p.from()), Math.toDegrees(p.to()), p.manway());
            for (var n : s.nozzles()) System.out.printf("  nozzle %s y %.2f axis %d sign %d od %.2f face %.2f,%.2f,%.2f%n", n.role(), n.base()[1], n.axis(), n.sign(), n.maxOd(), n.face()[0], n.face()[1], n.face()[2]);
            System.out.printf("  axis %.2f sump %.2f tray1 %.2f%n", s.axis(), ColumnTrays.bottomSpace(d), t.trayHeight(d, 1));
        }
    }
}
