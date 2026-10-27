package com.wormzjl.icheme.science.equipment;
import java.util.List;
/**
 * Rack flange placement by line size (batch 2026-10-26-pipe-laying-system, RACK_FLANGES_AIM_PLAN section 1): over every
 * tier position, both brace directions and six frames (shared, whole, stacked, none), the spools that find no room, the
 * open ends capped, and the spool and blind-flange depths. Run against the compiled mod classes:
 * javac -d <out> -cp build/classes/java/main FlangeProbe.java; java -cp "<out>;build/classes/java/main" com.wormzjl.icheme.science.equipment.FlangeProbe
 */
public class FlangeProbe {
    public static void main(String[] args) {
        int N = 2, S = 3;
        int[] frames = {RackGeometry.BENT_BACK | RackGeometry.BENT_FRONT | RackGeometry.GROUNDED, RackGeometry.BENT_BACK | RackGeometry.BENT_FRONT | RackGeometry.END_BACK | RackGeometry.END_FRONT | RackGeometry.GROUNDED,
            RackGeometry.BENT_BACK | RackGeometry.BENT_FRONT | RackGeometry.STACKED | RackGeometry.GROUNDED, RackGeometry.BENT_BACK | RackGeometry.BENT_FRONT | RackGeometry.END_BACK | RackGeometry.END_FRONT | RackGeometry.STACKED | RackGeometry.GROUNDED, 0, RackGeometry.STACKED};
        for (int code = 2; code <= 47; code += code < 12 ? 3 : 4) {
            double od = PipeSizes.outsideOf(code);
            int width = RackGeometry.width(od);
            if (width > RackGeometry.MAX_WIDTH) continue;
            int cases = 0, noSpool = 0, capped = 0, stackedNoSpool = 0, stackedCapped = 0;
            double spoolMin = 9, spoolMax = 0, backMax = 0;
            for (int rising = 0; rising < 2; rising++)
                for (int f : frames)
                    for (int p = 0; p + width <= RackGeometry.POSITIONS; p++) {
                        int steel = f | (rising == 1 ? RackGeometry.RISING : 0);
                        boolean stacked = (f & RackGeometry.STACKED) != 0;
                        var lane = new RackGeometry.Lane(RackGeometry.option(p, RackGeometry.TIER), width, 1 << N | 1 << S, 1 << N | 1 << S, code);
                        var rack = new RackGeometry.Rack(N, RackGeometry.STRAIGHT, steel, RackGeometry.beam(List.of(lane)));
                        double at = RackGeometry.spoolAt(rack, lane);
                        var path = RackGeometry.path(rack, lane);
                        cases++;
                        if (at < 0) { noSpool++; if (stacked) stackedNoSpool++; } else { spoolMin = Math.min(spoolMin, at); spoolMax = Math.max(spoolMax, at); }
                        for (int face : new int[]{N, S}) {
                            double back = RackGeometry.setback(rack, od, face, face == RackGeometry.first(rack, lane) ? path.getFirst() : path.getLast());
                            if (back < 0) { capped++; if (stacked) stackedCapped++; } else backMax = Math.max(backMax, back);
                        }
                    }
            System.out.printf("od %.3f (%4.1f in) w%d: cells %d, no spool %d (stacked %d), spool %.1f..%.1f/32; ends %d, capped %d (stacked %d), setback max %.1f/32%n",
                    od, od / .0254, width, cases, noSpool, stackedNoSpool, spoolMin * 32, spoolMax * 32, 2 * cases, capped, stackedCapped, backMax * 32);
        }
    }
}
