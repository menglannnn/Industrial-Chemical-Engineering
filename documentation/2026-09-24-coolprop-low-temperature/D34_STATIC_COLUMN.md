# D34: a phase port's static column is what fills its line (2026-10-17)

Batch `2026-09-24-coolprop-low-temperature`, `claude/unified-thermo-port`, commit `4199dbe6` on `4eaae450`. Owner decision D34 (M5 seed 322, `M5_WORSE_WORLDS_TRACE.md`). Tools: `tools/unified-thermo-port-m5/d34/`.

**Physics (seed 322, tick 997).** Tank T1 (z = 0, 430 kPa) holds 1.83e-8 m3 (16 mg) of crude condensate (880 kg/m3) under gas of 2.262 kg/m3. Its bottom port drains through 1 m of 0.05 m line (1.96 L) into the top of tank T2 (z = -2, a dead end), 37.9 Pa higher. In real life the condensate runs down and the line holds gas: the column is the gas plus the trace's share of the line, 2.262 + (1.83e-8 / 1.96e-3) x 878 = 2.270 kg/m3, 44.5 Pa over 2 m. The line drives 6.6 Pa forward until T2 stands 44.5 Pa above T1, then nothing flows. Steady state: T1 dry, the condensate in T2, no flow in the drop line.

**What the code did.** Every static column read the port's leading phase (`endDensity`): the 16 mg trace, 17.3 kPa over 2 m, a column it cannot fill. At tick 833 that column let T2 stand 18.0 kPa above T1; at tick 997 T1 still held 1.83e-8 m3, drawn alone at a carried flow of -7e-5 kg/s with no root at any step size. The island held at tick 1097.

**The change.** `PassiveStepSolver.columnDensity(model, state, port, volume)` is the one statement of a static column. It fills the connection's own volume (`PassiveNetwork.Pipe.volume`, a contracted run's summed in `runColumnDensity`) with the port's phases in priority order: each phase up to what the state holds, the last one held taking the rest. The fill is `PhaseDraw.fill`, the one priority fill, which `InletPhase.draw` now shares to the bit (with its N ends). Special cases: a BULK end, a state holding none of the port's phases, and a leading phase that fills the line alone return `endDensity`'s value to the bit, so a tank with liquid for the whole line keeps its liquid column. `phaseDensity` is factored out of `endDensity`.

| Site | Static column (now `columnDensity`) | Draw (kept on the leading phase) |
|---|---|---|
| Row and pass | `Equations.edgeRows` passive column; `columnEnds` | mover column = suction (D6); loss density, viscosity, cap (`drawn`, `capLimit`) |
| Start drives | `staticDrive`; `initialMassFlows` per link; `supplyFlow`; `balancePressure`; valve start head; one-way estimate (new `column[][]`, its hull margin) | their losses and viscosities; `massFlowLimit` (start-flow velocity cap); `headLimitMassFlow`, `riseLimit`, shutoff margin |
| Sign tests | `closeDeadHeads`, `reopenable`, cold generator closure (run volume); `illegalWithEitherDensity` | none |
| Rest gauge | `NetworkReduction.drives` (rest test, loop drives) | `resolutionFlow` (the law's slope at zero flow) |

The start cap mode (`startCaps`, `anchorJunctions`) reads the row's driving, so it follows the column. Not changed: `SolidEventIntegrator`, `PhaseDraw.Segment`, capacities.

**Jacobian.** The column is read on the trial state and differentiated by the block and coloured finite-difference Jacobian, as the leading phase's density was. The column end stays frozen per pass, and the volume is a constant: no new structural entry. The fill is continuous in the phase volumes, with a kink where the leading phase exactly fills the line. Before, the column jumped from 880 to 2.26 kg/m3 when the trace vanished.

**Gate.** `HoldClassWorldsTest#aLiquidTraceAtATanksBottomPortLeavesItsDropLineAGasColumn` runs seed 322's 13 blocks (exact doubles) on the event path for 40 slices, in 3.3 s. Rule on: no hold, T1 dry, T2 44.895 Pa above T1 (T1's gas column is 44.895 Pa), 6.7e-9 kg/s in the drop line. That flow is T2 following T1's gas as it cools 0.08 K a slice. Rule off (`rule-off-reverse.patch` on `4199dbe6`, gate alone): held at ticks 1097, 1197, 1297, 1397 and 1497 (island 19, committed 997, Newton iteration limit at residual 0.642, pass 0). The unit case does not reproduce the hold, so it is kept as `probe/StaticColumnTraceTest.java`: a tank with the trace, a long line and a lower gas tank drain in one slice and rest, with 20 refused substeps on HEAD.

**Classes run** (javac lane, `run-touching.sh`, against `4eaae450`): all 37 `science.fluid.network` classes (183 tests) and all 61 `runtime.fluid` classes (296 tests, `scheduler.verify`) pass. Printed numbers move in two tests:
- `LiquidFullTankOutletsTest`, slices 23-40 (21-22 within the Newton tolerance, 1e-10). The top 0.01 m outlet stands as water once the tank's gas trace (2e-5 m3) is smaller than the line (7.9e-5 m3). It overflows 0.183 kg/s instead of 0.332, the feed is 0.994 instead of 1.143 kg/s, and the tank stands at 129932.9 Pa instead of 129913.3. This is the baseline's own steady state after its flush at slice 41, 18 slices earlier; `F2_END` is unchanged.
- `PhysicalFluidTopologyTest#aCompiledStackPassesTheUpperTanksWaterDownAndItsGasUp`. The last 20 L drain over slices 12-22 with no refused substep (24 before). The tanks rest with 1.4e-8 m3 (14 mg) of the 0.3 m3 left in the upper tank (0 before): the lower tank stands on the column with that trace's share (0.14 Pa). STACK_PASS: upperP 103305.62 (103487.26), lowerP 103328.37 (103509.96), upperLiquidKg 1.40e-5 (0.0), lowerLiquidKg 292.51518 (292.51535). The upper gas is at 298.61 K, not 299.57 K.
- `MIXED_GAS_COST`: newtonSolves stay at 339. allocatedMB is 170.3 against 167.8, but the test run alone gives 144.1-145.4 on both, so this is noise. Everything else is wall-clock timings and log timestamps. The baseline's `aGasStreamDrawsNoComponentItsTankNeverHeld` soft-budget hold, which happened while two lanes overlapped, passes when replayed alone.

**Code guide, fluid-science chapter.**
- Connection row: the column density is the suction stream for a mover, and for a passive run it is `columnDensity` at the pass's column end (D34).
- Class map: `PhaseDraw` adds `fill`, shared with `InletPhase`. `NetworkReduction.drives` reads `columnDensity`. `PassiveNetwork.Pipe.volume` is new.
- Routing row "a line's static column (a trace at a port, a liquid-full top outlet)": `PassiveStepSolver.columnDensity`, `PhaseDraw.fill`. Tests: runtime `HoldClassWorldsTest` (static column), `LiquidFullTankOutletsTest`, `PhysicalFluidTopologyTest` (stack).

**For the owner (proposition).**
- (1) "Start-flow cap" is read as the start drives and the start cap mode. The cap's value, density x area x velocity limit, stays the draw, as the brief says.
- (2) The column fills from the whole vessel, with no share among N ports (D34's words).
- (3) A trace whose share of the column the receiving dead end balances can stay (14 mg in the stack test). In real life it would drip down while gas rises. This is proposed as accepted at game level, below a gram.
