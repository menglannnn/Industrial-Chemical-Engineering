// WP4 offline edit helper: DataRangeContinuationTest's interim ice hold becomes the ice's stand-in.
const fs = require("fs");
const f = process.argv[2] + "/src/test/java/com/wormzjl/createcheme/science/fluid/thermo/DataRangeContinuationTest.java";
let s = fs.readFileSync(f, "utf8");
const rep = (a, b) => { if (!s.includes(a)) throw new Error("missing: " + a.slice(0, 120)); s = s.replace(a, b); };
rep(` * box refuses a state, as a rejected trial (a plain {@link IllegalArgumentException}, never a {@link ThermoHold}), and free
 * liquid water below 273.16 K is the interim ice hold until ice is modelled.
 */`, ` * box refuses a state, as a rejected trial (a plain {@link IllegalArgumentException}, never a {@link ThermoHold}). Free
 * water below its melting line is ice (WP4), carried by a step as its stand-in, the supercooled liquid held at its
 * 273.16 K values.
 */`);
rep(`    /** Liquid water below 273.16 K would freeze; until ice is modelled (WP4) it is a typed capability hold. Its vapour is
     * carried: humid nitrogen at 263 K holds 0.1 % steam, below the supercooled liquid's 287 Pa, as a gas. */
    @Test void freeLiquidWaterBelowItsTriplePointIsTheInterimIceHoldAndItsVapourIsCarried() {
        double[] none=new double[model.hydrocarbon.componentCount()];double[] nitrogen=none.clone();nitrogen[index("Nitrogen")]=1;
        var ice=assertThrows(FluidThermodynamics.UnsupportedPhases.class,()->model.state(270,101325,none,nitrogen,1,0,101325));
        assertEquals(FluidThermodynamics.UnsupportedPhases.Kind.ICE,ice.kind());
        assertTrue(ice.status("").startsWith("free water below its melting point: ice not modelled yet"),ice.status(""));
        var pure=assertThrows(FluidThermodynamics.UnsupportedPhases.class,()->model.flashTP(270,101325,overall("Water",1.0),()->{}));
        assertEquals(FluidThermodynamics.UnsupportedPhases.Kind.ICE,pure.kind());`, `    /** Liquid water below its melting line freezes (WP4): a TP answer states its ice, and the network carries it as its
     * stand-in, free liquid water, until a vessel's refresh freezes it; no hold. Its vapour keeps the ice's sublimation
     * pressure (260 Pa at 263.15 K, against the supercooled liquid's 287 Pa), and a humid gas below it is a gas. */
    @Test void freeLiquidWaterBelowItsMeltingLineIsTheIcesStandInAndItsVapourKeepsTheIcesPressure() {
        double[] none=new double[model.hydrocarbon.componentCount()];double[] nitrogen=none.clone();nitrogen[index("Nitrogen")]=1;
        var standIn=assertDoesNotThrow(()->model.state(270,101325,none,nitrogen,1,0,101325));
        assertEquals(1,standIn.waterLiquid());
        var pure=assertDoesNotThrow(()->model.flashTP(270,101325,overall("Water",1.0),()->{}));
        assertEquals(1,pure.waterLiquid(),"pure water at 270 K and 1 atm is ice, carried as its stand-in");
        var frost=assertDoesNotThrow(()->model.flashTP(263.15,101325,overall("Nitrogen",.995,"Water",.005),()->{}));
        assertTrue(frost.waterLiquid()>0,"the water above the ice's pressure is ice: "+frost.waterLiquid());
        assertEquals(model.freeWaterIce().fugacity(263.15,101325),frost.waterPartialPressure(),1e-6*frost.waterPartialPressure(),"the steam over ice");
        assertEquals(259.89,frost.waterPartialPressure(),.02,"R14-08 at 263.15 K");`);
fs.writeFileSync(f, s);
console.log("ok");
