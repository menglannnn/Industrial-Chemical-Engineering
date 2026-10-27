package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.equipment.EquipmentShape.Heading;
import com.wormzjl.createcheme.science.equipment.EquipmentShape.Kind;

import java.util.Locale;

/**
 * FCC form A in-game check (2026-10-18): args are groups "x z d riser support [ox oz]" (a placement command's x and z, or
 * a refreshed unit's world origin ox oz); prints the box, the regenerator and riser axes, the floors and the stair tower's
 * flights in world coordinates (heading north, grade y -60).
 */
public class FccAProbe {
    public static void main(String[] a) {
        for (int i = 0; i + 4 < a.length; ) {
            int x = Integer.parseInt(a[i]), z = Integer.parseInt(a[i + 1]);
            double d = Double.parseDouble(a[i + 2]);
            int len = Integer.parseInt(a[i + 3]), sup = Integer.parseInt(a[i + 4]);
            var shape = EquipmentShape.unit(Kind.FCC_CONVERTER, d, len, Heading.NORTH, sup);
            int ox = x - shape.sizeX() / 2, oz = z - shape.sizeZ() / 2;
            i += 5;
            if (i + 1 < a.length && a[i].equals("at")) {
                ox = Integer.parseInt(a[i + 1]);
                oz = Integer.parseInt(a[i + 2]);
                i += 3;
            }
            var u = ProcessUnit.of(shape);
            double[] b = u.box();
            var l = new FccConverterLayout(d, len, sup);
            // world = origin + (frame - box min); the layout's frame is shifted O = 0.5 (regenerator axis on a cell centre)
            double wx0 = ox - b[0] + .5, wz0 = oz - b[2] + .5;
            System.out.printf(Locale.ROOT, "fcc %s x %d s%d placed %d %d: box %d x %d x %d origin %d,-60,%d (x %d..%d z %d..%d y -60..%d) fits=%s%n", d, len, sup, x, z,
                    shape.sizeX(), shape.sizeY(), shape.sizeZ(), ox, oz, ox, ox + shape.sizeX() - 1, oz, oz + shape.sizeZ() - 1, -60 + shape.sizeY() - 1, l.fits);
            System.out.printf(Locale.ROOT, "  frame box x %.1f..%.1f z %.1f..%.1f; regen axis world x %.2f z %.2f; riser axis x %.2f; regen straight %.2f top tangent y %.2f; reactor %.2f x %.2f, bottom %.2f top %.2f; stripper %.2f x %.2f bottom %.2f%n",
                    b[0], b[3], b[2], b[5], wx0, wz0, wx0 + l.x, l.straight, -60 + sup + l.straight, 2 * l.rr, l.reactorShell, -60 + l.reactorBottom, -60 + l.reactorTop,
                    2 * l.rs, l.stripperShell, -60 + l.stripperBottom);
            double[] floors = l.rcsvOnInjectorFloor ? new double[]{l.injectorFloor, l.scsvFloor, l.stripperFloor, l.manwayFloor, l.topFloor}
                    : new double[]{l.injectorFloor, l.rcsvFloor, l.scsvFloor, l.stripperFloor, l.manwayFloor, l.topFloor};
            StringBuilder sb = new StringBuilder("  floors (walking surface y):");
            double[] stops = new double[floors.length];
            for (int k = 0; k < floors.length; k++) {
                sb.append(String.format(Locale.ROOT, " %.2f", -60 + floors[k]));
                stops[k] = floors[k] - Access.DECK;
            }
            System.out.println(sb + "  frame x " + String.format(Locale.ROOT, "%.2f..%.2f", wx0 + l.xf, wx0 + l.xe) + " half " + l.half);
            System.out.printf(Locale.ROOT, "  rcsv (x %.2f y %.2f) on floor %.2f; scsv (x %.2f y %.2f) on floor %.2f; spent standpipe: stripper start x %.2f, elbow (x %.2f y %.2f), shell entry y %.2f; regen standpipe top y %.2f, wye y %.2f%n",
                    wx0 + l.rcsvX, -60 + l.rcsvY, -60 + l.rcsvFloor, wx0 + l.scsvX, -60 + l.scsvY, -60 + l.scsvFloor, wx0 + l.s0x, wx0 + l.p1x, -60 + l.p1y, -60 + l.scspIn,
                    -60 + l.rcspTop, -60 + l.wye);
            var plan = ProcessUnit.towerPlan(stops);
            double laneA = Math.floor(l.xe + .5 + ProcessUnit.TOWER_FLIGHT / 2) + .5; // frame x of lane A's centre (box frame)
            double footZ = ProcessUnit.TowerPlan.foot(-l.half + .5, 1, plan.near());
            double wa = ox - b[0] + laneA, wfoot = oz - b[2] + footZ;
            System.out.printf(Locale.ROOT, "  tower: lane A x %.2f, lane B x %.2f; lane A foot z %.3f; far (lane A top / lane B foot) z %.3f; landings %.3f..%.3f and %.3f..%.3f%n",
                    wa, wa + 1, wfoot, wfoot + plan.longest(), wfoot - 1.2, wfoot, wfoot + plan.longest(), wfoot + plan.longest() + 1.2);
            double base = 0;
            for (int f = 0; f < plan.levels().size(); f++) {
                double lv = plan.levels().get(f);
                System.out.printf(Locale.ROOT, "    flight %d lane %s from y %.3f to %.3f, length %.2f, reaches %s%n", f, f % 2 == 0 ? "A +z" : "B -z", -60 + base, -60 + lv + Access.DECK,
                        plan.lengths().get(f), plan.reached().get(f) >= 0 ? "floor " + plan.reached().get(f) : "landing");
                base = lv + Access.DECK;
            }
        }
    }
}
