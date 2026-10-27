# Backward compatibility for the fluid network and equipment models: plan

Batch `2026-10-26-backward-compat`. Base `2fbb80f2` (main, 0.12.3). Status: Implemented 2026-10-26 (0.13.0, `8d4c8df6`). Owner decisions 2026-10-26: D6 "simply remove it (and don't leave ghost blocks behind)", "other as recommended" (D1 to D5, D7 to D9 and the AGENTS.md rule of section 11, now in AGENTS.md).

Owner request 2026-10-26: "Design a backward compatibility system for both the fluid system and the model system, aiming to minimize the world changes when something changed".

Scope:

- **Fluid system**: the fluid checkpoint (`icheme_fluid_core`, packs, island and topology units), the fluid device block entities, the accessory store (`icheme_accessories`).
- **Model system**: equipment models: the shape record in shell and constructor NBT, layouts, and their fluid registrations. This continues the owner request of 2026-10-18 ("when loading an old save updated model is replaced by new ones").
- Out of scope: wire protocols (client and server ship together; a mismatch refuses a connection and changes no world). The column calculator block (`DATA_VERSION` 12) loses its input on a mismatch too; the facts rule of section 3 fits its `Input` in a later batch.

This plan reverses the AGENTS.md rule "Do no compatibility work" for these two systems. Draft rule text: section 11.

## 1. Expected behaviour

A code or data update changes the description of the plant, not the plant. After an update:

- every tank and junction holds the same amount of each component, at the same temperature;
- every valve, pump, cutoff, filter and generator keeps its setting;
- every vessel stands where it stood, with its paint, with its nozzles at its pipes, and with its fluid registration;
- only what the new code computes differently changes: a corrected property moves a pressure, a new layout adds or drops cells, a solver change re-solves each island once.

## 2. What the code does now

| Trigger | Code | World change |
|---|---|---|
| Checkpoint, unit or pack format differs | `FluidSavedData.open` catches `FormatMismatchException` and starts empty (`FluidCheckpointStore.resetForFormatChange`) | All contents, settings and pending events lost; devices re-register with defaults and new identities as their chunks load |
| Property basis differs: any material JSON edit (it changes `MaterialCatalog.fluidThermoFingerprint`, part of `FluidThermodynamics.thermodynamicRevision`) | `FluidCheckpointStore.load` throws "Incompatible fluid property basis"; `open` rethrows | World does not open |
| The same during `/reload` in a running world | `FluidPropertyReloadGuard.HOLD` | Every island held until the old data returns |
| A saved solid's id is unknown, or its density or heat capacity changed | codec refuses "(no migration; create a fresh world)" | World does not open |
| An enum reordered (six tables saved by ordinal in `FluidCheckpointCodec`) | needs a format bump | As row 1 |
| A shell's shape no longer valid (kind renamed, range or encoding changed) | `EquipmentShellBlockEntity.loadAdditional` logs; the cell stays unbound | The vessel loses its drawing; each cell collides as a full block |
| A constructor design no longer valid | `Design.fresh()` | The player's design is lost |
| A layout stamp differs | `EquipmentRefresh.plan`: shift within 3 blocks that covers the most cells; clear, place; blocked cells left out | Cells change (wanted); a nozzle can move away from its pipe; a moved pump or compressor gets a new identity (`FluidWorldAuthority.relocateMover`) |

Format history. Between 2026-10-20 and 2026-10-25 the checkpoint went from format 11 to 19 (unit format 4 to 12): eight bumps, each a fresh world (before 2026-10-23) or a reset (after). Each of the eight added fields: vessel ports and level heights (12); open vessels, the OVERFLOW port and boundary kinds (13); directed mover ports (14); requested flow and on/off (15); the generator flag (16); the cutoff code (17); CHECK and direction masks (18); filters (19). Formats 10 and 11 removed fields only (mover efficiency, the approximate fallback, interval acceptance, velocity-limit modes). None of these ten bumps changed how contents are stored. Under this design none of them changes a world.

## 3. Principle: facts and state

Each saved value is a fact or state.

- **Facts**: what the player built and put in. Device registrations (identity, position, kind, facing, ports, fittings, spec), device settings, node contents (moles per component, energy, temperature, pressure, solids), filter captures, pending recoveries, ledger totals, the next identity, the world clock, installed accessories, equipment shapes, paint and nozzle sizes. A code or data change never discards a fact. It can restate one (4.4) or move one (5.2).
- **State**: what the engine can compute again. Solved intervals and end flows, junction seeds, certificates, cadence, retry and fence ticks, solver caches. Any doubt drops state. The engine rebuilds it on its own schedule: the island starts awake from `BalanceNet`, as a new island does. Cost: one solve per island, and transients while flows settle.
- **Pending events** are player inputs not yet applied (at most one fence, about 5 s). They are state (D5): the facts path drops them with a log line, and the player sees the old setting.

## 4. Fluid system

### 4.1 One frozen envelope, two sections

- Every unit is: a frozen header (magic, envelope 1, kind, id, revision, generation), a length-prefixed **facts section** (facts schema N), a length-prefixed **state section** (state format M). The envelope and the pack layout never change.
- Island unit: facts = its nodes' contents and its pipes' filter captures; state = everything else. Topology unit: facts = the registry with settings, recoveries, ledger totals, next identity; state = pending events.
- The core record keeps named NBT keys. `FluidFormat` splits into `Envelope`, `FactsSchema` and `StateFormat`.
- Size: contents move from state to facts. Added per node: temperature and pressure, 16 B. Component ids once per island. A certified island also keeps its certificate base in its state, as today; that is the only place contents appear twice.
- Incremental saves keep working: a unit is rewritten only when its island changes. A certified island's facts are its materialised contents, so an island that vents or spills rewrites its unit at each save; a resting one does not.

### 4.2 Load ladder

1. **Exact**: state format current, basis unchanged. Today's load, bitwise.
2. **Facts**: the state format differs, a state field fails validation, or the basis changed.
   - Read the registry from the topology facts: same identities, same settings. Compile the islands from it on the topology commit's own path. Facts do not wait for chunk loads; block entities rebind by identity as on any load.
   - Attach contents by node id (a node id is a device identity). A device that is no longer a retained node gives its contents to the nearer end of its run (amounts and energy add; ties to the lower id).
   - Restate the contents if the basis changed (4.4). Drop the state. Islands start awake.
3. **Refuse**: the facts section is damaged (checksum, length, an invalid fact). Today's refusal; files untouched.

There is no reset to empty.

### 4.3 Facts schema rules

- Fields are tagged and named. A new field is optional. Its declared default equals the behaviour of the build before the field existed (a closed vessel, no cutoff, on, no filter, mask 0, passive control).
- A field never changes meaning or unit. A changed meaning is a new field: the reader reads the old field once into the new one, and the writer never writes the old one again.
- Enums are saved by name. A renamed or removed constant gets an entry in one alias table.
- Every fact is validated. A setting outside the current range is clamped, logged and named on the island's status line.
- An unknown field (written by a newer build) is refused, not skipped (D9).
- `FactsSchema` rises only when a field is added. The reader reads every older schema, because a missing field reads its default.

### 4.4 Restating contents after a model or data change

Expected behaviour: the same material, at the same temperature, in the same volume. The model only describes it.

- Kept exact: moles per component id, the volume (from the device), each solid's mass and size, the temperature T.
- Recomputed with the new model: internal energy U = U_new(T, V, n), pressure, phase split.
- Rejected option, keep U (D2): with the energy reference `icheme:sensible-298.15K-v1`, a cp edit of x moves T by about x · (T − 298.15 K). A 5 % edit moves a 350 K tank by about 2.5 K and a 120 K cryogenic tank by 8.5 K or more, which can cross a phase boundary on a data edit alone.
- Conservation: moles are exact. The energy difference U_new − U_old goes to the ledger as a restatement (a new ledger line). Scale: a 1000 m³ crude tank at 350 K holds about 9 × 10¹⁰ J; a 5 % cp edit books about 4.6 GJ.
- A component the package no longer has: its moles leave, are booked as removed by a data change, and the island's status line names it (D3). A component the package now has starts at zero. A renamed component or package maps through an alias in the material data (`replaces`), which the datapack owns.
- A solid is kept by id with its mass and size; its energy is restated at T. An unknown solid id is booked as removed.
- Where it runs: the island's first engine job, on a worker; never on the server thread or in a chunk load. `/reload` uses the same job.

## 5. Equipment models

Kept as today: a cell draws from the code at every load (a drawing-only change changes no block), `REACH` 3, never overwriting another block, keeping origin, paint, nozzle sizes and a drum's registration.

### 5.1 Shape record

- Shell and constructor NBT share one shape reader with the rules of 4.3: named fields, declared defaults, enum names with aliases for `Kind`, `Orientation`, `Heading` and `Form`.
- A value outside the current range (diameter, length, support, trays, bank, outlet) is clamped and snapped to the kind's step, then logged (`equipment_compat kind=… field=… from=… to=…`). The changed shape has another stamp, so the normal refresh rebuilds the vessel.
- A kind with no successor (D6, owner 2026-10-26: "simply remove it (and don't leave ghost blocks behind)"): the structure is removed whole, as a dismantle removes it: every shell and interior cell is cleared, its fluid registration is removed as a broken vessel's is, and its gauges are refunded. The cells are found from saved data only (origin, box, the saved kind name), because the shape can no longer be read.
- `Design.fresh()` stays only for a constructor that holds no design.

### 5.2 Refresh with the least world change

- Two stamps in place of one: a cells stamp (box, shell and interior cells) and a ports stamp (nozzle cells, faces, port classes). Ports changed alone: one queued registration edit, no block change. Cells changed alone: a block refresh, no fluid edit.
- Ports first: the anchor takes the shift that keeps the most nozzle pipe blocks at their old world positions, then the most cells. A pipe stays joined wherever its nozzle can stay.
- A plan with blocked cells leaves the old stamp on the cells it wrote, so the refresh tries again at the next load, after the player clears the block. The datasheet shows "N cells blocked".
- A moved mover keeps its identity, as a moved reservoir does (D7). Check first why `relocateMover` issues a new identity; gauges and accessories bind by identity.

### 5.3 Layout change review

- New gate `EquipmentLayoutGoldenTest`: per kind at its smallest, middle and largest size, a checked-in table of the box, the cell count, the cells stamp and each nozzle's pipe block. A layout change then shows its world cost in review: cells added and dropped, ports moved. A moved port needs owner approval.

## 6. Change classes

| Change | World change | The change must carry |
|---|---|---|
| Drawing only | none | nothing |
| Solver or state field | each island re-solves once | nothing (state dropped) |
| New fact field or shape field | none | a default equal to the old behaviour |
| Enum constant renamed or removed | none | an alias entry |
| Material data or model revision | contents restated at T | nothing; notice and ledger line follow |
| Component, solid or package removed | that material removed and booked | an alias entry if it was renamed |
| Layout cells changed | cells added or dropped | the golden table update |
| A nozzle moved | its pipe can part | the golden table update and owner approval |
| A fact's meaning changed | — | not allowed: add a field |

## 7. Gates and test corpus

- `FluidFactsCompatTest`: a corpus of facts images in `src/test/resources/compat/fluid/`, one per facts schema, from a synthetic world rig (drum, open tank, pump, valves, filter with solids, two packages). Every image loads with every fact intact: identities, settings, moles and T bitwise, solids. Add an image at each `FactsSchema` change.
- Round trip: facts written, read and compared bitwise; the exact path stays bitwise (chain-100 exact).
- Restatement: the same basis keeps U bitwise; a changed basis keeps n and T, books ΔU, and removes a dropped component with its ledger line.
- `EquipmentShapeCompatTest`: a corpus of shell and constructor NBT per kind. The reader never fails; clamps and aliases log as expected.
- `EquipmentRefreshTest` adds: the ports-first anchor; a blocked plan retries.

## 8. Work packages

| WP | Content | Needs |
|---|---|---|
| W1 | Envelope, facts and state sections, core keys; format 20 | — |
| W2 | Facts load path | W1 |
| W3 | Restatement job for load and `/reload`; ledger line | W2 |
| W4 | Shape reader: names, defaults, aliases, clamps; the constructor | — |
| W5 | Two stamps, ports-first anchor, blocked retry, mover identity | W4 |
| W6 | Corpus and gates, AGENTS.md rule, code guide | each WP |

- W1 to W3 and W4 to W5 touch separate files: two sessions can run them.
- Measurement: the quick suite per WP; one final lane at the end. No campaign: no solver changes. Chain-100 exact and the `SaveTiming` counters show the cost.
- One in-game check at the end: open a world saved before a deliberate state format bump and a material edit.
- Transition (D4): worlds at format 19 or older have no facts section.

## 9. What this deletes

- The reset branch of `FluidSavedData.open` and `FluidCheckpointStore.resetForFormatChange`; the reset role of `EquipmentStructure.restoreFluidBinding` (after W2 the registry returns with its identities).
- `FluidPropertyReloadGuard.HOLD`; the property-basis, energy-reference and solid refusals of the load.
- Format bumps for added fields and for reordered enums.
- With D7, `FluidWorldAuthority.relocateMover`.

## 10. Owner decisions

| | Question | Recommended | Alternative |
|---|---|---|---|
| D1 | Scope | Fluid and equipment now; column block later | Include the column block |
| D2 | Restatement basis | Keep T | Keep U |
| D3 | Removed component or solid | Booked as removed, status notice | Refuse the world |
| D4 | Worlds at format 19 or older | One last reset | A one-off format-19 reader, deleted after one release |
| D5 | Pending events on the facts path | Dropped | Kept as facts |
| D6 | Retired unit kind | Removed whole, no cells left (owner choice) | Plain casing kept (rejected) |
| D7 | A moved mover | Keeps its identity (after the check) | New identity, as today |
| D8 | Where facts live | Inside the units (incremental, checksummed) | A separate NBT file (simpler; rewritten whole at every save) |
| D9 | A field from a newer build | Refuse | Skip |

## 11. Draft AGENTS.md rule (replaces "Saves and data formats")

Applied 2026-10-26 with two additions: the D6 removal sentence, and a last bullet that keeps "no compatibility work" for the formats out of scope (wire protocols, the column calculator block). AGENTS.md is the authority.

- Saved data holds facts and state. Facts are what the player built and put in: device registrations and settings, node contents, captured solids, ledger totals, the world clock, accessories, equipment shapes, paint and nozzle sizes. State is what the engine can compute again: solved intervals, junction seeds, certificates, cadence, caches, pending events.
- A code or data change never discards a fact. State formats change freely: a mismatch drops the state, and the engine rebuilds it on its schedule.
- Fact and shape fields are append-only. A new field has a default equal to the old behaviour. A field never changes meaning or unit. Enums are saved by name; a renamed or removed constant gets an alias.
- After a material or model change, contents keep their amounts and temperature. An engine job recomputes energy and pressure. The ledger books the energy difference and every removed component; the island's status line names them.
- After a placed equipment structure loads, rebuild it to the current layout if another layout placed its cells (`EquipmentRefresh`, `EquipmentStructure.refreshLater`). Keep nozzle pipe blocks in place first, then cells. Keep the origin, paint, nozzle sizes and the fluid identity. Never overwrite another block.
- Every format validates what it reads. A damaged facts section refuses the world and leaves its files untouched.
- At every facts-schema or shape-field change, add a corpus image under `src/test/resources/compat/`. The gates read every image. Do not test against player worlds.

## 12. Risks

- A wrong default changes old worlds silently. The default must equal the old behaviour; the previous schema's corpus image checks it.
- A restatement can change a phase split. That is the new model's answer; the notice names it.
- The corpus catches a reader change, not a writer that reuses a field with a new meaning. Review enforces "a field never changes meaning".
- A load after a state bump solves every island at once. The bounded worker pool spreads it; the first presentation waits for it.
- If the envelope ever has to change, that build resets worlds once, as today.

## 13. Implementation notes (2026-10-26)

What the code does where it differs from or adds to the plan above.

- Restatement runs in the load (`FluidFacts.rebuild`), off the tick loop: a TP flash per vessel at its saved T and P. A running world's `/reload` keeps a hold, now "save and reopen the world"; restating in process is an open item.
- Vessel contents are facts of the island units, beside the state. With `icheme.fluid.scheduler.verify` (the test tasks) every exact load cross-checks the two (`FluidFacts.disagreement`).
- The alias maps (`FluidFacts.ALIASES`, `ShapeRepair.*_ALIASES`) exist and are empty: nothing is renamed yet.
- Two stamps (5.2) dropped as a simplification. A layout whose cells all match stands where it is with an empty plan, and `FluidWorldAuthority.edit` queues nothing when the registration is unchanged, so ports changed alone already give one edit and no block change.
- Defect found in W5 (it predates this batch). Expected: a refreshed drum that moves or changes volume stays the same vessel with the same moles and energy. Code: the ledger refused both edits ("Moved or stale device edit", "Finite-reservoir initialization cannot be reapplied"), so `reregister` threw after the blocks had changed; a moved mover was replaced under a new identity. Fix, one rule per stage:
  - ledger: an edit keeps kind and dimension; a vessel's edit changes at most its geometry (`FluidDeviceSpec.sameCharge`);
  - registry: a rebuilt vessel keeps its moles, energy and solids in the new geometry and elevation; the chunk index follows a move; movers move by the same edit (D7), `relocateMover` deleted;
  - coordinator: a topology batch conserves contents (`Inventory.sameContents`); geometry and elevation follow the registry (no ledger carries potential energy, D37 S1);
  - rest test: a vessel off its wall row by more than the gauge reads (`NetworkReduction.WALL_RESOLUTION`, 2e-6) is not at rest, so the interval's refresh closes it to the new wall. A closed state meets the row to 1e-11. The one fixture that started below the ambient in a closed bag (`LevelHeadTest`, 100 kPa) moved to 200 kPa, as its neighbour had for D54.
- Ports first (5.2): a pipe block is a `Holding.PIPE`; the anchor ranks shifts by nozzles kept on a pipe, then cells, then distance. The refresh region reaches one cell further for the pipe blocks.
- Blocked retry (5.2): the written cells keep stamp 0 (`EquipmentShellBlockEntity.layoutIncomplete`), so the next load refreshes again. The datasheet line "N cells blocked" is counted on the client from the cells it holds.
- Layout table (5.3): `src/test/resources/compat/layouts/layouts.txt` (81 shapes), outside `compat/equipment/`, whose files the shapes corpus reads as SNBT.
- Open: a vessel rebuilt smaller than its liquid (only a volume-formula change or a clamp repair can do that) is compressed by its taut wall; its pressure drives the excess out of its ports, or the closure fails and the island holds.
