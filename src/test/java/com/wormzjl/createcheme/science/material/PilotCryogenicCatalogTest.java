package com.wormzjl.createcheme.science.material;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.wormzjl.createcheme.science.thermo.IdealGasFunction;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

/**
 * The bundled pilot package {@code createcheme:pilot_cryogenic} (batch 2026-09-24-coolprop-low-temperature; P2 record
 * kinds, P3 WP3 bundling): it loads from the main resources, it is the only package with a reference spine, the test
 * network override selects it, the loader enforces its rules (spine and crystals, D6's single ideal-gas source, the
 * {@code volume_translation} field), and the fingerprints of the eight column and network packages stay byte-identical.
 */
class PilotCryogenicCatalogTest {
    private static final String PACKAGE = "data/createcheme/materials/packages/pilot_cryogenic.json";
    private static final String NITROGEN = "data/createcheme/materials/spine/nitrogen.json";
    private static final String METHANE = "data/createcheme/materials/spine/methane.json";
    private static final String CRYSTAL = "data/createcheme/materials/crystals/carbon_dioxide_i.json";
    private static final String PILOT_METHANE = "data/createcheme/materials/properties/pilot_methane.json";
    private static final String TJL20_METHANE = "data/createcheme/materials/properties/tjl20_methane.json";
    private static final String CALIBRATION = "data/createcheme/materials/transport/liquid_calibration.json";

    /**
     * Package id, scientific revision and physics fingerprint of every bundled column and network package, read at
     * 9ca0f9c before the P2 loader change (a javac-run main against the compiled classes) and unchanged since.
     */
    private static final String[][] BUNDLED_PINS = {
        {"createcheme:bonga_tjl20", "crude-regrouped-r1:61dcf6df94ee836e4063f2871c761e3fad61576e8a2c62e80cb4f2b3ab5528f3", "b1b9f1eb4d77f7868982c4eec28fabd5b4b9fd36b91ca8a5869b7fbcbea6ceb8"},
        {"createcheme:cold_lake_blend_tjl20", "crude-regrouped-r1:08620e3b0bb8b059d3a54bb80ac904faa89ece1fb1c405339eb14012f3a7d7e3", "b1b9f1eb4d77f7868982c4eec28fabd5b4b9fd36b91ca8a5869b7fbcbea6ceb8"},
        {"createcheme:dalia_tjl20", "crude-regrouped-r1:36ef9ad1fea2c55ea7a8bc5a618a84358e4cdb669bc1b93395ff0293eae119fd", "b1b9f1eb4d77f7868982c4eec28fabd5b4b9fd36b91ca8a5869b7fbcbea6ceb8"},
        {"createcheme:tjl19_dwsim", "crude-regrouped-r1:f74f65646c865ee96e22c89ee13a37dfa81e9a4aa918c4e4df218c64df0b6a18", "3361fe82944a086e30a433f7f43d9122583a7ee762d8c5caff8b2d5af00303b4"},
        {"createcheme:tjl20_methane", "crude-regrouped-r1:dc4be6bfc41142eeb374e0fa2cad11120258db1d0bd99fe770c8fce607b071e4", "b1b9f1eb4d77f7868982c4eec28fabd5b4b9fd36b91ca8a5869b7fbcbea6ceb8"},
        {"createcheme:tjl20_methane_nitrogen", "crude-regrouped-r1:c8e890f912d4c4626e8c14fa41d267c8704480d7305ad3ccb2d02f47caf9a341", "c9ff50b0b82ac9ff12dafaca84860747829c58bfd5acddeb70dd6ca489b5f854"},
        {"createcheme:upper_zakum_tjl20", "crude-regrouped-r1:8274de881b2c1aac526ec81f9c8d8142f76a3fca956dcfdd0f19a7b132bc45b2", "b1b9f1eb4d77f7868982c4eec28fabd5b4b9fd36b91ca8a5869b7fbcbea6ceb8"},
        {"createcheme:wti_light_export_tjl20", "crude-regrouped-r1:9f0540a5aecea7f54f60a61c2eb630dec95ee5796a00b037dddafd705f29eacc", "b1b9f1eb4d77f7868982c4eec28fabd5b4b9fd36b91ca8a5869b7fbcbea6ceb8"},
    };

    /**
     * The pilot package as committed in P3 WP3 (pilot records r1, the E-PPR78 rule on createcheme:eppr78_2022 r1), with
     * the spine records of P3 WP9a (N2, CH4 and C2H6 r2, CO2 r3) and the CO2-I crystal of P4 stage 1 (r2). A numeric edit
     * of any of them, or of the hash rules, moves a pin; the pin is then updated with the record's revision.
     *
     * <p>Pin moves. P3 WP9a moved the spine fingerprint from {@code e750073d...} (WP3: spine r1 of N2/CH4/C2H6, r2 of
     * CO2) to {@code 1b33b193...}, for two decided changes: the CEA segments and standard entropies with CEA's own gas
     * constant 8.314510 instead of 8.314472 (N2, C2H6, CO2), and methane's segments above 425 K replaced by the GERG-2008
     * ideal-gas function to 1200 K (decision D13). P4 stage 1 moved it from {@code 1b33b193...} to the value below: the
     * CO2-I crystal r1 (research-only segments) became r2, the Jaeger and Span 2012 Gibbs function with an anchor fusion
     * enthalpy of 9019 J/mol. The scientific revision and the physics fingerprint do not see the spine or the crystal and
     * did not move.</p>
     */
    private static final String PILOT_SCIENTIFIC_REVISION = "pilot-cryogenic-p3-r1:37a33a6190bd9fba7433c494c63ae4bfab24995b6902cf58c59381033c3a7c64";
    private static final String PILOT_PHYSICS_FINGERPRINT = "7378e1991b885b5a129a7e74eae0ee5d0ebd5f1d6d9be298182bc163519230c0";
    private static final String PILOT_SPINE_FINGERPRINT = "581c9098c10bef8de71cd8fb029221ffae849c65ebc3d01a2e62ba715645df08";

    @Test
    void bundledPackageFingerprintsAreUnchanged() {
        for (MaterialCatalog catalog : List.of(MaterialCatalog.bundled(), PilotCryogenicTestCatalog.catalog())) {
            for (String[] pin : BUNDLED_PINS) {
                var p = catalog.requirePackage(pin[0]);
                assertEquals(pin[1], p.scientificRevision(), pin[0]);
                assertEquals(pin[2], catalog.physicsFingerprint(pin[0], p.components()), pin[0]);
                assertTrue(p.spine().isEmpty() && p.crystals().isEmpty() && p.spineFingerprint().isEmpty() && p.groupContributions() == null, pin[0]);
            }
            // The pilot is the ninth bundled package and the only one with a reference spine.
            assertEquals(BUNDLED_PINS.length + 1, catalog.packages().size());
            assertEquals(List.of(PilotCryogenicTestCatalog.PACKAGE_ID), catalog.packages().values().stream()
                    .filter(p -> !p.spine().isEmpty()).map(MaterialCatalog.Package::id).toList());
            var pilot = catalog.requirePackage(PilotCryogenicTestCatalog.PACKAGE_ID);
            assertEquals(4, pilot.components().stream().map(c -> catalog.requireSpine(pilot.id(), c)).distinct().count());
            assertEquals(1, catalog.crystals().size());
        }
    }

    @Test
    void pilotPackageIsBundled() {
        var catalog = MaterialCatalog.bundled();
        var p = catalog.requirePackage(PilotCryogenicTestCatalog.PACKAGE_ID);
        System.out.printf("P3 pilot pins: scientific revision %s, physics %s, spine %s, fluid thermo %s%n", p.scientificRevision(),
                catalog.physicsFingerprint(p.id(), p.components()), p.spineFingerprint(), catalog.fluidThermoFingerprint(p.id()));
        assertEquals(PILOT_SCIENTIFIC_REVISION, p.scientificRevision());
        assertEquals(PILOT_PHYSICS_FINGERPRINT, catalog.physicsFingerprint(p.id(), p.components()));
        assertEquals(PILOT_SPINE_FINGERPRINT, p.spineFingerprint());
        assertEquals(List.of("Nitrogen", "Methane", "Ethane", "CarbonDioxide"), p.components());
        assertEquals(List.of("createcheme:pilot_nitrogen", "createcheme:pilot_methane", "createcheme:pilot_ethane", "createcheme:pilot_carbon_dioxide"),
                p.properties().stream().map(MaterialCatalog.Property::id).toList());
        assertEquals(List.of("createcheme:spine_nitrogen", "createcheme:spine_methane", "createcheme:spine_ethane",
                "createcheme:spine_carbon_dioxide"), p.spine());
        assertEquals(List.of("createcheme:crystal_carbon_dioxide_i"), p.crystals());
        assertEquals("pr78", p.model());
        for (int i = 0; i < p.components().size(); i++) {
            var property = p.properties().get(i);
            assertTrue(property.cp().isEmpty(), property.id() + ": the spine is the only ideal-gas source (D6)");
            assertNotNull(property.volumeTranslation(), property.id());
            assertEquals(catalog.volumeTranslation(p.id(), p.components().get(i)).orElseThrow(), property.volumeTranslation());
            IdealGasFunction function = catalog.requireSpine(p.id(), p.components().get(i));
            assertTrue(function.minimumTemperature() <= catalog.fluidValidity(p.id(), p.components().get(i)).orElseThrow().minimumTemperature());
            // Methane's ideal gas ends at 1200 K (D13, spine r2), the package's upper temperature; the others reach 6000 K.
            assertEquals(p.components().get(i).equals("Methane") ? 1200 : 6000, function.maximumTemperature(), 0.0);
            // The vapour table spans the component's fluid range, and for the species of the admitted crystal the gas
            // floor of the crystal competition (CO2 from 90 K, record r2 of P4 stage 2b); the liquid table runs from the
            // triple point to Tc, and for the crystal's species from the competition's floor (CO2's subcooled liquid from
            // 90 K, dissolved in liquid methane: record r2 of P5).
            var vapor = catalog.viscosity(p.id(), p.components().get(i), ViscosityCorrelation.Phase.VAPOR).orElseThrow();
            var liquid = catalog.viscosity(p.id(), p.components().get(i), ViscosityCorrelation.Phase.LIQUID).orElseThrow();
            var domain = catalog.fluidValidity(p.id(), p.components().get(i)).orElseThrow();
            assertEquals(p.components().get(i).equals("CarbonDioxide") ? 90 : domain.minimumTemperature(), vapor.minimumTemperatureKelvin(), 0.0);
            assertEquals(1200, vapor.maximumTemperatureKelvin(), 0.0);
            assertEquals(p.components().get(i).equals("CarbonDioxide") ? 90 : domain.minimumTemperature(), liquid.minimumTemperatureKelvin(), 0.0);
            assertEquals(1e7, domain.maximumPressure(), 0.0);
        }
        var envelope = catalog.fluidValidity(p.id()).orElseThrow();
        assertEquals(63.151, envelope.minimumTemperature(), 0.0);
        assertEquals(1200, envelope.maximumTemperature(), 0.0);
        assertEquals(1e7, envelope.maximumPressure(), 0.0);
        assertEquals(6, p.groupContributions().pairs().size());
        assertEquals(0.0, p.interactions().stream().flatMap(List::stream).mapToDouble(Double::doubleValue).map(Math::abs).sum(),
                "every pair is rule-resolved; the constant entries are 0 and are not the kij");
        assertEquals(MaterialCoverage.Grade.QUALIFIED, catalog.requireSpine(p.id(), "Nitrogen").coverage().grade());
        assertEquals(MaterialCoverage.Grade.QUALIFIED, catalog.requireSpine(p.id(), "Methane").coverage().grade());
        assertEquals(90, catalog.requireSpine(p.id(), "CarbonDioxide").minimumTemperature(), 0.0);
        assertTrue(catalog.crystals().get("createcheme:crystal_carbon_dioxide_i").usable(), "the CO2-I crystal is the Jaeger-Span function since P4 stage 1");
        assertEquals("Carbon dioxide", JsonParser.parseString(PilotCryogenicTestCatalog.read("assets/createcheme/lang/en_us.json"))
                .getAsJsonObject().get("material.createcheme.carbon_dioxide").getAsString());
        assertEquals("material.createcheme.carbon_dioxide", catalog.name("CarbonDioxide").translationKey());
    }

    @Test
    void theTestNetworkOverrideSelectsThePilot() {
        var bundled = MaterialCatalog.bundled();
        var pilot = PilotCryogenicTestCatalog.catalog();
        assertEquals("createcheme:tjl20_methane_nitrogen", bundled.presets().network().packageId(), "the bundled network is unchanged");
        var network = pilot.presets().network();
        assertEquals(PilotCryogenicTestCatalog.PACKAGE_ID, network.packageId());
        assertEquals(List.of("water", "nitrogen", "methane", "ethane", "carbon_dioxide"), network.fluidPresets());
        assertEquals("CarbonDioxide", pilot.presets().fluid("carbon_dioxide").component());
        assertEquals(bundled.requirePackage(PilotCryogenicTestCatalog.PACKAGE_ID).fingerprint(),
                pilot.requirePackage(PilotCryogenicTestCatalog.PACKAGE_ID).fingerprint(), "the override selects, it does not change, the package");
        // One anchor per species per package: the pilot's liquids are anchored by their volume_translation, so the
        // global calibration points do not enter its fluid identity; the bundled network's do.
        var edited = PilotCryogenicTestCatalog.parseWith(CALIBRATION, o -> o.getAsJsonArray("points").get(0).getAsJsonObject()
                .addProperty("molarVolumeCubicMetres", 4.46e-5));
        assertEquals(pilot.fluidThermoFingerprint(PilotCryogenicTestCatalog.PACKAGE_ID), edited.fluidThermoFingerprint(PilotCryogenicTestCatalog.PACKAGE_ID));
        assertNotEquals(pilot.fluidThermoFingerprint("createcheme:tjl20_methane_nitrogen"), edited.fluidThermoFingerprint("createcheme:tjl20_methane_nitrogen"));
    }

    @Test
    void spineFingerprintIsDeterministicAndSeesOnlyNumbers() {
        var a = MaterialCatalog.parse(PilotCryogenicTestCatalog.resources()).requirePackage(PilotCryogenicTestCatalog.PACKAGE_ID);
        var b = MaterialCatalog.parse(PilotCryogenicTestCatalog.resources()).requirePackage(PilotCryogenicTestCatalog.PACKAGE_ID);
        assertEquals(a.spineFingerprint(), b.spineFingerprint());
        assertEquals(a.fingerprint(), b.fingerprint());
        assertEquals(PILOT_SPINE_FINGERPRINT, a.spineFingerprint(), "the pilot set's spine fingerprint moved: a spine or crystal number or the hash rule changed");
        String text = PilotCryogenicTestCatalog.parseWith(NITROGEN, o -> o.addProperty("source", "edited provenance text"))
                .requirePackage(PilotCryogenicTestCatalog.PACKAGE_ID).spineFingerprint();
        assertEquals(a.spineFingerprint(), text);
        var edited = PilotCryogenicTestCatalog.parseWith(NITROGEN, o -> o.getAsJsonObject("standard_entropy").addProperty("value_j_per_mol_kelvin", 191.6))
                .requirePackage(PilotCryogenicTestCatalog.PACKAGE_ID);
        assertFalse(a.spineFingerprint().equals(edited.spineFingerprint()), "a numeric edit must move the spine fingerprint");
        assertEquals(a.fingerprint(), edited.fingerprint(), "the package physics fingerprint does not see the spine");
        var crystal = PilotCryogenicTestCatalog.parseWith(CRYSTAL, o -> o.getAsJsonObject("anchor").addProperty("fusion_enthalpy_j_per_mol", 9000))
                .requirePackage(PilotCryogenicTestCatalog.PACKAGE_ID);
        assertFalse(a.spineFingerprint().equals(crystal.spineFingerprint()), "a crystal edit must move the spine fingerprint");
        // A volume_translation number is package physics (hashed through the property record); its texts are not.
        var anchor = PilotCryogenicTestCatalog.parseWith(PILOT_METHANE, o -> o.getAsJsonObject("volume_translation").addProperty("molar_volume_m3_per_mol", 4.55e-5))
                .requirePackage(PilotCryogenicTestCatalog.PACKAGE_ID);
        assertNotEquals(a.fingerprint(), anchor.fingerprint());
        var anchorText = PilotCryogenicTestCatalog.parseWith(PILOT_METHANE, o -> o.getAsJsonObject("volume_translation").addProperty("source", "edited"))
                .requirePackage(PilotCryogenicTestCatalog.PACKAGE_ID);
        assertEquals(a.fingerprint(), anchorText.fingerprint());
    }

    @Test
    void packageSpineRulesAreEnforced() {
        refused(PACKAGE, o -> o.getAsJsonArray("spine").remove(3), "expected one reference-spine record per component");
        refused(PACKAGE, o -> {
            JsonArray spine = o.getAsJsonArray("spine");
            spine.set(1, new JsonPrimitive("createcheme:spine_ethane"));
            spine.set(2, new JsonPrimitive("createcheme:spine_methane"));
        }, "is for Ethane, not component Methane");
        refused(PACKAGE, o -> o.getAsJsonArray("spine").set(0, new JsonPrimitive("createcheme:spine_argon")), "spine: unresolved createcheme:spine_argon");
        refused(METHANE, o -> o.addProperty("molar_mass_kg_per_mol", 0.01605), "above 1e-4");
        refused(PACKAGE, o -> o.getAsJsonArray("crystals").add("createcheme:crystal_carbon_dioxide_i"), "crystals: duplicate crystal record");
        refused(PACKAGE, o -> o.getAsJsonArray("crystals").set(0, new JsonPrimitive("createcheme:crystal_water_ih")), "crystals: unresolved");
        refused(CRYSTAL, o -> o.addProperty("component", "Water"), "is for Water, outside the package components");
    }

    /** D6: one ideal-gas source per species; ideal_gas_cp is optional only in a package that declares a spine. */
    @Test
    void idealGasSourceRules() {
        // A property record without a Cp fit in a package without a spine.
        refused(PACKAGE, o -> o.remove("spine"), "Property createcheme:pilot_nitrogen has no ideal_gas_cp: a package without a reference spine needs a shifted_polynomial_5 fit");
        // A Cp fit in a package with a spine.
        var fit = JsonParser.parseString(PilotCryogenicTestCatalog.read(TJL20_METHANE)).getAsJsonObject().getAsJsonObject("ideal_gas_cp");
        refused(PILOT_METHANE, o -> o.add("ideal_gas_cp", fit), "Property createcheme:pilot_methane carries ideal_gas_cp in a package with a reference spine");
        // The record itself stays loadable without a fit; only its use is refused.
        assertTrue(MaterialCatalog.bundled().requirePackage(PilotCryogenicTestCatalog.PACKAGE_ID).properties().get(1).cp().isEmpty());
    }

    @Test
    void volumeTranslationRules() {
        refused(PILOT_METHANE, o -> o.getAsJsonObject("volume_translation").addProperty("temperature_kelvin", 152.46),
                "volume_translation.temperature_kelvin: 152.46 K is not reduced_temperature x Tc");
        refused(PILOT_METHANE, o -> o.getAsJsonObject("volume_translation").addProperty("type", "standard_density"),
                "volume_translation.type: expected saturated_liquid_reduced_temperature");
        refused(PILOT_METHANE, o -> o.getAsJsonObject("volume_translation").addProperty("reduced_temperature", 1.0),
                "volume_translation.reduced_temperature: expected a value in (0, 1)");
        refused(PILOT_METHANE, o -> o.getAsJsonObject("volume_translation").remove("oracle"), "volume_translation.oracle: required field");
        refused(PILOT_METHANE, o -> o.getAsJsonObject("volume_translation").addProperty("molar_volume_m3_per_mol", -1), "volume_translation.molar_volume_m3_per_mol: must be positive");
        refused(PILOT_METHANE, o -> o.getAsJsonObject("models").remove("pr78"), "volume_translation.models.pr78: required");
    }

    @Test
    void spineAndCrystalRecordsNeedCoverageAndAKnownComponent() {
        refused(NITROGEN, o -> o.remove("coverage"), "coverage: required field");
        refused(NITROGEN, o -> o.getAsJsonObject("coverage").addProperty("grade", "good"), "coverage.grade: expected");
        refused(NITROGEN, o -> o.addProperty("component", "Argon"), "component: unresolved Argon");
        refused(CRYSTAL, o -> o.remove("coverage"), "coverage: required field");
        refused(NITROGEN, o -> o.addProperty("revision", "spine nitrogen r1"), "revision: expected a stable identifier");
    }

    static void refused(String path, Consumer<com.google.gson.JsonObject> edit, String message) {
        var error = assertThrows(IllegalArgumentException.class, () -> PilotCryogenicTestCatalog.parseWith(path, edit));
        assertTrue(error.getMessage().contains(message), () -> "expected '" + message + "' in: " + error.getMessage());
    }
}
