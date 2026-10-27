# D36: a gas column does not exist (2026-10-17)

Batch `2026-09-24-coolprop-low-temperature`, `claude/unified-thermo-port`, commit `03bc70f6` on D34's `4199dbe6`. Owner decision D36 (`DECISION_LOG.md`). Tools: `tools/unified-thermo-port-m5/d36/`.

**Physics in numbers.** A gas column weighs 11 Pa/m (air at 1 atm) to 58 Pa/m (nitrogen at 480 kPa, 5.95 kg/m3); liquids weigh 4 to 10 kPa/m. At game level the gas weighs nothing. Two nitrogen tanks at 480 kPa, 2 m apart on a riser, then rest at one pressure and nothing flows; weighed, the riser drives 116.7 Pa and the lower tank ends that much higher. A loop of a 272.7 K and a 372.7 K riser then carries nothing; weighed, the cold riser outweighs the hot one by 31.6 Pa round the loop, a 0.016 to 0.023 kg/s thermosiphon. Lines holding only gas are flat at rest; condensed phases keep their weight.

**What the code did.** Every static column weighed the vapour: D34's fill counted the line's gas share, a BULK end used its bulk density with the gas, a D33 gas-drawing bulk end the vapour density, and a mover's column (D6) its whole suction stream.

**The change** (`PassiveStepSolver`):
- `columnDensity` keeps D34's fill and skips the GAS share: condensed mass over the line's volume. `endColumn` (a vanishing flow, and the fill's fast path) gives zero for gas, a liquid's density to the bit, zero at a D33 bulk end. `condensedOverBulk` gives `liquidMass/volume` (alpha_L rho_L) for a BULK end or a state holding none of the port's phases (`mass/volume` to the bit with no vapour).
- Every D34 site follows: the row and `columnEnds`, `staticDrive`, `initialMassFlows`, `supplyFlow`, the balanced junction seed, the valve start head, the one-way estimate and its hull margin, `closeDeadHeads`, `reopenable`, the cold generator closure, `illegalWithEitherDensity`, `NetworkReduction.drives`.
- Mover (D6): `Equations.Transport` gains `column` (the bulk's alpha_L rho_L, zero for the gas phase stream, a liquid's density, a priority mixture's condensed mass flow over its volume flow). The hydraulic row and `demand` read it (the shutoff margin in `nextMode` and `polishRefusedPoint`). `endColumn` gives it at a vanishing flow to the carry's shutoff offer and to `headLimitMassFlow`'s drive.
- Unchanged (the draw): `massFlowLimit` and caps, losses, viscosities, the rise limit's density everywhere (the `riseLimit` sites, the PUMP_TARGET and PUMP_HEAD_LIMIT rows), mover power, `resolutionFlow`. The D9 level head (`portHead`, `TankOutlets`) already read condensed mass only; there was no gas-space term to zero.

**Vapour slot: verified, no freeze.** "Vapour" is the GAS slot of the pass's layout. `PhaseLayout` is built per pass from the pass's seeds (the `Equations` constructor) and never rebuilt inside the Newton. `decode` writes vapour amounts only into the vapour slots the layout carries, and an omitted phase is exactly zero. So `holdsVapor` and which amounts are vapour are the pass's; the drawn segment is frozen per pass too. The row stays smooth: the fill is continuous, and the mixture's `gas>0` branch meets `gas=0` continuously.

**Deleted, as gas weighs nothing.**
- (1) The one-way estimate's pressure scaling of a gas-only junction's column (`column*scale` in `OneWay.flow`; the column is zero, and its density and cap keep the scaling).
- (2) The `columnEnds` comment on one gas density per riser (1e-4 per metre at 480 kPa; random 498's 1.8e-5 and 2.4e-7 kg/s round its loops).
- (3) In the tests, the D34 gate's computed gas column and its import. No gas-only branch or guard remained: the column-end rule and `donates` exist for liquid against gas ends.

**Gates** (rule off = `rule-off-reverse.patch` on `03bc70f6`, each test alone):
- (a) `RestResolutionTest#twoGasTanksJoinedByARiserRestAtOnePressure`, the two tanks above. Rule on: drives 0, gauge at rest, a 5 s step 0.0 kg/s, pressures within 1e-10 Pa. Rule off: fails, drive -116.717 Pa.
- (b) `RestResolutionTest#aLoopOfGasRisersCarriesNoCirculation`, a unit loop (the loop above). Rule on: net 0, `loopDriven` false, 0.0 kg/s in all four connections. Rule off: fails, net 31.623 Pa.
- Random 498's pocket was rebuilt (TurbulentWalk's minimized ladder). At rest it carries no circulation on either rule (the gauge reads its 7.5e-4 Pa loop drive as rest), so it cannot decide (b). It now asserts every junction at the generator's pressure: spread 1.4e-6 Pa; rule off fails at 116.7 Pa above.

**Restated tests (old -> new; the gas column is gone).**
- The D34 gate `...LeavesItsDropLineAGasColumn` (name kept). The lower tank above the upper: 44.895 Pa (asserted within 0.1) -> 0 within 0.1 (3.5e-10). Drop line: 6.7e-9 -> -5.7e-12 kg/s, as the lower tank no longer follows a cooling gas column.
- `PhysicalFluidTopologyTest` stack: `0 < dP < 100` (22.75 Pa) -> `|dP| <= 0.1 Pa` (0.088 Pa, the column of the 8.8 mg water trace upstairs).
- `FedTankOpenOutletsTest` x2: "runs dry and rests empty" -> "keeps its 1 atm charge" (1.145 kg within 1e-9). Weighed, the charge drained down the bottom outlet on a 22.5 Pa column at 0.019 kg/s. maxAttempts 3 -> 1, steady 102 -> 119.
- Javadoc only: TurbulentWalk ("plus its gas column"), and the drain-side world (seed 992, end state identical on both rules).

**Moved numbers** (`run-touching.sh` against `4199dbe6`). Science: 37 classes, 185 tests (2 gates new), no number moves but timings. Runtime: 61 classes, 296 tests pass; physics moves in 15 tests, the 4 restated plus:
- `MixedGasJunctionTransientTest`, the 5- and 6-port cases (voids 1 m above and below the junction). MIXED_GAS_COST newtonSolves 339 -> 312 and allocatedMB 170.3 -> 159.7. Cases 0.05:6:false 52 -> 40, 0.05:6:true 33 -> 25. Unequal 6-port junctionShare 0.496 -> 3e-15 (0.05 m) and 0.233 -> 4e-15 (0.02 m). The 4-port cases and the ledgers are unchanged.
- `LiquidFullTankOutletsTest`: F2_END goneAt 41 -> 42, traceSteady 16 -> 17. Slice 0 has one more refused substep (Newton iteration limit; 263/2 against 262/1 over 86 slices), with the same steady 129932.897 Pa.
- `TankOutletsTest`: side outlet 0.3669 -> 0.3486 kg/s, one 5 s interval over a transient (at 0.25 s intervals 0.5866 -> 0.5847). Top 0.017090 -> 0.017162 kg/s; head 3750.3 -> 3751.3 Pa.
- Smaller moves: `LiquidCo2LetDownTest` slice 2 substeps 75/12 -> 75/10, the tank -7.6 Pa at slice 4; `SlurryTankPumpTest` the vented tank 101360.6 -> 101325.0 Pa, the pump 2.17435 -> 2.17436 kg/s; `SealedDischargeTest` one connection PASSIVE at 3.4e-16 kg/s -> CLOSED; `PumpBottomPortDrainTest` x3 at most 8.2e-11 relative.
- Wall-clock only: 12 tests (`logs/moved-classes.txt`). The owner's job held the CPU at 56 to 96 %. No soft-budget or wall-clock failure occurred, so nothing was replayed alone.

**Code guide (`fluid-science.md`, at merge).**
- Physics paragraph: static columns are condensed only; gas weighs nothing (D36).
- Connection row: a passive run's column is `columnDensity` at the pass's column end; a mover's is its drawn suction stream's `Transport.column`.
- Class map: `endColumn`, `condensedOverBulk`. Tests: `RestResolutionTest` (D36 gates).

**For the owner (proposition).**
- (1) `headLimitMassFlow` was listed as unchanged: its loss, rise limit and caps are unchanged, but its drive now reads `endColumn`, so the warm start stays at the root the row reaches.
- (2) The shutoff margin keeps the rise limit on the suction density; its column is the row's, so in PUMP_HEAD_LIMIT the margin stays the edge's own loss.
- (3) A gas thermosiphon does not circulate at game level (b); in real life that loop would carry 0.02 kg/s.
