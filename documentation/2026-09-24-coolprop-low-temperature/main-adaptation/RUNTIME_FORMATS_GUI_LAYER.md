# Main adaptation: runtime, formats, presentation, GUI, consumers and GameTests

Batch `2026-09-24-coolprop-low-temperature`, input to the plan for porting branch `claude/coolprop-multiphase-thermo-37f6b0`
(HEAD `9aaafd43`, base `f9d6be10`, 61 commits) onto `main` (`ffeafe8a`, mod 0.8.0, 88 commits since the base). Written
2026-10-15 by a read-only analysis agent: no code changed, no Gradle run, no ref or index touched. ASCII only.

Sources read: main's code guide (`documentation/reference/code-guide/README.md`, `fluid-runtime.md`, `client-gui.md`,
`verification.md`, `fluid-science.md` for the wall row and junction holdup), `git show main:AGENTS.md`, this batch's
`P4_NETWORK_COUPLING.md`, `P6_PILOT_ACCEPTANCE.md`, `P6B_RUNTIME_FAILURE_DIAGNOSIS.md`, `DECISION_LOG.md` D9, D12, D18 to
D20, the tool folders `tools/p6-pilot-acceptance/` and `tools/fluid-tank-column-gui-fixes/` (main checkout), and the
diffs `f9d6be1..HEAD` and `f9d6be1..main` of every file named below.

Dispositions: **T** take main; **R** re-apply the branch change mechanically on main's code; **D** redesign on main's
structure (how is stated); **X** obsolete on main. "OD-n" points to section F (choices not forced by physics, by main's
code or by a recorded rule).

## 0. Findings in short

1. The branch's runtime footprint is small: 8 commits touch the scoped production files, and only two carry substance:
   `959ea0d9` (P3 WP4, D9/D12: the global liquid compressibility leaves the package key, checkpoint format 5) and
   `2802e5e1` (P4 stage 2b, D18: the crystal inventory in the island unit, certificates, ledger totals, the view,
   checkpoint format 6). `37235698`, `4d26a45a`, `7bf22842`, `e4d355f2`, `50dfe315`, `6ec5c2cb` are small.
2. The branch's "format 6" therefore means "format 5 (no compressibility) plus crystals". Main's formats 6, 7 and 8 are
   phase ports, end flows and the removal of the module ledger. The ported branch declares **checkpoint format 9,
   island-unit format 2, menu protocol `fluid-10`** (section B).
3. The branch never bumped the menu protocol (`fluid-4` at base and at HEAD) although `FluidView.State` gained two wire
   fields. On main the bump is required (`fluid-9` -> `fluid-10`).
4. Every branch test change under `runtime/`, `fluid/` and `src/fluidGameTest` except `CrystalIslandRuntimeTest`, the two
   crystal round trips in `FluidCheckpointCodecTest`, the format-refusal update, `FluidDeviceSpecDomainTest`'s 10 MPa case
   and the two 3 MPa -> 12 MPa packet cases is the mechanical removal of the `1e-9` compressibility argument. The four
   branch-modified tests main deleted (`FluidModuleGameTests`, `BufferedCommitTest`, `CausalModuleCoordinatorTest`,
   `ModuleTransferPlannerTest`) carried nothing else: X, nothing to re-home.
5. `CrystalIslandRuntimeTest` cannot compile on main as written (its private rig passes `IslandCoordinator.CommitHook`,
   gone on main) and its second test asserts D18 item 5 ("a held island takes no events"), which main's owner decision of
   2026-10-15 (`IslandCoordinator.acceptAtFence`) supersedes. It moves onto `CertifyingRig` and is rewritten (D3).
6. The screen change is a D: main rewrote `FluidDeviceScreen` (tank rail, phase-ratio and status blocks, outlets page,
   kmol and kg side by side). The branch's right-column lines have no place left; the crystal amount goes to the tank
   rail, and "fluid phases together" moves to the outlets page (section C).
7. Main's `FluidView.State.fill()` is "the share of the capacity the contents take". The contents include the crystals,
   so `fill` must add `crystalVolume` (forced by main's definition, not a choice).
8. Gaps found: no test proves the codec's crystal refusals, the ledger's crystal totals, the certificate's crystal gate or
   the wire's crystal validation; the P6 claim "a pilot checkpoint loaded with the bundled model is refused" rests on code
   reading. Section D4 lists the five small tests that close them.

## A. Production files

| File / feature | Branch change and purpose | Main change and purpose | Disposition | Proving test |
|---|---|---|---|---|
| `runtime/fluid/FluidCheckpointCodec` | (a) `959ea0d9` (D9, D12; direct liquid path): format 5 / unit 2; the global liquid compressibility leaves `PackageKey`, `PackageRecord`, `IslandEntry`, the island unit (`w.f64(compressibility)` after the package ref), the core `Packages` entry (`Compressibility`) and the envelope digest; `IslandEntry.key()` added; `requireDouble` deleted. (b) `2802e5e1` (D18 item 6): format 6 / unit 3; each node's crystals after its particle populations: count (varint, at most `CrystalInventory.MAXIMUM_CRYSTALS` = 16), then per stock id (text), species index (varint, below the basis), moles (f64); on read: admitted by the island's model, at the model's own species index, finite and positive, canonical order, no crystal in a junction; volume and energy re-evaluated by `model.withCrystals(state, crystals)`, never stored; formats 1 to 5 refused. (c) `37235698`: the archived gameplay-capture reader keeps its own four-field inventory (`ArchivedInventory`) | Format 6 = phase ports per pipe end, 7 = end flows of the last interval, 8 = the core `Ledger` of buffered transfers and modules removed (0.7.2). `FluidUnitIO` nested as `FluidCheckpointCodec.Writer`/`Reader`. `Checkpoint(List<IslandEntry>)` only. Topology unit holds registrations, events, totals, `WorldTopologyLedger.FluidBasis` and `RecoveredSolid`. `withIsland`/`withUnit` moved to the test rig `CheckpointImages`. The archived-capture reader moved to the test tree (`McpGameplayRegressionTest`) | (a) **R** at main's `IslandEntry`, `PackageKey`, `PackageRecord`, `islandUnit`, `island`, `coreTag`, `readCore`, `manifest`; only if the science port takes the direct liquid path (section G). (b) **D**: two private helpers `crystals(Writer, CrystalInventory)` / `crystals(Reader, model, basis)` beside main's `solids(Writer, ...)` / `solids(Reader, ...)`, called in main's `state(Writer, graph)` and `state(Reader, Topology, model)` right after `solids`; the 5-argument `PassiveNetwork.Inventory`; tighten the read check from "not a junction" to "only a `RESERVOIR` node" (D18 item 3: finite vessels; the branch's allowance for `PORT` served the causal modules, gone on main); `VERSION` 8 -> 9, `UNIT_FORMAT` 1 -> 2; class Javadoc names format 9. (c) **X** | `FluidCheckpointCodecTest` (2 ported crystal round trips + 1 new refusal test, D4), `FluidCheckpointFormatTest` (older formats `{1,2,3,7,8}`, unit format 1 refused), `FluidCheckpointStoreTest`, `FluidCheckpointGameTests` (G5) |
| `runtime/fluid/FluidUnitIO` | `959ea0d9`: Javadoc "format 5, unit format 2" | Deleted (0.7.2; nested `Writer`/`Reader`) | **X** (the format line goes into the codec Javadoc) | none |
| `runtime/fluid/FluidCheckpointStore` | `959ea0d9`: `var key=entry.key()`; Javadoc format 5, then 6 | Module ledger gone; Javadoc still says "format 7" (guide open item 5) | **R**: the key line with (a); Javadoc -> "format 9" | `FluidCheckpointStoreTest` |
| `runtime/fluid/FluidSavedData` | Javadoc only (formats 5, 6) | `legacyUnbound` gone; a world-less checkpoint is refused by `open`; Javadoc still "format 7" | **R**: Javadoc -> "format 9" | `FluidCheckpointFormatTest`, `FluidHarnessGameTests` |
| `runtime/fluid/FluidView` | `2802e5e1` (D18 presentation): `State` gains `CrystalInventory crystals` and `double crystalVolume` (finite, at least 0); `from(s)` fills them from `s.crystals().inventory()` / `.volume()`; `volume` and `mass` documented as the mobile phases' (fluid and inert particles) | `State(pressure, temperature, volume, mass, phaseVolumes, phaseMoles, solids, capacity)` with `fill()` (D54); `from(s, capacity)`; `Outlet`, `PipeInfo`, `PipeConnection`, `PipeRoute`; `hydraulicOwner` and `intervalQuality` removed (`fluid-9`) | **D**: `State(..., solids, crystals, crystalVolume, capacity)`; `from(s, capacity)` fills the crystals; `fill()` = `min(1, (volume + crystalVolume) / capacity)`, because main defines fill as the share of the capacity the contents take and the crystals are contents; `volume`/`mass` stay mobile (main's `displayedFlow` divides phase moles by `mass`, which must stay the mobile mass). Protocol `fluid-10` | `FluidPacketCodecTest` (new crystal case, D4), `CrystalIslandRuntimeTest` (bucket-only), MCP check (C4) |
| `network/FluidNetwork` (not touched by the branch) | none: `PROTOCOL` stayed `fluid-4` although the live JSON gained the crystal fields | `fluid-9`; `MenuData`/`StaticData`/`LiveData` validated in compact constructors | **D**: `PROTOCOL = "fluid-10"`; `MenuData` adds a crystal check beside `requirePhaseAxis`: every stock's species index below `components.size() - 1` (the hydrocarbon axis; the last component is Water), at most 16 stocks (the `CrystalInventory` record enforces canonical order and positive amounts on both sides) | `FluidPacketCodecTest` |
| `runtime/fluid/FluidWorldAuthority` | `959ea0d9`: the `compressibility` field and its uses (model key, reload guard, capture) removed | D54 placement, queued recoveries, `RegistryHost.committed`, view with outlets and inlet reasons, `legacyUnbound` gone | **R** at main's field (line 26), constructor (56 to 58), load loop (70), `model(key)` (113), `capture` (252); the view needs no code change (it already calls `State.from(state, capacity)`) | `FluidHarnessGameTests`, `FluidPresentationGameTests`, `FluidCheckpointGameTests` |
| `runtime/fluid/FluidPropertyReloadGuard` | `959ea0d9`: compressibility field and argument removed | `SlurryTransport.Settings`, `validateSolids`, new `HOLD` text | **R**. Crystal records are already guarded: the network's thermodynamic revision is the `ThermoIdentity`, whose `spineFingerprint` hashes the spine and crystal records (`MaterialCatalog` line 593 on the branch) | `FluidPropertyReloadGuardTest` (+ a crystal-record case, D4), `PropertyReloadCoordinatorTest`, `FluidPropertyReloadGameTests` |
| `runtime/fluid/IslandCertificate` | `2802e5e1` (D18 items 4, 6): a crystal inventory that differs between the interval's ends is a node-solid change (no certificate across a deposition or sublimation); `graphAt` rebuilds with `inventory.crystals()`. `Signature` and `graphIdentity` unchanged: crystals sit only in vessels, which are not fixed nodes and are skipped by the identity digest | One certificate kind (REST and STEADY merged); the summary also walks junction nodes (junction holdup); identity digest `-2` with phase ports | **R**: the gate at main's line 83 (`||!a.inventory().crystals().equals(b.inventory().crystals())`) and the 5-argument `Inventory` in `graphAt` (line 468; with the 4-argument constructor the replay would silently drop the crystals, see G5). `Signature`: no change | New `CertifyingRig` case (D4); `IslandCertificateTest`, `RestIslandTest` unchanged |
| `runtime/fluid/WorldTopologyLedger` | `2802e5e1` (D18 item 3): `MaterialTotal.plus` reads `speciesMoles()` (fluid plus crystals) for the species totals and the potential energy, so constructed and destroyed material balance when a crystal vessel is removed | `FluidBasis` and `RecoveredSolid` nested; joined-neighbour fencing; events | **R** (one line, main's line 39). The topology unit's encoding does not change: crystal moles land in the existing species columns of `constructed`/`destroyed` | New `WorldTopologyLedgerTest` case (D4) |
| `CreateChemE` (config) | `959ea0d9`: key `fluid.liquidCompressibility` and `FluidOptions.compressibility` removed | Keys `initialNitrogenTemperatureKelvin`/`Pressure` removed (D54); `SlurryTransport.Settings`; `FluidRuntimeDiagnostics.Meter` | **R** (remove the key and the record component), with (a). A stale key in an old `createcheme-common.toml` is dropped by NeoForge's config correction; no compatibility work (AGENTS.md) | compile; `FluidHarnessGameTests` (options read at server start) |
| `client/gui/screens/inventory/FluidDeviceScreen` | `2802e5e1`, `4d26a45a` (D18 presentation, D20 item 5): in the old reservoir layout's right column "Crystals x mol, y L (stay)", "Bulk withdrawal: fluid phases together" (else "all phases together"), and "Fluid volume" / "Fluid mass" beside crystals | Rewritten: tank rail (T, p, Volume, Fill, Mass, then `renderPhaseRatio` and `renderStatus`), outlets page (page 4), kmol and kg side by side, `tr()` keys; the "Adiabatic reservoir / Bulk withdrawal" lines no longer exist | **D** (section C) | MCP dev-client check (C4); no unit test pins the screen |
| `assets/createcheme/lang/en_us.json` | `50dfe315`: `material.createcheme.carbon_dioxide` = "Carbon dioxide" | Rail, outlet, phase and status keys of 0.7.0 to 0.8.0 | **R** + new crystal keys (C3) | MCP check (no test catches a missing key) |
| `science/column/v3/V3NeuralRegistry` | `7bf22842` (P6 consumers, D20): `bind` returns `UNAVAILABLE` for a package that admits crystals (classical initialization, never a fluid-only projection) | `V3NeuralModels` folded in, `TransformerArtifact` nested; `bind` keeps the same guard shape (main line 78 to 81) | **R** (3 lines after the component check); needs `MaterialCatalog.Package.crystals()` from the data-layer port | `CrystalPackageConsumersTest` |
| `science/column/v3/thermo/V3CatalogPropertyPackage` | `e4d355f2` (D6, D9): a spine package is refused with `IdealGasFitUnavailable`; `7bf22842`: a package with crystals is refused first with `CrystalPhasesUnavailable(packageId, crystals, "the V3 column")` | unchanged | **R** (4 lines); needs `Package.spine()`, `Package.crystals()` and the two exception types (data layer) | `CrystalPackageConsumersTest`, `SpineNetworkPathTest.theColumnRefusesASpinePackageTyped` |
| `materials-index.json` | `50dfe315`, `6ec5c2cb`: 13 new entries (CO2 component, `crystals/carbon_dioxide_i`, `group_interactions/eppr78_2022`, `interactions/pilot_cryogenic`, `packages/pilot_cryogenic`, four `properties/pilot_*`, four `spine/*`) and two lines reordered | `presets/column_tia_juana_nitrogen.json` added | **R** (union; order is free: `verifyMaterialIndex` compares sorted lists) | `verifyMaterialIndex` (runs with `processResources`) |
| `runtime/fluid/TankOutlets` (not touched by the branch) | none | Bottom-port level head `g * liquidMass * H / V` (phase-ports decision D9), shown on the outlets page | **T**, or **D** if OD-7 (a) is chosen (the science layer owns the solver's matching `LEVEL_HEAD` row) | `TankOutletsTest` |

## B. Formats after the port

### B1. Numbers to declare

| Format | Base `f9d6be1` | Branch HEAD | Main `ffeafe8a` | Ported branch | Why |
|---|---|---|---|---|---|
| Fluid checkpoint (`FluidCheckpointCodec.VERSION`, core `FluidFormat`) | 4 | 6 (5 = no compressibility, 6 = crystals) | 8 (6 ports, 7 end flows, 8 no module ledger) | **9** | The island unit gains the crystal stocks and (with the direct liquid path) loses the compressibility; the core `Packages` entry loses `Compressibility` |
| Island/topology unit header (`UNIT_FORMAT`, shared by both unit kinds) | 1 | 3 | 1 | **2** | The island unit's node state and header fields change; the topology unit's content does not, but the header constant is shared |
| Fluid menu protocol (`FluidNetwork.PROTOCOL`) | `fluid-4` | `fluid-4` (not bumped) | `fluid-9` | **`fluid-10`** | `FluidView.State` gains `crystals` and `crystalVolume`; `MenuData` gains the crystal check |
| Column wire schema / block `DATA_VERSION` | 15 / 12 | unchanged | 16 / 12 | **16 / 12** | `CrystalPhasesUnavailable` is raised at package resolution; nothing on the column wire or in the column NBT changes |
| Client mixture / column preset files | 1 / 1 | unchanged | 1 / 2 | **1 / 2** | unchanged |

Staging (OD-11): if the direct liquid path (compressibility out) and the crystal coupling reach `main` in the same merge,
one bump to 9 covers both. If they merge separately, each merge bumps once (9, then 10) and each needs its own fresh
world. No migration in either case.

### B2. What the branch's format 6 added, and where it lands in main's format 8

| Item | Branch (format 6) | Main format-8 location | Port |
|---|---|---|---|
| Crystal inventory per node | `state(FluidUnitIO.Writer, graph)`: after `solids(w, inventory.solids())`, `crystals(w, inventory.crystals())` = varint count, then per stock `text(id)`, `varint(species index)`, `f64(moles)`; reader `crystals(Reader, model, basis)` with the four checks | `FluidCheckpointCodec.state(Writer, PassiveNetwork)` (main line 338ff.) and `state(Reader, Topology, model)` (main line 468ff.); helpers beside `solids(Writer, ...)` (line 350) and `solids(Reader, ...)` (line 490); `Reader.invalid(...)` exists | Same bytes and order; read check tightened to `t.kinds[n] == RESERVOIR`; `new PassiveNetwork.Inventory(volume, moles, energy, solids, crystals)`; then `state = model.withCrystals(state, crystals)` |
| Crystals in the related graphs | Implicit: the anchor graph, the certificate base and the certified interval's start graph are written by the same `state(...)` over the shared topology | Same: main's `related(...)` and the certificate block call `state(...)` | Nothing extra |
| Crystals in the last interval (boundaries, pipe transfers, end flows) | None: crystals are never transported (D18 item 3, D20 item 1) | `BoundaryTransfer`, `PipeTransfer.Stream`, end flows (format 7) | Nothing |
| Certificate | Stationarity gate and replay keep crystals; the saved `Signature` and `graphIdentity` are unchanged | `IslandCertificate.Saved(kind, since, horizon, interval, signature)`; identity digest `createcheme-certificate-graph-identity-2` | Gate and replay R (section A); signature unchanged. A restored certificate replays a crystal vessel exactly because the crystals are constant over a certified interval |
| Ledger material totals | `MaterialTotal.plus` counts `speciesMoles()` | Topology unit: `total(body, world.constructed(), tables)` / `destroyed` | Encoding unchanged; the one-line R in `WorldTopologyLedger` |
| Package key | `PackageKey(packageId)`; `Packages` entry without `Compressibility`; unit without the f64 | `PackageKey(packageId, compressibility)`; `Packages.Compressibility`; unit f64 after the package ref; `manifest` digests it | R with the direct liquid path (else keep main's) |
| Views | `FluidView.State.crystals`, `crystalVolume` (Gson in the live payload) | `FluidView.State(..., solids, capacity)`; `LiveData` JSON | D (section A); `fluid-10` |
| Archived gameplay captures (`37235698`) | Production reader with `ArchivedInventory` (4 fields) | Test-side reader in `McpGameplayRegressionTest` with its own `Inventory(volume, moles, internalEnergy)` record, building `new PassiveNetwork.Inventory(..., SolidInventory.EMPTY)` | X. Keep the science port's 4-argument `Inventory` constructor for this test, or pass `CrystalInventory.EMPTY` explicitly there |

### B3. No-compatibility rule

- Formats 1 to 8 are refused with the existing fresh-world message; the tag is left untouched (`olderFormatsAreRefused...`
  with `{1,2,3,7,8}`). An island unit of format 1 is refused by `requireHeader` with the same instruction.
- No absent-field normalisation: a format-9 unit always carries the crystal count (0 for none).
- Round trips required by AGENTS.md: clocks, material (fluid, energy, particles, crystals), pending events (fences),
  fallback allowance, anchor, last interval; the branch's two crystal round-trip tests prove exactly this and move to
  main's `FluidCheckpointCodecTest`.
- Stale saves and worlds: the P6 dev world (`run/mcp-client/saves/p6-pilot-20260925/`, format 6) and the P6b probe input
  (`research/.../p6-pilot-acceptance/world-final/`) cannot be loaded on the ported build. `tools/p6b-runtime-failures/`
  (`P6bWorldProbe`) and `tools/p6-pilot-acceptance/` (`P6WorldIslandProbe`) read them; they stay historical.

## C. Presentation and GUI

### C1. Branch changes against main's engine-owned presentation rule (AGENTS.md, amended 2026-09-24)

| Branch change | Rule clause | Verdict on main |
|---|---|---|
| `FluidView.State.crystals` filled by `State.from` from the island's presentation snapshot | Snapshots produced by the engine on its schedule | Compliant: main's `FluidPresentation.flush` builds each view once per bucket from one island read |
| Block entity and menu receive the crystal only at the island's bucket (`CrystalIslandRuntimeTest`: deliveries at tick % 100 = 1, nothing on subscribe or load) | Nothing pushed from a handler, load or open | Compliant; the test moves to main (D3) |
| Menu open shows the crystal | Opening may replay the last published snapshot, a read of cached data | Compliant: main's `replayOnOpen` replays `publishedMenuData` built at the last bucket; no crystal is recomputed on open |
| Screen sums the stocks, converts mol to kmol and kg, shows the crystal volume | The screen shows what the server sent | Compliant: display arithmetic on delivered values, like main's kmol/kg columns (molecular weights come in `StaticData`) |
| Equilibration (deposition, sublimation) runs in the worker's interval solve | No process work on the tick loop | Compliant (science layer, D18 item 4) |
| D18 item 5 "a held island takes no events" | Owner decision 2026-10-15 (`acceptAtFence`) | Superseded on main: a held crystal island accepts a fenced topology event at its committed tick with its inventory, crystals included, unchanged. The test asserting the old rule is rewritten (D3) |

### C2. Where the crystal goes on main's screen (tank rail, outlets page)

| Place on main's screen | Today on main | With crystals (recommended) |
|---|---|---|
| Tank rail values (`renderDeviceRail`, `RESERVOIR` branch) | Temperature, Pressure, Volume (L), Fill (%), Mass (kg) | With crystals only: "Fluid volume" and "Fluid mass" (D20 item 5; see OD-12 on the wording), Fill including the crystal volume (A, `FluidView`), then two new rail lines: "Crystals (stay in the tank)" = `x kmol · y kg` (moles from the stocks, kg = mol x molecular weight of the stock's component) and "Crystal volume" = `z L`; hover hint `crystals.hint`. Without crystals the rail is unchanged |
| Phase ratio block (`renderPhaseRatio`, tank hint "volume fractions ... this tank holds") | Organic, aqueous, vapor, solids (inert particles) | OD-1: a fifth "Crystals" segment and legend entry for tanks, so the percentages describe what the tank holds |
| Status / errors block (`renderStatus`, `statusText`) | Passes `HELD`/`ERROR`/`WAITING` lines through | No change: the typed crystal holds (`CRYSTAL_PHASES`, the water-domain park, the round deadline of D21) already read in full here |
| Overview phase tabs (Organic / Aqueous / Vapor / Solids) | Component tables with %, kmol, kg | Solids tab keeps inert particles only (D20 item 2; P6 check "the Solids page lists no crystal"). OD-2: a fifth "Crystals" tab or none |
| Outlets page (page 4) | Top "drawing gas", side "drawing all phases together", bottom "drawing organic/aqueous phase", head | When the tank holds crystals: a side outlet reads "drawing fluid phases together (crystals stay)" (new key; the branch's "Bulk withdrawal: fluid phases together" moves here); the page hint gains "Crystals deposited in a tank stay in it; no outlet draws them." Bottom head: OD-7 |
| Single status line (non-tank kinds), pipe rail | unchanged | Nothing: pipes, junctions and boundaries hold no crystals |
| Generator Composition page | Server presets, then client mixtures whose components are all on the network axis | Nothing in code; pilot mixtures come as client files (E2, OD-5) |

### C3. New or changed translation keys (all in `lang/en_us.json`)

`material.createcheme.carbon_dioxide` (branch, R); `gui.createcheme.fluid.fluid_volume` "Fluid volume";
`gui.createcheme.fluid.fluid_mass` "Fluid mass"; `gui.createcheme.fluid.crystals` "Crystals (stay in the tank)";
`gui.createcheme.fluid.crystal_volume` "Crystal volume"; `gui.createcheme.fluid.crystals.hint` "Deposited crystals (for
example dry ice) of the last published state. They stay in this tank: no outlet, pump or filter draws them.";
`gui.createcheme.fluid.outlet.drawing_fluid` "drawing fluid phases together (crystals stay)"; the outlets hint text;
with OD-1 `gui.createcheme.fluid.phase_short.4` "Crystals" and the tank hint; with OD-2 `gui.createcheme.fluid.phase.4`
"Crystals". A crystal's display name is its component's `MaterialName` from `StaticData` plus " (solid)"; no new wire
field is needed.

### C4. What to verify in the dev client through the MCP bridge (`runMcpClient` lane, bridge only)

| Sub-check | Evidence to record |
|---|---|
| Fresh world on the ported build | log `fluid_world status=LOADED format=9 ...`; the P6 world refused if tried (do not try on the only copy) |
| First open "Waiting for the next simulation update."; reopen shows the last delivered view; values change in bucket steps | view time on the screen advances in ~5 s steps |
| Tank rail with crystals: Fluid volume, Fill (incl. crystals), Fluid mass, Crystals kmol · kg, Crystal volume L | screenshot of a frosted N2/CO2 tank and of a liquid-methane freeze-out tank |
| Tank rail without crystals unchanged (Volume, Mass) | same tank before deposition |
| Phase ratio block (OD-1) | percentages sum to 100 with the crystal segment |
| Solids tab lists no crystal (D20) | screenshot |
| Outlets page wording with crystals (side "fluid phases together", top gas, bottom liquid, head) | screenshot of a tank with top, side and bottom connections |
| Basis toggle (mol/mass) and C/K toggle leave the crystal kmol/kg values consistent | two screenshots |
| Typed holds read in full in the status block (`CRYSTAL_PHASES`; water below 273.16 K, parked; round deadline) | screenshots |
| A held crystal island accepts an edit at its committed tick (acceptAtFence) | reply "Applied", status changes |
| Narrow window (427 x 240 GUI px): the longer tank rail scrolls | screenshot |
| Save and reload: crystals, clocks, a pending event, certificates | log `certificates_saved/restored`, rail values before and after |

## D. Tests and GameTests

### D1. Branch-modified tests that main deleted

| Test | Branch change | Main | Disposition | Checks to re-home |
|---|---|---|---|---|
| `fluidGameTest/.../FluidModuleGameTests` | `959ea0d9`: `forNetwork(..., 1e-9)` -> `forNetwork(...)` (2 lines) | Deleted with the causal modules (GameTests 33 -> 31) | **X** | none |
| `runtime/fluid/BufferedCommitTest` | same, 1 line | Deleted | **X** | none |
| `runtime/fluid/CausalModuleCoordinatorTest` | same, 3 lines | Deleted | **X** | none |
| `runtime/fluid/ModuleTransferPlannerTest` | same, 1 line | Deleted | **X** | none |

### D2. Branch-modified tests that main changed

| Test | Branch change | Main change | Disposition | Proving / note |
|---|---|---|---|---|
| `runtime/fluid/FluidCheckpointCodecTest` | `959ea0d9`: arity; the `2e-9` compressibility refusal removed (no compressibility in the key); `2802e5e1`, `0a8e5442`, `12cf449c`: `aCrystalInventoryRoundTripsWithItsIslandsClocksMaterialEventsAndOptimisationState`, `aLiquidSolidVesselRoundTripsWithItsIslandsClocksMaterialEventsAndOptimisationState` | `Checkpoint(List)`, `CheckpointImages`, `ApproximationAnchor.Rejected`, `portsACompressorAndARefusedMoverRoundTripExactly` (asserts the literal 8), topology round trip | **T** + **R** arity and the removed `2e-9` line + port the two crystal tests (drop the `BufferedTransfers.Snapshot` argument; assert `FluidCheckpointCodec.VERSION`); change the literal 8 to `VERSION` | the round trip AGENTS.md requires |
| `runtime/fluid/FluidCheckpointFormatTest` | arity; `formatFiveAndOlderAreRefused...` `{1..5}`; unit-format refusal byte 2 -> 1 | `olderFormatsAreRefused...` `{1,2,3,7}`; `otherFormat[4]=2`; `CertifyingRig` | **T** + **R**: `{1,2,3,7,8}`; `otherFormat[4]=1` with `UNIT_FORMAT` 2; `forNetwork(..., 1e-9, 10)` -> `(..., 10, DEFAULT_TRACE_CUTOFF_MOLE_FRACTION)` | |
| `runtime/fluid/FluidCheckpointStoreTest` | arity | module ledger gone | **T** + **R** arity | |
| `runtime/fluid/McpGameplayRegressionTest` and fixtures | arity (3 lines) | test-side format-3 reader; fixtures carry an ignored `compressibility` field | **T** + **R** arity; fixtures unchanged | |
| `runtime/fluid/SolidRuntimeTest` | arity; `SolidTransportSettings` | `SlurryTransport.Settings` | **T** + **R** arity | |
| `runtime/FluidThermoDomainHoldTest` | arity | thermo-domain park | **T** + **R** arity | |
| `runtime/FluidFallbackQualificationTest` | arity; the anchor-refusal case changes the model by one cp coefficient of `crude_pc04` instead of a compressibility of 1.1e-9 | `FallbackAllowance.Policy`, `ApproximationAnchor.Rejected` | **T** + **R** (same replacement) | |
| `runtime/fluid/IslandCoordinatorTest`, `RetainedSolverTest`, `IslandSchedulerTest`, `PhysicalFluidTopologyTest`, `PropertyReloadCoordinatorTest`, `FluidPropertyReloadGuardTest`, `WorldTopologyLedgerTest`, `runtime/FluidIslandCommandTest` | arity only | various (acceptAtFence, joined fencing, nested types) | **T** + **R** arity | |
| `runtime/fluid/FluidDeviceSpecDomainTest` | arity; new `nitrogenDevicesReachTenMegapascalsAndCrudeChargesKeepTwo` (10 MPa envelope, P3 WP7) charging a **reservoir** at 110 K, 8 MPa | D54: a reservoir is placed empty (`PassiveNetwork.emptyVessel`) | **T** + **D**: run the 10 MPa case on a `GENERATOR` (compressed liquid nitrogen at 110 K, 8 MPa) | |
| `runtime/fluid/FluidPacketCodecTest` | the out-of-domain pressure 3 MPa -> 12 MPa and its message (10 MPa envelope) | packet split, delivered-view cache | **T** + **R** (main lines 34, 237, 238) + the new crystal case (D4) | |
| `runtime/fluid/ElevatedBlockLineIslandTest` | `8e5274ea`: the pumped line settles on the model's shutoff state (density-scaled pump limit, P3 WP4) | moved onto `BlockLineHarness` | **T**; the expectation is a science-layer result: the science port recomputes it on main's backward-Euler basis | |
| `fluid/support/FluidTestSupport` | `DEFAULT_COMPRESSIBILITY` removed | `model(catalog, packageId, liquidCompressibility)` added (used by about 9 science tests), `COLD_START_SECONDS`, `NEVER` | **D**: drop the constant and the `liquidCompressibility` parameter of `model(...)`, sweep its callers | |
| `fluid/support/LegacyLiquidPath` (branch-added) | the retired 2 MPa reference path for continuity checks | `PhaseRoot` deleted; `PengRobinson78` is test-tree, same package | **D**: `PhaseRoot.LIQUID` -> `PengRobinsonKernel.Root.LIQUID`; only user `DirectLiquidContinuityTest` (science) | |
| `fluid/benchmark/FluidPropertyBenchmarkTest`, `SolverRegressionHarness` | arity | island fixtures removed (chain-100 only) | **T** + **R** arity | |
| `src/test/resources/fluid/regression/chain-100.json` | `ac6ebc15`: re-captured on the P3 engine (WP4 moved liquid volume +1.03e-3) | re-baselined in 0.7.0 (`c7a7dc26`, JDK determinism) | Neither: **re-capture once on the ported engine** (`-PfluidRegressionCapture=true`), a reviewed event named in `CHANGELOG.md` (verification.md) | G2 exact afterwards |
| GameTests `FluidCheckpointGameTests`, `FluidCompletionBudgetGameTests`, `FluidHarnessGameTests`, `FluidPropertyReloadGameTests`, `SolidPhaseGameTests` | arity; Javadoc format | D54 `TankCharge`, queued recoveries | **T** + **R** arity | G5 (31 tests) |
| `fluidGameTest/.../FluidServerBenchmark` | arity; the `INVALID` edit's pressure 3e6 -> 1.2e7 (above the 10 MPa envelope) | 60 s measurement default, `module` profile gone | **T** + **R** (main line 142) | benchmark only |

About 36 test files on main carry the compressibility argument (`forNetwork(..., 1e-9)`, `IslandEntry(..., 1e-9, ...)`,
`FluidTestSupport.model(..., k)`); the sweep is mechanical but must follow the science port's decision on the direct
liquid path.

### D3. Branch-added runtime tests (`git diff --name-status f9d6be1 HEAD -- src/test/.../runtime src/fluidGameTest`)

Only one: `runtime/fluid/CrystalIslandRuntimeTest` (`0a8e5442`, `2802e5e1`). No GameTest was added (30 of 30 on the
branch, 31 on main).

| Test method | What it proves | Problem on main | Re-home onto | Change |
|---|---|---|---|---|
| `theCrystalReachesTheViewsAndMenusOnTheEnginesBucketOnly` | crystal reaches menu and block entity at the island's bucket only; nothing before the commit; Gson round trip of the crystal view | private rig passes `IslandCoordinator.CommitHook.NO_MATERIAL` (gone); `FluidView.State.from(s)` (main needs `capacity`) | `CertifyingRig(CertificatePolicy.defaults(), pilotModel, 0)` (it takes a model) + the anonymous `FluidPresentation.Host` (main's interface is compatible: its new methods have defaults) | `State.from(node.state(), node.inventory().volume())`; move the bare Gson round trip to `FluidPacketCodecTest` through `LiveData`/`MenuData` |
| `aCrystalBesideTwoLiquidsHoldsTheIslandTypedAndItTakesNoEventsWhileHeld` | typed `CRYSTAL_PHASES` hold naming node 101; nothing commits while held; a fence stays pending | the second half asserts D18 item 5, superseded by main's `acceptAtFence` | `CertifyingRig` with the pilot model | Keep the typed-hold half; rewrite the second: `coordinator.fence(event, 150, List.of(1L))`, `assertTrue(coordinator.aligned(event, List.of(1L)))`, committed tick = 150, inventory (crystals included) equal, as `IslandCoordinatorTest.aHeldOwnerAcceptsAFencedEventAtItsCommittedTickWithItsInventoryUnchanged` |

Branch-added science tests on the runtime path (owned by the science port, listed for completeness):
`CrystalDepositionIslandTest`, `CrystalMorphologyPolicyTest`, `CrystalWarmStartTest`, `CrystalFlushBudgetTest`,
`LiquidFullBubblePointTest`, `StagnantJunctionTest`, `WaterIntoCryogenicMethaneTest`, `UnsupportedPhasesIslandTest`,
`CrystalPackageConsumersTest`. Four of them heat, cool or withdraw through `ScheduledTransfer`, the only heat input the
network has (OD-9).

### D4. New tests the port needs (gaps; one gate case each, lean rule)

| Test (class) | Asserts | Rig |
|---|---|---|
| `FluidCheckpointCodecTest.crystalsOutsideAVesselOrOutsideThePackageAreRefused` | a unit with a crystal in a junction or a generator, a crystal the package does not admit (pilot checkpoint read with the bundled model), a wrong species index, a non-canonical order: each refused with its reason; the tag untouched | `CheckpointImages.withUnit` / `withIsland` |
| `FluidPacketCodecTest.aCrystalViewCrossesTheWireAndAnOffAxisCrystalIsRefused` | `LiveData`/`MenuData` with a crystal state round-trips through Gson; a stock whose species index is off the hydrocarbon axis is refused; `PROTOCOL` is `fluid-10` | none |
| `WorldTopologyLedgerTest.aRemovedCrystalVesselBooksItsCrystalSpecies` | `MaterialTotal.plus` of a vessel holding crystals adds `speciesMoles()` and their mass to the potential energy | none (pilot model state) |
| `CrystalIslandRuntimeTest.aDepositedVesselCertifiesAndReplaysItsCrystalsExactly` | after deposition the vessel certifies; a replay (`graphAt`) keeps the crystal stock bit for bit; an interval that changes the crystal does not qualify | `CertifyingRig` |
| `FluidPropertyReloadGuardTest.aChangedCrystalRecordHoldsTheWorld` | editing the CO2-I record (pilot catalog, `PilotCryogenicTestCatalog.parseWith`) gives `HOLD` | none |

### D5. Main's shared rigs and the pilot

| Rig | Takes a model / catalog? | For the pilot |
|---|---|---|
| `CertifyingRig` | yes (`CertifyingRig(policy, model, start)`) | Use directly: `CrystalIslandRuntimeTest`, the certificate case |
| `EventPathWorld` | no: `FluidTestSupport.networkModel()`, `MaterialCatalog.bundled()`, `WorldTopologyLedger.Snapshot.empty(CATALOG)` | Add a constructor `(MaterialCatalog catalog, String packageId, int workers)`; then placed pilot worlds can join `HoldClassWorldsTest` if the plan wants them (water meeting cryogenic methane parked on the water domain; a `CRYSTAL_PHASES` world accepting an edit). Optional under the lean rule |
| `BlockLineHarness` | no (world defaults, bundled) | Not needed for the port |
| `CheckpointImages` | model-agnostic (`Function<PackageKey, model>`) | Use for D4's refusal test |
| `HoldClassWorldsTest` | via `EventPathWorld` | Optional pilot worlds (above) |

GameTests: none of the 31 needs a crystal; a pilot GameTest needs a separate server with a pilot network datapack
because the network configuration is global (`MaterialPresets`: one network only), OD-4. Gates after the port: G1 with
the 33 junction lines and `MIXED_GAS_COST` (both move with the P3 engine: re-record the reference once, reviewed), G2
chain-100 exact after its re-capture, G3, G5 31/31 on a fresh run id.

## E. The owner's test world (after the port and after D21)

### E1. P6 datapack `p6_pilot` on main

| Part | P6 (branch) | Main | Change |
|---|---|---|---|
| `pack.mcmeta` | `pack_format` 48 | 1.21.1 unchanged | none |
| `data/createcheme/materials/networks/default.json` | `package` `createcheme:pilot_cryogenic`, `default_column_preset` `tia_juana`, `fluid_presets` water, nitrogen, methane, ethane, carbon_dioxide | `MaterialPresets.read`: package must be PR78, contain `Nitrogen`, not contain `Water`; the default column preset must be visible; every fluid preset known; `MaterialCatalog` load: component presets on the network axis (or Water), assay presets through `requireSharedFluidPhysics`, nitrogen transport for all phases, a `fluid_domain` | none: the pilot passes all of them (it did on the branch: same checks); the crude assay presets stay out (their physics differ from the pilot) |
| `presets/fluid_pilot_{methane,ethane,carbon_dioxide}.json` | component presets | unchanged schema | none; mixtures move to client files (E2) |
| World creation | copy of `tools/fluid-in-game-rig/template/bench-template` with the datapack in `datapacks/` | same lane (`runMcpClient`); fresh world rule; agents use no desktop input | copy the template again (no fluid data in it); never reuse the P6 world |

### E2. Client mixture presets (main's 0.8.0 feature)

Custom files in `<gameDir>/CreateChemE/mixturepreset/` (game dir `run/mcp-client/`), schema 1, `kind` "mixture",
`package` `createcheme:pilot_cryogenic`, component ids `Nitrogen`, `Methane`, `Ethane`, `CarbonDioxide`, `Water`. The
generator lists a client mixture only when all its component ids are on the network axis, so the bundled crude mixtures
drop out and these appear. Proposed set: "Pilot: N2 + 10 % CO2"; "Pilot: CH4 + 3 % CO2"; "Pilot: CH4 + 2 % C2H6";
"Pilot: N2/C2H6/CO2 40/55/5" (the `CRYSTAL_PHASES` case); "Pilot: water + demo particles" with a `solids` block
(`createcheme:demo_particle`, 100 um) for the particle-beside-crystal path. Store them in the tool folder (OD-5).

### E3. MCP helpers (`tools/p6-pilot-acceptance/mcp/`)

| Helper | On main |
|---|---|
| `bridge.js` | Use the copy in `tools/fluid-tank-column-gui-fixes/scripts/` (the 0.8.0 checks ran on it) |
| `gen.js` | Rewrite. It clicks fixed coordinates (628, 162/226) and button indices 23 (preset cycle) and 21 (Apply) of the old screen. On main: find the rail fields by `enumerate_widgets` label ("Pressure (Pa abs)", "Temperature (°C)"); the temperature unit defaults to Celsius (press the C/K button or type Celsius); mixtures load on the Composition page ("Load mixture", then the choice by label); "Apply changes" by label; wait one bucket (3 to 8 s) before the screenshot |
| `robot.ps1`, `KeyTap.java`, `Poke.java`, `Desk.java` | Desktop input: not for agents (owner incident 2026-09-26); the template world avoids the world-creation clicks |
| `Field.java`, `Flatten.java` | unchanged (also in `tools/mcp-gui-helpers/`) |
| `gates.sh` (P6) | Obsolete for main's gates; use the 0.8.0 `gates-run.sh` pattern (G1 with junction lines, G2, G3, G5) |
| Manual note "reservoirs start as N2 at 298.15 K; configure generators while isolated, a held island takes no events" | Obsolete: tanks are placed empty (D54) and fill from their connections; a held island accepts queued events (acceptAtFence) |
| Block layout | Main's tank compiles a connection on its top face to a gas port, on its bottom face to a liquid port, on a side to a mixed port; the P6 horizontal line gives side ports only. New blocks: `fluid_compressor`, `pressure_control_valve` |

### E4. Scenario rows

Physics-first expectations are qualitative where main's slack vessel (D54: below capacity a tank stands at 101.325 kPa)
changes the P6 numbers, which came from rigid 1000 L vessels.

| # | Source | Set-up on main | Expected | What to check |
|---|---|---|---|---|
| 1 | P6b s.10, rows B / D | water generator 298 K and liquid methane generator 115 to 120 K into one tank, void | the mixture falls below 273.16 K: typed hold "Water ... below 273.16 K ... in <tank>", retried once, then parked until the network changes | status block text; parked (no retries); the fix behind it (`db43b22`, TR-BDF2 companion) is obsolete on main's backward-Euler integrator, so this row re-verifies the typed outcome |
| 2 | P6b s.10, row E | liquid methane 115 K / 200 kPa and CO2 250 K / 250 kPa through a tank to a void | CO2 freezes out of the liquid; fewer soft-budget holds during the flush; after the liquid drains the tank enters D21's near-pure window (hold on the round deadline) unless D21 is implemented | crystal rail lines; holds in the status block; time to the D21 window |
| 3 | P6b s.10, row A | N2 generator 150 K / 300 kPa, then CO2 210 K / 400 kPa into a tank | deposition from N2; once flushed by CO2 the near-pure vessel is D21's case | as row 2; D21's outcome |
| 4 | P6b s.10, pilot chain | two tanks of different mixtures (10 % CO2 in N2 at 250 K; N2 at 140 K) joined through a junction (three pipe pieces) | settle at one pressure, deposition only in the cold tank, no numerical hold | the P6b fix `a2f48d4` is a science-layer port on main's junction holdup; check no "active-set cycle" hold |
| 5 | P6 s.5 rows A, E | as P6 (deposition from N2; freeze-out from liquid methane) | crystals shown, "Fluid volume/mass", Solids tab empty | C4 rows |
| 6 | P6 s.5 save/reload | two save-and-reload cycles with a pending edit | format 9 loads; clocks, crystals, pending event, certificates restored | log line, rail before and after |
| 7 | new: crystals in a tank with top, side and bottom outlets | methane/CO2 freeze-out tank (row 2 feed) with a pipe on top to a void, one on a side to a void, one below to a void | top draws gas, bottom draws the organic liquid, side draws fluid phases together; the crystals stay (D20); the CO2 crystal (about 1560 kg/m3) lies under liquid methane (about 420 kg/m3) | outlets page wording and flows; crystal amount unchanged by draws (it changes only by equilibrium); bottom head (OD-7) |
| 8 | new: compressor on a frosting gas | a tank of N2 + 10 % CO2 at its frost line holding crystals, its top port to a compressor (ratio 3) to a second tank or void | suction pressure falls, so CO2 partial pressure falls and crystals sublime, cooling the suction tank (latent heat 25.2 kJ/mol); the compressed gas is heated and deposits nothing downstream | compressor rail (flow, ratio); suction tank crystals shrink and T drops; no deposit downstream |
| 9 | new: pump on a liquid-methane slurry | the row 2 tank's bottom port to a pump to a void | the pump draws liquid only (D20: crystals stay); when the liquid is exhausted the pump is refused ("pump inlet not liquid") or cavitates | pump status; crystals left with the gas; no crystal in the pump's stream |
| 10 | new: held crystal island accepts an edit | generator with "N2/C2H6/CO2 40/55/5" at 95 K, 2 MPa into a tank: typed `CRYSTAL_PHASES` hold; then warm the generator | the edit applies at the held island's committed tick (acceptAtFence); the replaced island solves | reply "Applied"; status leaves `HELD` |
| 11 | new: empty tank fills cold (D54) | an empty tank fed N2 + 10 % CO2 at 160 K, outlet to a void | the tank fills as a slack bag at ambient pressure, then taut; deposition at the frost point of the actual pressure | fill %, slack-to-taut, crystal onset (depends on G3 below) |
| 12 | new: breaking a crystal tank | break the row 5 tank | its contents, crystals included, leave the world with the device (ledger destroyed totals); no item drops (only filter cake drops, as `RecoveredSolidsItem`) | no drop; ledger totals via the debugger if wanted |
| 13 | optional: pressure let-down | CO2/N2 generator at about 8 MPa, 220 K through a valve set to 0.3 MPa into a tank | Joule-Thomson cooling of a few tens of K may reach the frost point; compute the expectation before the run (physics-first rule) | valve rail, tank crystals |

## F. OWNER DECISIONS

| # | Question | Options | Recommendation |
|---|---|---|---|
| OD-1 | Crystals in the tank's phase-ratio bar | (a) fifth segment "Crystals" for tanks; (b) bar unchanged, crystals only as rail lines | (a): the tank hint says "volume fractions ... this tank holds"; without the segment a frosted tank's percentages omit part of its contents |
| OD-2 | A "Crystals" tab beside Organic / Aqueous / Vapor / Solids | (a) fifth tab with component, %, kmol, kg per crystal; (b) rail lines only | (b) now (one crystal in the pilot; the four-tab row is already narrow at 300 px); (a) at P7 when a package admits a second crystal |
| OD-4 | A pilot GameTest | (a) none: unit tests on `CertifyingRig` plus the MCP test world; (b) a separate `runPilotGameTestServer` with a pilot network datapack in its own run directory | (a) (lean rule; the network configuration is global, so (b) needs a new run configuration and gate) |
| OD-5 | Where the pilot mixtures live | (a) custom files in a tool folder, copied to the test world's game dir; (b) bundled in `assets/createcheme/mixturepreset/` (bundled count test 8 -> 13; they show only on a pilot network) | (a): the pilot is selected only by a dev datapack (D9) |
| OD-7 | Bottom-port level head of a tank holding crystals | (a) count the crystals' displacement: head = rho_L g H (V_L + V_s) / V, the interstitial liquid of a settled bed (crystal weight borne by the floor); (b) keep main's D9 formula g m_L H / V (error V_s / V_L) | (a), in the science layer so the solver's row and the outlets page agree; the settled-bed assumption is the D20 v1 picture (immobile) |
| OD-9 | Main's open item 5: keep or remove `ScheduledTransfer` | (a) keep (test-only producer); (b) remove | (a): four crystal science tests use it as the network's only heater, cooler and controlled withdrawal |
| OD-11 | Staging of the format bumps | (a) one merge, one bump to 9; (b) two merges, 9 then 10 | (a): one fresh world, one chain-100 re-capture, one junction-line re-record |
| OD-12 | Rail wording beside crystals | (a) D20 item 5 as recorded: "Fluid volume", "Fluid mass" (they include suspended inert particles); (b) exact: "Volume without crystals", "Mass without crystals" | (a) unless the owner wants the exact wording; D20 is a recorded decision |

Recorded, not open: D18 item 5 ("a held island takes no events") is superseded on main by the owner decision of
2026-10-15 (`acceptAtFence`); breaking a tank removes its crystals with it, as main does for every tank content.

## G. Dependencies on the other ports (science and data layers)

1. **Direct liquid path** (`959ea0d9`, D9/D12): every compressibility removal in A and D2 follows it. If the science
   port keeps main's global compressibility, all those rows become T and format 9 carries only the crystals.
2. **`MaterialCatalog.Package.crystals()` / `spine()`**, `CrystalPhasesUnavailable`, `IdealGasFitUnavailable`,
   `CrystalInventory`, `CrystalStock`, `FluidThermodynamics.State.crystals()`, `withCrystals`, `crystalComponent`: the
   runtime, view and consumer rows need them first.
3. **Slack vessels (main's D54 wall row)**: the branch's crystal equilibration is a UV answer at the vessel volume (D18
   item 4). On main a vessel below capacity stands at 101.325 kPa with free contents volume, so a slack vessel's
   equilibration is a fixed-pressure (PH) answer, a taut one the UV answer at capacity. This decides `fill` values and
   test-world rows 5, 7 to 11.
4. **Junction holdup** (0.6.0): junctions now hold a small buffer `m_J`; D18 keeps them crystal-free (the codec check
   stays). The P6b junction-donor fix (`a2f48d4`) and the TR-BDF2 companion fix (`db43b22`) target code main replaced.
5. **The 4-argument `PassiveNetwork.Inventory` constructor**: main has 12 production construction sites (codec, spec,
   certificate, `ConservativeTransport`, `PassiveIntervalSolver`, `PassiveStepSolver`, `PassiveNetwork`); each must
   carry `inventory.crystals()` or a site silently drops them. Recommendation for the science port: no 4-argument
   constructor in production (tests may keep one).
6. **Gate references**: the P3 engine moves chain-100 and, likely, the 33 junction lines and `MIXED_GAS_COST`; one
   reviewed re-record each, named in `CHANGELOG.md`.

## H. Effort

| Section | Effort | Main cost |
|---|---|---|
| A. Production files | medium | `FluidDeviceScreen` (D) and the codec (D); the rest is a few lines each |
| B. Formats | small | three constants, Javadoc, refusal tests |
| C. Presentation and GUI | medium | screen design per OD-1/2/12, keys, one MCP session |
| D. Tests and GameTests | medium to large | about 36 mechanical arity edits, 2 ported round trips, `CrystalIslandRuntimeTest` re-homed and rewritten, 5 new gap tests, chain-100 and junction-line re-records with the science port |
| E. Test world | medium | `gen.js` rewrite, 5 mixture files, 12 to 13 scenario rows in one or two MCP sessions |
