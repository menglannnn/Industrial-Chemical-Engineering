package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.equipment.EquipmentShape.Heading;
import com.wormzjl.createcheme.science.equipment.EquipmentShape.Kind;

/**
 * World points for the in-game walkway-end check: for a unit placed by {@code /createcheme equipment unit X Y Z kind size
 * length support north}, a point 3 m over the middle of each closed end's rail band, and the height a player dropped
 * there stops at if the band collides (its top, the fence over the walking surface). Args: kind size length support X Y Z.
 */
public final class FenceDrops {
    public static void main(String[] args) {
        var shape = EquipmentShape.unit(Kind.valueOf(args[0]), Double.parseDouble(args[1]), Double.parseDouble(args[2]), Heading.NORTH, Integer.parseInt(args[3]));
        int x = Integer.parseInt(args[4]), y = Integer.parseInt(args[5]), z = Integer.parseInt(args[6]);
        int ox = x - shape.sizeX() / 2, oz = z - shape.sizeZ() / 2, k = 0;
        for (var d : ProcessUnit.of(shape).decks()) {
            for (int e = 0; e < 2; e++) {
                if (!(e == 0 ? d.closedStart() : d.closedEnd())) continue;
                double s = e == 0 ? Access.RAIL_BAND / 2 : d.run().length() - Access.RAIL_BAND / 2, off = (Access.GAP + d.width() - Access.RAIL_BAND) / 2;
                double walk = d.level() + Access.DECK;
                var p = shape.transform().point(d.run().at(s, off, walk + 3));
                System.out.printf("deck %d end %d: tp @s %.3f %.2f %.3f -> fence top %.3f, deck %.3f%n", k, e, ox + p[0], y + p[1], oz + p[2], y + walk + Access.FENCE, y + walk);
            }
            k++;
        }
    }
}
