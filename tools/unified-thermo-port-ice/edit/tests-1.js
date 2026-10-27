// WP4 offline edit helper: moves the tests that pinned the interim ice hold to the ice model.
const fs = require("fs");
const root = process.argv[2];
function edit(path, pairs) {
  const f = root + "/" + path;
  let s = fs.readFileSync(f, "utf8");
  for (const [a, b] of pairs) { if (!s.includes(a)) throw new Error(path + " missing: " + a.slice(0, 120)); s = s.replace(a, b); }
  fs.writeFileSync(f, s);
  console.log("edited " + path);
}
edit("src/test/java/com/wormzjl/createcheme/runtime/fluid/FluidDeviceSpecDomainTest.java", [
  [`     * accepted and continued. Water at 260 K passes the input box and meets the interim ice hold (free liquid water below
     * its melting point) when the generator is first activated, until ice is modelled. */`,
   `     * accepted and continued. Water at 260 K passes the input box and the generator supplies it, the stand-in of its ice
     * (WP4: free liquid water, which a vessel's refresh freezes), with no hold. */`],
  [`        var frozen=assertThrows(FluidThermodynamics.UnsupportedPhases.class,()->ice.initialize(device(TopologyCompiler.Kind.GENERATOR),model,()->{}));
        assertEquals(FluidThermodynamics.UnsupportedPhases.Kind.ICE,frozen.kind());`,
   `        var source=assertDoesNotThrow(()->ice.initialize(device(TopologyCompiler.Kind.GENERATOR),model,()->{}));
        assertTrue(source.state().waterLiquid()>0&&source.state().waterVapor()==0,"condensed below the melting line, carried as its stand-in");`],
]);
edit("src/test/java/com/wormzjl/createcheme/runtime/fluid/IslandCoordinatorTest.java", [
  [`    /** A phase hold (D21 option C): free liquid water below its triple point, the interim ice hold of the range batch. */
    private static com.wormzjl.createcheme.science.fluid.thermo.ThermoHold violation(double value,long node) {
        return new com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics.UnsupportedPhases("createcheme:tjl20_methane_nitrogen",value,101325,
                com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics.UnsupportedPhases.Kind.ICE,"1.0 mol of free liquid water below the water model's triple point 273.16 K").at(node);
    }`,
   `    /** A phase hold (D21 option C): a third fluid phase, a capability limit (the interim ice hold it replaced is gone, WP4). */
    private static com.wormzjl.createcheme.science.fluid.thermo.ThermoHold violation(double value,long node) {
        return new com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics.UnsupportedPhases("createcheme:tjl20_methane_nitrogen",value,101325,
                com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics.UnsupportedPhases.Kind.THREE_PHASES,"a third fluid phase is indicated").at(node);
    }`],
  [`            assertEquals("HELD (thermo domain): free water below its melting point: ice not modelled yet at 260.12 K, 101325 Pa in tank at 1, 64, 0 (package createcheme:tjl20_methane_nitrogen)",status);`,
   `            assertEquals("HELD (thermo domain): a third fluid phase, which the network does not carry at 260.12 K, 101325 Pa in tank at 1, 64, 0 (package createcheme:tjl20_methane_nitrogen)",status);`],
  [`            assertTrue(coordinator.snapshot(1).status().startsWith("HELD (thermo domain): free water below its melting point: ice not modelled yet at 258.40 K, 101325 Pa in tank at 1, 64, 0"),coordinator.snapshot(1).status());`,
   `            assertTrue(coordinator.snapshot(1).status().startsWith("HELD (thermo domain): a third fluid phase, which the network does not carry at 258.40 K, 101325 Pa in tank at 1, 64, 0"),coordinator.snapshot(1).status());`],
  [`            assertEquals("1 thermo-domain: phases ICE tank at 1, 64, 0 false",warned.getFirst());`,
   `            assertEquals("1 thermo-domain: phases THREE_PHASES tank at 1, 64, 0 false",warned.getFirst());`],
]);
edit("src/test/java/com/wormzjl/createcheme/science/fluid/network/PumpRiseScalingTest.java", [
  [`        var v=new FluidThermodynamics.UnsupportedPhases(NETWORK,260,101325,FluidThermodynamics.UnsupportedPhases.Kind.ICE,"free liquid water at 260 K");`,
   `        var v=new FluidThermodynamics.UnsupportedPhases(NETWORK,260,101325,FluidThermodynamics.UnsupportedPhases.Kind.THREE_PHASES,"a third fluid phase at 260 K");`],
]);
edit("src/test/java/com/wormzjl/createcheme/science/fluid/network/CrystalDepositionIslandTest.java", [
  [`    @Test void theCrystalIsThePilotsSolidAndTheBundledNetworkPackageHasNone() {
        var crystals=model.crystalSolids().all();assertEquals(1,crystals.size());`,
   `    @Test void theCrystalsAreThePilotsSolidAndTheIceAndTheBundledNetworkPackageHasTheIceOnly() {
        var crystals=model.crystalSolids().all();assertEquals(2,crystals.size(),"CO2-I and, last, the free water's ice (WP4)");
        assertTrue(crystals.getLast().water());assertEquals("createcheme:crystal_ice_ih",crystals.getLast().material().id());`],
  [`        assertTrue(FluidTestSupport.networkModel().crystalSolids().isEmpty(),"the bundled network package keeps the fluid-only path");`,
   `        var bundled=FluidTestSupport.networkModel().crystalSolids().all();
        assertEquals(1,bundled.size(),"the bundled network package has no crystal competition: its one crystal is the free water's ice");
        assertTrue(bundled.getFirst().water());`],
]);
