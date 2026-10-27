// WP4 offline edit helper: generalizes VesselCrystals' presence, drive and size to the free water's ice.
const fs = require("fs");
const f = process.argv[2];
let s = fs.readFileSync(f, "utf8");
const rep = (a, b) => { if (!s.includes(a)) throw new Error("missing: " + a.slice(0, 100)); s = s.replace(a, b); };
rep(` * A vessel with free water, or above the crystals' ceiling, holds no crystal (D20 (3), D8): its crystals melt into the
 * fluid. A vessel holding no fluid is main's dry-solids vessel (OD-P4): its crystals have no fluid to exchange with.
 */`, ` * Above the crystals' ceiling a vessel holds none of the package's crystals: they melt into the fluid. The free water's
 * ice (WP4) is water's own rule, on every package: free water below its melting line freezes here, from its liquid (the
 * stand-in the step carried it as) and from the gas's steam, which keeps the ice's sublimation pressure; gas hydrates are
 * not modelled (decision 6 A), so ice and a competing crystal coexist. A vessel holding no fluid is main's dry-solids
 * vessel (OD-P4): its crystals have no fluid to exchange with.
 */`);
rep(`    /**
     * Whether crystals compete in a vessel holding {@code inventory} near {@code state}: the package admits them and the
     * vessel's species, its crystals' included, are dry and carry a crystal's species at or below the crystals' ceiling.
     */
    static boolean competes(FluidThermodynamics model,PassiveNetwork.Inventory inventory,FluidThermodynamics.State state) {
        var crystals=model.crystalSolids();
        if(crystals.isEmpty()||PhaseLayout.sum(inventory.moles())==0)return false;
        return model.crystalsCompete(state.temperature(),totals(crystals,inventory,held(crystals,inventory)));
    }`, `    /**
     * Whether a crystal may be present in a vessel holding {@code inventory} near {@code state}: its species, its crystals'
     * included, hold water (the free water's ice, WP4) or are under the package's crystal competition (a crystal's species
     * at or below the crystals' ceiling, {@link FluidThermodynamics#crystalsCompete}).
     */
    static boolean competes(FluidThermodynamics model,PassiveNetwork.Inventory inventory,FluidThermodynamics.State state) {
        var crystals=model.crystalSolids();
        if(PhaseLayout.sum(inventory.moles())==0)return false;
        double[] totals=totals(crystals,inventory,held(crystals,inventory));
        return totals[totals.length-1]>0||model.crystalsCompete(state.temperature(),totals);
    }`);
rep(`        var crystals=model.crystalSolids();
        if(crystals.isEmpty())return null;
        var all=crystals.all();int count=all.size();`, `        var crystals=model.crystalSolids();
        var all=crystals.all();int count=all.size();`);
rep(`        // Free water or a temperature above the crystals' ceiling: no crystal competes, and what a vessel holds melts.
        if(!model.crystalsCompete(t,totals))return holds&&isClosed?melt(model,inventory,closed,totals,trueEnergy,checkpoint):null;
        boolean drive=!isClosed;
        for(int k=0;k<count&&!drive;k++) {
            var crystal=all.get(k);
            double d=drive(closed,crystal,t,p);
            if(held[k]>0?!(Math.abs(d)<=DEADBAND):d>DEADBAND)drive=true;
        }`, `        // The package's crystals compete at or below their ceiling, the free water's ice wherever the vessel holds water
        // (WP4); neither: what a vessel holds melts. A held crystal that cannot be present melts in the crystal test.
        boolean competing=model.crystalsCompete(t,totals),wet=totals[totals.length-1]>0;
        if(!competing&&!wet)return holds&&isClosed?melt(model,inventory,closed,totals,trueEnergy,checkpoint):null;
        boolean drive=!isClosed;
        for(int k=0;k<count&&!drive;k++) {
            var crystal=all.get(k);
            if(!(crystal.water()?wet:competing)){drive=held[k]>0;continue;}
            double d=drive(model,closed,crystal,t,p);
            if(held[k]>0?!(Math.abs(d)<=DEADBAND):d>DEADBAND)drive=true;
        }`);
rep(`            answer=taut?model.crystalsAtVolume(totals,crystalEnergy,capacity-inertMoments.volume(),warm?t:Double.NaN,warm?p:Double.NaN,checkpoint)
                    :model.crystalsAtPressure(totals,crystalEnergy+PhaseLayout.AMBIENT_PRESSURE*(bag-inertMoments.volume()),PhaseLayout.AMBIENT_PRESSURE,checkpoint);`, `            answer=taut?model.crystalsAtVolume(totals,crystalEnergy,capacity-inertMoments.volume(),warm?t:Double.NaN,warm?p:Double.NaN,competing,checkpoint)
                    :model.crystalsAtPressure(totals,crystalEnergy+PhaseLayout.AMBIENT_PRESSURE*(bag-inertMoments.volume()),PhaseLayout.AMBIENT_PRESSURE,competing,checkpoint);`);
rep(`    /** The fluid's drive to {@code crystal} at {@code state}: {@code ln f_i(fluid) - ln f_s(T, P)}, read in the gas where
     * the state holds the component there, else in the liquid; minus infinity where the fluid holds none of it. */
    static double drive(FluidThermodynamics.State state,NetworkCrystals.Crystal crystal,double t,double p) {
        return logFugacity(state,crystal.component(),state.vaporView()[crystal.component()]>0)-crystal.logFugacityCoefficient(t,p);
    }`, `    /**
     * The fluid's drive to {@code crystal} at {@code state}: {@code ln f_i(fluid) - ln f_s(T, P)}, read in the gas where
     * the state holds the component there, else in the liquid; minus infinity where the fluid holds none of it. For the
     * free water's ice (WP4) the water's fugacity is its steam's partial pressure, else its liquid's saturation pressure;
     * minus infinity in a gas holding no steam (the ice sublimes into it); where the fluid holds no water and no gas the
     * ice has nothing to exchange with but its own melt, and its drive is the liquid's against it where that is negative
     * (above the melting line), else zero.
     */
    static double drive(FluidThermodynamics model,FluidThermodynamics.State state,NetworkCrystals.Crystal crystal,double t,double p) {
        if(!crystal.water())return logFugacity(state,crystal.component(),state.vaporView()[crystal.component()]>0)-crystal.logFugacityCoefficient(t,p);
        if(state.waterVapor()>0||state.waterLiquid()>0)return waterLogFugacity(model,state,state.waterVapor()>0)-crystal.logFugacityCoefficient(t,p);
        if(state.vaporProperties()!=null)return Double.NEGATIVE_INFINITY;
        return Math.min(0,DeterministicMath.log(model.saturationPressure(t)/p)-crystal.logFugacityCoefficient(t,p));
    }
    /** {@code ln (f_w / P)} of the free water in the state's gas ({@code vapour}: its steam's partial pressure) or in its
     * liquid water (its saturation pressure, the free-water rule's fugacity of the liquid); minus infinity without it. */
    private static double waterLogFugacity(FluidThermodynamics model,FluidThermodynamics.State state,boolean vapour) {
        if(vapour)return state.waterVapor()>0?DeterministicMath.log(state.waterPartialPressure()/state.pressure()):Double.NEGATIVE_INFINITY;
        return state.waterLiquid()>0?DeterministicMath.log(model.saturationPressure(state.temperature())/state.pressure()):Double.NEGATIVE_INFINITY;
    }`);
rep(`     * share of {@code fluid}'s liquids as the crystal ({@code n_i,liquid v_s / V_liquids}), capped at {@link #SLUSH_SHARE},
     * 0 where no liquid holds it (frost from a gas) or where there is no fluid (null). 0.25 mm up to phi = 0.09, 1.01 mm at
     * the slush. Rounded to {@link SolidInventory#SIZE_DIGITS} significant digits from the exact double, as a merged size is.
     */
    static ParticleSize precipitateDiameter(FluidThermodynamics.State fluid,NetworkCrystals.Crystal crystal) {
        double liquids=fluid==null?0:fluid.liquidVolume()+fluid.waterVolume(),phi=0;
        if(liquids>0)phi=Math.min(SLUSH_SHARE,fluid.liquidView()[crystal.component()]*crystal.molarMass()/crystal.material().density()/liquids);`, `     * share of {@code fluid}'s liquids as the crystal ({@code n_i,liquid v_s / V_liquids}; for ice, the free liquid water),
     * capped at {@link #SLUSH_SHARE}, 0 where no liquid holds it (frost from a gas) or where there is no fluid (null). 0.25 mm
     * up to phi = 0.09, 1.01 mm at the slush (a CO2-rich liquid, water freezing on its own). Rounded to
     * {@link SolidInventory#SIZE_DIGITS} significant digits from the exact double, as a merged size is.
     */
    static ParticleSize precipitateDiameter(FluidThermodynamics.State fluid,NetworkCrystals.Crystal crystal) {
        double liquids=fluid==null?0:fluid.liquidVolume()+fluid.waterVolume(),phi=0;
        double held=fluid==null?0:crystal.water()?fluid.waterLiquid():fluid.liquidView()[crystal.component()];
        if(liquids>0)phi=Math.min(SLUSH_SHARE,held*crystal.molarMass()/crystal.material().density()/liquids);`);
// The joint closure: the ice's row by the layout's water phases, its water balance over the vessel's water.
rep(`        private final double[] totals,crystalScale;
        private final boolean[] vapourRow;`, `        private final double[] totals,crystalScale;
        private final Row[] row;`);
rep(`            fluidSize=layout.size();crystalScale=new double[crystals.size()];vapourRow=new boolean[crystals.size()];
            for(int j=0;j<crystals.size();j++) {
                int c=crystals.get(j).component();var support=layout.support(c);
                if(support==PhaseSupport.ABSENT)throw new IllegalArgumentException("Crystal "+crystals.get(j).material().id()+" beside a fluid that holds none of its species");
                vapourRow[j]=support.vapor();crystalScale[j]=Math.max(1e-6,seeds.get(j)/layout.amountScale());
            }`, `            fluidSize=layout.size();crystalScale=new double[crystals.size()];row=new Row[crystals.size()];
            for(int j=0;j<crystals.size();j++) {
                var crystal=crystals.get(j);
                if(crystal.water()) {
                    // The ice against the free water (WP4): its steam where the layout carries steam, else its liquid (the
                    // melting line), else the vessel's water is all ice (no fluid water for it to exchange with).
                    row[j]=layout.carriesWaterVapor()?Row.VAPOUR:layout.carriesWaterLiquid()?Row.LIQUID:Row.ALL;
                } else {
                    var support=layout.support(crystal.component());
                    if(support==PhaseSupport.ABSENT)throw new IllegalArgumentException("Crystal "+crystal.material().id()+" beside a fluid that holds none of its species");
                    row[j]=support.vapor()?Row.VAPOUR:Row.LIQUID;
                }
                crystalScale[j]=Math.max(1e-6,seeds.get(j)/layout.amountScale());
            }`);
rep(`            layout.residual(state,targets,energy,capacity,workFrom,f,0,x,local,sweeping);
            double t=fluid.temperature(),p=fluid.pressure();
            for(int j=0;j<crystals.size();j++)f[fluidSize+j]=logFugacity(fluid,crystals.get(j).component(),vapourRow[j])-crystals.get(j).logFugacityCoefficient(t,p);
            return f;
        }`, `            layout.residual(state,targets,energy,capacity,workFrom,f,0,x,local,sweeping);
            double t=fluid.temperature(),p=fluid.pressure();
            for(int j=0;j<crystals.size();j++) {
                var crystal=crystals.get(j);int c=crystal.component();
                if(crystal.water()) {
                    // The water balance over the vessel's water, its ice included: the fluid's own share may be a trace of
                    // steam (5.6e-12 Pa over ice at 111.6 K), against which the ice's amount is not resolved.
                    int balance=layout.waterBalanceRow();
                    if(balance>=0)f[balance]*=layout.componentScale(c)/Math.max(layout.componentScale(c),totals[c]);
                    if(row[j]==Row.ALL){f[fluidSize+j]=(crystalMoles(x,j)-totals[c])/totals[c];continue;}
                    f[fluidSize+j]=waterLogFugacity(model,fluid,row[j]==Row.VAPOUR)-crystal.logFugacityCoefficient(t,p);
                    continue;
                }
                f[fluidSize+j]=logFugacity(fluid,c,row[j]==Row.VAPOUR)-crystal.logFugacityCoefficient(t,p);
            }
            return f;
        }`);
rep(`    private static final class Joint implements SparseNewton.Equations {`, `    /** The fluid phase a crystal's row reads its component in: the gas, the liquid, or none (the ice holds all the water). */
    private enum Row {VAPOUR,LIQUID,ALL}
    private static final class Joint implements SparseNewton.Equations {`);
fs.writeFileSync(f, s);
console.log("ok");
