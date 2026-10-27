# Unified thermo port: the owner's test world (run sheet)

Batch `2026-09-24-coolprop-low-temperature`, branch `claude/unified-thermo-port`, written 2026-10-16. Tools, row files,
presets and logs: `tools/unified-thermo-port-test-world/` (README there). Scope: `MAIN_ADAPTATION_PLAN.md` "The owner's
test world" (13 rows of `main-adaptation/RUNTIME_FORMATS_GUI_LAYER.md` E4, plus two for D23), prepared files-only.

Numbers come from two probes on the javac lane, no Gradle (`index-lane/`):
- `TestWorldEngineProbeTest`: each feed's generator state (TP) and where its stream ends in a tank at a given pressure
  (PH, with the CO2-I crystal competing). This is the physics expectation: an ideal well-mixed tank at equilibrium.
- `TestWorldRigTest`: the row files replayed on the runtime the game runs (`PhysicalRegistry`, `IslandCoordinator`, 20 ticks
  per second, game defaults, the driver's own step times). This is what the screens should show on the code.
- Commits: form A + D33 = `c9e7c128` (HEAD when written, the expected form); form A = `40a44732`. Only R3, R4, R11 and
  R13 read differently between the two forms; R2-R7-R9 holds liquid throughout, so it does not.
- Updated 2026-10-17 against `87c106f6` (range work WP1-WP3, ice Ih WP4, quick-check fixes), from the code, the gates
  and the archived rig logs; the rig was not re-run. Rows and sections that changed carry "(updated 2026-10-17: range
  work / ice)"; row 12 (acceptAtFence inside a network) is new. The branch has since moved to `95e4325d` (`db6b1e6f`,
  `95e4325d`: trial bounds and dry vessels). Those commits change none of the texts quoted here.
- Hold texts: the status prefix "HELD (thermo domain): " becomes "HELD (phases): " and the log's `status=THERMO_DOMAIN`
  becomes `status=PHASE_HOLD` before the session (every thermo hold left is a phase hold); this sheet uses the new texts.
- Updated 2026-10-18 against `482264fc` (20 commits on `87c106f6`), from the code read at that commit and the gates'
  archived output; the rig was not re-run. Rows and sections that change carry "(updated 2026-10-18: D34, S1 to S6, D38)".
  Rows 2, 9, 10 and 12 are marked for the rig re-run (§4); rows 1, 3, 4, 6 and 7 do not change.
- Rig re-run 2026-10-18 at `00ee8385` (D39 on `482264fc`, the session's code): rows 2, 8, 9, 10 and 12 and a +600 s
  tail of row 2. Their blocks "Rig at `00ee8385`" replace the predictions above them. Four numerical holds that no gate
  covers are findings H1-H4 (§3), traced in `TEST_WORLD_RIG_HOLDS_TRACE.md`. The attribution runs at six older commits
  are listed in §4. The in-game session waits for their fixes (the lead's stop of 2026-10-18).

## 0. Before the session

1. **D32's fix must be on the branch.** Without it the second save of R6-R12 crashes (`NoSuchElementException` in
   `FluidWorldAuthority.capture`, M4 open item 1). It is, since `15220a07` (updated 2026-10-17: range work / ice).
2. **Fixes since `c9e7c128`** (updated 2026-10-17: range work / ice). On the branch now: form 4 (`58d9dac4`, `2ab43f23`;
   fixes F1), D32, review section 5 (`81129851`: every hold names its node), F4 (`60b62694`), F2 fix A (`ed2cdff4`), D33
   on every path (`1b75ce08`), the range work (`a169fa85`, `085676ea`, `90d68ca1`: leaving a data range never holds; the
   status line carries a notice), ice Ih (`13bcfe31`, `1674e3db`, `ead6d6f2`) and the quick-check fixes (`8220d4bc`,
   `fd0dc531`, `87c106f6`). Unmarked rows keep their `c9e7c128` numbers. R9 (F2) and D23-2b's crystal size (F4) are
   expected to differ there; they were not re-run.
   - Since `87c106f6` (updated 2026-10-18: D34, S1 to S6, D38): `db6b1e6f` (the box bounds a vapour's partial pressure),
     `95e4325d` (a vessel holding no fluid is a dry end), `04f84aa5` (hold texts, used here already), `e4769fa2`,
     `703d69b3`, `7bd5fafc` (water at its melting line and triple point), `4199dbe6` (D34), `03bc70f6` reverted by
     `26090bf0` (net: gas weighs its density, as at D34), S5 `bff22e37`, S3 `819d774b`, S6 `9445106a`, S4 `22b76697`,
     S1 `eec02758`, S2 `65036f2b`, rule C `67e5f3e7`. Effects on this world are in "Common facts" and in rows 2, 5, 8 to 12.
   - Rows 1, 3, 4, 6 and 7 lie at one height, so D34 and S1 give them no column and no lift. No other commit moves a value
     they read at the rail's precision (0.1 °C, 0.1 Pa, 0.1 kg).
3. Set-up (README "Session set-up"):
   - Write `guiScale:1` and `pauseOnLostFocus:false` in `run/mcp-client/options.txt`.
   - Copy `world-template/` to `run/mcp-client/saves/<new folder>` and `mixturepreset/*.json` to
     `run/mcp-client/CreateChemE/mixturepreset/`.
   - Start `runMcpClient` and run `node mcp/bridge.js wait-ready`.
   - Run `node mcp/row.js open-world <folder>`, then `node mcp/row.js setup`.
   - Then run `MC_WORLD=<folder> node mcp/row.js rows/NN-*.json` for each row in the order below.
   - `node mcp/check-rows.js` must print "no problems" first.
4. Comparing reads: compare states and settled values, not seconds.
   - In game a `set` takes about 10 s, and the cadence adapts (`adaptiveCadence` true; the rig uses a fixed 5 s).
   - The rig has no wall clock, so in-game wall-budget holds ("HELD: wall deadline": an interval past the 2 s budget)
     may appear that the rig does not show. Since S2 no interval is cut at the 1.5 s soft budget: there is no soft-budget
     hold and no "APPROXIMATE: soft budget" status. The soft budget only lengthens the adaptive cadence (updated
     2026-10-18: D34, S1 to S6, D38).

Common facts used below.
- Tanks:
  - Tanks are 1 m³ and slack at 101.325 kPa until full (D54).
  - Pipes are 1 m long and 5 cm wide unless a row sets the diameter. Pipe flow is capped at 100 m/s ("VELOCITY_LIMITED").
    - Since S5 a liquid phase stream's limit is this cap itself (updated 2026-10-18: D34, S1 to S6, D38). Every liquid
      here has an isothermal sound speed far above it (LNG about 1000 m/s, liquid nitrogen at 95 K about 450 m/s, liquid
      CO2 at 250 K about 450 m/s), so no limit moves.
  - Generators take no backflow: a generator below its tank's pressure shows CLOSED.
- Statics and energy (updated 2026-10-18: D34, S1 to S6, D38):
  - D34: a line's static column is the port's phases in priority order, filling the line's own volume (1.96 L per metre
    of 5 cm pipe). Only the vertical lines of rows 2, 5, 8 and 9 have a column. It differs from the old leading-phase
    column only while the tank holds less of that phase than the line.
  - D36 is reverted (`26090bf0`): gas weighs its density in every column, as at D34.
  - S1: no potential energy. Lift work arrives as heat, g Δz / c_p: +0.04 K at most here (row 5's 4 m gas lift), under the
    rail's 0.1 °C.
  - S6: no screen ever had an efficiency field; the GUI and the world default built 1. Nothing moves in game, and the row
    files set only flow, head and ratio. S3 is bitwise.
  - Rule C (`67e5f3e7`): a phase under 1e-6 of a node's moles that shrank since its solve's start leaves at once, in a
    junction as in a vessel. It moves row 10's gate.
- Crystals (D23):
  - Crystal mass moves only at a vessel refresh, at most every 5 s.
  - Each stock holds one size per solid, shown as one Solids row "Carbon dioxide (solid)" at its mean size (0.25 mm for every row here; since F4, D23-2b's slush about 0.5 mm) (updated 2026-10-17: range work / ice).
  - An outlet carrying solids closes "blocked with solid: DEPOSITION" when its liquid velocity is at or below
    10 × the Ferguson-Church settling speed. For 0.25 mm dry ice that is 1.02 m/s in liquid methane and 1.05 m/s in LNG.
  - An outlet closes "PACKED_SLURRY" at 0.5 solids.
- Solids in transport (D33): solids ride the liquid. A bulk end on a donor without liquid draws its gas.
- Ice Ih (WP4) (updated 2026-10-17: range work / ice):
  - Free water below its melting line (273.15 K at 1 atm) is ice. Like dry ice, it forms or melts only at a tank's
    refresh, then rides the liquid.
  - Between refreshes, the steps, pipes and generators carry it as a stand-in: liquid water with its 273.16 K
    properties, shown in the Aqueous tab as liquid water. At the refresh it freezes and releases the fusion heat less the
    stand-in's extra heat capacity (76 against the ice's 37.8 J/(mol K)): 6.007 − 0.0382 × (273.16 − T) kJ/mol, so
    +1.25 kJ/mol at 148.7 K.
  - Solids tab (headers "Solid", "Mean size (µm)", "Mass (kg)"): one row "Water (solid)". Its hover tooltip, which the
    bridge cannot read, names `createcheme:crystal_ice_ih`. The record's own name, "ordinary hexagonal ice (ice Ih), the
    phase of IAPWS R10-06(2009)", is shown on no screen. Size: 250 µm up to a solids share of 0.09, up to 1.01 mm at 0.5.
- Data ranges (WP1-WP3) (updated 2026-10-17: range work / ice):
  - A state past a data range runs: its properties extrapolate. No hold.
  - The island's status line then ends "; outside data range: <what> extrapolated below|above <bound> at <device> at x,
    y, z", plus " (+N more)". It is printed in the muted colour, never the warning colour.
  - In this world only row 10's water crosses a data end (273.16 K). The pilot's CO2 data reach 90 K (CO2-I, 80 K). A
    tank never falls below 1 atm, so LNG stays above 111 K. Nitrogen dissolved above 126.19 K reads its vapour data.
  - S4 does not reach this world (updated 2026-10-18: D34, S1 to S6, D38).
    - It holds the ideal-gas fit of the bundled record `createcheme:fluid_nitrogen` (the `tjl20_*` packages) below 273.16 K.
    - The pilot package takes every ideal gas from its reference spines (`HydrocarbonModel` spine path). Nitrogen's,
      `spine_nitrogen` (Span 2000 alpha0), starts at 63.151 K, the triple point.
    - So no nitrogen notice appears here, LNG and the 95 K liquid nitrogen of row 12 included.

## 1. Rows in session order

Each row gives: set-up | physics (what a real plant does, in numbers) | expected on `c9e7c128` (on `87c106f6` where a row is marked updated 2026-10-17) | read-back | decision exercised.

### 1. R3 + R5: frosting tank (`rows/01-R3.json`, z = 20) (updated 2026-10-17: range work / ice)
- **Set-up.** Generator → pipe → tank → pipe → void, all side ports. N2 at 150 K, 300 kPa; then CO2 at 220 K, 400 kPa.
- **Physics.**
  - The N2 flows through at the outlet's velocity cap: the tank holds 288 kPa and 150 K at 1.29 kg/s.
  - The CO2 gas is 7 K above its sublimation line (212.9 K at 400 kPa). Mixed into the cold N2 it frosts.
  - As the N2 is flushed, the tank warms to the CO2's own let-down state (PH at 382 kPa: 219.6 K) and the frost sublimes.
  - Gas leaves through any nozzle.
  - The E4 setting of 210 K lies below the line. That generator holds a stand-in liquid, 64 % of which would freeze at 400 kPa. On `c9e7c128` the row then held (F1). That hold is gone (form 4; and the 90 K CO2 floor is now a data end, not a hold). The row keeps 220 K so its numbers stay comparable; 210 K was not re-run.
- **Expected (A + D33).**
  - n2: 150.1 K, 287.8 kPa, 1.29 kg/s.
  - co2-early (+8 s): 214.1 K, 382 kPa, 0.196 kg dry ice, N2 32 mol beside CO2 193 mol. The east (side) outlet draws gas at 1.82 kg/s.
  - co2-late (+48 s): 219.6 K, no frost, 1.91 kg/s, status FULL.
- **Form A.**
  - At the first frost (0.154 kg), the side outlet reads "blocked with solid: NO_LIQUID_CARRIER".
  - The tank fills to 400 kPa and rests ("STEADY: no flow") at 185.2 K with N2 233 + CO2 32 mol. The frost stays at its frost point (186 K).
- **Read-back.** Tank overview, vapour tab, Solids tab and outlets page at each tag; pipe P2 overview; island log lines.
- **Decision.** D33; D23 refresh; R5's Solids tab for frost from a gas.

### 2. R2 + R5 / R7 / R9: methane pool, outlets, pump (`rows/02-R2-R7-R9.json`, raised tank at y = 102, z = 26..33)
- **Set-up.**
  - The CO2 line is set to 1 cm before its generator exists. A 5 cm line passes 1.06 kg/s, whose freezing heat boils the pool and chokes the vent.
  - Methane at 115 K, 200 kPa fills the tank through the top vent. The feed is then cut.
  - CO2 gas at 250 K, 250 kPa enters the pool.
  - R7 adds side and bottom voids. R9 replaces the bottom void with a pump: 0.0005, then 0.005 m³/s.
- **Physics.**
  - Fill: the tank is liquid-full at 155 kPa and 113.8 K, with 19.6 kg/s leaving through the top port as liquid.
  - Vented pool: it boils at 111.6 K (2.4 % flashes, engine).
  - CO2 at the 1 cm cap (0.0426 kg/s, 0.97 mol/s) freezes, since its solubility is about 0.05 mol %. About 30 kJ/mol (cooling plus deposition) boils 3.6 mol/s = 0.058 kg/s of methane, and dry ice grows at 0.043 kg/s.
  - R7: the bottom drains the slurry under its head, and the crystals ride the liquid. The side outlet runs near 1 m/s, which is the deposition limit.
  - R9: at 0.25 m/s the crystals settle in the suction line, which plugs. At 2.55 m/s the slurry flows, the remaining ~175 kg drains in about 80 s, and the pump then reports "pump inlet not liquid".
- **Expected (both forms).**
  - fill: 113.8 K, 155 kPa, 428.8 kg.
  - r2-30s: 111.6 K, 101.4 kPa, liquid 810 L, 0.87 kg dry ice. The top draws 0.0595 kg/s of vapour.
  - r2-60s: 2.36 kg.
  - r7: bottom 4.43 kg/s (head 2.4 kPa). The side reads "DEPOSITION (1.02 m/s, needs 1.02 m/s)", passing 0.22 kg/s on average while it flickers at the limit. Dry ice 2.9 kg.
  - r9-slow: the suction line reads "blocked with solid: DEPOSITION (7.04e-04 m/s, needs 1.02 m/s)" and the pump "ERROR: pump inlet dry (from node …)".
  - r9-fast: the same, at 6.98e-03 m/s (F2).
  - r9-dry (+90 s): still 330 L of liquid and 8.96 kg of dry ice.
- **Expected at `482264fc`** (updated 2026-10-18: D34, S1 to S6, D38). Marked for the rig re-run: F2's fix A (`ed2cdff4`)
  was never replayed here, and D34 and rule C act on this row's vertical lines and pool. The r9 lines above are F2's.
  - fill to r7: as above. The 20 commits move them only through D34 (the first 2 L of vapour at the top port read as
    liquid) and S1 (about +0.01 K).
  - r9-slow: "blocked with solid: DEPOSITION (0.25 m/s, needs 1.02 m/s)". Fix A converts the 0.5 L/s target at the drawn
    liquid's density (gate `SlurryTankPumpTest`). The pump's own line: the rig's to state.
  - r9-fast: the line opens at about 2.5 m/s. The pump runs PUMP_TARGET at about 2.1 kg/s (0.005 m³/s of methane at
    422 kg/m³; the gate: 2.17 kg/s of LNG) and carries the crystals with the liquid (D33).
  - r9-dry (+90 s): the remaining liquid, about 175 kg, is gone or nearly. The pump reads "ERROR: pump inlet not liquid
    (vapour … % by volume, from reservoir at 24, 102, 28)" or "ERROR: pump inlet dry (from reservoir at 24, 102, 28)". The
    crystals left with the slurry; new frost forms from the CO2 feed while the gas is cold.
  - After the row the CO2 feed (0.97 mol/s) flushes the methane vapour out (a turnover of 1 to 2 min). Physics: while
    frost remains in nearly pure CO2 at 1 atm, the tank sits at 194.7 K, which is E4 row 2's D21 window. Once the frost
    has sublimed, it warms towards the feed's let-down state, about 248 K. Row 11 reads this tank about 25 min later
    (T2); a rig-only tail of this row would predict which state it reads.
- **Rig at `00ee8385`** (2026-10-18; `logs/2026-10-18-session/rig-head-00ee8385.log`; the predictions above for
  r2-30s, r2-60s, r9-fast and r9-dry do not hold):
  - fill: 113.76 K, 155.0 kPa, 428.8 kg, "Ready."; the top port passes 19.71 kg/s.
  - r2-30s and r2-60s: HELD from committed 84.1 s, "HELD: Substep refinement exhausted: Newton iteration limit at
    residual 28.909854272354895; active-set pass=2; …" (H1). The tank stands at 112.30 K, 107.5 kPa, 354.1 kg, liquid
    821 L, vapour 179 L, no dry ice yet.
  - r7: the edit re-forms the island, and the hold clears: 111.59 K, 101.37 kPa, 229.4 kg, 0.47 kg of dry ice at 250 µm.
    Bottom 4.34 kg/s (head 2.24 kPa). The side reads "blocked with solid: DEPOSITION (1.02 m/s, needs 1.02 m/s)" at
    0.20 kg/s.
  - r9-slow: the suction line reads "blocked with solid: DEPOSITION (0.25 m/s, needs 1.02 m/s)"; the pump's island is
    FULL.
  - r9-fast: HELD for good from committed 253.9 s, "HELD: Substep refinement exhausted: Newton line search stalled at
    residual 0.4575197947270483; active-set pass=0; …" (H2), on the tank, the pump and its lines. The frozen state is
    111.61 K, 101.58 kPa, 164.4 kg, liquid 371 L, vapour 627 L, 3.03 kg of dry ice. The outlets read pump 2.165 kg/s
    (head 1.60 kPa), side 0.906 kg/s, top 0.0885 kg/s of vapour.
  - r9-dry (+90 s) and the tail at +300 s and +600 s: unchanged, with the retries deferred (events at 289, 309, 349,
    429, 589 and 909 s). So row 11 reads T2 in this frozen state under that HELD line, not at 194.7 K or 248 K.
- **Read-back.** The Solids tab grows by about 0.21 kg per 5 s; the outlets page; pump and pipe overviews.
- **Decision.** D22 (5)/D23: crystals are solids, the tank is well mixed, and they ride the liquid. D23 (4) deposition law. R5 freeze-out from liquid methane. E4 row 2's D21 window is not reached, because the liquid is never exhausted (F2). Since fix A the liquid drains, so the window can be reached after the row (updated 2026-10-18: D34, S1 to S6, D38).

### 3. R4: two tanks through a junction (`rows/03-R4.json`, z = 36..38)
- **Set-up.**
  - Ta is fed N2 + 10 % CO2 at 250 K, 300 kPa.
  - Tb is charged from N2 at 110 K, 300 kPa, then closed off.
  - J1-J2-J3 join Ta and Tb. A third leg Jc → Tc (an empty tank) makes J2 a junction; main compiles a straight run as one pipe, so without the leg there is no junction.
  - Ga is then raised to 500 kPa.
- **Physics.**
  - Charging a closed tank heats it: Ta ends at 306 K and Tb at 133.5 K.
  - The three tanks settle at one pressure (301.7 kPa). The warm mix reaches the cold tank only, and frosts there: 18 g at 137.9 K.
  - Raising the feed compresses Tb to 184.4 K. CO2 at 10 kPa partial pressure is then below the sublimation pressure (42 kPa), so the frost sublimes.
  - Everything rests at 500 kPa. The junction holds 1.0 kg.
- **Expected (A + D33).**
  - charged: Ta 306.0 K, Tb 133.5 K.
  - joined: Tb 137.9 K with 0.018 kg; Ta 273.1 K, Tc 328.0 K; J2 junction 209 K at rest.
  - raised and settled: Tb 184.4 K, Tc 348.8 K, Ta 278.6 K, all "STEADY: no flow" at 500 kPa, with no hold (no "active-set cycle").
- **Form A.** While Tb holds frost, its leg reads "blocked with solid: NO_LIQUID_CARRIER". The end state is the same (Tb 183.9 K).
- **Decision.** Main's junction holdup at stagnation without P6b's `a2f48d4`; D33.

### 4. R11: empty tank fills cold (`rows/04-R11.json`, z = 44)
- **Set-up.** N2 + 10 % CO2 at 175 K, 300 kPa through an empty tank to a void.
- **Physics.**
  - The generator holds 4.3 % of the feed's moles condensed: the stand-in for dry ice, since the feed is below its 181 K frost point.
  - PH (engine): 167.8 K and 2.2 % of the moles as dry ice at 1 atm; 175.2 K and 2.95 % at 250 kPa; 176.75 K and 3.1 % at 300 kPa.
  - The tank is slack for only about 2 s (it holds about 2 kg at 1 atm; the feed is 1.27 kg/s). It then runs taut at about 280 kPa: expect about 176 K and about 0.057 kg/s of dry ice.
- **Expected (A + D33).**
  - early (26 s): 171.5 K, 152 kPa, 0.064 kg.
  - taut (50 s): 160.3 K, 277.5 kPa, 0.54 kg.
  - steady (115 s): 163.6 K, 282.7 kPa, 2.79 kg (0.034 kg/s), outlet 1.20 kg/s gas.
  - This is 13 K colder and about 60 % of the equilibrium rate (F3).
- **Form A.** The outlet closes at the first frost (NO_LIQUID_CARRIER, 202.6 kPa). The tank fills to 301.3 kPa and rests at 178.75 K with 0.139 kg.
- **Decision.** D54 slack then taut; D23 refresh; D33. The slack phase is too short to read at this flow.

### 5. R8: compressor on a frosted tank (`rows/05-R8.json`, z = 52, compressor at y = 103)
- **Set-up.** Ta is charged closed from N2 + 10 % CO2 at 160 K, 300 kPa. Its top port goes to a compressor (0.05 m³/s, ratio 3, facing up) into an empty closed tank Tb.
- **Physics.**
  - Deposition heat after the feed closes lifts Ta to 175.4 K and 319.5 kPa, with 0.446 kg of frost.
  - Blowdown of an adiabatic tank cools the remaining gas (isentropic to 1 atm would give about 126 K). More CO2 therefore deposits, and its heat holds the gas near 155 K.
  - E4's "crystals sublime" needs heat from the walls, which the model has none of.
  - The suction tank cannot go below 1 atm (D54): it goes slack and its bag collapses.
  - The discharge is heated and frost-free. The compressor stops at its ratio.
- **Expected (both forms).**
  - blowdown: Ta 153.1 K, 101.3 kPa, fill 94 %, 0.636 kg; Tb 203.7 K, 239.8 kPa; compressor PUMP_TARGET 0.12 kg/s.
  - late: Ta 157.4 K, fill 61 %, 0.658 kg; Tb 216.5 K, 303.8 kPa (ratio 3.0); compressor CLOSED; "STEADY".
  - At `482264fc` (updated 2026-10-18: D34, S1 to S6, D38): unchanged at the rail's precision. S1 delivers the 4 m lift's
    g Δz = 39 J/kg into Tb as heat, +0.04 K at c_p of about 1 kJ/(kg K). S6: the game's compressor always had efficiency 1.
    D34: both lines carry gas only. Not re-run.
- **Decision.** D54; the compressor's gas inlet (the top port draws no solids, so no refusal).

### 6. R10: the phase set the engine does not implement (`rows/06-R10.json`, z = 60) (updated 2026-10-17: range work / ice)
- **Set-up.** A generator set to N2/C2H6/CO2 40/55/5 at 95 K, 2 MPa, then at 130 K, into a tank vented through 5 cm pipes.
- **Physics.**
  - At 95 K the mixture forms two liquids, a vapour and dry ice, a set the engine does not implement.
  - At 130 K it is liquid plus vapour. PH: 140.75 K with 4.67 % of the moles as dry ice at 2 MPa; 132.2 K with 4.86 % at 1 atm.
- **Expected.**
  - 95 K: the reply reads "Not applied: Unsupported phases: NOT_IMPLEMENTED (CRYSTAL_PHASES) at 95.0 K, 2000000.0 Pa …". The generator keeps water at 25 °C, so no island holds.
  - 130 K applies. The tank reaches 133.8 K, 1.74 MPa, liquid 158 L, 4.18 kg dry ice, with 29 kg/s through.
  - No data-range notice: nitrogen above its 126.19 K critical point reads its vapour data, and CO2's data reach 90 K.
- **Decision.** The typed refusal of CRYSTAL_PHASES. E4 row 10's held island cannot be made by a generator. acceptAtFence is exercised by row 12, a phase hold inside a network. R1's water hold, which it used before, is gone with the ice.

### 7. R13: high-pressure let-down (`rows/07-R13.json`, z = 68)
- **Set-up.** N2 + 10 % CO2 at 8 MPa, 220 K → 4 mm pipe → tank → pressure valve facing east at 0.3 MPa → void. The valve sustains its upstream pressure, which here is the tank.
- **Physics.**
  - Isenthalpic let-down to 0.3 MPa gives 180.0 K (engine), 1 K below the 181 K frost point. 0.25 % of the moles deposit.
  - The 4 mm pipe passes 0.192 kg/s, so the tank turns over in about 30 s.
- **Expected (A + D33).**
  - 30 s: 210.2 K. The heat of charging is still in the tank.
  - 140 s: 179.75 K, 300.5 kPa, valve VALVE_REGULATING 0.192 kg/s, 18.8 g of dry ice growing at about 0.7 g/s.
- **Form A.**
  - At the first frost the tank outlet closes, and the valve reads CLOSED.
  - The tank pressurises to 760 kPa at 217 K, and the closure stays after the frost has sublimed. This is M4 item 2's sticking closure.
- **Decision.** D23 (5): the valve and pipe stay open and the tank precipitates. D33.

### 8. D23-1: liquid CO2 let down to 1 bar (`rows/08-D23-1.json`, z = 76) (updated 2026-10-17: range work / ice)
- **Set-up.** Liquid CO2 at 250 K, 2.5 MPa → valve at 1.5 MPa → 4 mm pipe → tank with a top vent. A 5 cm pipe would pass 78 kg/s.
- **Physics.**
  - The let-down to 1 atm gives 194.63 K: 47.9 % of the mass becomes dry ice and 52.1 % vapour (engine).
  - At 0.283 kg/s that is 0.135 kg/s of dry ice, about 0.7 kg per bucket.
  - The valve stays open, and dry ice shows within 5 s.
- **Expected (`87c106f6`).** Sources: the gate `LiquidCo2LetDownTest` (`c63e9a8b`) and the form-4 bisect run (§5).
  - No hold at any read. From the first interval the tank sits at 184.8 K and 101.3-101.5 kPa. That is the stand-in liquid's boiling point at 1 atm, 9.8 K under the frost point, as in F3.
  - first-bucket (+6 s): dry ice from the first refresh on, "Carbon dioxide (solid)" at 250 µm. Expect about 0.5 kg per 5 s; the first refresh may not be published yet.
  - steady (+26 s): about 2.5-3.5 kg of dry ice, growing by about 0.1 kg/s (0.135 kg/s at equilibrium).
    - The gate holds 2.0 kg after 20 s. The form-4 bisect run read 3.55 kg in 7.34 kg (48 %).
    - The valve reads VALVE_OPEN 0.283 kg/s, and the top vent draws about 0.15 kg/s of vapour.
  - No data-range notice: the pilot's CO2 data reach 90 K. The log shows no HELD or PHASE_HOLD line.
  - The 5 cm variant (`rows-rig/`, rig only) floods instead, as a real 5 cm let-down would.
    - The gate's 5 cm case: 1.69 kg of dry ice at the first refresh, then 1.37 MPa and 242.3 K, liquid CO2, and no dry ice.
  - At `482264fc` (updated 2026-10-18: D34, S1 to S6, D38): unchanged; not re-run.
    - The 4 mm gate prints as at D34: 184.812 K and 101.325 kPa at every slice, 2.04 kg of dry ice at slice 5, valve
      VALVE_OPEN 0.283 kg/s.
    - Rule C moved only the 5 cm case's slice 2 (483.40 against 483.51 kg); the two agree within 5 g by slice 5.
    - The top vent carries gas, so D34 changes nothing here.
  - Rig at `00ee8385` (2026-10-18): as expected, with no hold. first-bucket: 184.81 K, 101.33 kPa, 0.43 kg of dry ice.
    steady: 184.84 K, 101.52 kPa, 3.55 kg of dry ice in 7.34 kg; valve 0.2826 kg/s; top vent 0.153 kg/s.
- **Decision.** D23 (5): the valve stays open and the tank precipitates. F1 is fixed by form 4. The range work removes the 90 K hold: nothing holds.

### 9. D23-2: traces in LNG, and slush (`rows/09-D23-2.json`, z = 84..88) (updated 2026-10-17: range work / ice)
**(a) Traces in LNG.**
- **Set-up.** LNG at 120 K, 1.2 MPa (1 % N2, 93.5 % CH4, 5 % C2H6, 0.5 % CO2) → raised tank with a top vent → bottom line → pump at 0.0005, then 0.005 m³/s.
- **Physics.**
  - Let down to 1 atm, the LNG reaches 111.1 K and 0.48 % of its moles freeze. Solids are 0.4 % of the condensed volume (engine).
  - The full tank (taut at about 650 kPa; the top port then passes liquid) holds 2.29 kg at 120.7 K.
  - At 0.25 m/s the crystals settle (the limit is 1.05 m/s). At 2.55 m/s they pass.
- **Expected.**
  - slow: the bottom line reads "blocked with solid: DEPOSITION (0.02 m/s, needs 1.05 m/s)" and the pump "ERROR: pump inlet dry".
  - fast: the line opens and the pump runs PUMP_TARGET 2.20 kg/s, carrying 0.077 kg of crystals per 5 s. The tank keeps 2.28 kg.

**(b) Slush.**
- **Set-up.** CH4 + 30 % CO2 at 140 K, 2 MPa fills a tank with a top vent. The feed is cut and the tank vents. A bottom outlet is then placed.
- **Physics.**
  - At 1 atm (engine), the tank reaches 111.58 K and all the CO2 freezes. Solids are 0.70 of the condensed volume, above the 0.5 slush share, so a bottom outlet blocks.
  - A 20 % feed would give 0.32 and flow.
- **Expected** (vented and slush: `c9e7c128` numbers, not re-run).
  - fill:
    - On `c9e7c128` it held for about 10 s ("Substep refinement exhausted: Newton line search stalled at residual 230.65 …", F5), and the cut cleared it.
    - The rig counted range refusals inside that hold ("Methane temperature < 90.6941 K", "Methane pressure < 100 Pa"). Those trials now continue past the data end, so the hold may be gone. Not re-run.
    - If the line search still stalls, the status reads "HELD: …" and the log prints `fluid_island=… committed_tick=… status=HELD: …`. The cut still clears it.
    - No PHASE_HOLD line and no data-range notice: the tank stays at or above 111.6 K.
  - vented (30 s): 135.5 K, 505 kPa (the vent is at its cap), 342 kg of dry ice (218 L) against 237 L of liquid, a share of 0.48.
  - slush (+10 s): 123.2 K, 239 kPa, 343 kg against 177 L of liquid, a share of 0.553. The outlet reads "blocked with solid: PACKED_SLURRY (10.15 m/s, needs 1.14 m/s)".
    - On `c9e7c128` the size read 250 µm (F4). Since `60b62694` the crystal takes the size of the liquid it froze from, about 0.5 mm, and the "needs" speed rises with it.
- **Expected at `482264fc`** (updated 2026-10-18: D34, S1 to S6, D38). Marked for the rig re-run: the row was last
  replayed on `c9e7c128`, before F2's fix A and F4.
  - (a) slow: "blocked with solid: DEPOSITION (0.25 m/s, needs 1.05 m/s)" (0.02 m/s before fix A: the 0.5 L/s target is
    now converted at the drawn LNG's density). fast: as above.
  - (a) The trace crystals keep 250 µm: the LNG's solid share φ is about 0.004, under the size rule's floor.
  - (b) slush: the size is 0.25 mm × 4.04 (φ/(1−φ))^0.6 = about 0.52 mm at φ about 0.25. The outlet still reads
    PACKED_SLURRY at a share of 0.553; its "needs" figure becomes about 1.9 m/s (Ferguson-Church, ×10; F6: it does not
    apply to that reason).
  - D34: while the tank holds under 2 L of gas (the fill, the first seconds of venting), the top vent's 1 m line reads
    mostly liquid, about 4 kPa where it read the gas's 20 Pa.
  - F5 (the fill's line-search stall): rule C now folds a vanishing trace at a failed iterate. The rig shows whether
    the fill still holds.
- **Rig at `00ee8385`** (2026-10-18; the 0.25 m/s and 0.52 mm predictions above do not hold):
  - (a) slow: 120.69 K, 655 kPa, 2.30 kg of dry ice at 250 µm. The bottom line reads "blocked with solid: DEPOSITION
    (0.02 m/s, needs 1.05 m/s)", as on `c9e7c128`. fast: the line opens, and the pump passes 2.2007 kg/s with 0.0765 kg
    of crystals per 5 s. The tank keeps 2.28 kg at 120.70 K. No hold.
  - (b) fill: HELD from committed 185.7 s, "HELD: Substep refinement exhausted: Newton line search stalled at residual
    0.8996406859027278; active-set pass=3; …" (F5). The tank is at 157.32 K and 1.261 MPa with 261.0 kg of dry ice at
    330 µm.
  - (b) vented and slush: the cut does not clear it. The new island holds for good from committed 195.6 s, "HELD:
    Interval substep limit; no partial interval may commit: advanced=0.0201818842122738 of 0.1 s, accepted=490,
    rejected=534, …" (H3). The tank stays liquid-full at 172.76 K and 2.576 MPa with 343.7 kg of dry ice at 315 µm.
    The top vent reads 117.9 kg/s, at its cap. The bottom outlet's island repeats the hold, so the outlet shows no
    PACKED_SLURRY line.
- **Decision.** D23 (2)-(4); the slush threshold.

### 10. R1: water into a 120 K methane tank freezes; the data-range notice (`rows/10-R1.json`, z = 96..98) (updated 2026-10-17: range work / ice)
- **Set-up.**
  - Methane at 120 K, 1.2 MPa flows through a tank to a void (pipes 1 m, 5 cm).
  - Water at 25 °C is fed through P1 at 1 atm, then at 1 MPa, then at 1 atm again.
  - Last, the water generator is set to 260 K at 1 atm (the notice check).
- **Physics.**
  - Before water: the tank sits at the hydraulic midpoint, 651 kPa and 120.2 K, liquid-full, with 68 kg/s through it. Water at 1 atm is below the tank and cannot enter.
  - Water at 1 MPa is above the tank and enters. Cooled from 298 K to ice at about 150 K, each mole gives up about 11.6 kJ: 1.9 as liquid, 6.0 as fusion, about 3.7 as ice. The methane takes this up by warming and boiling (8.2 kJ/mol at 1 atm, about 6.4 near 1 MPa).
  - At 651 kPa, 1 MPa would drive 84 kg/s of water: 54 MW. 68 kg/s of methane absorbs at most about 35 MW, even boiling all of it. So the tank boils and pressurises towards 1 MPa, where methane boils at about 149 K. That throttles the water.
  - Balance (order of magnitude, an estimate): about 7 kg/s of water (5 MW) heats about 40 kg/s of methane and boils a few per cent of it. The outlet draws liquid and vapour together.
  - The ice leaves with the liquid; a well-mixed tank holds a few tens of kg of it. A real water nozzle into a cryogenic vessel freezes shut.
  - Back at 1 atm the water stops. The tank returns to 651 kPa and 120 K, and the ice washes out with a time constant of about 6 s (419 kg at 68 kg/s).
  - Water at 260 K is below its 273.15 K melting point: a real feed would be ice, or supercooled water that freezes in the line. At 1 atm it cannot enter.
- **What the model does.** Source: gate `WaterIntoCryogenicMethaneTest` (c). It has the same layout but starts from an empty tank, so expect the pattern, not the numbers. The model alternates over two 5 s intervals:
  - water interval: water enters as the stand-in. The tank ends at 149-164 K and 0.97-1.0 MPa, holding 29-82 kg of stand-in.
  - refresh: all the stand-in freezes. Its +1.25 kJ/mol boils methane: 77 mol for 1597 mol of water at the gate's first refresh.
  - closed interval: the boil-off lifts the tank above 1 MPa, so the water line starts the interval with no flow beside a tank of ice. It closes by deposition for that interval.
    - The tank falls to 129-132 K and 0.34-0.41 MPa, and holds 14-36 kg of ice. Once, after 82 kg froze, it reached 160 K and 1.6 MPa.
    - Over the gate's six intervals the liquid methane fell from 7623 to 5904 mol, and the vapour rose from 612 to 1087 mol.
  - At `482264fc` (updated 2026-10-18: D34, S1 to S6, D38). The gate's output at `67e5f3e7` is archived in
    `tools/unified-thermo-port-m5/d38/logs/d38-logs.tar.gz`. Rule C moved intervals 5 and 6; the other intervals print
    as above.
    - Interval 5: the vessel's methane vapour (1.56 mol at the step start) leaves at a failed iterate. The interval ends
      at 164.3 K and 970 kPa with 82 kg of stand-in, in 40 substeps with 16 rejected (43 and 19 before).
    - Interval 6: 160.6 K and 1.65 MPa with 37.6 kg of ice (36.1 kg before).
    - So the closed intervals hold 14.4, 21.2 and 37.6 kg of ice. The liquid methane falls from 7623 to 6201 mol, and the
      vapour rises from 612 to 1085 mol. P1's "needs" stays 0.85 to 1.05 m/s.
    - Marked for the rig re-run: this row was never replayed (§4).
- **Expected.**
  - atm-water: 120.2 K, 651 kPa, 419.2 kg, liquid-full, "Ready.". Outlets: west (water) "no flow"; east "receiving" 68.09 kg/s; north "drawing all phases together" 68.09 kg/s.
  - ice-a and ice-b (1 MPa; T and P1 read about 15 s and 25 s after the set). The two reads usually land in different halves:
    - water half: the Aqueous tab shows tens of kg of liquid water, the stand-in. The status block reads "Ready.; outside data range: liquid water extrapolated below 273.16 K at reservoir at 22, 101, 98 (+1 more)". The "+1" is the steam trace.
    - closed half: the Solids tab shows "Water (solid)", 14-38 kg at 250 µm (14-36 kg before rule C; updated 2026-10-18: D34, S1 to S6, D38). P1 reads "blocked with solid: DEPOSITION (0.00e+00 m/s, needs 0.85 m/s) / …", with "needs" between 0.85 and 1.05 m/s.
    - closed half status: "Ready.", or "Ready.; outside data range: water vapour extrapolated below 273.16 K at reservoir at 22, 101, 98" while a steam trace is left.
    - The Vapor tab shows the boiled-off methane. There is no HELD at any read.
  - washed (1 atm, +30 s): about 120 K and 651 kPa again.
    - Ice residue below 1 kg, gone by the next snapshot.
    - P1 shows the DEPOSITION line while ice is left, then no flow.
  - notice (260 K, 1 atm, +10 s): the tank's status block reads exactly "Ready.; outside data range: liquid water extrapolated below 273.16 K at generator at 20, 101, 98".
    - Why: the generator's own state is the stand-in, and the generator is the island's first node.
    - Muted colour, not the warning colour. The `rail-end` view shows the block if it falls below the rail.
    - Gw's overview ends its top status line with the same notice. The line is cut to its width; it reads in full on hover.
  - log: no PHASE_HOLD line and no HELD line.
- **Rig at `00ee8385`** (2026-10-18; `logs/2026-10-18-session/rig-head-00ee8385.log`; the expectations above from ice-a on
  do not hold):
  - atm-water: 120.21 K, 651.3 kPa, 419.2 kg, liquid-full, "Ready."; east receiving and north drawing 68.09 kg/s.
  - ice-a to notice: HELD for good from committed 83.8 s, "HELD: Crystal equilibrium did not converge: no convergence of
    the UV pressure search at 124.71836321193986 K in 100 TP equilibria" (H4), on the tank, P1 and the generator. The
    frozen state is 125.29 K, 956.2 kPa, 416.8 kg, liquid 990 L, stand-in water 5.7 L, 4.29 kg of ice at 250 µm, with
    28.9 kg/s of water entering through P1.
  - So washed and notice read that frozen state; the WP3 notice check is not reached.
- **Decision.** WP4 ice: the stand-in in steps, freezing at the refresh, the water line closed by deposition. The range work: no hold past a data range, and the notice (WP3's one GUI check). acceptAtFence moved to row 12.

### 11. R6 + R12: two save-and-reload cycles (`rows/11-R6-R12.json`; reads R11, R2 and R4 tanks; breaks the D23-2 slush tank)
- **Cycle 1.** G11 is set to 165 K and the world saved within about 2 s, a best-effort window for a pending event. Expect:
  - "fluid_world status=LOADED format=10 … certificates_restored=…" (updated 2026-10-18: D34, S1 to S6, D38: S6 and S2
    share checkpoint format 10 and unit format 3; the menu protocol stays `fluid-10`);
  - Tb4 (R4, certified at rest) identical before and after;
  - T11 and T2 continuing without a jump. Since fix A, T2 has drained and runs on CO2 gas (row 2's last bullet: 194.7 K
    over frost, or about 248 K once the frost has sublimed) (updated 2026-10-18: D34, S1 to S6, D38);
  - G11 at −108.15 °C after the reload, with the event applied once.
- **Row 12.** Break the slush tank. Its 343 kg of dry ice and its liquid leave with it (the ledger's destroyed totals). The chat data shows only the tank's own block item, not a recovered-solids item.
- **Cycle 2.** The save that crashed in M4. Expect no `NoSuchElementException` (D32). Not replayed by the rig.

### 12. R10 at a fence: a phase hold inside a network, then an edit on the held island (`rows/12-R10-fence.json`, z = 104..106) (updated 2026-10-17: range work / ice)
- **Why this row.** A generator refuses an unsupported phase set at Apply (row 6), so the hold must form where two allowed streams mix.
- **Set-up.**
  - Ethane generator Ge (95 K, 2 MPa) → P1 → tank T → P2 → void V, all side ports.
  - Nitrogen generator Gn (95 K, 2 MPa) stands south of T, not yet joined.
  - After 30 s, P3 joins Gn to T's south face. After the hold, Gn is set to 1 atm (95 K).
- **Physics.**
  - Ethane at 95 K is a subcooled liquid: its vapour pressure is a few Pa, and its density is about 646 kg/m³.
  - Alone, the tank sits at the hydraulic midpoint: about 1.05 MPa and 95 K. About 110 kg/s pass through, at 88 m/s in each 5 cm line (under the 100 m/s cap).
  - Gn's nitrogen is also liquid: its vapour pressure at 95 K is 0.54 MPa, and its density about 720 kg/m³.
  - With two feeds the outlet reaches the 100 m/s cap. The tank rises to about 1.7 MPa, with about 65 kg/s from each feed.
  - Below roughly 130 K, liquid nitrogen and liquid ethane are only partly miscible. The engine finds two liquids for N2/C2H6 0.3/0.7 at 95 K and 2 MPa (`NetworkPhaseEngineTest`).
  - Nitrogen arrives at 2.3 kmol/s into about 21 kmol of ethane. The ethane-rich liquid saturates within a second or two (solubility of order 10 mol %, not computed here). A second, denser, nitrogen-rich liquid then forms: a real tank shows two layers.
  - The network carries one hydrocarbon liquid. This is a capability limit, so the island holds, typed.
  - Gn at 1 atm and 95 K is a gas, below the tank's pressure. A generator takes no backflow, so nothing enters, and the tank returns to ethane alone.
- **Expected.** Not run: replay on the rig before the session (§4).
  - ethane (+30 s): about 95 K, about 1.05 MPa, liquid-full, one liquid (ethane) in the Organic tab, "Ready.". Outlets: west "receiving" and east "drawing all phases together", each about 110 kg/s.
  - held (P3 placed, +15 s): the status block, in the warning colour, reads "HELD (phases): two hydrocarbon liquids, which the network does not carry at 9x.xx K, 1xxxxxx Pa in reservoir at 22, 101, 104 (package createcheme:pilot_cryogenic)".
    - Expect T at about 95 K, and P between 1.05 and 1.7 MPa.
    - The tank keeps its last committed state: ethane only.
    - If the failing trial lies on the three-phase line, the kind reads "a third fluid phase, which the network does not carry" (THREE_PHASES) instead. Either is the capability limit this row needs.
  - The log has exactly one hold line: "fluid_island=<id> dimension=… node=<n> device=reservoir at 22, 101, 104 status=PHASE_HOLD code=phases LIQUID_LIQUID package=createcheme:pilot_cryogenic temperature=… pressure=… waits_for_inputs=false detail=Unsupported phases: PHASE_COMPETITION_NOT_QUALIFIED (LIQUID_LIQUID) at … action=…".
  - parked (+20 s more): after one retry on a halved slice, the line ends "…; the retry failed the same way, so the island waits for a change to its network". Gn's overview shows the same status. The repeat logs nothing: one line per island and hold per 5 minutes.
  - accepted (Gn at 1 atm, +15 s): the edit's fence wakes the parked island (an input change).
    - It retries once towards the fence and, with nitrogen still at 2 MPa there, holds again. That repeat is not logged.
    - Held with nothing in flight, it accepts the edit (acceptAtFence). Its clock moves from the committed tick to the fence without a solve, the tank's inventory standing still (ethane only), and the edit applies.
    - Then "Ready." at about 95 K and 1.05 MPa, with about 110 kg/s; the south face reads "no flow". No new log line. A short "WAITING: event alignment" may show first.
- **At `482264fc`** (updated 2026-10-18: D34, S1 to S6, D38): the expected texts do not change. Still to be replayed on
  the rig before the session (marked).
  - No nitrogen notice: the pilot's nitrogen ideal gas is its spine, from 63.151 K.
  - S5 moves no limit: the liquids' sound speeds are about 1900 m/s (ethane) and 450 m/s (nitrogen), against the 100 m/s cap.
  - S2 removed only the budget fallback. The phase hold's policy is unchanged: one retry on the halved slice, then park.
  - Rule C acts only on vanishing phases. It does not touch the two-liquid split that makes the hold.
- **Rig at `00ee8385`** (2026-10-18; same log): as expected, with two details.
  - ethane: 95.66 K, 1.051 MPa, 635.7 kg, liquid-full, "Ready."; 109.77 kg/s in at the west, out at the east.
  - held: from committed 75.9 s, "HELD (phases): two hydrocarbon liquids, which the network does not carry at 91.98 K,
    1608461 Pa in node 7 (package createcheme:pilot_cryogenic)"; after one retry "…; the retry failed the same way, so the
    island waits for a change to its network". The rig prints the node's index; the screen's device text is for the
    session to read.
  - The committed state at the hold already holds 2726 mol of nitrogen beside 19,330 mol of ethane (92.54 K, 1.607 MPa),
    not ethane only.
  - accepted (Gn at 1 atm): the island accepts the edit and runs: 95.51 K, 1.052 MPa, 109.9 kg/s, south "no flow", 105 mol
    of nitrogen left to wash out.
- **Decision.** acceptAtFence on a held island inside a network (owner, 2026-10-15). The phase hold is the one kind left, a capability limit (AGENTS.md, 2026-10-16). Retry and park.
- **If the rig shows no hold** (the engine answers one liquid): first try Gn at 3 MPa, which gives more nitrogen in the tank.
  - If that still does not hold, report it to the lead. In this world a generator refuses every unsupported set, so a phase hold can only form where streams mix.
  - The evidence then stays with the coordinator tests: `IslandCoordinatorTest#aHeldOwnerAcceptsAFencedEventAtItsCommittedTickWithItsInventoryUnchanged` and `#aReproducedPhaseHoldWaitsForAnInputChangeInsteadOfTheLadder`.

## 2. Read-back common to every row
- The driver's screenshots go to `shots/<row>/`.
- Rail values to read: T (°C), P, fill, mass, solid share; the Solids tab row "Carbon dioxide (solid)" or "Water (solid)" with its size and kg; the outlets page (port, "drawing …", flow, head); the pipe or pump overview with its mode and the "blocked with solid: …" line; the status block (HELD texts as quoted).
- `log` steps print `fluid_island=…` and `status=PHASE_HOLD` lines from `latest.log` since the row's mark. (updated 2026-10-17: range work / ice)
  - A phase hold logs one WARN line per island and hold every 5 minutes, with the status prefix "HELD (phases): ".
  - Any other hold logs `fluid_island=… committed_tick=… status=HELD: …`.
- Data-range notices (updated 2026-10-17: range work / ice):
  - On a tank or a pipe, the notice ends the "Status / errors" block after the translated words, e.g. "Ready.; outside data range: …".
  - On other devices it ends the raw status line at the top, which is cut to its width.
  - The `rail-end` view scrolls a tank's rail to the block.
- Status words (updated 2026-10-18: D34, S1 to S6, D38). The rig prints raw statuses; the screen translates them:
  - "FULL" reads "Ready.";
  - "STEADY: no flow" reads "No flow.";
  - a STEADY status with VELOCITY_LIMIT reads "Flow is limited by the allowed fluid velocity.", and any other STEADY status
    reads "Flowing normally.";
  - HELD, ERROR and WAITING lines read in full. No "APPROXIMATE" status exists since S2.
- Not checkable by the bridge: hover tooltips (they need the real cursor), and the pending-event window, which is best effort.

## 3. Findings from the rig (for the lead; physics first)
- **F1, defect.** A tank receiving liquid CO2, or a stand-in CO2 liquid, holds in its first interval with "CarbonDioxide at 89.99999999999999 K is below 90 K" and parks.
  - Physics: liquid CO2 let down to 1 atm reaches 194.63 K, with 48 % dry ice; nothing in the tank approaches 90 K.
  - Affected: D23-1 (both pipe sizes) and R3 with a 210 K feed (`logs`: `rig-ad33-first.log`).
  - Proposition: trace the tank's first step. The message suggests an iterate reaching the CO2 domain floor that the step refuses instead of bounding; not yet traced.
  - Closed (updated 2026-10-17: range work / ice).
    - Traced in §5: form 4 (`58d9dac4`, `2ab43f23`) proposes the liquid slot again, so the let-down stays at 184.8 K.
    - Since the range work, 90 K is a data end, not a hold: a state below it would run, with a notice.
    - D23-1 (row 8) and R3 at 210 K now run.
- **F2, defect.** A pump on a vented slurry tank never starts (R9).
  - At each interval start the suction line's DEPOSITION test reads the previous, near-zero flow (7e-3 m/s). The pump's persisted INLET_WRONG_PHASE "inlet dry" keeps it off.
  - Physics: at 0.005 m³/s the line runs at 2.55 m/s, which is above 1.02 m/s, and carries the crystals.
  - D23-2a's pump does start, probably because its tank is pressurised (650 kPa) and pushes liquid through the reopened line.
  - Proposition: test deposition on the mover's target velocity.
  - Fix A is on the branch (`ed2cdff4`, §5's proposition); R9 was not re-run (updated 2026-10-17: range work / ice).
  - At `482264fc` the gate `SlurryTankPumpTest` holds fix A: 2.17 kg/s through an open line; a 0.5 L/s target closes the
    line on deposition at 0.25 m/s. R9 and D23-2a are marked for the rig re-run (updated 2026-10-18: D34, S1 to S6, D38).
- **F3, observation.** A tank fed below the frost point runs colder than its feed's equilibrium.
  - R11: 163.6 K against 176.75 K. With a 160 K feed: 144 K against 168.4 K.
  - Its gas leaves supersaturated, and it deposits about 60 % of the equilibrium amount.
  - Cause: the generator's stand-in liquid (4.3 % of the moles at 175 K) evaporates in the tank's steps (D31 B), and the tank turns over in about one refresh period (D23 lag).
  - Owner to validate.
- **F4, observation.** The precipitated size reads 250 µm even in the 30 % CO2 slush, because φ is read from the liquid left after the freeze (about 0.1 % CO2). D23 (1) meant the freezing phase's share, about 0.25, which gives about 0.5 mm, so the deposition limit would rise. Owner to decide. Since `60b62694` the size follows the liquid it froze from (updated 2026-10-17: range work / ice).
- **F5, robustness.**
  - The slush fill holds "Newton line search stalled" for about 10 s. With a 4 mm feed it held for good (`rig-ad33-second.log`).
  - A liquid-methane tank at 150 kPa, opened to top and bottom voids, held "Interval substep limit; thermo-domain: Methane temperature < 90.69 K" (the flow-through R7 layout, same log).
  - Range part gone (updated 2026-10-17: range work / ice).
    - The range refusals counted in both holds ("Methane temperature < 90.6941 K", "Methane pressure < 100 Pa") are now continued trials. Only the numerical box refuses a trial: 10 K, 1 Pa, and since `db6b1e6f` a vapour's partial pressure. A lost cubic root (`8220d4bc`) does too.
    - Not re-run, so two questions stay open: whether the slush fill's line search still stalls, and the R7 layout's 516 "Phase/device active-set cycle" rejections, which the range work does not touch.
  - At `482264fc` (updated 2026-10-18: D34, S1 to S6, D38) rule C folds a vanishing trace at a failed iterate, and S2
    gives every interval the whole 2 s. Both can change the slush fill's stall; the rig re-run of row 9 answers the first
    question.
  - Answered 2026-10-18 at `00ee8385`: the slush fill still holds at committed 185.7 s, and it is the same bubble-point
    crossing as H3 (`TEST_WORLD_RIG_HOLDS_TRACE.md`).
- **F6, GUI nit.** "PACKED_SLURRY (10.15 m/s, needs 1.14 m/s)" shows deposition velocities that do not apply to that reason.
- **H1 to H4, numerical holds that no gate covers** (rig at `00ee8385`, 2026-10-18). They are traced physics first, with
  the bisect and the propositions for the owner, in `TEST_WORLD_RIG_HOLDS_TRACE.md`.
  - H1, row 2, transient. Pure methane at its saturation is restated at a pass start by a TP split: 183 m³ of vapour in
    a 1 m³ tank. The hold toggles with the commit (path sensitivity).
  - H2, row 9 fast, for good. When the side outlet deposits, main's island-wide start rule puts the pump at its head
    limit (`440a7542`). The pump draws 19.76 kg/s and the suction junction falls to the 1 Pa floor. Present from
    `87c106f6` on, and exposed by fix A.
  - H3, row 9b, for good. The liquid-full slush tank's bubble-point crossing: its own closure fails, and the closure's
    last iterate refuses the vapour or relabels the liquid as vapour. Rule C folds nothing in the vent's island and
    re-locks the fill. Held for good already at `87c106f6` and `9445106a`.
  - H4, row 10, for good. The crystal test's UV pressure search has no case for methane's own saturation step beside
    water, and a stale secant stalls it. Port-only (WP4).
  - H1 and H3 share one cause: seeds from TP amounts that ignore the vessel's volume. The in-game session waits for the
    fixes.

## 4. Commands and logs
- Engine probe: `bash index-lane/run.sh <label> com.wormzjl.createcheme.science.fluid.network.TestWorldEngineProbeTest`.
- Rig: `bash index-lane/run.sh <label> com.wormzjl.createcheme.runtime.fluid.TestWorldRigTest "-DtestWorld.rows=$(cygpath -m rows);$(cygpath -m rows-rig)" -DtestWorld.presets=$(cygpath -m mixturepreset)`.
- Compile: `bash index-lane/compile.sh ad33 c9e7c128` and `a 40a44732` (`PROBES_ONLY=1` reuses an export).
- Before the session (updated 2026-10-17: range work / ice):
  - Compile the session's head, e.g. `bash index-lane/compile.sh head 95e4325d`. The probes were checked against the API at `87c106f6` by reading only; they were not compiled.
  - Replay rows 10 and 12: `bash index-lane/run.sh head com.wormzjl.createcheme.runtime.fluid.TestWorldRigTest "-DtestWorld.rows=$(cygpath -m rows)" -DtestWorld.presets=$(cygpath -m mixturepreset) -DtestWorld.only=R1,R10-fence`.
  - Row 12's hold is predicted, not observed.
- Before the session at `482264fc` (updated 2026-10-18: D34, S1 to S6, D38):
  - Compile: `bash index-lane/compile.sh head 482264fc`.
  - `probes/TestWorldTrace.java` was adapted: it now calls the step and rate solves by reflection, since S2 deleted
    `PassiveStepSolver.Acceptance`, and its `phaseCheck` call takes D38 C's `from`. It still compiles on older commits.
    Without the adaptation the probe compile fails at `482264fc`.
  - Rig rows to re-run: `-DtestWorld.only=R2-R7-R9,D23-2,R1,R10-fence`. Rows 2 and 9: F2's fix A, F4, D34 and rule C;
    row 10: never replayed, and its gate moved with rule C; row 12: never replayed.
  - The engine probe need not run: no TP or PH answer of the pilot package moved (S4 changed only the bundled nitrogen
    record).
- Logs: `logs/testworld-logs.tar.gz` holds four rig logs on A + D33 (the last one is final), one on form A, two engine logs, and their condensed views.
- Rig re-run and attribution, 2026-10-18 (logs in `logs/2026-10-18-session/`, the large traces packed in
  `h-traces.tar.gz`; switches and labels in the tool README):
  - At HEAD: `bash index-lane/compile.sh head 00ee8385`, then the rig with
    `-DtestWorld.only=R2-R7-R9,D23-2,R1,R10-fence,R2-tail,D23-1` (`rig-head-00ee8385.log`).
  - Attribution: the same rig with `-DtestWorld.only=R2-R7-R9,D23-2,R1` at `482264fc`, `26090bf0`, `65036f2b`,
    `eec02758`, `9445106a` and `87c106f6` (`rig-pre-d39-482264fc.log`, `rig-at-*.log`, `rig-base-87c106f6.log`);
    `c9e7c128` from `logs/testworld-logs.tar.gz` (`rig-ad33.log`).
  - Traces: the M5, M8, M9 and M10 hooks on the exports only (`PATCH=`, `EXTRA_PROBES=`;
    `tools/unified-thermo-port-m5/trace/README.md`), with `-DtestWorld.m5=ROW@from-to` and
    `-DtestWorld.history=ROW@from-to`. H4 uses `rows-rig/R1-h4.json` with the `h4` trace.

## 5. F1/F2 bisect (2026-10-16)
- **F1 is a regression; the first bad commit is 40a44732 (D31 B).** Row D23-1 at 5 cm runs at 76e87740 and at its parent 91a2c14a (the tank floods with liquid CO2 at about 240 K and 1.3 MPa). It holds at 40a44732, and runs again from the form-4 head 2ab43f23 on. The later head 81129851 was not run; its diff does not touch `phaseCheck` or `lacking`.
  - Physics: the first interval gives the expected layout, vapour plus a stand-in liquid at 184.81 K (Peng-Robinson's extrapolated saturation at 1 atm). 55.4 mol of stand-in carry the frost share beside 33.2 mol of vapour. The next refresh freezes the stand-in to 1.69 kg of dry ice, leaving 50.1 mol of vapour at 194.63 K and no liquid slot.
  - With the stand-in's vote gone, `phaseCheck` proposes no liquid at any temperature from 194 K down to 91 K. The arrival (1771 mol/s, 10.9 kJ/mol below that vapour) can then only cool the vapour, which holds 3.3 kJ/mol of sensible heat down to 90 K, so 22 mol (12 ms of flow) take it to the floor. With a liquid slot, the PH answer stays at 194.63 K.
  - The coordinator's estimate holds: 184 K, with the stand-in missing from the layout. D31 A's seed and §1's refusal are cleared. Form 4's `lacking` (58d9dac4) proposes the liquid again. At 4 mm, D23-1 then reads 184.84 K and 101.5 kPa, with 3.55 kg of dry ice in 7.34 kg (48 %) and the vent passing 0.153 of 0.283 kg/s. Proposition: keep form 4 as the fix and gate D23-1 at both pipe sizes. Its stand-in sits 9.8 K under the frost point, as in F3.
- **F2 is pre-existing on the port** (76e87740, c9e7c128, 2ab43f23). It does not occur with water plus demo particles, neither on main ffeafe8a nor on the port at 76e87740: the pump runs 5.73 kg/s at 0.005 m³/s. At 0.0001 m³/s the line deposits, rightly (0.04 m/s against 0.08 m/s).
  - The DEPOSITION test's flow comes from `SolidEventIntegrator.solve`'s t0 `portRate`, with the pump free. Its PUMP_TARGET row converts 0.005 m³/s at the pump junction's density. That junction still holds the 298.15 K N2 placeholder (1.145 kg/m³), which the mint (`fixedPointStarts`/`supplierSeeds`) leaves in place. The result is 0.0061 kg/s instead of 2.24 kg/s: 6.9e-3 m/s against the 1.016 m/s required.
  - "Inlet dry" persists through `PassiveIntervalSolver.decideInlets` → `InletPhase.decide` on the committed graph. `held` does not cross the suction's DEPOSITION closure (`boundaryAllowed`), so the held share is 0, below DRY_RESUME. With the closures lifted, the inlet is not refused. Why the mint skips the LNG stream is open; the suspect is `seedAt` swallowing a flash failure.
  - Proposition: seed a mover's junction from its supplier's stream, so the target row reads the drawn liquid's density (447.6 kg/m³, 2.55 m/s in the bore). Take the slice's inlet decision with the interval-start closures read as open, as D33 does for transport.
- Lane `index-lane/onejvm.sh` (one JVM per commit, 8 runs), rows in `rows-bisect/` and `rows-main/`, logs in `logs/bisect-logs.tar.gz`. Run 6 was spent: main's package refuses methane below 293.15 K.
  - On the port since the range work (updated 2026-10-17: range work / ice), the bundled cold rows run.
    - The bundled package's envelope, 63.151..900 K, admits methane at 115 K.
    - Its methane ideal-gas and vapour-viscosity data, which start at 293.15 K, are extrapolated with a notice: "; outside data range: Methane ideal-gas heat capacity extrapolated below 293.15 K at … (+N more)".
    - Main (`ffeafe8a`) still refuses. Not re-run. The rig runs on the pilot package only, so this needs the rig on the bundled package; it is not part of the session.
    - The count in "(+N more)" depends on the row's phases and components. The vapour viscosity's end is one of them.
