# Floating-roof tanks and storage spheres: plan

**Batch:** `2026-10-21-floating-roof-tanks` · **Branch:** `claude/floating-roof-tanks-81d68b` from `main` `ef64bd06` (0.11.2) · **Status:** Planned 2026-10-21; the 3D model references are waiting for the owner's approval (section 8) · **Owner request (2026-10-21):** add scalable floating-roof tanks to the equipment list and make them valid fluid reservoirs. The roof floats on the liquid level. A sight glass on the shell shows the liquid's colour. The tank has a maximum volume, and its volume follows the liquid level. It degasses by itself and keeps only the liquid. Concern: the impact on the fluid network solver.

**Owner decisions (2026-10-21, second message):**
- Version **0.11.3**.
- FR1–FR8 as recommended in section 5.
- The tank is not designed for volatile compounds. No gate, test or tuning targets a volatile feed (section 6).
- Add a **spherical storage vessel** for bulk gas storage. It is identical to the drum except for its 3D model and its ports, which are fixed (section 3.5).
- Show reference photos for approval before the 3D modelling starts.

**Owner approval of the concept sheets (2026-10-21, third message):** "Both should have stair around them, others looks ok."
- Each vessel gets one stair wound round it. On the tank, it runs round the shell from grade to the gauger's platform. On the sphere, it runs outside the legs below the equator, then over the upper hemisphere to the top platform.
- Everything else on the sheets is approved as drawn: pontoon ring, roof legs, wind girder, sight glass, the tank's nozzles, the sphere's legs and tie rods, its two fixed ports, and the grids.
- Sheets: `figures/floating-roof-tank-concept.{svg,png}`, `figures/storage-sphere-concept.{svg,png}`.
- Owner change (fourth message): the tank has two identical bottom ports, with no water/organic distinction, and a top overflow port. "Start the work."
- Real photos and proportions: `REFERENCE_PHOTO_SURVEY.md` (ten Commons photos, licences checked). Its drawing-only refinements are taken: wind girder 1.2 m below the top, rolling-ladder length equal to the fill height, sphere legs 0.05 D wide, two relief valves with 2 m vertical stacks.

## 1. Expected behaviour (real-life physics, simplified)

- **Pressure.** The roof floats on the liquid, so the tank has no vapour space. The liquid surface stands at the ambient pressure plus the roof's load. A steel pontoon roof weighs about 60–90 kg/m², which is 0.6–0.9 kPa, or 7–11 cm of oil at 850 kg/m³.
- **Level head.** The tank is a flat-bottomed vertical cylinder. A bottom nozzle sees `ρ g h` with `h = V_liquid / A`. This equals the solver's `g m_c H / V` exactly when `H` is the fill height at `V_max` (not the shell height).
- **Scope (owner decision).** The tank is designed for stabilised liquids: crude, products and water, with a true vapour pressure below 1 atm. Volatile liquids (LPG, light ends) belong in pressure storage such as the sphere (section 3.5).
- **Degassing.** Gas that arrives with the liquid (entrained nitrogen, blanket or stripping gas) separates at the surface and leaves through the rim and the roof vents. A stabilised liquid itself loses nothing. The steady state is a gas-free liquid at ambient pressure. A volatile feed would vent and cool by the same rule, but it is outside the design and is not tested.
- **Standing losses** (rim seal, wind) are of the order of tonnes per year on a 30 m crude tank, under 0.1 % of the stock a year. Leave them out.
- **Maximum volume.** At its top position the roof stops. A real tank at that point overflows through shell slots, or a level trip closes the inlet. It never holds pressure: an atmospheric shell fails at a few kPa over.
- **Low level.** Below about 1.8 m the roof lands on its legs. This is drawing only: the space under a landed roof vents to ambient too.

## 2. What the solver does today

Every vessel is already a bag with a stiff wall (owner decision D54, [`PhaseLayout.wallRow`](../../src/main/java/com/wormzjl/icheme/science/fluid/solver/PhaseLayout.java)): below `V_max` the pressure is the ambient 101 325 Pa and the volume is the contents' own volume; at `V_max` the contents set the pressure. A partly filled drum is therefore already a "floating roof" with one difference: it keeps its gas at ambient pressure in the bag. The existing pieces cover the rest:

- The level head over the vessel's own height: `Inventory.levelHeight`, `PassiveStepSolver.portHead`.
- Drum registration as a reservoir with a port per nozzle: [`EquipmentStructure.register`](../../src/main/java/com/wormzjl/icheme/world/level/block/EquipmentStructure.java), `FluidWorldAuthority.placeVessel`.
- Exact booking of removed material as a boundary transfer: [`PassiveIntervalSolver.flushTraces`](../../src/main/java/com/wormzjl/icheme/science/fluid/network/PassiveIntervalSolver.java).
- A phase drawn at its capacity at the step-start state (D11, [`PhaseDraw`](../../src/main/java/com/wormzjl/icheme/science/fluid/network/PhaseDraw.java)).
- A colour per component in the data (`appearance`, all 22 components, [`MaterialCatalog.FluidAppearance`](../../src/main/java/com/wormzjl/icheme/science/material/MaterialCatalog.java)). Nothing reads these colours today.

What is missing: a vessel that sends its gas to the atmosphere, a top that does not pressurise, sizes above `FluidDeviceSpec.MAX_VOLUME` (10 000 m³), and in-world drawing of process state. The block entities sync no process data today ([`FluidDeviceBlockEntity.getUpdateTag`](../../src/main/java/com/wormzjl/icheme/world/level/block/entity/FluidDeviceBlockEntity.java)).

## 3. Design

### 3.1 Science: the open vessel (`science/fluid`)

- `PassiveNetwork.Inventory` gets an `open` flag beside `levelHeight`. It is part of the vessel's identity (`equals`, `hashCode`). `Inventory.with` keeps it. Every rebuild site listed in the code guide goes through `with`, and a test proves that a solve keeps the flag, as `LevelHeadTest` proves it for the height.
- **Degassing, form B (recommended): the vent as a one-step-lagged draw.** At each step start, an open vessel's gas phases (gas and water vapour) leave over the step at their start composition and enthalpy: `m_v,start / dt`, the way D11 books a pinned phase. The step's balance rows get constant terms only: no unknown, no Jacobian entry, no closure. The gas that flashes during the step leaves in the next step. While the tank degasses, its gas phase stays in its layout, so the phase set does not change every step. When the feed stops, the last gas leaves in one step and the phase vanishes by the existing rules. While gas is held, the tank is a supplier with gas beside its liquid, which the existing suction-floor rule (`suctionFloors`) reads under its own conditions. This matches the physics: a liquid at its bubble point gives only its static head as NPSH. WP1 checks which pump arrangements the rule covers.
- **Degassing, form A (the comparison):** remove the gas after each accepted step, where `flushTraces` runs. The committed state never holds gas. But each step starts at the bubble point, so a degassing tank needs a vapour proposal and a second pass every step. Starting from the bubble point is a known delicate state (`aReceiverLandingOnItsBubblePointIsNotProposedAVapourBelowItsResolution`).
- **Top of the tank (FR2):**
  - O2 (taken): the open vessel's wall row is its slack branch only, `P = P_ambient`. The row count stays the same. An open tank is never taut, so the stiff liquid-full state (bulk modulus about 1–2 GPa: 1e-4 overfill gives 100–200 kPa) cannot arise.
  - **Overflow port (owner change, 2026-10-21, fourth message).** The overflow nozzle stands at the fill height `H`, and the shell top is 1 m (`FREEBOARD`) above it. The liquid in that band, between `V_max = A H` and `V_top = A (H + 1)`, leaves only through the overflow port. It is drawn on a new phase-port class, OVERFLOW: the liquid held above `V_max` at the step start is its capacity, pinned as D11 pins a phase, lighter liquid first (it skims the surface). Below `V_max` the port draws nothing, so the port is one-way for outflow; inflow through it is delivered as through a bulk end (D3). The liquid above `V_top` (over the shell top) leaves over the step by the same lagged draw as the vent, as a spill out of the world. An unconnected or blocked overflow line therefore ends in a spill once the band is full, as a real tank overtops.
  - O3: slack-only wall plus a per-slice inflow trip with hysteresis (refuse at 100 %, resume at 98 %), on the `InletPhase` pattern.
  - O1: taut like a drum. No code, but not physical.
- **Booking.** Vent and spill go to `ConservativeTransport` as boundary transfers at the vessel's node. `BoundaryTransfer` gets a kind (FLUSH, VENT, SPILL), so `checkConservation` audits them and the certificate can tell a rate from a flush ([`IslandCertificate`](../../src/main/java/com/wormzjl/icheme/runtime/fluid/IslandCertificate.java) line 96 now treats any material transfer at a vessel as a one-time flush). Without this, a steadily degassing tank would never certify STEADY.
- **Rest.** `NetworkReduction.atRest` is false for an open vessel that holds gas or liquid above `V_max`.

### 3.2 Runtime (`runtime/fluid`)

- `FluidDeviceSpec` and the registration carry `open`. `placeVessel(node, ports, volume, levelHeight, open)`. `MAX_VOLUME` goes from 10 000 to 30 000 m³ (FR6).
- `FluidCheckpointCodec` format 12 → 13, unit format 5 → 6: the flag goes in the topology registration and in each island node. Older formats are refused (fresh world, AGENTS.md).
- Status line: `; venting x kg/s`, `; overflowing y kg/s`. These are non-blocking notices, like the data-range notice.
- `MenuData` (protocol `fluid-10` → `fluid-11`) adds level, fill %, the vent rate and the spill rate, taken from the interval's boundary transfers. The world ledger books vented and spilled material as destroyed, like a void's intake (FR8).

### 3.3 Equipment (`science/equipment`, `world`)

- `Kind.TANK` goes before `EXCHANGER` (`Kind.unit()` is ordinal-based). Add `Form.FLOATING_ROOF_TANK`. The new `Form` ordinal needs protocol `equipment-9` → `equipment-10`.
- Grid (FR6): diameter 4–40 m in whole metres, shell 4–24 m. Fill height `H = shell − 1 m` (the roof's own depth). `volume() = π D²/4 · H`, `levelHeight() = H`. Sizes run from 38 m³ to 28 900 m³.
- Nozzles, fixed (owner change 2026-10-21, fourth message; no player side connections):
  - N1 and N2: two identical bottom ports on the front bottom course, both LIQUID (FR5). They have no water or product roles.
  - N3: the overflow port, on the front at the fill height `H` (the shell top less 1 m), class OVERFLOW (section 3.1).
  - Pipe blocks keep one free block between them (`Nozzle.crowds`, `pipesApart`).
- Layout:
  - **Shell cells (chunk mesh):** flat bottom on a ringwall, shell courses as body classes, top angle, wind girder with walkway, a stair wound round the shell to the gauger's platform (owner 2026-10-21), sight-glass column.
  - **Floating parts (drawn by the client, not in the chunk mesh):** the roof (pontoon ring, deck, rim seal, legs) and the rolling ladder.
- Interior cells: full collision as a drum's (no entry), but transparent to light and non-occluding, so the roof shows from above. The interior block is shared with drums, so a tank uses its own interior block or a state of it. A walkable roof needs process-driven collision; that is out of scope.
- `EquipmentStructure.find` walks at most 17 cells over interior cells. Widen it so that a click deep inside a wide tank finds its shell.
- Measure the largest build: about 30 000 interior blocks in one tick. Above 0.5 s of server time, the grid stops at 32 m.

### 3.4 Client

- The renderer follows `MachineAnimation`: one vertex buffer per tank shape and per part, drawn within `RANGE` and in view.
  - The roof is translated to `max(h, legs)`.
  - The rolling ladder turns about its platform pivot, with its foot on the roof track.
  - The sight glass shows two bands: free water at the bottom, hydrocarbon liquid above, with heights from the phase volumes over `A`.
- Data path (FR4):
  - R-B (recommended, keeps the AGENTS.md rule as written): the client sends a watch request for each tank whose node cell is loaded within `RANGE`, capped at 64 per player. The server replays the cached display snapshot and then sends one on the island's bucket. Unwatch, or leaving range, ends the subscription.
  - R-A: draw the roof in the chunk mesh as cell states (block-quantised to 1/8 m), synced by vanilla to every tracking client. The roof becomes walkable, but this amends the rule.
  - The payload is about 24 bytes: fill, water share, and two colours with their transparency.
- Colour (FR7): the server mixes the liquid's component colours subtractively (Beer–Lambert: per channel `T = Π T_i^{w_i}`, `w_i` the mass fractions). Free water takes water's colour. Clients have no datapack.
- Tank screen and datasheet: level `x m of H m (z %)`, liquid volume, venting, overflowing.

### 3.5 Spherical storage vessel (owner request, 2026-10-21)

- **Physics: the drum's, unchanged.** It is a RESERVOIR registered through `EquipmentStructure.register` → `placeVessel`, as a D54 bag with a stiff wall (no `open` flag). At `V_max` the contents set the pressure, so it holds gas under pressure.
  - Level head over `H = D`, as for a lying drum: `g m_c D / V` is exact at half full.
  - No new solver code. The sphere adds nothing to section 4.
- **Kind and form.** `Kind.SPHERE` goes before `EXCHANGER`, and `Form.SPHERE` is new, in the same `equipment-10` bump as the tank.
- **Grid (proposal):** diameter 4–24 m in whole metres; clearance under the sphere (`support`) 2–8 m, default 2 m. Volume `π D³/6`, from 33.5 m³ to 7 238 m³, inside `MAX_VOLUME`.
- **Fixed ports** (the drum's two fixed outlets; no player side connections, and `connections` must be empty):
  - N1, VAPOUR: at the top pole, flange facing up above the top platform. This is the drum's top vapour outlet.
  - N2, LIQUID: from the bottom pole. The line drops, turns to the front, and ends at the front edge of the box on the lowest row, as a skirted drum's bottom outlet leaves through its skirt. In the network both ports pass flow both ways.
- **Structure** (proportions to be confirmed from the approved photos, section 8):
  - column legs tangent to the shell at the equator, about one per 4.5 m of equator, an even count of at least 4 [unc];
  - crossed tie rods between adjacent legs below the equator;
  - a stair wound round the sphere (owner 2026-10-21): it starts at grade beside a leg, runs outside the legs below the equator, and continues over the upper hemisphere to a ring platform round the top nozzle. The platform has a handrail, two relief valves and a manway.
  - Interior cells as a drum's.
- **Gates:**
  - `ShapeRasterizerTest` sphere rows: grid, volume, ports by position, `pipesApart`, climbs with rests within 9 m, every size;
  - the registration (ports, volume, level height) in `TankReservoirTest`.

## 4. Impact on the fluid solver

| Piece | Rows / unknowns | Active set | Reuses | Risk |
|---|---|---|---|---|
| Liquid below `V_max` | none | none | slack branch (D54), the commonest vessel state in the game | none |
| Degas, form B | constant terms in the vessel rows | none; the gas stays laid out while degassing | D11 pinned draw, boundary transfers | one-step lag: gas held = generation × dt (1 kg/s × 20 s ≈ 30 m³ at 1 atm) |
| Degas, form A | none (post-step removal) | vapour proposal + second pass each degassing step | `flushTraces` | bubble-point start every step |
| Top, O2 | wall row reduced to its slack branch (same count) | the taut branch is gone for open tanks | the same lagged draw | overshoot = net inflow × dt (0.1 m³/s × 20 s = 2 m³; 0.02 % of 10 000 m³, 1.4 % of a 6 × 6 m tank) |
| Top, O3 | as O2 | new per-slice closure with hysteresis | `InletPhase` | dead-headed upstream lines |
| Certificate | vent/spill read as rates | none | linear replay already scales boundaries | without it: no STEADY |
| Up to 30 000 m³ | scales only | none | none | check `HOLDS_NOTHING` (0.029 m³), the `STATE_CAP` reference (2.6e7 kg) and energy scales on the largest tank |

The feature is opt-in per vessel. Gate: in a world without an open tank, every result is bitwise unchanged (chain-100 exact, the 33 junction lines, `MIXED_GAS_COST`, the quick check's counters).

## 5. Owner decisions

Decided 2026-10-21: every row as recommended, and version 0.11.3 in place of 0.12.0.

| Id | Question | Recommendation (taken) | Alternatives |
|---|---|---|---|
| FR1 | Degas form | B, chosen on the quick check against A | A |
| FR2 | At `V_max` | O2 overflow spill (physical, removes the stiff state) | O3 inlet trip; O1 pressurise |
| FR3 | Roof load (0.6–0.9 kPa) | leave out (< 1 % of a 10 m oil head, 83 kPa) | a constant 0.75 kPa on the slack branch |
| FR4 | In-world data path | R-B watch request | R-A roof as world cells |
| FR5 | Nozzle classes | all LIQUID: pumps never draw the lag gas; free water leaves first | shell BULK + drain LIQUID |
| FR6 | Grid, `MAX_VOLUME` | D 4–40 m, shell 4–24 m, 30 000 m³ | stop at D 32 m |
| FR7 | Colour mix | subtractive by mass fraction | linear average |
| FR8 | Vented/spilled material | leaves the world, booked, with a notice | a vent nozzle to vapour recovery (later) |
| — | Version at merge | owner: **0.11.3** | — |

## 6. Work packages

1. **WP1 Science.** The flag, form B (and form A behind a test switch for the comparison), O2, booking kinds, `atRest`. Gates in `science/fluid/network/OpenTankTest`, on non-volatile cases only (owner decision):
   - water carrying nitrogen: the nitrogen vents and the water stays at ambient pressure, free of gas within one step of the feed stopping;
   - a stabilised crude vents nothing at ambient;
   - at `V_max` the pressure stays ambient and the steady spill equals the net inflow;
   - head = `ρ g h`;
   - a solve keeps the flag;
   - every case passes the conservation audit.
2. **WP2 Runtime.** Spec, registration, codec 13/6, `placeVessel`, `MAX_VOLUME`, notices, `MenuData`, ledger. Gates: a degassing tank certifies STEADY and replays its vent (`runtime/fluid/OpenTankRuntimeTest`); the codec round-trips the flag (`TankReservoirTest`, like `DrumReservoirTest`).
3. **WP3 Equipment.** Starts after the owner approves the model references (section 8).
   - Tank: kind, form, grid, volume, level height, nozzles, layout, interior cells, `find`, constructor rows, datasheet.
   - Sphere (section 3.5): kind, form, grid, fixed ports, legs, bracing, stairway, top platform.
   - One `equipment-10` bump for both.
   - The wound stair is one kit piece in `Access`, shared by the tank (a cylinder) and the sphere (a surface of revolution), and placed by its start angle, slope (about 40°) and stand-off. Its collision is walkable at 1/8 block: 0.25 m treads and rises, within the player's step height. It is checked by the walk test the stair towers already pass. The 9 m rest rule applies to ladders, so the stair needs no landings.
   - Gates: `ShapeRasterizerTest` tank and sphere rows (grid, volume, ports by position, `pipesApart`, every size) and the largest-build timing. Software renders of both shapes go to the owner before the in-game check.
4. **WP4 Client.** Data path (FR4), renderer, colours, screen lines. One in-game check through the `runMcpClient` lane: roof at three levels, ladder, sight glass with water and oil, GUI lines.
5. **WP5 Measurement and close** (lean rule):
   - Bitwise gate as in section 4.
   - The hold-fuzz generator gets an "open tanks" option (one-block open reservoirs, bundled distribution only; the cold distribution's LNG and CO2 feeds are volatile and outside the design). Forms A and B on the 200-seed quick check; choose by clean count and Newton counters. A hold that comes from a volatile feed reaching an open tank is reported and not fixed, unless the owner asks.
   - Spheres need no campaign: their solver path is the drum's.
   - One full campaign (1000 seeds, 8 workers) of the chosen form; trace at most the two smallest new holds.
   - One final lane.
   - Code guide (equipment, fluid-science, fluid-runtime, client-gui, verification), `CHANGELOG.md` entry under `[Unreleased]` (0.11.3 at the merge), `mod_version` 0.11.3, tooling cleanup.

WP1 and WP3 touch disjoint files and can run in parallel. WP2 needs WP1's flag. WP4 needs WP2 and WP3.

## 7. Open risks

- The sight glass shows a well-mixed tank: two layers, no stratification.
- A tank partly in unloaded client chunks draws its roof only while its node cell is loaded (R-B).
- Large tank farms: watch requests are capped at 64 per player. The bucket sends about 24 bytes per tank every 5 s.
