# M4c: the owner's freezing-point rule, E8 and D33's gap (2026-10-16)

Batch `2026-09-24-coolprop-low-temperature`, branch `claude/unified-thermo-port` from `1b15fd43`.

**Status (2026-10-16).** Item 1, the freezing-point rule, is superseded by the owner's direction "no hold for leaving a package's range": the range-extrapolation plan (`RANGE_EXTRAPOLATION_PLAN.md`) takes it over, and the survey below is one of its inputs. Nothing of item 1 was built. Items 2 and 3 are committed: E8 `b46b4a81`, D33's gap `1b75ce08`. No quick check ran; they run once, after the extrapolation change lands.

## E8: a vessel whose closure folds its last bubble is closed again without it (`b46b4a81`)
- **Physics.** Seed 48's tank: LNG full beside dry ice, with a bubble of 1.0e-9 of the moles (2.0e-8 of the volume). Fed 100 Pa above it, a 1e-6 s step brings in 2.3e-8 kg. The bubble dissolves and the pressure falls about 5 Pa.
- **What the code did.** The reconstruction's closure ended with the bubble just under the trace share. The fold (D34) removed it, and the folded state was only checked: its volume missed by 1.9e-8 against the 1e-8 check, so the step was refused.
- **Fix.** `InventoryEquilibrium.close` closes the folded vessel again in the folded layout (D30's form). A fold only removes a phase, so the recursion ends. This covers the reconstruction, the own split and the refresh.
- **Gate.** `PrecipitatedSolidsTest#aLiquidFullTankWhoseClosureFoldsItsLastBubbleIsClosedAgainWithoutIt`: one fold pass, no vapour, 1141308.4 Pa. With rule e8 off: "does not meet full equation closure" (logs `m4c/01`, `02`).

## D33's gap: every transport closure reads open at an interval start, on every path (`1b75ce08`)
- **Physics.** A closure is the solid events' finding for one interval, and a gas carries no solid. Two 0.1 m3 gas tanks at 300 and 101 kPa, joined by 10 m of 4 mm line, pass about 1.5e-3 kg/s. That moves each tank about 7.7 kPa in 5 s.
- **What the code did.** The plain integrate (no filter, solids or liquid) kept the committed masks. The island was not at rest and was solved, but moved 0 kg/s with the closure kept.
- **Fix.** `PassiveIntervalSolver.solve` and `solveApproximate` read the interval start once, as `NetworkReduction.reopened`, and every path reads that start. Absorbed: `atRest`'s `flows==null` switch and `decideInlets`' own reopening. A rest interval still returns the committed graph.
- **Gate (c), extended.** 1.56e-3 kg/s, 292.4 and 108.5 kPa, no closure left. With rule d33i off: 0 kg/s and closure 3 (logs `m4c/03`, `04`).
- **Two tests relied on the gap,** holding a caller's both-way block across slices:
  - `InletPhaseTest` fixture 4: the control is now a compressor set to no flow. It is still bitwise, with unchanged lines.
  - `PhasePortTest`'s closed ports: the slices run through the interval integrator below the interval start, the solid events' segment form. Still bitwise.
  - Tried and dropped: closed valves (up to 51 ulp from the bulk ports) and refused movers (2 to 6 ulp) (logs `m4c/08`, `09`).

## Affected classes (index lane, 69 classes: F2's 51 plus 18 closure, fold, rest and gate classes)
- **Pass counts.** Head `1b15fd43`: 386 of 387 passed. Final: 387 of 388. The one failure in both is `FluidCheckpointFormatTest`'s property check. The lane lacks Gradle's `createcheme.fluid.scheduler.verify`; with it, 8 of 8 pass (`m4c/11`).
- **Printed lines.** Only gate (c)'s new line moved; the E8 gate is new. E8 moved no line.
- **Seen, not in scope.** The E8 probe's 10 Pa rise at a 0.05 s step stalls its line search on head and fix (`m4c/00`).

## Item 1 survey: places where a freezing or triple point bounds a component (input to the extrapolation plan)
| # | Place | Bound today |
|---|---|---|
| 1 | `PassiveStepSolver.seedAt` window (D55 round 0, F2's root) | `FluidDomain.range`: CO2 216.592 K |
| 2 | `fixedPointStarts` round clip (one empty window stops the round for the whole island) | as 1 |
| 3 | `balancedBoundarySeed` (a cold star junction between held nodes) | as 1 |
| 4 | `FluidDomain`: `range` reads the record's floor; `check`, `checkPhaseTemperature` the crystal floor (`vaporMinimumTemperature`, `requireSplit`); `checkTotals` the crystal floor dry, the record's wet | CO2: 216.592, 90 and 90 K |
| 5 | Engine `PhaseDomain.of(FluidDomain)`: FLUID_ONLY keeps the contract domain (`contractChecks`); `crystalRange` lowers it only under the competition | CO2 216.592 K |
| 6 | Pilot records' `fluid_domain` floors, each a triple point | N2 63.151, CH4 90.6941, C2H6 90.368, CO2 216.592 K |
| 7 | Water: `FluidDomain` takes its `triple_point`; also `WaterRegion1`, `saturationPressure`, `vaporWaterEnthalpy`, `NetworkFreeWater.steam` | 273.16 K |
| 8 | Envelopes: pilot and bundled `tjl20_methane_nitrogen` at 63.151 K (N2's triple point); the other bundled packages 273.16 K | package floor |
| 9 | Data limits, not freezing points: CO2 spine r2 and both viscosity tables from 90 K, CO2-I from 80 K, the engine's search windows (`searchWindow`, `crystalStepTemperature`, the onset window), the catalog's viscosity-table check | data |
| 10 | Records without a spine: `CoefficientIdealGas` takes the record's range (crude, light ends, methane 293.15 K; nitrogen 63.151 K); the pilot's column-only `temperature_min_kelvin` 216.592 K | data |

- **Data.** CO2's reach 90 K, so LNG at 111.6 K saturated with CO2 (0.05 %) is a valid solution; methane's and ethane's end at their triple points. Water freezes at 273.16 K, and no package models ice yet; the owner has since chosen ice Ih as a crystal.
- **Bundled worlds.** The bundled network envelope (63.151 K) lies below water's 273.16 K, so a rule on floors reaches bundled worlds through water.
- **Tests that pin 216.592 K as the fluid-only floor.** G3F7's contract assertion, plus comments in G3F9 and `SpineNetworkPathTest`. The owner questions Q1 to Q3 are withdrawn.

Tools and logs: `tools/unified-thermo-port-forms/`. That holds rule-off rules e8 and d33i, `scripts/m4c-affected.sh`, `probe/E8ProbeTest.java`, and the logs in `m4c/` of `logs/forms-logs.tar.gz`. Code guide: updated at the merge.
