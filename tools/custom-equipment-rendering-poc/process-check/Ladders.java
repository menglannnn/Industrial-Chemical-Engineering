package com.wormzjl.createcheme.science.equipment;


/** Prints, for the check column (D 3 m, 20 trays, skirt 3 m, front north, origin 229 -60 21), each ladder's foot in front of the rungs, the yaw facing them, and each platform deck. */
public class Ladders {
    public static void main(String[] args) {
        var shape = EquipmentShape.column(3, ColumnTrays.simple(20, 10), EquipmentShape.Heading.NORTH, 3);
        int ox = 229, oy = -60, oz = 21;
        var plan = ColumnStructure.plan(shape);
        double rung = shape.outerRadius() + ColumnStructure.STANDOFF, axis = shape.axis();
        for (var l : plan.ladders()) {
            var foot = shape.transform().point(new double[]{axis + (rung + .45) * Math.cos(l.angle()), l.base(), axis + (rung + .45) * Math.sin(l.angle())});
            var at = shape.transform().point(new double[]{axis + rung * Math.cos(l.angle()), l.base(), axis + rung * Math.sin(l.angle())});
            double dx = at[0] - foot[0], dz = at[2] - foot[2], yaw = Math.toDegrees(Math.atan2(-dx, dz));
            System.out.printf("ladder base %.3f top %.3f foot %.3f %.3f %.3f yaw %.1f%n", l.base(), l.top(), foot[0] + ox, foot[1] + oy, foot[2] + oz, yaw);
        }
        for (var p : plan.platforms()) {
            double a = p.ring() ? p.from() + Math.PI : (p.from() + p.to()) / 2, r = (shape.outerRadius() + ColumnStructure.GAP + plan.deckRadius()) / 2;
            var d = shape.transform().point(new double[]{axis + r * Math.cos(a), p.level() + ColumnStructure.DECK, axis + r * Math.sin(a)});
            System.out.printf("platform level %d manway %s deck %.3f %.3f %.3f%n", p.level(), p.manway(), d[0] + ox, d[1] + oy, d[2] + oz);
        }
        System.out.println("size " + shape.sizeX() + " " + shape.sizeY() + " " + shape.sizeZ());
    }
}
