# Kit consolidation: integration brief (2026-10-18)

You are the single writer of the kit consolidation's integration step in the worktree
`D:/Minecraft/Modding/1.21/CreateChemE/.claude/worktrees/custom-equipment-rendering-805e7d` (branch
`claude/custom-equipment-rendering-805e7d`, head 4dc78964 plus uncommitted work). Read `AGENTS.md` first.

## Where things stand
- Eight lanes moved the 14 new process units (UnitDesign files in `src/main/java/com/wormzjl/createcheme/science/equipment/`)
  onto the shared kit (ProcessUnit, Access, Mesh, PipeSizes). Their results, visible changes, kit gaps and owner
  choices: `tools/new-units/CONSOLIDATION_LANES.md`. The kit API: `tools/new-units/KIT_API.md`. The plan:
  `documentation/2026-10-15-custom-equipment-rendering/KIT_CONSOLIDATION_PLAN.md`.
- All lanes compile together. The combined check (`bash tools/new-units/check.sh pre <22 kinds>`) gives 50 passed,
  3 failed: `designsShareOnlyTheKit` (a javadoc `{@link SteamTurbineDesign}` in CondensingTurbineDesign counted as a
  crossing) and two climb failures of the cabin heater (12 x 15 on support 3 climbs 3.125 -> 13.125; 12 x 40 on
  support 4 climbs 4.125 -> 14.125).
- Snapshot before your work: sources `build/tmp/units/pre/src/` (and `lanes.diff`), counts
  `build/tmp/units/pre/counts.md`, renders `build/tmp/units/pre/render/`. The baseline before the lanes:
  `build/tmp/units/base/counts.md`, `build/tmp/units/base/render/`.
- Owner decision C1 (2026-10-18): no caged climb longer than Access.MAX_CLIMB (9 m; the gate allows 9.125 m with the
  deck's grating), rest platforms where needed, every unit ladder gated. Owner rule E3: small visible changes are
  allowed where a shared component replaces a hand-built one, listed in the review.

## Your task: fold what the lanes duplicated or worked around into the kit, fix the kit defects they found
Each item is a structural change in the kit, with the designs' copies or workarounds deleted in the same step.

Kit defects:
- K1 `Mesh.bend`: its end pieces stop square to their chord, not to the pipe's axis, so a wedge gap shows where a
  straight pipe meets a mitred elbow (lane G, `build/tmp/units/G/diff/tr30-elbow-after.png`). Every kit elbow and
  `pipe()` goes through it. Make the ends square to the tangent so straight runs meet them flush.
- K2 `ProcessUnit.pipe`: with `solid=true` a leg's solid is extended by r at the leg's lower coordinate whatever the
  leg's direction (lane C drew the FCC feed line `solid=false` with its own rods); extend at the leg's ends by
  direction and give the FCC line back to `pipe`. A leg shorter than about 1 cm gets no elbow beside it: drop such legs
  inside `pipe` and remove lane A's filter in SteamTurbineTrain.tripThrottle.
- K3 `stairTower`: its corner posts on the deck side reach 0.025 m into the deck and touch its toe board and rails
  (lanes A, C). Put them flush with the deck edge.
- K4 `ringAccess` drops arc pieces under 0.8 m even when a ladder stands on one (lane E lengthened the gasifier stack
  arcs to >= .85/(wall + GAP) to work round it). Keep a piece a ladder starts on or lands on; remove the workaround if
  it is then not needed.
- K5 `fill` draws a face the body always hides (about 48 quads per drum at 24 sides; lane B). Drop it.

Kit pieces that replace copies:
- P1 Tower plan. Lanes A, B, D (and C, E) recompute where a stair tower ends, or which end a stop's landing opens at,
  before drawing it: the condensing set copies the flight-length formula plus the 0.175 m landing-and-post overhang;
  the coker cannot centre its tower; the grate boiler recomputes the split from TOWER_RISE, STAIR_RISE and STAIR_GOING.
  Give the kit a plan computed before drawing (its length, and per stop the landing's end and station) that
  `stairTower` draws from, plus the conversion of a tower's openings into a deck run's rail gaps (done by hand in
  each design now). Replace every copy.
- P2 One rest piece for a flat face. Cyclone, stirred reactor and adsorber each have a `rests(...)` method, and the
  fluidized-bed gasifier's feed floor builds its own in `access()`: a deck Access.MIN_DECK out from a face at the
  level Access.climbs gives, on posts (base plates) or on brackets off a member, railed on every open side with a gap
  at each ladder, the lower ladder walked through onto it from outside, the upper ladder above it, the sections
  offset so a fall from the upper one lands on the rest. Make it one kit piece with the options those four need and
  delete the four copies. Ring rests stay `ringAccess` + `Access.climbs` (FCC, vertical heater, gasifier vessel,
  reactors).
- P3 `post` with an optional base plate (the cyclone's local `post(u, ...)`, and any other design drawing plates by box).
- P4 Shared fittings. Lane G left identical private copies in TrickleBedReactorDesign and EbullatedBedReactorDesign
  (topNozzle, valve, thermowell, topDeck, the move-off-the-tangent loop, constants, about 90 lines each) because
  `squareDeck` has no hole for a ladder coming up through it. Lanes C and F each keep a local reducer. Put the generic
  fittings (valve, thermowell, reducer) in the kit and give `squareDeck` a ladder hole. What is specific to the two
  hydroprocessing reactors goes either into the kit (if generic enough) or into one shared file owned by both kinds,
  declared in `designsShareOnlyTheKit` beside FluidizedBedParts. No identical code left in two designs.
- P5 Side counts. `pipe` has no side count (lane B); `stack` has none (lane E: 60 sides at 6 m OD cost 7.3k quads, so
  lane E capped the fluidized-bed boiler's stack at 5.5 m OD where the survey gives 6 m). Add the side count; give the
  boiler stack back its survey OD with a side count that keeps its worst case (26 x 3 on support 8) under 80 000.
- P6 Remove every delegating old signature in the kit (KIT_API.md last bullets) and every kit helper no caller uses;
  the 4-argument `brace` stays only if a caller still needs it.

Gates and the two failures:
- G1 `designsShareOnlyTheKit` (ProcessUnitTest): strip comments before the scan; add any shared file you create.
- G2 Cabin heater climbs over 9 m. Reason from the real equipment first: tall cabin walls carry platforms at the
  peep-door rows; the vertical heater already adds a 270-degree platform every 8 m of height. Give the cabin heater
  the same kind of intermediate side walkway (or a rest from P2 where a walkway does not fit) so every climb of every
  size and support passes the 9.125 m gate.

Not in scope (owner choices, listed in the review; keep the lanes' choices as built): every "Owner choices" bullet in
CONSOLIDATION_LANES.md; radialSide on any axis (one user), fill per quadrant (one user), hoop cut round fittings,
Access.climbs on fractional levels, Access.deck brackets at every post, ringAccess per-ring radius, `end` depth,
squareDeck opening to one face, the ebullated rests over the skirt opening.

## Rules
- Edit sources with the Edit/Write tools. Work only in this worktree. No commits, no push, no bare git stash.
- Gradle: only `JAVA_OPTS=-Xshare:off ./gradlew.bat compileJava compileTestJava --offline --console=plain`, one at a
  time; never the `test` task (the orchestrator runs the gates once afterwards). The quick gate after a compile:
  `bash tools/new-units/check.sh int ADSORBER,AIR_COOLER,CABIN_HEATER,CENTRIFUGAL_COMPRESSOR,COMPRESSOR,CONDENSING_TURBINE,CYCLONE,DELAYED_COKER,EBULLATED_BED_REACTOR,EJECTOR,EXCHANGER,FCC_CONVERTER,FIXED_BED_GASIFIER,FLUIDIZED_BED_BOILER,FLUIDIZED_BED_GASIFIER,GRATE_BOILER,KETTLE,PUMP,STEAM_TURBINE,STIRRED_REACTOR,TRICKLE_BED_REACTOR,VERTICAL_HEATER`
  (no source arguments needed once Gradle compiled; it runs ProcessUnitTest, ShapeRasterizerTest, LineGeometryTest,
  DeterministicMathSourceTest and prints UnitCounts).
- The suite gates the lowest support only. Sweep every size, length and support of the kinds you touch with a probe
  (lanes left some: `build/tmp/units/laneA/probe/AllSupports.java`, `build/tmp/units/H/probe-src/`): walkedAndClimbed,
  the free pipe blocks, pipesApart. Keep probes under `build/tmp/units/int/`.
- Every unit under 80 000 quads at every support; check the known worst cases (coker 10 x 2 on support 16: 79,533;
  fluidized-bed boiler 26 x 3 on support 8: 79,168) and each kind's largest size at its highest support.
- Nozzles (role, base, axis, length, ODs, exit) and moving parts must not change except where an item says so; dump
  and compare against `build/tmp/units/pre/` classes or sources where in doubt.
- science/ uses DeterministicMath, never Math.sin/cos/tan/pow/cbrt/hypot/log/exp/atan/asin/acos (gate
  DeterministicMathSourceTest). One free block between nozzle pipe blocks (gate pipesApart). Columns: behaviour
  unchanged (ColumnStructureTest and the column renders as before).
- Match the surrounding code: javadoc density, naming, the frame (y up from grade, front -z, +x the constructor's
  left, metres).

## Done when
- The combined check passes with 0 failures, the sweeps pass, counts are under 80k everywhere.
- Renders of the 16 unit and heater kinds (`RenderAll build/tmp/units/int/render iso,iso3 <kinds>`; class path
  `build/tmp/units/int/out;build/classes/java/main;build/resources/main`) compared with `build/tmp/units/pre/render/`:
  look at the elbows (K1), towers (K3, P1) and rests (P2, G2).
- Counts written to `build/tmp/units/int/counts.md`.
- Report (at most 50 lines) as your final message: per item what changed (files, lines), what you could not do and
  why, counts against `pre/counts.md` (box and quads per kind, worst cases), visible changes with the render file
  names, nozzle and moving-part checks, new owner choices. Also save it as `tools/new-units/INTEGRATION_REPORT.md`.
