# unified-thermo-port-ice

Purpose: the ice Ih record's fit, the probes and the rule-off proofs of WP4 of the range-extrapolation plan (ice Ih as a
crystal, `documentation/2026-09-24-coolprop-low-temperature/RANGE_EXTRAPOLATION_PLAN.md` section 6, decisions 3 to 6 A),
on branch `claude/unified-thermo-port`: commits `13bcfe31` (the ice record, the water record's ice block), `1674e3db`
(engine: free water below its melting line is ice) and `ead6d6f2` (network: stand-in in the steps, ice at a vessel's
refresh), on top of `90d68ca1`. Batch `2026-09-24-coolprop-low-temperature`; report `ICE_IH_WP4.md` in that batch folder.

Nothing here is a gate and nothing here was ever tracked, so there is nothing to re-attach. The Java probes and the
rule-off proofs run on the Gradle-free index lane of `tools/unified-thermo-port-m3/scripts` (`index-compile.sh [tree]`,
`index-run.sh <selectors>`), with `JAVA_TOOL_OPTIONS="-Duser.language=en -Duser.country=US"`. Paths name the worktree
`.claude/worktrees/coolprop-multiphase-thermo-37f6b0`; set `WT` to replay in another worktree.

| Path | What it is |
|---|---|
| `fit/R1006.java` | IAPWS R10-06(2009), the Gibbs function of ice Ih, and R14-08(2011) equation 6, the sublimation pressure; `R1006.verify()` checks R10-06's seven check values (reproduced to 2.2e-12) and prints R14-08 at 263.15, 230 (8.94735 Pa), 190, 150 and 111.6 K |
| `fit/Fit.java` | Fits the record's two c_p segments (10..60 K, 60..273.16 K) to R10-06 at 611.657 Pa and prints them as `crystals/ice_ih.json` carries them, then the record against R10-06 (c_p, h - h(Tt), s - s(Tt), the linear molar volume) and the worst deviations. Run in `fit/`: `javac -d out R1006.java Fit.java && java -cp out Fit` with JDK 21 (`C:/Program Files/Java/jdk-21.0.11/bin`; classes from a newer javac do not load on 21) |
| `fit/Survey.java` | R10-06's molar c_p, density, expansion and compressibility at the triple-point pressure and 1 atm, 10 to 273.16 K (the numbers behind the plan's decision 4) |
| `rule-off/run.sh` | The rule-off proofs, each in a temporary index (`/tmp/m3/ruleoff-ice`; the worktree is never touched): `a` free water below its melting line is not ice (FreeWaterIceTest, 4 methods), `b` no freeze step from the balance (FreeWaterIceTest#phAndUv...), `c` a vessel's refresh does not test the ice, `span` the steam's difference step uncapped (both WaterIntoCryogenicMethaneTest#waterDrivenIntoTheCooledVesselFreezesAtTheRefresh). `bash run.sh a|b|c|span [base]`; every one must fail. Recompile the head afterwards |
| `probes/IceGateProbe.java` | Gate (c)'s island stepped in short intervals, the vessel printed after each (`-Dprobe.interval=<s> -Dprobe.count=<n> -Dprobe.joint=true`): found the trace steam's difference step behind the step and joint stalls (`PhaseLayout.LOG_READ_SPAN`) |
| `probes/WetCrystalGridProbe.java`, `EngineWetProbe.java` | SpineNetworkPathTest's wet CO2 feed below 273.16 K against its dry part, through `flashTP` and the engine's TP: the wet states that held where the dry part answers (the dry-at-pc fallback of `FluidTpEquilibrium.solveWetWithCrystals`) |
| `probes/LegacyMixtureProbe.java` | LegacyNetworkPathPinTest's one point that turned from the ice hold into THREE_PHASES (110 K, 1 bar): the dry mixture holds the same |
| `probes/PackagePinProbe.java` | Every bundled package's scientific revision, physics fingerprint and fluid-thermo fingerprint: the water record's revision moves the first, never the second |
| `edit/` | Node helpers that applied the multi-line edits to the CRLF sources (`splice.js` with its `*-block.txt`, `tests-1..5.js`, `evidence.js`, `pins.js`, ...); a record of the edits, not needed to rebuild anything |

Probes compile with the tree's tests through `EXTRA_TESTS="<absolute probe path>" index-compile.sh` and run with
`index-run.sh <package.Class>`.

## Index record (moved from tools/INDEX.md on 2026-10-26)

Purpose: WP4, ice Ih (`13bcfe31`, `1674e3db`, `ead6d6f2`): the IAPWS R10-06 Gibbs function and R14-08 checked against their published check values, the two c_p segments of `crystals/ice_ih.json` fitted to it, the engine and network probes, `rule-off/run.sh` for gates a, b, c and the log-read span (index lane, nothing tracked)

Batch: 2026-09-24-coolprop-low-temperature

Status: Done 2026-10-16 (report `ICE_IH_WP4.md`). Index: [../INDEX.md](../INDEX.md).

### History

- Done 2026-10-16 (report `ICE_IH_WP4.md`)
