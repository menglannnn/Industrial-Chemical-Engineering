package com.wormzjl.createcheme.science.fluid.thermo;

import com.wormzjl.createcheme.fluid.support.FluidTestSupport;
import com.wormzjl.createcheme.science.material.MaterialCatalog;
import java.util.Arrays;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/**
 * WP4 probe (tools/unified-thermo-port-ice/probes, not a tracked test): SpineNetworkPathTest's wet CO2 feed below 273.16 K
 * against its dry hydrocarbon part, the same amounts without the water, on the pilot: does the dry part hold too?
 */
class WetCrystalGridProbe {
    @Test
    void wetAgainstDry() {
        var model = FluidTestSupport.model(MaterialCatalog.bundled(), "createcheme:pilot_cryogenic");
        double[] temperatures = {80, 100, 120, 150, 180, 230, 260};
        double[] pressures = {1.0e5, 3.0e5, 1.0e6, 2.0e6, 5.0e6, 1.0e7};
        double[] wet = {0.1, 0.6, 0.1, 0.1, 0.1}, dry = {0.1, 0.6, 0.1, 0.1, 0};
        for (double t : temperatures) for (double p : pressures) {
            System.out.printf(Locale.ROOT, "%6.1f K %9.0f Pa  wet: %s%n                     dry: %s%n", t, p, answer(model, t, p, wet), answer(model, t, p, dry));
        }
    }

    static String answer(FluidThermodynamics model, double t, double p, double[] feed) {
        try {
            var s = model.flashTP(t, p, feed, () -> {});
            return String.format(Locale.ROOT, "L%s V%s W %.3e S %.3e pw %.3e", Arrays.toString(round(s.liquidView())), Arrays.toString(round(s.vaporView())), s.waterLiquid(), s.waterVapor(), s.waterPartialPressure());
        } catch (RuntimeException failed) {
            return failed.getClass().getSimpleName() + ": " + failed.getMessage();
        }
    }

    static double[] round(double[] a) {
        double[] r = new double[a.length];
        for (int i = 0; i < a.length; i++) r[i] = Math.round(a[i] * 1e4) / 1e4;
        return r;
    }
}
