package com.wormzjl.icheme.science.equipment;

/** Why a 0.5 m column's feed finds no row: its band, the platform levels, and each row and side tried. */
public final class FeedProbe {
    public static void main(String[] args) {
        var t = ColumnTrays.simple(10, 5);
        var s = EquipmentShape.column(.5, t, EquipmentShape.Heading.NORTH, EquipmentShape.minimumSkirt(.5));
        System.out.println("misfit " + ColumnDesign.misfit(s));
        System.out.printf("outer %.3f face? deck %.3f clear %.1f yB %.2f yT %.2f%n", s.outerRadius(), ColumnStructure.deckRadius(s), Math.toDegrees(ColumnStructure.clearance(s)), s.bottomTangent(), s.topTangent());
        var plan = ColumnStructure.plan(s);
        for (var p : plan.platforms()) System.out.printf("platform %d %.0f-%.0f ring %s manway %s%n", p.level(), Math.toDegrees(p.from()), Math.toDegrees(p.to()), p.ring(), p.manway());
        for (var n : ColumnDesign.sides(s)) System.out.println(n.role() + " " + n.index() + " home " + n.home() + " side " + n.side() + " placed " + n.placed() + (n.placed() ? " y " + n.nozzle().base()[1] : ""));
        double d = s.diameter(), yB = s.bottomTangent();
        System.out.printf("feed band %.2f..%.2f%n", yB + t.trayHeight(d, t.feed()), yB + t.trayHeight(d, t.feed() - 1));
    }
}
