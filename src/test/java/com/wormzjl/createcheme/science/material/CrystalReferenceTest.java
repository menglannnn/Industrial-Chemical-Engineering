package com.wormzjl.createcheme.science.material;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

/**
 * The crystal record kind (batch 2026-09-24-coolprop-low-temperature): the CO2-I record is the Jaeger and Span 2012
 * Gibbs function (P4) and its generic accessors agree with the function; a synthetic complete segment record (P2, not a
 * real substance) checks the anchored segment model; the loader's rules of both kinds are refused with named reasons.
 */
class CrystalReferenceTest {
    private static final double TT = 200, PT = 5e5, FUSION = 8000;

    /**
     * Synthetic crystal: Cp = 20 + 0.1 T below 150 K then a two-point table to 250 K, v0 = 3e-5 m3/mol at 180 K and
     * 1e5 Pa, alpha 2e-4 /K, kappa 1e-10 /Pa, one transition at 120 K. Anchored at 200 K and 5e5 Pa.
     */
    private static final String SYNTHETIC = """
            {"schema_version":1,"id":"createcheme:crystal_synthetic_a","component":"Nitrogen","crystal":"synthetic phase A",
             "revision":"synthetic-r1","source":"synthetic test record, not a real substance",
             "heat_capacity":{"segments":[
               {"type":"polynomial","temperature_min_kelvin":50,"temperature_max_kelvin":150,"coefficients":[20,0.1],"source":"synthetic"},
               {"type":"table","temperature_min_kelvin":150,"temperature_max_kelvin":250,"temperatures_kelvin":[150,200,250],
                "values_j_per_mol_kelvin":[35,42,45],"source":"synthetic"}]},
             "molar_volume":{"reference_temperature_kelvin":180,"reference_pressure_pascal":100000,"value_m3_per_mol":3e-5,
               "thermal_expansion_per_kelvin":2e-4,"isothermal_compressibility_per_pascal":1e-10,"temperature_min_kelvin":50,
               "temperature_max_kelvin":250,"pressure_min_pascal":1,"pressure_max_pascal":1e8,"source":"synthetic"},
             "transitions":[{"temperature_kelvin":120,"enthalpy_j_per_mol":500,"volume_change_m3_per_mol":1e-7,"to_crystal":"synthetic phase A"}],
             "anchor":{"type":"triple_point","temperature_kelvin":200,"pressure_pascal":500000,"fusion_enthalpy_j_per_mol":8000,"source":"synthetic"},
             "coverage":{"grade":"estimated_declared_error","evidence":"synthetic"}}
            """;

    private static CrystalReference synthetic(Consumer<JsonObject> edit) {
        JsonObject o = JsonParser.parseString(SYNTHETIC).getAsJsonObject();
        edit.accept(o);
        return CrystalReference.read(o);
    }

    private static final String CRYSTAL_PATH = "data/createcheme/materials/crystals/carbon_dioxide_i.json";

    /** The bundled CO2-I record as JSON, for edits. */
    private static JsonObject carbonDioxide() {
        return JsonParser.parseString(PilotCryogenicTestCatalog.read(CRYSTAL_PATH)).getAsJsonObject();
    }

    @Test
    void carbonDioxideRecordIsTheJaegerSpanFunction() {
        var crystal = PilotCryogenicTestCatalog.catalog().crystals().get("createcheme:crystal_carbon_dioxide_i");
        assertEquals("CarbonDioxide", crystal.component());
        assertEquals("crystal-carbon_dioxide-i-r2", crystal.revision());
        assertEquals(CrystalReference.Kind.JAEGER_SPAN_2012, crystal.kind());
        assertEquals(MaterialCoverage.Grade.ESTIMATED_DECLARED_ERROR, crystal.coverage().grade());
        assertTrue(crystal.usable(), crystal.missing()::toString);
        assertTrue(crystal.transitions().isEmpty());
        assertEquals(216.592, crystal.anchor().temperature(), 0.0);
        assertEquals(517950, crystal.anchor().pressure(), 0.0);
        assertEquals(9019, crystal.anchor().fusionEnthalpy(), 0.0);
        assertEquals(80, crystal.minimumTemperature(), 0.0);
        assertEquals(300, crystal.maximumTemperature(), 0.0);
        assertEquals(5e8, crystal.maximumPressure(), 0.0);
        var js = crystal.gibbsFunction();
        // The generic accessors are the function's (independent of g0 and g1).
        for (double[] state : new double[][] {{216.592, 517950}, {100, 1e8}, {180, 1e5}, {250, 2e8}}) {
            var f = js.evaluate(state[0], state[1], 0, 0);
            assertEquals(f.heatCapacity(), crystal.heatCapacity(state[0], state[1]), 1e-12 * f.heatCapacity());
            assertEquals(f.volume(), crystal.molarVolume(state[0], state[1]), 1e-12 * f.volume());
            assertEquals(f.expansion(), crystal.expansion(state[0], state[1]), 1e-12 * f.expansion());
            assertEquals(f.compressibility(), crystal.compressibility(state[0], state[1]), 1e-12 * f.compressibility());
        }
        assertEquals(0, crystal.enthalpyChange(216.592, 517950), 0.0);
        assertEquals(0, crystal.entropyChange(216.592, 517950), 0.0);
        // Anchored on arbitrary liquid values: the solved constants and the generic difference path give one function.
        double muL = -437_397.04, hL = -412_054.04, sS = crystal.anchorEntropy(muL, hL);
        assertEquals((hL - muL) / 216.592 - 9019 / 216.592, sS, 1e-12);
        var anchored = crystal.state(216.592, 517950, muL, hL);
        assertEquals(muL, anchored.gibbs(), 1e-9 * Math.abs(muL));
        assertEquals(hL - 9019, anchored.enthalpy(), 1e-9 * Math.abs(hL));
        assertEquals(sS, anchored.entropy(), 1e-9);
        for (double t : new double[] {80, 130, 195, 216.592, 260, 300}) {
            for (double p : new double[] {1, 1e5, 1e7, 4e8}) {
                var s = crystal.state(t, p, muL, hL);
                assertEquals(s.gibbs(), crystal.gibbs(t, p, muL, hL), 1e-9 * Math.abs(s.gibbs()), "g at " + t + " K, " + p + " Pa");
                assertEquals(s.enthalpy() - t * s.entropy(), s.gibbs(), 1e-9 * Math.abs(s.gibbs()));
                assertEquals(sS + crystal.entropyChange(t, p), s.entropy(), 1e-9);
            }
        }
        var outside = assertThrows(IllegalArgumentException.class, () -> crystal.heatCapacity(79, 1e5));
        assertTrue(outside.getMessage().contains("outside 80.0..300.0 K"), outside.getMessage());
        assertThrows(IllegalArgumentException.class, () -> crystal.state(200, 6e8, muL, hL));
    }

    @Test
    void gibbsFunctionRecordRulesAreRefused() {
        refusedCarbonDioxide(o -> o.add("heat_capacity", new JsonObject()), "gibbs_function: a record with a Gibbs function carries no heat_capacity");
        refusedCarbonDioxide(o -> o.add("transitions", new JsonArray()), "carries no transitions");
        refusedCarbonDioxide(o -> gibbs(o).addProperty("type", "trusler_2011"), "gibbs_function.type: expected jaeger_span_2012");
        refusedCarbonDioxide(o -> gibbs(o).getAsJsonArray("g2_to_g10").remove(8), "g2_to_g10: expected 9 to 9 numbers");
        refusedCarbonDioxide(o -> gibbs(o).getAsJsonArray("g2_to_g10").set(8, new JsonPrimitive(-1)), "g10 must be positive");
        refusedCarbonDioxide(o -> gibbs(o).getAsJsonArray("g_kappa_0_to_2").add(0.1), "g_kappa_0_to_2: expected 3 to 3 numbers");
        refusedCarbonDioxide(o -> gibbs(o).addProperty("n", 1), "gibbs_function.n: expected a finite value above 1");
        refusedCarbonDioxide(o -> gibbs(o).addProperty("temperature_max_kelvin", 70), "invalid temperature or pressure range");
        refusedCarbonDioxide(o -> gibbs(o).remove("published_g0"), "published_g0");
        // An anchor outside the function's range makes the record incomplete: loadable only as research_only.
        refusedCarbonDioxide(o -> o.getAsJsonObject("anchor").addProperty("temperature_kelvin", 310),
                "an incomplete crystal record must be research_only or unavailable");
        JsonObject incomplete = carbonDioxide();
        incomplete.getAsJsonObject("anchor").addProperty("temperature_kelvin", 310);
        incomplete.getAsJsonObject("coverage").addProperty("grade", "research_only");
        var record = CrystalReference.read(incomplete);
        assertFalse(record.usable());
        assertTrue(record.missing().getFirst().contains("does not reach the triple-point anchor"), record.missing()::toString);
        assertThrows(IllegalStateException.class, () -> record.heatCapacity(200, 1e5));
        assertThrows(IllegalStateException.class, () -> record.integrationConstants(0, 0));
    }

    private static JsonObject gibbs(JsonObject o) { return o.getAsJsonObject("gibbs_function"); }

    private static void refusedCarbonDioxide(Consumer<JsonObject> edit, String message) {
        JsonObject o = carbonDioxide();
        edit.accept(o);
        var error = assertThrows(IllegalArgumentException.class, () -> CrystalReference.read(o));
        assertTrue(error.getMessage().contains(message), () -> "expected '" + message + "' in: " + error.getMessage());
    }

    @Test
    void anchoredGibbsFunctionIsConsistent() {
        var c = synthetic(o -> {});
        assertTrue(c.usable());
        assertEquals(0, c.enthalpyChange(TT, PT), 0.0);
        assertEquals(0, c.entropyChange(TT, PT), 0.0);
        double muL = -12345.6, hL = 4321.0, sS = c.anchorEntropy(muL, hL);
        assertEquals((hL - muL) / TT - FUSION / TT, sS, 1e-12);
        assertEquals(muL, c.gibbs(TT, PT, muL, hL), 1e-9);
        // The solid's own enthalpy and Gibbs energy at the anchor: h_s = h_L - dH_fus, g_s = h_s - T s_s = mu_L.
        assertEquals(hL - FUSION - TT * sS, muL, 1e-9);
        for (double t : new double[] {60, 100, 135, 175, 199, 230, 249}) {
            for (double p : new double[] {1e3, 1e5, 5e5, 3e7}) {
                double dt = 1e-3, dp = 1e2;
                double s = sS + c.entropyChange(t, p);
                assertEquals(-s, (c.gibbs(t + dt, p, muL, hL) - c.gibbs(t - dt, p, muL, hL)) / (2 * dt), 1e-6 * Math.max(Math.abs(s), 1), "dg/dT at " + t + ", " + p);
                assertEquals(c.molarVolume(t, p), (c.gibbs(t, p + dp, muL, hL) - c.gibbs(t, p - dp, muL, hL)) / (2 * dp), 1e-6 * c.molarVolume(t, p), "dg/dP");
                double cp = c.heatCapacity(t, p);
                assertEquals(cp, (c.enthalpyChange(t + dt, p) - c.enthalpyChange(t - dt, p)) / (2 * dt), 1e-6 * cp, "dh/dT at " + t);
                assertEquals(cp / t, (c.entropyChange(t + dt, p) - c.entropyChange(t - dt, p)) / (2 * dt), 1e-6 * cp / t, "ds/dT at " + t);
                // The one-call state of a segment record composes the same function.
                var state = c.state(t, p, muL, hL);
                assertEquals(c.gibbs(t, p, muL, hL), state.gibbs(), 1e-9 * Math.max(1, Math.abs(state.gibbs())));
                assertEquals(s, state.entropy(), 1e-12 * Math.max(1, Math.abs(s)));
                assertEquals(c.molarVolume(t, p), state.volume(), 0.0);
                assertEquals(2e-4 * 3e-5 / c.molarVolume(t, p), state.expansion(), 1e-15);
                assertEquals(1e-10 * 3e-5 / c.molarVolume(t, p), state.compressibility(), 1e-22);
            }
        }
        assertEquals(CrystalReference.Kind.SEGMENTS, c.kind());
        assertEquals(null, c.gibbsFunction());
        assertThrows(IllegalStateException.class, () -> c.integrationConstants(muL, hL));
    }

    @Test
    void closedFormsAndTransitionJumps() {
        var c = synthetic(o -> {});
        // Cp in the polynomial segment and in the table.
        assertEquals(30, c.heatCapacity(100, 1e5), 1e-12);
        assertEquals(38.5, c.heatCapacity(175, 1e5), 1e-12);
        // The transition at 120 K adds its enthalpy, dH/T and volume change.
        double e = 1e-9;
        assertEquals(500, c.enthalpyChange(120 + e, 1e5) - c.enthalpyChange(120 - e, 1e5), 1e-4);
        assertEquals(500 / 120.0, c.entropyChange(120 + e, 1e5) - c.entropyChange(120 - e, 1e5), 1e-6);
        assertEquals(1e-7, c.molarVolume(120 + e, 1e5) - c.molarVolume(120 - e, 1e5), 1e-15);
        // Volume at the reference state and its linear response.
        assertEquals(3e-5, c.molarVolume(180, 1e5), 1e-18);
        assertEquals(3e-5 * (1 + 2e-4 * 20 - 1e-10 * 9.9e6), c.molarVolume(200, 1e7), 1e-18);
        // Enthalpy between two table nodes at the anchor pressure: the integral of the linear Cp.
        assertEquals(0.5 * (42 + 45) * 50, c.enthalpyChange(250, PT), 1e-9);
    }

    @Test
    void recordRulesAreRefused() {
        refused(o -> o.getAsJsonArray("transitions").get(0).getAsJsonObject().addProperty("temperature_kelvin", 260), "lies outside the heat-capacity range");
        refused(o -> segment(o, 0).addProperty("temperature_max_kelvin", 151), "heat_capacity.segments[1]: overlaps segment 0");
        refused(o -> segment(o, 0).addProperty("temperature_max_kelvin", 149), "heat_capacity.segments[1]: gap after segment 0");
        refused(o -> segment(o, 1).addProperty("temperature_min_kelvin", 151), "temperatures_kelvin: must start at temperature_min_kelvin");
        refused(o -> o.getAsJsonObject("molar_volume").remove("thermal_expansion_per_kelvin"), "an incomplete crystal record must be research_only or unavailable");
        refused(o -> o.getAsJsonObject("anchor").addProperty("temperature_kelvin", 260), "an incomplete crystal record must be research_only or unavailable");
        refused(o -> o.getAsJsonObject("anchor").addProperty("type", "melting_point"), "anchor.type: expected triple_point");
        refused(o -> o.getAsJsonObject("molar_volume").addProperty("reference_temperature_kelvin", 300), "reference state lies outside its declared range");
        refused(o -> o.remove("coverage"), "coverage: required field");
        refused(o -> {
            var second = o.getAsJsonArray("transitions").get(0).getAsJsonObject().deepCopy();
            second.addProperty("temperature_kelvin", 110);
            o.getAsJsonArray("transitions").add(second);
        }, "transitions: temperatures must increase");
        refused(o -> segment(o, 1).getAsJsonArray("values_j_per_mol_kelvin").set(1, new JsonPrimitive(-1)), "values_j_per_mol_kelvin: must be positive");
        refused(o -> segment(o, 0).addProperty("type", "debye"), "unsupported heat-capacity segment debye");
        var incomplete = synthetic(o -> {
            o.getAsJsonObject("molar_volume").remove("isothermal_compressibility_per_pascal");
            o.getAsJsonObject("coverage").addProperty("grade", "research_only");
        });
        assertFalse(incomplete.usable());
        assertThrows(IllegalStateException.class, () -> incomplete.enthalpyChange(210, 5e5));
    }

    private static JsonObject segment(JsonObject o, int index) {
        return o.getAsJsonObject("heat_capacity").getAsJsonArray("segments").get(index).getAsJsonObject();
    }

    private static void refused(Consumer<JsonObject> edit, String message) {
        var error = assertThrows(IllegalArgumentException.class, () -> synthetic(edit));
        assertTrue(error.getMessage().contains(message), () -> "expected '" + message + "' in: " + error.getMessage());
    }
}
