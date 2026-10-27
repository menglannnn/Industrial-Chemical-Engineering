# Moved references of the junction seeding batch (t44b, 2026-10-19)

Every printed line that moves on `claude/junction-seeding` at `bbbd8626` (the follow-ups `373c8fee` and `bbbd8626` on `c51df66a`; the same 54 tests and lines as at `c51df66a`) against `32bf3fa4`, from the javac-lane suites (`science-fluid-thermo`, `science-fluid-network`, `runtime-fluid`) and the two classes outside them that t46 found moving, `FluidPumpedFillLineTest` and `FluidDataRangeIslandTest` (logs `tools/junction-seeding/logs/t44b-logs.tar.gz`, `c3/suites/cmp-c3-*.txt` against the baseline logs `suites/t44b-*.txt` of `t44-logs.tar.gz`). Every assertion holds: 66/66, 184/184, 304/304. Timings (`ms=`, `cpuMs=`, `allocatedMB=`) are not references and are left out of the magnitudes below.

**Totals.** 54 tests print moved lines: 0 of 66 (thermo), 10 of 184 (network), 44 of 298 (runtime). 27 move at roundoff only (every number within 1e-6 relative: the balanced start lands the same root a few ulps away), 3 within 1e-3, 24 beyond. The moves sit where the plan said they would: a placed fixture's first interval (the balance's start, the rate seed's converged point) and a junction's minted mixture (D55's supplier composition in place of the placeholder).

**Named references.**
- `MIXED_GAS_COST interval=5 cases=12 newtonSolves=299`: unchanged (the plan's prototype read 283 on the balance alone; the mint form is back at the baseline's count); `ms` and `allocatedMB` move.
- `STATIC_COLUMN_WORLD` (`HoldClassWorldsTest.aLiquidTraceAtATanksBottomPortLeavesItsDropLineAGasColumn`): gas column 45.061 to 44.999 Pa (the upper tank's column, as at the baseline; the balance alone had rested on the lower tank's, 37.06 Pa), the lower tank's condensate 4.96e-6 to 4.25e-7 m3, the drop line 8.9e-9 to 6.6e-6 kg/s. The assertion is the band of owner decision 2 (between the two tanks' columns, the flow within the gas the line holds over the run); it passes on both trees.
- `EXTREME_TOPOLOGY` (`ExtremeTopologyIslandTest` [1] [2] [3]): the same Newton solve counts (39, 37, 49), accepted and rejected steps; the component and energy ledgers move at 1e-16; no assertion.
- `PUMP_CAVITATION_696` (`PumpCavitationTest.aHeadLimitTrialBelowTheVapourPressureIsSolvedOnTheSuctionLimit`): a closed line's carried flow 2.8e-38 to 0.0 kg/s; `PUMP_CAVITATION_STEADY`: the suction and vapour pressures move in the 13th digit.
- chain-100 (`FluidSolverRegressionTest`, replayed on the lane against the tracked fixture): max deviation 0 in state, temperature, phase fraction and flow. Not recaptured.
- `FluidPumpedFillLineTest` (4 of its 6 tests, the `runtime` package): the three-tank fill's first 0.6 s takes 6 accepted and 2 rejected steps against 4 and 0 (a line-search stall and an iteration limit at pass 1, the balanced start's first interval), the fill ends equal (601 325 Pa, resting flow 3.245e-10 to 3.248e-10 kg/s, the certificate drift 1.43e-11 either way); the gas transfer's compressor rests at 3.8e-35 against 2.9e-19 kg/s. `FluidDataRangeIslandTest`: the resting flow 1.2e-18 to 4.2e-23 kg/s, the hottest temperature at 1e-14. No assertion.

**Beyond 1e-3 relative (24; the largest relative change of a printed number, the line it sits in).**
| Test | Largest move | Line |
|---|---|---|
| `VirtualJunctionHoldupTest` sum of m_J | digest | `VIRTUAL_HOLDUP_DIGEST` sha256 of the trajectory (a different first interval) |
| `SealedDischargeTest` dead-heading pump | flows | `OWN_END_DISCHARGE` island 14: a closed line's carried flow at 1e-38 reads 0 |
| `PumpCavitationTest` 696 | flows | `PUMP_CAVITATION_696`: as above |
| `LiquidJunctionTransientTest` 5 s and 0.1 s | m_J, m_J/Q | `LIQUID_JUNCTION ... m_J=1.565 kg Q(1s)=0.006672 kg/s m_J/Q=234.6 s`: the junction holdup is sized on the supplier composition, not on the placeholder (D46's unbooked mass, see the plan) |
| `HoldClassWorldsTest` gas column | 1.0 (the flow) | `STATIC_COLUMN_WORLD`, above |
| `MixedGasJunctionTransientTest` 5 s and 0.1 s | ledgers 1e-16 | `MIXED_GAS_TRANSIENT Outcome[... worstMoles=4.8e-16 ...]`; the Newton solve count 299 is unchanged |
| `ExtremeTopologyIslandTest` [1] [2] [3] | ledgers 1e-16 | `EXTREME_TOPOLOGY`, above |
| `JunctionWaterTraceTest` (3 cases) | ledgers 1e-16; `waterToVoidKg` 1 % | `JUNCTION_WATER_TRACE`, `JUNCTION_ARRIVING_PHASE waterToVoidKg=18.32`: the minted junction's water share moves with its mixture |
| `PhasePortTest` ledger | 1e-16 | `PHASE_PORT_JUNCTIONS worstComponentLedger=2.4e-16` |
| `HoldClassWorldsTest` vanishing rule | 13 % | `VANISHING_RULE island 18 node 17`: the junction's P and T at the failed iterate the rule reads |
| `VesselStateFollowsContentsTest` | lag 1e-14 | `VESSEL_CLOSURE grid-10 ... worst lag 1.9e-14` |
| `LevelHeadTest` pump into a bottom port | 0.9 % | `LEVEL_HEAD_RUN` headspace pressures of the first intervals |
| `PumpBottomPortDrainTest` gas trace | 0.1 % | `FED_PUMP slice=0 ... junction 4` the first slice's junction state |
| `FluidCheckpointStoreTest` (3), `FluidCheckpointFormatTest`, `PhysicalRegistryTest` | timings only | encode, decode, save and placement milliseconds |

**Roundoff only (27).** `CarriedGasTest`, `ElevatedBlockLineIslandTest`, `EmptyRiserTest`, `EmptyTankFillsTest`, `FilterBlockLineIslandTest` (3), `FullTankSolidsEventTest` (3), `IslandCertificateTest` (3), `InlineFilterLineHoldTest` (2), `LiquidFullTankOutletsTest`, `McpGameplayRegressionTest`, `PumpBottomPortDrainTest` (2), `SlurryTankPumpTest` (2), `CompressorTest`, `PhasePortPriorityTest` (4), `LevelHeadTest` cushions, `SolidChainTransportTest` (2): every printed number within 1e-6 of the baseline's.

**Not moved.** The whole `science-fluid-thermo` suite; 174 of 184 network tests; 254 of 298 runtime tests; the checkpoint and topology formats; chain-100.
