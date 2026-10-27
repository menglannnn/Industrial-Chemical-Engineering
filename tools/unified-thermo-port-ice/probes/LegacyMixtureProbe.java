package com.wormzjl.createcheme.science.fluid.thermo;

import com.wormzjl.createcheme.fluid.support.FluidTestSupport;
import com.wormzjl.createcheme.science.material.MaterialCatalog;
import org.junit.jupiter.api.Test;

/**
 * WP4 probe (tools/unified-thermo-port-ice/probes, not a tracked test): LegacyNetworkPathPinTest's one wet flash that
 * WP4 turned from the ice hold into a three-phase hold (110 K, 1 bar, the twelve-component mixture with 0.1 water): the
 * same mixture dry, so the hold is the hydrocarbons' own.
 */
class LegacyMixtureProbe {
    @Test
    void dryAgainstWet() {
        var catalog = MaterialCatalog.bundled();
        var model = FluidTestSupport.model(catalog, LegacyNetworkPathPinTest.NETWORK);
        int n = model.hydrocarbon.componentCount();
        int nitrogen = model.hydrocarbon.components().indexOf("Nitrogen");
        double[] mixture = new double[n + 1];
        for (int i = 0; i < n; i++) mixture[i] = i == nitrogen ? 0.1 : (i % 5 + 1) * 0.01;
        for (double water : new double[] {0.0, 0.1}) {
            mixture[n] = water;
            try {
                var s = model.flashTP(110, 1e5, mixture, () -> {});
                System.out.println("water " + water + ": answered, liquid " + java.util.Arrays.toString(s.liquidView()) + " vapour " + java.util.Arrays.toString(s.vaporView()));
            } catch (RuntimeException refused) {
                System.out.println("water " + water + ": " + refused.getMessage().substring(0, Math.min(220, refused.getMessage().length())));
            }
        }
    }
}
