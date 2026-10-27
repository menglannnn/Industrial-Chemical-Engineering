package com.wormzjl.icheme.science.equipment;

import java.util.List;

/** The review column's trays and side nozzles: each nozzle's wanted height against the row it got. */
public final class SteamProbe {
    public static void main(String[] args) {
        var c = new ColumnComponents(ColumnComponents.Condenser.AIR_COOLER, ColumnComponents.Pump.STANDARD, List.of(ColumnComponents.Pump.STANDARD), true, true);
        var t = new ColumnTrays(20, 12, List.of(9), List.of(21), List.of(new ColumnTrays.Pumparound(7, 5)), c);
        double d = 2;
        var s = EquipmentShape.column(d, t, EquipmentShape.Heading.NORTH, EquipmentShape.minimumSkirt(d));
        double yB = s.bottomTangent();
        System.out.printf("yB %.2f yT %.2f sump %.2f box %d%n", yB, s.topTangent(), ColumnTrays.bottomSpace(d), s.sizeY());
        for (int k = 1; k <= t.trays() + 1; k++) System.out.printf("  stage %2d at %.2f%n", k, yB + t.trayHeight(d, k));
        var plan = ColumnStructure.plan(s);
        for (var p : plan.platforms()) System.out.printf("platform level %d from %.0f to %.0f deg ring %s%n", p.level(), Math.toDegrees(p.from()), Math.toDegrees(p.to()), p.ring());
        for (var l : plan.ladders()) System.out.printf("ladder at %.0f deg from %.2f to %.2f%n", Math.toDegrees(l.angle()), l.base(), l.top());
        System.out.printf("davit %.0f deg top %.2f%n", Math.toDegrees(plan.davitAngle()), plan.davitTop());
        for (var n : s.nozzles()) System.out.printf("%-16s base y %.2f axis %d sign %d od %.2f%n", n.role(), n.base()[1], n.axis(), n.sign(), n.defaultOd());
    }
}
