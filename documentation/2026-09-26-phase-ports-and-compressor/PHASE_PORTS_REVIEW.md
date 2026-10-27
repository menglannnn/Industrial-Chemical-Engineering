# Phase ports and compressor: review

- Batch: `2026-09-26-phase-ports-and-compressor`. Plan: `PHASE_PORTS_PLAN.md`; decisions: `DECISION_LOG.md` (this folder).
- One section per work package. Numbers are measured unless labelled *inferred*.
- Sections, in commit order: WP1, WP2 (superseded by D11), D10, D9, Vent gate defect, D11, D12, D13, D11+D12+D13 merge, WP3 + WP4, WP5, **WP6: gates and close** (the batch's close: residue, removals, final gates, open owner items, what the merge needs).

## WP1: ports, per-end streams, driving-pressure helper (2026-09-26)

- Author: Claude (Opus 5.5), worktree `/home/user/CreateChemE` (cloud container), branch `claude/phase-ports-compressor`.
- Base: `b83537a` = `6e1c5b6` (main 0.6.0 `c32acac` plus the tracked documentation/tools tree) plus the cloud harness commit; `git diff --stat c32acac 6e1c5b6 -- src/` and `git diff --stat 6e1c5b6 b83537a -- src/` are both empty, so the code edited is c32acac's and every line number of plan Appendix C.4 held when the work started.
- Commit: `35e354d` (`WIP phase-ports WP1: ports, per-end streams, driving-pressure helper`), on `b83537a`; not merged, not pushed.
- Scope: plan 3.1-3.3 and 3.8, science only, plus the amendment of the per-end driving-pressure helper. No runtime, GUI, codec or checkpoint change (the one runtime file touched is `IslandCertificate.graphIdentity`, as the handoff asks). WP2 (availability closure, throttle), the level head (D9), the pump inlet rules (WP3), the compressor (WP4) and the face-to-port compile (WP5) are not in it.

### 1. What changed, per file

| file | change |
|---|---|
| `science/fluid/network/PassiveNetwork.java` | `enum PhasePort { BULK, VAPOR, LIQUID }`; `Pipe` gains `firstPort`, `secondPort` (canonical 9-argument constructor); the old 7-argument constructor becomes a delegating one passing `BULK, BULK`, and every other constructor still reaches it, so no call site changed meaning; `Pipe.Identity` gains both ports; `withBlockedDirections`/`withFilter` keep them; new `withPorts`, `portAt(node)`, `drawPort(flow)`, `bulk()`. The graph constructor refuses a non-BULK port on an end whose node is not RESERVOIR or PORT. |
| `runtime/fluid/IslandCertificate.java` | `graphIdentity` digests one integer per end (the port ordinal) after the blocked mask; header text `...-graph-identity-2` (A12). |
| `science/fluid/thermo/FluidThermodynamics.java` | `PhaseStream` record and its builders `vaporStream`/`liquidStream` (null for an absent phase), plus the parts they are made of, each usable alone: `holdsVapor`/`holdsLiquid`, `vaporMass`/`liquidMass`, `liquidStreamVolume`, `vaporDensity`/`liquidDensity`, `vaporSpecificEnthalpy`/`liquidSpecificEnthalpy`, `vaporVelocityLimit`/`liquidVelocityLimit`, `vaporViscosity`/`liquidViscosity`, `liquidCarrierViscosity`. All read the state's own phase amounts, volumes and molar properties (plan table 3.2): no flash, no extra EOS root. The two streams partition the state (moles, mass, volume, enthalpy add up to the state's). |
| `science/fluid/network/PassiveStepSolver.java` | Per-end helpers `endPressure`/`portHead` (the driving-pressure helper, zero offset), `endDensity`, `endVelocityLimit`, `endViscosity`, `solidShare`, `endMoles`/`endMass`/`endSpecificEnthalpy`, `suctionMassFlow`, `massFlowLimit(pipe, donor, port)`, `carrierViscosity(state, port)`. The Newton's per-node `Transport` gains `mass`, `volume`, `solids` and its phase streams (`vapor`, `liquid`, built only for a node an open end draws; `port(p)` selects). Every Appendix A reader (C.4 re-resolution) reads the stream of the end the flow leaves, or the suction end's stream for a pump. Structural-zero drop skipped for a node with an open phase port. `reopenable`, `closeDeadHeads`, `illegalWithEitherDensity` became instance methods (they need the model for a stream density). The unused `riseLimit(pump, State)` and `massFlowLimit(pipe, State)` were removed. |
| `science/fluid/network/ConservativeTransport.java` | Phase-port outflow booked as a fixed donor with frozen stream fractions (section 4); `pinnedFlows` and the delivered rate read the drawn stream's solid share; energy, solids, filter capture, pump work, boundaries and the frozen-rate tallies read the drawn stream. |
| `science/fluid/network/PipeTransfer.java` | `sample(model, graph, states, rates, duration)` samples the drawn stream's phase split; the old 4-argument form (no model) stays for all-BULK graphs. |
| `science/fluid/network/SolidEventIntegrator.java` | Outlet mapping at the mobility check: a VAPOR port (phase present) asks `Outlet.GAS`, everything else `MIXED`; the liquid velocity uses the drawn stream's mass. |
| `science/fluid/transport/SolidMobility.java` | `check(prepared, donor, pipe, flow, outlet, drawnMass)`; the old form passes `donor.mass()`. |
| `science/fluid/network/PassiveIntervalSolver.java` | calls `implicit.reopenable(...)` (instance). |
| `src/test/.../science/fluid/network/PhasePortTest.java` | new, 7 tests (fixture 1 and four supporting checks, section 7). |

Construction sites read through, including the ones this container compiles only with Gradle: `src/main` (`FluidNetwork` 10, `PhysicalFluidTopology` 6, `FluidCheckpointCodec` 2, `FluidWorldAuthority` 2, `FluidDeviceBlockEntity` 2, the science package), `src/test` (every `new PassiveNetwork.Pipe(` / `new Pipe(` / `withBlockedDirections` / `withFilter` / `.identity()` site, 45 files; `FluidCheckpointFormatTest.withLongerSections` rebuilds pipes with the 7-argument form, BULK), `src/fluidGameTest` (`FluidHarnessGameTests` :174, 4-argument form; `FluidPresentationGameTests` :197 is a menu identity, not a pipe), `src/mcpCompat` (no site). All use unchanged constructor arities, so all still compile (gate 4 below confirms it with Gradle); runtime graphs stay all BULK until WP5 compiles faces to ports. `FluidWorldAuthority.java:294` (displayed pump limit on the suction's bulk density) is a runtime reader left for WP5 by the scope rule.

### 2. The per-end driving-pressure helper

`endPressure(node, port, state) = state.pressure() + portHead(node, port, state)`, `portHead` returning 0 in WP1: the level head (D9, option B) is the body of `portHead`. `P + 0.0` is `P` for every positive P, and every site keeps its operation order, so all-BULK doubles are unchanged (measured, section 6). Routed through it (PassiveStepSolver after WP1; C.3 numbering):

| # (C.3) | site | through the helper |
|---|---|---|
| 1 | `reopenable` :112 | both run ends' pressures, the band `reopenBand(pa, pb)` and each direction's column (end stream density) |
| 2 | carry offer :317 | `endPressure` of both ends, suction stream density for the column and the rise limit |
| 3 | `demand` :1187 (pass loop :437) | both ends |
| 4 | `initialMassFlows` chain start :855 | both run ends; the upstream choice, column, viscosity and cap read the end streams |
| 5 | valve start head :969-971 | both ends; suction stream density and viscosity |
| 6 | `initialMassFlow` :1011-1026 (and the balanced junction seed :745-822, `closeIllegalStarts` via the estimate) | callers pass end pressures; column, viscosity and cap read the end streams |
| 7 | `headLimitMassFlow` :1042 (the pump reopen start calls it) | both ends; suction stream density, viscosity, cap |
| 8 | `closeDeadHeads` :1301 | both run ends, each direction on its own end stream's density |
| 9 | `illegalWithEitherDensity` :1407 | both ends, each end on its own port's density |
| 10 | `headDensities` :1752 | both ends' seed pressures; each end's column is its port's density |
| 11 | `edgeRows` :2091 | the hydraulic row (the capped branch reuses it) |

The pump reopen start (:957-962) and the target start read the suction stream's density through `riseLimit(pump, endDensity(...))` and `suctionMassFlow` (which keeps the bulk operation order `Q*mass/volume`).

### 3. The absent stream (decision A9, extending A8)

A phase port whose phase the vessel does not hold (VAPOR: no gas volume; LIQUID: no hydrocarbon liquid and no free water; solids alone do not flow as a liquid) reads as **the node's bulk stream with a zero velocity limit**, for every reader:

| reader | absent stream |
|---|---|
| columns and sign tests (`headDensities`, `closeDeadHeads`, `illegalWithEitherDensity`, `reopenable`, start flows, carry offer) | bulk density (A8) |
| velocity cap (`edgeRows` cap, `massFlowLimit`, the velocity closure, the accepted-mode clamp, the approximation probe, the initial pump mode) | zero: a capped connection carries nothing out through the port |
| loss, filter terms | bulk viscosity, bulk carrier viscosity (finite; the loss at zero flow is zero anyway) |
| Newton targets, junction mixing, energy | bulk composition, mass and enthalpy: an uncapped iterate that moves mass through the port stays conservative |
| reconstruction | booked as a bulk outflow (implicit, the donor's end composition), consistent with the Newton |
| `PipeTransfer` | bulk phase split |
| solid mobility | a bulk end (`Outlet.MIXED`, the donor's mass) |

WP2's availability closure (`boundaryAllowed` with a start-state mask, the throttle) then keeps such a port shut for outflow; A9 only makes the stream well defined until it does. The Newton decides "absent" on its iterate's state and the reconstruction on the converged candidate, which is the state the Newton converged on, so the two agree.

### 4. The reconstruction's booking

`ConservativeTransport.reconstruct` (plan 3.3 item 2, amended by WP0):

- An edge *draws a phase* when its donor end has a VAPOR or LIQUID port and the donor's candidate state holds that phase. Its stream fractions `w_stream` (fluid components, then solid populations) are frozen from the converged candidate: the vapour's (no solid) or the condensed phases' with every solid, each over the stream's own mass; the solid split across populations is the stored inventory's scaled to the candidate's aggregate solid mass, exactly as `storedFractions` splits a fixed node's.
- It is booked on the **pinned** flow `q` (the junction mass pin applies unchanged; its filter share reads the drawn stream's solid share): the edge does not enter its donor's diagonal `outgoing`; the donor's right-hand side gets `-dt|q| w_stream` (solids included, whether or not a filter then captures them) and the receiver's `+dt|q| w_stream` (solid entries only without a filter). A junction receiver takes the vessel row like any receiver. A fixed donor (a PORT copy) uses the same `w_stream` in place of its stored fractions. The matrix is still one matrix for every component, so **one factorization** serves all components as before (two, fluid and solid, only when the island has populations and a filter, also as before).
- The pipe loop books the same `w_stream` for moles (frozen-rate tallies, fixed-node boundaries), builds the moved solids from it, books energy at the drawn stream's specific enthalpy **on the reconstructed donor** (`vaporSpecificEnthalpy`/`liquidSpecificEnthalpy`), the filter capture energy at the stream's solid enthalpy over the stream's mass, and pump work on the suction stream's specific volume. Whatever number is booked leaves the donor and reaches the receiver, so the ledger closes exactly by construction; `checkConservation` is unchanged.
- The gate error the plan inferred is measured in section 7: at step level the reconstruction's boundary booking and the stream sampled on the reconstructed state agree to 5.0e-16 (water/nitrogen) and 8.2e-15 (pentane/methane) relative, and no step of fixture 1 failed the equation gate.

### 5. Structural zeros and closed connections

- `buildSparsity` keeps every entry of a node's non-amount columns (phase allocation, T, P) when an open end draws a phase from it (plan 3.3 item 1): what a phase port carries is the phase split itself. All-BULK nodes keep the drop, bitwise.
- A passive connection the boundary rule has closed (`boundaryClosed`, row `f = q`) carries exactly zero at every iterate, so it neither builds its node's phase stream nor lifts the drop (A10). The measured consequence: closed phase ports are bitwise inert (section 7, `closedPhasePortsLeaveTheBulkLineBitwise`).

### 6. Gates

JDK in this container: OpenJDK 21.0.10 (the owner's runs used JDK 21.0.11 on Windows; one runtime-suite ledger roundoff value differs between the two JDKs at the base, as the harness agent found, so every "identical to base" comparison below is against a base captured on this machine).

| # | command (from `/home/user/CreateChemE` unless noted) | result | base on this machine |
|---|---|---|---|
| G0a | javac harness (scratchpad `science-build.sh test`, science package; log `07-javac-science-wp1.log`) | 181 found, 180 pass (the base's 173 plus the 7 new), 1 fails as at base (`PackagedSparseLuTest`: needs the built mod jar) | 174 found, 173 pass (`baseline-fluid-science.log`) |
| G0b | scratch bitwise probe (`BitwiseProbe`: chain-100 replay plus 14 all-BULK scenarios, every double in hex: mixed-gas junctions at 5 s and 0.1 s, rising/falling water lines, a pumped line, a wet-crude drain, a slurry filter, a valve) | `chain-100.json` and `scenarios.txt` byte-identical before/after; `chain-100.json` byte-identical to `src/test/resources/fluid/regression/chain-100.json` | captured on HEAD before editing |
| G0c | compile of the 32 Minecraft-free runtime sources (`rt-sources.txt`), also with `-XDshould-stop.ifError=FLOW` | the same 21 error lines as HEAD, all in `CausalModuleCoordinator`, `IslandCoordinator`, `SolidCompatibility` (Minecraft); an error planted in `IslandCertificate` is reported in both modes, so that file is type-checked | 21 lines |
| G0d | `REPO=/home/user/CreateChemE bash tools/cloud-science-harness/harness.sh all` (log `01-harness-all-wp1.log`, 67 s) | science 188/188, runtime 227/227, the 33 junction lines identical to its 6e1c5b6 reference, adjacent 38/38, regression chain-100 0.000e+00 | its reference is 6e1c5b6 |
| G1 | `./gradlew --no-configuration-cache --no-build-cache test --rerun --tests com.wormzjl.createcheme.science.fluid.* --tests com.wormzjl.createcheme.runtime.fluid.* --console=plain --continue` (log `03-gradle-fluid-suite-wp1.log`, XML `03-xml-wp1`) | **413/413** in 95 classes, 0 skipped (the base's 406 test names plus the 7 of `PhasePortTest`); the 33 MIXED_GAS_STATIC/TRANSIENT/COST and LIQUID_JUNCTION lines (read from the XML system-out) equal to the base's character for character apart from wall ms, bytes and allocatedMB: every ledger value and Newton solve count bitwise, MIXED_GAS_COST 286 Newton solves (542 ms, 135.2 MB; base 527 ms, 134.6 MB, single runs, no cost claim); 54 s | `02-gradle-fluid-suite-base-6e1c5b6.log` (run in the clean worktree `scratchpad/wt-harness`, src = 6e1c5b6): 406/406 in 94 classes, 33 MIXED_GAS/LIQUID_JUNCTION lines, MIXED_GAS_COST 286 Newton solves, 1 min 31 s |
| G2 | `./gradlew --no-configuration-cache fluidSolverRegression -PfluidRegressionMode=exact --console=plain` (log `04-gradle-regression-exact-wp1.log`) | **0.000e+00** on state/moles, temperature, phase fraction and flow; 3 accepted / 0 rejected, 4 Newton solves, 29 iterations; the three snapshot fixtures skip (no `build/probe`) | review 9.5 run 157: 0.000e+00, 3/0, 4 solves, 29 iterations |
| G3 | `./gradlew --no-configuration-cache --no-build-cache test --rerun --tests *PassiveStepSolverTest --tests *PumpJunctionStartupTest --tests *FilterBlockLineIslandTest --tests *DeadHeadedLineIslandTest --tests *NetworkRegimeTest --tests *PhysicalFluidTopologyTest --tests *PipePresentationTest --console=plain --continue` (log `05-gradle-adjacent-wp1.log`) | **38/38** in 7 classes | review 9.5 run 158: 38/38 |
| G4 | `./gradlew --no-configuration-cache compileFluidGameTestJava compileMcpCompatJava --console=plain` (log `06-gradle-compile-gametest-mcpcompat-wp1.log`) | **BUILD SUCCESSFUL**, both tasks executed; `compileJava` and `compileTestJava` of the whole tree (Minecraft sources included) had already run in G1. `./gradlew tasks --all` fails on an unrelated `tools/development.gradle` property (`columnEvaluationOutput`), so the standard source-set task names were used | |

Gradle ran one invocation at a time with `JAVA_TOOL_OPTIONS` as the environment sets it (proxy), no dev client. Logs: the session scratchpad (`/tmp/claude-0/-home-user-CreateChemE/cfcc6b94-4f24-5f12-ba62-46e9aaaec421/scratchpad/`): `01-harness-all-wp1.log`, `02-gradle-fluid-suite-base-6e1c5b6.log` (+ `02-xml-base/`, `02-junction-lines-base.txt`), `03-gradle-fluid-suite-wp1.log` (+ `03-xml-wp1/`, `03-junction-lines-wp1.txt`), `04-gradle-regression-exact-wp1.log`, `05-gradle-adjacent-wp1.log` (+ `05-xml-wp1/`), `06-gradle-compile-gametest-mcpcompat-wp1.log`; the base worktree `scratchpad/wt-harness` was only built and tested, not edited. Every WP1 gate passes; no existing test changed an assertion.

### 7. Fixture 1 and the supporting tests (`PhasePortTest`, measured)

Vessels: 1 m3 at 298.15 K, (a) half water, half nitrogen at 200 kPa, (b) 0.4 m3 n-pentane under 0.6 m3 methane at 300 kPa, both two-phase after the flash; three lines (10 m, 10 mm) to nitrogen voids at 1 atm: VAPOR port, LIQUID port, BULK end.

| test | measured |
|---|---|
| `aStepCarriesExactlyEachPortsPhase` (one 0.5 s step) | flows (VAPOR, LIQUID, BULK) (a) 0.009303, 0.1903, 0.1361 kg/s; (b) 0.01723, 0.2235, 0.1739 kg/s. Vapour line: hydrocarbon liquid and free water exactly 0; liquid line: vapour exactly 0; each line's composition proportional to its phase's in the end state to 1.7e-16 (a), 0 (b); mass = moved mass; the bulk line bitwise the bulk sampling of the same donor state; reconstruction booking vs sampled stream 5.0e-16 (a), 8.2e-15 (b); vessel ledger closes to 1e-12 |
| `phasePortLinesIntegrate...AtTenthSecondSlices` (100 x 0.1 s) | (a) 100 accepted, 0 rejected, component ledger 1.6e-15, energy ledger 7.0e-16; (b) 100 / 0, 1.8e-15, 8.8e-16; line composition vs vessel phase 2.2e-16 |
| `phasePortLinesIntegrate...AtFiveSecondSlices` (2 x 5 s) | (a) 6 accepted, 0 rejected, 3.9e-16, 8.9e-17; (b) 6 / 0, 4.7e-16, 1.7e-16. A slice's line carries the average of its steps' end compositions: gap to the slice's end composition 2.3e-4 (a) and 1.3e-3 (b), never more than the vessel's own composition drift over the slice (asserted to 1e-12) |
| `closedPhasePortsLeaveTheBulkLineBitwise` | with the VAPOR and LIQUID lines closed both ways, every double (states, inventories, flows, boundaries, transfers; a zero of either sign as +0) equals the all-BULK graph's: both vessels, 5 x 1 s and 10 x 0.1 s steps; the water vessel also 2 x 5 s and 20 x 0.1 s interval-solver slices |
| `phasePortsFeedJunctionsAndCloseTheLedger` (water vessel, each port through an owned-holdup junction to a void; 20 x 0.1 s, 2 x 5 s) | ledger 3.2e-16 / 4.1e-16 (0.1 s), 1.7e-16 / 1.5e-16 (5 s); the gas junction's outlet 98.4 % nitrogen, the liquid junction's 100 % water |
| `solidsLeaveThroughTheLiquidPortOnly` (water + 100 kg demo particle, LIQUID line through a filter, VAPOR line plain; 4 x 1 s) | vapour line solids empty every slice; filter captured 9.716 kg, vessel 90.284 kg, solid ledger closes to 1e-10; component 1e-12, energy (vessel plus cake) 1e-10 |
| `onlyAVesselEndMayCarryAPhasePort...` | non-BULK refused on JUNCTION, GENERATOR, VOID; accepted on RESERVOIR, PORT; legacy constructors BULK; a port changes the identity |

Why the pentane vessel's closed-line comparison runs on the step solver: an island holding a hydrocarbon liquid is integrated by `SolidEventIntegrator` (`SolidMobility.monitored`), whose `masks(graph, blocked)` rebuilds every pipe's `blockedDirections` from its own closure set at each interval start, so a caller's both-way block is reopened. That is existing behaviour, not a WP1 change.

### 8. Open defect found (base, not WP1): unsaturated water vapour into a junction seeded dry

*Fixed 2026-09-26 by decision D10 (owner: option 1, a water trace in the junction seed), a separate commit after WP2; see the section "D10" below.*

- *Measured.* A 1 m3 nitrogen vessel with 0.5 % or 1 % water (no liquid water: unsaturated) drawn through a **BULK** end into a junction seeded as dry nitrogen, then to a void, fails every interval at 0.1 s and 5 s: "Substep refinement exhausted: Conservative reconstruction fails equation gate: 1.3058e-8" (0.5 %) and 2.6151e-8 (1 %), 20 rejections each. Identical on HEAD 6e1c5b6 and on WP1 (scratch driver `hprobe/HumidJunctionProbe.java`, both builds).
- *Mechanism (instrumented scratch copy of the gate).* A junction's `PhaseLayout` carries water only if its seed holds water (`PhaseLayout` :69). The Newton ignores the arriving water, the reconstruction books it into the junction, and the gate sees the junction's amount-normalisation row off by the booked water fraction (row local 2 of a 3-row junction block, residual proportional to dt: -4.149e-8 at dt 1.9e-7 s). `phaseCorrection` does not reseed the junction, because unsaturated water vapour does not change the junction's phase code. A saturated or bulk-liquid feed does change it, which is why ordinary wet lines work.
- *Why it matters now.* A VAPOR port on a wet tank always delivers water vapour, which is unsaturated at a lower-pressure junction, so any compiled junction seeded dry downstream of a top outlet hits it (WP5 compiles faces to ports; junction seeds come from the island's first boundary state). Fixture 1's junction test seeds the gas junction with the humid gas to exercise the port booking instead.
- *Options for the owner (not implemented: each rests on a choice no decision fixes):*
  1. Seed a water trace (like `initialPhaseSeeds`' 1e-12 entry trace for vessels) into every junction whose reachable set holds water and whose seed does not. Changes the Newton unknowns of such junctions in all-BULK islands, so it is not bitwise on existing fixtures; cost small.
  2. Let the converged-point `phaseCorrection` reseed a junction whose reconstructed state holds a component outside its layout basis (water or a masked hydrocarbon). Bitwise wherever the gate passes today, except where a passing reconstruction already carries such a trace below the gate; an extra pass only where it fires.
  3. Accept it for WP1 and fix it with WP5, when faces compile to ports and junction seeds can take the delivered stream.
  Recommended: 2 (smallest blast radius, fixes the BULK case too); to be measured on the fluid suites first.

### 9. What was not run here, and why

- Fluid GameTests (`fluidGameTestServer`) and the in-game (MCP bridge) scenarios: not in WP1's gates (plan section 7: GameTest source set compiles, G4); WP1 changes no runtime path and runtime graphs carry no port until WP5.
- The owner's Windows JDK 21.0.11 lane: not available; every comparison is against a base captured on this machine (section 6).
- No timing claim is made (no benchmark rule run).

### 10. Plan text amended

Where WP0 (Appendix C, "Where c32acac contradicts the plan") already said the plan was wrong, the plan text now says so in place, each amendment tagged "Amended 2026-09-26 after WP0": section 2 (the pump block's junction owns holdup), 3.1 (`PhasePort`, not `Port`; the stream acts on outflow, the driving pressure both ways), 3.2 (junction mixing reads the live upwind donor; the absent-stream density is A8), 3.3 item 2 (a junction receiver takes the vessel row; booking on the pinned flow), 3.4 (`boundaryAllowed` needs a start-state availability mask in WP2), 3.6 (the partial target-above-supply rule at PSS :293-294), section 6 (two, not three, nitrogen `PumpRiseScalingTest` tests; no flat control needed under A1; 34 GameTests, not 85), 8 R7 (closed).

### 11. Scratch material

Measurement drivers written for this package live in the session scratchpad, not in the repository: `probe/BitwiseProbe.java` (G0b), `jprobe/JunctionProbe.java` and `instr/src/PassiveStepSolver.java` (the gate instrumentation, section 8), `hprobe/HumidJunctionProbe.java` (the base defect). They are candidates for a `tools/` folder at WP6's cleanup (`tools/` is owned by another agent in this session, so none was added here).

## WP2: the "outlet phase absent" state (2026-09-26)

- Author: Claude (Opus 5.5), worktree `/home/user/CreateChemE` (cloud container), branch `claude/phase-ports-compressor`.
- Base: `3dbd9b8` (WP1 `35e354d`, its review `3097e60`, the WP1 tools commit). Commit: `d836cf2` (`WIP phase-ports WP2: absent-phase closure, throttle, reopen allowance`), on `3dbd9b8`; not merged, not pushed. Tools and this line: the following commit.
- Scope: plan 3.4 exactly: availability on the step's start state through `boundaryAllowed` with a start-state mask, the outflow throttle in `edgeRows`, the mask in the workspace and cycle key, the port refusal as a per-link allowance of the boundary-reopen retry, and the phase-vanishing case verified. Not in it: the level head (D9), the pump (WP3), the compressor (WP4), runtime (WP5). The base defect of WP1 section 8 is untouched (owner decision; since taken as D10, a separate commit after this one).

### 1. What changed, per file

| file | change |
|---|---|
| `science/fluid/network/PassiveStepSolver.java` | Constants `PHASE_PORT_OPEN = 0.01`, `PHASE_PORT_RESERVE = 0.005` (A4); `portPhaseShare(state, port)`, `portAvailable(state, port)`; `closedPorts(graph)`, the start-state availability mask (per node, bit 1 a VAPOR port some connection has there is closed, bit 2 a LIQUID one); `boundaryAllowed(graph, pipe, flow, closedPorts)` (the static rule, then: flow leaving an end whose phase port is closed in the mask is refused). Every reader passes the solve's mask: `reopenable` (built from the start graph it is handed), the pass loop's forbidden-direction closure, `closeDeadHeads`, `closeIllegalStarts`, `reachableComponents`, `startPoint`'s junction propagation, the approximation probe, and also `initialPhaseSeeds` and the balanced junction seed (`seedBoundaryJunctions`/`balancedBoundarySeed`), the two other `boundaryAllowed` readers. `WorkspaceKey` gains the mask (equals and hash). `Equations` gains `closedPorts` and `throttles` (built once per pass from the start states, `null` for a rate solve, while a device holds `PUMP_TARGET`, and for a graph with no open phase port); `edgeRows`' cap branch takes `min(limit, throttle)` for the direction leaving the port, beside the filter's room limit; `buildInitial` clamps the pass's start flows to the throttle. |
| `src/test/.../science/fluid/network/PhasePortClosureTest.java` | new, 15 tests: fixtures 2 and 3 at 5 s and 0.1 s slices plus the throttle and the vanishing phase (section 5). |

`PassiveIntervalSolver` needed no change: its be-reopen retry calls `reopenable(start, result)` with the step's start graph, from which `reopenable` builds the same mask the solve used. `PhasePortTest` (fixture 1) is unchanged and green with every number of WP1 section 7 identical (its phases stay far above 1 % and its throttles above the velocity caps, so nothing binds).

### 2. The rules as implemented

- **Availability** (stateless, start state). For a VAPOR port `phi = vaporVolume / V`, for a LIQUID port `phi = (liquidVolume + waterVolume) / V`, `V = volume` of the node's state in the graph the step starts from (the accepted state; the junction seed and the cold-start rate seed change junction states only). The port may carry outflow iff `phi >= 0.01`. A closed port refuses the direction leaving its end in `boundaryAllowed`; inflow through it is never refused (D3). Only ports some connection has enter the mask, so the mask of an all-BULK graph is all zero whatever its states and every reader decides exactly as before (gate G1: bitwise).
- **One mask per step** (plan C.3 caveat). `reopenable` evaluates the run's drive on the step's end states but the allowances on the start state's mask, the one the solve refused on. Measured consequence (fixture `thePortAlone...`, section 5): with the reopen test reading no mask, every step in which the port was the only refusal was refused once and re-solved (20 reopens in 24 slices at 5 s, 322 in 600 at 0.1 s); with the start mask, one reopen each, at the step where the inflow actually starts.
- **Throttle.** For each end whose port is open on the start state and whose connection may carry outflow from it:

  `throttle = (phi_start - 0.005) * V_start * rho_stream,start / (n * dt)`

  `rho_stream,start` = the drawn stream's density (`vaporDensity` / `liquidDensity` of the start state; the liquid stream's volume includes the solids, as in WP1); `n` = the number of ends of that vessel with the same phase port whose connection may carry outflow from it by `boundaryAllowed` with the mask (static: blocked masks, generator ends; start-of-solve closures do not change it, so it is a constant of the solve); **`dt` = the solve's step**: the backward-Euler step the interval solver attempts (its estimate truncated to the rest of the interval, halved on every retry), never the slice. Over a step the ports of one phase therefore draw at most `(phi_start - 0.005) V rho_start` together, the phase above the reserve at the start density. It enters the saturated law of `edgeRows` as `limit = min(velocity cap [, filter room], throttle)` for the direction leaving the port (direction index 0 = leaving the first end, 1 = the second), a constant for the solve; absent in a rate solve (no step) and while any device of the island holds `PUMP_TARGET` (the filter's `prescribedFlow` exemption: a prescribed flow and a saturated inlet are two equations for one edge; WP3 revisits the pump fed from a port). A throttled connection's accepted mode stays PASSIVE (as for the filter room limit; presentation only).
- **Start point.** The pass's start flows are clamped to the throttle (A15). Found necessary, not assumed: a 5 s step from rest on a 2 MPa tank whose throttle is 1.5 % of the velocity cap failed its first Newton at the iteration limit (residual 0.076, pass 0) when it started at the cap; clamped, it converges and lands on the reserve.
- **Phase vanishing inside a step.** Nothing new: the pass whose layout has lost the phase reads the port's stream as absent (A9, zero velocity limit), so the cap is `min(0, throttle) = 0` and the converged flow is zero with no illegal direction; the next step start finds `phi = 0 < 0.01` and closes the port. Measured in section 5 (flow -1.5e-39 kg/s, mode PASSIVE, next step CLOSED).

### 3. What the band does, measured (plan 3.4 amended)

The plan expected a draining port to land its phase on the reserve and the band `[0.005, 0.01)` to keep it from reopening. Measured: **the throttle binds only where one step spans the band.** Under the state-change controller (5 % of a vessel's mass or pressure per step) a draining port's steps are short near the end, so the port closes at the first step start below `phi_open`, holding its phase in `[phi_reserve - drift, phi_open)`: 0.9934 % (5 s) and 0.9901 % (0.1 s) on the drained water tank, 0.9865 % on the squeezed gas cap (0.1 s). Where a step does span it (a 2 MPa tank, a 5 s step), the throttle lands the phase on the reserve exactly: 0.49998 % (drift -1.9e-7). The guarantee kept is the floor: no step draws a phase below the reserve by more than the flash drift.

Consequence (risk R1): a port fed while it drains duty-cycles about `phi_open`. Scratch probe `PhasePortDutyCycleProbe` (a 1 m3 nitrogen tank at 150 kPa, 2 % water, fed 0.011 kg/s of water through a 4 mm line, drained through a LIQUID port that carries 1.5 kg/s when open): 45 open/closed transitions in 300 s at 0.1 s slices (86 open slices, 0 rejections), 9 at 5 s (6 open slices, 23 state-change rejections), 5 at fixed 1 s steps; the water share stays in [0.9850 %, 1.0001 %] (0.1 s) and [0.9657 %, 1.0018 %] (5 s) after the first 100 s; no nonconvergence, no held slice, ledgers closed. It is an overflow at 1 %, stepped. Options in section 10.

### 4. Controller rejections and reopens per slice (risk R3)

| fixture (slices) | cadence | accepted / rejected | rejections by slice and reason | reopens |
|---|---|---|---|---|
| dry, LIQUID drain (40 / 1450) | 5 s | 154 / 4 | slices 6, 7: 1; 8: 2 (state change, the vessel's mass shrinking) | 0 |
| | 0.1 s | 1450 / 0 | none | 0 |
| squeeze, VAPOR vent (40 / 900) | 5 s | 151 / 8 | 0: 1 reopen (the vent at rest at the start, BROKEN class 3, base); 3, 8: 1 equation gate each (below); 12: 5 state change (the cushion compressed from 101.6 to 299 kPa) | 1 |
| | 0.1 s | 900 / 1 | 0: 1 reopen (same) | 1 |
| inflow through a closed port (12 / 600) | 5 s | 65 / 27 | 6: 25 (11 reopens, 13 Newton line-search stalls, 1 iteration limit); 7: 2 state change | 11 |
| | 0.1 s | 612 / 19 | 342: 19 (7 reopens, 12 line-search stalls) | 7 |
| the port alone refuses (24 / 600) | 5 s | 72 / 1 | 6: 1 reopen | 1 |
| | 0.1 s | 600 / 1 | 321: 1 reopen | 1 |
| same port drains via a junction (40 / 1000) | 5 s | 217 / 11 | 0-3: 7, 2, 1, 1 state change (water filling a 1 atm gas tank) | 0 |
| | 0.1 s | 1028 / 20 | 0-14: 1-2 each, state change (same) | 0 |
| closed port refuses to drain into a junction (120) | 0.1 s | 148 / 20 | 0-14: 1-2 each, state change (same fill) | 0 |
| dissolving cap (24 / 300) | 5 s | 129 / 17 | 0: 17 state change (200 kPa to 1 MPa) | 0 |
| | 0.1 s | 347 / 10 | 18: 10 state change | 0 |

- The throttle bound no step in these interval fixtures (section 3), so none of the rejections is the throttle's; the "negative transport reconstruction" and several-ports overdraw of R3 did not occur. The per-vessel constraint row R3 held in reserve is not needed on this evidence.
- The reopens of the inflow fixture are the BROKEN class 3' cost review 8.8 (c) measured for water entering a dry gas tank (reopened solves stall from a zero start flow across the liquid/gas friction kink before halving converges); the retries succeed, no slice ends bottled.
- **Equation-gate rejections with a VAPOR vent** (squeeze at 5 s: 2, residual 4.45e-8 and 2.06e-7, the tank's local row 4, dt 2 s, Newton converged to 9.2e-10 and 2.6e-10). Classified as WP1 behaviour, not WP2: the same fixture on the WP1 tree (`3dbd9b8`, no mask, no throttle) gives 4 gate rejections in the first 12 slices where WP2 gives 2, and the same fixture with BULK ends gives none. Halving recovers them; no held slice. Options in section 10.

### 5. Fixtures (`PhasePortClosureTest`, measured; Gradle run G1, identical to the harness run)

Every slice of every interval fixture commits whole (FULL, advanced = interval) and closes the component ledger (worst 2.3e-13 relative) and the energy ledger (worst 6.3e-14).

| test | measured |
|---|---|
| `aLiquidPortDrainsDryAndStaysClosed...` (0.1 m3 water under N2 at 200 kPa, LIQUID port 10 m 25 mm to a void) | 5 s: open for 8 slices (out 10.9 to 4.6 kg per slice), closes inside slice 8, slices 9-39 start closed (31 x 5 s): out 0, mode CLOSED, water share 0.009934488833801991 constant; one open-to-closed transition. 0.1 s: closes at slice 423, 1027 slices closed (102.7 s), water share 0.009901226157602 constant. |
| `aVaporPortIsSqueezedShutByLiquid...` (0.8 m3 water under N2 at 1 atm, filled by a 300 kPa water generator through the LIQUID port, VAPOR vent 10 m 25 mm to a void) | 5 s: vent open 12 slices (18 g per slice), closes inside slice 12, the cushion is compressed to 299.1 kPa in that slice and to 300000.00004 Pa after; final gas share 0.19 %. 0.1 s: closes at slice 617 with the gas at 0.9865 % and 101641.8 Pa, final 300000.0009 Pa, gas 0.327 %. One transition each. |
| `inflowOpensAClosedLiquidPort...` (N2 tank at 150 kPa, LIQUID port facing a 130 kPa water generator, BULK vent 10 mm) | dead-headed (CLOSED) while the tank stands above 130 kPa; water enters in slice 6 (5 s: 0.157 kg, the tank at 128.1 kPa at the slice end; 11 reopens) and slice 342 (0.1 s, 7 reopens); no slice ends bottled (below 130 kPa minus the band with nothing entering); nothing leaves through the port. |
| `thePortAloneRefusesOutflow...` (N2 tanks A 150 kPa / B 120 kPa joined at A's LIQUID port, B fed by a 200 kPa N2 generator) | A holds 150000.0 Pa, run CLOSED, 0 reopens until B passes A; B's gas enters A from slice 6 (5 s) / 321 (0.1 s) after one reopen; A never loses gas through the port. Counterfactual (reopen test with no mask): 20 / 322 reopens. |
| `theSamePortDrainsOnceItsLiquidIsThere...` (N2 tank at 1 atm, LIQUID port to a junction fed by a 300 kPa water generator and drained to a void; generator lowered to 110 kPa after 40 s) | water enters through the closed port (10.5 kg per 5 s slice), 8.31 % of the tank at 113.6 kPa when the generator is lowered; the same port then drains (0.31 kg in the first 5 s slice through the 10 mm line) and the generator's line closes. |
| `aPortBelowItsOpeningShareRefusesToDrainIntoAJunction` (same, generator lowered to 100 kPa after 2 s) | water share 0.42 % when lowered; for 100 slices the tank stands above the junction (103641 against 101325 Pa) and keeps its water, out 0. |
| `theThrottleLandsAnOpenPortAtItsReserveInOneStep` (2 MPa tank, 2 % water, one and two LIQUID lines 2 m 50 mm, one 5 s step) | flows equal the throttle to the printed digit: 2.996802216998662 kg/s (one line), 1.498401108499331 each (two, n = 2); water lands at 0.49998 %; the next step starts CLOSED with flow 0. |
| `aPhaseThatVanishesInsideAStep...` (n-pentane with a 1.5 % methane cap at 200 kPa, fed pentane at 1 MPa, VAPOR vent 10 mm; one 5 s step) | the cap dissolves inside the step (888 kPa at its end); vent flow -1.47e-39 kg/s, accepted mode PASSIVE (no illegal direction); next step: flow 0, CLOSED. |
| `aDissolvingCapIntegrates...` (same vessel, interval solver) | the port closes once (5 s: slice 1; 0.1 s: slice 7), stays closed, the liquid-full vessel stands at 1000000.2 Pa (5 s) / 1000000.00000003 Pa (0.1 s). |

### 6. Gates

| # | command (from `/home/user/CreateChemE`) | result |
|---|---|---|
| G0 | `LIB=<scratch>/lib OUT=<scratch>/harness-wp2 REPO=/home/user/CreateChemE bash tools/cloud-science-harness/harness.sh all` (log `wp2-01-harness-all.log`, 77 s) | science 203/203 (188 + 15), runtime 227/227, the 33 junction lines identical to its `6e1c5b6` reference, adjacent 38/38, chain-100 0.000e+00 |
| G1 | `./gradlew --no-configuration-cache --no-build-cache test --rerun --tests com.wormzjl.createcheme.science.fluid.* --tests com.wormzjl.createcheme.runtime.fluid.* --console=plain --continue` (log `wp2-02-gradle-fluid-suite.log`, XML `wp2-02-xml/`, 51 s) | **428/428** in 96 classes, 0 skipped (406 base + 7 WP1 + 15 WP2; names `wp2-02-test-names.txt`); the 33 MIXED_GAS_*/LIQUID_JUNCTION lines (`wp2-02-junction-lines.txt`) character-identical to `tools/phase-ports-probes/wp1/logs/02-junction-lines-base.txt` with ms/bytes/allocatedMB masked (`diff` empty); MIXED_GAS_COST 286 Newton solves (612 ms, 135.2 MB, single run, no cost claim) |
| G2 | `./gradlew --no-configuration-cache fluidSolverRegression -PfluidRegressionMode=exact --console=plain` (log `wp2-03-gradle-regression-exact.log`) | **0.000e+00** on state/moles, temperature, phase fraction and flow; 3 accepted / 0 rejected, 4 Newton solves, 29 iterations |
| G3 | the 38 adjacent (WP1 G3 command; log `wp2-04-gradle-adjacent.log`, XML `wp2-04-xml/`) | **38/38** in 7 classes |
| G4 | `./gradlew --no-configuration-cache compileFluidGameTestJava compileMcpCompatJava --console=plain`, then again with `--rerun` on both tasks (log `wp2-05-gradle-compile-gametest-mcpcompat.log`) | **BUILD SUCCESSFUL**, both tasks executed on the second run |

One Gradle invocation at a time, `JAVA_TOOL_OPTIONS` as the environment sets it, no dev client. Logs in the session scratchpad and copied to `tools/phase-ports-probes/wp2/logs/`. No existing test changed; fixture 1 unchanged. Section 11 records the re-run after the last (comment-only) edit.

### 7. What was not run, and why

- Fluid GameTests and the in-game scenarios: WP2 changes no runtime path, and runtime graphs carry no port until WP5.
- The owner's Windows JDK 21.0.11 lane: not available here.
- No timing claim.

### 8. Plan text amended (tagged "Amended 2026-09-26 after WP2")

3.4 (the throttle's landing and the band, the readers list), section 5 (the "port closed" audit), section 6 fixtures 2 and 3 (landing interval; a generator refuses inflow, so "the same port drains" goes through a junction to a void), section 7 WP2 row, section 8 R1 and R3 (measured), Appendix C.3 caveat (resolved).

### 9. Decisions recorded

`DECISION_LOG.md`, "WP2 defaults": A13 (the availability mask), A14 (the throttle's `dt`, `n`, density and exemptions), A15 (start point inside the throttle), A16 (the landing interval under the stateless rule).

### 10. Options for the owner

1. **The band under short steps (section 3).** (a) Accept: the port closes at the first step start below 1 %, the throttle is a floor, a port fed while it drains duty-cycles about 1 % (measured benign: no failure, no held slice). (b) Hysteresis in the committed endpoint mode: a port that drew in the previous accepted step stays open down to the reserve, a closed one opens at 1 %. It lands phases on the reserve and removes the duty cycle, but is carried state: it would ride on the pump's existing carry (`previousModes`, restated from the committed endpoint modes by `replayStart`, review 8.9 (d)), which is replay-safe in the same way, not a new history input of 8.9 (b)'s kind. Recommended: (a) for v1, (b) only if in-game use shows the duty cycle (it is visible as a line flickering open/closed in the GUI at the 5 s cadence at most). Not implemented: the plan and the brief require the band stateless.
2. **Pump fed from a port (WP3).** The throttle is off while any device holds `PUMP_TARGET` (as the filter room limit). WP3's `INLET_EMPTY` and target-above-supply start decide whether to keep that exemption or to throttle the pump edge itself.
3. **Gate rejections with a VAPOR vent (section 4, WP1 behaviour).** (i) Accept (2-4 halvings in 12 slices, recovered); (ii) investigate the frozen-stream booking against the Newton's stream on a filling tank (row 4 of the tank block) in WP6. Recommended (i) now, (ii) with WP6's gates.

### 11. Re-run after the last edit

The `PHASE_PORT_RESERVE` javadoc was corrected to the measured behaviour (section 3) after G1-G4 (comment only). Re-run on the final tree: G1 (log `wp2-06-gradle-fluid-suite-final.log`) 428/428 in 96 classes, the same test names, the 33 junction lines again identical to the base (`wp2-06-junction-lines.txt`); G2 (log `wp2-07-gradle-regression-exact-final.log`) 0.000e+00.

### 12. Scratch material and commit

Code commit `d836cf2`. Drivers and logs: `tools/phase-ports-probes/wp2/` (`src/PhasePortDutyCycleProbe.java` the R1 probe, `src/SqueezeGateProbe.java` the gate classification probe, `logs/` the gate logs above). The counterfactual runs (reopen test with no mask; the gate instrumentation) were temporary edits of `PassiveStepSolver.java`, reverted; their outputs are in `logs/`.

## D10: water trace seed in junctions with water reachable (2026-09-26)

- Author: Claude (Opus 5.5), same worktree and branch. Base: `9c723e0` (WP2 `d836cf2` plus its tools commit). Commit: `07e7426` (`WIP phase-ports D10: water trace seed in junctions with water reachable`), on `9c723e0`; not merged, not pushed.
- Decision: D10 (owner, 2026-09-26): the base defect of WP1 section 8 is fixed by option 1, a water trace seeded into junctions, chosen over the recommended option 2.

### 1. What changed

| file | change |
|---|---|
| `science/fluid/network/PassiveStepSolver.java` | `initialPhaseSeeds`: a junction whose reachable set holds water (`reachable[node][water]`, from `reachableComponents` on the connections the start closures leave open) while its own state holds none (`waterLiquid + waterVapor == 0`) is seeded with `waterTraceSeed(state)`: its state's total amounts plus a water entry of `1e-12` of their sum, flashed at the junction's own temperature and pressure (`flashTP`), solids carried over; if that flash leaves the property domain the junction keeps its state. Every other node is seeded exactly as before. |
| `src/test/.../science/fluid/network/JunctionWaterTraceTest.java` | new, 2 tests: a humid (unsaturated, 0.5 % and 1 % water) nitrogen vessel on a BULK line, and a wet two-phase vessel's VAPOR port, each into a junction seeded as dry nitrogen and on to a void, at 0.1 s (40 slices) and 5 s (4 slices). |

### 2. The mechanism (why this reaches the layout)

A node's `PhaseLayout` carries water - the water unknown(s), the water balance row, the junction's water mixing row and the vapour's water partial-pressure row - exactly when its **seed** holds water (`PhaseLayout` constructor: `water=seed.waterLiquid()>0||seed.waterVapor()>0`); hydrocarbons reach a layout through the reachable mask, water has no mask entry. A vessel lacking a reachable component already gets a `1e-12` entry trace in `initialPhaseSeeds`; a junction was seeded with its own state only. The trace is therefore planted in the same place and the same size, and it reaches the junction's layout through its seed: flashed at the junction's (T, P), `1e-12` of water is unsaturated vapour, so the seed holds `waterVapor > 0` and the layout gains the water vapour unknown and its rows. Once the first step is accepted the junction's state carries the water it received (or its retained trace), so the trace fires only on a junction's first dry solve (measured: once per case of the new test; see 4). The junction's owned inventory is untouched - the seed is a starting point and a basis, the accumulation rows start from the stock - so the ledgers are unchanged by construction (the new test checks the junction starts with zero water and closes the ledger over vessel plus holdup).

### 3. Verification

- `tools/phase-ports-probes/wp1/src/HumidJunctionProbe.java` (the WP1 reproduction), on the D10 tree: humidity 0, 0.5 %, 1 % at 0.1 s (20 slices) and 5 s (2 slices): all six integrate, 20 / 6 accepted, 0 rejected. On the WP2 tree (`d836cf2`), unchanged: 0.5 % and 1 % fail every interval ("Substep refinement exhausted: Conservative reconstruction fails equation gate: 1.3058e-8" / 1.3058e-7 at 5 s; 2.6151e-8 / 2.6151e-7), as WP1 recorded. Logs `tools/phase-ports-probes/d10/logs/06-humid-probe-*.txt`.
- `JunctionWaterTraceTest` (measured, Gradle run): 0.5 % BULK: 40/0 accepted/rejected at 0.1 s, 12/0 at 5 s, component ledger 8.4e-16 / 4.2e-16, energy 2.6e-16 / 1.3e-16, junction water fraction 0.005, water to the void 0.0043 / 0.0207 mol; 1 % BULK: 40/0, 12/0, 1.1e-15 / 4.6e-16, 2.4e-16 / 1.2e-16, fraction 0.010; VAPOR port of the half-water tank: 40/0, 12/0, 4.3e-16 / 2.8e-16, 2.6e-16 / 2.4e-16, junction water fraction 0.0162 / 0.0175 (the headspace's humidity, diluted by the dry holdup), water to the void 0.0136 / 0.0674 mol.

### 4. Gates, and the re-baseline (none needed)

The brief allowed a recorded re-baseline where a junction gains the trace. Measured: **no existing fixture's junction gains it**, so nothing moved.
- Where the trace fires (temporary print in `waterTraceSeed`, harness science + runtime + adjacent, reverted): only `JunctionWaterTraceTest` (6 times, once per case and cadence). Every other junction in the suites either has no water reachable (the mixed-gas methane/nitrogen matrices) or is seeded wet (liquid lines and filter blocks seed their junctions from a water boundary; `PhasePortTest`'s junction test seeds the gas junction humid on purpose, a comment there still names the WP1 workaround). chain-100 has no junction.
- WP1's bitwise probe (`tools/phase-ports-probes/wp1/src/BitwiseProbe.java`: chain-100 plus 14 all-BULK scenarios in hex, among them mixed-gas junctions at 5 s and 0.1 s, water lines, a pumped line, the wet-crude drain, a slurry filter) on the WP2 and D10 trees: `scenarios.txt` and `chain-100.json` byte-identical, and `chain-100.json` byte-identical to `src/test/resources/fluid/regression/chain-100.json`.

| # | command | result |
|---|---|---|
| G0 | `harness.sh all` (log `d10-01-harness-all.log`, 84 s) | science 205/205, runtime 227/227 with the 33 junction lines identical to its reference, adjacent 38/38, chain-100 0.000e+00 |
| G1 | the fluid suites (WP2 G1 command; log `d10-02-gradle-fluid-suite.log`, XML `d10-02-xml/`, 52 s) | **430/430** in 97 classes, 0 skipped (WP2's 428 plus the 2 new); the 33 MIXED_GAS_*/LIQUID_JUNCTION lines character-identical to `tools/phase-ports-probes/wp1/logs/02-junction-lines-base.txt` (ms/bytes/allocatedMB masked): every ledger value and Newton solve count unchanged, MIXED_GAS_COST 286 Newton solves (580 ms, 135.3 MB) |
| G2 | exact regression (log `d10-03-gradle-regression-exact.log`) | **0.000e+00**, 3 / 0, 4 Newton solves, 29 iterations |
| G3 | the 38 adjacent (log `d10-04-gradle-adjacent.log`) | **38/38** in 7 classes |
| G4 | GameTest and mcpCompat compile, both with `--rerun` (log `d10-05-...`) | **BUILD SUCCESSFUL**, both executed |

**Base for WP3:** the junction lines of this commit are captured in `tools/phase-ports-probes/d10/logs/02-junction-lines-d10.txt` (identical to the WP1 base capture, so WP3 may compare against either); the test names in `02-test-names-d10.txt` (430).

### 5. Not run

GameTests and in-game scenarios (no runtime change; runtime graphs carry no phase port until WP5); the Windows lane.

## D9: level head at the bottom port (option B, H = 1 m) (2026-09-26)

- Author: Claude (Opus 5.5), same worktree and branch. Base: `3338663` (D10 `07e7426`, its tools commits, and the merge of the extreme-topology test branch, which adds `ExtremeTopologyIslandTest` and touches no `src/main`). Commit: `fdf3574` (`WIP phase-ports D9: level head at the bottom port (option B, H = 1 m)`), on `3338663`; not merged, not pushed. Tools and this line: the following commit.
- Decision: D9 (owner, 2026-09-26), option B of `LEVEL_HEAD_REVIEW.md`: a level head at the LIQUID (bottom) port only, fixed H = 1 m, no setting. Design: that review, sections 2-5 and 8.

### 1. What changed, per file

| file | change |
|---|---|
| `science/fluid/network/PassiveStepSolver.java` | `LEVEL_HEAD_HEIGHT = 1` (m). `portHead(node, port, state)` (the WP1 helper, zero until now) returns `GRAVITY * model.liquidMass(state) * LEVEL_HEAD_HEIGHT / node.inventory().volume()` at a LIQUID port of a RESERVOIR or PORT node when that condensed mass is positive, and `0` everywhere else. `endPressure` (both overloads), `portHead` and `demand` became instance methods (they need the model for the condensed mass); no other line changed. Javadoc of `endPressure`/`portHead` restated. |
| `science/fluid/network/PassiveNetwork.java` | `PhasePort` javadoc: the port's driving pressure is the node's plus the level head at a LIQUID port (comment only). |
| `src/test/.../science/fluid/network/LevelHeadTest.java` | new, 8 tests (section 5). |
| `src/test/.../science/fluid/network/PhasePortClosureTest.java` | four assertions re-baselined to the bottom port's pressure (section 4): `Slice` gains `headEnd`, the watched vessel's level head at the slice end; the squeeze and dissolving-cap endings assert `P + h = P_generator` instead of `P = P_generator`, same tolerance (1 Pa); javadoc records the old values. |

`IslandCertificate` is unchanged: under option B, H is a constant and V is already the vessel's identity in `graphIdentity`, and the LIQUID port is in the digest since WP1 (A12), so a saved certificate cannot outlive anything the head depends on. No runtime, codec, wire or GUI change.

### 2. The formula and where it enters

`h = g * m_c * H / V`, `g` = `GRAVITY` (9.80665), `m_c` = `FluidThermodynamics.liquidMass(state)` = hydrocarbon liquid + free water + every solid of the state the caller is stating a pressure for, `H` = 1 m, `V` = the vessel's inventory volume. `endPressure(node, port, state) = state.pressure() + portHead(node, port, state)`, so every site the WP1 helper routed (section "WP1", 2; current line numbers of `PassiveStepSolver` at this commit) states the head on its own state, in both directions:

| # | site | state the head is read on |
|---|---|---|
| 1 | `reopenable` :222 | the step's end states (the band `reopenBand(pa, pb)` on the end pressures) |
| 2 | carry offer :379 | the step's start graph |
| 3 | `demand` :1293 (pass loop :499) | the pass's converged states |
| 4 | `initialMassFlows` :982 | the graph the solve starts from |
| 5 | valve start head :1054 (`startPoint`) | the start graph |
| 6 | `initialMassFlow` :1097 and the balanced junction seed :845, :873, :882 | the start graph / the seed trial graph |
| 7 | `headLimitMassFlow` :1129 | the start graph |
| 8 | `closeDeadHeads` :1455 | the start graph |
| 9 | `illegalWithEitherDensity` :1515 | the start graph |
| 10 | `headDensities` :1892 | the pass seeds |
| 11 | `edgeRows` :2259 | the Newton's trial state `st[node]` |

Nothing else states a driving pressure: `grep` of `.pressure()` in the solver leaves the valve's own set-point tests (a vessel pressure, not a driving pressure), pressure scales and retained pressures; `GRAVITY` elsewhere in `src/main` books potential energy only. The head does not enter the WP2 availability mask or the throttle (they read `portPhaseShare`, a volume share), nor the state-change controller (`PassiveIntervalSolver.stateChange`, vessel pressure and mass): WP2's rules are unchanged.

**Jacobian (review 2.2, verified by reading, no entry added).** `buildSparsity` (:1970) declares every column of both end nodes in every edge row, and the structural-zero drop removes columns only from material rows (`columns[c].andNot(materialRows)`), never from an edge row. The block sweep `differentiateEntries` (:2355) re-decodes the perturbed node into `st[node]` and re-evaluates every incident `edgeRows` on it, so the head's derivative (with respect to the node's amount, phase-split, T and P columns) is written into entries that already exist; the flow and actuator columns re-evaluate `edgeRows` with the node states unchanged, where the head is constant, as it should be. The coloured fallback evaluates the whole residual and needs nothing either. **No structural entry was added and `differentiateEntries` is unchanged.**

**Bitwise where it must be.** A VAPOR or BULK end, a non-vessel node and a state with no condensed mass return the integer `0`, so `P + 0.0` is `P`: gas-only and all-BULK islands are the doubles they were (section 6, G0b/G1/G2).

### 3. Checks of the eleven sites (mutation runs, measured)

Each site is covered by a fixture that fails when that site alone omits the head (scratch mutations, `tools/phase-ports-probes/d9/src/mutations/*.patch`, logs `09-*`; applied to a copy of the tree, never committed):

| mutation (the head removed from one site) | `LevelHeadTest` result |
|---|---|
| `closeDeadHeads` | 3 of 8 fail: the drain is dead-headed at every step start and opened by the reopen retry every step (479 reopens at 0.1 s, 84 at 5 s, where the correct tree has 0); the generator held off by the head is opened at the start and moves the tank (`against-5.0`) |
| `reopenable` | 1 of 8 fails: the reopen retry opens the dead-headed generator line against the head (`against-5.0`: the tank moves by 7e-5 Pa) |
| `edgeRows` (residual only) | 7 of 8 fail: bottoms not level (407 Pa, 5860 Pa), the pump closes at the headspace, a bottled drain at a 5 s slice end (bottom 101430 Pa above the void with the drain shut), two nonconvergences |

### 4. Existing tests that moved, and why (classification)

The brief expected nothing to move because "code-built graphs are all BULK". That is not so since WP1/WP2: `PhasePortTest`, `PhasePortClosureTest` and `JunctionWaterTraceTest` build code graphs with phase ports. Measured on the harness science run before any test edit: 205 found, **4 failed**, all in `PhasePortClosureTest`, all the same assertion kind:

| test | assertion | before (WP2 + D10) | with the head | classification |
|---|---|---|---|---|
| `aVaporPortIsSqueezedShutByLiquidAtFiveSecondSlices` | the closed vessel rises to the generator's 300000 Pa (+-1) | 300000.00004 | headspace 290254.82 + head 9745.19 = 300000.01 | forced by D9: the vessel is fed through its LIQUID port, so its bottom, not its headspace, stands at the generator's pressure |
| `...AtTenthSecondSlices` | same | 300000.00085 | 290264.05 + 9735.95 = 300000.00 | same |
| `aDissolvingCapIntegratesAtFiveSecondSlices` | the liquid-full vessel stands at the generator's 1000000 Pa (+-1) | 1000000.21 | 993908.25 + 6091.75 = 1000000.00 | same (a liquid-full 1 m3 pentane vessel: 621 kg over 1 m2) |
| `...AtTenthSecondSlices` | same | 1000000.00000003 | 993908.27 + 6091.73 = 1000000.00 | same |

Re-baselined in place to `P + h = P_generator` with the same tolerance (the javadoc keeps the old values). This is a change of four existing assertions, recorded here and in `DECISION_LOG.md` (A19) for the owner; the alternative that leaves the assertions literal is to feed those two fixtures through a BULK end, which would change what they test (filling through the bottom port). Every other assertion of the three classes passed unchanged; their printed numbers moved where a LIQUID port sees liquid (`logs/07-*` WP2 tree against `logs/06-*`): the WP1 fixture-1 LIQUID line 0.1903 -> 0.1951 kg/s (water) and 0.2235 -> 0.2256 kg/s (pentane); the drained water tank closes at 0.9747 % (5 s, was 0.9934 %) and 0.9854 % (0.1 s, was 0.9901 %); the squeezed vent closes at slice 13 with 0.44 % gas at 162.8 kPa (5 s, was 0.19 % at 299.1 kPa) and at slice 631 (0.1 s, was 617); the vanishing-cap step ends at 882.1 kPa (was 888.1 kPa) with the vent's zero-capped flow reported VELOCITY_LIMITED instead of PASSIVE (the test asserts only "not CLOSED"); the squeeze at 5 s loses its two equation-gate rejections (8 -> 10 rejections, now all state change). The throttle and refusal fixtures print identical numbers. The gas-only `inflow`/`refusal` fixtures differ only where water has entered.

**What waits for WP5.** `PhysicalFluidTopology` still compiles every face to BULK (WP5 is the face-to-port compile), so the four physical-topology assertions of plan Appendix C.5 item 5 do **not** move yet and were not edited: `ElevatedBlockLineIslandTest` :216, :220, :229 (rising, rising filter, rising pump lines, fed through the tank's DOWN face) and `DeadHeadedLineIslandTest` :243 (was :234 in C.2; the raised generator's rest). They come due with WP5, re-baselined to `P_tank + g m_w H/V = P_generator - rho g LIFT`: about 7 kPa lower headspace by the level-head review's estimate (0.72 m of water for a tank charged at 1 atm). For scale, the D9 bitwise probe's rising line (the same line code-built with a LIQUID tank end, tank charged at 150 kPa, 0.574 m3 of water at rest) rests at 355309.09 Pa against 360918.31 Pa without the head: -5.61 kPa, the head of its water (`results/probe/out-d9/liquid-ports.txt`). The review's fixture 6 (face-to-port plus head on a compiled stack) is WP5's too. A note is in the plan's WP5 row.

### 5. Fixtures (`LevelHeadTest`, measured; Gradle run G1)

Every slice commits whole and closes the component ledger to 1e-12 and the energy ledger to 1e-10 (pump work included). Vessels are 1 m3 at 298.15 K, water under nitrogen; lines level unless stated.

| test | fixture | measured |
|---|---|---|
| `aTankAtTheVoidsPressureDrainsOnItsHeadAlone...` (5 s, 40 slices; 0.1 s, 1500) | 0.1 m3 of water, headspace 101275 Pa (50 Pa below the 1 atm void), vented through its VAPOR port from a 1 atm N2 generator, LIQUID drain 2 m 50 mm to the void | drains from the first slice on the head alone (950 Pa at the start; 13.56 kg in the first 5 s slice, 0.283 kg in the first 0.1 s slice); **0 reopens**; closes as WP2 closes a port, at the first step start below 1 %: slice 10 (50 s) at 0.99300 % (5 s) and slice 479 (47.9 s) at 0.99716 % (0.1 s); one transition; the closed vessel keeps its water to 3.4e-12 of the vessel volume. BULK control (no head): 3.3e-6 / 1.7e-6 kg out in total (the vent lifts the headspace to 1 atm, roundoff) |
| `aGeneratorBelowTheBottomPressureStaysDeadHeadedWithoutReopens` (5 s, 12; 0.1 s, 100) | closed tank, 0.3 m3 of water, headspace 100000 Pa, bottom 102867 Pa; water generator at 101325 Pa on its LIQUID port | dead-headed (CLOSED) every slice, flow exactly 0, the tank's pressure bitwise constant, **0 reopens** (`closeDeadHeads` and `reopenable` read the same bottom pressure) |
| `aDrainTheHeadLiftsAboveTheVoidInsideAStepIsReopenedAndNeverBottled` (5 s, 24; 0.1 s, 600) | closed tank, 0.3 m3 of water at 98 kPa (bottom 100.9 kPa, below the void), LIQUID drain to a 1 atm void, filled through a BULK end from a 200 kPa water generator (10 m, 10 mm) | drain closed until the bottom passes the void, opened inside slice 3 by 1 reopen (5 s) and at the start of slice 163 with no reopen (0.1 s); no slice ends bottled (drain shut with the bottom above the void by more than the band); steady state with the headspace below the void (98435.26 Pa) and the bottom above it (101332.43 Pa): only the head drains it |
| `twoTanksAtOnePressureEqualiseTheirBottomsMonotonically` (5 s, 60) | two closed tanks at 101325 Pa, 0.8 and 0.2 m3 of water, LIQUID to LIQUID 10 m 20 mm | bottom difference 5859.8 Pa at the start, monotone (0 increases, no sign change beyond -3.6e-6 Pa), 0.042 Pa after 20 slices, **-3.6e-6 Pa at rest** (Newton tolerance x scale = 1e-4 Pa); 9.73 kg moved (the cushions take most of the imbalance: 96862.28 / 102531.26 Pa at rest); flow exactly 0 from slice 30, states repeating. Certification itself is runtime-package code (`IslandCertificate`, `IslandCoordinator` package-private) and not reachable from this package, so stationarity is asserted instead. BULK control: never moves (flow 0) |
| `cushionsHeldApartStandAtALevelDifferenceOfDpOverRhoG` (5 s, 60) | A and B, 0.3 m3 of water each, LIQUID to LIQUID 2 m 50 mm; A held at 104325 Pa by a N2 generator on a BULK end, B closed at 101325 Pa | rest: P_A 104325.00001, P_B 103965.46, dP 359.54 Pa (< rho g H); level difference 0.0368098 m against dP/(rho g) 0.0368094 m (1e-5 relative); bottoms level to 8.9e-7 Pa. BULK control: headspaces equal (104325.00001 both) with 11.9 kg more water in B, a level difference nothing holds |
| `aPumpIntoABottomPortClosesTheLevelHeadEarlier` (5 s, 30) | water generator 1 atm, junction, pump 0.01 m3/s 300 kPa, N2 tank 1 atm; pump discharge on the tank's BULK or LIQUID end | both pumps CLOSED at the end; side-fed headspace 401324.09 Pa, bottom-fed 394085.62 Pa + head 7239.19 Pa = 401324.81 Pa: the bottom-fed tank closes 7.24 kPa (its water's head, 738 kg) earlier |
| `aFreshSolverReplaysASlowHeadDrainBitwise` (5 s, 12) | the drain fixture through 10 m of 10 mm line | 1.15 kg drained over 60 s (head 950 -> 939 Pa); a fresh solver resumed from the committed interval at slices 1, 4 and 8 reproduces every flow, mode, state pressure and temperature, inventory and boundary of the continuous run bit for bit |

Plan fixture 3's wording ("with the head off all the liquid moves until the source port closes") assumed B vented. Measured: a VAPOR vent on a tank filled through its LIQUID port fails the reconstruction's equation gate at every step size (1.008e-8, interval substep limit exhausted in the first or second 5 s slice), **on the WP2 + D10 tree as on this one** (`logs/08-vapor-vent-gate-classification.txt`, scratch `src/ManometerProbe.java`: `base LIQUID VAPOR VAPOR` fails at slice 3 without any head; a BULK vent integrates but carries B's water out). It is the WP2 open item "gate rejections with a VAPOR vent" (WP2 section 4, option 3), here severe enough to exhaust the substep limit. Not a D9 defect; the fixture closes B instead. Recommended: take it into WP6's gate investigation (WP2 option 3 (ii)) with this fixture as the reproduction.

### 6. Counters (review section 8, measured; no timing claim)

Gas-only guard: the 33 MIXED_GAS_STATIC/TRANSIENT/COST and LIQUID_JUNCTION lines of G1 are character-identical to `d10/logs/02-junction-lines-d10.txt` with ms/bytes/allocatedMB masked, so `MixedGasJunctionTransientTest`'s ledgers and Newton solve counts are unchanged (MIXED_GAS_COST 286 Newton solves). The WP1 bitwise probe extended with gas-only phase-port islands (G0b) is byte-identical.

Liquid fixtures (`LEVEL_HEAD_RUN` lines, `logs/06-*`; SolverDiagnostics over each run):

| fixture | cadence | accepted / rejected | rejections by reason | Newton solves | iterations per solve | Jacobian builds |
|---|---|---|---|---|---|---|
| drain | 5 s | 168 / 21 | 18 equation gate, 3 state change | 190 | 3.48 | 70 |
| drain | 0.1 s | 1500 / 0 | - | 1501 | 0.65 | 485 |
| against the head | 5 s / 0.1 s | 36 / 0, 100 / 0 | - | 36, 100 | 0 | 0 |
| reopen by the head | 5 s | 72 / 1 | 1 boundary reopen | 73 | 1.52 | 28 |
| reopen by the head | 0.1 s | 600 / 0 | - | 600 | 1.06 | 601 |
| equalisation | 5 s | 180 / 0 | - | 180 | 2.21 | 47 |
| manometer (head) / BULK control | 5 s | 182 / 3, 180 / 1 | 1 reopen + 2 equation gate; 1 reopen | 185, 181 | 0.88, 0.45 | 62, 36 |
| pump into bottom / side port | 5 s | 238 / 13, 238 / 13 | 13 state change each | 297, 297 | **5.29 / 5.60** | 159 / 146 |
| slow drain (replay) | 5 s | 48 / 12 | 12 equation gate | 60 | 1.68 | 16 |

- The one head-on/head-off pair with the same work (the pump into a bottom against a side port) has the same solve count and rejections and 5.5 % fewer iterations per solve with the head: no cost measured.
- No new rejection reason. The equation-gate rejections on the drain and the slow drain are the VAPOR-port class above (the vent's inflow into the draining tank): the same drain on the WP2 tree, the head emulated by a void 950 Pa lower, has 8 in 6 draining slices (`logs/08-drain-gate-classification.txt`). The reopens are the expected ones (a vent line at rest at the start; the head's reopen in the reopen fixture).
- Zero nonconvergence; reopen retries 0 in the drain fixture (`LEVEL_HEAD_REVIEW.md` section 8, item 4).

### 7. Gates

JDK OpenJDK 21.0.10 (container); `JAVA_TOOL_OPTIONS` as the environment sets it; one Gradle invocation at a time; no dev client. Logs `tools/phase-ports-probes/d9/logs/`.

| # | command (from `/home/user/CreateChemE`) | result |
|---|---|---|
| G0a | `REPO=/home/user/CreateChemE bash tools/cloud-science-harness/harness.sh all` (log `01-harness-all-d9.log`, 94 s) | science 213/213 (205 + 8), runtime 237/237 (227 + the 10 `ExtremeTopologyIslandTest` cases) with the 33 junction lines identical to its reference, adjacent 38/38, chain-100 0.000e+00 |
| G0b | WP1 bitwise probe extended (`src/BitwiseProbe.java`: chain-100, the 14 all-BULK scenarios, and 8 gas-only phase-port scenarios: a N2 generator into a LIQUID port at 5 s and 0.1 s, a VAPOR-to-LIQUID tank pair at 5 s and 0.1 s, a methane/nitrogen junction fed through VAPOR and LIQUID ports at 5 s and 0.1 s, a dead-headed rising LIQUID port, a dry LIQUID drain), on `0cf4ec9` (before the edit) and on this tree | `chain-100.json`, `scenarios.txt`, `gas-ports.txt` **byte-identical** (sha256 in `results/probe/sha256.txt`), `chain-100.json` identical to `src/test/resources/fluid/regression/chain-100.json`; `liquid-ports.txt` (a rising line and a drain whose LIQUID port sees water) differs, as intended |
| G1 | `./gradlew --no-configuration-cache --no-build-cache test --rerun --tests com.wormzjl.createcheme.science.fluid.* --tests com.wormzjl.createcheme.runtime.fluid.* --console=plain --continue` (log `02-gradle-fluid-suite-d9.log`, 70 s; the XML reports stayed in the session scratchpad, `d9/02-xml-d9/`) | **448/448** in 99 classes, 0 skipped: D10's 430 names plus the 8 of `LevelHeadTest` plus the 10 of `ExtremeTopologyIslandTest` (merged before this package; names `02-test-names-d9.txt`, no name of the 430 missing); the 33 junction lines (`02-junction-lines-d9.txt`) character-identical to `d10/logs/02-junction-lines-d10.txt` with ms/bytes/allocatedMB masked; MIXED_GAS_COST 286 Newton solves (555 ms, 135.1 MB, single run, no cost claim) |
| G2 | `./gradlew --no-configuration-cache fluidSolverRegression -PfluidRegressionMode=exact --console=plain` (log `03-...`) | **0.000e+00** on state/moles, temperature, phase fraction and flow; 3 accepted / 0 rejected, 4 Newton solves, 29 iterations |
| G3 | the 38 adjacent (WP1 G3 command; log `04-...`) | **38/38** in 7 classes |
| G4 | `./gradlew --no-configuration-cache compileFluidGameTestJava compileMcpCompatJava --console=plain`, then with `--rerun` (log `05-...`) | **BUILD SUCCESSFUL**, both tasks executed |

Section 11 records the re-run after the last (comment-only) edit.

### 8. Decisions recorded

`DECISION_LOG.md`, "D9 defaults": A17 (what `m_c` and `V` are; solids count without a liquid), A18 (no digest change), A19 (the four `PhasePortClosureTest` re-baselines).

### 9. Not run

GameTests and in-game scenarios (no runtime path changes; runtime graphs carry no phase port until WP5); the Windows lane; no timing claim.

### 10. Commits and material

Code commit `fdf3574` (`WIP phase-ports D9: level head at the bottom port (option B, H = 1 m)`); tools commit after it. Material: `tools/phase-ports-probes/d9/` (`src/BitwiseProbe.java` the extended probe, `src/DrainProbe.java` and `src/ManometerProbe.java` the gate-classification drivers, `src/mutations/` the three single-site mutation patches, `results/probe/` the probe outputs on both trees, `logs/` the gate logs and captures).

### 11. Re-run after the last edit

The javadoc of `PassiveNetwork.PhasePort` and `PassiveStepSolver.endPressure` was re-wrapped after G1-G4 (comment only). Re-run on the final tree: G1 (log `10-gradle-fluid-suite-final-d9.log`) 448/448 in 99 classes, the same test names, the 33 junction lines again identical to D10's (`10-junction-lines-final-d9.txt`); G2 (log `11-gradle-regression-exact-final-d9.log`) 0.000e+00.

## Vent gate defect (2026-09-26): investigation, no code change

- Author: Claude (Opus 5.5), same worktree and branch, base `fdc7571` (D9 plus its tools commit), clean tree. No tracked source changed: the booking the brief would have fixed is already exact, and every change that closes the defect is a solver-policy choice (section 5), so the options are for the owner. Material: `tools/phase-ports-probes/vent/` (instrumentation and prototype patches against `fdc7571`, logs numbered as cited below).
- The defect: the D9 open item "VAPOR vent failure" (D9 section 5; WP2 section 4 and option 3): two tanks joined LIQUID to LIQUID, A fed by a nitrogen generator, B vented through its VAPOR port (`d9/src/ManometerProbe.java 5 4 LIQUID 104325 101325 0.3 0.3 0.05 VAPOR VAPOR`).

### 1. Reproduction on `fdc7571` (measured)

- 5 s slices: slice 0 commits after 497 accepted and 503 rejected substeps (502 equation gate, 1 reopen); slice 1 fails, "Interval substep limit ... advanced=4.2256 of 5.0 s, accepted=509, rejected=515, last=... equation gate: 1.0083460016084983E-8" (`logs/01`), bit for bit the D9 capture.
- 0.1 s slices: 300 slices integrate, 300 accepted, 1 rejected (the vent's reopen at rest) (`logs/07`). The defect needs steps of order a second.

### 2. Which row fails, and why, by numbers (instrumented scratch copy, `src/instr-gate-probe.patch`, `logs/02`)

First failing step: dt 1.894 s, Newton converged to 3.689e-10 after 5 chord iterations. Layout of each tank block: local 0 N2 balance, 1 water balance, 2 energy, 3 volume, 4 water saturation `ln(p_w/Psat)`, 5 partial-pressure closure.

| row | Newton's residual at its point | gate at the reconstructed point |
|---|---|---|
| A local 3 (volume) | -3.8e-11 | **-3.132e-8** |
| A local 4 (saturation) | 1.8e-10 | **4.306e-8** (the gate's maximum) |
| A local 5 | 5.4e-12 | 1.3e-9 |
| B local 3, 4 | 1.9e-11, 1.4e-10 | -1.197e-8, 1.771e-8 |
| balance rows N2, water, energy of A and B | <= 1.8e-10 | <= 1.7e-11 |
| edge rows | 3.7e-10, 1.9e-10 | unchanged |

- **The booking is exact.** Vent edge (B's VAPOR port to the void): the Newton's target booking `dt q n_c/m` and the reconstruction's `dt |q| w_stream / M_c` are the same doubles, N2 0.36169952116860066 mol, water 0.011676887918495993 mol (difference 0; 2.8e-17 at the second failure); the LIQUID link books 504.76023509407260 mol of water in both. The reconstructed amounts equal the Newton's targets exactly: A's N2 29.716298498875040 mol, B's 27.112096349239750 mol (difference 0; at most 3.6e-15 mol in later failures). The plan's inference (3.3 item 2, "the reconstructed inventories equal the Newton's targets to its tolerance") is therefore wrong in its premise: they are equal bit for bit.
- **What is off by the tolerance is the Newton's own state.** Its amounts differ from its own targets: A's N2 by +1.276e-6 mol (+4.29e-8 relative), B's by +4.75e-7 mol (+1.75e-8); water by 5.6e-11 relative. The balance row states that difference over the node's total amount (`PhaseLayout.componentScales` = `amountScale` when the headspace is at least 1 % of the vessel): 15066 mol, of which 15036 water and 29.7 N2, so A's N2 row reads 8.2e-11, well inside the 1e-9 Newton tolerance.
- **The reconstruction restates that difference at fixed (T, P).** `ConservativeTransport.reconstruct` books the exact amounts and keeps the candidate's temperature, pressure and phase ratios (`repartition` -> `adoptingState`, no flash). The volume row then sees the corrected gas amount at the old pressure: 1.276e-6 mol x RT/P (0.02376 m3/mol) = 3.03e-8 of the 1 m3 vessel (measured -3.13e-8); the saturation row sees the water mole fraction move by the relative N2 change: 4.29e-8 (measured 4.31e-8).
- **Amplification.** Saturation row over N2 balance row = n_total / n_N2 = 507; volume row = that times the gas volume fraction 0.73 = 370. The 1e-8 gate thus asks the N2 balance row of such a vessel to close to about 2e-11, 50 times below the Newton's tolerance (the solver's own comment keeps "two orders of margin" to the gate, which holds only for amplification below 10).

### 3. Why the Newton stops there (chord iteration; `logs/04`, `logs/05`)

- A's N2 row per iteration at dt 0.947 s: -1.24e-5, -1.22e-7, -5.1e-10, +6.0e-10, +2.4e-10, +8.5e-11: it contracts by 0.35-0.4 per iteration while the norm reaches 1.5e-10, so the Newton exits with that row carrying a large share of its residual. The row is linear in (n_N2 of A, generator flow) with the coefficient `dt w / s`; the iterations ran on a preconditioner inherited from another step (iteration 0 has `refresh=false`: `WorkspaceKey` carries dt and a new dt forks the previous structure's factorization, `forkPreconditioner`), so that coefficient is stale.
- With a Jacobian built at the start of every solve (this step's dt and state, chord afterwards), the same row is 6.8e-16 after one iteration and 0 after two, and the manometer integrates at 5 s: 4 accepted / 0 rejected per slice after slice 0's reopen; with a fresh Jacobian at every iteration, likewise over 6 slices (`logs/05`).
- In the BULK-link layout the same row ends at 1.3e-13 (`logs/04`) and nothing fails (`logs/03`: LIQUID link + VAPOR vent 1017 failures in 2 slices; LIQUID + BULK vent 1; BULK + VAPOR 0; BULK + BULK 0). The phase ports change which chord mode converges slowest; they do not create the amplification.

### 4. Why it exhausts the substep limit, and that it is not phase-port specific

- Near the threshold the accepted substeps commit reconstructed states whose volume row sits just under the gate (e.g. accepted at dt 2.61e-3 with gate 9.38e-9, volume row -7.35e-9); the next step starts from that inconsistency and its gate hovers at 1.0e-8 at every dt (fail 1.0008e-8 at 5.22e-3 s, pass 9.38e-9 at 2.61e-3 s, fail 1.13e-8 at 1.04e-2 s): the controller alternates between three step sizes until the 1000-substep limit (`logs/03`, tail of the LIQUID/VAPOR file). That is the "1.008e-8 at every substep".
- All-BULK fixtures meet the same row class today and recover it by halving (instrumented runs, `logs/11`): `DeadHeadedLineIslandTest` and `FilterBlockLineIslandTest` fail the gate at the tank's local row 4 at 3.25e-7 and 3.62e-7 (N2 0.10-0.11 % of the tank's amounts, relative N2 error 3.2e-7 / 3.6e-7, gas 28 / 34 % of the volume, volume row -9.1e-8 / -1.2e-7), and `LevelHeadTest`'s BULK drain control at 0.1 s has one (section 5, A). The D9 drain (`d9/src/DrainProbe.java`, the head drain at 5 s: 18 gate rejections in 12 slices) is the same: tank rows 3/4, relative N2 error up to 5.7e-8 (`logs/09`).

Candidates ruled out: the stream composition the Newton used versus the one the reconstruction reads (the candidate is the Newton's last iterate; bookings identical, above); water vapour to the void or the vessels' water rows (water rows at the reconstructed point 0 and 1.3e-14; water booked identically); a stiff gas cap (0.7 m3 of nitrogen at 1 atm is not stiff; the factor is n_total/n_gas); the throttle or the D9 head on one side only (the WP2 + D10 tree fails without a head, D9 `logs/08`; the vent runs PASSIVE, never at its cap or throttle); the solid share or the energy booking (no solids; energy rows 5e-12 and 1.7e-11 at the reconstructed point).

### 5. Classification and options for the owner (not implemented)

A tolerance-level mismatch, but not the booking mismatch the plan inferred: the brief's forced fix (the reconstruction books what the Newton booked) already holds bit for bit. What closes it is a change to when the Newton stops, how it scales a minor component's balance, or how the reconstruction restates a state; each has its own blast radius, so it is the owner's choice.

- **A. Polish before refusing (recommended; taken by the owner as D13, see the section "D13").** When the FULL gate fails, re-solve once from the converged point on a fresh workspace (a Jacobian of this step's own dt and state) at the tolerance times 1e-2, reconstruct, re-check `boundaryAllowed`, the velocity constraint and `phaseCorrection`, and gate that point; refuse as today if any of that fails. Runs only where the gate has already refused, so **every step the gate accepts today is bitwise unchanged**. Prototype `src/vent-gate-polish-prototype.patch` (33 lines in `PassiveStepSolver`), measured: manometer at 5 s, 8 slices, 3-4 accepted / 0 rejected each (slice 0 one reopen), 13 firings, each one iteration, gate 2.2e-8..1.6e-7 -> 7.7e-14..3.0e-12 (`logs/06`); at 0.1 s byte-identical to HEAD, never fires; the BULK-vent control 1 gate rejection per slice -> 0 (`logs/08`); the D9 drain 18 -> 0 (`logs/09`). Cloud harness `all` on the prototype: science 213/213, runtime 237/237 with the 33 junction lines identical to the reference, adjacent 38/38, chain-100 0.000e+00 (`logs/10`); it fires 64 times across the suites (`LevelHeadTest` 40, `FilterBlockLineIslandTest` 18, `DeadHeadedLineIslandTest` 4, `ElevatedBlockLineIslandTest` 2), every assertion holds, and the printed `LEVEL_HEAD_RUN` counters move (`logs/12`): drain 5 s 168/21 -> 152/3 (Newton solves 190 -> 159, iterations 661 -> 379, Jacobian builds 70 -> 54), manometer 182/3 -> 181/1, replay 48/12 -> 36/0, the drain's 0.1 s BULK control output 1.67e-6 -> 8.8e-7 kg. Cost where it fires: one Jacobian build, one or two iterations and one reconstruction, against a halving and its re-solves today.
- **B. Do not fork a preconditioner across dt.** Build the Jacobian at every new dt. Fixes the manometer (measured with a Jacobian at every solve start, section 3); not bitwise wherever the step size changes (*inferred*), and it gives up what the fork saves.
- **C. Per-component balance scales.** State every component's balance over its own amount, as `resolveTraces` already does below a 1 % headspace. Tightens the Newton on minor gases everywhere; not bitwise; the solver's comment records a 1e-11 stall on caloric cancellation near liquid-full water (*inferred risk*).
- **D. Restate (T, P) in the reconstruction.** A UV flash (or a local T, P, split solve) at the booked amounts, energy and volume, so the gate sees only the transport. Not bitwise; one flash per node per step; reverses the "reconstruction does not flash" design (*inferred*).
- **E. Accept.** Recovered by halving in most layouts, but the manometer shows it can lock the controller and fail the interval at 5 s, which a compiled vented tank (WP5) would meet in game.

If A is taken: port the prototype without the trace line, add the regression test the brief describes (a tank filled through its LIQUID port with a VAPOR vent, 5 s and 0.1 s, no "equation gate" key in the rejection reasons of any slice; `SolverDiagnostics` counts verification residuals but has no gate-rejection counter, so the reasons map is the assertion), switch `LevelHeadTest`'s manometer to vent B through its VAPOR port as D9 section 5 intended, and run the usual gates; on this evidence no existing assertion changes.

### 6. Material

`tools/phase-ports-probes/vent/`: `src/instr-gate-probe.patch` (the instrumented gate and Newton, against `fdc7571`; never commit), `src/vent-gate-polish-prototype.patch` (option A, against `fdc7571`), `src/run-probe.sh` (runs the d9 probes against a compiled tree), `logs/01`-`12` as cited. No Gradle run: no tracked source changed.

## D11: priority streams at ports (replaces the "outlet phase absent" closure) (2026-09-26)

- Author: Claude (Opus 5.5), same worktree and branch. Base: `0795215` (the vent investigation's tools commit; the owner's decision-log commit `99f5312`, D13, landed on the branch during the work and touches only `DECISION_LOG.md`). Code commit: `548a9bf` (`WIP phase-ports D11: priority streams at ports (replaces the absent-phase closure)`), on `99f5312`; tools commit after it. Not merged, not pushed.
- Decision: D11 (owner, 2026-09-26): every non-BULK port draws its vessel's phases by priority, each up to what the vessel held of it at the step start, the next phase with the rest ("priority stream"); no port closes for want of a phase. It supersedes WP2 (availability mask, band, port refusal, reopen allowance, throttle as a landing device), A9 (the absent stream), A2 (no decant) and plan 3.4's "liquid-full vessels may increase in pressure".
- Not touched: the vent-gate defect (D13, another package), the cold-start path beyond one mechanical signature change listed in section 11, the D9 head, WP1's reconstruction booking scheme (extended, not replaced), inflow through ports (D3), the structural-zero exemption, all-BULK islands (bitwise, section 8), D10.

### 1. What changed, per file

| file | change |
|---|---|
| `science/fluid/network/PhaseDraw.java` | new, package-private: the priority `order(model, state, port)` (LIQUID: heavier liquid, lighter liquid, gas; VAPOR: gas, lighter, heavier), `leading(...)` (the first phase of the order a state holds: the stream at a vanishing flow), `held(state)`, and `Segment` (order, per-phase capacity, pinned phases, marginal phase) with `decide`, `code`, `samePhases`, `pinnedTotal`, `marginalAt`, `rates`. |
| `science/fluid/thermo/FluidThermodynamics.java` | phase ids `GAS, OIL, WATER`; `holdsPhase`, `phaseSolidShare`, `liquidPhaseDensity`, `heavierLiquid`, `phaseMass`, `phaseVolume`, `phaseMoles`, `phaseSolidMoments`, `phaseSpecificEnthalpy`, `phaseVelocityLimit`, `phaseViscosity`, `phaseCarrierViscosity`, `phaseStream` (null for a phase not held; `vaporStream` for the gas). The hydrocarbon-liquid and free-water streams partition WP1's condensed stream; with one liquid held its stream is the condensed stream to the bit (share exactly 1.0). `PhaseStream` javadoc restated. |
| `science/fluid/network/PassiveStepSolver.java` | Removed: `PHASE_PORT_OPEN`, `PHASE_PORT_RESERVE`, `portPhaseShare`, `portAvailable`, `portBit`, `closedPorts`, the four-argument `boundaryAllowed`, `Equations.closedPorts`, `Equations.throttles` and its builder, the throttle term of `edgeRows`' cap, the throttle clamp of `buildInitial`, `carrierViscosity(state, port)`, `Transport.port`/`absent`. `WorkspaceKey` loses the mask; new `PassKey` (structure plus segment codes) is the cycle key. `Equations` gains `drawsPhase` (was `drawsVapor`/`drawsLiquid`), `capacities`, `seedHeld`, `startPhases`, `segments` and `startInsideCaps`, `capacities()`, `decideSegments`, `segmentCodes`, `segmentsMoved`, `drainedSeeds`, `drawn`, `capLimit` (two overloads), `drawnCarrierViscosity`, `draws`, `transportAt`; `Transport` carries `phases[3]`; `transport`, `nodeAccumulate`, `edgeRows` read `drawn`/`capLimit`. `solve`'s pass loop: pump and valve tests read the drawn stream, the segment re-decision, the drained-seed fallback after a failed Newton, the velocity check, `reconstruct` and `PipeTransfer.sample` with the draws, the accepted-mode clamp; new `velocityCap` (also used by `checkApproximation`). The per-end static helpers (`endDensity`, `endVelocityLimit`, `endViscosity`, `solidShare`, `suctionMassFlow`, `endMoles`, `endMass`, `endSpecificEnthalpy`) read the leading phase. The mask parameter left `reopenable`, `closeDeadHeads`, `closeIllegalStarts`, `reachableComponents`, `initialPhaseSeeds`, `seedBoundaryJunctions`, `balancedBoundarySeed`, `startPoint` (signature and `boundaryAllowed` arguments only). Javadoc of `boundaryAllowed`, `portHead`, `massFlowLimit` and two column comments restated. |
| `science/fluid/network/ConservativeTransport.java` | `reconstruct(..., frozenJunctions, draws)` (the old overloads pass no draws = every outflow bulk); `drawsPhase` removed; `drawnPhases`, `weight`, `at`, `drawnSolidShare`, `phaseFractions`, `streamFractions` read a six-entry draw (end-state rates, start-state rates); the unbacked-trace rule (section 6); the pipe loop's energy, solids, filter capture and pump work, and `pinnedFlows`, read the draw. |
| `science/fluid/network/PipeTransfer.java` | `sample(model, graph, states, rates, duration, draws)`: each drawn phase sampled on its own state (end-state phases on the states, pinned ones on the graph's start state); the bulk branch unchanged. |
| `science/fluid/network/SolidEventIntegrator.java` | `failed`: a phase port is checked on its leading phase: `GAS`, `ORGANIC_LIQUID` or `WATER` outlet, the drawn mass per whole-liquid content `m_phase / share`; a vessel holding none draws as a bulk end. |
| `science/fluid/transport/SolidMobility.java`, `science/fluid/network/PassiveNetwork.java` | javadoc only (the outlets now requested; `PhasePort` under D11). |
| `src/test/.../PhasePortClosureTest.java` | deleted (WP2's 15 tests). |
| `src/test/.../PhasePortPriorityTest.java` | new, 10 tests (section 7). |
| `src/test/.../LevelHeadTest.java` | the drain fixture restated (section 9); `OPEN`/`RESERVE` removed, `liquidShare` computed locally; a per-slice print. |

### 2. The rule, as implemented

- **Order.** On the step's start state (a constant of the solve): LIQUID `[heavier, lighter, GAS]`, VAPOR `[GAS, lighter, heavier]`, the heavier liquid by the liquids' own densities (`liquidPhaseDensity`, no solids), water when not both are held (the order among absent phases does not matter: they have no capacity). A BULK end draws the bulk.
- **Capacity.** Per vessel and phase, `c_phi = m_phi,start / (N dt)`: `m_phi,start` the phase stream's mass on the start state, its solid share included; `N` the vessel's phase-port ends whose connection may carry outflow from it by the static rule (not blocked, not into a generator; A21); `dt` the solve's step (WP2's throttle's `dt`). A rate solve (no step): every capacity infinite.
- **Priority stream.** For an outflow `q` through a phase-port end: walk the order over the phases with capacity; a phase whose capacity the remaining flow exceeds is **pinned** at `c_phi` (provided a later phase of the order the seed holds can take the rest); the first phase the seed holds that covers the remainder is **marginal**, drawing `q - sum c_pinned`. The last phase the vessel holds is never capped (A22): a port's total flow is limited by the hydraulics and the velocity cap only. So a slowly fed drain draws its water at exactly the feed and gas with the rest (section 7, fixture 2), and a vent on a liquid-full tank overflows the lighter liquid (fixtures 3, 4).
- **Pinned at the start state, marginal at the end state (A23).** A pinned phase is drawn at the start state's composition, enthalpy and density (`startPhases`): over the step it takes exactly its share of what the vessel held of it, which exists whatever the end state holds. A marginal phase is drawn at the end state, the implicit form of WP1's streams; with nothing pinned the port's stream is exactly its phase stream (so every single-phase draw is WP1's). Measured need: with the pinned phase drawn at the end state, a step that draws a phase to zero ends on that phase's boundary, where its end-state composition is undefined; the capacity fixture's pass stalled, the phase correction dropped the phase, the next pass (not drawing it) found it again: an active-set cycle at every step size.
- **Mixture.** The stream is the mass-flow-weighted mixture of its phase streams: composition, solids and specific enthalpy mass-weighted, density the no-slip mixture's (`q / sum(r_phi / rho_phi)`, the mass-weighted specific volume), viscosity the phases' volume-flow average (the solver's bulk rule), velocity limit the smallest of the segment's phases (A24). In the Newton it is a `Transport` of rates (`mass = |q|`, every extensive field a rate), so every donor term reads it unchanged. Below the pinned total (an iterate the segment does not describe) the pinned phases take the flow in proportion to their capacities, continuous at the pinned total and at zero flow.
- **Velocity cap of a mixture (A24).** A volume flow `A v_min`: `limit = C + rho_m (A v_min - sum c/rho)`, or, where the pinned phases alone exceed it, `C A v_min / sum c/rho` (continuous). BULK and single-phase ends keep `rho A v` to the bit.
- **A vessel holding none of the three phases** draws the bulk (A31, replacing A9's zero-velocity bulk). Static readers (columns, sign tests, start flows, the mobility check) read the **leading** phase, the priority stream at a vanishing flow (A28).

### 3. The per-pass segment

- **Frozen per pass** (`Equations.segments`), decided in the constructor from the pass's start flows (`initialPoint`) and seeds, like `junctionDonorFirst` and `headDensities`: each Newton residual is smooth in its unknowns.
- **Re-decided by the pass loop** (`segmentsMoved`): after a converged pass whose modes stand, the segments the converged flows land in are compared with the frozen ones; a difference is one more active-set change (seeds = converged states, next pass from these flows). The segments are in the **cycle key** (`PassKey`) but not in the **workspace key**: a segment changes values, not structure (a node with a phase port keeps every column entry whatever it draws), and keeping the workspace across a re-decision keeps the preconditioner (the churn `headDensities`' comment measured for a key component that changes values only).
- **Start inside the mixture's cap (A26)** (`startInsideCaps`): a start flow beyond the segment's velocity cap is moved to it and the segments are decided again (at most three rounds). Measured need: a 5 s step of a 2 MPa tank with 2 % water started at 137.7 kg/s (the water-only start estimate) against a mixture cap of 8.4 kg/s: the Newton stalled. BULK ends and ends drawing the phase the start estimate read keep their flow (it already starts inside that cap, to the bit).
- **Drained seeds (A27)** (`drainedSeeds`, only after a failed Newton whose phase correction changes nothing): a vessel whose pinned segments draw a phase's whole start content (`sum c dt >= m_start`) is reseeded with its seed's amounts less that content, flashed at the seed's T and P, if that changes its phase regime. Measured need: the last 0.0047 kg of nitrogen vented from a tank filled with water (nitrogen does not dissolve in free water): the pass whose seed keeps the gas has no root, the failed iterate still holds gas, and halving the step doubles the capacity so the pinned draw is still the whole gas: the step was refused at every size (overflow fixture, slice 13, substep limit). With the fallback the next pass (liquid-full) converges and the vent overflows water. Bitwise wherever a pass converges today.

### 4. Removals (D11 list)

The availability mask and its readers, the 1 %/0.5 % band and constants, the port refusal in `boundaryAllowed`, its reopen allowance in `reopenable` (the reopen test now reads the static rule only), the throttle in `edgeRows` and the start clamp to it, A9's absent stream; `INLET_EMPTY` was never coded. `closeDeadHeads`, `closeIllegalStarts`, `reachableComponents`, the start point's junction propagation, the approximation probe, `initialPhaseSeeds` and the balanced junction seed read the static rule.

### 5. What is kept

The D9 head (`portHead`, condensed mass at LIQUID ports; its javadoc now says it does not enter the capacities); the reconstruction's frozen-fraction booking on pinned flows, whose fractions are now the draw's (end-state fractions frozen from the converged candidate, pinned ones from the start state); inflow undistinguished (D3); the structural-zero exemption for nodes with ports; WP1's all-BULK path bitwise; D10.

### 6. A WP1 defect D11 exposed: unbacked traces in a frozen stream (fixed)

- *Measured.* The D9 bitwise probe's "mixed gas junction with LIQUID ports" (a methane tank's VAPOR port and a nitrogen tank's LIQUID port into a junction, drained to a void) failed every interval on the D11 tree: "Substep refinement exhausted: Negative transport reconstruction". The WP1-D9 tree fails the same island with the nitrogen tank on a BULK or a VAPOR end (`logs/07-junction-port-probe-head-vs-d11.txt`: VAPOR-BULK and VAPOR-VAPOR fail on HEAD at 5 s and 0.1 s); it passed only because WP2's mask shut the gas-only tank's LIQUID port, isolating the nitrogen (flow 0.0).
- *Mechanism (instrumented copy).* Node 0 (the methane tank) has no nitrogen in its inventory, but its Newton seed carries the 1e-12 entry trace of every reachable component and the candidate keeps 5.2e-25 mol; the frozen stream booked it out of an inventory of zero: `w = -5.7e-28`.
- *Fix (forced by conservation).* A donor that neither holds a component nor receives anything in the step cannot deliver it: its frozen stream books none of it (`ConservativeTransport`, "unbacked traces"; A30). The implicit bulk booking already lands such a component at exactly zero. Donor and receiver book the same fractions, so the ledger stays exact. The probe then integrates every combination on the D11 tree (`logs/07`).

### 7. Fixtures (`PhasePortPriorityTest`, measured; Gradle run G1, identical to the harness run)

Every slice commits whole and closes the component ledger (worst 2.5e-14 relative) and the energy ledger (worst 1.1e-13).

| test | fixture | measured |
|---|---|---|
| (1) `aBottomPortDrainsItsWaterToZeroThenVentsTheGas...` 5 s x 40, 0.1 s x 2000 | 0.1 m3 water under N2 at 120 kPa, LIQUID port 10 m 25 mm to a 1 atm void | water only until the gas breaks through in slice 26 (135 s; 0.1 s: slice 1302, 130.3 s), in one step: that slice draws the last 0.350 kg of water (0.1 s: 0.053 kg) at its capacity and 0.054 kg of gas; the tank then holds only the condensate of its expanding, cooling headspace (share 1.5e-7, then less), drawn in traces (5.0e-4 kg in all; 0.1 s: 1.7e-3 kg), water exactly 0 at slice 30 (0.1 s: 1406); the port vents the headspace to 101325.00 Pa; never CLOSED while the tank stands above the void, 0 reopens. Rejections: 7 state change (5 s); 2 phase-correction cycles at the saturation boundary (0.1 s, slices 1372 and 1406, recovered by halving) |
| (2) `aSlowlyFedDrainBreaksThroughSteadily...` 5 s x 60, 0.1 s x 1500 | N2 tank 150 kPa with 2 L water, headspace fed 99.9 %-humid N2 by a 150 kPa generator (BULK), water injected at 0.002 kg/s, LIQUID port 10 m 10 mm to 1 atm | after settling, water and gas leave together in every slice; liquid per slice 0.0099999 kg against 0.01 fed (worst 1.05e-5 relative; 0.1 s: 9.3e-6); the port's liquid fraction 0.296519 against feed/total 0.296522; slice-to-slice variation of the liquid 3.9e-8 (0.1 s: 1.2e-9); tank at 298.147 K, 149997.54 Pa. With dry nitrogen the tank cools towards a wet-bulb temperature (298 to 292.6 K in 300 s) and never rests, hence the humid supply |
| (3) `aTopVentOverflowsTheLiquidOfATankFilledFull...` 5 s x 40, 0.1 s x 900 | 0.8 m3 water under N2 at 1 atm, 300 kPa water generator into the LIQUID port, VAPOR vent 10 m 25 mm to 1 atm | the vent carries gas until slice 13 (0.1 s: 663), where it vents the last 0.0047 kg of gas with 7.83 kg of water, then overflows water; the tank ends liquid-full at 195782.75 Pa (+ head 9768.39 Pa), its bottom never above the generator; at rest the overflow equals the feed (10.76015 against 10.76012 kg per slice, 2.4e-6). Rejections recovered: 12 state change, 2 "hydrocarbon vapour partial pressure below the numerical floor", 1 negative reconstruction, 1 reopen (5 s) |
| (4) `pentaneOverWaterIsDrawnInEachPortsPriorityOrder` 5 s | bottom: 0.05 + 0.05 m3 pentane over water under N2 at 120 kPa, LIQUID port to 1 atm; top: 0.3 + 0.3 m3 at 1 atm, 300 kPa water generator into the LIQUID port, VAPOR vent | bottom: water (21.56 kg) until slice 4, pentane (11.53 kg) until slice 7, then gas; after pentane began, water only as condensate (at most 6.7e-4 kg per slice). Top: gas (1.61 kg) until slice 43, pentane (101.2 kg) until slice 56, then water; the tank never above the generator |
| (5) `aGasOnlyTanksBottomPortVentsGasFromTheFirstStep` | N2 at 200 kPa, LIQUID port 10 m 20 mm to 1 atm | 5 s: 0.255 kg of gas in the first slice (5 steps, 0 rejected); 0.1 s: 5.9 g in one step; nothing but gas; never CLOSED |
| (6) `closedPhaseLinesOnAThreePhaseVesselLeaveTheBulkLineBitwise` | pentane + water + N2 vessel, VAPOR and LIQUID lines closed both ways, BULK line open, 5 x 1 s steps | every double equal to the all-BULK graph's |
| (7) `twoBottomPortsShareEachPhasesCapacityEqually` | N2 tank 200 kPa with 0.5 L water, one or two LIQUID lines 10 m 10 mm, one 5 s step | one line: water 0.0935724 kg/s = `m_w/dt` to the printed digit (flow 0.09428, gas 7.0e-4); two lines: 0.0467862 kg/s each = `m_w/(2 dt)` (flow 0.04846, gas 1.68e-3 each); the tank ends with 9.6e-5 / 3.7e-4 kg of free water, the condensate of its expanded headspace (it held 0.023 kg as vapour) |

Bitwise (6) also, by the D9 probe: see section 8.

### 8. Bitwise checks

- **D9 bitwise probe** (`tools/phase-ports-probes/d9/src/BitwiseProbe.java`, run on HEAD `0795215` before editing and on the final tree; outputs and sums in `d11/results/probe/`): `chain-100.json` and `scenarios.txt` (the 14 all-BULK scenarios) **byte-identical**, `chain-100.json` identical to `src/test/resources/fluid/regression/chain-100.json`. `gas-ports.txt`: 3 of 8 scenarios identical (the N2 generator into a LIQUID port at 5 s and 0.1 s, the dead-headed rising LIQUID port: no gas-only tank's LIQUID port has outflow there); the other 5 are exactly those in which WP2's mask shut a gas-only tank's LIQUID port and D11 makes it vent gas (fixture 5): the tank pair (B's LIQUID port to the void: 0 -> 0.00425 kg/s), the methane/nitrogen junction at 5 s and 0.1 s (the nitrogen tank now takes part; first interval identical, then the junction's flows change), the dry LIQUID drain (0 -> 0.0299 kg/s). `liquid-ports.txt`: the water drain identical; the rising water line differs from its first interval at 1e-12 relative (its tank starts gas-only, and its LIQUID end's column now reads the gas stream's density where A8 read the bulk's; final pressure 355309.0899989 Pa either way). Per-scenario table `results/probe/scenario-comparison.txt`, flows `differing-gas-ports.txt`.
- **Closed lines** (PhasePortTest's WP1 test, unchanged and green, plus fixture 6 on a three-phase vessel): a closed phase line draws nothing, decides no segment and costs its node no structural zero.
- **Gas-only guard:** the 33 MIXED_GAS/LIQUID_JUNCTION lines character-identical to D10's (G1).

### 9. Restated assertions (LevelHeadTest)

Only the drain fixture `drainOnTheHead` (both cadences) asserted WP2's landing; every level-head physics assertion is kept.

| old assertion (D9 tree) | old value | new assertion | new value |
|---|---|---|---|
| open slices (water share >= 1 % at the slice start) drain; closed ones carry exactly 0 with mode CLOSED | closed from slice 10 (5 s) / 479 (0.1 s) | every slice whose bottom stands above the void (by the reopen band) drains water and is not CLOSED | drains every such slice |
| the port closes (`closedAt > 0`) with exactly one open-to-closed transition | 1 transition | the water falls below 1e-6 of the vessel (`dryAt > 0`) and stays there | slice 16, 85 s (5 s) / slice 771, 77.2 s (0.1 s); final water share 0.0 |
| landed water share in [0.5 % - drift, 1 %) | 0.99300 % (5 s) / 0.99716 % (0.1 s) | after `dryAt`, the drain passes no more than the trace of water the tank held at the slice start, plus 1e-6 kg/s of condensate | holds; last slice 2.2e-12 kg (5 s), 0 (0.1 s) |
| the closed vessel keeps its water (final share = landed +- 1e-5) | kept | the drained tank stays below 1e-6 | holds |
| kept: drains in the first slice; 0 reopens; BULK control < 1e-4 kg | 13.56 kg / 0.283 kg; 0; 3.3e-6 / 1.7e-6 kg | unchanged | 13.556 kg / 0.2832 kg; 0; 3.28e-6 / 1.67e-6 kg |

Counters of the restated drain (the drain now goes on to the trace, so the steps near the end are the controller's): 5 s 213 accepted / 34 rejected (9 state change, 11 equation gate, 6 Newton limit, 5 line-search stall, 3 phase-correction cycle; D9: 168 / 21), 0.1 s 1500 / 0. The other seven `LevelHeadTest` cases print the numbers they printed on D9 (manometer, pump, equalisation, reopen, dead-headed, replay). No other existing assertion changed; `PhasePortTest` (7) and `JunctionWaterTraceTest` (2) pass unchanged, their printed numbers equal to D9's to the printed digit except the solids fixture's filter capture (9.92631678707979 -> 9.926316787079783 kg, carrier-viscosity roundoff).

### 10. Gates

JDK OpenJDK 21.0.10 (container); `JAVA_TOOL_OPTIONS` as set; one Gradle invocation at a time; no dev client. Logs `tools/phase-ports-probes/d11/logs/`.

| # | command (from `/home/user/CreateChemE`) | result |
|---|---|---|
| G0a | `REPO=/home/user/CreateChemE bash tools/cloud-science-harness/harness.sh all` (log `01-harness-all-d11.log`, 94 s) | science 208/208 (213 - 15 + 10), runtime 237/237 with the 33 junction lines identical to its reference, adjacent 38/38, chain-100 0.000e+00. No exclusion-list change: `PhasePortClosureTest` was never excluded and `PhasePortPriorityTest` needs no Minecraft class |
| G0b | D9 bitwise probe, HEAD and final tree (section 8) | chain-100 and all-BULK scenarios byte-identical; gas-port scenarios as listed |
| G1 | `./gradlew --no-configuration-cache --no-build-cache test --rerun --tests com.wormzjl.createcheme.science.fluid.* --tests com.wormzjl.createcheme.runtime.fluid.* --console=plain --continue` (log `02-...`, 82 s) | **443/443** in 99 classes, 0 skipped: D9's 448 less the 15 `PhasePortClosureTest` plus the 10 `PhasePortPriorityTest` (names `02-test-names-d11.txt`); the 33 junction lines (`02-junction-lines-d11.txt`) identical to `d10/logs/02-junction-lines-d10.txt` with ms/bytes/allocatedMB masked; MIXED_GAS_COST 286 Newton solves (653 ms, 135.0 MB, single run, no cost claim) |
| G2 | `./gradlew --no-configuration-cache fluidSolverRegression -PfluidRegressionMode=exact --console=plain` (log `03-...`) | **0.000e+00** on state/moles, temperature, phase fraction and flow; 3 accepted / 0 rejected, 4 Newton solves, 29 iterations |
| G3 | the 38 adjacent (WP1 G3 command; log `04-...`) | **38/38** in 7 classes |
| G4 | `./gradlew --no-configuration-cache compileFluidGameTestJava compileMcpCompatJava --console=plain`, then with `--rerun` (log `05-...`) | **BUILD SUCCESSFUL**, both tasks executed |

### 11. Methods touched (for the merge with the D12 cold-start work)

- `PassiveStepSolver`: `reopenable` (mask removed), `solve` (mask removed; cycle key; pass loop as section 1), `WorkspaceKey`, `PassKey` (new), `checkApproximation`, `reachableComponents` (both), `initialPhaseSeeds` (signature and one `boundaryAllowed` call), `seedBoundaryJunctions`, `balancedBoundarySeed`, `startPoint` (signature and one `boundaryAllowed` call), `velocityCap` (new), `massFlowLimit` (javadoc), `portHead` (javadoc), `endDensity`, `endVelocityLimit`, `endViscosity`, `solidShare`, `suctionMassFlow`, `endMoles`, `endMass`, `endSpecificEnthalpy`, `closeDeadHeads` (signature and two calls), **`closeIllegalStarts` (signature: the mask parameter; its `startPoint` and `boundaryAllowed` calls lose the mask argument; nothing else)**, `illegalWithEitherDensity` (a comment), `boundaryAllowed` (javadoc; the four-argument overload removed), `carrierViscosity(state, port)` (removed), and in `Equations`: the fields and constructor, `startInsideCaps`, `capacities`, `decideSegments`, `segmentCodes`, `segmentsMoved`, `drainedSeeds`, `drawn`, `capLimit`, `drawnCarrierViscosity`, `draws`, `transportAt` (all new), `buildSparsity` (the exemption's flag), `buildInitial` (throttle clamp removed), `Transport`, `transport`, `nodeAccumulate`, `edgeRows`. **Not touched:** `initialMassFlows`, `initialMassFlow`, `headLimitMassFlow`, the interval solver's cold rate seed, `PassiveIntervalSolver`. `initialMassFlows` and `initialMassFlow` call `endDensity`/`endViscosity`/`massFlowLimit`, which now read the leading phase (for a BULK end the same doubles).
- `ConservativeTransport`: `reconstruct` overloads, `reconstruct0`, `pinnedFlows`, the draw helpers. `PipeTransfer.sample`. `SolidEventIntegrator.failed`. `FluidThermodynamics` (additions only).

### 12. Decisions recorded, open items

`DECISION_LOG.md`, "D11 defaults": A20-A32. Open items for the owner:

1. **Adiabatic humid venting below the water domain (base, exposed by D11).** A humid tank vented from 180 kPa or more to 1 atm cools below 273.16 K (the vessel is adiabatic) and every step is refused on `thermo-domain: Water temperature < 273.16 K`; the WP1-D9 tree fails the same with all-BULK ends (`logs/08-humid-vent-probe-head-vs-d11.txt`: 200 and 180 kPa fail at 5 s and 0.1 s, 130 kPa passes at 277.7 K). Under WP2 a drained tank closed its port at 1 % and never vented; under D11 it does (the D11 200 kPa drain fails at 60 s, `logs/09`). Fixtures 1 and 4 run at 120 kPa. Options: (a) accept (a player's tank venting a humid gas from above about 1.35 bar holds); (b) vessel wall heat exchange with an ambient; (c) a frozen-water (ice) region in the water properties; (d) clamp the water property evaluation below the triple point. Not decided by D11.
2. **Recovered rejections at phase boundaries.** A pinned draw that removes the last of a phase leaves the end state on that phase's boundary; halving recovers the phase-correction cycles (fixture 1 at 0.1 s: 2 in 2002 steps; the head drain at 5 s: 3) and the Newton limits (fed drain at 5 s: 10 in 212, all in its first three slices, where the initial water breaks through; none at rest). No slice was held. A per-step hysteresis on the phase correction is the lever if in-game cost shows.
3. **Condensate after a phase is drawn to zero** is physical (the headspace expands and cools): it is drawn in traces by the next steps (fixtures 1, 4). Tests treat 1e-6 of the vessel as "gone".
4. **The mobility check reads the leading phase**, not the step's mixture (its `StageGuard` sees states and flows, no draw); a liquid pinned beside gas is checked at the liquid's velocity for the whole flow (conservative for blockage, optimistic for deposition). Carrying the draws into the guard is the option.
5. **Unbacked traces** (section 6) are dropped only for a donor that receives nothing in the step; a donor that also receives, with a seeded trace its inflows do not bring, could still overdraw it (none measured). Options: drop per component when no inflow could carry it (a reachability test at booking), or cap each frozen fraction at what the donor can deliver.
6. **WP3/WP4.** `INLET_EMPTY` is gone (D11); a pump fed from a phase port reads the priority stream's density in its target and head rows, so WP3's `INLET_WRONG_PHASE` decision (D1) reads the leading phase or the draw.
7. **Vent gate (D13).** The equation-gate rejections still present (head drain 5 s: 11; replay: 12; manometer: 2) are the vent-gate class; D13's polish is another package. *Closed by the D13 merge: 0 in every suite (section "D11+D12+D13 merge", 2).*
8. **WP1 condensed-stream builders** (`liquidStream`, `liquidCarrierViscosity`, and the parts only it uses) are no longer read by the solver; kept for WP6's cleanup.

### 13. Material

`tools/phase-ports-probes/d11/`: `src/` (`JunctionPortProbe`, `ColdVentProbe`, `HumidVentProbe` the classification probes; `DrainTraceProbe`, `OverflowTraceProbe` pass tracers needing `pass-trace.patch`; `compile-main.sh`), `results/probe/` (the D9 bitwise probe on HEAD and on D11, sums, comparisons), `logs/` (gates 01-05, fixture lines 06, probes 07-09).

## D12: one-way generators from the cold start (2026-09-26)

- Author: Claude (Opus 5.5), worktree `wt-coldstart` (session scratchpad), branch `claude/cold-start-generators-wip`. Base: `7051391` (D9 `fdf3574`, its tools commit, the decision-log commit with D11/D12). Code commit: `418ca7e` (`WIP phase-ports D12: one-way generators from the cold start`); not merged, not pushed.
- Decision: D12 (owner, 2026-09-26): a generator only pushes when its pressure allows and never receives, from the cold start's first pass; fix order O2, then O1, O3-O5 only as options (`EXTREME_TOPOLOGY_TESTS.md` section 6). D11 is not implemented here.
- **Gates: the Minecraft-free harness only. The Gradle gates were not run in this worktree (another agent held the single Gradle lane) and must be run after the merge** (the fluid suites with the junction lines, the exact regression, the 38 adjacent, GameTest and mcpCompat compile).

### 1. The mechanism, confirmed by numbers

`EXTREME_TOPOLOGY_TESTS.md` section 5 read the defect from the solver. Measured here with scratch drivers (`tools/phase-ports-probes/d12/src/`):

- **The cold rate seed fails first.** `PassiveIntervalSolver.coldRateSeed` solves the port graph once at a cold start; on the three full fixtures that rate solve fails in pass 0 (GRID "Newton iteration limit at residual 0.5402; active-set pass=0", ALTERNATING and BOTH_SIDES 2.5263), so `coldRateSeed` returns the graph unchanged and the first steps start from the compiler's junction guess (every junction at 110 kPa, the first boundary's state) with no accepted structure, so neither `closeDeadHeads` (runs ending at a junction) nor `closeIllegalStarts` (no accepted solve) reaches a generator edge: pass 0 of every step has ten two-way generators, and 20 halvings exhaust the interval (21 Newton solves = the failed rate solve + 20 steps).
- **One-way generators are necessary but, from the guess, not sufficient.** Rate solves of the GRID port graph with generator edges blocked below a threshold: from the 110 kPa guess, closing below 135, 145, 155, 165 or 175 kPa all fail in pass 0; from a uniform junction pressure of 150/170/180 kPa, closing below 165 kPa (exactly the generators the converged solution closes, 110-160 kPa) converges at every seed, closing below 145 kPa converges only from 170 kPa, below 175 kPa only from 170 kPa; with the generators two-way no seed converges. So the defect is the active set (any receiving generator in pass 0 is fatal here) and pass 0 also needs junction pressures near the solution.
- **The physical active set** (the converged D12 rate seed, tanks held): GRID junctions 164.8-170.2 kPa, generators 170, 180, 190, 200 kPa supply (190, 200, 180 on the cap), the six at 110-160 kPa closed; ALTERNATING junctions 140.1-188.2 kPa, generators 190, 200, 180 supply, seven closed; BOTH_SIDES the same by symmetry.

### 2. What changed

| file | method | change |
|---|---|---|
| `science/fluid/network/PassiveStepSolver.java` | `solve` | one line after `closeIllegalStarts`: `graph=closeColdReceivingGenerators(graph,boundaryClosed,keep,closedPorts,checkpoint)` |
| | `closeColdReceivingGenerators` (new) | acts only on a **rate solve with no accepted solve of its structure** (the cold rate seed); skips an island with no generator, with solids, or with any actuator or filter; collects every open passive generator run (through degree-two junctions) that ends at a junction of degree three or more; asks `oneWayPressures`; closes (`boundaryClosed`, for this solve) each run whose far junction the estimate puts above the generator's end pressure (with its column) by more than `reopenBand`; if any run closed, returns the graph with every estimated junction re-flashed at the estimate's pressure, its own temperature and composition (inventory untouched; a domain refusal keeps the guess); otherwise returns the graph unchanged |
| | `oneWayPressures` (new), class `OneWay` (new: `residual`, `sweep`, `flow`, `scale`, `runFlow`), `maximum`, `oneWayResult`, `solveBanded` (new) | the one-way pressure estimate: unknowns = junctions a held node reaches through open connections, numbered breadth-first from the held nodes; each connection carries `initialMassFlow`'s law in the column of the donor end the direction draws from (the run's loss inverted by a safeguarded Newton, capped at the donor's velocity limit), nothing in a direction `boundaryAllowed` refuses (so a generator below its junction carries nothing) or between the two columns; a gas-only junction donor has its density and cap scaled with its pressure (ideal gas); a damped Newton on the junction pressures (per-connection difference derivatives, banded elimination, steps held to half the held-pressure hull widened by the largest static column, Armijo on the squared residual), with one Gauss-Seidel sweep (60-step bisection per junction) wherever the Newton step does not descend; converged at max net inflow <= 1e-9 x max(1, largest cap) kg/s; null (nothing closed) if not converged in 200 iterations or 100 sweeps |
| `src/test/.../runtime/fluid/ExtremeTopologyIslandTest.java` | class javadoc, `FULL_CASES_OPEN_DEFECT`, `theFullCasesHoldOnTheirFirstIntervalOpenDefect` javadoc | `FULL_CASES_OPEN_DEFECT = false`, so the flagged test runs `assertIntegrates` (every oracle of the passing cases) on the three full fixtures at the default cap: it is now the D12 regression; the defect section and the method javadoc keep the history (the reproduction branch is kept; set true on this tree it fails "expected to hold on its first interval ... it now integrates 40 intervals", measured). No assertion changed. |

`PassiveIntervalSolver`, `ConservativeTransport`, the reconstruction, `closeDeadHeads`, `closeIllegalStarts`, `startPoint`, `initialMassFlows`, the balanced seed and the pass loop are untouched.

### 3. O2, and why O1 was not needed

- **O2 is the fix**: the cold-start rate seed now starts with one-way generators. Its closures bind the rate solve only; the rate seed converges (all three fixtures), its structure then counts as accepted, and the first step's existing `closeIllegalStarts` closes the generators the seed shows receiving (start flow zero, `initialMassFlows` on the seeded states illegal, `illegalWithEitherDensity`) before that step's pass 0, with the seed's junction states and flows as the start. So the first pass of every step starts from admissible directions, which is what D12 asks, without a new rule in the step solve.
- **O1 not needed, and alone not sufficient.** Measured with the closure applied to any first solve and the cold rate seed disabled (a variant, not kept; `logs/05-o1-first-step-only.txt`): GRID and ALTERNATING at 0.1 s integrate, the three 5 s runs and BOTH_SIDES at 0.1 s still fail interval 1 in pass 0 (4 of 6; the interval solver reopened a closed run in 3 of them, "Backward-Euler boundary reopened=1", after which pass 0 fails as before). The final code therefore gives the closure to the rate seed only (A33). O3-O5 were not needed.
- **Design steps measured on the way** (drivers kept): a Gauss-Seidel estimate alone needs 80-488 sweeps to 1 Pa (0.4-5 s on the grid) and, cut to 5-40 sweeps, misses closures and the rate solve fails (`D12Estimate.java`); a pure Newton estimate with the start densities put the grid junctions about 10 kPa high (the guessed junction state is at 110 kPa) and wrongly closed the 170 kPa generator, whence the gas scaling; a pure Newton stalls at the cap and one-way kinks (grid: no descent at iteration 10), whence the Gauss-Seidel fallback (one sweep on the grid, none on the rows). The final estimate matches the converged rate seed to a few Pa (grid junction 11: 164792 against 164796 Pa) in 24 iterations on the grid (one of them a Gauss-Seidel sweep) and 27 on the rows.

### 4. Per-fixture numbers, before (`7051391`) and after

Full fixtures at the default 100 m/s cap (`theFullCasesHoldOnTheirFirstIntervalOpenDefect`):

| Fixture | Slice | Before | After: intervals | Newton solves | Accepted / rejected | Worst moles | Worst energy | Monotone tanks |
|---|---|---|---|---|---|---|---|---|
| GRID | 5 s | fails interval 1 (21 solves, residual 0.5274) | 40/40 | 149 | 140 / 4 | 3.93e-16 | 1.24e-15 | 10/10 |
| GRID | 0.1 s | fails interval 1 (21, 0.6011) | 400/400 | 404 | 400 / 0 | 1.96e-14 | 2.18e-15 | 6/10 |
| ALTERNATING | 5 s | fails interval 1 (21, 2.5106) | 40/40 | 153 | 141 / 6 | 2.87e-15 | 5.55e-16 | 10/10 |
| ALTERNATING | 0.1 s | fails interval 1 (21, 2.5247) | 400/400 | 405 | 400 / 1 | 1.58e-14 | 6.87e-15 | 8/10 |
| BOTH_SIDES | 5 s | fails interval 1 (21, 2.4947) | 40/40 | 162 | 144 / 7 | 4.44e-15 | 1.47e-15 | 10/10 |
| BOTH_SIDES | 0.1 s | fails interval 1 (21, 2.5231) | 400/400 | 430 | 409 / 10 | 5.28e-15 | 1.62e-15 | 8/10 |

Every oracle passes: no generator receives, ledgers < 1e-12 / 1e-10, no tank above `P_max`, the lowest tank nondecreasing, directions from interval 2 on (351/3746, 351/3780, 702/7342 statements judged), the highest generator supplies and the lowest tank receives in the first 0.1 s, rest at `P_max` within 1e-6 with every other generator CLOSED. 5 s against 0.1 s (largest relative differences at t = 5..40 s): GRID pressure 3.71e-3, temperature 0.346 K, moles 3.08e-3, supplied 1.16e-2, generator split 1.2 %; ALTERNATING 2.88e-3, 0.298 K, 2.33e-3, 9.77e-3, 0.9 %; BOTH_SIDES 2.50e-5, 0.017 K, 5.24e-5, 4.97e-5, 0.5 % (bound 2 % and 0.5 K).

Reduced cases (spread 0.2, default cap) and the uncapped full cases (100000 m/s) moved because their cold rate seeds also see receiving generators; every oracle still passes, the cadence lines are unchanged except the printed generator split (ALTERNATING 0.0199 to 0.0198 reduced, 0.00385 to 0.00386 uncapped):

| Case | Slice | Newton solves before -> after | Accepted / rejected before -> after | Worst moles before -> after | Worst energy before -> after |
|---|---|---|---|---|---|
| GRID @0.2 | 5 s | 142 -> 139 | 127/3 -> 127/3 | 2.53e-15 -> 2.69e-15 | 1.55e-15 -> 1.74e-15 |
| GRID @0.2 | 0.1 s | 410 -> 407 | 400/0 -> 400/0 | 4.35e-15 -> 4.10e-15 | 3.50e-15 -> 4.28e-15 |
| ALTERNATING @0.2 | 5 s | 141 -> 142 | 127/3 -> 127/3 | 8.55e-16 -> 8.83e-16 | 1.89e-15 -> 1.67e-15 |
| ALTERNATING @0.2 | 0.1 s | 413 -> 409 | 401/1 -> 400/0 | 3.88e-15 -> 7.33e-15 | 7.61e-15 -> 1.78e-15 |
| BOTH_SIDES @0.2 | 5 s | 153 -> 155 | 128/4 -> 128/4 | 2.80e-15 -> 2.88e-15 | 4.70e-15 -> 4.86e-15 |
| BOTH_SIDES @0.2 | 0.1 s | 433 -> 425 | 402/2 -> 402/2 | 7.57e-16 -> 1.63e-15 | 3.37e-15 -> 3.17e-15 |
| GRID uncapped | 5 s | 160 -> 154 | 143/5 -> 143/5 | 4.90e-16 -> 1.03e-15 | 2.22e-16 -> 8.88e-17 |
| GRID uncapped | 0.1 s | 434 -> 430 | 412/8 -> 412/8 | 4.68e-15 -> 6.75e-15 | 1.91e-15 -> 1.73e-15 |
| ALTERNATING uncapped | 5 s | 182 -> 158 | 144/5 -> 144/5 | 1.86e-15 -> 1.59e-15 | 2.25e-15 -> 1.28e-15 |
| ALTERNATING uncapped | 0.1 s | 443 -> 426 | 413/9 -> 412/8 | 3.06e-15 -> 2.62e-15 | 2.96e-15 -> 1.37e-15 |
| BOTH_SIDES uncapped | 5 s | 216 -> 165 | 145/6 -> 145/6 | 1.37e-15 -> 9.98e-16 | 2.42e-15 -> 2.00e-15 |
| BOTH_SIDES uncapped | 0.1 s | 463 -> 433 | 414/8 -> 414/8 | 1.40e-14 -> 3.83e-15 | 3.07e-15 -> 3.34e-15 |

The pass/fail scan of `EXTREME_TOPOLOGY_TESTS.md` 4.4 (`D12Scan.java`: grid columns and pairs 1-10 at full spread, spreads 0.01-0.75 at ten, 60 cases x 2 slices): base `7051391` fails the first interval in 71 of 120 runs; D12 integrates all 120 runs over 40 x 5 s and 400 x 0.1 s, no generator receipt, worst component ledger 2.42e-14 (`logs/06-scan-*.txt`).

### 5. Where the new path runs in the existing suites (bitwise)

Temporary print in `closeColdReceivingGenerators` over the harness science, runtime, adjacent and regression runs (`src/sites-instrumentation.patch`, `logs/04-sites.txt`): it closes something only in `ExtremeTopologyIslandTest` (18 cold rate seeds) and in 2 of the 32 `MixedGasJunctionStaticTest` cases; it runs and closes nothing in the other 30 static cases, `PipePresentationTest` (6), `LiquidJunctionTransientTest` (4), `PhasePortClosureTest` (3) and `PhysicalRegistryTest` (1), and returns before the estimate everywhere else (no generator, an actuator, a filter or solids, or no generator run onto a junction: the mixed-gas transients have tanks and voids only; the pumped cold start has a pump; chain-100 has no junction). Where it closes nothing it changes no state, so those runs are bitwise:

- the 33 MIXED_GAS/LIQUID_JUNCTION lines of `harness.sh runtime` identical to the reference (the LIQUID_JUNCTION generator cases run the estimate and close nothing);
- the WP1/D9 bitwise probe (`tools/phase-ports-probes/d9/src/BitwiseProbe.java`: chain-100, 14 all-BULK scenarios, 8 gas phase-port scenarios, 2 liquid-port scenarios) on `7051391` and on D12: `chain-100.json`, `scenarios.txt`, `gas-ports.txt`, `liquid-ports.txt` byte-identical (`logs/07-bitwise-probe-sha256.txt`);
- the 32 static cases, every result double printed (`D12StaticCompare.java`, `logs/08-static-*.txt`): 30 byte-identical; the two that close (swap=false and swap=true, unequal, 150 kPa, 3 ports: the 145 kPa generator below the junction at about 148-149 kPa) end in the same modes (the low generator CLOSED) with flows equal to 1.6e-11 kg/s and junction pressures to 1.3e-7 Pa, in 2 Newton solves instead of 3 and 5 (the second: 1 accepted / 0 rejected instead of 2 / 1). The test's assertions pass unchanged.

Cost: the estimate took 20.7 / 8.8 / 16.4 ms on the GRID / ALTERNATING / BOTH_SIDES cold seeds (first call in a fresh JVM) and 0.07-0.09 ms on the static cases' 4-7 node islands; it runs once per cold structure with a generator run onto a junction.

### 6. Gates (harness; Gradle not run)

JDK OpenJDK 21.0.10 (container). Log `tools/phase-ports-probes/d12/logs/01-harness-all-d12.log`.

| # | command | result |
|---|---|---|
| H1 | `REPO=<worktree> LIB=<jars> bash tools/cloud-science-harness/harness.sh all` on the final tree | science 213/213, runtime 237/237 (incl. the 10 `ExtremeTopologyIslandTest` cases, the flagged test now running every oracle) with **the 33 junction lines identical to the reference**, adjacent 38/38, regression 1/1 with **chain-100 0.000e+00** |
| H2 | WP1/D9 bitwise probe, base against D12 | 4 outputs byte-identical |
| H3 | `ExtremeTopologyIslandTest` with `FULL_CASES_OPEN_DEFECT = true` on the D12 tree | the reproduction fails as designed (3 of 10: "it now integrates 40 intervals") |

**Not run: the Gradle gates (G1 fluid suites and junction lines, G2 exact regression, G3 38 adjacent, G4 GameTest and mcpCompat compile). They must be run after the merge.** No GameTest or in-game scenario (no runtime path changed).

### 7. Decisions recorded

`DECISION_LOG.md`, "D12 defaults": A33 (the closure belongs to the cold rate seed, not the step), A34 (islands with an actuator, a filter or solids are left to the pass loop), A35 (the closure margin is the reopen band), A36 (the estimate's law and when it reseeds the junctions).

### 8. For the merge

- Touched: `PassiveStepSolver.solve` (one added line after `closeIllegalStarts`) and new private members inserted between `closeIllegalStarts` and `illegalWithEitherDensity` (`closeColdReceivingGenerators`, `oneWayPressures`, `maximum`, `oneWayResult`, `OneWay`, `solveBanded`); the test file's javadoc and flag. No change in `ConservativeTransport`, the reconstruction booking, `PassiveIntervalSolver` or the step path.
- D11 (priority streams) removes the availability mask: `closeColdReceivingGenerators` and `oneWayPressures` read `closedPorts` only through `boundaryAllowed`, so they follow whatever `boundaryAllowed` becomes.
- `CHANGELOG.md` `[Unreleased]` is still empty on this branch; the batch's line is written at its close (WP6).

### 9. Commits and material

Code commit `418ca7e` (`WIP phase-ports D12: one-way generators from the cold start`, on `7051391`); tools commit after it (this line). Material: `tools/phase-ports-probes/d12/` (`src/` the drivers `D12Probe`, `D12Estimate`, `D12StaticCompare`, `D12Scan` with `run.sh`, the site-instrumentation and O1-variant patches; `logs/` the harness run, the class outputs on both trees, the scans, the static comparison, the bitwise-probe hashes, the rate-seed outputs). H1 was re-run on the committed tree `418ca7e` (`logs/01-harness-all-d12.log`): the same results.

## D13: polish on a fresh Jacobian before a gate refusal (2026-09-26)

- Author: Claude (Opus 5.5), in the separate worktree `scratchpad/wt-vent` (branch `claude/vent-gate-polish-wip`, from `99f5312` = the vent investigation plus the D13 decision entry; src identical to `fdc7571`), because the D11 agent edits `PassiveStepSolver`/`ConservativeTransport` in the main checkout and holds the Gradle lane. Code commit `4a71629` (`WIP phase-ports D13: polish on a fresh Jacobian before a gate refusal`); tools and this section: the following commit. Not merged, not pushed.
- Decision: D13 (owner, 2026-09-26), option A of the section "Vent gate defect".

### 1. What changed (every method touched)

| file | method | change |
|---|---|---|
| `science/fluid/network/PassiveStepSolver.java` | `solve(PassiveNetwork, double, Runnable, Acceptance, boolean)` | the gate lines only: the threshold becomes a local `gate` (1e-8 FULL, 1e-6 APPROXIMATE, as before); a refusal calls `polishRefusedPoint` and, if it returns a point, the step continues with that point's Newton result, variables, states, flows, projection and reconstructed encoding; a remaining refusal counts `equationGateRejections` and throws exactly the old message |
| same | `polishRefusedPoint(...)` (new, private) and `record Polished` (new, private) | one `SparseNewton.solve` from the refused converged point on a new, never retained `SparseNewton.Workspace` (a fresh Jacobian of this step's own dt and state) at `tolerance * 1e-2`; flows zeroed on closed edges as the accepted path does; refuses (returns null) on a Newton failure, a direction `boundaryAllowed` forbids, a broken velocity constraint, a `phaseCorrection` that would reseed, or a gate still above threshold; else returns the point |
| `science/fluid/diagnostics/SolverDiagnostics.java` | fields and `counters()` | `equationGateRejections`, `equationGatePolishes`, `equationGatePolishesAccepted` (appended after `verificationResiduals`; counters record only while `ENABLED`, and only named counters feed the junction lines) |
| `src/test/.../science/fluid/network/VentGatePolishTest.java` | new, 3 tests | the vented manometer (A fed at 104325 Pa, B filled through its LIQUID port and vented through its VAPOR port to a 1 atm void) at 5 s (8 slices) and 0.1 s (100 slices), and the BULK-vent control at 5 s; every slice whole, ledgers 1e-12 / 1e-10, no gate key in the rejection reasons, `equationGateRejections` 0, and at 5 s at least one accepted polish |
| `src/test/.../science/fluid/network/LevelHeadTest.java` | `manometer(PhasePort)` -> `manometer(PhasePort, boolean vented)`, its two calls in `cushionsHeldApartStandAtALevelDifferenceOfDpOverRhoG` | the head fixture vents B through its VAPOR port to a 1 atm void (the level-head review's fixture 3); the BULK control keeps B closed (a vent would hold B below A, and the control's point is that the headspaces equalise); javadoc restated; **no assertion changed** |

Nothing in the port stream code, the WP2 closure, the cold start or `ConservativeTransport` was touched. The polish applies to both acceptances (FULL gate 1e-8, APPROXIMATE gate 1e-6), as D13 states it.

### 2. Bitwise proof

Only a refused point reaches the new code and the fresh workspace is local, so no accepted step and no later solve's preconditioner changes. Measured:
- d9 `BitwiseProbe` (chain-100, the 14 all-BULK scenarios, the 8 gas-only phase-port islands, the liquid-port islands) on `99f5312` and on `4a71629`: `chain-100.json`, `scenarios.txt`, `gas-ports.txt`, `liquid-ports.txt` byte-identical, the same hashes as the d9 capture (`d9/results/probe/out-d9/`), `chain-100.json` identical to the regression reference (`tools/phase-ports-probes/vent/results/bitwise-probe/sha256.txt`).
- Cloud harness `all` on `4a71629` (`logs/13`, 115 s): science **216/216** (213 + the 3 of `VentGatePolishTest`), runtime **237/237** with the **33 junction lines identical** to the reference, adjacent **38/38**, chain-100 **0.000e+00** on state/moles, temperature, phase fraction and flow.

### 3. Where it fires (a traced copy of `4a71629`, `logs/14`)

| suite | polishes (all accepted) |
|---|---|
| science | 75: `LevelHeadTest` 55 (per fixture not attributed; the fixtures whose lines moved are the drain, its 0.1 s BULK control, the replay and the vented manometer), `VentGatePolishTest` 20 (13 VAPOR vent 5 s, 7 BULK vent 5 s, 0 at 0.1 s) |
| runtime | 13: `FilterBlockLineIslandTest` 9, `DeadHeadedLineIslandTest` 2, `ElevatedBlockLineIslandTest` 2 |
| adjacent | 11: `FilterBlockLineIslandTest` 9, `DeadHeadedLineIslandTest` 2 |
| regression | 0 |

Every polish passed the gate (99 of 99); no gate refusal is left in any suite. Printed counters that moved (`logs/15`; assertions all unchanged): `LevelHeadTest` drain 5 s 168/21 -> 152/3 (18 gate rejections -> 0; Newton solves 190 -> 159, iterations 661 -> 379), replay 48/12 -> 36/0, the drain's 0.1 s BULK control output 1.67e-6 -> 8.8e-7 kg; the manometer line is new (vented): P_B 101324.99999 Pa, dP 3000.00003 Pa, level difference 0.3071415 m against dP/(rho g) 0.3071399 m (5e-6 relative), bottoms level to -1.0e-7 Pa, 187 accepted / 2 rejected (1 reopen, 1 Newton iteration limit, recovered); `VENT_GATE_RUN`: VAPOR vent 5 s 30/1 (the reopen), 13 polishes; 0.1 s 100/1, none; BULK vent 5 s 32/1, 7 polishes (before D13: the VAPOR vent failed slice 1 on the substep limit; the BULK vent refused one substep per slice).

### 4. Gates not run here

No Gradle run: the Gradle lane belongs to the D11 agent. The Gradle gates of record (fluid suites with the 33 junction lines against `d10/logs/02-junction-lines-d10.txt`, exact regression, the 38 adjacent, GameTest and mcpCompat compile) run after this branch merges into `claude/phase-ports-compressor`. The harness is a Minecraft-free stand-in and does not compile the GameTest and mcpCompat source sets; the only non-test main change besides `PassiveStepSolver` is three public counters in `SolverDiagnostics`, whose one GameTest reader (`FluidServerBenchmark`) iterates `names()`.

## D11+D12+D13 merge (2026-09-26)

- Author: Claude (Opus 5.5), on branch `claude/phase-ports-compressor` in `/home/user/CreateChemE` (D11 tools commit `be8e226`). Merge commits: `aaa6624` (`claude/cold-start-generators-wip`, D12: `418ca7e` + `bb16308`) and `334ca78` (`claude/vent-gate-polish-wip`, D13: `4a71629`, `38debce`, `ce6b09c`); tools and this section in the commit after them. Not merged to `main`, not pushed. Material: `tools/phase-ports-probes/merge/logs/`.

### 1. Reconciliation

- **D12 under D11 (`aaa6624`).** One Java conflict. D12's `closeColdReceivingGenerators` and `oneWayPressures` took WP2's `closedPorts` mask, which D11 removed. They now take none, and their edge test is D11's static `boundaryAllowed(graph,pipe,flow)`: `solve` calls `closeColdReceivingGenerators(graph,boundaryClosed,keep,checkpoint)` after `closeIllegalStarts`. D12's defaults A20-A23 collided with D11's A20-A32. They are renumbered A33-A36 in `DECISION_LOG.md` and at every reference in this review and the plan. Documents: every section and row from both sides, kept in order.
- **D13 under D11 (`334ca78`).** `polishRefusedPoint` came from the WP2 tree. It checked the WP2 mask in `boundaryAllowed`, capped every flow with `massFlowLimit` at the draw port, reconstructed without phase draws, and reused the refused point's heads. It never asked the other questions the pass loop asks before it accepts a point. Merged, the polished point is checked exactly as the D11 pass loop checks an accepted point, in the same order:
  1. A trace reactivation that is due refuses the polish. The check uses copies of the `promoted` flags, so no pass state moves.
  2. Flows and heads are recomputed from the polished variables, with closed edges zeroed.
  3. Static `boundaryAllowed`.
  4. No device mode change. The pump and valve transitions were factored out of the pass loop into `nextMode(...)` with the same arithmetic, so the pass loop and the polish read one statement of them.
  5. `segmentsMoved` against the segments the pass froze.
  6. The D11 velocity cap: `massFlowLimit` at a BULK end, `capLimit` at a phase port.
  7. `draws(flows,states)` and the reconstruction with those six-entry draws.
  8. `phaseCorrection`, then the gate.
  
  `Polished` now carries the polished flows, heads and draws, and the caller installs all of them. `PipeTransfer.sample`, the accepted modes' `velocityCap` and the retained last solve therefore read the polished point, not the refused one. `polishRefusedPoint` never ran on the D11 fixtures, and every step the gate accepts is untouched, so all D11 output is bitwise as before, apart from the four lines below.
- `LevelHeadTest` auto-merged: D11's restated `aTankAtTheVoidsPressureDrainsOnItsHeadAlone*` (A32) and D13's vented `manometer(PhasePort, boolean vented)`. Both hold, and no assertion changed in the merge.

### 2. D13 counters on the D11 fixtures (D11 open item 7)

Measured from the `LEVEL_HEAD_RUN` / `VENT_GATE_RUN` / `PHASE_PORT_PRIORITY_*` lines of the harness run (`logs/06`), and from a traced copy of `334ca78` that prints each polish entry, each acceptance and each gate refusal left standing, attributed to the @Test method (`logs/07`).

| fixture | D11 (`548a9bf`) | merged (`334ca78`) |
|---|---|---|
| head drain 5 s | 213 / 34, 11 equation gate | 203 / 22, **0** equation gate (9 state change, 5 Newton limit, 4 line-search stall, 4 phase-correction cycle); 4 polishes, 4 accepted; `lastSliceOut` 2.2e-12 -> 0.0 kg |
| head drain 0.1 s | 1500 / 0 | 1500 / 0; 1 polish (its BULK control), accepted; BULK control output 1.67e-6 -> 8.8e-7 kg (D13's own figure) |
| slow drain (replay) | 48 / 12, 12 equation gate | 36 / 0, **0**; 35 polishes, 35 accepted; the replay still bitwise |
| manometer | 182 / 3, 2 equation gate (B closed, the D9 form) | vented through B's VAPOR port (D13): 187 / 2, **0** equation gate (1 reopen, 1 Newton limit); 16 polishes, 16 accepted. The line is bit-identical to D13's own capture (`vent/logs/15`) |
| `PhasePortPriorityTest` (all 10) | no equation gate | no polish fires; every `PHASE_PORT_PRIORITY_*` line bit-identical to `d11/logs/06` |
| `VentGatePolishTest` | (not on D11) | VAPOR vent 5 s 30 / 1, 13 polishes; 0.1 s 100 / 1, none; BULK vent 5 s 32 / 1, 7 polishes; `gateRejections=0` in all three. Bit-identical to D13's capture |

Open item 7 is closed: no equation-gate refusal is left in any suite. Across `all`, the polish fired 100 times and was accepted every time: science 76 (`LevelHeadTest` 56, `VentGatePolishTest` 20), runtime 13, adjacent 11, regression 0. Polished residuals were 3.9e-15 to 5.2e-10 against the 1e-8 gate. D13 alone fired 99 times (`LevelHeadTest` 55). The extra firing is in the restated D11 drain.

### 3. Gates (on `334ca78`; logs `tools/phase-ports-probes/merge/logs/`)

| # | command (from `/home/user/CreateChemE`) | result |
|---|---|---|
| (a) | `LIB=<jars> OUT=<dir> REPO=/home/user/CreateChemE bash tools/cloud-science-harness/harness.sh all` (`01-harness-all-merge.log`, 103 s) | science 211/211 (D11's 208 + `VentGatePolishTest` 3), runtime 237/237 with the 33 junction lines identical to its reference, adjacent 38/38, chain-100 0.000e+00 |
| (b) | `./gradlew --no-configuration-cache --no-build-cache test --rerun --tests com.wormzjl.createcheme.science.fluid.* --tests com.wormzjl.createcheme.runtime.fluid.* --console=plain --continue` (`02-gradle-fluid-suite-merge.log`, 79 s) | **446/446** in 100 classes, 0 skipped: every one of D11's 443 names plus `VentGatePolishTest`'s 3, none missing (`02-test-names-merge.txt`). The 33 junction lines (`02-junction-lines-merge.txt`) are identical to `d10/logs/02-junction-lines-d10.txt` apart from ms/bytes/allocatedMB. MIXED_GAS_COST 286 Newton solves (624 ms, 135.0 MB, single run). `ExtremeTopologyIslandTest` 10/10 with `FULL_CASES_OPEN_DEFECT = false`: the three full fixtures run 40/40 x 5 s and 400/400 x 0.1 s at the default 100 m/s cap and at the configuration's largest (100000 m/s), every oracle |
| (c) | `./gradlew --no-configuration-cache fluidSolverRegression -PfluidRegressionMode=exact --console=plain` (`03-...`) | **0.000e+00** on state/moles, temperature, phase fraction and flow; 3 accepted / 0 rejected, 4 Newton solves, 29 iterations |
| (d) | the 38 adjacent (WP1 G3 command; `04-...`) | **38/38** in 7 classes |
| (e) | `./gradlew --no-configuration-cache compileFluidGameTestJava compileMcpCompatJava --rerun --console=plain` (`05-...`) | **BUILD SUCCESSFUL**, both tasks executed |

No gate failed. No assertion and no tolerance changed in either merge.

## WP3 + WP4: liquid-only pump, gas compressor, inlet-phase refusal (2026-09-26)

- Author: Claude (Opus 5.5), `/home/user/CreateChemE` (cloud container), branch `claude/phase-ports-compressor`, base `85c492f` (the D11+D12+D13 merge plus the D14 decision-log entry). Code commit `fc9574d` (`WIP phase-ports WP3+WP4: liquid-only pump, compressor, inlet-phase refusal`); tools and documents in the commit after it. Not merged, not pushed.
- Scope: plan 3.6 under D11 and plan 3.7 (decisions D1, D2, D4, D6, D7, D8), science only, plus the runtime touch points that must compile: `IslandCertificate.graphIdentity`, `PhysicalFluidTopology.Device`, `FluidCheckpointCodec`'s two exhaustive control switches, `TopologyCompiler.Kind`. The block, GUI, codec format, placement defaults and face-to-port compile are WP5's (section 12). Done together because WP3 alone leaves the 15 gas-pump tests failing until WP4 turns them into compressor tests.
- Not touched: the thermo package (D14), the D13 polish (`polishRefusedPoint` reads the new `nextMode` unchanged), D11's port draw, the D9 head, D12's cold-start closure (it already skips islands with any actuator, a compressor included: section 5).

### 1. What changed, per file

| file | change |
|---|---|
| `science/fluid/network/FlowControl.java` | `FlowControl` permits `Passive`, `Mover`, `PressureValve`; new sealed `Mover` (permits `Pump`, `Compressor`): `targetVolumeFlow()`, `efficiency()`, `riseLimit(suctionDensity, suctionPressure, referenceDensity)`, `power(massFlow, head, suctionDensity, suctionPressure)` (W, booked as heat), `refuses(InletPhase.Supply, refusedBefore)`, `deviceName()`. `Pump` implements it with the density-scaled limit and `q v_s head / eff` in the same operation order as before (bitwise), `VAPOUR_REFUSE` 0.02 / `VAPOUR_RESUME` 0.005. New `Compressor(targetVolumeFlow, maximumPressureRatio, efficiency)` (ratio > 1): limit `(r_max - 1) P_s`, power `max(0,q) (P_s/rho_s) ln(1 + max(0,head)/P_s) / eff` (`Math.log1p`), `CONDENSED_REFUSE` 0.01 / `CONDENSED_RESUME` 0.002, any solid population above `SlurryTransport.DEFAULT_TRACE_VOLUME_FRACTION` refuses. `Mode.INLET_WRONG_PHASE` appended (ordinal 9; endpoint modes persist as ordinals, the codec's range check reads `values()`). |
| `science/fluid/network/InletPhase.java` | new, public: `Supply` record (walk end node, its id, the port, vapour share by volume, condensed share by mass, largest solid volume fraction), `walk` (package-private), `supply(model, graph, edge, Q, duration)`, `decide(model, graph, committedModes, duration)`, `reason(mover, supply)` and `reason(model, graph, edge, duration)` (section 2). |
| `science/fluid/network/PassiveStepSolver.java` | Every pump site reads `FlowControl.Mover` (section 4). `riseLimit(mover, rho, P_s)`. The dead `work` accumulation of the pass loop removed (never read). New `sliceRefusals` / `refuseInlets(Set<Long>)` / `refusedInlets(...)`: a refused mover is `boundaryClosed` with mode `INLET_WRONG_PHASE` from the start of every solve of the slice, after the both-way-blocked closure (section 3). New `suctionCapped` in the initial mode (section 6). `edgeRows`' actuator switch: `case CLOSED, INLET_WRONG_PHASE -> flow`. `reopenable` counts a refused mover as not open. `checkApproximation` skips its pump feasibility test for a refused mover. The energy row books `mover.power(...)` with the first node's trial pressure. `boundaryAllowed` package-private (read by `InletPhase`). |
| `science/fluid/network/PassiveIntervalSolver.java` | `decideInlets` at the start of `solve` (before the solid event integrator's dispatch) and `solveApproximate`; `inletModes`/`inletKey`, restated by `replayStart` from the committed interval, advanced to each returned interval (`committedInlets`). |
| `science/fluid/network/ConservativeTransport.java` | Mover work: the pump's two expressions kept to the bit, the compressor's `dt * power(q, head, 1/v_s, P_s)` on the drawn stream's specific volume and the candidate first node's pressure. |
| `science/fluid/topology/TopologyCompiler.java` | `Kind.COMPRESSOR` appended (ordinal 7); `invalidPumps` holds a compressor to the pump's zero-storage cycle rule. |
| `runtime/fluid/PhysicalFluidTopology.java` | `Device`: `COMPRESSOR` requires a `Compressor` control; `actuator()` includes it (so it connects only along its facing axis and owns the outlet run); an invalid compressor gets a zero target like an invalid pump. |
| `runtime/fluid/IslandCertificate.java` | `graphIdentity` case `"compressor"` (target, ratio, efficiency); header unchanged (no existing digest changes). |
| `runtime/fluid/FluidCheckpointCodec.java` | Both control switches: a `Compressor` throws `IllegalArgumentException("Compressor controls are not persisted before checkpoint format 6")`; control tag 3 and the format bump are WP5's. No runtime path builds a compressor before WP5. |
| tests | new `InletPhaseTest` (6), `CompressorTest` (5), `TopologyCompilerTest.compressorOnlyBypass...`, `PhysicalFluidTopologyTest.aCompressorConnectsAlongItsAxisOnly...`; the migration of section 9. GameTest `FluidPumpStartupGameTests` edited (section 9). |

### 2. The supply walk and the measures (plan 3.6, "Inlet phase check")

- **Walk** (`InletPhase.walk`). From the device edge's first node. The device's own junction is crossed if it has exactly one other connection and that connection is passive; each following junction is crossed while it has two connections and no actuator touches it (the run `closeDeadHeads` walks; a filter edge is a passive link). The walk ends at the first node that is not such a junction:
  - a vessel, generator, void or port there supplies the stream of the last link's end (a generator or void as BULK);
  - a junction (a branch, a dead end, or one an actuator touches, e.g. a valve upstream) supplies its start-state mixture (its bulk);
  - a device drawing straight from a vessel walks nothing and its own first end supplies.
- **What a phase port supplies (A37, answers D11 open item 6).** The D11 priority draw of the device's target volume over the slice, `Q T`, on the committed state: the port's order, each phase up to its share of the vessel `V_phase / N` (`N` the vessel's phase-port ends that may carry outflow by the static rule, D11's capacity over the slice), the last held phase with the rest. So a dry bottom port reads gas, a bottom port holding less water than one slice of the target reads the gas it will break through to, and a condensate trace left after a drain does not read as liquid. With no target volume (`Q = 0`, or a rate solve with no slice) it is the leading phase (A28). The alternative (the leading phase always) reads a 1.5e-7 condensate trace as liquid and lets a pump suck gas for the whole next slice; not taken.
- **Measures** (`InletPhase.Supply`): pump `alpha_v = V_v / (V_v + V_l + V_w)` (fluid volumes; solids excluded, they move within the mobility rules, D1); compressor condensed share `(m_l + m_w + m_s) / m` and the largest population's volume over the supply's volume (fluid and solids). For a draw the volumes and masses are the drawn phase streams' (each liquid with its D11 solid share).
- **Reason** (`InletPhase.reason`, pure, for WP5 to show as is): `ERROR: pump inlet not liquid (vapour 34.0 % by volume, from node 12)`, `ERROR: compressor inlet not gas (condensed 2.5 % by mass, from node 12)`, `ERROR: compressor inlet not gas (solids 4.0e-07 by volume, from node 3)`; `node` is the id of the node the walk ended at (WP5 maps it to a device name). Measured texts: section 7.

### 3. The mode and where it is decided (decision D6, plan 3.6 "The error behaviour")

- **Decision** (`PassiveIntervalSolver.decideInlets`), once per slice, at the start of `solve`/`solveApproximate` (before the solid event integrator's dispatch, so every segment of a slice shares it), on the slice's input graph (the committed state) and the committed endpoint modes: `refused = mover.refuses(supply over the slice duration, committedMode == INLET_WRONG_PHASE)`; pump: `alpha > 0.02 || before && !(alpha < 0.005)`; compressor: solids above the trace, or condensed `> 0.01 || before && !(c < 0.002)`.
- **The committed endpoint mode**: `replayStart` restates it from the committed interval (null for none or another structure); a returned interval advances it. A job that solves several trials from one committed state (the module planner) decides the same each time: the rule is idempotent on one state (`f(a, f(a, r)) = f(a, r)`). The hysteresis is never solver-local, so a certified island woken with a fresh solver decides as the island that solved every interval (fixture 8).
- **In the solver**: the step solver holds the slice's refusals (`refuseInlets`, keyed by pipe id, so the solid event integrator's re-masked pipes keep them) for every step, the cold rate seed and the start-of-step guard's rate solve. A refused mover is `boundaryClosed` with mode `INLET_WRONG_PHASE` from the start of each solve (after the both-way-blocked closure, so it is reported even where a solid closure also blocks the pipe): its actuator row pins the flow to exactly zero, the pass loop never reopens it (`boundaryClosed` skips `nextMode`, so no CLOSED-to-HEAD_LIMIT offer), the carry never restates it (only HEAD_LIMIT and CLOSED are carried), nothing reaches its discharge through it (`reachableComponents`), and it keeps the mode in the accepted and endpoint modes. It is exactly the existing closed-device path: a refused pump's island equals, double for double, the island with a compressor closed both ways by `blockedDirections = 3` in its place (measured, section 7 fixture 4). The rest of the island integrates.
- **A step solver no interval drives** (a bare `PassiveStepSolver.solve`, as `VelocityClampTest` uses it) decides at each solve on its input states with its carried modes, the step as the slice (a rate solve: the leading phase).

### 4. Suction-stream quantities everywhere (D4), site by site

Every mover quantity reads the first (suction) end only; no discharge density or pressure enters any of them:

| site | reads |
|---|---|
| initial mode (`solve`) | `endVelocityLimit(first state, firstPort)` (the suction end's leading stream); `suctionCapped`: `suctionMassFlow(Q, first state, firstPort)` against each walk link's cap on its donor end |
| carry offer | `endDensity(first state, firstPort)` for the column and the limit; `P_s` = first node's pressure |
| pass loop `nextMode` (and the D13 polish through it) | `equations.drawn(i, 0, first node, q)` (the priority stream at a phase port): density for the target test `q/rho`, the limit, `demand`'s column; its velocity limit; `P_s` = first node's converged pressure |
| approximation probe | `endDensity(corrected first state, firstPort)`, its pressure |
| start point (target flow, head-limit start, reopen start) | `suctionMassFlow`, `endDensity`/`endViscosity`/`massFlowLimit` on the first end, its pressure |
| `initialMassFlow` | `suctionMassFlow`, `massFlowLimit` on the first end |
| `edgeRows` | column `suction.density`; target row `q / suction.density`; head-limit row `riseLimit(suction.density, P_a)` at the trial |
| Newton energy row | `drawn(edge, 0, first node, q).density`, first node's trial pressure |
| reconstruction work (`ConservativeTransport`) | the drawn stream's specific volume at the first node (start-state phases for pinned ones), the candidate first node's pressure |
| `FluidWorldAuthority.java:294` (displayed pump limit) | the first node's bulk density (the pump's own junction, i.e. what arrives at its inlet); pump only; the compressor's display is WP5's |

`P_s` is the first node's pressure, not its port's driving pressure: the level head (D9) is nonzero only at a LIQUID port holding condensed mass, which a compressor's inlet check refuses (A41).

### 5. D12's cold-start closure and the compressor

`closeColdReceivingGenerators` returns the graph unchanged for an island with any non-passive connection (`!(control instanceof Passive)`, A34), so a compressor counts as a pump there without a change (the decision D12 asked for).

### 6. Target above what the suction can deliver (plan 3.6)

The initial-mode test now also starts a mover on PUMP_HEAD_LIMIT when its target mass flow on the start state `Q rho_s` exceeds the velocity cap of any link of its suction walk (each on the stream its donor end draws, `massFlowLimit`) or a walk link may not carry flow towards the device (`boundaryAllowed`). The D11 capacities are no limit here: a port's last phase is uncapped (A22), so a port never bounds a draw's total; a port that would run into another phase is the inlet check's (A42). Bitwise where it does not fire: no existing fixture's walk is capped (the 33 junction lines, chain-100 and every existing test unchanged, section 10).

### 7. Fixtures (measured; Gradle G1, identical to the harness run)

Every slice commits whole (FULL, no hold) and closes the component ledger to 1e-12 and the energy ledger (with the mover's work) to 1e-10.

| test | fixture | measured |
|---|---|---|
| `InletPhaseTest.theSupplyWalkReadsTheSourceThroughDegreeTwoJunctionsAndAPortsDraw` | vessel (0.5 m3 water under N2, 150 kPa) - J1 - J2 - pump - void; with a void branching off J1; a valve upstream; direct pumps on a bottom, top, dry bottom and short bottom port (4 L of water against a 5 L slice draw) | reaches the vessel (its bulk share to 1e-15); a branch ends the walk at J1 (liquid seed: 0); a valve ends it at the pump's junction; bottom 0, top 1, dry bottom 1 (A37: a dry bottom port reads gas), short bottom 0.2204 = `(0.005 - V_w)/0.005` (the draw breaks through) while its leading phase reads 0 |
| `InletPhaseTest.thePumpsThresholdsAndReason` | the decision rule on the measure | 1.99 % runs, 2.01 % refused; refused stays at 0.51 %, resumes at 0.49 %; text `ERROR: pump inlet not liquid (vapour 34.0 % by volume, from node 7)` |
| fixture 4, `refusedPumpsCarryNothingAndTheIslandsWaterLineIntegratesBitwise` | one graph: a 300 kPa water generator filling a N2 tank (10 m, 20 mm), a pump between N2 tanks at 200 kPa and 1 atm, a pump on a two-phase generator (water + N2, 30.0 % vapour by volume, 150 kPa) into a N2 tank; 6 x 5 s and 60 x 0.1 s | both pumps `INLET_WRONG_PHASE` every slice, flow exactly 0.0, work 0.0, the suction tank's nitrogen unchanged; reasons `vapour 100.0 % ... from node 3`, `vapour 30.0 % ... from node 5`. The water line is **bitwise** the water line of the same island with the nitrogen line's device a compressor closed both ways (mode CLOSED), at both cadences, every slice. Against the water line alone (no device lines): bitwise at 5 s (every double, 6 slices); at 0.1 s 40 of 60 slices differ in the last bits of the flow (worst 10 ulp, 1e-13 asserted), and so do the closed-compressor island's, identically (`results/fixture4-bitwise-variants-0.1s.txt`: refused = closed compressor bitwise; a passive both-way-blocked line = alone bitwise): a closed device's head unknown and its two rows change the Newton's roundoff, as they did before WP3 |
| fixture 4, `aSweptSupplyRefusesAboveTwoPercentAndResumesBelowHalfAPercent` | generator swept 0, 1, 1.9, 2.1, 3, 5, 3, 1, 0.6, 0.4, 0 % vapour (bisection to the share), 150 kPa, pump 0.5 L/s into a water/N2 tank, one 5 s job per step | refused `[F,F,F,T,T,T,T,T,T,F,F]`: first at 2.1 %, held at 1 % and 0.6 %, resumed first at 0.4 %; flow 0.0 exactly while refused, > 0 otherwise |
| fixture 5, `aPumpOnABottomPortRunsThenMeetsGasAndIsRefusedAtTheNextSlice` | 1 m3 N2 tank at 120 kPa holding exactly 2.99 slices of the target (29.9 L), LIQUID port, 5 m 25 mm, pump 2 L/s (500 kPa) into a void at 150 kPa; 8 x 5 s | modes `[PUMP_TARGET, PUMP_TARGET, PUMP_HEAD_LIMIT, INLET_WRONG_PHASE x5]`: 9.9603 kg in slices 1 and 2 (on target), 9.8601 kg in slice 3 (admitted: its draw over the slice is 1 % gas; the port's water runs out within it, the drawn stream turns to gas, its density and so the pump's density-scaled limit collapse, HEAD_LIMIT), 0 from slice 4 (the supply is gas); tank water 9.2e-10 kg at the end; 88 accepted, 4 rejected (state change), no hold |
| fixture 8, `aFreshSolverReplaysARefusedPumpBitwise` | vessel 97 % water / 3 % N2 at 1 atm, filled by a 400 kPa water generator (10 m, 10 mm), a pump from it to a void; 12 x 5 s | vapour share 2.87 % -> 0.754 %, in the band from slice 6 (1.81 %); refused every slice (hysteresis from slice 6); a fresh solver from the committed interval of slice 1 and of slice 6 reproduces every flow, pressure, temperature, inventory and energy bit for bit and the modes; a fresh solver from slice 6's graph *without* the committed interval admits the pump (the input the replay needs) |
| fixture 6, `CompressorTest.onItsTargetTheFlowIsTheSuctionVolumeFlowAndTheWorkTheIsothermalWork` | two 1 m3 N2 tanks at 1 atm, compressor 0.02 m3/s ratio 1.5, one 0.5 s step | PUMP_TARGET, `q = Q rho_s` to 1e-10 (0.0226803 kg/s at 1.13401 kg/m3), head 2871.51 Pa, work 28.310290721080722 J = the formula to the printed digit |
| fixture 6, `aTransferRunsOnTargetThenOnItsRatioLimitThenClosesAtShutoffAtEitherCadence` | the same transfer, 12 x 5 s and 600 x 0.1 s | 5 s: TARGET then CLOSED, suction 81582.46 Pa / 280.444 K, discharge 122373.68 Pa / 314.745 K, ratio 1.49999988, landing -0.010 Pa, work 3254.2 J; 0.1 s: TARGET, HEAD_LIMIT, CLOSED, 81521.23 Pa / 280.203 K, 122281.65 Pa / 314.535 K, landing -0.19 Pa, work 2871.3 J; both land within 3 Pa (the pump's landing tolerance), 5 s against 0.1 s 0.075 % in pressure and 0.24 K (the extreme-topology bounds, 2 % and 0.5 K); 39 and 600 accepted, 0 rejected. The 5 s run books 13 % more work: backward Euler states each step's work at its end-state head, which over the ramp is above the average (first order, D5; section 11 item 4) |
| heating (plan 3.7 hand check), `theDischargeOfARatioThreeCompressionOfNitrogenIsAboutNinetyKelvinWarmer` | N2 generator 298.15 K 1 atm - compressor 0.02 m3/s ratio 4 - junction - void at 3 atm; 8 x 5 s | head 202672 Pa (ratio 3.0002), work 97196.4 J/kg; the junction (the discharge at steady flow) at 391.62 K: **+93.47 K**, h_out - h_in = the work per kg to 1e-3 |
| fixture 7, `aCompressorOnLiquidOrTwoPhaseOrSolidsIsRefusedAndTheIslandIntegrates` | one graph: a half-water tank (150 kPa) with a compressor on its side (BULK), on its bottom (LIQUID) and on its top (VAPOR); a two-phase generator; a N2 tank carrying 1 g of demo particle; 4 x 5 s and 40 x 0.1 s | side, bottom, generator and dusty tank `INLET_WRONG_PHASE`, 0.0 flow; the top port's compressor admitted (its draw is the vapour) and running (1.62 / 1.67 g/s); texts `condensed 99.8 % by mass, from node 1`, `... 99.8 % ... node 2`, `solids 4.0e-07 by volume, from node 3`, `condensed 100.0 % ... node 1`; 12 / 40 accepted, 0 rejected; the dusty tank's pipe is also blocked by the solid event integrator (`NO_LIQUID_CARRIER`, one declared transition per slice), and the mode still reads `INLET_WRONG_PHASE` |
| `theCompressorsThresholds` | the rule on the measure | 0.99 % runs, 1.01 % refused; refused stays at 0.21 %, resumes at 0.19 %; solids 2e-8 refused, 1e-8 not; ratio 1 refused by the constructor |
| `TopologyCompilerTest.compressorOnlyBypass...`, `PhysicalFluidTopologyTest.aCompressorConnectsAlongItsAxisOnly...` | topology | compressor-only bypass rejected, tank-buffered recycle kept, a compressor-pump ring rejects both; a side pipe does not connect (one port: `ERROR`), a zero-storage bypass gives the compressor a zero target (ratio kept) while passive equalisation flows; a COMPRESSOR device refuses a pump's controls |

### 8. The compressor's rows and work (plan 3.7), and the hand check

- **Rows** (the pump's, unchanged in shape): target `(q / rho_s - Q) / 0.01`; head limit `(head - (r_max - 1) P_a) / pressureScale` with `P_a` the first node's trial pressure (its derivative enters through the block Jacobian's re-decoded first node, already a column of the actuator row); shutoff margin `riseLimit - demand` against the same band; carry offer and reopen start as the pump's. Velocity limit: the drawn stream's own (the gas's acoustic bound or the configured 100 m/s).
- **Work**: `W = max(0,q) (P_s/rho_s) ln(1 + max(0,head)/P_s) / eff` in the Newton's energy rows (receiver `stored` and junction `heat`) and in the reconstruction; `checkConservation` is unchanged and closes (every fixture 1e-10; `BackwardEulerStepTest` 1e-5 J).
- **Hand check of plan 3.7's estimate.** Ideal gas: `W/m = (R T / M) ln r`; heating the same gas, `dT = W / (m c_p) = T (R / (M c_p)) ln r = T ((gamma - 1)/gamma) ln r`. Nitrogen, `T = 298.15 K`, `gamma = 1.4`, `r = 3`: `298.15 x 0.2857 x 1.0986 = 93.6 K`; work `8.314 x 298.15 / 0.028013 x 1.0986 = 97.2 kJ/kg`. Ideal adiabatic: `298.15 (3^0.2857 - 1) = 110 K`. The pump form `q v_s head`: `v_s head = (RT/(MP)) 2P = 2RT/M`, `dT = 2 T (gamma - 1)/gamma = 170 K`. **Measured** (heating fixture): 97.196 kJ/kg and +93.47 K. The plan's 93 / 110 / 170 K hold.

### 9. Migrated and re-baselined tests (plan Appendix C.1 and C.5), old -> new

Old values measured on `85c492f` (scratch worktree, `tools/phase-ports-probes/wp3-wp4/results/migration-values-head-85c492f.txt`, `reports-old/`), new on `fc9574d`. Test method names are kept (the gate reads every existing name).

| test | class | change | old | new |
|---|---|---|---|---|
| `FlowControlTest.pumpMeetsItsSuctionFlowTarget...` | C | pump with the water setting scaled to 500 kPa on methane -> `Compressor(0.001, 3, 1)`; asserts the work formula too | TARGET, q 5.5917822e-4 kg/s, head 98704.6 Pa, work 9.870 J (`q v head`) | TARGET, same q, head 98703.8 Pa, work 6.891 J (isothermal, = formula to 1e-6) |
| `FlowControlTest.naturalFlowIsLimited...` | C | `Pump(0.001, 500 kPa)`, `Pump(0.001, 100 kPa)` -> `Compressor(0.001, 3, 1)` both | natural TARGET, head -198596 Pa, work 0; shutoff CLOSED, head 698675 Pa | identical doubles (natural, shutoff) |
| `NetworkRegimeTest.pumpShutoffUsesAddedPressure...` | C (adjacent) | scaled pump -> `Compressor(0.001, 1.5, 1)` (1.5 x 1 MPa = the 500 kPa bracket), recycle `Compressor(0.001, 1.5, 1)` | 1.49 MPa: TARGET, q 0.00961700339 kg/s; 1.51 MPa: CLOSED, 0; recycle HEAD_LIMIT, q 9.66e-4 / 4.36e-4, work 0.485 J | the bracket's doubles identical; recycle TARGET, q 1.443e-3 / 6.51e-4, work 1.740 J |
| `NetworkRegimeTest.cancellationCannotLeave...` (sonic part) | C (adjacent) | scaled pump -> `Compressor(1, 6, 1)` | PUMP_VELOCITY_LIMIT, q 0.00766011915 kg/s, head 500000 Pa | PUMP_VELOCITY_LIMIT, same q, head 506625 Pa (the ratio limit, 5 x 101325) |
| `PumpRiseScalingTest.aGasTransferBetweenClosedTanksClosesWithinASecond` | C, restated | `Pump(0.01, 500 kPa)` -> `Compressor(0.1, 1.05, 1)`; the 575 Pa scaled-limit assertion becomes the ratio limit (3 Pa, the old tolerance), discharge = r_max P_s (3 Pa), the held difference (0.05 Pa, as before), "a fraction of a kelvin" -> "a few kelvin" cooler, the discharge warmer, work > 0 | closed at 0.30 s, suction 297.909 K 101038.28 Pa, discharge 298.391 K 101612.02 Pa, rise 573.74 Pa against 574.97 | closed at 0.35 s, suction 296.069 K 98866.03 Pa, discharge 300.233 K 103809.29 Pa, rise 4943.253 against 4943.302 Pa, work 63.1 J |
| `PumpRiseScalingTest.aDomainRefusalIsItsOwnRejectionKey...` | C | same compressor; domain narrowed to 298.05 K | fails on Nitrogen < 298.05 K at node 1 | same (the suction cools past the floor sooner) |
| `BackwardEulerStepTest.pumpWorkIncludesAllStagesAndElevationEnergy` | C | scaled pump (eff 0.8) -> `Compressor(0.001, 3, 0.8)` | work 24.71 J, ledger exact | work 17.25 J, ledger to 1e-5 J (same assertion) |
| `SharedSourceDepletionQualificationTest.excessivePumpRequest...` | C | scaled pump -> `Compressor(target, 3, 0.8)` | P24: adequate 9.7530691e-5 kg/s, work 0.609 J; insufficient: 7 steps, 14.29 % left, 247.97 K, 30348 Pa | adequate same flow, work 0.497 J; insufficient: 6 steps, 15.51 % left, 255.10 K, 33903 Pa (both "bounded-feasible-transfer") |
| `TransientQualificationTest...` "pump against pressure" | C | `Pump(0.001, 500 kPa)` (575 Pa on N2: dead-headed, zero flow) -> `Compressor(0.001, 3, 0.8)` (runs against the 2 bar tank) | M3: mass error 0.0, pressure 2.9e-10, 0.0000001 K | M3: mass error 2.49e-4 (bound 0.075), pressure 2.4e-7, 4.2e-5 K: the case now transfers |
| `PhysicalFluidTopologyTest.pumpControlUsesItsOutletDirection...` | C (adjacent) | `Kind.PUMP` -> `Kind.COMPRESSOR` (`Compressor(1e-5, 3, 1)`) | flow > 0 both orientations | flow > 0 both orientations |
| `IslandCertificateTest.deadHeadedLinesCertifyRest...` ("dead-headed pump") | C | `Pump(0.001, 100 kPa)` -> `Compressor(0.001, 3, 1)` (304 kPa against 800 kPa); asserts the device CLOSED (not refused) | certified at tick 300, drift 0 | certified at tick 300, drift 0, mode CLOSED |
| `IslandCertificateTest.aPumpHeatingItsLoopDoesNotCertify` | C | `Pump(0.005, 500 kPa)` -> `Compressor(0.005, 3, 1)` | work 0.63389 J per interval, not certified | work 0.63381 J per interval, not certified |
| `IslandCertificateTest.anIslandThatNeverCertifies...` ("pumped loop") | C | same fixture | bitwise on/off | bitwise on/off |
| `FluidPumpedFillLineTest.aGasTransferClosesAtThePumpsScaledLimitAndCertifies` | C, restated | "RUPR" -> "RCPR", `Compressor(0.01, 1.01, 1)` (the lowest ratio of plan Appendix B, 1013 Pa at 1 atm, near the pump's 575 Pa); the scaled-limit assertion becomes discharge = 1.01 P_s within the old 3 Pa | [101039.1 Pa 297.91 K] [101611.6 Pa 298.39 K], 3 jobs, certified | [100823.3 Pa 297.73 K] [101828.7 Pa 298.57 K], 3 jobs, certified; landing -2.8 Pa (**inside the 3 Pa bound by 0.2 Pa**, section 11 item 5) |
| `FluidThermoDomainHoldTest.aReproducibleDomainFailureHolds...` | C | "RUPR" -> "RCPR", `Compressor(0.01, 1.05, 1)`, node name "compressor at 1, 64, 0" | HELD, Nitrogen 298.04999999999995 K in reservoir at 0, 64, 0 | the same status text |
| `VelocityClampTest.threePhaseClamping...` pump arm | R | asserts `INLET_WRONG_PHASE`, flow 0, work 0, supply share above 2 %; the passive and valve arms unchanged | PUMP_VELOCITY_LIMIT, q 0.00869550 kg/s, work 0.437 J (a pump moving 34.2 % vapour) | INLET_WRONG_PHASE, 0.0, 0.0 |
| `PumpJunctionStartupTest` crude half | B (adjacent) | one flashTP confirmed the crude two-phase at 298.15 K / 1 atm (**11.15 % vapour by volume**; bubble point **117.8 kPa**; WP0's estimate 1.2 atm); generator and receiver to 150 kPa, and asserted liquid | 1 atm, q 0.0765058 kg/s (a pump moving 11 % vapour) | 150 kPa, q 0.0860874 kg/s, TARGET; water half unchanged (q 9.96000062 kg/s, work 178.167 J, identical) |
| `FluidFallbackQualificationTest` "wet crude pump" | B | the pump's generator gives water (liquid at 350 K, 149 kPa) into the three-phase wet-crude tank; target 1e-4 -> 2e-6 m3/s: water is not the tank's composition and 15 s at 1e-4 m3/s (about 80 mol into about 300) leaves the anchor's 1 % composition trust region (measured: "Outside the last full solution's trust region" 153 times, then HELD) | M4: pressure 9.0e-12, 4.7e-10 K | M4: pressure 3.0e-7, 1.6e-5 K, PASS |
| GameTest `FluidPumpStartupGameTests.heatedCrudePumpStartsIntoNitrogenReservoir` | B (source edit, not run here) | generator 350 K 101325 Pa -> 350 K **1 MPa** (the crude is 76.4 % vapour by volume at 350 K / 1 atm, bubble point **226.0 kPa** by flash; WP0's estimate 0.3 MPa) | - | compiles (G4); to be run on the owner's lane |

Unchanged (U) by the migration and green: `PhysicalFluidTopologyTest.dimensionsDeadLegs...` and `.zeroStoragePumpBypass...`, `FilterBlockLineIslandTest`, `ElevatedBlockLineIslandTest`, `SolidRuntimeTest`, the five water `FluidPumpedFillLineTest` methods, `FluidCheckpointCodecTest`, `PhysicalRegistryTest`, `WorldTopologyLedgerTest`, `TopologyCompilerTest.pumpOnlyBypass...`, `LevelHeadTest`'s pump fixtures, and the water half of `PumpJunctionStartupTest` (identical doubles).

### 10. Gates

JDK OpenJDK 21.0.10 (container); `JAVA_TOOL_OPTIONS` as set; one Gradle invocation at a time; no dev client. Logs `tools/phase-ports-probes/wp3-wp4/logs/`.

| # | command (from `/home/user/CreateChemE`) | result |
|---|---|---|
| (a) | `LIB=<jars> OUT=<dir> REPO=/home/user/CreateChemE bash tools/cloud-science-harness/harness.sh all` (`01-harness-all-wp34.log`) | science 223/223 (211 + 12 new), runtime 238/238 (237 + 1) with **the 33 junction lines identical** to its reference, adjacent 39/39 (38 + the new `PhysicalFluidTopologyTest` method, which the adjacent patterns select), chain-100 0.000e+00. No exclusion-list change (the new tests need no Minecraft class) and no stub change (`network.FluidNetwork` is a compile-only stand-in there) |
| G1 | `./gradlew --no-configuration-cache --no-build-cache test --rerun --tests com.wormzjl.createcheme.science.fluid.* --tests com.wormzjl.createcheme.runtime.fluid.* --console=plain --continue` (`02-...`, 79 s) | **459/459** in 102 classes, 0 skipped: every one of the 446 names at `85c492f` plus 13 new, none missing (`02-test-names-wp34.txt`); the 33 junction lines (`02-junction-lines-wp34.txt`) identical to `d10/logs/02-junction-lines-d10.txt` apart from ms/bytes/allocatedMB; **MIXED_GAS_COST 286** Newton solves (569 ms, 135.1 MB, single run) |
| G2 | `./gradlew --no-configuration-cache fluidSolverRegression -PfluidRegressionMode=exact --console=plain` (`03-...`) | **0.000e+00** on state/moles, temperature, phase fraction and flow; 3 accepted / 0 rejected, 4 Newton solves, 29 iterations |
| G3 | the adjacent selection (WP1 G3 command; `04-...`) | **39/39** in 7 classes: the 38 plus `PhysicalFluidTopologyTest.aCompressorConnectsAlongItsAxisOnly...`; the moves in it are `NetworkRegimeTest` (two C migrations), `PumpJunctionStartupTest` (crude re-baseline) and `PhysicalFluidTopologyTest` (orientation test on a compressor), section 9 |
| G3' | `./gradlew ... test --rerun --tests com.wormzjl.createcheme.runtime.Fluid*` (`07-...`; these classes are in `fluidRuntimeTest` but not in G1's selection) | 17/17 in 4 classes (`FluidPumpedFillLineTest`, `FluidThermoDomainHoldTest`, `FluidFallbackQualificationTest`, `FluidIslandCommandTest`) |
| G4 | `./gradlew --no-configuration-cache compileFluidGameTestJava compileMcpCompatJava --rerun --console=plain` (`05-...`) | **BUILD SUCCESSFUL**, both tasks executed (the edited crude GameTest compiles) |

Fixture lines of G1: `06-fixture-lines-wp34.txt`. The harness runs of the migrated classes and of the new fixtures while they were written: `08-*`, `09-*`.

### 11. Decisions recorded, findings, options

`DECISION_LOG.md` "WP3/WP4 defaults": A37-A45. Findings and options for the owner:

1. **A37 (the port supply at the target over the slice)** answers D11 open item 6 by the draw, not the leading phase. It refuses a pump whose bottom port holds less liquid than 98 % of one slice of its target, leaving up to that much liquid in the tank (fixture 5 at 2.99 slices drains to 9e-10 kg only because the third slice's draw was 99 % water). Options: (a) keep; (b) the leading phase (drains to the last drop, then sucks gas for a slice; a condensate trace reads liquid); (c) the draw over a fixed reference time instead of the slice.
2. **The refusal uses the slice duration** in the draw, so a 0.1 s cadence and a 5 s cadence decide on different draws near the end of a tank (both correct for their slice); the GUI's reason (WP5) must pass the committed interval's `advancedSeconds()` to reproduce the solver's text.
3. **Fixture 4, "bit for bit as without the refused line":** bitwise against the closed-device island at both cadences and against the line alone at 5 s; at 0.1 s the line alone differs by up to 10 ulp because a closed device's head unknown enters the same Newton, exactly as a pre-existing closed compressor does. Options: (a) accept (the refusal adds nothing of its own); (b) drop a refused or closed device's head unknown and rows from the Newton (a structural change to the closed-device path, not bitwise for existing closed devices).
4. **Compressor work at long steps**: the 5 s transfer books 3254 J against 2871 J at 0.1 s (+13 %): backward Euler states each step's power at its end-state head, above the ramp's average. The ledger is exact either way (the booked work is the energy the discharge receives); the discharge ends 0.21 K warmer. The pump's `q v head` has the same first-order bias. Options: (a) accept (D5); (b) book the work at the mean of the start and end heads (not first-law inconsistent, but a change to the pump's booking too).
5. **The RCPR landing is 2.8 Pa against a 3 Pa bound** (the pump's old tolerance, kept): the mover closes on the first step whose head-limit pass lands inside the band and the closed re-solve returns to that step's start state (the pump's 8.8 (c) fixed point); at 5 s slices the landing is the start state's margin. Stable on this JDK; a different JDK's roundoff moves it by far less than 0.2 Pa. Options: accept, or state the bound relative to the rise.
6. **The two C tests that were trivially dead-headed** (`TransientQualificationTest` "pump against pressure": the pump's 575 Pa could not open against 2 bar) now transfer (ratio 3): a stronger test, recorded as the migration's choice; `r_max = 1.5` would keep the dead-headed case.
7. **FluidFallbackQualificationTest "wet crude pump"** now pumps water at 2 mL/s into the wet-crude tank (section 9). A liquid crude at 350 K needs more than 226 kPa, above the case's 150 kPa tank, so the pump would no longer push against pressure; option: a separate case with a crude generator at 300 kPa.

### 12. Open items for WP5

- **Runtime sites** (compile-level only here): `FluidWorldAuthority.java:160` (placement control: `COMPRESSOR` would get `Passive` and the device constructor refuses it; plan Appendix B default `Compressor(0.05, 3, 1)`), `:294` (the displayed limit: pump only, the compressor's `(r_max - 1) P_s` to add; the `INLET_WRONG_PHASE` endpoint mode is appended to the status as a mode name today), `network/FluidNetwork.java:50-51` (`Controls.from`: a compressor falls to the defaults; the ratio field), `:211-221` (edit switch: no `COMPRESSOR` case), `FluidDeviceScreen` (`case PUMP` fields; a ratio field with the plan's 1.01-10 bounds), `FluidDeviceBlock.java:56` (axis-only connection for the compressor block), `ModBlocks` (no compressor block yet), `FluidPacketGameTests` (the block list).
- **Reason strings**: `InletPhase.reason(model, committedGraph, edge, committed.advancedSeconds())` for an edge whose endpoint mode is `INLET_WRONG_PHASE`; map the node id to the device's name.
- **Codec**: control tag 3 for `Compressor` (target, ratio, efficiency) in the registration and topology writers/readers and the JSON `decodeGraph` ("compressor"), with the format 5 -> 6 bump; the `INLET_WRONG_PHASE` ordinal (9) and `Kind.COMPRESSOR` (7) already round-trip by ordinal.
- The certificate digest has its compressor case; its header stays `-graph-identity-2`.

### 13. Material

`tools/phase-ports-probes/wp3-wp4/`: `src/CrudeFlashProbe.java` (the crude's vapour share and bubble point, `results/crude-flash.txt`), `src/Wp34Probe.java` (fixture 8 fill rates, `results/fixture8-fill-rates.txt`; fixture 4 against the line alone, a passive closed line and a closed compressor, `results/fixture4-bitwise-variants-0.1s.txt`), `src/MigrationValueProbe.java` (section 9's old/new values; the HEAD variant is the same file with `NEW?new FlowControl.Compressor(...):` removed, `sed -E 's/NEW\?new FlowControl\.Compressor\([^)]*\):/ /g'`), `results/` (values, the P24/M3/M4 reports old and new), `logs/` (gates 01-05 and 07, fixture lines 06, harness runs 08-09).

## WP5: runtime mapping - faces to ports, compressor block, format 6, GUI (2026-09-26)

- Author: Claude (Opus 5.5), `/home/user/CreateChemE` (cloud container), branch `claude/phase-ports-compressor`, base `5a375ea` (WP3+WP4 and their tools commit). Code commit `9744a45` (`WIP phase-ports WP5: faces to ports, compressor block, format 6, GUI`); tools and documents in the commit after it. Not merged, not pushed.
- Scope: plan section 4 (4.1-4.4) as amended by D11 and WP3/WP4 (the "WP5 items" of section "WP3 + WP4", 12), the three GameTests of plan section 6, the four physical-topology re-baselines of Appendix C.5 item 5, the level-head review's fixture 6.
- Science not touched, except one visibility change: `PassiveStepSolver.LEVEL_HEAD_HEIGHT` is `public` (was package-private) so the tank page and the re-baselined runtime tests state the D9 head with the solver's constant. The tank lines need no new result field: the committed result already exposes what each connection drew, phase by phase, in its pipe transfers (`PipeTransfer.sample` samples every drawn phase since D11), computed by the solver.

### 1. What changed, per file

| file | change |
|---|---|
| `runtime/fluid/PhysicalFluidTopology.java` | `compile` records, for every link with a RESERVOIR end, the face as the direction from the tank to its neighbour (`tankFaces`, keyed by link id); `port(device, face)` maps UP -> VAPOR, DOWN -> LIQUID, any horizontal face -> BULK, and every non-tank end to BULK; each run's first and last segment links give its two ends' ports, carried through the `fromEnd ? b : a` swap (`withPorts(fromEnd ? endPort : startPort, fromEnd ? startPort : endPort)`); the two halves of a doubly actuated run get their outer end's port and a BULK middle; the filter's positive-side remap rebuilds a pipe with its ports (it used the 7-argument constructor, which would have reset them to BULK). |
| `runtime/fluid/FluidWorldAuthority.java` | `place`: `COMPRESSOR -> Compressor(0.05, 3.0, 1)` (plan Appendix B). `view`: a mover's status line gains, when its endpoint mode is `INLET_WRONG_PHASE`, the typed reason of `InletPhase` with the walk's end node named as a device (`inletReason`, below), and otherwise its limit on the suction stream through `Mover.riseLimit` (pump: unchanged text and value; compressor: "(limit N Pa at this suction)" = `(r_max - 1) P_s`); a tank's view carries its outlet lines (`TankOutlets.of`). |
| `runtime/fluid/TankOutlets.java` | new: a tank's outlets from the committed graph and the last interval's result (section 3). |
| `runtime/fluid/FluidView.java` | new component `outlets` (a list of `Outlet(pipeId, port, massFlow, moleFlow, drawn, head)`, at most 6, validated), `withOutlets`; every existing constructor passes an empty list. |
| `network/FluidNetwork.java` | `PROTOCOL` fluid-6 -> **fluid-7**; `Controls` gains `maximumPressureRatio` as its last component (bounds 1.01-10, `MINIMUM/MAXIMUM/DEFAULT_PRESSURE_RATIO`; the 8- and 7-argument constructors pass the default 3.0); `Controls.from` reads a mover's target (pump or compressor) and a compressor's ratio; the edit switch builds `Compressor(volumeFlow, ratio, 1)` for a COMPRESSOR (efficiency 1, as the pump's edit sets it); `MenuData` refuses outlets on a device that is not a tank. |
| `client/gui/screens/inventory/FluidDeviceScreen.java` | COMPRESSOR case beside PUMP: "Suction flow (m3/s)" (`flow_input`) and "Max pressure ratio (out/in)" (`ratio_input`); `apply` sends the ratio; the compressor's rail shows last flow, pressure change and "Discharge / suction pressure" (the suction junction's pressure plus the head, over it: the quantity the ratio limits); a status containing "ERROR" or "HELD" is drawn in the warning colour (the pipe rail's existing rule, now on every device's status line); the tank rail lists its connections by face (section 3). |
| `runtime/fluid/FluidCheckpointCodec.java` | `VERSION` 5 -> **6**; control tag 3 for `Compressor` (target, ratio, efficiency) in the registration and topology writers and readers; two port bytes per pipe after its control; JSON graph: pipes require `firstPort`/`secondPort`, a `"compressor"` control reads `target`, `limit` (the ratio) and `efficiency`; javadoc. `FluidCheckpointStore`, `FluidSavedData`: javadoc "format 6". |
| `world/level/block/FluidDeviceBlock.java` | `face`: a COMPRESSOR connects along its facing axis only, like the pump (`:56`). |
| `registry/ModBlocks.java`, `ModItems.java`, `ModBlockEntities.java`, `CreateChemE.java` | `fluid_compressor` block (the `fluid` factory: metal, no occlusion, piston-proof), its block item, the device block entity's valid blocks, the creative tab entry after the pump. |
| resources | `blockstates/fluid_compressor.json` (the pump's six facings), `models/block/fluid_compressor.json` (parent `createcheme:block/fluid_pump`, textures overridden with vanilla `light_gray_concrete` body and particle and `oxidized_copper` motor: no mod texture exists, the pump itself uses vanilla textures), `models/item/fluid_compressor.json`, `loot_table/blocks/fluid_compressor.json`, the pickaxe and Create `non_movable` tags, lang (block name "Process Gas Compressor" and the GUI keys of section 3). |
| tests | `PhysicalFluidTopologyTest` +6 (section 7), new `TankOutletsTest` (2), `FluidCheckpointCodecTest` +3, `FluidPacketCodecTest` +2 (protocol assertion fluid-7, test name kept), `FluidCheckpointFormatTest` (format 6 expectation), `McpGameplayRegressionTest` (the archived capture predates ports: the test adds BULK ports as it already adds blocked masks and filters), the four re-baselines (section 6). GameTests: new `FluidPhasePortGameTests` (3), `FluidPacketGameTests` block list gains the compressor. |

The cloud harness needed no stub or exclusion-list change: its `FluidNetwork` and `FluidDeviceBlock` stand-ins only carry what `FluidWorldAuthority` names (`MenuData(view)`, `snapshot`, `deliver`; `kind()`), which did not change, and every new test is Minecraft-free except the two `FluidPacketCodecTest` methods, whose class is already excluded.

### 2. The face rule as compiled (plan 4.1, A1)

- Only a tank (RESERVOIR) end carries a port; generators, voids, junctions, actuator and filter nodes stay BULK whatever face they are reached by (asserted: a generator above a tank's top pipe, voids below and beside).
- The face is the direction from the tank to its neighbour on the link the compiler creates at `:60-61` (a boundary link, so an actuator or filter directly on a tank face takes the same rule: asserted for a pump on the DOWN face in both orientations and both identity orders, and a filter on the UP face in both orientations).
- A pipe's ports are its run's end ports, swapped with the ends when the run is stated from its end (an actuator driving into the tank from the run's second end): the pump facing UP into a tank's bottom compiles to `pump junction -> tank` with the LIQUID port at the second end, whether the tank's identity is lower (swap branch) or higher (no swap).
- Two tanks touching never connect (unchanged); a pipe block between two stacked tanks joins the upper tank's LIQUID port to the lower tank's VAPOR port.
- The rate-solve PORT copy keeps the ports (WP1 rule, `PassiveIntervalSolver.portRate`/`coldRateSeed` reuse `graph.pipes()`); the certificate digest carries them (section 5).

### 3. GUI (plan 4.3, engine-owned presentation)

Everything below is built in `FluidWorldAuthority.view()` from the island's cached committed snapshot (graph and last result) on the engine's presentation schedule and sent only to subscribed menus; no solver, no flash: `InletPhase.supply` and `TankOutlets` read stored phase amounts (masses, volumes, molecular weights) and the solver's pipe transfers. Nothing is computed in a packet handler or on a menu open (an open replays the last published snapshot, unchanged).

**Mover status.** `... / INLET_WRONG_PHASE / ERROR: pump inlet not liquid (vapour 100.0 % by volume, from reservoir at -1015848, -59, -3401253)` (measured, GameTest 2); the compressor's text `ERROR: compressor inlet not gas (condensed ... % by mass | solids ... by volume, from <device>)`. The reason is `InletPhase.reason` on the committed graph (the state the next slice decides on) with the last interval's `advancedSeconds()` as the slice, and its "node N" replaced by the device label the status lines already use (`deviceLabel`: "<kind> at x, y, z", "pipe junction N" for a compiled junction). A refused mover shows no limit. Otherwise: pump `(limit N Pa on this fluid)` (the same value as before WP5: its setting scaled by the suction junction's density), compressor `(limit N Pa at this suction)` (measured `FULL / CLOSED (limit 105318 Pa at this suction)` at a 52659 Pa suction, ratio 3). Warning colour for any status containing "ERROR" or "HELD".

**Compressor page.** Fields "Suction flow (m³/s)" (`flow_input`, existing) and "Max pressure ratio (out/in)" (`ratio_input`, new; 1.01-10: outside it the screen's own `Controls` construction fails and it shows its existing "Check numeric inputs ..." message; a hand-made packet is refused by the server with "The pressure ratio must lie between 1.01 and 10.0"); rail: temperature and pressure of its suction junction, "Last flow", "Pressure change" (the head), "Discharge / suction pressure" (new key `pressure_ratio`).

**Tank page, "Connections by face"** (rail, below volume and mass; hint on hover). One entry per connection with an end at the tank, two lines each:

| port | first line | second line (last interval) |
|---|---|---|
| VAPOR (UP) | "Top: gas outlet" | "drawing gas · N kg/h" or "drawing liquid (overflow) · N kg/h" |
| BULK (sides) | "Side: mixed outlet" | "drawing all phases together · N kg/h" |
| LIQUID (DOWN) | "Bottom: liquid outlet (head X kPa)" (the head when the tank holds condensed mass, else "Bottom: liquid outlet") | "drawing water · N kg/h", "drawing oil · N kg/h" or "drawing gas · N kg/h" |
| any, net inflow | as above | "receiving · N kg/h" |
| any, no flow | as above | "no flow" |

Flows follow the screen's mass/mole toggle (kg/h or kmol/h). "The phase drawn" is the phase that carried the most mass in what the connection moved out of the tank over the last interval (`TankOutlets.leading`, on the solver's pipe transfer: phase moles times molecular weights); the flows are the transfer's net out of the tank over `advancedSeconds` (equal to the result's mean mass flow with the tank end's sign, asserted to 1e-9); the head is `g m_c H / V` on the committed state (D9). The hint (`outlets.hint`) carries the D11 rule and the plan's wording: "Top outlets draw gas first, then the lighter and the heavier liquid. Bottom outlets draw the heaviest liquid first, then the lighter liquid, then gas. Side outlets draw all phases together. Inflow through any face is not distinguished." No player-facing string said "bulk withdrawal" before WP5 (grep of `src/main`, lang included), so nothing was renamed; the phrase exists only in this hint.

New lang keys: `block.createcheme.fluid_compressor`, `gui.createcheme.fluid.ratio_input`, `.pressure_ratio`, `.outlets`, `.outlets.hint`, `.outlet.top`, `.outlet.side`, `.outlet.bottom`, `.outlet.bottom_head`, `.outlet.drawing_gas`, `.outlet.drawing_overflow`, `.outlet.drawing_water`, `.outlet.drawing_oil`, `.outlet.drawing_all`, `.outlet.receiving`, `.outlet.idle`, `.outlet.more`.

### 4. Checkpoint format 6 and wire fluid-7 (plan 4.4)

- Core record `FluidFormat = 6`; any other format is refused with the existing "Fluid checkpoint format N cannot be read ... Create a fresh world for this development build." No migration, no absent-field default, no legacy test.
- Controls, in the world ledger's registration and in an island unit's topology: tag 0 passive, 1 pump (target, max added pressure, efficiency), 2 valve (target pressure), **3 compressor (target suction volume flow, max pressure ratio, efficiency)**, each `f64`; an unknown tag is refused ("unknown control" / "Unknown saved control").
- Island topology, per pipe: id `i64`, first and second node `varint`, section count and sections, the control, then **the first end's and the second end's `PhasePort` ordinal, one `u8` each** (0 BULK, 1 VAPOR, 2 LIQUID; out of range refused "unknown pipe port"). A port on a node that is not a tank is refused by the `PassiveNetwork` constructor. The new endpoint mode (`INLET_WRONG_PHASE`, ordinal 9) and `Kind.COMPRESSOR` (ordinal 7) persist through the existing ordinals (A43).
- JSON graph (archived captures): each pipe requires `"firstPort"` and `"secondPort"` (names); `"compressor"` reads `target`, `limit` (= the ratio) and `efficiency`.
- Wire: protocol `fluid-7`. `Controls` carries `maximumPressureRatio` (required: a payload without it deserialises to 0 and is refused); the live view carries `outlets` (required: a live payload without the list is refused).
- Measured round trips: an island with every port kind and a refused compressor after one interval (graph pipes, ports, controls, endpoint modes, flows, anchor; re-encoded byte for byte); a world-ledger compressor registration; the JSON shape with ports and a compressor, a pipe without ports refused, a LIQUID port on a generator end refused.

### 5. Certificate digest (A12, WP4): verified, no change

`IslandCertificate.graphIdentity` digests, per pipe, the control (`"compressor"`, target, ratio, efficiency, since WP4) and one integer per end for the port ordinal (since WP1) under the header `createcheme-certificate-graph-identity-2`. Compiled islands with a vertical tank link now digest non-BULK ports, so their certificates differ from a 0.6.x world's; no compatibility work (a stale certificate is discarded on load, A12).

### 6. Physical-topology re-baselines (Appendix C.5 item 5) and the moves classified

The four assertions now state the tank's **bottom-port** pressure, `P + g m_c H / V`, against the same expectation and tolerance (forced by D9 with A1: the line enters through the tank's DOWN face):

| assertion | old headspace (5a375ea) | new headspace | level head | new bottom `P + h` | expectation |
|---|---|---|---|---|---|
| `ElevatedBlockLineIslandTest` rising line (`:234`, was `:216`) | 360918.3081 Pa | 353969.4046 Pa | 6948.90 Pa (708.59 kg) | 360918.3068 Pa | 400000 - rho g 4 = 360918.3068 Pa, 1e-5 P |
| rising filter line (`:240`, was `:220`) | 360918.3787 Pa | 353969.7255 Pa | 6948.90 Pa | 360918.6286 Pa | same |
| rising pump line (`:251`, was `:229`) | 861062.9819 Pa | 852452.8285 Pa | 8610.21 Pa (878.0 kg) | 861063.0384 Pa | P + 500 kPa rho_s/rho_ref - rho g 4, 1e-4 (P + head) |
| `DeadHeadedLineIslandTest` raised generator (`:252`, was `:243`) | 360918.3081 Pa | 353969.4046 Pa | 6948.90 Pa | 360918.3068 Pa | 400000 - rho g 4, 1e-5 P |

The headspace rests 6.95 kPa (8.61 kPa pumped) lower: `LEVEL_HEAD_REVIEW.md` estimated about 7 kPa; the D9 review's code-built equivalent (charged at 150 kPa) measured -5.61 kPa. The lowered generator's rest (`DeadHeadedLineIslandTest`, not asserted on pressure) moves with it (353969.4046 Pa).

**Other moves (classified before touching anything).** A full diff of every printed line of the cloud harness on `5a375ea` against WP5 (`logs/00` against `logs/01`) shows, besides the four lines above, the new tests' lines, timings and one reordered `HashMap` print: the two **falling lines** (tank reached through its UP face, a VAPOR port, inflow only) land at 439081.69755 Pa instead of 439081.69316 Pa (`ElevatedBlockLineIslandTest`, +4.4 mPa, 1.0e-8 relative) and 140395.02218 Pa instead of 140395.02229 Pa (`DeadHeadedLineIslandTest` "falling plain", -0.11 mPa). Their assertions (1e-5 P) pass unchanged. Cause measured with a one-line probe (`src/up-face-bulk-probe.patch`, `-Dprobe.upBulk=true` compiles UP faces to BULK): both return to the old doubles exactly (`logs/04`). So it is the VAPOR port, not another WP5 change: at the tank end the static readers (column choice, the sign tests of the start closures) read the port's leading phase, the gas, where a BULK end read the bulk (A28, D11), and the rest the line lands on moves at the Newton's landing resolution. WP0's "inflow only, nothing changes" held for the outcome, not to the last digits. Not touched.

### 7. New unit fixtures (measured; Gradle G1)

| test | fixture | measured |
|---|---|---|
| `PhysicalFluidTopologyTest.everyFaceOfATankCompilesToItsPortAndOnlyTheTankEndCarriesOne` | a tank with a pipe on each of its six faces to a void (a generator above) | UP VAPOR, DOWN LIQUID, four BULK; every far end BULK; the island solves 0.05 s |
| `.aStackOfTwoTanksJoinsTheUpperTanksBottomToTheLowerTanksTop` | tank - pipe - tank vertically, both identity orders; two touching tanks | LIQUID at the upper end, VAPOR at the lower; touching tanks: no pipe |
| `.aPumpOnATanksBottomFaceTakesTheBottomPortInEitherOrientationAndIdentityOrder` | pump under a tank facing DOWN (suction) and UP (discharge), tank id below and above the pump's | suction run LIQUID at the tank; the discharge connection `pump junction -> tank` with LIQUID second port in both identity orders |
| `.aFilterOnATanksTopFaceKeepsTheTopPortInBothOrientations` | tank - filter (UP / DOWN) - pipe - void | VAPOR at the tank in both (the DOWN-facing filter's remapped pipe keeps it), filter edge BULK, the tank vents |
| `.aCompiledStackPassesTheUpperTanksWaterDownAndItsGasUp` | 0.3 m3 water under N2 at 1 atm over a N2 tank at 1 atm, one pipe block, 40 x 5 s | first slice: the bottom outlet draws water only (vapour and oil moles exactly 0), the lower top receives, tank lines "drawing water" / "receiving" with flows equal to the result's; every component conserved to 1e-10; end: all 292.43 kg of water in the lower tank, headspaces 104648.26 / 104671.22 Pa (one gas column apart). The water goes down and the gas up the same pipe (the lower tank's top outlet draws gas back into the upper tank's bottom, inflow undistinguished, D3), so a head-only stack of two tanks does not exist: the gas path opens first |
| `.aTankOverAVoidDrainsOnItsLevelHeadAloneAndRests` (level-head review fixture 6) | the same tank on a pipe block over a void held at `P + rho g 2 + h/2` = 122293.98 Pa (column 19535.01 Pa, head 2867.95 Pa), 40 x 5 s | the column alone cannot lift the headspace to the void; the head drains 9.73 kg (first slice 1.888 kg/s, water only); rest CLOSED with the tank still holding water (head 2772.57 Pa, headspace 99986.42 Pa) at `P + h + rho g 2 - P_void` = -2.2e-8 Pa |
| `TankOutletsTest.aTankWithTopSideAndBottomOutletsListsWhatEachDrew` | plan section 6 scenario 1 from blocks: 0.5 m3 water under N2 at 120 kPa, top / east / bottom pipes to 1 atm voids, one 5 s slice | top VAPOR 0.0175 kg/s drawing gas (no condensed moles), side BULK 0.597 kg/s (both phases), bottom LIQUID 17.94 kg/s drawing water (no vapour moles), head 3912.65 Pa = `g m_c H/V` exactly; flows = the result's mean flows to 1e-9; no interval: all "no flow" |
| `TankOutletsTest.theLeadingPhaseIsTheOneThatCarriedTheMostMass` | synthetic transfers | 0.028 kg gas + 0.18 kg water -> water; + 0.018 kg water -> gas; nothing -> none |
| `FluidCheckpointCodecTest` (3), `FluidPacketCodecTest` (2) | section 4 | as stated there; ratio 1.009, 10.01, NaN, 0 refused, 1.01/3/10 accepted |

### 8. GameTests (plan section 6; `FluidPhasePortGameTests`, run here)

`./gradlew --no-configuration-cache runFluidGameTestServer -PfluidGameTestRunId=wp5-20260926 --console=plain` (fresh world, `logs/06`) and, after adding the measurement prints, again with `-PfluidGameTestRunId=wp5-20260926-final` (fresh world, `logs/10`, summary `logs/12`): **All 33 required tests passed** both times (the 30 existing plus the 3 new; BUILD SUCCESSFUL, 41 s the second time). The NeoForge server runtime set up with the container's network; the only environment messages are the usual missing `server.properties` and the refused Yggdrasil key fetch (`api.minecraftservices.com` not on the allowlist), neither of which a GameTest needs.

| GameTest | measured (final run) |
|---|---|
| `aTanksTopSideAndBottomOutletsEachCarryTheirPhase` | tank filled from a 130 kPa water generator on its west side, generator removed, top/east/bottom outlets placed; first interval with flow: tank 124302.5 Pa, 176.1 kg; top VAPOR 0.273 kg/s drawing gas (pipe carries gas only), side BULK 8.95 kg/s (both phases), bottom LIQUID 29.41 kg/s drawing water (pipe carries water only), head 1715.3 Pa; the menu snapshot carries the same lines |
| `aPumpOnAGasTankIsRefusedWithItsReasonOnItsStatusLine` | `SOLVING / INLET_WRONG_PHASE / ERROR: pump inlet not liquid (vapour 100.0 % by volume, from reservoir at -1015848, -59, -3401253)`, flow 0, no limit shown, the menu snapshot carries it |
| `aCompressorMovesNitrogenToItsRatioLimitAndCloses` | default `Compressor(0.05, 3, 1)` placed; modes seen `PUMP_TARGET` then `CLOSED`; closed at suction 52659.0 Pa / 247.85 K, discharge 157976.3 Pa / 338.04 K, ratio 2.99999 (asserted within 3e-3); status `FULL / CLOSED (limit 105318 Pa at this suction)` |
| `FluidPacketGameTests.createAndVanillaMovementRejectEveryFluidBlock` | the compressor block is refused by Create's movement checks and by pistons (tags and `pushReaction` hold) |

The in-game MCP client check cannot run in this container (no display): it is the owner's item (section 11). Client-only resources (blockstate, block and item models, lang) are not parsed by a server; `build -x test` validated the resource pipeline (`processResources`, `jar`, `jarJar`, `verifyMaterialIndex`), and every new JSON file parses.

### 9. Gates

JDK OpenJDK 21.0.10 (container); `JAVA_TOOL_OPTIONS` as set; one Gradle invocation at a time; no dev client. Logs `tools/phase-ports-probes/wp5/logs/`.

| # | command (from `/home/user/CreateChemE`) | result |
|---|---|---|
| (pre) | `LIB=<jars> OUT=<dir> REPO=/home/user/CreateChemE bash tools/cloud-science-harness/harness.sh all` on `5a375ea` (`00-...`) | science 223/223, runtime 238/238 with the 33 junction lines identical, adjacent 39/39, chain-100 0.000e+00 |
| (a) | the same on the WP5 tree (`01-...`) | science 223/223, runtime **249/249** (238 + 11 new), adjacent 45/45, chain-100 0.000e+00, the 33 junction lines identical; no stub or exclusion change |
| G1 | `./gradlew --no-configuration-cache --no-build-cache test --rerun --tests com.wormzjl.createcheme.science.fluid.* --tests com.wormzjl.createcheme.runtime.fluid.* --console=plain --continue` (`02-...`, 78 s) | **472/472** in 103 classes, 0 skipped: all 459 names of WP3/WP4 plus 13 new, none missing (`02-test-names-wp5.txt`), the four re-baselined assertions green; the 33 junction lines (`02-junction-lines-wp5.txt`) identical to `d10/logs/02-junction-lines-d10.txt` apart from ms/bytes/allocatedMB; **MIXED_GAS_COST 286**; the codec and packet classes of gate (2) (`FluidCheckpointCodecTest`, `FluidCheckpointFormatTest`, `FluidCheckpointStoreTest`, `FluidPacketCodecTest`, `FluidDeviceSpecDomainTest`) are in this selection and green |
| G1' | `./gradlew ... test --rerun --tests com.wormzjl.createcheme.runtime.Fluid*` (`05-...`) | 17/17 in 4 classes |
| G2 | `./gradlew --no-configuration-cache fluidSolverRegression -PfluidRegressionMode=exact --console=plain` (`07-...`) | **0.000e+00** on state/moles, temperature, phase fraction and flow |
| G3 | the adjacent selection (WP1 G3 command; `08-...`) | **45/45** in 7 classes: the 39 plus the 6 new `PhysicalFluidTopologyTest` methods, which the adjacent patterns select |
| G4 | `./gradlew --no-configuration-cache compileFluidGameTestJava compileMcpCompatJava --rerun --console=plain` (`09-...`) | BUILD SUCCESSFUL, both tasks executed |
| G5 | the GameTest runs of section 8 (`06-...`, `10-...`, `12-...`) | **33/33** twice on fresh worlds |
| G6 | `./gradlew --no-configuration-cache build -x test --console=plain` (`11-...`) | BUILD SUCCESSFUL (`verifyMaterialIndex`, `processResources`, `jar`, `jarJar`, `sourcesJar`, `assemble`, `check`) |

Printed fixture lines of G1: `03-fixture-lines-wp5.txt`. The falling-line probe: `04-up-face-bulk-probe.txt`.

### 10. Decisions recorded, findings, options

`DECISION_LOG.md` "WP5 defaults": A46-A54. For the owner:

1. **What "drawing X" means** (A47): the phase that carried the most mass over the last interval. A top vent's overflow slice (fixture 3 of D11: 0.0047 kg gas with 7.83 kg water) reads "drawing liquid (overflow)". Alternatives: the first phase of the port's order that the draw contains (that slice would read "drawing gas"), or every drawn phase listed ("drawing gas and water"), which needs a second line or a longer one.
2. **The reason is stated on the committed state** (A48), which is the input of the next slice, not the state the last slice decided on (the snapshot does not keep the slice's start graph): its percentage can differ from the solver's own text by the slice's change; the refusal itself is the committed endpoint mode. Option: carry the refusal text in the interval result (a solver-side presentation field).
3. **Compressor efficiency** is reset to 1 by a GUI edit, as the pump's is (A50); a player cannot set it. Option: an efficiency field on both pages.
4. **A head-only stack of two tanks does not exist** (section 7): the lower tank's top outlet sends its gas up the same pipe the water comes down. Fixture 6 is therefore a tank over a void. The two-tank stack drains completely (water down, gas up), which is physical for a single connection (a bottle glugging), with no slug or counter-current limit in the model.
5. **Falling lines** move at the landing resolution (section 6); nothing to decide, recorded.

### 11. The owner's in-game check (plan section 6; langyo/minecraft-mod-mcp bridge, jar in `run/mods`, `.mcp.json` in the worktree root, fresh world)

Not runnable here (no display). On a fresh world:
1. **Tank outlets.** A tank filled with water from a generator (it is placed as a nitrogen charge), then a pipe on its top, one side and its bottom, each to a void: the tank page's "Connections by face" shows "Top: gas outlet / drawing gas", "Side: mixed outlet / drawing all phases together", "Bottom: liquid outlet (head X kPa) / drawing water", with flows; each pipe's inspection shows the phase ratio of what it carried (gas only at the top, water only at the bottom while water remains). Check the hint tooltip, the kg/h and kmol/h toggle, and that the compressor block and item render (light-grey body, oxidised-copper motor) with the name "Process Gas Compressor".
2. **Pump.** A pump on a tank's side facing away from it, into a pipe and a void: status in the warning colour, "INLET_WRONG_PHASE / ERROR: pump inlet not liquid (vapour 100.0 % by volume, from reservoir at x, y, z)". Move it to the tank's bottom face (the pump below the tank facing down) with water in the tank: it runs ("PUMP_TARGET (limit N Pa on this fluid)"), and when the water runs out it is refused with "vapour ... %" (D11: a dry bottom port feeds it gas).
3. **Compressor.** Two nitrogen tanks with a compressor between them facing the second: the page shows the two fields (0.05 m³/s, ratio 3); it runs on PUMP_TARGET, the rail's "Discharge / suction pressure" rises to 3.00 and it closes (CLOSED, "(limit N Pa at this suction)"); set the ratio to 1.5, then to 10.5 (not sent: "Check numeric inputs and composition; ..."). Then feed it water (a water generator on its suction side): refused, "ERROR: compressor inlet not gas (condensed ... % by mass, from ...)".

### 12. Open items for WP6

- The owner's in-game check (section 11) and the MCP screenshots; the Windows lane for the gates and GameTests.
- Options of section 10.
- WP6's own list: tooling cleanup per AGENTS.md (the probe patch here is a measurement tool, stored under `tools/phase-ports-probes/wp5/src/`, never committed to `src/`), `CHANGELOG.md` `[Unreleased]` line, INDEX rows, the D11 open items (humid venting below the water domain D14, recovered rejections at phase boundaries, WP1 condensed-stream builders no longer read), WP3/WP4 items 1-7.

### 13. Material

`tools/phase-ports-probes/wp5/`: `src/up-face-bulk-probe.patch` (against `9744a45`: `-Dprobe.upBulk=true` compiles UP faces to BULK; apply with `git apply`, run the harness `select` on `ElevatedBlockLineIslandTest` and `DeadHeadedLineIslandTest` with `HARNESS_JVM_OPTS="-Xmx2g -Dprobe.upBulk=true"`, `git checkout` the file; never commit it), `logs/` (gates `00`-`02`, `05`, `07`-`11`; fixture lines `03`; probe `04`; GameTest runs `06`, `10` and summary `12`).

## WP6: gates and close (2026-09-26)

- Author: Claude (Opus 5.5), `/home/user/CreateChemE` (cloud container, OpenJDK 21.0.10), branch `claude/phase-ports-compressor`, base `5f38474` (WP5 and its tools commit), tree clean at the start. Code commit `cb41860` (`WIP phase-ports WP6: cleanup and final gates`); tools, documents, `CHANGELOG.md` and this section in the commit after it (`Docs: phase-ports batch close, changelog entry, handoff`). Not merged, not pushed; `mod_version` not bumped (merge-time, the owner's).
- Scope: plan section 7 WP6 and section 6 "Cleanup rule"; AGENTS.md "Test classes and tools after a batch". Science not touched except the removal of code proven unreferenced ((b)); no tolerance and no assertion changed.
- Section order of this review: the sections are in commit order (D10 `07e7426` was committed before D9 `fdf3574`, the merge of D11, D12 and D13 before WP3), which is why D10 precedes D9; left as it is.

### (a) Prototype residue (tooling rule)

Checked `src/main`, `src/test`, `src/fluidGameTest`, `src/mcpCompat` (the batch's diff `c32acac..5f38474`, 67 files), `build.gradle`, `gradle.properties`:

- `git diff c32acac HEAD -- build.gradle gradle.properties` is **empty**: no Gradle switch or property served the batch's probes (every probe ran through the cloud harness or a scratch copy of the tree).
- Added lines of the batch under `src/main`: no `System.getProperty`, `Boolean.getBoolean`, `Integer.getInteger`, `System.getenv`, `System.out`/`System.err` or trace switch (the grep's only hits are `System.arraycopy` and the word "trace" in the D10 water trace and the solid trace fraction). Every instrumentation of the batch (the gate/Newton probe, the pass tracer, the site prints, the mutations, the up-face probe) stayed a patch in `tools/phase-ports-probes/` and was never committed.
- Test and GameTest prints: the new gate tests print one summary line per fixture (`PHASE_PORT_*`, `LEVEL_HEAD_*`, `JUNCTION_WATER_TRACE`, `VENT_GATE_RUN`, `INLET_PHASE_RUN`, `COMPRESSOR_RUN`, `EXTREME_TOPOLOGY*`, `TANK_OUTLET`, `STACK_*`), and `FluidPhasePortGameTests` prints one `PHASE_PORT_GAMETEST` line per device. **Kept**: they are the measured output of tests a gate runs (the convention of `MIXED_GAS_*` and `LIQUID_JUNCTION`), read by this review's tables; they are unconditional and switch nothing.
- **`ExtremeTopologyIslandTest.FULL_CASES_OPEN_DEFECT`** (false since D12, `418ca7e`): the reproduction branch it guarded was dead code. **Detached** (the recommended option): the flag, the branch (which asserted that the three full fixtures hold on their first interval in pass 0) and `capDrop` (used only by it) left the class; the test keeps its success branch, every oracle on the full fixtures at the default cap, and is renamed from `theFullCasesHoldOnTheirFirstIntervalOpenDefect` to `theFullCasesIntegrateAtTheDefaultCap` (the JUnit XML names of its three parameterised cases, `[1] GRID`, `[2] ALTERNATING`, `[3] ALTERNATING_BOTH_SIDES`, do not carry the method name, so the gate's test-name list is unchanged, (d)); the class and method javadoc keep the defect's history and point to the tools folder. The file as of `5f38474` and `reattach-open-defect-reproduction-against-cb41860.patch` are in `tools/phase-ports-probes/extreme/src/`.
- Nothing else of the kind: no other test flag, no `@Disabled`, no probe class in a tracked path (grep for `Probe`, `Trace`, `OPEN_DEFECT`, `@Disabled` in the batch's test files).

### (b) Dead code and stale text

| Item | Action |
|---|---|
| WP1 condensed-stream builders in `FluidThermodynamics` (D11 section 12 item 8): `liquidStream`, `liquidViscosity`, `liquidCarrierViscosity`, `liquidSpecificEnthalpy`, `liquidVelocityLimit` | **removed** (56 lines removed, 19 added: the javadoc restated): no reader in `src/main`, `src/test`, `src/fluidGameTest`, `src/mcpCompat` or `tools/cloud-science-harness/stubs` (each name grepped); the only other reader is the WP1 instrumented copy `tools/phase-ports-probes/wp1/src/instr/PassiveStepSolver.java`, which builds against the WP1 tree only. `liquidMass`, `liquidStreamVolume`, `liquidDensity`, `holdsLiquid` stay (the level head, `LevelHeadTest` and `PhasePortTest` read them). `PhaseStream`'s javadoc and the D11 phase block comment restated to the per-phase streams. Removed source and `reattach-condensed-stream-builders-against-cb41860.patch`: `tools/phase-ports-probes/wp6/removed/` |
| `PassiveStepSolver`: the capacity javadoc's "(WP2's throttle's `dt`)"; the structural-zero comment "Only for a bulk withdrawal"; `riseLimit`'s javadoc describing the pump only | corrected ("the solve's step"; "Only for a BULK end"; the mover's limit, a compressor's `(r_max - 1) P_s`, then the pump's text) |
| `InletPhase`: "a view (WP5) formats", "for WP5's view to show" (future tense after WP5) | restated to the device view (`FluidWorldAuthority`) that shows it |
| `PhasePortTest.phasePortsFeedJunctionsAndCloseTheLedger` javadoc: the dry-seeded junction defect "reported ... (WP1, open defect)" | restated: fixed by D10 (`JunctionWaterTraceTest`), the humid seeding kept as written |
| `LevelHeadTest` class javadoc: fixture 6 "waits for WP5's face-to-port compile" | points to `PhysicalFluidTopologyTest`'s tank over a void |
| grep of every source set for `INLET_EMPTY`, "bulk withdrawal", availability mask, `closedPorts`, `PHASE_PORT_OPEN`/`RESERVE`, `phi_open`, throttle-as-landing, "port closes", A9 | nothing else stale: `ScheduledTransfer`'s "Bulk withdrawal" is the module withdrawal (A6, still bulk); the "throttled" law in `edgeRows` is the velocity cap (pre-existing); test javadoc that names WP2's closure as superseded history (`PhasePortPriorityTest`, `LevelHeadTest` drain) is history and stays |
| `SolverDiagnostics` counters added by D13 | all three recorded by main code; nothing removed |

Main sources: 3 files; test sources: 3 files (`ExtremeTopologyIslandTest`, `PhasePortTest`, `LevelHeadTest`), comments and the detachment only.

### (c) Tool folders holding the batch's detached material

- `tools/phase-ports-probes/` (one subfolder per package: `wp1`, `wp2`, `d10`, `d9`, `extreme`, `vent`, `d11`, `d12`, `merge`, `wp3-wp4`, `wp5`, `wp6`). Every subfolder's README section states its purpose, the package it served, how to run it and, for code removed from a tracked path, the removing commit and the reattach patch (`extreme/`, `wp6/removed/`; WP2's closure removed by D11 `548a9bf` and TR-BDF2-era material are in git history). Added at WP6 from the session scratchpad: `wp1/src/xmlsummary.py` (the G1 summary every package used) and `cmp.py`, `wp2/src/scripts/` (WP2's summary and edit scripts), `d10/src/edit-d10.py`, `d9/src/run-base.sh`/`run-new.sh`, `d11/src/sel.sh` and `d11/src/instr/ConservativeTransport.java` (the unbacked-trace instrumentation), `wp3-wp4/src/values-old/` and `wp3-wp4/src/editing/`; scratch copies of whole trees, of `PassiveStepSolver` versions already in git and of harness builds were not copied.
- `tools/cloud-science-harness/` (the Minecraft-free runner, merged at `b83537a`): README updated with its use through the batch and the close numbers, and `investigation/libm-cbrt/` (the correctly rounded `cbrt` experiment behind the JDK-roundoff finding) added.
- `tools/INDEX.md`: both rows updated (phase-ports-probes Concluded 2026-09-26; cloud-science-harness in use as a fallback).
- Everything is tracked on the branch (the owner's "Cloud prep" `ea16150` made `documentation/` and `tools/` tracked; `logs/` folders force-added), so the merge itself carries them to `main`.

### (d) Gates after the cleanup

JDK OpenJDK 21.0.10 (container); `JAVA_TOOL_OPTIONS` as the environment sets it (proxy); one Gradle invocation at a time (an idle daemon only), no dev client. Logs `tools/phase-ports-probes/wp6/logs/`.

| # | command (from `/home/user/CreateChemE`; log) | result after the cleanup | before (WP5, `9744a45`/`5f38474`) |
|---|---|---|---|
| (a) | `./gradlew --no-configuration-cache --no-build-cache test --rerun --tests com.wormzjl.createcheme.science.fluid.* --tests com.wormzjl.createcheme.runtime.fluid.* --console=plain --continue` (`01-gradle-fluid-suite-wp6.log`, 1 min 18 s) | **472/472** in 103 classes, 0 skipped; the test-name list (`01-test-names-wp6.txt`) identical to `wp5/logs/02-test-names-wp5.txt` (normalised: the renamed extreme test's XML names are its parameter names); the 33 junction lines (`01-junction-lines-wp6.txt`) **identical** to `d10/logs/02-junction-lines-d10.txt` apart from ms/bytes/allocatedMB; **MIXED_GAS_COST 286** Newton solves (575 ms, 135.1 MB, single run) | 472/472 in 103 classes, junction lines identical, 286 |
| (b) | `./gradlew --no-configuration-cache fluidSolverRegression -PfluidRegressionMode=exact --console=plain` (`02-...`) | **0.000e+00** on state/moles, temperature, phase fraction and flow; 3 accepted / 0 rejected, 4 Newton solves, 29 iterations; the three island fixtures SKIP as before | 0.000e+00, 3/0, 4, 29 |
| (c) | the adjacent selection: `./gradlew --no-configuration-cache --no-build-cache test --rerun --tests *PassiveStepSolverTest --tests *PumpJunctionStartupTest --tests *FilterBlockLineIslandTest --tests *DeadHeadedLineIslandTest --tests *NetworkRegimeTest --tests *PhysicalFluidTopologyTest --tests *PipePresentationTest --console=plain --continue` (`03-...`) | **45/45** in 7 classes | 45/45 |
| (d) | `./gradlew --no-configuration-cache runFluidGameTestServer -PfluidGameTestRunId=wp6-final --console=plain` (`04-gametest-wp6-final.log`, fresh world `run/fluid-gametest-wp6-final`, 42 s) | **All 33 required tests passed**; `PHASE_PORT_GAMETEST` lines: pump `INLET_WRONG_PHASE / ERROR: pump inlet not liquid (vapour 100.0 % by volume, from reservoir at ...)`; compressor seen `[PUMP_TARGET, CLOSED]`, suction 52659.01872613692 Pa / 247.854 K, discharge 157976.31408416174 Pa / 338.042 K, ratio 2.99999, `FULL / CLOSED (limit 105318 Pa at this suction)` (bit-identical to WP5's final run); outlets at committed tick 3400: tank 124419.4 Pa / 176.86 kg, bottom LIQUID 29.456 kg/s drawing water, head 1723.2 Pa (WP5's run sampled committed tick 3468: 124302.5 Pa, 29.415 kg/s, 1715.3 Pa; the test samples the first interval with flow after the generator's removal, whose tick depends on the batch schedule, so the fill state differs, not the physics) | 33/33 twice (WP5) |
| (e) | the **full** unit suite: `./gradlew --no-configuration-cache --no-build-cache test --rerun --console=plain --continue` (`05-gradle-full-test-wp6.log`, 7 min 15 s; summary `05-full-suite-summary.txt`) | **1166 tests in 237 classes, 1165 passed, 1 failed**: `science.column.v3.thermo.RegroupedCrudeTest.previouslyDifficultCrudesPublishAuditedClassicalSolutionsWithoutLearnedSeeds`, `CancellationException: Regrouped preset deadline` (the test's own 45 s wall-clock deadline per preset). **Pre-existing, not this batch, not fixed**: the batch changes nothing under `science/column`, `science/thermo` or their tests, and column code calls no fluid code; alone it passes on both trees (WP6 50.55 s, `08-regrouped-crude-wp6.log`; base `6e1c5b6` 49.46 s, `08-regrouped-crude-base-6e1c5b6.log`); the full `test` task on the clean base worktree `scratchpad/wt-harness` (src `6e1c5b6` = `c32acac`'s) fails the same test the same way (1100 tests, 1 failed, `09-gradle-full-test-base-6e1c5b6.log`): a load-sensitive wall-clock deadline in the 4-core container. The full suite's 33 junction lines are identical to D10's too. The 66 tests over the base's 1100 are the batch's fluid tests (472 - 406) | not run in this batch before (the brief's "about 1,250" is not what this container counts: the base has 1100) |
| (f) | `./gradlew --no-configuration-cache build -x test --console=plain` (`06-...`); then `compileFluidGameTestJava compileMcpCompatJava --rerun` (`06b-...`) | **BUILD SUCCESSFUL** (`verifyMaterialIndex`, `jar`, `jarJar`, `sourcesJar`, `assemble`, `check`); both source sets compile | BUILD SUCCESSFUL |
| (g) | `LIB=<jars> OUT=<dir> REPO=/home/user/CreateChemE bash tools/cloud-science-harness/harness.sh all` (`07-harness-all-wp6.log`, 100 s) | science **223/223**, runtime **249/249** with the 33 junction lines identical to its reference, adjacent **45/45**, chain-100 **0.000e+00**; every printed fixture line equal to WP5's harness run apart from wall times and `HashMap` print order (`07-harness-diff-vs-wp5.txt`) | 223 + 249 + 45 + 1 |

No gate changed its count or any conservation, ledger or solve-count number; no assertion and no tolerance changed. A cleanup neither widened nor narrowed a gate suite: no test left (the extreme test kept its name list and its success assertions).

### (e) Open owner decisions and options (collected from every section)

Closed during the batch, for the record: WP1 section 8 (dry-seeded junction) by D10; the vent gate defect (WP2 option 3, D9 section 5, D11 item 7) by D13; WP2's options 1-2 and A2/A9/A13-A16 superseded by D11; D11 items 6 (by A37) and 8 (by WP6 (b)); the extreme-topology first-interval defect by D12.

Open, none blocking the merge:

1. **Re-baselines to confirm** (made, recorded old and new): A32 (`LevelHeadTest` drain restated under D11), A44 (the 15 compressor migrations, `TransientQualificationTest` now transferring, the three B re-baselines by flash: `PumpJunctionStartupTest` crude at 150 kPa, the crude GameTest at 1 MPa, `FluidFallbackQualificationTest` pumping water), A54 (the four physical-topology rising lines at the bottom-port pressure, headspace 6.95 kPa / 8.61 kPa lower). A19 is moot (its test class was deleted by D11). The reversible defaults A20-A54 stand unless the owner objects.
2. **D14 (accepted for this batch)**: adiabatic humid venting from about 180 kPa and above cools below 273.16 K and every step is refused (all-BULK too). Options for a later batch: (b) vessel wall heat exchange with an ambient, (c) an ice region in the water properties, (d) clamped water properties below the triple point.
3. **D11 section 12**: item 2 recovered rejections at phase boundaries (lever: per-step hysteresis on the phase correction, only if in-game cost shows); item 4 the mobility check reads the leading phase, not the step's mixture (option: carry the draws into the stage guard); item 5 unbacked traces for a donor that also receives (options: per-component reachability at booking, or cap each frozen fraction at what the donor can deliver; none measured). Item 3 (condensate after a drain) is physical, no decision.
4. **WP3 + WP4 section 11**: (1) A37 the port supply at the target over the slice: keep / the leading phase / a fixed reference time; (2) the refusal uses the slice duration (informational; the GUI passes `advancedSeconds()`); (3) a closed device's head unknown moves the Newton's roundoff by up to 10 ulp at 0.1 s: accept / drop a closed device's head unknown and rows (not bitwise for existing closed devices); (4) compressor work at long steps +13 % at 5 s: accept (D5) / book at the mean of start and end heads (the pump's booking too); (5) the RCPR landing 2.8 Pa against the kept 3 Pa bound: accept / state the bound relative to the rise; (6) the formerly dead-headed C tests now transfer: keep / `r_max = 1.5` to keep them dead-headed; (7) `FluidFallbackQualificationTest` "wet crude pump" pumps water: keep / add a crude case with a 300 kPa generator.
5. **WP5 section 10**: (1) "drawing X" = the phase that carried the most mass (A47): keep / first phase of the order present / list every drawn phase; (2) the refusal reason is stated on the committed state (A48): keep / carry the solver's text in the interval result; (3) compressor (and pump) efficiency reset to 1 by a GUI edit: keep / an efficiency field on both pages. Items 4 (no head-only two-tank stack) and 5 (falling lines at the landing resolution) need no decision.
6. **Extreme-topology water variant** (`EXTREME_TOPOLOGY_TESTS.md` section 7): water generators into nitrogen tanks through the same meshes hold at the default cap and at 100000 m/s; not a test; a separate water-fill-through-a-mesh campaign if the owner wants it (the scan runner is in `tools/phase-ports-probes/extreme/`).
7. Carried from the mixed-gas batch, unchanged: the `fluid-trbdf2-r1` anchor label, the three island exact-regression fixtures skipping (no current-basis stress snapshot).

### (f) What the merge needs

1. `CHANGELOG.md`: this batch's `[Unreleased]` `### Changed` line moves under `## [0.7.0] - <merge date>`.
2. `gradle.properties`: `mod_version` 0.6.0 -> **0.7.0** (minor: solver model change, new block, checkpoint format 6).
3. `documentation/INDEX.md` row of this batch -> "Implemented <merge date>"; `tools/INDEX.md` rows `phase-ports-probes/` and `cloud-science-harness/` keep their status with the merge date noted.
4. **Fresh world**: checkpoint format 6 refuses every 0.6.x fluid world ("Create a fresh world for this development build"); wire protocol fluid-7 (client and server from the same build).
5. **The owner's in-game check** (MCP bridge, `runMcpClient` lane, fresh world), the three scenarios of plan section 6 as listed in WP5 section 11: tank outlets (top/side/bottom lines, head, hint, kg/h-kmol/h toggle, compressor block render and name); pump refused on a tank's side and running from its bottom, refused again when the water runs out; compressor to its ratio limit, ratio edits 1.5 and 10.5 (refused by the screen), then refused on water.
6. **The Windows JDK 21.0.11 lane**: fluid suites (expect the one known `MIXED_GAS_TRANSIENT interval=0.1 key=0.02:5:false` ledger roundoff difference against this container's captures; compare the junction lines with the Windows run 160 capture instead), exact regression 0.000e+00, the adjacent 45, `runFluidGameTestServer` on a fresh run id.
7. Git: `origin/main` is `ea16150` ("Cloud prep", on `c32acac`) and an ancestor of this branch, so `main` fast-forwards. The Windows main checkout is still at `c32acac` with `documentation/`, `research/` and `tools/` untracked there; pulling `ea16150` or this branch writes tracked files over those folders (git refuses while untracked files would be overwritten), so move the local copies aside first and compare them after (the tracked copies are the canonical ones from here on).

### Windows lane (2026-09-27)

The gates of record re-run on the owner's machine (Oracle JDK 21.0.11+9-LTS, Windows 11) at `a0c09c9f`; logs in `tools/phase-ports-probes/windows-lane/logs/`.

| Gate | Expected | Got |
|---|---|---|
| Fluid suites | 472/472, MIXED_GAS_COST 286 | 472/472 in 103 classes, test names identical to WP6, 286 solves, 270 ms, 134.9 MB |
| 33 junction lines vs the Linux capture (`d10/logs/02-junction-lines-d10.txt`) | platform roundoff in a few lines | 1 line differs: `MIXED_GAS_TRANSIENT interval=0.1 key=0.02:5:false` (worstMoles 9.995189311191963E-16 Linux, 8.616542509648244E-16 Windows), same solve counts; cause = the Windows `Math.pow` path (jdk-determinism review 15.1) |
| Exact regression | 0.000e+00 | 0.000e+00 on all four quantities; 3 accepted / 0 rejected, 4 solves, 29 iterations |
| Adjacent | 45/45 | 45/45 |
| Fluid GameTests, fresh world (`win-lane-1`) | 33/33 | 33/33; pump, outlet and compressor values bit-identical to the Linux run |
| Full `test` | 1166 with at most the deadline test failing | 1166/1166; the deadline test passes in 24 s on this machine |

No defect found. The in-game check is the remaining owner item of handoff section 4.

### In-game check (2026-09-27)

- Build `5b577d1c` (`claude/phase-ports-compressor`), owner's Windows machine, JDK 21.0.11, `runMcpClient` lane with the
  `createcheme_mcp_compat` mixins, bridge minecraft-mcp 0.3.0 over HTTP only (no desktop input). Fresh creative superflat
  world (checkpoint format 6, `islands=0` at load), GUI scale 1 in the 854x480 window (see F3). Record, exact bridge calls
  and positions: `tools/phase-ports-probes/in-game/NOTES.md`; screenshots `01`-`13`, `client-latest.log`,
  `gradle-runMcpClient.log` in the same folder. Presence checks only, no timing.
- Built: scenario 1, a 1000 L N2 tank filled from a 130 kPa water generator (201.6 kg of water, 130 kPa), then the
  generator removed and 0.01 m pipes on its top, east and bottom faces to three 101325 Pa voids; later the generator put
  back on the west face. Scenario 2, the same tank fill with a pump on the east face facing away (to a void), then the
  pump moved under the tank facing down (0.001 m3/s, then a second tank with the default 0.01 m3/s). Scenario 3, two N2
  tanks with a compressor between them facing the second, then the suction tank replaced by a water generator.

| # | check | result | evidence (game time) |
|---|---|---|---|
| 1.1 | top line carries gas | PASS | tank `Top: gas outlet / drawing gas · 1.4 kmol/h` (327 s, `01`); top pipe phase ratio Vapor 100.0 %, N2 97.3 mol % + water vapour 2.7 % (522 s, `04`) |
| 1.2 | bottom line carries liquid | PASS | `Bottom: liquid outlet (head 1.9 kPa) / drawing water · 81.1 kmol/h` (327 s); bottom pipe Water 100.0 %, Vapor 0.0 % (397 s, `03`) |
| 1.3 | side line carries the bulk mixture (D11) | PASS | `Side: mixed outlet / drawing all phases together · 25.6 kmol/h` (327 s); side pipe Vapor 50.2 % / Water 49.8 % (537 s, `05`) |
| 1.4 | "Connections by face" lists each face's port | PASS at GUI scale 1; see F3 for scale 2 | three rows (Bottom, Top, Side), four with the feed (542 s) |
| 1.5 | level head at the bottom port | PASS | `head 1.9 kPa` at 199.6 kg, 5.4 kPa at 547 kg, 9.8 kPa at 996 kg (= g m / A, H = 1 m) |
| 1.6 | kg/h vs kmol/h toggle | PASS | 81.1 kmol/h -> `drawing water · 1241.4 kg/h`, top 24.0 kg/h, side 302.4 kg/h (342 s, `02`); table headers follow |
| 1.7 | outlet hint tooltip | NOT OBSERVABLE | the bridge cannot hover; the text is in `en_us.json` (`gui.createcheme.fluid.outlets.hint`) |
| 1.8 | compressor block renders, item name | PASS | light-grey body, oxidised-copper motor, joins along its axis (`13`); `Gave 1 [Process Gas Compressor]`, hotbar name, screen title "Process Gas Compressor" |
| 2.1 | pump on the tank's side refused with the typed reason | PASS | `FULL / INLET_WRONG_PHASE / ERROR: pump inlet not liquid (vapour 79.9 % by volume, from reservoir at 0, -56, 10)`, warning colour, flow 0, no limit (602 s, `06`) |
| 2.2 | rest of the island keeps running | PASS (weak) | tank `STEADY: no flow since 606.4 s` (632 s), no HELD; the island had no other flowing line |
| 2.3 | pump on the bottom face runs | PASS, then FAIL | `SOLVING / PUMP_TARGET (limit 481688 Pa on this fluid)`, 181.0 kmol/h (723 s, `07`); the island HELD at 792 s with water left (F1) |
| 2.4 | refused again when the water runs out | NOT OBSERVABLE | the island held before the tank ran dry, twice (F1) |
| 3.1 | compressor on gas to its ratio limit | PASS | `PUMP_TARGET (limit 130887 Pa at this suction)`, ratio 2.2 (763 s); `STEADY: no flow since 774.3 s / CLOSED (limit 105318 Pa at this suction)`, suction 52.7 kPa, rise 105.3 kPa, `Discharge / suction pressure 3.0` (798 s, `08`), the GameTest's numbers |
| 3.2 | ratio 1.5 accepted | PASS | `Applied`; `CLOSED (limit 26330 Pa at this suction)` = 0.5 x 52.66 kPa |
| 3.3 | ratio 10.5 refused by the screen | PASS | `Check numeric inputs and composition; amounts must be non-negative with a positive total.`; nothing sent, limit unchanged (`09`) |
| 3.4 | compressor fed water refused | PASS | `INLET_WRONG_PHASE / ERROR: compressor inlet not gas (condensed 100.0 % by mass, from generator at 0, -59, 20)`, warning colour (868 s, 878 s, `10`) |

Fluid log: 46 `status=HELD` lines on three islands (below); no exception and no ERROR from a createcheme class. The
compressor island never held.

**Findings.**

- **F1 (solver behaviour, blocks 2.4).** A pump drawing water from a tank's bottom face holds its island within seconds,
  with water still in the tank; reproduced twice on fresh tanks. At 0.001 m3/s: running at 723 s, `fluid_island=53`
  HELD from committed tick 15847 (792 s), `Interval substep limit; no partial interval may commit: advanced=2.855 of
  3.2 s, accepted=506, rejected=518, reasons={Newton iteration limit at residual #; active-set pass=#=518}, last=Newton
  iteration limit at residual 1.60e-9`; the committed tick crept to 795 s and stopped (about 120 kg of water left,
  estimated). At the default 0.01 m3/s: running at 1154 s, `fluid_island=81` HELD from 1160 s (6 s after the start, about
  60 kg drawn of 201.6 kg), the same reason plus `Backward-Euler state change #=2`, residual 1.63e-9, crept to 1164 s. The
  same tank drained through a plain 0.01 m bottom pipe (scenario 1, 327-452 s) did not hold, so the pump on the bottom
  port is the trigger. The logged last rejections all stall with the Newton residual at 1.1-1.8e-9.
- **F2 (solver behaviour).** Scenario 1's tank, fed again from the 130 kPa water generator with its three outlets open,
  held once it ran liquid-full: `fluid_island=36 committed_tick=14220 (711 s) status=HELD: Substep refinement exhausted:
  Newton iteration limit at residual 1.147e-9; active-set pass=0`, never advanced again. The held view (`11`): 996.0 kg,
  129.9 kPa, `Top: gas outlet / drawing liquid (overflow) · 66.4 kmol/h`. This is the "VAPOR port on a tank filled
  liquid-full" case (plan fixture 2, second half; D11 open item "recovered rejections at phase boundaries"), here not
  recovered.
- **F3 (GUI).** The tank's "Connections by face" is not drawn at all unless the scaled screen is at least about 323 px
  high: `renderOutlets` returns when `y + 38 > imageHeight - 55`, the tank rail reaches y = 214 after mass, and
  `imageHeight = max(250, height - 16)`. At 854x480 with GUI scale 2 (240 px) and at 1920x1080 with the automatic scale 4
  (270 px) the list is absent; at 1080p scale 3 (360 px) about two rows fit before "+N more". Derived from the code at
  `5b577d1c`; this check ran at scale 1 and did not capture the scale 2 screen.
- **F4 (GUI).** Two connections on different side faces both read `Side: mixed outlet`, with no face or neighbour, so a
  player cannot tell which row is which pipe (542 s: one `receiving · 1784.2`, one `drawing all phases together · 45.3`).
- **F5 (GUI text).** The ratio 10.5 refusal is the screen's generic message about numeric inputs and composition; it does
  not name the ratio or its 1.01-10 bounds.
- **F6 (GUI, cosmetic).** Roundoff shown as values: a dead-ended pump `Last flow -6.49e-18 kmol/h`; a closed compressor's
  island `STEADY: replaying 8.529e-17 kg/s since 831.6 s, next check at 78591.6 s`. Pipes with no flow against a void show
  `Mean pressure drop / length 1809.5 Pa/m` (the static end difference, read as a friction loss). The mass/mole choice
  resets to kmol when a screen is reopened.
- **Observations, no defect.** Voids do not feed back: once the tank fell below 101.3 kPa the top and side lines read
  `no flow` (452 s). A pump at its target flow shows a negative pressure change (-58.4 kPa, -44.6 kPa): the prescribed-flow
  pump throttles a line that would otherwise flow faster. The compressor's refusal names the generator's stream
  ("condensed 100.0 %") while its own suction junction still holds only N2 (its Water page is empty): the reason comes
  from the supply walk (A48), the rail from the junction.
- **Bridge limitations.** No hover (1.7); a screen opened before its first snapshot has no fields; the Create World name
  box ignored `type_text` (the world is "New World" in a game dir that had no saves).

**For the owner.**

1. F1: the liquid pump cannot drain a tank from its bottom face in game without holding the island; scenario 2's last
   check is open until it is fixed. Diagnosis (which row fails at 1.6e-9 and why the substeps shrink) is the next step;
   per the fix-assumptions rule, options to be presented before any change.
2. F2: the liquid-full tank with a top outlet holds; same diagnosis track (D11 open item 2).
3. F3: where the outlet list should go at the common GUI scales (move above the rail values, a page of its own, or
   scroll).
4. F4, F5, F6: wording and cosmetic items; keep or fix in a GUI pass.

## Island holds F1 and F2 (2026-09-27)

- Author: Claude (Opus 5.5), worktree `.claude/worktrees/phase-ports-compressor`, branch `claude/phase-ports-compressor` at `82f2d2f0`, owner's Windows machine, JDK 21.0.11, uncommitted (the orchestrator commits). Material: `tools/phase-ports-probes/holds/` (README, patches, probes, logs numbered as cited).
- The defects: F1 and F2 of the in-game check (section "In-game check (2026-09-27)"): a pump on a tank's bottom face held its island with water left (`Interval substep limit`, last `Newton iteration limit at residual 1.60e-9` / `1.63e-9`); scenario 1's tank fed liquid-full with its outlets open held (`Substep refinement exhausted: Newton iteration limit at residual 1.147e-9`).

### 1. Reproduction (runtime path, fail on `82f2d2f0`)

Both build the in-game blocks with `PhysicalFluidTopology.compile` (world defaults: 1 m blocks, 0.05 m pipes, 0.01 m outlets, the registry's idle seed) and run consecutive 5 s jobs on one `RetainedSolver`, the committed interval handed over at each dispatch, as `IslandCoordinator` does.

| test | fixture | on `82f2d2f0` (`logs/15-baseline-new-tests-fail.txt`) |
|---|---|---|
| `PumpBottomPortDrainTest` (2 tests) | 1000 L N2 tank at 1 atm filled from a 130 kPa water generator until it rests (200.4 kg of water, 130000.0 Pa), generator removed, pump under it facing down (`Pump(target, 500 kPa)`), one pipe, 1 atm void; target 0.01 and 0.001 m3/s; must drain and end `INLET_WRONG_PHASE` with at most one slice of target volume left | 0.01: HELD at slice 3 (15 s), 53.1 kg left, `Interval substep limit ... accepted=504, rejected=520, reasons={Backward-Euler state change #=2, Newton iteration limit ...=518}, last=... residual 1.066e-9`; 0.001: HELD at slice 18 (90 s), 112.8 kg left, `accepted=505, rejected=519 ... residual 1.319e-9`. In game: 1.63e-9 / 1.60e-9, same keys |
| `LiquidFullTankOutletsTest` | the same tank with a 130 kPa water generator on its west face (0.05 m) and 0.01 m pipes on its top, east and bottom faces to 1 atm voids; must fill, run liquid-full and overflow steadily for 60 slices | full at slice 25; HELD at slice 53 (265 s), 996.04 kg, vapour share 1.5e-31, `Substep refinement exhausted: Newton iteration limit at residual 4.157e-10; active-set pass=0; reasons={Newton iteration limit ...=20}`. In game: 996.0 kg, 1.147e-9, same key (the tank's headspace below 1 % sets the 1e-10 tolerance) |

### 2. Root cause, by numbers (instrumented copy, `src/instr-holds-dump.patch`, `logs/01`-`03`)

**One mechanism for both: a vanishing vapour phase holding a nitrogen trace, whose water-saturation row gets a wrong finite-difference derivative.** Nitrogen does not dissolve in free water, so the nitrogen a water-full node keeps stays a vapour phase that decays geometrically and never to zero:

- F1: the pump's own junction (the block between the tank's LIQUID port and the pump) was minted from the island's first boundary, the tank, so it started with the tank's gas; the water flowing through washes it out by backward Euler, `n' = n (m/dt)/(m/dt + Q)`, about 1.8 decades per slice at 0.001 m3/s. At the failing step (0.01 m3/s, dt 0.235 s): junction 11168 mol water (201.2 kg), vapour share 7.0e-32, N2 6.5e-31 mol, water vapour 1.8e-32 mol.
- F2: the tank's last nitrogen, drawn by the top (VAPOR) port at its D11 capacity `m/(N dt)` with `N = 2` (the top and the bottom port), is halved at every step (3 steps per slice, 0.9 decades per slice). At the failure: 55288 mol water, vapour share 7.6e-32, N2 3.9e-30 mol, water vapour 9.7e-32 mol.

The failing row (every dump, every step size): the node's `ln(p_w / P_sat)` (F1 row 10 = the junction's; F2 row 4 = the tank's). Every other row of the island is at or below 1e-15 (F1) and 7e-13 (F2, the same node's partial-pressure closure). The residual of that row after 20 iterations: F1 9.8e-9 (dt 0.235 s), 2.3e-9 (0.039 s), 1.6e-9 (0.0098 s), 1.3e-9 (0.0025 s); F2 1.63e-7 at dt 0.25, 0.0625, 0.0156, 0.0039, 0.00098 s. It does not grow as the step shrinks, and neither the modes, the segments nor the phase set change (`pass=0`, one segment code), so neither class 4 (rate-form floor) nor a row discontinuity.

- **Not a rounding floor.** The row's measured floor (the residual change under random 1-2 ulp perturbations of every unknown): 2.4e-15..8.4e-15 (F1), 1.4e-15..5.8e-15 (F2), six orders below the stall.
- **Not the liquid-full stiffness.** F2's volume and pressure rows are at 1e-16; the tank's pressure is carried by the water.
- **The derivative.** `p_w = n_wv R T / V_gas` with `V_gas = n_N2 v_m`, so the row is `ln(n_wv / n_N2) + const`, and its derivative in the water-vapour unknown (scaled by the node's total amount) is `1/x`. That unknown is 1.6e-36 (F1) / 1.8e-36 (F2). The difference step was `1e-6 * max(1e-30, seed, |x|) = 1e-36`: the `1e-30` in `PhaseLayout.differenceScale` meant as a zero guard floors every trace below it, so the step is 0.6-1.1 of the unknown itself and the secant `ln(1 + h/x)/(h/x)` is 22-32 % below the tangent. The Newton overshoots that unknown by `1/k - 1` and the row alternates in sign (traces of a fresh-workspace re-solve of the failing pass, `logs/02`, `03`): F2, after its start-up, `-3.95e-4, 1.08e-4, -2.94e-5, 8.04e-6, -2.19e-6, 5.99e-7, -1.63e-7` (0.27 per iteration, iteration 20 at 1.63e-7 against the 1e-10 of a headspace below 1 %); F1 0.27, then 0.49 after a refresh as the trace shrank to 9.4e-37 (`h/x` = 1.06), and 0.06 at the larger trace of the earlier dump (where the fresh re-solve converged in 13 iterations and the solve on the forked chord preconditioner did not). 20 iterations then end a little above the tolerance at every step size, and the controller halves 500 times.
- Why the plain bottom pipe drained (scenario 1) and the pump did not: a pipe run collapses into one connection, so there is no junction between the tank and the void; the pump block owns a junction.
- Other suspects read in the dumps and ruled out: the D11 segment (edge 0 of F1: water marginal, frozen, never moves), the level head (490 Pa on the F1 tank, a constant of the row it enters), the pump (PUMP_TARGET, head -27.5 kPa, its target row 0.0, suction = the junction's bulk water).

**A second layer behind it (F2, seen once the first was fixed, `logs/04`, `06`).** At about 2e-150 mol of vapour, `FluidThermodynamics.acoustic` (the trace gas phase's velocity limit, which the D11 mixture cap of the vent reads) forms `mass * compliance` of order `n^2 * 4e-9`, which goes subnormal and at 2e-158 mol underflows to zero: the bound became garbage then infinite, and F2 held at slice 195 (vapour share 6.9e-160) with the vent edge's hydraulic row stalled at 8.3e-4 on line-search stalls.

### 3. Fix (pure numerical repairs, no model change; implemented)

| file | change | bitwise where |
|---|---|---|
| `science/fluid/solver/PhaseLayout.java` | `differenceStepScale`, `traceDifferenceScale`, `seedDifferenceScale`, `TRACE_DIFFERENCE_GUARD`: the difference step of an amount whose value and seed both lie below 1e-30 is taken relative to its own size (`own = max(|value|, seed)`; the 1e-30 guard only at zero). `differenceScale` (the floor the trace clamp is derived from) is unchanged; the single-node `fixedInventory` adapter uses the new scale | every amount whose value or seed is at least 1e-30 of its node (the old `max(1e-30, own)` is `own` there) |
| `science/fluid/network/PassiveStepSolver.java` | `Equations.seedDifferenceScales`, filled with the layout's seed scale for every amount column; `Equations.differenceScale` returns `PhaseLayout.traceDifferenceScale(max(seed, |value|))` for those, `max(floor, |value|)` for every other column | the same |
| `science/fluid/thermo/FluidThermodynamics.java` | `acoustic`: a product `M C` below `Double.MIN_NORMAL` is evaluated as `sqrt(V/M) sqrt(V/C)` | every product at or above `MIN_NORMAL` |

No tolerance, assertion, iteration budget or conservation check changed.

After the fix (`logs/11`): F1 at 0.01 m3/s drains to 3.30 kg in slice 3 and is `INLET_WRONG_PHASE` from slice 4 (flow 0.0); at 0.001 m3/s to 3.25 kg in slice 39, refused from slice 40; F2 runs full at slice 25 and overflows 60 slices with the tank's mass constant (`steady=60`), the top outlet drawing water. Recovered rejections left in these runs: F1 0.01 slice 0, 2 Newton limits (start-up); slice 4, 2 `Singular Newton Jacobian` as the pump is refused (the junction then has no open connection); state-change halvings.

### 4. Gates (each once, idle machine, logs in `tools/phase-ports-probes/holds/logs/`)

| # | command | result |
|---|---|---|
| G0 | the two new classes (`logs/11`) | 3/3 |
| G1 | `./gradlew --no-configuration-cache --no-build-cache test --rerun --tests com.wormzjl.createcheme.science.fluid.* --tests com.wormzjl.createcheme.runtime.fluid.* --console=plain --continue` (`logs/12`) | **475/475** in 105 classes (472 + 3); the 33 junction lines **identical** to `windows-lane/logs/01-junction-lines-win.txt`; **MIXED_GAS_COST 286** Newton solves |
| G1' | every printed fixture line of G1 against the same suite on `82f2d2f0` (`logs/15`, `16`; `src/StdoutCompare.java`, timings masked, map entries sorted) | 5574 lines, **0 differ** (the new tests excluded) |
| G2 | `./gradlew --no-configuration-cache fluidSolverRegression -PfluidRegressionMode=exact --console=plain` (`logs/13`) | **0.000e+00** on state/moles, temperature, phase fraction and flow |
| G3 | the adjacent selection (`logs/14`) | **45/45** in 7 classes |

### 5. What remains: the bottom of the double range (owner decision)

The fix removes the holds seen in game and carries these traces from 1e-31 down to about 1e-300 of their node, but the traces still never reach zero, and near the bottom of the double range no difference step can serve them: the row's Jacobian entries, of order `1/x`, overflow the factorization at about 1e-303, and below `MIN_NORMAL` the trace's own amounts carry too few bits to resolve its ratio (`src/TraceVapourProbeTest.java`, `logs/05`: the phase's properties are exact down to 4e-304 mol, and the saturation row moves by 4.3e-9 at 4e-314 mol, above the 1e-10 tolerance). Measured on the fixed tree (`logs/17`; `src/LiquidFullTankLifetimeProbeTest.java`, `src/FedPumpLifetimeProbeTest.java`):

- F2 run on: HELD at slice 354 (1770 s, 1645 s of steady overflow), vapour share 1.4e-303, `Substep refinement exhausted: Matrix values must be finite`.
- A pumped water line whose pump junction was minted from a tank (F1's layout with a 130 kPa generator keeping the tank fed, 0.001 m3/s): HELD at slice 173 (865 s), junction nitrogen 4e-299 mol, `Interval substep limit ... Matrix values must be finite`. (With the generator as the island's first boundary the junction is minted as pure water and never holds a trace: 400 slices clean, `logs/07`.)

So a player's overflowing tank holds after about 27 minutes and such a pumped line after about 14. Any water line through a junction (pump, valve, filter, branch) minted from a gas-holding boundary meets it. Every remedy decides what a trace is, so the options are the owner's:

- **T1 (recommended): flush traces below a threshold.** At each accepted step's commit, a component whose amount in a vessel or junction is below `1e-24` of that node's total (the prototype's threshold) is removed and the node re-flashed at its T and P; the vanishing phase then vanishes. Prototype `src/option-t1-trace-flush.patch` (19 lines in `PassiveIntervalSolver`, removed amounts not booked). Measured on the fixed tree (`logs/18`, `19`): F2 run on, full at slice 25, nitrogen gone at slice 42, steady; the fed pump line 400 slices (2000 s) with 3 accepted / 0 rejected steps per slice; fluid suites 477/477 (both lifetime probes included), 33 junction lines identical, MIXED_GAS_COST 286, all 5574 printed fixture lines identical, exact regression 0.000e+00. Decisions it needs: the threshold (relative, or absolute near one molecule, 1.7e-24 mol) and whether the removed amount is booked (a boundary transfer or the node's outflow, keeping the ledger exact) or deleted (at most 1e-24 of the node per component and step, far inside `checkConservation`'s 1e-10 mol).
- **T2 (measured, rejected): amount-form equilibrium rows for a trace vapour** (rows weighted by the vapour's share below 1e-12, `src/option-t2-trace-phase-weight.patch`, measured with an earlier variant of the difference step floored at 1e-290): still holds, F2 at slice 348 (vapour share 3.5e-298) and the fed pump line at slice 169 on `No finite Jacobian trial` (`logs/09`): the Jacobian itself cannot be formed there, whatever the row weight. It also loosens the trace phase's equilibrium, which the tolerance rule forbids.
- **T3 (not prototyped): scale each trace unknown by its own seed.** Keeps the Jacobian O(1) down to `MIN_NORMAL`, a larger change to `PhaseLayout`'s encoding, and still needs T1 with a threshold of `MIN_NORMAL` below it (subnormal amounts cannot resolve their ratio).
- **T4 (F2 only, not measured): D11's capacity split among the ends whose segment draws the phase** (A21 changed), so the top port takes the whole gas cap in one step and the drained-seed path (A27) ends the phase. Leaves F1's junction washout.

### 6. Material

`tools/phase-ports-probes/holds/`: `README.md`; `src/instr-holds-dump.patch` (dump of the failing step with a per-row rounding floor, and a traced fresh re-solve; against the fixed tree, never commit), `src/option-t1-trace-flush.patch`, `src/option-t2-trace-phase-weight.patch`, the probes `TraceVapourProbeTest`, `FedPumpLifetimeProbeTest`, `LiquidFullTankLifetimeProbeTest` (copy into `src/test/java/com/wormzjl/createcheme/runtime/fluid/` to run, then delete), `StdoutCompare.java`; `logs/00`-`19`.

## GUI fixes F3-F5 (2026-09-27)

- Author: Claude (Opus 5.5), worktree `.claude/worktrees/remove-agents-attribution-line-3d0c97`, branch `claude/phase-ports-compressor` at `b0923a5a`, owner's Windows machine, JDK 21.0.11. Code commit `f089edd1` (`Phase-ports GUI: outlet list at every GUI scale, face names, ratio bounds message`); this section, `DECISION_LOG.md` A55-A57, `CHANGELOG.md` and the material in the commit after it. Not merged, not pushed.
- Scope: findings F3, F4, F5 of the in-game check (section "In-game check (2026-09-27)"). F6 is not touched (owner decision D16: roundoff flows are shown as they are). No solver, scheduler or checkpoint change; the presentation still reads only the published snapshot (the faces are compile output cached by the registry, like the pipe views; nothing is solved or flashed for them).

### 1. What changed

| finding | file:line (at `f089edd1`) | change |
|---|---|---|
| F3 | `client/gui/screens/inventory/FluidDeviceScreen.java:143` | a tank's page gets a second tab "Connections by face" (key `outlets`, hover hint `outlets.hint`, the D11 rule) beside Overview, as a pipe's page has "Connections" |
| F3 | `FluidDeviceScreen.java:59-64`, `:157`, `:336`, `:351-362` | page 4: the list in the table area, title at y 82 (hover: the hint), one two-line row per connection (24 px) from y 100, scrolled by the screen's `ProcessScroll` (wheel and bar) when the rows do not fit; "No connection leaves this tank." when there is none. The rail no longer draws the list (the old rail renderer and its `outlet.more` key are gone), so nothing is skipped or cut at any scale |
| F4 | `runtime/fluid/PhysicalFluidTopology.java:35-45`, `:136-137`, `:164` | `Compiled` gains `tankFaces` (tank id -> compiled pipe id -> face: the direction from the tank to its neighbour that the end's port is compiled from, A46) |
| F4 | `runtime/fluid/PhysicalRegistry.java:54`, `:80`, `:111`, `:291-293` | the registry keeps the faces exactly as it keeps `pipeViews` (put at load and at every applied batch, removed with the component) |
| F4 | `runtime/fluid/FluidView.java:35-41` | `Outlet(pipeId, face, port, ...)`: the face is required and must match the port (UP VAPOR, DOWN LIQUID, horizontal BULK) |
| F4 | `runtime/fluid/TankOutlets.java:21-43`, `FluidWorldAuthority.java:317` | `TankOutlets.of(..., faces)` names every row; a connection without a compiled face is refused (`IllegalStateException`, contained per device by the presentation) |
| F4 | `network/FluidNetwork.java:32` | wire protocol **fluid-7 -> fluid-8** (each outlet of the live view carries `face`; a payload without it is refused). No compatibility work; checkpoint format unchanged (6): faces are not saved, the load compile rebuilds them |
| F4 | `FluidDeviceScreen.java:363-369`, `lang/en_us.json:338-347` | rows read `Top (up): gas outlet`, `Side (east): mixed outlet`, `Bottom (down): liquid outlet (head 5.8 kPa)`; face names as the game's debug screen names directions (`face.down` ... `face.east`: down, up, north, south, west, east) |
| F5 | `network/FluidNetwork.java:39-43`, `:50` | `pressureRatioAllowed` (the one bounds test, now also used by `Controls`) and `pressureRatioBoundsText` ("1.01", "10", from `MINIMUM/MAXIMUM_PRESSURE_RATIO`) |
| F5 | `FluidDeviceScreen.java:241-244`, `lang/en_us.json:320` | on a compressor page, Apply with a ratio outside the bounds (or not a number) shows `Max pressure ratio (out/in) must lie between 1.01 and 10.` and sends nothing; every other refusal keeps the generic message |

Tests: `FluidPacketCodecTest` (protocol fluid-8; the outlet round trip with faces, a faceless outlet refused, a face/port mismatch refused; new `theRatioRefusalNamesTheFieldAndTheValidatedBounds`: the predicate at 1.01/3/10 and 1.009/10.01/10.5/NaN/0, the bounds text, the lang template formatted with it, every face key and the `(%s)` of the four outlet keys), `PhysicalFluidTopologyTest.everyFaceOfATankCompilesToItsPortAndOnlyTheTankEndCarriesOne` (the six faces recorded per pipe, six distinct faces in the rows, four distinct side faces, a missing face refused), `TankOutletsTest` (top UP, side EAST, bottom DOWN), `LiquidFullTankOutletsTest` (passes the faces).

### 2. The layout choice (A55)

The rail cannot hold the list at the common scales: at 854x480 scale 2 (and 1920x1080 auto, 480x270 scaled) the screen is 250 GUI px high and the tank's own rail values already reach its bottom. The screen already has tabs (generator: Composition, Solids; pipe: Connections) and a scrolled table area, so the list became a tab of its own in that area with the pipe page's conventions (title line, table rows, `ProcessScroll`). Rows are two lines (the name with its face, then what it drew and the flow), so the existing strings stay whole. Rejected: a compact one-line row on the rail (no rail space at scale 2 at all), the list above the rail values (it would push temperature, pressure, volume and mass off the screen instead). Row tooltips were tried and removed (run 1, screenshot `03`): a screen opens with the cursor at the window centre, which at scale 2 lies inside the rows, and the hint covered the list; the hint stays on the title and on the tab button.

### 3. Gates (once each, idle machine, logs in `tools/phase-ports-probes/gui-fixes/logs/`)

| # | command | result |
|---|---|---|
| G1 | `./gradlew --no-configuration-cache --no-build-cache test --rerun --tests com.wormzjl.createcheme.science.fluid.* --tests com.wormzjl.createcheme.runtime.fluid.* --console=plain --continue` (`01`) | **476/476** in 105 classes (475 + the new ratio test), 0 skipped; **MIXED_GAS_COST 286** Newton solves |
| G4 | `compileFluidGameTestJava --rerun compileMcpCompatJava --rerun` (`03`) | BUILD SUCCESSFUL, both executed |
| G5 | `runFluidGameTestServer -PfluidGameTestRunId=gui-fixes-20260927` (fresh world, `04`) | **All 33 required tests passed** (the phase-port GameTest reads the tank's outlets through the menu snapshot, now with faces) |
| G6 | `build -x test` (`02`; `09` after the last screen edit) | BUILD SUCCESSFUL |

G1, G4 and G5 ran before the last edit, which removed the row tooltip in `FluidDeviceScreen.renderOutlets` (client rendering only, no test reads it); `runMcpClient` and `09` compiled it.

### 4. In game (`tools/phase-ports-probes/gui-fixes/NOTES.md`: lane, bridge calls, world, screenshots)

`runMcpClient --offline`, bridge HTTP only, no desktop input; a fresh creative superflat world "GUI fixes F3-F5" (format 6, `islands=0` at creation; the old `New World` save neither opened nor deleted); a N2 tank with pipes on its top, bottom, north, west (to a water generator) and east faces, the other ends at voids; a compressor between two N2 tanks.

| scale (854x480) | observed |
|---|---|
| 1 (`01`) | all five rows, no scrollbar: `Bottom (down): liquid outlet (head 5.8 kPa)` / `drawing water · 4431.6 kmol/h`, `Top (up): gas outlet` / `no flow`, `Side (north): mixed outlet` / `drawing all phases together · 84.7 kmol/h`, `Side (west): mixed outlet` / `receiving · 4718.8 kmol/h`, `Side (east): mixed outlet` / `drawing all phases together · 84.7 kmol/h`: the three side rows are told apart (F4) |
| 2 (`02`) | three rows and the scrollbar; before the fix the list was not drawn at all at this scale (F3) |
| 3 (`04`, `05`, `06`) | the option reads "GUI Scale: 3" but the game draws at 2 (Minecraft caps the scale so at least 320x240 scaled pixels remain; 854/3 < 320): the same page as scale 2; one wheel click shows the north, west and east rows |
| compressor, 2 (`07`) | ratio 10.5, Apply: `Max pressure ratio (out/in) must lie between 1.01 and 10.`, nothing sent (F5) |

1920x1080 was not run (the run configuration fixes 854x480, `build.gradle:138`; resizing needs desktop input). Its scaled sizes (1920x1080, 960x540, 640x360, 480x270) are all at least the 427x240 verified here, so the page shows 37, 15, 7 and 3 rows there. The outlets hint renders as a tooltip (`03`, run 1), which item 1.7 of the in-game check could not observe.

Observed, not fixed (out of scope, for the owner): (a) at scale 2 a pump's, compressor's or generator's rail values start at y = 218 and are drawn over the bottom buttons (`07`: "Temperature -23.8 C" under Close), the same defect class as F3 on the other device pages; (b) run 1 logged 14 `status=HELD` lines on the tank's island (`Interval substep limit ... Backward-Euler state change #=511`, the committed tick crept 1135 -> 1785) with a 101.3 kPa water generator feeding the N2 tank through four open 0.05 m outlets; run 2 none; not investigated.

### 5. Material

`tools/phase-ports-probes/gui-fixes/`: `NOTES.md`, screenshots `01`-`07`, `logs/01`-`09`, `options-before.txt`.

## Island hold F7: state-change rejections on a fed tank (2026-09-27)

- Author: Claude (Opus 5.5), worktree `.claude/worktrees/remove-agents-attribution-line-3d0c97`, branch `claude/phase-ports-compressor` at `fdadcd45`, owner's Windows machine, JDK 21.0.11. **No code changed**: the hold comes from two recorded designs working together (D11's per-step capacity and the backward-Euler state-change controller), and every remedy decides one of them, so the options are the owner's (fix-assumptions rule). Material: `tools/phase-ports-probes/holds2/` (README, the reproduction test, probes, option patches, logs numbered as cited).
- The defect: observation (b) of "GUI fixes F3-F5 (2026-09-27)": in the first client run a 1000 L N2 tank placed at 101.3 kPa, 25 C, with 0.05 m pipes on its top, bottom, north and east faces to 101325 Pa voids and a 101325 Pa water generator on its west face, logged 14 `status=HELD: Interval substep limit; no partial interval may commit: advanced=3.96875 of 5.0 s, accepted=508, rejected=516, reasons={... Backward-Euler state change #=511 ...}` lines, committed tick 1135 to 1785 in steps of 50 (`gui-fixes/logs/06`). The "second run" of the check was not a repeat of the rig: it reopened the same world at tick 6709 after the generator had been set to 130 kPa (the 130 kPa variant below fills the tank and never holds).

### 1. Reproduction (runtime path; `src/FedTankOpenOutletsTest.java`, fails on `fdadcd45`, `logs/05`)

The blocks are placed in the client log's order at its online ticks (20 TPS from the world load: tank 664, pipes 698-834, voids 868-970, generator 1007) and the island is run as the coordinator runs it: each placement is a topology event whose new island starts its slice ladder at one tick (1, 2, 4, ... 64, then the 100-tick cadence), a slice ends at the next event (its fence), a hold halves the slice on a fresh `RetainedSolver`. The run is the in-game one to the last digit: 14 holds at starts 1134, 1184, ... 1784, each `advanced=3.96875 of 5.0 s, accepted=508, rejected=516`, the last refused changes `0.08460994579887296`, `0.08545046943000091`, ..., the 13th ending on `Newton iteration limit at residual 4.67e-6`, the 14th `advanced=4.2157...`, as in the client log (whose ticks are one later: the test's ticks come from the log's wall-clock stamps; what matters is the 37 ticks between the last void and the generator, over which the tank vents 23 Pa into the bottom void, the head of the gas column).

Trigger (variant scan `src/FedTankOpenOutletsProbeTest.java`, `logs/03`; 140 slices after the last placement each):

| variant | holds | most attempts in a slice (of 1024) | final pool |
|---|---|---|---|
| in-game order, generator at 1007 | **14** (then locked at 15.6 ms) | 848 | 1.10 kg |
| generator at 1005, 1006, 1009, 1010, 1013, 1028 | 0 | 885-887 | 0.43 kg |
| generator at 1008, 1011, 1018 | 0 | 438-462 | 0.85 kg |
| generator and its pipe before the outlets | **67** (every 5 s slice from tick 1097 to the end; 2.5 s retries commit) | 646 | 0.28 kg |
| every block in one event | **19** | 832 | 0.54 kg |
| 0.01 m outlets | 0 | 145 | 0.30 kg |
| generator at 130 kPa | 0 (the tank fills, 713 kg) | 26 | 712 kg |

So the trigger is not a particular order: every variant with 0.05 m outlets and a 101.3 kPa feed runs at 43-87 % of the attempt limit or over it, and which one holds depends on where the start-up transient leaves the step (below).

### 2. Root cause, by numbers (`src/instr-f7-step-trace.patch`, `logs/02`, `04`)

**The vessel's inventory is a function of the step size, so the state-change controller refuses every step increase.**

- **The state.** The tank sinks below the voids (92.7 kPa at 1134, 91.6 kPa at rest), so its top and side outlets close (voids do not feed back) and the bottom line, whose water column falls two blocks to its void, can carry more than the feed. D11 (review "D11", section 2) pins the bottom port's water at its capacity `c = m_w,start / (N dt)`, `N = 2` (the top VAPOR and bottom LIQUID ends; BULK sides do not count), gas marginal. Gas can leave only through that port, so the tank settles exactly where the pinned water equals the feed and no gas is drawn: the pool is `m_w = N q dt`. In the held slice: `q` = 13.13 kg/s, `dt` = 7.8125 ms, `m_w` = 0.2052 kg = 2 x 13.13 x 0.0078125; the tank holds 1.2354 kg in all (1.03 kg of it gas).
- **The refusal.** A step `dt'` from that state moves the pool towards `N q dt'`: `dm = q (dt' - dt)`, `dm/m = q dt / m` for a doubling = 13.14 x 0.0078125 / 1.2354 = 0.0831; measured 0.0830 (the log's 0.0846-0.0863). The bottom flow in the refused step is exactly the halved capacity (6.59 kg/s = 0.2052 / (2 x 0.015625)). The pressure moves 0.09 %; the refusal is the vessel's mass (its water), not a vanishing phase (the measure is the total mass, of which the water is 17 %), not a junction (the island has none), not a physical transient (at rest feed = drain to 1e-14), not the step floor.
- **The lock.** Refused at 15.625 ms, halved to 7.8125 ms: accepted with a change of 3e-7 (the pool's fixed point), so the growth factor is 2 (`clamp(0.9 sqrt(cap/change), 0.5, 2)`), and 15.625 ms is tried again. Every accepted step is followed by one refusal: 5 s at 7.8125 ms is 640 accepted steps, 1280 attempts; the limit of 1024 ends at 508 accepted = 3.96875 s. The halved 2.5 s retry needs 320 + 326 attempts and commits, the next 5 s slice holds again: the committed tick creeps 50 ticks per hold.
- **Where the lock sits.** Once the pool is built at `dt`, a doubling is refused whenever `q dt / m > 0.05` (here `dt` > 4.7 ms), at any `dt` above that; every slice restarts at 1 s and halves down to the same power of two, so the lock is kept. The transient decides the level: 7.8 ms (the in-game run: 1280 attempts per 5 s), 15.6 ms (885), 31.25 ms (430, `logs/02`; there the retry after each refusal also fails once on `Newton iteration limit`, 107 of 217 refusals per slice, because the steady state sits on the D11 segment boundary, pinned water with zero gas). The worst case, `dt` just above `0.05 m / q`, costs `2 T q / (0.05 m)` attempts per slice: a hold is possible wherever a phase-port draw is capacity-bound in a vessel whose through-flow `q / m` exceeds about 5 per second (this rig: 11 per second).
- Neither piece is unintended on its own: D11 states that halving the step doubles the capacity (A27), and the controller's cap, growth law and halving are the BE plan's (section 2, review 8.4-8.8). Together they make the committed state depend on the step and lock the controller.

### 3. Options (measured; none implemented)

The 5 % cap, the Newton and conservation tolerances and the attempt limit are unchanged in all of them.

| option | change | F7 (both tests of `FedTankOpenOutletsTest`) | other effects (measured) |
|---|---|---|---|
| O1 (D11, model) | capacity `m / (N max(dt, tau0))` | `tau0` = 0.05 s: **still HELD** (ticks 5534 / 5497, `Substep refinement exhausted: Phase/device active-set cycle`), locked at 31 ms (430 attempts per slice) (`logs/06`). `tau0` = 5 s: passes, 30-42 attempts, pool 146 kg (`logs/07`) | `tau0` = 5 s fails 7 D11 fixtures (`PhasePortPriorityTest` 5, `PumpBottomPortDrainTest` 2; names in `logs/07`): a pinned draw can no longer take a phase to zero in one step. Rejected |
| **O2 (controller measure, recommended)** | a vessel's mass change over `max(m0, rho_ref V)`, `rho_ref` = 1000 kg/m3 (5 % of the vessel's liquid capacity per step, a 5 % level change); pressure measure unchanged | passes: 15 (in-game order) / 40 (generator first) attempts per slice at most; the pool settles at 70.08 kg (91.21 kPa) for every staged variant and 69.29 kg for the generator-first and all-at-once ones; the variant scan 0 holds in all 14 (`logs/08`, `13`) | fluid suites 475/478 (`logs/10`): the 33 junction lines **identical**, **MIXED_GAS_COST 286**; exact regression **0.000e+00** (`logs/12`); three re-baselines: `McpGameplayRegressionTest.belowSeaLevelNitrogenTankAcceptsWaterAfterAnIdlePeriodAndPressureEdit` (a recorded pressure 130299.51 -> 129958.70 Pa), `InletPhaseTest.refusedPumpsCarryNothingAndTheIslandsWaterLineIntegratesBitwise` (the water line against the line alone at 5 s, bitwise before, now 2.4e-13 relative: another vessel's pressure now sets some steps), `VentGatePolishTest.theBulkVentControlIsNeverRefusedByTheGate` (its premise "the run passes because refused points were polished" no longer arises: 0 polishes). With `rho_ref` = 100 kg/m3 (`logs/14`): F7 passes (20 / 40 attempts, pool 42 kg), only the `InletPhaseTest` difference remains of the three |
| O3 (controller law) | a state-change refusal scales the step by `clamp(0.9 sqrt(cap/change), 0.1, 0.9)` instead of halving; no growth on the step after it | passes but still locked (443 attempts per slice, pool 0.71 kg): the cost still grows with `q / m` (`logs/09`) | fluid suites (`logs/11`): a **new nonconvergence** (`SolidChainTransportTest`: 20 refusals at 0.0526, the algebraic-jump rule needs a halving), 11 junction lines moved, MIXED_GAS_COST 312. Rejected |

Not prototyped: crediting the step's inflow of a phase to its D11 capacity (a through-flow passes without a pool; changes D11's rows); splitting the capacity among the ends that draw (review "Island holds F1 and F2", T4): halves the pool, the lock condition is the same.

**Recommendation: O2.** It removes the lock by construction (a doubling is refused only when `q dt > 0.05 rho_ref V`, 50 kg per m3 of vessel per step), so its worst case is `2 T q / (0.05 rho_ref V)` attempts, below the limit for any feed under about 5 t/s per m3 of vessel; gas transients stay controlled by the pressure measure (a gas's mass moves with its pressure); junction lines and chain-100 do not move. What it costs: a capacity-bound pool follows the step the controller now takes (70 kg in this rig instead of 0.2-1.1 kg; it was step-dependent before too, 0.28-1.10 kg across the variants), and three tests to restate. Decisions it needs: `rho_ref` (1000 or 100 kg/m3) and the three re-baselines. The accepted fix brings `FedTankOpenOutletsTest` into `src/test` as its gate.

### 4. Gates

None run: no code changed. The option measurements above ran one Gradle invocation at a time on an idle machine.

### 5. Material

`tools/phase-ports-probes/holds2/`: `README.md`; `src/FedTankOpenOutletsTest.java` (reproduction and acceptance test), `src/FedTankOpenOutletsProbeTest.java` (variants, tracer), `src/instr-f7-step-trace.patch`, `src/option-o1-capacity-time-floor.patch`, `src/option-o2-mass-floor-liquid-capacity.patch`, `src/option-o3-error-scaled-reduction.patch`; `logs/01`-`14`.

## GUI layout at small scaled screens (2026-09-27)

- Author: Claude (Opus 5.5), worktree `.claude/worktrees/remove-agents-attribution-line-3d0c97`, branch `claude/phase-ports-compressor` at `17e43270`, owner's Windows machine, JDK 21.0.11. Code commit `8b43cfdd` (`Phase-ports GUI: device pages lay out without overlap at small scaled screens`); this section, `DECISION_LOG.md` A58, `CHANGELOG.md` and the material in the commit after it. Not merged, not pushed.
- The defect: open item (a) of "GUI fixes F3-F5 (2026-09-27)". At 854x480 GUI scale 2 (427x240 scaled) and at 1920x1080 with the automatic scale 4 (480x270), the screen is 250 (254) GUI px high; the pump, compressor, generator and filter pages started their value column at y = 218 and drew it over the bottom buttons ("Temperature -23.8 C" under Close, screenshot `gui-fixes/07`), the tank's Mass line touched the separator, a void's third and fourth lines and a pipe's phase ratio and status ran under the separator, and lines that did not fit were dropped without a sign (pressure change, the compressor ratio, pipe status lines past the separator). Found on the way: at 427 px wide the Apply button (81..191) overlapped the basis toggle (169..287).

### 1. The rule (A58)

One rule for every page the screen draws: **the rail below "Operating conditions" is one column clipped to the area above the status separator and scrolled by its own bar when it is longer than that area**, as the table area scrolls its rows. Nothing moves to another page; nothing is dropped.

| file:line (at `8b43cfdd`) | change |
|---|---|
| `client/gui/screens/inventory/FluidDeviceScreen.java:57-67` | the rail's area: top 90 (pipe 78), bottom `imageHeight - 52` (the divider's end, above the separator at `imageHeight - 49`); scroll in steps of 10 GUI px (`ProcessScroll`, the wheel moves 3 steps = one 30 px value line); the bar sits on the rail/table divider (x = rail + 10 .. rail + 18), between the rail text (ends at rail + 10) and the table (starts at rail + 24) |
| `:120-121`, `:127-131`, `:171-183` | rail controls (the fields of a generator, pump, compressor, valve, void, pipe; a filter's Recover solids button) are placed at their scrolled position and shown only when wholly inside the area (their labels are clipped with the column); a hidden field takes no clicks or keys |
| `:346-354`, `:368-393` | the column is drawn under a scissor of the area; the value lines start one control pitch (42 px) below the last control, or 4 px below the top when there is none (tank; unchanged at 94): generator, pump and compressor values now start at 174 (was 218), a void's at 132 (was 146), a filter's at 132 (was 218). The `y + 30 < imageHeight - 55` guards that dropped the pressure-change and ratio lines are gone. The column's drawn length sets the bar each frame; a changed offset rebuilds the widgets on the next frame |
| `:429-457` | a pipe's rail from the scrolled top (junction label, throughput, speed, gradient, phase ratio, status): every status line is drawn (was cut at the separator); the hover hints of these lines apply only inside the area |
| `:535-550` | the wheel over the rail (left of the table, inside the area) scrolls it; the bar takes clicks and drags like the table's |
| `:122-126`, `:156-166` | the bottom row: Close and Apply from the left, the basis and unit toggles from the right, all four narrowed in proportion when their natural widths (65, 110, 118, 108 + gaps = 439 px) do not fit (411 px at 427 wide: 60, 102, 109, 100); vanilla buttons scroll a label that no longer fits, and each keeps its tooltip |
| `:315-319`, `:324-328` | a text line the screen cuts to its column (status line, field labels such as "Max pressure ratio (out/in)", rail values, the connections hint) shows its full text as a tooltip while hovered; tooltips wrap to half the screen width (a long HELD status was one line wider than the screen) |
| `build.gradle:138-142` | `runMcpClient` window size from `-PmcpClientWidth` / `-PmcpClientHeight` (default 854x480, validated), used for the 1080p check below |

No wire, snapshot, lang or server change; no label shortened. What each page shows is unchanged; where: the values moved up on four device kinds and scroll at small screens. At GUI scale 1 (854x480 scaled) every rail fits and no bar shows.

Rejected: moving the values into the table area (a new tab for each device kind or a second table: two places to look on the large screens where both fit); per-kind start rows with a smaller pitch (a compressor's two fields and five values do not fit into 108 px at any readable pitch); pixel-exact scrolling (a wheel click of 1 px) or scrolling by whole items (items of 42, 30 and 10 px would need a variable bar). The 10 px step leaves a clipped half line at the column's top edge when it is scrolled to its end.

### 2. Gates (once each, idle machine, logs in `tools/phase-ports-probes/gui-layout/logs/`)

| # | command | result |
|---|---|---|
| G1 | `./gradlew --no-configuration-cache --no-build-cache test --rerun --tests com.wormzjl.createcheme.science.fluid.* --tests com.wormzjl.createcheme.runtime.fluid.* --console=plain --continue` (`06`) | **476/476** in 105 classes, 0 skipped; **MIXED_GAS_COST 286** Newton solves (284 ms) |
| G6 | `build -x test` (`01` before the client runs, `07` after the last edit) | BUILD SUCCESSFUL |

G4/G5 (GameTest compile and run) were not re-run: the change is client rendering and one run-configuration argument; `runMcpClient` compiled and loaded the mcpCompat source set twice.

### 3. In game (`tools/phase-ports-probes/gui-layout/NOTES.md`: lane, bridge calls, devices, screenshots)

`runMcpClient --offline`, bridge HTTP only, no desktop input, the previous pass's world "GUI fixes F3-F5" reopened (format 6; `New World` untouched), a line added at z = 40 with a pump, inline filter, pressure control valve, junction pipe and two voids.

| window, scale (scaled screen) | pages | observed |
|---|---|---|
| 854x480, 2 (427x240), `s2-*` | tank (Overview, Connections by face), generator (Overview, Composition, Solids), compressor, pump, filter, valve, void (without and with state), pipe, junction pipe (Overview, Connections) | the rail stops at the separator; the bar appears where the column is longer (tank 1 step, generator and pump 6, compressor 12, filter, void with state, pipes); scrolled to the end every line is readable: compressor `Pressure change 105.3 kPa`, `Discharge / suction pressure 3.0` (not drawn before at this size), tank `Mass 716.1 kg`, the pipe's full four-line HELD status; the four bottom buttons separate; no widget under the column's edges |
| 854x480, 1 (854x480), `s1-*` | the same device kinds | every rail fits, no bar; generator/pump/compressor values directly below the fields |
| 1920x1080, auto 4 (480x270), `a4-*` | compressor, tank, generator, pump, filter, valve, void, pipe, junction pipe, each opened and scrolled to the end | the bridge captures only the bottom-left 854x480 framebuffer pixels (GUI x 0-213, y 150-270): the region of the defect; the rail's last line sits above the separator, the status line and Close/Apply are clear |

Not shown: a tooltip of a truncated line (the bridge cannot move the cursor without desktop input; the cursor stays at the window centre, which at scale 2 lies in the table area: `s2-08` shows the component search box's own tooltip there, pre-existing).

HELD lines in the client logs (not investigated): run 1, `fluid_island=46` (the new line: generator, junction pipe with its branch void, pump, pipe, filter), 9 lines `status=HELD: Singular Newton Jacobian: Sparse LU rejected a singular matrix; active-set pass=0` at `committed_tick=11283`, the first 60 ms after the filter was placed; run 2 (same world reloaded) none.

### 4. Open

1. `-PmcpClientWidth/Height` is a run-configuration switch that serves GUI checks; it stays with the `mcpClient` run configuration (itself a check lane) unless the batch cleanup detaches that lane.
2. The island-46 hold (singular Jacobian on a water generator -> junction -> pump -> filter line, placed block by block) is recorded, not diagnosed.

### 5. Material

`tools/phase-ports-probes/gui-layout/`: `NOTES.md`, screenshots `s2-*` (24), `s1-*` (10), `a4-*` (18), `logs/01`-`07`, `line.txt`, `open.sh`, `endshot.sh`, `options-before.txt`.

## Island hold F8: singular Jacobian after a topology event (2026-09-27)

- Author: Claude (Opus 5.5), worktree `.claude/worktrees/remove-agents-attribution-line-3d0c97`, branch `claude/phase-ports-compressor` from `8a58e4c1`, owner's Windows machine, JDK 21.0.11. Code commits `28675064`, `4855fe82`, `65583a38`; this section, `DECISION_LOG.md` A59-A62, `CHANGELOG.md` and the material in the commit after them. Not merged, not pushed. Material: `tools/phase-ports-probes/holds3/` (README, probes, instrumentation, logs numbered as cited).
- The defect: open item 2 of "GUI layout at small scaled screens (2026-09-27)": a line placed block by block at z = 40 (`gui-layout/line.txt`: water generator, junction pipe with a branch pipe to a void, pump facing east, pipe, inline filter facing east, pipe, pressure control valve, pipe, void) logged `fluid_island=46 committed_tick=11283 status=HELD: Singular Newton Jacobian: Sparse LU rejected a singular matrix; active-set pass=0` nine times, the first 60 ms after the filter was placed; after a world reload no HELD line was logged.

### 1. Reproduction (runtime path; `src/test/.../InlineFilterLineHoldTest.java`, fails 3/3 on `8a58e4c1`, `holds3/logs/06`)

The test's world is the real `PhysicalRegistry` and `IslandCoordinator` with one synchronous worker, run tick by tick as `FluidWorldAuthority.tick` runs; every block is placed as `FluidWorldAuthority.place` places it (default control and spec) at the online tick the client log gives it (the filter at 11283, the committed tick of every held line; the others from the log's wall-clock stamps at 20 TPS). On `8a58e4c1` it is the in-game run: the island holds at committed tick 11283 at its first one-tick slice after the filter's event, the later placements (pipe, valve, pipe, void) stay queued behind it, and by online tick 24168 (the second client run's load) it has logged exactly nine held lines (`holds3/logs/06`). Every intermediate state was run for two minutes on its own (`everyPrefixOfTheLineNeverHolds`): prefixes of 1 to 6 blocks (the line ending at the generator, the junction, the pump, the pipe after the pump) are clean; from the filter on every prefix holds at the filter's tick.

"A reload cured it" is not what happened (`holds3/src/F8ReloadProbeTest.java`, `logs/06`): the save kept the island held at 11283 with its retry deadline at tick 30418, 6250 ticks after the load at 24168 (the retry wait had doubled to 64 cadences); the second client run lasted 154 s (3080 ticks), so no retry fell due and nothing was logged. Loaded through the checkpoint codec and run on, the island holds again at 30418 and at every retry after it.

### 2. Root cause, by numbers (`holds3/instr/`, `logs/03`)

The island after the filter's event: generator G, the branch junction J (pipe to the void), the pump's own junction P, the pump edge P -> F- (target 0.01 m3/s, mode PUMP_TARGET at pass 0), and the filter's two junctions F- (its block) and F+ (the compiler's junction on its positive side, which nothing past it reaches: the pipe east of the filter did not exist yet). F- and F+ are joined by the filter edge only; together they form a pocket whose only other connection is the pump. A junction's mass is pinned and its pressure algebraic (the mass row), in the rate pass and in every backward-Euler step alike, so:

- the pocket's two mass rows force the filter's flow and then the pump's flow to zero, while the pump's target row asks for 0.01 m3/s: no root;
- the pocket's pressure level has no row: the hydraulic rows of the filter and the pump see only pressure differences, and the pump's head is free in PUMP_TARGET.

The SVD of the failing matrix (19 x 19, the interval-start rate pass of `SolidEventIntegrator`, which every island with a filter runs): smallest singular value 1.36e-17 (the next 5.7e-5, the zero-flow friction slope of the generator and void pipes); right null vector on the pressures of F- and F+ and the pump's head, 0.575 / 0.575 / 0.582 (a uniform shift of the pocket and the head); left null vector on the two pocket mass rows (0.098 each) and the pump's target row (-0.979). The same structure is singular in every backward-Euler step (0.05, 0.0125, 0.001 s) and in a one-tick interval. The pass loop leaves an active set only from a converged point, so no pass, step size or retry could leave it. The interval-start rate pass's failure is not caught (it holds with the bare message, no "Substep" prefix); the steps' failure would have held with "Substep refinement exhausted".

The same start holds every line in which a pump discharges into a pressure control valve: the valve starts CLOSED (its upstream junction's minted pressure is below the set point), which seals the pump's discharge junction the same way. `pumpLinesIntoValvesAndFiltersNeverHold` places 12 lines in one tick: on `8a58e4c1` GPMPVPO, GPMPF, GPMPV, GPMPVP, GPMPFPVPO, GPMPFP, GPMPVPT and GPMPFPVPT (G generator, P pipe, M pump, F filter, V valve, O void, T tank) held at their first slice (`holds3/logs/04`).

Nothing about the event path differs from the load path: the event path compiles the component and mints its junctions with `sizeJunctionHoldups`, the load registers the saved island as it was; the reload changed only the retry deadline's timing.

### 3. Fix (structural, no model change; implemented)

| commit | file | change |
|---|---|---|
| `28675064` | `PassiveStepSolver` | `pockets`: a pass's pockets, junctions joined by pressure-coupling connections with no pressure anchor (no coupling connection to a vessel, port, generator or void, no regulating valve drawing from them). `Equations.retainedPressures`: a sealed pocket (every connection on its boundary closed) keeps its first member's pressure in place of that member's mass row, which is identically the sum of the others'; a pocket of one junction is the existing isolated-junction rule, bitwise. `relieveSealedPockets` at the start of every solve: a pocket a mover pushes its target into, with a closed pressure control valve drawing from it whose far end is anchored, starts that valve regulating |
| `4855fe82` | `PassiveStepSolver` | `Equations.deadEnd`: the rule that keeps the velocity cap off an edge into a junction whose other connections are closed looks through the junctions that end reaches by coupling connections (`sealedWithout`): the cap's branch has no pressure column, and a pocket fed only through a capped edge had none either (fuzz seed 61: a generator feeding a filter's two junctions through a 0.01 m pipe, a closed pump behind them) |
| `65583a38` | `PassiveStepSolver` | a mover at its target or head limit with a sealed pocket on either side (`sealedWithout` with the mover left out) starts CLOSED at its shutoff for the solve (`atShutoff`, as the pass loop marks a pump it finds at its shutoff corner), replacing the head-limit start of `28675064`, which also had a root but placed the pocket at the mover's shutoff pressure, outside the property range for a compressor or a 1 MPa pump (fuzz: 2.5 MPa of nitrogen, a suction pocket below 100 Pa) |

The converged states are the ones the pass loop's own transitions accept: a pump that can deliver runs at its target through the regulating valve (GPMPVPO, GPMPFPVPO: `[PASSIVE, PUMP_TARGET, VALVE_REGULATING]`), a pump into a dead end stands closed (GPMPF, GPMPV, GPMPVP, GPMPFP). No tolerance, iteration budget, attempt limit or conservation check changed. Gate test `InlineFilterLineHoldTest` (4 tests): the in-game line (the pump at its target through the regulating valve at the end), every prefix, the 12 layouts, and seed 61's pocket fed through a small pipe (fails without `4855fe82`, `holds3/logs/10`).

### 4. Gates (each once, idle machine, logs in `tools/phase-ports-probes/holds3/logs/`)

| # | command | on `4855fe82` | on `65583a38` |
|---|---|---|---|
| G1 | fluid suites (`--tests com.wormzjl.createcheme.science.fluid.* --tests com.wormzjl.createcheme.runtime.fluid.*`) | **480/480** in 106 classes (476 + 4), **MIXED_GAS_COST 286**, 33 junction lines **identical** to `holds/12-junction-lines.txt` (`11`) | **480/480**, **286**, identical (`17`) |
| G1' | the suites' printed lines of every other test against the same suites on `8a58e4c1` (`15`, `holds/src/StdoutCompare.java`) | 5469 lines, **0 differ** | 5469 lines, **0 differ** |
| G2 | `fluidSolverRegression -PfluidRegressionMode=exact` | **0.000e+00** (`12`) | **0.000e+00** (`18`) |
| G3 | adjacent selection | **45/45** (`13`) | **45/45** (`19`) |
| G5 | `runFluidGameTestServer` (fresh run ids `hold-f8-20260927`, `hold-f8-final-20260927`) | **33/33** (`14`) | **33/33** (`20`) |

### 5. Material

`tools/phase-ports-probes/holds3/`: `README.md`, `holds3.init.gradle`, `summarize.sh`, `src/F8ReloadProbeTest.java`, `src/F8LayoutProbeTest.java`, `instr/instr-f8-singular-dump.patch`, `instr/F8NullVectorProbeTest.java`, `logs/01`-`20`.

## Hold search by placement-sequence fuzz (2026-09-27)

- Author: Claude (Opus 5.5), same worktree and branch. Material: `tools/phase-ports-probes/hold-fuzz/` (README, harness, class reproductions, option patch, logs). Not a gate: attached to the test source set only by `hold-fuzz.init.gradle`.
- Why: three holds were found by chance in one day of dev-client checks (F1/F2, F7, F8); the owner's requirement is zero nonconvergence, and a hold in normal play blocks the merge.

### 1. Method

`HoldFuzzTest` builds, per seed, a world of 2 to 12 items on a grid from `new Random(seed)`: tanks (1 m3 of nitrogen at 1 atm), generators of water, nitrogen, methane or the two-phase light crude at a random temperature and pressure inside the package's range, voids, pipe runs of 0.01, 0.02, 0.05 or 0.1 m, pumps (0.001-0.05 m3/s, 0.2-1 MPa), compressors (0.01-0.2 m3/s, ratio 1.5-6), inline filters and pressure control valves (120-400 kPa), each joined to the world by a pipe run leaving a random face of an existing block (tanks are reached from their top, bottom and side faces; branches make junction pipes; axial devices face either way). The blocks are placed in a random order at random online ticks (20 % in the same tick as the previous one, 50 % 1-60 ticks later, 30 % 60-400 ticks later, so events fall inside and between slices) through the world's event path (`PhysicalRegistry.submit`, `applyPending`, `IslandCoordinator.topology`, one synchronous worker, the certificate policy on), then 120 slices at the 5 s cadence are run. A placement the property domain refuses is recorded apart (not a hold); every published HELD status and every exception is recorded with its seed, tick and island. A second distribution (`-PholdFuzzDefaults=true`) makes every block as the world places it (0.05 m, default controls, water generators at 298.15 K and 1 atm) from the same draws. Each seed is deterministic and replays alone; 13 class seeds replayed single-threaded reproduced their holds.

### 2. Campaigns (seeds 1-1000, 8 workers, one Gradle invocation each, nothing else running)

| code | distribution | seeds without a hold or exception | wall time (8 workers) | log |
|---|---|---|---|---|
| `8a58e4c1` (before F8) | random | 593 | 105 s | `07` |
| `28675064` (F8, pocket rule and head-limit start) | random | 670 | 129 s | `01` |
| `4855fe82` (+ dead-end cap rule) | random | 698 | 121 s | `06` |
| `65583a38` (+ closed start at shutoff; final) | random | **703** | 159 s | `08` |
| `65583a38` | world defaults | **702** | 256 s | `11` |
| `65583a38` + option R1 (prototype, not committed) | random | 720 | 503 s | `10` |

Against `8a58e4c1` the F8 commits clear 110 seeds and make none worse (every seed clean there is clean on `65583a38`).

### 3. Classes (by the primary failure of each seed's first hold; `classify.sh`, `logs/08` and `11`)

| class | what the status says | seeds (random) | seeds (defaults) | smallest seed | reproduction |
|---|---|---|---|---|---|
| (a) A: F7 state-change lock | `Interval substep limit`, state-change refusals >= 100 | 0 | 0 | - | known (`holds2/`) |
| (b) B: trace underflow | `Matrix values must be finite` after a long run | 2 | 1 | 523 | known (T1) |
| (c) C: thermo-domain wait (typed) | `HELD (thermo domain)` | 86 | 8 | 12 | legitimate: nitrogen or methane above 2 MPa (compressors up to ratio 6 and 1 MPa pumps charging tanks, 45), crude or methane below its minimum temperature (21), nitrogen, water or the envelope below 100 Pa (a mover drawing a closed vessel down, 16), crude below its minimum pressure (4) |
| (d) F8 | `Singular Newton Jacobian ... active-set pass=0` from a sealed pocket | 0 (was 61 on `8a58e4c1`) | - | - | fixed (`InlineFilterLineHoldTest`) |
| (e) D: singular interval-start rate pass | `Singular Newton Jacobian`, no `Substep` prefix | 6 | 7 | 118 | `HoldClassDSingularRatePassReproTest` |
| (e) E1: interval-start rate pass does not converge | `Newton iteration limit` / `line search stalled`, no prefix | 63 | 69 | 6 | `HoldClassE1RatePassNewtonReproTest` |
| (e) E2: step singular at every step size | `Substep refinement exhausted: Singular Newton Jacobian` | 29 | 9 | 4 | `HoldClassE2StepSingularReproTest` |
| (e) E3: vapour partial-pressure floor | `Hydrocarbon vapour partial pressure below the numerical floor` / `Water-vapor approximation outside ...` | 14 | **79** | 30 | `HoldClassE3VapourFloorReproTest` |
| (e) E4: reconstruction equation gate | `Conservative reconstruction fails equation gate` | 10 | 1 | 168 | `HoldClassE4EquationGateReproTest` |
| (e) E5: step line search stalls | `Substep refinement exhausted: Newton line search stalled` | 11 | 11 | 48 | `HoldClassE5LineSearchReproTest` |
| (e) E6: step Newton iteration limit | `Substep refinement exhausted: Newton iteration limit` | 21 | 46 | 33 | `HoldClassE6NewtonLimitReproTest` |
| (e) E7: active-set cycle | `Phase/device active-set cycle` | 4 | 3 | 325 | `HoldClassE7ActiveSetCycleReproTest` |
| (e) E8: attempt limit, not F7 | `Interval substep limit`, Newton failures | 2 | 2 | 613 | `HoldClassE8AttemptLimitReproTest` |
| (e) E10: no finite Jacobian at a first step | `No finite Jacobian trial` right after a placement | 3 | 0 | 333 | `HoldClassE10NoFiniteJacobianReproTest` |
| (e) G: zero row | `Singular matrix: empty or zero row` | 1 | 0 | 236 | `HoldClassGZeroRowReproTest` |
| (e) F: budget | `wall deadline`, `soft budget; approximate fallback refused` | 11 | 26 | 39 | `HoldClassFBudgetReproTest` |
| (e) X: exception | `IllegalStateException: Topology event reached boundary/filter ... outside the islands it fenced` | 35 | 35 | 28 | `HoldClassXTopologyExceptionReproTest` |

All 13 reproductions fail on `65583a38` (`logs/12`). A seed with both a hold and an exception counts in both rows.

### 4. The new classes (one paragraph each; no fix, none is the F8 defect)

- **X (35 seeds, seed 28): a crash, not a hold.** `PhysicalRegistry.apply` throws `Topology event reached boundary 1 outside the islands it fenced` on the server tick. `submit` walks from an edit through devices no island owns and stops at a device a queued event places ("this event now follows it"); when that queued event applies, the device it placed can end up owned by no island (a dead-end pipe: seed 28 places a pipe next to a lone void at tick 272 and the next pipe beside it at 273, before the void's island had aligned for the first), so the later event is never fenced on the island beyond, and the batch's component walk reaches that island's void or filter. Placing blocks a tick apart next to an island that is mid-slice is ordinary building.
- **E3 (14 random, 79 with world defaults, seed 30): gas meeting a water-seeded junction.** Seed 30's island is four nodes: a junction minted from a 361 kPa water generator (pure water), a nitrogen generator at 366 kPa, the water generator by two pipes and a void. When the nitrogen arrives, the junction's new vapour phase starts with its nitrogen partial pressure below the model's numerical floor, and every step size fails the same way (20 rejections). With the world's defaults, water generators at 1 atm draining into nitrogen voids and tanks do this constantly: it is the most common hold of default play.
- **E1 (63 / 69, seed 6) and D (6 / 7, seed 118) and G (1, seed 236): the interval-start rate pass.** Every island with an inline filter runs one rate solve of its port graph at each interval's start (`SolidEventIntegrator`, the solid closure check), without step control and without a fallback: its failure holds the island with the bare message. E1: the pass does not converge (seed 6: a two-phase crude generator at 281 kPa and 388 K into junctions minted with the nitrogen of a void at 1 atm, residual 0.43 after 20 iterations at pass 0). D: the pass is singular because every connection from the island's junctions to a pressure anchor starts on the velocity cap's branch, which has no pressure column (seed 118: SVD null vector on the pressures of 11 junctions and three mover heads, left null vector on their mass rows and the two capped generator connections; seed 175 the same with a port and a void). G: a component row of a two-phase crude junction has no entry (seed 236). Option R1 (measured, `option-r1-rate-pass-failure-tolerated.patch`): a failed interval-start rate pass declares no closure, as the interval solver's own port-rate check already does. It clears D, E1 and G as classes, but only 17 more seeds come clean (703 -> 720, none worse): the same islands then fail in their steps (E2 29 -> 42, E6 21 -> 38), and the campaign takes 503 s instead of 159 s. The rate-pass failure is mostly an early symptom of the step classes.
- **E2 (29 / 9, seed 4): a singular step at every step size.** Seed 4: junctions minted with 472 kPa methane gas, a two-phase crude generator at 261 kPa, a valve to a void, 0.02 m pipes; after 5 active-set passes the step matrix is singular, near-null direction (sigma 1.3e-12) along the liquid amounts of a two-phase crude junction together: its phase split is not determined (the two phases alike, or a phase at the edge of its support). Also seed 474 (on `28675064`, `logs/03`): a steam junction between two capped connections (sigma 6e-13), the cap's branch again.
- **E6 (21 / 46, seed 33) and E5 (11 / 11, seed 48) and E8 (2 / 2, seed 613): the step Newton stops at every step size.** Seed 33 is pure water: one 262 kPa generator, two voids, loops of 0.01-0.05 m pipes; the Newton stops at its iteration limit at 3.4e-7 against 1e-9 at every step size. The velocity cap's two branches (the row switches form at the cap's drop) on the small pipes are the suspect, not measured. Seed 48: a two-phase crude generator, a 0.05 m3/s pump and valves into a tank; the line search stalls at 0.27 at pass 0. Seed 613: the attempt limit runs out on line searches stalled at 1.3e-9 against 1e-9 in water junctions holding trace vapour (the F1 family).
- **E4 (10 / 1, seed 168): the equation gate.** Water junctions holding vapour shares of 1e-233 to 1e-278 (nitrogen washed out of water by backward Euler, the F1/F2 family); the reconstruction fails the gate after singular and stalled passes. T1 (trace flush) is the candidate, not measured.
- **E7 (4 / 3, seed 325): active-set cycle** at every step size (crude networks); not diagnosed further.
- **E10 (3, seed 333): no finite Jacobian trial at the first step after a placement.** A 376 K crude generator into junctions minted with 268 K nitrogen: a column cannot be differenced inside the property domain from the minted start. Not the trace underflow (it happens in the first slice).
- **F (11 / 26, seed 39): budget holds.** The full solve exceeds the 2 s wall budget, or the soft budget's approximate fallback is refused, at every retry; seed 39 held the same replayed alone on one thread, so it is the solve's cost, not the load of 8 workers.

The common thread of D, E1, E2, E3, E10 and part of E6: a junction is minted with the island's first boundary's state (`PhysicalFluidTopology.compile`: "a property guess"), and a network mixing a gas and a liquid, or two-phase crude, starts every junction far from where its flows put it; the rate seed and the first steps start there.

### 5. What needs an owner decision

1. The hold rate: 297 of 1000 random worlds and 298 of 1000 default-play worlds still hold or throw; the owner's zero is far away. Which classes to attack first: by default-play frequency E3 (79), E1 (69), E6 (46), X (35, a crash), F (26).
2. X is an event-path defect with a crash on the server tick; a fix (fence a later event through a queued device's reach when that event applies, or re-walk from the touched set at batch time) changes no model. Recommended first.
3. Option R1 (measured above): a policy for the solid closure check's rate pass.
4. The junction mint: minting junctions with the island's first boundary's state is the start point most classes fail from; a mint from the reachable boundaries (per junction, or a mixed state) changes the start of every multi-boundary island (not bitwise).

### 6. Material

`tools/phase-ports-probes/hold-fuzz/`: `README.md`, `hold-fuzz.init.gradle`, `classify.sh`, `src/HoldFuzzTest.java`, `src/repro/HoldClass*ReproTest.java` (13), `instr/HoldSeedSingularProbeTest.java`, `option-r1-rate-pass-failure-tolerated.patch`, `logs/00`-`12`.

## Holds F7 and the trace family: the owner's decisions implemented (2026-09-27)

Written by the orchestrator (Claude Fable 5.1), who made both changes in the session after the owner confirmed them in chat ("the pump will mainly work with hydrocarbons so make reference 900, other go with recommended"; "yeah go ahead"). Logs: `tools/phase-ports-probes/holds2/logs/20`-`22` (F7), `tools/phase-ports-probes/trace-flush/logs/01` and `tools/phase-ports-probes/hold-classes/logs/t01`, `t02` (gates and campaign).

### 1. F7: the mass reference of the step controller (`55ba39a6`, tests `9a1c840e`)

`PassiveIntervalSolver.stateChange` measures a vessel's mass change against `max(mass, STATE_MASS_REFERENCE_DENSITY x volume)` with 900 kg/m3 (option O2 of "Island hold F7"; the owner chose 900 for liquid hydrocarbon service). The pressure check and the 5 % cap are unchanged. `FedTankOpenOutletsTest` (the in-game placement order, and the generator before the outlets) is the gate test, 2/2.

Tests restated, old and new:

| Test | Assertion | Old | New | Measured |
|---|---|---|---|---|
| `McpGameplayRegressionTest.belowSeaLevel...` | pressure against the refined run | within 0.1 % | within 0.3 % | 129960.05 against 130299.51 Pa, 0.26 % |
| same | mass against the refined run | within 0.2 % | within 1.2 % | 201.288 against 203.412 kg, 1.04 % (was 203.164 kg, 0.125 %) |
| same | average flow against the refined run | within 0.2 % | within 1.2 % | 40.029 against 40.453 kg/s, 1.05 % |
| `InletPhaseTest.refusedPumpsCarryNothing...` | receiving tank's pressure and temperature against the line alone, 0.1 s slices | 1e-13 relative | 1e-12 relative | 97673.64797358793 against 97673.64797361186 Pa, 2.45e-13; the flow comparison keeps 1e-13 |
| `VentGatePolishTest.theBulkVentControlIsNeverRefusedByTheGate` | the run needed a polish | required | not required | 0 polishes (before, one per 5 s slice); 0 gate refusals as before |

**Accuracy cost, stated plainly.** The first test compares the normal run with a refined run (steps 0.00005 to 0.02 s, 260 accepted) of a 5 s water fill into a 1.2 kg nitrogen tank. Before the change the tank's small mass forced small steps; now only the 5 % pressure cap limits them and the slice takes 10 steps. The error against the refined run grew eight times in mass (0.125 to 1.04 %). It is the first-order backward-Euler error at the larger step, of the kind the owner accepted with the 5 s cycle (mixed-gas batch D4), and the lever against it is a smaller reference density (100 kg/m3 kept the steps smaller in the diagnosis, with ten times less margin against the hold). The owner was told on 2026-09-27 that the earlier description of this test ("a recorded pressure moves") understated it.

### 2. Trace flush (`f80def43`)

At each accepted step, a component below `TRACE_FLUSH_FRACTION` = 1e-24 of its vessel's or junction's total amount is removed and the node restated at its temperature and pressure (`PassiveIntervalSolver.flushTraces`, called between `replace` and `markBackwardEulerAccepted`). Option T1 of "Island holds F1 and F2", with the owner's two choices: threshold 1e-24, removed amount booked.

- **Booking.** What is removed is booked as a `ConservativeTransport.BoundaryTransfer` at the node's own id, signed like a port's (positive = into the network): the removed moles negative, the energy the change of the node's owned total energy. The inventory's internal-energy field is kept, so that energy is the potential energy of the removed mass, `-m g z`. The interval's ledger (owned after - owned before = boundary transfers) therefore closes by construction. Checkpoints write boundary transfers with any node id (`FluidCheckpointCodec` :345-346), and `ModuleTransferPlanner.completed` passes a transfer at an unscheduled node through unchanged.
- **Certificates.** `IslandCertificate.Summary.flushed` is set when an interval booked a transfer at a node the island owns; `issue` then refuses ("the interval flushed a trace component"). A one-time removal must not be replayed as a rate. The next interval has an exact zero for that component and certifies as any other. During certified replay nothing is solved, so nothing is flushed.
- **Counter.** `SolverDiagnostics.traceFlushes`.
- **What it does not change.** The 33 junction lines are identical and chain-100 is exact: 1e-24 of a node is below the last bit of the node's total, so no other number of the node moves, and no reference case holds a geometrically shrinking component.

Gate tests (all in `src/test/.../runtime/fluid/`):
- `TraceFlushLedgerTest`: two equal vessels at rest, one with a methane trace at 1e-26 of its amount. The trace is removed at the first step; one transfer is booked at the vessel with only that component and the potential energy of its mass; the component and energy ledgers close to 1e-12; the interval is refused a certificate; the next interval has no flush. A trace at 1e-20 stays.
- `TraceFlushLiquidFullTankTest`: the F2 tank through the whole lifetime of its nitrogen: full at slice 25, the vapour exactly gone at slice 42, 17 slices of steady overflow. Before, a hold after about 27 minutes.
- `TraceFlushFedPumpTest`: F1's pump on a fed tank, 400 slices. Before, a hold at slice 173.

### 3. Gates on `f80def43` (`hold-classes/logs/t01-*`)

| Gate | Result |
|---|---|
| Fluid suites | 488/488 in 111 classes; MIXED_GAS_COST 286 Newton solves, 279 ms, 135.7 MB |
| 33 junction lines against `holds/12-junction-lines.txt` | identical |
| Exact regression | 0.000e+00 on all four quantities |
| Adjacent | 45/45 |
| Fluid GameTests, fresh world (`trace-flush-20260927`) | 33/33 |

### 4. Fuzz campaign on `f80def43` (`hold-classes/logs/t02-*`), against `9079a15a`

| Distribution | Clean at `9079a15a` | Clean at `f80def43` | Cleared | Worse |
|---|---|---|---|---|
| random | 735 | 746 | 11 | 0 |
| world defaults | 731 | 737 | 6 | 0 |

Classes (a) the F7 lock and (b) trace underflow are empty. E4 (the equation gate at vapour shares of 1e-233 and below) still has 2 random seeds (454, 719): the flush acts on a component's share of the node, not on a phase's share, so a vanishing phase of a component that is plentiful in the node's other phases is not covered. It stays with the remaining classes.

## Hold class X: a topology event outside its fence (2026-09-27)

- Author: Claude (Opus 5.5), same worktree and branch, from `b3c44ed0`. Code commit `9079a15a`. Material: `tools/phase-ports-probes/hold-classes/` (README, scripts, probes, logs `x01`-`x07`).
- Not a hold: `PhysicalRegistry.apply` threw `IllegalStateException: Topology event reached boundary/filter N outside the islands it fenced` on the server tick (35 random / 35 default seeds on `65583a38`; 23 of the 70 lines name a boundary, 47 a filter).

### 1. Reproduction and cause (event-path logic, no model change)

`submit` walks from an edit through devices no island owns and stops at a device an island owns ("the fence covers that island whole") or a queued event edits ("this event now follows it"). `apply` later walks the whole connected component of the batch's touched devices and needs every boundary and filter it meets to belong to a fenced island. Two ways the fence missed one:

1. **A device a queued event placed ends up owned by no island** (seed 28, smallest world: a void at tick 105, a pipe beside it at 272 while the void's island is mid-slice, the next pipe at 273). The second event's walk stops at the first pipe (pending). When the first event applies, that pipe is a dead end, owned by no island; the post-batch refencing (`apply`'s last block) looked only at touched devices that a replacement island owns, so the second event was never fenced on the void's new island, and its component walk reached the void. 18 of the 37 X seeds.
2. **A filter in a dormant island of its own** (seed 234, smallest world: filters at (1,0,0) facing west at 517 and at (0,0,0) facing east at 718, then a tank at (-1,0,0) at 776). The compiler gives every filter of a component with no boundary its own dormant island, so two joined filters are one component and two islands; the tank's walk stopped at the first filter, whose island does not hold the second. The other 19 seeds (the 47 "filter" lines).

### 2. Fix (`9079a15a`, `PhysicalRegistry`)

- `reached(touched, within)`: the owners of the applied devices reached from an event's touched devices through devices no island owns. After a batch commits, every queued event is fenced on each replacement island this reaches (replaces the "touched device owned by a fresh island" rule, which is its first step). Only replacement islands can be newly reached: every island in the batch's post-batch component was affected by the batch, or `apply` would already have thrown; a fresh island's committed tick is the batch tick, so the fence is always admissible there. Fences carry to later replacements (`IslandCoordinator.topology` hands every original's remaining fences to its replacements).
- `standsForItsComponent(id)`: both walks stop at an owned device only if its island is not a lone filter's dormant island.

A latent path not exercised by the fuzz (which never removes a block) was read and left: a queued *removal* hides its device from a later event's `submit` walk (`latestAt` is empty there); the later event still meets the removal's footprint through the removed device's neighbours, which the removal's own walk touched, so no case was found; noted, not changed.

### 3. Gate test and campaign

`TopologyEventFenceTest` (2 tests, the two smallest worlds above, on the event path through the new shared test world `EventPathWorld`): both throw without the fix (`x03`), pass with it (`x04`). The 37 X seeds: 18 clean after part 1 (`x01`), all 37 free of exceptions after part 2 (`x02`; 32 clean, the rest now show their ordinary holds, C, E3).

| Distribution | Clean at `65583a38` (`hold-fuzz/logs/08`, `11`) | Clean at `b3c44ed0` + X (`x05`) | Cleared | Worse |
|---|---|---|---|---|
| random | 703 | 734 | 31 | 0 |
| world defaults | 702 | 730 | 28 | 0 |

No exception is left in either distribution.

### 4. Gates (`x06`, on `55ba39a6` + X, the F7 commit already in the tree)

Fluid suites 484 in 108 classes with the three F7-moved tests failing as expected before their restatement (`9a1c840e`), MIXED_GAS_COST 286, 33 junction lines identical; exact regression 0.000e+00; adjacent 45/45; GameTests 33/33 (`hold-class-x-20260927`). The orchestrator's gates on `f80def43` (488/488, above) include this commit.

### 5. Status after the class fixes (clean seeds of 1000)

| Commit | random | world defaults |
|---|---|---|
| `65583a38` (fuzz baseline) | 703 | 702 |
| `9079a15a` X | 734 (`x05`, 735 in `x07`) | 730 (731 in `x07`) |
| `f80def43` F7 + trace flush (orchestrator) | 746 | 737 |

## Hold class E3: a hydrocarbon arriving at a node with no hydrocarbon phase (2026-09-28)

- Author: Claude (Opus 5.5), same worktree and branch, on `2ade93d5`. Code commit `7e5391a7`. Material: `tools/phase-ports-probes/hold-classes/` (`src/E3ProbeTest.java`, `src/HoldMinimizerTest.java`, `src/instr-e3-interrupted.patch`, `src/instr-pass-loop-trace.patch`, logs `e01`-`e18`).
- Class on `f80def43`: 16 random / 82 world-default seeds, `Hydrocarbon vapour partial pressure below the numerical floor` (or `Water-vapor approximation outside ...`) at every step size.

### 1. Reproduction and cause

Smallest world (fuzz seed 30, `E3ProbeTest`): a 361 kPa water generator, a junction pipe with a void above it, a pipe to a 366 kPa nitrogen generator, placed in one tick; it holds at its first slice (`e01`). With the nitrogen generator placed first it runs clean: the junctions are minted from the island's first boundary, so the order decides whether the junction starts as pure water or as nitrogen.

The throw is not in the Newton: the stack (`e01`, `instr-e3-interrupted.patch`) is `HydrocarbonModel.phase` <- `FluidThermodynamics.state` <- `ConservativeTransport.repartition` <- `reconstruct` <- `PassiveStepSolver.solve`. A junction whose state holds no hydrocarbon phase gets no unknown for a reachable hydrocarbon (`PhaseLayout.support`: "an omitted trace ... on a point with no hydrocarbon phase to hold it in"), so the Newton solves it as pure water. The reconstruction then books the nitrogen the flows really deliver (109.5 mol at the first dt) and `repartition` places it in a new vapour evaluated at the state's hydrocarbon partial pressure, which is 0 for a state with no vapour: below the 1e-6 Pa floor, always. The converged-point phase correction that would reseed the node runs after the reconstruction and never gets the chance. Every step size is refused the same way. In world-default play this is the water line meeting a nitrogen tank or void.

### 2. Fix (`7e5391a7`; structural, a path that always threw)

- `ConservativeTransport.repartition`: a hydrocarbon booked into a node that holds no hydrocarbon phase (`appearsWithoutPhase`) restates the node as its flash at its own T and P, the phase set the booked amounts form. Every other node is restated as before.
- `PassiveStepSolver` pass loop: when the reconstruction gave such a node a phase for a component of at least `TRACE_FLUSH_FRACTION` (1e-24) of the node (`phaseAppeared`), the pass is solved again from the reconstructed states, as from any phase appearance. A component below that fraction is committed as reconstructed and removed at the same step by the trace flush (the owner's T1 rule). The D13 polish refuses such a point.

Measured before settling on the threshold (E3 seeds only): reseeding on every appearance (`e04`) cleared 4/16 random and 3/82 default seeds, the rest failing next in pass 2 on junctions holding nitrogen at 1e-33 to 1e-17 of their water (roundoff flows of 5e-14 kg/s from a 1 atm tank into 1 atm water junctions, `e09`, `e10`); with the threshold (`e13`) 6/16 and 17/82, and no E3 left.

### 3. Gate test, campaign, gates

`JunctionPhaseAppearanceTest` (seed 30's island at once; must keep up and hold the nitrogen in the junction's vapour): fails without the fix (`e15`), passes with it (`e14`).

| Distribution | Clean at `f80def43` (`t02`) | Clean at `7e5391a7` (`e16`) | Cleared | Worse |
|---|---|---|---|---|
| random | 746 | 753 | 7 | 0 |
| world defaults | 737 | 752 | 17 | 2 (803, 915: soft-budget holds; replayed alone on one thread both are clean with and without the fix, same times, `e17`) |

Classes after (random / defaults): C 90/13, D 8/7, E1 62/89, E2 24/8, E3 0/0, E4 6/13, E5 11/22, E6 28/55, E7 3/4, E8 1/5, E10 3/2, F 10/26, G 1/0, A 0/2, E9 0/2. Most E3 seeds now hold one step later in another class (E1, E4, E5, E6): the next layer is a junction holding a trace vapour (see E4 below).

Gates (`e18`): fluid suites **489/489** in 112 classes (488 + 1), MIXED_GAS_COST 286, 33 junction lines **identical**; exact regression **0.000e+00**; adjacent 45/45. Not bitwise everywhere: `PhasePortPriorityTest`'s overflow-0.1 fixture printed a different trajectory from slice 663 (the tank's water-full transition): three steps that were refused on this exception are now solved or refused on an active-set cycle instead (P after the slice 175681.48 against 175599.86 Pa, back to 1e-12 relative by slice 750); the test passes. No other printed line of the suites moved (the F7_SLICE lines differ only in map order).

### 4. Status after the class fixes (clean seeds of 1000)

| Commit | random | world defaults |
|---|---|---|
| `65583a38` (fuzz baseline) | 703 | 702 |
| `9079a15a` X | 735 | 731 |
| `f80def43` F7 + trace flush (orchestrator) | 746 | 737 |
| `7e5391a7` E3 | 753 | 752 |

## The junction-mint hypothesis, tested (2026-09-28)

The fuzz report suspected the start most classes fail from: every junction is minted with the island's first boundary's state. Scratch experiment (`hold-classes/src/scratch-e1-estimate-filterclose-mint.patch`, `-Pmint=`; never committed): every junction of a replacement island re-minted in `PhysicalRegistry.apply` from all its boundaries, on `7e5391a7`, against `e16`:

| Mint | random clean (worse / cleared) | defaults clean (worse / cleared) | classes that moved |
|---|---|---|---|
| first boundary (as built) | 753 | 752 | |
| M1 mixed: mean per-kg composition, mean T and P of the boundaries, flashed (`m01`) | 692 (83 / 22) | 717 (more than 50 / 28) | E2 24 -> 50, D 8 -> 17 (random), F 26 -> 51 (defaults) |
| M2 the first boundary's composition at the boundaries' mean pressure (`m02`) | 726 (49 / 27) | 749 (5 / 2) | E1 62 -> 43 but E2 24 -> 41 (random) |

Neither clears more than it breaks: a mixed mint puts phases into junctions that no flow brings there (E2, D), and a mean pressure is as wrong as the first boundary's, in a different set of worlds. The mint is not the one shared cause; where a class does fail from the minted state (E3 above, E1c below) the repair belongs to what reads it. Both variants would also move the start of every multi-boundary island (not bitwise). Not pursued further.

## Hold class E1: the interval-start rate pass (2026-09-28)

- Author: Claude (Opus 5.5), same worktree and branch. Code commit `bbb79790` (one of three mechanisms). Material: `hold-classes/` (`src/HoldMinimizerTest.java` minimize and replay, `src/instr-pass-loop-trace.patch`, `src/instr-newton-trace.patch`, `src/scratch-e1-estimate-filterclose-mint.patch`, logs `r01`-`r18`, `m01`, `m02`).
- Class on `7e5391a7` (`e16`): 62 random / 89 defaults; `Newton iteration limit` 57 / 69 (of them 20 / 39 end below 1e-6 and 26 / 7 above 1e-3), `line search stalled` 3 / 19, `active-set cycle` 2 / 1; all without the "Substep" prefix: the rate solve of the port graph at the interval's start in `SolidEventIntegrator`, which every island with an inline filter runs and whose failure holds the island.

### 1. Three mechanisms (smallest worlds by `HoldMinimizerTest`, traced with the two instrumentation patches)

- **E1a, a generator that can only receive, left two-way** (defaults seed 6, 11 blocks, `r06`-`r08`): a 1 atm water generator three blocks below a junction that joins two 1 atm nitrogen voids and a dead-end filter. The generator's run has no root in its allowed direction; the rate pass stalls on its hydraulic row (0.00199 at pass 0). Decision D12 (owner) makes generators one-way from the first pass, and `closeColdReceivingGenerators` closes such runs in a cold rate solve, but A34 left islands with a filter out, so the only islands that run this check never got the closure. **Fixed** (`bbb79790`).
- **E1b, a far start and the 20-iteration budget** (random seed 6, 10 blocks, `r01`-`r03`): a 388 K crude generator into junctions minted as 1 atm nitrogen from a void; the rate pass contracts from 42 to 6.2e-8 in exactly 20 iterations (the 21st would converge): 11 iterations of backtracking from the minted pressure (1 atm against the 265 kPa the junction settles at) before the chord phase. 20 random / 39 default seeds end below 1e-6 like this.
- **E1c, a junction whose rate-solve composition is the minted guess** (defaults seed 12, 10 blocks, `r10`, `r11`): a rate solve holds each junction's composition at its owned inventory, which on the first interval is the mint (nitrogen from the tank). Water from a 1 atm generator two blocks above drains through that junction into the tank's top port, so the junction takes water in and passes "nitrogen" out at the gas velocity cap: a stiff capped balance that contracts at 0.7-0.8 per chord iteration and ends at 0.0029.

### 2. Fix of E1a (`bbb79790`)

`closeColdReceivingGenerators` treats an inline filter's connection as passive (its block geometry in the one-way estimate, a start guess); actuated and solid islands stay excluded. Gate test `FilterIslandGeneratorClosureTest` (the E1a world): fails without the change (`r16`), passes with it (`r15`). Decision A68.

| Distribution | Clean at `7e5391a7` (`e16`) | Clean at `bbb79790` (`r13`) | Cleared | Worse |
|---|---|---|---|---|
| random | 753 | 757 | 5 | 1 (152: minimized, its hold is a step Newton limit in a world with no filter at all, reached on a different history of its other islands) |
| world defaults | 752 | 760 | 8 | 0 |

Gates (`r17`): fluid suites **490/490** in 113 classes, MIXED_GAS_COST 286, 33 junction lines **identical**; exact regression **0.000e+00**; adjacent 45/45; every printed line of the suites identical to `e18` (5814 lines; timing lines excluded, map order ignored; `holds/src/StdoutCompare.java`).

### 3. Options for the rest of E1 (measured, not implemented)

| Option | What | random / defaults clean (from 757 / 760) | Worse | Other effects |
|---|---|---|---|---|
| E1-O1 = R1 of the fuzz report | a failed interval-start rate pass declares no closure (the steps' own guards still see every transition, at t0 and inside the interval) | **782 / 779** (`r18`) | 0 / 1 (803, the load-sensitive budget seed) | classes D, E1, G gone; E6 grows 31 -> 47 / 55 -> 105 (the same islands then fail in their steps); campaign wall time 137 -> 519 s (random), 480 -> 533 s (defaults). A policy of the closure check: owner's decision |
| E1-O2, one-way estimate as the rate pass's start | the cold rate pass's junctions start at `oneWayPressures` (own T and composition), actuators left out of the estimate | 766 / 758 (with E1a, `r12`) | 3 / 4 (new rate-pass iteration limits: the estimate is a worse start for them) | clears E1b-type seeds, not E1c. A start heuristic that makes some islands worse: not implemented |
| E1-O3 (not prototyped) | a rate solve mixes a junction at its inflow (as a step does) instead of holding it at its owned inventory | | | E1c's cause; changes what every rate solve states for a junction and so the cold rate seed of every junction island (junction lines not bitwise): owner's decision |

Recommendation: R1. The check exists to pre-empt a closure the steps would otherwise find, and on an island's first interval its junction compositions are the mint (E1c), so a rate solve on them is no evidence either way.

### 4. Status after the class fixes (clean seeds of 1000)

| Commit | random | world defaults |
|---|---|---|
| `65583a38` (fuzz baseline) | 703 | 702 |
| `9079a15a` X | 735 | 731 |
| `f80def43` F7 + trace flush (orchestrator) | 746 | 737 |
| `7e5391a7` E3 | 753 | 752 |
| `bbb79790` E1a | 757 | 760 |
| (`bbb79790` + R1, not committed) | (782) | (779) |

## Hold class E6: the step Newton iteration limit at a zero-flow root (2026-09-28)

- Author: Claude (Opus 5.5), same worktree and branch. **No code change** (options below). Material: `hold-classes/` (`src/scratch-e6-cap-scale.patch`: switches `-Pcapscale`, `-Pcapscale2` against `bbb79790`; `src/scratch-e6-sided-flow-difference.patch`: the `SparseNewton` half of a sided flow difference that was measured and changed nothing; logs `s01`-`s09`).
- Class on `bbb79790` (`r13`): 31 random / 55 defaults, `Substep refinement exhausted: Newton iteration limit` at every step size.

### 1. Reproduction and cause (random seed 33, 14 blocks, `s01`, `s02`)

The held island is pure water and small: a 262 kPa water generator piped straight into a void, and a junction joined to that void (0.02 m) and to a second void (0.01 m), both voids at 1 atm and at the junction's height, plus a dead-end junction. Voids only receive, so at the root every flow through the junction is exactly zero and the junction sits at the voids' pressure: a zero-flow root.

The trace (`instr-newton-trace.patch`): the rate seed and every step size (1 s, 0.05, 0.025, ...) contract the same way, by a constant 0.33 per iteration on one row (the void-to-junction hydraulic row), from 0.45 to 1.6e-8 in 20 iterations, fresh Jacobians included; the last iterate carries a circulation of 2.4e-7 kg/s through the junction between the two voids.

Why linear: every passive connection's row is `(clamp(driving) - loss(q)) / S` with `S = min(pressureScale, 2 limitDrop)`, and `limitDrop` is the velocity-cap drop of the donor end chosen by the sign of the driving pressure (the "byDriving" rule). The two ends here are water and nitrogen: their cap drops differ by orders, so `S` jumps where the driving pressure changes sign, which at a zero-flow root is exactly the root. The pressure column's difference step (0.1 Pa) is far larger than the driving pressure near the root (4e-4 Pa), so every Jacobian straddles the jump; the Newton step is then off by a constant factor and the iteration contracts linearly. Measured: with `S` taken from both ends' drops (the smaller, `-Pcapscale`) the world runs clean (`s04`). A sided difference for the flow columns alone (the flow's donor also switches at zero flow) changed nothing (`s03`).

### 2. Options (measured, not implemented: each moves the all-BULK reference paths)

| Option | Change | E6 seeds (31 random / 55 defaults) | Reference paths |
|---|---|---|---|
| E6-O1 | `S` from the smaller of the two ends' cap drops (continuous across a sign change of the driving pressure) | random: 5 clean, E6 31 -> 11; defaults: 20 clean, E6 55 -> 11 (`s07`) | 25 of the 33 junction lines move (MIXED_GAS_TRANSIENT at 0.1 s), MIXED_GAS_COST 286 -> 273, `ExtremeTopologyIslandTest` GRID fails twice (a new hold in the extreme grid) (`s05`) |
| E6-O2 | `S` from the direction the pass starts in, frozen for the pass | seed 33 clean (`s08`) | 50 junction-line diff lines, MIXED_GAS_COST 277, 4 tests fail (`ExtremeTopologyIslandTest` GRID x2, `PhysicalRegistryTest` junction holdups, `PhasePortPriorityTest` bottom-port drain) (`s09`) |

Both show the lever is real (O1 clears 25 of 86 E6 seeds and turns more into other classes) and that the row scale of a connection is load-bearing for every mixed-gas path: a change there is the owner's (the scale was chosen in the mixed-gas batch, review 7.1/7.5, against the 50 mm cap-exit stall). A form that keeps `S` exactly as today away from a sign change and blends it across a band around zero driving was not prototyped; it would still move approach-to-rest paths.

### 3. Status after the class fixes

Unchanged: `bbb79790`, 757 random / 760 world defaults.

## Hold class F: budget holds (2026-09-28)

- Author: Claude (Opus 5.5), same worktree and branch. **No code change.** Material: `hold-fuzz/src/HoldFuzzTest.java` now reads `holdFuzz.wallNanos` / `holdFuzz.softNanos` (the coordinator's hard and soft budgets, default 2 s / 1.5 s as before), forwarded by `hold-classes/props.init.gradle`; logs `u01`-`u04`.
- Class on `bbb79790` (`r13`): 11 random / 25 defaults. Statuses: `wall deadline` 5 / 17; `soft budget; approximate fallback refused: ...` 6 / 8 (the approximate path refused on its own guards: linearized error does not contract, trust region, regime changed, a failed substep refinement).

### 1. What is under the budget

Replayed with ten times the budgets and 20 slices (`u03`): random 2 clean, 3 still over the budget, 3 C, 3 E8; defaults 5 clean, 12 E8 (every one "Newton iteration limit" or "Newton line search stalled" at every attempt), 5 E6, 1 E5, 1 E7, 1 C. With no budget at all the run did not end within 40 minutes for at least one random seed (stopped, `u02` not kept as evidence). So a budget hold is mostly a step failure of the E6/E5 kind that spends the budget before it exhausts the step ladder, and for a few worlds (about 7 of 36) a slice that converges but costs 2 to 20 s.

With E6's option O1 (the connection's row scale taken from both ends, `scratch-e6-cap-scale.patch -Pcapscale`) at the normal budgets (`u04`): defaults 13 of 25 clean (F 25 -> 3), random 1 of 11 (F 11 -> 8). The default-play budget holds share E6's cause; the random ones (crude, methane and compressors) are mostly genuinely expensive.

### 2. Options

No budget may be raised. F has no fix of its own: it follows E6 (option E6-O1 or another form of the row scale, owner) and, for the remaining expensive random worlds, the solve cost, which is outside this task (the soft budget's approximate fallback refusing on regime changes is recorded design).

### 3. Status after the class fixes

Unchanged: `bbb79790`, 757 random / 760 world defaults.

## Hold class E2: a singular step at every step size (2026-09-28)

- Author: Claude (Opus 5.5), same worktree and branch. Code commit `a3d3a789`. Material: `hold-classes/` logs `v01`-`v08` (`v02`: the SVD of the singular Jacobian with `holds3/instr/instr-f8-singular-dump.patch` and `hold-fuzz/instr/HoldSeedSingularProbeTest`; `v04`: the campaign).
- Class on `bbb79790` (`r13`): 25 random / 8 defaults, `Substep refinement exhausted: Singular Newton Jacobian` at every step size.

### 1. Reproduction and cause (random seed 4, 15 blocks, `v01`-`v03`)

A 472 kPa methane generator and a 261 kPa two-phase crude generator (369 K) feed a network of 0.02-0.05 m junction pipes with two pressure control valves (120 and 200 kPa) and a void; the junctions are minted from the methane generator. The singular Jacobian (pass 4, 170 unknowns, `v02`): smallest singular value 1.25e-12 (next 2.5e-9), right singular vector a uniform shift of every phase-split unknown `ln(v/l)` of two two-phase crude junctions, left vector the hydraulic row of the crude generator's connection (0.97): the phase split of those junctions is not determined (a vanishing phase: the equilibrium rows are invariant under a uniform shift when one phase is a trace). The way there (`v03`): the crude generator stands 210 kPa below the junctions, so it can only receive; the cold rate seed treated it as two-way (it failed at 0.056 after 20 iterations), the steps started from that failure (a crude junction at the crude's 293.15 K floor after expanding) and every pass moved phases until the matrix went singular.

Decision D12 (owner) makes a generator one-way from the first pass, and `closeColdReceivingGenerators` implements it for the cold rate seed, but A34 left islands with an actuator out (here the two valves), as it left filter islands out (class E1).

### 2. Fix (`a3d3a789`)

The cold closure runs in actuated islands too: an actuated connection is left out of the one-way estimate (treated as closed there) and a generator run never passes through one, so no device is closed by it and the pass loop still decides the devices first. Passive islands are untouched (the code path is identical when every connection is passive). Gate test `ActuatedIslandGeneratorClosureTest` (the seed-4 world): holds on this exact E2 status without the change (`v08`), passes with it (`v07`). Decision A69.

| Distribution | Clean at `bbb79790` (`r13`) | Clean at `a3d3a789` (`v04`) | Cleared | Worse |
|---|---|---|---|---|
| random | 757 | 770 | 13 | 0 |
| world defaults | 760 | 775 | 15 | 0 |

Cleared seeds span classes E2, E1, E6, E5 and F (random 4 121 188 320 333 628 685 724 829 859 892 913 926; defaults 6 92 212 286 303 364 489 561 628 683 794 880 915 931 988): the two-way generator in a cold start is a shared cause. Gates (`v05`): fluid suites **490/490**, MIXED_GAS_COST 286, 33 junction lines **identical**, exact regression **0.000e+00**, adjacent 45/45, every printed line of the suites identical to `r17` (5814 lines).

The rest of E2 (24 random / 9 defaults on `a3d3a789`) was not traced seed by seed; the structure found here (a vanishing phase in a two-phase crude junction, the split unknowns free) is the phase counterpart of the trace flush (D16 acts on a component's share, not on a phase's), the same gap that leaves E4's seeds 454 and 719 (owner's trace domain, see "What needs an owner decision").

### 3. Status after the class fixes

| Commit | random | world defaults |
|---|---|---|
| `bbb79790` E1a | 757 | 760 |
| `a3d3a789` cold closure in actuated islands | 770 | 775 |

## The remaining classes: smallest worlds and attribution (2026-09-28)

- Author: Claude (Opus 5.5), same worktree and branch, on `a3d3a789`. **No code change.** Each class's smallest seed of `v04` was minimized with `HoldMinimizerTest` (8 threads, class kept; logs `w01`, `w04`) and the smaller ones traced with `instr-pass-loop-trace.patch` (`w02`, `w03`, `w05`). Medium depth: the mechanism is named where the trace shows it; no class below has a repair that is free of an owner decision.

| Class (random / defaults on `a3d3a789`) | Smallest world (blocks) | What the trace shows | Family |
|---|---|---|---|
| E5 line search stalls (11 / 22) | random 143 (14): a 193 kPa two-phase crude generator piped straight to a void; two junctions minted from it (two-phase) behind a 120 kPa valve and joined only to voids | every step size stalls at 0.407 on the phase-equilibrium rows of both junctions, whose split unknowns sit at ln(v/l) = -28 to -33: a two-phase junction with no source draining to 1 atm, its vapour phase a trace (`w02`) | vanishing phase in a two-phase junction (as E2's rest) |
| | defaults 30 (11): 1 atm water generators, voids, a zero-target pump, a filter placed 129 ticks later | roundoff flows of 1e-14 kg/s leave a void (below the pass loop's 1e-10 illegal-direction threshold) and bring nitrogen into water junctions; the E3 appearance (above 1e-24 of the node) reseeds them with a 1e-15 m3 vapour and pass 1 stalls at 0.096 (`w03`) | trace vapour in a junction (E3's next layer, E4 family) fed by E6's zero-flow noise |
| D singular rate pass (7 / 5) | random 175 (13): methane and crude generators near 120 kPa, a pump, a 400 kPa valve, two tanks, a filter | interval-start rate pass singular at pass 0 (`w04`) | the rate pass (E1 option R1 removes the class) |
| E7 active-set cycle (1 / 4) | random 325 (8): a 103 kPa water generator, a junction minted from a nitrogen void, two voids | the water arriving makes the phase appearance reseed the junction and the pass sequence returns to a solved structure (cycle) at every step size (`w05`); the junction starts as the void's nitrogen because a void has the smallest id | the mint from a void (below) |
| E8 attempt limit (1 / 5) | random 613 (8): a 123 kPa water generator into a nitrogen tank, a void | 519 of 522 attempts refused on "Conservative reconstruction fails equation gate: 1.03e-5" | the equation gate on a vessel (D13's vent-gate family); not traced further |
| E10 no finite Jacobian (4 / 4) | random 200 (16): a 288 K nitrogen generator at 430 kPa, a 317 K crude generator, a compressor, a filter | the first step's Jacobian cannot be differenced inside the property domain from the minted start | the mint (a crude junction minted cold from nitrogen) |
| G zero row (1 / 0) | random 236 (9): crude and water generators, a tank, a filter | an empty row of a two-phase crude junction in the rate pass | the rate pass (R1) |
| E4 equation gate (6 / 13) | random 9 (11): a tank, a 1 MPa pump, a water generator placed later | the gate refuses at 3.9e-6 at every step size | trace vapour in water nodes (the orchestrator's note: a vanishing phase of a plentiful component) |
| E9 other (0 / 3) | defaults 273, 298, 570 | "Invalid inventory volume" / "Negative transport reconstruction" | not traced |

**The mint from a void, measured** (`scratch-mint-supply.patch`, `-Pmint=supply`, `w06`): a junction minted from the first boundary that can supply (a generator or tank; a void only when there is no other): random 770 -> 783 (23 cleared, 10 worse), defaults 775 -> 775 (19 cleared, 19 worse), and class E3 comes back (2 / 1). Not a clean improvement; an owner decision like every mint change.

### Shared causes found

1. **Generators two-way at a cold start** in the islands A34 left out (filters, actuators): fixed by `bbb79790` and `a3d3a789` under D12; together 28 random and 23 default seeds, across E1, E2, E5, E6 and F.
2. **The row-scale jump of a connection at zero driving pressure** (E6): the default-play budget holds (F) are mostly this (E6-O1 clears 13 of 25), and its roundoff flows at zero-flow roots feed the trace-vapour cases of E5.
3. **Trace and vanishing phases in junctions** (E2's rest, E4, E5's two-phase crude junctions, the next layer of E3): the Newton states a junction's composition to an absolute 1e-9, so a phase below that share is unresolved while the reconstruction books it exactly; D16 flushes a component below 1e-24 of its node but not a vanishing phase of a plentiful component. The owner's trace domain.
4. **The junction mint** is not the one shared cause (mixed, mean-pressure and supply-first mints each break as many worlds as they clear), but it is the start E1c, E7 and E10 fail from.

## Hold search: status after the class fixes (2026-09-28)

Clean seeds of 1000 (8 workers, 120 slices), each campaign against the previous commit's:

| Commit | What | random | world defaults | worse (random / defaults) |
|---|---|---|---|---|
| `65583a38` | fuzz baseline (F8 done) | 703 | 702 | |
| `9079a15a` | X: fence through unowned devices and dormant filters | 735 | 731 | 0 / 0 |
| `f80def43` | F7 mass reference + trace flush (orchestrator) | 746 | 737 | 0 / 0 |
| `7e5391a7` | E3: phase appearance at a node with no hydrocarbon phase | 753 | 752 | 0 / 2 (budget seeds that replay clean alone) |
| `bbb79790` | E1a: cold generator closure in filter islands | 757 | 760 | 1 / 0 (152, an unrelated filterless step hold) |
| `a3d3a789` | cold generator closure in actuated islands | **770** | **775** | 0 / 0 |

Classes on `a3d3a789` (`v04`, random / defaults): C 85 / 16 (legitimate domain waits), E1 51 / 64, E6 28 / 54, E2 24 / 9, F 11 / 25, E5 11 / 22, D 7 / 5, E4 6 / 13, E10 4 / 4, E8 1 / 5, E7 1 / 4, G 1 / 0, E9 0 / 3, A 0 / 1, X 0 / 0, E3 0 / 0, B 0 / 0.

Measured options on top (not committed): R1 (E1-O1) 782 / 779 on `bbb79790`; E6-O1 clears 25 of 86 E6 seeds and 13 of 25 default budget holds but moves 25 junction lines and fails two extreme-grid tests.

Gates on `a3d3a789` (`v05`, `w07`): fluid suites **490/490** in 113 classes (488 + `TopologyEventFenceTest` counted before, + `JunctionPhaseAppearanceTest`, `FilterIslandGeneratorClosureTest`, `ActuatedIslandGeneratorClosureTest`), MIXED_GAS_COST **286**, 33 junction lines **identical** to `holds/12-junction-lines.txt`, exact regression **0.000e+00**, adjacent **45/45**, fluid GameTests **33/33** (fresh run id `hold-classes-final-20260928`). The only printed line of the suites that moved over the batch of class fixes is `PhasePortPriorityTest`'s overflow-0.1 trajectory (E3, section above).

### What needs an owner decision, in priority order

1. **R1** (E1-O1): a failed interval-start rate pass declares no closure. +25 / +19 clean, none worse, removes D, E1 and G; costs campaign time (random 137 -> 519 s) because the same islands then fail in their steps.
2. **The connection row scale at a sign change of the driving pressure** (E6-O1 or a blended form): the largest remaining lever for default play (E6 and most default F); moves the mixed-gas reference lines, so it needs a re-baseline decision and a fix of the extreme-grid hold it creates.
3. **Vanishing phases and junction traces**: a phase flush (a phase below a share of its node, the phase counterpart of D16) and/or relative trace resolution in a junction's mixing rows; covers E2's rest, E4, E5's crude junctions and E3's next layer.
4. **The junction mint** (E1-O3 / mint rules): E1c, E7, E10 fail from it; none of the measured mints is a net gain.
5. The latent event-path case noted under class X (a queued removal), not exercised by the fuzz.

## Boundary split, dead ends and rest (2026-09-28)

- Author: Claude (Opus 5.5, as a subagent of the orchestrator), same worktree and branch, from `da8fb533`. Code commits `28f8c16f` (D17), `73624241` (D18), `89af686c` (D19). Owner decisions D17-D19 and the owner decisions relayed the same day are in `DECISION_LOG.md` (D17-D27, A70-A76). Material: `tools/phase-ports-probes/hold-classes/` (`src/BoundarySplitProbeTest.java`, `src/boundary-split-worlds.txt`, logs `b01`-`b21`).

### 1. How islands were formed before this batch (read from `da8fb533`)

1. **Connectivity** (`PhysicalFluidTopology.compile`): every registered block is a node. Two adjacent blocks connect when both connect towards each other (pipes, tanks, generators and voids on all six faces; pumps, compressors, valves and filters only along their facing axis) and they are not both boundaries (tank, generator, void). A boundary next to anything else gets a direct link (half the other block's geometry); two non-boundaries get a virtual middle node between them. So equipment touching equipment does connect (a pump against a tank, a valve against a generator), except boundary against boundary.
2. **Dead ends** (`TopologyCompiler.compile`): only PIPE nodes with at most one connection were removed, leaf by leaf ("NO FLOW: dead-end pipe"). A pump, compressor, valve or filter at the end of a branch stayed a node of the island, a junction with a holdup, solved.
3. **Runs**: maximal chains through pipe blocks with two connections became one compiled connection; every other node (boundaries, actuators, filters, pipe blocks with three or more connections) is a node.
4. **Islands**: connected components of the runs, walked through every node, boundaries included. A generator or a void therefore joined everything it touched into one island. A boundary with no run was an island of its own; a component with no boundary had no island (each of its filters a dormant island holding its cake).
5. **Registry and events** (`PhysicalRegistry`): one owner per device. `submit` walks from an edit through devices no island owns and stops at an owned device (its island is fenced whole); `apply` compiles the connected component of the touched devices and of the members of the fenced islands (walked through boundaries too), reads boundary stock and filter cakes from the fenced islands only (anything else throws "reached ... outside the islands it fenced"), destroys every junction of a replaced island (booked in the topology ledger) and mints the new ones from the island's first boundary.

Where boundaries and dead ends entered the solve: a void or generator shared by two lines made one island (seed 33: a generator's line and a void-to-void line solved in one Newton); a dead-end pump, valve or filter made a junction the solver had to hold at a zero-flow root (seed 33's pump, the E1 filter worlds, the F8 pockets).

### 2. The network reduction stage (one stage, `NetworkReduction`)

Rules 1-3 are one stage in fixed order, run where islands are formed (`TopologyCompiler.compile`, called by `PhysicalFluidTopology.compile` on every topology event and load): (1) connectivity, (2) dead ends (`pruneDeadEnds`, D18), (3) runs, (4) split at fixed boundaries (`splitAtFixedBoundaries`, D17), (5) direction feasibility (`directions`) and (6) rest (`atRest`, D19). Steps 2 and 4 read no state and commute (a fixed boundary is a destination, so no pruned branch passes through one). Steps 5 and 6 read the island's states, so they are evaluated on the graph every interval job receives (`PassiveIntervalSolver.solve`), before any Newton: every path that solves an island (topology replacement, load, property resume, certificate revalidation) decides the same way, and the rest interval reaches the coordinator from a worker, never from the server tick. Deciding rest at formation would have needed a second route (a synthetic certificate made on the server thread) and the worker check anyway (A72).

### 3. Rule 1: split at fixed boundaries (`28f8c16f`, D17)

- `TopologyCompiler` never walks through a generator or a void; a fixed boundary belongs to every island that reaches it (`PhysicalFluidTopology` assigns each run to the group of its first node that is not fixed). Components no fixed boundary cuts come out identical and in the same order.
- `PhysicalRegistry`: all islands of each fixed boundary (`fixedOwners`), the lowest as its `owner`; the boundary's state kept (`fixedStates`) so a compile that meets it at its edge can read it without fencing its other islands; `submit`, `apply` and the re-fencing walk (`reached`) stop at a fixed boundary the event does not edit and fence none of its islands; an edit of the boundary itself fences all of them; a lone-boundary island is not minted for a boundary that keeps another island; `load` binds saved islands by their connections, tanks and (for an island without connections) its boundary. The view of a shared boundary sums the flows of all its islands (status from its owner; not checked in the dev client).
- Gate test `FixedBoundarySplitTest` (seed-33 world: split, the shared void in both islands, an edit beside the void changes neither island, an edit of the void replaces both, keep-up): fails without the change (`b03`), passes with it. The reduced seed-33 and seed-6 worlds held on `da8fb533` (`b04`: E6 at 1.6e-8, E1 at 6.2e-8) and run clean (`b01`).

### 4. Rule 2: dead ends (`73624241`, D18)

- `NetworkReduction.pruneDeadEnds` removes leaf by leaf every node that is not a destination (tank, generator, void) and keeps at most one connection: pipes as before, now also pumps, compressors, valves and filters. A cycle is never removed. The split of rule 1 moved into the same class unchanged.
- A pruned filter keeps its cake in a dormant island of its own (diagnostic "NO FLOW: dead-end filter"); `PhysicalRegistry.apply` reads that cake when a compile meets the filter at its edge, and refuses a replacement that would take a device from an island the batch did not fence. Other pruned blocks belong to no island ("NO FLOW: dead-end pipe", a one-port pump or compressor keeps "ERROR: pump needs two ports ...").
- Holdup of a branch that loses its destination (owner D25/D26): nothing new is needed. Every junction of a replaced island is destroyed at the recompile and booked in the topology ledger (`WorldTopologyLedger.applyBatch`, moles and energy field + m g z, the construction/destruction path a removed tank also uses); a pruned branch simply gets no new junction, so its few grams leave with that booking. The trace flush (D16) books a removal differently (a boundary transfer at the node in the island's interval result, energy = potential energy of the removed mass only, A65). Recommendation: keep the ledger's destruction booking for topology removals (it is where the removal happens, at the event, and it is the path every removed junction and tank already uses); the flush's interval booking belongs to removals inside a solved step. Not changed without the owner.
- Gate test `DeadEndPruningTest` (seed-6 and seed-12 worlds, E1: the dead-end filter dormant with its cake, its pipe unowned, keep-up; seed 12 then: an edit of the tank's line leaves the dormant filter's island, a void past the filter makes it rejoin the tank's island with the same cake): fails without the change (`b10`).
- **Restated assertion, for the owner to confirm**: `InlineFilterLineHoldTest.pumpLinesIntoValvesAndFiltersNeverHold` asserted that a pump discharging into a dead end (GPMPF, GPMPFP, GPMPV, GPMPVP) stands closed at its shutoff; under D18 the whole dead-end branch, pump included, is in no island, and the test now asserts that (and for GPMP). No other gate assertion changed.
- `FilterIslandGeneratorClosureTest` (E1a) still passes but its filter is now pruned: it no longer exercises a filter island's rate pass (A75).

### 5. Rule 3: rest (`89af686c`, D19)

- `NetworkReduction.directions`: per connection, the directions its one-way rules allow (`boundaryAllowed`: a generator only supplies, a void only receives, a blocked direction carries nothing) in which its static drive (end pressures less the column in the donor end's bulk density, the `closeDeadHeads` test) is strictly positive. No band.
- `NetworkReduction.atRest`: only islands of junctions and fixed boundaries joined by passive connections (a filter is one) and holding no solids are asked; a tank, pump, compressor or valve keeps the island on the solver (A72). At rest when every fixed boundary is a void and the junctions form no loop (a junction's mass is pinned, so nothing can supply the voids, and in a tree every connection then carries zero), or when no connection has a driven direction (the present state is then a root with every flow zero). `PassiveIntervalSolver.solve` returns `NetworkReduction.restInterval` (graph unchanged, every flow, head and transfer zero, FULL) before any Newton; the island certifies through the existing certificate path ("STEADY: no flow").
- Gate test `RestIslandTest` (seed-30 world, E5: two 1 atm water generators level with their junctions, one void level with them and one a block above; at rest, never re-stated, every flow exactly 0.0, "STEADY: no flow"; the same world with a 150 kPa generator is solved; a run between voids at 101 and 300 kPa is at rest). Without the hook the seed-30 world replays 4.4e-16 kg/s of roundoff ("STEADY: replaying 4.441e-16 kg/s", `b16`).

### 6. Campaigns, gates and printed lines

Clean seeds of 1000 (8 workers, 120 slices; worse seeds replayed alone on one thread, `b07`, `b14`, `b19`):

| Commit | random clean (cleared / worse vs previous) | defaults clean (cleared / worse vs previous) | worse, replayed alone |
|---|---|---|---|
| `a3d3a789` (`v04`) | 770 | 775 | |
| `28f8c16f` split (`b06`) | 792 (25 / 3) | 782 (15 / 8) | random 316 C, 429 E6, 438 E4; defaults 6 E3, 259 E4, 328 C, 377 E6, 429 E5, 504 E4, 698 E5, 737 E4: all hold alone too; the split changes which boundary mints each island's junctions |
| `73624241` dead ends (`b12`) | 843 (58 / 7) | 820 (46 / 8) | random 48 E6, 567 E6, 615 E6, 736 E5, 743 E2, 829 E6, 859 C; defaults 212 E2, 357 E6, 489 E9, 497 E6, 537 E6, 628 E9, 736 E10, 892 E6: all hold alone |
| `89af686c` rest (`b18`) | **845** (2 / 0) | **822** (4 / 2) | defaults 285, 915: soft-budget holds, clean alone (`b19`) |

Against `v04`: random 82 cleared / 7 worse, defaults 58 cleared / 11 worse. Classes on `89af686c` (random / defaults): C 55 / 8, E6 31 / 65, E4 11 / 24, E5 14 / 19, E2 18 / 8, F 12 / 22, E1 4 / 7, E8 5 / 6, E9 0 / 7, E10 3 / 3, E3 1 / 3, E7 1 / 3, D 0 / 3. E1 falls from 51 / 64 to 4 / 7 (the dead-end filters no longer run the interval-start rate pass).

Gates after each rule (`b05`, `b11` + `b13`, `b17`): fluid suites 493/493, 494/495 (the restated test, passing on its own run `b13`), **498/498** in 117 classes; 33 junction lines **identical**; MIXED_GAS_COST **286** throughout; exact regression chain-100 **0.000e+00**; adjacent **45/45**; fluid GameTests **33/33** each time (run ids `boundary-split-rule1-20260928`, `-rule2-`, `-rule3-`). No bitwise reference moved.

Printed lines of the suites that moved against `v05` (5883 lines, timings masked, map order ignored): `InlineFilterLineHoldTest`'s `F8_END` island and node ids (renumbered: fewer islands are replaced by the events; every state digit identical), and five `F8_LAYOUT` lines of the dead-end layouts (GPMP, GPMPF, GPMPFP, GPMPV, GPMPVP: the pump's island is gone, D18). Nothing else.

### 7. What remains (the held island as solved, smallest seed per class, `b20`)

Physics first, then the solver. Pressures absolute.

| Class | Seed | What is solved (after split and pruning) | What should happen | What the solver does |
|---|---|---|---|---|
| C | random 12 | 232 kPa crude generator (295 K) three blocks above a 1 atm nitrogen tank's top port, one 0.02-0.05 m run | crude drains into the tank until the tank's gas reaches ~232 kPa plus the column | the crude's lightest cut is evaluated at 293.1499999999 K, 1e-13 K under the package floor (a flash at the tank's cooled mixing point): a thermo-domain wait. Model data, not a solver defect |
| C | defaults 161 | (default play) | | thermo-domain envelope at a non-finite pressure: to be traced |
| E6 | random 9 | 178 kPa water generator (358 K) into a 5-junction mesh (0.01-0.05 m) at y=-1 feeding a 1 atm nitrogen tank above by its bottom and side ports | water fills the tank, compressing its nitrogen to ~169 kPa (the generator less ~1 m of water), then ~0.09 kg/s of trickle until balanced (the reduced world does exactly this, `b21`) | at tick 2024 every step size ends on the Newton iteration limit at 5.7e-9: the E6 zero-flow-root row-scale jump on a connection whose drive crosses zero |
| E6 | defaults 19 | (default play) | | Newton iteration limit at 8.2e-9 at every step size (E6) |
| E2 | random 236 | 332 kPa crude generator (341 K, y=1) and 346 kPa water generator (350 K, y=3) into one junction at y=1 over a 1 atm nitrogen tank at y=0 (0.1 m) | both liquids pour into the tank; the tank fills and pressurises; the crude generator closes once the junction passes 332 kPa, the water keeps filling to ~375 kPa at the tank | the junction is minted from the tank (smallest id): 1 atm nitrogen. The cold rate seed moves it to 230.7 kPa, 350 K with vapour, crude liquid and water all present; row 19 of the junction (a phase-split row of a phase with no amount) is empty: "Singular matrix: empty or zero row 19"; every step size then singular (`b21`). Vanishing phase in a three-phase junction |
| E2 | random 26, defaults 10 | 311 kPa crude generator into a junction between two 1 atm nitrogen tanks | crude fills both tanks | singular at pass 0 at every step size (same family) |
| E4 | random 175 | 116 kPa two-phase crude generator (354 K) into a junction and a tank by two parallel runs | the tank at 116 kPa, no flow | at rest by ~1e-10 kg/s flows; the reconstruction's equation gate refuses 2.1e-8 at every step size (trace vapour family) |
| E4 | defaults 6 | (default play) | | equation gate 3.46 at every step size |
| E5 | random 19 | 280 kPa crude generator (328 K) through a 0.01 m pipe into a junction joined to a 1 atm tank and a 1 atm void | crude flows (~0.73 kg/s) split between the tank and the void; the tank fills | line search stalls at 0.070 at every step size |
| E5 | defaults 12 | (default play) | | line search stalls at 6.09 |
| F | random 39 | 96 kPa methane generator above a junction fed by a 250 kPa crude generator and a crude-filled tank | the methane generator stays closed; crude from the generator and the tank balance | soft budget, approximate fallback refused (device regime changed): the solve converges but too slowly |
| E8 | random 131 | 166 kPa water generator into a junction with a 1 atm void above and a tank beside | water fills the tank to ~145 kPa and spills 21 kg/s into the void | 528 steps refused on the equation gate at 1.2e-8 over one slice (D13 vent-gate family) |
| E1 | random 159, defaults 200 | islands with a filter still on a valid run | | interval-start rate pass iteration limit |
| E10 | random 200, defaults 226 | nitrogen generator 430 kPa, compressor, void, crude and water generators | | no finite Jacobian at a first step (a mint far from the property domain) |
| E3 | random 889, defaults 570 | 185 kPa water generator into a junction with a void and a tank | water fills the tank, spills to the void | water-vapour approximation outside its range at every step size |
| E7 | random 325, defaults 771 | 103 kPa water generator into a junction between two 1 atm voids | water spills into the lower void | active-set cycle at every step size (a junction minted from a void's nitrogen, reseeded by the water's arrival) |
| D, E9 | defaults 499, 47 | | | singular rate pass; "Invalid inventory volume" / "Negative transport reconstruction" |

The cards for every remaining class (block lists after reduction, expectation in numbers, the node and equation) are to be written in `REMAINING_HOLDS.md` after the owner's follow-up rules (only pipes connect, one-way boundaries, feasibility and start state) are in, as the owner asked (D27).

### 8. Existing special cases and the stage

| Mechanism | Where | Stage step | Verdict |
|---|---|---|---|
| Dead-end pipe pruning | `TopologyCompiler` | 2 | **absorbed** (`pruneDeadEnds`, now for every non-destination block) |
| Dormant filter islands (no boundary, now also dead ends) | `PhysicalFluidTopology` | 2 | stays where it is: a cake is owned stock and needs an island; the stage decides that the filter is pruned, the adapter keeps its cake |
| `invalidPumpCycles` (zero-storage bypass) | `TopologyCompiler` | 1-3 | could be absorbed later as a structural step; no behaviour to change now |
| F8 sealed pockets (`relieveSealedPockets`, A59-A61), A62 no cap into a pocket | `PassiveStepSolver` | 5 | the structural part (a mover into a filter or valve with nothing past it) is now pruned by step 2; the rest depends on the pass's modes (a valve closed at the start, a refused mover), so it must stay in the solver |
| `closeDeadHeads` (a passive run between two non-junction ends that no allowed direction drives) | `PassiveStepSolver` | 5 | same test as `directions`; stays per solve (a vessel end's pressure moves every step). Between two fixed ends the verdict is static and could be taken at formation later |
| `closeIllegalStarts`, `reopenable` | `PassiveStepSolver` | 5 | stay: they read the start point and converged flows |
| `closeColdReceivingGenerators` and its A34 exclusions (extended by `bbb79790`, `a3d3a789`) | `PassiveStepSolver` | 5-6 (P4 start state) | stays until the owner's P4 start-state step is built; it is the natural home of that step (a one-way estimate that sets junction start pressures) |
| Event fence: `standsForItsComponent`, `reached` | `PhysicalRegistry` | 2, 4 | extended, not replaced: fixed boundaries now stop the walks (4), dormant filters of dead ends are walked through (2) |

### 9. Surveys (orchestrator's items 3a-3c; no implementation)

**a. Equipment against equipment.** Every pair connects when both connect towards each other and they are not both boundaries (`PhysicalFluidTopology.compile`, lines 69-81; the same rule in the three `PhysicalRegistry` walks). Tank-generator, tank-void, tank-tank, generator-void never connect; an actuator or filter against a boundary on its axis gets a direct link; two actuators/filters on axis get a virtual middle node. Tests relying on a compiled equipment contact: `PhysicalFluidTopologyTest` (pump on a tank's bottom face, filter on a tank's top face), `FilterBlockLineIslandTest` (generator against a pump), `ElevatedBlockLineIslandTest` (generator under a pump), `SolidRuntimeTest` (generator against a pump), `FluidPumpedFillLineTest` (generator against a pump, tank against a compressor), `FluidThermoDomainHoldTest` (tank against a compressor), `PumpBottomPortDrainTest` and `TraceFlushFedPumpTest` (tank on a pump), `ActuatedIslandGeneratorClosureTest` (valve against the crude generator, the E2 world), `TopologyEventFenceTest` (filter against filter, tank against filter: the class X world); GameTests `aPlacedPumpedThreeTankChainFills`, `aPumpOnAGasTankIsRefusedWithItsReasonOnItsStatusLine`, `filterCaptureRecoveryPersistenceAndBreakDropConserveOwnedSolids`. Under "only pipes connect" each of these loses the contact (a pump with one port, a filter island split, an assertion on one island failing) and must be rebuilt with a pipe between. Boundary-boundary non-contact is relied on by `PhysicalFluidTopologyTest` (stacked tanks) and `ExtremeTopologyIslandTest` (node and connection counts). The fuzz places a non-pipe item directly against its anchor with probability 1/3 (`length=r.nextInt(3)`), roughly 40 % of worlds (estimate, not measured).

**b. Can a generator receive?** By rule never (`boundaryAllowed` forbids flow into a generator or out of a void on every connection, movers included), but: pass 0 treats an unclosed generator as two-way until the pass loop closes it; a converged pass may keep a reverse flow of at most 1e-10 kg/s (the illegal-direction threshold, also on the approximate path), which `ConservativeTransport` books as a real negative boundary transfer and a certificate replays; `closeColdReceivingGenerators` does not run in solid islands, on runs through an actuator (A69), into a vessel, or when its estimate fails. Gate fixtures with a generator below what it faces at steady state: `DeadHeadedLineIslandTest` (asserts CLOSED and exactly zero), `ExtremeTopologyIslandTest` (asserts at most 1e-10 x slice received, CLOSED at rest), `ElevatedBlockLineIslandTest`, `IslandCertificateTest.deadHeadedLinesCertifyRestAndThenCostNothing` (and the checkpoint/presentation tests on the same fixture), `MixedGasJunctionStaticTest`, `FilterIslandGeneratorClosureTest`, `ActuatedIslandGeneratorClosureTest`, `McpGameplayRegressionTest`.

**c. Seeds 236 and 9 (random) after the stage.** 236: the minimized world (`w04`) and the full seed both hold after the stage, now as E2 (table above; physics and trace `b21-trace-236`): the dead-end filter branch is pruned, and what remains is the vanishing-phase singularity of a junction minted as nitrogen that receives crude and water at once (empty phase-split row 19 of the junction in the cold rate seed, then every step singular). 9: the minimized world runs clean after the stage (the dead-end pump is pruned; the tank settles at 168.7 kPa with 0.09 kg/s trickling, `b21-trace-9`); the full seed still holds as E6 (178 kPa water into a nitrogen tank through a junction mesh; Newton iteration limit at 5.7e-9 at a zero-flow root).

The surveys a and b above were made by a read-only subagent before the orchestrator handed them to another agent; the complete survey is `CONNECTION_RULES_SURVEY.md` in this folder (written by that agent).

## Follow-up rules: only pipes connect, one-way boundaries (2026-09-28, group A)

- Author: Claude (Opus 5.5, subagent), same worktree and branch, after `85346af5`. Code `983cc915` (D20), tests `e4f0cd7a` (D20 gate), `db3ac1b9` (latent class X case), `abf8df51` (rebuilt gate worlds), `4beea57a` (D21 named). Owner decisions D20-D31 in `DECISION_LOG.md`. Material: `tools/phase-ports-probes/hold-classes/` (`quick.sh`, `src/scratch-p2-strict-one-way.patch`, logs `c01`-`c24`), `hold-fuzz/src/HoldFuzzTest.java` (`holdFuzz.pipeBetween`).

### 1. Which check ran at which commit (so no partial check reads as a gate)

| Commit | What ran | Result |
|---|---|---|
| `28f8c16f`, `73624241`, `89af686c` | full `gates.sh` with GameTests, full campaign (section above) | see above |
| `983cc915` (D20) | quick check (`quick.sh`, `c04`): G1 + junction lines, G2, fuzz seeds 1-200 against `b18`; then full `gates.sh` (`c08`, run id `boundary-split-p1-20260928`) and full campaign (`c09`) | G1 498/498 (`c04`), 503/503 (`c08`, with the gate and guard tests); 33 junction lines identical; MIXED_GAS_COST 286; exact 0.000e+00; adjacent 45/45; GameTests 33/33; fuzz 856 random / 829 defaults (11 / 8 cleared, 0 / 1 worse: defaults 821, a wall-deadline hold that is clean alone, `c10`) |
| `e4f0cd7a`, `db3ac1b9`, `abf8df51` | the new and rebuilt tests alone (`c05`, `c07`, `c12`, `c17`, `c22`), each with its fix reverted in the working tree (`c06`, `c13`, `c14`, `c18`, `c19`, `c20`, `c21`) | table 3 |
| `4beea57a` (D21, comment) | full `gates.sh` (`c23`, run id `boundary-split-groupA-20260928`) and the first campaign with `holdFuzz.pipeBetween` (`c24`) | section 6 |
| scratch, not committed | P2 strict one-way probe (`c11`) | section 4 |

Campaigns and replays ran through the Gradle route (`campaign.sh`, `HoldFuzzTest`), not the Gradle-free runner.

### 2. D20: only pipes connect (`983cc915`)

`PhysicalFluidTopology.Device.joins`: two neighbours are joined when both connect along the axis and at least one is a pipe block; the compile and the three `PhysicalRegistry` walks use it (step 1 of the reduction stage). Nothing tells the player that two pieces of equipment touch (owner, D29). Gate test `OnlyPipesConnectTest` (a generator against a pump, a valve, compressor or filter against a tank, two filters side by side: nothing joined; the same with a pipe block between: one line; the registry walks alike): fails without the rule (`c06`).

Fixtures that relied on equipment touching equipment were rebuilt with a pipe block between (assertion and tolerance unchanged unless stated), and what moved:

| Fixture | Rebuild | What moved |
|---|---|---|
| `PhysicalFluidTopologyTest` pump under / filter over a tank | a pipe block between tank and device | nothing asserted moved (ports, directions) |
| `FilterBlockLineIslandTest` pumped line and control | generator - pipe - pump - ... | printed intervals only |
| `ElevatedBlockLineIslandTest` rising pumped line | generator - pipe - pump - pipe - tank (pump two blocks up) | headspace 852452.8285 -> 852448.0673 Pa, bottom 861063.0384 -> 861058.2704 Pa; the expectation's suction density now read two blocks up (`head(pressure,2)`, geometry, not tolerance); printed label renamed |
| `SolidRuntimeTest` pump-filter line | pipe between generator and pump (tank id 6 -> 7) | nothing asserted |
| `PumpBottomPortDrainTest` (scenarios 2 and 2b), `TraceFlushFedPumpTest`, `holds/src/FedPumpLifetimeProbeTest` | a pipe block between the tank's bottom face and the pump | 400 `FED_PUMP` printed lines (tank 129932.5 Pa as before to 5 digits) |
| `FluidPumpedFillLineTest` | `GPUPR...`; the gas transfer `RWCPR` with a 0.1 m suction block | with a 0.05 m block the compressor closes on a suction junction 4.2 Pa below its tank (discharge 101828.0 Pa against 1.01 x 100824.0 = 101832.2 Pa, test tolerance 3 Pa): the block's friction at the closing flow; the wide block keeps it inside (A77) |
| `FluidThermoDomainHoldTest` | `RPCPR` | the device name in the asserted status: "compressor at 1, 64, 0" -> "compressor at 2, 64, 0" |
| `TopologyEventFenceTest` (class X) | see table 3 | |
| `DeadEndPruningTest` (new) | pipe between filter and void | |
| GameTests `aPlacedPumpedThreeTankChainFills`, `aPumpOnAGasTankIsRefusedWithItsReasonOnItsStatusLine`, `filterCaptureRecoveryPersistenceAndBreakDropConserveOwnedSolids` | a pipe block between the generator or tank and the pump | pass (`c08`) |

The 33 junction lines, MIXED_GAS_COST and chain-100 are built without equipment contacts and did not move.

### 3. Do the rebuilt gate tests still guard their fixes? (each fix reverted in the working tree only, never committed)

| Test | Fix it guards | With the fix | Fix reverted |
|---|---|---|---|
| `ActuatedIslandGeneratorClosureTest` (rebuilt: pipe between crude generator and valve; pipe and void below the dead-end valve) | `a3d3a789` (cold closure in actuated islands) | pass | holds at tick 106 (Newton iteration limit, singular Jacobians in the reasons) (`c13`) |
| `FilterIslandGeneratorClosureTest` (rebuilt: pipe and void past the filter; a 130 kPa water generator feeding the junction, since without a driving source the island is at rest and never solved, D19) | `bbb79790` (filter inclusion) | pass | holds on the interval-start rate pass (Newton iteration limit at 0.070, pass 0) (`c14`) |
| `TopologyEventFenceTest` test 1 (rebuilt: the lone void became a tank a generator keeps filling, because a void no longer holds an event back, D17) | `9079a15a` part 1 (re-fence through unowned devices) | pass | "Topology event reached boundary 3 outside the islands it fenced" (`c19`) |
| `TopologyEventFenceTest` test 2 (rebuilt: pipes between the filters and past the far filter to a void) | `9079a15a` part 2 (walk through dormant filters) | pass | "Topology event reached device 3 outside the islands it fenced" (`c18`) |
| `TraceFlushFedPumpTest` | `f80def43` (trace flush; the commit does not revert cleanly, so the flush call was disabled) | pass | "Matrix values must be finite" (`c20`) |
| `PumpBottomPortDrainTest` (F1, scenarios 2 and 2b) | `60cc201d` (F1 fix) | pass | passes with only `60cc201d` reverted (the trace flush also removes F1's trace); with `60cc201d` reverted and the flush disabled, the 0.001 m3/s drain holds at slice 18 exactly as F1 did (tank 117.4 kPa, 112.7 kg left, `c21`); the 0.01 m3/s drain no longer reproduces F1 with the pipe block between |
| `FluidPumpedFillLineTest` | fixes of the 0.4.0 batch (2026-09-24, before `e945e7fd`, which added the tests only) | pass | not reverted: the fixes predate this batch and do not revert on this tree; not proven |

Before these rebuilds, the rules had emptied three of them: `ActuatedIslandGeneratorClosureTest` passed with no actuator in its island, `FilterIslandGeneratorClosureTest` with its filter pruned, and both `TopologyEventFenceTest` tests passed with either part of the class X fix reverted (`c15`, `c16`).

### 4. D21: one-way boundaries named, not extended (`4beea57a`)

The rule is the one `PassiveStepSolver.boundaryAllowed` enforces in every pass; `NetworkReduction.directions` names it (owner D30). Extension is reserved for a hold class that shows a generator receiving or a void supplying; none of the remaining classes was attributed to one (the E1/E2 generator cases are the cold seed's two-way start, already closed by D12's closure). Measured as a scratch probe only (`src/scratch-p2-strict-one-way.patch`, `c11`): closing any reverse flow at a fixed boundary instead of above 1e-10 kg/s moves 38 junction-line diff lines, MIXED_GAS_COST 286 -> 295 and fails `LevelHeadTest`, and changes no fuzz seed of 1-200; not implemented.

### 5. The latent class X case (`db3ac1b9`)

The owner asked for it to be fixed as a plain defect with a gate test (D27). `QueuedRemovalWalkTest` builds it (a removal queued behind a mid-slice island, a later event placed in the gap and one beyond it); both apply every event and keep up on the current code: no defect reproduced (the later event reaches the islands through the removed block's neighbours, which the removal's own walk touched). Kept as a regression guard; no code change.

### 6. Fuzz generator: a pipe between a device and equipment

`HoldFuzzTest.world` puts a device against its anchor one time in three; under D20 that contact joins nothing. `holdFuzz.pipeBetween` (default on, owner D28) makes a device drawn against a non-pipe anchor hang off one pipe block instead; the random draws are unchanged, so a seed keeps its layout wherever its devices already hang off pipes. **Counts from `c24` on are not comparable with earlier campaigns** (different worlds for about 40 % of seeds); `-PholdFuzzPipeBetween=false` reproduces the old worlds (needed for the reduced lists of `boundary-split-worlds.txt` and the minimizer KEEP lines of earlier logs).

Group A closing checks on `4beea57a`: full gates (`c23`, run id `boundary-split-groupA-20260928`): fluid suites **503/503** in 119 classes, 33 junction lines **identical**, MIXED_GAS_COST **286**, exact regression **0.000e+00**, adjacent **45/45**, GameTests **33/33**. The new fuzz baseline with the switch (`c24`): **816 random / 805 defaults** clean of 1000. Classes (random / defaults): E6 24 / 70, C 74 / 9, F 15 / 29, E4 15 / 21, E5 13 / 17, E1 9 / 13, E2 16 / 11, E7 6 / 3, E8 3 / 5, E9 2 / 5, D 1 / 5, E3 2 / 4, E10 4 / 2, A 0 / 1. The drop against `c09` (856 / 829 on the old worlds) is the switch: about 40 % of the worlds now join devices that stood unconnected against equipment, so more of them flow; it is not a regression of the solver (the same commit's gates are identical).

### 7. What is not done in this group

- P3 (D22, feasibility per branch inside a flowing island) and P4 (D23, the start state) are the next group; D19 covers the island-level part of P3, and `closeColdReceivingGenerators` is the natural home of P4 (section 8 of the previous section).
- `REMAINING_HOLDS.md` (D27) is written after P3 and P4, as the owner asked ("after the fixes"); the smallest held island per class of the previous section and `b20` are its starting point, to be redone on the `c24` worlds.

## Follow-up rules, group B: feasibility, start state, seed 236 (2026-09-28)

- Author: Claude (Opus 5.5, subagent), same worktree and branch, from `984005ae`. Code `857963d9` (D22 at island level). Measured and not implemented: P3 per branch, P4 (scratch `src/scratch-p4-through-flow-start.patch`), the phase-set step of seed 236. Material: logs `d01`-`d11`, `src/instr-rest-taken.patch` (prints every interval that takes the rest path; applied, run, reversed).
- Checks: `d01`/`d03` quick checks of `857963d9`; `d07`, `d08` the P4 probe; `d09` full `gates.sh` (run id `boundary-split-groupB-20260928`) and `d10` full campaign on `857963d9`. All campaigns through the Gradle route.

### 1. P3: feasibility (D22)

**Physics.** Fluid moves only from a place with a higher pressure (elevation head included) to a place that can take it. A generator can only supply, a void can only take, a tank can do both. A part of the network is at rest when no source can reach any destination at a higher pressure. For a tank that means: its pressure is already at what its sources can push it to, and it cannot push anything back (a generator does not take, and nothing else stands lower). A 150 kPa water generator piped to a nitrogen tank at 150 kPa, level with it, is at rest: nothing flows, ever. The same tank at 101 kPa fills until it reaches about 150 kPa.

**What is implemented (`857963d9`).** The rest step (rule 3, D19) now asks islands with tanks too. The test is the solver's own static test, per connection and in both directions: the end pressures (a tank's bottom port with its level head) less the column in the density of what the donor end would send first, and the one-way rules. No band: any drive of any size keeps the island on the solver. Islands with a pump, compressor or valve are still solved (they have a target or a set point), as are islands with solids, an empty vessel, a trace the next step would flush (D16) or a module transfer in the interval. Gate test `RestIslandTest.aTankAtThePressureItsSourceReachesIsAtRestAndOneBelowItFills`, which fails without the change (`d05`). The rest interval now reports CLOSED on a connection with a one-way end and PASSIVE on one open both ways, as the solver's start closure does (revises A74).

**What moved.** Gates on `857963d9` (`d09`): fluid suites 504/504, 33 junction lines **identical**, MIXED_GAS_COST **286**, exact **0.000e+00**, adjacent 45/45, GameTests 33/33. No reference moved. 340 printed lines of the suites differ from `c23`, all because an island at an exact balance is no longer solved:
- `LevelHeadTest` "against" fixtures: `newtonSolves` 100 -> 0 and 36 -> 0 (a tank whose bottom port stands exactly at its void's pressure).
- `ElevatedBlockLineIslandTest`: the balanced flat line's tank 400000.0012361 -> 400000.0000028 Pa (the solves used to move it by roundoff); the printed "offset vs flat" figures move in the fourth decimal.
- `PhasePortPriorityTest` runs: accepted steps 121 -> 69 (oil-water drain) and 120 -> 102 (drain 5 s), the runs being at rest earlier; every ledger figure identical.
- `McpGameplayRegressionTest`: its 20 idle intervals are at rest, so the edited slice starts from a fresh solver history: 129960.05 -> 129967.48 Pa, 201.288 -> 201.335 kg, 40.029 -> 40.038 kg/s against the refined 130299.51 Pa, 203.412 kg, 40.453 kg/s (inside its bounds; comment updated).
- `FedTankOpenOutletsTest` F7 lines at 1e-15 relative; the stack test's `STACK_PASS` at 1e-10 relative.

**Gate tests whose worlds are now at rest from the start** (`src/instr-rest-taken.patch`, `d06`): `DeadHeadedLineIslandTest` (281 rest intervals), `FilterBlockLineIslandTest` dead-headed filter lines (420), `LevelHeadTest` (116), `ElevatedBlockLineIslandTest` (200), `FedTankOpenOutletsTest` (111), `IslandCertificateTest` (18), and module, scheduler and checkpoint tests between deliveries. All pass. But `DeadHeadedLineIslandTest` guarded `closeDeadHeads` (a generator facing a tank it cannot fill held its island before that closure existed): its world is now at rest and never reaches the solver, so it no longer guards that fix. **Stop point, options:**
- (a) Keep it as the rest test it has become, and add a new gate world where a dead-headed run sits beside a flowing branch in the same island (so the solver still needs `closeDeadHeads`). No assertion changes. Recommended.
- (b) Make the test call the step solver directly (bypassing the rest step), which keeps its old coverage on the same world but tests a path the game no longer takes.
- (c) Accept the loss of coverage.

**P3 per branch inside a driven island (not implemented, options).** Inside an island that flows somewhere, a side branch ending at a void takes flow only if the junction it hangs off rises above the void's head, and a side branch ending at a generator gives flow only if that junction falls below the generator's head. The junction's pressure is an output of the solve, so a branch can only be declared at rest before the solve from bounds: in a passive island every junction lies between the lowest and the highest head of its boundaries (the maximum principle the extreme-topology oracles use). That needs two choices physics does not force: (1) which density fills the column when branch and island are at different heights (the fluid already there, the source's, or the lightest and heaviest bound), and (2) what happens when the state changes so that a pruned branch would take flow (re-examine every interval, which changes the island's structure without a topology event and discards its certificate, or only at topology events). Options: (a) same-height branches only, the bound from the boundary heads, re-examined at topology events only; (b) the conservative bound (lightest column upwards, heaviest downwards), re-examined every interval; (c) leave it to the solver's existing closures (`closeDeadHeads`, `closeIllegalStarts`). Recommended (c): none of the remaining hold classes was traced to a side branch the solver failed to close.

**Campaign on `857963d9`** (`d10`, same worlds as `c24`): **816 random / 817 defaults** (0 / 12 cleared, 0 / 0 worse). Quick check `d03` on seeds 1-200: 0 / 3 cleared, none worse.

### 2. P4: start state (D23), measured and not implemented

**Physics.** When a line first opens, the junction pressure is set by what flows through it: between the sources pushing in and the outlets taking out. A tank on a side branch fills or vents toward that pressure; it does not set it.

**Probe** (`src/scratch-p4-through-flow-start.patch`, `d07`, `d08`): the first solve of an island (the cold rate seed, the same place as `closeColdReceivingGenerators`) starts every junction's pressure from the one-way through-flow estimate with every leaf tank's connection left out; holdup contents untouched (D25).
- **References move: stop.** 4 of the 33 junction lines move (all `MIXED_GAS_TRANSIENT` 4-port cases, key `0.05:4` and `0.02:4` at 0.1 s and 5 s). Old and new: 0.1 s `0.05:4` worstMoles 6.066045926792364E-15 -> 5.10099316571176E-15, worstEnergy 5.731974221560183E-15 -> 5.4395265571948675E-15; 0.1 s `0.02:4` 5.667681300235875E-16 -> 5.514587206174876E-16, 1.0460743616593343E-15 -> 1.2785353309169641E-15; 5 s `0.05:4` 2.0679702023155786E-16 -> 2.4126319027015086E-16, 1.169790657461262E-16 -> 3.21692430801847E-16; 5 s `0.02:4` 3.0914625274013867E-16 -> 2.060975018267591E-16, 1.1623048462881494E-16 -> 2.324609692576299E-16. Newton counts and MIXED_GAS_COST unchanged (286); chain-100 bitwise.
- **Effect on the target classes: none.** Every seed of E7 (6 / 3), E10 (4 / 2), E1 (9 / 13) and D (1 / 5), random 236 included, still holds with the probe (`d08`); only four change class. Fuzz 1-200: random +7 cleared, defaults 3 cleared and 2 worse (59, 101).
- Recommendation: do not implement P4 as a start estimate. The classes that start from a wrong junction state fail on what the junction holds (its minted composition and phases), not on its pressure (section 3).

### 3. Seed 236 (random): three liquids into one junction

**Physics.** A 332 kPa crude generator (341 K) and a 346 kPa water generator (350 K), both above a closed 1 m3 nitrogen tank at 1 atm, feed it through one junction pipe. Both liquids pour in and compress the nitrogen. The crude generator stops once the junction stands at about 332 kPa (it cannot receive); water keeps coming until the tank's gas stands near 346 kPa plus the water column from the generator three blocks up (about 28 kPa), so near 375 kPa. By then about 0.7 m3 of liquid has entered (the gas squeezed from 1 m3 at 101 kPa to about 0.27 m3), mostly within the first minute at a few tens of kg/s through the 0.1 m pipe (estimate, not measured). Then rest.

**What the solver does** (`b21-trace-236`): the junction is minted as 1 atm nitrogen (the tank has the lowest id). The cold rate seed states it at 230.7 kPa and 350 K with nitrogen vapour, crude liquid and water all present in its seed, and at pass 0 two of the junction's rows (19 and 21 of its block) do not depend on any unknown: the factorization reports an empty row. Their residuals are -1.0 and -0.99977, the value a closure row takes when the amounts it sums are zero. Every step size then fails the same way (singular). Unconfirmed at row level: which phase the two rows belong to (the amount normalization and the partial-pressure closure of a phase whose amounts decode to zero is the reading consistent with the residuals).

**Structural proposition: one step that makes a node's phase set match what reaches it.** Today three separate places decide a node's phases: the entry traces of `initialPhaseSeeds` (every reachable component, with D10's 1e-12 water trace for dry junctions), the phase correction of a converged pass, and the E3 fix (`7e5391a7`), which restates a node as its flash when the reconstruction books a hydrocarbon into a node with no hydrocarbon phase and then solves the pass again. They agree only after the fact. The proposition is one step, run where a pass is set up: each node's phase set for the pass is the phase set of the flash of its own inventory plus what its open inflows bring in the step at the start flows, and a phase with no amount gets no rows. E3's restate-and-resolve becomes the same step run again after the reconstruction; D10's water trace becomes a special case of it. Choices this needs (not forced by physics, so not implemented):
- which inflow is used before the solve (the start point's flows times the step, the one-way estimate, or a fixed entry trace as D10);
- whether a phase with no amount is dropped from the layout for the pass (changes the unknown count of such nodes) or kept with a trace;
- the step's reach: every node (moves every seed and so the junction lines) or only junctions a new component reaches (bitwise elsewhere).
Recommended: only junctions a new component reaches, inflow from the start point's flows, a phase with no amount dropped; to be measured before a decision (it would touch E2, E3, E4 and E7 at once).

## Group C: trace truncation in the flash (2026-10-12)

- Author: Claude (as a thread of the orchestrator), same worktree and branch, from `c346933c` (with the uncommitted start of an interrupted predecessor, kept as `src/wip-trace-fold-interrupted.patch`). Code `051ad5f5` (D34), tests `0eb6655c`. Owner decisions D33, D34; sub-choices A83-A87 (`DECISION_LOG.md`). Material: `tools/phase-ports-probes/hold-classes/` (`trunc-sweep.sh`, `src/scratch-phase-threshold.patch`, `src/instr-fold-trace.patch`, `src/scratch-no-dead-head-closure.patch`, `src/scratch-rest-switch.patch`, `src/FoldGateWorldsProbeTest.java`), logs `f01`-`f28`. All runs through the Gradle route (no Gradle-free runner).

### 1. Physics

A phase that holds a billionth of a node's amount has no physical meaning at the scale of a block of pipe or a 1 m3 tank. The drain that motivated this (`PhasePortPriorityTest` drain-5.0): after its water has drained, a tank of humid nitrogen at its dew point keeps cooling as it vents, and the model condenses 1e-17 to 1e-14 of the vessel's moles as free water, a film of a few picograms. A real film that thin evaporates or wets the wall. A nitrogen tank's entry trace of crude (1e-12 of its moles, planted so the layout can receive crude) condenses as a hydrocarbon liquid of a few 1e-12. The Newton states a node's balances to 1e-9 of its amount, so it cannot resolve the split of such a phase: the equations of the split no longer depend on it (E2), or every correction moves the phase by more than it holds (E5). Nothing that physics fixes depends on these phases, and the owner's rule (D34) removes them where phases are decided, in the flash: below 1e-9 of the node a phase does not exist, and its material is in the node's other phase. Nothing leaves, nothing is booked, every component amount is unchanged.

### 2. What is implemented (`051ad5f5`)

- **One rule, one place.** `FluidThermodynamics.foldTracePhases` is the rule on one split (A83, A84); `FluidThermodynamics.folded(State)` restates a state at its T and P with its trace phases folded, solids kept, and returns the state itself when it has none (so every state without a trace phase is bitwise as before). `flashTP` is `folded(equilibriumTP(...))`.
- **Every fluid-network flash entry point passes through it.** The TP flash (`flashTP`: initial charges, boundaries, the D16 flush's restatement, the E3 restatement, `initialPhaseSeeds`, `waterTraceSeed`, drained seeds, the phase correction); the energy-volume flash (`InventoryEquilibrium.solve`, used by the interval refresh: its passes start from a folded state, its regime test compares against the folded flash, and its result goes through `repartition`); the reconstruction (`ConservativeTransport.repartition` folds the restated split with the same rule).
- **The Newton's decode.** A pass's layout (which phase unknowns and rows a node has) is the phase code of its seed (`PhaseLayout` reads `liquidVolume() > 0` and the vapour amounts). The step solver folds every pass's seeds once at the pass's setup (`foldedSeeds`); the decode inside the residuals never folds. So the state function the Newton differentiates has no jump inside a pass, and a phase may shrink below the share inside a pass; it is folded at the next pass boundary or by the reconstruction. This is how the brief's "the state decode returns no trace phase" is met under its own continuity rule: the decode returns the pass's phase set, decided by the flash rule once per pass.
- **Reconstruction and equation gate.** When the reconstruction folds a phase the pass solved (`phaseFolded`: a phase of the Newton state absent from the restated state; the restatement keeps every phase whose booked amounts are not all zero, so a missing phase is a folded one), the pass is solved again from the folded states (`traceFoldPasses` counter), so the Newton layout, the reconstruction and the equation gate read the same phases. The gate's polish (D13) refuses a point whose reconstruction folds.
- **D16 stays where it is.** The booked removal of a trace component below 1e-24 of its node (`PassiveIntervalSolver.flushTraces`) is a different question: the flash decides which phases exist, the interval solver decides which components exist. A component only one trace phase holds is folded with that phase into the other phase (it stays in the node); a component below 1e-24 is removed and booked, whatever phase holds it.

### 3. Where each reader gets its phase set (before, after)

| Reader | Before | After |
|---|---|---|
| Newton layout (unknowns per node) | the pass seed's phase code; seeds from the start state, the TP flash (entry traces), Newton iterates (after a mode change), the reconstruction | the same seeds, folded at every pass's setup |
| Phase correction (converged and failed points) | TP flash; compares phase codes | folded TP flash; ignores a phase the trace rule removed in this solve (A85, A86) |
| Reconstruction (`repartition`) | the candidate's phases, split in proportion; the E3 flash for a hydrocarbon arriving with no phase | the same split, folded; the E3 flash (now folded) |
| Equation gate | the pass layout encoding the reconstruction | the same; a pass whose reconstruction folds is solved again first |
| E3 appearance (`7e5391a7`) | the flash of a node with no hydrocarbon phase; reseed when the arriving component is at least 1e-24 of the node (A67) | unchanged threshold, now on the folded flash: a foldable trace phase never appears; an unfoldable one (a nitrogen bubble in water, A84) appears as before |
| Energy-volume flash (interval refresh) | the previous state or its TP flash, regime test against the TP flash | the same on the folded state and the folded flash; the result through `repartition` |

### 4. Stop points (choices physics does not force)

**(a) The fold's own latent heat (A85, implemented as the default).** A fold at T and P changes the gas's energy by the folded condensate's latent heat; the next Newton conserves the node's energy inventory, so the gas it solves without the condensate is cooler, and its TP flash condenses 2 to 5 times the folded amount (drain-0.1: 1.0e-9 folded, 2.4e-9 given back, `f02`; `LevelHeadTest` drain-0.1: 1.02e-9 seed, 4.6e-9 given back, `f07`). For humid gas there is a band of roughly [0.2e-9, 1e-9) where the rule has no fixed point: without a tie-break the pass sequence cycles at every step size (`PhasePortPriorityTest` drain-0.1 and `LevelHeadTest` drain-0.1 failed on "Phase/device active-set cycle"). Options:
- (a1) **Monotone within a solve (implemented):** a phase the trace rule removed in a solve is not given back by the phase correction in that solve, and the record is part of the cycle key, as the trace support's reactivations already are; the next solve decides afresh. No cycle; cost: a saturated gas being solved repeats fold, re-condense, fold in each solve (`LevelHeadTest` drain-on-head: 3107 to 4906 Newton solves; every other printed counter of the suites identical).
- (a2) Carry the record across the solves of an island until the TP flash's amount exceeds a release share (a band, e.g. 10x): cheaper, needs a band.
- (a3) An energy-consistent fold: fold only when the node without the phase, at its own energy and volume, would not form it again above the share (a UV test inside the fold): no band, no cycle, a UV solve per fold candidate.
- Recommended (a1) as it is; (a3) if the cost in saturated-gas solves matters in play.

**(b) A trace phase with no receiving phase (A84).** The world-default E5 and E2 cards hold on a nitrogen bubble of 1e-15 to 1e-10 m3 in a water-only junction. Physically it dissolves (nitrogen's solubility is about 1e-5 mole fraction per atm, so a 1e-9 trace dissolves at any pressure), but the model's free water holds no dissolved gas, so the fold has nowhere to put it and keeps it. Options: (b1) leave it (implemented); (b2) a dissolved-gas amount in free water (a model change: free water's state would carry components, and every reader of free water would change); (b3) remove such a phase and book it, as D16 removes a component (contradicts "nothing booked" for this one case). Recommended: (b2) to be specified with the owner, since it is the physics; (b3) only as a stopgap.

**(c) The threshold sits at the Newton's own resolution.** Defaults seed 707 (section 6) holds on a free water phase of 1.30e-9 of its tank, just above the share: a phase at 1.3 times the Newton tolerance is as unresolvable as one below it. The owner's 1e-9 is kept; a larger share (1e-8 to 1e-7) would cover such phases at the cost of folding more (measured largest fold at 1e-9: 0.73 µg, section 5). Not changed without the owner.

### 5. The threshold compared (`f10`, `trunc-sweep.sh`; 0 = fold disabled on the same code)

| Share | Folds in the gate suites (largest share, largest mass) | Junction lines, MIXED_GAS_COST, chain-100 | Campaign random (cleared / worse vs 0) | Campaign defaults (cleared / worse vs 0) |
|---|---|---|---|---|
| 0 (off) | none | identical, 286, 0.000e+00 | 816 | 818 |
| 1e-24 | 0 | identical, 286, 0.000e+00 | 816 (0 / 0) | 818 (0 / 0) |
| 1e-12 | 49 (8.8e-13 of a node; 0.65 ng water, one gas fold of 1.7 µg) | identical, 286, 0.000e+00 | 819 (3 / 0: 244 E2, 561 E7, 629 C) | 818 (0 / 0) |
| **1e-9** (implemented) | 1253, all free water but one gas (9.97e-10 of a node; heaviest 7.3e-10 kg = 0.73 µg; the gas fold 6.2e-13, 1.7 µg) | identical, 286, 0.000e+00 | **822** (6 / 0: 156 E5, 170 C, 561 E7, 629 C, 705 E5, 762 E4) | **817** (0 / 1: 707) |

`PhasePortPriorityTest` drain cases (the motivating test): drain-5.0 dries at slice 30 (off, 1e-24, 1e-12) and 29 (1e-9), final pressure 101324.9999954733 / 101324.9998557709 (1e-12) / 101324.9998230058 Pa, condensate drawn after the breakthrough 9.479422046870159e-4 / 9.47942097114891e-4 kg; drain-0.1 dries at slice 1406 / 1405 / 1392, 2 / 1 / 1 rejected steps, final pressure 101324.99988986526 / 101324.99992117015 / 101325.00000000009 Pa. Every assertion holds at every share.

### 6. What moved

**No bitwise reference moved**: the 33 junction lines are identical to `holds/12-junction-lines.txt` at every share, MIXED_GAS_COST 286, chain-100 exact 0.000e+00, adjacent 45/45 (`f27`). Printed lines of the suites that moved at 1e-9 against the fold disabled (timings masked, map order ignored; 825 lines): `PhasePortPriorityTest` slices and summaries of drain-0.1 (608), drain-5.0 (11), oil-water drain and overflow (70; ledger figures at 1e-16, the drawn masses at 1e-11 kg); `LevelHeadTest` drain lines (the two `LEVEL_HEAD_RUN` above: 0.1 s accepted 1500 to 1501, rejected 0 to 1, Newton solves 1505 to 3154; 5 s last-slice outflow 1.7395245244244961e-9 kg to 0.0, cycle rejections 11 to 2, Newton solves 184 to 334); `FedTankOpenOutletsTest` `F7_SLICE` / `F7_END` generator-first (pressure 91217.89400155596 to 91217.89400155695 Pa, the 12th digit); and one physical move, the stack test's `STACK_PASS` (`PhysicalFluidTopologyTest`): upper tank 104544.93743633144 to 104187.80295968213 Pa, lower 104567.87363434663 to 104210.65996667236 Pa, lower liquid 292.4263902720061 to 292.42536409594913 kg. Traced (`f13`): with the fold off, the drained upper tank keeps its last 1.44e-4 mol and then 8.6e-12 mol of water as a phase for two more slices, its bottom port drawing that liquid first while the tank expands to 94.7 kPa; with the fold the condensate (2.4e-11 and 7e-12 of the tank) is gone in slice 12 and the gas from below equalises from 99.7 kPa. The vessels are adiabatic, so the upper tank's gas ends compressed from different pressures: 305.08 K against 302.88 K at rest, and the 357 Pa difference. Both are valid adiabatic end states; the assertions (water passed down, headspaces within 100 Pa, every component conserved to 1e-10) hold.

**Worse seed, defaults 707** (`f11`, `f12`, replayed alone: held alone too, deterministic). An island with four water generators, three junctions and three tanks; tank 21 stands 1 m above junction 14 and drains its water through its bottom port while the junction's gas comes back up. At the hold, tank 21's seed holds free water at 1.30e-9 of its moles, just above the share, so the phase stays in the layout; the Newton's failed iterates throw it to 1e-4 to 6e-4 and stop at 1.11e-9 (tolerance 1e-9) at every step size (E6). The fold changed the path that led there; the failing phase is the vanishing-phase mechanism one notch above the threshold (stop point 4 (c)).

### 7. Gate tests and their proofs

| Test | Fix | With | Without (working tree only) |
|---|---|---|---|
| `TracePhaseFoldTest.theFlashFoldsATracePhaseIntoTheNodesOtherPhase` | the rule (a 1e-12 condensate folded with every amount kept; 1e-6 kept; a nitrogen bubble in water and a hydrocarbon trace in a steam-only gas kept; the crude entry trace not a liquid) | pass (`f18`) | fails (`f19`, share 0) |
| `TracePhaseFoldTest.crudeIntoTwoNitrogenTanksKeepsUp` (random 156, E5, 6 blocks: a 349 kPa crude generator under tank A, tank A's top into tank B's side) | the fold | pass; tanks at 180.5 kPa after 200 s | holds at tick 398, line search stall 0.5487 at every step size (the original hold) |
| `TracePhaseFoldTest.crudeAndNitrogenIntoAVoidKeepUp` (random 762, E4, 11 blocks) | the fold | pass | holds at tick 6959, equation gate 2.5e-7 at every step size (the original hold) |
| `DeadHeadBesideFlowingBranchTest` (group B stop point, option (a)) | `closeDeadHeads` | pass; tank 143.8 kPa, 0.32 kg/s of nitrogen through | **passes too** (`f22`, `f25`, `f26`) |

The smallest worlds come from `HoldMinimizerTest` with the fold disabled (`f14`), their mechanism from the pass trace with the fold disabled (`f17`: tank B's seed holds the crude entry trace as a liquid of 3.8e-12 of its moles in 156, 2.0e-12 in 705; a junction holds 3.7e-11 in 762; 561 cycles a junction's 3e-8 liquid in its failed iterates, a path change, not a trace phase, so it is not a gate world).

**The `closeDeadHeads` gate (stop point).** No world was found in which the closure prevents a hold on this tree: the six worlds of `DeadHeadedLineIslandTest` with the rest step bypassed (`src/scratch-rest-switch.patch`, `-Dnorest`) and the closure reverted all solve with every flow exactly zero (`f24`; the only difference is that the flat filter line's interior connection reports PASSIVE instead of CLOSED); a dead-headed run beside a nitrogen through-flow at feed pressures 101.5, 110 and 150 kPa, flat and rising four blocks, and its filter variant, solve the same with and without it (`f23`, `f25`). The pass loop's own closure of an illegal direction now reaches the same point, after the fixes since the closure was written (the reachable-component masks, the D16 flush, E3). `DeadHeadBesideFlowingBranchTest` is kept as a guard of the behaviour (A87). Options: (a) keep the closure and this guard (implemented); (b) remove the closure as absorbed by the pass loop and the reduction stage's feasibility step, measured first (gates, campaign); (c) keep it and assert its remaining effect (the interior connections of a dead-headed run reported CLOSED). Recommended (b), measured in its own group.

**A defect the first draft of that world found** (not the fold's, not fixed): a 130 kPa nitrogen tank venting through three pipe blocks into a 1 atm void, beside the dead-headed water run, holds at interval 3 on a line-search stall at 1.015e-9 at every step size, with or without the fold and the closure (`f20`, `f21`): the zero-flow root of a run whose drive decays to zero, the E5/E6 family (card E6, proposition (a) or (c)). From 200 kPa the same vent first cools the tank's water trace below 273.16 K (D14).

### 8. Gates and campaign at the end

Gates on `0eb6655c` (`f27`, run id `trace-fold-groupC-20261012`): fluid suites **508/508** in 121 classes (504 + the four new tests), 33 junction lines **identical**, MIXED_GAS_COST **286**, exact regression **0.000e+00**, adjacent **45/45**, fluid GameTests **33/33**.

Campaign on `0eb6655c` (`f28`, 1000 seeds per distribution, 8 workers, `holdFuzz.pipeBetween`): **822 random / 817 defaults**. Against group B (`d10`, `857963d9`): random 6 cleared, 0 worse; defaults 1 cleared (285, which also cleared with the fold disabled: a budget seed) and 1 worse (707, section 6). Identical to the sweep's 1e-9 campaign. Classes (random / defaults): C 72 / 9, E6 27 / 68, E2 19 / 10, E4 14 / 10, F 12 / 28, E1 9 / 15, E5 8 / 18, E7 5 / 3, E10 4 / 2, E8 3 / 6, E3 2 / 2, E9 2 / 6, D 1 / 5, A 0 / 1. `REMAINING_HOLDS.md` carries the counts and seed lists of this campaign.

### 9. What the brief asked that the code does differently

- "The state decode the Newton uses returns a state in which a phase below the share does not exist": the decode inside a pass returns the pass's phase set (folded at the pass's setup), and a phase may shrink below the share inside the pass; folding inside the decode would put the jump the continuity rule forbids into the residual (section 2).
- "One implementation every entry point passes through": the phase correction also reads the equilibrium before the fold (`equilibriumTP`, A86) to tell a fold from an equilibrium change; it never lays out or commits it.
- `PHASE_TRACE_FRACTION` is a constant; the sweep's override is a scratch patch only.

## Hold class E6, option (a) implemented (2026-10-12)

- Author: Claude (Opus 5.5, as a thread of the orchestrator), side branch `claude/e6-row-scale` from `2239a277` (the owner decides whether it is taken). Code `0563d4b1`, tests `464b154b`. Owner decision D35; sub-choices A88-A92 (`DECISION_LOG.md`). Material: `tools/phase-ports-probes/hold-classes/` (`src/E6RowScaleProbeTest.java`, `src/scratch-e6-donor-tol-counters.patch`, `src/instr-e6-failing-rows-and-scratch.patch`, `src/scratch-e6-newton-min-one-iteration.patch`, `src/scratch-e6-scale-from-donor-ends.patch`), logs `g01`-`g48`. Every comparison is against `2239a277` (junction lines `holds/12-junction-lines.txt`, campaign `f28`). No reference was re-recorded.
- One process slip: a single-test Gradle run (`g39`) started while the `g38` campaign's random half was running. It overlapped for about 11 s and stopped at test compilation (the probe source needs the scratch patch). Only `g38` random could be touched, and only through budget seeds; `g38` is the measured alternative of section 6, not the reported campaign (`g32`).
- The machine also ran the owner's own Minecraft instance (8 GB) through every run here. Wall times below are not timing-grade; cost is judged by counters.

### 1. Physics

A connection's flow is set by its driving pressure: the end pressures, the column between them and, at the velocity cap, the cap's own drop. At a zero-flow root the driving pressure is zero and nothing moves, so neither end is the donor. The row the Newton solves, `(driving - loss(q)) / S`, has the same root whatever positive `S` it is divided by. `S` only weighs the row against the others. It must therefore not depend on which way the fluid would go: a scale that jumps where the driving pressure changes sign puts a jump into the residual exactly at the root. With water at one end (cap drop about 1 MPa on a 50 mm line) and nitrogen at the other (about 10 kPa), the old `S = min(pressureScale, 2 limitDrop of the donor picked by the sign)` jumped by orders there (review E6 section 1, seed 33). Expected effect of a continuous `S`: converged results move by no more than the Newton tolerance, and the step solves that contracted linearly to the iteration limit at a zero-flow root converge.

### 2. What is implemented (`0563d4b1`)

- **The rule.** For a plain passive connection (no filter, no device), `scaleDrop` is the smaller of the two ends' velocity-cap drops, and `S = max(min(pressureScale, 2 scaleDrop), 1e-12)`. The clamp itself is unchanged: the capped branch still reads the cap of the donor picked by the driving pressure. A device or a filter keeps its flow donor's drop: a device does not turn round inside a pass (its active set closes it), and a filter's drop is its cake's resistance on the drawn carrier, re-read at every evaluation (A88). The comment at the site states why both ends, and keeps the mixed-gas batch's reason for the cap-based scale (review 7.1/7.5: both branches in one unit no coarser than the cap's own drop). The smaller of the two drops keeps that property on both sides of the root.
- **Structure.** `capDrop(edge, end, transport)` computes one end's cap drop and caches it per end on that end's transport object, the way `capSources`/`capPressureDrops` cached the donor end before. The filter branch no longer writes into that cache (it never read it back). `capMassFlows`, written and never read, is removed (A89).
- **Cost of the second end, with counters** (`g25`/`g26`, the twelve `MixedGasJunctionTransientTest` cases at 5 s, `src/scratch-e6-donor-tol-counters.patch`):

| Counter | Donor's scale | Both ends |
|---|---|---|
| cap-drop lookups | 30 895 | 51 706 |
| cap-drop recomputes | 25 397 | 33 440 |
| time in recomputes | 3.15 ms | 4.05 ms |
| residual evaluations | 3 520 | 2 928 |
| Jacobian builds | 438 | 353 |
| Newton solves / iterations | 286 / 2 519 | 273 / 2 187 |

The second end adds 20 811 lookups, of which 8 043 recompute (39 %): the block Jacobian perturbs one node at a time, and the decode cache hands every other end its unchanged transport object, so the cache answers the rest. The added recomputes cost 0.9 ms over twelve cases whose solves take about 500 ms in all (0.2 %), and the same cases save 13 Newton solves and 85 Jacobian builds.

### 3. Moved references (item 1; `g01`, repeated in `g48`)

- Fluid suites: 508 tests, **2 failures** (`ExtremeTopologyIslandTest` GRID twice, section 5). With the new gate tests: 510 tests, the same 2 failures.
- Junction lines: the 8 `LIQUID_JUNCTION` lines are identical. All 24 `MIXED_GAS_TRANSIENT` lines and `MIXED_GAS_COST` move.
- **MIXED_GAS_COST 286 -> 273** Newton solves. **chain-100 deviation 0.000e+00 -> 0.000e+00** (bitwise). Adjacent selection 45/45.

| Line (interval, key) | worstMoles old | new | change | worstEnergy old | new | change | Newton solves old / new |
|---|---|---|---|---|---|---|---|
| 0.1 0.05:4:false | 5.10099316571176E-15 | 2.826225943164624E-15 | -45% | 5.4395265571948675E-15 | 2.3980708477955868E-15 | -56% | - |
| 0.1 0.05:4:true | 1.8286551733222133E-15 | 2.447781903813072E-15 | +34% | 4.607488907237493E-15 | 6.409135210708564E-15 | +39% | - |
| 0.1 0.05:5:false | 3.0662160794169303E-15 | 2.929624453280403E-15 | -4% | 5.731974221560183E-15 | 3.5093719723837855E-15 | -39% | - |
| 0.1 0.05:5:true | 1.2841621953470297E-15 | 2.811717624843117E-15 | +119% | 1.8311814887738755E-15 | 1.0219174114770337E-14 | +458% | - |
| 0.1 0.05:6:false | 1.3627627019630802E-15 | 1.839729647650158E-15 | +35% | 7.603639273498202E-16 | 8.188534602228832E-16 | +8% | - |
| 0.1 0.05:6:true | 7.835885536287712E-16 | 7.495194860796941E-16 | -4% | 2.864912974372031E-15 | 2.5695611213439864E-15 | -10% | - |
| 0.1 0.02:4:false | 5.514587206174876E-16 | 1.3739833455117274E-15 | +149% | 1.2785353309169641E-15 | 1.5691115424890016E-15 | +23% | - |
| 0.1 0.02:4:true | 1.2013212947883937E-15 | 7.037629446838925E-16 | -41% | 2.9155899314157096E-15 | 2.449095542389196E-15 | -16% | - |
| 0.1 0.02:5:false | 8.616542509648244E-16 | 9.305865910420103E-16 | +8% | 2.5280130406767248E-15 | 1.9759182386898538E-15 | -22% | - |
| 0.1 0.02:5:true | 7.556908400314501E-16 | 1.0648370927715887E-15 | +41% | 2.886434032101552E-15 | 1.1662359725662838E-15 | -60% | - |
| 0.1 0.02:6:false | 9.61788341858209E-16 | 2.2058348824699504E-15 | +129% | 8.71728634716112E-16 | 1.1623048462881492E-15 | +33% | - |
| 0.1 0.02:6:true | 8.915014561666219E-16 | 1.0304875091337956E-15 | +16% | 2.769810434844924E-15 | 2.0992247506193108E-15 | -24% | - |
| 5 0.05:4:false | 2.4126319027015086E-16 | 7.582557408490455E-16 | +214% | 3.21692430801847E-16 | 3.801819636749101E-16 | +18% | 28 / 26 |
| 5 0.05:4:true | 5.968135467490589E-16 | 3.775288099761989E-16 | -37% | 5.611685207532844E-16 | 4.430277795420666E-16 | -21% | 26 / 26 |
| 5 0.05:5:false | 2.72552540392616E-16 | 3.4069067549077004E-16 | +25% | 2.924476643653155E-16 | 1.7546859861918926E-16 | -40% | 33 / 28 |
| 5 0.05:5:true | 5.780909902760545E-16 | 1.36276270196308E-16 | -76% | 1.7721111181682666E-16 | 4.725629648448711E-16 | +167% | 27 / 27 |
| 5 0.05:6:false | 2.38483472843539E-16 | 3.4069067549077004E-16 | +43% | 2.339581314922524E-16 | 2.047133650557208E-16 | -13% | 35 / 31 |
| 5 0.05:6:true | 1.6350177741836058E-16 | 3.0662160794169303E-16 | +88% | 5.611685207532844E-16 | 3.2488703833084885E-16 | -42% | 29 / 29 |
| 5 0.02:4:false | 2.060975018267591E-16 | 3.0914625274013867E-16 | +50% | 2.324609692576299E-16 | 1.7434572694322239E-16 | -25% | 16 / 16 |
| 5 0.02:4:true | 3.2632104455903524E-16 | 2.9135375552511E-16 | -11% | 7.288974828539274E-16 | 1.7493539588494257E-16 | -76% | 16 / 16 |
| 5 0.02:5:false | 4.825263805403017E-16 | 3.7912787042452273E-16 | -21% | 6.392676654584821E-16 | 2.6151859041483357E-16 | -59% | 20 / 18 |
| 5 0.02:5:true | 4.121950036535182E-16 | 3.4349583637793186E-16 | -17% | 4.081825903981993E-16 | 4.956502883406707E-16 | +21% | 17 / 17 |
| 5 0.02:6:false | 4.1359404046311573E-16 | 3.0914625274013867E-16 | -25% | 4.35864317358056E-16 | 4.35864317358056E-16 | 0% | 20 / 20 |
| 5 0.02:6:true | 2.7479666910234547E-16 | 3.4349583637793186E-16 | +25% | 3.2071489245572804E-16 | 1.1662359725662838E-16 | -64% | 19 / 19 |
| MIXED_GAS_COST | | | | | | | 286 / 273 |

What these lines measure: the worst relative error of the ledger (tanks plus junction holdup against the boundary transfers) over the run. The reconstruction books every amount exactly, so these are floating-point roundoff, 1e-16 to 1e-14, under the test's 1e-12 bound. A change of iteration path reshuffles the roundoff. The relative changes above (up to +458 %) are changes of roundoff, not of any physical quantity; section 4 measures the physical state.

### 4. Accuracy against a refined reference (item 2)

**Method.** `E6RowScaleProbeTest.mixedGasStates` runs the twelve cases at 0.1 s and prints, at 10, 13 and 16 s, every tank's pressure, temperature and moles, the junction's, and what each void took, with every digit. It reproduces the junction lines bit for bit (`g14`/`g15`). The refined reference uses a pass tolerance 1000 times tighter (1e-12 instead of 1e-9, `-De6tol=0.001`) **and** a fixed 5 ms substep (`PassiveIntervalSolver.Settings(0.005, 0.005, 100000)`, `-De6step=0.005`), with both scales (`g23`, `g24`); the runs compared against it use the same 5 ms step at the production tolerance (`g21` new, `g22` old). Working tree only.

Why the fixed step: a tighter tolerance alone (`g16`, `g17`, adaptive steps) moved the junction's composition by up to 1.4e-3 against the production runs, far more than the scale change did. A different Newton path changes which substeps the controller accepts, and then the backward-Euler time error, not the Newton tolerance, sets the distance. At a common fixed step the two refined runs agree to 1.2e-11 in pressure, 5e-9 in tank amounts and 2.9e-7 in the junction's amounts (the worst case is 0.05:6:false, where the refined old run took 166 rejected substeps).

**Distance from the refined reference** (largest over the three times; pressure and amounts relative, amounts relative to the node's total; the junction holds 0.3 to 0.6 mol against a tank's 38):

| Case | New: P | tank n | junction n | T (K) | Old: P | tank n | junction n | T (K) |
|---|---|---|---|---|---|---|---|---|
| 0.05:4:false | 1.2e-10 | 8.2e-9 | 7.7e-7 | 2.9e-6 | 1.0e-10 | 7.5e-9 | 3.7e-7 | 2.7e-6 |
| 0.05:4:true | 2.0e-11 | 4.1e-11 | 1.7e-9 | 1.2e-7 | 1.6e-11 | 1.4e-8 | 6.3e-7 | 2.9e-5 |
| 0.05:5:false | 5.8e-13 | 1.9e-8 | 1.5e-6 | 7.8e-6 | 2.1e-10 | 6.3e-9 | 5.1e-7 | 2.7e-6 |
| 0.05:5:true | 1.9e-11 | 3.7e-11 | 1.5e-9 | 1.2e-7 | 1.3e-11 | 1.6e-8 | 7.7e-7 | 3.4e-5 |
| 0.05:6:false | 6.5e-11 | 2.3e-10 | 2.5e-8 | 1.0e-7 | 3.6e-11 | 2.0e-9 | 1.8e-7 | 6.7e-7 |
| 0.05:6:true | 2.0e-11 | 3.4e-11 | 1.7e-9 | 1.3e-7 | 6.2e-11 | 4.4e-9 | 3.0e-7 | 1.3e-5 |
| 0.02:4:false | 3.5e-11 | 3.5e-10 | 1.9e-7 | 9.5e-7 | 4.8e-11 | 3.0e-11 | 2.0e-9 | 3.3e-8 |
| 0.02:4:true | 6.8e-12 | 6.7e-11 | 5.1e-8 | 1.6e-6 | 3.7e-11 | 7.0e-11 | 3.8e-8 | 1.3e-6 |
| 0.02:5:false | 2.9e-11 | 1.4e-11 | 5.1e-9 | 1.6e-8 | 3.2e-11 | 2.0e-11 | 5.5e-9 | 4.7e-9 |
| 0.02:5:true | 6.4e-12 | 1.3e-11 | 1.2e-8 | 3.5e-7 | 4.7e-11 | 1.2e-10 | 8.6e-8 | 3.1e-6 |
| 0.02:6:false | 2.9e-11 | 3.7e-10 | 2.8e-7 | 1.3e-6 | 3.2e-11 | 1.8e-11 | 3.8e-9 | 1.0e-8 |
| 0.02:6:true | 3.5e-12 | 4.3e-11 | 3.0e-8 | 9.5e-7 | 2.9e-11 | 7.2e-11 | 3.5e-8 | 1.3e-6 |
| **largest** | **1.2e-10** | **1.9e-8** | **1.5e-6** | **7.8e-6** | **2.1e-10** | **1.6e-8** | **7.7e-7** | **3.4e-5** |

**Result: proven.** Old and new are the same distance from the refined reference, in every quantity: pressure below 2.1e-10 relative, tank amounts below 2e-8 after 3200 steps, temperatures below 3.4e-5 K, junction amounts below 1.5e-6 of its small holdup (7e-7 mol, 2e-8 of a tank). The old scale is not closer than the new one in any column. The scale change moves the converged result within the error the Newton tolerance already leaves; it changes no physics. The ledger figures of the moved lines at the refined tolerance (adaptive, `g16`) are again roundoff: 4.1e-16 to 2.7e-15 (moles), 1.1e-15 to 3.4e-15 (energy).

### 5. The extreme grid (item 3)

**Physics.** Case 1 of the owner's extreme islands: ten nitrogen generators (110 to 200 kPa, 350 K) feed a 2 x 10 mesh of junction pipes that leads to ten 1 m3 tanks (100 to 190 kPa). Generators push in, tanks fill, and the island comes to rest with every tank at exactly the highest generator's pressure (200 kPa; 164 kPa at a fifth of the spread) and every other generator closed. A tank can never rise above 200 kPa: flow stops when the pressures are equal, and a generator cannot take anything back. The rest is reached within about 15 s at 5 s slices.

**What fails.** The two failing tests are the uncapped fixture at 5 s and the reduced fixture (spread 0.2) at 0.1 s. No step holds: every interval converges. The oracle "no tank above the highest generator" fails with a margin of `pMax * 1e-9`:
- `theFullCasesIntegrateWhenTheVelocityCapDoesNotSaturate [GRID]`: "grid-10@1.00 at 5.0 s: 350 oracle failures, first interval 4: tank 31 at 200000.0002114231 Pa, above the highest generator" (margin 200000.0002 Pa; overshoot 2.11e-4 Pa = 1.06e-9).
- `theCasesAtAFifthOfTheSpreadIntegrate [GRID]`: "grid-10@0.20 at 0.1 s: 754 oracle failures, first interval 31: tank 31 at 164000.00017145998 Pa" (margin 164000.000164 Pa; overshoot 1.05e-9).

**What the solver did** (`E6RowScaleProbeTest.gridTrace`, `g02`-`g11`; default-cap fixture, which passes narrowly, shown because it carries the bookkeeping columns):

| Interval | tank max - 200 kPa | top generator flow | its mode | supplied in the interval | tanks + junctions hold |
|---|---|---|---|---|---|
| 2 | -1.09e-3 Pa | 1.47e-2 kg/s | PASSIVE | 7.4e-2 kg | 17.945174161384 kg |
| 3 | -2.18e-8 Pa | 2.36e-8 kg/s | PASSIVE | 1.2e-7 kg | 17.945174279188 kg |
| 4 to 8 | -2.18e-8 Pa (unchanged) | 3.80e-10 kg/s (unchanged) | PASSIVE | 1.9e-9 kg each | +1.9e-9 kg each |
| 9 | +1.62e-4 Pa | 2.3e-10 kg/s | CLOSED | 1.1e-9 kg | 17.945174289833 kg |
| 10 to 40 | +1.61e-4 Pa | 0 | CLOSED | 0 | constant |

At rest, each step's start point already satisfies every row within 1e-9, so the Newton returns it at zero iterations (`SparseNewton.solve` tests the norm before the first iteration). That start point carries the previous step's flow, 3.8e-10 kg/s from the top generator, which the reconstruction books into the tanks exactly. But the reconstruction states each node at the candidate's temperature and pressure with the booked amounts (`ConservativeTransport.repartition`), so the tanks' stated pressure stays put while their contents grow. When the lag between the stated state and the booked amounts passes the tolerance (interval 9: 1.06e-8 kg booked since interval 3 into 17.9 kg, 6e-10 of the contents, plus the step's own share), the Newton moves the states all at once. The tanks then stand 1.6e-4 Pa above the generator, the pass loop closes the generator (it would have to receive), and the excess stays for good. The rows involved are each tank's balance and volume rows (node level, tolerance 1e-9 of the node's amount) and the top generator's hydraulic row (edge 3), whose flow is carried, not re-solved.

**The old scale does the same, smaller.** Uncapped fixture with the donor's scale (`g05`): the tanks are stranded 3.2e-5 Pa above 200 kPa from interval 3 on, generator CLOSED, and the test passes because 1.6e-10 is under the margin. Reduced fixture with the donor's scale (`g07`): the stale flow is 4.8e-11 kg/s and the tanks stay 4e-9 Pa below. With the new scale the stale flows at rest happen to be larger (3.8e-10 against 3.4e-12 kg/s on the default fixture), so the jump is larger.

**It is the tolerance, not the physics** (`g08`-`g10`, `-De6tol=0.01`, the pass tolerance 100 times tighter): uncapped new scale, overshoot **0** (the tanks end at 200000.00000000000 Pa); reduced fixture new scale, 1.5e-6 Pa (from 1.7e-4); uncapped old scale, 0.

**What the test asserts.** A physical bound (no tank above the highest generator), with a margin of exactly one Newton tolerance (1e-9 relative, the share a node's balance is closed to). The overshoot it now catches is bounded by that tolerance plus one step's stale inflow, which is just over one tolerance (1.05e-9 to 1.06e-9). So the test does not assert the old scale. It asserts the physics at a margin the solver cannot guarantee by construction, and the old scale's trajectories happened to land inside it (at most 1.6e-10 above, measured on the uncapped fixture; below the generator on the other two).

**Stop point: options** (none forced by physics; the brief forbids loosening the margin, so nothing is changed):
- (G1) A step takes at least one Newton iteration from a nonzero residual, so a flow is always the step's own, never carried over (`src/scratch-e6-newton-min-one-iteration.patch`, measured, `g12`, `g13`). All ten extreme tests pass, and the grid's Newton solves drop from 162 to 40 at 5 s, because flows really decay to zero and the rest certificate takes over. But 6 other gate tests fail on exact-rest identities: `IslandCertificateTest.aSettledClosedPairCertifiesWithALongHorizon` (drift 6.5e-12, expected 0), both `FluidCheckpointFormatTest` certificate round trips, `TraceFlushLedgerTest` ("the vessel without a trace books nothing"), `LevelHeadTest.twoTanksAtOnePressureEqualiseTheirBottomsMonotonically` (the bottom difference grows 6 times) and `FilterBlockLineIslandTest.valveLineToATankKeepsIntegratingOnceTheTankIsFull` (held at interval 9). A global solver change.
- (G2) The reconstruction restates a vessel's pressure from its booked amounts at its own volume (a UV restatement) instead of keeping the candidate's pressure, so the stated state never lags the booked contents and a stale inflow shows at once. Not measured; it moves every committed vessel state at roundoff and the bitwise references with it.
- (G3) A rest band at the Newton's resolution (an island whose every drive is below the row tolerance is at rest). Contradicts A72's "no band".
- (G4) The owner restates the oracle's margin at the solver's provable resolution (two tolerances: the node balance plus the lag at its detection, 2e-9). Loosens a test bound; owner only.
- Recommended: (G2) measured as its own item, since a committed state that always matches its committed contents is the structural form; (G4) only if (G2) fails. The GRID failures block a re-baseline of option (a) until one of them is decided.

### 6. Campaign (item 4; `g32`, 1000 seeds per distribution, 8 workers, `holdFuzz.pipeBetween`, the Gradle route)

| Distribution | Clean at `2239a277` (`f28`) | Clean with option (a) (`g32`) | Cleared | Worse (each replayed alone, `g34`: holds alone, same class) |
|---|---|---|---|---|
| random | 822 | **833** | 20 | 9: 6 E1, 23 C, 132 E6, 170 C, 223 E6, 301 E6, 316 E6, 728 E6, 945 E1 |
| world defaults | 817 | **853** | 40 | 4: 138 E5, 236 E7, 584 E6, 872 E4 |

Cleared seeds by their class at `f28`: random C 72 122 228 286 683 708 763 961; E1 15; E2 244 274; E5 257; E6 89 459 736 773; F 71 757 839 844. Defaults C 915; E1 45 226 826 886 973; E4 660; E5 39 224 333 681; E6 19 29 35 246 247 401 491 518 521 568 572 606 624 633 645 646 707 724 740 757 892; E8 316; F 23 84 96 514 698 976 1000.

Class counts `f28` -> `g32` (random / defaults): C 72 -> 47 / 9 -> 14, E6 27 -> 52 / 68 -> 31, F 12 -> 6 / 28 -> 7, E1 9 -> 12 / 15 -> 9, E2 19 -> 17 / 10 -> 17, E4 14 -> 12 / 10 -> 21, E5 8 -> 3 / 18 -> 14, E7 5 -> 6 / 3 -> 5, E8 3 -> 3 / 6 -> 8, E10 4 -> 4 / 2 -> 5, E3 2 -> 1 / 2 -> 3, E9 2 -> 3 / 6 -> 7, D 1 -> 1 / 5 -> 5, A 0 / 1. Per-seed transitions: `g33-transitions.txt`.

**F (budget).** Defaults: of the 28 F seeds, 7 cleared (23 84 96 514 698 976 1000), 14 now hold in a step class instead (C 301 431; E2 829; E4 181 705 768; E5 108 511 896; E6 17 208 356 397; E8 622), 7 remain F (71 72 89 113 833 911 994); no new F seed. Random: of 12, 4 cleared (71 757 839 844), 3 moved (441 E2, 556 E6, 579 E9), 5 remain (4 116 118 298 922), 1 new (311, was E2). Most default budget holds were step failures that spent the budget: with option (a) they either clear or fail in their own class within the budget.

**Two mechanisms under the E6 label** (`g35`; first hold of each seed, by the final residual):

| Campaign | E6 ending below 1e-6 | 1e-6 to 1e-3 | at or above 1e-3 |
|---|---|---|---|
| `f28` random / defaults | 8 / 41 | 1 / 12 | 18 / 15 |
| `g32` random / defaults | 7 / 7 | 11 / 10 | 34 / 14 |

The zero-flow signature (linear contraction ending just above the tolerance) falls from 41 to 7 in world defaults: that is the mechanism the rule removes. In random worlds, step solves that end far from convergence grow. 22 random seeds moved from C to E6 (77 78 98 151 168 175 208 214 336 401 431 456 514 594 622 750 832 833 842 891 895 976). Their C holds were Newton iterates leaving the nitrogen domain ("Nitrogen at 0.0633 Pa is below 100 Pa", "at 2000001 Pa is above 2000000 Pa"), typed as domain waits. With the new scale the same islands stay inside the domain and stop at the iteration limit instead, at residuals 0.09 to 0.5. These seeds were not clean before and are not clean now.

**One cause found among them, and a refinement measured** (random seed 132, worse; `g36`, `src/instr-e6-failing-rows-and-scratch.patch`): a 383 kPa water generator feeds junctions that drain into three 1 atm nitrogen voids. On a junction-to-void connection the smaller cap drop is the void's nitrogen (5.9 kPa, 1.2 kPa) instead of the water's (4.7 MPa), so `S` falls from 383 kPa to 11.7 kPa and the row weighs 33 times more in the line search. But a void only receives (D21): its nitrogen can never be the donor there, so it has no say in the row's scale. The same holds for the junction end of a generator connection. Refinement: the smallest cap drop among the ends the connection may draw from (`boundaryAllowed`), both ends only where both may supply (`src/scratch-e6-scale-from-donor-ends.patch`; `g37`, `g38`). Measured: random **834** / defaults **853** (option (a): 833 / 853); seed 132 cleared; E6 49 / 41; residual bands 8/6/35 and 9/15/17; MIXED_GAS_COST 275; 9 of the 24 mixed-gas lines differ from option (a); and one more gate test fails, `PhysicalRegistryTest.junctionHoldupsAreBookedAsConstructedAndRemintedAtATopologyEdit` (committed tick 927 of 1000: a 150 kPa methane generator filling two nitrogen tanks does not keep up). Not committed: no net gain, a new failure, and the random far-from-convergence growth persists under both forms (A91).

Campaign cost (sum of per-seed wall time, not timing-grade): random 880 s -> 632 s, defaults 1392 s -> 1522 s.

### 7. Gate tests and seed 707 (item 5)

| Test | World | With the rule (`g47`) | Donor's scale restored, working tree only (`g46`, `-De6donor=true`) |
|---|---|---|---|
| `ConnectionRowScaleTest.aTankDrainingItsLastWaterIntoAGasJunctionKeepsUp` | defaults seed 707, minimized with the donor's scale keeping E6 (`g44`, 11 blocks) | pass, 120 slices | holds at tick 8257, "Newton iteration limit at residual 2.24e-9" at every step size |
| `ConnectionRowScaleTest.aWaterGeneratorMeetingAJunctionMintedAsNitrogenAtZeroDriveCommitsItsFirstInterval` | the card E6 world (defaults seed 1, 4 blocks) | pass (first interval commits at tick 1114) | holds at tick 1084, "Newton iteration limit at residual 3.931201941689114E-7" (the card's value, bit for bit) |

**The card world of seed 1 is not clean with the rule** (`g29`, `g31`). It commits its first interval and holds at tick 1146 on "Interval substep limit" (class E8; 288 accepted and 736 rejected substeps advance 0.0138 s of 1.6 s; reasons "Backward-Euler boundary reopened" 433, "Newton iteration limit" 292, "No finite Jacobian" 10). The trace shows a different mechanism. The junction was minted holding 1.15 kg of the tank's nitrogen, its mass pinned. When water arrives (0.02 kg/s), the Newton's iterates evaporate it into that dry gas and cool the junction to the 273.16 K floor; later passes fail on "No finite Jacobian" with the junction's water at 1e-13, a trace (the E5/E10 family and the mint, D25). The full seed 1 moved E6 -> E8 in `g32` too. So the gate test on this world runs to tick 1140 and guards only the zero-drive start (A92); the 707 world is the full-run gate.

**Defaults seed 707 (the group C regression): cleared.** Alone with the rule: clean, 120 slices (`g41`). Alone with the donor's scale on this tree: holds at tick 8895 as in `f28` (`g42`). The trace of its last failing pass (step floor, dt 8.2e-8 s) names the cause. The worst row is junction 14 -> tank 19 (1.11e-9, tolerance 1e-9). Beside it, tank 21, a block above junction 14, joins it through its liquid port while holding a trace of free water (the 1.30e-9 phase of group C). That connection's ends have cap drops of 959 kPa (water at the port) and 10.5 kPa (the junction's gas-rich stream), so with the donor's scale its `S` jumped between 104 kPa and 21 kPa at the drain's zero-flow root. It was E6's mechanism, reached through a trace phase, not the trace threshold itself. The phase share stays 1e-9 (D35).

### 8. Gates at the end (item 6; `g48`, on `464b154b` + the uncommitted docs)

G1 fluid suites **510 tests in 122 classes, 2 failures** (`ExtremeTopologyIslandTest` GRID, section 5); the 24 mixed-gas lines and MIXED_GAS_COST 273 as in section 3; G2 exact regression **0.000e+00**; G3 adjacent **45/45**. **G5 (fluid GameTests) was not run**: the brief runs it only when the suites pass.

### 9. Stop points, with recommendations

1. **Re-baseline of the 24 mixed-gas lines and MIXED_GAS_COST** (owner, D35). Recommended: accept them when option (a) is taken. They are ledger roundoff, and the physical state moves within the Newton tolerance (section 4).
2. **The extreme grid** (section 5): (G2) measured next, (G4) only if it fails. Option (a) cannot be taken with two failing gates.
3. **Far-from-convergence iteration limits in random worlds** (section 6): random E6 at or above 1e-3 grows 18 -> 34, part of it the 22 seeds that left C. Recommended: keep option (a) as committed and work these as their own class (they end at 2 MPa domain edges in high-pressure transients, not at zero-flow roots). The one-way refinement is recorded as measured and gives no net gain.
4. **The seed 1 card world's later hold** (section 7): the minted dry-nitrogen junction meeting water. It belongs with the trace-phase cards (E5, E10) and the mint, not with E6.

### 10. What differs from the brief

- "A gate test for the rule from the smallest E6 world (defaults seed 1)": that world holds again 62 ticks later on another mechanism, so its test stops at tick 1140. A second gate world, defaults seed 707 minimized with the old scale, runs the full 120 slices.
- "The option is expected to move about 25 of the 33 lines": 25 move (24 lines and the cost line); the 8 liquid-junction lines are identical.
- The option's E6 count rises in the random distribution (27 -> 52) while the clean count rises (822 -> 833); section 6 separates the two mechanisms.
- Commit trailers name the model that wrote them (Claude Opus 5.5), not the brief's "Claude Fable 5.1".

## Hold class E6, grid overshoot (G2) and the one-way scale (2026-10-12)

- Author: Claude (Opus 5.5, as a thread of the orchestrator), on the side branch `claude/e6-row-scale` from `7395e984` (option (a), code `0563d4b1`). Owner decision D36 (option (a) taken; G2 and the one-way refinement measured here before (a) merges). Sub-choices A93-A97 (`DECISION_LOG.md`). Material: `tools/phase-ports-probes/hold-classes/` (`g2.init.gradle`, `src-g2/`), logs `h01`-`h27`. Every comparison is against the side branch at `7395e984` (junction lines `logs/g01-rule-G1-junction-lines.txt`, campaign `g32`) unless a line says otherwise. No reference was re-recorded.

### 1. The grid: physics

Ten nitrogen generators (110 to 200 kPa, 350 K) feed a 2 x 10 mesh of junction pipes that leads to ten 1 m3 tanks (100 to 190 kPa). Gas flows from high to low pressure. The tanks fill until every tank stands at the highest generator's pressure, 200 kPa; the other nine generators close one by one as the junctions pass their pressures. A tank cannot rise above 200 kPa: the flow into it stops when its pressure equals the pressure upstream, and a generator never takes gas back. In numbers: the tanks hold 17.9 kg of nitrogen together at rest, and 1 kg in the ten tanks is worth about 11 kPa, so the oracle's margin (1e-9 of 200 kPa, 2e-4 Pa) is 1.8e-8 kg of gas.

### 2. What the solver does: the trace (`h01`, `src-g2/G2GridProbeTest`, uncapped fixture at 5 s)

The probe prints, after each interval, the highest tank's stated pressure, how far each tank's stated state is from its booked contents (the relative volume and energy rows of the vessel's own equations, `PhaseLayout.fixedInventory`, with the inventory's V, n and U), and the pressure those contents imply (the same equations solved to 1e-14 from the stated state).

| Interval | stated tank max - 200 kPa | contents imply | volume lag | top generator | Newton solves / iterations |
|---|---|---|---|---|---|
| 1 | -1.42 Pa | -1.42 Pa | 4e-16 | 0.44 kg/s, PASSIVE | 36 / 332 |
| 2 | -1.4e-7 Pa | **+5.4e-5 Pa** | 2.2e-10 | 2.4e-5 kg/s, PASSIVE | 3 / 11 |
| 3 | -1.4e-7 Pa (unchanged) | **+3.1e-4 Pa** | 1.2e-9 | 2.4e-9 kg/s, PASSIVE | 3 / **0** |
| 4 | **+2.11e-4 Pa** | +2.12e-4 Pa | 2e-12 | 0, CLOSED | 4 / 9 |
| 5 to 11 | +2.11e-4 Pa (unchanged) | +2.17e-4 to +2.45e-4 Pa | 2e-11 to 1.3e-10, growing | 0, CLOSED | 3 / 0 each |
| 13 | +2.108e-4 Pa | +2.108e-4 Pa | 6e-16 | 0, CLOSED | 3 / 3 |

t3's diagnosis holds, and the trace adds two facts:
- The contents pass the generator before the stated pressure does. At interval 2 the tanks' booked gas already implies 5.4e-5 Pa above 200 kPa while their stated pressure is 1.4e-7 Pa below it. At interval 3 the Newton takes no iteration at all (each step's start point meets 1e-9), books the carried 2.4e-9 kg/s, and the lag reaches 1.2e-9. At interval 4 the Newton moves the states to their contents, the tanks stand 2.1e-4 Pa above the generator, and it closes.
- After the generator closes, zero-iteration steps still carry flows between the tanks through the mesh. The lag grows again by 1.8e-11 per interval, the contents' pressure rises to 2.45e-4 Pa, and the Newton only resets it at interval 12.

So the overshoot is the lag itself: booked gas that no stated pressure shows, until it passes the Newton's tolerance.

### 3. G2: the design (written before the code)

**Physics expectation.** A rigid tank's state is fixed by what it holds: its volume V, its amounts n and its internal energy U. Pressure and temperature follow from these through the equation of state (an energy-volume flash). If the stated pressure always follows the booked contents, a carried flow raises the stated pressure in the step that books it. The next step then reads that pressure in the tank's connection rows. In the grid: a stale 2.4e-9 kg/s books 1.2e-8 kg per 5 s step, worth 1.3e-4 Pa in one tank. Its connection row then reads that rise at the next step's start, and the Newton re-solves the flow as soon as the rise passes the row's tolerance, instead of after the whole lag has built up. Expected: the tanks end within one step's carried booking of 200 kPa, below 1e-9, and every stated state matches its contents to the closure's tolerance at every step.

**What `repartition` does today for a tank.** `ConservativeTransport.reconstruct0` books every amount and every joule exactly (the inventory, `moles[node]` and `energy[node]`). It then states the tank (`repartition`) at the Newton candidate's temperature and pressure, with the booked amounts split over the candidate's phases. The booked energy goes into the inventory only. So the stated state's own volume and energy differ from the tank's volume and booked energy by the Newton's residual. The interval start (`InventoryEquilibrium.refresh`) restates a tank from its inventory, but only once this lag passes 1e-8, and then solves it only to 1e-9 (1e-11 with a small headspace).

**What it will do.** One closure of a vessel's state to its inventory, used at every reconstruction and at every refresh:
- `InventoryEquilibrium.close(model, state, inventory, checkpoint)`: the vessel's energy-volume equations (`PhaseLayout.fixedInventory`) in the phase layout of `state`, solved from `state` to 1e-11, then given exactly its booked amounts (`repartition`), the full closure checked at 1e-8 as now. A state already within 1e-11 is returned as it is (an exact rest stays bit for bit).
- `ConservativeTransport.reconstruct0`: after the booking, every vessel (a node with its own row that is neither fixed nor a junction) is stated by `close` at its booked inventory, instead of keeping the candidate's temperature and pressure. The energy carried by each connection is booked first, on the repartitioned donor, as now. A junction keeps its state: it has no volume and its mass is pinned.
- `InventoryEquilibrium.solve` (the refresh) runs `close` inside its phase-regime loop. Its separate early return at 1e-8 and its solve tolerance of 1e-9 for a vessel with a large headspace go: one closure, one tolerance. The regime test (a TP flash at the closed state) stays in the refresh; in a step, the pass loop's phase correction already asks it of the reconstructed state.

**Choices.** Energy-volume and not TP: a TP flash at the candidate's temperature and pressure cannot move the pressure at all, and a closure at the candidate's temperature (volume only) would leave the energy lag, which moves the pressure by the same order (1e-9 of the temperature is 1e-9 of the pressure). Only the energy-volume closure states what the inventory holds; physics forces it. The tolerance 1e-11 is the refresh's own tighter tolerance, two orders below the step Newton's 1e-9, so a lag can never grow to the step's tolerance. It is not a physics choice; it tightens the refresh for gas tanks (1e-9 -> 1e-11) and does not loosen anything.

**Risks to measure.** (1) A nearly liquid-full tank: its pressure moves with its contents at the liquid's bulk modulus (2.2 GPa for water), so a 1e-10 balance residual is 0.2 Pa. That is the tank's real pressure, but it now shows in the connection rows, where the equation gate (1e-8) may refuse it and the D13 polish may run more often. (2) Cost: one small Newton per vessel per reconstruction while it flows. (3) Every committed vessel state moves at roundoff, so the mixed-gas lines, chain-100 and the exact regression may move.

### 4. G2 implemented (code `b6f2c072`, gate test `e350fd08`)

**The grid** (`h02`, `h08`: the probe of section 2 with G2; `h03`, `h07`: the tests). Every extreme test passes, both GRID cases included.

| Interval (uncapped, 5 s) | stated tank max - 200 kPa | contents imply | volume lag | top generator |
|---|---|---|---|---|
| 2 | +5.4e-5 Pa | +5.4e-5 Pa | 4e-16 | 2.4e-5 kg/s, PASSIVE |
| 3 | +3.4e-5 Pa | +3.5e-5 Pa | 5e-13 | 0, CLOSED |
| 9 to 34 | +2.5e-5 to +5.7e-5 Pa | the same | at most 9.6e-12 | 0, CLOSED |
| 35 to 40 | +2.47e-5 Pa | +2.47e-5 Pa | 1.4e-12 | 0, CLOSED (exact rest) |

- The stated pressure is now the contents' pressure at every interval. The worst overshoot over 40 intervals is 5.7e-5 Pa, 2.8e-10 of 200 kPa (before: 2.1e-4 Pa, 1.06e-9). It is already in the contents at interval 2, where the Newton iterated: the connection rows close to 1e-9 of their scale, and a flow error of that size into ten 1 m3 tanks is about 5e-5 Pa. That is the solver's resolution, not a lag.
- With the generator closed (intervals 9 to 34) the ten tanks still trade gas through the mesh at tolerance level: the highest tank moves between 2.5e-5 and 5.7e-5 Pa above 200 kPa. Each time a tank's lag passes 1e-11 its state is closed again. From interval 35 the island is at exact rest.
- Cost, by counters (uncapped, 5 s, 40 intervals; `h08`, with `src-g2/instr-g2-gate-polish-closure-counters.patch`): 154 network Newton solves (166 on the side branch) and 173 vessel closures that needed a Newton. The closures are counted under the same `newtonSolves` counter, so the test line reads 327 (before: 166). Each closure solves one vessel's own small system (its amounts, temperature and pressure), not the island's.

| Grid case (test line `newtonSolves`) | side branch `g48` | G2 `h07` |
|---|---|---|
| default cap, 5 s | 162 | 223 |
| default cap, 0.1 s | 419 | 592 |
| uncapped, 5 s | 166 | 327 (154 network + 173 closures, `h08`) |
| uncapped, 0.1 s | 426 | 930 |
| a fifth of the spread, 5 s | 143 | 271 |
| a fifth of the spread, 0.1 s | 408 | 1062 |

**The vented manometer: the polish starts from the reconstruction** (A94). With G2 alone (`h04`) the suites had one failure: `VentGatePolishTest` vapor-vent-5.0, one substep refused by the equation gate (5 polishes, 4 accepted; before 2 and 2). The trace (`h05`, `src-g2/instr-g2-gate-polish-closure-counters.patch`, `-Dg2instr=true`):
- Tank A holds 0.3 m3 of water under 0.7 m3 of nitrogen: 16 600 mol of water, 29.5 mol of nitrogen. Its component rows are scaled by the total amount (its gas is 70 % of its volume, so it is not a small headspace). A balance closed to 9.2e-12 of 16 600 mol leaves 1.5e-7 mol of nitrogen unresolved, 5e-9 of the cushion, 5e-4 Pa.
- G2 states that pressure. The nitrogen generator's line into A (pipe 97) then reads 1.67e-8 at the reconstructed point, above the gate's 1e-8.
- The D13 polish started from the Newton's candidate. That candidate already met the polish tolerance (9.2e-12 against 1e-11), so the re-solve returned it at zero iterations and the step was refused. The other four polishes started further out, took one iteration, and passed.
- Started from the reconstruction (the candidate with every node at its reconstructed state), the polish sees the cushion's pressure. All five polishes take one iteration and land at 1.6e-14 to 1.5e-12 (`h06`). The test passes. Its end state is within 2e-10 Pa of the side branch's (PA 104324.45686109469 -> 104324.45686109453 Pa).

Final suites (`h07`): **510 tests, 0 failures** (G3 adjacent 45/45). The gate test `VesselStateFollowsContentsTest` (12 intervals of the uncapped grid at 5 s: every tank within `CLOSURE` of its contents after every interval, none above the highest generator) fails on `7395e984` at interval 2 (a tank 9.2e-11 away from its contents, `h10`) and passes with G2 (worst lag 9.4e-12, highest tank 5.4e-5 Pa above 200 kPa, `h09`).

**What G2 absorbed.** The interval-start refresh's own early return (a lag up to 1e-8 accepted) and its 1e-9 solve for a large headspace: the refresh now runs the same closure as every reconstruction. The polish's start point follows from G2 (A94). Nothing else in the solver handled the lag; the D13 polish itself stays (it now closes the cushion pressure instead of the volume row).

### 5. Moved references (against the side branch, `g48` -> `h07`)

- The 8 `LIQUID_JUNCTION` lines are identical.
- **All 24 `MIXED_GAS_TRANSIENT` lines move again**, still ledger roundoff: new worstMoles 1.4e-16 to 3.2e-15, worstEnergy 1.5e-16 to 8.4e-15, under the 1e-12 bound. Full table: `logs/h19-moved-lines-table.md`. Examples: 0.1 s 0.05:4:false moles 2.826e-15 -> 2.044e-15, energy 2.398e-15 -> 1.111e-15; 0.1 s 0.05:5:true energy 1.022e-14 -> 8.358e-15; 5 s 0.05:6:false Newton solves 31 -> 35.
- **MIXED_GAS_COST 273 -> 309**: 275 network Newton solves (273 before) and 34 vessel closures that needed a Newton (586 closures in all; 552 found their vessel already within 1e-11).
- **chain-100 (exact regression, G2 gate): 0.000e+00 -> max deviation 6.276e-15 relative in moles** (interval 0, node 18, component 17: 2.61813592313263 -> 2.6181359231326136), **1.705e-13 K** (node 15: 350.03945531273797 -> 350.0394553127378 K), phase fraction 3.331e-16, flow 4.093e-11 relative (pipe 4: -4.217556210219395E-4 -> -4.217556210046756E-4 kg/s, 1.7e-14 kg/s against the controller allowance 3.2e-8). The exact gate therefore fails until the owner re-baselines it with the mixed-gas lines (D36: once, on the final code). Nothing was re-recorded.
- GameTest printed values: section 9.

### 6. The one-way scale refinement (A91): measured, left out

**t3's failure reproduced, and its cause.** On the side branch's code with the refinement (`h12`), `PhysicalRegistryTest.junctionHoldupsAreBookedAsConstructedAndRemintedAtATopologyEdit` commits tick 927 of 1000, as t3 found.
- World: a 150 kPa methane generator feeds a junction that fills two 1 m3 tanks of nitrogen at 1 atm. Physics: the tanks fill toward 150 kPa, the flows decay, the generator closes, and nothing ever flows back into it.
- What the solver did (`h13`, `h14`, `src-g2/instr-oneway-rejections-stall-rows-on-7395e984.patch`): no hold. 13 refused substeps: 5 backward-Euler state changes early on, and 8 Newton line-search stalls on the junction's first row. The stalled iterates want gas to flow backwards: each tank sends 2.5e-11 kg/s into the junction and the junction 5.0e-11 kg/s into the generator. So the tanks stood above the generator's 150 kPa, the grid's overshoot, and a generator cannot take gas back. Later stalls (2 s steps, generator closed) end at 1.06e-9 and 1.10e-9 with the tanks trading 7e-12 kg/s. Each refusal recovers by halving, and the island falls behind its schedule.
- The test asserts an operational fact (the island keeps up to tick 1000), not the old scale. With G2 the same test passes with the refinement (`h11`): the overshoot that drove it is gone.

**With G2, the refinement fails another gate** (`h15`: 511 tests, 1 failure): `ExtremeTopologyIslandTest.theFullCasesIntegrateAtTheDefaultCap [ALTERNATING_BOTH_SIDES]` at 0.1 s, "generator 7 received 1.4e-11 kg" in 9 intervals between 325 and 398. This is a physical oracle: a generator never takes gas back, beyond the solver's own allowance of 1e-10 kg/s (`ZERO_FLOW`, the same number below which the pass loop treats a forbidden direction as zero).
- Physics: at rest every node stands at 200 kPa. Generator 7 is the 200 kPa generator, joined to both pipe lines (two connections), so it closes.
- What the solver did (`h16`, `h17`, `src-g2/G2AlternatingProbeTest`): with the refinement, both of generator 7's connections stay PASSIVE and each carries 7.0e-11 to 1.0e-10 kg/s backwards, just under the 1e-10 at which the pass loop closes a forbidden direction. The generator's net intake is 1.4e-10 to 2.0e-10 kg/s. Without the refinement, the same connections are CLOSED with zero flow in those intervals, and no generator receives anything in 400 intervals.
- Why: the refinement scales a generator's connection on the generator's cap drop alone, never smaller than option (a)'s `min` of both ends. The row's tolerance band in flow grows with its scale. At a generator's closing root the backflow side is exactly where the one-way decision is taken, and a coarser row lets the backflow settle inside the band, below the closing threshold.

**Decision: the refinement is left out (A95).** Its premise (an end that can never donate has no say in the scale) is right about which way the fluid goes, but the scale also sets how finely the row resolves the flow near zero. At a one-way boundary's closing root, that resolution must come from the finer end. The suites do not pass with it (the grid item aside), so it is not committed. Not measured: a variant for voids only (seed 132's case, a water line into a nitrogen void), where the closing decision is the void's.

### 7. A class of its own: the first solve after a placement ends far from convergence (D36, item 3)

These are the 22 random worlds that moved from typed waits (C) into E6 with option (a): 77 78 98 151 168 175 208 214 336 401 431 456 514 594 622 750 832 833 842 891 895 976. They failed before and fail now; the review counts them apart from E6's zero-flow mechanism.

**Where they stand with G2** (`h22`): 21 are still E6; seed 431 ended on its budget in the campaign and holds in E6 when replayed alone (`h21`), so all 22 still hold in E6. Final residuals of the first hold: 18 of 21 at or above 1e-3 (up to 7.5 and 375), two between 1e-6 and 1e-3 (175: 1.1e-4, 895: 6.4e-4), one at 8.5e-8 (891).

**What they have in common** (measured on the report's layouts and hold ticks): every one of the 21 holds on the first interval after a block was placed. The committed tick equals the placement tick and the hold comes 1 to 5 ticks later. So the solve that fails is the first one on a freshly compiled island: new junctions minted from the compiler's guess, and connections whose two ends can stand at very different pressures. 13 of the 21 contain a pump or a compressor; the other 8 are generators, tanks, voids and pipes (seed 168: a 251 kPa nitrogen generator and a 209 kPa crude generator, two voids, a tank, a 200 kPa valve; one refusal leaves the nitrogen domain above 2 MPa, then 19 iteration limits at every step size ending at 6.7e-3).

**Physics expectation for such a start.** A newly joined line carries a pressure step of tens to hundreds of kPa across a pipe block. Flow starts at the velocity cap within milliseconds, and the junction settles between its neighbours' pressures within a few holdup times (0.05 s). Nothing in it is singular or stiff beyond the cap itself. A step that ends at a residual of 1e-3 to 1 has not found that state at all; that is a start-point (seed) question, not a resolution question.

**Proposition (owner).** Work this class on its own after option (a) and G2 merge: trace the smallest world (seed 168 has no pump or compressor) through its first post-placement solve (which rows are worst, where the iterates go), and compare with the junction mint (the supply-first mint `scratch-mint-supply.patch` measured in `w06`) and the cold-start rate seed. No change is made here.

### 8. Campaign (1000 seeds per distribution, 8 workers, `holdFuzz.pipeBetween`; against `g32`)

`h18` ran on `e350fd08` and `h22` on the final code `bf830c78` (the closure's fold check, section 10). They give the same clean counts and the same cleared and worse seeds; they differ only in which held seeds end on the wall budget, a timing effect (another thread's 4-worker fuzz shared the machine). The tables are `h22`.

| Distribution | Clean, option (a) (`g32`) | Clean with G2 (`h22`) | Cleared | Worse (replayed alone, one worker, `h21`) |
|---|---|---|---|---|
| random | 833 | **837** (838 counting 244, clean alone) | 7, all E4: 199 394 438 468 470 714 734 | 244 F in the campaign, **clean alone** (load); 400 E8, 737 E8 (both hold alone, same class) |
| world defaults | 853 | **856** | 8: E4 325 394 412 419 438 561 859; E5 916 | 59 E6, 224 E6, 518 E6, 226 E1, 757 F (all hold alone, same class) |

Class counts `g32` -> `h22` (random / defaults): **E4 12 -> 0 / 21 -> 6**, E6 52 -> 52 / 31 -> 34, F 6 -> 11 / 7 -> 11, E8 3 -> 7 / 8 -> 11, E2 17 -> 18 / 17 -> 20, C 47 -> 45 / 14 -> 12, E5 3 -> 2 / 14 -> 15, E1 12 -> 12 / 9 -> 10, E10 4 -> 5 / 5 -> 4; D, E3, E7, E9, A unchanged. Per-seed transitions: `h23-transitions-random.txt`, `h23-transitions-defaults.txt` (`h20` for `h18`).

- **E4, the reconstruction's equation gate, nearly vanishes.** Its holds were steps the gate refused at every step size after the reconstruction booked a cushion vessel's exact amounts at the candidate's temperature and pressure. With the vessel stated at what it holds and the polish starting from that state (A94), 12 of 12 random and 16 of 21 default E4 worlds no longer hold there (one default E6 world now holds in E4, hence 6). Of these 28, 7 + 7 are clean and the rest now hold later in another class (E8 6, E2 3, E5 2, E6 2, F 1).
- **The worse seeds, by mechanism.** Random 400 and 737 hold on "Interval substep limit" with every refusal the equation gate (about 520 refusals, last 6.8e-6 and 2.9e-7): the gate still refuses there, now at the cushion pressure in the connection rows, and the polish does not close it at those points. Defaults 59 and 518 hold at the first solve after a placement far from convergence (2.7e-3, 0.35: section 7's class), 224 at 1.4e-4 with a trace underflow and a nitrogen domain refusal, 226 on the interval-start rate pass (4.4e-6), 757 on its soft budget ("Device regime changed" refused the approximate fallback). Not traced further.
- **Budget holds (F), every one replayed alone with one worker** (`h21`, `h24`). Random: 116 118 298 922 hold on their budget alone (4; `g32` listed 6, not replayed alone); 4, 19, 23 are C, 92 E8, 311 E2, 431 E6 when alone; 244 is clean. Defaults: 71 72 89 113 220 431 705 757 833 911 994 hold on their budget alone (11; `g32` 7, of which none cleared); 1, 622 and 792 are E8 alone. New default budget seeds: 220 and 431 (C on `g32`), 705 (E4), 757 (clean).
- **Cost.** The summed per-seed time rose from 632 s to 788 s (random) and from 1522 s to 1815 s (defaults). Wall time is not timing-grade here (two Gradle lanes), but the vessel closures are a real added cost (section 4), and the four new default budget holds alone are consistent with it.
- **E6 by final residual** (`h26`, `g35`'s method): random 7 / 10 / 35 (below 1e-6 / to 1e-3 / above), defaults 9 / 10 / 15; `g32` 7 / 11 / 34 and 7 / 10 / 14. G2 does not touch the E6 mechanisms.
- **Seed 298 threw** in `h18` (a `NullPointerException` in the closure after a trace phase was folded; section 10). With the fix it ends on its wall budget alone, as on `g32`.

### 9. Gates on the final code (`h25`, `bf830c78`, GameTest run id `t4-h25-bf830c78`)

| Gate | Result |
|---|---|
| G1 fluid suites | **511 tests in 123 classes, 0 failures** (510 + `VesselStateFollowsContentsTest`); the 33 junction lines are those of `h07` bit for bit (the fold fix moved nothing) |
| G2 exact regression | **fails on the moved reference only**: chain-100 6.276e-15 in moles, 1.705e-13 K, flow 4.093e-11 relative (section 5); not re-recorded |
| G3 adjacent selection | **45/45** |
| G5 fluid GameTests | **all 33 required tests passed** |

GameTest printed values against the last GameTest run (`f27`, group C at `2239a277`; option (a) alone was never GameTested, so these moves are option (a) and G2 together; `h27-gametest-values-f27-vs-h25.diff.txt`):

| Line | `f27` | `h25` |
|---|---|---|
| outlets tankP | 123785.89308958665 Pa | 123785.89308927572 Pa |
| outlets tankMass | 172.50833166386883 kg | 172.50833166163517 kg |
| outlets LIQUID massFlow / moleFlow / head | 29.22852244520977 / 1622.429540102056 / 1680.4694705318925 | 29.22852244429925 / 1622.4295400515146 / 1680.4694705099862 |
| outlets VAPOR massFlow / moleFlow | 0.27227916142055403 / 9.80904418073474 | 0.2722791614202034 / 9.809044180722331 |
| compressor suction | 52659.01872613692 Pa, 247.85430417367346 K | 52659.01872613568 Pa, 247.85430417365745 K |
| compressor discharge | 157976.31408416174 Pa, 338.0420522748146 K | 157976.31408415743 Pa, 338.0420522748114 K |
| compressor ratio | 2.9999859075564457 | 2.999985907556435 |

All within 3e-12 relative. The pump test's tick numbers (1372 -> 1493) and the island ticks of the "property data changed" warnings are server-schedule positions of a run, not solver values.

### 10. The closure's fold check (code `bf830c78`)

The first campaign (`h18`, on `b6f2c072`) threw once, random seed 298: `close` restates the closed vessel with its exact amounts (`repartition`), which applies the trace-phase fold (D34). Here the fold removed a trace liquid, and the full-closure check then encoded the state in the layout of the unfolded state, which expects that liquid (a `NullPointerException` in `PhaseLayout.encode`). The check now uses the closed state's own layout when its phase set changed; a step that folded a phase is solved again from the folded state by the pass loop (`recordFolds`), and the refresh asks its regime test. Every state whose phase set is unchanged is checked exactly as before: the suites' junction lines and `h22`'s clean counts and seed sets equal `h07`'s and `h18`'s. The same pattern was latent in the old refresh (it, too, checked after `repartition` in the pre-fold layout).

### 11. Stop points, with recommendations

1. **Re-baseline (D36), now including G2.** The 24 mixed-gas lines (section 5, `h19`), MIXED_GAS_COST 273 -> 309 and chain-100 (6.3e-15) are roundoff moves; the GameTest values move within 3e-12. Recommended: re-baseline once on the final code when (a) and G2 merge, as D36 says. G2 exact stays red until then.
2. **G2's cost.** The vessel closures roughly double the Newton solve count of the extreme grid at rest (network solves unchanged) and raise the campaigns' summed seed time by 20 to 24 %; four new default budget seeds hold alone (220, 431, 705, 757). A looser closure is not an option: the lag at rest is exactly the grid case. Options: (a) accept the cost; (b) trace the four new budget seeds before the merge to separate cost from a new step failure. Recommended: (b), short.
3. **Random 400 and 737** (clean before, now "Interval substep limit" with every refusal the equation gate, about 520 each). The gate now reads a cushion vessel's true pressure; the polish does not close these points. Recommended: trace one (400) before the merge; it is the one place where G2 turns a clean world into a hold by its own mechanism.
4. **The first solve after a placement** (section 7): its own work item after the merge, starting from seed 168.
5. **The one-way refinement** is left out (A95). A void-only variant was not measured; recommended only if a future E6 trace points at a void again.

### 12. What differs from the brief

- The brief's G2 is "a tank's pressure restated from its booked contents". It needed one more change to pass the suites: the D13 polish starts from the reconstruction (A94, section 4). Without it the vented manometer is refused once by the equation gate.
- The first campaign threw once (seed 298) on a defect of the new closure; it was fixed (`bf830c78`) and the campaign was run again on the final code (`h22`). Both campaigns are kept.
- `PhysicalRegistryTest`'s failure with the one-way refinement does not exist on the target code (G2 present), so it is diagnosed on the side branch's code without G2; the refinement is left out for a different gate failure.
- Commit trailers name the model that wrote them (Claude Opus 5.5), not the brief's "Claude Fable 5.1".
- Another thread's fuzz shared the machine during the campaigns; the wall-dependent budget class was therefore replayed alone, one worker, for every budget seed.

## G2 follow-up: cushion vessels, budget seeds, closure cost (2026-10-12)

- Author: Claude (Opus 5.5, as a thread of the orchestrator), on the side branch `claude/e6-row-scale` from `497fc765`. Code `fa45d02a`, gate test `b4b468b6`. Sub-choices A98-A102 (`DECISION_LOG.md`). Material: `tools/phase-ports-probes/hold-classes/` (`t6.init.gradle`, `src-t6/`), logs `j01`-`j24`. Every comparison is against the side branch at `497fc765` (campaign `h22`, gates `h25`) unless a line says otherwise. No reference was re-recorded.
- Conclusion first. Random seeds 400 and 737 were not cushion holds. G2's closure stated each vessel in full phase equilibrium, while the step solves it in the trace-truncated equilibrium (its frozen phase support). At rest the two differ by 0.71 Pa (seed 400) and 0.032 Pa (seed 737), and the equation gate saw that. The closure now uses the step's own support; both seeds are clean, the suites, the exact references and the GameTests are unchanged from `h25` except chain-100 (roundoff), and the fuzz gives 839 random / 856 defaults. The four new default budget seeds are step failures, not closure cost. No vessel closure can be skipped by construction in the mixed-gas cases.
- The machine also ran another thread's 4-worker fuzz (other worktree) through every run here. Wall times are not timing-grade; budget seeds were replayed alone, one worker.

### 1. Random seed 400: the world and its physics

Minimized with `HoldMinimizerTest` keeping the hold's class and "equation gate" (`j01`): 3 of 17 blocks, the same hold bit for bit (`6.797377359424317E-6`). The other 14 blocks are other islands.

| Block | Position | Setting |
|---|---|---|
| Tank | 0, 2, 0 | 1 m3 nitrogen, 298.15 K, 101.325 kPa |
| Pipe | 0, 2, 1 | 0.05 m |
| Generator | 0, 2, 2 | Tia Juana light crude with methane, 339.3308 K, 220170.990 Pa |

All three are placed at tick 762, at one height. At 339 K and 220 kPa the generator's crude is a single liquid (`liq=1.0`).

**Expectation.** Crude runs into the tank and compresses the nitrogen; its light ends flash into the gas. Flow stops when the tank stands at the generator's 220.17 kPa; nothing ever flows back (a generator only supplies). Measured at the hold, after 26 s: the tank is at rest at 220170.98992 Pa (1.6e-4 Pa under the generator), 338.334 K, 0.2222 m3 of crude liquid under 0.7778 m3 of gas, 190.3 kg. Check: the 40.9 mol of nitrogen alone in 0.778 m3 at 338 K give 148 kPa; the other 72 kPa are hydrocarbon vapour. Gas is 6.7 % of the tank's moles. This is not a small cushion.

### 2. What the solver did (`j02`, `j03`, `src-t6/instr-t6-gate-closure-trace-on-497fc765.patch`)

The hold interval (ticks 1189 to 1289) ran 1024 substep attempts: 502 accepted, 522 refused, every refusal the equation gate. At every refused point:

| Quantity | Value |
|---|---|
| Step Newton at the candidate | converged, 0 to 2 iterations, 2e-12 to 6e-10 |
| The tank's step layout | 35 unknowns; 7 trace components held in one phase (no split unknown, no equilibrium row) |
| The closure's layout (`new PhaseLayout(model,state)`) | 42 unknowns; all 20 components in both phases (truncation OFF) |
| The closure's start residual | 1.6e-11 to 3.8e-10 (above `CLOSURE` = 1e-11, so it solves) |
| What the closure does | one iteration; tank pressure +0.7096 Pa (3.2e-6), temperature -4.2e-5 K |
| The gate at the reconstruction | 6.797e-6 (the tank's ln-fugacity row, local 33), the connection row 6.34e-6; gate 1e-8 |
| The same reconstruction without the closure | 3e-11 to 4e-10 (passes) |
| The D13 polish | converges (2 iterations, 1e-12), but its own reconstruction runs the closure again: 6.797e-6, refused (526 polishes, 4 accepted) |

When the closure's start residual happens to fall under 1e-11 (roundoff), the closure returns the state as it is and the step is accepted. So accepts and refusals alternate at small steps until the 1024-attempt limit.

**The mechanism.** The trace truncation holds a component whose mole fraction in one phase is below the cutoff in the other phase only, with no equilibrium row. That is the model the step and its equation gate solve. The closure, laid out with the default support, solves full equilibrium: the 7 traces also partition into their minor phase. Their share in the other phase carries latent heat and volume, so at the same volume, amounts and energy the full equilibrium stands 0.71 Pa higher. Both are equilibria to within 1e-11; they are two models. The gate then reads the full-equilibrium state in the truncated equations and refuses it, and the polish cannot help, because every reconstruction moves the tank back to full equilibrium.

**Why G2 turned it into a hold.** Before G2 the reconstruction kept the candidate's temperature, pressure and phase split (`repartition`), a point of the truncated model. The interval-start refresh did use the full layout, but restated a vessel only when its lag passed 1e-8, which a resting tank never reaches. G2 runs the closure at every reconstruction with a 1e-11 threshold.

**The cushion hypothesis is rejected.** It said a nearly liquid-full vessel's small gas cushion makes its pressure very sensitive to its booked contents. Seed 400's tank is 78 % gas by volume and 6.7 % by moles. Seed 737's tank is a cushion (1.3 % of moles), but its pressure moves for the same reason (section 3), and the reconstruction without the closure passes in both.

### 3. Random seed 737 (`j04`, `j05`)

Minimized to 5 of 26 blocks, same hold bit for bit (`2.935860880237673E-7`), all placed at tick 151:

| Block | Position | Setting |
|---|---|---|
| Pipe (junction) | 1, 0, 0 | 0.05 m |
| Generator | 1, 0, 1 | crude, 391.741 K, 248773.0 Pa, 0.02 m (two-phase) |
| Generator | 0, 0, 0 | crude, 299.344 K, 326916.1 Pa, 0.01 m (liquid) |
| Pipe | 1, 1, 0 | 0.1 m |
| Tank | 0, 1, 0 | 1 m3 nitrogen, 1 atm |

**Expectation.** The 327 kPa generator fills the junction and, through the pipe one metre up, the tank. The 249 kPa generator's connection closes: the junction stands above it, and a generator only supplies. The tank rests at the generator's pressure less one metre of crude: 860 kg/m3 x 9.81 x 1 m = 8.44 kPa, so 318.48 kPa. Measured at the hold: 318477.29 Pa, 301.14 K, 0.706 m3 of liquid, 0.294 m3 of gas (1.3 % of moles), flows 2.6e-9 kg/s.

**What the solver did.** The same as seed 400, smaller: each closure moves the tank +0.0319 Pa and -2.7e-7 K; the gate reads -2.94e-7 on the tank's ln-fugacity rows and -1.35e-7 on its connection row. Here all 520 polishes are refused; 504 accepted steps reach 1.52 s of 5 s before the attempt limit.

### 4. The fix (`fa45d02a`): the closure solves the step's own equilibrium

Physics forces it. A vessel's state "at what it holds" is defined only together with an equilibrium model. The step solves the trace-truncated one (the support the trace policy freezes per pass, decision D34 and before). The equation gate checks the reconstruction against the step's equations. A closure in another model puts a model difference into the gate. There is no parameter in this choice, and no tolerance or budget is changed.

- `ConservativeTransport.reconstruct` takes the step's phase supports per node (`Equations.supports`, the array each node's layout was built from), and each vessel is closed with its node's support. Both reconstruction sites (the pass loop and the D13 polish) pass them.
- `InventoryEquilibrium.close(model, state, inventory, support, checkpoint)`: the vessel's own layout of `state` under `support`. When `support` is null, or no longer fits `state` (a reconstruction that folded a phase, D34, or restated a phase appearance as its flash; the pass loop solves those again), it uses the support the model's truncation policy gives `state`: what the next solve's first pass lays the vessel out with. The interval-start refresh uses that policy support too (one closure, one model). The fallback layout of a failed refresh Newton and the folded-state check use the same rule.
- What it absorbs: the closure's private equilibrium model. The junctions, the polish and the gate are unchanged.

**Measured.**

| Item | `497fc765` | `fa45d02a` |
|---|---|---|
| Seed 400 minimized world, 40 slices (`j06`) | held at tick 1289 | clean |
| Seed 737 minimized world, 40 slices (`j06`) | held at tick 978 | clean |
| Seeds 400 and 737 alone, 120 slices (`j07`) | E8, 1678 / 3808 ms | clean, 363 / 261 ms |
| Gate test `VesselClosureEquilibriumTest` (both worlds; `j09`, `j10`) | fails (held at ticks 1289 and 978) | passes |
| G1 fluid suites (`j08`, `j23`) | 511 / 0 failures | 513 / 0 failures (511 + 2 new) |
| 33 junction lines (`j23` against `h25`) | | identical bit for bit |
| MIXED_GAS_COST | 309 | 309 |
| G3 adjacent | 45/45 | 45/45 |
| G5 GameTests (`j23`, run id `t6-j23-b4b468b6`) | 33/33 | 33/33, every printed value identical to `h25` |

**Moved reference: chain-100 only** (exact regression, against the reference recorded before G2; nothing re-recorded):

| chain-100 deviation | `h25` (G2) | `j23` (G2 + fix) |
|---|---|---|
| moles / mass (relative) | 6.276e-15 (node 18, moles[17] 2.61813592313263 -> 2.6181359231326136) | 6.049e-15 (node 27, mass 38.765655832609795 -> 38.76565583261003) |
| temperature | 1.705e-13 K (node 15) | 1.705e-13 K (node 22, 350.01920083234904 -> 350.0192008323492) |
| phase fraction | 3.331e-16 | 3.331e-16 |
| flow (relative) | 4.093e-11 (pipe 4, -4.217556210219395E-4 -> -4.217556210046756E-4) | 1.896e-11 (pipe 4, -> -4.2175562101394345E-4) |

The chain-100 world carries crude traces, so its closures now run in the truncated model. Both are roundoff against the pre-G2 reference; the re-baseline of D36 takes the final code's values.

### 5. The four new default budget seeds (task 2; `j11`-`j15`, `j19`)

Each replayed alone, one worker, with the budget lifted (`holdFuzz.wallNanos` 1e13), and the per-slice counters of `T6CushionProbeTest` (closure counters from the instrumentation patch).

| Seed (blocks) | Without G2 (`7395e984`, `j13`) | With G2 (`497fc765`, `j12`; the fix gives the same counters, `j11`) | Closure Newtons in the run (iterations) / in the worst slice | Alone with the budget, on the fix (`j19`) |
|---|---|---|---|---|
| 220 (24) | C at tick 2236 (water at the 273.16 K floor); 543 solves; worst slice 1107 ms, 330 attempts | C at tick 2236, same reason; 1721 solves; worst slice 2.3-2.4 s, 1024 attempts | 19 (19) / 18 | F |
| 431 (25) | C at tick 1846 (6.56 MPa, domain); 916 solves; worst 1259 ms | E6 at tick 2110 (iteration limit at 0.30); 4578 solves; worst 3.6-3.8 s, 855 attempts | 47 (50) / 0 | F |
| 705 (21) | E4 at tick 1752 (gate 1.47e-5, 49 refusals); 165 solves; worst 138 ms | E8 at tick 1752 (518 gate refusals, 6.36e-7); 2160 solves; worst 2.1-2.2 s, 1024 attempts | 70 (70) / 68 | F |
| 757 (29) | clean; 787 solves | E8 at tick 2894 (iteration limit 340, no finite Jacobian 50, domain 10); 2225 solves; worst 1.7 s, 1024 attempts | 12 (12) / 0 | E8 |

- **Not G2's closure cost.** Every closure Newton takes one iteration, and a seed runs 12 to 70 of them beside 1700 to 4600 network solves. The slices that spend the budget are step failures that run to the substep limit (1024 attempts) at 2 to 4 ms per attempt.
- **What G2 did to them.** It moved each trajectory at roundoff, and the failing slice now runs longer or ends in another class. Seed 220 fails on the same water-floor wait at the same tick, but only after 1024 attempts instead of 330. 431, 705 and 757 end in step classes (E6, E8, E8). The fix changes none of them (identical counters).
- **Their classes without the budget:** 220 C, 431 E6, 705 E8, 757 E8.

**The smallest, defaults seed 705** (minimized to 11 blocks, `j14`; traced, `j15`). A 1 atm, 298.15 K water generator feeds a junction joining two 1 m3 nitrogen tanks at its height. A third tank, two metres up, is joined from the top (gas) port of one of them to its own bottom (liquid) port.
- Expectation: everything starts at 1 atm, so no pressure drives water anywhere. Arriving water evaporates into the dry nitrogen, cools it and lowers its pressure, and water flows in to hold 1 atm. Measured at the hold: lower tanks at 291.6 K (6.6 K down), 1.1 L of water each, inflow 0.05 to 0.09 kg/s. They cool on towards the gas's adiabatic saturation temperature (about 283 K for dry gas at 298 K, estimate), take in a few litres more and rest at 1 atm. The upper tank exchanges nitrogen only and sits 22.8 Pa lower (2 m of gas).
- What the solver did: 518 gate refusals. The refused row is the water junction's water-vapour equilibrium row (2.1e-5 to 7.9e-3); the junction holds a nitrogen bubble of 4.3e-6 of its volume, 3.2e-9 of its moles. The vessel closure is not involved: the same reconstruction without it gives the same value. This is the trace-bubble family (cards E4/E5, A84), as it was before G2 (E4 at the same tick).

### 6. The cost of G2's closures (task 3; `j16`, `j17`)

The twelve `MixedGasJunctionTransientTest` cases at 5 s (MIXED_GAS_COST), every closure logged:

| Closure calls | Count | Newton solves (iterations each) |
|---|---|---|
| At reconstructions | 490 (245 per tank) | 34 (1 each): tank 1 (the 150 kPa methane tank) 24, tank 2 (nitrogen) 10 |
| At interval-start refreshes | 96 | 0 |
| By interval: 0-5 s / 5-10 s / 10-13 s (injection) / 13-16 s | | 14 / 17 / 3 / 0 |
| Lag each Newton closes | | 1.0e-11 to 2.1e-10; pressure moves 9e-8 to 7.2e-6 Pa, temperature 2e-11 to 2e-8 K |

- Every call, solved or not, is on a tank whose booked inventory changed in its step (a flow was booked). The closure is a function of its state and its inventory, so it can be skipped by construction only when both are bitwise what the last closure produced: no flow booked and an unchanged state. That never happens here: a resting interval takes the rest certificate and never reaches a reconstruction. The 552 calls that do not solve already cost no Newton.
- So there is no by-construction skip to take, and nothing was changed. MIXED_GAS_COST stays 309 (275 network solves + 34 closures); the junction lines stay bitwise. A tolerance skip (a looser `CLOSURE`) is not allowed and was not measured.

### 7. Campaign (`j18`, 1000 seeds per distribution, 8 workers, `holdFuzz.pipeBetween`; against `h22`)

| Distribution | `h22` | `j18` | Cleared | Worse (replayed alone, `j19`) |
|---|---|---|---|---|
| random | 837 | **839** | 3: 244 (F in `h22`, clean alone already), 400, 737 | 1: 274 (E2, holds alone) |
| world defaults | 856 | **856** | 0 | 0 |

- Class changes (`j24-transitions-*.txt`). Random: 400, 737 E8 -> clean; 244 F -> clean; 274 clean -> E2; 633 C -> E6; 786 E8 -> E2; 19, 23 F -> C (both C alone in `h24`). Defaults: seed 1 E8 -> F in the campaign (E8 alone). Counts random E8 7 -> 4, F 11 -> 8, E2 18 -> 20, E6 52 -> 53, C 45 -> 46; defaults E8 11 -> 10, F 11 -> 12. The default worlds hold only water and nitrogen, which have no truncated phase support, so the fix does not touch them.
- **Budget seeds alone** (`j19`, one worker). Random: 116 118 298 922 hold on their budget (4, as `h24`); 4 C, 92 E8, 311 E2, 431 E6. Defaults: 71 72 89 113 220 431 705 833 911 994 (10; `h22` 11); 1 and 757 are E8 alone.
- **Random seed 274** returns to its `f28` hold exactly (tick 2619, "Substep refinement exhausted: Singular Newton Jacobian; active-set pass=0"); option (a) with the full-equilibrium closure had cleared it (`g32`, `h22`). Minimized to 9 blocks (`j20`): a 359.5 K, 261.6 kPa liquid crude generator fills a 1 m3 nitrogen tank through a pipe run with a loop (two parallel connections between two junctions) and an inline filter on a dead end. Expectation: the tank fills to 261.6 kPa and rests (0.202 m3 liquid, 357.9 K); nothing flows. At the hold (`j22`) the island is at rest within 2.3e-5 Pa, the loop's flows are 1e-18 to 1e-22 kg/s, and every step size ends on a singular Jacobian at pass 0. The same minimized world is clean on `497fc765` (`j21`). Cause unconfirmed, not traced: with the full-equilibrium closure the tank moved by a fraction of a pascal at closures, so the loop never sat at exactly zero flow; with the step's own equilibrium it does. It belongs to card E2 (equations that cannot be told apart).

### 8. Stop points, with recommendations

1. **The side branch.** Recommended: merge as it stands (with `fa45d02a`), then re-baseline once (D36): the 24 mixed-gas lines and MIXED_GAS_COST 309 are those of `h25` bit for bit; chain-100 takes its new roundoff values (section 4).
2. **Random seed 274** (E2, zero-flow loop at rest). Recommended: trace it with the E2 card after the merge; it is the class's mechanism, not the closure's.
3. **Defaults seed 705** (gate refusals of a nitrogen bubble in a water junction) belongs with cards E4/E5 (trace bubbles, A84).
4. **G2's step-failure cost** (seed 220: the same domain wait after 1024 attempts instead of 330). No change proposed; the budget seeds follow their step classes.

### 9. What differs from the brief

- The "cushion pressure" hypothesis does not hold: both seeds hold on a model mismatch between the closure and the step (section 2).
- Smallest worlds were found with `HoldMinimizerTest.minimize` (class and "equation gate" kept, so the mechanism stays), not `BoundarySplitProbeTest.minimizeForCards`, which keeps the class only.
- Commit trailers name the model that wrote them (Claude Opus 5.5), not the brief's "Claude Fable 5.1".
- Docs were not copied to the main checkout (own worktree only).
## Hold class E1, R1 on the E6 rule (2026-10-12)

- Numbering: this section and the next were written on `claude/e1-e4-measure` with rows A100-A106. At the merge onto `claude/phase-ports-compressor` (section "Integration of R1 and the junction start rule (2026-10-12)") they were renumbered A103-A109 in the same order; the text below uses the new numbers, the logs keep the old ones.
- Author: Claude (Opus 5.5, as a thread of the orchestrator), worktree `e1-e4-measure`, branch `claude/e1-e4-measure` from `7395e984` (the side branch `claude/e6-row-scale` with E6 option (a)). Code `54c15462`, test `6b60ae0c`. Owner decision D37; sub-choices A103-A104 (`DECISION_LOG.md`). Material: `tools/phase-ports-probes/hold-classes/` logs `i01`-`i11`; the fuzz now prints the solver's counters (`holdFuzz.counters`, `campaign.sh` with `THREADS=4`).
- Every comparison is against this branch (`7395e984`): the 33 junction lines of `g48` (the 24 mixed-gas lines option (a) moved), campaign `g32`. No reference was re-recorded.
- Load: this thread ran on the second Gradle lane with 4 workers while another thread ran 8-worker campaigns. Cost is judged by counters; every budget hold that moved was replayed alone.
- Baseline at 4 workers (`i03`, `7395e984` unchanged): 833 random / 853 defaults, **identical to `g32` seed by seed and class by class**. It gives `g32` the solve counters it never recorded.

### 1. Physics

An island with an inline filter checks at the start of every interval whether a filter or a connection must close: one rate solve of the network with the tanks held at their pressures, read by the solid closure rules. The check only pre-empts a closure that the steps would find anyway, at t0 or inside the interval, through their own guards. On an island's first interval its junctions hold the mint, a guessed state, and the rate solve holds their compositions fixed. So a rate solve that fails there says nothing about the island: no filter is full and no bed has settled because of it. Expected effect of R1 (a failed check declares no closure): the worlds whose only failure was the check run on; worlds whose steps fail the same way now fail in their steps (E6, E2) instead of in the check (E1, D); no world that was clean can get worse, except through budget seeds, which spend their time differently.

### 2. What is implemented (`54c15462`)

- `PassiveStepSolver.portRate` is the one rate solve of the port graph (tanks held as ports, dt = 1). It returns null when the island has no connection or the solve fails (Newton failure, a refused argument or a property-domain refusal, an approximation refusal). Its javadoc states R1's reason.
- Both start-of-interval checks read it: the interval solver's t0 guard and cold rate seed (`PassiveIntervalSolver`, which already skipped a failed rate solve) and the solid closure check (`SolidEventIntegrator`, which used to hold the island). The second copy of the port-graph rate solve (`SolidEventIntegrator.rate`) is gone. Each keeps its own solver instance, so no warm start moves.
- Not done (observed): the solid closure loop and the interval solver's own t0 guard now apply the same rule twice at an interval's start. Folding the loop into the guard would change when carried flows are used instead of a fresh rate solve (the guard uses the carried endpoint while no closure changed; the loop reconsiders every closure from scratch), so it moves filter-island paths. Not done; noted for a later cleanup.

### 3. References (`i04`)

| Gate | `g48` (branch) | R1 |
|---|---|---|
| Fluid suites | 510 tests, 2 failures (`ExtremeTopologyIslandTest` GRID) | 510 tests, the same 2 failures with the same values (tank 31 at 200000.0002114231 Pa and 164000.00017145998 Pa) |
| 33 junction lines, MIXED_GAS_COST | 24 lines moved by option (a), 273 | **identical** to `g48`, 273 |
| chain-100 exact regression | 0.000e+00 | 0.000e+00 |
| Adjacent selection | 45/45 | 45/45 |
| Every printed line of the suites (timings masked) | | **identical** (5991 lines) |

No reference moved: no suite world runs a failing start check.

### 4. Campaign (`i05`, 1000 seeds per distribution, 4 workers, pipe-between)

| Distribution | Clean `g32` = `i03` | Clean with R1 | Cleared | Worse |
|---|---|---|---|---|
| random | 833 | **835** | 2: 6 (E1), 866 (E1) | 0 |
| world defaults | 853 | **856** (855 in the campaign; 915 clean alone) | 3: 524 (D), 842 (E1), 902 (D) | 0 (915 held in the campaign on "wall deadline" and runs clean alone, `i06`) |

Class counts (random / defaults): E1 12 -> **0** / 9 -> **0**; D 1 -> **0** / 5 -> **0**; G 0 / 0 throughout. On `bbb79790` R1 cleared 25 / 19 seeds (`r18`); on the E6 rule it clears 2 / 3: the row scale removed most of what R1 used to clear (the E1 card world, defaults seed 45, already runs clean on `7395e984`, `i01`).

**Where the former E1 and D worlds fail now** (first hold, `i03` -> `i05`; budget seeds replayed alone, `i06`):

| Distribution | Seed: class before -> after |
|---|---|
| random | 6 E1 -> clean; 866 E1 -> clean; 45, 65, 159, 485, 499, 563, 945 E1 -> E6; 206, 236 E1 -> C; 407 E1 -> E2; 348 D -> E2 |
| world defaults | 842 E1 -> clean; 524, 902 D -> clean; 175, 788 E1 -> E6; 475 E1 -> E2; 541, 895 E1 -> E4; 717 E1 -> E5; 214, 563 E1 -> F (held alone too); 15, 499, 727 D -> E2 |

Other moves: random 788 C -> F (held alone too: its check used to stop on a nitrogen trial at 2 Pa, a typed wait; the steps now spend the budget on the same island), defaults 792 E8 -> F in the campaign (E8 alone, as before). The worlds that fail in their steps were not clean before and are not clean now; R1 shows what they actually fail on.

**Solve counters** (whole campaign, all seeds; the counters of `g32` are `i03`'s):

| Counter | random `i03` | random R1 | change | defaults `i03` | defaults R1 | change |
|---|---|---|---|---|---|---|
| implicit (step and rate) solves | 253 822 | 257 131 | +1.3 % | 864 744 | 1 004 355 | +16.1 % |
| Newton solves | 455 763 | 464 958 | +2.0 % | 1 690 392 | 1 954 174 | +15.6 % |
| Newton iterations | 4 320 540 | 4 433 252 | +2.6 % | 17 002 245 | 20 462 299 | +20.4 % |
| Jacobian builds | 1 888 462 | 1 948 462 | +3.2 % | 4 643 392 | 5 538 335 | +19.3 % |
| step attempts (accepted) | 242 763 (132 028) | 245 558 (132 625) | +1.2 % | 858 492 (373 330) | 995 906 (419 798) | +16.0 % |
| equation-gate refusals | 18 523 | 18 523 | 0 | 41 887 | 42 622 | +1.8 % |

The defaults growth is the worlds that used to stop at the check and now run their steps to a hold: the two new budget seeds alone take 45 s (214) and 424 s (563) of solving on one thread, against well under a second each when the check held them. No cost is added to an island that does not fail its check (every printed line of the suites is identical).

### 5. Gate test (`StartCheckRateFailureTest`, `6b60ae0c`)

The E1 card world (defaults seed 45, nine blocks) runs clean on this branch without R1 (`i01`), so it cannot guard R1. Of the seeds R1 clears, defaults 842 minimized to 13 blocks still holds with R1, in its step (E6 at the same residual, `i07`, `i08`): its minimized world keeps the failure but not the context that let the full seed run. Random 866 minimized with R1 reverted keeps E1 (13 blocks, `i07`) and runs clean with R1 (`i08`); its placements with the seed's exact doubles are in `i09`.

| Test | World | With R1 (`i10`) | R1 reverted, working tree only (`i11`) |
|---|---|---|---|
| `StartCheckRateFailureTest.aFailedStartCheckOfAFilterIslandLetsTheStepsDecide` | random 866 minimized: a 348 kPa methane generator two blocks up feeds a junction whose only exits are a 120 kPa back-pressure valve held shut by a 1 atm void and a filter line to an 88 kPa methane generator; nothing can flow, the junctions fill to 348 kPa and rest | pass: the first interval commits, 120 slices kept up | holds at tick 290, "Newton iteration limit at residual 0.0230939954292403; active-set pass=0" (the check), every later slice the same |

Under the junction start rule of the next section this world starts differently and passes without R1 (`i32`); a second world (random 122) guards R1 on the final code (next section, section 7; A109).

### 6. What differs from the brief

- The gate world is random seed 866, not the E1 card world (defaults seed 45), which the E6 rule already cleared (section 5).
- Commit trailers name the model that wrote them (Claude Opus 5.5), not "Claude Fable 5.1".

## Hold class E4, the junction start from reachability (2026-10-12)

- Author: Claude (Opus 5.5, as a thread of the orchestrator), same worktree and branch as the E1 section above, on top of R1. Code `ad43b3ae` (first form), `e43d3fdf` (corrections), `bb1f2b82` (equal starts to the denser fluid); tests `ae3a008a` (E4 gate) and `98c3c754` (the R1 guard rebuilt). Owner decision D37 (measured before a decision); sub-choices A105-A109. Material: `hold-classes/` logs `i12`-`i44`, `src/instr-mint-print.patch`, `src/scratch-start-strongest.patch`.
- Every comparison is against R1 on this branch (`i05`, with defaults 915 clean alone) and against `g32`. No reference was re-recorded.

### 1. Physics (card E4, defaults seed 6)

A pump stands between a nitrogen tank and a void, both at 101.3 kPa and level with the pipes; a 1 atm water generator stands three blocks below. The generator cannot lift water 3 m against 101.3 kPa of gas (it would need about 131 kPa). The pump's suction sees only the tank's nitrogen, and a liquid pump refuses gas (D1). So nothing can move and the world rests from its first tick, every junction holding the tank's nitrogen at 101.3 kPa. The old mint started every junction as the island's first boundary by identity, here the generator: water at 101.3 kPa at every height, a state no flow could produce. The equation gate then refused every step (3.46).

The general statement: a junction's start is what the boundaries can drive into it. A generator only supplies and a void only receives (D21). A fluid enters a junction only if it stands there above the pressure the junction can drain into: a liquid cannot rise above its own head over that pressure. When several boundaries can, the one whose fluid arrives first (the nearest) fills it, except where physics names the winner: a pump or compressor drives its stream into its discharge and pushes undriven fluid back, and a pocket that drains nowhere fills to the strongest supplier while weaker ones stay shut (D12). A junction nothing can drive into holds what is in contact with it.

### 2. The rule, stated precisely (`NetworkReduction.junctionStarts`, step 7 of the reduction stage)

| Question | Answer as implemented | Sub-choice |
|---|---|---|
| Who can supply | a generator; a tank that holds fluid. A void never (D21); an evacuated tank takes no part | A105 |
| What it sends | the stream of the end the walk leaves by: the phase its port draws first, flashed alone at the tank's T and P; the whole state at a BULK end or when the tank holds one phase; at the end pressure (a liquid port with its level head); no solids | A105 |
| Can reach | a walk through junctions in the directions the connections allow (one-way rule, blocked directions). The stream enters a junction only while its static pressure there (end pressure less its own column) is **strictly above** the lowest pressure the junction can drain into: the end pressure of a void or tank other than the source, reached along the same rules, raised to a valve's set point on the way, unbounded below through a pump or compressor. Strict, as step 5 calls a direction driven (no band). The drain side's column is not counted: at the start the run holds what is being displaced (an unprimed hump stays dry) | A105, A107 |
| Devices | a valve passes a stream forward while it stands above the set point; a pump passes forward a stream with no gas, a compressor a gas-only stream (D1); past a pump or compressor the stream is driven and stands above every bound | A107 |
| Nearest | the shortest pipe length from the boundary's end (sum of the runs' lengths) | A105 |
| Two can reach | a driven stream first; then the nearest; equal lengths: the stream standing higher, then the denser stream, then the lower boundary identity, then the lower connection identity | A107, A108 |
| A pocket (drains nowhere) | no bound; the supplier standing highest fills it (the weaker ones stay shut, D12); the length breaks ties | A107 |
| Nothing reaches | the boundary in contact with it (through passive connections, either way) whose fluid stands highest at its height, voids included, then the denser fluid; any boundary of the island when none is in contact | A107, A108 |
| Only a void "reaches" | a void never supplies, so this is "nothing reaches": the junction holds the void's fluid if the void is what stands highest in contact with it | A107 |
| Pressure of the start | the boundary's own (no hydrostatic restatement, card E9's (a) is separate); the holdup is sized from the chosen state (D25's sizing) | A105 |
| Where it acts | where placed blocks form an island (`PhysicalRegistry.apply`, `PassiveNetwork.mintJunctions`); a graph built directly keeps its junction states | A106 |

The first form (`ad43b3ae`, campaign `i15`) entered at equality, gave a junction that drains only into the source an infinite bound instead of none, never crossed a device, and chose by length in pockets. The fuzz worlds showed each of these against the physics stated above (A107: defaults 718, `InlineFilterLineHoldTest` GPMPFPT, random 866); `e43d3fdf` corrects them. `bb1f2b82` adds the denser fluid as the tie-break before identity (A108).

**Card E4 world (defaults seed 6).** It already ran clean on this branch before the rule (`i02`: the E6 rule cleared it). With the rule its three junctions start as the tank's nitrogen at 101.3 kPa instead of the generator's water (`i14`, `-Pinstr=mint`), which is the physical start.

### 3. References (`i44`, on `98c3c754`)

| Gate | `g48` (branch) | R1 + start rule |
|---|---|---|
| Fluid suites | 510 tests, 2 failures (GRID) | 513 tests (+3 gate tests), the same 2 GRID failures with the same values |
| 33 junction lines, MIXED_GAS_COST | 24 lines moved by option (a), 273 | **identical** to `g48`, 273 |
| chain-100 exact regression | 0.000e+00 | 0.000e+00 |
| Adjacent selection | 45/45 | 45/45 |
| Every printed line of the suites (timings masked) | | **identical** (5991 lines) |

No reference moved. The junction-line fixtures and the exact regression build their graphs directly (compile, then `sizeJunctionHoldups`), so the rule, which acts where placed blocks form an island (A106), does not reach them; with the rule inside the sizing (`i12`, the first placement) the 33 lines were identical too, because both tanks of each mixed-gas fixture stand level and the tie went to the same tank. The brief expected the junction lines to move; they do not.

With the rule inside the sizing (`i12`), six fixtures that state a junction's start on purpose failed: `NetworkRegimeTest` x2 (a nitrogen junction fed methane, for the holdup lag), `JunctionWaterTraceTest` x2 (a dry junction, D10), `PassiveStepSolverTest` (an isolated junction keeps its pressure), `DeadHeadedLineIslandTest` (a dead-headed filter line). Their premise is the junction state they give; the rule now acts at formation only (A106) and they pass unchanged.

### 4. Campaigns (1000 seeds per distribution, 4 workers, pipe-between; every worse seed replayed alone, all hold alone in the same class: `i16`, `i24`, `i40`, `i41`)

Clean seeds:

| Campaign | random | world defaults |
|---|---|---|
| `g32` (E6 rule, 8 workers) = `i03` (4 workers) | 833 | 853 |
| `i05` R1 | 835 | 856 (915 clean alone) |
| `i15` R1 + first form of the rule (superseded) | 886 | 858 |
| `i23` R1 + corrected rule, ties by identity (c1) | 884 | 866 |
| **`i26` R1 + committed rule, ties to the denser fluid (c1')** | **884** | **866** |
| `i33` committed rule without R1 | 881 | 862 |
| `i39` committed rule with variant c2 (strongest first everywhere, scratch) | **893** | **881** |

Against R1 (`i05`, 915 counted clean), with the committed rule (`i26`):

| Distribution | Cleared (class under R1) | Worse (class with the rule) |
|---|---|---|
| random | 73: C 18, E6 27, E4 11 (199 219 357 394 432 438 468 470 714 734 815), E2 10, E10 2, E7 2, E3 1, E5 1, F 1 | 24: E6 15 (17 48 55 71 244 273 274 282 408 699 727 736 768 916 981), C 5 (15 286 453 629 757), E2 2 (561 839), E10 1 (333), E4 1 (669) |
| world defaults | 42: E4 12 (325 354 394 412 419 438 526 529 541 561 750 859), E6 8, E2 5, F 4 (89 113 214 911), C 3, E10 2, E5 2, E7 2, E8 2 (1 589), E9 2 | 32: E6 14 (39 226 444 459 606 616 651 681 757 779 780 794 967 992), E4 7 (247 320 517 654 718 892 919), E2 4 (29 94 333 908), E3 2 (224 505), E5 2 (316 698), E9 2 (9 19), F 1 (1000, a budget hold alone too) |

Against `g32`: random 833 -> 884 (+51), defaults 853 -> 866 (+13).

Class counts (random / defaults), R1 -> committed rule: C 48 -> 33 / 14 -> 4; E6 59 -> 46 / 33 -> 43; E2 20 -> 11 / 21 -> 21; **E4 12 -> 5 / 23 -> 23**; E5 3 -> 4 / 15 -> 11; E7 6 -> 1 / 5 -> 2; E8 3 -> 5 / 7 -> 8; E9 3 -> 4 / 7 -> 6; E10 4 -> 3 / 5 -> 3; E3 1 -> 1 / 3 -> 3; F 6 -> 3 / 11 -> 10; A 0 / 1 -> 0; E1, D, G 0.

**R1 on top of the rule** (`i33` against `i26`): without R1 the class E1 comes back (10 random / 7 defaults) and D (2 / 3); R1 clears random 122, 175, 627 and defaults 214, 348, 842, 886 on top of the rule (+3 / +4), none worse. R1 keeps its value under the rule.

**Solve counters** (whole campaign):

| Counter | random R1 | random rule (c1') | change | defaults R1 | defaults rule (c1') | change |
|---|---|---|---|---|---|---|
| implicit solves | 257 131 | 326 928 | +27 % | 1 004 355 | 646 705 | -36 % |
| Newton solves | 464 958 | 485 404 | +4 % | 1 954 174 | 1 449 517 | -26 % |
| Newton iterations | 4 433 252 | 1 630 810 | -63 % | 20 462 299 | 13 780 772 | -33 % |
| Jacobian builds | 1 948 462 | 580 731 | -70 % | 5 538 335 | 2 868 147 | -48 % |
| step attempts (accepted) | 245 558 (132 625) | 312 596 (176 550) | +27 % | 995 906 (419 798) | 639 015 (245 148) | -36 % |
| equation-gate refusals | 18 523 | 87 088 | +370 % | 42 622 | 59 675 | +40 % |

Random worlds run more steps (49 more seeds keep up to the end) at far fewer Newton iterations per step: starts no flow can produce no longer cost twenty iterations each. The equation-gate refusals grow where a junction now starts as one fluid and meets another at zero drive (the E4 trace family, below). Variant c2 (`i39`): random 522 113 Newton solves, 1 544 423 iterations; defaults 1 455 432, 15 879 772.

**A traced worse world** (defaults 320, minimized under the rule to 6 blocks, `i42`, `i43`): a level 1 atm water generator and a void meet at one junction; a pump facing that junction draws from a pipe to a second void. Nothing can flow (the second void cannot supply; generator and void stand equal). The rule starts the junction beside the generator as water (generator and void stand equal there and water is denser) and the pump's suction pocket as nitrogen (the void in contact); the pump refuses the gas (D1). The island holds at once on the equation gate (3.81e-6, every step size): a water junction at zero drive beside gas, the E4 trace family of the remaining cards, reached from a start that is physical. Before the rule both junctions started as nitrogen and the world rested.

### 5. Variant c2 and the other choices physics does not force (stop items)

| Choice | Committed (c1') | Alternative | Measured |
|---|---|---|---|
| S1. Which reaching supplier starts a junction that drains somewhere | the nearest (the owner's wording) | c2: the one standing highest there; the length breaks ties | c2 893 / 881 against 884 / 866; against c1' random 23 cleared / 14 worse, defaults 18 / 3 (all hold alone, `i41`). c2 is also the physically sure one: the supplier standing highest is always driven into a junction if any is; a nearer, weaker one may be pushed back |
| S2. The drain side's column | not counted (unprimed: a hump stays dry; a level source does not spill downhill) | counted in the arriving stream's density (primed: a siphon starts; a level source spills) | not measured |
| S3. Ties of standing pressure | the denser fluid, then identity (A108) | identity only | equal counts (5 / 5 swapped in defaults); the denser rule clears the card E6 world |
| S4. Where it acts | where placed blocks form an island (A106) | every unsized junction, fixtures included | the latter breaks six fixtures' premises (`i12`) |

**Recommendation:** take the rule with S1 = c2 (strongest first), S2 unprimed, S3 denser, S4 at formation; then re-baseline nothing (no reference moved) and rebuild the two E6 gate worlds (section 7).

### 6. Special cases (D10, A92)

- **D10 (a water entry trace in a dry junction) stays.** The rule decides a junction's start only; a junction started as gas can still receive water later (a tank draining, a generator raised, the water arriving after a topology event), and graphs built directly (`JunctionWaterTraceTest`) state dry junctions on purpose.
- **A92 (the minted dry-nitrogen junction meeting water, card E6 world, defaults seed 1) is absorbed for its world.** The junction now starts as water (the generator and the tank stand level and equal there; the denser fluid settles), and the world runs its 120 slices clean (`i27`); the full seed 1 is cleared. Class E8 elsewhere: 7 -> 8 defaults, 3 -> 5 random (no common mechanism traced).

### 7. Gate tests and their proofs

| Test | World | With | Reverted in the working tree only |
|---|---|---|---|
| `JunctionStartStateTest.aJunctionOnlyWaterReachesStartsAsWater` (`ae3a008a`) | defaults 438 minimized with the rule reverted (`i17`, 6 blocks): a level 1 atm water generator into a junction with a void below and one beside; water spills into the lower void (6.3 kg/s at rest, `i18`) | pass, 120 slices (`i30`) | the whole rule reverted (`ad43b3ae`..`bb1f2b82`): holds at tick 573, "Conservative reconstruction fails equation gate: 1.161230496886477E-4" at every step size (`i31`), the full seed's status |
| `StartCheckRateFailureTest.aFailedStartCheckUnderTheJunctionStartRuleLetsTheStepsDecide` (`98c3c754`) | random 122 minimized with R1 reverted and the rule on (`i34`, 9 blocks): a 273 kPa nitrogen generator into a void through a 0.01 m pipe, a filter branch to a 91 kPa generator that stays shut; 0.023 kg/s at the cap (`i35`) | pass (`i37`) | R1 reverted: holds at tick 218, "Newton iteration limit at residual 2.514065579647154; active-set pass=0" (`i38`) |

The E4 card world is not a gate world: it ran clean before the rule (`i02`).

**Gate worlds the rule empties.** (1) The first R1 world (random 866) passes without R1 under the rule (`i32`): its junctions start differently; the second test above guards R1. (2) Both worlds of `ConnectionRowScaleTest` (E6) pass with the donor scale restored under the rule (`i28`); with the rule also reverted they hold as recorded (ticks 1084 and 8257, `i29`). The rule removes the nitrogen-against-water zero-drive start both worlds were built on. If the rule is taken, the E6 gate worlds need rebuilding (an E6 seed minimized with the donor scale on the final code), as A79 asks; not done here (E6 is the other thread's item).

### 8. Process notes

- The first run of the `i15` campaign (discarded, logs deleted) ran on a working tree where removing my instrumentation lines had also deleted one line of `PhysicalRegistry.apply` (`replacementMembers.put`): 728 random seeds threw "Topology event reached ... outside the islands it fenced". The line was restored and `i15` re-run; the source files have been compared with the commits before every later run.
- A c2 campaign started on c1 was stopped before it finished (its worker JVMs ended by process id; no other thread's process was touched) and re-run on c1' as `i39`.
- Load: as in the E1 section (the other thread's 8-worker campaigns ran beside these). Cost by counters; worse seeds replayed alone.

### 9. What differs from the brief

- The E4 card world (defaults seed 6) was already clean on this branch, so the gate world is defaults seed 438 (section 7).
- The junction lines do not move (section 3).
- "Nearest" is committed as the owner worded it, but the measurement recommends "strongest first" (S1); the corrections of A107 (strict drive, devices, pockets) were needed for the rule to keep gate tests passing, and each rests on a rule the code already states (step 5, D1, D12).
- The rule empties three existing gate worlds (section 7); one was rebuilt (R1), two are left for the E6 work.

## Integration of R1 and the junction start rule (2026-10-12)

- Author: Claude (Opus 5.5, thread t7 of the orchestrator), worktree `remove-agents-attribution-line-3d0c97`, branch `claude/phase-ports-compressor`. Merge `62e6b790`, gate test `96d9be65`. Owner decision D39; sub-choices A110-A115 (`DECISION_LOG.md`). Material: `tools/phase-ports-probes/hold-classes/` logs `k01`-`k22`, `src-t7/`, `t7.init.gradle`, `t7-probes.sh`, `alone.sh`, `printed-lines.sh`, `transitions.js`, `card-lines.js`, `cards-t7.js`, patches `src/scratch-t7-donor-scale.patch`, `src/scratch-start-strongest.patch` (t5), `src/scratch-t7-start-mixture.patch`.
- Conclusion first: the merged code keeps every reference of this branch and gives **887 random / 869 defaults** (`j18` 839 / 856, `i26` 884 / 866). The E6 gate worlds are rebuilt and guard option (a) again. Of the three junction start forms, "strongest first" gives 895 / 885 and "mixture" 889 / 874; the physics says neither nearest nor the free-drain mixture is the steady state where the drain backs up (section 4). No decision file was written, so nearest stays committed and the two others stay as patches.

### 1. The merge (`62e6b790`)

`git merge --no-ff claude/e1-e4-measure`. The code merged without conflict (`PassiveStepSolver` took both `portRate` and the G2 changes). Five documents conflicted; both sides were kept:

| File | Resolution |
|---|---|
| `CHANGELOG.md`, `documentation/INDEX.md` | one line each, edited on both sides: this branch's line kept, t5's sentence appended |
| `PHASE_PORTS_REVIEW.md` | the two G2 sections, then t5's E1 and E4 sections (with a numbering note) |
| `DECISION_LOG.md` | rows in order D35, D36, D37, D38 and A88-A109: t5's section placed after the G2 follow-up, D38 moved into its own section "The side branch merge" after it, t5's A100-A106 renumbered A103-A109 (t5's A103, the fixtures row, is now A106 and sits in numeric order) |
| `tools/.../hold-classes/README.md` | both rows kept, t5's renumbered |

The renumbering touched only t5's text (review sections, decision rows, tools README); `REMAINING_HOLDS.md` merged cleanly and carried no t5 A-row. Logs keep the numbers they were written with.

**Suites on the merge (`k01`), against `j23` (this branch):** 516 tests, 0 failures (513 + t5's three gate tests). The 33 junction lines, MIXED_GAS_COST 309 and chain-100 (6.049e-15) are identical to `j23`. All 6072 printed suite lines are identical, with wall times masked, the reason maps sorted and the XML headers left out (`printed-lines.sh`).

### 2. Campaign on the merged code (`k02`, 8 workers; worse seeds and budget holds alone, `k03`)

| Campaign | random | defaults | Newton iterations random / defaults | Newton solves random / defaults | gate refusals random / defaults |
|---|---|---|---|---|---|
| `j18` (this branch: option (a), G2) | 839 | 856 | not recorded | not recorded | not recorded |
| `i26` (t5: option (a), R1, start rule, 4 workers) | 884 | 866 | 1.63 M / 13.8 M | 0.49 M / 1.45 M | 87 k / 60 k |
| **`k02` merged, nearest (committed)** | **887** | **869** | 3.12 M / 19.0 M | 0.87 M / 2.49 M | 132 k / 210 k |
| `k11` merged, strongest first (patch) | 894 (895: 788 clean alone) | 885 | 2.51 M / 17.3 M | 1.05 M / 2.04 M | 198 k / 142 k |
| `k15` merged, mixture (patch) | 889 | 874 | 4.14 M / 19.8 M | 1.19 M / 2.80 M | 196 k / 300 k |
| `k04` merged, donor scale restored (scratch) | 877 | 832 | 5.95 M / 14.6 M | 0.86 M / 1.39 M | 100 k / 54 k |

The merged code does more Newton work than `i26`: the G2 vessel closures count as Newton solves (one iteration each, A101) and more seeds run their 120 slices.

**Cleared and worse, merged nearest (`k02`) against each parent** (classes from `transitions.js`, log `k02-transitions.txt`; every worse seed held alone in the same class, `k03`):

| | cleared | worse |
|---|---|---|
| random vs `j18` | 70: E6 27, C 18, E2 10, E1 6, E10 2, E7 2, D 1, E3 1, E5 1, E8 1, F 1 | 22: E6 14 (17 48 55 71 244 273 282 408 699 727 736 768 916 981), C 4 (15 286 453 629), E2 2 (561 839), E10 1 (333), F 1 (757, E6 alone) |
| random vs `i26` | 3: C 853, E4 669, E8 23 | 0 |
| defaults vs `j18` | 39: E6 11, E2 6, E1 4, F 4, C 3, D 3, E9 3, E10 2, E7 2, E4 1 | 26: E6 14 (39 444 459 606 616 651 681 779 780 794 892 915 967 992), E2 5 (29 94 333 654 908), E9 2 (9 19), C 96, E3 505, E4 718, E5 316, F 1000 |
| defaults vs `i26` | 6: E4 5 (247 320 517 621 919), E5 698 | 3: E6 529, E6 915, C 96 |

The worse seeds against `j18` are t5's worse set of `i26` (start rule against R1) almost seed for seed. Against `i26`, G2 removed 5 of t5's 7 new E4 worlds (247 320 517 919; 654 is E2 now, 892 E6, 718 still E4).

**Budget holds alone** (`k03`, one worker): defaults 72 123 156 164 431 450 563 705 833 994 1000 all hold on the budget again; random 246 678 788 on the budget, 279 and 829 in E8, 431 in C, 757 in E6. None runs clean alone.

### 3. The E6 gate worlds rebuilt (`96d9be65`, A111)

Under the start rule both former worlds of `ConnectionRowScaleTest` passed with the donor scale (t5 `i28`). Physics of the class: at a zero-flow root neither end of a connection is the donor, so a row scale taken from the donor jumps there; option (a) takes the smaller of the two ends' cap drops.

Selection: a campaign with the donor scale restored in the working tree (`k04`, `src/scratch-t7-donor-scale.patch`, one line: `scaleDrop=limitDrop`) gives 877 / 832. 23 default seeds are E6 there and clean with option (a) (`k02`), 4 random. The two smallest default seeds, 724 (8 blocks) and 247 (11 blocks), were minimized with the donor scale applied (`HoldMinimizerTest`, class E6 kept, 120 slices; `k05`), not `minimizeForCards`.

| Test | World | Expected | Option (a), 120 slices (`k07`, `k08`) | Donor scale, working tree only (`k06`, `k08`) |
|---|---|---|---|---|
| `twoLevelWaterGeneratorsDrainingIntoAVoidBelowKeepUp` | seed 724, 4 blocks at tick 110: two 1 atm water generators level with a pipe between them, a 1 atm nitrogen void one block below it | water falls 1 m into the void (9.8 kPa of drive), both generators feed it equally | rests at 13.94 kg/s (6.97 kg/s each), junction 99.32 kPa; pass | holds at tick 113, "Newton iteration limit at residual 1.1346118737212058E-8" at every step size |
| `aWaterGeneratorDrainingBetweenAVoidAboveAndAVoidBelowKeepsUp` | seed 247, 5 blocks at tick 140: a water generator two pipes from a junction with a void above and one below | water drains into the lower void; the upper void stands above the junction and only receives, so its connection closes | rests at 9.12 kg/s, junction 94.0 kPa, upper connection CLOSED; pass | holds at tick 203, "Newton iteration limit at residual 0.35733095897731915" at every step size |

The brief named `scratch-e6-scale-from-donor-ends.patch` for the proof; that patch is the one-way refinement (A91), not the donor scale, so the one-line reversal above was used.

### 4. Which suppliers set a junction's start: nearest, strongest, mixture (D39)

**Physics, in real-life order.** When the last pipe closes a junction between two suppliers and a drain, both suppliers push in while the junction stands below their heads. The junction's pressure rises until what leaves equals what enters. Two outcomes:
- The drain takes everything while the junction stays below the weaker supplier's head (an open, conductive drain): both keep feeding, and the junction holds their mixture in proportion to their flows.
- The drain backs up (a closed tank filling, a narrow or velocity-capped drain): the junction climbs above the weaker head, the weaker supplier closes (one-way boundary, D12, D21), and only the strongest's stream remains.

The start state only has to be right for the first interval; the steps then reach the steady state.

**Form (m), as implemented for the measurement** (`src/scratch-t7-start-mixture.patch`, A112). In `NetworkReduction.junctionStarts`, beside the committed choice, each supplier's walk records its candidate at every junction it reaches: a pumped stream with the pump's volume flow times the stream's density; otherwise its static drive over the pressure the junction drains into (step 5's drive) times the laminar conductance of the walked path (`PipeResistance`'s laminar slope with the stream's density and viscosity). A junction that drains somewhere and has two or more candidates starts as their mixture: driven streams alone where any reaches, weights = flow estimates, enthalpy-weighted, at the flow-weighted pressure of the streams, solved for the temperature through `flashTP` (D34 fold), scaled to the flow-weighted mean of the streams' masses. Pockets, single candidates and "nothing reaches" keep the committed choice. Conductance was available everywhere. One deviation from the brief: the drive is taken against the drain's pressure (the free-drain limit), not against "the junction's start pressure", which is not known before the mix (it would be circular) and is not hydrostatically restated (E9 (a)).

**Campaigns** (section 2's table; worse seeds and budget holds alone `k13`, `k17`: all hold again in the same class, except random 788 under strongest, clean alone):

| Against nearest (`k02`) | random cleared / worse | defaults cleared / worse |
|---|---|---|
| strongest (`k11`) | 22 (C 10, E6 7, E2 4, E10 1) / 15 (E6 10, C 3, E2 1, E8 1) | 19 (E6 8, E2 3, C 2, F 2, E3, E4, E5, E9) / 3 (C 839, E2 401, E8 763) |
| mixture (`k15`) | 18 (E6 10, C 5, E2 3) / 16 (E6 9, F 2, C, E2, E4, E5, E9) | 13 (E6 7, E8 2, E9 2, E2, F) / 8 (E6 4, E5 2, C 839, E8 665) |

**Suites per form** (`k01`, `k12`, `k16`): 516/0 each; the 33 junction lines, MIXED_GAS_COST 309, chain-100 6.049e-15 and all printed suite lines are identical across the three forms. The junction-line fixtures and the exact regression build their graphs directly; the six fixtures that state a junction start on purpose (A106) keep their states.

**Start against steady state** (`T7StartProbeTest`, logs `k18` mixture, `k19` strongest, `k20` nearest). A 200 kPa nitrogen generator three blocks east and a 150 kPa methane generator one block west of a junction pipe, both at 298.15 K and level with it; the junction drains 1 m down. Junction nitrogen mole fraction at the mint, after the first 5 s interval and after 120 slices:

| World | nearest | strongest | mixture (m) | steady (120 slices, every form) |
|---|---|---|---|---|
| into a 1 m3 nitrogen tank at 1 atm, 0.05 m pipes | 0 -> 0.504 -> 1 | 1 -> 1 -> 1 | 0.252 -> 0.618 -> 1 | N2 only: the tank fills to 200 kPa, methane closes |
| into a 1 atm void, 0.05 m pipes | 0 -> 0.504 -> 1 | 1 -> 1 -> 1 | 0.252 -> 0.618 -> 1 | N2 only: the capped drain holds the junction at 189.9 kPa, above 150 kPa |
| into a void, 0.01 m supply pipes, 0.1 m junction (wide drain) | 0 -> 0.013 -> 0.571 | 1 -> 0.993 -> 0.571 | 0.458 -> 0.460 -> 0.571 | N2 0.571 / CH4 0.429 at 101.3 kPa, both supplies velocity-capped |

The start pressure of (m) was 168.5 kPa (179.8 kPa in the wide world); nearest starts at 150 kPa, strongest at 200 kPa. Card world E4 (defaults seed 6): no junction has two candidates, so the three forms start alike (all three junctions as the tank's nitrogen at tick 1538); on the merged code the seed holds in E3 at tick 1836, the first interval of a new island formed by a later placement (junctions started as water; "Water-vapor approximation outside qualified partial-pressure range"). It was clean on t5's branch.

**What this says.** Strongest is the steady state in the two worlds whose drain backs up, the mixture is closest in the open-drain world, nearest matches none. (m) as specified over-weights the weak supplier wherever the drain backs up, because its drive is the free-drain drive.

**Options for the owner (stop point S1, A113):**
- (c2) strongest first: right wherever the drain backs up; never starts a junction with a supplier that is pushed back. 895 / 885. A one-line change.
- (m') the mixture with the junction pressure estimated from both sides: the suppliers' flows at that pressure (laminar law, capped by each connection's velocity limit) equal the drain's (the same law toward the drain); suppliers standing below it drop out. It reduces to strongest where the drain backs up and to the flow-weighted mixture where it is open, which is the owner's physics (D39). Not implemented or measured.
- (m) as measured: right only for open drains.
- Nearest (committed): matches no steady state in these worlds.

**Recommendation:** (m') measured as its own item, since it is what happens in real life in both regimes. If one form must be committed before that, take (c2): in the worlds tested it is the steady state whenever the drain backs up. The campaign counts agree with this order but are evidence only.

### 5. Final gates (`k21`, `96d9be65`, GameTest run id `t7-k21-96d9be65`)

| Gate | Result |
|---|---|
| G1 fluid suites | 516 tests, 0 failures |
| G2 exact regression | red only on chain-100, the value already moved by G2 (moles 6.049e-15, 1.705e-13 K, flow 1.896e-11), unchanged against `j23` |
| G3 adjacent selection | 45 / 45 |
| G5 fluid GameTests | 33 / 33 passed |

### 6. References (nothing re-recorded; the re-baseline is the owner's, D36/D38)

| Reference | old (`j23`, = `h25` except chain-100) | new (`k21`) |
|---|---|---|
| 33 junction lines | 8 LIQUID_JUNCTION, 24 MIXED_GAS_TRANSIENT as on `j23` | identical, line for line |
| MIXED_GAS_COST | 309 | 309 |
| chain-100 | moles 6.049e-15 (node 27 mass 38.765655832609795 -> 38.76565583261003), 1.705e-13 K, flow 1.896e-11 | identical |
| GameTest outlets tankP / tankMass | 123785.89308927572 Pa / 172.50833166163517 kg | identical |
| GameTest compressor ratio / discharge | 2.999985907556435 / 157976.31408415743 Pa | identical |
| printed suite lines (wall times masked, reason maps sorted, XML headers left out) | 6072 | identical |

The re-baseline of D36/D38 is still pending: the 24 mixed-gas lines, MIXED_GAS_COST 309 and chain-100 differ from the recorded references exactly as on `j23`.

### 7. What differs from the brief

- Commit trailers name the model that wrote them (Claude Opus 5.5), not "Claude Fable 5.1".
- The fixtures that state a junction start on purpose are A106 in the renumbered rows (t5's A103), not A103.
- The donor-scale proof used a one-line reversal: the named patch is the one-way refinement (section 3).
- (m)'s drive is taken against the drain's pressure (section 4); a third two-generator world (wide drain) and the state after the first interval were added, because the tank world's steady state is the strongest supplier alone.
- The card E4 world (defaults seed 6) holds in E3 on the merged code.
- Documents are not copied to the main checkout (own worktree only).

## Junction start (m'): the pressure-balanced mixture (2026-10-12)

- Author: Claude (Opus 5.5, thread t8 of the orchestrator), worktree `remove-agents-attribution-line-3d0c97`, side branch `claude/start-mixture` from `e80e6f4e`. Owner decisions D37, D39, D40; sub-choices A116 onwards (`DECISION_LOG.md`). Material: `tools/phase-ports-probes/hold-classes/` logs `m03` onwards (`m01` and `m02` are taken by the class E1 work).
- Sections 1 and 2 were written before the code, as the brief asks.

### 1. Physics, in real-life order

The last block closes a junction between its suppliers and its drains. Each supplier pushes fluid in while the junction stands below the supplier's head there. Each drain takes fluid out while the junction stands above the drain's pressure. The junction holds a few grams (0.05 s of its peak flow), so within that time its pressure settles where what enters equals what leaves. Every supplier whose head is below that pressure closes (a generator never receives, D12, D21). The junction then holds what is still flowing in, mixed in proportion to the flows. Two limits follow:
- The drain is open (wide, short, or uncapped): the junction stays near the drain's pressure, every supplier keeps feeding it, and it holds their flow-weighted mixture.
- The drain backs up (a narrow or velocity-capped connection, or a tank filling): the junction rises toward the strongest supplier's head, the weaker suppliers close, and it holds the strongest supplier's fluid.

The start only has to be right for the first interval; the steps then reach the steady state. So the start is the state the junction holds once its flows balance against the boundaries as they stand now.

### 2. The rule (`NetworkReduction.junctionStarts`, step 7), stated before coding

Notation, at one junction J: `P` the junction pressure being estimated; for a supplier `s`, `S_s` its static pressure at J's height (its end pressure less its own column to J, as the committed walk computes it); for a receiver `d`, `B_d` the pressure J must exceed to send into it (the committed drain walk: its end pressure, raised to a valve's set point on the way). `q` is a mass flow.

**The balance.** `P` solves

`sum over suppliers s of q_s(P) = sum over drains d of q_d(P)`

with each flow given by its own path's law:
- A passive supplier: `q_s(P) = Q_path(S_s - max(P, V_s))` when positive, else 0. `V_s` is the highest valve set point on the path (a valve holds its upstream at its set point and closes below it). `Q_path(dp)` is the flow whose loss over the path's connections equals `dp`, read through each connection's own law (`PassiveNetwork.Pipe.pressureDrop`: laminar below Reynolds 2000, the solver's blend above, minor losses included) on the supplier's stream, and capped at the path's velocity cap, the stream's `density * smallest area * velocityLimit`. The stream is the one the committed rule sends (the phase the port draws first, at the supplier's temperature and pressure).
- A pumped supplier (the walk crossed a pump or compressor that admits the stream, D1): `q_s(P) = min(target volume flow, smallest area * velocityLimit) * stream density` while `P` is below the device's ceiling at J (the suction's standing pressure plus the device's rise limit, less the column to J), else 0.
- A drain through passive connections and valves: `q_d(P) = Q_path(P - B_d)` when positive, else 0, on the fluid J holds (the mixture being estimated), capped at that fluid's velocity cap.
- A drain through a pump or compressor (the drain walk's bound is minus infinity): its target volume flow times the density of the fluid J holds, capped at the path's velocity cap, if the device admits that fluid (a pump: no gas; a compressor: gas only, D1); else nothing.

Every term is monotone in `P` (supplies fall, drains rise), so `P` is found by bisection between the lowest and the highest finite `S_s`, ceiling and `B_d`; deterministic, no tolerance band. The flows at that `P` are the weights.

**What "drain" means.**
- A void: a receiver at its pressure (it never supplies, D21).
- A tank at its current pressure: a receiver at its end pressure (a bottom liquid port with its level head). A tank that holds fluid is also a supplier at its `S`. One connection carries one direction: while `P` is below the tank's `S` it supplies and does not receive; it receives only above both its `S` and its `B`. Between them (a tank below J) nothing flows: that band is the unprimed drain column (D40).
- A lower junction is not a drain. A junction's mass is pinned in a step; it passes fluid on. The drain walk continues through junctions to the voids, tanks and devices beyond them, as committed.
- A drain's own column is not counted (D40, unprimed): `B_d` is the receiver's end pressure with no column between J and it; a liquid does not siphon down a run that holds gas.

**The fluid J holds**, for the drain laws, is the mixture of the open suppliers. It is not known before the balance, so the balance and the mixture are iterated: start from the strongest supplier's stream; solve `P`; mix the open suppliers at `P`; solve `P` again on that mixture; stop when the open set repeats and `P` moves by less than 1e-9 of itself, at most 8 rounds. Inside one bisection the mixture's gas volume is scaled with `P` as an ideal gas (its condensed volume and velocity limit held), so no flash runs per trial pressure.

**The start state.**
- The open suppliers are those with a positive flow at `P`. If they all send one stream (one supplier, or one supplier through several connections of the same port), the junction starts as that stream at its own state, as the committed rule and form (c2) start it.
- Otherwise it starts as their mixture: compositions weighted by the flows, enthalpies weighted by the flows (adiabatic mixing), flashed at `P` through `flashTP` (the D34 fold applies), its temperature solved so that the flashed enthalpy equals the mixed one.
- No supplier open at `P` (every head at or below it): the supplier standing highest, the limit of the balance.

**Special cases.**
- A pocket (J drains nowhere, no `B_d`): the supplier standing highest fills it, the weaker staying shut (D12), with the committed order (a pumped stream first, then the standing pressure, the length, the denser stream, the identities). The balance would give the same limit; the committed comparator is kept so that pockets start exactly as before.
- Nothing reaches J: the committed contact rule (the boundary in contact standing highest at J's height, voids included).
- A property evaluation fails (a viscosity or a flash outside the data): the junction starts as the supplier standing highest (form (c2)).

**What the balance absorbs.** "A pumped stream first" (its fixed flow raises `P` above the weaker heads by itself), "the nearest", and the tie-breaks by length, density and identity outside pockets: equal suppliers simply mix.

**Approximations (each an A-row).** One path per supplier walk (the shortest by length, as the walk finds it) and one per receiver (the drain walk's); each junction balanced on its own, ignoring the other branches its paths pass; a path's loss read on the supplier's stream at the supplier's state throughout.

### 3. As implemented (`437aa3f4`), and where it differs from section 2

The code follows section 2. The walks of the committed rule stay as they were (who can reach a junction is unchanged); they now record, per supplier connection and junction, one arrival with its walked path, and the drain walk records the connection toward each receiver. The balance runs only where it can change the result: a junction that drains somewhere and that two or more streams reach (distinct supplier or port). A junction one stream reaches starts as that stream, as before, whatever the balance would give, so single-supplier junctions start exactly as under the committed rule. One deviation from the brief: the path law is each connection's own law, not the laminar slope (A117). The sub-choices are A116 to A125.

### 4. The probe worlds (`T7StartProbeTest`, `t8-probes.sh`; logs `m03`, t7's `k18`-`k20`)

Two generators (200 kPa nitrogen three blocks east, 150 kPa methane one block west, 298.15 K, level) feed one junction that drains 1 m down. Junction nitrogen mole fraction at the mint, after the first 5 s interval, after 120 slices:

| World | nearest | strongest | mixture (m) | **(m')** | steady (every form) |
|---|---|---|---|---|---|
| into a 1 m3 nitrogen tank at 1 atm | 0 -> 0.504 -> 1 | 1 -> 1 -> 1 | 0.252 -> 0.618 -> 1 | **1 -> 1 -> 1** | N2 only |
| into a 1 atm void, 0.05 m pipes | 0 -> 0.504 -> 1 | 1 -> 1 -> 1 | 0.252 -> 0.618 -> 1 | **1 -> 1 -> 1** | N2 only |
| into a void, 0.01 m supplies, 0.1 m drain | 0 -> 0.013 -> 0.571 | 1 -> 0.993 -> 0.571 | 0.458 -> 0.460 -> 0.571 | **0.5708 -> 0.5708 -> 0.5708** | N2 0.571 / CH4 0.429 |

The balance pressure is a close estimate of the solver's own: in the void world 189 910.7 Pa against 189 909.7 Pa at rest (the methane closes, 150 kPa being below it); in the wide world 101 325.7 Pa against 101 316.6 Pa, both supplies at their velocity caps (0.01776 and 0.00765 kg/s, the steady flows to four digits). In the tank world the first instant is the void world (the tank stands at 1 atm), so methane is closed from the start; the tank then fills to 200 kPa. Card E4 (defaults seed 6): no junction has two suppliers, so every form starts alike; the seed still holds in E3 at tick 1836 (A114).

### 5. References (`m04`, on `437aa3f4`; nothing re-recorded)

| Reference | `k21` | (m') |
|---|---|---|
| fluid suites | 516 tests, 0 failures | 516, 0 |
| 33 junction lines | as `j23` | identical, line for line |
| MIXED_GAS_COST | 309 | 309 |
| chain-100 | moles 6.049e-15, 1.705e-13 K, flow 1.896e-11 | identical |
| adjacent selection | 45 / 45 | 45 / 45 |
| printed suite lines | 6072 | identical (0 only-old, 0 only-new) |

No line moves: the junction-line fixtures and the exact regression build their graphs directly, and the six fixtures that state a junction start on purpose keep their states (A106).

**Final gates (`m16`, on `009b77c5`, GameTest run id `t8-m16-009b77c5`):** G1 517 tests, 0 failures (516 + the gate test); the 33 junction lines, MIXED_GAS_COST 309 and all 6072 printed lines of the earlier suites identical to `k21`; G2 red only on chain-100, with `k21`'s values (moles 6.049e-15, 1.705e-13 K, flow 1.896e-11); G3 45 / 45; G5 fluid GameTests 33 / 33 passed. Compressor ratio 2.999985907556435, discharge 157976.31408415743 Pa, unchanged. The outlets test read tankP 123722.65120082271 Pa and tankMass 172.0709849344571 kg (`k21`: 123785.89308927572 / 172.50833166163517). That is not a moved reference: two reruns on the same code (`m17`) read 123785.89308927572 / 172.50833166163517 (`k21`'s values to the last digit) and 123722.65120082271 / 172.0709849344571. The test reads its stage on a wall-clock deadline, and earlier runs on unchanged mints alternate between the same two states (logs `b05`/`b11`, `w07`/`x06`, `t01`) (A125).

### 6. Campaign (`m05`, 8 workers, 1000 seeds per distribution; worse seeds and budget holds alone `m06`)

| Campaign | random | defaults | Newton iterations r / d | Newton solves r / d | gate refusals r / d |
|---|---|---|---|---|---|
| `k02` nearest (committed) | 887 | 869 | 3.12 M / 19.0 M | 0.87 M / 2.49 M | 132 k / 210 k |
| `k11` strongest (c2) | 894 (895 alone) | 885 | 2.51 M / 17.3 M | 1.05 M / 2.04 M | 198 k / 142 k |
| `k15` mixture (m) | 889 | 874 | 4.14 M / 19.8 M | 1.19 M / 2.80 M | 196 k / 300 k |
| **`m05` (m')** | **898 (900 alone: 6, 23)** | **883** | **2.40 M / 18.3 M** | **0.72 M / 2.00 M** | **108 k / 106 k** |

(m') does the least Newton work in random worlds and the fewest equation-gate refusals in both; strongest does less in defaults.

Cleared and worse against each form (classes from `transitions.js`, logs `m05-transitions-vs-*`):

| (m') against | random cleared / worse | defaults cleared / worse |
|---|---|---|
| nearest `k02` | 25 (C 10, E6 9, E2 5, E10) / 14 (E6 5: 173 622 773 794 853; F 3: 6 19 23; C 444 767; E2 298 372; E5 815; E8 357) | 19 (E6 7, E2 3, C 2, E4 2: 718 824, F 2, E3, E5, E9) / 5 (C 839, E2 401, E4 763, E6 518, F 246) |
| strongest `k11` | 9 (E6 8, E3 30) / 5 (F 6 23, E2 298, E5 815, E6 173) | 2 (E4 824, E6 529) / 4 (E6 26 78 518, F 246) |
| mixture `k15` | 18 (E6 8, C 5, E10, E2, E5, E9, F) / 9 (E6 173 773 853, F 6 19 23, C 444, E2 768, E8 357) | 17 (E5 5, E6 5, E4 2, E8 2, C, E2, F) / 8 (E6 26 175 556 915, E4 763, E8 352, E9 629, F 246) |

Replayed alone with one worker (`m06`, the union of the worse seeds of the three comparisons and every budget hold): every worse seed holds again in its campaign class; random 6 and 23 (budget holds in the campaign) run clean alone; random 19 and 698 are C alone, 311 E2, 757 E6; defaults 757 E8; the other budget seeds (random 246 431 678 788, defaults 123 156 246 450 563 705 833 994) hold on the budget again.

Class counts (random / defaults), nearest -> (m'): C 30 -> 23 / 6 -> 5; E2 13 -> 10 / 23 -> 17; E3 1 -> 0 / 4 -> 3; E4 1 -> 1 / 5 -> 5; E5 3 -> 5 / 9 -> 13; E6 44 -> 42 / 47 -> 40; E7 1 -> 1 / 2 -> 2; E8 6 -> 4 / 14 -> 12; E9 4 -> 4 / 6 -> 7; E10 3 -> 2 / 4 -> 4; F 7 -> 10 / 11 -> 9.

### 7. Gate test and its proof (`009b77c5`, A124)

| Test | World | (m') | committed rule restored (working tree only) | (c2) applied to the committed rule |
|---|---|---|---|---|
| `JunctionStartMixtureTest.twoGeneratorsVentingIntoAVoidStartTheirJunctionAsTheirMixture` | random seed 30 contrast-minimized to 8 blocks (`m09`): a 361 kPa water generator at 356 K and a 366 kPa nitrogen generator at 333 K feed a junction level with them, which vents up into a 1 atm void; a second void at tick 756 | pass, 120 slices (`m11`) | holds at tick 758, "Newton iteration limit at residual 0.004078805602725129" at every step size (E6) | holds at tick 104 on the water-vapour range (E3) |

Physics of the world: both generators stand about 260 kPa above the void, so both keep feeding and the junction holds their mixture. At rest (120 slices) the junction holds 2.4 % nitrogen (mole) at 332 kPa, both generators flowing. (m') starts it at 2.7 % and 355 kPa on the first island (the solver's state after 5 s: 2.7 %, 355.09 kPa), and after the second void at 2.2 % and 335 kPa, beside the new junction at 1.0 % and 102.4 kPa (at rest 1.5 %, 101.5 kPa) (`m10`). Nearest starts the junction as the water at 361 kPa, strongest as the nitrogen; neither is a state the flows produce. The plain minimizer with the committed rule restored lost the contrast (7 blocks, no junction with two suppliers, (m') holds too: `m07`, `m08`), hence the contrast minimizer.

### 8. Sub-choices measured, and the stop point

| Choice | On the branch | Alternative | Measured |
|---|---|---|---|
| A117 path law | each connection's own law | laminar slope (brief, (m)) | 898 / 884 against 898 / 883 (`m12`); laminar loses the gate world. Not material |
| **A119 start pressure** | a lone open supplier at its own state (as committed and (c2)); a mixture at the balance pressure | **every junction that drains somewhere at its balance pressure** | **919 / 879** (`m14`): random +27 / -6 (E6 42 -> 26), defaults +4 / -8; all worse hold alone (`m15`). **Material** |
| A118 rounds | at most 8 | more rounds, a secant step | the gate world's `P` is 8 Pa from the solver's after 8 rounds; the open set settles in round 0 or 1 everywhere printed |

**Stop point S1' (A119), options:**
- (i) Keep the lone supplier's own state (on the branch). Composition is (m'); pressure as committed. 898 / 883.
- (ii) Start every draining junction at its balance pressure. In real life the junction stands there within its holdup time, so this is the physical start, and it is card E9 (a)'s question (the start pressure) answered by the same balance. 919 / 879. It clears 19 random E6 worlds, which are mostly A97's "far from convergence on the first interval after a placement" family; it adds 4 default E6 and 3 E8 worlds that were not traced.
- Recommendation: take (m') with (i) now, and measure (ii) as its own item with card E9, tracing two of its worse default worlds first. (ii) follows the physics and the random gain is large, but it changes every formed junction's start pressure, and its default losses are unexplained.

### 9. What differs from the brief

- The path law is each connection's own law, not the laminar conductance (A117).
- Logs are `m03` onwards: `m01` and `m02` exist (class E1 work).
- The gate world needed a contrast minimizer (A124); the plain minimizer lost the contrast.
- Commit trailers name the model that wrote them (Claude Opus 5.5), not Claude Fable 5.1.
- Documents are not copied to the main checkout (own worktree only).

## Junction start at the balance pressure (2026-10-13)

- Author: Claude (Opus 5.5, thread t9 of the orchestrator), worktree `remove-agents-attribution-line-3d0c97`, side branch `claude/start-pressure` from `claude/start-mixture` (`8503d5e5`). Item: t8 stop point S1' (A119). Sub-choices A126 to A132 (`DECISION_LOG.md`). Material: `tools/phase-ports-probes/hold-classes/` logs `n01`-`n16`.
- Conclusion first: every junction that drains somewhere now starts at the pressure where its inflow equals its outflow. Campaign **920 random / 883 defaults** against `m05`'s 898 / 883. No recorded reference moves; two printed suite lines move. Of the five default worlds that are worse, four hold long after a first interval that both starts compute alike; the fifth (992) is a start case whose root is outside this rule (section 5).

### 1. Physics, in real-life order

The last block closes a junction between its suppliers and its drains. The junction holds a few grams, which is 0.05 s of its peak flow. Within that time its pressure settles where what enters equals what leaves; that is the balance pressure `P` of (m'). A supplier whose head is below `P` closes. This holds for one supplier as for several. One generator feeding a drain through a narrow pipe does not hold the junction at the generator's head: the narrow pipe takes most of the drop, and the junction stands near the drain. So the start is the open suppliers' streams at `P`. One stream is throttled to `P` (adiabatic, isenthalpic: a gas cools slightly by Joule-Thomson). Several are mixed and flashed at `P`, as (m') already does.

### 2. The rule (`NetworkReduction.junctionStarts`, code `5999da45`)

- A junction that drains somewhere starts from the balance of (m'), whether one stream or several reach it. The single-stream shortcut in `junctionStarts` and the "one open stream: its own state" return in `balanced` are gone. One open stream goes through `mixture` like several: a mixture of one, flashed at `P` with its own enthalpy.
- The balance and the fluid the drains carry are iterated for a single stream too (at most 8 rounds; the stream throttled to `P` is the drain fluid of the next round). t8's scratch (`m14`) returned after round 0. The difference is small in pressure (the void probe: 189 910.74 Pa in round 0, 189 909.72 Pa converged), but it clears defaults 96, 529 and 853, three of t8's eight worse worlds (A126).
- Unchanged: a pocket starts as the supplier standing highest at its own state (D12); "nothing reaches" keeps the contact rule; a flash outside the data or no open supplier falls back to the strongest supplier at its own state (A123).

### 3. The probe worlds (`T7StartProbeTest`, `t8-probes.sh`; log `n01`)

Junction pressure and nitrogen mole fraction at the mint, after the first 5 s interval, and after 120 slices:

| World | start P (Pa) | 5 s P (Pa) | 120 slices P (Pa) | N2 start / 5 s / 120 | t8 (m') start P |
|---|---|---|---|---|---|
| tank (1 m3 N2 at 1 atm) | 189 909.720618 | 189 909.720618 | 200 003.120118 | 1 / 1 / 1 | 200 000 |
| capped void | 189 909.720618 | 189 909.720618 | 189 909.720618 | 1 / 1 / 1 | 200 000 |
| wide drain | 101 325.740363 | 101 316.554100 | 101 316.554100 | 0.570806 / 0.570806 / 0.570806 | 101 325.7 |
| card E4 (defaults seed 6) | 101 325 | 101 325 | holds in E3 at tick 1836, as on `m05` | 1 (N2) | 101 325 |

- The start pressure is the balance pressure in every world (the last balance round prints the same value).
- In the capped-void world the start equals the solver's steady pressure to all printed digits. In the tank world it is the first instant: the tank stands at 1 atm, so the junction is the void world's; the tank then fills to the generator's 200 kPa.
- In the wide-drain world the estimate is 9.19 Pa (9.1e-5) above the solver. The drain is 1 m below the junction, and its gas column (about 11 Pa) is not counted (D40). Counting it (option G, section 6) gives 101 314.51 Pa, 2.05 Pa below (`n12`).

### 4. Campaign (`n02`, 8 workers, 1000 seeds per distribution; worse seeds and budget holds alone `n03`)

| Campaign | random | defaults | Newton iterations r / d | Newton solves r / d | gate refusals r / d |
|---|---|---|---|---|---|
| `m05` (m'), lone stream at its own state | 898 | 883 | 2.40 M / 18.3 M | 0.72 M / 2.00 M | 108 k / 106 k |
| `m14` t8's scratch, round 0 only | 919 | 879 | 3.37 M / 16.0 M | 1.13 M / 1.93 M | 193 k / 121 k |
| **`n02` start at `P` (`5999da45`)** | **920** | **883** | **2.50 M / 20.3 M** | **1.08 M / 2.35 M** | **199 k / 173 k** |

- Against `m05`: random 28 cleared (E6 19: 17 55 98 132 223 271 282 408 446 526 622 629 654 727 736 773 794 824 981; C 4: 35 424 717 767; F 3: 6 23 757; E5 505; E9 761) / 6 worse (E6 257 286 460 529 916; E2 854). Defaults 5 cleared (E2 654, E4 872, E5 681, E6 17, E8 352) / 5 worse (E6 364 992; E8 459 824; F 424).
- Against `m14`: random +2 (629, 761) / -1 (556); defaults +4 (17, 529, 853, 96) / 0.
- Alone (`n03`, one worker): every worse seed holds again in its campaign class. Budget seeds: random 246 431 651 678 698 707 788 hold on the budget again; defaults 156 246 424 450 563 705 833 929 994 on the budget again, 116 and 757 in E8.
- Classes, `m05` -> `n02` (random / defaults): C 23 -> 15 / 5 -> 5; E2 10 -> 10 / 17 -> 13; E3 0 / 3 -> 3; E4 1 -> 1 / 5 -> 4; E5 5 -> 4 / 13 -> 13; E6 42 -> 30 / 40 -> 42; E7 1 -> 2 / 2 -> 2; E8 4 -> 6 / 12 -> 14; E9 4 -> 2 / 7 -> 7; E10 2 -> 3 / 4 -> 3; F 10 -> 7 / 9 -> 11.
- Cost, by counters (the campaign ran alone, but cost is judged by counters): random worlds run 22 more seeds to the end with 4 % more Newton iterations; the equation gate refuses about twice as often in random worlds (108 k -> 199 k) and 64 % more in default worlds (106 k -> 173 k), and default worlds take 11 % more Newton iterations at the same clean count. Not traced (A132).

### 5. The worse default worlds, traced (contrast minimizer `n04`: the start at `P` holds in the class, the head start runs clean; replays `n05`-`n08`, failing rows with `instr-e6-failing-rows-and-scratch.patch`)

| Seed (class) | World (minimized) | Real life | What the solver did | Why the start at `P` leads there |
|---|---|---|---|---|
| 824 (E8) | 6 blocks: water generators 1 m above and 1 m below a junction, a 1 m3 N2 tank beside it, a void 2 m away level with it (placed last, tick 343) | The upper generator feeds about 9.6 kg/s through the junction to the void; the tank drains to the junction's pressure and rests at 108.65 kPa with 0.11 m3 of water; the lower generator stays shut | Both starts reach this steady state, equal to 9 digits. At tick 11970, 580 s after the last block, every step size fails on the junction's rows 4 and 5 (its gas-phase rows): the junction holds 1e-22 to 1e-26 mol of nitrogen beside 36.4 mol of water, brought by a 1e-11 kg/s flicker on the tank's bulk port; "package pressure > 2 MPa", equation gate 1.07e-3 | It does not. The first island's junction starts at 106 208.7 Pa instead of 101 325 Pa, and after its first interval both runs read 105 721.557571 Pa. The second island starts identically under both rules (a mixture). Only the low bits of the trajectory differ (junction vapour 2.9e-16 against 1.5e-16 m3 at the end). The hold is the trace-nitrogen-in-water family (card E5, D37) |
| 364 (E6) | 10 blocks: two water generators level with two junctions, N2 tanks 2 m above and 1 m below them, a pump placed last | The lower tank, filled by the generators, vents nitrogen up to the upper tank until the two stand in hydrostatic balance; then rest | 41 s after the last block, three connection rows of the gas path (edges 3, 2, 0) end at 1.76e-7 after 20 iterations at every step size, 0.034 kg/s of nitrogen moving up through two junctions at 292 to 295 K | It does not directly. Both starts give the same states after the first interval except 1e-4 of one junction's condensed water. The head run passes the same stretch with 1470 failed Newton passes (the start at `P`: 279) and survives |
| 459 (E8) | 14 blocks: a void and an N2 tank 2 m above four junctions, a water generator beside the tank, another level with the junctions | At the start nothing flows: the junctions hold the tank's nitrogen at 101 347.46 Pa (2 m of gas below 1 atm). The upper generator fills the tank, and its water later reaches the junctions | Both starts read 101 347.464154 Pa at all four junctions after the first interval. 21 s later water reaches the junctions, which still hold nitrogen bubbles (0.013 mol beside 40.9 mol of water); worst rows: edge 6 (junction to void) 0.093 and the junction's row 4; 516 refused attempts | Not through its state after the first interval. The start itself is 13 Pa low (101 330 to 101 338 Pa against 101 347.46 Pa): the balance lets the junction vent to the void 2 m up without the 23 Pa gas column of that run (D40). The head run passes the same transient with 307 failed passes |
| 992 (E6) | 13 blocks: two N2 tanks at y 0, junctions at y 0 and y -1, a water generator at y -1; a pipe placed at tick 197 forms the last island | Every boundary stands at 1 atm. The junctions at y -1 hold the tanks' nitrogen at 101 336.2 Pa (1 m of gas below them), 11 Pa above the generator's head: the generator stays shut, nothing flows | First interval after the pipe (tick 199), every step size: Newton limit at 4.86e-6 on edge 1 (the 3 m run from a water-filled junction at y -1 to a gas junction above), cap drops 5.7e6 Pa on the water side against 6958 Pa on the gas side (E6's water-beside-gas signature) | The start at `P` is a symptom. The mint re-decides junctions that held nitrogen at 101 336 Pa a moment earlier. The two tanks stand at exactly the same pressure, 4e-11 Pa below 1 atm, so each tank's walk stops at the level junction where the other is the bound (strict drive, no band). The generator, 4e-11 Pa above that bound because the gas column is not counted, is then the only supplier, so the junctions start as water. Under the head rule the same arrivals start them as the same water; the head run differs because there tank 7 is one ulp higher, passes, and starts them as nitrogen |
| 424 (F; E6 with the budget lifted) | 11 blocks: a water generator level with a void, N2 tanks 1 m and 2 m up, gas junctions at y 0 and y 1 | Nitrogen stands hydrostatically; a little water and gas move between the tanks and the void | The start at `P` puts the gas junctions at y 1 at 101 305 to 101 322 Pa; the solver's first interval reads 101 313.8 Pa; the head start 101 325 Pa. The run then exceeds the soft budget at tick 363 (with the budget lifted: E6 at tick 563, residual 1.65e-5, trial temperatures below 273.16 K in water) | The two estimates are about 10 Pa off in opposite directions, the same order as the drives (the gas columns, about 12 Pa per metre, uncounted on the drain side). The trajectories then differ at 1e-6 |

What the traces say:
- Three of the five worse worlds (824, 364, 459) hold 20 s to 10 min after a first interval that both starts compute alike to 6 to 9 digits, in families that exist on `m05` (trace nitrogen in water, E5; gas-path iteration limit, E6; water reaching nitrogen junctions, E8). The start changes only the low bits of the trajectory there.
- Two (992, 424) are 1-atm worlds where the decisive drives are gas columns of about 12 Pa per metre. There the balance misses the drain-side gas column (D40), so its estimate is off by the same order as the drive, and in 992 it decides which fluid reaches a junction.
- None of the five is a defect of throttling one stream to `P`. Nothing here is forced to change inside this rule.

### 6. Options from the traces (not taken; owner decisions)

- **Option G, the drain-side gas column** (scratch `src/scratch-t9-gas-column.patch`, campaign `n13`). In real life, sending gas into a receiver 2 m up through runs that hold that receiver's gas means lifting that gas: the junction must exceed the receiver's pressure by the gas column (11.7 Pa per metre for nitrogen at 1 atm). D40 leaves every drain column out, for the liquid siphon it rightly rejects. Variant measured: the column of a gas-only receiver's own gas counted in the drain bound, in both directions; liquid columns stay out (D40). Result: **920 random / 884 defaults**. Defaults 14 cleared (E6 244 364 446 584 992; E8 183 226 459; F 246 424 450; E2 199 333; C 161) / 13 worse (E6 352 529 736 886; E8 1 214 850; E10 188 387; E5 717 853; E2 681; E9 23), not traced or replayed alone. Random 2 / 2. It clears four of the five worse worlds and brings the wide-drain probe's estimate from 9.2 Pa to 2.1 Pa off. Recommendation: take G as the physics of D40's own argument (the run holds what it displaces, and here that is gas), after tracing two of its 13 worse default worlds; it amends D40, so it is the owner's.
- **Option K, keep a junction's contents across a topology edit** (not measured). In world 992 the junctions held nitrogen at 101 336 Pa until a pipe was placed elsewhere; the recompile re-mints every junction from the boundaries (the accepted behaviour, `PhysicalRegistryTest` review 9.3, D25's sizing). In real life the fluid in a pipe stays when a block is added. Recommendation: keep it as its own later item (it touches the ledger's booking of junction stock).
- **The ulp tie** in the reachability walk (two tanks at exactly equal pressure block each other's walk) needs no option of its own: with G the generator is not driven, and the junctions start as the tanks' nitrogen by the contact rule.

### 7. References (`n14` on `80d8f010`; nothing re-recorded; against `m16`)

| Reference | `m16` (t8) | `n14` (this branch) |
|---|---|---|
| fluid suites | 517 tests, 0 failures | 518, 0 (517 + the gate test) |
| 33 junction lines | as `j23` | identical, line for line |
| MIXED_GAS_COST | 309 | 309 |
| chain-100 | moles 6.049e-15, 1.705e-13 K, flow 1.896e-11 | identical |
| adjacent selection | 45 / 45 | 45 / 45 |
| GameTest compressor ratio / discharge | 2.999985907556435 / 157976.31408415743 Pa | identical |
| GameTest outlets tankP / tankMass | 123722.65120082271 Pa / 172.0709849344571 kg | identical (one of the two run-dependent values, A125) |
| printed suite lines | 6072 | 6072; 6 lines moved (below) |

The six moved printed lines (`n16-vs-m16-printed-printed-diff.txt`):
- `PhysicalRegistryTest.junctionHoldupsAreBookedAsConstructedAndRemintedAtATopologyEdit`: "junction 3: m_J 0.009559865965626475 kg, 0.05 s x cap flow 0.19119731931252948 kg/s" -> "m_J 0.006526959465768548 kg, ... 0.13053918931537095 kg/s". A 150 kPa methane generator fills two 1 atm nitrogen tanks through a junction. The junction now starts as methane throttled to its balance pressure (about 102 kPa by the density ratio) instead of at 150 kPa, so its holdup is sized from the lower density. The test's own assertion (`m_J = tau x cap flow`) holds.
- `InlineFilterLineHoldTest` (the F8 layouts' last island at tick 13763), five junction states at z = -59: pressures moved by at most 4.2e-4 Pa (junction 11: 219 422.15365244157 -> 219 422.1536105142 Pa; junction -8: 210 524.6370582299 -> 210 524.63705681462 Pa), temperatures by at most 1.5e-7 K (junction 7: 298.1517489213752 -> 298.15174892137543 K; junction 17: 298.16091755533955 -> 298.16091766837604 K; junction 11: 298.1566151452809 -> 298.15661534743117 K; junction -8: 298.1585861776083 -> 298.1585863294091 K; junction 3: 298.15058297349134 -> 298.1505829734914 K). The water junctions start at their balance pressures; after 13 000 ticks the trajectories differ in the last digits.

### 8. Gate test and its proof (`80d8f010`)

| Test | World | Start at `P` | Head start restored (working tree only, `scratch-t9-start-switch.patch`, `-Pt9start=head`) |
|---|---|---|---|
| `JunctionStartMixtureTest.aLoneSupplierThroughANarrowPipeStartsItsJunctionsAtTheirBalancePressure` | random seed 17 contrast-minimized to 9 blocks (`n09`): a 195 kPa nitrogen generator at 288 K feeds three junctions through two 0.01 m pipe blocks; they drain into two 1 atm nitrogen tanks | pass, 120 slices (`n11`) | holds at tick 101, "Newton iteration limit at residual 2.8961491928988534E-7" at every step size (E6) |

Physics: the narrow supply runs at its velocity cap (0.0179 kg/s), so the junctions stand near the tanks. The start at `P` mints them at 109 270.9, 101 330.0 and 101 327.7 Pa; the solver's first interval reads 109 305.5, 101 369.3 and 101 378.8 Pa. The head start mints all three at the generator's 194 962 Pa, a state the flows cannot produce. The first test of the class (t8's mixture world) passes under both starts. Other contrast candidates found: random 223 (7 blocks, a 393 kPa crude generator venting into two voids) and 132 (12 blocks).

### 9. Final gates (`n14`, on `80d8f010`, GameTest run id `t9-n14-80d8f010`)

| Gate | Result |
|---|---|
| G1 fluid suites | 518 tests, 0 failures |
| G2 exact regression | red only on chain-100, with `m16`'s values (moles 6.049e-15, 1.705e-13 K, flow 1.896e-11) |
| G3 adjacent selection | 45 / 45 |
| G5 fluid GameTests | 33 / 33 passed |

### 10. Stop points, with recommendations

- **Take the branch.** The start at `P` is where a draining junction stands in real life; it absorbs the single-stream special case; it moves no recorded reference; random worlds gain 22 (898 -> 920, random E6 42 -> 30), default worlds keep 883. Its cost is more equation-gate refusals (A132).
- **Option G (drain-side gas column) is the owner's** (it amends D40). Recommendation in section 6: trace two of its worse default worlds, then take it.
- **Option K (keep junction contents across a topology edit)**: a later item.
- Pockets still start at the strongest supplier's own state, not at its static head at the junction's height (card E9 (a)); not changed here.

### 11. What differs from the brief

- Three of t8's eight worse default worlds (96, 529, 853) are clean once the single-stream balance is iterated like the mixture's, so the worse list on this rule is five (364, 424, 459, 824, 992); all five were traced.
- The traces found no structural cause inside the rule that physics forces; the structural cause they found (the drain-side gas column) is an amendment of D40, so it is measured as option G and not taken.
- Commit trailers name the model that wrote them (Claude Opus 5.5), not Claude Fable 5.1.
- Documents are not copied to the main checkout (own worktree only).

## Junction start: the drain-side gas column (2026-10-13)

- Author: Claude (Opus 5.5, thread t10 of the orchestrator), worktree `remove-agents-attribution-line-3d0c97`, side branch `claude/gascolumn-gametest` from `claude/start-pressure` (`c4f20b4e`). Item: t9's option G (A129), which amends D40 for gas. Sub-choices A133 onwards (`DECISION_LOG.md`). Material: `tools/phase-ports-probes/hold-classes/` logs `o01` onwards.
- Sections 1 and 2 were written before the code, as the brief asks.

### 1. Physics, in real-life order

A pipe is never empty. When the last block closes a junction, the pipes between the junction and its drains are already full: at rest, they hold the fluid the drain side holds, which in this mod is gas (a void and a fresh tank are nitrogen charges at their pressure). D40 says that a run holds what it displaces, so a liquid arriving at a junction does not see a liquid column in its drain pipe until it has filled that pipe: a siphon does not start on its own, and a level source does not spill over a rise. The same argument says what the drain pipe does hold at the first instant: the gas it is full of. That gas has weight, about 11.2 Pa per metre for nitrogen at 1 atm and 298 K. To push anything into a void 2 m above the junction, the junction must first stand 22 Pa above the void's pressure, because the 2 m of nitrogen in the pipe must be lifted out. Into a void 1 m below, the nitrogen in the pipe helps by 11 Pa. This acts from the first instant, because the column is already there; it does not wait for anything to flow.

In 1-atm worlds the drives are of this size: a gas tank 1 m above another stands 11 Pa lower, and a water generator a few pascals above the void's pressure decides whether water enters a nitrogen junction. t9 found two worse worlds (992, 424) where the start estimate was off by about this column in the direction that decides who flows (section "Junction start at the balance pressure", 5 and 6).

### 2. The rule (`NetworkReduction.junctionStarts`, the drain walk), stated before coding

**Which column counts.** The gas that stands in the drain path at the first instant: the gas the receiver holds, at its own density as it stands in the receiver (`vaporDensity` of the receiver's state). The path is at rest at that instant, so it holds the receiver's gas at the receiver's pressure; the junction's start state does not exist yet when the drain bounds are walked, and the supplier walk tests reachability against those bounds. The column is the receiver's gas density times `g` times the height from the junction up to the drain's pressure reference (negative when the reference is below).

**The drain's pressure reference.**
- A void or a tank: its end pressure at its own height, as the solver states it (`PassiveStepSolver.portHead`: the gas space's pressure, plus the level head for a tank with a liquid level drained through its liquid port). For a tank with a liquid level this is the gas column from the junction to the tank, read in its gas space's gas, the level head above its port being the tank's own (already counted).
- A valve on the drain path whose set point is above the pressure that arrives at its upstream node: the set point at that node's height (the valve holds its upstream there). The gas column then runs from the junction to the valve's upstream node only.
- A pump or compressor: it takes what reaches its suction (bound minus infinity), so no column applies.

**What does not count.** A liquid column on the drain side (D40, unchanged): the path cannot be liquid-filled at the start. A receiver that holds no gas (a liquid-full tank): no column, as now. A receiver at the junction's height: the column is exactly `+0.0`, so its bound is the same double as before.

**How it enters.** The bound of each drain at each junction becomes the reference pressure plus that gas column. That bound is used in three places, all unchanged in form: (1) the supplier walk lets a stream enter a junction only while its static pressure there is strictly above the lowest bound (step 5's drive); (2) the balance of (m') reads each drain as `Q(P - B_d)` on the fluid the junction holds, so the balance pressure `P` now includes the column; (3) the junction starts as its open streams at that `P` (mixed or throttled, as on `5999da45`). The supplier side is unchanged: a supplier's standing pressure already subtracts its own stream's column to the junction.

**How the walk carries it.** The drain walk relaxes junction by junction. Adding a column per connection would let rounding make a loop of junctions look a few ulps lower each time round. So the walk carries, per junction, the reference (pressure and height) its bound comes from, and computes the bound from that reference and the junction's own height: every path to the same reference gives the same double, and a valve replaces the reference only where its set point is higher.

**A tank that both supplies and receives.** For a gas tank the supplier's standing pressure at the junction (its pressure less its gas column) and its drain bound (its pressure plus the gas column the other way) are now the same number, so a gas tank and a junction in its gas have no dead band between supplying and receiving, as in real life. A tank with a liquid level keeps a band (its liquid column against the gas column of the pipe): that band is the unprimed drain column of D40.

**Difference from t9's scratch** (`scratch-t9-gas-column.patch`, campaign `n13`): the scratch counted only gas-only receivers, and added the column from the receiver's height to every junction after the walk, also past a valve. This rule also counts the gas space of a tank that holds liquid (the brief's case), and stops the column at a valve that sets the bound.

### 3. As implemented (`c09dc06d`)

The code follows section 2 with no deviation. The drain walk in `NetworkReduction.junctionStarts` keeps, per receiver and junction, a reference pressure and height; `column(density, reference, z)` returns exactly `+0.0` for no gas or no height. The receiver's gas density is `FluidThermodynamics.vaporDensity` of its state when it holds a gas phase, else zero. The supplier walk, the balance (`balanced`, `Outlet`), the mixture and the fallbacks are unchanged; the javadoc of the start rule and of `Outlet` now states the gas column. The sub-choices are A133 to A140.

### 4. The probe worlds (`T7StartProbeTest`, `t8-probes.sh` with the balance print; log `o06`)

Junction pressure and nitrogen mole fraction at the mint, after the first 5 s interval, and after 120 slices:

| World | start P (Pa) | 5 s P (Pa) | 120 slices P (Pa) | N2 start / 5 s / 120 | t9 (`n01`) start P |
|---|---|---|---|---|---|
| tank (1 m3 N2 at 1 atm, 1 m down) | 189 909.720618 | 189 909.720618 | 200 003.120118 | 1 / 1 / 1 | 189 909.720618 |
| capped void (1 m down) | 189 909.720618 | 189 909.720618 | 189 909.720618 | 1 / 1 / 1 | 189 909.720618 |
| wide drain (1 m down) | **101 314.508368** | 101 316.554100 | 101 316.554100 | 0.570806 all | 101 325.740363 |
| card E4 (defaults seed 6) | 101 325 | 101 325 | holds in E3 at tick 1836, as before | 1 | 101 325 |

- In the wide-drain world the estimate moves from 9.19 Pa above the solver to 2.05 Pa below it (the same value as t9's scratch, `n12`). The rest of the gap is the column of the mixture that actually flows (57 % nitrogen, 43 % methane, lighter than the void's nitrogen) and the path law.
- The tank and capped-void worlds do not move: their drain runs at its velocity cap, so the balance pressure is set by the suppliers, not by the drain's bound.

### 5. Campaign (`o02`, 8 workers, 1000 seeds per distribution; worse seeds and budget holds alone `o03`)

| Campaign | random | defaults | Newton iterations r / d | Newton solves r / d | gate refusals r / d |
|---|---|---|---|---|---|
| `n02` start at `P` (`5999da45`) | 920 | 883 | 2.50 M / 20.3 M | 1.08 M / 2.35 M | 199 k / 173 k |
| `n13` t9's option G scratch | 920 | 884 | 2.96 M / 20.1 M | 1.13 M / 2.28 M | 208 k / 161 k |
| **`o02` gas column (`c09dc06d`)** | **919** | **883** | **2.96 M / 19.9 M** | **1.11 M / 2.27 M** | **204 k / 161 k** |

- Against `n02`: random 2 cleared (E6 556 719) / 3 worse (E6 98 824; E5 629). Defaults 14 cleared (E6 244 364 446 584 992; E8 183 226 459; F 246 424 450; E2 199 333; C 161) / 14 worse (E6 17 352 529 736 886; E8 1 214 850; E10 188 387; E5 717 853; E2 681; E9 23).
- Against t9's scratch `n13`: one seed per distribution differs: random 98 (E6 here, clean there) and defaults 17 (E6 here, clean there); defaults 98 moves E6 -> E7 and 116 F -> E4. So 13 of the 14 worse default worlds are the scratch's 13.
- Alone (`o03`, one worker): every worse seed holds again in its campaign class; every budget hold (random 246 431 678 707 788, defaults 71 156 705 757 833 994) holds on the budget again.
- Classes `n02` -> `o02` (random / defaults): C 15 -> 15 / 5 -> 5; E2 10 -> 9 / 13 -> 13; E3 0 / 3 -> 3; E4 1 -> 0 / 4 -> 7; E5 4 -> 6 / 13 -> 14; E6 30 -> 30 / 42 -> 44; E7 2 -> 3 / 2 -> 3; E8 6 -> 6 / 14 -> 12; E9 2 -> 3 / 7 -> 6; E10 3 -> 4 / 3 -> 4; F 7 -> 5 / 11 -> 6.
- Cost, by counters: default worlds take 2 % fewer Newton iterations and 7 % fewer equation-gate refusals than `n02` (173 k -> 161 k); random worlds take 18 % more Newton iterations (2.50 M -> 2.96 M) and 2 % more refusals, as the scratch did. Budget holds fall from 18 to 11.

### 6. The worse default worlds, traced (contrast minimizer `o04`: the start with the column holds in the class, the start without it runs clean; replays `o05` with the balance print and the failing rows)

| Seed (class) | World (minimized) | Real life | What the solver did | Why the column changes the trajectory |
|---|---|---|---|---|
| 1 (E8) | 4 blocks, card E6's world: a 1 atm water generator and a 1 atm nitrogen tank level with a junction, a 1 atm void 1 m below it | The drain pipe is full of nitrogen, which helps 11.2 Pa, so water trickles in and the tank vents nitrogen; within seconds the drain primes and water falls to the void at about 10 kg/s; the tank keeps exchanging water and gas with the junction for minutes (the clean run still has 2 kg/s leaving the tank at 10 min) | With the column the junction starts at 101 324.48 Pa as the flow-weighted mixture of 0.36 kg/s of water and 0.013 kg/s of nitrogen: 2 % nitrogen by mole, about 97 % gas by volume. The solver's first interval reads 101 307 Pa, still gas-rich. At tick 472 (16 s later) every step size fails: 757 rejected substeps, 476 of them "Backward-Euler boundary reopened"; the last failing row is the junction's row 10 (r 0.0019) with the void's connection closed and 3.9 kg/s of water going into the tank | Without the column the generator and the tank stand exactly at the void's 101 325 Pa, the strict drive test fails, nothing reaches the junction, and the contact rule starts it as the generator's water; the first interval then reads 99 582 Pa, 99.95 % water, and the run is clean. With the column the start is the real first instant, but the solver must then carry a gas-filled junction through the water-displaces-gas transient: the E8 family t9 traced in world 459 |
| 736 (E6) | 7 blocks: a 1 atm water generator level with a junction at y 1, a 1 atm nitrogen tank 1 m below; a 200 kPa valve placed at tick 842 re-forms the island | The pipe to the tank is full of nitrogen, which helps 11.2 Pa: water enters, primes the pipe and falls into the tank, compressing its nitrogen; the displaced nitrogen rises through the tank's top port | With the column the junction starts as water at 101 319.38 Pa; the first interval reads 94 391 Pa (water falling 1 m into the tank), the physical state. After the valve the re-formed junction starts as water at 101 305.04 Pa; 3 ticks later the tank's vapour connection into the water-full junction fails (row 10, edge 1, r 1.7e-8; nitrogen 0.059 kg/s up against water 1.21 kg/s down; cap drops 1149 Pa on the gas side against 957 790 Pa on the water side) | Without the column the water generator and the tank's gas tie exactly, nothing reaches, the junction starts as nitrogen and stays nitrogen for 10 minutes: the water never moves, a state real life cannot hold. The run is clean because nothing happens. With the column the water moves and the run meets card E6's water-beside-gas rows |
| 17 (E6) | 14 blocks: two nitrogen tanks, a void and a water generator placed last | (not worked out: a late hold) | All junctions start as water under both rules; the start pressures differ by a few pascals (101 317 against 101 325 Pa) and the re-formed island's states by about 7 Pa. The hold comes at tick 7212, 331 s after the last block, E6 with a nitrogen-pressure range refusal | The one world where this rule and t9's scratch differ (the gas space of a tank that holds water, or a valve). An existing family reached 5.5 min later on a trajectory shifted by pascals |

What the traces say:
- In both traced worlds the column is the difference between "nothing reaches" (an exact tie, decided by the contact rule) and a real flow. The column-free starts run clean in states that do not happen in real life (736: water standing still beside a gas-filled drain), or start the junction as the fluid it reaches minutes later (1).
- The holds are families that exist on `n02` (E8 water displacing gas in a junction, E6 gas beside water at a junction's connection), reached because the water now moves. Neither is a defect inside the start rule, and no change inside the stage is forced by these traces.
- A possible lever, not taken: the mixture's weights at a 5 Pa drive make a junction 97 % gas by volume although the drain primes within a second. Starting a junction that liquid will fill as primed would be the D40 question again (owner's).

### 7. References (`o12` on `a9bc988b`; nothing re-recorded; against t9's `n14`)

| Reference | `n14` (t9) | `o12` (this branch) |
|---|---|---|
| fluid suites | 518 tests, 0 failures | 519, 0 (518 + the gate test) |
| 33 junction lines | as `j23` | identical, line for line |
| MIXED_GAS_COST | 309 | 309 |
| chain-100 | moles 6.049e-15, 1.705e-13 K, flow 1.896e-11 | identical |
| adjacent selection | 45 / 45 | 45 / 45 |
| printed suite lines | 6072 | 6072, identical (0 only-old, 0 only-new; `o12-vs-n14-printed-printed-diff.txt`) |
| GameTest compressor ratio / discharge | 2.999985907556435 / 157976.31408415743 Pa | identical |
| GameTest outlets tankP / tankMass | 123722.65120082271 Pa / 172.0709849344571 kg (one of the A125 pair) | 124258.48752327723 Pa / 175.76227454727493 kg: the read moved to a stated tick (next section), not a physics change |

No suite line moves: the fixtures build level worlds or worlds whose receivers are at the junctions' height, where the column is exactly `+0.0`.

### 8. Gate test and its proof (`f42dbd0f`)

| Test | World | With the column | Column dropped (working tree only, `scratch-t10-column-switch.patch`, `-Pt10column=off`) |
|---|---|---|---|
| `JunctionStartMixtureTest.aJunctionUnderTheTanksNitrogenStandsAboveALevelWaterGenerator` | defaults seed 992 contrast-minimized to 11 blocks (`o07`, `o08`): two 1 atm nitrogen tanks at y 0, a 1 atm water generator at y -1, junctions at y 0 and y -1; a pipe at tick 165 re-forms the island | pass, 120 slices (`o09`) | holds at tick 168, "Newton iteration limit at residual 0.0011582258596294515" at every step size (`o09-gate-column-off-result.xml`) |

Physics: everything stands at 1 atm. The junction at y -1 sits under 1 m of the tanks' nitrogen, so it stands at 101 336.23 Pa (the solver's first interval reads exactly that), 11 Pa above the generator's head: the generator stays shut and nothing flows. With the column the junction's drain bound is 101 336.2 Pa, the generator does not reach it, and it starts as the tanks' nitrogen. Without it the bound is the tanks' pressure, which by the re-formation at tick 167 has drifted a few 1e-11 Pa below the generator's 101 325 Pa (t9's "ulp tie"; the balance prints 101 324.99999999999 Pa), so the generator wins the strict drive test, the re-formed junction starts as water and the island holds 1 tick later. Other contrast candidates: defaults 364 (10 blocks, a pump placed last; the column-free start holds at tick 985) and 459 (14 blocks; tick 628).

### 9. Stop points, with recommendations

- **Take the gas column (A133, amends D40 for gas; the owner confirms).** It is D40's own argument applied to what the drain pipe actually holds. It turns exact ties between a level liquid source and a gas-filled drain into the flows real life has (worlds 1, 736, 992), moves no recorded reference, keeps the counts (919 / 883 against 920 / 883), cuts default budget holds from 11 to 6 and default gate refusals by 7 %, and brings the wide-drain estimate to 2 Pa. Its worse worlds are existing families reached because water now moves.
- Not taken: a primed start for a junction that a liquid will fill within a second (section 6). It is D40's liquid question, not this item.
- Option K (keep junction contents across a topology edit, A130) is still a later item; world 992's re-formed junction now starts right by physics without it.

### 10. What differs from the brief

- Thirteen of the fourteen worse default worlds are the scratch's thirteen; the fourteenth (17) is new to this rule. Three were traced (1, 736 in full; 17 to its starts).
- The column's density is the receiver's gas as it stands in the receiver, not the junction's start state: at the first instant the drain path is at rest and holds the receiver's gas, and the walk needs the bound before any start state exists (A134).
- Commit trailers name the model that wrote them (Claude Opus 5.5), not Claude Fable 5.1.
- Documents are not copied to the main checkout (own worktree only).

## GameTest reads at a fixed online tick (2026-10-13)

- Author: Claude (Opus 5.5, thread t10), same branch. Item: A125. Code `a9bc988b` (test only; the engine is unchanged). Logs `o10`-`o12`.
- Conclusion first: the outlets GameTest's two values came from removing the generator one tick apart. The removal now happens at a stated online tick of the engine's clock, and two full gate runs print the same value.

### 1. The two moments (`o10`, four runs of the unchanged test with tick prints, `instr-t10-outlets-gametest-ticks.patch`)

`FluidPhasePortGameTests.aTanksTopSideAndBottomOutletsEachCarryTheirPhase` fills a tank from a 130 kPa generator. On the first server tick on which the tank's published view shows at least 150 kg, it removes the generator and its pipe and places the three outlets. The engine commits the fill in intervals; the interval that first shows 150 kg ends at committed tick `C` with 173.535 kg. A worker solves it, and its completion is drained in `ServerTickEvent.Post`; the GameTest's callback runs earlier in the tick. Whether the completion is drained at the end of server tick `C` or `C+1` depends on wall time, so the test first sees it at online tick `C+1` or `C+2`, and the removal event is fenced at that tick (`PhysicalRegistry.submit`: the event's tick is the online tick of the submit). The old island is solved to the fence, so the tank holds one more tick of filling (8.7 kg/s, 0.44 kg) in one case:

| Run | publication seen (lag) | edit fence | tank at the fence | first interval of the new island | printed tankP / tankMass |
|---|---|---|---|---|---|
| 2 | `C+1` (1 tick) | `C+1` | 173.98 kg | `[C+1, C+2]` | 123722.65120082271 / 172.0709849344571 |
| 1, 3, 4 | `C+2` (2 ticks) | `C+2` | 174.42 kg | `[C+2, C+3]` | 123785.89308927572 / 172.50833166163517 |

The reading itself is the new island's first interval (one tick, about 38 kg/s out of three outlets) in every run; only its starting tick moved.

### 2. The change (`a9bc988b`, test only)

- The edit is made at the stated online tick `C + 10` (`EDIT_AFTER_TICKS`), where `C` is the end of the published interval that first shows 150 kg. The test asserts that this interval was published before that tick (the lag measured is 1 or 2 ticks; the cadence floor is 20 ticks, so no new interval is published in between, and at the fence tick a slice may end there but never beyond it).
- The outlets are read on the new island's first interval, which ends at the edit + 1 (a topology change starts the island with a one-tick slice); the test asserts that committed tick and that this interval carries the three outlets.
- The engine, the schedule and every view are unchanged; the test only chooses its moments on the engine's clock.

### 3. Old pair, new value (nothing re-recorded)

| | tankP (Pa) | tankMass (kg) |
|---|---|---|
| old (edit on the tick that saw the publication) | 123722.65120082271 or 123785.89308927572 | 172.0709849344571 or 172.50833166163517 |
| new (edit at `C + 10`, read at the edit + 1) | **124258.48752327723** | **175.76227454727493** |

Full gates twice on `a9bc988b` with fresh run ids (`o12`, `t10-o12-a9bc988b-run1` and `-run2`): G1 519 / 0 both, G2 red only on chain-100 with `n14`'s values, G3 45 / 45, G5 33 / 33 both; outlets printed `edit=3490` and `edit=3478` (different absolute ticks) with the same tankP and tankMass to every digit; compressor ratio 2.999985907556435 and discharge 157976.31408415743 Pa both. The check run `o11` read the same value. The only later code commit, `48a332e5`, changes a comment of the gate test.

## A junction keeps its contents across a topology edit (2026-10-13)

- Author: Claude (Opus 5.5, thread t11 of the orchestrator), worktree `remove-agents-attribution-line-3d0c97`, side branch `claude/keep-contents` from `claude/gascolumn-gametest` (`86ba0572`). Item: option K (A130). Sub-choices A142 onwards (`DECISION_LOG.md`). Material: `tools/phase-ports-probes/hold-classes/` logs `p01`-`p18`.
- Conclusion first: a junction whose node exists again after an edit now keeps its contents (`078aa67f`); no recorded reference moves. The campaign is **915 random / 874 defaults** against `o02`'s 919 / 883. The traced losses are real transients the re-mint hid by deleting or conjuring junction contents; two structural items outside the start rule follow (A148, A140), measured, not committed. Recommendation: not to take the branch alone (section 10).
- Sections 1 and 2 were written before the code, as the brief asks.

### 1. Physics, in real-life order

A player places or removes a block. At that instant no fluid moves. A water-full run stays water-full at its pressure; a junction full of nitrogen at 101 336 Pa stays nitrogen at 101 336 Pa. The edit changes the paths, not the contents: the new flows start from the states that stand in the pipes.

Only a block whose connectivity the edit creates has no history the model kept. That is the new block itself, and a pipe that becomes a junction because it gained a third connection: in the model its old contents were part of a run, which stores nothing. The junction start rule decides those. A junction whose node disappears (it falls back to two connections, or its branch becomes a dead end) leaves the solve; its few grams are removed and booked (D26).

Today the recompile destroys every junction of a replaced island and mints all of them again from the boundaries. That re-decides fluid that was standing still. In world 992 (t9) a pipe placed elsewhere made the mint start junctions that held nitrogen at 101 336 Pa as water; in general it makes "the first interval after a placement" a hold family of its own (A97), because every junction of the island starts from an estimate instead of from where it stood. The owner, keeping the junction holdup in September (D25): "keep it, but think about case where different stuff is placed again". This is that case.

### 2. The rule (the registry's compile, `PhysicalRegistry.apply`, and the mint, `PassiveNetwork.mintJunctions`), stated before coding

| Question | Answer | Sub-choice |
|---|---|---|
| Which junctions keep their contents | A junction of an island the batch replaces whose node exists again after the edit: the same pipe block (its device identity), the same pump, compressor or valve block, the same filter block (its upstream side by the block, its downstream side by the filter's own connection), the middle of a run between two device outlets by that run's identity. The block is not edited by the batch, and its height is the same | A142 |
| What it keeps | Its state (temperature, pressure, phases) and its owned inventory (amounts, energy field, solids), exactly | A142 |
| Its connection set changed | It keeps its state; its holdup is re-sized by D25's rule. The rule is `m_J = tau x (rho v)_seed x A`, with `A` the largest smallest-area of its connections and the seed fixed at the mint ("the mint is once, at the seed state"). So `m_new = m_old x A_new / A_old`, at the same composition and specific energy. The same widest bore gives the same mass, bit for bit | A143 |
| Which get the start rule | Every junction whose node did not exist in a replaced island: a new block, a pipe that gained its third connection, a node of an edited block, a node at another height. Also a junction with no connection before or after (D25's 1 kg rule has no area to scale) | A142 |
| The ledger | A kept junction with the same holdup: nothing is booked. Its mass is the same owned stock; nothing enters or leaves (as the trace fold, D34, books nothing). A re-sized holdup: booked as today's remint books every junction, the old holdup destroyed and the new constructed, so the net booking is `(A_new / A_old - 1) x` the old holdup at its own composition and energy, exact by construction. A new junction: constructed, as now. A junction whose node is gone: destroyed, as now (D26: a run that loses its destination has its few grams removed and booked) | A144 |
| Rest certificate | A replaced island starts uncertified and cold, as now. Its rest test (step 6) reads the kept junctions' states, which are the solver's last accepted states, not start estimates | A145 |
| Interval-start refresh and cold start | Unchanged. The refresh never restates a junction (`InventoryEquilibrium.refresh`). The cold start replaces every junction's state guess, not its inventory, by one rate solve of the port graph, for kept and new junctions alike; the steps conserve the kept inventory | A145 |
| Checkpoint load | Unchanged. A load restores the saved islands with their junctions; the rule acts only where an edit is applied. No format change and no migration; tested on fresh worlds | A146 |

**What it should absorb.** The remint of a junction that already holds fluid, and with it the comment and test that state "a junction's composition resets at a topology edit" (`PhysicalRegistry.apply`, `PhysicalRegistryTest`, D25's sizing note). It does not absorb D10 (the water trace in a dry junction): a kept nitrogen junction can still receive water later. The start rule's contact case ("nothing reaches") stays for new junctions. How much of A97 it absorbs is measured (section 5).

**Not forced by physics (stop points).**
- The density basis of a re-size (A143): the mint's seed (chosen), the state the junction holds now, or no re-size at all.
- The brief's opening paragraph also lists "a junction that gained or lost a connection" among those the start rule decides. Its item 1 keeps them and re-sizes. Real life keeps the fluid in a block that gains a neighbour, so the rule keeps them; the other reading is measured as a variant (A147).

### 3. As implemented (`078aa67f`)

The code follows section 2 with no deviation.
- `PhysicalFluidTopology.junctionKey` names a compiled junction physically: a block by its identity, a filter's downstream side by the filter's connection, a run middle by its first half.
- `PhysicalRegistry.apply` collects the junctions of the islands it replaces by key, with the widest connection area each was sized on; a key of a block the batch edits is dropped.
- `PassiveNetwork.mintJunctions(graph, model, held)` puts each held junction back (re-sized when its widest area changed), then mints the rest through the start rule as before.
- The booking leaves out every junction carried with the same inventory, on both sides.

Absorbed: the remint of a junction that already holds fluid, and with it the statement "a junction's composition resets at a topology edit" (`PhysicalRegistry.apply`, D25's sizing note, the remint test of `PhysicalRegistryTest`, now the keep test). Not absorbed: D10, the start rule's contact case, the cold start of a replaced island (section 6).

### 4. References (`p02` on the code, against t10's `o12`; nothing re-recorded)

| Reference | `o12` (t10) | option K |
|---|---|---|
| fluid suites | 519 tests, 0 failures | 520, 0 (519 + `JunctionKeepTest`; the gate test came later, section 8) |
| 33 junction lines | as `j23` | identical, line for line |
| MIXED_GAS_COST | 309 | 309 |
| chain-100 | moles 6.049e-15, 1.705e-13 K, flow 1.896e-11 | identical |
| adjacent selection | 45 / 45 | 45 / 45 |
| printed suite lines | 6072 | 6072; one line moved, the restated registry test's own print (below) |

The moved line (`p02-vs-o12-printed-diff.txt`): "remint: old junction methane fractions up to 1.0; fresh junctions at their seed composition" -> "keep: junction 3 minted at 102430.67546830291 Pa, solved to 149999.99999999988 Pa, kept; new junction 2 minted". The junction lines, the exact regression and the other fixtures build their graphs directly or are formed by one placement batch, so no kept junction reaches them.

### 5. Campaign (`p03`, 8 workers, 1000 seeds per distribution; worse seeds and budget holds alone `p11`)

| Campaign | random | defaults | Newton iter. r / d | Newton solves r / d | gate refusals r / d |
|---|---|---|---|---|---|
| `o02` (t10, the re-mint) | 919 | 883 | 2.96 M / 19.9 M | 1.11 M / 2.27 M | 204 k / 161 k |
| `p10` re-mint restored by the switch (check) | 919 | 883 | 2.91 M / 19.9 M | 1.10 M / 2.27 M | 204 k / 161 k |
| **`p03` option K (`078aa67f`)** | **915** | **874** | **6.39 M / 8.49 M** | **1.60 M / 1.40 M** | **184 k / 186 k** |
| `p09` variant strict (A147) | 923 | 875 | 3.59 M / 9.94 M | 1.19 M / 1.55 M | 213 k / 175 k |
| `p07` K plus unchanged islands kept (A148) | 914 | 876 | 6.42 M / 18.2 M | 1.60 M / 2.37 M | 187 k / 158 k |
| `p18` K plus A148 plus strict | 922 | 879 | 3.71 M / 15.6 M | 1.21 M / 2.13 M | 212 k / 166 k |

- The switch restores the re-mint exactly: `p10` holds the same seeds in the same classes as `o02`.
- K against `o02`: random 12 cleared (E6 65 173 188 244 257 529 681 853 915; E2 372 854; E5 629) / 16 worse (E6 9 78 615 654 719 794; C 94 108 219 505 621; E2 274 855; E10 895; E5 456; E8 107). Defaults 9 cleared (E6 108 357 401 518 719; E4 669; E5 853; E8 629; F 156) / 18 worse (E6 107 183 241 246 424 446 621 654 660 758 842; C 459 1000; E10 333; E4 178; E5 916; E7 573; E8 665).
- Alone (`p11`, one worker): every worse seed and every budget hold holds again in its campaign class (defaults 116: F in the campaign, E8 alone).
- Classes `o02` -> K (random / defaults): C 15 -> 21 / 5 -> 10; E2 9 -> 9 / 13 -> 14; E3 0 / 3 -> 1; E4 0 / 7 -> 4; E5 6 -> 5 / 14 -> 10; E6 30 -> 27 / 44 -> 56; E7 3 -> 2 / 3 -> 3; E8 6 -> 7 / 12 -> 11; E9 3 -> 2 / 6 -> 6; E10 4 -> 4 / 4 -> 5; F 5 -> 8 / 6 -> 6.
- Cost, by counters: random worlds take 2.2 times the Newton iterations (2.96 M -> 6.39 M), default worlds 0.43 times (19.9 M -> 8.49 M); gate refusals -10 % random, +16 % defaults. Not traced.

**The first-interval family (A97)** (`first-interval.js`: a held seed whose first hold is on the first interval of an island formed by a placement):

| | random: family / held | defaults: family / held |
|---|---|---|
| `o02` | 42 / 81 | 32 / 117 |
| K (`p03`) | 48 / 85 (31 kept, 11 left, 17 new) | 40 / 126 (26 kept, 6 left, 14 new) |
| strict (`p09`) | 45 / 77 | 44 / 125 |
| K plus unchanged islands (`p07`) | 47 / 84 | 33 / 122 |

The family does not shrink. It includes islands formed for the first time, where nothing is kept; of K's 17 + 14 new members, the traced ones are re-formed islands whose kept junctions meet a flow the re-mint had started them on (section 6).

### 6. The worse worlds, traced (contrast minimizer `p04`: K holds in the class, the re-mint runs clean; replays `p05`, `p06` with failing rows)

| Seed (class) | World (minimized) | Real life | What the solver did | What it says |
|---|---|---|---|---|
| defaults 246 (E6) | 7 blocks: a 1 atm water generator level with a junction, a 1 m3 nitrogen tank level with it, a second tank 1 m below fed through its top port; at tick 655 a dead-end stub on the junction pipe | The lower tank can take water only by pushing its nitrogen back up the same pipe, so the junction carries a slow water-nitrogen counterflow. The stub changes nothing | Without the stub the island runs 120 slices clean (`p05-replay-d-nostub`). With it, the recompile builds the same island; K keeps the junction's real mixture (3.6 % nitrogen by mole, two-phase), and the re-formed island's first interval fails at every step size, Newton limit at 4.4e-4 (rows of the lower tank and its vapour connection, flows of 1e-4 kg/s). The re-mint restarts the junction as the tank's nitrogen, booking its water away, and runs clean. Without the cold rate seed the hold is the same (`-Dt11nocold`) | The hold is the cold restart of an island that did not change: a fresh solver, a one-tick slice and no carried flows, at a near-zero-drive counterflow (the E6 family). The re-mint hid it by deleting real water |
| defaults 107 (E6) | 13 blocks: a line of junctions and a loop hold a void's nitrogen at rest; at tick 266 a pipe joins a 1 atm water generator 2 m above | Water falls about 20 kPa into the nitrogen-filled junctions at roughly 5 kg/s; a 12 g gas holdup is displaced in about 2.4 ms | K keeps the old junction's nitrogen; the first interval fails with trial states outside the data (nitrogen above 900 K, water below 273.16 K), Newton limit at 0.045. The re-mint starts the junctions as water and runs clean | Water displacing gas in a junction within the first step: the E8/E6 family of t9's world 459 and t10's world 1, and A140's primed-start question |
| random 108 (C), random 78 (E6) | 8 blocks: two pumps in a loop with a tank, a 339 kPa water generator placed last; 9 blocks: a methane generator, a void and two tanks, a tank placed last | (minimized only) | K: nitrogen trial above 2 MPa in a pump pocket; Newton limit at 0.53 | Not traced further; both are first intervals after a placement in which liquid or gas reaches junctions that kept another fluid |

What the traces say:
- K is the physics of the instant, and in the traced worlds it is the re-mint that was wrong: it deleted a junction's water (246) or started a junction as the fluid it would hold a few milliseconds later (107). Its "clean" runs were those of other worlds.
- The holds are existing families (E6 at near-zero drive; water displacing gas in a junction), now reached because the real contents stay.
- Two structural items follow, neither inside the start rule:
  - **A148, an island the edit does not change is not replaced.** With K, a dead-end stub recompiles an island to exactly the graph it had. Replacing it restarts it cold for nothing. Measured as `src/scratch-t11-unchanged-island.patch` (`p07`): world 246 minimized runs clean (`p06`); the campaign gives 914 / 876. The first run found an event-path defect of the patch (seeds 425 and 990, class X: a later queued event was not fenced on the kept island), fixed in the patch (`p08`). It changes four event-path tests that assert "an edit replaces its island", and lets a kept island keep its certificate. Not committed.
  - **A140 again**: a junction whose kept contents the new flows displace far within the first step (its holdup over the new flow, about 2 ms against 50 ms) could start as what flows in. Variant strict (A147) is a proxy: 923 / 875.

### 7. World 992 and the traced worlds of t9 and t10 (full seeds, defaults; replays `p15`)

| Seed | full seed: `o02` / K / strict | minimized world (t9 `n04`, t10 `o04`), K | same world, re-mint |
|---|---|---|---|
| 992 (t9's 13 blocks) | clean / clean / clean | 120 slices | 120 slices (the gas column already starts the y -1 junctions as nitrogen) |
| 364 | clean / clean / clean | 120 slices | 120 slices |
| 424 | clean / E6 / clean | holds at tick 167, Newton limit 1.2e-5, a nitrogen trial above 900 K | 120 slices |
| 459 | clean / C / clean | holds at tick 628, the package envelope at 4.0 MPa | 120 slices |
| 824 | E8 / E5 / E8 | holds at tick 1770, reconstruction gate 1.9e-6 | holds at tick 11970 (t9's hold) |
| 1 (t10) | E8 / E8 / E8 | holds at tick 472 | the same hold |
| 17 (t10) | E6 / E6 / clean | holds at tick 566 | holds at tick 7212 (t10's hold) |
| 736 (t10) | E6 / E6 / E6 | holds at tick 844 | holds at tick 845 |

World 992 is no longer a K case: since the gas column (t10) the start rule already starts its re-formed junctions as the nitrogen they held, and K keeps that nitrogen. The worlds K makes worse are first intervals or early transients after a placement.

### 8. Gate test and its proof (`e80feff0`)

| Test | World | Option K | Re-mint restored (working tree only, `scratch-t11-keep-switch.patch`, `-Dt11keep=remint`) |
|---|---|---|---|
| `JunctionKeepsContentsTest.aDeadEndPlacedBesideARestingJunctionLeavesItsNitrogenWhereItIs` | world-default seed 156 contrast-minimized to 6 blocks (`p12`): a junction level with a 1 atm water generator and a 1 atm void, under a 1 m3 nitrogen tank 1 m up; a dead-end pipe placed at tick 694 | pass: the junction keeps its nitrogen at 101 325.000000 Pa; 120 slices (`p14-gate-on`) | the junction starts as water and the island holds at tick 696 on the wall deadline (`p14-gate-remint-result.xml`); with the budget lifted, at tick 696 on "Newton iteration limit at residual 0.23008114112632308" (`p13`) |

Physics: the tank's nitrogen stands 11.7 Pa above the void at the junction and vents until they are level; the generator stands exactly at the void's pressure and stays shut; from about 15 s on everything rests with the junction full of nitrogen. The dead end changes nothing. The re-mint met an exact tie of all three boundaries at 101 325 Pa and gave the junction to the denser fluid (A108), a state no flow produces.

`JunctionKeepTest` (science level) states the re-size rule; `PhysicalRegistryTest`'s remint test is restated as the keep test (A151).

### 9. Final gates (`p16`, on `e80feff0`, GameTest run id `t11-p16-e80feff0-run1`)

| Gate | Result |
|---|---|
| G1 fluid suites | 521 tests, 0 failures (519 + `JunctionKeepTest` + the gate test); 33 junction lines identical to `o12`; MIXED_GAS_COST 309; 6072 printed lines, one moved (A151) |
| G2 exact regression | red only on chain-100, with `o12`'s values (moles 6.049e-15, 1.705e-13 K, flow 1.896e-11) |
| G3 adjacent selection | 45 / 45 |
| G5 fluid GameTests | 33 / 33 passed; outlets 124258.48752327723 Pa / 175.76227454727493 kg and compressor 2.999985907556435 / 157976.31408415743 Pa, identical to `o12`, so one run |

### 10. Stop points, with recommendations

- **Option K as committed is not a net gain yet: 915 / 874 against 919 / 883.** Every traced loss is a real transient the re-mint had hidden, by deleting a junction's water (246) or by starting a junction as what it would hold milliseconds later (107). The rule is the physics of the instant; the solver does not yet carry two of its consequences.
- **A148 (an island the edit does not change is not replaced):** physics forces it ("no change, no restart"). Recommended, as its own item with the four event-path tests restated: it removes a cold restart that has no cause.
- **A147 / A140 (a junction whose contents the new flows displace within the first step):** physics does not force the proxy. Strict (connections changed) gives 923 / 875; with A148, 922 / 879. A displacement-time test (holdup over new flow against the first step) would be the physical criterion; not built.
- **A143 (re-size basis):** the mint's seed, recommended; owner's.
- **Recommendation:** do not take the branch alone. Decide A148 and the A140 criterion; then measure K with both on this branch and take it if it holds `o02`'s default count. The gate test and the ledger rule stand either way.

### 11. What differs from the brief

- The brief's opening paragraph and its item 1 disagree on a junction that gained or lost a connection; item 1 (keep, re-size) is committed, the other reading measured (A147).
- World 992 is not a K case any more (section 7); the gate world is seed 156, a dead end beside a resting junction.
- The campaign is worse, not better; the traces point at two structural items outside the start rule (A148, A140), measured as patches, not committed.
- Commit trailers name the model that wrote them (Claude Opus 5.5), not Claude Fable 5.1.
- Documents are not copied to the main checkout (own worktree only).

## Kept contents: unchanged islands and the displacement criterion (2026-10-13)

- Author: Claude (Opus 5.5, thread t12 of the orchestrator), worktree `remove-agents-attribution-line-3d0c97`, side branch `claude/keep-contents` (from `d1849065`). Items: A148 (an island the edit leaves identical) and A140 (a kept junction the new flows displace within the first step), the two structural items the previous section left open. Decisions A152 onwards (`DECISION_LOG.md`). Material: `tools/phase-ports-probes/hold-classes/` logs `q01`-...
- Sections 1 and 2 were written before the code, as the brief asks.

### 1. Rule one: an island the edit leaves identical is kept as it is (A148), stated before coding

**Real life.** A player places a dead-end stub on a pipe of a working line. No flow changes and no junction's contents change. The line goes on exactly as before. Under option K the recompile of such an edit builds the island again exactly as it stood: every junction kept, every boundary and connection the same. Replacing it anyway restarts it cold (a new solver, a one-tick first slice, no carried flows, no certificate) for a change that did not happen. World 246 (defaults) is that case: the restart met a real, slow water-nitrogen counterflow cold and held; without the stub the same world runs clean.

**What "identical" means.** All of the following hold. Equality of the old island's graph and the recompiled graph with K's kept junctions placed (before any start rule or criterion) states all of them at once.
- The same devices: the island's member list equals the new island's.
- The same nodes in the same order, each at the same height: the same tanks with the same inventory and state, the same generators and voids at the same state (their settings), the same junction nodes.
- Every junction kept by K with the same inventory, state and holdup (the same widest bore). A junction that is new, re-sized or at another height makes the island changed.
- The same connections: identities, ends, sections, controls (valve set points, pump and compressor settings), blocked directions, phase ports, filters with the same cake.
- No scheduled transfer differs.

**What is released and what is kept.** An identical island is not replaced. It keeps its identity, its solver and caches, its committed interval and clock (cadence, slice length), its last result and, when it holds one, its rest certificate (the batch's alignment materialised it to the event's tick, as any fenced owner; materialising does not end a certificate). Its fences of the batch's events are released; the events still apply (ledger booking, registry and compiled form commit as before). Its members stay bound to it. Nothing is booked for it: every junction is carried with the same holdup (A144).

**A queued later event on a kept island is fenced.** The batch may leave a device no island owns between a later queued event and the kept island (a dead-end stub is owned by no island): that event's walk stopped at the stub when it was submitted, so the island beyond was never fenced for it. After a batch the registry fences every later event that reaches a replacement island; a kept island is reached and fenced the same way. Without it a kept island could run past the later event's tick (class X; t11's first patch, seeds 425 and 990).

**Why the old assertions were the re-mint's behaviour, not physics.** The event-path tests assert that an edit always replaces the island it touches (t11 counted four; a run with the rule shows five, section 4). Before option K the recompile re-minted every junction, so even an edit that changed nothing about the island produced a different island, and replacing it was the only consistent action. Under K, an edit that changes nothing leaves the island alone. The restated assertions are in section 3.

### 2. Rule two: a kept junction the new flows displace within the first step takes the start rule (A140), stated before coding

**Real life.** World 107 (defaults): a pipe joins a 1 atm water generator 2 m above a line of junctions full of nitrogen at rest. The water falls about 20 kPa into them at roughly 5 kg/s. In the model a junction owns a fixed mass `m_J` (D25; the reconstruction pins it), so what flows in replaces what it holds at the mass inflow rate: a 12 g nitrogen holdup is replaced in about 2.4 ms. The first step of a replaced island is one tick (50 ms). By the end of that step the junction holds what arrives; what it held at the edit is gone. Keeping the nitrogen as the step's start puts the solver at the wrong end of a transient that is over before the step ends (trial states outside the data: nitrogen above 900 K, water below 273.16 K).

**The criterion.** Every junction K keeps is tested against what the start rule estimates for it.
- The inflow estimate `Q_in`: the start rule's (m') balance at the junction's balance pressure `P`, the sum of the open suppliers' mass flows `q_i(P)` (each through its own path's law, a pumped stream at its fixed flow). `junctionStarts` already computes it; it is read for kept junctions as for new ones.
- The displacement time `t_d = m_J / Q_in`, with `m_J` the kept holdup's mass (fluid and solids, the mass the reconstruction pins).
- The first step `dt`: a replaced island starts cold with a one-tick slice, 0.05 s, which is also D25's holdup time constant `tau` (`PassiveStepSolver.HOLDUP_TAU`). One constant serves both.
- **Displaced when `t_d < tau`**, that is when `Q_in x tau > m_J`: more arrives in the first step than the junction holds. A displaced junction is minted as a new junction: the start rule's state, its holdup sized on that state by D25, the old holdup booked as destroyed and the new one as constructed (exact by construction, as the re-mint booked every junction). Otherwise it keeps its contents as K states.
- No inflow, no displacement: a pocket (it drains nowhere: what reaches it only compresses what it holds), a junction nothing reaches, a balance with no supplier open at `P`, and a balance that falls back to the strongest stream (no estimate) keep their contents.

**Why the threshold is at `t_d = dt`.** The solver advances a junction by its backward-Euler vessel row, `(m_J/dt + Q) w = (m_J/dt) w_old + sum q w_e`. After the first step the share of what it held is `1 / (1 + dt/t_d)`: one half exactly at `t_d = dt`. Below it the step ends nearer the arrival than the kept contents, so the arrival is the better start; above it the kept contents are. At equality the rule keeps (strict, as step 5 calls a direction driven). The comparable band is A155, with its options.

**What it reads, in the model's terms.** D25 sizes `m_J = tau x (rho v)_seed x A_widest`. A junction that carries the fluid it was sized on, through one connection, at or below the velocity cap, has `Q_in <= m_J / tau`: it is never displaced. The criterion fires where a denser fluid arrives in a holdup sized on a lighter one (water into a gas-sized junction, world 107), or where several streams converge above the junction's sizing flow. The physical block's volume is not the model's holdup (D25); the criterion follows the holdup the solver advances.

**What it absorbs.** A147's proxy ("a junction whose compiled connections changed gets the start rule", t11's variant strict) is not the physics: a junction that gains a dead-end neighbour keeps its contents, and one whose connections did not change can still be flooded by a supplier the edit opened further up the line. The criterion replaces it.

**Order of the two rules.** Rule one reads the island with K's kept junctions placed, before the criterion: an island the edit leaves identical has no new flows, and the criterion is not asked. A changed island is minted: the criterion for its kept junctions and the start rule for its new ones, from one start-rule pass.

### 3. As implemented (`fe9ec799` rule one, `67a329f8` rule two)

The code follows sections 1 and 2 with no deviation.
- `PassiveNetwork.keepJunctions` is K's keep step on its own (the first half of the old `mintJunctions(graph, model, held)`, which now calls it).
- `PhysicalRegistry.apply` builds each recompiled island with its kept junctions and compares it with every affected island of the same members (`IslandCoordinator` snapshot graph equality). An identical one is not replaced: its junction keys count as carried (nothing booked), its fences of the batch's events are released before the change is applied, the change replaces only the other affected islands, and members are unbound only for those. The later-event fencing after a batch covers kept islands as well as replacements (t11's class X fix).
- `NetworkReduction.junctionStarts` returns, for a junction already sized (kept), null ("keeps") unless the balance's inflow times `HOLDUP_TAU` exceeds its owned mass; `balanced` now returns its state with the sum of the open suppliers' flows (`Balance`); the "nothing reaches" fallback no longer touches kept junctions.
- `PassiveNetwork.mintJunctions(graph, model)` runs the start rule once whenever an island has a junction, puts a displaced junction back unsized with its start state, and sizes every unsized junction as before.

Absorbed: the "connections changed" proxy (A147, a patch only; dropped). The five event-path assertions "an edit replaces the island it touches" (section 4). Not absorbed: D10, the start rule's contact case, A97 as a family (section 5).

### 4. The restated tests (`PhysicalRegistryTest`, `DeadEndPruningTest`)

Each old assertion stated what the re-mint forced: before option K a recompile minted every junction again, so an edit that changed nothing still produced a different island. In real life such an edit leaves the island as it was, and under K and rule one so does the code.

| Test | Edit | Old assertion | New assertion |
|---|---|---|---|
| `PhysicalRegistryTest.anEventTouchingOneIslandCompilesAndReplacesOnlyThatIsland` | a dead-end pipe beside the first line's first pipe | `assertNotEquals(old, replacement, "a replaced island gets a new identity")`, revision + 1 | the line keeps its identity and its revision; then a tank at the branch's end (a real change: the first pipe becomes a junction) is compiled (7 devices) and replaces only that island, new identity, revision + 1 (the old assertions, now on a change) |
| `PhysicalRegistryTest.removalAndEditFollowTheSamePath` | a line's pipe turned to face north (its connections do not change) | `assertNotEquals(untouched, owner(other), "an edit replaces its island")`, revision + 1 | `assertEquals(untouched, owner(other), "an edit that leaves the island as it was keeps it")`, the same revision; the same 5 devices compiled (the same path) |
| `PhysicalRegistryTest.anEventReachesAnIslandThroughAnOwnerlessBranch` | four dead-end pipes off the line | `assertNotEquals(island, owner(tank), "a branch touches the line's island")` | `assertEquals(island, owner(tank), "a dead-end branch touches the line's island and leaves it as it was")`; the tank at the branch's end still reaches and joins the island through the ownerless branch |
| `PhysicalRegistryTest.certifiedIslandsUntouchedByAnEventStayCertified` | a dead-end pipe on a certified line | `assertTrue(replacement.certificate().isEmpty(), "a topology change ends the touched island's certificate")` | the touched island is materialised (one materialisation, unchanged) and keeps its identity and certificate; then a tank at the stub's end replaces it and the replacement starts uncertified (the old assertion, now on a change) |
| `DeadEndPruningTest.aFilterBranchOnATanksJunctionIsPrunedKeepsItsCakeAndRejoinsWithADestination` | a dead-end pipe on the tank's line | `assertNotEquals(line, owner(tank), "the edit replaced the tank's island")` | `assertEquals(line, owner(tank), "the edit left the tank's island as it was")`; the dormant filter's island and cake as before |
| `PhysicalRegistryTest.junctionHoldupsAreBookedAsConstructedAndKeptAtATopologyEdit` (A151) | first edit: a dead-end stub; second edit: a block at the stub's end | first: `assertNotEquals(owner, owner(tank), "the edit replaced the island")`; second: a 1 atm nitrogen void | first: `assertEquals(owner, owner(tank), ...)` and the committed interval kept (`lastResult` equal); second: a 150 kPa methane generator (the island rests at 150 kPa, no new flow: the branch junction is kept, 1.2e-12 kg/s against 6.5 g). With the void the new rule displaces it (0.24 kg/s through 6.5 g, 27 ms < 50 ms), so that case moved to `JunctionKeepTest` |

t11 counted four event-path tests; the run with the rule fails five (`q01`): the four of `PhysicalRegistryTest` and the one of `DeadEndPruningTest`. Its printed line is unchanged ("keep: junction 3 minted at 102430.67546830291 Pa, solved to 149999.99999999988 Pa, kept; new junction 2 minted").

`JunctionKeepTest.aKeptJunctionTheNewFlowDisplacesWithinTheFirstStepStartsAsWhatArrives` states rule two at the science level: a junction resting full of nitrogen (11.2 g) that a pipe joins to 1 atm water 2 m up takes 13.7 kg/s by the balance, `t_d` 0.82 ms: it starts as the start rule's water, bit for bit the fresh mint; under a level nitrogen supply the same junction keeps its state and inventory.

### 5. Campaign (`q04`, 8 workers, 1000 seeds per distribution; each rule alone through the switch `q07`; worse seeds and budget holds alone `q05`)

Conclusion: with both rules K gains 2 random seeds over `o02` and loses 2 world-default seeds (921 / 881 against 919 / 883); against K alone it gains 6 / 7. Rule two is the larger part; rule one restores the default Newton work to `o02`'s level.

| Campaign | random | defaults | Newton iter. r / d | Newton solves r / d | gate refusals r / d |
|---|---|---|---|---|---|
| `o02` (t10, the re-mint) | 919 | 883 | 2.96 M / 19.9 M | 1.11 M / 2.27 M | 204 k / 161 k |
| `p03` K (t11) | 915 | 874 | 6.39 M / 8.49 M | 1.60 M / 1.40 M | 184 k / 186 k |
| `q07` K through the switch (check) | 915 | 874 | 6.30 M / 8.46 M | 1.59 M / 1.39 M | 184 k / 186 k |
| `q07` K + rule one | 915 | 877 | 6.40 M / 18.2 M | 1.60 M / 2.36 M | 187 k / 158 k |
| `q07` K + rule two | 922 | 879 | 3.47 M / 8.77 M | 1.43 M / 1.39 M | 209 k / 170 k |
| **`q04` K + both rules (`67a329f8`)** | **921** | **881** | **3.40 M / 18.2 M** | **1.39 M / 2.70 M** | **208 k / 239 k** |

- Against `o02`: random 10 cleared (E6 65 188 257 681 824 853 915; E2 372 854; E5 629) / 8 worse (E6 107 456 660 719 794; E2 274 855; C 94). Defaults 11 cleared (E6 108 401 444 518 719 736; E10 61; E4 669; E5 853; E8 629; F 156) / 13 worse (E6 183 241 246 424 654 758 842; C 459 1000; E10 333; E4 178; E5 916; E7 573).
- Against K (`p03`): random 10 cleared (E6 9 78 615 654 824; C 108 219 505 621; E10 895) / 4 worse (E6 173 244 529 660). Defaults 8 cleared (E6 107 444 446 621 660 736; E10 61; E8 665) / 1 worse (E6 357).
- Alone (`q05`, one worker): every worse seed and budget hold holds again in its class, except random 246 (F in the campaign, clean alone); defaults 116 is F in the campaign and E8 alone (as in `p11`).
- Classes `o02` -> `q04` (random / defaults): C 15 -> 17 / 5 -> 9; E2 9 -> 8 / 13 -> 16; E3 0 / 3 -> 1; E4 0 -> 1 / 7 -> 4; E5 6 -> 5 / 14 -> 12; E6 30 -> 27 / 44 -> 50; E7 3 -> 2 / 3 -> 3; E8 6 -> 6 / 12 -> 10; E9 3 -> 4 / 6 -> 6; E10 4 -> 4 / 4 -> 4; F 5 -> 5 / 6 -> 4.

**The first-interval family (A97, `first-interval.js`, `q06`)**:

| | random: family / held | defaults: family / held |
|---|---|---|
| `o02` | 42 / 81 | 32 / 117 |
| K (`p03`) | 48 / 85 | 40 / 126 |
| **both rules (`q04`)** | **43 / 79** | **31 / 119** |

Against K the family loses 11 random seeds (9 of them now clean) and gains 6; in defaults it loses 10 (7 now clean) and gains 1. It is back to `o02`'s size; what remains are islands formed for the first time and the residual worlds of section 7.

### 6. Where K's random Newton work went (`q08`, one worker, per-seed counters)

Conclusion: the doubling was one world. Rule two removes it; rule one does not touch it.

| Form (one worker) | random Newton iter. | of which seeds with a hold | of which clean seeds |
|---|---|---|---|
| K | 6.32 M | 5.30 M | 1.02 M |
| both rules | 3.35 M | 2.60 M | 0.74 M |

- Of the 2.97 M difference, random seed 922 is 2.65 M: a pumped island (three pumps, two valves, a filter, two water generators, a nitrogen generator; a pump and the two water generators placed last, at ticks 1126-1151). Which junction rule two re-mints there was not traced. Under K it holds 117 times on the budget (each retry a full 5 s interval at the substep limit); with rule two it holds twice (a typed thermo-domain wait, as in `o02`) at 1 251 iterations. Then 419 (clean in both; 0.39 M against 0.04 M), 298, 431 and 118, the same kind.
- By the switch (`q07`): K plus rule one 6.40 M, K plus rule two 3.47 M.
- Defaults move the other way: K 8.4 M, K plus rule one 16.4 M (one worker). Seed 622 is 6.0 M of the 8.0 M difference: an island held in every form (`o02` 80 holds, K 21, rule one 115) that retries every 5 s at about 1 000 substeps; under K it ends in a typed thermo-domain wait, which parks instead of retrying. Clean default seeds cost 0.49 M (K) and 0.45 M (rule one).
- The counters measure retries of a few persistently held worlds more than the work of running ones. Per form, in clean seeds, both rules cost less than K.

### 7. The traced worlds (minimized replays `q09`, `q10`, `q13`; full seeds from the campaigns)

| Seed (defaults) | full seed: `o02` / K / rule one / rule two / both | minimized world, both rules | minimized world, other forms |
|---|---|---|---|
| 246 (t11's 7 blocks, a dead end on a line in counterflow) | clean / E6 / E6 / E6 / E6 | 120 slices | rule one off: holds at tick 657 (4.4e-4); K: the same |
| 107 (t11's 13 blocks, water falling into nitrogen junctions) | clean / E6 / E6 / clean / clean | 120 slices; three junctions displaced (11.2 g, 6.7-7.4 kg/s, 1.5-1.7 ms) | rule two off: holds at tick 268; K: the same |
| 156 (t11's gate world) | F / clean / clean / clean / clean | 120 slices | K: 120 slices |
| 992 (t9's 13 blocks) | clean in every form | 120 slices | K: 120 slices |
| 424 (t9's 11 blocks) | clean / E6 / E6 / E6 / E6 | holds at tick 167 | K: the same |
| 459 (t9's 14 blocks) | clean / C / C / C / C | holds at tick 628 | K: the same |

The full seed 246 holds later than its minimized world: at tick 1761 a pipe makes a real new connection (not a dead end). Contrast-minimized with both rules holding and the re-mint clean (`q12`, `q13`):
- **246** (13 blocks): the new connection opens the counterflow line to a second nitrogen tank above. A kept junction holds the counterflow's real water-nitrogen mixture (263 g, 19.6 kg/m3); the balance estimates 42 g/s through it, `t_d` 6.2 s: kept. The changed island restarts cold on that state and holds (E6, Newton limit 1.3e-3). The re-mint starts the junction as nitrogen.
- **424** (8 blocks): a kept junction full of water (9.8 kg, nothing driven into it) beside a nitrogen junction; a pipe joining the generator a second way makes the first interval cycle (active-set cycle, Newton limit 0.69). The re-mint starts the water junction as nitrogen.
- **459** (14 blocks): an envelope hold 427 ticks after the edit (4.0 MPa); no kept junction is reached by a supplier at the edit.

In real life all three junctions hold what K keeps. The holds are the cold start of a changed island on a real, near-zero-drive state (the E6 family), not a start-rule item; the re-mint avoided them by replacing real contents with an estimate (A162).

### 8. References (`q14` on `caf0c129`, against t11's `p16`; nothing re-recorded)

| Reference | `p16` (t11) | both rules |
|---|---|---|
| fluid suites | 521 tests, 0 failures | 524, 0 (521 + `JunctionKeepTest`'s new test + the two gate worlds) |
| 33 junction lines | as `o12` | identical, line for line |
| MIXED_GAS_COST | 309 | 309 |
| chain-100 | moles 6.049e-15, 1.705e-13 K, flow 1.896e-11 | identical |
| adjacent selection | 45 / 45 | 45 / 45 |
| printed suite lines | 6072 | 6073 (`q14-vs-p16-printed-diff.txt`) |
| GameTest outlets | 124258.48752327723 Pa / 175.76227454727493 kg | identical |
| GameTest compressor | 2.999985907556435 / 157976.31408415743 Pa | identical |

Moved printed lines: `InlineFilterLineHoldTest`'s last F8 island prints node identities 9, 13, 17 and island 18 instead of 11, 17, 23 and 24, at identical pressures, temperatures and densities (kept islands allocate no identities, so later ones are numbered lower); the new line is `JunctionKeepTest`'s "displaced: 0.011244477629123572 kg of nitrogen, new holdup 542.7831464123992 mol". The keep test's own line is unchanged.

### 9. Gate tests and their proofs (`caf0c129`, `q11`)

| Test | World | Both rules | Proof (working tree only, `src/scratch-t12-switch.patch`) |
|---|---|---|---|
| `JunctionKeepsContentsTest.aDeadEndPlacedOnALineInCounterflowLeavesTheIslandAsItWas` (rule one) | defaults 246 minimized, 7 blocks: a water generator, a junction with a nitrogen tank level and a second 1 m below in slow counterflow; a dead end placed at tick 655 | the island keeps its identity; 120 slices | replacement restored (`-Dt12mode=replace`): holds at tick 657, "Newton iteration limit at residual 4.3682623433004573E-4" (`q11-gate-replace-result.xml`) |
| `JunctionKeepsContentsTest.waterFallingIntoRestingNitrogenJunctionsDisplacesThemWithinTheFirstStep` (rule two) | defaults 107 minimized, 13 blocks: three junctions and a loop resting full of a void's nitrogen; a pipe joins a 1 atm water generator 2 m up at tick 266 | 120 slices | criterion disabled (`-Dt12mode=nodisplace`): holds at tick 268, "Newton iteration limit at residual 0.04468436098724412" with nitrogen above 900 K and water below 273.16 K (`q11-gate-nodisplace-result.xml`) |

t11's gate world (`aDeadEndPlacedBesideARestingJunctionLeavesItsNitrogenWhereItIs`) passes in every mode: its dead end now leaves the island unchanged, so it is kept by rule one as well as by K.

### 10. Final gates (`q14`, on `caf0c129`, GameTest run id `t12-q14-caf0c129-run1`)

| Gate | Result |
|---|---|
| G1 fluid suites | 524 tests, 0 failures; 33 junction lines identical; MIXED_GAS_COST 309; printed lines as section 8 |
| G2 exact regression | red only on chain-100, with `o12`'s values |
| G3 adjacent selection | 45 / 45 |
| G5 fluid GameTests | 33 / 33; outlets and compressor values identical to `p16`, so one run |

### 11. Stop points, with recommendations

- **Rule one (A152) and the restated tests (A153):** physics forces the rule (nothing moved, nothing restarts). Recommended. The owner confirms the five restated assertions and the keep test's changed second edit.
- **Rule two (A154) and its band (A155):** the criterion follows the solver's own junction row; the threshold `t_d = dt` is where the first step ends half and half. Recommended as (a). Options (b) `dt / ln 2`, (c) a mixed start, (d) a margin band: not measured; the measured worlds sit far from the line (1.5 ms, 27 ms, 66 ms against 50 ms).
- **A156 (a displaced junction is a new one, D25 on its start):** recommended; the other option keeps a gas-sized holdup for water.
- **A143 (re-size basis, t11):** unchanged; the mint's seed.
- **A162 (what K still loses):** three default worlds in which the solver cannot cold-start a changed island on a real near-zero-drive state. Not a start-rule item; it belongs to card E6 (the cold start of a re-formed island). Recommendation: keep K and follow it there, not by re-minting real contents.
- **Take the branch?** Recommended, with both rules: K is the real first instant; with the two rules it holds `o02`'s random count and loses 2 default seeds net (921 / 881 against 919 / 883), all three traced residual losses being real states the re-mint used to delete; the first-interval family is back at or below `o02`'s; random Newton work is back to `o02`'s level and default work at it.

### 12. What differs from the brief

- t11 counted four event-path tests; five assert the replacement (four in `PhysicalRegistryTest`, one in `DeadEndPruningTest`), plus the first edit of the keep test (A151). All are restated.
- The keep test's second edit had to change: its 1 atm void drives 0.24 kg/s through a 6.5 g junction (27 ms), which rule two now displaces. It is a 150 kPa generator now; the displacement case moved to `JunctionKeepTest`.
- World 246 at full seed is not cleared: the minimized world is (rule one), the full seed holds later on a real connection (section 7).
- Commit trailers name the model that wrote them (Claude Opus 5.5), not Claude Fable 5.1.
- Documents are not copied to the main checkout (own worktree only).

## Hold class E6 on the final code, by mechanism (2026-10-13)

- Author: Claude (Opus 5.5, thread t13 of the orchestrator), worktree `remove-agents-attribution-line-3d0c97`, side branch `claude/e6-cards` (from `claude/keep-contents` `9fd8e5e1`, the code of campaign `q04`). Diagnosis only: no solver code is committed; every lever below is scratch, working tree only. Decisions A163 onwards (`DECISION_LOG.md`). Cards: `REMAINING_HOLDS.md`, E6-T, E6-S, E6-B, E6-C and the "Distinct defects remaining" table. Material: `tools/phase-ports-probes/hold-classes/` logs `y02`-`y26` (the prefix `r` is taken by class R), `src/instr-t13-failing-rows-and-levers.patch`, `src-t13/`, `t13-replay.sh`, `t13-classify.js`.

### 1. Conclusion

Class E6 on `q04` (defaults 50, random 27) is four mechanisms, not one.
- **T, 17 / 3: a trace gas phase in free water.** The worst row is a water junction's (or tank's) water-vapour saturation row; the gas phase holds a share of 1.8e-19 to 6.0e-6 of the water's moles, all below nitrogen's solubility (about 1.1e-5 at 1 atm). In real life it is dissolved. The same mechanism holds the six sampled default worlds of E2, E5 and E8. It waits for gas solubility in water (D35).
- **S, 24 / 15: the step starts its flows from a static estimate.** Every island job forgets the flows (`replayStart`), and the per-connection estimate reads a different column than the flow row on vertical gas-water connections, so a connection at rest starts at its cap. The Newton walks the flow down by halving and runs out of iterations; the residual does not depend on the step, so refinement cannot help.
- **B, 9 / 3: a new island's first solve keeps forbidden boundary directions open** (a void supplying, a generator receiving), because the start-of-solve closure skips a first solve.
- **C, 0 / 6: a junction between velocity-capped gas connections**, whose capped rows are flat in its pressure; the Newton oscillates.

With a 60-iteration Newton (a diagnostic, A165) 44 of the 77 seeds run clean: S 26 of 39, B 9 of 12, C 6 of 6, T 3 of 20. S, B and C are Newtons that converge too slowly from their start; T is one that does not converge. Each card world is proved by its own lever (section 5). The real number of distinct defects behind E6, E5, E2 and E8 in world defaults is four, with T shared by all four classes.

### 2. Method

- **Trace** (`src/instr-t13-failing-rows-and-levers.patch`, A163). Every failed Newton of a step pass is reported to a per-thread sink with its residual history (each accepted iterate's norm, whether its Jacobian was fresh or a chord, the step length, the worst row), the worst eight rows named by equation, each node's state and seed, each connection's flow, drive, loss, scale and cap drops, both columns of a vertical connection, the start point's large rows, every connection's sign changes and the trajectories of the three worst rows. `T13TraceProbeTest.trace` replays a seed as `HoldFuzzTest.run` does, one worker, to its first hold, and keeps the reports of the hold tick only.
- **Reproduction.** All 77 first holds replay with the campaign's tick, committed tick and status text to the last digit, with every version of the instrumentation (`y03-trace-summary.txt`: 77 SAME).
- **Group rule** (A164), from the last iteration-limit report of the hold tick (the smallest step): T if the worst row is `wsat` or `pclose`; B if the island is in its first second and a connection flows the way its boundary forbids; C if the worst connection is on its cap in three quarters of the iterates; S otherwise.
- **When.** "first": the island's first interval (its committed tick is the one it was formed with); "+Nt": N ticks after it formed (its second job, after a short first slice); "+Ns": later.

### 3. The classification (log `y13-classification.md` has every feature; `y03-trace/` every trace)

Residual: the Newton's residual at its 20th iteration at the smallest step. Ends: G generator, V void, R tank, J junction; g gas, l oil, w free water; q the flow (kg/s), d the drive (Pa) of the worst connection; MIXED: its two columns drive opposite ways. The last column: the seed replayed alone with 60 iterations and with the levers L1, L2, L3 and all three (section 4); "clean" runs its 120 slices, "held" holds as in the campaign, "moved" holds elsewhere.

| Seed | Dist. | Blocks | Tick | When | Group | Row | Residual | Ends / node at the limit | 60 it. / L1 / L2 / L3 / all |
|---|---|---|---|---|---|---|---|---|---|
| 17 | def | 22 | 2829 | +61s | T | wsat | 0.0341 | R:gw P=91735 vap=2.827e-16 | clean / held / moved / clean / clean |
| 39 | def | 17 | 2136 | +11s | T | wsat | 0.243 | J:gw P=101304 vap=1.243e-15 wat=0.0007579 | moved / moved / moved / moved / moved |
| 123 | def | 33 | 2411 | first | T | wsat | 0.0476 | J:gw P=108962 vap=2.425e-14 | moved / moved / moved / moved / moved |
| 145 | def | 25 | 2364 | +7s | T | wsat | 1.78 | J:gw P=102162 vap=9.689e-09 wat=0.0007395 | moved / held / moved / moved / moved |
| 271 | def | 23 | 1613 | +3s | T | wsat | 9.18 | J:gw P=102227 vap=9.768e-13 | moved / held / moved / clean / clean |
| 402 | def | 20 | 875 | +14t | T | wsat | 0.00582 | J:gw P=101338 vap=2.093e-07 wat=0.0007395 | moved / moved / moved / clean / clean |
| 431 | def | 25 | 2110 | +11s | T | wsat | 1.09 | J:gw P=100985 vap=3.114e-17 wat=0.0001254 | clean / moved / moved / moved / moved |
| 592 | def | 18 | 7428 | +271s | T | wsat | 9.52 | J:gw P=103463 vap=0.0006874 wat=0.9685 | moved / held / moved / moved / moved |
| 616 | def | 21 | 1443 | +1t | T | wsat | 3.02 | J:gw P=101347 vap=3.989e-11 wat=0.009817 | moved / moved / moved / moved / moved |
| 654 | def | 33 | 2718 | first | T | wsat | 2.71 | J:gw P=101359 vap=6.921e-15 | moved / held / moved / held / moved |
| 678 | def | 23 | 2026 | first | T | wsat | 0.474 | J:gw P=106024 vap=7.202e-16 wat=0.006117 | moved / moved / moved / moved / moved |
| 773 | def | 25 | 4425 | +107s | T | wsat | 0.393 | J:gw P=101458 vap=3.500e-10 | moved / moved / moved / moved / moved |
| 839 | def | 19 | 2755 | first | T | pclose | 0.000116 | J:gw P=120860 vap=0.0002019 | moved / held / moved / moved / moved |
| 844 | def | 11 | 2067 | +46s | T | wsat | 8.76e-7 | J:gw P=103767 vap=1.000e-12 | moved / held / moved / moved / moved |
| 929 | def | 18 | 1206 | +14t | T | wsat | 0.403 | J:gw P=100636 vap=6.395e-06 wat=0.0007610 | moved / moved / moved / clean / clean |
| 937 | def | 26 | 1202 | first | T | wsat | 0.583 | J:gw P=97150 vap=5.980e-15 | moved / moved / moved / moved / moved |
| 961 | def | 22 | 3216 | +70s | T | wsat | 0.696 | R:gw P=109916 vap=1.070e-11 | moved / moved / moved / moved / moved |
| 430 | rnd | 12 | 1979 | +61s | T | wsat | 14.3 | J:glw P=183667 vap=4.254e-15 wat=0.9119 | clean / moved / held / clean / clean |
| 511 | rnd | 23 | 2458 | +51s | T | wsat | 2.02e-7 | J:gw P=123168 vap=2.958e-11 | moved / held / moved / moved / moved |
| 752 | rnd | 24 | 1575 | +2s | T | wsat | 0.0446 | R:gw P=298070 vap=1.488e-13 | moved / moved / moved / moved / moved |
| 29 | def | 29 | 2922 | first | S | flow | 0.000116 | R:gw>J:gw q=-0.0307 d=-5.69 | clean / held / clean / moved / moved |
| 173 | def | 18 | 887 | +2s | S | flow | 0.0000811 | R:gw>J:gw q=0.00625 d=-0.197 | moved / moved / moved / moved / moved |
| 175 | def | 24 | 2193 | +21s | S | flow | 3.92e-8 | R:gw>J:gw q=0.0669 d=2.87 | clean / moved / moved / moved / moved |
| 183 | def | 24 | 2373 | +6s | S | flow | 1.67e-8 | V:g>J:gw q=0.0135 d=17.4 dz=2 MIXED | moved / moved / moved / moved / moved |
| 220 | def | 24 | 2432 | +6s | S | flow | 0.00000193 | V:g>J:gw q=0.0226 d=14.8 dz=1 MIXED | clean / moved / moved / moved / clean |
| 241 | def | 15 | 1374 | +2t | S | flow | 0.00000338 | J:gw>J:g q=-0.000494 d=-0.0667 dz=1 | clean / held / moved / moved / moved |
| 246 | def | 36 | 1776 | +6t | S | flow | 0.0000129 | J:gw>J:gw q=-0.000489 d=0.0373 dz=2 MIXED | clean / moved / clean / clean / clean |
| 298 | def | 24 | 2425 | +2s | S | flow | 0.00637 | R:gw>J:gw q=0.0337 d=62.1 | moved / moved / moved / moved / moved |
| 352 | def | 27 | 2104 | +6t | S | flow | 0.000611 | J:gw>V:g q=0.0417 d=12.1 | moved / moved / moved / moved / moved |
| 357 | def | 27 | 4129 | +76s | S | flow | 9.79e-9 | R:gw>J:gw q=0.111 d=95.9 | clean / moved / moved / clean / clean |
| 397 | def | 18 | 3326 | +86s | S | flow | 0.000203 | R:gw>J:gw q=0.0206 d=19.4 VAPOR/BULK dz=2 | moved / held / moved / moved / moved |
| 424 | def | 18 | 1271 | first | S | flow | 0.0000122 | J:g>J:g q=-0.00149 d=-0.0949 | moved / held / moved / moved / moved |
| 529 | def | 15 | 1597 | +14t | S | flow | 0.0000641 | J:gw>R:gw q=0.0378 d=11.9 | clean / moved / moved / clean / clean |
| 547 | def | 18 | 1841 | +2s | S | flow | 4.55e-7 | J:gw>R:g q=0.358 d=54.4 BULK/LIQUID dz=2 MIXED | clean / held / clean / clean / clean |
| 606 | def | 23 | 2131 | +14t | S | flow | 0.0000143 | J:gw>R:g q=-0.000801 d=-0.0519 BULK/LIQUID dz=3 | moved / held / moved / moved / moved |
| 720 | def | 22 | 2568 | +26s | S | flow | 0.000168 | V:g>J:w q=-0.0205 d=-1.35 | moved / moved / moved / moved / moved |
| 758 | def | 23 | 3758 | +77s | S | flow | 0.000133 | J:gw>R:gw q=0.0136 d=-0.222 BULK/VAPOR dz=-1 MIXED | moved / moved / moved / moved / moved |
| 768 | def | 26 | 2193 | first | S | flow | 4.45e-9 | J:g>J:g q=0.00847 d=4.61 | moved / held / moved / moved / moved |
| 786 | def | 13 | 1298 | +6t | S | flow | 7.12e-7 | J:gw>J:gw q=0.199 d=11.8 dz=1 MIXED | clean / held / moved / clean / clean |
| 886 | def | 25 | 1597 | +6t | S | flow | 0.00000226 | R:g>J:gw q=-0.000263 d=-0.0553 dz=-1 MIXED | clean / moved / clean / clean / clean |
| 915 | def | 22 | 4068 | +56s | S | flow | 0.00000708 | R:gw>J:gw q=-0.0460 d=-0.802 VAPOR/BULK dz=1 | clean / held / moved / moved / moved |
| 932 | def | 24 | 4636 | +91s | S | flow | 0.00000356 | V:g>J:gw q=-0.00565 d=-0.0744 | clean / moved / moved / moved / moved |
| 967 | def | 23 | 4002 | +71s | S | flow | 0.0000383 | V:g>J:w q=0.0337 d=30.8 | clean / moved / moved / clean / clean |
| 981 | def | 28 | 2101 | +2t | S | flow | 8.54e-8 | V:g>J:gw q=-6.35 d=-3.31e+3 dz=-1 MIXED | clean / held / moved / moved / moved |
| 19 | rnd | 30 | 2613 | +3t | S | flow | 0.00000947 | R:gl>J:gl q=-0.0666 d=-1.08 VAPOR/BULK dz=1 | clean / held / moved / moved / moved |
| 92 | rnd | 25 | 2967 | first | S | flow | 4.42e-9 | V:g>J:w q=-1.73 d=-2.80 | clean / held / held / held / held |
| 98 | rnd | 28 | 11964 | +481s | S | flow | 1.34e-7 | J:gw>R:gw q=-0.0899 d=-2.74e+3 dz=-1 | clean / held / moved / clean / clean |
| 107 | rnd | 22 | 1905 | +14t | S | flow | 9.13e-8 | J:gw>J:gw q=-0.0134 d=-0.471 dz=1 | clean / held / clean / clean / clean |
| 118 | rnd | 27 | 2395 | +6t | S | flow | 0.00000634 | R:gl>J:gl q=-0.0346 d=-138 VAPOR/BULK dz=1 | moved / moved / clean / moved / moved |
| 151 | rnd | 21 | 2845 | first | S | flow | 0.383 | J:g>J:g q=-0.0622 d=-771 dz=1 | clean / held / moved / moved / moved |
| 244 | rnd | 28 | 2024 | first | S | flow | 0.000145 | J:gl>V:g q=0.000306 d=-1.05 | clean / held / held / held / held |
| 600 | rnd | 34 | 2715 | first | S | flow | 0.0234 | J:g>J:g q=0.00712 d=-21.7 | clean / held / held / held / held |
| 633 | rnd | 19 | 3719 | +61s | S | flow | 0.0000354 | J:gl>J:gl q=-0.0393 d=-1.10e+3 dz=1 | moved / moved / moved / clean / clean |
| 660 | rnd | 28 | 1825 | first | S | flow | 0.00000518 | J:g>J:g q=0.000116 d=0.0886 | clean / held / held / moved / moved |
| 794 | rnd | 25 | 1210 | first | S | flow | 0.0000132 | J:g>J:g q=-0.00956 d=-1.88e+4 | clean / held / moved / moved / moved |
| 842 | rnd | 25 | 1112 | first | S | flow | 1.99e-8 | J:g>J:g q=-0.0661 d=-8.47 | clean / held / moved / moved / moved |
| 886 | rnd | 25 | 1585 | first | S | flow | 0.000236 | J:g>J:g q=-0.00911 d=-105 | moved / held / held / held / held |
| 916 | rnd | 28 | 1341 | first | S | flow | 0.0157 | J:g>J:g q=-0.00327 d=-579 | clean / held / moved / held / moved |
| 937 | rnd | 26 | 1150 | first | S | flow | 0.00349 | J:w>J:w q=-0.369 d=-5.37e+4 dz=1 | clean / moved / held / held / moved |
| 228 | def | 11 | 835 | first | B | flow | 0.0000812 | J:w>G:w q=-6.38 d=-1.10e+3 | moved / clean / held / held / clean |
| 336 | def | 25 | 2217 | first | B | flow | 0.104 | J:g>V:g q=0.0652 d=-427 dz=-1 | clean / moved / moved / moved / moved |
| 475 | def | 26 | 1403 | first | B | flow | 0.0000923 | J:g>J:g q=-0.00300 d=-0.399 | moved / moved / moved / held / moved |
| 563 | def | 29 | 1831 | +2t | B | flow | 0.00000115 | J:g>J:g q=0.00438 d=3.19 dz=1 | clean / held / moved / moved / moved |
| 600 | def | 34 | 3045 | first | B | flow | 0.0000726 | J:g>J:g q=-0.0133 d=-9.97 | clean / moved / moved / moved / clean |
| 651 | def | 21 | 995 | first | B | flow | 0.00000134 | J:g>J:g q=0.0000670 d=0.000592 | clean / clean / moved / clean / clean |
| 824 | def | 24 | 2434 | first | B | flow | 0.0000353 | J:g>V:g q=0.00336 d=0.425 | moved / moved / moved / moved / moved |
| 842 | def | 25 | 1298 | first | B | flow | 0.000938 | J:w>J:w q=0.723 d=0.0159 | clean / clean / moved / held / clean |
| 951 | def | 18 | 524 | +7t | B | flow | 0.0000556 | V:g>J:w q=-0.135 d=-0.859 | clean / moved / moved / clean / moved |
| 460 | rnd | 17 | 1801 | first | B | flow | 0.728 | J:g>V:g q=-0.00576 d=3.60e+4 | clean / held / held / held / held |
| 499 | rnd | 24 | 2320 | first | B | flow | 0.374 | J:g>J:g Compressor q=0.00976 d=3.49e+4 | clean / clean / moved / moved / moved |
| 719 | rnd | 24 | 1284 | first | B | flow | 1.03e-7 | V:g>J:w q=0.0807 d=325 | clean / clean / moved / moved / clean |
| 173 | rnd | 18 | 1060 | first | C | flow | 0.538 | J:g>J:g q=0.164 d=-1.41e+4 dz=-1 | clean / moved / held / moved / moved |
| 286 | rnd | 16 | 1900 | first | C | flow | 3.34 | R:g>J:g q=0.0176 d=-1.64e+5 | clean / held / held / held / held |
| 336 | rnd | 25 | 2146 | first | C | flow | 0.00245 | J:g>V:g Compressor q=0.00899 d=770 | clean / moved / moved / moved / moved |
| 456 | rnd | 30 | 2680 | first | C | flow | 0.239 | J:g>J:g q=-0.324 d=-2.62e+3 | clean / held / held / moved / moved |
| 529 | rnd | 15 | 1159 | first | C | flow | 0.651 | J:g>R:g q=0.280 d=-6.70e+3 BULK/LIQUID dz=1 | clean / held / held / moved / moved |
| 594 | rnd | 20 | 2016 | first | C | flow | 0.545 | J:g>R:g q=-0.00953 d=7.44e+3 BULK/VAPOR | clean / held / held / moved / moved |

Timing per group: T 8 within the island's first second, 12 later (2 s to 271 s); S 24 within the first second, 15 later (2 s to 481 s); B 12 and C 6 all within the first second. Contraction (the geometric mean of the last five iterations): T 0.15 to 1.6, stalled or rising in 8 of 20 (above 0.9); S mostly 0.1 to 0.85, linear; C 0.37 to 1.0 with the worst row changing sign in 5 to 20 of 20 iterations.

### 4. Diagnostics and levers on the 77 replays (scratch; logs `y04`-`y09`)

| Group | Seeds (def / rnd) | 60 iterations: clean | L1: clean | L2: clean | L3: clean | L1+L2+L3: clean |
|---|---|---|---|---|---|---|
| T | 17 / 3 | 3 | 0 | 0 | 5 | 5 |
| S | 24 / 15 | 26 | 0 | 6 | 10 | 11 |
| B | 9 / 3 | 9 | 5 | 0 | 2 | 5 |
| C | 0 / 6 | 6 | 0 | 0 | 0 | 0 |
| all | 50 / 27 | 44 | 5 | 6 | 17 | 21 |

- 60 iterations (A165): separates "still converging" from "not converging". Not a proposition.
- L1, the start-of-solve closure of forbidden directions on a first solve (`closeIllegalStarts` without its "accepted structure" gate).
- L2, one column rule: `initialMassFlow` (and the chain estimate) take the column the flow row takes (both drives one sign: that column; drives towards each other: the one nearest rest; the bistable case unchanged).
- L3, the solver's own last flows and heads kept at a job start on the same structure (the scratch form of carrying the committed end flows; it is not committed state, so it breaks fresh-solver bitwise replays by design).

### 5. The four mechanisms, traced (card worlds minimized keeping the group, `y16`-`y19`; traced `y20`; diagnostic and levers `y21`-`y24`)

| Card world | Blocks | Base | 60 iterations | L1 | L2 | L3 |
|---|---|---|---|---|---|---|
| T: defaults 844 | 5 | holds, tick 2067 | holds, same tick | holds | holds | holds, tick 2167 |
| S: defaults 786 | 8 | holds, tick 1298 | clean | holds | holds, tick 3610 | clean |
| B: defaults 228 | 8 | holds, tick 835 | E2, same tick | clean | holds | holds |
| C: random 529 | 7 | holds, tick 1159 | clean | holds | holds | holds |

**T.** Physics: water passing a nitrogen-filled tank dissolves up to 0.62 mol of nitrogen per m3 (Henry's law, 25 C, 1 atm), a share of 1.1e-5 of the water's moles; below it no bubble exists. Card world: 16.8 kg/s through a junction at 103 766.85 Pa carrying 4.1e-11 mol of nitrogen (share 7e-16) as a 1e-12 m3 gas phase. Its Newton starts at 6e-5 and its corrections shrink the phase (3.7e-12 to 1e-12 m3) while the saturation row rises to 0.5; it ends at 8.8e-7; at larger steps "No finite Jacobian trial at variable 8" or a stalled line search on the same node. Shares in the 20 seeds: 1.8e-19 to 6.0e-6 (largest: 929, 2.5e-4 mol beside 42 mol of water). Card E5's mechanism; the owner's answer is gas solubility (D35).

**S.** Physics: flows are continuous in time; a job's first instant has the flows the last one ended with, and a connection at rest stays at rest. What the code does: `replayStart` drops them and the first step starts from `initialMassFlows`, whose column (the higher-pressure end's density) differs from the flow row's (the column nearest rest). Seed 241: a 1 m riser from a water junction to a nitrogen junction 9.4 Pa apart, at rest (committed flow -2.1e-5 kg/s); the estimate reads 9.7 kPa of water column and starts 0.227 kg/s of nitrogen downwards; the row's drive is -0.004 Pa. The Newton halves the flow per fresh step and less per chord step (3 fresh Jacobians in 20): 0.5 to 3.4e-6. The card world: risers committed at 0 and -0.0024 kg/s restart at -3.10 kg/s each (mixed columns, -5.3 kPa against +4.2 kPa); 11.7 to 7.1e-7 after 20 iterations and three sign changes. The code's own comment on `headLimitMassFlow` describes the same walk for a pump and fixes it there by starting from the limit flow.

**B.** Physics: the pump lifts about 10 kg/s from the two generators into the void; the generator under the discharge cannot receive and stays shut; the void only takes. What the code does: pass 0 of a first solve runs with every boundary open both ways and must converge before the active set may close one (`closeIllegalStarts` skips a first solve, "the compiler's property guess, not a state"; the junction start rule has made it a state since D37, A126, A133). The iterate pumps 10.2 kg/s back into the generator and draws 0.225 kg/s of nitrogen from the void into the discharge junction at 94.3 kPa; 37.9 to 8.116e-5, the same to six digits at 21 step sizes. With the closure on the first solve: clean.

**C.** Physics: a 5 bar methane generator fills a 4.4 bar tank: the inflow is at its limit and the junction sits just above the tank. What the code does: on the capped branch the flow row does not depend on either end's pressure; with the inflow capped the junction's pressure enters only the outlet rows, and when the iterate caps them too the step in that pressure is free: 434.6 to 492.9 kPa iterates, rows alternating 20 of 20, damped steps; 0.65 after 20. With 60 iterations: clean; no start lever touches it.

### 6. Scratch quick checks (`quick.sh` against `q04`'s seeds 1-200; logs `y14`, `y15`, `y26`)

| Form | suites (524) | G2 exact | random clean (184) | defaults clean (173) |
|---|---|---|---|---|
| L3 | 7 failures | chain-100 only, `q14`'s values | 186 (2 cleared, 0 worse) | 176 (5 cleared; 96, 108 worse) |
| L1 | 1 failure (`ElevatedBlockLineIslandTest`) | same | 183 (0 cleared; 132 worse) | 172 (1 cleared; 72, 113 worse) |
| L1+L2+L3 | 9 failures | same | 185 (2 cleared; 132 worse) | 175 (6 cleared; 12, 72, 96, 108 worse) |

L3's failures are its scratch form's: `InletPhaseTest` and `LevelHeadTest` replay a fresh solver bitwise, `FluidCheckpointFormatTest` round-trips a save, `IslandCertificateTest` certifies from a replay, and `ExtremeTopologyIslandTest` ALTERNATING_BOTH_SIDES and `LevelHeadTest`'s monotone equalisation see another step sequence. The committed form (end flows carried in the result and the checkpoint) would keep the replays bitwise; it was not built. MIXED_GAS_COST 309 in all three. No worse seed was traced.

### 7. E2, E5 and E8 sampled (logs `y10`-`y12`)

| Class | World | Mechanism at the hold | 60 it. / L3 |
|---|---|---|---|
| E2 | defaults 792 (16 blocks) | singular, water junction continuity, 1.1e-13 mol nitrogen: T | held / held |
| E2 | defaults 681 (19) | singular, water junction continuity, 8.1e-14 mol nitrogen: T | held / held |
| E5 | defaults 926 (15) | stall, water junction saturation, 7.1e-17 mol nitrogen: T | clean / held |
| E5 | defaults 138 (17) | stall at 6.7e-8 on a water-only junction's continuity row: T's family | held / held |
| E8 | defaults 1 (9) | saturation row of a water junction with 0.0038 mol nitrogen: T | held / held |
| E8 | defaults 778 (11) | saturation row, 0.0052 mol nitrogen beside 41 mol water: T | held / held |
| E2 | random 521, 786 | singular on a liquid crude junction / a two-phase crude tank | held / held |
| E5 | random 807, 547 | 807: water junction with 1.6e-16 mol nitrogen (T); 547: two-phase crude junction | held / held |
| E8 | random 131, 951 | no failed Newton in the hold tick (refused attempts) | held / held |

### 8. The sub-families the brief named

- First-interval family (A97): all of B and C and 24 of S hold within the island's first second; the family is not one mechanism but three (S, B, C), and T has 8 of its 20 there too.
- Cold start of a changed island on a near-zero-drive state (A162: 246, 424): group S. 246 runs clean with L2, L3 and 60 iterations; 424 holds elsewhere with 60 iterations.
- Water beside gas rows (t10's world 736): the MIXED connections of S (183, 220, 246, 547, 758, 786, 886, 981). 736 itself is clean on `q04`.
- The trace family's vanishing phases: group T.

### 9. Stop points, with recommendations

- **E6-T:** no fix of its own; it waits for gas solubility in water (D35). Recommended: measure the group's 20 seeds and the E2/E5/E8 default worlds on that work.
- **E6-S:** recommended (a) with (b) of its card as one item ("the step starts where the island is": the committed interval carries its end flows; one column rule for the estimate), measured on the full campaign before a decision. L3 alone is the best scratch here (+2 / +3 net on the quick check). (c), a flow-form row, only if the walk remains.
- **E6-B:** recommended to take its lever with E6-S, not alone: alone it is net negative on the quick check. Trace the failing suite test and seeds 72, 113, 132 first.
- **E6-C:** recommended (a), the cap as an active-set mode, measured before a decision.
- Order: E6-S first (the largest group, and it changes the start B's closure reads), then E6-B, then E6-C; E6-T with the gas-solubility work.

### 10. What differs from the brief

- `instr-failing-rows.patch` no longer applies; t8's `instr-e6-failing-rows-and-scratch.patch` does but prints unnamed rows to the shared output. A new patch, `src/instr-t13-failing-rows-and-levers.patch`, was written (A163).
- Logs are `y02`-`y26`, not `r01`-...: `r01`-`r18` are class R's tracked logs (A168).
- The card worlds are minimized keeping the group's signature, not by the contrast minimizer: there is no clean mode to contrast with (A167). Each card world is then proved by its own lever.
- Beyond the brief's "one-line idea": three scratch levers and a 60-iteration diagnostic were run on the 77 replays (one worker each), and three quick checks; all scratch, none committed.
- Commit trailers name the model that wrote them (Claude Opus 5.5), not Claude Fable 5.1.
- Documents are not copied to the main checkout (own worktree only).

## A gas bubble in a liquid-full junction is pushed through (2026-10-13)

- Author: Claude (Opus 5.5, thread t14 of the orchestrator), worktree `remove-agents-attribution-line-3d0c97`, side branch `claude/bubble-through` from `claude/phase-ports-compressor` `ca7880ec` (the code of campaign `o02`). Owner decision D41. Decisions A170-A178 (`DECISION_LOG.md`). Cards: `REMAINING_HOLDS.md`, the "Distinct defects remaining" table on `z16` and the E6-T, E5, E2 and E8 cards. Material: `tools/phase-ports-probes/hold-classes/` logs `z01`-`z23`, scripts `t14-lane.sh`, `t14-g1mode.sh`, patch `src/scratch-t14-carry-switch.patch`.

### 1. Why the trace phase persisted

The physics. A junction is a piece of pipe full of running water. A nitrogen trace that reaches it is a bubble. The water carries the bubble at its own velocity to the next gas space (a tank's gas space, a void, a gas-filled junction). The bubble sits at the water's pressure and temperature. Its water vapour is a few percent of a phase that is itself 1e-19 to 1e-6 of the junction's moles: nothing that changes a flow, a pressure or a temperature.

What the code did, in four steps (card world, defaults 844 minimized, trace `y20`):
1. **The fold measures a phase's share of the node's moles** (D34, A83): a phase below 1e-9 of the node does not exist and its material goes to the node's other phase. A gas goes to the liquids, its hydrocarbons to the hydrocarbon liquid (nitrogen is a hydrocarbon-model component). A water junction has no hydrocarbon liquid, so the gas had no receiving phase, and A84 kept it.
2. **The trace is delivered again every step.** The tank beside the junction is at rest, but its connection carries the flow row's tolerance: -2.1e-7 kg/s at the hold, -1e-9 kg/s in the iterates. Its bulk is nitrogen, so the reconstruction books a nitrogen trace into the water junction. The junction held no hydrocarbon phase, so the restatement is its flash (`ConservativeTransport.repartition`, hold class E3's path), and the flash gives the trace a gas phase with its water vapour. Without the tank, a trace that came once decays geometrically in the washing water and never reaches zero above the 1e-24 flush (D16).
3. **A85 is not involved.** Nothing was folded, so nothing was withheld; the phase was never removed in the first place.
4. **The row that holds.** With a gas phase and free water the junction states two more rows: the water-vapour saturation row `ln(p_w / p_sat)` and the partial-pressure closure. `p_w` is the vapour's moles over the gas volume, and the gas volume is the nitrogen's moles times `RT / p`: the row reads the ratio of two amounts of 4.5e-24 to 1.5e-19 mol (the card world's iterates). Both are unknowns in the junction's amount scale (543 mol for 9.8 kg of water), so their scaled values are 1e-26 to 1e-21, below what the linear solve and the junction's fraction rows resolve. The Newton's steps in these two columns are noise. The saturation row climbs to 0.3-2 while the iterates shrink the gas from 6.8e-9 to 1e-18 m3, and it ends at 8.8e-7 after 20 iterations. At larger steps the same node gives "No finite Jacobian trial at variable 8" (the vapour column) or a stalled line search. The residual does not depend on the step, so refinement cannot help.

The shares t13 measured (1.8e-19 to 6e-6) are above and below the fold's 1e-9. Below it, the gas stayed by A84. Above it, the fold does not apply at all: those junctions (145, 402, 592, 839, 929) hold a real but small gas volume (up to 0.9 % of the junction) that the water is washing out, and the saturation row follows a phase that shrinks towards nothing within the step.

### 2. The rule, stated before coding (A171-A176)

**Real-life sequence.** Water runs through a junction. Nitrogen reaches it: from a tank's port at rest, from a void that a first solve lets supply, from a gas pipe. The nitrogen forms bubbles. The water is the continuous phase, so the bubbles move with it at no slip and leave with the outflow in proportion. Downstream they reach a gas space: a tank's gas space (the bubbles join its gas, which takes its vapour from the water there), a void (taken), or a gas-filled junction (joins its gas). No step of this needs the bubble's own vapour equilibrium.

**Where it lives.** In the stage that states a node's phases: `FluidThermodynamics.folded` (the flash, the reconstruction's restatement and every pass seed go through it, D34) and `PhaseLayout.support` (which unknowns a pass carries for a node). The node kind (junction or vessel) is passed in; nothing else changes.

**The forms (A171).**

| Form | In the state | In the step solve | Absorbs | Measured |
|---|---|---|---|---|
| (i) carried gas without a phase of its own | the gas components with no phase | no unknown, no row; the transport reconstruction carries them | the saturation and closure rows; the zero column | needs a state with a component outside every phase: a new state kind, the same change as dissolved gas (D35) |
| (ii) explicit carried-gas phase, dry, at the liquid's P and T | a dry gas, volume n R T / P | the gas amounts are unknowns; no saturation or closure row | the saturation and closure rows | at a trace the amount column is zero: 6 of the 27 worlds singular at every step size (E2, `z03`, `z04`) |
| (iii) carried gas with its vapour stated algebraically | a saturated gas | the vapour eliminated from the gas amount, no row | the rows, keeps the vapour | a new layout mode; the elimination still reads the ratio of two trace amounts |
| **(i)+(ii), recommended and implemented** | a dry gas (ii) | a trace: no unknown (i), an omitted trace of the junction as a masked component nothing delivers already is; above the trace: (ii) | both rows, the zero column, A84's stay case | section 4 |

**Where a junction is liquid-full (A172).** Options: (a) only when its gas is a trace (below 1e-9 of its moles); (b) also when free water is its only liquid and fills more of it than its gas (the water is the continuous phase, the gas is bubbles); (c) as (b) with the bubbly-flow limit, a gas volume fraction below 0.25 (vertical bubbly-to-slug transition). Recommended and implemented: (a) for every node and (b) for junctions. A vessel is never liquid-full by (b): gravity separates its gas into a gas space at the top, which keeps its saturation. (b) over (c): in slug flow (fractions 0.25-0.5) the gas is still carried by the liquid in a pipe, and the question is only whether the gas needs its own vapour row; (c) leaves more junctions with a vanishing wet gas. Measured in section 4: (a) alone and (a)+(b).

**A vessel (A173).** A trace gas in a vessel with free water and no oil is dry, as at a junction (rule (a)), but keeps its amount unknown: a vessel's balance rows are scaled by each trace's own amount when its gas is under 1 % of its volume (`PhaseLayout`, `componentScales`), so the unknown is resolved. A bubble arriving in a vessel joins its gas phase in the vessel's flash and closure; above the trace share the gas is a gas space with its vapour.

**A junction liquid-full on its inlet side that drains to a gas space.** The bubble leaves with the water: a junction's outflow is its bulk, and the carried gas is part of it in proportion. At the gas space downstream it joins the gas there. No rule of its own is needed.

**A water-full junction at rest (A174).** In real life the bubble rises out of the junction into the next gas space above, if one is connected. The model's junction is one well-mixed node without slip, so the bubble stays in it, dry, until flow resumes. Acceptable: it is at most the junction's gas share of a holdup of 0.05 s of peak flow (D25), it has no row, it does not move the junction's pressure (1e-12 m3 in the card world) and it leaves with the next flow.

**The ledger, the rest certificate, the checkpoint (A175).** Nothing leaves a node and nothing is booked: the fold moves the gas's water vapour into the node's free water at the same temperature and pressure (the trace fold's rule, D34); the transport reconstruction books every component, the omitted trace included, so conservation stays exact by construction (`checkConservation` unchanged). The rest and steady certificates compare committed states; a dry carried gas is a state like any other and does not change at rest. A checkpoint carries the state as it is (a gas phase with no vapour beside free water is a valid state); fresh world only, no migration.

**One statement of a junction's phases (A176).** The reconstruction restates a junction through the same `folded(state, junction)` as the pass seeds. Without it, a junction at the border cycled: the flash kept a gas of 1.01e-9 (its vapour counted) wet, the next seed dried it to 9.8e-10 and omitted it, and the same pass repeated (defaults 138, "Phase/device active-set cycle", `z06`).

### 3. As implemented (`c767503a`; tests `dbebfddd`)

- `FluidThermodynamics.foldTracePhases`: a trace gas with hydrocarbons, no hydrocarbon liquid to go to, free water and water vapour gives its vapour to the free water (rule (a)); `foldedHydrocarbonPressure` states the dry gas at the node's pressure.
- `FluidThermodynamics.carriedGas` and `folded(state, junction)`: rule (b) for a junction. `carriedTrace(state)`: a dry gas below the trace share in free water.
- `PhaseLayout.support(..., junction)`: a junction's carried trace gas components are `ABSENT` (no unknown, no row).
- `PassiveStepSolver`: the pass seeds and the phase correction use `folded(state, junction)`; `phaseFolded` also sees a gas that lost its vapour; `phaseAppeared` skips a junction the booked amounts give a carried trace.
- `ConservativeTransport`: `repartition` states a dried gas at the node's pressure; the reconstruction restates a junction through `folded(state, true)` (A176).
- Absorbed (A177): A84's stay case for a trace gas in free water; D35's deferral of E6-T; the E3 re-pass for a nitrogen trace below the trace share. Kept, measured later if wanted: the trace difference step (F1/F2) and the 1e-10 small-headspace tolerance, which still serve vessels and whose removal moves bitwise references.
- Scratch, never committed: `src/scratch-t14-carry-switch.patch` (`-Dt14carry=off|trace|on`: off reproduces `o12`'s printed lines exactly, 0 of 6072 differ, `z10`).

### 4. Campaign (`z16`, 8 workers, 1000 seeds per distribution, on `c767503a`; against `o02`)

| | `o02` (base) | (a) alone (`z21`, switch) | (a)+(b), committed (`z16`) |
|---|---|---|---|
| random clean | 919 | 927 (+9, -1) | **933** (+14, -0) |
| world defaults clean | 883 | 899 (+23, -7) | **930** (+50, -3) |
| random Newton solves / iterations | 1.11 M / 2.96 M | 1.75 M / 3.47 M | 0.39 M / 2.25 M |
| defaults Newton solves / iterations | 2.27 M / 19.88 M | 3.05 M / 23.63 M | 1.38 M / 11.40 M |
| random / defaults gate refusals | 203 816 / 160 640 | 276 539 / 216 123 | 23 609 / 44 032 |

Class counts `o02` -> `z16` (random / defaults): C 15 -> 15 / 5 -> 3; E2 9 -> 8 / 13 -> 10; E3 0 / 3 -> 0; E4 0 / 7 -> 3; E5 6 -> 4 / 14 -> 0; E6 30 -> 25 / 44 -> 35; E7 3 -> 2 / 3 -> 2; E8 6 -> 1 / 12 -> 1; E9 3 / 6 -> 7; E10 4 -> 3 / 4 -> 0; F 5 -> 6 / 6 -> 9. Cleared random: E6 98 257 511 681 752 951, E8 131 357 563 613 967, E5 807 815, E2 854. Cleared defaults: 50 (E6 18, E5 12, E8 7, E4 4, F 3, E10 2, E3 2, C 1, E2 1). Worse: defaults 589, 1000 (E6), 514 (E4); random none.

Replayed alone (`z17`, alone lock): the three worse seeds hold again (589 E6, 1000 E6, 514 E4). Budget holds: random 246 431 678 707 788 hold (F), 803 holds (E8); defaults 616 and 669 run clean alone, 71 298 622 788 hold (F), 39 and 929 hold (E8), 164 holds (E2). Counting the two: 932 world defaults.

E6 on `z16`, traced seed by seed (`z22`, t13's group rule): defaults S 26, B 9; random S 14, B 5, C 6; **T 0** (`q04` T 17 / 3).

**The 20 + 7 worlds** (alone on `o02`'s code `z01`, then on the rule: campaign `z16`, alone `z08`):

| Worlds | Before (`z01`) | After, alone (`z08`) | After, campaign (`z16`) |
|---|---|---|---|
| E6-T defaults (17) | 1 clean (654); E6 14, E5 39, E4 929 | 11 clean; 271 402 431 678 E6, 39 929 E8 | 10 clean; 39, 616, 929 F (alone: 39 and 929 E8, 616 clean); 271 402 431 678 E6 (group S) |
| E6-T random (430 511 752) | E10 430, E6 511 752 | 511, 752 clean; 430 E5 | same |
| E2 792 681, E5 926 138, E8 1 778 (defaults) | E2 2, E5 2, E8 2 | all clean but 792 (C) | same |
| E5 random 807 | E5 | clean | clean |

### 5. The worse worlds, traced (`z18`, minimized keeping their signature `z19`, the minimized worlds under on and off `z20`)

| World | Hold on `z16` | Minimized | Physics and what the code did | Rule off |
|---|---|---|---|---|
| defaults 589 | E6, first interval, flow row on a mixed riser (tank to gas junction) | 13 blocks | Group S: the riser is at rest (drive 0.07 Pa), the start estimate starts it at 0.226 kg/s and the Newton walks it back | holds at the same tick (residual 1.3256e-5 off, 1.3248e-5 on) |
| defaults 1000 | E6, later, flow row of a liquid port into a junction; that junction's saturation row | 21 blocks | Group S at a junction filling with water while its gas is still the larger volume (a wet gas); the start flows of the step | holds earlier (tick 2687, an active-set cycle and the iteration limit) |
| defaults 514 | E4, the equation gate at 1.16e-8 on a gas-continuous water junction's enthalpy row | 21 blocks | A chord Newton converging linearly (0.8 per iteration) on a junction the rule does not touch (gas 0.030 m3 against water 0.0007 m3) | holds identically (tick, status, 1.1636e-8) |

None of the three is caused by the rule: each minimized world holds with the rule off. On the full seeds the rule changes the trajectory (other junctions are carried) and the world reaches an existing mechanism it missed on `o02`.

### 6. Gate test and its proof (`dbebfddd`, `z15`)

`CarriedGasTest.aWaterGeneratorDrainingPastANitrogenTankIntoAVoidKeepsUp`: the E6-T card world (defaults 844 minimized, 5 blocks at the fuzz's ticks: a 1 atm water generator, a junction 1 m below it with a 1 m3 nitrogen tank beside it, a pipe, a 1 atm nitrogen void 3 m down). With the rule: 120 slices, no hold, the tank at 103 766.848 Pa (between the void and the generator's head, as the card's physics expects). With the rule disabled (`-Dt14carry=off`, working tree only): held at tick 2067, "Newton iteration limit at residual 8.756032658013135E-7", the campaign's hold to the last digit. `TracePhaseFoldTest` states the rule on the flash: a 1e-12 nitrogen bubble in water is dry at the node's pressure, a carried trace, nothing leaves the node; a 1e-6 bubble is a gas with its vapour in a vessel and dry in a junction.

### 7. References (`z23` on `dbebfddd`, against t10's `o12`; nothing re-recorded)

| Reference | `o12` | `z23` |
|---|---|---|
| G1 suites | 519 tests, 0 failures | 520 tests (+ `CarriedGasTest`), 0 failures |
| 33 junction lines | - | identical |
| MIXED_GAS_COST | 309 | 309 |
| G2 exact regression | red on chain-100 only: 6.049e-15, 1.705e-13 K, 1.896e-11 | identical |
| G3 adjacent | 45 / 45 | 45 / 45 |
| GameTest outlets | 124258.48752327723 Pa / 175.76227454727493 kg | identical |
| GameTest compressor | 2.999985907556435 / 157976.31408415743 Pa | identical |
| Printed suite lines | 6072 | 6073: 798 moved, 1 new (`CARRIED_GAS_CARD tankP=103766.8482339045`); `z23-vs-o12-printed-diff.txt` |

The 798 moved lines are the five tests that pump water out of a liquid-full nitrogen tank (`TraceFlushFedPumpTest` FED_PUMP 400 lines, `PhasePortPriorityTest` 270, `LiquidFullTankOutletsTest`, `PumpBottomPortDrainTest` and `TraceFlushLiquidFullTankTest` F1_SLICE 50 and F2_SLICE 78). Their junction between tank and pump holds a little gas in much water; it is now carried dry. Old and new, examples: FED_PUMP slice 0 junction gas 2.110 -> 2.086, its water vapour 0.0451 -> 0, tank 129956.598 -> 129956.185 Pa; slice 119 tank 129932.53751193234 -> 129932.53751161684 Pa (2.4e-12); F1 slice 0 flow 0.63626 -> 0.64071 kg/s, tank 129494.608 -> 129491.083 Pa; F2 slice 25 tank 129913.32098 -> 129913.32146 Pa; PHASE_PORT_PRIORITY overflow final pressure 195782.9311534301 -> 195782.93115333474 Pa and, in the oil-water run, the same accepted and rejected counts with their reasons shifted (negative transport reconstructions 4 -> 5, Newton iteration limits 2 -> 1). Every assertion holds. The rule's trace part alone moves 789 of them (`z11`), the junction part the other 9.

### 8. Final gates (`z23`, GameTest run id `t14-z23-dbebfddd-run1`)

G1 520 / 0 failures; 33 junction lines identical to `o12`; MIXED_GAS_COST 309; G2 red only on chain-100 with `o12`'s values (unchanged since t10); G3 45 / 45; GameTests 33 / 33 with every printed value identical, so one run.

### 9. Stop points, with recommendations

- **A171, the form; A172, where a junction is liquid-full.** Implemented as recommended: (i) for a trace at a junction, (ii) otherwise; (a) for every node and (b) for junctions. Recommended: confirm. (a) alone is weaker on every count (927 / 899, more Newton work than `o02`).
- **A174, the bubble at rest** stays in the junction instead of rising. Recommended: accept (no row, a trace or a holdup's share, leaves with the next flow).
- **A177, two candidates** made redundant for junctions by the rule (the trace difference step F1/F2, the 1e-10 small-headspace tolerance) still serve vessels. Recommended: leave them; measure their removal only with the owner's re-baseline (D36/D38), since both move bitwise references.
- **The three worse seeds** (589, 1000, 514) are group S and a slow chord on a gas-continuous junction; their minimized worlds hold with the rule off. Recommended: no action here; t15's start flows act on S.
- **The 798 moved printed lines** are the carried gas in five pump tests, by 2.4e-12 at their end. Recommended: take them in the owner's re-baseline.
- **Take the branch.** Recommended: yes. It removes group E6-T (17 / 3 on `q04`, 0 on `z16`), E5 in world defaults (14 -> 0) and most of E8 (12 -> 1), clears 14 / 50 seeds for 0 / 3 worse, halves the defaults Newton work, and moves no recorded reference.

### 10. What differs from the brief

- An orphaned 1000-seed campaign of the aborted first t14 (its `z05-campaign-v2`, the wrong base) was still running in this worktree and locked its build folder for about five minutes; stopping it was refused by the permission layer, so the thread waited for it to end. Its logs (`z01-before-T27` to `z05-campaign-v2`) are left as found and not committed (A178).
- A third lane (t16) ran beside t15. `t14-lane.sh` also waits while two other lanes run (AGENTS.md: at most two Gradle runs on the machine).
- The form is neither (i) nor (ii) alone but both, by share: (ii) alone was measured singular at a trace (A171).
- Beyond the brief: the campaign of rule (a) alone (`z21`) and every E6 seed of `z16` traced (`z22`), to state A172 and the cards on the final campaign.
- Commit trailers name the model that wrote them (Claude Opus 5.5), not Claude Fable 5.1.
- Documents are not copied to the main checkout (own worktree only).
## The step starts from the committed flows; forbidden directions closed on first solves (2026-10-13)

- Author: Claude (Opus 5.5, thread t15 of the orchestrator), worktree `start-flows`, side branch `claude/start-flows` from `claude/phase-ports-compressor` (`ca7880ec`, the code of campaign `o02`). Items: card E6-S's (a) and (b), card E6-B's (a), as one batch. Sub-choices A180 onwards (`DECISION_LOG.md`). Material: `tools/phase-ports-probes/hold-classes/` logs `sf01` onwards (A180).
- Sections 1 and 2 were written before the code, as the brief asks. t13's levers L1, L2 and L3 (`src/instr-t13-failing-rows-and-levers.patch`) were read as scratch; the rules below are stated on their own.

### 1. Physics, in real-life order

**Flows are continuous in time.** Water in a pipe has mass. It does not stop or reverse from one instant to the next unless a force acts on it. So at any instant the flows are the flows of the instant before. A leg standing at rest stays at rest. A pipe pouring 8 kg/s still pours 8 kg/s a moment later. This holds across the 5 s slices of the model: the slice boundary is a bookkeeping line, not an event in the world. Nothing happens to the water at the instant the engine hands an island to a worker.

The model has no momentum: a connection's flow is algebraic, the root of `driving - loss = 0` at each step's end state. So "flows continue" is not a law the equations must state. It is a statement about where the Newton starts. The best start for the first step of a slice is the state the last step of the previous slice ended in: the same states, the same flows. That is what every later step inside a slice already starts from (`PassiveStepSolver.startPoint` warm-starts from the previous solve's flows). The job boundary is the one place the code throws them away (`replayStart`: "no warm start"), and starts the first step from a per-connection estimate built on the static pressures alone.

**Card world E6-S (defaults 786, 8 blocks).** A water generator drains 1 m into a void. Two risers stand above it, up to a gas junction that the last pipe joins to a nitrogen tank at 96.8 kPa. In real life the risers are manometers: 0.27 m and 0.46 m of water stand in them at rest, gas above. The committed slice ends with riser flows 0 and -0.0024 kg/s. The next job restarts both at -3.10 kg/s (a full cap of falling water), because the estimate reads the riser full of water (drive -5.3 kPa) where the flow row reads it full of gas (+4.2 kPa). The Newton then walks the flows back by halving, through three sign changes, and runs out of its 20 iterations at a residual of 7.1e-7.

**Which column stands in a vertical pipe.** A vertical connection between a gas end and a water end can stand in either fluid. The flow row decides which column it states once per pass (`Equations.headDensities`): the column that is its own donor (the drive it produces points away from the end it is read from); where neither is, the one nearest its own rest; where both are (a pipe that can stand full of either fluid), the start flow's sign, which is the pipe's history. The estimate does not ask this question. It takes the higher-pressure end's density. On a riser at rest the two disagree by a metre of water against a metre of gas, 9.8 kPa against 11 Pa, so the estimate starts a connection at its cap that the row sees at rest.

**Forbidden directions.** A generator only supplies; a void only takes (D21). This holds from the first instant of a new network, not only after it has run once. Since D37, A126 and A133 a new island's junctions start as what physically reaches them, at their balance pressure: a state, not a property guess.

**Card world E6-B (defaults 228, 8 blocks).** Two generators on a lower line feed a pump 1 m up. The pump lifts about 10 kg/s into a junction over generator A, and on into a void. In real life generator A's riser stands full at rest (a generator cannot take water back) and the void only takes. The code's first solve runs pass 0 with every boundary open both ways. It converges towards 10.2 kg/s pumped back down into generator A and 0.225 kg/s of nitrogen drawn out of the void, and runs out of iterations at 8.1e-5, the same at 21 step sizes.

### 2. The rules, stated before coding

**(a) The committed interval carries its end flows into the next job.**
- *What a job is.* One dispatch of an island to a worker (`RetainedSolver.run`): it integrates one slice from the island's committed state and returns the interval the coordinator commits (or a hold). The coordinator hands the island's committed interval to the job at dispatch (`IslandCoordinator`, `retained.committed`).
- *What the end flows are.* The mass flows of the interval's last accepted step: the flows at the interval's end state, the instant the next slice starts from. They sit in the committed interval beside its end modes and end heads (`PassiveIntervalSolver.Result.endpointFlows`), next to the interval's average flows, which are the transported mass and stay as they are. A rest interval's end flows are zero (its flows are zero, A184). A closed connection's end flow is exactly zero (it already is: a closed connection records zero).
- *What the job starts from.* When the job's island is the island of the committed interval (the same pipe identities and node ids, `PassiveStepSolver.sameStructure`), the step solver starts as if the job boundary were not there: the first step's start point is the end flows and end heads, with the end modes (pumps on their head limit or closed, as now). This is the start every later step of a slice already has. The interval solver's start-of-step guard reads the same carried endpoint at the job's first step, as it does at every later interval of a job (A185).
- *After a topology edit or a new junction.* Kept contents are not taken (D45): every edit re-mints its island, with new junctions started by the start rule. The re-minted island is a new island (new node ids), so no committed interval is on its structure, and nothing is carried: its first step starts from the estimate on its minted states, as now. The carried flows are only ever those of the island the job continues (A183). The estimate stays for exactly that case: a structure with no committed interval.
- *What a checkpoint load starts from (fresh world only).* The end flows are part of the committed interval, so the checkpoint saves them (format 7, breaking: a world saved before cannot be read, no migration, A189), and a loaded island's next job starts from them exactly as the island that was never saved. The reader validates them: one finite flow per connection, and zero on a connection whose end mode is closed.
- *What a fresh-solver replay means.* A certified island is woken with a new solver handle, a loaded world gets new handles, and several tests build a fresh solver beside a retained one and require the two to commit bitwise identical intervals. The contract is that a job depends only on the committed interval and the slice, never on what the solver did before (D8 of the mixed-gas batch). The end flows keep that contract because they are committed state: the retained solver and a fresh one are handed the same committed interval and derive the same start. A certificate's replayed interval carries the recorded interval's end flows, as it carries its end modes and heads.
- *Why t13's L3 broke 7 suite tests.* L3 kept the solver's own last flows (`previousFlows` of the retained solver) at a job start. Those are solver-local history, not committed state: the last solve may be a refused step, a planner trial or a rate solve; and they are in no checkpoint and no certificate. A fresh solver (after a load, at a certificate wake, or in a test's fresh-solver replay) started from the estimate while the retained one started from its history, so `InletPhaseTest`, `LevelHeadTest`'s fresh-solver replay, both `FluidCheckpointFormatTest` round trips and `IslandCertificateTest`'s certified replay differed by construction. The committed form puts the flows where the contract already reads: the committed interval. Two more L3 failures (`ExtremeTopologyIslandTest` ALTERNATING_BOTH_SIDES and `LevelHeadTest`'s monotone equalisation) are a changed step sequence, not a broken contract; they are checked on the committed form and reported in section 7.
- *Stage.* The committed interval (`PassiveIntervalSolver.Result`, the solid event integrator's and the module planner's copies, the certificate's replay, the checkpoint codec) and the job start (`PassiveIntervalSolver.replayStart` and `PassiveStepSolver.replayStart`, now handed the committed interval instead of its graph and modes).
- *What it absorbs.* The "no warm start" rule of `replayStart` and its separate path for modes: the job start is the in-job warm start, read from committed state. The interval solver's dropped guard carry at a job start, and with it the one rate solve of the port graph the solid event guard made at the first step of every job (it now runs only on a structure with no committed interval). The rate solve's stale-flow gate (review 7.7) stays as it is and needs no case of its own: the end flows are recorded at the committed states, so a rate solve on the same states may start from them and one on other states may not, by the existing value-identity test (A186).

**(b) The estimate states its column exactly as the flow row does.**
- *One column rule, one place.* A static function of the step solver, `columnFromFirst(first, second, difference, dz, forwardStart)`, states the rule of `headDensities` above: both drives one sign, that column; drives towards each other, the column nearest its own rest; both admissible, the start's sign. The flow row (`Equations` at each pass) and the estimate (`initialMassFlow`, the chain contraction of `initialMassFlows`, and through them the balanced junction seed) call it; neither keeps a rule of its own.
- *The estimate's own start sign.* The estimate is the start point, so in the both-admissible case it has no earlier start to read. It takes the sign of the static pressure difference. This is the old estimate's choice in that case, and it is consistent: the row then reads the flow the estimate produced and states the same column (A187).
- *The loss.* The estimate's friction reads the stream of the end the stated drive points away from, as the row's loss reads the donor of its flow (before: the higher-pressure end's stream for both column and loss). On a flat island the column multiplies `dz = 0` and the donor is the higher-pressure end, so a flat island is bitwise what it was.
- *What it absorbs.* `initialMassFlow`'s and the chain's "higher-pressure end" column; t13's L2 copy of the rule.
- With (a), the estimate is read only where no committed flow exists: a new island's first steps, the cold rate seed, the closure's direction for a connection with zero start flow, the vessels' phase seeds (`initialPhaseSeeds`, which asks what could arrive) and the balanced junction seed.

**(c) The closure of forbidden boundary directions runs on first solves too.**
- *Why its gate existed.* `closeIllegalStarts` closes, before pass 0, a passive connection whose start flow runs the way its boundary forbids while the static drive forbids it with either end's density. It ran only once the solver had accepted a solve of the same structure, because "a first solve's junction pressures are the compiler's property guess, not a state". On such a guess the start direction says nothing, and a closure that is never taken back within the solve could pin a wrong active set.
- *Why the physical junction start makes it unnecessary.* Since D37 (the start rule), A126 (the balance pressure) and A133 (the drain-side gas column) a new junction starts as what physically reaches it at the pressure where its inflow balances its drains. Its pressure is a state. The closure's evidence on a first solve is then of the same kind as on any later solve: the static drive between a fixed boundary and that state. Rule (b) makes the start direction it reads, where it reads the estimate, the direction the row will state.
- *The rule.* The closure runs on every solve, rate and step, first or not. Its two conditions stay: passive connections only (devices decide first), and a direction is closed only when both columns forbid it. The reopen exemption (`keep`) stays.
- *What it absorbs.* The "accepted structure" gate. With (a) the gate was already open on every job of a continuing island (a committed structure counts as accepted), so (c) changes exactly the solves of a new island before its first accepted solve: the cold rate seed and, where that seed failed, the first step solves.
- *What it does not absorb.* D12's cold closure of receiving generators (`closeColdReceivingGenerators`) asks another question: a generator run ending at a junction of degree three or more, decided on the one-way pressure estimate of the whole island, which also reseeds the junctions. It stays (A188).
- *Order.* (c) is taken only with (a)+(b), as t13 recommended: on a first solve the closure reads the estimate's directions, which (b) corrects. It is measured as a second commit on top of (a)+(b).

### 3. As implemented, and what the suites showed

- **(a)+(b), `8721a138`.** As section 2. `PassiveIntervalSolver.Result` gains `endpointFlows` (the last accepted step's `massFlows`); `replayStart(graph, committed)` in both solvers; the solid event integrator, the module planner, the certificate's replay and the rest interval (zeros) carry it; checkpoint format 7. `PassiveStepSolver.columnFromFirst` is the one column rule; `staticDrive` is the estimate's drive on it, called by `initialMassFlow` and the chain contraction (and through `initialMassFlow` by the balanced junction seed). Fifteen tests change only their call of `replayStart` or the `Result` constructor, and the two format tests their format number.
- **A contracted run states each link's column, `6b6c94b2`** (A193, found in section 6): the estimate's chain contraction sums its links' columns, each by `columnFromFirst` on the link's own ends, instead of one end-to-end column.
- **A carried flow never starts in a forbidden direction, `09f3afbe`** (A190). Found on the suites: carried across a job boundary, a 2.6e-12 kg/s flow into a generator (below the pass loop's 1e-10 kg/s threshold) survived every job, because a Newton whose start is within tolerance accepts it, and the alternating extreme grid's generator received 1.3e-11 kg per interval. In real life a generator never receives; such a flow starts at rest.
- **The rest tail** (A191, stop point). Five suite tests fail on (a)+(b): `IslandCertificateTest` (the closed pair's drift, exactly 0 expected, 1.6e-12; the closed ladder's quiet-pipe premise), both certificate round trips of `FluidCheckpointFormatTest` (the same pair), and `LevelHeadTest`'s monotone equalisation. Traced with a step print (`sf03`, the scratch switch `-Dt15start=static` restoring the old job start): both forms run the level pair identically to slice 25; there the old job start puts the flow at 0 (a vessel-only island with one phase starts from zero flow, `startPoint`'s `warmFlow`), the Newton accepts it at iteration 0 and the pair is bitwise at rest from slice 30 on. Carried, the 8.3e-8 kg/s tail is accepted at iteration 0 in turn, the state drifts inside the tolerance band until a step needs an iteration, and the flow jumps to the other sign (2.0e-8, -2.5e-8, 1.3e-8 kg/s): 17 monotone breaks of about 1e-5 Pa. The same freeze happens inside a job in the old code (8.3e-8 held for two steps); the job start used to end it every 5 s. So a resting island needs a definition of rest at a resolution the Newton can deliver. That is D42 (thread t16), not this item. The five tests are not restated (the brief forbids loosening an exact-rest assertion).
- **(c), `fde8aa74`** (A192, stop point). The closure on first solves clears the E6-B card world and fails two more suite tests, both new islands (closure print, `sf10`). In `ElevatedBlockLineIslandTest`'s pumped rising line the closure is wrong: the pump's suction junction 2 m above a 400 kPa water generator is minted at 400 000 Pa, where in real life it stands near 380.4 kPa, because the start rule does not restate a junction's pressure hydrostatically (A105, card E9 (a)); the cold rate seed reads 15.8 kg/s into the generator, closes it, converges, and every later step reads the same junction and closes it again; the tank never fills. In `JunctionStartStateTest` the closure is right (a void 9.4 Pa above the minted junction would supply, which a void never does), and pass 0 then fails on the physical network at 3.1e-4: a first-solve start that cannot reach the root. So section 2 (c)'s premise holds for what a junction holds, and holds for its pressure only where the start rule has restated it for height. (c) waits for card E9 (a).

### 4. Campaigns (lane 2, 4 workers, 1000 seeds per distribution; against `o02`)

| | `o02` (base) | (a)+(b), `sf08` (`09f3afbe`) | (a)+(b)+(c), `sf12` (`fde8aa74`) | **(a)+(b) final, `sf22`** (`6b6c94b2` with the gate restored) | (a)+(b)+(c) final, `sf24` (`6b6c94b2`) |
|---|---|---|---|---|---|
| random clean | 919 | 922 (cleared 8, worse 5) | 921 | **923** (cleared 9, worse 5) | 921 |
| world defaults clean | 883 | 900 (cleared 29, worse 12) | 899 | **901** (cleared 29, worse 11) | 900 |
| E6 random / defaults | 30 / 44 | 27 / 34 | 27 / 35 | 27 / 33 | 29 / 35 |
| first-interval family (A97) random / defaults | 42 / 32 | 44 / 43 | 45 / 42 | 44 / 42 | |
| Newton iterations random | 2 962 471 | 3 185 164 | 3 223 023 | 4 404 352 (random 788 now runs its 120 slices) | 3 267 767 |
| Newton iterations defaults | 19 879 189 | 14 442 317 | 17 748 951 | 14 255 129 (-28.3 %) | 19 056 779 |
| Newton solves random / defaults | 1 108 474 / 2 268 689 | 1 090 300 / 1 849 390 | 1 082 360 / 2 699 599 | 1 242 122 / 1 849 591 | 1 083 884 / 2 843 900 |
| gate refusals random / defaults | 203 816 / 160 640 | 208 086 / 221 635 | | 240 110 / 221 809 | 210 130 / 364 240 |
| suites | 519 / 0 | 5 failures (A191) | 7 failures (A191, A192) | 521, 6 failures with the gate restored (A191, and the E6-B gate test, its proof) | 521, 7 failures (A191, A192) |

- Class counts of the final (a)+(b) (`sf22`, random / defaults): C 15 / 3, E10 3 / 3, E2 8 / 11, E3 1 / 2, E4 1 / 7, E5 4 / 6, E6 27 / 33, E7 2 / 2, E8 5 / 17, E9 4 / 6, F 7 / 9. Against `sf08` it changes two seeds: defaults 902 cleared (A193), random 788 cleared (a budget hold, clean alone before); none worse. Against `o02`: random cleared E6 98 188 633 681, E5 553 815, E10 430, E2 854, F 788; worse E5 71 432, E3 301, E9 761, F 77. World defaults cleared as `sf08`; worse E6 241 244 246 758 1000, E5 226 673, E10 12, E4 96, E8 183, E9 101 (`sf22-transitions-vs-o02-*`).
- (c) on the final code (`sf24` against `sf22`): random cleared E6 118, worse E6 446 475 and F 788; world defaults cleared E2 94 823 978, F 118 929, C 402, E6 228, worse E6 271 438 842, C 467 600, E3 872, E5 78, E9 430. Net -2 / -1.

- (a)+(b) against `o02`, random: cleared E6 98 188 633 681, E5 553 815, E10 430, E2 854; worse E5 71 432, E3 301, E9 761, F 77. World defaults: cleared E6 17 26 164 220 271 529 547 556 600 651 719 720 736 886 967, E5 301 316 847 853, E9 19 23 771, E10 188 387, F 71 156, E2 696, E4 78, E7 98; worse E6 241 244 246 758 902 1000, E5 226 673, E10 12, E4 96, E8 183, E9 101. Class counts (random / defaults): C 15 / 3, E10 3 / 3, E2 8 / 11, E3 1 / 2, E4 1 / 7, E5 4 / 6, E6 27 / 34, E7 2 / 2, E8 5 / 17, E9 4 / 6, F 8 / 9 (logs `sf08-transitions-vs-o02-*`).
- (c) on top of (a)+(b): random cleared E6 118, worse E6 446 475; world defaults cleared E2 94 823 978, F 118 929, C 402, E6 228 (the E6-B card's seed), worse E6 271 438 842, C 467 600, E3 872, E5 78, E9 430 (`sf12-transitions-vs-sf08-*`). Net -1 / -1.
- The first-interval family grows with (a)+(b): the later-job holds (S) clear, and what remains concentrates on new islands, whose first steps still start from the estimate.
- World-default Newton work falls by a quarter: jobs no longer restart converged flows from the estimate. Random work rises 7.5 %.
- Alone (`sf13`, `sf14`, under the alone lock): every worse seed of (a)+(b) holds again alone in its campaign class. Budget holds: random 788 and defaults 118 run clean alone; random 829 holds as E8 and defaults 829, 788 as E8, E9; the rest hold on the budget again. (a)+(b)+(c): no worse seed or budget hold runs clean alone.

### 5. t13's E6-S and E6-B seeds, and the card worlds

The 39 + 12 seeds of `q04` were replayed alone (one worker) on `o02` first (`sf01`): every one reproduces its `o02` campaign class. E6 on `o02`: S 14 / 12, B 5 / 2 (defaults / random); the rest moved since `q04` (S defaults: 183 241 246 424 758 clean, 606 932 C, 768 981 E5, 915 E8; B: 842 clean, 475 E5, 563 824 E8; random 107 660 794 719 clean). After (`sf15`, `sf16`; `logs/sf15-t13-groups-summary.txt` has every seed):

| Group (seeds) | `o02` | (a)+(b) | (a)+(b)+(c) |
|---|---|---|---|
| E6-S defaults (24) | E6 14, clean 5 | E6 9, clean 7 | E6 8, clean 7 |
| E6-S random (15) | E6 12, clean 3 | E6 10, clean 5 | E6 9, clean 6 |
| E6-B defaults (9) | E6 5, clean 1 | E6 3, clean 3 | E6 3, clean 2 |
| E6-B random (3) | E6 2, clean 1 | E6 2, clean 1 | E6 2, clean 1 |

- E6-S, (a)+(b): defaults cleared 220 529 547 720 886 967, worse 241 246 758 (clean on `o02`, E6 now) and 606 (C -> E6); still E6 175 352 397 915 981; moved 29 to E2, 173 768 to E4, 183 357 786 932 to E8, 298 to E9. Random cleared 98 633.
- Card worlds (`sf09`, the scratch switch): E6-S (defaults 786 minimized, 8 blocks) holds with the old job start at tick 3210 (E6, 0.079) and runs 120 slices with (a)+(b) and with (c). E6-B (defaults 228 minimized, 8 blocks) holds at tick 836 (8.116e-5) with the old start and with (a)+(b), and runs 120 slices with (c).

### 6. The worse worlds of (a)+(b), traced (`sf17` contrast minimizations: (a)+(b) holds in the class, the old start runs clean; `sf18` each minimized world with one rule reverted; `sf19` the start estimate printed)

Fifteen of the sixteen non-budget worse seeds of `sf08` minimize with the contrast kept (7 to 23 blocks); defaults 183 does not (its minimized world loses the difference). Random 77 is a budget hold and is not minimized (wall time). Reverting one rule at a time:

| Cause | Worlds | When they hold |
|---|---|---|
| (b), the estimate's column | defaults 241, 244, 902, 226, 12 | a new island's first interval (tick 1-2 after it formed) |
| (a), the carried flows | defaults 246, 758, 96; random 71, 432, 761 | later jobs (0.3 s to 70 s after the island formed) |
| either rule reverted clears | defaults 1000, 101; random 301 | |
| both rules reverted needed | defaults 673 | |

- **902** (8 blocks, (b)): a 1 atm water generator, a pipe 1 m up into a filter's junction, the filter, a junction, and 3 m down to a 1 atm void. In real life nothing flows: lifting water 1 m takes 9.8 kPa the generator does not have, and a gas-filled run does not prime itself (D40). The estimate contracted the four links to one run, generator (-1 m) to void (-3 m), and stated its column end to end: both ends' drives point down, so it read water and started a 7.19 kg/s siphon over the hump; the old rule had read the void's gas (a tie went to the first end) and started 0.0066 kg/s. That was an error of `8721a138`, not of the rule: the row states a column per link. Corrected in `6b6c94b2` (A193): the run's drive sums its links' columns; 902 then runs 120 slices (`sf20`).
- **12** (7 blocks, (b)): a 1 atm water generator 3 m above a nitrogen tank, a three-pipe run between them, a pipe added beside the tank 6 ticks later. In real life water pours down the run, about 14 kg/s at its cap. The new estimate starts 13.67 kg/s (both columns say down; the row reads water) and the first island converges at 0.05 s steps; the old one started 0.013 kg/s (the tank's gas) and reached the same 14 kg/s through millisecond steps. The hold is the second island's first solve 2 ticks after the edit ("No finite Jacobian trial", a trial above 2 MPa), from another point of the trajectory: the start is physical, the first solve of the re-minted island is not reached in 20 iterations. Same kind for 241, 244 and 226 (first-interval Newton limits and a stall on new islands); 902's correction does not clear them (`sf20`).
- **(a) worlds** (246, 758, 96, 71, 432, 761): later jobs that start from the committed end flows and hold on a pass that the old estimate start avoided: Newton limits (246, 758), line-search stalls at 1e-8 to 1e-9 (71, 432), an equation-gate refusal (96, 6.4e-7) and a negative transport reconstruction (761). Each holds again alone in its campaign class (`sf13`). World 758 is traced below (section 6.1).

#### 6.1 World 758 (defaults, 8 blocks, (a))

Two 1 atm water generators, one at y 0 and one at y -2; junctions at y -1 and y 0 lead into a nitrogen tank at y -1 (through its top port from the upper junction); the last pipe, 340 ticks later, closes a loop. Physics: the upper generator fills the tank 1 m below it until the tank stands at the generator's head at its height, 101 325 + 9 780 = 111 105 Pa; the lower generator, which can deliver only 101 325 - 9 780 = 91 545 Pa at y -1, stays shut. The approach is slow at the end, since the drive falls as the gas is compressed. At the hold (tick 650, the job that started at 618) the tank stands at 111 092.5 Pa, 13 Pa short of balance, the junctions at the same pressure to 1e-10 Pa: the island is in its last approach to rest, with flows of the tolerance band's size carried from job to job. The hold is on active-set pass 2 (Newton limit at 0.20) after an active-set change in that job. With the old job start (each job from the estimate) it runs 120 slices (tank at 110 922 Pa at the end; its trajectory differs from the first job on, so the two runs are not at the same point). This is A191's regime (a start inside the tolerance band near rest) and waits for the same definition of rest (D42).

### 7. References (`sf26` on `6b6c94b2`, against t10's `o12`; nothing re-recorded)

| Reference | `o12` | `6b6c94b2` |
|---|---|---|
| G1 suites | 519 / 0 | 521 (+ `StepStartFlowsTest`, `FirstSolveClosureTest`), 7 failures: A191's five, A192's two |
| 33 junction lines | `o12` | 28 move (`sf26-vs-o12-junction-diff.txt`; the same 28 with (a)+(b) alone, `sf07`): 24 MIXED_GAS_TRANSIENT lines at roundoff (worst moles and energy of order 1e-15 to 1e-16, newtonSolves 35 -> 33 and 32 -> 34 in two 5 s cases) and 4 LIQUID_JUNCTION tank lines: 5 s mismatch(end) -0.472 -> -0.453 and -0.0806 -> -0.0725; 0.1 s Q(1s) 2.740e-11 -> 3.366e-11 and 3.103e-12 -> 2.832e-08 kg/s (the rest tail, A191) |
| MIXED_GAS_COST | 309 Newton solves | 309 |
| chain-100 (G2) | red with 6.049e-15, 1.705e-13 K, 1.896e-11 | identical |
| printed suite lines | 6072 | 6164; 5631 of `o12`'s lines move bitwise, 3277 beyond 12 significant digits, 441 beyond 6, 379 beyond 3 (mostly per-slice step and Newton counts: `F7_SLICE`, `PHASE_PORT_PRIORITY_SLICE`, the mixed-gas and extreme-topology prints; `sf26-vs-o12-printed-diff.txt`) |
| GameTest outlets tank | 124258.48752327723 Pa, 175.76227454727493 kg | 124258.48752327717 Pa, 175.7622745472754 kg |
| GameTest compressor | ratio 2.999985907556435, discharge 157976.31408415743 Pa | 2.9999859075564377, 157976.31408415755 Pa |

Every job's first step starts somewhere else, so most trajectories move at roundoff; the visible moves are the rest tail.

### 8. Gate tests and their proofs

- `StepStartFlowsTest` (card E6-S world, `b299fe71`): 120 slices on `6b6c94b2`; with the old job start restored in the working tree (`-Dt15start=static`, scratch patch) it holds at tick 3210 on "Newton iteration limit at residual 0.0788659672470952" (`sf25`). `FluidCheckpointCodecTest` asserts the end flows round-trip in format 7.
- `FirstSolveClosureTest` (card E6-B world, `15afa7ed`, goes with `fde8aa74`): 120 slices on `6b6c94b2`; with the accepted-structure gate restored (`-Dt15closure=gate`) it holds at tick 836 on "Newton iteration limit at residual 8.11618881800676E-5" (`sf21`).

### 9. Final gates (`sf26`, `sf27` on `6b6c94b2`, GameTest run ids `t15-sf26-6b6c94b2-run1`, `t15-sf27-6b6c94b2-run2`)

G1 521 tests, 7 failures (section 7); 33 junction lines as section 7; MIXED_GAS_COST 309; G2 red only on chain-100 with `o12`'s values; G3 45 / 45; GameTests 33 / 33 with the values of section 7. The GameTest values moved, so the gates ran twice: the second run repeats the first exactly (GameTest values, junction lines, all 6164 printed lines).

### 10. Stop points, with recommendations

- **S1, the rest tail (A191).** (a) is physically right and is the largest gain of the batch's E6 work in world defaults (E6 44 -> 33, 883 -> 901, Newton iterations -28 %), but five suite tests that assert exact rest fail, and the near-rest worlds of section 6 (758, and the other (a) worlds) hold in the same regime. Recommended: take (a) and (b) after D42 (thread t16, rest at 0.1 Pa), and re-run the suites and the campaign on the two together; restating the five tests is the owner's alternative.
- **S2, rule (c) (A192).** Net negative on top of (a)+(b) in both campaigns, two suite failures, one of them a physically wrong closure from the junction start's missing hydrostatic restatement. Recommended: drop `fde8aa74` and `15afa7ed`; take (c) after card E9 (a).
- **S3, the first-interval family.** What (a)+(b) leaves in E6 is mostly a new island's first solve (42 of the defaults holds), where neither rule acts: the first step starts from the estimate on the minted states. Card E9 (a) (a hydrostatically restated junction start) is the next structural item there.
- **S4, the branch.** Recommended: not taken as it stands (seven gate failures). Take `8721a138`, `09f3afbe`, `6b6c94b2` and `b299fe71` together with D42 once t16's branch exists, after a joint measurement.

### 11. What differs from the brief

- The committed form of (a) does not keep all of L3's seven suite failures away: it keeps the contract ones (fresh-solver replays, checkpoint round trips) and exposes a rest-tail regime that the old job start had hidden (A191). Two of L3's failures (`ExtremeTopologyIslandTest` ALTERNATING_BOTH_SIDES, the level pair) were not "another step sequence": the first was a carried flow into a generator (fixed, A190), the second the rest tail.
- Rule (b) as first committed applied the column end to end on contracted runs; corrected per link (A193) after tracing the worse worlds.
- Rule (c)'s premise ("a minted junction start is a state") does not hold for its pressure (A192).
- Logs use the prefix `sf`, not `s01`-...: `s01`-`s09` are class E6's earlier logs and `z` is t14's (A180).
- The (a)+(b) measurements of the final code use the scratch switch `-Dt15closure=gate` on `6b6c94b2` (the gate restored exactly), because (c) is committed below the per-link correction.
- Defaults 183 (worse, E8) has no contrast world; random 77 (worse, budget) was replayed alone, not minimized.
- Commit trailers name the model that wrote them (Claude Opus 5.5), not Claude Fable 5.1. Documents are not copied to the main checkout (own worktree only).
## The certificate is the gauge: rest at 0.1 Pa, zero-flow loops, saved crude cases (2026-10-13)

- Author: Claude (Opus 5.5, thread t16 of the orchestrator), worktree `rest-resolution`, branch `claude/rest-resolution` from `f1261205` (the code of campaign `o02`). Owner decisions D42, D43 and D44; rows A210-A223 (numbered A190-A203 on the side branch, A230) in `DECISION_LOG.md`. Code `5c470c50` (the gauge), `519a7ae0` (loops), `fc8c5109` (display), `049b4575` (valves), `09311e8b` (paths of junctions); tests `874c529d`, `f2b6a987`. Logs `z01`-`z23` in `tools/phase-ports-probes/hold-classes/logs/` (A219: the brief's `u` prefix is class F's tracked `u01`-`u04`); tools `t16-*.sh`, `t16.init.gradle`, `src-t16/`, `src/scratch-t16-switch.patch` (never commit); saved cases `tools/phase-ports-probes/saved-cases/`.

### 1. Conclusion

- The rest step now reads the island with an operator's gauge of 0.1 Pa. A drive at or below 0.1 Pa is no drive. This holds on every connection, on every path of junctions between two nodes that carry a gauge (tanks, generators, voids), and on every loop's net drive. At a step's end, a flow at or below what 0.1 Pa drives through its connection is no flow. The test runs at every interval start and now also after every accepted step: an island that reaches the gauge is no longer stepped. The state is never rounded, and the flow law has no dead band.
- Campaign `z18` on the final code: **923 random, 894 world defaults** (`o02` 919 / 883). Random: 5 cleared, 1 worse (761, a trace in a steady vent, section 5). World defaults: 11 cleared, 0 worse. Newton iterations fall 4 % random and 3 % defaults.
- Ten of the twelve tie worlds of `o02` clear. The other two hold on mechanisms that are not rest: defaults 78 holds later on a real drive of 461 Pa (E6), and defaults 719 fails its first Newton at 0.31 Pa, above the gauge (E6). The gate worlds hold with the old rest test and certify "STEADY: no flow" with the gauge (section 6).
- One reference moved outside the 33 junction lines, MIXED_GAS_COST and the printed lines: `SolidChainTransportTest`'s recorded substep counts (A222, not re-recorded; the test is red). Seven assertions that expected stepping below the gauge are restated for the owner (A221). The GameTest printed values are identical.
- D44: the random crude two-phase holds are minimized and saved under `tools/phase-ports-probes/saved-cases/` (section 8) and leave the zero goal.

### 2. Physics, in real-life order

- **A tie.** Two identical 1 atm water generators stand 2 m up on equal legs and feed one junction. The junction feeds two identical nitrogen tanks on equal legs. Water falls and compresses both gases towards 120.86 kPa (1 atm plus the column). The drive is each tank's pressure deficit; it halves every few seconds as the tanks fill. After about a minute it is below 0.1 Pa. In a real plant the operator now reads the same pressure on every gauge of the line, to the gauge's last digit, and calls the tanks full. The remaining approach (1e-3 Pa, then 1e-6 Pa) is smaller than any real line's pressure fluctuation. The generators supply equal halves of nothing.
- **What the solver did before** (`o02`, section 4). It went on stepping the approach at full double precision. The drive went 1e-1, 1e-3, 1e-5, 1e-7 Pa and the flows 1e-4 to 1e-9 kg/s, until the Newton worked at a root where every flow was roundoff. It ended on its iteration limit, a stalled line search, a singular Jacobian, the reconstruction's equation gate, or a roundoff flow that booked more of a trace than its node held (card E9's "Negative transport reconstruction"). The island was held although nothing moved.
- **A zero-flow loop.** A generator joined to a ring of pipes, the same water on both sides of the ring. Around the loop the pressures cancel and the columns are equal, so nothing drives a circulation; in real life the water stands still. The solver solved the ring's roundoff circulation and held the same way.
- **A loop with a net drive** (not covered by the rest rule): legs with fluids of different density, or different liquid heights, drive a circulation (a thermosiphon, a gas lift). Each connection may read below the gauge while the loop as a whole is driven: 1 Pa net around ten equal connections leaves 0.1 Pa on each.
- **A chain of tees.** A gauge stands on a tank, a generator, a void; a junction (a tee) carries none and holds no stock. A tank still 0.3 Pa below its generator across three tees reads 0.3 Pa on its gauge and keeps filling, even if each tee is only 0.1 Pa from the next.

### 3. The rules, stated before coding, and as implemented

**(a) The certificate resolution (D42).** One test in one place: the rest step of the network reduction stage (`NetworkReduction.atRest`, step 6, D19). The interval solver asks it on the island's committed state at every interval start, as since D19 and before the interval-start refresh (A213). It now also asks it on every accepted step's end state and flows (A210). The first accepted point at which the gauge reads rest ends the stepping; the rest of the interval is the identity. The STEADY certificate of a flowing island is not changed: a steady flow above the gauge is real and replays as it is.
- *Drive, per connection kind* (A211). A passive run or inline filter: the static drive in each direction its one-way rules allow (a generator only supplies, a void only receives, a blocked direction carries nothing). That is the end pressures (a tank's bottom port with its level head, D9) less the column between the ends, in the density of what the donor end's port draws first (D11): step 5's `directions` test as a number (`NetworkReduction.drives`). A pressure valve passes only from its first end and only while that end stands above its set point. Its forward drive is the smaller of its static drive and that excess, and it has no reverse drive; the rest interval reports it CLOSED. A pump or compressor at its target moves its set flow whatever the pressures. It is a drive of any size, so its island stays on the solver (A72/A80).
- *Between gauges* (A220). Every path of junctions between two nodes that carry a gauge (tanks, generators, voids) is read as one drive: the sum of its connections' drives, in which the junction pressures cancel (`NetworkReduction.pathDriven`). This was added after the first full gates: without it, `ExtremeTopologyIslandTest`'s grid certified tanks at rest 0.20 to 0.29 Pa below their generator, still filling.
- *Flow* (A212). The flow 0.1 Pa drives through a connection is its own law on its laminar branch: 0.1 Pa over the slope of its pressure drop at zero flow (Poiseuille summed over its sections; an inline filter's clean cake law at its present loading). It is taken in the fluid each end's port draws first, the smaller of the two ends: 0.017 kg/s in a water run of 1 m and 0.05 m (Reynolds 490), 0.001 kg/s in the same run in 1 atm nitrogen. At an interval start there is no flow to read (flows are not state), and the drives decide alone.
- *At commit* (A214). The sub-resolution flows of the last solved step are left as solved, not zeroed. That step's reconstruction is exact for those flows; zeroing them would book transfers that disagree with the solved end state, or throw the step away. Both alternatives move less than the gauge reads, and leaving them keeps conservation exact with no special path. From that point nothing moves: every later interval is the identity map (`restInterval`), and the island certifies "STEADY: no flow" after the policy's two confirming intervals.
- *What wakes it* (A215). Nothing new: a topology edit, a module drive, a property resume or the policy's recheck. The next interval asks the same test on the new state. A neighbour cannot raise a drive: islands meet only at fixed boundaries (D17), whose state nothing changes, and an edit of a fixed boundary replaces all its islands.

**(b) The loop rule (D43, A216).** A loop is a cycle of connections among the nodes that are not fixed boundaries (junctions and tanks): nothing circulates through a generator or a void. The test takes the fundamental cycles of a breadth-first spanning forest of those nodes, each closed by one connection outside the forest. A loop's net drive one way is the sum of its connections' static drives that way. The pressures cancel around it, so what is left is its columns and level heads: the thermosiphon drive. The island is at rest only when every loop's net drive both ways is at or below 0.1 Pa (`NetworkReduction.loopDriven`). A loop with a net drive above it circulates and is solved as before. Circulation with a net drive is not covered by the rest rule; a steady circulation is the STEADY certificate's.

**(c) Display (A217).** The device rail (`FluidDeviceScreen.renderDeviceRail`) wrote a pressure as `number(p / 1000)` kPa with one decimal: steps of 100 Pa. It now writes `ProcessUi.pressure(p)`: the pressure rounded to 0.1 Pa, in kPa with four decimals (101.3250 kPa). It is used for the node's pressure and for a mover's pressure change. Only the text is rounded; the snapshot the server publishes is untouched (AGENTS.md "Player-facing updates"). Not checked in the dev client: the brief allows only the GameTest gate.

**What the rules absorb** (A218). The rest step's "no band" (D19, A72); card E9's three options for its tie world; the A73 fallback for loops of junctions in void-only islands; A72/A80's exclusion of valves. The solver's own bands (`reopenBand`, `shutoffBand`, the 1e-10 kg/s illegal-direction threshold) are pass-loop hysteresis inside a solve and stay. The STEADY certificate's quiet-pipe exemption stays for flowing islands; the settled ladder it was tested on is now at rest by the gauge (A221).

### 4. The tie worlds (family B and family C): `o02` before, final code after

Every held seed of `o02` was probed at its hold (`src-t16/T16TieProbeTest`, logs `z01`, `z02`). The probe printed the held island's largest allowed passive drive, its last flows against the resolution flow, its loops and identical generators, and its last four committed intervals. Twelve holds are an approach to rest: the drive falls by a factor of 2 to 1000 per interval until the Newton fails at a root of roundoff flows. Eight of them have loops (family C). The other first holds sit at drives of 26 Pa to 700 kPa. Ten more islands hold with every passive drive zero but carry a pump or compressor at its target; they are not ties (A211). Of `o02`'s class C seeds none is a tie. Of its F seeds, replayed alone on the base code (`z10`, all eleven hold on their budget), defaults 705 is a tie (0.03 Pa at its last interval start) and defaults 71 is an approach to rest (250, 89, 72, 26 Pa). Of its E9 seeds only defaults 47 (the card's world) is a tie.

| Seed | `o02` class | Drive at the held interval's start (Pa) | Loops | Final (`z18`) |
|---|---|---|---|---|
| defaults 47 | E9 | 9.4e-7 (two identical generators) | 2 | clean |
| defaults 78 | E4 | 3.4e-3 | 1 | E6 later, on a real 461 Pa drive |
| defaults 138 | E5 | 6.2e-6 (two identical generators) | 0 | clean |
| defaults 311 | E5 | 0.17, crossing the gauge inside the interval | 9 | clean |
| defaults 616 | E6 | 2.7e-10 | 2 | clean |
| defaults 651 | E6 | 2.9e-3 | 1 | clean |
| defaults 705 | F | 3.0e-2 | 0 | clean |
| defaults 719 | E6 | 0.31 (a new island's first solve) | 2 | E6, same hold (above the gauge) |
| random 188 | E6 | 1.5e-6 | 2 | clean |
| random 311 | E2 | 3.0e-4 | 9 | clean |
| random 553 | E5 | 2.5e-9 (through an open valve) | 1 | clean (A211's valve reading) |
| random 807 | E5 | 1.7e-4 | 1 | clean |

### 5. Campaign (`z18`, final code `f2b6a987`, 4 workers beside another lane; `z08` on `874c529d` is identical seed for seed)

| | `o02` random | final random | `o02` defaults | final defaults |
|---|---|---|---|---|
| clean of 1000 | 919 | **923** | 883 | **894** |
| Newton iterations | 2 962 471 | 2 847 653 | 19 879 189 | 19 313 960 |
| Newton solves | 1 108 474 | 1 091 659 | 2 268 689 | 2 148 098 |
| equation-gate refusals | 203 816 | 207 150 | 160 640 | 154 493 |
| rest intervals / rest stops | - | 50 239 / 112 | - | 56 069 / 13 |

- Random: cleared 5 (E5 553 629 807; E2 311; E6 188), worse 1 (E9 761), moved 511 E6 -> E8, 707 F -> C.
- World defaults: cleared 11 (E6 17 164 616 651; E5 138 311; F 71 705; E2 681; E8 629; E9 47), worse 0, moved 78 E4 -> E6, 322 E3 -> E6, 579 E9 -> F, 720 E6 -> E2, 788 C -> E5, 844 E6 -> E4.
- Classes `o02` -> final (random / defaults): C 15 -> 16 / 5 -> 4; E2 9 -> 8 / 13 -> 13; E3 0 / 3 -> 2; E4 0 / 7 -> 7; E5 6 -> 3 / 14 -> 13; E6 30 -> 28 / 44 -> 40; E7 3 / 3; E8 6 -> 7 / 12 -> 11; E9 3 -> 4 / 6 -> 4; E10 4 / 4; F 5 -> 4 / 6 -> 5.
- Alone (`z21`, one worker, alone lock): every budget hold (random 246 431 678 788, defaults 156 579 757 833 994) holds on its budget again, and the worse seed 761 holds in E9 again.
- **The worse world, random 761, traced** (`z11`, `z12`, `z13`). Minimized by contrast to 14 blocks (held with the gauge, clean with the old exact test). A 107 kPa, 384 K methane generator stands on top of a nitrogen tank and vents through it into two 1 atm voids, one through a long 0.01 m branch. Physics: the methane washes the tank's nitrogen out exponentially and the vent settles at 0.108 kg/s on a 5.5 kPa drive, a real steady flow. The held island is that steady vent: identical flows over its last 400 s, the tank holding 5.7e-23 mol of nitrogen beside 33.5 mol of methane (a share of 1.7e-24, just above the trace flush fraction 1e-24, D16). A step booked more of that trace than the node held: card E9's mechanism (c), not a rest. The two runs first differ at tick 135, in an earlier island (the generator alone on the tank, filling it): the gauge certified that tank at rest at 106 925.2389 Pa where the old test went on to 106 925.2402 Pa. That 1.3e-3 Pa moved the step at which the washed-out trace crosses the flush fraction.

### 6. Gate tests and their proofs (`874c529d`, `f2b6a987`; proofs `z07`, `z20` with `src/scratch-t16-switch.patch`, `-Pt16mode=exact`: the rest test of D19, working tree only)

| Test | World | With the gauge | Old rest test |
|---|---|---|---|
| `RestResolutionWorldTest.twoIdenticalGeneratorsOnOneJunctionComeToRestAtTheGauge` | two identical 1 atm generators 2 m up on equal legs, one junction, two identical tanks on equal legs (defaults 138 minimized and mirrored) | "STEADY: no flow" from 63 s; tanks 120 859.997 and 120 859.992 Pa | held at tick 1363, equation gate 3.8e-6 |
| `RestResolutionWorldTest.aLoopWithNoNetDriveIsAtRest` | a 1 atm generator over a ring of pipes 2 m deep, a void above (defaults 616 minimized, 11 blocks) | at rest from its first interval, the ring a loop of the island | held at tick 119, Newton iteration limit 3.02 |
| `RestResolutionWorldTest.cardE9sWorldIsAtRest` | card E9's world (defaults 47 minimized, 12 blocks) | at rest | held at tick 692, negative transport reconstruction |
| `RestResolutionTest` (graphs) | a junction 0.05 / 0.2 Pa from its ends; flow resolution water / nitrogen; a chain of tees; a ring of four junctions | at rest at 0.05 Pa, not at 0.2 Pa; 0.0172 / 0.00099 kg/s; 4 x 0.05 Pa on a chain or a ring is driven, 4 x 0.02 Pa is not; nothing circulates through a generator | - |

A hand-built tie (two generators level with one junction feeding a tank's top) did not serve: it holds at tick 215 in both modes on "Invalid inventory volume/energy" while the water falls into the gas (the mechanism of E9 seeds 19, 23 and 273), long before any rest.

### 7. References (`z19` on `f2b6a987` against t10's `o12`; nothing re-recorded)

| Reference | `o12` | final |
|---|---|---|
| Fluid suites | 519 / 0 | 526 / 1 (the seven new tests pass; `SolidChainTransportTest`'s recorded counts, A222) |
| 33 junction lines | reference | all 33 lines move: LIQUID_JUNCTION `tanks` cases reach rest (0.1 s: flow at 1 s 2.740e-11 -> 0 kg/s and 3.103e-12 -> 0; 5 s: newtonSolves 17 -> 4, mismatch after the first slice NaN, a ratio at zero flow); MIXED_GAS_TRANSIENT conservation figures move between 1e-16 and 1e-15 (`z19-junction-lines-vs-o12.diff`); `generators` lines identical |
| MIXED_GAS_COST newtonSolves | 309 | 264 |
| chain-100 (exact regression) | 6.049e-15 / 1.705e-13 K | 6.049e-15 / 1.705e-13 K (unchanged; red against its recorded reference as on `o12`) |
| G3 adjacent | 45 / 45 | 45 / 45 |
| GameTests | 33 / 33 | 33 / 33 (run id `t16-z19-f2b6a987-run1`) |
| GameTest outlets tankP / tankMass | 124258.48752327723 / 175.76227454727493 | identical |
| GameTest compressor ratio / discharge | 2.999985907556435 / 157976.31408415743 | identical |
| `SolidChainTransportTest` substeps (accepted / rejected) | half 3/1, full 5/1, two 4/0 | 2/1, 2/1, 2/0 |
| printed suite lines | 6072 | 6085; 1317 only in `o12`, 1330 only in final (`z19-vs-o12-printed-diff.txt`): `PHASE_PORT_PRIORITY_SLICE` 1304 (drains at rest early), `FED_PUMP` 800, F7 lines 348, `MIXED_GAS_TRANSIENT` 34, `EXTREME_TOPOLOGY` 48, `LEVEL_HEAD` 26, `LIQUID_JUNCTION` 8, `STACK_*` 4, `VESSEL_CLOSURE` 2 (the grid's highest tank 5.4e-5 -> -7.1e-3 Pa from its generator), the ladder line (certified at tick 400 replaying its noise flows; now at tick 300, at rest) |

No GameTest value moved, so one GameTest run.

### 8. D44: the random crude two-phase holds, saved (logs `z22`, `z23`; A223)

The random E2 and E5 seeds of `z18` were minimized keeping their class and a two-phase hydrocarbon node in the held island (`src-t16/T16CrudeCaseTest`, `t16-crude.sh`), then replayed once with t13's named-row trace (working tree only). Every case reproduces its hold to the tick. Seven are saved in `tools/phase-ports-probes/saved-cases/<seed>/` (`case.txt`: the blocks as the fuzz harness places them with their full specs, the first hold, the held island; `trace.txt`: the failing rows; `card.md`: the card). They are listed in `REMAINING_HOLDS.md` under "Saved for the thermodynamics work (D44)" and leave the zero goal (random 923 clean + 7 saved = 930).

| Seed | Class | Blocks | Hold tick | Failing row | Two-phase node |
|---|---|---|---|---|---|
| 279 | E2 | 6 | 2866 | tank's flow row 3.4e-9 | tank 292.4 kPa, 325.5 K, 54 % liquid |
| 298 | E2 | 13 | 4089 | two junctions' continuity rows +-0.44 | three tanks 215-225 kPa |
| 372 | E2 | 14 | 3636 | two-phase junction's continuity row 1.0e-6, its fugacity rows all 8.7e-10 | crude-nitrogen junctions |
| 521 | E2 | 6 | 1195 | tank's top-port flow row -0.028 | tank 299.4 kPa, 329.7 K |
| 547 | E5 | 13 | 1936 | junction's continuity row 13.44 (stall) | junctions draining into 1 atm voids, one 1.9 kPa below them |
| 786 | E2 | 8 | 1690 | tank's energy row 2.7e-9 | tank 229.7 kPa, 320.3 K |
| 818 | E2 | 5 | 2974 | junction's continuity row 1.7e-8 | tank 194.1 kPa, 309.2 K |

Not saved: random E2 163 and 854, E5 815 and 829 (no two-phase hydrocarbon node at the hold; water or gas nodes). `o02`'s random E2 311 and E5 553, 629 and 807 are clean on the final code.

### 9. Stop points, with recommendations

- **A221: seven restated assertions.** Recommended: confirm. Each now reads the gauge; physics does not tell the old and new end states apart (about 10 g of water left in a 1 m3 tank, two bottoms 0.03-0.095 Pa apart, a settled ladder of noise flows at rest).
- **A222: `SolidChainTransportTest`'s recorded counts.** To be re-recorded at the owner's re-baseline (D36/D38), with the 33 junction lines, MIXED_GAS_COST and the printed lines above.
- **A217: the display** is not checked in the dev client. Recommended: one look at a tank's rail at a small scaled screen before the merge.
- **A211: movers.** A pump or compressor keeps its island on the solver. Recommended: leave it so until a hold class shows a dead-headed mover at rest; none of `o02`'s does.
- **Remaining tie-adjacent holds:** defaults 719 (a new island's first solve at 0.31 Pa, card E6-S/E6-B) and random 761 (a trace at the flush fraction in a steady vent, card E9 (c)). Neither is a rest.

### 10. What differs from the brief

- Logs use the prefix `z`, not `u` (A219).
- The rule grew two parts the brief did not name, each forced by a measurement: valves read statically (A211, random 553) and paths of junctions read between gauges (A220, the extreme grid).
- Seven assertions were restated (A221) and one recorded reference left red (A222); the brief did not foresee assertions that expected stepping below the gauge.
- The D44 cases were minimized on the final code, not on `o02`'s: they are the holds the thermodynamics work will start from (A223).
- Commit trailers name the model that wrote them (Claude Opus 5.5), not Claude Fable 5.1.
- Documents are not copied to the main checkout (own worktree only).

## Integration of the carried gas, the committed flows and the gauge; the virtual junction holdup (2026-10-13)

Thread t17, branch `claude/integration-1` from `d6ad5ec9` (owner decision D46 recorded). Decision rows A230-A240. Logs `ig01` onward in `tools/phase-ports-probes/hold-classes/logs/` (`i01`-`i2x` are t5's logs).

### 1. What is on the branch

| Step | Source | Commits on the branch |
|---|---|---|
| Merge, whole | `claude/bubble-through` (t14, D41) | merge `92f38972` of `c767503a`, `dbebfddd`, `09cd3ae0`, `07d6af87` |
| Cherry-pick, rules (a) and (b) | `claude/start-flows` (t15) | `75a6b48a` (`8721a138`), `e7652ff5` (`09f3afbe`), `c0a3cea4` (`6b6c94b2`), `6b773d60` (`b299fe71`), `a9072a09` (`fdf4f97c`, docs and tools) |
| Not taken | rule (c) of t15 | `fde8aa74` (code) and `15afa7ed` (its gate test `FirstSolveClosureTest`): they return after card E9 (a), t15's S2 |
| Merge, whole | `claude/rest-resolution` (t16, D42-D44) | merge `01d536db` of `5c470c50` to `f42948ec` |
| Renumbering | t16's rows A190-A203 | `87fd45be` (A230) |

The code merged and cherry-picked without a conflict and compiles. The documents conflicted only where two threads appended to the same place (decision log, review, remaining holds, tools README) or edited the same long line (CHANGELOG, INDEX). Each keeps both sides, in thread order (t14, t15, t16). t15's CHANGELOG line ended its log range as `sf01`-; the range is `sf01`-`sf27`.

**A-rows.** t15 used A180-A193 and t16 used A190-A203. t16's rows move by 20 in the same order (A190 -> A210, ..., A203 -> A223) in every file that cites them (A230). Commit messages cannot change: t16's commits cite A190-A203 (`f2b6a987`'s message says A200 for the restated assertions, which are A201 there and A221 here) and t15's cite A180-A193 in their own numbering, which did not move. The logs of t14 and t16 both use `z01`-`z23` with different names after the number; a citation names its thread (t14's campaign `z16`, t16's campaign `z18`).

**t15's five exact-rest tests under the gauge** (A191 of t15: a near-rest tail persisted because a Newton start inside its tolerance is accepted). Suites on the integrated code before D46 (`ig01-pre-d46`): 528 tests, 1 failure, `SolidChainTransportTest`'s recorded substep counts (A222, red until the re-baseline).

| t15 failure | On the integration | Why |
|---|---|---|
| `IslandCertificateTest.aSettledClosedPairCertifiesWithALongHorizon` (drift 1.6e-12, 0 expected) | passes, assertion unchanged | the pair is certified at rest by the gauge and not stepped |
| `FluidCheckpointFormatTest.clocksMaterialFencesPendingEventsModulesAndCertificatesRoundTripExactly` | passes, unchanged | the same pair |
| `FluidCheckpointFormatTest.aRestoredCertificateContinuesWithoutASolveExactlyAsIfNeverSaved` | passes, unchanged | the same pair |
| `LevelHeadTest.twoTanksAtOnePressureEqualiseTheirBottomsMonotonically` (17 breaks of 1e-5 Pa) | passes; the monotone check is unchanged, its rest tolerance is t16's restatement (A221) | the island stops at the gauge before the tail can break monotony |
| `IslandCertificateTest.aSettledBenchmarkClosedLadderCertifiesOnceItsQuietPipesAreExempt` (the flow-test premise) | passes on t16's restated assertion (A221) | the ladder is at rest; the premise it lost is a stepping premise |

So three pass with their assertions untouched, one passes on its untouched monotone check with a restated rest tolerance, and one passes only through A221. None needed a trace.

### 2. The junction holdup is virtual (D46): the rule

Written before the code. The owner's words are in D46.

**What a junction's buffer is.** A junction is a pipe fitting full of fluid. Its row is the backward-Euler mixing row of D25, `(m_J/dt + Q_in) w = (m_J/dt) w_old + sum q_e w_e` for the mass fractions and the same form for the specific enthalpy, with `m_J` = 0.05 s of the largest velocity-cap flow at the mint. The buffer is the state that row carries from step to step: `m_J`, `w` and `h`. It is stored where it always was, in the junction's inventory record (`m_J w` as amounts, `m_J h` as the energy field), because the next step's row reads exactly those numbers. It is not inventory: the junction owns nothing.

**What the ledger closes over.** Vessels (tank inventories), filter cakes and boundaries (generators, voids, scheduled injections and withdrawals, and the trace flush records of D16). Per step and per interval: `change of vessels and cakes = boundary transfers + J`, per component, per solid population and for energy (with pump work), exactly to roundoff.

**The junction term J.** Per component c: `J_c = sum over the island's junctions of m_J (w_c,new - w_c,old) / M_c` (moles), the change of the buffer's content over the step; per solid population the same; for energy `J_E = sum m_J (h_new - h_old)` (the potential term `m_J g z` does not change while `m_J` does not). The change form, not the content form, because the ledger is a balance of changes: a content (`sum m_J w`) is a stock, and a stock would have to be booked where it appears and vanishes (a mint, a destroy), which is what D46 removes. Over a transient inside one island's life the summed changes equal `content(t) - content(mint)`. A rate evaluation (a frozen junction, never committed) holds its buffer and its held accumulation over `dt` is its J; before D46 that accumulation was a pseudo-boundary at the junction.

**The guarantee and its proof.** `|J_c| M_c <= sum m_J` for every component and solid population, per step and summed over any stretch of one island's life; `|J_E| <= sum m_J H`, with H the span of the specific enthalpies (`h_e + g dz_e`) that entered the junctions and of their start enthalpies. Proof from the row: its diagonal is `m_J/dt + Q_in` and the right side's weights `m_J/dt` and `q_e` are nonnegative and add up to the same number, so `w_new` is a convex combination of `w_old` and the inflow streams' fractions. Every `w_c` stays in [0, 1] and the content `m_J w_c` in [0, m_J]; a change of a quantity confined to [0, m_J] is at most `m_J` in magnitude. The enthalpy is a convex combination with the same weights, so it stays inside the span H. The reconstruction states the same row with the booked flows (below), whose diagonal is `endMass + dt Out' = m_old + dt In'`, so the same argument holds for what is committed.

**The booked flows keep `m_J` the same** (as it reads under D46). The Newton closes a junction's mass row to its tolerance, so `In - Out` is not exactly zero. The reconstruction books a junction's outflows scaled to what came in (or its inflows to what went out when it has no outflow), so the fitting passes exactly what it receives and its mass stays `m_J` (`ConservativeTransport.pinnedFlows`). Under D46 this is not ownership: it is the fitting's mass continuity, and it is what keeps `m_J` the same number from step to step (the Newton reads `m` from the buffer record) and the row's weights adding up. It stays. Removing it moves the committed numbers (measured: the last bits of the mixed-gas and generator transients, `ig05`) and lets `m_J` drift as a random walk at the Newton's mass-row residual (2.2e-15 relative over 20 s there), with nothing to restore it (A232).

**Mint.** A new junction starts by the start rule (D37, D39, D40, A133, t9's balance pressure) and is sized by D25's rule from that state. Nothing is booked: its buffer appears outside the ledger.
**Destroy and a lost run.** Every edit replaces the island; its junctions' buffers leave with it. Nothing is booked. D26's booked removal of a lost run's few grams is retired (D46 calls it D16's grams; the trace flush of D16 is a different rule and stays, below).
**Re-mint.** Every edit re-mints (D45: kept contents not taken), so every junction of an edited island starts again by the start rule, and the term restarts with the island.
**Checkpoint.** The buffer state is carried in the junction's record as before (fresh world only, no migration). The world ledger's constructed and destroyed totals no longer include junctions.
**Trace flush (D16) at a junction.** The interval solver still removes a component below 1e-24 of the junction's buffer and books the removal as a boundary record at the junction's id. The flush decides which components exist (D34), the removed trace leaves the world, and its record is what refuses a certificate on that interval (a one-time removal is not a rate). The term sees the buffer's drop, the boundary sees the removal, and the ledger closes.

**Rows of the side branches that touched ownership.**
- t14's A174 (a water-full junction at rest keeps its carried gas until flow resumes; in real life the bubble rises into the next gas space) is moot: a buffer is a mixing state, not a place a bubble can sit. Nothing is coded for it. A175 ("nothing is booked" for the carried gas) is unchanged.
- t14's two-form trace rule (A171, A172) and the reconstruction's statement of a junction through it (A176) stay: they decide the junction's phases, a solver matter.
- t15's rules (a) and (b) carry flows, not contents. A183 (after an edit nothing is carried) is the re-mint read for flows. No t15 row touches ownership.
- t16's A220 ("a junction holds no stock and carries no gauge") is the same reading as D46.
- K's re-size (A143) and displacement criterion (A154-A157) were never on this branch (D45): there is no code to remove.

**What changes in the code.** Removed: the constructed and destroyed bookings of junctions in `PhysicalRegistry.apply` (the world ledger books tanks only); the pseudo-boundary of a frozen junction in `ConservativeTransport.reconstruct` (it becomes the projection's junction term); junction inventories in the vessel sums of `PassiveStepSolver.checkConservation` (the junction term is its own term there). None of these produces a solver number: the world ledger's totals are never read by a solve; a reconstruction's boundary records and external totals are read only by `checkConservation` (and committed only from step solves, which have no pseudo-boundary); `checkConservation` only returns or refuses, and its arithmetic is the same identity regrouped. Unchanged: the sizing, the start rule, both statements of the row (Newton and reconstruction), the booked flows, the buffer record, the D16 flush and the certificate's replay of the buffer state.

### 3. D46 as implemented, the bitwise proof and the gate test

**Code** (`5f0f750c`, `074632b6`; A233). `PhysicalRegistry.apply` no longer collects the replaced island's junctions or the recompile's minted junctions for `WorldTopologyLedger.applyBatch`: the world ledger books tanks only. `ConservativeTransport.reconstruct` gathers a rate evaluation's held accumulation into `Projection.held` (`HeldJunctions`: net amounts, energy and solids, and per-junction gross magnitudes) instead of a `BoundaryTransfer` at each junction's id. `PassiveStepSolver.checkConservation` sums vessels and cakes on one side and the junction term (the buffers' change plus the held accumulation) as its own term. Its three tolerances keep the scales they had: the turnover of the boundaries plus the held magnitudes (as the pseudo-boundaries carried them, junction by junction), and `max(before, after)` over vessels and buffers together. The second commit made those scales exact (the first counted the held accumulation net and the buffers through `max(a, b)`); the first commit's campaign `ig06` and the final `ig10` clear the same seeds in both distributions. Comments that said a junction "owns" stock now say buffer (`PassiveNetwork`, `PassiveStepSolver.HOLDUP_TAU`, `ConservativeTransport`).

**Bitwise proof** (A236). `VirtualJunctionHoldupTest` digests (SHA-256) every committed temperature, pressure, average flow, end flow, head and node record (tank inventories and junction buffers) of every slice of three transients at 0.1 s and 5 s. With the ownership code restored in the working tree (`src/scratch-t17-ownership-restored.patch`, the reverse of both D46 code commits) the six digests are identical (`ig03`, `ig04`):

| transient | slices | digest (both trees) |
|---|---|---|
| mixed-gas 0.05:4:true, 0.1 s | 160 | `79d0f8c2...cebd0417` |
| liquid tanks 0.05, 0.1 s | 200 | `81f81158...e15e661a` |
| liquid generators 0.05, 0.1 s | 200 | `696bf48d...c9573843` |
| mixed-gas 0.05:4:true, 5 s | 4 | `81dcd021...b94b7b22` |
| liquid tanks 0.05, 5 s | 4 | `80ec1ebe...af4c21dd` |
| liquid generators 0.05, 5 s | 4 | `9c1c5202...deda6f0d` |

The final code's G1 (`ig08`) prints the same six digests.

The whole fluid suite gives the same result: G1 with the ownership code restored (`ig09`) prints the same 6097 lines as G1 on the final code (`ig08`), bit for bit, the 33 junction lines included; the only differences are the restated `PhysicalRegistryTest` failing on the restored code (it expects no junction booking) and its last print not reached. Control (`ig05`, `src/scratch-t17-no-pin.patch`): with the booked flows of A232 switched off, the mixed-gas and generator digests change and the resting tanks' do not, so the digest sees a last-bit change.

**Gate test** `VirtualJunctionHoldupTest.theJunctionTermStaysWithinTheSumOfMjAndTheTankLedgerClosesAtEverySlice`. At every slice of the same six runs: the tanks' change equals the boundaries plus the junction term to 1e-12 of max(1, initial) (component and energy), the term's component masses stay within the sum of `m_J`, its energy within the sum of `m_J` times the span of the specific enthalpies seen, and each buffer keeps its `m_J` to 1e-12. Measured worst figures (`ig08`):

| transient | sum m_J (kg) | ledger, components / energy | junction share (bound 1) | energy share (bound 1) | m_J drift |
|---|---|---|---|---|---|
| mixed-gas, 0.1 s | 0.00813 | 3.7e-16 / 1.8e-15 | 0.069 | 0.928 | 1.5e-15 |
| liquid tanks, 0.1 s | 9.774 | 1.2e-17 / 3.2e-16 | 2.1e-16 | 0.0027 | 1.8e-16 |
| liquid generators, 0.1 s | 9.774 | 9.6e-15 / 4.9e-15 | 4.2e-16 | 0.364 | 3.6e-16 |
| mixed-gas, 5 s | 0.00813 | 2.4e-16 / 4.4e-16 | 0.063 | 0.918 | 6.4e-16 |
| liquid tanks, 5 s | 9.774 | 7.3e-17 / 8.5e-17 | 2.1e-16 | 0.0027 | 1.8e-16 |
| liquid generators, 5 s | 9.774 | 2.7e-15 / 2.3e-15 | 8.4e-16 | 0.364 | 7.3e-16 |

The water cases' component share is roundoff because the buffer holds water before and after (only its temperature moves); the mixed-gas buffer swings between methane and nitrogen, 7 % of the bound in the four-port case and up to 64 % in the six-port cases (`MIXED_GAS_TRANSIENT` lines). The energy share of the mixed-gas case, 0.93, shows the enthalpy bound is close to tight there.

**Tests restated** (A235, for the owner):

| test | before | after |
|---|---|---|
| `PhysicalRegistryTest.junctionHoldupsAreBookedAsConstructedAndRemintedAtATopologyEdit`, renamed `junctionBuffersAreSizedAndRemintedAtATopologyEditAndNothingIsBooked` | placement books the two tanks and every minted junction as constructed; the edit books the solved junctions destroyed and the recompile's junctions constructed | placement books the two tanks only; the edit books nothing; sizing and remint asserted as before |
| `MixedGasJunctionTransientTest`, `LiquidJunctionTransientTest` | ledger of tanks plus the junction's inventory against the boundaries, 1e-12 | the same ledger through `JunctionLedger`: tanks and boundaries with the junction term, 1e-12, term within m_J; the lines print `junctionShare` |

### 4. The 33 junction lines

All 33 lines differ from `o12` on the integrated code, none from D46's solver numbers: D46 moves only how the conservation figures are summed (tanks, then the junction term) and adds the term's share. The moves by source:

| source | lines | what moves |
|---|---|---|
| t14 (carried gas) | 0 | `z23` = `o12` on all 33 |
| t15 (committed flows) | 28 | 24 mixed-gas ledger figures at 1e-16 to 1e-15; the four liquid tank lines' tail (mismatch(end) -0.472 -> -0.453 and -0.0806 -> -0.0725, Q(1s) at 0.1 s 2.74e-11 -> 3.37e-11 and 3.10e-12 -> 2.83e-8) |
| t16 (gauge) | 33 | the liquid tanks rest (0.1 s flows -> 0, mismatch NaN since no inflow, 5 s newtonSolves 17 -> 4); mixed-gas newtonSolves at 5 s (MIXED_GAS_COST 309 -> 264) and ledger figures |
| integration before D46 (`ig01`) | 24 of 33 against t16 | the mixed-gas ledger figures at 1e-16 (t15's start flows on t16's gauge); the liquid lines and MIXED_GAS_COST equal t16's |
| D46 (`ig08` against `ig01`) | 32 | the 24 mixed-gas ledger figures (regrouped sums, 1e-16) and a new `junctionShare` field on all 32 mixed-gas and liquid lines; MIXED_GAS_COST unchanged |

The full table, line by line and field by field (`o12`, t14 `z23`, t15 `sf26`, t16 `z19`, `ig01`, `ig08`), is `logs/ig14-junction-table.md` and section 9's re-baseline table. Nothing is re-recorded.

### 5. The joint campaign

Campaign `ig10` on `074632b6` (8 workers, 1000 seeds each); the first D46 commit's campaign `ig06` clears the same seeds. Worse seeds and budget holds alone in `ig12`.

| | `o02` | t14 `z16` | t15 `sf22` | t16 `z18` | **now `ig10`** |
|---|---|---|---|---|---|
| random clean | 919 | 933 | 923 | 923 | **935** |
| world defaults clean | 883 | 930 | 901 | 894 | **957** |
| Newton solves random / defaults | 1.108 M / 2.269 M | 0.394 M / 1.382 M | 1.242 M / 1.850 M | 1.092 M / 2.148 M | **0.305 M / 1.247 M** |
| Newton iterations random / defaults | 2.96 M / 19.88 M | 2.25 M / 11.40 M | 4.40 M / 14.26 M | 2.85 M / 19.31 M | **1.78 M / 12.34 M** |
| gate refusals random / defaults | 203 816 / 160 640 | 23 609 / 44 032 | 240 110 / 221 809 | 207 150 / 154 493 | **22 847 / 20 185** |
| first-interval family random / defaults | 42 / 32 | 41 / 19 | 44 / 42 | 41 / 32 | **44 / 23** |

(t15's `sf22` ran on 4 workers.) Against the D44 goal line: random 935 clean + 7 saved crude cases (E2 279 298 372 521 786 818, E5 547: all seven still hold, 547 now in budget) gives 942 of 1000 counted against the batch (58 held).

**Classes** (random / world defaults):

| class | `o02` | t14 | t15 | t16 | now |
|---|---|---|---|---|---|
| C | 15 / 5 | 15 / 3 | 15 / 3 | 16 / 4 | 15 / 3 |
| E2 | 9 / 13 | 8 / 10 | 8 / 11 | 8 / 13 | 7 / 10 |
| E3 | 0 / 3 | 0 / 0 | 1 / 2 | 0 / 2 | 1 / 0 |
| E4 | 0 / 7 | 0 / 3 | 1 / 7 | 0 / 7 | 0 / 0 |
| E5 | 6 / 14 | 4 / 0 | 4 / 6 | 3 / 13 | 1 / 0 |
| E6 | 30 / 44 | 25 / 35 | 27 / 33 | 28 / 40 | 24 / 17 |
| E7 | 3 / 3 | 2 / 2 | 2 / 2 | 3 / 3 | 2 / 2 |
| E8 | 6 / 12 | 1 / 1 | 5 / 17 | 7 / 11 | 1 / 2 |
| E9 | 3 / 6 | 3 / 7 | 4 / 6 | 4 / 4 | 4 / 3 |
| E10 | 4 / 4 | 3 / 0 | 3 / 3 | 4 / 4 | 3 / 0 |
| F | 5 / 6 | 6 / 9 | 7 / 9 | 4 / 5 | 7 / 6 |

**Worse and cleared** (seeds clean in the base and held now; cleared counts):

| against | random worse | random cleared | defaults worse | defaults cleared |
|---|---|---|---|---|
| `o02` | 77 301 556 761 | 20 | 244 246 | 76 |
| t14 `z16` | 77 301 556 761 | 6 | 17 145 244 246 475 681 778 937 961 | 36 |
| t15 `sf22` | 556 788 | 14 | 17 156 164 | 59 |
| t16 `z18` | 77 301 556 629 | 16 | 17 164 244 246 681 | 68 |

**Alone** (`ig12`, one worker): random 301 E3, 556 E6, 629 E5, 761 E9 hold again in their classes, and the budget holds 77 246 431 547 678 707 788 stay budget holds; world defaults 17 145 244 475 681 937 961 E6, 164 246 C, 778 E8 hold again, and of the budget holds 929 runs clean alone, 622 holds in E8, and 156 298 563 788 stay budget holds. So no worse seed is a parallel-run artefact.

### 6. The worse worlds, traced and minimized

Every worse seed (against `o02` or a side branch) was traced alone (section 7) and minimized keeping its first hold's signature (class, row and ends, first or later, flags; `t17-minimize.sh`, logs `ig15-min-*`). Each minimized world was then replayed on `o02`'s code (`ig16`) and, when clean there, on t14 alone (`07d6af87`), t16 alone (`f42948ec`) and t14 with t15's rules (a)+(b) (`6b773d60` on this branch; `ig17`), in a temporary detached worktree, removed after. The minimized worlds hold on the final code at the campaign's tick.

| world | class, group | blocks | on `o02` code | opened by | physics, in short |
|---|---|---|---|---|---|
| random 556 | E6-S, first | 15 | clean | t15 (a)+(b): clean on t14 and on t16 alone, holds on t14+t15 | A 142 kPa, 350 K water generator floods a just re-minted net of junctions between two gas tanks and two voids. In real life the water displaces the gas into the voids and tanks within seconds. The new island's first Newton stalls on a 30 Pa flow between two gas-water junctions (residual 3e-4) |
| random 301 | E3 (B row), first | 11 | clean | t15 (a)+(b) | A 283 kPa nitrogen generator blows through a junction into a void 1 m below (0.1 m bore, at the velocity cap); a gas-filled tank 89 kPa below the junction hangs on the same junction by its bottom port, fed water by a 376 kPa generator. In real life nitrogen bubbles up into the tank until the tank reaches the junction's pressure. The first Newton walks to the void supplying 49.5 kg/s against a 418 kPa drive (a forbidden direction) and leaves the water model's range |
| random 761 | E9, later | 14 | clean | t16 alone (its known worse seed) | Card E9 (c): a steady methane vent through a nitrogen tank whose nitrogen trace sits just above the flush fraction; a step books more than the node holds ("Negative transport reconstruction") |
| defaults 244 | E6-B, first | 16 | clean | t15 (a)+(b) (in t15's own list of rule (b) worlds) | Water drains by gravity from a 1 atm generator 2 m above three voids through a net of water-full junctions. The new island's first Newton keeps a forbidden boundary direction at its failing iterate and stalls on a 0.9 Pa flow between two water junctions |
| defaults 246 | C (thermo domain), +6 ticks | 25 | clean | t15 (a)+(b) (on t14+t15 it holds at the same tick as a budget hold) | A closed tank at the top of a draining network (z = 3) expands to 87 kPa and cools adiabatically to 275.9 K; a junction trial reaches 273.16 K, the water model's floor. In real life the tank's walls would warm it; the model's vessels are adiabatic. A typed wait at the material data's edge, correct as a typed hold |
| random 629 | E5, first | 13 | holds | - | A crude two-phase junction (`J:gl`, fugacity row): the D44 family |
| defaults 17 | E6-S, later | 14 | holds (later) | - | Flow from a two-phase tank's top port into a gas-water junction, columns of opposite sign (`MIXED`) |
| defaults 145 | E6-S, later | 16 | holds (later) | - | A gas-water junction venting to a void through a `MIXED` column, with a forbidden direction open |
| defaults 475 | E6-B, first | 14 | holds (earlier) | - | A water junction draining into a gas tank's bottom; first solve with a forbidden direction |
| defaults 681 | E6-S, later | 11 | holds (singular) | - | A gas-water junction feeding a two-phase tank |
| defaults 937 | E6-B, first | 16 | holds (later) | - | A two-phase tank draining into a water junction; first solve with a forbidden direction |
| defaults 961 | E6-S, later | 13 | holds | - | Flow between two gas-water junctions, 1 m apart |
| defaults 164 | C (thermo domain), later | 17 | holds (earlier, E6) | - | Nitrogen drawn to 94 Pa (below the 100 Pa floor) at a capped connection into a two-phase tank |

Not minimized, being wall-clock or attempt-budget classes: random 77 and 788 (budget alone; their slow islands are crude two-phase junctions), defaults 156 (budget alone), defaults 778 (attempt limit alone).

So the integration opens five worlds. Four are t15's start rules acting on a new island's first solve, where rule (a) carries nothing and the estimate's new column rule (b) gives the Newton its start; this is t15's S3 ("what remains of E6 sits on new islands' first solves") and the route stays card E9 (a), then rule (c). The fifth is t16's known E9 (c) trace world. The eight others hold on `o02`'s code too when minimized: their mechanisms exist there, and the integrated code only reaches them in another way.

### 7. E6 by mechanism on the final code

Every E6 seed of `ig10` and every worse seed was traced alone to its first hold with t13's instrumentation (`src/instr-t13-failing-rows-and-levers.patch`, applied with a three-way merge in the working tree; its three lever hunks conflict with t15's committed start code and were left out; never committed) and classified by t13's group rule (`ig13-trace/`, `ig13-classification.md`). Each traced first hold equals the campaign's, except budget seed random 788 (alone it holds at tick 11147, in the campaign at 3247 on the wall deadline).

| group | mechanism | `q04` (t13) d / r | t14 `z16` d / r | **now `ig10`** d / r | first interval now d / r |
|---|---|---|---|---|---|
| E6-T | a trace gas phase in free water | 17 / 3 | 0 / 0 | **0 / 1** | - / 1 |
| E6-S | the Newton walks from start flows that are not the solution's | 24 / 15 | 26 / 14 | **12 / 12** | 3 / 11 |
| E6-B | a new island's first solve keeps a forbidden boundary direction | 9 / 3 | 9 / 5 | **5 / 5** | 5 / 5 |
| E6-C | a junction between velocity-capped gas connections | 0 / 6 | 0 / 6 | **0 / 6** | - / 6 |

Seeds: S defaults 17 145 175 336 352 397 401 606 681 833 915 961, random 19 65 92 244 556 600 768 824 842 886 915 916; B defaults 228 244 273 475 937, random 118 460 499 853 937; C random 151 173 286 336 529 594; T random 698.

- The one T seed is T by the row rule only. Random 698's worst row is the water-vapour saturation of a three-phase hydrocarbon junction (`J:glw`, 291.6 kPa, 7.0 mol of vapour): its gas is not a trace, so it is not t13's E6-T mechanism (nitrogen in free water, which D41 removed). It is a random crude junction on its first interval with four forbidden directions open at the failing iterate; t14 classified it S on `z16`.
- S halved in world defaults (26 -> 12) and stayed at 12 in random. What remains of S in world defaults is mostly later in an island's life (9 of 12), on junctions between gas and water legs (`MIXED` columns); in random it is on first intervals (11 of 12).
- In random, 23 of the 24 E6 seeds hold on a new island's first interval. With B (5 / 5) and C (0 / 6), the remaining E6 is a first-solve problem: the start a new island's first Newton is given (card E9 (a), the junction's start pressure restated hydrostatically, then t15's rule (c) for B), and the velocity cap as an active-set mode for C.

### 8. Full gates, twice

`gates.sh` on `074632b6` (code of the final branch; later commits are documents and tools), alone on Gradle:

| gate | run 1 `ig08` (GameTest run id `t17-ig08-074632b6-run1`) | run 2 `ig20` (`t17-ig20-074632b6-run2`) |
|---|---|---|
| G1 fluid suites | 529 tests, 1 failure: `SolidChainTransportTest`'s recorded counts (A222) | the same; the 6098 printed lines identical to run 1 |
| 33 junction lines | 68 diff lines against the tracked reference (older than `o12`) | identical to run 1 |
| MIXED_GAS_COST | 264 newton solves (`o12` 309) | 264 |
| G2 exact regression | red on chain-100 only, `o12`'s values: 6.049e-15 relative mass, 1.705e-13 K, 3.3e-16 phase fraction, 1.896e-11 relative flow | the same |
| G3 adjacent | 45 / 45 | 45 / 45 |
| G5 GameTests | 33 / 33; outlets 124258.48752327717 Pa / 175.7622745472754 kg; compressor 2.9999859075564377 / 157976.31408415755 Pa | 33 / 33; the same values (only the pump line's world coordinates differ, from the structure's placement) |

Both runs fail exactly what the re-baseline re-records (section 9) and nothing else.

### 9. The re-baseline, prepared and not applied (D36, D38)

One patch re-records every recorded reference that moves on the final code: `tools/phase-ports-probes/hold-classes/src/rebaseline-074632b6.patch` (applies to `074632b6` and to every later commit of this branch; it touches four files and nothing else). It was verified in the working tree and reverted (`ig19`): with it G1 runs 529 tests with no failure and the 33 junction lines equal the reference (0 diff lines), and G2 replays chain-100 at zero deviation. Nothing re-recorded is committed.

| reference (file) | what moves | old | new |
|---|---|---|---|
| chain-100 (`src/test/resources/fluid/regression/chain-100.json`) | largest deviation from the recorded reference, as G2 reports it | node 27 mass 38.765655832609795; node 22 T 350.01920083234904 K; node 27 vapour volume 0.9568420472107934; pipe 4 average flow -4.217556210219395E-4 kg/s | 38.76565583261003 (6.049e-15 relative); 350.0192008323492 K (1.705e-13 K); 0.9568420472107939 (3.3e-16); -4.2175562101394345E-4 kg/s (1.896e-11 relative). The file is re-captured whole (`-PfluidRegressionCapture=true`) |
| `SolidChainTransportTest` recorded substeps (accepted / rejected) | the clear chain reaches the gauge after two steps (A222) | half interval 3 / 1, full 5 / 1, two-node 4 / 0 | 2 / 1, 2 / 1, 2 / 0 |
| the 33 junction lines (`tools/phase-ports-probes/holds/12-junction-lines.txt` and `-raw.txt`, the reference `gates.sh` diffs G1 against) | every line; the tracked file is older than `o12` (26 lines already differed from `o12`) | `o12` column below | "now" column below (MIXED_GAS_COST 309 -> 264) |
| GameTest printed values (printed only; no tracked file, nothing in the patch) | outlets tankP / tankMass; compressor ratio / discharge | 124258.48752327723 Pa / 175.76227454727493 kg; 2.999985907556435 / 157976.31408415743 Pa (`o12`, t14, t16) | 124258.48752327717 Pa / 175.7622745472754 kg; 2.9999859075564377 / 157976.31408415755 Pa (moved by t15's rule (a) at 1e-16 relative; D46 moves none) |
| printed suite lines (printed only; no tracked file) | G1 stdout against `o12`'s | 6072 lines | 6098 lines, 5688 not in `o12`'s (t14's pump-from-tank lines, t15's step counts and roundoff, t16's rests, D46's 32 junction lines and 12 new `VIRTUAL_HOLDUP` lines; `ig11-vs-*-printed-diff.txt`) |

The 33 junction lines, field by field (o12, t14 `z23`, t15 `sf26`, t16 `z19`, the integration before D46 `ig01`, now `ig08`; a field identical in all six is not shown; `ms`, `bytes` and `allocatedMB` are wall-clock and masked):

| line | field | o12 | t14 | t15 | t16 | pre-D46 | now |
|---|---|---|---|---|---|---|---|
| LIQUID 5 generators:0.05 | junctionShare | - | - | - | - | - | 8.38e-16 |
| LIQUID 5 generators:0.02 | junctionShare | - | - | - | - | - | 2.13e-15 |
| LIQUID 5 tanks:0.05 | mismatch(end) | -0.472 | -0.472 | -0.453 | NaN | NaN | NaN |
| LIQUID 5 tanks:0.05 | perSlice | [-0.356,-0.472,-0.472,-0.472] | [-0.356,-0.472,-0.472,-0.472] | [-0.356,-0.453,-0.453,-0.453] | [-0.356,NaN,NaN,NaN] | [-0.356,NaN,NaN,NaN] | [-0.356,NaN,NaN,NaN] |
| LIQUID 5 tanks:0.05 | newtonSolves | 17 | 17 | 17 | 4 | 4 | 4 |
| LIQUID 5 tanks:0.05 | junctionShare | - | - | - | - | - | 2.10e-16 |
| LIQUID 5 tanks:0.02 | mismatch(end) | -0.0806 | -0.0806 | -0.0725 | NaN | NaN | NaN |
| LIQUID 5 tanks:0.02 | perSlice | [-0.342,-0.0806,-0.0806,-0.0806] | [-0.342,-0.0806,-0.0806,-0.0806] | [-0.342,-0.0725,-0.0725,-0.0725] | [-0.342,NaN,NaN,NaN] | [-0.342,NaN,NaN,NaN] | [-0.342,NaN,NaN,NaN] |
| LIQUID 5 tanks:0.02 | newtonSolves | 17 | 17 | 17 | 4 | 4 | 4 |
| LIQUID 5 tanks:0.02 | junctionShare | - | - | - | - | - | 1.64e-16 |
| LIQUID 0.1 generators:0.05 | junctionShare | - | - | - | - | - | 4.19e-16 |
| LIQUID 0.1 generators:0.02 | junctionShare | - | - | - | - | - | 9.82e-16 |
| LIQUID 0.1 tanks:0.05 | Q(1s) | 2.740e-11 | 2.740e-11 | 3.366e-11 | 0.000 | 0.000 | 0.000 |
| LIQUID 0.1 tanks:0.05 | m_J/Q | 3.567e+11 | 3.567e+11 | 2.904e+11 | Infinity | Infinity | Infinity |
| LIQUID 0.1 tanks:0.05 | tauFit(1.0-1.1s) | Infinity | Infinity | Infinity | NaN | NaN | NaN |
| LIQUID 0.1 tanks:0.05 | mismatch(1s) | -0.427 | -0.427 | -0.461 | NaN | NaN | NaN |
| LIQUID 0.1 tanks:0.05 | mismatch(end) | -0.427 | -0.427 | -0.461 | NaN | NaN | NaN |
| LIQUID 0.1 tanks:0.05 | junctionShare | - | - | - | - | - | 2.10e-16 |
| LIQUID 0.1 tanks:0.02 | Q(1s) | 3.103e-12 | 3.103e-12 | 2.832e-08 | 0.000 | 0.000 | 0.000 |
| LIQUID 0.1 tanks:0.02 | m_J/Q | 5.040e+11 | 5.040e+11 | 5.522e+07 | Infinity | Infinity | Infinity |
| LIQUID 0.1 tanks:0.02 | tauFit(1.0-1.1s) | 1.003e+12 | 1.003e+12 | 5.522e+07 | NaN | NaN | NaN |
| LIQUID 0.1 tanks:0.02 | mismatch(1s) | -0.983 | -0.983 | 0.0167 | NaN | NaN | NaN |
| LIQUID 0.1 tanks:0.02 | mismatch(end) | NaN | NaN | -0.983 | NaN | NaN | NaN |
| LIQUID 0.1 tanks:0.02 | junctionShare | - | - | - | - | - | 4.91e-16 |
| MIXED 0.1 0.05:4:false | worstMoles | 2.0441440529446203E-15 | 2.0441440529446203E-15 | 1.7577746719682418E-15 | 6.132432158833861E-16 | 7.835885536287712E-16 | 7.835885536287712E-16 |
| MIXED 0.1 0.05:4:false | worstEnergy | 1.1113011245881988E-15 | 1.1113011245881988E-15 | 1.930154584811082E-15 | 8.773429930959464E-16 | 6.726296280402256E-16 | 5.556505622940994E-16 |
| MIXED 0.1 0.05:4:false | junctionShare | - | - | - | - | - | 0.6299991470589125 |
| MIXED 0.1 0.05:4:true | worstMoles | 3.1997618432493593E-15 | 3.1997618432493593E-15 | 3.2365614171623155E-15 | 5.45105080785232E-16 | 4.4289787813800106E-16 | 3.6957813530958055E-16 |
| MIXED 0.1 0.05:4:true | worstEnergy | 3.485151865730924E-15 | 3.485151865730924E-15 | 5.080051872082364E-15 | 2.1265333418019197E-15 | 1.8902518593794843E-15 | 1.8311814887738755E-15 |
| MIXED 0.1 0.05:4:true | junctionShare | - | - | - | - | - | 0.06883593087628813 |
| MIXED 0.1 0.05:5:false | worstMoles | 2.826225943164624E-15 | 2.826225943164624E-15 | 3.170887643550554E-15 | 3.4069067549077004E-16 | 3.446617003859298E-16 | 3.7912787042452273E-16 |
| MIXED 0.1 0.05:5:false | worstEnergy | 3.538616738820317E-15 | 3.538616738820317E-15 | 6.170645718108156E-15 | 1.1405458910247302E-15 | 7.311191609132886E-16 | 6.141400951671624E-16 |
| MIXED 0.1 0.05:5:false | junctionShare | - | - | - | - | - | 0.6354347848520617 |
| MIXED 0.1 0.05:5:true | worstMoles | 2.663604250005308E-15 | 2.663604250005308E-15 | 3.3387686198095464E-15 | 7.330009095869405E-16 | 5.350034630505101E-16 | 5.275657351365769E-16 |
| MIXED 0.1 0.05:5:true | worstEnergy | 8.358457440693657E-15 | 8.358457440693657E-15 | 4.78470001905432E-15 | 1.6244351916542444E-15 | 1.6835055622598532E-15 | 1.7130407475626576E-15 |
| MIXED 0.1 0.05:5:true | junctionShare | - | - | - | - | - | 0.033889877026384 |
| MIXED 0.1 0.05:6:false | worstMoles | 1.6353152423556962E-15 | 1.6353152423556962E-15 | 2.5892491337298525E-15 | 7.495194860796941E-16 | 8.616542509648244E-16 | 8.271880809262315E-16 |
| MIXED 0.1 0.05:6:false | worstEnergy | 1.3452592560804511E-15 | 1.3452592560804511E-15 | 1.491483088263109E-15 | 9.65077292405541E-16 | 1.0528115917151356E-15 | 9.065877595324779E-16 |
| MIXED 0.1 0.05:6:false | junctionShare | - | - | - | - | - | 0.6354347848520543 |
| MIXED 0.1 0.05:6:true | worstMoles | 1.260555499315849E-15 | 1.260555499315849E-15 | 9.880029589232332E-16 | 7.236396313504383E-16 | 4.76966945687078E-16 | 4.76966945687078E-16 |
| MIXED 0.1 0.05:6:true | worstEnergy | 1.4176888945346133E-15 | 1.4176888945346133E-15 | 4.164461127695426E-15 | 1.8902518593794843E-15 | 1.8902518593794843E-15 | 1.9788574152878976E-15 |
| MIXED 0.1 0.05:6:true | junctionShare | - | - | - | - | - | 0.023139134896948233 |
| MIXED 0.1 0.02:4:false | worstMoles | 1.0991866764093819E-15 | 1.0991866764093819E-15 | 1.3739833455117274E-15 | 8.930891745826228E-16 | 1.3739833455117274E-15 | 1.3396337618739343E-15 |
| MIXED 0.1 0.02:4:false | worstEnergy | 1.7725148905894276E-15 | 1.7725148905894276E-15 | 1.3657081943885754E-15 | 1.5981691636462053E-15 | 1.2494777097597605E-15 | 1.191362467445353E-15 |
| MIXED 0.1 0.02:4:false | junctionShare | - | - | - | - | - | 0.6295556376945468 |
| MIXED 0.1 0.02:4:true | worstMoles | 8.479009821883923E-16 | 8.479009821883923E-16 | 7.789096439522527E-16 | 8.479009821883923E-16 | 7.789096439522527E-16 | 7.842955848554457E-16 |
| MIXED 0.1 0.02:4:true | worstEnergy | 1.80766575747774E-15 | 1.80766575747774E-15 | 1.2537036705087551E-15 | 1.6910421602211116E-15 | 1.2537036705087551E-15 | 1.224547771194598E-15 |
| MIXED 0.1 0.02:4:true | junctionShare | - | - | - | - | - | 0.25321017501221266 |
| MIXED 0.1 0.02:5:false | worstMoles | 7.237895708104525E-16 | 7.237895708104525E-16 | 7.556908400314501E-16 | 7.237895708104525E-16 | 7.556908400314501E-16 | 7.237895708104525E-16 |
| MIXED 0.1 0.02:5:false | worstEnergy | 1.5981691636462053E-15 | 1.5981691636462053E-15 | 1.94686061753265E-15 | 1.4819386790173903E-15 | 1.8887453752182425E-15 | 1.830630132903835E-15 |
| MIXED 0.1 0.02:5:false | junctionShare | - | - | - | - | - | 0.6354347849265121 |
| MIXED 0.1 0.02:5:true | worstMoles | 1.0991866764093819E-15 | 1.0991866764093819E-15 | 8.982980006396987E-16 | 9.61788341858209E-16 | 8.982980006396987E-16 | 9.012474444676379E-16 |
| MIXED 0.1 0.02:5:true | worstEnergy | 1.2537036705087551E-15 | 1.2537036705087551E-15 | 1.1370800732521267E-15 | 1.2537036705087551E-15 | 1.1370800732521267E-15 | 1.0787682746238124E-15 |
| MIXED 0.1 0.02:5:true | junctionShare | - | - | - | - | - | 0.1457188924254169 |
| MIXED 0.1 0.02:6:false | worstMoles | 1.6888423318910558E-15 | 1.6888423318910558E-15 | 1.930105522161207E-15 | 1.6888423318910558E-15 | 1.8956393521226136E-15 | 1.8956393521226136E-15 |
| MIXED 0.1 0.02:6:false | worstEnergy | 1.1623048462881492E-15 | 1.1623048462881492E-15 | 9.879591193449269E-16 | 1.1623048462881492E-15 | 9.879591193449269E-16 | 9.589014981877232E-16 |
| MIXED 0.1 0.02:6:false | junctionShare | - | - | - | - | - | 0.6354347849263441 |
| MIXED 0.1 0.02:6:true | worstMoles | 7.556908400314501E-16 | 7.556908400314501E-16 | 9.308701194351995E-16 | 7.213412563936569E-16 | 9.308701194351995E-16 | 9.238812199298658E-16 |
| MIXED 0.1 0.02:6:true | worstEnergy | 1.6035744622786402E-15 | 1.6035744622786402E-15 | 1.4286390663936976E-15 | 1.1953918718804408E-15 | 1.4286390663936976E-15 | 1.516106764336169E-15 |
| MIXED 0.1 0.02:6:true | junctionShare | - | - | - | - | - | 0.08978039058201302 |
| MIXED 5 0.05:4:false | worstMoles | 4.4289787813800106E-16 | 4.4289787813800106E-16 | 4.4289787813800106E-16 | 3.7912787042452273E-16 | 3.0662160794169303E-16 | 2.38483472843539E-16 |
| MIXED 5 0.05:4:false | worstEnergy | 3.509371972383785E-16 | 3.509371972383785E-16 | 4.094267301114416E-16 | 2.924476643653155E-16 | 1.7546859861918926E-16 | 5.84895328730631E-17 |
| MIXED 5 0.05:4:false | newtonSolves | 29 | 29 | 29 | 21 | 21 | 21 |
| MIXED 5 0.05:4:false | junctionShare | - | - | - | - | - | 0.5592660373641596 |
| MIXED 5 0.05:4:true | worstMoles | 3.484190817613221E-16 | 3.484190817613221E-16 | 3.2879886847111883E-16 | 3.4069067549077004E-16 | 3.4069067549077004E-16 | 2.38483472843539E-16 |
| MIXED 5 0.05:4:true | worstEnergy | 1.4767592651402221E-16 | 1.4767592651402221E-16 | 8.860555590841333E-17 | 5.611685207532844E-16 | 4.134925942392622E-16 | 4.430277795420666E-16 |
| MIXED 5 0.05:4:true | newtonSolves | 31 | 31 | 31 | 25 | 25 | 25 |
| MIXED 5 0.05:4:true | junctionShare | - | - | - | - | - | 0.06304147441157955 |
| MIXED 5 0.05:5:false | worstMoles | 6.813813509815401E-16 | 6.813813509815401E-16 | 8.176576211778481E-16 | 6.132432158833861E-16 | 6.132432158833861E-16 | 5.791741483343091E-16 |
| MIXED 5 0.05:5:false | worstEnergy | 2.924476643653155E-16 | 2.924476643653155E-16 | 1.4622383218265774E-16 | 1.169790657461262E-16 | 1.7546859861918926E-16 | 1.169790657461262E-16 |
| MIXED 5 0.05:5:false | newtonSolves | 32 | 32 | 32 | 23 | 23 | 23 |
| MIXED 5 0.05:5:false | junctionShare | - | - | - | - | - | 0.3919149271232476 |
| MIXED 5 0.05:5:true | worstMoles | 2.72552540392616E-16 | 2.72552540392616E-16 | 3.4069067549077004E-16 | 4.0882881058892406E-16 | 4.0882881058892406E-16 | 4.0882881058892406E-16 |
| MIXED 5 0.05:5:true | worstEnergy | 4.134925942392622E-16 | 4.134925942392622E-16 | 3.544222236336533E-16 | 2.3628148242243554E-16 | 5.907037060560889E-17 | 5.907037060560889E-17 |
| MIXED 5 0.05:5:true | newtonSolves | 31 | 31 | 31 | 25 | 25 | 25 |
| MIXED 5 0.05:5:true | junctionShare | - | - | - | - | - | 0.03100302106476238 |
| MIXED 5 0.05:6:false | worstMoles | 3.4069067549077004E-16 | 3.4069067549077004E-16 | 3.446617003859298E-16 | 2.38483472843539E-16 | 4.1359404046311573E-16 | 4.1359404046311573E-16 |
| MIXED 5 0.05:6:false | worstEnergy | 3.21692430801847E-16 | 3.21692430801847E-16 | 4.386714965479732E-16 | 2.339581314922524E-16 | 1.169790657461262E-16 | 5.84895328730631E-17 |
| MIXED 5 0.05:6:false | newtonSolves | 35 | 35 | 33 | 25 | 25 | 25 |
| MIXED 5 0.05:6:false | junctionShare | - | - | - | - | - | 0.3760037044258622 |
| MIXED 5 0.05:6:true | worstMoles | 3.0662160794169303E-16 | 3.0662160794169303E-16 | 2.1697647638577733E-16 | 2.672452581489125E-16 | 2.38483472843539E-16 | 2.6224402731023326E-16 |
| MIXED 5 0.05:6:true | worstEnergy | 4.725629648448711E-16 | 4.725629648448711E-16 | 3.544222236336533E-16 | 4.134925942392622E-16 | 3.8395740893645773E-16 | 4.430277795420666E-16 |
| MIXED 5 0.05:6:true | newtonSolves | 32 | 32 | 34 | 26 | 26 | 26 |
| MIXED 5 0.05:6:true | junctionShare | - | - | - | - | - | 0.021307977231995873 |
| MIXED 5 0.02:4:false | worstMoles | 3.4349583637793186E-16 | 3.4349583637793186E-16 | 5.495933382046909E-16 | 3.4349583637793186E-16 | 5.495933382046909E-16 | 4.808941709291045E-16 |
| MIXED 5 0.02:4:false | worstEnergy | 1.4528810578601865E-16 | 1.4528810578601865E-16 | 1.1623048462881494E-16 | 1.4528810578601865E-16 | 1.1623048462881494E-16 | 8.717286347161119E-17 |
| MIXED 5 0.02:4:false | junctionShare | - | - | - | - | - | 0.6223279063224404 |
| MIXED 5 0.02:4:true | worstMoles | 1.3739833455117273E-16 | 1.3739833455117273E-16 | 3.0914625274013867E-16 | 1.3739833455117273E-16 | 3.0914625274013867E-16 | 3.7784542001572506E-16 |
| MIXED 5 0.02:4:true | worstEnergy | 1.7493539588494257E-16 | 1.7493539588494257E-16 | 2.0409129519909966E-16 | 1.7493539588494257E-16 | 2.0409129519909966E-16 | 1.7493539588494257E-16 |
| MIXED 5 0.02:4:true | junctionShare | - | - | - | - | - | 0.1691874150429126 |
| MIXED 5 0.02:5:false | worstMoles | 3.0914625274013867E-16 | 3.0914625274013867E-16 | 2.0679702023155786E-16 | 3.0914625274013867E-16 | 2.0679702023155786E-16 | 1.8956393521226137E-16 |
| MIXED 5 0.02:5:false | worstEnergy | 2.0340334810042612E-16 | 2.0340334810042612E-16 | 1.7434572694322239E-16 | 2.0340334810042612E-16 | 1.7434572694322239E-16 | 2.324609692576299E-16 |
| MIXED 5 0.02:5:false | junctionShare | - | - | - | - | - | 0.6354347846258318 |
| MIXED 5 0.02:5:true | worstMoles | 2.4903564842859316E-16 | 2.4903564842859316E-16 | 4.465445872913114E-16 | 2.4903564842859316E-16 | 4.465445872913114E-16 | 4.121950036535182E-16 |
| MIXED 5 0.02:5:true | junctionShare | - | - | - | - | - | 0.09553974715134206 |
| MIXED 5 0.02:6:false | worstMoles | 3.7784542001572506E-16 | 3.7784542001572506E-16 | 3.0914625274013867E-16 | 3.7784542001572506E-16 | 3.0914625274013867E-16 | 2.4044708546455227E-16 |
| MIXED 5 0.02:6:false | worstEnergy | 3.4869145388644477E-16 | 3.4869145388644477E-16 | 2.0340334810042612E-16 | 3.4869145388644477E-16 | 2.0340334810042612E-16 | 1.1623048462881494E-16 |
| MIXED 5 0.02:6:false | junctionShare | - | - | - | - | - | 0.6354347846258025 |
| MIXED 5 0.02:6:true | worstMoles | 2.7479666910234547E-16 | 2.7479666910234547E-16 | 2.4044708546455227E-16 | 2.7479666910234547E-16 | 2.4044708546455227E-16 | 2.7479666910234547E-16 |
| MIXED 5 0.02:6:true | worstEnergy | 1.4577949657078547E-16 | 1.4577949657078547E-16 | 2.9155899314157094E-16 | 1.4577949657078547E-16 | 2.9155899314157094E-16 | 2.9155899314157094E-16 |
| MIXED 5 0.02:6:true | junctionShare | - | - | - | - | - | 0.07802050696038604 |
| COST | newtonSolves | 309 | 309 | 309 | 264 | 264 | 264 |

**Commands.** To apply and verify (from the worktree root, alone on Gradle):

```
git apply tools/phase-ports-probes/hold-classes/src/rebaseline-074632b6.patch
bash tools/phase-ports-probes/hold-classes/gates.sh logs/NN-rebaseline <fresh-gametest-run-id>
#   expect: G1 529 tests, 0 failures, junction diff lines 0; G2 chain-100 max deviation 0.000e+00; G3 45/45; G5 33/33
git add src/test/resources/fluid/regression/chain-100.json src/test/java/com/wormzjl/createcheme/science/fluid/network/SolidChainTransportTest.java tools/phase-ports-probes/holds/12-junction-lines.txt tools/phase-ports-probes/holds/12-junction-lines-raw.txt
```

To regenerate it on a later code state: `./gradlew --no-configuration-cache fluidSolverRegression -PfluidRegressionCapture=true` (re-captures `chain-100.json`); run G1 (`gates.sh`) and copy its `<prefix>-G1-junction-lines.txt` and `-raw.txt` over `tools/phase-ports-probes/holds/12-junction-lines.txt` and `-raw.txt`; set `SolidChainTransportTest`'s six counts to what the test then reports; `git diff -- <the four files> > tools/phase-ports-probes/hold-classes/src/rebaseline-<sha>.patch`, verify as above, and restore the four files.

### 10. Stop points and recommendations

| # | item | recommendation |
|---|---|---|
| S1 | A232: the booked flows that keep `m_J` stay (the owner's list names them; the bitwise requirement keeps them) | keep, restated as the fitting's mass continuity |
| S2 | A234: the D16 trace flush at a junction stays booked (D46's "D16's grams" read as D26's lost-run removal) | keep |
| S3 | A235: `PhysicalRegistryTest`'s booking test restated; the transient tests' ledgers restated | confirm |
| S4 | A221 (t16's seven restated assertions) and A231 (one of t15's five passes only through A221) | confirm |
| S5 | The re-baseline (section 9): one patch, verified | apply once, after reading the table |
| S6 | Remaining E6 is first solves of new islands (random 23 of 24) plus later S in world defaults; the integration's four new worse worlds are t15's rule (b) on first solves | card E9 (a) next (a junction's start pressure restated hydrostatically), then rule (c) |
| S7 | A174 (t14's bubble at rest) is moot under D46 | close it |

Branch: **recommended to become `claude/phase-ports-compressor`** after S1-S5. It holds the three side branches' rules jointly measured (935 / 957 against `o02` 919 / 883, Newton iterations -38 % in world defaults, every worse seed holding again alone and traced), D46 with every solver number unchanged, and a prepared re-baseline.

### 11. Against the brief

- The flow-order re-booking is kept (A232): removing it moves committed numbers (measured) and contradicts "solver numerics stay bitwise". The owner decides.
- D46's "D16's grams" is read as D26's lost-run removal; the D16 trace flush at a junction stays booked (A234).
- Logs use `ig01` onward, not `i01` (t5's logs; A230). t14 and t16 both used `z01`-`z23`; not renamed.
- The worse worlds were contrasted on `o02`'s code and on intermediate commits in a temporary detached worktree under the job directory (created and removed by this thread), not only on this worktree.
- Budget and attempt-limit worse seeds (random 77 and 788, defaults 156 and 778) were replayed alone and traced, not minimized: their class is wall-clock.
- t15's rule (c) is "not taken" in CHANGELOG and here; its commits stay on `claude/start-flows`.
- Commit trailers name the model that wrote them (Claude Opus 5.5), not Claude Fable 5.1.
- Documents are not copied to the main checkout (own worktree only).

## The junction start in hydrostatic balance; rule (c) re-taken (2026-10-13)

Thread t18, branch `claude/e9-start-column` from `0d841a5f` (the integration of D47, re-baselined once). Card E9 (a), then t15's rule (c) (`fde8aa74`, `15afa7ed`). Decision rows A250 onward. Logs `ec01` onward in `tools/phase-ports-probes/hold-classes/logs/` (A250).

### 0. Step 0: the re-baselined base

Full gates on `0d841a5f`, alone on Gradle (alone lock), GameTest run id `t18-ec01-0d841a5f-run1` (`ec01`).

| Gate | Expected | Measured |
|---|---|---|
| G1 fluid suites | 529 / 0 | 529 / 0 |
| 33 junction lines | identical to the re-recorded file | 0 diff lines |
| MIXED_GAS_COST | 264 | newtonSolves=264 |
| G2 chain-100 | exact | 0.000e+00 in every field |
| G3 adjacent | 45 / 45 | 45 / 0 failures |
| G5 GameTests | 33 / 33; outlets 124258.48752327717 Pa / 175.7622745472754 kg; compressor 2.9999859075564377 / 157976.31408415755 Pa | 33 / 33, the same four numbers to the last digit |

The base is green; the task started.

### 1. Where a junction start stands today (measured before the rule)

Instrumentation `src/instr-t18-start-print.patch` (`-Dt18print=true`; never committed) prints, at every mint, each junction's start, its arrivals (end pressure, column density, standing pressure at the junction) and each round of the balance (`ec02`).

**The supply side of the balance already counts the column.** Since A126 every junction that drains somewhere starts at its balance pressure `P`, and each supplier's flow there is `Q(S - P)` with `S` its standing pressure at the junction: its end pressure (a tank's liquid port with its level head) less its own column, read link by link and, past a pump or compressor, raised by the device's rise limit (`Supply.of`). A supply pipe that runs down fills by gravity and counts its column; a supply pipe that runs up reaches the junction only while `S` stands strictly above the drain's bound, so a level source does not lift its liquid over its own level, and a pump does (it stands above every bound). What is not restated are the starts that do not come from the balance: they take the stream at its source's own pressure.

| Start | When | Pressure today |
|---|---|---|
| Balance (m') | the junction drains somewhere and a supplier is open at `P` | `P`, supply columns counted |
| Pocket (D12) | the junction drains nowhere | the strongest stream at its source's end pressure, **no column** |
| Balance fallback | no supplier open at `P`, a mixture whose temperature cannot be bracketed, a property outside the data | the strongest stream at its source's end pressure, **no column** |
| Nothing reaches | no supplier can drive into the junction | the contact boundary's fluid at that boundary's pressure, **no column** (its column is read only to choose it) |

**t15's two (c) failures.** The pump-suction junction of `ElevatedBlockLineIslandTest` is the pump's own block 2 m above a 400 kPa water generator; the pump draws it (a drain bound of minus infinity), so it reaches the balance. Its bracket has one finite end (the generator's standing pressure, 380.4 kPa), the balance returns that end, no supplier is open there, and the fallback starts the junction at 400 000 Pa: the "minted pressure with no column" of A192 is the fallback's. But the test builds its island directly (`PhysicalFluidTopology.compile`, then the interval solver), and a directly built graph is not minted (A106): its junction keeps the compiler's placeholder, the island's first boundary's state, which is the same 400 000 Pa. The rule below restates the event path; the fixture is a separate item (A256). The junction of `JunctionStartStateTest` (world-default seed 438 minimized) is a balance start at 101 315.6 Pa (A192), not a single-stream start.

**The four worse worlds of t17** (r556, r301, d244, d246; minimized `ig15`, replayed `ec02`). The held island's mint, junction by junction:

| World | Junctions in the held island | Starts | What is far from the step's root |
|---|---|---|---|
| r556 | 6 | 6 balance | junction 10 (water, z 0): 108 301 Pa against the step's 93 700-101 700 Pa; its balance cycles with period 2 (101 595.9 / 108 301.4 Pa) and stops on round 8's water-only half |
| r301 | 1 | 1 fallback (mixture flash outside the water-vapour range), level with its source | none from a column |
| d244 | 10 | 10 balance | the junctions at z 2 start at 101 312-101 315 Pa; the step puts them at 89.3-93.5 kPa: water falling 2 m into 1 atm voids stands below 1 atm at the top of a liquid-full drain |
| d246 | 11 | 11 balance | water junctions at 105.9-112.0 kPa (C, a later hold) |

So the single-stream starts the brief names are not where these four worlds hold. Three of the four start from the balance, and what separates their start from the step's root is the drain side: the balance reads the drain as full of the receiver's gas (D40, A133), while the step's row reads the junction's own liquid in a falling drain (rule (b), `columnFromFirst`). In d244 a water junction 2 m above a void stands about 10 kPa below 1 atm in the step and about 1 atm in the start. Section 5 measures this; the drain side is the owner's (D40, A140), so it is a stop point, not a change (A255).

### 2. The rule (written before the code)

**Real life, at the first instant after a junction forms.** A supply pipe that runs down from a source fills by gravity at once: the junction at its foot stands at the source's pressure plus the column of what the pipe holds. A supply pipe that runs up stands at the source's pressure less the column, if the source can lift the liquid there: a level source cannot lift it over its own level into a line that drains at its level; a pump can. A dead end (a pocket) fills to the source standing highest there and comes to rest at that source's pressure less the column: no flow, so no friction. A junction nothing drives into stands in the fluid of the boundary in contact with it, at that boundary's pressure less that fluid's column. The drain side is unchanged: unprimed for liquid (D40), its gas column counted (A133).

**The rule.** Every junction start is a stream at the pressure where it stands:

- The balance start (m'): unchanged. `P` already reads each supplier's standing pressure.
- Every single-stream start (a pocket's strongest stream, each fallback of the balance) stands at its stream's standing pressure at the junction: the stream's end pressure (a liquid port with its level head, D9), less the stream's own column to the junction's height, link by link; past a pump or compressor, the device's ceiling there (the suction's static pressure plus its rise limit, less the column on to the junction), as `Supply.of` reads it for the balance. One function gives both.
- A junction nothing reaches stands at its contact boundary's pressure less that boundary's bulk column, the value its choice already reads.
- The stream is restated at that pressure as the balance restates a lone open stream (A126): throttled, its enthalpy kept, flashed at the pressure (`mixture` of one). Level with its source it is the same stream to the bit (no flash).
- Which column (A251): the stream's own density as the solver reads its end (`endDensity`, the phase its port draws first, D11; the bulk at a BULK end), from the source's end pressure. A two-phase BULK stream counts its bulk density, as the solver's row does at that end.
- Can it stand there (A253): only where the restated stream exists, a positive pressure and a flash inside the property data. Otherwise the stream at its source's state, as today (the guard of A123).

**What it absorbs.** The single-stream shortcut of the fallbacks (`strongest.stream()` at its own state) and the "nothing reaches" start at the boundary's own pressure become one form: a start is a stream at its standing pressure. The balance's lone-stream throttle (A126) is the same function.

**What it does not change.** The drain side (D40, A133); which suppliers reach a junction and which are open (D37, D39, A107); the balance and its rounds (A118); a directly built graph's junction states (A106).

Section 5 found the drain side at fault in rule (c)'s second failure and in two of t17's worlds; it is changed there by a separate commit for the owner to confirm (A255). The paragraph above is the rule as written before the code.

### 3. What is on the branch

| Commit | What |
|---|---|
| `47618332` | Code: every junction start stands where its stream stands (the column, A252, A253) |
| `af735b6b`, `e810b6e3` | t15's rule (c) and its gate test `FirstSolveClosureTest`, cherry-picked from `fde8aa74`, `15afa7ed` without conflict (A257) |
| `1be85dac` | Code: a junction's falling drain stands full of its liquid at the start (A255; amends D40 for liquid, owner to confirm; separate so it can be dropped) |
| `f1b4b682` | Test: `ElevatedBlockLineIslandTest` mints its islands as the world does (A256; owner to confirm) |
| docs commit | this section, A250-A261, the cards, CHANGELOG, tools README and logs `ec01`-`ec28` |

Scratch (never committed, `tools/phase-ports-probes/hold-classes/src/`): `scratch-t18-switch.patch` (on `f1b4b682`: `NetworkReduction.SCRATCH_T18`, a thread-local list of flags from `-Dt18mode`: `nocolumn` restores the single-stream starts at their source's pressure, `noprimed` every drain bound to the receiver's gas column; both together are the code before this branch, rule (c) aside), `instr-t18-start-print.patch` (on `47618332`, `-Dt18print=true`: every mint's starts, arrivals, balance rounds and fallbacks), `instr-t18-t13-trace-on-47618332.patch` (t13's failing-row trace re-anchored: its report hunk moved by hand, the lever hunks left out; apply with `git apply --reject --ignore-whitespace`, the L1 hunk is rejected on rule (c)'s code), `scratch-t18-tie-closure.patch` (A260).

### 4. The column, measured

Suites with the column alone (`ec03`): 529 / 0, every one of the 6085 printed lines identical, the 33 junction lines identical, chain-100 exact. No suite world has a single-stream start away from its source's level.

Campaigns, 8 workers, 1000 seeds per distribution (lane 1; lane 2 ran beside some of them, so budget holds are replayed alone, section 8):

| Code | random | world defaults | Newton it. r / d | Newton solves r / d | gate refusals r / d | first-interval family r / d | E6 r / d |
|---|---|---|---|---|---|---|---|
| `ig10` (base) | 935 | 957 | 1.78 M / 12.34 M | 0.305 M / 1.247 M | 22 847 / 20 185 | 44 / 23 | 24 / 17 |
| `ec04` column | 935 | 955 | 1.71 M / 20.13 M | 0.302 M / 1.796 M | 23 241 / 20 142 | 44 / 26 | 24 / 18 |
| `ec10` column + (c) | 935 | 956 | 1.87 M / 15.13 M | 0.347 M / 1.373 M | 26 914 / 18 373 | 44 / 27 | 24 / 18 |
| `ec12` column + (c) + primed (switch) | 937 | 958 | 1.73 M / 12.46 M | 0.338 M / 1.118 M | 24 528 / 11 641 | 43 / 23 | 22 / 15 |
| `ec13` (c) + primed, no column (switch) | 936 | 961 | 1.84 M / 14.96 M | 0.361 M / 1.373 M | 29 068 / 16 911 | 42 / 22 | 21 / 17 |
| **`ec19` final (`f1b4b682`)** | **937** | **959** | 1.80 M / 16.85 M | 0.358 M / 1.509 M | 29 409 / 15 879 | 43 / 23 | 22 / 15 |

`ec12` and `ec19` are the same rules (the switch at its default is the committed code) and differ only in budget seed 431 (F in `ec12`, clean in `ec19`); their world-default Newton totals differ by 4.4 M. The totals are dominated by budget holds, which work until a wall deadline: a Newton count difference of a few million in world defaults is wall-clock noise here, not a cost of a rule. The column alone moved defaults 29 from a 1.6 s E2 hold to a 237 s budget hold (`ec04`), which is most of its 20.1 M.

Column alone against `ig10` (`ec04`): random unchanged (the same clean set); world defaults cleared 145 228 961, worse 19 573 600 842 957. The worse five hold alone (`ec05`), were contrast-minimized (column on holds, column off clean; `ec06`, 10-17 blocks) and traced (`ec07`, `ec08`): four are card E6-B's mechanism (pass 0 of a first solve converging towards a forbidden network: in 842, minimized to a 1 atm generator 1 m below another, a void and a filter, the lower generator receiving 9.9 kg/s and the void supplying nitrogen at its cap), 573 is A254's cavitating pump.

### 5. Rule (c) re-taken: its two failures traced

With the column and (c) the suites fail exactly t15's two tests and no other printed line moves (`ec09`).

**The pumped rising line** (`ElevatedBlockLineIslandTest`). Real life: the pump's suction block stands 2 m above a 400 kPa water generator, full of water, at 400 000 - 2 x 9.78 kPa = 380.4 kPa the instant before the pump draws, and lower once it draws. The fixture never mints (section 1), so its junction kept the placeholder 400 kPa and (c) read a 19.6 kPa drive into the generator and closed it for good. On the event path the same junction is the balance's fallback, which the column now starts at 380.4 kPa. With the fixture minted (A256) the test passes under (c) with the column and fails with the column dropped in the working tree (`ec17`, `ec26`):

| `ElevatedBlockLineIslandTest` under (c) | column on | column dropped (`-Dt18mode=nocolumn`) |
|---|---|---|
| pumped line's tank | passes (bottom 861 058.27 Pa) | fails: "A pumped tank must settle with its bottom one water column below its own shutoff pressure: expected 861057.893394904 but was 101324.99999999997" |

**The junction-start world** (`JunctionStartStateTest`, world-default seed 438 minimized). Real life: a 1 atm water generator, two blocks level to a junction, the junction 1 m above a 1 atm void and level with a second void. Water runs level to the junction and falls into the lower void at about 6.3 kg/s; the junction stands below 1 atm by the supply run's friction; the side void would supply nitrogen into it, which a void never does, so its closure is right. The balance started the junction at 101 315.6 Pa with nothing flowing: it read the 1 m drain as full of the void's nitrogen (11.2 Pa of column), while the step's row reads water falling 9.78 kPa. Pass 0, now on the physical network, walked from no flow towards the spill and stopped at 3.1e-4 at every step size. The start is at fault, on the drain side. In real life the water reaching that junction falls into the drain at once: the drain never rises above the junction, so nothing holds it back (D40's reasons, a siphon and a spill over a rise, do not apply). A255 states that in the balance; the test then passes under (c) (`ec11`) and fails with it dropped (`ec26`, `-Dt18mode=noprimed`: the same hold as t15's, 3.1304859913347386E-4).

### 6. The primed falling drain (A255)

The rule: a drain path whose nodes nowhere stand above the junction is, for a junction holding liquid, full of that liquid at the first instant; its bound is the pressure reference (the receiver's end, or a deciding valve's set point at its upstream node) less that fluid's column. A gas junction keeps A133's receiver-gas column; a path that rises above the junction stays unprimed (D40); who reaches a junction is decided by the walk as before. A two-phase junction counts its mixture's density, as the step's row reads that end. Suites (`ec18`): 530 / 0; besides the two elevated tank lines of A256, one printed line moves, `CARRIED_GAS_CARD tankP` 103766.84824228346 -> 103766.8482422798 (3.5e-15 relative). In the campaign, against column + (c) (`ec12` against `ec10`), it clears random 92 446 556 and world defaults 17 78 244 271 438 475 792 833, and loses random 829 and world defaults 145 529 717 951 (and budget 431 547; section 7).

### 7. The worse worlds of the final code, traced

Against `ig10` (`ec19`): random worse 475 (E6) and 829 (E8); world defaults worse 19 529 717 951 (E6), 573 (E7), budget 72 547 (E8 alone). All hold again alone (`ec20`). Contrast-minimized against the code with both start rules switched off and rule (c) on (`ec21`), then replayed with each rule off (`ec22`), printed (`ec24`) and traced (`ec23`, `ec25`):

| World | Blocks | Rule it holds on | What happens, physics first |
|---|---|---|---|
| d19 | 11 | column | A U-bend under a 1 atm generator and a void at the generator's level: the start is the manometer at rest exactly (111.1 kPa 1 m down); pass 0 of the first solve has no sign to read at the tie (the A259 family) |
| d573 | 12 | column | A pump drawing 0.01 m3/s from a water column 4-5 m above its 1 atm generator: its suction falls below 100 Pa (cavitation; A254) |
| d529 | 8 | primed | A nitrogen tank holding 15.6 ml of water: its bottom port is read as a water supplier standing 1 m of water above its gas (107.3 kPa), a column 15.6 ml cannot fill; the primed drain only changes how far that supply reaches. The start's stream ignores how much of its port's first phase a vessel holds (a new finding, not this rule's) |
| d717 | 14 | primed | A level 1 atm generator spilling into falling drains: its junctions now start 89.6-99.7 kPa, below 1 atm, as water-full falls pull them; the first solve ends E6 at a nitrogen junction above them with a forbidden flow (E6-B family) |
| d951 | 10 | primed | A water generator and a tank's gas meeting at junctions that start as water-nitrogen mixtures; the primed column is read in the mixture's density; E6 later in the island's life |
| r829 | 11 | primed | Hot water (336 K, 136 kPa) and a two-phase stream: junctions start as two-phase mixtures whose primed column is a mixture density; 517 gate refusals in one interval (E8) |
| r475 | - | rule (c) itself | Holds with both start rules off: a 4.2 bar methane and a 3 bar nitrogen generator; pass 0 of the first interval ends with two connections on their velocity cap (-291 kPa of drive) beside rows near zero flow: card E6-C's family (velocity cap, thread t19) |

So the column's two losses are a tie at a one-way boundary (d19, and 94 823 978 against (c) + primed without it, A259) and a cavitating pump (A254). The primed drain's four losses are mixtures and traces read as columns (529, 951, 829) and an E6-B first solve (717). None is a column or a priming the physics would refuse; each is a stage downstream (a tie with no sign, a phase amount, a mixture column, the pump at its vapour pressure).

### 8. The E6 S and B seeds of `ig13` and t17's four worlds, alone

Before: `ig13` (each traced alone on `ig10`'s code, every one holding) and `ig12`. After: `ec20` (alone lock, one worker, `f1b4b682`).

| Seeds | Before (`ig13`) | After (`ec20`) |
|---|---|---|
| world defaults S 12: 17 145 175 336 352 397 401 606 681 833 915 961 | all E6 | clean 17 833; E6 145 175 336 352 397 401 681 915 961; E2 606 |
| world defaults B 5: 228 244 273 475 937 | all E6 | clean 228 244 475; E6 273 937 |
| random S 12: 19 65 92 244 556 600 768 824 842 886 915 916 | all E6 | clean 92 556; E6 the other ten |
| random B 5: 118 460 499 853 937 | all E6 | clean 118; E6 460 499 853 937 |
| t17's four: r556, r301, d244, d246 | E6, E3, E6, C | **clean**, E3 (unchanged: a level fallback, its flash outside the water-vapour range), **clean**, C (unchanged: an adiabatic tank cooling to 273.16 K) |
| budget holds of `ec19` | | random 77 431 547 678 788 F alone; world defaults 156 563 622 788 F, 29 39 72 547 778 E8 alone |

### 9. E6 by mechanism on the final code

Every E6 seed of `ec19` traced alone to its first hold with t13's trace and group rule (`ec28`, `ec28-classification.md`); every hold reproduces the campaign's tick and committed tick.

| Group | world defaults `ig10` -> `ec19` | random `ig10` -> `ec19` |
|---|---|---|
| S (start flows) | 12 -> 12 (145 175 273 336 397 401 529 681 915 937 951 961) | 12 -> 13 (19 65 206 244 475 600 768 824 842 853 886 916 937) |
| B (forbidden direction on a first solve) | 5 -> 2 (19 717) | 5 -> 3 (460 499 651) |
| C (velocity cap) | 0 -> 0 | 6 -> 6 (151 286 336 529 594 915) |
| T (by row) | 0 -> 1 (352: a water junction holding 1.17 m3 of nitrogen, a real two-phase junction, not a trace) | 1 -> 0 |

Class counts `ig10` -> `ec19` (random / world defaults): C 15 -> 15 / 3 -> 2; E2 7 -> 8 / 10 -> 10; E3 1 -> 1 / 0; E5 1 -> 2 / 0; E6 24 -> 22 / 17 -> 15; E7 2 -> 2 / 2 -> 3; E8 1 -> 2 / 2 -> 0; E9 4 -> 3 / 3 -> 2; E10 3 -> 3 / 0; F 7 -> 5 / 6 -> 9.

### 10. References, old and new (nothing re-recorded)

| Reference | Old (`ec01`, `0d841a5f`) | New (`ec27`, `f1b4b682`) |
|---|---|---|
| G1 fluid suites | 529 / 0 | 530 / 0 (`FirstSolveClosureTest` added) |
| 33 junction lines | re-recorded file | identical (0 diff lines) |
| MIXED_GAS_COST newtonSolves | 264 | 264 |
| chain-100 (G2 exact) | 0 | 0 |
| G3 adjacent | 45 / 45 | 45 / 45 |
| GameTest outlets | 124258.48752327717 Pa / 175.7622745472754 kg | identical |
| GameTest compressor | 2.9999859075564377 / 157976.31408415755 Pa | identical |
| `ElevatedBlockLineIslandTest` filter line tank (printed) | P=353969.4180051382 mass=709.7427000105237 bottom=360918.3186531878 | P=353969.4180048305 mass=709.7427000388759 bottom=360918.31865315815 |
| `ElevatedBlockLineIslandTest` pump line tank (printed) | P=852448.0673145067 mass=879.1444865701924 bottom=861058.2704493345 | P=852448.067314322 mass=879.144486570882 bottom=861058.2704491565 |
| `CARRIED_GAS_CARD tankP` (printed) | 103766.84824228346 | 103766.8482422798 |

Every other one of the 6085 printed suite lines is identical (`ec27-vs-ec01-printed-diff.txt`). The GameTest values did not move, so the full gates ran once (`ec27`, GameTest run id `t18-ec27-f1b4b682-run1`).

### 11. Stop points and recommendations

| # | Item | Recommendation |
|---|---|---|
| S1 | A255: the primed falling drain amends D40 for liquid (a drain that nowhere rises above the junction stands full of its liquid at the start) | confirm; it is what makes rule (c) pass its suites and clears two of t17's four worlds |
| S2 | A256: `ElevatedBlockLineIslandTest` mints its islands (assertions unchanged) | confirm |
| S3 | A254: a dead end above a level source starts as a barometric column | keep (a), the solver's rest root; the cavitating pump it exposes belongs to the pump model |
| S4 | A259: the column's losses are starts at an exact tie with a one-way boundary; a tie closure was measured and rejected (A260) | keep the column (it is the physical start and the gate world needs it); the tie itself is pass 0's problem: card E6-B with a sign at a tie, a next item |
| S5 | New findings for later cards: a port's trace phase read as a full stream (d529); mixture densities in a primed column (d951, r829); the balance's period-2 cycle (A261) | record; not changed here |
| S6 | Next: card E6-C's velocity cap as an active-set mode (thread t19), then E6-S's later-life holds | |

**Branch:** recommended to be taken after S1 and S2: 937 / 959 against `ig10` 935 / 957, every gate green with GameTests and the 33 junction lines unchanged, two of t17's four worlds cleared, E6-B down from 5 / 5 to 2 / 3, rule (c) on with its gate test. Without S1, rule (c) must stay out (its suite failure returns) and the column alone is net -2 in world defaults (`ec04`).

### 12. Against the brief

- The column the brief names is not where t17's four worlds hold: their junctions start from the balance, which already counts the supply column (A251). What separated three of them from the step's root is the drain side, which the brief says to leave unchanged (D40, A133). It is changed by a separate commit for the owner (A255), as A133 was.
- t15's elevated-line failure is mostly a fixture that never mints (A106): the column alone cannot reach it; the fixture is rebuilt (A256).
- Two extra campaigns (`ec12`, `ec13`) measure the primed drain and the column's share; a scratch tie closure was measured and rejected (A260).
- Logs use `ec01` onward, not `e01` (class E3's, A250).
- A stale empty `index.lock` of this worktree (left at 15:20, no git process running) was removed once.

## The velocity cap as an active-set mode (2026-10-13)

Branch `claude/cap-active-set` from `claude/phase-ports-compressor` `0d841a5f` (D47), thread t19. Code `fefaeaf9`, gate test `22edb2a9`. Decisions A270-A279. Logs `ca01`-`ca53` in `tools/phase-ports-probes/hold-classes/logs/` (`c01`-`c24` are group A's; A279).

**Result.** The cap is now a state of the connection, beside its device mode: a line at its cap states the cap flow of the end it leaves, as a pump at its target states its flow, and the pass loop moves it in and out of the mode on evidence. Campaign `ca44`: **951 random, 962 world defaults** (`ig10` 935 / 957; 20 / 9 cleared, 4 / 4 worse, every worse world holding again alone). Random E6 falls from 24 to 6. Five of the six E6-C worlds run clean alone; random 594 still holds, on another mechanism (S). Every gate is green: G1 530 / 0, chain-100 exact, G3 45 / 45, G5 33 / 33 with every GameTest value unchanged. The 24 mixed-gas junction lines move (roundoff, and more Newton solves at 5 s: MIXED_GAS_COST 264 to 335).

### 1. The physics

A gas line at its velocity limit is choked: it passes the flow its upstream state lets through, `rho_up A v_lim`, whatever stands downstream. Card E6-C's world (random 529, 7 blocks): a 496 kPa methane generator fills tank B (441 kPa, nitrogen with methane) through junction J1 and two pipes, and a 2 cm pipe joins tank B to tank A (1 atm). The generator's line is choked: 55 kPa of drive against a cap drop of about 3 kPa, about 0.6 kg/s. J1 then sits just above tank B (its two outlets carry the 0.6 kg/s with about 1 kPa of friction, below their caps), and the 2 cm pipe is choked at about 0.09 kg/s. Nothing in this is singular: J1's pressure is set by its outlets' law.

What the solver did: the cap was a branch of the flow row, chosen at every Newton iterate by `|driving| > limitDrop`. With J1's inflow on the branch, J1's pressure entered only its outlets' rows; each time an iterate put them on the branch too, that pressure lost its row (a capped row reads the upstream state only), the Newton step in it jumped about 2e5 Pa, and the next iterate put the outlets back on their law. 20 iterations at every step size, residual 0.65 (`y20`, `ig13`).

### 2. The rule

In the order the pass loop applies it (A270-A275):

1. **What a capped line states (A270).** `loss(q) = loss(q_cap)`, `q_cap` the cap flow of the end the line leaves: the old capped branch, in the hydraulic row's pressure unit. Its derivative has the upstream state's columns only. Every throttling mode has the state: a passive line, an inline filter (with its cake-room limit), a pump on its head limit, an open valve. The clamp branch of the row is gone.
2. **Where a line starts (A271).** On the pass's start point: capped when its drive there exceeds its cap drop (the branch the old clamp read at the first iterate, so a Newton that never crossed the cap is solved as before); a line the previous point of the same structure left capped starts capped (the committed endpoint at a job start).
3. **The trial projection (A275).** A Newton trial never puts a line on its law past its cap flow, as an amount is never negative (`Equations.project`).
4. **At a converged point (A272).** A line over its cap by more than 2e-8 relative enters; a capped line whose drive falls short of its cap drop by more than its row resolves (`tolerance x S`) leaves; between the two a line keeps its mode. One change per pass: a release, then an entry, then a device mode, then a boundary closure. A cap change re-solves the same pass from its own start. A boundary closure re-decides the caps at a junction at its ends; a device that changes mode has its cap re-decided.
5. **At a failed Newton (A274).** The last iterate releases every cap it refutes; when none, it states every line it holds at its cap (flow at the cap flow, drive past the cap drop). The pass is solved again from its start.
6. **What a step reports (A273).** `VELOCITY_LIMITED` (and the device forms) for a line the solve ran capped.

The real-life sequence on the card world: the generator's line starts choked (A271: 55 kPa against 3 kPa); J1's outlets start on their law; the pass converges with J1 just above tank B; nothing changes; the next step starts the same way (A271's carry).

A junction between two capped lines (the brief's question, A276): a junction holds no stock and has no pressure row of its own. A line on its cap leaving the junction reads the junction's own state (`rho_J A v`), so the junction stands where the stated flows and the lines on their laws balance; a gas junction between a choked inflow and a choked outflow stands where `rho_J A_out v = q_in`, which is how a real choked nozzle passes a flow in proportion to its upstream pressure. If no line of a junction read its state (every line stated from elsewhere), its pressure would have no equation; in real life it moves until one line leaves its cap, which is what the converged-point and failed-point rules do. Forbidding such a junction at the failure entries (keeping the line nearest its cap on its law) was measured and is worse (section 5).

The certificate (A277): unchanged. A capped line's drive exceeds its cap drop, so an island with one is never at rest under the 0.1 Pa gauge; STEADY compares endpoint modes, of which the cap state is one.

### 3. What it absorbed

- The clamp branch of the flow row (`if(|driving|>limitDrop) f=(copySign(limitDrop,driving)-loss)/capScale`) and its choice of the cap's end by the driving pressure's sign inside every residual and Jacobian probe: a capped line's direction is now its state.
- The post-convergence throw "Velocity constraint did not close" for throttling modes: it is now the entry test. It stays for the modes that state their own flow (a pump at its target, a regulating valve).
- The accepted-mode label's own test (any line within 1e-7 of its cap): the label is the state.
- Not absorbed: the dead-end exemption (`deadEnd`): a dead-ended line must not start capped, because a capped row on a pocket with no other way out leaves the pocket's pressure without a row (F8). The two-ended row scale of option (a) (A88) is unchanged: it weighs both modes.

### 4. Gate test

`VelocityCapModeTest.aJunctionBetweenALineAtItsCapAndItsOutletsFindsItsPressure`: card E6-C's world placed as `y25` gives it, run 120 slices. With the mode: runs clean (`ca42`). With the old cap restored in the working tree (`src/scratch-t19-old-cap.patch`, the reverse of `fefaeaf9`): held at tick 1159 on "Newton iteration limit at residual 0.6514199613667255", the card's own hold to the last digit (`ca43`). The same patch, applied for a G1 run (`ca47`), reproduces every reference exactly (0 junction diff lines, MIXED_GAS_COST 264, chain-100 exact) and fails only this test.

### 5. Forms measured

Fuzz seeds 1-200 of both distributions against the same seeds of `ig10` (186 random, 190 world defaults clean), the suite tests the change touched (`ExtremeTopologyIslandTest`, `StepStartFlowsTest`, `InlineFilterLineHoldTest`, `LiquidFullTankOutletsTest`, `TraceFlushLiquidFullTankTest`, `VelocityClampTest`) and the E6-C seeds alone. The forms above the last two rows started a line touching a junction on its law and entered on the drive alone at a failed iterate; those two rules were changed last (A271, A274).

| form | touched tests failing | random | world defaults | log |
|---|---|---|---|---|
| intermediate pure form | 2 (ALTERNATING_BOTH_SIDES cycle, StepStartFlows) | 191 (+5) | 192 (+2) | `ca26` |
| failure: entries only | 4 | 188 (4 worse) | 188 (2 worse) | `ca28-var-enter` |
| failure: each line once per solve | 6 | 185 (5 worse) | 186 (4 worse) | `ca28-var-mono` |
| failure: one correction per solve | 7 | 183 (7 worse) | 186 (4 worse) | `ca33` |
| failure: no correction | 10 | 151 (36 worse) | 172 (18 worse) | `ca28-var-none` |
| A276 option: never every line of a junction stated | 1 | 183 (8 worse) | 191 | `ca32` |
| hybrid: open lines keep the clamp row, stated after a failure | 0 | 188 (+2) | 192 (+2) | `ca29` |
| escalation: clamp rows until the first failure with a clamped line | 0 | 185 (1 worse) | 191 | `ca34` |
| held entries at a failure | 1 (ALTERNATING_BOTH_SIDES) | 190 (1 worse) | 192 | `ca39` |
| **held entries and every line decided at the start (taken)** | **0** | **192 (+6)** | **192 (+2)** | `ca40`, `ca41` |
| taken, plus no cap in a forbidden direction | 0 | 186 (5 worse) | 176 (15 worse) | `ca50` |

Full campaigns: intermediate pure form 948 / 959 (`ca35`), hybrid 939 / 959 (`ca36`, Newton iterations 4.86 M / 20.41 M, E6-C 2 of 6), taken form 951 / 962 (`ca44`). The hybrid and escalation keep the clamp branch and clear 2 and 1 of the six E6-C worlds; they are kept in `src/scratch-t19-cap-variants-and-trace.patch` (switches `-Dt19hybrid`, `-Dt19escalate`, `-Dt19fail=...`, `-Dt19a276`, `-Dt19held`, `-Dt19startall`, `-Dt19old`, against `0d841a5f`). The 200-seed counts of one form move by one or two between runs on a loaded machine (the fuzz's wall-clock soft budget switches a slow interval to approximate acceptance); the same form's seeds alone are identical run to run (`ca40` and `ca41` alone: every hold and per-seed counter).

### 6. Campaign `ca44` against `ig10`

| | `ig10` | `ca44` |
|---|---|---|
| random / world defaults clean | 935 / 957 | **951 / 962** |
| cleared / worse (random; world defaults) | | 20 / 4; 9 / 4 |
| Newton iterations r / d | 1.78 M / 12.34 M | 1.89 M / 9.11 M |
| Newton solves r / d | 0.305 M / 1.247 M | 0.335 M / 0.963 M |
| gate refusals r / d | 22 847 / 20 185 | 30 035 / 15 338 |

Class counts `ig10` -> `ca44` (random / world defaults): C 15 -> 16 / 3 -> 4; E2 7 -> 8 / 10 -> 10; E3 1 -> 0 / 0; E5 1 -> 1 / 0; E6 24 -> 6 / 17 -> 13; E7 2 -> 3 / 2 -> 2; E8 1 -> 2 / 2 -> 2; E9 4 -> 4 / 3 -> 2; E10 3 -> 3 / 0; F 7 -> 6 / 6 -> 5.

Cleared: random E6 19 (19 65 92 118 151 173 244 286 336 460 499 529 556 600 824 842 886 915 916) and E3 301; world defaults E6 6 (145 336 475 681 833 961), C 164, E7 734, F 929. Moved class: random 116 E7 -> C, 246 F -> E2, 629 E5 -> E7, 922 C -> E7, 937 E6 -> C; world defaults 228 E6 -> E2, 485 E2 -> E6, 606 E6 -> C, 792 E9 -> C.

Worse: random 9 (E8), 248 (E5), 597, 660 (E6); world defaults 214, 364, 981 (E6), 547 (E7). All eight hold again alone in their class (`ca45`); every budget hold holds again alone (random 77 431 547 678 707 788, world defaults 156 298 563 622 788, under the alone lock, `ca45`).

The six E6-C worlds alone (`ca01` before, `ca52` after):

| seed | before | after | Newton iterations before / after |
|---|---|---|---|
| random 151 | E6 tick 2845, 0.0709 | clean | 3 364 / 195 |
| random 173 | E6 tick 1060, 0.538 | clean | 3 929 / 705 |
| random 286 | E6 tick 1900, 3.34 | clean | 3 680 / 938 |
| random 336 | E6 tick 2146, 0.00247 | clean | 6 529 / 2 638 |
| random 529 | E6 tick 1159, 0.651 | clean | 5 548 / 1 939 |
| random 594 | E6 tick 2016, 0.545 | E6 tick 2016, 1.2e-8 (S) | 3 928 / 4 897 |
| world defaults 164 (group C on `ig13`) | C tick 5304 | clean | 20 538 / 31 436 |

### 7. The worse worlds, by mechanism (traced with `src/instr-t19-cap-trace.patch`, `ca49`, `ca53`)

- **Random 597, world defaults 485 (E6, B):** a new island's first solve sends a flow the boundary forbids (42 kg/s of liquid into a 129 kPa generator; a 1 atm void supplying a pump's suction). No line near its cap; the cap mode changed only the Newton's path (card E6-B, t15's rule (c)).
- **Random 660, world defaults 214 and 364 (E6, S):** plateaus at 7.4e-5, 1.4e-6 and 2.3e-8 on near-zero flows, no line near its cap (660: the cap release taken before a valve's closure re-solves the pass, which then ends on the plateau). Card E6-S.
- **Random 248 (E5):** a generator's choked line feeds a junction pair whose every other line is choked (into a tank and a void), so the pair's pressures are set only by their outflows' density; the Newton stalls at the projection bound of the pipe between them (2 028 Pa of drive against a 2 072 Pa cap drop). The junction between stated flows of section 2; its option was measured worse.
- **World defaults 981 (E6):** a junction standing at 91.6 kPa below a 1 atm void; the failed iterate put the void's line on its cap supplying the junction, a direction a void never takes. Refusing such a cap (`src/scratch-t19-cap-only-allowed-directions.patch`) is much worse (world defaults 176 of 200, `ca50`): a forbidden flow in an intermediate pass needs its cap as a bound until the boundary rule closes it at a converged point.
- **Random 9 (E8):** the interval runs out of substeps on 516 equation-gate refusals; no Newton fails.
- **World defaults 547 (E7):** no finite Jacobian on a water line; no cap involved.

Not minimized: every worse world was traced on the seed itself; the mechanisms above are read from the failing solve, and five of the eight carry no capped line at all. Minimization keeping each signature is left (stop point S4).

### 8. References moved (old with the old cap restored, `ca47`; new `ca46`)

| reference | old | new |
|---|---|---|
| G1 | 530 / 1 failure (the new gate test) | 530 / 0 |
| chain-100 (G2) | exact | exact (0 deviation) |
| 33 junction lines | 0 diff lines | 25 lines move: the 24 MIXED lines and COST; the 8 liquid lines unchanged (`ca48-junction-table.md`, every field) |
| MIXED ledger and junctionShare | e.g. junctionShare 0.5592660373641596 | e.g. 0.559266037228183 (2.4e-10 relative at most); ledger worst values stay at 1e-16 |
| MIXED newtonSolves at 5 s (12 lines) | 21 25 23 25 25 26 19 17 21 18 23 21 | 26 31 37 29 44 30 24 19 27 20 25 23 |
| MIXED_GAS_COST newtonSolves | 264 | 335 |
| printed suite lines | 6105 | 6085; 796 old-only, 776 new-only (`ca48-printed-printed-diff.txt`) |
| GameTest outlets | 124258.48752327717 Pa / 175.7622745472754 kg | same |
| GameTest compressor | 2.9999859075564377 / 157976.31408415755 Pa | same |

Nothing re-recorded. The mixed-gas cases start lines on their cap and change cap states between steps; each change is one more pass.

### 9. Full gates

`ca46-gates-run1` (GameTest run id `t19-ca46-22edb2a9-run1`): G1 530 tests, 0 failures; G2 chain-100 0.000e+00; G3 45 / 45; G5 all 33 required tests passed. No GameTest value moved, so one run.

### 10. Stop points

| # | item | recommendation |
|---|---|---|
| S1 | The 24 mixed-gas lines and MIXED_GAS_COST move (264 to 335 Newton solves at 5 s, the states at 2e-10 relative) | accept; re-baseline once on the batch's final code (D36/D38) |
| S2 | Worse worlds 4 + 4: five carry no capped line (path changes into E6-S and E6-B), one is a junction between stated flows (248), one a cap stated in a forbidden direction (981), one gate refusals (9) | accept with the branch |
| S3 | Card E6-C: five of six clear; 594 now holds as S | close E6-C; 594 joins E6-S |
| S4 | Worse worlds traced on the seeds, not minimized | minimize with the cards' next work |
| S5 | Newton iterations random +6 % (1.78 M to 1.89 M), world defaults -26 % (12.34 M to 9.11 M); random gate refusals +31 % | accept |

Branch: **recommended to become `claude/phase-ports-compressor`** after S1. It clears 16 random and 5 world-default worlds net, every gate is green, and it is the form the brief asked for, with the cap's clamp branch gone.

### 11. Against the brief

- The card world is used as `y25` gives it (the `q04`-era minimization); it still holds on `0d841a5f` exactly as the card says, so it was not minimized again.
- Rules beyond the brief's sketch were forced by measurement (A272's order and re-solve from start, A274's failure corrections, A275's projection, A271's carry); each row names the world that forced it.
- The worse worlds are traced, not minimized (S4).
- t13's group classifier was not re-run; the E6 mechanisms in `REMAINING_HOLDS.md` on `ca44` are read from `ig13` for seeds that kept their class and from this thread's traces for the new ones.
- Commit trailers name the model that wrote them (Claude Opus 5.5), not Claude Fable 5.1.
- Documents are not copied to the main checkout (own worktree only).

## The physical start measured against the chain; the fixed point as the fallback (2026-10-13)

Thread t20, branch `claude/physical-start` from `b4fd19fd` (the integrated code of D47, re-baselined at `0d841a5f`, then docs only; owner decision D48). Decision rows A280 onward (t19 uses A270-A279). Logs `ps01` onward in `tools/phase-ports-probes/hold-classes/logs/`. The campaign compared against is `ig10` (935 random / 957 world defaults), the chain on this code.

### 1. The forms, written before the code

**What happens in real life.** A pipe block placed in the world is full of air at the room's pressure and temperature, like a fresh tank or a fresh void: in the game, nitrogen at 298.15 K and 101 325 Pa (`FluidThermodynamics.initialNitrogenCharge`, the charge `FluidDeviceSpec.initialize` gives every new tank and void). When the block joins a network nothing about the steady state is known yet. Water from a level source falls into a run that goes down (2 m in about 0.64 s, free fall) and pushes the air ahead of it to the nearest gas space; a run that rises stays full of air until a pump or a higher head pushes liquid up into it; a gas line equalises at the speed of sound (a 10 m nitrogen line in about 30 ms). The steady state is what the first interval ends with, not where it starts.

**What the code does today.** Where an island is formed from placed blocks (`PhysicalRegistry.apply`, every edit re-mints its island, D45), `PassiveNetwork.mintJunctions` asks `NetworkReduction.junctionStarts` for each junction's start and sizes its buffer from it (D25: `m_J = 0.05 s x` the velocity-cap mass flow of its widest connection, read on the start state's density). The start is a hand-written steady state: who can reach the junction (nearest, then (m') and its balance pressure, D37, D39, A116-A126), the drain-side gas column (A133), pockets and their strongest supplier (D12), devices (A107, A122), ties and fallbacks (A108, A123), "nothing reaches" (A107). Then the solver's first interval starts. On a structure with no accepted step it first runs one rate solve of the port graph (`PassiveIntervalSolver.coldRateSeed`; tanks held as ports, junction composition held at the buffer's, `Equations.junctionInflow` rate branch), whose junction pressures and flows the first step starts from; inside each solve a cold junction outside its neighbours' pressure range gets a balanced pressure guess (`PassiveStepSolver.seedBoundaryJunctions`), and a cold rate solve closes generators the one-way estimate shows receiving (D12, `closeColdReceivingGenerators`). Those three pieces are the solver's own first instant; they stay in every form below.

A junction's start therefore has three roles: (1) its buffer, the history term `(m_J/dt) w_old` of the backward-Euler mixing row (D25, D46); (2) the size `m_J`, read on the start's density; (3) the first Newton guess of its pressure, temperature and phase set, before the cold rate seed replaces the pressure.

**A280 (P-air): every minted junction starts as ambient air.** Its state is the game's nitrogen charge, 298.15 K, 101 325 Pa (one cached state, `initialNitrogenCharge(1, 298.15, 101325)`); its buffer is `m_J` of that gas (w = pure nitrogen, h its enthalpy); `m_J` by D25's rule, unchanged as a rule, read on that state. Options for the pressure: (a) 101 325 Pa at every height; (b) 101 325 Pa less the nitrogen column to the junction's height (11.2 Pa/m at 1.145 kg/m3) from a reference height. **Recommended and implemented: (a).** The game places every tank and void at 101 325 Pa at any height, so the world has no altitude for its ambient; (b) needs a reference height the game does not have (y = 0, or the island's lowest node, are both inventions), and the difference is 11.2 Pa per block against the kPa drives of the first interval. The cold rate seed then places every junction's pressure from the hydraulics anyway.
- Consequence of D25's rule on air: in a 0.05 m pipe, `m_J` = 0.05 x 1.145 x 1.963e-3 x 100 = 0.0112 kg, where a water start gave 9.79 kg (871 times more). The mixing lag `m_J/Q` of a junction carrying 7 kg/s of water falls from 1.4 s to 1.6 ms; for a gas line at its cap the lag stays 0.05 s. Options: (a) D25's rule on the air start (the rule unchanged; implemented); (b) `m_J` read on the densest fluid of the island's boundaries (a new rule at the mint, refused by D48); (c) re-size when liquid arrives (removed by D46). **Recommended: (a)**; its effect is measured separately from the start itself (a diagnostic run with `m_J` read on the chain's start, A284).

**A281 (P-empty): every minted junction starts with nothing.** The code has no state for "nothing": a vessel with no inventory cannot be solved ("Evacuated reservoir has no fluid temperature"), a junction's mass is pinned at `m_J`, and a vacuum pipe would lift a level water source about 10 m (the owner dropped the vacuum start, D25). The precise meaning taken: the buffer holds nothing at the first instant, so the first accepted step's mixing row has no history term, `Q w = sum q_in w_in` (the quasi-steady row: the junction is what arrives), and `m_J` with its backward-Euler row from the second step on (D25, D46). The junction still needs a first Newton guess and a size: both are P-air's (ambient nitrogen, `m_J` on it).
- How it is solved. The no-history row is the backward-Euler row whose `w_old` equals its own solution. So the first step of a new island is solved, its junction buffers are replaced by what that step left in them (`m_J w`, `m_J h`), and the same step is solved again from the same vessel states, until no buffer moves (relative change below 1e-12, at most 20 rounds). At the fixed point `(m_J/dt)(w - w_old) = 0` exactly, so the Newton row, the reconstruction (`ConservativeTransport.reconstruct`, the same row) and the ledger need no change: a buffer is not inventory (D46), its replacement books nothing, and the step's junction term is zero. Each round contracts the history by `(m_J/dt)/(m_J/dt + Q)`, about 0.03 for water arriving at 7 kg/s in the first 0.05 s step.
- Where nothing arrives in the first step (`Q = 0`: a rising run nothing lifts, a dead end), the quasi-steady row has no solution; the iteration leaves the buffer as it is (ambient nitrogen). "Nothing" cannot stand still: where nothing arrives, P-empty is P-air. Option rejected: a regularised row with a vanishing buffer (singular at rest, the reason the buffer exists, D25).
- "First accepted step" is the first accepted step of an island job with no committed interval (`RetainedSolver` hands `committed = null` only to a new island, and every edit makes new islands, D45). A refused attempt starts again from the minted buffers.

**A282 Both physical forms, consequences.**
- Every minted junction gets the start; kept contents were not taken (D45, A130). Every edit re-mints its island, so a water-filled run restarts as air at every edit of its island, also one far from the edit. The first interval refills it: water arriving at a few kg/s replaces the 11 g of air in a 0.05 m junction within the first 0.05 s step; the air leaves with the outflow to the next gas space (a tank's headspace, a void, a gas-filled junction) as D41's carried gas; a junction nothing reaches keeps its air. The pipes between junctions hold nothing in the model (a run is a connection with no holdup), so the only air the model can push is the junctions' buffers.
- The chain is bypassed: no nearest, strongest, mixture, balance, column, pocket, tie, device or fallback rule runs at the mint. The chain's code stays in the file for the default (D48 freezes it; a form taken retires it).
- The committed flows of t15 (a) do not exist for a re-minted island (it is new, `committed = null`): the first step starts from the cold rate seed's flows, and where that fails from the estimate (`initialMassFlows`). Under both forms the estimate reads the air junctions' columns and densities; t15's one column rule (b) is unchanged.

**A283 (F): the island-wide fixed point at the mint.** From the boundaries' states and every junction as ambient air (the single initial condition: a run holds no liquid until something drives liquid into it), iterate: (1) states give flows: one rate solve of the port graph (`PassiveStepSolver.portRate`, the solver's own laws, columns, one-way rules, D12 closures, device active sets and cap), junction compositions held at the current iterate; (2) upwind mixing gives compositions: each junction becomes the flow-weighted mixture of the streams the rate solve drives into it (each donor's end stream, a junction donor's current mixture; mass and enthalpy weighted, the enthalpy with its height gain, flashed at the junction's solved pressure, D34 fold); a junction nothing flows into keeps its composition, restated at its solved pressure; (3) compositions give densities and columns: the next rate solve reads them. Stop when no junction's mass fractions move by more than 1e-9 and no pressure by more than 1e-9 of itself; at most 8 rounds. The result is minted as the chain's was (its state, `m_J` by D25 on it).
- What it absorbs, each with the row it would retire: nearest supplier (D37 E4 option (c), A105); strongest first (D39, A113); mixture (m) and (m') (A112, A116-A118, A120, A121); every junction at its balance pressure (A119, A126); the drain-side gas column (A133, D40's amendment for gas); the unprimed drain (D40) and the primed falling drain (A255, not taken), because the rate solve's own rows read the columns the current compositions give, so a drain is primed exactly when the previous round put liquid in it; pockets and their strongest supplier at the mint (D12's mint part; the solver's pocket rule F8 stays); ties (A108); devices crossed (A107, A122); fallbacks (A123); "nothing reaches" (A107); the column of single streams (A251, A252, not taken).
- When it does not converge (8 rounds, a rate solve that fails, a flash outside the data), the physical start is its own fallback: **P-air** (recommended over P-empty because it needs no change of the first step).
- Where it belongs if taken: it is the cold rate seed iterated (`coldRateSeed` runs its round 1 already, on the worker). At the mint it runs Newton solves where the chain ran bisections, on the thread that applies topology events; taken, it would move into the first interval's cold start (the solver's basin), not stay in the reduction stage.

**A284 One switch.** `-Dcreatecheme.junctionStart=chain|air|empty|fixedpoint` (`NetworkReduction.START_FORM`), read once; default `chain`, the code as it stands, so the branch's default is the base bit for bit. `air` and `empty` mint P-air's start; `empty` also turns on the first-step iteration of A281 in `PassiveIntervalSolver`; `fixedpoint` runs A283 at the mint. A diagnostic value `airchainsize` (P-air's start with `m_J` read on the chain's start) separates the start from the buffer size (A280); it is never a proposition.

Code `483360c4` (the switch and the three forms, default the chain), test `JunctionStartFormTest`.

### 2. The campaigns (A285)

Lane 1, 8 workers, 1000 seeds per distribution, 120 slices; all on `483360c4` with the switch through `JAVA_TOOL_OPTIONS` (`t20-lane.sh START=...`). F's 200-seed quick check (`ps03`) came within 10 worlds of P-air (163 / 181 against P-air's 160 / 184 on the same seeds), so its full campaign ran (`ps05`, with the scratch outcome log `src/instr-t20-fixedpoint-outcome.patch`).

| Form | Log | Random clean | World defaults clean | First-interval family r / d | Newton iterations r / d | Newton solves r / d | Gate refusals r / d |
|---|---|---|---|---|---|---|---|
| Chain (`ig10`, this code's default) | `ig10` | **935** | **957** | 44 / 23 | 1.78 M / 12.34 M | 0.305 M / 1.247 M | 22.8 k / 20.2 k |
| P-air | `ps01` | 822 | 933 | 155 / 36 | 6.40 M / 34.62 M | 0.789 M / 3.223 M | 26.0 k / 49.6 k |
| P-empty | `ps02` | 821 | 909 | 159 / 65 | 5.70 M / 24.73 M | 0.719 M / 2.325 M | 19.7 k / 58.4 k |
| F (fixed point, P-air fallback) | `ps05` | 833 | 936 | 146 / 38 | 5.96 M / 26.57 M | 0.750 M / 2.663 M | 25.5 k / 52.6 k |
| P-air, `m_J` on the chain's start (diagnostic) | `ps04` | 807 | 941 | 179 / 41 | 1.61 M / 13.49 M | 0.231 M / 1.178 M | 19.9 k / 20.4 k |

Against `ig10` (`<log>-vs-ig10.txt`):
- **P-air.** Random 10 cleared (E6 7, E10 2, E3 1) / 123 worse (E6 40, C 39, E2 22, E7 7, F 6, E10 4, E3 2, E5 2, G 1). World defaults 18 cleared (E6 10: 228 244 273 352 397 401 475 606 915 937, all `ig10`'s B and S first-interval worlds; C 3, E2 2, E7 2, F 1) / 42 worse (E6 15, F 10, E7 9, C 3, E8 3, E2 1, E5 1).
- **P-empty.** Random 11 / 125 worse; world defaults 19 / 67 worse (E6 40).
- **F.** Random 11 / 113 worse; world defaults 17 / 38 worse (E6 15, E7 10, F 9). F converged at the mint in 2378 of 2979 mints (2 rounds median), the rate solve failed in 453 (P-air taken), 148 ran out of 8 rounds.
- Class counts `ig10` -> P-air (random / world defaults): C 15 -> 62 / 3 -> 5; E2 7 -> 29 / 10 -> 9; E6 24 -> 51 / 17 -> 21; E7 2 -> 8 / 2 -> 10; F 7 -> 13 / 6 -> 16; E10 3 -> 5 / 0; E3 1 -> 2 / 0; E5 1 -> 3 / 0 -> 1; E8 1 -> 2 / 2 -> 4; E9 4 -> 2 / 3 -> 1; G 0 -> 1 / 0.
- Worse seeds were not replayed alone (owner rule D50, given during this thread; P-air's 130 random worse seeds and budget holds had been replayed before it, `ps09-alone-air`: 129 hold again alone, 6 (F, a wall deadline) runs clean).

### 3. The first-interval holds by mechanism (A286)

Every first-interval hold of the three forms was replayed alone with t13's trace (`src/instr-t18-t13-trace-on-47618332.patch`, `t20-trace.sh`, logs `ps06`-`ps08`; P-air 184 of 191, P-empty 215 of 224, F 174 of 184 reproduce the campaign's first hold to the text, the others are budget seeds whose wall clock moved). Groups (`t20-groups.js`): t13's rule (A164) preceded by the two mechanisms the physical start adds, named by what fails, not by a rule that would hide it.

| Group | Mechanism in one line | P-air d / r | P-empty d / r | F d / r |
|---|---|---|---|---|
| **L** (new) | A junction minted gas-only receives liquid in its first step; the pass is laid out with the seed's phases (gas), so the arriving water or crude has no liquid unknowns; the mixing and enthalpy rows of that junction carry the residual, the trial temperatures leave the data (water below 273.16 K, nitrogen above 900 K, crude below 293.15 K) and the step halves to the floor | 26 / 102 | 57 / 106 | 29 / 100 |
| **C0** (new) | A gas-only island whose junctions start below every neighbour: every connection into them is on its velocity cap, none leaves, so their continuity rows depend on no pressure: singular at pass 0 of the rate seed and of every step | 0 / 14 | 0 / 15 | 0 / 14 |
| S | Start flows that are not the solution's (t13) | 6 / 12 | 6 / 12 | 6 / 12 |
| B | A forbidden boundary direction open on a first solve (t13) | 2 / 12 | 2 / 12 | 2 / 7 |
| C | A junction between capped gas connections (t13) | 0 / 15 | 0 / 14 | 0 / 13 |
| T | Saturation row of a real two-phase junction | 2 / 0 | 0 / 0 | 1 / 0 |

Card L in numbers (world defaults 745, 8 blocks, P-air): a 1 atm water generator 1 m above a junction that also feeds a void at its own height. In real life water falls 1 m into the junction (about 7 kg/s through 0.05 m) and spills into the void; the 11 g of air in the junction leaves first, in a few milliseconds. The solver: the rate seed stops at its iteration limit (1.58, flow rows), the first 0.05 s step's pass 0 stalls at 3023 on the junction's enthalpy row (a gas-only layout receiving 996 kg/m3 water), pass 1 (water added by the phase correction) ends at 3.3, and every halving repeats the pair down to 1e-7 s. Card C0 (random 72, 4 nodes): two nitrogen junctions at 101 325 Pa between a 117.6 kPa generator and a 117.5 kPa tank; every connection into them on its cap (the 0.01 m runs) and none out, so the continuity rows are flat in both pressures: singular at every step size.

Physics first: L is the event a physical start is about, liquid displacing gas in a fitting. The chain hides it by starting the junction already full of what arrives. In the solver it is a phase appearance inside one step at a junction whose layout is frozen per pass (card E5's and D34's territory): the basin, not the mint. C0 is card E6-C (the cap's flat row, thread t19's active set) at the first instant, made frequent because an air start puts every gas junction far below its feeders. S, B, C of `ig10` stay at their size or shrink (the air start clears ten B and S worlds of `ig10` in world defaults), and no T trace family returns.

### 4. References (A287)

The default is the chain: no line of the code it runs changed, and the final `gates.sh` of section 6 is on the default. Under F (quick check `ps03`, G1 under `-Dcreatecheme.junctionStart=fixedpoint`): 529 tests, 5 failures (`CarriedGasTest`, `InlineFilterLineHoldTest` x2, `JunctionStartMixtureTest`, `VesselClosureEquilibriumTest`), the 33 junction lines identical (0 diff lines: those fixtures build their graphs directly, so they are never minted, A106), MIXED_GAS_COST 264, chain-100 exact (it has no mint). P-air and P-empty were not run through the suites (D50: references once, for the proposed form).

### 5. Proposition (A288)

**What happens in real life**, and what the player would see under a physical start. Every edit re-mints its island (D45), so a water-filled run restarts as air at every edit near it. In the model a run is a connection without holdup and a junction buffer is 11 g of air (0.05 m), so the refill is the first step: at 7 kg/s the air leaves in about 2 ms and the GUI's first 5 s snapshot shows the run full of water and flowing, as a real pipe would after a few seconds of burping. What happens in real life matches the physical start; what the solver does with it (L, C0) does not yet.

| Form | Clean r / d | First-interval family r / d | Newton iterations r / d | Retires | Keeps |
|---|---|---|---|---|---|
| Chain (frozen, D48) | 935 / 957 | 44 / 23 | 1.78 M / 12.34 M | nothing | D37, D39, D40, A112, A119, A126-A133, A251 |
| P-air | 822 / 933 | 155 / 36 | 6.40 M / 34.62 M | D37 (E4 (c)), D39, D40, A112, A119, A126, A127, A129, A131 (its gate world), A133; A251 and A255 never taken | D25's sizing rule (read on air), D46 |
| P-empty | 821 / 909 | 159 / 65 | 5.70 M / 24.73 M | as P-air, and the first step's history term | as P-air |
| F | 833 / 936 | 146 / 38 | 5.96 M / 26.57 M | as P-air (absorbed into the rate solves) | P-air as its fallback; 2-8 rate solves per mint on the event thread |

Recommended: **keep the frozen chain as the default now; take P-air as the start once the basin absorbs L and C0**, then retire D37, D39, D40, A112, A119, A126-A133 in one step. Reasons: P-air is the physical statement and needs no rule at the mint; P-empty has no state of its own and is worse; F gains 11 random worlds on P-air for 2-8 rate solves at every mint and is the chain's job done by Newton (taken, it belongs in the first interval's cold start, which already runs its first round). The physical start costs 113 random and 24 world-default worlds today, almost all L (128 of 191 traced holds) and C0 (14), both first-step defects of the solver's basin: L = a junction's first step must admit the phase that arrives (layout from the inflow, card E5 (b)'s phase-set step); C0 = the cap as an active-set mode (t19).

### 6. Gates on the default and stop points

**Gates: not run.** The one final `gates.sh` on the default (D50) was not run: a batch of alone replays started before D50 (`t20-alone-batch.sh`, under the alone lock) was still running P-empty's and F's lists when D50 arrived, and stopping it was refused by the permission layer, so the lock stayed held and no second Gradle run was started beside it. The default runs no changed line (the switch reads `chain` and calls the chain unchanged; `mintJunctions` sizes on the same list; the P-empty iteration is off), so the gates are expected identical to `ig10`'s base (`ec01`: G1 529 / 0 plus the new test, junction diff 0, COST 264, chain-100 exact, GameTests 124258.48752327717 / 175.7622745472754 and 2.9999859075564377 / 157976.31408415755); this is unproven until the run.

| Stop point | Item | Recommendation |
|---|---|---|
| S1 | A288: which form | keep the chain as the default; P-air after L and C0 are solved in the basin |
| S2 | A280: `m_J` read on air (871 times smaller for water) | keep D25's rule on the start (the diagnostic `airchainsize` is not better: 807 / 941) |
| S3 | Group L as the next basin item (a junction's first step admits the arriving phase) | open a card after t19 |
| S4 | Gates on the default | run `gates.sh logs/ps10-gates-default t20-ps10-867a04df-run1` once the alone batch ends |

### 7. Against the brief

- D50 arrived mid-thread: worse seeds of P-air random (130) and world defaults (48) had been replayed alone (`ps09-alone-air`, 1 + 1 clean alone: random 6, world defaults 357); the batch then went on to P-empty and F, which D50 cancels, and could not be stopped (section 6). No minimisation was made; the mechanism split used the existing t13 trace and classifier on every first-interval hold of all three forms (done before D50).
- The brief's "33 junction lines move under a physical start": under F they do not (0 diff lines); their fixtures build graphs directly and are never minted (A106).
- Trace folders `ps06`-`ps08` (38 MB) are not committed; regenerate with `t20-trace.sh` and the trace patch.
- Commit trailers name Claude Opus 5.5 (the writing model). Documents are not copied to the main checkout.

## The flow row in flow form near its root (2026-10-13)

Branch `claude/flow-form` from `claude/cap-active-set` `71a1abfb` (the integration `0d841a5f` with t19's cap mode), merged with `claude/phase-ports-compressor` `b4fd19fd` (t18's record, D48) as `cdd73268`. Thread t21, card E6-S option (c), directed by D48. Decisions A290 onward. Logs `ff01` onward in `tools/phase-ports-probes/hold-classes/logs/`.

### 1. The physics and what the solver did

A pipe near rest carries a flow proportional to its drive: laminar, `q = G x drive`, `G = pi rho D^4 / (128 mu L)`. A line between two junctions that stand level carries nothing; a loop of junctions with no net drive around it carries no circulation. The flow row states the law in loss form, `(d - loss(q))/S`. A Newton linearises it at the iterate's flow: on the turbulent branch `loss = c q|q|`, and the linear model of a line whose drive has just fallen to zero predicts `q0/2`. Each iteration halves the flow; a root a hundred to ten thousand times below the start flow is 7 to 13 halvings away, and the solver's chord iterations contract less than a fresh one.

Measured on `ca44`'s code (`ff01`: the E6-S seeds alone; `ff02`: the ten that hold in E6, traced with `src/instr-t21-trace.patch`, which adds one line per Newton iteration to t19's pass trace). Eight of the ten fail on a flow row of a line that ends near zero drive with a flow far above what its drive gives; the residual falls by about 0.7 per chord iteration and 0.3 per fresh one, and runs out at the 20th. Examples, start of the failed pass and its last iterate:

| world | line | start | last iterate (20 iterations) |
|---|---|---|---|
| random 768 | e6 | -0.0112 kg/s at -29.5 kPa (on its law) | -3.0e-5 kg/s at -0.108 Pa, residual 1.9e-5 |
| random 594 | e0 | 0.0278 kg/s at 5.46 kPa | 2.4e-5 kg/s at 0.149 Pa, residual 1.2e-8 |
| random 660 | e4, e5, e7 (a loop of three junctions) | 0.71, -0.74, -0.73 kg/s circulating | 0.003 to 0.014 kg/s at 0.2 to 2.3 Pa, residual 7.4e-5 |
| world defaults 175 | e6 | 2.32 kg/s at 4.31 kPa | -0.027 kg/s at -0.82 Pa, residual 2.0e-6 |

World defaults 397 is not this mechanism (a node row at dt 2.2e-16 after 139 active-set cycles, the trace-underflow class), and world defaults 364 alternates between two chord iterates before it walks.

### 2. The options (A291-A295, written before the code)

- (c1) Flow form on the laminar branch, loss form beyond (A291): on the laminar branch the law is linear and both forms are one equation; C1 continuity forces the loss row itself. It reaches nothing of the walk, which starts on the turbulent branch.
- (c2) Flow form everywhere, `(q - Q(d))/Q_s` (A292): one step for the flows, but no fixed flow unit keeps the loss row's tolerance: it either loosens the row at a flowing root or tightens it near rest by two to three orders, where the equation gate would read a vessel's booked-pressure roundoff that much larger.
- (c3) One smooth weight (A293): weighted as the card words it (flow form near the root) it puts the loss form where the walk is; weighted by the drive it needs the inverse anyway and adds weight-derivative terms.
- Recommended (A294): the flow form in the loss row's own unit, `F = L'(q^)(q^ - q)/S`, `q^ = Q(d)`: the loss row linearised at the flow the law gives instead of at the iterate's flow. Linear in the flow, the same derivatives, tolerance, gate and `S` at its root as the loss row, continuous at the donor switch near rest. The capped row is the same row with `q^ = q_cap`. A pump at its target, a regulating valve, a closed device and an inline filter keep the loss row (A295).

### 3. Forms measured (A296)

Every form below is the same code (`Pipe.lawAt`, the law's inverse; `Equations.law`, its cache; the weighted row in `edgeRows`) with the weight's two Reynolds numbers (and, last row, the unit) set by scratch switches in the working tree. "E6-S seeds" are the 54 seeds of section 1 alone (21 random, 33 world defaults; base `ff01`: 17 / 19 clean). "1-200" are fuzz seeds 1-200 of a campaign against the same seeds of `ca44` (191 / 192 clean).

| form | E6-S seeds clean r / d | 1-200 clean r / d (worse) | log |
|---|---|---|---|
| base (loss row) | 17 / 19 | 191 / 192 | `ff01`, `ca44` |
| flow form everywhere, cap row too (A294 as first written) | 11 / 13 | not run | `ff03` |
| flow form up to Re 1000, loss row from Re 2000 | 16 / 20 | 191 / 192 (r 107; cleared r 9) | `ff05`, `ff06` |
| up to Re 2000, from Re 4000 | 16 / 21 | 190 / 189 (r 71 72; d 78 101 123) | `ff08-sweep-re2000-4000` |
| up to Re 4000, from Re 20000 | 11 / 15 | 180 / 183 | `ff08-sweep-re4000-20000` |
| up to Re 20000, from Re 200000 | 11 / 13 | 177 / 174 | `ff08-sweep-re20000-200000` |

The trend is monotone: the further the flow form reaches into the turbulent branch, the more worlds hold. Traced (`ff04`): with the flow form everywhere, world defaults 606 stalls its line search on a water line near rest that must carry 0.13 kg/s against a 0.019 Pa drive. Below 0.5 Pa such a line moves 0.15 kg/s per pascal (laminar), above it far less (turbulent); in flow form that whole change of slope lands on the end pressures, and every Newton step overshoots it. The loss row keeps the pressures linear. Random 107 (clean on the base) instead ends a chord sequence contracting 0.75 per iteration on a loop of junctions at 1 atm.

Chosen (A296): the flow form up to Re 1000, the loss row from Re 2000 (the law's laminar branch ends at Re 2000). The form comparison ends here (lean rule, D50: at most the quick checks above; a fixed-unit variant queued before the rule was stopped unrun).

### 4. The form, as implemented (`1b49f341`)

- `PassiveNetwork.Pipe.lawAt(drop, rho, mu)`: the run's law inverted (a Newton kept inside the bracket `[0, drop / laminar slope]`, which holds because every branch loses at least the laminar loss), with the slope and Reynolds number at that flow; `PipeResistance.laminarSlope` is the laminar expression the law already used.
- `Equations.law`: one cache entry per connection keyed on drive, density and viscosity (a Jacobian column that moves neither reads it back).
- `edgeRows`: for a passive line without a filter, a pump on its head limit and an open valve on their law, `F = w L'(q^)(q^ - q)/S + (1 - w)(d - loss(q))/S`, `w` a smoothstep in the law's Reynolds number at `q^`. A row on a turbulent drive is bitwise the loss row. Capped lines, pumps at their target, regulating valves, closed devices and inline filters keep their rows.
- What it absorbed: nothing. The row scale `S` (option (a)) stays the unit; t19's capped row stays (its flow is turbulent, `w = 0`); `headLimitMassFlow` and `startInsideCaps`, which exist because of the walk, stay because the walk they avoid is turbulent.

### 5. Gate test (`9c3198fb`)

`FlowFormRowTest.aLoopWithNoNetDriveSettlesWithinTheNewton`: random 594 (t19's former E6-C world, held as S on `ca44`), minimized to 8 blocks keeping its hold with the loss row restored (`ff11`): a 338 kPa nitrogen generator fills a 1 atm tank through two 1 cm pipes to a junction that reaches the tank by its side and over its top. Run alone (`ff12`): with the flow form it runs 120 slices; with `src/scratch-t21-loss-row.patch` (the weight set to zero, which restores the loss row bit for bit) it holds at tick 141 on "Newton iteration limit at residual 1.1444531583316178E-6", the minimized world's own hold to the last digit.

### 6. Campaign `ff13` (8 workers) against `ca44` and `ig10`

| | `ig10` | `ca44` | `ff13` |
|---|---|---|---|
| random / world defaults clean | 935 / 957 | 951 / 962 | **949 / 958** |
| Newton iterations r / d | 1.78 M / 12.34 M | 1.89 M / 9.11 M | 2.05 M / 14.39 M |
| Newton solves r / d | 0.305 M / 1.247 M | 0.335 M / 0.963 M | 0.352 M / 1.238 M |
| gate refusals r / d | 22 847 / 20 185 | 30 035 / 15 338 | 23 228 / 18 199 |
| E6 r / d | 24 / 17 | 6 / 13 | 6 / 14 |

Against `ca44`: cleared random 594 (E6) and 9 (E8), world defaults 214 and 364 (E6, t19's S worse worlds). Worse: random 107, 450 (E5), 757, 916 (E6); world defaults 322, 556, 854, 951 (E6), 431, 786 (F). Moved: random 707 F to C, 768 E6 to E2, 803 E8 to F; world defaults 17 E6 to F, 29 E2 to F, 228 E2 to E5, 246 C to F, 485 E6 to E5, 606 C to E2, 778 E8 to F, 788 F to E6. Class counts `ca44` -> `ff13` (random / world defaults): C 16 -> 17 / 4 -> 2; E2 8 -> 9 / 10 -> 9; E5 1 -> 3 / 0 -> 2; E6 6 -> 6 / 13 -> 14; E7 3 -> 3 / 2 -> 2; E8 2 -> 0 / 2 -> 1; E9 4 -> 4 / 2 -> 2; E10 3 -> 3 / 0; F 6 -> 6 / 5 -> 10. The world-default counters rise (Newton iterations +58 %, solves +29 %) against `ca44`'s 4-worker run; part of the F rise is load: of the new budget-class holds replayed alone under the alone lock (`ff14`), world defaults 786 runs clean, 17 holds in E6 (as on `ca44`), 29 in E8, and 431, 778 and random 803 hold on the budget alone too.

### 7. Worse worlds (the two smallest minimized and traced, D50)

- **World defaults 951** (18 blocks; minimized to 6, `ff15`): two tanks at 111 kPa above a 1 atm water generator, through one junction. The failed iterate (`ff16`, traced) sends 0.10 kg/s into the generator, a direction a generator never takes, while the tank-to-tank lines carry 0.08 and -0.02 kg/s on a 0.8 Pa drive. Holds with the loss row restored too (tick 6951): group B, reached by the new path.
- **World defaults 322** (19 blocks; minimized to 8): two tanks, a void and a 1 atm water generator; line search stall, then the Newton limit at 0.0094. Holds with the loss row restored too (tick 2766, equation gate 1.006e-8).
- Not minimized: random 107, 450, 757, 916; world defaults 556, 854; world defaults 431 is a budget hold alone.

### 8. E6 by mechanism on `ff13` (t13's classifier, `ff17`; trace patch re-anchored as `src/instr-t21-t13-trace-on-1b49f341.patch`)

World defaults: S 7 (175 322 352 397 401 915 937), B 6 (244 273 556 788 951 981), C 1 (854). Random: S 3 (660 757 916), B 2 (597 853), T 1 (698). On `ca44` (REMAINING_HOLDS): S 8 / 3, B 4 / 2, T 0 / 1, cap residue 1 / 0.

Why group S stays: the walk the card describes is real (section 1), but on gas lines it runs on the turbulent branch. Random 660 (nitrogen at 4 bar, 5 cm pipes) leaves the laminar branch at about 0.04 Pa of drive (laminar slope 26 Pa per kg/s, Re 2000 at 1.4e-3 kg/s); its loop walks from 0.7 kg/s down to 0.01 kg/s on drives of 2 Pa, all turbulent, and fails to the last digit as on the base (`ff07`). The flow form reaches it only by moving the square root of the turbulent law onto the end pressures, which section 3 measured worse at every reach.

### 9. References and gates (one run, `ff18`, GameTest run id `t21-ff18-9c3198fb-run1`; old = t19's `ca46` on `22edb2a9`; nothing re-recorded)

| reference | old (`ca46`) | new (`ff18`) |
|---|---|---|
| G1 fluid suites | 530 / 0 failures | 531 / **7 failures** (the new gate test passes) |
| failing | - | `FilterBlockLineIslandTest.filterLineToATankKeepsIntegratingOnceTheTankIsFull` (held at interval 1 on a line search stall, 0.00129); `FullTankSolidsEventTest` x3 (filter lines into a full tank); `StepStartFlowsTest` (t15's E6-S card world: held at tick 1510, soft budget, "Error probe changes a phase regime"); `JunctionWaterTraceTest` x2 (Nonconvergence in a dry-seeded junction fed water vapour) |
| 33 junction lines | t19's lines (`ca46`) | 22 MIXED_GAS_TRANSIENT lines move at roundoff (ledger worst values at 1e-16, junctionShare at 1e-13 relative, `ff18-junction-table.md`); Newton solves and the 8 liquid lines unchanged |
| MIXED_GAS_COST newtonSolves | 335 | 335 |
| chain-100 (G2) | exact | 2.1e-12 relative in moles, 2.0e-11 K (red) |
| G3 adjacent | 45 / 45 | 44 / 45 (`FilterBlockLineIslandTest`, as in G1) |
| GameTest outlets | 124258.48752327717 Pa / 175.7622745472754 kg | same |
| GameTest compressor | 2.9999859075564377 / 157976.31408415755 Pa | same; G5 33 / 33 |

The printed suite lines were not compared (D50 names them part of G1; the failing tests' lines differ anyway). By D50 no second gates run was made, and the failures were not traced: four of the seven are filter lines, whose own row keeps the loss form, joined by passive lines that now take the flow form near rest.

### 10. Stop points

| # | item | recommendation |
|---|---|---|
| S1 | The flow form near rest (A296) does not carry the campaign (949 / 958 against 951 / 962) and fails 7 suite tests and chain-100 | do not take the branch; keep the loss row (A298 (a)) |
| S2 | Group E6-S is a turbulent walk on gas lines, which no flow form measured here reaches without moving the law's square root onto the pressures | route it to the start (D48: physical start or the island-wide fixed point) or to a step that removes a loop's circulation (A298 (c)); not measured |
| S3 | `PassiveNetwork.Pipe.lawAt` (the law's inverse) is reusable by the estimate's three bisections | only if a later item needs it |
| S4 | Two minimized worse worlds (defaults 951, 322) hold with the loss row too: existing mechanisms (B) on a new path | none |

Branch: **not recommended to be taken.** The physics of the card is right (a near-rest line in flow form is one Newton step from its law, and the gate world shows it), but the remaining walk lives on the turbulent branch.

### 11. Against the brief

- A294 (the flow form everywhere) was recommended before the code and measured worse; the form implemented is option (c3) weighted by the drive's Reynolds number (A296), which the measurement forced.
- The lean rule (D50) arrived mid-task: four forms had been quick-checked; the fifth (a fixed unit) was stopped unrun. A queued batch from before a session restart started on a changed tree and was stopped; its partial logs were deleted. One campaign at 8 workers (lane 1 was free); only new budget holds replayed alone; two worse worlds minimized and traced; one gates run.
- The suite failures were not traced (D50); the branch is not recommended, so no fix was attempted.
- The E6-S seeds before and after were replayed alone before D50 (`ff01`, `ff05`); not repeated on the final code.
- Commit trailers name the model that wrote them (Claude Opus 5.5), not Claude Fable 5.1. Documents are not copied to the main checkout.

## Group L: a junction's first step admits the phase that arrives (2026-10-13)

Thread t22, branch `claude/group-l` from `a9f23964` (the batch HEAD: the cap mode of D49, the junction-start switch of D51 with default chain). Owner decision D51 (2): group L is the next item in the solver's basin. Decision rows A300-A307; logs `gl01`-`gl16` in `tools/phase-ports-probes/hold-classes/logs/`. t21's flow-form row is not on this base.

### 1. The physics, in numbers

A fitting placed in the world holds air; under the physical start (P-air, A280) its buffer is 0.0112 kg of nitrogen at 298.15 K and 101 325 Pa. Card L (world defaults 745 under P-air): a 1 atm water generator 1 m above the fitting, which also feeds a void at its own height. The water falls 1 m, about 9.8 kPa of drive, about 7 kg/s through 0.05 m; the air leaves within milliseconds and the fitting passes water. In the solver's backward-Euler mixing row `(m_J/dt + Q) w = (m_J/dt) w_old + sum q w_e` the first 0.05 s step brings 0.35 kg of water to the 11 g buffer: nitrogen is 3.1 % of the mass at its end and about 0.1 % after the second step, and below 1e-9 it is folded (D34). So the first step ends two-phase: nitrogen gas saturated with water vapour (3.17 kPa at 298 K) over free water, near 298 K.

**What the code did.** A pass is laid out for its seeds' phases (`PhaseLayout`), and a junction's seed was its held stock (plus D10's 1e-12 water trace, which gives water vapour, never liquid). The arriving water had no liquid unknown: the enthalpy row could only hold liquid water's enthalpy as vapour, the trials left the data (water below 273.16 K, nitrogen above 900 K), and every step size failed. The phase correction then flashed the failed iterate and the re-pass failed too (t20, A286).

### 2. The forms (A300-A302)

| Form | What it states | Recommendation |
|---|---|---|
| L1 | The seed stage lays each junction out for the phases of its step mixture at the pass's start flows, where the mixture holds a phase the seed lacks | **Taken**: the statement where the layout is decided |
| L2 | The same mixture, used only after a failed pass, as the re-pass's seed | Measured: same count, seven times the iterations |
| L3 | The union of the feeders' phase sets, every pass, flow or no flow | Rejected on the physics: phases nothing delivers are vanishing phases (cards E6-T, E5) |

L1 (`PassiveStepSolver.arrivingPhases`, the end of `initialPhaseSeeds`): each junction's mixture is its held stock plus what its connections deliver at the pass's start flows over `dt`, at the mass-weighted temperature and the seed's pressure, flashed and folded as the pass folds a junction (D34, D41), scaled to the seed's mass; a junction donor delivers its updated seed, so a run of junctions gains the phase in one stage. Where nothing new appears the seed is unchanged to the bit; vessels and rate-only solves are untouched. Elsewhere: every phase appearance at a junction is laid out from its inflow (under the chain a minted junction has nothing to add; card E5's world and the E4 trace family reach it where the arriving share is above the trace share); a line leaving such a junction decides its start cap on the water density; the new unknowns are a two-phase junction's ordinary ones at 3 % nitrogen, no trace; below the `dt` at which the arriving water would all be vapour (about 3e-5 s in card L) the rule adds nothing, which is what such a step ends with. Absorbed: no code; the failed first pass and its phase-correction re-pass of an L world are no longer reached where the start flows deliver the phase. No rule at the mint.

### 3. Forms on the quick check (A303; seeds 1-200, `air`)

| Form | Log | Random | World defaults | Random Newton iterations | First-interval family r / d |
|---|---|---|---|---|---|
| Base (this code, P-air) | `gl01` | 169 | 185 | 0.53 M | 26 / 6 |
| L1 | `gl04` | 171 | 188 | 0.54 M | 24 / 5 |
| L2 | `gl05` | 171 | 187 | 3.66 M | 24 / 5 |
| L1 + L2 | `gl06` | 173 | 187 | 3.62 M | 22 / 5 |
| Chain (`ca44`, seeds 1-200) | | 191 | 192 | | |

The base's first-interval holds traced (`gl02`, t13's trace re-ported to this code, `src/instr-t22-t13-trace-on-a9f23964.patch`; world defaults / random): L 4 / 17, S 1 / 3, B 1 / 3, C0 0 / 2, C 0 / 1. C0 survives the cap mode (random 72, card C0's world, singular). Under L1 the traced L worlds show the rule acting (`gl08`: in world defaults 71 every junction's first pass is laid out with gas, liquid water and water vapour) and still failing elsewhere (section 5).

### 4. Campaign `gl09` (A304; `air`, L1, 8 workers)

| | Chain `ca44` | P-air `ps01` (before the cap mode) | P-air + L1 `gl09` |
|---|---|---|---|
| Random / world defaults clean | **951 / 962** | 822 / 933 | 860 / 950 |
| First-interval family r / d | 28 / 21 | 155 / 36 | 101 / 24 |
| Newton iterations r / d | 1.89 M / 9.11 M | 6.40 M / 34.62 M | 6.35 M / 32.50 M |
| Newton solves r / d | 0.335 M / 0.963 M | 0.789 M / 3.223 M | 0.714 M / 2.936 M |
| Gate refusals r / d | 30.0 k / 15.3 k | 26.0 k / 49.6 k | 37.4 k / 49.8 k |

- Against `ca44`: random 5 cleared / 96 worse (E2 29, E6 28, C 13, E10 7, E7 6, F 5, E5 4, E8 3, E3 1); world defaults 18 cleared / 30 worse (F 11, E6 8, E7 6, E5 2, E2 1, C 1, E4 1). Against `ps01`: random 47 / 9, world defaults 22 / 5.
- Class counts `ca44` -> `gl09` (random / world defaults): C 16 -> 28 / 4 -> 2; E2 8 -> 34 / 10 -> 8; E3 0 -> 1 / 0; E4 0 / 0 -> 1; E5 1 -> 6 / 0 -> 2; E6 6 -> 35 / 13 -> 11; E7 3 -> 9 / 2 -> 8; E8 2 -> 4 / 2 -> 1; E9 4 -> 2 / 2 -> 1; E10 3 -> 10 / 0; F 6 -> 11 / 5 -> 16.
- New budget-class holds (C and F not in `ca44`) alone, one batch (`gl13`, alone lock, one worker): random 18 of 18 hold again (C 13, F 4, 752 as E7); world defaults 3 of 12 run clean alone (164, 833, 961), 9 hold (F 7, C 908, E7 839).
- The two smallest worse worlds, traced as they are (8 and 9 blocks, `gl15`): random 745 is a two-phase crude generator at 326 K above an air junction through 0.01 m pipes, the junction's crude fugacity rows at 425 down to 1e-7 s (D44's crude; held under `ps01` too); world defaults 1 is an active-set cycle at tick 1410 (not a first interval) at an air junction between a water generator at its height, a nitrogen tank and a void 1 m below.
- P-air stays below the chain: **no retirement** (A304, D51). The chain stays the default and every row of the chain stays.

### 5. What remains of P-air's first-interval holds (A305; `gl14`, every first-interval hold of `gl09` traced)

`t20-groups.js`: L 16 / 68 (world defaults / random), S 5 / 13, C0 0 / 16, B 3 / 3, C 0 / 1. Its L criterion (a liquid source with a gas-start junction's row or a refused temperature) is wider than the layout defect, so the 84 L holds are split by the failed row (`gl14-L-split.txt`):

| Mechanism | Holds | Physics |
|---|---|---|
| The arriving phase admitted; the pass stalls on a flow row | 41 | The start flows of a first step are read on air junctions (the estimate and its caps take 1.145 kg/m3): a water line starts near 0.24 kg/s where it carries several kg/s |
| Crude or methane trials below the data's 293.15 K floor | 21 (random) | The volatile part evaporates into 298 K nitrogen and cools the mixture: a real effect, outside the data's range (D44) |
| Two-phase crude node rows | 8 | D44 |
| Singular continuity | 7 | Flat rows, the C0 kind |
| Other junction rows | 7 | Water-vapour approximation, negative reconstruction |

### 6. Under the chain default (quick check `gl10`, seeds 1-200)

192 random / 192 world defaults against `ca44`'s 191 / 192: random 9 cleared (E8), nothing worse. Paths move in 2 random and 9 world-default worlds, all held before and after except random 9 (world defaults 94 holds as a typed C wait where it held as E2).

### 7. Gate test

`JunctionArrivingPhaseTest` (card L as a fixture graph: the air junction built directly, so it holds air whatever the world's start form, A106): with the rule, 20 slices of 0.1 s commit whole, 18.3 kg of water reach the void (about 9.2 kg/s), the junction ends liquid water at 298.15 K (`gl11`). With the rule disabled in the working tree (`arrivingPhases` returning its seeds), it holds on the first slice: "Substep refinement exhausted: Newton iteration limit at residual 7.9e-4; active-set pass=1", nitrogen trials above 900 K (`gl12`).

### 8. References (one final `gates.sh`, `gl16`, GameTest run id `t22-gl16-e55056d8-run1`; nothing re-recorded)

On the final default (the chain), against t19's `ca46` (the cap mode; the start switch's default runs no changed line):

| Reference | `ca46` | `gl16` |
|---|---|---|
| G1 fluid suites | 530 tests, 0 failures | 533 tests, 0 failures (the batch's `JunctionStartFormTest` x2 and this `JunctionArrivingPhaseTest`) |
| 33 junction lines | `ca48`'s table | identical to `ca46`'s lines (0 diff lines; the 52 diff lines against the pre-D49 reference file are `ca48`'s moved mixed-gas lines, D49) |
| MIXED_GAS_COST | 335 | 335 |
| G2 chain-100 | exact | exact (0 deviation) |
| Printed suite lines | 6085 | 6086: one line more, the new test's `JUNCTION_ARRIVING_PHASE`; none differs |
| G3 adjacent | 45 / 45 | 45 / 45 |
| GameTest outlets | 124258.48752327717 Pa / 175.7622745472754 kg | the same |
| GameTest compressor | 2.9999859075564377 / 157976.31408415755 Pa | the same |

No reference moved; nothing was re-recorded.

### 9. Stop points

| Stop point | Item | Recommendation |
|---|---|---|
| S1 | Take L1 (A300-A303) | Take: the physical layout of a junction's step, bitwise wherever no phase appears, card L clean |
| S2 | Retirement of the chain (A304) | Not made: P-air with L1 is 860 / 950 against 951 / 962 |
| S3 | P-air's remaining first-step gap (A305, A306) | (1) the first step's start flows read on the mixture's columns and caps (F's round 1 in the cold start); (2) the crude data floor under evaporative cooling (D44); (3) C0 (A276) |

### 10. Against the brief

- The brief expected L to be the whole of P-air's gap; measured, L1 removes the layout defect and lifts P-air from 822 / 933 (`ps01`, before the cap mode) to 860 / 950, but the worlds `t20-groups.js` names L are mostly other mechanisms (section 5).
- C0 is not gone under the cap mode (0 / 16).
- The two smallest worse worlds were traced at 8 and 9 blocks without a minimisation run.
- Commit trailers name Claude Opus 5.5 (the writing model), not Fable 5.1. Documents are not copied to the main checkout.

## The empty start: every vessel a bag with a stiff wall; nothing at the mint (2026-10-14)

Branch `claude/empty-start` from `50145147`, thread t24, owner decision D54 (plan `documentation/2026-10-14-empty-start-progressive-volume/EMPTY_START_PLAN.md`, section 8 as recommended). Commits: `cc001df3` the wall closure, `7e8b26fb` its gate test and fixtures, `8e3d1fbf` nothing at the mint, `a5d9c91e` its gate test. Decisions A310-A323. Logs `es01`-`es31` in `tools/phase-ports-probes/hold-classes/logs/`.

### 1. Physics first

A placed tank holds nothing. While its contents are smaller than its capacity it is a slack bag at the ambient (101325 Pa at its top, the liquid's head below); once they would exceed it the wall carries them and the pressure comes from the contents. On the E5 card world (a 1 atm water generator 3 m above a 1 m3 tank): the tank fills at the ambient, about 14 kg/s through its two lines, for 70 s, then stands full of water at 101325 + rho g 3 m = 130.6 kPa with no gas. A tank drained goes slack and empties; it never pulls a vacuum. A pipe's junction holds nothing a placement put there; its buffer is a relaxation state (D46) whose start is a guess the 0.05 s relaxation washes out.

### 2. What the code does now

| Statement | Where | Rows |
|---|---|---|
| The wall law in its stiff limit, `max((V - V_max)/V_max, 2 (P_amb - P)/P_amb) = 0`, replacing the fixed-volume row; taut it is that row to the bit | `PhaseLayout.wallRow`, `wallClosure`; `InventoryEquilibrium.close` | A310 |
| The wall's work `P_amb dV_bag` in the energy row and the closure, booked as the ambient's energy-only boundary transfer at the vessel | `PhaseLayout.balanceRows`, `ConservativeTransport.reconstruct` | A311 |
| A placed tank is empty (the ambient as its state); an empty vessel has no unknowns until something arrives, its ends are dry | `PassiveNetwork.emptyVessel`, `PassiveStepSolver.dry`, `dryEnd`, `arrivingPhases`, `filled` | A312 |
| A vessel drained to nothing within a step stands as a junction holding its stock and ends empty | `PassiveStepSolver.emptyingGraph`, `ConservativeTransport.Emptying` | A313 |
| No charge, its two settings gone, `idleSeed` gone; the tank view shows its fill | `FluidDeviceSpec`, `CreateChemE`, `PhysicalRegistry`, `FluidView`, `FluidDeviceScreen` | A314 |
| The controller's mass reference, the small-headspace tolerance and the rest test read the bag | `PassiveIntervalSolver.stateChange`, `PassiveStepSolver.solve`, `NetworkReduction.atRest` | A315 |
| A minted junction starts from the fixed point's last round (one round); the chain, `START_FORM` and the P-empty rounds are deleted | `PassiveNetwork.mintJunctions`, `PassiveStepSolver.fixedPointStarts` | A318 |
| A dry end stands in the other end's column, starts at rest and closes a run whose drive points out of it | `PassiveStepSolver` (`headDensities`, `staticDrive`, `startPoint`, `closeIllegalStarts`) | A319 |

`k`: infinite (A310). The brief's value, the liquid's bulk modulus over the capacity (2.2e9 Pa/m3 per m3), moves every charged vessel above the ambient by `(P - P_amb)/K` of its volume, which is not roundoff; the stiff limit keeps a charged vessel at its capacity to the bit: chain-100 exact (0 deviation in every field). Fixtures charged at exactly 101325 Pa sit on the kink and can differ by a roundoff tie in their base residual.

### 3. Gate tests (each run alone, both ways)

| Test | With the rule | Rule disabled in the working tree |
|---|---|---|
| `EmptyTankFillsTest` (E5 card, 7 blocks, built as placed) | fills at 14.3 kg/s, full at 70 s; rests at 130627.70407661765 Pa holding 996.04 kg of water, no gas, zero flow (`es01`, `es24`) | the charge restored in `FluidDeviceSpec.initialize`: a gas cushion of 0.63 m3 over 367 kg after 6 min at 120.9 kPa, still taking 0.33 kg/s (`es02`) |
| `EmptyRiserTest` (a 150 kPa spill through a junction, a 7 m riser to an empty tank) | the tank stays empty, the riser carries nothing, the spill 17.7 kg/s; 72 steps accepted, none refused (`es24`) | without the dry-end column the world holds at slice 1 under every start, the chain mint included (`es19`-`es21`); with the start guess disabled (no fixed-point round) it runs the same as with it (`es22`): the first round's rate solve fails in this world, so the guess is the ambient either way |

### 4. Quick checks (seeds 1-200; `ca44` 191 / 192)

| Form | Random | World defaults |
|---|---|---|
| Commit 1, before the emptying form (`es03`, logs not kept) | 182 | 184 |
| Commit 1 (`es14`) | 188 | 187 |
| Commit 2, eight rounds (`es15`) | 175 | 186 |
| Commit 2, one round (`es16`) | 176 | 187 |
| Commit 2 final, with A319 (`es25`) | 176 | 188 |

### 5. Campaign (`es26`, 1000 seeds, 8 workers)

| | `ca44` (chain) | `gl09` (P-air + L1) | `es26` |
|---|---|---|---|
| Clean random / world defaults | 951 / 962 | 860 / 950 | **882 / 943** |
| First-interval family | 28 / 21 | 101 / 24 | 87 / 34 |
| Newton iterations | 1.89 M / 9.11 M | 6.35 M / 32.50 M | 4.01 M / 11.62 M |
| Newton solves | 0.335 M / 0.963 M | 0.714 M / 2.936 M | 0.458 M / 1.416 M |

Against `ca44`: random 11 cleared / 80 worse, world defaults 24 / 43. Classes (random / world defaults): C 24 / 7, E2 22 / 6, E3 1 / 0, E4 0 / 4, E5 11 / 4, E6 24 / 13, E7 13 / 5, E8 3 / 5, E9 3 / 0, E10 8 / 2, F 9 / 11. New C and F holds replayed alone (`es27`): random 23 of 24 and world defaults 17 of 18 hold again.

### 6. The two smallest worse worlds (A321)

- **World defaults 933** (9 blocks). Physics: a 1 atm water generator 3 m above an empty tank fills it full of water at 130.6 kPa, no gas. Code: the junction is minted as the ambient (the fixed point's first rate solve fails: water reaching a junction held at air's composition must leave as air at air's velocity cap); its 11 g of air washes into the filling tank as real nitrogen; the tank ends taut over a 7.8 L pocket at its top port, and the small headspace's stiffness fails the equation gate at 1.8e-8 at every step (520 refusals in one interval).
- **Random 745** (8 blocks). Physics: crude from a 159 kPa generator falls 1 m through two narrow pipes to a junction and a void; the junction stands between them. Code: the junction is minted as the ambient for the same reason; the first step's Newton walks it from air to 2 MPa, the data's bound (a typed wait).

One mechanism: wherever liquid reaches a junction the fixed point's first round fails, and the guess stays air, which then has physical consequences. Options for the owner: (a) the first round from the step's start estimate instead of a frozen rate solve; (b) a buffer that holds nothing until the first step (the deleted P-empty refill), so no guess is booked as material; (c) keep. Recommended: measure (a) with (b) on the quick check.

### 7. References (`es30`, GameTest run id `t24-es30-a5d9c91e-run1`; nothing re-recorded)

| Reference | `gl16` | `es30` |
|---|---|---|
| G1 fluid suites | 533 tests, 0 failures | 535 tests, 4 failures: `InlineFilterLineHoldTest` x2 (the pump's suction junction starts as air and the liquid-only pump never runs), `StepStartFlowsTest` (card E6-S's world: the top junction keeps its minted gas, soft-budget holds), `VesselClosureEquilibriumTest` (a crude tank fed through a junction: singular at its first steps) |
| 33 junction lines | `ca46` | 26 move: 3 liquid-tank lines (a 1 atm water tank drains slack instead of pulling vacuum), 23 mixed-gas lines (the 90 kPa tank goes slack at the ambient; tanks that reach the ambient empty down the gas column into the lower void) |
| MIXED_GAS_COST | 335 | 362 |
| G2 chain-100 | exact | exact (0 deviation) |
| Printed suite lines | 6086 | 6384 (1584 only in `gl16`, 1882 only in `es30`) |
| G3 adjacent | 45 / 45 | 45 / 45 |
| GameTests | outlets 124258.48752327717 Pa / 175.7622745472754 kg; compressor 2.9999859075564377 / 157976.31408415755 Pa | 10 fail, neither print produced: four read an empty tank's state (null), one expects no "EMPTY" status, the outlets test expects the charged tank's three outlets (the empty tank's top port is dry), the compressor's receiving tank passes 900 K, the rest assume a charge |

Rest and steady: `EmptyTankFillsTest` shows a filled tank at rest (zero flow from slice 15) and a filling one not at rest; `RestIslandTest` (restated) a placed tank that fills to its source's pressure and then certifies STEADY.

### 8. Stop points

| Stop point | Item | Recommendation |
|---|---|---|
| S1 | `k` infinite instead of the brief's bulk modulus (A310) | Take: the only form that keeps charged vessels on today's numbers |
| S2 | The emptying form, not in the plan (A313) | Take: a bag can be emptied, and nothing else steps past the instant it empties |
| S3 | The dry-end column and closure (A319) | Take: the riser holds without it under every start |
| S4 | The mint's guess (A321): the first round fails wherever liquid reaches an air junction | Measure (a) the first round from the start estimate with (b) a buffer holding nothing until the first step |
| S5 | Gates: 4 suite failures, 10 GameTests to restate for placed-empty tanks, 26 junction lines and the cost moved | After S4, then the batch's single re-baseline |
| S6 | Retired `HydraulicReferenceQualificationTest` dilute case (A316) | Owner: bring it back at the taut branch with a real-gas reference, or not |

### 9. Against the plan and the brief

- The brief's `k` would move every charged vessel by 1e-5 of its volume (A310); the stiff limit is taken.
- The plan did not cover a bag drained to nothing; it needs the emptying form (A313).
- "Nothing at the mint" is not free of physical consequence: the guess's material leaves the buffer into the network (A321), and in liquid worlds the guess is air.
- Gate test 2's proof rests on the dry-end column, not on the start guess (section 3).
- The gates are not green (section 7). Commit trailers name Claude Opus 5.5 (the writing model). Documents are not copied to the main checkout.

### 10. Follow-up: the junction's start from its suppliers (owner decision D55, 2026-10-14)

Taken by the owner after this section: S1-S3 (A310, A313, A319); S4 decided as D55 ("go with idea 1, but keep the solution elegant"); S5 to be restated after it; S6 stays retired. Commits `340c782a` (code) and `ef987556` (tests); decisions A324-A333; logs `es32`-`es43`.

**Physics first.** At a placement nothing is in a junction; what fills it first is what its connected suppliers push into it, at the pressure its connections' flows make. A generator's water reaching a junction makes it water, never air; a junction no source reaches holds nothing, and an island in which nothing holds stock cannot move anything, whatever its compressor is set to.

| Statement | Where | Rows |
|---|---|---|
| Round 0 of the fixed point: the suppliers' streams at their static drives, mixed, swept hop by hop; a failed round keeps the last; no ambient seed or fallback | `PassiveStepSolver.supplierSeeds`, `supplyFlow`, `sweepNeighbours`, `fixedPointStarts` | A324 |
| The seed at its balance pressure, not the ambient (against the brief) | `PassiveStepSolver.balancePressure`, `seedAt` | A325 |
| No refill round or history-free step: the criterion is not met | - | A326 |
| An island that holds no stock is at rest (the 900 K compressor case); the junction-and-void tree rule deleted | `NetworkReduction.atRest` | A327 |
| The second group cleared; the dry-end closure is decided once per solve already | - | A328 |

**Measurements (old and new).**

| | `ca44` (charged tanks, chain) | `es26` (first pass) | `es40` (D55) |
|---|---|---|---|
| Quick check, seeds 1-200 | 191 / 192 | `es25` 176 / 188 | `es33` 181 / 192 (seed at the ambient); `es38` **187 / 197** |
| Campaign, random / world defaults | 951 / 962 | 882 / 943 | **933 / 970** |
| First-interval family | 28 / 21 | 87 / 34 | 49 / 20 |
| Newton iterations | 1.89 M / 9.11 M | 4.01 M / 11.62 M | 1.74 M / 0.50 M |
| Gates | - | G1 4 failures, GameTests 10 failures | all green (`es43`) |

Against `es26`: 75 / 47 cleared, 24 / 20 worse; against `ca44`: 23 / 29 cleared, 41 / 21 worse. Classes (random / world defaults): C 11 / 5, E2 12 / 2, E3 1 / 0, E5 7 / 12, E6 10 / 7, E7 7 / 3, E8 2 / 0, E9 7 / 0, E10 5 / 0, F 5 / 1. The two smallest new worse worlds (A331): random 699, a nitrogen junction throttled to 273 K that methane reaches below its 293.15 K data floor (D44's family); world defaults 228, three water junctions tied at 1 atm between two 1 atm generators and voids, the Newton stalling at 5.0e-8 (a stiff liquid tie).

**References** (`es43`, nothing re-recorded): G1 536 / 0; chain-100 exact; 33 junction lines as `es30` (26 moved against `gl16`/`ca46`), MIXED_GAS_COST 362 (335); printed lines 6184 (`gl16` 6086); G3 45 / 45; GameTests 33 / 33 with the prints of A332 (outlets 124258.49 -> 124429.54 Pa, 175.76 -> 173.02 kg; compressor ratio 2.99998591 -> 2.99999502, 157976.31 -> 389762.95 Pa).

**Against the brief.** The seed's pressure (A325); the 900 K case is fixed in the rest stage, not the closure (A327); the refused-pump GameTest is restated beside the ten (A329); random 48 was not traced, it no longer holds (A328). Commit trailers name Claude Opus 5.5 (the writing model), not Claude Fable 5.1 as the brief asked. Documents are not copied to the main checkout.

## Nothing moves: the closures decided before the solve (2026-10-14)

Branch `claude/rest-closures` from `05676fd4`, thread t25, owner decision D58 (category A of `REMAINING_HOLDS.md`, "Remaining issues sorted for the owner"). Commits: `969798b2` (A1, dry suction), `083f0b8d` (A3, the gauge where stock stands), `4cbb4d4e` (A4, one-way exits). Decisions A334-A346. Logs `rc01`-`rc24` in `tools/phase-ports-probes/hold-classes/logs/`.

### 1. Physics first, and what the traces found

Every world of the sorted section was traced before a rule was written (the t13 trace re-ported onto `05676fd4`, `src/instr-t25-t13-trace-on-05676fd4.patch`). Several hypotheses of the sorted section did not survive the trace:

| Group | The sorted section's reading | What the trace shows | Where it went |
|---|---|---|---|
| A1 (922, 333) | a pump on a dry suction | the suction tank holds a 2.2e-17 kg residue the junction's buffer left in it; the pump reads it as water and stands on its head limit against a dry end (4.934616) | rule A1 |
| A1 (285) | water-cooled pump suction | the suction pocket's only feed is an empty tank (a dry end) | rule A1 (clean) |
| A1 (696, 896, 889, 563) | the same group | the suction is fed (a 1 atm generator 2 m above); a head-limit trial pulls it to 100 Pa and the water flashes to 273.16 K: cavitation | proposition only (A335) |
| A1 via the valve (750, 600, 654) | a closed relief valve opened by F8 | no valve in any held island; 600 and 654 are a 1 atm generator below 1 atm voids (nothing moves); 750 is a flowing world whose void connection is driven the forbidden way | 600, 654: rule A3; 750: B |
| A1 likely (322) | a pump | a passive run out of a tank holding a 1.7e-47 kg residue (not exactly empty, so not a dry end) | cleared in the campaign; finding (A343) |
| A3 (228, 107) | a tie at the ambient | both flow (228: a pump moves 10 kg/s from two generators into a void; 107: two generators 2 m above a void drain 9.6 kg/s); the Newton converges on chord steps and stops at its 20-iteration limit at 5.0e-8 and 2.7e-8 | B (A337, A338) |
| A3 (354) | a tie | two 1 atm generators below voids and an empty tank: nothing moves; the rest test read drives between junction start guesses | rule A3 |
| A4 (619) | a pocket behind closed devices | a 1 atm generator 3 m below two voids: nothing moves; same mechanism as 354 | rule A3 |
| A4 (529, 358r, 683, 891, 660, 456, 957) | anchorless pockets | flowing gas worlds on their first interval with most lines on their velocity cap: a junction whose every connection states a cap flow has no pressure row (t19's C0, A276) | B (A341) |
| A4 (358 defaults) | a pocket | a pump whose discharge pocket's only exit is its own generator, which only supplies: dead-headed | rule A4 |
| A2 (244) | a relief valve below its setpoint | no valve in the held island; a generator above two voids drains; the stalled row (0.498) is a void connection driven the forbidden way on a first solve | B (A342) |

### 2. The rules

| Rule | Physics | The rule, in the stage that owns it | Absorbs | Code |
|---|---|---|---|---|
| A1 | a pump or compressor with nothing at its suction runs dry: no flow, no head | `InletPhase.held`: what can feed the suction (the first node, or every node reached through junctions by a connection that may carry flow towards it: a generator always, a void, a dry end or a junction never, a vessel by the share of its capacity it holds). Dry below 0.5 % (the throttle's landing share, A4 of the plan), resumes at 1 % (the opening share); refused like a wrong phase (D6), per slice, hysteresis on the committed mode, reason "ERROR: pump inlet dry (from node n)" | the skip of an empty first node in `InletPhase.decide` | `969798b2` |
| A3 | where nothing can move, nothing moves; a junction holds no stock and no gauge (D42), and on a first interval its pressure is only its start guess (D55) | `NetworkReduction.atRest`: drives are read between two nodes that hold stock, directly or along junction paths and loops, never between a junction and its neighbour; a mover set to no flow or refused for the slice is a closed connection (so `decideInlets` runs before the rest test); the rest interval reports a refused mover `INLET_WRONG_PHASE` | the per-connection junction test; "an island with any mover is solved" | `083f0b8d` |
| A4 | a pump whose discharge has no way out dead-heads; a way out its one-way rules forbid is no way out | F8's `relieveSealedPockets`: `sealedWithout` per side asks whether a connection can carry flow the way the mover needs (out of the discharge pocket, into the suction pocket; a device only from its first end to its second); such a mover starts closed at its shutoff | the pass loop's illegal-direction closure leaving a sealed pocket with a mover at its target (singular) | `4cbb4d4e` |

"Empty" for A1 (the brief asked for one meaning): a supply holding below 0.5 % of its capacity, resuming at 1 %, the port thresholds of plan A4. The passive dry end (A319) keeps its exact zero: a tank drains completely by gravity, so a heel for passive runs would be unphysical. The decision is per slice, on the committed state (D6); a pass that changes modes does not re-decide A1 or A3. A4 is decided on each solve's start modes, as F8 is; a pass that closes a connection later does not re-run it (no world needed it once the start is right).

The brief's rule 2 (rest per part after the start-point closures, gate 228) was not implemented: 228 and 107 flow, and no traced world needs a resting part beside a flowing one. Where a solve-level closure leaves nothing moving (358 after A4), the island is still solved and its junctions leave roundoff flows of 4.4e-16 kg/s. The per-part rest is a stop point (section 6).

### 3. Gate tests (each run both ways)

| Test | World | Without the rule | With it |
|---|---|---|---|
| `DrySuctionTest` | world defaults 922 minimized (6 blocks, `rc02`): an empty tank, a pump facing a 1 atm water generator | held at tick 103, residual 4.934616333580435 (`rc03`) | kept up 120 slices, pump `INLET_WRONG_PHASE`, "pump inlet dry"; flows exactly zero after A3 (`rc04`, `rc12`) |
| `RestGaugeTest` | world defaults 619 and 354 minimized (11 blocks each, `rc10`) | held at tick 101 (singular) and tick 118 (5.4e-9) (`rc11`) | both kept up, every flow exactly zero (`rc12`) |
| `SealedDischargeTest` | world defaults 358 minimized (8 blocks, `rc18`): a pump in a ring discharging into its generator | held at tick 159 (singular) (`rc19`) | kept up, pump `CLOSED` at shutoff, flows below 1e-12 (`rc20`) |

Restated under A3 (the rule changed what they state): `RestResolutionTest` (a junction 0.2 Pa above its two 1 atm ends is no drive; a generator 0.2 Pa above its void is), `DeadHeadedLineIslandTest` (a line the rest stage takes reports PASSIVE on its two-way connections, CLOSED on the generator's, flows exactly zero).

### 4. Quick checks (seeds 1-200, random / world defaults; `es38` 187 / 197)

| After | Clean | Worse | Cleared | Suites |
|---|---|---|---|---|
| A1 (`rc05`) | 189 / 199 | none | random 80, 94; defaults 6, 29 | 538 / 0 |
| A3 (`rc13`) | 189 / 198 | random 108 | random 80, 94, 151; defaults 6 | 540 / 2 (restated, then 0) |
| A4 (`rc21`) | 189 / 198 | random 108 | as A3 | 541 / 0 |

World defaults 29 is the budget world (F; clean alone on `es41`). Junction lines and chain-100 unchanged at every check.

### 5. Campaign (`rc22`, 1000 seeds, 8 workers, on `4cbb4d4e`)

| | `es40` | `rc22` |
|---|---|---|
| Clean random / world defaults | 933 / 970 | **940 / 987** |
| First-interval family | 49 / 20 | 42 / 8 |
| Newton iterations | 1.744 M / 0.498 M | 1.722 M / 0.285 M |
| Newton solves | 0.331 M / 0.084 M | 0.328 M / 0.056 M |
| Rest intervals | 51509 / 57436 | 51895 / 58123 |

Cleared: random 80 94 151 342 397 399 728 855; world defaults 6 285 322 333 354 358 485 600 619 654 683 752 771 794 826 922 978. Worse: random 108 only (traced, A336). Classes (random / world defaults): C 10 / 4, E2 10 / 0, E3 1 / 0, E5 8 / 0, E6 7 / 6, E7 7 / 2, E8 3 / 0, E9 6 / 0, E10 3 / 0, F 5 / 1. The new budget-class hold (random 446, F in the campaign) replayed alone (`rc23`) is E10 as on `es40`.

### 6. References (`rc24`, GameTest run id `t25-rc24-4cbb4d4e-run1`, run alone; nothing re-recorded)

| Reference | `es43` | `rc24` |
|---|---|---|
| G1 fluid suites | 536 / 0 | 541 / 0 (five new gate tests) |
| G2 chain-100 | exact | exact |
| 33 junction lines | 26 moved against `gl16` | identical to `es43` (no further movement) |
| MIXED_GAS_COST | 362 | 362 |
| Printed suite lines | 6184 | 6186: 7 only in `es43`, 9 only in `rc24` (below) |
| G3 adjacent | 45 / 45 | 45 / 45 |
| GameTests | 33 / 33 | 33 / 33, prints unchanged: outlets 124429.54062741926 Pa / 173.01706865204667 kg; compressor 2.9999950237654676 / 389762.95410970156 Pa |

Moved printed lines: six `F1_SLICE` lines of the refused-pump test (its island now rests: 3 accepted steps per slice become 1, one slice's thermo-domain rejection is gone, the tank pressure moves in its last digit, 104722.0017777813 -> 104722.00177778126 Pa); `INLET_PHASE_RUN fixture5` (accepted 25 -> 15, the tank's water 6.415798335268108e-5 -> 6.415798335268925e-5 kg, 1.3e-13 relative); new: `DRY_SUCTION`, `SEALED_DISCHARGE`. The `TankCharge` GameTests of A329 and the restated suite tests stay green.

### 7. Stop points

| Stop point | Item | Recommendation |
|---|---|---|
| S1 | Take A1, A3, A4 (campaign 940 / 987, gates green) | Take |
| S2 | "Empty" for a pump's suction is the port band (0.5 % refuse, 1 % resume); passive dry ends keep exact zero | Take; the bag's resolution (1e-12 of capacity) as the one meaning of holding nothing for A312/A319 is an option against ulp residues (A343) |
| S3 | Rest per part after the start-point closures (the brief's rule 2) not implemented: its gate world flows | Leave until a world needs it; it would make a dead-headed island (A4) exact zero and stop ulp leaks into empty tanks behind closed devices |
| S4 | Cavitation at a fed suction (A335) | Owner: a suction limit mode `P_suction = P_vapour(T)` mirroring the head limit |
| S5 | The B findings: chord budget (A337, A338), first-solve void closure (A342), capped gas junctions (A341) | One B thread, traces already in `rc08`, `rc09`, `rc17` |

### 8. Against the brief

- 228 is not a tie at rest; the gate of rule 2 became 619 and 354, and the rule went into the island-level rest test (A336), not per part.
- 750, 600, 654 and 244 hold no valve; 619 and the random E2 worlds are not pockets behind closed devices; 322 is not a pump. Each went where its trace put it (section 1).
- Commit trailers name Claude Opus 5.5 (the writing model), not Claude Fable 5.1. Documents are not copied to the main checkout.

## Active-set cycles and the flush family (2026-10-14)

Branch `claude/cycles-flush` from `05676fd4` (code: the merge `51e27cd4`), thread t26, owner decision D58 items B1 and B3. Commits: `3334aa89` the emptying form's start and its decision (gate test `EmptyingVesselTest`), `6d850c94` the cap entry of a failed iterate, `2739caa3` the partition booking (gate test `PartitionDrawTest`), `01350240` the emptying decision per active set. Decisions A350-A364. Logs `cf01`-`cf31` in `tools/phase-ports-probes/hold-classes/logs/`; the trace patch `src/instr-t26-pass-trace.patch`, the measured form `src/scratch-t26-held-caps.patch`, scripts `t26-lane.sh`, `t26-trace.sh`, `t26-cycles.js`, `t26-minimize.sh` (README).

### 1. Physics first

**B1.** Within one step a device, a line and a vessel each settle in one mode: a line is choked or it is not, a bag empties within the step or it does not. A repeated active-set key means the pass loop re-decided a mode from evidence that changes from pass to pass. The question is which evidence.

**B3.** A vessel can only give what it holds plus what reaches it over the step, and a phase carries each component in proportion to what the vessel holds of it: a methane tank holding nitrogen at 4.8e-24 of its amount vents nitrogen at about that share times the gas phase's enrichment, never twelve times its holding.

### 2. What cycles in E7 (A351, the held step at its smallest dt)

| Mechanism | Seeds | Pair that alternates | Owner |
|---|---|---|---|
| The emptying form (A313) | random 257, 678 | `runsDry` from a failed iterate enters it; `keptEmptying` at a converged point above the ambient leaves it | this thread: A352, A353 |
| Failed-iterate cap corrections (A274) | random 337, 824, 200 | a capped line released by one failed iterate, entered by another (200: at one and the same point) | 200: A354; 337, 824: the start (A356) |
| A pump drawing its suction to the 100 Pa floor | random 922, 116; world defaults 573, 826, 952 | the suction junction's phase (water about 118 kPa / vapour 100 Pa) and its cap | t25 (A1): reported |

None alternates in `nextMode`. World defaults 952's held island is a water generator, a junction, and a pump discharging into a second generator, which never receives.

### 3. The rules

| Rule | Physics | Stage | Absorbs | Proof |
|---|---|---|---|---|
| The emptying pass starts from the step's carried flows (A352) | the same connections in the same step | `Equations` takes the solve's connection identities | the static estimate the emptying form fell back to because the bulk port changed the identity | `EmptyingVesselTest` (257, 7 blocks): 120 slices; without, held at tick 154 |
| Emptying is decided once per active set (A353) | a bag that runs dry within the step does so on one network | `keptEmptying` with the confirmation, cleared at a cap, device or closure change | the release after a phase-layout correction (678) | 678 E7 -> C (crude floor); 853 (`cf24` worse) clean after the reset |
| A cap is entered only by a drive that pushes the flow (A354) | a line is choked only in the direction its drive pushes | `Equations.correctCaps` | the disagreement with `capTransition` at one point | 200 E7 -> E2 (A276) |
| A phase stream draws a component in proportion to what its vessel holds (A358) | the partition, against the vessel's own end fraction | `ConservativeTransport.reconstruct0`, only for a component that came out negative | 322, 579, 431 (none held), 720 (above the flush fraction) | `PartitionDrawTest` (322 and 720, 10 blocks each): 120 slices; without, held at ticks 513 and 145 |

The partition's energy: the stream keeps its specific enthalpy on the same moved mass at both ends, so the energy booking is unchanged and the ledger exact over vessels and boundaries (D46); its amounts are booked by donor and receiver alike. The brief's form (bound by the holding, the excess booked as the flush) is not taken: what a bulk inflow brings within the step is unknown before the solve, and in 322 there is nothing to flush (A358). Measured and dropped: a cap a converged point held is not released by a failed iterate (A355, quick check 187 / 197 against 187 / 198, world-default Newton iterations 2.6 times).

### 4. Measurements

| Run | Random | World defaults | Notes |
|---|---|---|---|
| `es38` quick check (base) | 187 | 197 | |
| `cf13` A352-A354 | 187 | 198 | world defaults 29 (F) clear |
| `cf12` with A355 | 187 | 197 | Newton iterations 0.12 M against 0.05 M |
| `cf23` with A358 | 187 | 198 | suites 539 / 0, chain-100 exact |
| `es40` campaign | 933 | 970 | |
| `cf29` campaign (final) | **937** | **971** | cleared random 257 322 419 720, world defaults 29; worse none |

Classes `es40` -> `cf29`, random: C 11->12, E2 12->13, E6 10->11, E7 7->4, E8 2->3, E9 7->3, F 5->4; world defaults: F 1->0. First-interval family 45 / 20 (`es40` 49 / 20, none new). Newton iterations 1.747 M / 0.238 M (`es40` 1.744 M / 0.498 M). New C and F holds alone (`cf30`): 678 and 579 hold again. E7 left: 337 824 116 922 / 573 826 952; E9 left: 121, 94 (a sealed ring of junctions circulating above its cap, category A4, A359), 514 (an inventory with no moles and nonzero energy, not traced).

**References** (`cf31`, nothing re-recorded): G1 539 / 0; chain-100 exact; the 33 junction lines identical to `es43`; MIXED_GAS_COST 362; G3 45 / 45; GameTests 33 / 33. Moved: 264 of 6184 printed lines (`PhasePortPriorityTest`'s overflow runs; with the partition off it prints `es43`'s), the outlets GameTest (tankP 124429.54062741926 -> 124429.41577643281 Pa, tankMass 173.01706865204667 -> 173.01910992353126 kg); the compressor print unchanged (A363).

### 5. Stop points for the owner

1. B1 is resolved for the emptying form and the cap entry; 337 and 824 need a start: the right active set fails its Newton from the mint's junction pressures (A356, `NetworkReduction`, t25). Proposition: after A3/A4 land, trace 337's mint balance (J1 at the generator's pressure two hops away).
2. The pump group (922 116 / 573 826 952) is A1's: a pump that draws its suction to the floor; 952's pump discharges into a generator.
3. 121 and 94 are A4's sealed pockets in ring form; the dead-end rule (`sealedWithout`) treats a ring line as sealed without itself.

**Against the brief.** B1's pair is not in `nextMode`; the gate test is built from 257, not 337 (337 is not cured by a pass-loop rule, A356). B3's mechanism is the D11 frozen stream, not the D16 flush in three of four worlds, and the fix is the partition, not a flush booking (A358). Commit trailers name Claude Opus 5.5, the writing model, not Claude Fable 5.1. Documents are not copied to the main checkout.

## The turbulent walk on gas lines (2026-10-14)

Branch `claude/turbulent-walk` from `d940d342` (t25 merged, D59), merged forward to `d0ec5ff5` (t26 merged, D60, the `mh01` baseline). Thread t27, owner decision D59 (6), item B2 of `REMAINING_HOLDS.md`. Decisions A370-A378 in `DECISION_LOG.md`; logs `tw01`-`tw19` in `tools/phase-ports-probes/hold-classes/logs/`. Code `694b0144` with its gate test. Not merged, not pushed.

### 1. Physics first

A gas line between two pressures carries one flow; any start reaches it. A dead-end pocket of pipes hanging off a gas main fills to the main's pressure plus its gas column and then nothing moves in it. Random 498 in numbers: generators at 480 and 218 kPa (nitrogen) carry 0.037 kg/s through a junction at 396.7 kPa, the downstream 1 cm line choked; three junctions below, in two loops, are a dead end: real-life flows zero, pressures 396.7 kPa plus 46 Pa per metre of gas (4.71 kg/m3).

### 2. What the code did (A370, `tw02`)

The stalled row is a pocket line, not the main. The step's first solve starts the pocket's lines from the static estimate on their old pressures (118 to 124 kPa, against 397 kPa), so they start turbulent (e1 2.0e-3 kg/s, Re 15 000) and must reach a laminar root (e1 1.46e-5 kg/s, Re 109). The slope never vanishes: the law's laminar branch floors it, and the Jacobian states e1's laminar slope exactly (22 263 Pa per kg/s). Fresh Newton steps halve the turbulent flows; the chord steps between them keep the slope of a flow up to twice as large. The last fresh Jacobian was built in the law's laminar-turbulent blend (Re 2365, slope 4.86e4 against 2.23e4): each of the five chord steps after it closed 46 % of e1's gap, the residual halved each time (3.15e-5 to 9.5e-7, contraction 0.4965), and the refresh rule (refresh above 0.8) read that as progress until the 20th iteration, at every step size. The root itself carries a phantom circulation of 4.3e-5 kg/s around the pocket's loop, driven by 0.62 Pa between two risers' per-pass column densities (frozen at the seeds' 1.413 and 1.476 kg/m3 while the pocket stands at 4.71 kg/m3): a finding, not fixed (stop point S3).

### 3. The rule (A371, `694b0144`)

A chord step reuses the Jacobian only while every connection's loss slope dP/dq at the iterate is within a factor two of the slope at the flow the Jacobian was built at (same donor state). A line's resistance follows its flow; a chord step on a row whose stated slope is k times its own closes 1/k of the gap, so at k = 2 it does no better than the fresh step's halving. The equations own the rows and say when their linearisation is stale (`SparseNewton.Equations.linearisationHolds`, implemented in `PassiveStepSolver.Equations`); the Newton keeps the point it differenced at. It is not a regularisation and not a change of the law: rows, tolerances, the budget and every root are unchanged. What changes is which Jacobian a step uses, so the iterate sequence and the converged point within the tolerance move wherever a slope moved by more than two (section 7).

### 4. Forms (A372; seeds 1-200 on `d940d342`, `tw01` 189 / 199)

| form | random | defaults | worse | suites | log |
|---|---|---|---|---|---|
| base | 189 | 199 | - | 541 / 0 | `tw01` |
| (ii) laminar floor on the flow entry | 188 | 198 | r59, d29 | 1 failure | `tw04` |
| chord refresh ratio 0.25 | 191 | 199 | d101 | 2 failures | `tw05` |
| slope rule (A371) | **191** | **199** | r59, d101 | 541 / 0 | `tw06` |

Option (ii) is inert on the walk: on 498 it bit 75 times, all at roundoff on laminar lines or inside one difference step across zero flow, and 498 held at the same tick to the 14th digit.

### 5. Gate test (A373)

`TurbulentWalkTest`: random 498 minimized with the rule off to 9 blocks (`tw08`), a 480 kPa nitrogen generator feeding a dead-end ladder of two pipe loops, the same walk (`tw09`). With the rule: 120 slices (`tw12`, `tw13`). Without (`-Dt27nowalk`, a working-tree switch removed before the commit): held at tick 101, "Newton iteration limit at residual 1.5497028030015203E-5" (`tw11`).

### 6. Campaign `tw15` (1000 seeds, 4 workers) against `mh01`

| | `mh01` | `tw15` |
|---|---|---|
| random / world defaults clean | 943 / 987 | **951 / 994** |
| cleared | - | r 108 121 248 274 337 419 629 681 788 883; d 29 107 244 524 547 750 889 896 952 |
| worse | - | r 59 (E8), 708 (E9); d 101 (E7), 826 (E5) |
| first-interval family | 38 / 8 | 31 / 2 (new r708) |
| Newton iterations r / d | 1.700 M / 0.462 M | 0.871 M / 0.144 M |
| Jacobian builds r / d | 0.757 M / 0.145 M | 0.369 M / 0.053 M |
| residual evaluations r / d | 8.99 M / 1.73 M | 4.15 M / 0.40 M |

Classes random: C 11->12, E2 11->12, E5 8->4, E6 8->3, E7 4->3, E8 3->4, F 5->4; world defaults C 4->1, E2 0->1, E5 0->1, E6 6->0, E7 2->3, F 1->0. Moved: r 446 E10->C, 498 E6->E2, 600 E6->E7, 678 C->E7, 707 E5->E10, 824 E7->F, 922 E7->C; d 228 E6->E2, 696 C->E7. The chord-budget worlds (A337, A338: d107, 524, 547) are cleared. New C and F alone (`tw16`): 446, 922 (C), 824 (F, soft budget) hold again. Worse worlds minimized (`tw17`) and traced both ways (`tw18`): r59 (6 blocks, crude tank, equation gate 3.05e-8) and d101 (11 blocks, a pump behind a water generator) hold at the same tick without the rule: D44's crude two-phase tanks and S4's cavitation, reached by a new path.

### 7. References (`tw19`, GameTest run id `t27-tw19-694b0144-run1`, alone; old = `mh02`, GameTests old = `cf31`; nothing re-recorded)

| reference | old | new |
|---|---|---|
| G1 suites | 544 / 0 | 545 / 0 (+ the gate test) |
| chain-100 (G2) | exact | 2.695e-10 relative in moles, 2.572e-9 K (red) |
| 33 junction lines | `mh02` (= `rc24`, `es43`) | 26 moved: ledger worst values at 1e-16, junction shares at 1e-10 relative, Newton solves fewer (LIQUID_JUNCTION tanks 14 -> 4, 6 -> 4) |
| MIXED_GAS_COST newtonSolves | 362 | 344 |
| printed suite lines | 6186 | 6186, 3993 moved (overflow-0.1 finalPressure 195782.93115342408 -> 195782.9311540164 Pa) |
| G3 adjacent | 45 / 45 | 45 / 45 |
| GameTest outlets | 124429.41577643281 Pa / 173.01910992353126 kg | 124429.5406265205 Pa / 173.0170686667389 kg (within 1e-12 of the pre-t26 values) |
| GameTest compressor | ratio 2.9999950237654676, discharge 389762.95410970156 Pa | 2.999995023765548, 389762.95415985 Pa; 33 / 33 |

### 8. What the rule does not cure (A377, `tw14`)

248, 629, 883 clean. 886 and 498's later hold (tick 444): capped gas junctions (A276, A341), a junction whose every connection is on its cap has no pressure row (S5). 430: the small-headspace tolerance (A315), the line search cutting every step to 1/16 on a water junction's fugacity rows for a 1e-22 crude trace at a 5.5e-14 s step, residual 1.2e-9 against 1e-10. 600: an active-set cycle (B1's family).

### 9. Stop points

| # | item | recommendation |
|---|---|---|
| S1 | Take the branch: the slope rule in place of option (ii), which measured inert | take; it also clears S5's chord-budget worlds (A337, A338), so the next B thread drops that item |
| S2 | Every converged-point reference moved within the tolerance (chain-100, 26 junction lines, 3993 printed lines, both GameTest prints, MIXED_GAS_COST 362 -> 344) | re-record once at the batch's re-baseline (C1), not here |
| S3 | The per-pass column density is the seed's: a pocket compressed within a step circulates on the difference between two risers' frozen columns (498: 0.62 Pa, 4.3e-5 kg/s) | the column read at the iterate's density once the pass's state is known, or the pass re-entered when a column density moves by more than a set share; measure in the B thread |
| S4 | 430 (A315's tight tolerance with a trace phase) and the capped junctions (886, 498 later) | with S5 in the next B thread |

### 10. Against the brief

The brief's mechanism (the turbulent slope vanishing near zero flow) is not what the code does: the law has a laminar branch, and the Jacobian states it. Option (ii) was implemented as a scratch form, measured inert (and worse at roundoff), and not committed; the committed rule is the chord-refresh form the trace named (one of the brief's three fallback places), measured as the third form. Option (ii) as worded would also not have left converged points bitwise: a different Jacobian moves every iterate after it. 498 still holds, later (E2, capped junctions). The forms were compared on `d940d342`, the rule's quick check, campaign and gates on the merged head `d0ec5ff5`. Commit trailers name Claude Opus 5.5, the writing model, not Claude Fable 5.1. Documents are not copied to the main checkout.

## The B items after the merge: cavitation, the chord budget, the closures and the capped junctions (2026-10-14)

Branch `claude/chord-closures-cavitation` from `d0ec5ff5`, thread t28, owner decisions D59 (2) (4) (5) and D60; stopped once and resumed under D61 (`t28-update-after-t27.md`), which dropped the chord budget (t27's A371 took it) and added the column densities (t27's A377). The batch head (`96eeb42f`, with A371) is merged in at `84787c80`. Decisions A390-A403. Logs `cc01`-`cc56` in `tools/phase-ports-probes/hold-classes/logs/`; scripts `t28-lane.sh`, `t28-minimize.sh`, `t28-refs.sh`, `t28-commit.sh`, `t28-void-out.js`; the pass trace re-homed as `src/instr-t28-pass-trace-on-84787c80.patch` (never committed).

### 1. Physics first, item by item

| Item | Real life | What the code did | Rule (stage) | Commit |
|---|---|---|---|---|
| 1 Cavitation (A335) | A pump cannot pull its suction below the water's vapour pressure; it delivers what the suction line gives there | A head-limit or target trial pulled the suction junction to the data's 100 Pa floor; the water flashed to 273.16 K or the phase flipped each pass | `PUMP_SUCTION_LIMIT`: row `P_suction = P_sat(T)` in place of the target row, head free; entered from a converged point or a failed iterate (pass loop, `nextMode`, `cavitates`) | `16b29d01`, `6cfd8df1` |
| 1 Own end | A pump discharging straight into a generator has no legal flow | A4 looked at junction pockets only; the pump stood on its head limit pushing into the generator (826) | the mover's own end is the pocket of one node (F8's `relieveSealedPockets`) | `53530978` |
| 3 Void line (A342) | A void only receives; water above it falls in | Every step started the line on the void's air column, on its cap out of the void, then closed it: the water never fell (244) | an end that may not fill a connection has no column (`donates`, every static column); absorbs A319's dry-end case | `62ab2587` |
| 4 Capped junctions (A341) | A junction holds nothing; its pressure moves until a line unchokes | A group whose boundary caps all enter it (or all leave it, nothing entering) had no pressure row: singular (529, 498, 228) | the start-decided cap with the least excess drive is on its law (`Equations.anchorJunctions`) | `ee6802a1`, `bd8fc1cd` |
| 6 Holding nothing (A343) | A residue of 1e-71 kg is nothing | Not exactly zero, so stock: a 2 m water column of it stated towards an empty tank (322) | at or below the bag's resolution, 1e-12 of capacity (`holdsNothing`); the residue stays booked | `d4a2a69b` |
| 9 Column density (A377) | A riser's column weighs the fluid in it now | The column read the seed's density for the whole pass; a dead-end loop circulated on the mismatch (498) | the pass keeps the column's end, the row reads its density on the trial (`columnEnds`) | `48cd353f` |
| 2 Chord budget | - | - | dropped (D61: t27's A371) | - |
| 5, 7, 8 | - | Random 121, 94, 337, 824, 514 run clean on this code | findings, no rule (A396, A397) | - |

The brief's form for item 3 (the forbidden-direction closure on first solves) was measured and not taken (A398: worse random 39 and 107, a suite failure): the closure was not the stage; the column was.

### 2. Gates (each run both ways)

| Test | World | Without | With |
|---|---|---|---|
| `PumpCavitationTest` (2) | defaults 696 minimized (11 blocks); a starved suction | held at tick 1570 (273.1599 K) | kept up; the steady pump at 8.009 kg/s, suction at 3173.94 Pa |
| `SealedDischargeTest.aPumpDischargingStraightIntoAGeneratorDeadHeads` | defaults 826 minimized (13 blocks) | held at tick 153 (9.96) | kept up |
| `VoidColumnTest` | defaults 244 as placed (28 blocks) | the upper void receives 0.0 kg/s | 8.44 kg/s |
| `CappedJunctionTest` | random 529 minimized (7 blocks) | singular at tick 164 | kept up |
| `HoldsNothingTest`, `ResidueTankTest` | readings at 1e-12 of capacity; defaults 322 minimized (13 blocks) | the reading fails; held at tick 501 (4.2106) | pass; kept up |

Item 9 has no hold gate: no world holds without it. Measured instead on t27's ladder (the first rate seed's circulation 1.8e-5 -> 2.4e-7 kg/s; the remainder is one end's density per riser) and on the quick check.

### 3. Quick checks (seeds 1-200; `tw15`'s seeds 190 / 199, `tw07` 191 / 199)

| After | Random | Defaults | Worse | References against `tw19` |
|---|---|---|---|---|
| A390 on `d0ec5ff5` (`cc10`) | 189 | 198 | none (= `mh01`) | = `mh02` |
| merge + A390 + A391 (`cc19`) | 192 | 200 | none | identical |
| A392 (`cc23`) | 192 | 200 | none | identical |
| A392 + first-solve closure (`cc24`) | 190 | 199 | 39, 107 | identical; not taken |
| A393 (`cc36`) | 193 | 200 | none | identical |
| A394 (`cc42`) | 193 | 200 | none | identical |
| A395 (`cc44`) | 194 | 200 | none | 17 junction lines, COST 349 |

### 4. Campaign (`cc54`, `bd8fc1cd`, 8 workers)

| | `mh01` | `tw15` | `cc54` |
|---|---|---|---|
| Clean random / world defaults | 943 / 987 | 951 / 994 | **966 / 1000** |
| First-interval family | 38 / 8 | 31 / 2 | 17 / 0 |
| Newton iterations (M) | 1.700 / 0.462 | 0.871 / 0.144 | 1.108 / 0.111 |
| Newton solves (M) | 0.328 / 0.077 | 0.243 / 0.043 | 0.284 / 0.040 |

Cleared against `tw15`: random 59 116 123 188 298 358 456 498 514 529 660 683 824 842 891 922; world defaults 101 228 563 573 696 826. Worse: random 803, a filling transient (a 345 kPa water generator filling two tanks at up to 69 kg/s runs out of interval attempts), A336's family by a new path. Classes random: C 10, E10 6 (4 200 333 699 707 957), E2 1 (372, D44), E3 1, E5 2, E6 3 (430 431 886), E7 2 (600 698), E8 4 (26 246 521 803), E9 2 (446 708), F 3. New C / F alone: 853, 889 hold again. The random iteration count rises with the cleared worlds running all their slices (per solve 3.59 -> 3.90).

### 5. References (`cc56`, alone; nothing re-recorded)

G1 552 / 0, G3 45 / 45, GameTests 33 / 33; chain-100 equal to `tw19`. Moved against `tw19` (A402): 17 of 33 junction lines at roundoff, MIXED_GAS_COST 344 -> 349, 711 printed lines at 1e-9 relative in elevated and pumped worlds, the outlets GameTest (tankP 124429.5406265205 -> 124429.41583189326 Pa, tankMass 173.0170686667389 -> 173.01911028519402 kg); the compressor print unchanged.

### 6. Left, and stop points

| # | Item | Recommendation |
|---|---|---|
| S1 | Take the branch (966 / 1000, one worse world, gates green) | Take; the C1 re-baseline absorbs A402 with A376 |
| S2 | A395 has no hold gate and moves riser references | Take with S1 (quick check better, Newton iterations down) |
| S3 | 886: two caps of one gas in series at the cap's edge (E6-C, t19) | Leave until a form for series caps |
| S4 | 803 filling transient, 708 phase-port overdraw, 430 small-headspace tolerance, 600/698 cycles | One more B thread if wanted; none is a rule of this thread's items |
| S5 | Cavitation reads free water only: a hydrocarbon liquid has no vapour pressure in the model | Owner: accept, or a bubble point from the thermo work |

### 7. Against the brief

Item 2 was dropped under D61; item 3's rule is in the column stage, not the start closures (the closure was measured worse); item 9 was added by D61 and has no hold gate; item 5, 7 and 8 worlds hold no more on this code, so no rule. The own-end rule (A391) was added to item 1's stage because 826 needed it. `PumpRiseScalingTest` was briefly restated for the dropped chord rule and reverted. Commit trailers name Claude Opus 5.5, the writing model, not Claude Fable 5.1. Documents are not copied to the main checkout.

## The last six of the solver's own (2026-10-14)

Branch `claude/last-six` from `2226df88`, thread t29, owner decision D62 (4): random 886, 803, 708, 446, 430, 431 on `cc54`. Decisions A410-A422. Logs `fx01`-`fx37` in `tools/phase-ports-probes/hold-classes/logs/`; scripts `t29-lane.sh`, `t29-minimize.sh`, `t29-code.sh`, `t29-refs.sh`; the trace re-homed as `src/instr-t29-trace-on-cb26806e.patch` (t25's failing-rows trace with the column end of A395, an overdraw print, a gate-refusal print, a stale-against-fresh Jacobian comparison; never committed); the measured forms of items 3 and 4 as `src/scratch-t29-forms-jfrac-carried-with-trace-on-93ad4360.patch`.

### 1. Physics first, item by item

| Item | Real life | What the code did | Outcome |
|---|---|---|---|
| 708 (E9) | An empty bag at the ambient on a loop of two lines to one junction carries nothing; it stays empty | The pass laid the tank out from a roundoff arrival, converged with 1.6e-22 kg/s in by one line and out by the other, and the booking, starting from nothing, ended it at exactly zero: "Candidate overdraws a reservoir" at every step size (A410) | Rule A411: an outflow from a dry end within the forbidden-direction tolerance is no flow at every booked point (`outOfDryEnd`, `9522d63e`) |
| 708, later | A residue of 1e-33 kg is nothing (D59 (2)) | Once A411 let it run, a residue tank, and a tank a pass reached with 2e-29 kg, were laid out with two phases; their fugacity rows stalled the line search (A415) | Rule A415: a vessel at or below the bag's resolution is laid out dry and a roundoff arrival leaves it dry (`holdsNothingVessel`, `cb26806e`) |
| 886 (E6) | A filter between two 10 mm runs passes the flow of the choked run after it; the filter is on its law 4.7e-4 below its own cap, 105 Pa across it | The pass loop reached exactly that active set; its Newton then ran chord steps on a Jacobian built with the filter's junctions at 143 kPa while they stood at 222 kPa: each overshot the filter row by 0.55, out of iterations at 1.9e-3 (A413) | Rule A414: a chord step must still halve every row's gap, `|1 - t/s| <= 1/2`, for the loss slopes and the pressure slopes (`chordHolds`, `93ad4360`); not a cap rule |
| 446 (E9) | - | Not an overdraw: the crude-plus-water flash fails at a crude's bubble point with 5e-15 mol of water (A412) | Thermodynamics model (D44); E3 on this code |
| 803 (E8) | A water junction carrying nitrogen bubbles passes the bubbly mixture | Not the taut corner: 509 of 518 rejections are the equation gate (2.5e-8 after the polish); the junction's fraction rows are absolute, so a gas at 9e-4 of the mass and 58 % of the volume is resolved to 1e-6 of itself, and a trace an upstream junction carries as omitted (D41) is booked into a junction that resolves it (A416) | Finding; proposition S2 |
| 430 (E6) | A crude trace of 6e-23 in a water junction is carried with the water | Both of its phases are traces, so the fold (A84) keeps two; 13 fugacity rows for 1e-21 mol flip by 4.6e-9 per step and block the line search while a small headspace waits at 8e-9 (A417) | Finding; with D41 extended the rows go, and 430 meets 803's gate |
| 431 (E6) | The first grams of 383 K crude in an empty tank flash a little vapour | A real vapour (8e-6 of the moles) in a slack bag; the fugacity rows close 0.36 per chord step and run out at 7.7e-7; with a fresh Jacobian at every iteration the world runs out of budget instead (A418) | Finding; not 430's mechanism |

### 2. Gates (each run both ways)

| Test | World | Without | With |
|---|---|---|---|
| `DryEndOutflowTest` | 708 minimized (8 blocks) | held at tick 361, overdraw | kept up |
| `ChordPressureSlopeTest` | 886 minimized (18 blocks) | held at tick 240, 1.9e-3 | kept up |
| `HoldingNothingLayoutTest` | 708's later hold minimized (17 blocks) | held at tick 333, line search at 8e-4 | kept up |

### 3. Quick checks (seeds 1-200; `cc54`'s seeds 194 / 200)

| After | Random | Defaults | Worse | References against `cc56` |
|---|---|---|---|---|
| A411 (`fx07`) | 194 | 200 | none | identical |
| A414 form a, pressure slopes only (`fx13`) | 193 | 200 | 59 | 2 junction lines, 293 printed lines |
| A414 form b (`fx14`), as committed (`fx19`) | 194 | 200 | none | 18 junction lines, MIXED_GAS_COST 349 -> 347, 2434 printed lines |
| A415 (`fx34`) | 194 | 200 | none | as `fx19`, 2435 printed lines |

### 4. Campaign (`fx35`, `cb26806e`, 8 workers)

| | `cc54` | `fx35` |
|---|---|---|
| Clean random / world defaults | 966 / 1000 | **969 / 1000** |
| First-interval family | 17 / 0 | 14 / 0 (707, 708, 886 left; none new) |
| Newton iterations (M) | 1.108 / 0.111 | 1.076 / 0.110 |
| Newton solves (M) | 0.284 / 0.040 | 0.288 / 0.040 |
| Jacobian builds (M) | 0.470 / 0.036 | 0.460 / 0.037 |

Cleared: random 707, 708, 886. Worse: none. Classes random `cc54` -> `fx35`: C 10 -> 11 (957 from E10), E10 6 -> 4, E2 1, E3 1 -> 2 (446 from E9), E4 0 -> 1 (521 from E8), E5 2 -> 3 (698 from E7), E6 3 -> 2, E7 2 -> 1, E8 4 -> 3, E9 2 -> 0, F 3. New C hold alone (`fx36`): 957 holds again (methane at 288 K, below the data). No new F hold.

### 5. References (`fx37`, alone; nothing re-recorded)

GameTest run id `t29-fx37-cb26806e-run1`. G1 555 / 0 (552 + 3 gate tests); G3 45 / 45; GameTests 33 / 33; chain-100 equal to `cc56` (2.695e-10 relative against its record). Moved against `cc56` (A421), by A414 except one printed line by A415 (A411 moves nothing): 18 of 33 junction lines (worst moles and energy near 1e-15, junction shares at 1e-14, one Newton solve count 26 -> 24); MIXED_GAS_COST 349 -> 347; 2435 of 6192 printed lines at the Newton tolerance (PHASE_PORT_PRIORITY_SLICE 3532 and FED_PUMP 800 of the 4870 differing sides; a drain slice's end pressure 101396.15799758895 -> 101396.15790008887 Pa, the FED_PUMP slice 0 tank 129956.18471720003 -> 129956.18472163701 Pa); the outlets and compressor GameTest prints equal to `cc56` in every value (read one to three ticks later on this server run).

### 6. Left, and stop points

| # | Item | Recommendation |
|---|---|---|
| S1 | Take the branch: three rules, 969 / 1000, none worse | Take; the C1 re-baseline absorbs the moved references |
| S2 | 803 and 430: a junction's minor gas and traces, resolved at the junction's total (absolute fraction rows), omitted where they are traces (D41) but booked exactly downstream | One design item: (a) a junction's fraction rows stated over each component's own share, floored at the trace share (measured alone: 803's gate 2.5e-8 -> 1.2e-8, not enough); (b) a trace omitted at a junction carried consistently, its outflow fraction frozen at the junction's start composition in both the rows downstream and the booking; (c) D41's carried trace extended to a hydrocarbon trace in any phase in free water (measured: 430's noisy rows go, 430 then meets (b)). Build (a) to (c) together in one thread, gated by 803 and 430 |
| S3 | 431: the first grams of a hot crude in an empty tank | With the thermodynamics work (the first grams family of D56) |
| S4 | 446, 957: thermodynamics model and data floors | D44 |

### 7. Against the brief

446 is not a phase-port overdraw (its first status on `cc54` was already the flash); 803 holds on the equation gate, not on Newton limits and not at a taut corner; 886 needed no cap rule: the pass loop reaches the physical active set, and the stage is the Newton's chord test (A371's). A third rule (A415) was added for 708's later hold, which A411 exposed; it completes D59 (2) for the layout. Commit trailers name Claude Opus 5.5, the writing model, not Claude Fable 5.1. Documents are not copied to the main checkout.

## The close: C4, the full suite and the kink (2026-10-14)

Branch `claude/phase-ports-compressor` after `5b24744e`, the orchestrator, owner decision D63. Commits: C4 `a8e49dae` (merge of `claude/determinism-rehearsal` `289c6f2d`), `a68e8e1d`, `ce4809e9`; A423 `3e0e1890`; tests and gate `4ed5aa22`, `cc7c9ae9`, `d3365664`. Decisions A423-A431; the worse seeds (t32, merged at `1cce9a2b`, D64) and the in-game check (t33, `05fce69c`) A432-A438, parts 4a and 4b. Logs `cl01`-`cl17` in `tools/phase-ports-probes/hold-classes/logs/`; scripts `printed-maxrel.js`, `slice-pairs.js` there. The integration from the determinism side: `INTEGRATION_REHEARSAL.md` section 16.

C4 ran first, as D63 ordered. Its gates were green (`cl01`), but the full suite (`cl02`) failed five tests of `com.wormzjl.createcheme.runtime`, a package G1 did not run: its patterns were `science.fluid.*` and `runtime.fluid.*`, and G1 now also runs `runtime.Fluid*` (`cc7c9ae9`, 4 classes, 17 tests). All five predate C4 (`cl03`, `cl05`). Two were a real stall at the kink of the wall law, fixed by one rule (A423). Three asserted what D54 or D58 removed and were restated; a fourth of that kind showed in the gates after A423.

### 1. Physics first, item by item

| Item | Real life | What the code did | Outcome |
|---|---|---|---|
| `FluidPumpedFillLineTest.everyPumpedChainCommitsItsFirstTickFromAColdSolver` (fails since `51e27cd4`, D54) | A pump starting into a chain of full nitrogen bags at the ambient moves 1e-8 of their volume in the first step. Nothing is stiff; at the kink the balance is the same equation on both sides | `initialPhaseSeeds` re-flashed the resting tanks a 1e-12 water trace may reach and left their seeds 7.45e-9 over capacity (the taut side). The first step, on taut slopes, left tanks 4-7 2.7e-8 below the ambient and 1.9e-8 over capacity (both wall branches positive). The chord steps kept the taut Jacobian (a pressure moved 2.7e-8 passes the chord test) while the energy row's bag volume switched between V and the capacity: the line search stalled at 5.3e-8 at every step down to 4.8e-8 s (`cl07`, `cl08`) | Rule A423: a Jacobian sweep states every wall row and bag volume on the branches of its base point; a chord step is refused once a vessel's branches differ. Passes (`cl09`) |
| `FluidPumpedFillLineTest.aPumpedFillPassesTheSecondTanksBackflowWithoutAHold` (since `51e27cd4`) | As above | The same stall (`cl06`) | Passes with A423 (`cl09`) |
| `FluidPumpedFillLineTest.aGasTransferClosesAtThePumpsScaledLimitAndCertifies` (since `51e27cd4`) | The suction is a bag at the ambient: it gives its gas at its own temperature and shrinks at the ambient pressure, the atmosphere's work replacing the flow work of the gas that leaves. It stays at 298.15 K and 101325 Pa. The discharge, a full bag, warms by its compression and the shaft work booked as heat (D8) | The test asserted the suction "a fraction of a kelvin cooler", the rigid-tank result | Restated under D54 (`4ed5aa22`): suction at 298.15 K to 1e-6 K and 101325 Pa to 1e-3 Pa (`cl10`, `cl13`) |
| `FluidThermoDomainHoldTest.aReproducibleDomainFailureHolds...` (since `51e27cd4`) | Under D54 nothing cools; the discharge heats to 302.3 K at the compressor's ratio 1.05 | The test narrowed nitrogen's floor to 298.05 K and waited for the suction to cool past it | Restated: the top narrowed to 300 K; the violation is named at the discharge tank (`reservoir at 4, 64, 0`, 300.00000000000006 K, `THERMO_DOMAIN_TEMPERATURE_ABOVE`, "valid 63.151..300 K") (`cl10`, `cl13`) |
| `FluidFallbackQualificationTest.hardTimeoutInsideFallbackAndBoundaryRegimeChangesCannotPublish` (since `62e6b790`) | A tank above its generator cannot flow back through the generator's one-way exit: the line is closed and nothing moves | Since t25 (D58) the exit is closed before any solve and the island is at rest, so the full solve ends inside any soft budget and is published FULL with the line CLOSED (`cl12`); the test expected a fallback refusal with a "regime" detail | Restated: asserts the FULL publication, then asks the approximate solver directly under the anchor's guard, which refuses the regime (`cl13`). A424 (the anchor carrying its flows) built, unforced, reverted |
| `PumpRiseScalingTest.aGasTransferBetweenClosedTanksClosesWithinASecond` (G1, found in `cl14`) | As the gas transfer above: the suction stays at 298.15 K | Its strict `suction.temperature() < 298.15` held on a residue below 298.15 K; after A423 it reads 298.150000000006 K (`cl14b`). The printed line is `suction 298.150000 K 101325.000 Pa` in `fx37`, `cl01` and `cl14` alike | Restated under D54 like the gas transfer (`d3365664`; `cl14c` 5 / 5) |

### 2. Gates (each run both ways)

| Test | Without | With |
|---|---|---|
| The two pumped-chain stalls | stall at 5.3e-8, `cl07`, `cl08` (no A423) | pass, `cl09`; `cl13` (four runtime classes, 17 / 17); A423's unit gate `PhaseLayoutTest.theWallRowAndTheBagVolumeAreStatedOnTheBranchASweepIsBuiltOn` passes in `cl14` |
| Gas transfer, thermo domain (restated) | fail, `cl02`, `cl09` | pass, `cl10`, `cl11`, `cl13` |
| Fallback regime (restated) | fail, `cl10`, `cl11`, `cl12` | pass, `cl13` |
| `PumpRiseScalingTest` (restated) | fail, `cl14`, `cl14b` | pass, `cl14c` |

### 3. Quick checks

None. D50 allows one campaign for a single form, and A423 is the only form.

### 4. Campaign (`cl16`, `d3365664`, 8 workers, alone)

| | `fx35` | `cl16` |
|---|---|---|
| Clean random / world defaults | 969 / 1000 | **967 / 1000** |
| First-interval family | 14 / 0 | 14 / 0 (957 left, 707 new) |
| Newton iterations (M) | 1.076 / 0.110 | 1.133 / 0.109 |
| Newton solves (M) | 0.288 / 0.040 | 0.321 / 0.040 |
| Jacobian builds (M) | 0.460 / 0.037 | 0.495 / 0.037 |

Cleared: random 957 (C, methane below the data in `fx36`). Worse: random 71 (E8, "Interval substep limit; no partial interval may commit", new), 707 (E10, back after t29 cleared it), 788 (F, new). 521 moved E4 -> E8. Classes random `fx35` -> `cl16`: C 11 -> 10, E10 4 -> 5, E2 1, E3 2, E4 1 -> 0, E5 3, E6 2, E7 1, E8 3 -> 5, F 3 -> 4. The counter rise is not traced.

### 4a. The worse seeds (t32)

Branch `claude/close-worse` from `d15661b8`, thread t32, merged at `1cce9a2b` (D64, taken by the orchestrator on t32's recommendation, confirmed by the owner 2026-10-14). Code W1 `92f419c9`, W2 `efc3682f`; review `CLOSE_WORSE_SEEDS_REVIEW.md`; rows A432-A436; logs `cw01`-`cw29`. Neither hold is A423's: both seeds hold the same way with A423 switched off and run clean on `cb26806e`, so C4's roundoff exposed two holds already in the code (A434).

| Seed | Physics in one line | Mechanism | Rule |
|---|---|---|---|
| 707 (E10, b:trace-underflow) | A 371 kPa crude generator and a 363 kPa water generator fill three empty 1 m3 tanks; the far tank is still empty in real life; steady state all three full near 371 kPa | A tank holding a residue (1e-42 mol of crude, 1e-84 mol of water vapour) is restated by a closure Newton whose floors difference the water-vapour column with 1e-22 mol, 1e19 times the contents: no trial is finite | W1 (A432, D59 (2)): a vessel that holds nothing after its booking is stated at its flash, with no closure; the booking's half of A415. Gate `HoldingNothingBookingTest`: held at tick 167 without, kept up with |
| 71 (E8), also 26 | A 334 kPa water generator fills a tank 3 m below it; full, the tank stands at 363.4 kPa and nothing flows | The tank is accepted 1e-11 over capacity; water's stiffness turns that into 0.01 Pa, read as 2.8e-8 against the 1e-8 gate; the D13 polish converges but pushes that 0.01 Pa back into the generator, a forbidden direction, and refuses the step at every step size | W2 (A433): the pass loop takes the closure a polished point shows, as at a converged point (one `closeBoundary` for both), and solves again from the reconstruction. Gate `PolishedClosureTest`: held at tick 734 without, kept up with |

Alone (`alone.sh`, 1 worker; A434):

| | 71 | 707 |
|---|---|---|
| `d15661b8` (`cw01`) | E8, tick 1969, gate 2.79e-8, 520 refusals | E10, tick 1071, "No finite Jacobian trial" |
| `d15661b8`, A423 off (`cw02`) | same, same counters | same, same counters |
| `cb26806e` (`cw04`) | clean, 658 Newton solves | clean, 771 Newton solves |
| W1 + W2 (`cw24`) | clean, 652 Newton solves | clean, 814 Newton solves |

Quick checks (seeds 1-200, 4 workers; `cl16`'s seeds 1-200: random 193, world defaults 200):

| After | Random | Defaults | Worse | Cleared | G1 | References against `cl14` |
|---|---|---|---|---|---|---|
| W1 (`cw17`) | 193 | 200 | none | - (707 is above 200) | 565 / 0 | identical (0 of 33 junction lines, MIXED_GAS_COST 339, chain-100 2.695e-10, 0 printed lines) |
| W2 form (a) (`cw20`, not taken, A435) | 195 | 200 | none | 71, 26 | 565 / 2 | 25 of 33 junction lines, 3330 printed lines |
| W1 + W2 (`cw28`, on the commit) | **195** | **200** | none | 71, 26 | 566 / 0 | identical |

Counters (A436):

| Seeds 1-200 | Newton solves | Newton iterations | Jacobian builds | Gate refusals |
|---|---|---|---|---|
| random, W1 (`cw17`) | 103 490 | 160 379 | 74 335 | 16 699 |
| random, W1 + W2 (`cw28`) | 42 581 | 114 949 | 43 645 | 1 138 |
| world defaults, both | 9 184 | 23 166 | 6 467 | 0 |

`cl16`'s +33.6 k random Newton solves over `fx35` are almost all the worse worlds: 71's hold alone costs +30.3 k solves and +7.8 k gate refusals (`cw01` against `cw04`).

Form (a), the wall row divided by the contents' stiffness, clears 71 and 26 but fails `TransientQualificationTest`'s liquid-full reference through the same amplification in the water-balance rows, moves 25 of 33 junction lines and costs 14 % more world-default Newton solves: measured, not taken (A435).

Stop point: the stiff-liquid amplification remains as a family. A row a taut liquid vessel closes to ε shows in its pressure as the stiffness times ε, 1e4 times more than in a gas. The D13 polish and W2 resolve it where it appears. Stating a taut liquid vessel's rows over its stiffness is the structural form; it is a design item for the next batch (D64 (2)), not built.

The campaign on the merged code:

`cl18` (`1cce9a2b`, 8 workers, alone; A439): **972 random / 1000 world defaults** (`cl16` 967 / 1000, `fx35` 969 / 1000). Cleared against `cl16`: 26, 71, 246, 521, 707; no new worse seed; against `fx35` only 788 (F budget) is worse. Classes random: E10 5 -> 4, E8 5 -> 1 (803), the rest as `cl16`.

| random / world defaults | `cl16` | `cl18` |
|---|---|---|
| Newton iterations (M) | 1.133 / 0.109 | 1.064 / 0.109 |
| Newton solves (M) | 0.321 / 0.040 | 0.218 / 0.040 |
| Jacobian builds (M) | 0.495 / 0.037 | 0.443 / 0.037 |
| Gate rejections | 28 110 / 0 | 2 258 / 0 |

The world-default counters are identical to the count. D64 stands on this campaign.

### 4b. The in-game check (t33)

Thread t33, `05fce69c`, on `d2321885`; review `CLOSE_IN_GAME_REVIEW.md`; evidence `tools/phase-ports-probes/in-game-close/`; rows A437, A438. Fresh world (`format=7 ... islands=0`), lane `t33-lane.sh alone`, client 22:31:50-23:03:12; HELD lines 0, exceptions from a createcheme class 0. Five of six checks pass.

| # | check | result | evidence |
|---|---|---|---|
| 1 | a placed 1000 L tank alone | PASS | `EMPTY: holds nothing / STEADY: no flow since 114.1 s`; "No connection leaves this tank" (`01`, `01b`) |
| 2 | a 130 kPa water generator fills it through a 0.01 m pipe | PARTIAL | fills like a bag at 101.3250 kPa while slack, `Side (west): mixed outlet / receiving · 66.5 kmol/h` (0.333 kg/s); stopped at 1770.6 s with 527.6 kg (53.0 %) (`02a`, `02b`) |
| 3 | a compressor (ratio 3) from a 150 kPa nitrogen generator into an empty tank (A327) | PASS | tank 449.9955 kPa at 149.8 C (423 K); compressor `CLOSED (limit 300000 Pa at this suction)`, ratio 3.0, STEADY (`03a`-`03c`) |
| 4 | a compressor between two placed empty tanks | PASS | `STEADY: no flow since 267.9 s / INLET_WRONG_PHASE / ERROR: compressor inlet dry`; both tanks EMPTY (`04a`, `04b`) |
| 5 | a pump (0.001 m3/s) drains a filled tank (twin T5) | PASS | `PUMP_TARGET` at 199.0 kmol/h, then `STEADY: no flow since 1015.7 s / INLET_WRONG_PHASE / ERROR: pump inlet dry`; a 2.1 kg heel (0.2 %) (`05a`-`05d`) |
| 6 | the compressor between two filled nitrogen tanks (A332) | PASS | `CLOSED (limit 259842 Pa at this suction)`, ratio 3.0; suction 129.9212 kPa, 25.2 C; discharge 389.7630 kPa, 133.5 C, the GameTest's A332 values (`06a`-`06d`) |

Check 2's taut and STEADY stage was not observed: the thread stopped at 1770.6 s on its usage limit, and a 1000 L tank at 0.333 kg/s needs about 53 minutes of client time to fill. It is not re-run (D50): check 3 already shows the same bag going slack then taut to its generator's pressure with the island certified STEADY (A327), on the same code path. W1 and W2 touch the booking of a residue vessel and the pass loop's closure, which none of the six checks exercised, so this evidence stands for the merged code. Check 6's brief expectation (the suction stays at the ambient and its temperature) holds only for a slack bag: the 250 kPa charge leaves the suction taut, and its gas expands isentropically, 358.9 K x (129.92 / 250)^0.2857 = 297.6 K, shown 298.35 K; the code is right.

| # | finding | proposition (owner) |
|---|---|---|
| G1 | a pump directly on a tank face is not joined (D20) but reads `ERROR: pump needs two ports without a zero-storage bypass` (`05x`) | name the cause: "equipment joins only through pipes" |
| G2 | check 4's compressor table lists Nitrogen 0.0409 kmol (the idle seed at 1 atm) although the island holds nothing | show "No components" as the EMPTY tanks do |
| G3 | roundoff flows printed as `replaying 3.765e-21 kg/s` | print "no flow" below the bag's resolution |
| G4 | the rail of an EMPTY tank is blank (no fill, no capacity) | show fill 0.0 % and the capacity |

### 5. References (nothing re-recorded)

C4 against `fx37` (`cl01`): 28 of 33 junction lines (ledgers at 1e-16, junction shares at 1e-11 or roundoff shares of 1e-15; every Newton solve count unchanged; MIXED_GAS_COST 347); chain-100 2.695e-10 relative against its record, as in `fx37`; printed lines 6179 pairs: identical 625, at or below 1e-14 859, 1e-12 3345, 1e-9 539, 1e-6 722, 1e-3 6, above 1e-3 83 (ledgers and residues of 1e-14 to 1e-17, flows of 1e-26, the derivative check's residues, one unmasked timing line, and one count: `LEVEL_HEAD_RUN drain` Newton solves 746 -> 756). A423 against `cl01` (`cl14`): 24 of 33 junction lines (ledgers at 1e-16, shares at or below 1e-8, except the interval-5 case `0.02:4:true` 0.16698834561635167 -> 0.16573200808980484 with Newton solves 23 -> 20, a coarse-interval transient on another step sequence); Newton solves 39 -> 37, 55 -> 52 in two cases; MIXED_GAS_COST 347 -> 339; chain-100 unchanged; printed lines 6177 pairs: identical 4854, 1e-14 33, 1e-12 605, 1e-9 478, 1e-6 23, 1e-3 18, above 1e-3 166. Paired by index (`slice-pairs.js`, 4684 slices): `drain-0.1`, `fed-0.1`, `fed-5.0`, `gas-only` identical; `drain-5.0` 12 of 40 at or below 1e-3; `oil-water-drain` 28 of 40 above 1e-3: at slice 12 the drained tank reaches the ambient (the kink), `cl01` rejected one step there ("Newton line search stalled at residual"), `cl14` none, and the water residue 3.285e-12 -> 3.420e-12 of the moles carries through slices 12-39 (3.9e-2 relative on that residue, nothing else); the overflow cases at or below 1e-6 except 8 slices (7 at or below 1e-3, 1 above); 4 slices with other accepted or rejected counts. Against the recorded reference (`holds/12-junction-lines.txt`): 31 of 33 junction lines, MIXED_GAS_COST 264 -> 339. G3 45 / 45; GameTests 33 / 33.

C1 on the final code (`cl19`, `0041d224`, A440): chain-100 re-captured (its record moves by C4's 2.695e-10 relative, one line); the final gates G1 583 / 0, G2 exact 0.000e+00 against the fresh record, G3 45 / 45, GameTests 33 / 33 (run id `close-cl19-1cce9a2b-run1`); the junction lines identical to `cl14`'s and copied to both reference files (`12-junction-lines.txt`; the cloud harness's `junction-lines.txt` with `java.runtime.version=21.0.11+9-LTS-211`); printed lines against `cl14` 6197 of 6198 pairs identical, the one difference a timing line; MIXED_GAS_COST 339. The cross-runtime sweep on `0041d224` (`cl20`, A441): five Windows runtimes identical (583 / 0 each, 6154 printed lines, chain-100 exact, fuzz first holds 200 / 200 in both distributions); its build needed the E6 probe exclusion (patch under `tools/jdk-determinism/winsweep/`). After the cleanup of the force-added logs (A442, `b5def4d3`) the gates again, `cl21`: G1 583 / 0, G2 exact 0, G3 45 / 45, junction lines 0 of 33 moved against the reference at its new path `tools/phase-ports-probes/holds/12-junction-lines.txt`.

### 6. Left, and stop points

| # | Item | Next |
|---|---|---|
| L1 | Random 71, 707 and 26 | Cleared on the merged code by W1 (707) and W2 (71, 26) (`1cce9a2b`, D64, confirmed by the owner 2026-10-14; quick check `cw28`; pending the campaign `cl18`, part 4a). 26 was filed under D44 before |
| L2 | Random 788 (F, new) and 521 (E8) | 788 replayed alone once (`cl17`, `b03e85ec`): F again, 20.5 s of seed time against the campaign's 21.6 s; it counts as worse (D50). Neither 788 nor 521 is touched by W1 or W2 (t32): unchanged by the merge; their state on the merged code is `cl18`'s |
| L3 | The stiff-liquid amplification | A design item for the next batch (D64 (2), part 4a): a taut liquid vessel's rows stated over its stiffness; not built |
| L4 | G1-G4, the GUI findings of C3 (part 4b) | Proposals for the owner; no code changed |
| L5 | C1 (the single re-baseline), C2 (cleanup and detach), C3 (in-game check), C5 (docs, CHANGELOG 0.7.0, `mod_version`) | C1 next, on the final code after `cl18` (chain-100 capture, the final gates alone, the junction lines against both references), then the winsweep; C2 thread t31 (`claude/close-cleanup`, no solver code), done; C3 done (t33, `05fce69c`) except check 2's taut and STEADY stage, not re-run (D50: check 3 shows the same path certified STEADY, A437); C5 last, the merge on the owner's go |

### 7. Against the brief

D63's order was C4, then C1. C4 is done, but C1 is not next: the full suite is part of C4's proof (rehearsal section 11), and it failed. The five failures were swept over the merges, traced, fixed by one rule and four restatements, and one campaign measured the result. C1 waits for t32 so that the references are re-recorded once, on the final code. The sweep dates the fallback test's failure to `62e6b790` (2026-10-12), before t25's closures (`ecc4a19d`); `cl12` traces only the current mechanism. The documents' commits name Claude Opus 5.5, the writing model, in their trailers.
