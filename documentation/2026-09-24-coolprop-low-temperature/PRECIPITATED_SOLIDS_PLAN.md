# Precipitated solids: size, blocking and the effect on the solver

**Batch:** 2026-09-24-coolprop-low-temperature · **Written:** 2026-10-15 · **Status:** decided 2026-10-15 (`DECISION_LOG.md` D23): as recommended, except decision 5, where precipitation is evaluated only in reservoirs (section 5). Amended 2026-10-15 (owner): one size per solid in each stock, no size distribution, for all solids (section 7, option A)

**Asked by the owner (D22, 2026-10-15).**
- Crystals are solids, of two kinds: formed by a phase transition, or custom.
- Particles in a vessel are homogeneous; every tank is well mixed and nothing settles in it (tank agitation is a later batch).
- Precipitation during transport blocks the pipe. (Replaced 2026-10-15 by the owner: precipitation is evaluated only in reservoirs; section 5, decision 5.)
- The formed particle size varies with the phase fraction of a freezing liquid, because pipe settling depends on size, densities, the pipe and the fluid.
- "Need to learn about how it affects the current solver."

**Inputs:**
- [main-adaptation/PRECIPITATED_SOLIDS_RESEARCH.md](main-adaptation/PRECIPITATED_SOLIDS_RESEARCH.md): particle sizes; Brauner's droplet size against the dispersed fraction.
- [main-adaptation/SOLID_SYSTEM_AND_CONVERGENCE.md](main-adaptation/SOLID_SYSTEM_AND_CONVERGENCE.md): main's solid system and the solver impact, with file and line.

Nothing was compiled or run. The main-code claims below were spot-checked against `ffeafe8a`.

## 1. In short

- **The island Newton does not change.** Crystal mass moves only in the vessel refresh that main already runs at the start of each interval and of each solid-event segment. Inside a step a crystal is an ordinary particle population. Main's Newton already ignores particle size: its three solid unknowns per node are mass, volume and heat capacity.
- **Particle size acts only at outlets,** through main's existing mobility check (sections 2 and 3). The check runs between steps, never inside the Newton.
- **One size per solid in each stock** (section 7). The refresh merges a stock's populations of one solid at their mass-weighted mean size. There is no size distribution for now.
- **On the bundled packages nothing moves.**
  - They have no crystal-forming species.
  - The merge changes only a stock holding two sizes of one solid. The hold fuzz never builds one, since it places no solids. Main's tests use one size per solid, except the key-identity rows of `SolidConservationTest`.
  - The hold-fuzz class tables are expected to be identical, and one quick check confirms it.
- **On cold-package worlds, the expected new holds are class C and class F.**
  - C is the thermo-domain wait: the cold package's data floors, and near-pure CO2 on its sublimation line, which D21 now answers.
  - F is the cost budget: islands holding solids never rest, and solid intervals have no approximate fallback.
  - The Newton classes (E1 to E10), A, B, D, G and X are expected to stay at zero or near it.
  - Decision 6 below removes most of the F cost.

## 2. What happens in real life, in numbers

- **A minority liquid freezes drop by drop.** Each particle is one drop, so the particle size is the drop size. Drops grow as the dispersed fraction phi rises, because they coalesce more (Brauner 2001). Relative to the dilute size: 1x up to phi = 0.09, then 1.8x at 0.2, 3.2x at 0.4 and 4x at 0.5.
- **A majority liquid freezes into a slush,** which stops flowing once solids fill about half of the condensed volume.
- **A solute (CO2 from liquid methane) crystallises as fine particles,** about 0.1 to 0.3 mm. **CO2 frost from a gas** has no liquid to carry it.
- **In a pipe, a particle stays suspended only above a velocity** set by its size, the solid and liquid densities and the liquid viscosity. Main's law requires these velocities (m/s):

| Particle | CO2-I in LNG | CO2-I in a CO2-rich liquid | Ice in light oil |
|---|---|---|---|
| 0.1 mm | 0.33 | 0.09 | 0.003 |
| 0.25 mm | 1.0 | 0.34 | 0.02 |
| 1 mm | 2.8 | 1.1 | 0.22 |
| 4 mm | 5.8 | 2.3 | 1.1 |

So CO2 crystals in LNG drop out of slow lines, which is one reason LNG plants remove CO2 to ppm levels first. Ice in oil keeps flowing until the slush stage.

## 3. The rule

1. **Where crystals form.**
   - The vessel refresh (`InventoryEquilibrium.refresh`, run at each interval and segment start) decides crystal presence with the engine's crystal test, D21's pseudo-pure step included. It closes the amount as one more unknown of the vessel closure, with the crystal's chemical potential equal to its component's.
   - Inside a step the crystal is frozen: an inert population moved by flow like any particle.
   - Energy: populations keep main's U = C (T − 298.15). The refresh adds the crystal's constant offset on the fluid datum.
2. **Size: one per solid in each stock** (amended 2026-10-15, section 7; no size distribution for now).
   - Newly precipitated mass takes d = d_dilute · max(1, 4.04 (phi / (1 − phi))^0.6).
   - phi is the crystal component's volume share of the vessel's liquids, capped at 0.5. It is 0 when the crystal forms from a gas.
   - d_dilute is a constant reference until tank agitation exists.
   - Each refresh merges a stock's populations of one solid into one, at the mass-weighted mean diameter Σ m d / Σ m, rounded to 9 significant digits.
     - Stocks are tanks, junction buffers and filter cakes.
     - Custom solids are merged the same way. A slurry feed with two grades of one solid is merged by the first stock it reaches.
   - Melting removes mass at the stock's size.
   - Main's outlet check reads the same per-solid mean, so a merge never changes an outlet decision.
3. **Slush.** Main's packed-slurry test blocks an outlet at solids ≥ 0.5 of the condensed volume, instead of 0.62. The slurry viscosity law keeps 0.62. The gas outlet stays open, since a gas port never carries solids in main.
4. **Pipes.**
   - Main's own outlet check decides, using each solid's mean size: deposition velocity = 10 × the Ferguson-Church settling speed, a bore check, and the liquid velocity of the line.
   - A failed check closes the connection for the rest of the interval and is re-decided at the next one.
   - Frost formed from a gas has no liquid carrier, so only the gas outlet stays open.
5. **Precipitation only in reservoirs** (owner, replaces the transport check).
   - Pipes, junctions, pumps, compressors, valves, generators and voids are solved with the fluid phases only. A stream may arrive supersaturated, and the receiving reservoir's next refresh precipitates the excess.
   - Particles already in a stream pass through unchanged. They neither grow nor melt until they reach a reservoir.
   - No connection is refused for freezing, and no new refusal mode or per-slice flash is needed.
   - Transport nodes call the engine the way a vessel does inside a step: `tpCrystalsHeldOut`, with the package's crystal competition. That competition admits a crystal's species below its triple point, down to the crystal's floor. A plain fluid-only request keeps the fluid range, so a CO2 let-down below 5.18 bar would hold in class C.
6. **Two kinds, one inventory.**
   - Crystals and custom particles share main's `SolidInventory`, transport, filters, outlet checks, slurry viscosity, codec and GUI. A crystal record adds its component and its energy offset; only the refresh exchanges its mass with the fluid.
   - The branch's immobile `CrystalInventory` is dropped.
   - Generators feed custom particles only.

## 4. Effect on the current solver

| Solver stage (main) | Change | Expected effect |
|---|---|---|
| Island Newton: rows, Jacobian, `PassKey` active set | None | None. The bundled packages are bit-identical |
| Reconstruction, equation gate, per-population audit | None: a crystal is an ordinary population key | None |
| Vessel refresh (`InventoryEquilibrium`) | A crystal phase in the closure, plus one crystal test per cold vessel per interval. The merge to one population per solid in every stock, junction buffers and filter cakes included | Small cost. A rare "phase cycle" for a vessel exactly on its frost line (class E9), damped by the 1e-8 threshold. The merge keeps mass, volume and heat capacity to rounding |
| Interval controller (5 % state cap, step growth) | None: a refresh jump is not a step | None |
| Solid events (outlet checks between steps) | More deposition and packed closures in cold worlds | More segments, bounded by 2 × pipes + 1 per interval. A larger size means more closures, never a Newton failure |
| Per-slice inlet decision | None (precipitation only in reservoirs) | None |
| Rest test and certificates | Islands holding unchanged solids may rest (decision 6), with the refresh run first | Most of the class F cost removed |
| Approximate fallback | Refused for solid intervals today | A slow solid interval holds F instead of degrading |

**Risks to watch.**
- **Lag within an interval.** A cold inflow can supersaturate a vessel between refreshes. The fluid then carries the excess CO2 until the next refresh, about 5 s later. If that fluid splits into a second, CO2-rich liquid, the island holds typed (class C). The hold policy halves the slice, and the retry starts from a refreshed state. If this shows up often, the fallback form is the branch's per-substep closure.
- **Population count.** After each refresh a stock holds one population per solid. Within an interval, arrivals add at most one per upstream stock, so main's 64-key limit is out of reach.
- **A junction mixing two streams that would precipitate.** The junction is solved with the fluid phases only. If that answer splits into two liquids beside a vapour, the network refuses the state with a typed hold (class C). Junctions get no refresh, so the condition lasts while the flows last. The transport check never covered two streams meeting either; dropping it adds the let-downs that end in a junction. If the cold-package fuzz shows such holds, the fix is to solve junction buffers with one liquid phase. Buffers are not inventory (D46), so folding a second liquid into the first changes no stored amount.

**Why not crystal rows inside the Newton.** That form puts a kink at every onset, of the kind main's wall row needed special Jacobian handling for. It puts latent heat into every energy row and creates mass inside steps, which the gate audits. It would open new holds in classes A, B, E2, E4, E5 to E7 and E10. D18 rejected it. It stays the reference form if the refresh lag ever shows in play.

**Measurement (lean rule).**
1. A 200-seed quick check on the port head, then on the change: class tables and solver counters identical (bundled identity).
2. A cold-package option for the detached hold fuzz, adding these sources:
   - LNG at 110 to 170 K with 0.5 to 5 % CO2;
   - CO2-rich gas let down from 1 to 6 MPa into tanks, and into junctions that mix it with LNG (the junction risk).

   Run 200 seeds. Try the per-substep form on the same seeds only if C or F holds come from the refresh.
3. One 1000-seed campaign: two bundled distributions plus the cold one. Replay only C and F holds alone. Trace the two smallest seeds of the largest class.
4. Unit gates, each proven with its rule off:
   - phi to size;
   - after a refresh, one population per solid in a tank, a junction buffer and a filter cake, at the mass-weighted mean, with mass, volume and heat capacity unchanged;
   - the outlet check reads the per-solid mean: 99 % at 0.25 mm with 1 % at 1 mm keeps a 1.5 m/s LNG line open;
   - slush at 0.5;
   - a let-down that would freeze passes, and the receiving reservoir precipitates at its next refresh;
   - a stock with one size per solid and no crystal leaves the refresh unchanged.
5. One final `gates.sh`.

## 5. Decisions for the owner (recommendation first)

1. **phi.** The crystal component's volume share of the liquids, and 0 from a gas. One formula covers ice beside oil, CO2 from LNG and a CO2-rich liquid. The alternative is the freezing phase's own fraction, which treats every single liquid as continuous and gives every solute the slush size.
2. **d_dilute = 0.25 mm.** CO2-I in LNG then needs 1.0 to 2.8 m/s to stay suspended, and ice in oil needs at most about 0.2 m/s (sizes up to 1 mm). At 0.1 mm almost every line carries CO2 crystals; at 1.3 mm (the pipe-flow drop size) almost none does.
3. **Slush threshold 0.5 for all solids**, not only crystals. It is one rule, and it keeps the steep part of the slurry viscosity (13x at 0.5, 200x at 0.6) out of every open line, which also helps the Newton on inert slurries. Main's slurry tests between 0.5 and 0.62 are restated.
4. **Keep main's deposition law** (10 × Ferguson-Church). It is tested and needs no new parameter. The pipe enters through the line velocity and the bore. Brauner's criterion would add the pipe diameter through friction; keep it as a later refinement.
5. **Transport blocking. Decided (owner): precipitation is evaluated only in reservoirs.** The recommendation had been to block a clear stream that would precipitate and let a crystal-carrying one pass; read strictly, the old rule would let a saturated slurry block its own line through its friction pressure drop.
   - **What breaks: nothing.**
     - Mass and energy stay conserved, since the receiving reservoir's refresh precipitates what the stream carried in.
     - Main never evaluates a phase change inside a pipe (friction reads the donor end's stream), so the rule matches main's structure.
     - It removes a per-slice flash and a refusal mode.
   - **What is lost:** let-down valves and lines plugging with solid formed in them, as dry ice does at real CO2 let-downs. The solid appears in the next tank instead, within about one interval.
   - **What to watch:** the junction risk in section 4.
6. **Let unchanged solids rest.** A well-mixed tank that nothing can drive is at rest whatever it holds. The owner's no-settling rule makes this exact. Main excludes solid-bearing islands from the rest test only as a conservative first rule from the phase-ports batch. Allowing them, with the crystal refresh run before the test, removes most of the class F cost. Crystals make solid-bearing islands common in every cold-CO2 world. The alternative, accepting the exclusion for now, keeps those islands solving every slice.
7. **The approximate fallback stays refused for solid intervals** for now. Revisit only if class F holds remain after decision 6.

## 6. What this changes in the adaptation plan

- **M3 becomes:**
  - the crystal solid kind in `SolidInventory`;
  - the refresh closure with D21;
  - the one-size rule (the merge in every stock, the outlet check on the mean) and slush, with precipitation only in reservoirs;
  - the rest change (decision 6);
  - the unit gates above, and the cold-package fuzz option in `tools/`.
- **Earlier owner items resolved by main's existing solid handling:**
  - **OD-P3, level head:** main already counts suspended solids in the bottom outlet's head.
  - **OD-P4, a tank holding only crystals:** main's dry-solids vessel.
  - **OD-P5, the emptying step:** crystals are inert within a step.
- **M4:**
  - Crystals ride main's solids encoding. A population's material id already names its record, so checkpoint 9 needs at most the lookup of crystal records beside custom ones.
  - The GUI shows them in main's Solids tab and the tank rail's solid share.
  - OD-P7's separate crystal lines are dropped.

## 7. Amendment: one size per solid in each stock (owner question, 2026-10-15; decided: A)

**Question.** "With this system, a unstable regime could produce infinite set of particles with different size. If we let same 'solid' in a reservoir average their particle size, will it make convergence more difficult?"

**What the decided rule does.**
- R10 snapping bounds a crystal to 7 sizes (0.25 to 1.0 mm), so the set is finite.
- Main never frees a population slot while a stock holds any mass of it: `SolidInventory` drops only exact zeros, and trace masking deletes nothing. An unstable regime therefore fills a crystal's 7 slots and keeps them.
- The bound rests on an arbitrary grid, and every future crystal or size input (agitation) multiplies it.

**Effect of averaging on the solver.**
- **Island Newton: none.** Size is read only by the outlet check (`SolidMobility.donor`), the GUI, the codec and the recovered-solids item. Merging one material's populations keeps its mass, volume (m/ρ) and heat capacity (m·c) to rounding.
- **Step audit: none, if the merge runs in the refresh.** Inside a step it would delete keys and trip the per-population balance ("Solid population balance failed", `PassiveStepSolver`).
- **Population limit: practically unreachable.** After each refresh a stock holds one population per solid, plus what arrives within one interval.
- **Rest: unchanged.** A material with one population is left untouched, so an unchanged stock keeps its bits.

**Two conditions from main's code.**
1. **Merge every stock: tanks, junction buffers and filter cakes.**
   - The refresh skips junctions (`InventoryEquilibrium.refresh`), and a cake only grows.
   - Without the merge there, a tank's drifting mean would send a new key downstream each interval and fill main's 64 slots. That stops the connection ("blocked with solid: POPULATION_LIMIT").
2. **The outlet check reads the same per-solid mean.**
   - Main's check takes the largest active population.
   - Between merges, a coarse arrival would close an outlet that the next merge reopens, once per segment.

**Form.**
- A stock's size for one solid is the mass-weighted mean diameter Σ m d / Σ m, rounded to 9 significant digits from the exact double (deterministic on every JDK).
- No coarse grid. An average snapped to R10 sticks: a 1.0 mm tank taking 5 % fines per interval averages 0.96 mm and snaps back to 1.0 mm forever.
- Precipitated mass enters at the current regime's size d(phi) and is averaged in. Melting removes mass at the stock's size.
- Deleted from section 3: R10 snapping, onset-only sizing, the 7-key bound, and proportional growth across populations.

**What changes in play.**
- **The outlet follows where the mass is.** Take 99 % CO2 crystals at 0.25 mm with 1 % at 1 mm, in LNG. They average 0.26 mm and keep a 1.5 m/s line open. Main's rule closes it, because the 1 mm crystals need 2.8 m/s.
- **A late CO2-rich freeze coarsens the tank.** When a CO2-rich liquid freezes after a dilute onset, the tank's size moves toward the slush size. Onset-only sizing kept the first size forever.
- **Custom grades merge.** Two grades of one custom solid fed together become one size in the first stock. Filters catch every size, so no size separation is lost. Main's tests use one size per material, except the key-identity rows of `SolidConservationTest`, which would be restated.

**Decision for the owner.**
- (A) All solids. Recommended: one rule.
- (B) Crystals only; custom solids keep main's per-size populations.
- (C) Keep section 3 as decided.

**Decided 2026-10-15 (owner):** "Make it one, ditch distribution for now", and "Also A". Each stock holds one size per solid and there is no size distribution for now. This applies to crystals and custom solids alike. Section 3 rule 2 is rewritten to it.
