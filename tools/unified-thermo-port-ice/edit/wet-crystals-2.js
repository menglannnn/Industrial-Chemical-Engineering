// WP4: the wet crystal solve falls back to the dry crystal solve at pc where the free-water rule's hydrocarbon split has
// no answer of its own (a third fluid phase indicated, two liquids), as the dry path's crystal test does.
const fs=require('fs');const f=process.argv[2];let s=fs.readFileSync(f,'utf8');
function swap(from,to){if(!s.includes(from))throw new Error('missing: '+from.slice(0,80));s=s.replace(from,to);}
swap(`    /**
     * TP with competing crystals and free water (WP4; decision 6 A: gas hydrates are not modelled, so the free water and a
     * competing crystal coexist, ice beside CO2 frost): the free-water rule's answer of the whole feed first
     * ({@link #solveWet}, exact at its branch pressures), each competing crystal tested against its hydrocarbon phases
     * ({@code D = (mu_i - mu_s)/(R T)}, as {@link #solveWithCrystals}). No crystal forms: that answer, the solids assessed.
     * Otherwise the hydrocarbons and their crystal are solved dry ({@link #solveWithCrystals}) at the hydrocarbon partial
     * pressure {@code pc = P - p_w}, where their vapour lies, and the water is laid around them by its rule: the steam in
     * their vapour at the condensed water's vapour pressure and the rest condensed, or all of it steam where that leaves
     * the gas unsaturated; {@code pc} then follows the new gas (the fixed point of {@link #solveWet}, accepted on its stop
     * test). The liquid and the crystal are restated at the state pressure. A crystal forms only where the condensed water
     * is ice and {@code p_w} at most a few pascal (R14-08: 1.6 Pa at CO2's triple point, 216.6 K), so solving the liquid
     * and the crystal at {@code pc} instead of {@code P} moves their chemical potentials by {@code v p_w / (R T)}, below
     * 1e-7. A wet pure hydrocarbon at its crystal's own coexistence (undetermined by T and P) is the typed
     * {@link EquilibriumResult#CRYSTAL_STEP} failure.
     */
    private EquilibriumResult solveWetWithCrystals(double t, double p, boolean frozen, PhaseCompetition competition, Workspace w) {
        EquilibriumResult fluid = solveWet(t, p, w.hydrocarbonTotal, w.waterTotal, frozen, competition, w);
        w.fluidEquilibria = 1;
        if (!fluid.converged()) return fluid;
        double rt = R * t;
        StringBuilder forces = new StringBuilder();
        boolean forms = false;
        for (int a = 0; a < w.activeCount; a++) {
            Crystal c = w.active[a];
            if (!(w.feed[c.component()] > 0.0)) continue;
            double d = (wetChemicalPotential(fluid, c.component()) - c.anchored().chemicalPotential(t, p)) / rt;
            forces.append(forces.isEmpty() ? "" : ", ").append(c.id()).append(": (mu - mu_s)/RT = ").append(d);
            if (d > DEPOSITION_TOLERANCE) forms = true;
        }
        if (!forms) {
            return assessed(fluid, forces.isEmpty() ? "solids assessed: no competing crystal's species is carried"
                    : "solids assessed: no crystal forms (" + forces + ")", w);
        }
        EquilibriumResult.FreeWater wet = fluid.freeWater();
        double saturation = wet.saturationPressure();
        double water = w.waterTotal;
        double pc = wet.hydrocarbonPressure();
        for (int step = 0; ; step++) {`,
`    /**
     * TP with competing crystals and free water (WP4; decision 6 A: gas hydrates are not modelled, so the free water and a
     * competing crystal coexist, ice beside CO2 frost): the free-water rule's answer of the whole feed first
     * ({@link #solveWet}, exact at its branch pressures), each competing crystal tested against its hydrocarbon phases
     * ({@code D = (mu_i - mu_s)/(R T)}, as {@link #solveWithCrystals}). No crystal forms: that answer, the solids assessed.
     * Otherwise, and where the rule's hydrocarbon split has no answer of its own (a third fluid phase indicated, two
     * liquids: the dry feed's crystal replaces the subcooled liquid of its species there, P5), the hydrocarbons and their
     * crystal are solved as a dry feed ({@link #solveWithCrystals}) at the hydrocarbon partial pressure {@code pc = P - p_w},
     * where their vapour lies, and the water is laid around them by its rule: the steam in their vapour at the condensed
     * water's vapour pressure and the rest condensed, or all of it steam where that leaves the gas unsaturated; {@code pc}
     * then follows the new gas (the fixed point of {@link #solveWet}, accepted on its stop test), started at the rule's own
     * {@code pc}, else at the saturated gas's {@code P - p_w}. The liquid and the crystal are restated at the state
     * pressure. Solving them at {@code pc} instead of {@code P} moves their chemical potentials by {@code v p_w / (R T)}:
     * below 1e-7 where a crystal forms beside ice (R14-08: 1.6 Pa at CO2's triple point, 216.6 K), 2e-5 for a liquid
     * beside liquid water at 280 K. A wet pure hydrocarbon at its crystal's own coexistence (undetermined by T and P) is the
     * typed {@link EquilibriumResult#CRYSTAL_STEP} failure.
     */
    private EquilibriumResult solveWetWithCrystals(double t, double p, boolean frozen, PhaseCompetition competition, Workspace w) {
        EquilibriumResult fluid = solveWet(t, p, w.hydrocarbonTotal, w.waterTotal, frozen, competition, w);
        w.fluidEquilibria = 1;
        double rt = R * t;
        StringBuilder forces = new StringBuilder();
        EquilibriumResult.FreeWater wet = fluid.converged() ? fluid.freeWater() : null;
        if (wet != null) {
            boolean forms = false;
            for (int a = 0; a < w.activeCount; a++) {
                Crystal c = w.active[a];
                if (!(w.feed[c.component()] > 0.0)) continue;
                double d = (wetChemicalPotential(fluid, c.component()) - c.anchored().chemicalPotential(t, p)) / rt;
                forces.append(forces.isEmpty() ? "" : ", ").append(c.id()).append(": (mu - mu_s)/RT = ").append(d);
                if (d > DEPOSITION_TOLERANCE) forms = true;
            }
            if (!forms) {
                return assessed(fluid, forces.isEmpty() ? "solids assessed: no competing crystal's species is carried"
                        : "solids assessed: no crystal forms (" + forces + ")", w);
            }
        }
        double saturation = wet != null ? wet.saturationPressure() : frozen ? freeWater.ice().fugacity(t, p) : freeWater.saturationPressure(t);
        double water = w.waterTotal;
        double pc = wet != null ? wet.hydrocarbonPressure() : p > saturation + SATURATION_MARGIN ? p - saturation : p;
        for (int step = 0; ; step++) {`);
swap(`            if (!dry.classification().holdsCrystal()) {
                return assessed(fluid, "solids assessed: no crystal forms at the hydrocarbon partial pressure " + pc + " Pa (" + forces + ")", w);
            }`,
`            if (wet != null && !dry.classification().holdsCrystal()) {
                return assessed(fluid, "solids assessed: no crystal forms at the hydrocarbon partial pressure " + pc + " Pa (" + forces + ")", w);
            }`);
fs.writeFileSync(f,s);
