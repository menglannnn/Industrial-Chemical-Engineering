# Multi-machine framework: plan (the distillation column first)

Batch `2026-10-26-multi-machine-framework`. Base `600549d9` (main, 0.13.2). Status: Planned; revised twice on 2026-10-26, after the owner's second and third messages; the owner answered Q1 to Q6 the same day (section 14.1). This stage builds the structure only: the design, the construction, the validation and the revert. The column's operation is the next stage. Open owner questions: section 14.2. The drawing of every case, with the GUI states: [COLUMN_ASSEMBLY_CASES.html](COLUMN_ASSEMBLY_CASES.html).

Owner request 2026-10-26 (summary):

1. Make a framework that combines several pieces of equipment into one structure. Start with the distillation column. Today the column is only its main body: the reboiler, condenser, separator, reflux and pumparound exchangers are missing. The player builds them and connects them to the column body with pipes. The column has every designed port ready for connection.
2. Right-clicking a column opens a construction checklist first, not its normal GUI. The checklist shows what is missing against the design, and where to connect what: a simple drawing, and coloured indicators on the ports in the world.
3. The checklist has a button that checks the connections. The player starts the validation.
4. When several pieces of equipment form a chain, the check lets the master equipment take over their controls and carries the indicators on to them.
5. When the construction is complete, the structure counts as one piece of equipment, and the column simulation runs instead of the single components.
6. Mandatory: a condenser (air cooler or exchanger), a separator drum and a control valve (the pressure control valve for now). Optional: a steam ejector, a reboiler, a pumparound pump with its exchanger.
7. Second message:
   - The feed and steam inputs, and the bottoms and side-draw outputs, each pass a control valve.
   - The receiver is a lying drum with a boot. The water leaves from the boot; the oil leaves from the other bottom nozzle.
   - The oil goes to a pump (standard or in-line), then splits into a reflux line and an output line, each with a control valve. The output line's valve defines the unit's top product.
   - A pumparound may use a standard or an in-line pump. A pumparound exchanger takes the pumparound on its shell side.
8. Third message:
   - The column is designed in the calculator first, with its components selected there and the design saved. The constructor loads it. The calculator GUI changes for this. A top pressure below ambient requires an ejector by itself. A typical VDU feed joins the mixture templates.
   - The column GUI draws every component of the design. Missing components are red, with a hover text. The ports have the colours that the pipeline tool shows (pipe-laying branch).
   - "Validate structure" goes through the connected equipment and finds what is missing or misplaced. A misplaced component (for example one turned the wrong way) is yellow, with a hover error.
   - The remaining ports of partly connected equipment are coloured in the GUI and synced with the pipeline tool.
   - A fully validated structure shows the full GUI. The column's function comes in the next stage; this stage deals with the structure only.
   - A break returns the structure to its initial state. The design must not check the structure all the time.
   - The steam ejector stands before the condenser. Exchangers get an inlet and an outlet on both the tube and the shell side, with a direction marker like the pump's. Lying drums get an inlet and outlet specification and a marker.

## 1. Expected behaviour (the plant first)

**The atmospheric plant.** The overhead vapour leaves the column's top head. It condenses in an air cooler or on the shell side of a water-cooled exchanger and drains by gravity into the receiver, a lying drum with a boot. The drum separates three phases:

- The gas that did not condense leaves on top through the pressure control valve. The valve holds the drum's pressure, and with it the column's.
- Free water, from the stripping steam, settles into the boot and leaves as sour water.
- The oil leaves at the bottom, beyond the boot. A pump sends it to a tee: one branch returns to the top tray as reflux, the other leaves as distillate.

**The vacuum plant (third message: the ejector before the condenser).** The column's top runs at the ejector's suction pressure. The ejector compresses the overhead vapour, together with its motive steam, to about the drum's pressure. The condenser and the drum then work near atmospheric pressure, and the off-gas valve holds the drum's pressure, not the column's.

- Why the order matters: at a 40 °C condenser outlet, water condenses only above its vapour pressure, 7.4 kPa. A vacuum column's top runs at 1.3 to 8 kPa (research for batch `2026-08-31-v3-vdu`). In front of the ejector, the stripping steam would not condense.
- The motive steam condenses too, so the boot carries more water than in the atmospheric plant.
- Real vacuum units use two or three ejector stages with intercondensers. The design draws one ejector; the next stage decides what the model needs.
- A real vacuum column usually has no top reflux: a top pumparound cools the top, and the receiver's oil leaves as a product. The reflux line is built all the same (Q2); at a zero reflux ratio its valve stays shut.

**The bottom and the side loops.** A kettle reboiler takes the sump liquid by gravity. It boils part of that liquid with a heating medium in its tube bundle, sends the vapour back under the bottom tray, and passes the rest over its weir as the bottoms product. A steam-stripped column has no reboiler; it takes stripping steam into the sump instead. Each pumparound draws liquid from a tray. A pump at grade sends it through the shell side of an exchanger, and it returns a few trays higher.

**Control valves.** Each stream that enters or leaves the column system passes a control valve, and each valve has one job:

- The off-gas valve holds the receiver's pressure (PC).
- The feed, each steam line and each side draw are flow-controlled (FC).
- The bottoms valve holds the sump level, or the kettle's level behind its weir (LC). The sour water valve holds the boot's interface level (LC).
- Of the two valves after the reflux pump, one sets the reflux and the other holds the drum level (D16, next stage).

The valves mark the unit's boundary. Everything between them is the column system; everything beyond them is outside.

**The player's path.**

1. Design the column in the calculator. Choose the components on its Components page. A top pressure below 101.325 kPa adds the steam ejector by itself. Press "Save design".
2. Import the saved design in the constructor and build the column.
3. Right-click the column. The column GUI draws the plant for this design: every component red ("Missing: ..." on hover), the column's ports in the pipeline tool's colours.
4. Build the components, run the pipes, and fit the control valves and in-line pumps.
5. Press "Validate structure". Within one engine bucket (up to 5 s), connected parts turn teal, misplaced parts yellow (the hover says what is wrong), and missing parts stay red. Ports still to connect keep their colours, in the GUI and in the world while the player holds the pipeline tool. Connected ports turn grey.
6. When every part passes, the column GUI becomes the full GUI (a placeholder in this stage).
7. A break returns the column to the state of step 3.

## 2. What the code does now (the constraints)

| Fact | Where | Consequence for this design |
|---|---|---|
| The calculator has four pages (overview, composition, results, diagnostics). It stores its input when a calculation begins. Domain limits are checked later, in the worker, so a refused input stays stored. | `ColumnCalculatorV3Screen.PAGES`, `ColumnCalculatorV3BlockEntity.tryBegin` | A design needs its own save action, separate from a run (4.3). |
| The constructor imports the nearest calculator's input within 16 blocks: the trays from `ColumnTrays.of(input)` and the diameter. It can also design a column itself. | `EquipmentConstructorBlockEntity`, `EquipmentNetwork.IMPORT_REACH` | The import carries the component design; the constructor's own column design goes (4.5). |
| Every crude package refused column pressures below 50 kPa (`pressure_min_pascal`); Q1 lowered the floor to 1 kPa in all eight crude packages (done). The fluid domain reaches 100 Pa. Below 100 kPa a run starts at 150 kPa and continues down in 10 kPa, then 5 kPa steps (about 26 steps to 1 kPa). The neural start serves only its trained range, 50 to 300 kPa. The VDU model batch is planned, not implemented. | `tjl20.json` and the other packages, `V3OperatingDomainValidator` (`BELOW_PACKAGE_PRESSURE`), `V3ColumnCalculator` (pressure continuation), the transformer sidecar's `designConstraints` | A vacuum design saves, builds and is admitted to a run. Node 0 behind an ejector comes with the next stage (14.4). |
| Mixture templates ("Load mixture") are client JSON files on the `icheme:tjl20_methane` package. Tia Juana Light has 12 pseudo-components with cut edges 84, 135, ... 329, 378, 450, 550, 650 and 750 °C. | `assets/icheme/mixturepreset/`, `materials/components/crude_pc*.json` | The VDU feed is a new template file (4.4). |
| The ambient pressure is 101 325 Pa. | `PhaseLayout.AMBIENT_PRESSURE` | The ejector rule's threshold. |
| Every nozzle role has a port class (VAPOUR, BULK, LIQUID). The pipeline tool's colours for them are on the pipe-laying branch only. | `EquipmentShape.Role` (main); `PipeLayingClient.PORT_COLOURS` (`claude/pipe-laying-system-092ac8`) | The colours move to a shared class (W6). |
| Flow arrows are drawn on the SUCTION and DISCHARGE nozzles of movers only. | `MoverPortMarkings`, `EquipmentShellModel` | Generalised to every kind by a flow sign per role (section 9). |
| An exchanger's tube inlet faces down on the channel, its tube outlet up. Its shell inlet is on top near the back tubesheet, its shell outlet underneath near the channel. | `ProcessUnit` (exchanger) | Condensate drains only on the shell side: the condenser exchanger takes the vapour on its shell side (D15). |
| A lying drum's outlets leave at its front end: the vapour on top, the oil underneath beyond the boot, the water from the boot. Its side connections are the player's; a new drum gets one on top near the far end, for the feed. | `EquipmentShape.nozzles`, `defaultConnections`, `bootFor` (owner decisions 2026-10-25) | The receiver is a lying drum with a boot. Side connections are its inlets (Q3). |
| The column has every port the blueprint needs. A nozzle with no free row on its side is left out silently. | `ColumnStructure.place`, `ColumnStructure.side` | Finding F1; the save refuses such a design (Q6). |
| Drawing-only units meet pipes only in the world. Drums register as reservoirs, constructed pumps as movers. | `FluidDeviceBlock.connects`, `EquipmentStructure.register` | The walk reads shell cells beyond pipe ends, in loaded chunks. |
| Fittings: the Pressure Control Valve (its arrow set by aim), the In-line Pump, check and shutoff valves, filters, sight glasses. One accessory fits a pipe block. | `PipelineFittingItem` | Each control valve is a Pressure Control Valve for now. On process-control (`claude/process-control-fixes`) the item is the Control Valve, bare (fully open) or with a pressure or flow module; the walk counts the valve body by its hydraulic law (`d5710373`) and never checks its module (owner, 2026-10-27). |
| A right-click on a column or a unit opens `VesselScreen` on the client, with no packet. | `EquipmentShellBlock.use` | The column's click goes to the server (section 10). |
| The column GUI uses per-player sessions on a `PresentationMailbox`; commands run at the bucket; an opening screen gets a replay. | `ColumnPresentation`, `PresentationMailbox` | The column GUI uses the same pattern. |

## 3. Core idea

1. **The design declares the assembly; the pipes connect it.** The calculator design says which components the column needs. The validation starts at the column's ports and follows the pipes. A unit or fitting of an allowed kind, reached at the right port, fills the slot that the blueprint gives that link. The walk then continues from that unit's other ports, so one press finds a whole chain.
2. **The facts stay small.** The only new fact is the component design on the column. Reports, members, the footprint and the colours are state, which a validation computes again.
3. **The player validates; the engine never polls.** A validation records its footprint. Hooks on the paths that change the world look the footprint up in O(1) and revert the column when something it relied on changes.
4. **Everything runs on the engine schedule.** The button queues a validation. It runs at the next presentation bucket, and its report goes to the open GUIs of that column and to the pipeline tool's watchers. Opening the GUI replays the last report.

**Terms.** Master: the column. Member: a unit or fitting that a validation bound to a slot. Slot: a part of the blueprint. Link: a required pipe path between two ports, or from a port through a boundary valve to outside. Report: the validation's result (a state per slot and link, the port colours, the messages). Footprint: the positions that a report relied on.

Future assemblies (an FCC with its main fractionator, a hydrotreater loop, a boiler with its deaerator) add blueprints. The walk, the matcher, the GUI and the revert are shared.

## 4. The calculator and the constructor

### 4.1 Components page

The calculator gets a fifth page, Components:

| Row | Choice |
|---|---|
| Condenser | Air cooler; Exchanger (shell side) |
| Reflux pump | Standard pump; In-line pump |
| PA i pump, one row per designed pumparound | Standard pump; In-line pump |
| Steam ejector | Derived: "Required: top pressure 5.0 kPa is below 101.325 kPa", or "Not needed" |
| Kettle reboiler | Derived: "Required: reboiler duty 2.4 MW", or "None: sump steam strips the bottom" |
| Receiver, reflux line, control valves | Fixed: a lying drum with a boot; the reflux tee and valve, also at a zero reflux ratio (Q2); the valve list |

"Save design" stores the design with its choices and shows the result: "Saved: the constructor imports this design", or the reason it was not saved. The calculator's format needs no compatibility work (AGENTS.md).

### 4.2 Requirement rules

| Component | Required when |
|---|---|
| Steam ejector | top pressure below `PhaseLayout.AMBIENT_PRESSURE` (101 325 Pa) |
| Condenser, receiver, reflux pump | always (kinds chosen) |
| Reflux line: tee and reflux valve | always, also at a zero reflux ratio (Q2) |
| Kettle reboiler | reboiler duty above zero |
| PA i pump and exchanger | each designed pumparound (pump kind chosen) |
| Off-gas, distillate, sour water (Q4), feed and bottoms valves | always |
| Steam valve i, draw valve i | each designed steam stage and side draw |

### 4.3 Save design

The save checks the structure only:

- `ColumnTrays.of(input)` accepts the trays, feed, draws, steam stages and pumparounds.
- Every designed nozzle fits on the shell (F1, Q6). The message names the nozzle: "Not saved: draw 4 has no free row on the left side of a 4-tray column."
- A column without a reboiler has sump steam. V3 refuses a zero reboiler duty without sump steam, because nothing then boils up at the bottom.

The save does not check the package's pressure floor or any other thermodynamic limit; the run does. Since Q1 the floor is 1 kPa, so a vacuum design is admitted to a run before the VDU model batch.

### 4.4 VDU feed template

A new mixture template, `tia_juana_residue.json`, "Tia Juana Light atmospheric residue (VDU feed)", on `icheme:tjl20_methane`, mole basis. It holds the crude's own pseudo-components from 378 °C up, in the crude's proportions, plus a 5 % tail of the gas oil cut, which a crude column's bottoms carries:

| Component | Cut (°C) | mol % |
|---|---|---|
| crude_pc07 | 329 to 378 | 5.0 |
| crude_pc08 | 378 to 450 | 32.1 |
| crude_pc09 | 450 to 550 | 35.0 |
| crude_pc10 | 550 to 650 | 15.7 |
| crude_pc11 | 650 to 750 | 7.9 |
| crude_pc12 | above 750 | 4.3 |

### 4.5 The constructor

- A column is built from an imported design only. Import takes the nearest calculator's saved design within 16 blocks, with its trays, diameter and components. The constructor's own column fields show the imported design and cannot be edited. Paint and nozzle sizes stay the player's.
- Without a saved design, Import answers "Save a design in the column calculator first".
- The component design is saved on the column's design (`ColumnTrays`, inside the shape) as `components`: the condenser kind, the reflux pump kind, one pump kind per designed pumparound, and two flags (ejector, kettle). The flags are resolved at the import, so a later change of the rules never changes a built column's requirements.

## 5. The column blueprint

### 5.1 Slots

| Slot | Kinds | Required | Next stage (V3) |
|---|---|---|---|
| COLUMN | COLUMN (the master) | always | Trays, feed, draws, steam, pumparounds, diameter |
| EJECTOR | EJECTOR | with the ejector flag | Top pressure below the drum's |
| CONDENSER | AIR_COOLER, or EXCHANGER with the vapour on its shell side | always; the kind of the design | Node 0 cooling |
| RECEIVER | DRUM, lying, with a boot | always | Node 0 phase split |
| REFLUX_PUMP | PUMP or an In-line Pump, the kind of the design | always | None (hydraulics) |
| REBOILER | KETTLE | with the kettle flag | Reboiler duty |
| PA_PUMP i, PA_COOLER i | the pump kind of the design; EXCHANGER with the pumparound on its shell side | each designed pumparound | Pumparound i |
| Valves: OFFGAS, REFLUX, DISTILLATE, WATER, FEED, BOTTOMS; STEAM i, DRAW i for each designed one | a Pressure Control Valve | always, or as stated | The boundary streams |

### 5.2 Links

| Link | Path | On the path |
|---|---|---|
| Overhead | COLUMN.VAPOUR_OUTLET → condenser inlet; with an ejector: → EJECTOR.SUCTION, EJECTOR.DISCHARGE → condenser inlet | pipes only |
| Condensate | condenser outlet (PROCESS_OUTLET or SHELL_OUTLET) → a receiver side connection | pipes only |
| Off-gas | RECEIVER.VAPOUR_OUTLET → off-gas valve → outside | one valve |
| Sour water | RECEIVER.BOOT_DRAIN → water valve → outside | one valve |
| Oil | RECEIVER.LIQUID_OUTLET → reflux pump inlet | pipes only |
| Reflux | pump outlet → tee → reflux valve → COLUMN.REFLUX | one tee, one valve after it |
| Distillate | the tee → distillate valve → outside | one valve |
| Feed; steam i | outside → valve → COLUMN.FEED; COLUMN.STEAM i | one valve |
| Draw i | COLUMN.DRAW i → valve → outside | one valve |
| Pumparound i | COLUMN.PUMPAROUND_DRAW i → PA pump i and PA cooler i's shell side, in either order → COLUMN.RETURN i | one pump, one shell pass, no valve |
| Reboiler | COLUMN.BOTTOMS → KETTLE.FEED; KETTLE.VAPOUR_OUTLET → COLUMN.VAPOUR_RETURN | pipes only |
| Bottoms | KETTLE.LIQUID_OUTLET, or COLUMN.BOTTOMS without a kettle → valve → outside | one valve |
| Utilities | exchanger tube sides; the kettle's bundle; EJECTOR.MOTIVE | advice (D2) |

- **Outside** is what the path reaches beyond its boundary valve: a tank, a generator, a void, another structure, a branch, or 16 pipe blocks (`EXTERNAL_REACH`). An open end is not outside.
- **Valve arrows** follow the stream: out for products, in for the feed and steam, from the tee towards the column for the reflux.
- **No blind ports** (Q9 (a)): a column without a kettle has no VAPOUR_RETURN nozzle, so every nozzle of a designed column has its line. REFLUX is always piped (Q2).
- **Direction**: the validation reads each unit's inlets and outlets from the same flow signs as the markers (section 9), the pump direction, the exchanger side and the valve arrows.

### 5.3 States and colours

| State | Colour | Hover text (example) |
|---|---|---|
| OK | teal | "Reflux pump: connected" |
| MISSING | red | "Missing: Reflux pump. On the line from the drum oil outlet." |
| OPEN_END | red | "Missing: Condenser. The overhead pipe ends at 131, 80, -30." |
| NOT_LOADED | red | "Not checked: the chunk at 8, -2 is not loaded." |
| WRONG_PORT | yellow | "Reflux pump: installed backwards: its discharge faces the drum. Turn it round." / "PA 1 exchanger: the pumparound is on the tube side. Use the shell inlet and outlet." |
| WRONG_KIND | yellow | "Receiver drum: a standing drum. Build a lying drum with a boot." |
| VALVE_REVERSED, EXTRA_VALVE, WRONG_ORDER, BRANCHED | yellow | "Feed valve: its arrow points out of the column." |
| SHARED | yellow | "Condenser: part of the column at 90, 64, 12." |

A port's square has its port class colour while it still needs a connection, grey when connected. The column's status: NO_DESIGN, NOT_VALIDATED, CHECKED (n connected, m misplaced, k missing), COMPLETE.

### 5.4 Placement rules (Q5: not enforced in this stage)

The validation does not check elevations in this stage (Q5). The rules stay recorded for a later stage. Each compares nozzle elevations, physics first:

- The condenser outlet stands above the receiver inlet: condensate drains by gravity.
- The reflux pump's suction stands below the drum's oil outlet: the oil is at its bubble point and flashes in the pump without static head.
- A pumparound pump's suction stands below its draw nozzle, for the same reason.
- The kettle's feed stands below the column's bottoms nozzle: the sump feeds it by gravity.

### 5.5 The cases

The drawing covers every combination: condenser (2) × top pressure (2) × column bottom with tray steam stages (7) × reflux pump (2) × side draws 0 to 4 (5) × pumparounds 0 to 4, each pump standard or in-line (31). That is 8,680 combinations; the reflux line is in every one (Q2). One blueprint with conditional slots covers them all.

## 6. Validation

**The walk** (server thread). `AssemblyWalker` builds a port graph lazily from the registry's latest projected registrations (`PhysicalRegistry.at`, `Device.contacts`, `joins`) and the shell cells beyond pipe ends in loaded chunks. Fittings are recorded in path order with their direction. Terminals: a structure port; a tank, generator or void (outside); an open end; `EXTERNAL_REACH` beyond a boundary valve; an unloaded chunk. The walk never passes through a vessel or a constructed mover, and visits at most `MAX_WALK` (8192) devices.

**The match** (Minecraft-free). `AssemblyMatcher.match(blueprint, components, graph)` binds the master, then works through the links in blueprint order. A unit of the right kind reached at the right port fills the link's target slot, and its links follow. The match is deterministic. A unit is bound as soon as one of its ports is reached correctly, so a partly connected unit is teal and its remaining ports keep their colours.

**The schedule.** The button sends `AssemblyValidatePayload(master)`. The handler stores one pending command in the player's session, as `ColumnPresentation` does. At the next bucket the server walks, matches, stores the report and its footprint, and publishes the report to every open GUI of that master and to the tool's watchers. A master gets one validation per bucket. Opening the GUI replays the stored report; without one, the client draws the design from the column's shape at once, all red.

**Cost.** One walk per press. A crude-unit assembly has a few hundred pipe blocks, well under a millisecond. W5 adds a counter gate (devices visited).

## 7. Revert without polling

**Footprint.** A report records what it relied on: each walked pipe block and fitting, with a hash of its structural fields (block kind, axis, joints, fitting kind and direction; no settings), and each member's origin and nozzle pipe blocks. An index maps each position to its master.

**Hooks.** Three paths change what a report relies on. Each does one hash lookup:

- `FluidWorldAuthority`, after `registry.submit`: a placed or removed pipe, an installed or removed fitting, a turned valve. A placement also looks up its six neighbours, because a new pipe that joins a walked line is a branch.
- `EquipmentStructure.place`, `dismantle` and `refresh`: a unit placed, removed or rebuilt over the footprint.
- Chunk load: when a chunk with footprint positions loads, an engine job compares the stored hashes with the blocks (reads only, no walk). This catches edits that bypass the hooks: an external world editor, or a crash between the chunk save and the store save.

Setting edits (a valve's target, a pump's switch) change no structure and revert nothing.

**Rules.**

- COMPLETE: any structural change in the footprint returns the column to NOT_VALIDATED (the owner's rule). The status line says "Structure changed at 131, 80, -30. Validate again."
- CHECKED: a change at a part that the report found (teal or yellow) also returns it to NOT_VALIDATED. A pipe added at an open end or at a free port changes nothing: the player is building, and the colours keep guiding them until the next press.
- Dismantling the column removes its report and its index entries.

**Cost.** No periodic work. Each world change costs one lookup, or seven for a placement; each validation costs one walk.

## 8. The column GUI

| Status | The GUI shows |
|---|---|
| NO_DESIGN (a column built before this batch) | "This column has no component design. Rebuild it from a design saved in the column calculator." |
| NOT_VALIDATED | The plant drawing for the design, every component red; the column's ports in their class colours; "Validate structure". |
| CHECKED | Teal, yellow and red parts; remaining ports coloured, connected ports grey; "Checked: 7 connected, 2 misplaced, 16 missing"; "Validate structure". |
| COMPLETE | The full GUI, a placeholder in this stage: "Structure complete. Column operation comes in the next stage." A Structure tab shows the drawing, all teal. |

```
+------------------------------------------------------------------------------+
| Distillation column at 120, 64, -30     Checked: 7 connected, 2 misplaced,   |
|                                         16 missing                           |
| +--------------------- plant drawing ----------------------+ +-- parts ----+ |
| | generated from the design, in the layout of               | | Condenser  ok| |
| | COLUMN_ASSEMBLY_CASES.html; teal, yellow, red parts;      | | Reflux pump !| |
| | port squares in the pipeline tool's colours               | | Feed valve  X| |
| +-----------------------------------------------------------+ +-------------+ |
| [ Validate structure ]   Queued: the engine answers within 5 s.               |
+------------------------------------------------------------------------------+
```

- The drawing reuses `ColumnDiagram` for the column body, with the overhead system at the top right, the pumparound loops at the right, the feed, steam and draws at the left and the kettle at the bottom left.
- Hovering over a part, a list row or a port shows its text from 5.3. A port's text names its role, class and partner: "Reflux: bulk port, to the reflux valve line".
- The list gives each part's state in words, so colour is never the only cue.

## 9. Direction markers and the pipeline tool

**Direction markers.** Each nozzle role gets a flow sign (in, out or none), and `EquipmentShellModel` draws the pump's arrows (`MoverPortMarkings`, generalised to `PortMarkings`) on every kind:

- Exchanger (requested): TUBE_INLET and SHELL_INLET in; TUBE_OUTLET and SHELL_OUTLET out.
- Drum, lying or standing (requested): VAPOUR_OUTLET, LIQUID_OUTLET and BOOT_DRAIN out; side connections in (Q3).
- By the same rule: air cooler (PROCESS_INLET in, PROCESS_OUTLET out); kettle (FEED and TUBE_INLET in; VAPOUR_OUTLET, LIQUID_OUTLET and TUBE_OUTLET out); ejector (SUCTION and MOTIVE in, DISCHARGE out); column (FEED, REFLUX, RETURN, STEAM, VAPOUR_RETURN in; VAPOUR_OUTLET, DRAW, PUMPAROUND_DRAW, BOTTOMS out); heaters' inlets and outlets. Tanks and spheres: none, because their side connections serve either way.

The markers are drawings: the fluid network does not enforce a nozzle's direction, as a real nozzle does not. The validation reads the same signs. No layout and no shape field change under Q3 (a).

**The tool sync** (W7, after the pipe-laying batch merges).

- While a player holds the pipeline tool, the client watches the assemblies within the tool's range (48 blocks). The server replays their stored reports and sends new ones as they are published. Putting the tool away ends the watch. Holding the tool is the request (engine-owned presentation rule).
- The tool's squares keep their class colours. On an assembly's remaining ports they get a frame in the same colour; a misplaced part's ports get a yellow frame.
- Aiming at such a port adds the assembly line to the tool's label: "Column 120, 64, -30: oil outlet → reflux pump suction (missing)". As built (W7 check): "pipes to Reflux pump (Missing)" after the tool's own text, which names the nozzle; the coordinates ran the action bar's one line off the screen.
- The full case has 73 ports, inside the tool's limit of 256 marks.

## 10. Takeover in this stage

- A right-click on a column goes to the server, which opens the column GUI.
- A member that a validation bound opens its master's GUI with its row selected. A button opens the member's own screen.
- A unit that two columns reach stays with the first; the second column's report says SHARED.
- No setting locks and no pump holds in this stage: nothing runs yet. The locks and the master's control come with the column's operation.

## 11. Saves

- **Facts**: `components` on the column's design (4.5). The field is append-only; absent (the default) means no component design, which is every column built before the field existed. Enums are saved by name. Corpus image `src/test/resources/compat/equipment/shapes-2.snbt`, read by `EquipmentShapeCompatTest`.
- **State**: the store `icheme_assemblies` (level saved data) holds the reports, the footprints and the index, with its own version. It validates what it reads and round-trips what it carries. A version mismatch or a damaged state drops it; each column with a component design then queues one validation when its controller loads, as an engine job, which rebuilds its report and footprint.
- **The calculator's saved design**: the calculator block's format, without compatibility work (AGENTS.md).
- **Wire**: a new registrar `assembly-1`; the calculator's payloads change freely.

## 12. Code map

**New**

| Class | Role |
|---|---|
| `science/equipment/ColumnComponents` | The component design: choices, derived flags (4.2), codec |
| `science/assembly/Blueprint`, `ColumnBlueprint` | Slots, links, requirements from `ColumnComponents` (Minecraft-free) |
| `science/assembly/PortGraph`, `AssemblyMatcher`, `AssemblyReport` | Port graph, the deterministic match, states, colours, texts |
| `science/equipment/PortMarkings` | Flow sign per role and kind; the arrows (from `MoverPortMarkings`) |
| `runtime/assembly/AssemblyWalker` | Registry plus loaded shell cells → port graph |
| `runtime/assembly/AssemblyStore` | Level saved data `icheme_assemblies`: reports, footprints, index (state) |
| `runtime/assembly/AssemblyService` | The validation queue, hooks, revert, chunk-load verification, rebuild after a state drop |
| `runtime/assembly/AssemblyPresentation` | Sessions on a `PresentationMailbox`; replay; the tool watch |
| `network/AssemblyNetwork` | Registrar `assembly-1` |
| `world/inventory/AssemblyMenu` | Master position, clicked structure, selected member |
| `client/gui/screens/inventory/AssemblyScreen`, `client/gui/common/AssemblyDiagram` | The column GUI and its drawing |
| `client/PortColours` | The port class colours, shared with the pipeline tool |
| `assets/icheme/mixturepreset/tia_juana_residue.json` | The VDU feed template (with `index.json` and `en_us.json`) |

**Touched**: `ColumnCalculatorV3Screen`, `ColumnCalculatorV3BlockEntity` and `ColumnV3Network` (Components page, Save design); `EquipmentConstructorBlockEntity` and `EquipmentConstructorScreen` (import only for columns); `ColumnTrays` (`components`); `ColumnStructure` (the fit check); `EquipmentShellModel` (arrows on every kind); `EquipmentShellBlock.use` (server routing); `EquipmentStructure` and `FluidWorldAuthority` (hooks); `IChemE` (registration, chunk-load event, the presentation tick); `en_us.json`. After the pipe-laying merge: `PipeLayingClient` (the tool sync).

## 13. Work packages and gates

| WP | Content | Gates |
|---|---|---|
| W1 | Calculator: Components page, requirement rules, Save design with its checks, the VDU template | `ColumnComponentsTest` (ejector below 101 325 Pa, kettle with duty, reflux line at every ratio, no reboiler without sump steam refused); an F1 case in `ColumnStructureTest`; a test that loads the template |
| W2 | Constructor import only; `components` on the column's design | `EquipmentShapeCompatTest` with `shapes-2.snbt`; an import test |
| W3 | Direction markers on every kind | Layout table unchanged (`EquipmentLayoutGoldenTest`); software renders |
| W4 | Blueprint, matcher, report | `ColumnBlueprintTest` (requirements, the case table); `AssemblyMatcherTest` (one test per state, partial units, the ejector order, pumparound pairing, SHARED) |
| W5 | Walker, store, hooks, revert, chunk-load verification, rebuild | `AssemblyGameTests`: complete; a removed pipe reverts; a pipe added at an open end keeps a partial report; reload keeps COMPLETE with no walk (counter); a dropped store rebuilds with one walk per column; each test removes what it registers. `AssemblyStoreTest` (round trip, validation) |
| W6 | Schedule, menu, packets, click routing, column GUI, `PortColours` | `AssemblyNetworkCodecTest`; `AssemblyPresentationTest` (replay is a cached read, one validation per master per bucket, nothing sent unasked); one in-game check through the MCP bridge (`runMcpClient` lane) |
| W7 | Pipeline tool sync, after the pipe-laying batch merges | `AssemblyToolWatchTest` (watch with the tool in hand, ends without it, replays stored reports) |
| W8 | Merge preparation | `./gradlew test` and `slowGates`; `git ls-files -i -c --exclude-standard` empty; code guide chapter `assembly.md` plus equipment, client-gui and column-v3 updates; CHANGELOG; a minor version, chosen by the owner |

No solver code changes, so the solver campaign and the junction lines are not needed (the owner confirms at the merge).

## 14. Owner decisions

### 14.1 Settled

| # | Decision | Source |
|---|---|---|
| D1 | What the design declares is required. Ejector and kettle follow from the design's numbers, and the reflux line is always required (Q2); condenser and pump kinds are chosen in the calculator. | Message 3 |
| D3 | Reflux pump mandatory, standard or in-line. | Message 2 |
| D6 | A complete structure opens the full GUI, a placeholder in this stage. | Message 3 |
| D9 | A break returns the column to its initial state; no periodic checks. | Message 3 |
| D10 | Ports in the pipeline tool's colours; missing red, misplaced yellow. | Message 3 |
| D12 | The top pressure comes from the design; the ejector stands before the condenser; the off-gas valve holds the drum's pressure. | Message 3 |
| D15 | The condenser exchanger takes the vapour on its shell side: the exchanger's layout forces it (tube outlet faces up). | Layout |
| D18 | Designed side draws and steam stages are required (from D1). | Message 3 |
| Q1 | Every crude package's column pressure floor is 1 kPa (was 50 kPa); done in this batch. The VDU feed template comes in W1. | Owner answers |
| Q2 | The reflux line (tee and reflux valve) is always required, also at a zero reflux ratio. REFLUX is never blind. | Owner answers |
| Q3 (a) | Inlets and outlets are fixed by role, with arrows; a drum's side connections are inlets. | Owner answers |
| Q4 (a), D17 | The sour water line has its valve. | Owner answers |
| Q5, D4 | The placement rules (5.4) are not enforced in this stage. | Owner answers |
| Q6 (a), D13 | The calculator's save and the constructor's import refuse a design whose nozzles do not fit, and name the nozzle. | Owner answers |
| H1 | Nothing in the package data is hashed. Data are identified by declared revisions; a science change bumps its revision. Save-file checksums and island graph signatures stay. Done in this batch (README history). | Owner, mid-batch |
| Q7 (a) | Every record declares its own revision and an edit bumps only the record it changes. The column identity (`scientificRevision`, about 900 characters) lists the package and each property, interactions, group-matrix and water record the column reads; the fluid identity (`fluidRevision`, about 3,100 to 3,300) adds each viscosity by its own revision, the ice and the fluid reference records. The three `interactions/` records got their revision. Departures from the option's wording, each because of what reads the data: viscosities are in the fluid identity only (the column never reads them; a viscosity edit keeps column results and the neural model, as before H1); spine and crystal records stay in the record-level `spineRevision` (the column refuses such packages); assays (compositions) and solids (the fluid facts carry their own revisions) are in no identity. The calculator shows `<package>@<revision>`. | Owner answers 2 |
| Q8 (a) | AGENTS.md, "Saves and data formats": material data are identified by declared revisions; a value change bumps its record's `revision` (a viscosity's own for a viscosity edit); a package's only when the package record changes; revisions are tokens of at most 64 characters. | Owner answers 2 |

| Q9 (a) | The reboiler's vapour return only on a column whose design has a kettle; a column stripped by sump steam has none. A column with a kettle, or without a component design, keeps its nozzles where they were. The remaining misfits stay refused at Save design. Done in this batch (README history). | Owner answers 3 |

"Owner answers": the owner's reply of 2026-10-26 to Q1 to Q6. "Owner answers 2": the owner's reply to Q7 and Q8, the same day. "Owner answers 3": the owner's reply of 2026-10-27 to Q9.

### 14.2 Open questions

None. Q9 as it was asked:

**Q9. The reboiler's vapour return on a column without a kettle (F1, widened in W1). Answered (a), 2026-10-27.**

1. Expected behaviour. A column stripped by sump steam has no reboiler, so it has no reboiler vapour return. The steam enters the sump below the bottom tray. A column with a kettle has the vapour return, and may also take sump steam.
2. What the code does. `ColumnStructure` always places the vapour return on the left side, 0.75 of the way up the sump (the sump is 2 m high up to D 4 m). Sump steam goes on the same side, 0.3 m under the sump's top. Each nozzle needs its own row, one free block from every other pipe, clear of the platforms and ladders. In the W1 probe (D 1 to 8 m, 1 to 10 trays, 90 designs), 51 designs leave a nozzle out. With sump steam and nothing else, 17 of 30 designs fail, and 13 of those lose the steam nozzle. Plain short columns also lose the vapour return (8 of 30), the reflux (3, all at D 1 m) or the feed (2, at D 4 m with 1 or 2 trays). The bundled crude design (40 trays, D 8 m) places every nozzle. Save design now refuses each of these designs and names the nozzle.
3. Proposal:
   - (a) Recommended: place the vapour return only when the design has a kettle. This needs the `components` fact on the shape (W2). A column without a component design keeps today's layout, so no existing nozzle moves. The remaining failures stay refused at Save design: the player adds trays or changes the diameter.
   - (b) Keep the vapour return on every column. Save design refuses the designs above.
   - (c) Let a crowded side fall back to the back diagonals. This is a larger layout change: the ladders and the manways stand there.

### 14.3 Defaults taken (reversible)

- D2 (a): beyond the boundary valves, and the utilities, are advice.
- D8: the footprint is saved state; a walk at load only after the state is dropped.
- D11: build on main; the port colours move to a shared class; the tool sync (W7) lands after the pipe-laying batch merges.
- Direction markers on every kind by one rule, not only exchangers and drums.
- The constructor builds columns from an imported design only; a column built before this batch asks for a rebuild.
- Takeover in this stage is the GUI redirect only.
- Only structural changes revert; a partial report survives additions at its open ends.
- The Components page is a fifth calculator page; the save checks the structure only.
- The VDU template's 5 % gas oil tail.
- Q1: no vacuum column preset yet (Q1's old option (b)). Solved today, it would put node 0 at the top pressure, where the steam cannot condense below 7.4 kPa at 40 °C; it waits for the VDU model batch.
- Q5: "not enforced" means no placement check and no placement message in this stage.
- W3 order: the flow table (`PortMarkings`) landed with W4, which reads it; the arrows are drawn after W5 and W6. Exposed necks are 0.15 to 1.15 m long and the pump arrow needs 0.28 m, so the drawing needs its own design and software renders.
- Matcher (W4): accessories other than a control valve or an In-line Pump (shutoff and check valves, strainers, instruments) are transparent. Fittings beyond a boundary valve are outside: a block valve after a control valve is normal practice. A valve met where no valve step waits is extra, never another line's valve out of order; only a unit fills a skipped step out of order. A slot keeps its worst fault. A part found clears a red fault left by an earlier line, but keeps a yellow fault met on its own line. A tee's branches are assigned by where they end. The kettle's vapour line has no step: its faults go to the reboiler.
- Revert (W5): a checked report reverts on a change at any block it walked (its colours rest on those blocks), not only at a found part, but not on an addition beside it. One-block movers count as in-line pumps. A dropped state store is rebuilt by one validation per designed column when its controller loads.
- GUI (W6): the server decides what every click on a structure opens, so datasheets now come by a server message; a unit that a report holds opens its column's GUI with the unit selected, and "Open unit" opens its own screen (a fluid device's menu stays open only within its own reach). The GUI's reach is counted from where it was opened. A reverted entry keeps the position of the change for the status line. A view crosses the wire without its footprint. The drawing has no valve tags (PC, FC, LC) in this stage: the control modes come with the column's operation.
- Arrows (W3): one rule for every kind, with the pump's arrow as the longest. Each side's arrow is as long as that side of the neck shows, not one length for the four: a neck into a header tee or out through a skirt shows more on some sides. The shortest arrow is 0.1 m; a side that shows less carries none (no arrows on the 0.5 m column's side nozzles or the 15 m column's top outlet). A short arrow is at most 0.6 of its length wide. A mover's coolant and other utility nozzles get arrows by the flow table (the earlier arrows batch left them out).

### 14.4 Next stage (the column's operation)

- D5 (members in the fluid network while the column runs), D7 (how the operating input is saved), D14 (the column in the fluid network: a quasi-steady split into real vessels recommended; generator ports rejected because they break conservation), D16 (the distillate valve: the product's port, or its rate through a new V3 specification).
- Behind an ejector, V3's node 0 must sit at the drum's pressure, not the top tray's, and take the motive steam. Today node 0 sits at the top pressure; below 7.4 kPa, water cannot condense there at a 40 °C outlet.
- The off-gas valve's target; flow and level modes on the control valve (process-control brings the pressure and flow modules; level control waits for its cable batch); ejector stages; component capacities.

## 15. Findings and risks

- **F1.** `ColumnStructure.side` left a nozzle out when the shell had no free row on its side, and nothing reported it. W1 (`6648144f`) confirmed this with a test (a 4-tray column with 4 draws and 2 steam stages loses its vapour return as well). `ColumnStructure.unplaced` now lists what is left out, and Save design refuses such a design. The probe showed the problem is wider than the example: see Q9. After Q9 (a), the same 90 designs (plain with a kettle; sump steam; a draw and sump steam; both without a kettle) leave a nozzle out in 35, down from 51: sump steam alone 8 of 30 (was 17), a draw and sump steam 17 of 30 (was 24), plain 10 of 30 (unchanged). What remains: the 1 m columns, where the access structure takes the left side (steam, reflux); the 4 m columns of 1 or 2 trays (feed); a side draw and sump steam on short shells (steam); and the vapour return of short columns with a kettle.
- **F2.** A constructor saved in one form lost the other forms' fields on reload (W2 found the drum connections; also the bank, the boot, and a standing drum's turned outlet when a boot was set). Fixed in `152aefa3`: `readDesign` keeps them as a form switch does.
- **F3.** A column side nozzle walks past its trays when its row is taken (`ColumnStructure.Rows.side`: a fixed side, nearest clear row, no bound). The review column has its sump steam at trays 15 to 16 and its reflux at trays 4 to 5. Of 126 swept designs, all 97 buildable ones have a nozzle 2 trays or more off its stage. The column model is unaffected. Options (A) nozzles first with access around them, (B) bound only, (C) GUI follows the build: [COLUMN_NOZZLE_ROWS_REVIEW.md](COLUMN_NOZZLE_ROWS_REVIEW.md). The owner chose (A) with headings the player can override in the constructor; done in `ef0ae447` (stage bands, turns, ladders moved, decks cut with every walk kept whole, shape fields 3, protocol `equipment-12`). The 1 m columns: the owner chose (a), keep refusing (review section 5).
- **Risks.**
  - A path that changes the world without either hook escapes the revert until its chunk loads again. The chunk-load check is the backstop.
  - The walk reads only loaded chunks; NOT_LOADED is a state, never a hold.
  - Routing a column's click through the server adds one round trip before its GUI opens.
  - The tool sync depends on the pipe-laying batch; until it merges, the GUI shows the colours and the tool does not.
  - One in-game check covers the GUI (AGENTS.md).
