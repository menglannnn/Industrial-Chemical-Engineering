package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.equipment.EquipmentShape.Heading;
import com.wormzjl.createcheme.science.equipment.EquipmentShape.Kind;

import java.util.Locale;

/**
 * Integration probe: the nozzles and moving parts of every size of the named kinds at their least and most length and
 * their lowest and highest support, one line each, to diff the integration against the lanes' state. Args: kind names.
 */
public class Dump {
    public static void main(String[] args) {
        for (String name : args) {
            var kind = Kind.valueOf(name);
            double step = ProcessUnit.diameterStep(kind);
            for (double d = ProcessUnit.minDiameter(kind); d <= ProcessUnit.maxDiameter(kind) + 1e-9; d += step) {
                double size = ProcessUnit.snap(kind, d);
                int least = ProcessUnit.minLength(kind, size), most = ProcessUnit.maxLength(kind, size);
                for (int length : most > least ? new int[]{least, most} : new int[]{least}) {
                    int low = ProcessUnit.minSupport(kind, size, length), high = ProcessUnit.maxSupport(kind);
                    for (int h : high > low ? new int[]{low, high} : new int[]{low}) {
                        var shape = EquipmentShape.unit(kind, size, length, Heading.NORTH, h);
                        String id = String.format(Locale.ROOT, "%s %.2f x %d s%d", kind, size, length, h);
                        System.out.println(id + " box " + shape.sizeX() + "x" + shape.sizeY() + "x" + shape.sizeZ());
                        for (var z : shape.nozzles())
                            System.out.println(id + " nozzle " + z.role() + " " + v(z.base()) + " axis " + z.axis() + " sign " + z.sign() + String.format(Locale.ROOT,
                                    " length %.4f max %.3f default %.3f riser %.4f exit %.4f", z.length(), z.maxOd(), z.defaultOd(), z.riser(), z.exit()));
                        for (var m : ShapeRasterizer.moving(shape)) {
                            var o = m.motion();
                            double[] lo = {1e9, 1e9, 1e9}, hi = {-1e9, -1e9, -1e9};
                            for (var p : m.polygons())
                                for (int i = 0; i < p.positions().length; i++) {
                                    lo[i % 3] = Math.min(lo[i % 3], p.positions()[i]);
                                    hi[i % 3] = Math.max(hi[i % 3], p.positions()[i]);
                                }
                            System.out.println(id + " moving pivot " + v(o.pivot()) + " axis " + v(o.axis()) + String.format(Locale.ROOT, " speed %.4f phase %.4f crank %.4f rod %.4f reach %.4f",
                                    o.speed(), o.phase(), o.crank(), o.rod(), m.reach()) + " polygons " + m.polygons().size() + " bounds " + v(lo) + " " + v(hi));
                        }
                    }
                }
            }
        }
    }

    private static String v(double[] p) {
        return String.format(Locale.ROOT, "(%.4f %.4f %.4f)", p[0], p[1], p[2]);
    }
}
