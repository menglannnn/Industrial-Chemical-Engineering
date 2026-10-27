// WP4 offline edit helper: G3F9's water rows cross the melting line continuously.
const fs = require("fs");
const f = process.argv[2] + "/src/test/java/com/wormzjl/createcheme/science/thermo/qualification/G3F9DataRangeBoundaryTest.java";
let s = fs.readFileSync(f, "utf8");
const rep = (a, b) => { if (!s.includes(a)) throw new Error("missing: " + a.slice(0, 120)); s = s.replace(a, b); };
rep(` * envelope {@code EXTRAPOLATED}; only its competition rules (D8) refuse, and free liquid water below 273.16 K is the
 * interim ice hold (a capability limit until ice is modelled). Paths: the pilot network ({@code createcheme:pilot_cryogenic}),`,
` * envelope {@code EXTRAPOLATED}; only its competition rules (D8) refuse. Free water across its melting line freezes (WP4):
 * the network carries the engine's ice as its stand-in, continuously. Paths: the pilot network ({@code createcheme:pilot_cryogenic}),`);
rep(`        // Free liquid water below its triple point would freeze: the interim ice hold, a capability limit.
        double[] wet = {0.5, 0, 0, 0, 0.5};
        pilot.flashTP(273.2, 1e5, wet, () -> { });
        var ice = assertThrows(FluidThermodynamics.UnsupportedPhases.class, () -> pilot.flashTP(273.1, 1e5, wet, () -> { }));
        assertEquals(FluidThermodynamics.UnsupportedPhases.Kind.ICE, ice.kind());
        net.flashTP(273.2, 1e6, netWater, () -> { });
        var netIce = assertThrows(FluidThermodynamics.UnsupportedPhases.class, () -> net.flashTP(273.1, 1e6, netWater, () -> { }));
        assertEquals(FluidThermodynamics.UnsupportedPhases.Kind.ICE, netIce.kind());
        table.row("273.16 K, free water", "pilot and bundled networks", "273.2 K", "273.1 K", "interim ice hold (" + ice.kind() + ")");`,
`        // Free water across its melting line (273.1526 K at 0.1 MPa, 273.0859 K at 1 MPa; WP4): below it the engine states
        // ice, which the network carries as its stand-in (free liquid water held at its 273.16 K values) until a vessel's
        // refresh freezes it: no hold, and continuous (the interim ice hold before WP4).
        double[] wet = {0.5, 0, 0, 0, 0.5};
        continued("melting line, free water (WP4)", pilot, "pilot network", wet, 273.2, 1e5, 273.1, 1e5);
        continued("melting line, free water (bundled, WP4)", net, "bundled network", netWater, 273.1, 1e6, 273.05, 1e6);`);
fs.writeFileSync(f, s);
console.log("ok");
