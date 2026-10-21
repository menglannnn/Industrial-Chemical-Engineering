package com.wormzjl.icheme.science.thermo.phase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.wormzjl.icheme.science.material.MaterialCatalog;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The identity string moves with every part and with nothing else. */
class ThermoIdentityTest {
    @Test
    void revisionChangesWhenAnyPartChanges() {
        var base = new ThermoIdentity("pkg-a", CubicPhaseEvaluator.FAMILY, "spine-1", "water-1");
        List<ThermoIdentity> variants = List.of(
                new ThermoIdentity("pkg-b", CubicPhaseEvaluator.FAMILY, "spine-1", "water-1"),
                new ThermoIdentity("pkg-a", "pr78_translated_v2", "spine-1", "water-1"),
                new ThermoIdentity("pkg-a", CubicPhaseEvaluator.FAMILY, "spine-2", "water-1"),
                new ThermoIdentity("pkg-a", CubicPhaseEvaluator.FAMILY, "spine-1", "water-2"));
        Set<String> revisions = new HashSet<>();
        revisions.add(base.revision());
        for (ThermoIdentity variant : variants) {
            assertNotEquals(base.revision(), variant.revision());
            assertNotEquals(base, variant);
            revisions.add(variant.revision());
        }
        assertEquals(5, revisions.size());
        // Equal parts, equal revision.
        var again = new ThermoIdentity("pkg-a", CubicPhaseEvaluator.FAMILY, "spine-1", "water-1");
        assertEquals(base.revision(), again.revision());
        assertEquals(base, again);
        assertEquals(base.hashCode(), again.hashCode());
        // Moving characters between parts changes the revision: the parts are length-prefixed.
        var a = new ThermoIdentity("x;1:y", "f", "s", "w");
        var b = new ThermoIdentity("x", "y;1:f", "s", "w");
        assertNotEquals(a.revision(), b.revision());
        assertThrows(IllegalArgumentException.class, () -> new ThermoIdentity(" ", "f", "s", "w"));
        assertThrows(NullPointerException.class, () -> new ThermoIdentity("p", null, "s", "w"));
    }

    @Test
    void catalogIdentityFollowsThePackageTheSpineAndTheWaterParticipation() {
        var catalog = MaterialCatalog.bundled();
        String network = PhaseTestSupport.NETWORK;
        var free = ThermoIdentity.of(catalog, network, CubicPhaseEvaluator.FAMILY, ThermoIdentity.NO_SPINE,
                WaterParticipation.SEPARATE_FREE_WATER);
        assertEquals(catalog.fluidThermoFingerprint(network), free.packageFingerprint());
        assertEquals(ThermoIdentity.SEPARATE_FREE_WATER_FORMULATION + ":" + catalog.requirePackage(network).water().revision(),
                free.waterModelRevision());
        var none = ThermoIdentity.of(catalog, network, CubicPhaseEvaluator.FAMILY, ThermoIdentity.NO_SPINE, WaterParticipation.NONE);
        var spine = ThermoIdentity.of(catalog, network, CubicPhaseEvaluator.FAMILY, "spine:test-1", WaterParticipation.SEPARATE_FREE_WATER);
        var other = ThermoIdentity.of(catalog, "icheme:tjl20_methane", CubicPhaseEvaluator.FAMILY, ThermoIdentity.NO_SPINE,
                WaterParticipation.SEPARATE_FREE_WATER);
        assertNotEquals(free.revision(), none.revision());
        assertNotEquals(free.revision(), spine.revision());
        assertNotEquals(free.revision(), other.revision());
        // Deterministic across catalog loads.
        assertEquals(free.revision(), ThermoIdentity.of(MaterialCatalog.bundled(), network, CubicPhaseEvaluator.FAMILY,
                ThermoIdentity.NO_SPINE, WaterParticipation.SEPARATE_FREE_WATER).revision());
        // The network contract carries it, and its results carry the contract's.
        var contract = PhaseContract.forNetworkPackage(catalog, network, ThermoIdentity.NO_SPINE);
        assertEquals(free, contract.identity());
    }
}
