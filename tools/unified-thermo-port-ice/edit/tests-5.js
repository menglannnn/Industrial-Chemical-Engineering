// WP4 offline edit helper: WaterIntoCryogenicMethaneTest's interim ice hold becomes the network ice gate (c).
const fs = require("fs");
const f = process.argv[2] + "/src/test/java/com/wormzjl/createcheme/science/fluid/network/WaterIntoCryogenicMethaneTest.java";
let s = fs.readFileSync(f, "utf8");
const rep = (a, b) => { if (!s.includes(a)) throw new Error("missing: " + a.slice(0, 120)); s = s.replace(a, b); };
rep(` * enters. Wherever water is driven into a vessel that falls below 273.16 K (the water generator at 1 MPa) the liquid water
 * would freeze: until ice is modelled (WP4 of the range-extrapolation plan) that is the interim ice hold, a phase hold
 * (leaving the water model's data is none since the owner rule of 2026-10-16). That hold names the vessel: it refuses
 * the line search of the vessel's closure, and the reconstruction that closes the vessel names the failed closure as it names a hold (the design
 * review's section 5; before it the hold named no node, an open item of milestone M3).</p>
 */`, ` * enters. Wherever water is driven into a vessel that falls below its melting line (the water generator at 1 MPa) the
 * water freezes (WP4 of the range-extrapolation plan, its gate (c)): the pipe and the vessel's steps carry it as its
 * stand-in, supercooled liquid water, and the vessel's refresh freezes it into its ice population, with the latent heat
 * in the vessel's energy, so more of the methane boils; nothing holds (the interim ice hold before WP4).</p>
 */`);
rep(`    /** Water driven in at 1 MPa while the methane cools the vessel below 273.16 K: the interim ice hold, naming the vessel. */
    @Test
    void waterDrivenIntoTheCooledVesselHoldsOnTheInterimIceHold() {
        var solver = new PassiveIntervalSolver(model);
        RuntimeException refused = assertThrows(RuntimeException.class, () -> {
            var graph = island(120, 1.2e6, 1e6);
            for (int k = 0; k < 4; k++) graph = solver.solve(graph, 5, PassiveIntervalSolver.Settings.defaults(), () -> {}, 0.05).graph();
        });
        System.out.println("water at 1 MPa: " + refused.getMessage());
        assertFalse(refused.getMessage().contains("Negative/nonfinite inventory"), "no untyped inventory refusal: " + refused.getMessage());
        var hold = assertInstanceOf(FluidThermodynamics.UnsupportedPhases.class, SparseNewton.domainViolation(refused), "a typed phase hold: " + refused.getMessage());
        assertEquals(FluidThermodynamics.UnsupportedPhases.Kind.ICE, hold.kind(), hold.getMessage());
        assertTrue(hold.temperature() < 273.16, hold.getMessage());
        assertEquals(VESSEL, hold.node(), "the hold names the vessel: " + hold.getMessage());
    }`, `    /** The vessel's ice, kg: its population of the free water's ice (WP4). */
    private static double ice(PassiveNetwork.Reservoir vessel) {
        double kg = 0;
        for (var population : vessel.inventory().solids().crystals()) if (population.material().id().equals(ICE)) kg += population.massKg();
        return kg;
    }

    /**
     * WP4 gate (c): water driven in at 1 MPa while the methane cools the vessel below its melting line freezes at the
     * vessel's refresh, and nothing holds. Physics: water is immiscible with the methane and nitrogen, and below its
     * melting line its vapour pressure is the ice's (5.6e-12 Pa at 111.6 K), so all of it freezes; from 25 °C to ice at the
     * vessel's temperature it gives up about 12 kJ/mol (1.9 liquid, 6.0 fusion, 4.4 ice), which boils about 1.5 mol of
     * methane (8.2 kJ/mol) per mol of water: the vessel's methane leaves as gas to the void.
     */
    @Test
    void waterDrivenIntoTheCooledVesselFreezesAtTheRefresh() {
        var solver = new PassiveIntervalSolver(model);
        var graph = island(120, 1.2e6, 1e6);
        double step = 0.05;
        double frozen = 0;
        for (int k = 0; k < INTERVALS; k++) {
            var result = solver.solve(graph, 5, PassiveIntervalSolver.Settings.defaults(), () -> {}, step);
            step = solver.nextStepEstimate();
            graph = result.graph();
            var vessel = graph.reservoirs().get(1);
            var state = vessel.state();
            System.out.printf(Locale.ROOT, "WP4 gate (c) interval %d: vessel %.3f K %.1f Pa, water %.4e mol (stand-in %.4e mol, steam %.4e mol), ice %.6f kg, "
                            + "methane liquid %.4e mol vapour %.4e mol (%d substeps, %d rejected, %s)%n", k + 1, state.temperature(), state.pressure(),
                    vessel.inventory().moles()[WATER], state.waterLiquid(), state.waterVapor(), ice(vessel), state.liquidView()[METHANE], state.vaporView()[METHANE],
                    result.acceptedSubsteps(), result.rejectedSubsteps(), result.rejectionReasons());
            assertTrue(result.rejectionReasons().keySet().stream().noneMatch(r -> r.startsWith(com.wormzjl.createcheme.science.fluid.thermo.ThermoHold.REASON_PREFIX)),
                    "no thermo hold: " + result.rejectionReasons());
            frozen = Math.max(frozen, ice(vessel));
        }
        var vessel = graph.reservoirs().get(1);
        assertTrue(frozen > 0, "the water froze at a refresh");
        assertTrue(vessel.state().temperature() < 273.0, "the vessel is below the melting line: " + vessel.state().temperature());
    }`);
rep(`    private static final int WATER = 4;`, `    private static final int WATER = 4;
    private static final int METHANE = 1;
    private static final int INTERVALS = 6;
    private static final String ICE = "createcheme:crystal_ice_ih";`);
fs.writeFileSync(f, s);
console.log("ok");
