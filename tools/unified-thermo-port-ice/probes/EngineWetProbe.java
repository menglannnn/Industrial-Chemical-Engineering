package com.wormzjl.createcheme.science.fluid.thermo;

import com.wormzjl.createcheme.fluid.support.FluidTestSupport;
import com.wormzjl.createcheme.science.material.MaterialCatalog;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/**
 * WP4 probe (tools/unified-thermo-port-ice/probes, not a tracked test): the engine's TP answers for SpineNetworkPathTest's
 * wet CO2 feed and its dry part on the pilot, with the crystal competition: status, classification and detail.
 */
class EngineWetProbe {
    @Test
    void engineAnswers() {
        var model = FluidTestSupport.model(MaterialCatalog.bundled(), "createcheme:pilot_cryogenic");
        var engine = model.phaseEngine();
        double[][] points = {{100, 1e5}, {120, 1e5}, {120, 1e6}, {120, 2e6}, {150, 1e5}, {150, 1e6}};
        String[] which = System.getProperty("probe.feeds", "wet,dry").split(",");
        for (double[] point : points) for (String kind : which) {
            double[] feed = kind.equals("wet") ? new double[] {0.1, 0.6, 0.1, 0.1, 0.1} : new double[] {0.1, 0.6, 0.1, 0.1, 0};
            try {
                var r = engine.tp(point[0], point[1], feed, true);
                System.out.printf(Locale.ROOT, "%5.0f K %8.0f Pa %s: %s %s%n    %s%n", point[0], point[1], kind, r.status(), r.classification(),
                        r.detail().length() > 900 ? r.detail().substring(0, 900) : r.detail());
            } catch (RuntimeException failed) {
                System.out.printf(Locale.ROOT, "%5.0f K %8.0f Pa %s: threw %s%n", point[0], point[1], kind, failed);
            }
        }
    }
}
