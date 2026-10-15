package com.wormzjl.createcheme.science.material;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FluidAppearanceTest {
    private static final String PACKAGE = "createcheme:tjl20_methane";

    @AfterEach void reset() { MaterialRuntime.reset(); }

    @Test void bundledComponentsWaterAndAssaysHaveIndependentAppearances() {
        var catalog = MaterialCatalog.bundled();
        assertThrows(IllegalArgumentException.class,()->catalog.componentAppearance(PACKAGE,"TJL_PC07"));
        assertEquals(.75, catalog.componentAppearance(PACKAGE, "Water").transparency());
        for (var p : catalog.packages().values()) {
            for (String component : p.components()) assertTrue(catalog.componentAppearance(p.id(), component).estimated());
            for (String assay : p.assays().keySet()) assertTrue(catalog.assayAppearance(p.id(), assay).estimated());
        }
        assertEquals(0x171615, catalog.assayAppearance("createcheme:cold_lake_blend_tjl20", "createcheme:cold_lake_blend").rgb());
        assertThrows(IllegalArgumentException.class, () -> catalog.componentAppearance(PACKAGE, "Unknown"));
        assertThrows(IllegalArgumentException.class, () -> catalog.assayAppearance(PACKAGE, "createcheme:cold_lake_blend"));
    }

    @Test void appearanceEditsPreserveScientificAndTransportFingerprints() {
        var original = MaterialCatalog.bundled();
        for (String path : new String[]{"components/crude_pc07.json", "assays/tjl20.json"}) {
            var changed = MaterialCatalog.parse(MaterialCatalogTest.changed(path, o -> o.add("appearance", appearance("#123456", .4))));
            assertEquals(original.requirePackage(PACKAGE).fingerprint(), changed.requirePackage(PACKAGE).fingerprint());
            assertEquals(original.viscosityFingerprint(PACKAGE), changed.viscosityFingerprint(PACKAGE));
            var result = path.startsWith("components") ? changed.componentAppearance(PACKAGE, "crude_pc07")
                    : changed.assayAppearance(PACKAGE, "createcheme:tia_juana_light_methane");
            assertEquals(new MaterialCatalog.FluidAppearance(0x123456, .4, true), result);
        }
    }

    @Test void recordsWithoutAppearanceAreRefused() {
        for (String path : new String[]{"components/water.json", "assays/tjl20.json"})
            assertThrows(IllegalArgumentException.class, () -> MaterialCatalog.parse(MaterialCatalogTest.changed(path, o -> o.remove("appearance"))), path);
    }

    @Test void invalidAppearanceIdentifiesResourceAndRetainsPublishedSnapshot() {
        var original = MaterialCatalog.bundled();
        MaterialRuntime.publish(original);
        for (String path : new String[]{"components/water.json", "assays/tjl20.json"}) {
            for (JsonObject appearance : new JsonObject[]{appearance("blue", .5), appearance("#00112233", .5),
                    appearance("#123456", -.1), appearance("#123456", 1.1)}) {
                var resources = MaterialCatalogTest.changed(path, o -> o.add("appearance", appearance));
                var error = assertThrows(IllegalArgumentException.class, () -> MaterialRuntime.publish(MaterialCatalog.parse(resources)));
                assertTrue(error.getMessage().contains(path));
                assertTrue(error.getMessage().contains("appearance"));
                assertSame(original, MaterialRuntime.active());
            }
        }
        assertThrows(IllegalArgumentException.class, () -> new MaterialCatalog.FluidAppearance(0, Double.NaN, true));
        assertThrows(IllegalArgumentException.class, () -> new MaterialCatalog.FluidAppearance(0, Double.POSITIVE_INFINITY, true));
    }

    @Test void transparencyHasExplicitOpaqueAndInvisibleEndpoints() {
        assertEquals(0xFF123456, new MaterialCatalog.FluidAppearance(0x123456, 0, true).argb());
        assertEquals(0x00123456, new MaterialCatalog.FluidAppearance(0x123456, 1, true).argb());
        assertEquals(0x80123456, new MaterialCatalog.FluidAppearance(0x123456, .5, true).argb());
    }

    private static JsonObject appearance(String color, double transparency) {
        var o = new JsonObject();
        o.addProperty("color", color); o.addProperty("transparency", transparency); o.addProperty("estimated", true);
        return o;
    }
}
