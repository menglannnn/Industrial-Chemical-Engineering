package com.wormzjl.icheme.science.equipment;

import com.wormzjl.icheme.science.equipment.EquipmentShape.Heading;
import com.wormzjl.icheme.science.equipment.EquipmentShape.Kind;

/** In-game check rig (batch 2026-10-21-floating-roof-tanks): a storage vessel's box, fluid cell and nozzle pipe blocks
 *  in world coordinates for /icheme equipment unit <x y z> <kind> <size> <length> <support> north.
 *  Args: KIND size length support x y z. */
public class Ports {
    public static void main(String[] a) {
        var kind = Kind.valueOf(a[0]);
        var s = EquipmentShape.designed(kind, Double.parseDouble(a[1]), Integer.parseInt(a[2]), Heading.NORTH, Integer.parseInt(a[3]), ProcessUnit.Bank.ONE);
        int x = Integer.parseInt(a[4]), y = Integer.parseInt(a[5]), z = Integer.parseInt(a[6]);
        int ox = x - s.sizeX() / 2, oz = z - s.sizeZ() / 2;
        System.out.printf("%s size %d x %d x %d origin %d %d %d volume %.2f levelHeight %.2f%n", kind, s.sizeX(), s.sizeY(), s.sizeZ(), ox, y, oz, s.volume(), s.levelHeight());
        var f = ShapeRasterizer.fluidCell(s);
        if (f != null) System.out.printf("fluid cell %d %d %d%n", ox + f.x(), y + f.y(), oz + f.z());
        for (var p : ShapeRasterizer.ports(s))
            System.out.printf("nozzle %d %s %s pipe block %d %d %d (dir %d,%d,%d)%n", p.nozzle(), s.nozzles().get(p.nozzle()).role(), p.port(),
                    ox + p.cell().x() + p.dx(), y + p.cell().y() + p.dy(), oz + p.cell().z() + p.dz(), p.dx(), p.dy(), p.dz());
    }
}
