# Pipe laying system: plan

Batch `2026-10-26-pipe-laying-system`. Base `600549d9` (main, 0.13.2). Status: In progress since 2026-10-26. Owner decisions 2026-10-26: "D2 keep that tool creative only, D6 single block, D7 Immediate, Others as recommended" (section 11).

Owner request 2026-10-26 (summary):

1. A pipeline tool. Shift + use opens a GUI that sets the diameter of the pipe to lay (diameter only for now). Use on an empty block or on a pipe rack (an empty rack, or a specific pipe on a rack) starts a laying operation; use on a second spot lays the pipe; Shift + use cancels an operation in progress; Shift + use on an existing section dismantles it. A ghost shows the pipe while laying. The routing algorithm needs thought.
2. A pipe rack block. Several pipes of suitable diameter share one rack block; a rack carries no junctions and no accessories. The arrangement inside a rack can change as pipes come in and go out, and needs special attention. Every pipe in a rack must be visible from all directions, so that a player can connect to it. Five pipes at most per rack block.
3. Equipment connection ports are highlighted while the player holds the tool.
4. A pipe can meet constructed equipment off-centre, for more freedom in equipment layout; it becomes centred once it leaves the constrained zone.
5. No major conflict with the backward-compatibility design (batch `2026-10-26-backward-compat`).

## 1. Expected behaviour

A player lays a line the way a piping designer routes one: pick a start, pick an end, see the route, accept it.

- The line follows racks where racks exist, turns as little as it can, and never passes through a block that is not free.
- A laid line connects only at its two ends and along its own path. A line laid beside another line, a nozzle or a tank does not join it.
- In a rack, each line keeps its own position along the whole rack, every line is in view from the sides, the top, the bottom and the ends, and any line can leave the rack through any side.
- A line that leaves a rack or an off-centre nozzle shifts back to the block centre within the first free block.
- Laying and dismantling are player inputs: the blocks change at once, and the fluid network takes the change as one queued engine event.

## 2. What the code does now (the constraints)

| Fact | Where | Consequence for this design |
|---|---|---|
| One pipe block is one registered device, 1 m long, its bore in the registry | `FluidWorldAuthority.place` (`PipeResistance.Geometry(1, d, …)`) | A laid line is a chain of ordinary pipe devices; the compiler already folds chains into runs (`TopologyCompiler`). |
| A pipe joins every neighbouring fluid block and every nozzle facing it | `FluidDeviceBlock.connects`, `PhysicalFluidTopology.Device.joins` | Two lines in adjacent blocks join. This is why nozzle pipe blocks need a free block between them (owner rule 2026-10-17, `ProcessUnitTest.pipesApart`). A laying tool cannot lay parallel lines today. |
| The registry and the compiler allow one device per cell | `PhysicalFluidTopology.compile` ("Duplicate physical identity or position"), `WorldTopologyLedger` position index | Several lines in one rack block need a key below the cell. |
| A ledger event carries a list of edits | `PhysicalRegistry.submit(List<Edit>, …)` | A whole laid line, and its edits at the anchors, can be one atomic event. |
| Every nozzle centre line runs through a cell centre | `EquipmentShape` frames, `ShapeRasterizer.Port` | An off-centre connection needs an offset on the port. |
| Pipes have no bend loss (minor loss 0) | `PipeResistance.Geometry(…, 0)` | Routes with more elbows flow as well as straight ones (section 9). |
| The current tool is creative-only, keeps one bore per stack (`pipeline_tool_settings`), resizes one pipe on use, and opens its GUI on Shift + use anywhere | `PipelineToolItem` | It stays as it is, creative only (D2). The laying tool is a new item. |
| Pipe blockstates already have 110 592 states | `FluidDeviceBlock.Pipe` | New per-pipe data goes in the block entity, not the blockstate. |

## 3. Core idea: explicit joints and face points

Two rules carry the whole design.

**Explicit joints.** A pipe has a joint set: the faces it connects through, a six-bit mask. A hand-placed pipe and every other device have all six faces (as today). A laid pipe has an explicit set: the faces its path uses. Two blocks join across a face only if both have a joint there (a pipe with all six faces, and every other device, keep today's rules). Laid lines then never join by accident, and the reason for the pipe spacing rule disappears for them.

**Face points.** Every pipe in a cell has a centre point P. A plain pipe's P is the block centre. Its arms run parallel to the axes from P to each joint face, so its face point on a face is P projected onto that face. Two pipes join only where their face points meet. For plain pipes this is today's rule, since all face points are face centres. A pipe whose neighbour offers an off-centre face point (a rack lane, an off-centre nozzle) is a **transition pipe**: it runs from that point to its own centre line inside its block (section 6).

## 4. The pipeline tool

The laying tool is a new item, `icheme:pipe_laying_tool`, with its own component `pipe_laying_settings` (diameter). It is usable in survival (D1). Its GUI has one diameter field (the request: diameter only). The existing `icheme:pipeline_tool` stays the creative-only tool that resizes one pipe on use (D2); its id, component and behaviour do not change, and its display name becomes "Pipe Sizing Tool" so the two items can be told apart.

### 4.1 Inputs

| Input | Idle | Laying (a start is set) |
|---|---|---|
| Use on an anchor (4.2) | Set the start | Lay the route to this anchor, if it is valid; then idle |
| Use in air | Nothing | Nothing |
| Shift + use on a pipe (a pipe block, or one lane of a rack) | Dismantle that one pipe at once (D6, D7) | Cancel |
| Shift + use elsewhere | Open the GUI | Cancel |

The client decides which row applies, because only it knows whether a start is set. It sends one explicit command to the server: `Open`, `Lay` or `Dismantle`. The server's own `use` does nothing, so a Shift + use that cancels never opens the GUI. The start lives only on the client. It is dropped when the player changes the held item or dimension, or moves more than 96 blocks from it.

### 4.2 Anchors (where a line may start or end)

| Anchor | The line's end cell | Change at the anchor |
|---|---|---|
| Free block (the cell beside the clicked face, as for block placement) | That cell | An open end with a blind flange |
| Open end of a plain pipe (one joint) | The next cell beyond the clicked face | The end gains a joint: the line is extended |
| Plain pipe with at most three joints, no fitting and no off-centre face point, clicked on a free face | The next cell | The pipe gains a joint: a tee or a cross (a junction). A pipe with all six faces needs no edit |
| Equipment nozzle, or a face of a one-block tank, generator or void | The plain cell in front of the nozzle | The end cell's joint faces the nozzle |
| Empty slot of a rack block | A lane in that rack cell | An open lane end, drawn with a cap |
| A lane on a rack, clicked | The lane's open end | Extension only. A lane with no open end is refused: "a rack carries no junctions; start the branch outside the rack" (D11) |

The clicked face of the start sets the line's first direction, so the player steers the first leg by the face they click.

### 4.3 Route search

The client searches the route each time the aimed anchor changes. It shows the route as the ghost and sends the route itself with `Lay`. The server checks it (4.4) and never searches, so client and server cannot disagree on the route.

- **Search space.** The box spanned by the two anchors, widened by 8 blocks, clipped to the build height and to loaded chunks. At most 32 768 expansions and 128 cells (config).
- **State.** (cell, heading, slot range). The slot range is set only while the route is inside a rack.
- **Moves.** To any of the six neighbours, except straight back.
- **Admissible cells.**
  - A free cell: replaceable (air, plants, fluids), with no fluid device or equipment in it.
  - A rack cell: it has a slot range free for the line's width (5.3), and the line's entry face takes it. A face that leads from a rack cell to a plain cell carries at most one lane (a plain cell holds one pipe).
  - The anchors' own cells.
  - Nothing else. A route never passes through another pipe, a device or an equipment cell.
- **Costs.**
  - A step through a free cell: 1.0.
  - A step through a rack cell along the rack's axis: 0.4. Routes therefore follow racks, as plant piping does.
  - A step through a rack cell across its axis: 1.0.
  - A change of heading (an elbow): +1.5.
  - A vertical step: +0.3.
  - A transition cell beside a rack or an off-centre nozzle: +0.5.
- **Order.** Weighted A* with the heuristic 0.4 × the Manhattan distance and weight 2. Ties go to the lower f, then the lower h, then the direction order DOWN, UP, NORTH, SOUTH, WEST, EAST. The result is deterministic, with a bounded search cost.
- **Slots.** When the route enters a rack portion (consecutive rack cells), the search branches over the slot ranges free in the entry cell; at most five. Inside the portion the range is fixed. An extension from a lane keeps that lane's range.
- **No route.** The ghost draws a red straight line between the anchors and names the reason: blocked, too long, rack full at x y z, or the anchor invalid.

### 4.4 Commit (server)

`Lay(hand, start anchor, end anchor, cells, slot ranges)` is checked as a whole before anything changes:

- the player's permissions;
- the end anchor within reach, the start within 64 blocks;
- every cell loaded, free (or a rack cell with its slot range free) and contiguous;
- the anchors still valid; one lane per rack face towards a plain cell; a transition cell has at most two joints;
- the items, in survival (D1).

Then, as `EquipmentStructure.place` does, the server places all blocks and lanes and submits **one** ledger event. The event holds the new pipe and lane registrations and the edited anchors (a tee's or an extended end's new joint). Blocks placed by the tool bind to their queued registration. `onPlace` sees the registration and does not place a second one. A refused lay changes nothing and names its first failing cell.

### 4.5 Dismantle

A section is a single block (D6): Shift + use removes the aimed pipe at once (D7), a plain pipe block or one lane of a rack cell. It is the normal removal of one device (one ledger event):

- the pipe's fittings and gauges are refunded through the accessory store, exactly once, as a broken pipe's are;
- in survival, one pipe item is returned;
- the neighbours keep their joints towards it, so each becomes an open end (blind flange, or a cap in a rack); laying a line to that face joins it again.

While Shift is held, the aimed pipe is outlined red, so the player sees which pipe goes.

### 4.6 Ghost and HUD (client only)

- The route is drawn translucent in the line's size: the same `LineGeometry` mesh a laid pipe would bake, with the transition offsets. Cells that fail are outlined red (the `Outliner` that `EquipmentGhost` uses).
- In a rack, the chosen slot shows on every rack cell of the route. Existing lanes never move in V1 (5.3), so the ghost shows nothing else there.
- The action bar shows: start → end, the length in metres, the elbows, the size, and the items needed.
- Search cost: one search per change of the aimed anchor, within the expansion cap, on the client thread.

### 4.7 Port highlight (item 3)

While either hand holds the tool, the client marks every equipment port within 48 blocks (at most 256 ports):

- a square outline on the flange face, at its offset;
- the colour of its port class (vapour, liquid, bulk, overflow) when the port is free, grey when a pipe is on it; a mover's suction and discharge are labelled;
- open pipe ends in yellow, and the free slots of an aimed rack.

The aimed port reads, for example: "Liquid outlet (bottom) · free · fits up to 0.8 m".

The data is static geometry the client already holds: `ShapeRasterizer.ports(shape)` from each shell cell's key, and the nozzle codes (code 0 means free). An index of loaded shell cells with ports is kept on the client, as the client already lists moving-part cells and tank node cells. Nothing is requested from the server, and no process data is read, so the engine-owned presentation rule is untouched.

## 5. Pipe rack

### 5.1 Geometry: five slots on the block's main diagonal

A rack cell has five slots. The slots sit on the main diagonal of the block, the same diagonal in every rack block (world-fixed):

- slot k has the centre P_k = (c_k, c_k, c_k) in block coordinates, with c_k = 1/8 + 3k/16, so c = 0.125, 0.3125, 0.5, 0.6875, 0.875;
- a lane in slot k is an ordinary pipe whose centre is P_k instead of the block centre. Its arms run parallel to the axes from P_k to its joint faces.

```
 up 1 ┌─────────────────────┐     end view of an east-west rack (looking west);
      │                ④    │     the same picture holds on every face, in
      │           ③         │     every rack, whatever its axis
      │       ②             │     ② is the block centre
      │   ①                 │
      │⓪                    │
    0 └─────────────────────┘
      0   north ... south   1
```

**Why this arrangement.** P_j and P_k differ by at least one slot pitch (0.1875 m) in **every** coordinate. Take any arm of lane j and any arm of lane k. Both are parallel to an axis. If they are parallel to the same axis, they differ in the two other coordinates. If they are parallel to different axes, the third axis is fixed on both, at c_j and at c_k. In both cases the arms stay at least one pitch apart. One pitch is more than the two radii plus a clearance of 1/64 m, because a lane's width is limited to its slots (table below). It follows that:

- **Clash-free in every case.** No two lanes touch, whatever faces they use: straight, turned out sideways, up or down, crossing the rack, or passing to the next rack.
- **Visible from all directions.** Seen along any face normal, each lane's arms project at its own coordinate c_k. No other lane covers them; another lane's arm crosses them in at most one place.
- **Any lane can leave through any face** without moving another lane. The arrangement therefore never has to change for an exit.
- **Continuity across racks.** On a shared face, the face point of slot k is (c_k, c_k) in both blocks. A lane keeps its slot from rack to rack, straight on, round a corner, at a tee of racks, through stacked racks and up a vertical rack. No rule depends on the rack's axis.

| Slots used | Largest outside diameter (m) | Largest bore (m) | Pipe that fits |
|---|---|---|---|---|
| 1 | 0.172 | 0.156 | 6" (OD 0.168) |
| 2 | 0.359 | 0.326 | 14" (0.356) |
| 3 | 0.547 | 0.497 | 20" (0.508) |
| 4 | 0.734 | 0.667 | 28" (0.711) |
| 5 | 0.8 (`PipeSizes.MAX_OUTSIDE`) | 0.8 | the largest line, at the block centre |

A line needs m = ⌈(OD + 1/64) / (3/16)⌉ consecutive slots. Its centre is the mean of their centres. The bound holds for wide lanes too. Five lines of one slot each is the maximum, as requested.

**Elbows in a rack are short.** A bend cuts its corner towards the diagonal, where the next slot's lane runs. Inside a rack, a lane turns with a bend radius of at most (m + 1)/2 × 3/16 m: 0.1875 m for a one-slot lane, about 1.1 OD, which is a short-radius elbow. Plain pipes keep their long-radius elbows (1.5 OD).

**Checked 2026-10-26** with a brute force (`tools/rack-slot-clearance/`): every pair of disjoint slot ranges, every pair of joint faces, the arms and the elbows sampled every 2 mm:

| Elbow in the rack | Smallest surface gap between two lanes | Smallest margin to the block edge |
|---|---|---|
| Arms only, square corners (2 520 arm pairs) | 0.0156 m = 1/64 m | 0.039 m (frame 1/32 m = 0.031 m) |
| Long radius 1.5 OD | −0.034 m: a clash | — |
| Short radius (m + 1)/2 × 3/16 m | 0.0156 m = 1/64 m | — |

### 5.2 What a rack cell may hold

- At most five lanes, with pairwise disjoint slot ranges.
- A lane has one or two joints. A rack carries no junctions (owner request; the geometry would allow one), no fitting and no gauge.
- A lane joins the lane with the same slot range in a neighbouring rack cell, or the plain pipe in a neighbouring plain cell. At most one lane per face leads to a plain cell. A lane that ends with nothing across its face is an open end, drawn with a pipe cap, not a blind flange, because a flange is wider than a slot.
- No flanges inside racks: lanes are welded, and spool flange pairs (`FluidDeviceBlockEntity.SPOOL`) skip rack cells.
- A nozzle meets a plain cell only. A route therefore never puts a rack cell directly in front of a nozzle.

### 5.3 Arrangement and slot allocation

Because any slot can reach any face (5.1), an incoming or outgoing line never forces another line to move. V1 therefore keeps every lane in its slot for its whole life:

- **Choice.** For each rack portion of a new route, the search (4.3) tries every slot range free in all of the portion's cells. The cost counts the other lanes the new line's arms cross, then prefers outer slots for wide lines, then the lower index.
- **No room.** If no range is free along the whole portion, the portion is closed to the search. The search may route round it; otherwise the lay is refused, naming the first full cell. This can happen while every single cell still has room (fragmentation).
- **Why no reshuffle in V1.** Moving a lane moves every cell of its rack portion. The portion can reach unloaded chunks, which a move must never load (review `2026-10-26-chunk-loading-review`). Fixed lanes also mean a laid line never moves on its own, far from the player.
- **V2 options** for fragmentation (D5):
  - (a) a slot change inside one rack cell where both ranges are free, an L-shaped jog along two axes, which the diagonal keeps clear of the other lanes;
  - (b) a re-slotting of the fewest existing lanes, offered only when all their cells are loaded, and shown in the ghost before the player confirms.

### 5.4 The block

- `icheme:pipe_rack`, with a blockstate `axis` (x, y or z). The axis orients the frame drawing and the route search's along-rack discount; the lanes do not depend on it. A rack placed in line with an existing rack takes its axis.
- **Frame:** thin members (1/32 m) along the edges of the block and one portal frame in the block's low face plane, with a small support stub under each lane. Nothing stands where an arm can pass.
- **Shape and collision:** the frame and the lanes' arm boxes, the same on server and client. Light passes.
- **Picking a lane:** the client casts the player's ray against each lane's arm boxes and sends the lane in the command. The server casts the ray again within a tolerance.
- **Breaking:** a rack that carries lanes refuses to break: "dismantle its pipes first" (D9).
- The block entity holds, per lane, its identity and a copy of its facts (slot range, joints, bore). The copy goes in the update tag for drawing and serves as the backup in section 8.

## 6. Off-centre connections and transition cells (item 4)

**Expected behaviour.** On a plant, a nozzle stands where the process needs it, not on a grid. The pipe leaves the nozzle on its centre line and offsets to its run line in the first metre.

**Design.**

- `ShapeRasterizer.Port` gains an offset: two in-face coordinates in 1/64 m (0 means centred). It is computed from the nozzle's centre line, never saved. Every current kind has offset 0, so no layout, stamp or nozzle pipe block changes, and `EquipmentLayoutGoldenTest` stays as it is.
- **The constrained zone** (V1) is the nozzle's pipe cell, the cell its flange faces into. The pipe there is a transition pipe, and the next cell is centred. A rack face is the same case: the plain cell beside a lane's off-centre face point is the transition cell. Wider zones (a line that runs off-centre along a column shell for several cells) are a V2 option (D8).
- **Transition pipe:** a plain laid pipe with at most two joints, at least one of them facing an off-centre point.
  - Opposite faces: an offset of two 45° bends from that point to the far face's point (the centre, or the next off-centre point).
  - Adjacent faces: an elbow followed by a rolled offset.
  - The largest offset (slot 0 to the centre, 0.375 m along two axes) is 0.53 m of a 1 m block. It fits a pipe up to one slot wide. A wider lane's centre is nearer the block centre.
- **Drawn data:** the pipe block entity's drawn geometry gains a signed offset pair per face, beside `Faces`. It is computed on the server in `computeFaces` and sent in the update tag. It is drawn state: it is recomputed at every load and never saved as a fact.
- **Fittings and gauges** refuse a transition pipe, because its run is not straight (`SightGlassGeometry.supports` and the axial-fitting checks get a "centred" condition).
- **Limit.** An off-centre nozzle's flange must stay on its own block face: |offset| + flange(OD)/2 ≤ 0.5 m. Such a nozzle takes a bore only up to that limit. The tool refuses larger lines, and the highlight shows the limit.
- **Which nozzles move off-centre** is a later per-kind decision. Each moved nozzle pipe block needs the owner's approval (AGENTS.md) and a layout table update. A candidate pilot: drum side connections at 0.25 m steps in the constructor. That is a new shape field, with default 0 and a corpus image `shapes-2.snbt` (D12).

## 7. Fluid side

- **`Device` gains `joints`**, a six-bit mask; all six faces (63) for every device but a laid pipe or lane. `mechanicallyConnects(direction)` also requires the joint. `joins` is unchanged otherwise.
- **`Position` gains `lane`.** 0 is the block itself; 1 to 5 are rack lanes. A lane device also carries its slot range (`Lane(slot, width)`).
- **The compile and the registry walk look up neighbours by cell.** An index cell → devices (at most five) replaces `positions.get(next)` in `PhysicalFluidTopology.compile`, `PhysicalRegistry.submit` (`at`) and `joined`. Among the devices that meet the opposite face, the one whose face point matches is chosen: plain to plain, lane to lane with an equal range, or the single lane at a face that leads to a plain cell.
- **Hydraulics do not change.**
  - A lane and a transition pipe are 1 m pipe devices, like any pipe block. The compiler folds them into runs as it does now.
  - A pipe with all six faces compiles bit for bit as today, so chain-100 stays exact.
- **Scheduling.** Laying, extending, tees and dismantling are queued ledger events. Packet handlers only validate and queue. No solve runs, and the engine schedule is untouched.

## 8. Backward compatibility (batch 2026-10-26-backward-compat)

| New datum | Kind | Rule it follows | Default (behaviour before it) |
|---|---|---|---|---|
| `joints` on a registration | Fact (registry) | Appended after the record's last field; facts schema 1 → 2; corpus image `compat/fluid/schema-2/` | Absent: 63, all six faces |
| `lane` in a position record, `Lane(slot, width)` on a registration | Fact (registry) | Appended fields of schema 2 | 0, none |
| Joints and lane copies in the pipe and rack block entities | Fact (world blocks): used only when a block has no registration (after the D4 reset or a `loadedChunk` re-registration), so that a laid pipe never comes back with all six faces | Read by name, validated, append-only; corpus `compat/racks/rack-1.snbt`, gate `PipeRackCompatTest` | Absent: 63, no lanes |
| Per-face offsets on a pipe | Drawn state | Recomputed at every load | — |
| Port offsets | Derived from the shape | No shape field in V1 | 0 |
| A rack block | New block | `EquipmentRefresh` counts it `OTHER`: never overwritten | — |
| Tool settings | Item component | `pipeline_tool_settings` unchanged (the sizing tool); the laying tool has its own `pipe_laying_settings`; later settings get a new component each, with its default | — |
| The start of a lay, the route | Client only | Never saved | — |
| Commands `Open`, `Lay`, `Dismantle` | Wire | Registrar `pipeline-tool-1` → `pipeline-tool-2`; no compatibility work | — |

An existing world loads unchanged: every pipe reads all six faces and no lane exists. The facts schema-1 image keeps reading, and the schema-2 image must rebuild with every joint and lane. The plan adds no state format bump that matters: state formats change freely. It changes no layout. The open items of the backward-compat batch (restating in process on `/reload`, a vessel rebuilt smaller than its liquid) do not interact with this plan.

## 9. Physics note (simplifications kept)

- **Elbows and offsets have no loss.** This is today's rule for every pipe. A long-radius 90° elbow has K ≈ 0.2 to 0.3, about 2 m of straight DN150 pipe (f ≈ 0.018, fL/D ≈ 0.12 per metre). A 30 m laid route with four elbows and two offsets (two 45° bends each) would lose about 35 to 50 % more pressure if bends counted. Laid and hand-placed lines stay consistent. Counting bend loss for all pipes is a solver change for its own batch, not part of this one.
- **The extra length of an offset is not counted.** It is d(√2 − 1), at most 0.22 m for the largest offset (d = 0.53 m), under 1 % of a 30 m line per transition cell.
- **A lane's height inside its block is ignored.** Node elevation stays the cell's y, as for every pipe. The error is at most 0.375 m, or 3.7 kPa of water: the same order as the one-block resolution the network already uses.

## 10. Work packages and gates

| WP | Content | Gates |
|---|---|---|
| W1 | Explicit joints: registry field, facts schema 2 and corpus, block entity copy, `connects` and `joins`, dead-end drawing | New `PipeJointsTest` (parallel laid lines stay apart; tee and extension edits; automatic unchanged); `FluidFactsCompatTest` (schema 1 and 2); chain-100 exact |
| W2 | Tool without racks: anchors, route search, ghost, HUD, `Open`/`Lay`/`Dismantle`, atomic event, single-block dismantle, port highlight | New `PipeRouteTest` (straight, obstacles, determinism, refusals; tampered routes rejected); GameTest: lay between two tanks, flow, dismantle, reload (routes EXACT and FACTS) |
| W3 | Rack: block, lanes, cell index, slot allocation, drawing, picking, breaking rule | New `PipeRackGeometryTest`: every slot range × every joint pair, on the drawn mesh (arms and short elbows), clearance ≥ 1/64 m and visibility from the six face directions; `PipeRackCompatTest`; GameTest through a rack corner |
| W4 | Off-centre points: port offsets, transition drawing, fitting refusal, bore limit | `LineGeometryTest` (transition pipes in their cell, watertight); `EquipmentLayoutGoldenTest` unchanged |
| W5 | Code guide (new chapter `pipe-laying.md`; fluid-runtime, equipment, client-gui, instrument-accessories, verification), tooling cleanup, CHANGELOG | One final lane |

- **Order.** W1 → W2 → W3 → W4, in one session: W3 (lanes) and W4 (transition pipes) both change `LineGeometry`.
- **Measurement.** No solver change, so no campaign. Run the quick suite per WP and one final lane (`lane.sh`).
- **In-game check.** One, at the end of W3, in the dev client through the MCP bridge: the ghost, the highlight and the rack's look. The client is the only judge of these.
- **Version.** Proposed 0.14.0 (a new feature) at the merge; the owner chooses.

## 11. Owner decisions

| | Question | Recommended | Alternative | Owner 2026-10-26 |
|---|---|---|---|---|
| D1 | Who may use the tool | Survival too: one `fluid_pipe` item per cell, refunded on dismantle; free in creative | Creative only, as today | Recommended |
| D2 | The current tool's per-block resize (use on a pipe now starts a lay) | A later GUI mode "resize section" | Dropped | Keep the current tool creative only; the laying tool is a new item |
| D3 | Hand-placed pipes | Keep automatic joints | The pipe item places a one-cell explicit pipe; automatic joints retired | Recommended |
| D4 | Rack arrangement | Five slots on the block's diagonal (section 5.1: every pipe visible, any exit clash-free) | A flat tier as on real racks: the side view hides pipes, and exits need reshuffles | Recommended |
| D5 | Fragmented rack | V1: lanes never move, the lay is refused or routed round | V2 (a) slot jogs, (b) re-slot with confirmation | Recommended |
| D6 | What "section" means for dismantling | Chain between junctions, devices and open ends | The cells laid by one operation (needs a line id fact) | A single block |
| D7 | Dismantle confirmation | Two Shift + use within 5 s | Immediate | Immediate |
| D8 | The constrained zone | The nozzle's pipe cell | Also the cells inside the equipment's box (V2) | Recommended |
| D9 | Breaking a rack that carries pipes | Refused | Cuts its lanes, leaving open ends | Recommended |
| D10 | Racks in any axis | Yes: lanes do not depend on the axis (5.1) | Horizontal racks only | Recommended |
| D11 | "Start on a specific pipe on a rack" | Extend that lane from its open end | Something else: please describe | Recommended |
| D12 | Off-centre nozzles in existing kinds | None in this batch; one pilot (drum side connections) after approval | Move named nozzles now | Recommended |
| D13 | Pipe spacing rule (2026-10-17) | Unchanged in this batch; revisit after W4, since laid pipes no longer join by accident | Relax it now for laid pipes | Recommended |

## 12. Risks

- **The cell index touches the topology hot path.** The automatic path must stay bit for bit (chain-100 exact, the 33 junction lines).
- **Hand-placed and laid pipes side by side.** A hand-placed pipe still joins every neighbour that accepts the face, so a player may expect a laid line to join a pipe placed beside it. The tooltip and the HUD say it does not.
- **Fragmentation refusals in busy racks until V2.** The refusal names the full cell.
- **The diagonal is not how real racks look.** It is what makes every line visible and every exit free (D4).
- **Client search cost on long routes.** It is capped by expansions and cells; the ghost shows "too long" past the cap.
