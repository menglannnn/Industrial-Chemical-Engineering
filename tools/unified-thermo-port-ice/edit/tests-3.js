// WP4 offline edit helper: SpineNetworkPathTest's wet feed below 273.16 K is evaluated (its water is ice).
const fs = require("fs");
const f = process.argv[2] + "/src/test/java/com/wormzjl/createcheme/science/fluid/thermo/SpineNetworkPathTest.java";
let s = fs.readFileSync(f, "utf8");
const rep = (a, b) => { if (!s.includes(a)) throw new Error("missing: " + a.slice(0, 120)); s = s.replace(a, b); };
rep(`     * limit, the envelope's 900 K): each is carried on its continuation. The wet feed below 273.16 K meets a capability
     * hold: free liquid water is the interim ice hold, and its CO2-bearing hydrocarbons, fluid-only beside water (D8), may
     * first show a third fluid phase the network does not carry. A CO2 gas below its triple point that deposits is stated as its gas, and since P5 a CO2-bearing liquid that`,
`     * limit, the envelope's 900 K): each is carried on its continuation. Since WP4 the wet feed below 273.16 K is evaluated
     * too: its water is ice, carried as its stand-in, beside hydrocarbons under the crystal competition (no hydrates,
     * decision 6 A). A CO2 gas below its triple point that deposits is stated as its gas, and since P5 a CO2-bearing liquid that`);
rep(`                    } catch (FluidThermodynamics.UnsupportedPhases hold) {
                        if (feed[4] > 0 && t < 273.16) {
                            // The wet feed below 273.16 K: a capability hold (ice not modelled, or a third fluid phase first).
                            wetCold++;
                            continue;
                        }
                        assertTrue(hold.kind() != FluidThermodynamics.UnsupportedPhases.Kind.ICE, "the ice hold only on water below 273.16 K");
                        // P4 stage 2b held`, `                    } catch (FluidThermodynamics.UnsupportedPhases hold) {
                        // P4 stage 2b held`);
rep(`                    evaluated++;
                    double[] liquid = state.liquidView(), vapor = state.vaporView();`, `                    evaluated++;
                    if (feed[4] > 0 && t < 273.16) {
                        // WP4: below the melting line the condensed water is ice, which a TP answer carries as its stand-in.
                        wetCold++;
                        assertTrue(state.waterLiquid() > 0, "the wet feed's ice at " + t + " K, " + p + " Pa");
                    }
                    double[] liquid = state.liquidView(), vapor = state.vaporView();`);
rep(`        System.out.printf("WP5 pilot flash grid: %d states evaluated (%d two-phase), %d wet holds below 273.16 K, typed holds %s"`,
    `        System.out.printf("WP5 pilot flash grid: %d states evaluated (%d two-phase), %d wet states below 273.16 K, typed holds %s"`);
fs.writeFileSync(f, s);
console.log("ok");
