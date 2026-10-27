# Unit drawing engine and resource JSONs: plan (2026-10-18) — ABANDONED

**Status: Abandoned 2026-10-18.** The owner changed course before any engine code was written ("ditch the json plan and go back to consolidation instead"); the kit consolidation goes ahead instead (`KIT_CONSOLIDATION_PLAN.md`). Kept as the record of decisions E1–E4; only E4 (the new-units baseline commit d084b777 + 4dc78964) was carried out.

Batch `2026-10-15-custom-equipment-rendering`, step "unit engine", on the branch after the new-units baseline
(d084b777 + 4dc78964). Owner request: "put the 3d model parameters into a json and make the system something looks like
drawing engine + resource jsons", before the kit consolidation.

## Owner decisions (2026-10-18)
- **E1 Route: hybrid engine.** One JSON per unit kind holds its sizes, constructor rows, parameters, derived values
  (expressions), parts with repeats and conditions, nozzles, decks, stairs, ladders, motions and datasheet lines. The Java
  engine gives the primitives and components, and named solver functions for what an expression cannot say.
- **E2 Location: in the mod jar**, `data/createcheme/equipment_units/<kind>.json`, read from the class path on both sides
  at first use. Server and client always agree; datapack loading can come later on the same format.
- **E3 Equivalence: may change where simpler.** A conversion may replace hand-built parts by engine components (stair
  tower, table, bracket-less deck); checked by the existing gates, counts and renders, not by bit parity.
- **E4 Baseline committed first** (done).

## What today's designs hold (survey)
- About 13,000 lines in 22 designs: plain parameters (ranges, rows, defaults, proportions, speeds), derived geometry
  (formulas over the sizes, clamps, snaps to cell centres, loops over beds or drums, conditions), and solvers (FCC
  standpipe angles, the cyclone's spool that puts the inlet on a cell centre, free slots between column frames, stair
  flight counts). The first two go to JSON; solvers stay Java, called by name.

## Format (format 1)
A resource is one JSON object. Unknown fields, unknown names and wrong types are refused at load with the file and the
field's path; the format number must be 1.

```
format, kind, orientation ("vertical" | "horizontal"), moves (bool), banks (bool, air cooler only)
size     {row, min, max, step}
length   {row | null, min, max, step, default, unit: "m" | "count"}     (numbers or expressions of D)
support  {row | null, min, max, default}                                 (expressions of D and L)
params   {name: number}                                                  constants
functions{name: {args: [..], let: {..}, value: expr}}                    pure expression functions
let      {name: expr}                                                    derived values, in order
parts    [node]                                                          the drawing
held     {key, args: [text expr]}
variant  [{when, suffix}]                                                vessel-screen kind line suffix
datasheet[{key, args, when}]
```
- **Expressions**: numbers, names, `+ - * / %`, comparisons, `&& || !`, `c ? a : b`, calls. Names: `D` (size), `L`
  (second size), `S` (support), params, lets, function arguments, repeat indices, and the kit's constants
  (`STAND_OUT`, `DECK`, `GAP`...). Built-ins: `min max clamp abs floor ceil round sqrt sin cos asin acos atan2 pow`
  (through `DeterministicMath`), `cell(x)` (the cell centre at or below), `sides(od)`, `flange(od)`, `pipe(od)`,
  `motor(kw)`. Evaluated in Java's order of operations, so a formula copied from a design gives the same double.
- **Text arguments** call a formatter at the top: `int(x)`, `number(x)` (one decimal), `whole(x)` (thousands),
  `size(x)`.
- **Nodes**: each is an object with one type key, optional `when` (condition) and `let` (local values):
  - control: `group {parts}`, `repeat {count, as, parts}`;
  - mesh primitives, one to one with `Mesh`: `prism`, `frustum`, `bore`, `annulus`, `bolts`, `head`, `cylinder`,
    `ring`, `box`, `block`, `bar`, `rail`, `torus`, `sphere`; a frame is `{at: [x, y, z], axis: "+y"}`;
  - kit: `nozzle`, `solid`, `volume`, `outline`, `edge`, `platform`, `railing`, `ladder`, `vertical_ladder`, `stair`,
    the kit's assemblies (`motor`, `large_motor`, `table`, `saddle`, `leg`, `girder`, `brace`, `stack`, `transition`,
    `spool`, `manifold`, `header`, `line`, `door`, `seam`, `joint`, `panels`, `band`, ...);
  - motion: `moving {turn | slide, parts}` (one motion per part, as today);
  - `solver {name, args}`: a named Java function that draws into the unit (FCC structure, coker derricks, ...).
- **Solvers** are registered by name in `UnitSolvers` (Java, `science/equipment`), a kind's in one file
  `<Kind>Solvers.java`; expression solvers (numbers) and drawing solvers (nodes) are both allowed.

## Engine (Java, `science/equipment`)
- `UnitExpr`: parser (precedence climbing, left-associative) and compiler to closures over a slot array; names resolved
  at load; deterministic math only.
- `UnitResource`: the validated, compiled resource (sizes, functions, lets, node tree, text).
- `UnitNodes`: the node vocabulary, one registration per node type (its fields and the kit call).
- `ResourceDesign extends UnitDesign`: sizes, rows, held, datasheet and `build()` from a resource. `UnitDesign.of(kind)`
  loads `/data/createcheme/equipment_units/<kind>.json` when present, else the Java design during the migration; at the
  end every kind is a resource and the Java designs are gone.
- Nothing else changes: `ProcessUnit`'s façade, the rasterizer, screens, network and lang keys stay as they are.

## Steps
1. Expression language and loader with unit tests (parse errors, unknown names, Java-equal results).
2. Node vocabulary, `ResourceDesign`, the loader in `UnitDesign.of`.
3. Pilots: ejector (sizes, rows, text, nozzles) and cyclone (repeats, conditions, decks, ladder, motion, solvers).
   Gates green; counts and renders against the Java design (bit-equal expected here, since nothing is simplified).
4. **Owner checkpoint: the pilot JSONs and the format**, before the mass conversion.
5. Engine components that absorb the consolidation (stair tower, table with edge rails, bracket-less deck, motor on any
   axis, stack without cap, unbored ring, rod heads, head bulging down, horizontal transition, scaled brace).
6. Conversion lanes (parallel opus, disjoint JSON and solver files, Gradle-free lane gate), then deletion of the Java
   designs, strict "every unit kind is a resource" gate, counts, renders, in-game check, review, CHANGELOG, code guide
   chapter at the merge.

## Gates
- `UnitExprTest`: grammar, precedence, errors with positions, determinism (results equal to the Java expressions).
- `UnitResourceTest`: every resource in the jar loads; refusals for unknown fields, names, types and format.
- The existing equipment gates on every size of every kind (decks, ladders, stairs, moving parts, nozzles, text fit).
- Layout time: at most 200 ms per unit (today's largest 79 ms).
