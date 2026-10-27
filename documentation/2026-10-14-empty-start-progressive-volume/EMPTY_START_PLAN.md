# Empty start and progressive volume (plan, 2026-10-14; revised the same day for simplicity)

Status: proposed, for the owner to validate. Branch: continues `claude/phase-ports-compressor` (the integrated solver with t19's cap mode, t20's start switch and t22's arriving-phase layout once taken). Related: the phase-ports batch's `DECISION_LOG.md` (D37 to D53, A280 to A307) and `REMAINING_HOLDS.md`.

## 0. The owner's direction (verbatim)

2026-10-14: "I'm thinking about to make reservoir and pipe contains literally nothing when placed, as the initial air pockets is giving too much nuisance. I want to investigate the possibility to make the initial volume to be zero, and scale the volume to max progressively when filled. This may require some major changes to the solver. Find a smart way of doing this."

Same day, on the first draft of this plan: "With this 'empty' objective, can you find a better way of achieving this with simplification in mind?"

## 1. Physics first

The only real object that holds nothing and grows as it fills is a bag with a stiff wall. While its contents are smaller than its capacity the wall carries no load: the inside pressure is the outside pressure (the game's ambient, 101 325 Pa) plus the head of the liquid, and the volume is the contents' volume at that pressure. Once the contents would exceed the capacity the wall carries the load and the pressure comes from the contents through the equation of state. The plant equivalent is a vented tank: ambient while it fills or drains, above ambient only when full and pushed. A tank full of water takes the source's pressure; a tank drained of water goes slack at ambient instead of pulling vacuum.

Numbers on the E5 card world (a 1 atm water generator 3 m above a 1 m3 tank through a 0.05 m pipe, 7 to 8 kg/s on a 29 kPa head):

| | today (1 atm nitrogen charge) | empty start |
|---|---|---|
| the tank at rest | 220 kg of water over 0.78 m3 of nitrogen at 130 kPa | 1000 kg of water, no gas, at 130.7 kPa |
| time to rest | about a minute | 2 to 3 minutes of filling at ambient, then the head's pressure within a step |
| what the player sees | a tank that never fills past its gas cushion | a tank that fills completely, then holds the source's pressure |

A gas source into an empty tank inflates it at ambient until it is full, then pressurises it: the same pressure history as today minus the 1.15 kg of nitrogen the charge used to put in a cubic metre.

Pipes: a run holds no mass in the solver. What a pipe "contains" is its junction buffer (`m_J`, D25: 0.05 s of the peak flow, virtual since D46) and the column it shows (the stream's fluid, t15 (b)). "A pipe contains nothing" is therefore: the buffer has no physical start state (nothing at the mint), and a run with no stream weighs nothing.

## 2. The simplest consistent model

Three statements, each a deletion more than an addition:

1. **Every vessel is a bag with a stiff wall.** One closure for every vessel, full or empty: given the inventory (amounts, energy) and the capacity, solve temperature, pressure and volume from the equation of state, the energy balance and the wall law `P = P_ambient + k * max(0, V - V_max)`. An empty vessel is at ambient with zero volume: no degeneracy, so no nitrogen charge is needed to give it a state. Today's rigid closure (`PhaseLayout.fixedInventory`, volume fixed) is the limit of large `k` and is replaced, not kept beside. No modes, no event inside the step, no projection, no new checkpoint field (the capacity is the stored volume as today; the current volume is derived).
2. **Nothing at the mint.** A new island's junction buffers have no physical start state. The Newton of its first step starts from a guess, and the guess is washed out by the buffer's 0.05 s relaxation (D25) within a few steps, so no rule about it has a physical claim. The guess is the fixed point the owner kept in D48 (t20's `fixedPointStarts`: the boundaries' states give flows, upwind mixing gives the junctions' mixtures, the mixtures give columns and caps, repeated; the last round reached is the guess, converged or not). The chain, the `junctionStart` switch and its forms, the P-empty refill rounds, the nitrogen charge and its config, and the drain-side gas column for empty receivers are deleted.
3. **Runs stay massless.** A run with a stream shows the stream's column (the one column rule); a run with no stream shows none. The per-run fill length (the owner's "pipe volume scales progressively" taken literally) is option R in section 4, deferred.

What the solver loses and gains:

| deleted | added |
|---|---|
| `NetworkReduction.junctionStarts` (nearest, strongest, mixture, balance pressure, gas column, pockets, ties, devices, fallbacks: D37, D39, D40, A112, A119, A126 to A133) | the wall row in the vessel closure (one equation, one constant `k`) |
| the `junctionStart` switch, P-air, P-empty and its `EMPTY_ROUNDS`, the diagnostic form | the work term `P dV` in the vessel's energy balance (zero when the volume does not move, so today's row is its special case) and a matching boundary term in the ledger |
| `initialNitrogenCharge` for placed tanks, `FLUID_INITIAL_PRESSURE`, `PhysicalRegistry.idleSeed` | the tank display's fill (contents over capacity) |
| t10's drain-side gas column rule for receivers that hold no gas | nothing else |

The first draft of this plan (same day) reached the same physics with two vessel modes decided in the active-set loop, a closure at fixed pressure, a step cut at the crossing and a projection. The wall law gives the same behaviour as one constitutive row: below capacity the pressure is ambient exactly; above it the wall is as stiff as the liquid (choose `k` as the liquid's bulk modulus over the capacity, so a full tank at 30 kPa over ambient is compressed by about 1e-5 of its volume, which is what a steel tank does). The mode form stays as the fallback if the one kink of the wall law troubles the Newton (section 4).

## 3. Design

### 3.1 The vessel closure

- Unknowns per vessel: temperature, pressure, the phase split, and now the volume. Equations: the amount balances per phase as today, the energy balance, the equation of state per phase, and the wall law. `InventoryEquilibrium.close` solves it from the previous state as today; `PhaseLayout.fixedInventory(n, U, V)` becomes `wallClosure(n, U, V_max)` with the volume an unknown.
- The kink: `max(0, V - V_max)` is one monotone kink per vessel, crossed once per filling. A Newton with the existing line search handles a monotone kink; if the quick check says otherwise, the smooth form `k * softplus(V - V_max, width)` with a width of 1e-4 of the capacity is a one-line change and costs a pressure rise of a few pascals just before full.
- Energy: a closed system with a moving wall, dU = sum(h_in dm_in) - P dV. The step books U from the end flows as today plus `-P_avg (V_end - V_0)`; the ledger reports the wall work as the ambient boundary's term, so it still closes exactly. When the volume does not move the term is zero and the row is today's.
- Empty vessels as donors: nothing to draw, so every port is dry (as a LIQUID port with no liquid behind it passes nothing today: the dry drain of the bitwise probe); a gas port on a slack tank holding only liquid is dry too (a vented tank's top port sees the atmosphere, which the model does not carry). Stated as a consequence: the model never draws air.
- Phase ports and the level head are unchanged: the liquid height is the liquid volume over the footprint, the gas space (if any gas was put there) sits above it, the top port draws gas by priority (D11).
- Rest and steady: a slack vessel with no flow is at rest (ambient is a constant); a filling vessel is not; STEADY compares what it compares today.

### 3.2 Nothing at the mint

- A placed tank: empty, slack, ambient. A placed pipe: its island re-mints (D45) with empty buffers. An edit on a running island re-mints its buffers; vessels keep their inventory (they are not minted).
- The first step of a new island: the ordinary backward-Euler step from committed flows that do not exist (t15 (a) has nothing to carry), so its Newton starts from the fixed point's last round: states, flows, caps (t19's start caps read the guess's states, A271). The buffer's `w_old` is the guess; the relaxation washes it out in 0.05 s. No separate first-step row, no refill rounds.
- Rounds: the fixed point is bounded (t20 used 8). The thread measures one round against the bound on the quick check and keeps the smaller that is not worse. A failed rate solve inside a round ends the rounds with the last good one; nothing falls back to a rule.
- t20's measurement of the fixed point at the mint (833 / 936 against the chain's 935 / 957, A285) was taken with every tank charged with air and before t22's layout; its first-interval holds were group L and C0, both air artefacts. It does not predict this design; section 6 measures it.

### 3.3 Runs

Massless, as today. The column of a run is its stream's; a run with no stream has none. This retires the question of what a never-flowed rising run "holds" (the chain's pockets and ties, the "rising run holds no liquid" rule): it holds nothing until something flows, which is the fixed point's initial condition and now simply the truth.

## 4. Options kept in reserve, not recommended now

- **Mode form** (the first draft): SLACK and TAUT as vessel modes in t19's loop, the closure at fixed pressure, the crossing as a step cut. Exact where the wall law is stiff; three mechanisms where the wall law is one row. Fallback if the kink fails the quick check.
- **One node type** (junction = a tiny bag of the pipe block's volume, about 2 L for a metre of 0.05 m bore, the same wall closure, no mixing row and no `m_J`). The deepest simplification: vessels and junctions become one thing and D25/D46 disappear. Its risk is the one the mixed-gas batch (0.6.0) paid for: a taut liquid-filled 2 L bag is as stiff as the liquid, so a junction's continuity would be enforced through the stiffness instead of an algebraic row, and the Jacobian's scaling would have to be re-measured across the whole campaign. Worth a measured attempt after this plan lands, not inside it.
- **Option R, the per-run fill length** (a FILLING mode per run with the front at ambient, the column from the filled part, the loss over the filled length, the run's inventory booked): exact filling transients and the manometer riser without rules, at the cost of a state per connection in the checkpoint, the wire format and the GUI, an event per run in a new island's first interval, and the ledger and transport over runs. Only if the campaign leaves the riser family (E6-S) as the largest class.

## 5. What moves

- References: the GameTests place their worlds, so their tanks start empty and their printed values move (reported old and new; re-recorded once at the batch's single re-baseline). The 33 junction lines and chain-100 build their vessels explicitly with `initialNitrogenCharge` in `FluidTestSupport`, so those vessels are above capacity at their first step and on the stiff branch: expected unchanged to the bit if `k` reproduces the rigid closure's limit, else at roundoff (reported). The fuzz campaigns place their worlds: a new baseline.
- Behaviour: a new island's first interval is a filling transient (as under the air start, without air); a tank fills completely; a drained tank goes slack instead of pulling vacuum; a gas port on a slack liquid tank is dry.
- Formats: the checkpoint loses the charge and gains nothing; the display gains a fill. Fresh world (the standing rule) because the runtime default state of a placed tank changes.

## 6. Measurement (the lean rule, D50)

- Quick check (200 seeds, both distributions) after each of the two commits; one campaign (1000 seeds, both distributions, 8 workers) after the second: the new baseline, holds by class, the two smallest traced; one `gates.sh` run with a GameTest id, old and new for every moved print, nothing re-recorded.
- Gate tests, each proven clean with the rule and held with it disabled in the working tree, run alone: (1) the E5 card world: the tank fills completely and rests at 130.7 kPa, no gas cushion; (2) an empty riser: a dead-end rising run from a pressurised junction holds a water column at the head and nothing above it.

## 7. Work plan

One thread, two code commits, one campaign: (1) the wall closure with its energy term and ledger term, the charge and its config removed, the fill in the display, gate test (1); (2) the fixed point as the first step's start guess, the chain, the switch, the refill rounds and the gas-column rule for empty receivers deleted, gate test (2). Quick check after each, the campaign and the gates after the second. Then the batch's close: the single re-baseline on the final code, cleanup and detach, the determinism integration, the merge as 0.7.0.

## 8. Decisions for the owner

1. Vessels as bags with a stiff wall, one closure, no modes (recommended), with the mode form as the fallback.
2. Nothing at the mint, the fixed point's last round as the Newton's start guess, the chain and the switch deleted (recommended).
3. Runs massless; the per-run fill length and the one-node-type simplification as later, measured options (recommended).
4. Scope: supersedes the air-start route (D51) and the queued start-flows item (t23); thread 22's layout is taken as part of it; thread 21's row stays not taken; the initial charge and its config retire.
