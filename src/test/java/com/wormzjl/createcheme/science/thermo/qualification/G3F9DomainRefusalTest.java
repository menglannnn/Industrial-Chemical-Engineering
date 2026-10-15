package com.wormzjl.createcheme.science.thermo.qualification;

import static com.wormzjl.createcheme.science.thermo.qualification.G3Support.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.createcheme.science.fluid.thermo.ThermoDomainViolation;
import com.wormzjl.createcheme.science.material.MaterialCatalog;
import com.wormzjl.createcheme.science.thermo.PengRobinsonKernel.Root;
import com.wormzjl.createcheme.science.thermo.phase.CubicPhaseEvaluator;
import com.wormzjl.createcheme.science.thermo.phase.EquilibriumRequest;
import com.wormzjl.createcheme.science.thermo.phase.EquilibriumResult;
import com.wormzjl.createcheme.science.thermo.phase.FluidTpEquilibrium;
import com.wormzjl.createcheme.science.thermo.phase.PhaseCompetition;
import com.wormzjl.createcheme.science.thermo.phase.PhaseContract;
import com.wormzjl.createcheme.science.thermo.phase.PhaseState;
import org.junit.jupiter.api.Test;

/**
 * G3 fixture family F9 (plan {@code P3_PILOT_ENGINE_PLAN.md} section 8.2; D8, D13): just outside each new boundary the
 * request is refused typed, naming the component, and just inside it is answered. Paths: the pilot network
 * ({@code createcheme:pilot_cryogenic}), the bundled network ({@code createcheme:tjl20_methane_nitrogen}), the pilot
 * evaluator (spine ranges) and the pilot engine's contract (competition and domain).
 */
class G3F9DomainRefusalTest {
    private final Table table = new Table("G3 F9", "Boundary", "Path", "Just inside (answered)", "Just outside", "Refusal: component, code");

    private void refusal(String boundary, FluidThermodynamics model, String label, double[] z, double tIn, double pIn, double tOut, double pOut,
                         String component, ThermoDomainViolation.Code code) {
        var inside = model.flashTP(tIn, pIn, z, () -> { });
        assertTrue(inside.volume() > 0, boundary + " inside");
        try {
            model.flashTP(tOut, pOut, z, () -> { });
            fail(boundary + ": " + tOut + " K " + pOut + " Pa was answered");
        } catch (ThermoDomainViolation refused) {
            assertEquals(component, refused.component(), boundary + ": " + refused.getMessage());
            assertEquals(code, refused.code(), boundary);
            table.row(boundary, label, f("%s K, %s MPa", number(tIn), f("%.4g", pIn / 1e6)), f("%s K, %s MPa", tOut, f("%.4g", pOut / 1e6)),
                    refused.component() + ", " + refused.code());
        }
    }

    @Test
    void refusedTypedJustOutsideEveryNewBoundary() {
        var pilot = pilot();
        var net = G3Support.network();
        double[] n2 = {1, 0, 0, 0, 0}, ch4 = {0, 1, 0, 0, 0}, c2h6 = {0, 0, 1, 0, 0}, co2 = {0, 0, 0, 1, 0};
        var pkg = net.hydrocarbon.components();
        double[] netN2 = new double[net.componentCount()], netCh4 = new double[net.componentCount()], netC2h6 = new double[net.componentCount()],
                netWater = new double[net.componentCount()];
        netN2[pkg.indexOf("Nitrogen")] = 1;
        netCh4[pkg.indexOf("Methane")] = 1;
        netC2h6[pkg.indexOf("Ethane")] = 1;
        netWater[net.componentCount() - 1] = 1;
        var above = ThermoDomainViolation.Code.THERMO_DOMAIN_PRESSURE_ABOVE;
        var cold = ThermoDomainViolation.Code.THERMO_DOMAIN_TEMPERATURE_BELOW;
        var hot = ThermoDomainViolation.Code.THERMO_DOMAIN_TEMPERATURE_ABOVE;
        // 10 MPa ceiling: every refusal names the carried component whose range the state leaves.
        refusal("10 MPa, N2", pilot, "pilot network", n2, 300, 1e7, 300, 1.005e7, "Nitrogen", above);
        refusal("10 MPa, CO2", pilot, "pilot network", co2, 400, 1e7, 400, 1.005e7, "CarbonDioxide", above);
        refusal("10 MPa, N2 (bundled)", net, "bundled network", netN2, 300, 1e7, 300, 1.005e7, "Nitrogen", above);
        refusal("10 MPa, CH4 (bundled)", net, "bundled network", netCh4, 300, 1e7, 300, 1.005e7, "Methane", above);
        refusal("2 MPa kept, C2H6 (bundled)", net, "bundled network", netC2h6, 350, 2e6, 350, 2.01e6, "Ethane", above);
        // Temperature floors.
        refusal("63.151 K, N2", pilot, "pilot network", n2, 63.2, 1e5, 63.1, 1e5, "Nitrogen", cold);
        refusal("63.151 K, N2 (bundled)", net, "bundled network", netN2, 63.2, 1e5, 63.1, 1e5, "Nitrogen", cold);
        refusal("90.694 K, CH4", pilot, "pilot network", ch4, 90.7, 1e5, 90.69, 1e5, "Methane", cold);
        refusal("90.368 K, C2H6", pilot, "pilot network", c2h6, 90.37, 1e5, 90.36, 1e5, "Ethane", cold);
        // P4 stage 2b: the pilot network requests the crystal competition for a dry gas carrying CO2, so CO2's network floor
        // is the competition's 90 K (a gas below its triple point deposits or holds typed); 216.592 K stays the fluid-only
        // floor of the engine's network contract (below) and of a liquid slot.
        refusal("90 K, CO2 gas (crystal competition)", pilot, "pilot network", co2, 90.1, 100, 89.9, 100, "CarbonDioxide", cold);
        refusal("293.15 K, CH4 (bundled)", net, "bundled network", netCh4, 293.15, 1e5, 293.1, 1e5, "Methane", cold);
        // Temperature ceilings: the pilot envelope 1200 K (methane's ideal gas ends there, D13), the bundled 900 K.
        refusal("1200 K, N2", pilot, "pilot network", n2, 1200, 2e5, 1200.5, 2e5, "Nitrogen", hot);
        refusal("1200 K, CH4", pilot, "pilot network", ch4, 1200, 2e5, 1200.5, 2e5, "Methane", hot);
        refusal("900 K (bundled)", net, "bundled network", netN2, 900, 2e5, 900.5, 2e5, "Nitrogen", hot);
        // Water: below its triple point, and liquid above Region 1.
        refusal("273.16 K, water", pilot, "pilot network", new double[] {0.5, 0, 0, 0, 0.5}, 273.2, 1e5, 273.1, 1e5, "Water", cold);
        refusal("273.16 K, water (bundled)", net, "bundled network", netWater, 273.2, 1e6, 273.1, 1e6, "Water", cold);
        var water = assertRefused(() -> pilot.waterLiquid(623.2, 2e7));
        pilot.waterLiquid(623.1, 2e7);
        assertEquals("Water", water.component());
        assertEquals(hot, water.code());
        table.row("Region 1 623.15 K, liquid water", "pilot network waterLiquid", "623.1 K, 20 MPa", "623.2 K, 20 MPa", water.component() + ", " + water.code());

        // The spine ranges through the evaluator: methane 90.6941-1200 K (D13), CO2 from 90 K (WP3), N2 to 6000 K.
        var evaluator = CubicPhaseEvaluator.forPackage(MaterialCatalog.bundled(), PILOT);
        var ws = evaluator.newWorkspace();
        var out = new PhaseState(4);
        evaluator.evaluate(1200, 2e5, new double[] {0, 1, 0, 0}, Root.VAPOR, ws, out);
        var spine = assertRefused(() -> evaluator.evaluate(1200.5, 2e5, new double[] {0, 1, 0, 0}, Root.VAPOR, ws, out));
        assertEquals("Methane", spine.component());
        assertEquals(hot, spine.code());
        table.row("methane spine 1200 K (D13)", "pilot evaluator", "1200 K, 0.2 MPa", "1200.5 K, 0.2 MPa", spine.component() + ", " + spine.code());
        evaluator.evaluate(1500, 2e5, new double[] {1, 0, 0, 0}, Root.VAPOR, ws, out);
        table.row("N2 spine to 6000 K (envelope 1200 K)", "pilot evaluator", "1500 K, 0.2 MPa (evaluates)", "-", "the envelope refuses above 1200 K");
        var methaneLow = assertRefused(() -> evaluator.evaluate(90.69, 1e5, new double[] {0, 1, 0, 0}, Root.LIQUID, ws, out));
        assertEquals("Methane", methaneLow.component());
        table.row("methane spine 90.6941 K", "pilot evaluator", "90.7 K", "90.69 K, 0.1 MPa", methaneLow.component() + ", " + methaneLow.code());
        evaluator.evaluate(90.0, 1e5, new double[] {0, 0, 0, 1}, Root.LIQUID, ws, out);
        var co2Low = assertRefused(() -> evaluator.evaluate(89.9, 1e5, new double[] {0, 0, 0, 1}, Root.LIQUID, ws, out));
        assertEquals("CarbonDioxide", co2Low.component());
        table.row("CO2 spine 90 K (WP3 extension)", "pilot evaluator", "90 K, 0.1 MPa", "89.9 K", co2Low.component() + ", " + co2Low.code());

        // The engine's contract: the CO2-I crystal is research-only, so no competition with it is qualified (D8).
        var engine = new FluidTpEquilibrium(PhaseContract.forNetworkPackage(MaterialCatalog.bundled(), PILOT), evaluator, FluidTpEquilibrium.Settings.DEFAULT, null);
        var ews = engine.newWorkspace();
        var components = engine.contract().components();
        var crystal = engine.tp(EquilibriumRequest.tp(180, 1e5, components, new double[] {0.99, 0, 0, 0.01, 0},
                PhaseCompetition.withCrystals("createcheme:crystal_carbon_dioxide_i")), ews);
        assertEquals(EquilibriumResult.Status.UNSUPPORTED, crystal.status());
        assertEquals(EquilibriumResult.UnsupportedReason.PHASE_COMPETITION_NOT_QUALIFIED, crystal.reason());
        table.row("CO2-I crystal (research-only record)", "pilot engine contract", "-", "competition with CO2-I at 180 K", crystal.status() + ", "
                + crystal.reason());
        var hydrate = engine.tp(EquilibriumRequest.tp(280, 1e6, components, new double[] {0.5, 0, 0, 0, 0.5}, new PhaseCompetition(new java.util.TreeSet<>(), true)), ews);
        assertEquals(EquilibriumResult.Status.UNSUPPORTED, hydrate.status());
        table.row("hydrates (D8)", "pilot engine contract", "-", "competition with hydrates", hydrate.status() + ", " + hydrate.reason());
        var pressure = engine.tp(EquilibriumRequest.tp(300, 1.005e7, components, new double[] {1, 0, 0, 0, 0}, PhaseCompetition.FLUID_ONLY), ews);
        assertEquals(EquilibriumResult.Status.OUT_OF_DOMAIN, pressure.status());
        table.row("10 MPa, N2", "pilot engine contract", "-", "300 K, 10.05 MPa", pressure.status() + ", " + pressure.violation().component() + ", "
                + pressure.violation().code());
        // A closed vessel whose state lies above the ceiling (P3 WP7d, the F8 finding): the UV search brackets against the
        // domain and refuses typed; its (U, V) are those of the liquid-like CH4/C2H6 0.5/0.5 at 283 K on an open contract.
        var open = researchEngine(evaluator, "f9-closed-vessel");
        double[] vessel = {0, 0.5, 0.5, 0};
        var beyond = open.tp(EquilibriumRequest.tp(283, 1.005e7, open.contract().components(), vessel, PhaseCompetition.FLUID_ONLY));
        var within = open.tp(EquilibriumRequest.tp(283, 9.95e6, open.contract().components(), vessel, PhaseCompetition.FLUID_ONLY));
        double[] closed = {0, 0.5, 0.5, 0, 0};
        var uvIn = engine.uv(EquilibriumRequest.uv(within.internalEnergy(), within.volume(), components, closed, PhaseCompetition.FLUID_ONLY), ews);
        assertTrue(uvIn.converged(), uvIn.detail());
        var uvOut = engine.uv(EquilibriumRequest.uv(beyond.internalEnergy(), beyond.volume(), components, closed, PhaseCompetition.FLUID_ONLY), ews);
        assertEquals(EquilibriumResult.Status.OUT_OF_DOMAIN, uvOut.status(), uvOut.detail());
        assertEquals(above, uvOut.violation().code());
        assertEquals(1e7, uvOut.violation().maximum());
        table.row("10 MPa, closed CH4/C2H6 vessel (UV)", "pilot engine contract", f("(U, V) of 283 K, 9.95 MPa: %.4g MPa", uvIn.phases().get(0).pressure() / 1e6),
                "(U, V) of 283 K, 10.05 MPa", uvOut.status() + ", " + uvOut.violation().component() + ", " + uvOut.violation().code()
                        + " (" + uvOut.diagnostics().outerIterations() + " TP equilibria)");
        var co2Contract = engine.tp(EquilibriumRequest.tp(216.5, 1e5, components, new double[] {0, 0, 0, 1, 0}, PhaseCompetition.FLUID_ONLY), ews);
        assertEquals(EquilibriumResult.Status.OUT_OF_DOMAIN, co2Contract.status());
        assertEquals("CarbonDioxide", co2Contract.violation().component());
        table.row("216.592 K, CO2", "pilot engine contract", "-", "216.5 K, 0.1 MPa", co2Contract.status() + ", " + co2Contract.violation().component());
        table.print("typed refusals just outside each new boundary (and the state just inside answered)");
    }

    static ThermoDomainViolation assertRefused(Runnable call) {
        try {
            call.run();
        } catch (ThermoDomainViolation refused) {
            return refused;
        }
        return fail("expected a ThermoDomainViolation");
    }
}
