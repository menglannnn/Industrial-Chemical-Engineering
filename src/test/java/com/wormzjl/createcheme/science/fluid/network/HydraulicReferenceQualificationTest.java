package com.wormzjl.createcheme.science.fluid.network;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.createcheme.science.material.MaterialCatalog;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/** Independent P03-P05 references. No production resistance call supplies an oracle. */
class HydraulicReferenceQualificationTest {
    private final FluidThermodynamics model = FluidThermodynamics.forNetwork(
            MaterialCatalog.bundled(), "createcheme:tjl20_methane_nitrogen", 1e-9);

    @Test void boundaryFlowsAcrossAllRegimesMatchOfflineDecimalFrictionInBothDirections() throws Exception {
        var rows = new ArrayList<Map<String, Object>>();
        try (var input = new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream(
                "/fluid/reference/hydraulic-friction.json")), StandardCharsets.UTF_8)) {
            var references = JsonParser.parseReader(input).getAsJsonObject().getAsJsonArray("rows");
            for (var element : references) {
                var reference = element.getAsJsonObject();
                double re = reference.get("reynolds").getAsDouble();
                double roughness = reference.get("relativeRoughness").getAsDouble();
                double friction = reference.get("specifiedDarcyFactor").getAsDouble();
                for (boolean water : new boolean[]{false, true}) {
                    var source = state(water, 200000);
                    double rho = source.mass() / source.volume();
                    double mu = water ? model.viscosity.waterLiquid(350)
                            : model.viscosity.vapor(350, source.vapor(), 0);
                    double diameter = .02, length = 1, area = Math.PI * diameter * diameter / 4;
                    double expectedFlow = re * Math.PI * mu * diameter / 4;
                    double velocity = expectedFlow / (rho * area);
                    assertTrue(velocity < model.velocityLimit(source), "Reference must not activate the velocity cap");
                    double pressureDrop = friction * length / diameter * rho * velocity * velocity / 2;
                    var sink = state(water, 200000 - pressureDrop);
                    var geometry = new PipeResistance.Geometry(length, diameter, roughness * diameter, 0);
                    for (boolean reverse : new boolean[]{false, true}) {
                        var generator = new PassiveNetwork.Reservoir(1, 0, source, PassiveNetwork.NodeKind.GENERATOR);
                        var drain = new PassiveNetwork.Reservoir(2, 0, sink, PassiveNetwork.NodeKind.VOID);
                        var graph = new PassiveNetwork(reverse ? List.of(drain, generator) : List.of(generator, drain),
                                List.of(new PassiveNetwork.Pipe(3, 0, 1, geometry)));
                        var actual = new PassiveStepSolver(model).solve(graph, 5, () -> {});
                        double expected = reverse ? -expectedFlow : expectedFlow;
                        assertEquals(expected, actual.massFlows()[0], 1e-10 + 1e-4 * expectedFlow);
                        assertEquals(FlowControl.Mode.PASSIVE, actual.modes().getFirst());
                        var row = new LinkedHashMap<String, Object>();
                        row.put("fluid", water ? "water" : "nitrogen");
                        row.put("reverse", reverse); row.put("reynolds", re);
                        row.put("relativeRoughness", roughness); row.put("pressureDropPa", pressureDrop);
                        row.put("expectedMassFlowKgPerSecond", expected);
                        row.put("actualMassFlowKgPerSecond", actual.massFlows()[0]);
                        row.put("relativeFlowError", Math.abs(actual.massFlows()[0] / expected - 1));
                        row.put("regime", reference.get("regime").getAsString());
                        if (!reference.get("colebrookDarcyFactor").isJsonNull()) {
                            row.put("colebrookCorrelationDifference", friction / reference.get("colebrookDarcyFactor").getAsDouble() - 1);
                        }
                        row.put("status", "PASS"); rows.add(row);
                    }
                }
            }
        }
        write("M2-turbulent-reference.json", rows);
    }

    @Test void individualGeometryChangesMatchPoiseuilleAndFittingReferencesIncludingNearZero() throws Exception {
        var rows = new ArrayList<Map<String, Object>>();
        var geometries = List.of(new PipeResistance.Geometry(1, .01, 0, 0),
                new PipeResistance.Geometry(10, .01, 0, 0),
                new PipeResistance.Geometry(1, .02, 0, 0),
                new PipeResistance.Geometry(1, .01, .0001, 0),
                new PipeResistance.Geometry(1, .01, 0, 7));
        for (var geometry : geometries) for (double massFlow : new double[]{-0.001, -1e-12, 0, 1e-12, .001}) {
            double rho = 1000, mu = .001, d = geometry.diameter(), volumeFlow = massFlow / rho;
            // Poiseuille in volumetric units plus independently evaluated local dynamic head.
            double expected = 8 * mu * geometry.length() * volumeFlow / (Math.PI * Math.pow(d / 2, 4));
            double velocity = volumeFlow / (Math.PI * d * d / 4);
            expected += geometry.minorLoss() * rho * velocity * Math.abs(velocity) / 2;
            double actual = PipeResistance.evaluate(geometry, massFlow, rho, mu).pressureDrop();
            assertEquals(expected, actual, 1e-12 + 1e-12 * Math.abs(expected));
            rows.add(Map.of("lengthMetres", geometry.length(), "diameterMetres", d,
                    "roughnessMetres", geometry.roughness(), "minorLoss", geometry.minorLoss(),
                    "massFlowKgPerSecond", massFlow, "expectedPressureDropPa", expected,
                    "actualPressureDropPa", actual, "status", "PASS"));
        }
        write("M2-geometry-reference.json", rows);
    }

    @Test void transitionHasMatchingOneSidedSlopesAndNoNegativeDifferentialResistance() throws Exception {
        var rows = new ArrayList<Map<String, Object>>();
        for (double roughness : new double[]{0, .000045, .001}) {
            var pipe = new PipeResistance.Geometry(20, .05, roughness, 2);
            double reToMass = Math.PI * .001 * .05 / 4;
            for (double boundary : new double[]{2000, 4000}) for (int sign : new int[]{-1, 1}) {
                double q = sign * boundary * reToMass, h = reToMass * .001;
                var at = PipeResistance.evaluate(pipe, q, 900, .001);
                double left = (at.pressureDrop() - PipeResistance.evaluate(pipe, q - h, 900, .001).pressureDrop()) / h;
                double right = (PipeResistance.evaluate(pipe, q + h, 900, .001).pressureDrop() - at.pressureDrop()) / h;
                assertEquals(left, right, 1e-5 * at.massFlowDerivative());
                assertEquals(at.massFlowDerivative(), (left + right) / 2, 1e-5 * at.massFlowDerivative());
                rows.add(Map.of("reynoldsBoundary", boundary, "sign", sign, "roughnessMetres", roughness,
                        "leftSlope", left, "rightSlope", right, "analyticSlope", at.massFlowDerivative(), "status", "PASS"));
            }
            for (int re = 0; re <= 6000; re += 10) {
                var value = PipeResistance.evaluate(pipe, re * reToMass, 900, .001);
                assertTrue(Double.isFinite(value.massFlowDerivative()) && value.massFlowDerivative() > 0);
            }
        }
        write("M2-transition-reference.json", rows);
    }

    // Retired with owner decision D54 (every vessel a bag with a stiff wall), thread t24, 2026-10-14:
    // twoFiniteVesselsMatchSmallSignalComplianceWithReversedInitialPressures compared two closed nitrogen vessels at 1 kPa
    // with the dilute ideal-gas compliance to 1e-4. A bag cannot hold 1 kPa (it goes slack at the ambient), and at the lowest
    // pressure it can hold, 1 kPa above the ambient with the interval and steps scaled to the same decay, the real gas
    // stands 1.7e-3 from the dilute reference (measured, logs es09). The test is in the history at 50145147.

    private FluidThermodynamics.State state(boolean water, double pressure) {
        double[] n = new double[com.wormzjl.createcheme.science.material.MaterialTestBasis.NETWORK+1]; n[water ? com.wormzjl.createcheme.science.material.MaterialTestBasis.NETWORK : com.wormzjl.createcheme.science.material.MaterialTestBasis.NITROGEN] = 1;
        var unit = model.flashTP(350, pressure, n, () -> {});
        n[water ? com.wormzjl.createcheme.science.material.MaterialTestBasis.NETWORK : com.wormzjl.createcheme.science.material.MaterialTestBasis.NITROGEN] /= unit.volume();
        return model.flashTP(350, pressure, n, () -> {});
    }

    private static void write(String name, Object rows) throws Exception {
        var path = Path.of("build/reports/fluid", name); Files.createDirectories(path.getParent());
        Files.writeString(path, new GsonBuilder().setPrettyPrinting().create().toJson(rows));
    }
}
