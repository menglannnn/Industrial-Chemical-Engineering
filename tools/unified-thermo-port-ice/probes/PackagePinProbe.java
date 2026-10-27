package com.wormzjl.createcheme.science.material;

import org.junit.jupiter.api.Test;

/** WP4 probe (tools/unified-thermo-port-ice/probes, not a tracked test): every bundled package's pins after the water record's ice. */
class PackagePinProbe {
    @Test
    void pins() {
        for (var catalog : java.util.List.of(MaterialCatalog.bundled(), PilotCryogenicTestCatalog.catalog())) {
            for (var p : new java.util.TreeMap<>(catalog.packages()).values()) {
                System.out.println("PINS " + p.id() + " " + p.scientificRevision() + " " + catalog.physicsFingerprint(p.id(), p.components())
                        + " fluid " + (catalog.fluidValidity(p.id()).isPresent() ? catalog.fluidThermoFingerprint(p.id()) : "-"));
            }
            System.out.println("CRYSTALS " + catalog.crystals().keySet());
        }
    }
}
