# Nozzle necks, pipe cap, skirt outlet and boot outlet review (2026-10-25)

Batch `2026-10-21-floating-roof-tanks`, branch `claude/floating-roof-tanks-81d68b`. Commits: `1a806141` (2026-10-21), `a590f592`, `f8532707`, `407c3f24`, `441df98f`, `d0b66376`, `7d53cef5`, `315e93a8`, `bae8620a`, `36e0f9fb` (2026-10-25). Plan and resume state: `BRANCH_RESUME_REVIEW.md`.

## Owner rules applied
- 2026-10-21: "if I have a 1m pipe then the root needs to be 1m. Other components should give way appropriately"; "Sometimes some overlap is fine"; "Skirt hole needs to adapt to the size of the pipe"; taller skirts.
- 2026-10-25:
  - cap pipes at 0.8 m so pipe + flange stays within one block;
  - rotatable drum/reactor bottom outlet;
  - lower minimum skirt;
  - a boot drum's second outlet behind its boot;
  - no client test by agents;
  - later the same day: the largest bore is 0.8 m, not 0.727 m; a lying drum's outlets all leave at its front end, the feed at its far end (oil outlet right of the boot in the owner's view);
  - "trickle bed/ebullated bed skirt design and material not line with others".

## Results
| Step | Commit | What |
|---|---|---|
| Necks follow the pipe | `1a806141` | `Nozzle.maxOd`/`exit` and the reducer deleted. Roots are buried for a pipe's largest. Skirt port facets and collar are drawn per pipe size. |
| 0.8 m cap | `a590f592` | `PipeSizes.MAX_OUTSIDE` 0.8; `MAX_BORE` 0.8 (Pipeline tool, stored bore; owner, same day). A bore from 0.727 m draws at 0.8 m outside: a large line's wall is thin (32": 812.8 on 793.7 mm). Hard-coded 1 m nozzle caps now follow the cap. Ejector suction ≤ 0.8 m, pump discharge ≤ 0.6 m (suction 0.8 m). Coker solids and gasifier ash outlets capped. |
| Lower skirt | `a590f592` | Bottom line on row 0.5 m; minimum skirt ceil(1.35 + D/4): 2 m to D 2.5, 3 m to D 6.5, 4 m to D 10.5, 6 m at D 15. `EquipmentShape.portRadius` narrows the 0.8 m hole's clearance to stay 5 mm over the base plate. |
| Rotatable outlet | `f8532707` | Field `outlet` (0–3) carried as `Design.outlet`, NBT `"Outlet"` and `ConstructorPayload.outlet`; protocol `equipment-11`. Allowed sides (`EquipmentShape.outlets`):<br>• skirted standing drum: 4 sides;<br>• trickle-bed effluent: front, left, and whichever of back or right lacks the ladder from grade (flips with platform count);<br>• columns, bare and lying drums: front only.<br>Port, slots and outlet nozzle turn together; an invalid value is rejected. |
| Boot outlet | `f8532707`, front end `441df98f`, 6 m rule `d0b66376`, depth `7d53cef5`, saddles `315e93a8` | References: Arnold and Stewart's three-phase separator (feed at one end; gas outlet, boot, overflow baffle and oil outlet at the other, the oil nearest the head), Esteem Projects reflux-drum GA (outlet F1 between the boot and its head), "boot at or near the suction end". `f8532707` put the oil inboard of the far saddle and the boot 2 cells further in: on 3 x 9 the boot hung at 4.5 m, on the feed half (69 of 655 boot drums). Owner decision: the front is the outlet end. `bootFor`: the boot on the first cell inboard of the front saddle, the oil outlet on the last cell beyond it (its own largest flange, 0.8 m or D/2, 0.15 m clear); no fallback. Owner: a boot from 6 m long, so the outlet may stand on the head's bottom, its centre at most 0.6 of the head depth past the tangent line (head bottom within 0.2 R of the shell's), its root buried where its largest neck meets the head. The vapour outlet moves to the front top, the default inlet (and the constructor's added connection) to the far top. 3 x 9: boot 3.5 m, oil 1.5 m; 2 x 6: boot 2.5 m, oil 0.5 m. Boot sizes 704 of 800 (661 before): every drum from 6 m long and at least as long as wide. Boot depth `7d53cef5`: the preferred 0.9 m or 0.6 D less what lifts the drain's face a block (0.02–0.97 m, at least 0.4 m), so boot drums stand a block lower (3 x 9 on 3 m, 2 x 6 on 2 m; D 0.5 stays at 2 m). Owner drawing (Ø900 x 4000 reflux drum: saddles 500 mm inside the tangent lines, boot just inboard of the outlet-end saddle, feed on top at the far end): saddles at 1/8 T/T (were 1/5); boot 0.05 m clear of its saddle; boot drain and oil outlet in block columns clear of the saddles; oil outlet on the front head where it has no room beyond the saddle; 733 of 800 sizes, typical boots 0.25 T/T from the front tangent line (drawing 0.24). Untouched default connections follow the drum's size in the constructor (a lengthened drum had kept its inlet mid-shell). Saddle sampling half-open along the axis (lattice-aligned saddles counted a layer more). Drain neck `bae8620a`: `7d53cef5` put the drain's face exactly 0.15 m under the boot's apex. On boots from 1.1 m across (D ≥ 3.5 m) that left 0.17–0.23 m of neck, under `NECK` (0.2425 m); the neck gate failed on 8 x 7. `bootDrop` also puts the face `NECK` under where a 0.8 m neck leaves the head. The boot gives way (owner rule 2026-10-21): it is 0.01–0.07 m shallower and no minimum saddle height changes. A lower face instead would have raised D 4.5 a block. |
| Reactor skirts | `36e0f9fb` | Were concrete fireproofing 0.05 m proud of the shell on a 0.3 m octagonal concrete pedestal. Now the drums' and columns' skirt: a 1/16 m steel plate at the shell's OD in the structure's paint, on grade, with base plate, 0.125 m ring and anchor chairs at a drum's count. Trickle bed: a column's slots (one facet, 0.4–2 m) at the quarters except the effluent's; the access covers and vent row are removed. Ebullated bed: front and back bays kept, down to grade. `ProcessUnit.pedestal`, `pedestalCorner`, `PEDESTAL`, `PEDESTAL_REACH` and `FIREPROOFING` deleted. Renders: `tools/floating-roof-tanks/skirt-render/renders/`. |
| Neck sort | `407c3f24` | `ProcessUnit.NECK` (0.2425 m) replaces `STAND_OUT` on unit nozzle faces. Supports, heights and bays follow (see below). `TOUCHES` replaces `MET_BEFORE`: 10 entries, 23 instances, each with its reason; a stale entry fails the gate too. |

## Layout changes (fresh world; `EquipmentRefresh` rebuilds placed old layouts)
- Footprint or height +1 block on some sizes of: kettle, compressor, steam turbine, condensing turbine, cyclone, FCC, fluidized-bed gasifier.
- Fixed-bed gasifier bay +2 m at D 2 and D 4.
- Port cells moved on: ejector, vertical heater, stirred reactor, centrifugal compressor, delayed coker, FB boiler, grate boiler.
- Standing drums and columns: skirt port on the ground row. A boot drum's liquid outlet is underneath instead of on the front head.
- Lying drums: vapour outlet on top near the front head, default inlet near the far head. A placed old drum keeps its stored inlet at the front top, where the vapour outlet now stands (fresh world).
- Trickle-bed and ebullated-bed reactors: no pedestal. The ebullated bed's pump stack is 0.24 m lower. Its feed and hydrogen nozzle planes now follow the base ring, not the wider octagon, so on some sizes they move a block in; sizes not listed.

## Accepted overlaps (owner: "some overlap is fine")
- 0.8 m skirt port: its collar reaches over the base ring. On drums of D ≤ about 2 m, the 0.8 m elbow crosses the skirt plate.
- Gate `TOUCHES` entries:

  | Equipment | Size | Overlap |
  |---|---|---|
  | Exchanger tube outlet and shell inlet | d 0.75, 1.75 | ≤ 0.02 m on girth flange / rod |
  | Kettle tube outlet | d 0.75, 1.0 | ≤ 0.02 m into channel flanges |
  | Vertical heater fuel | d 2.5 | 0.08 m into the front legs |
  | Column vapour outlet | d 3 | hub base 0.03 m into head |
  | Column side nozzles | d 0.5 | hub base 0.06 m into cladding |

## Gates
- Quick suite `./gradlew test` 1411/1411 at `407c3f24`. At `441df98f` the affected classes (ShapeRasterizer, PipelineToolSettings, LineGeometry, inline mover, sight glass, gauges, VesselPlacement, EquipmentRefresh, UnitText): 71/71.
- `slowGates --tests '*ProcessUnitTest'` 21/21, neck gate green. At `36e0f9fb`: the equipment package (`test --tests 'com.wormzjl.icheme.science.equipment.*'`) 66/66 and `ProcessUnitTest` 21/21. Before `bae8620a` the neck gate failed on the 8 x 7 boot drain.
- Full `test slowGates`, the final lane and a campaign wait for the owner's in-game test.
- Fixed on the way:
  - a pre-existing stale test (a mover's fluid node is its suction port cell, since `4e96e8ab`);
  - an OOM caused by temporary debug hooks, now archived.

## Tools
`tools/floating-roof-tanks/nozzle-room/` gains `NozzleClashDiagTest.java` and `debug-hooks-wip.diff`. `tools/floating-roof-tanks/skirt-render/`: renders before and after, `UnitRender` (icheme package, DRUM case), boot-drop table. Code guide at merge, equipment chapter: `PipeSizes` cap, `NECK`, skirt outlet row and `portRadius`, `outlet` field, boot outlet and `bootDrop`, saddles at 1/8, `ProcessUnit.skirt`/`baseRing` (pedestal gone).

## Open for the owner
1. A 0.5 m lying drum has no front-head liquid outlet (pre-existing), so it takes a boot only from 9 m.
2. Trickle-bed back/right validity flips with the platform count.
3. Vertical heater D 2.5 fuel touch: keep it, or set the heater on legs.
4. Exchanger and kettle touches of ≤ 0.02 m: keep them, or grow the box by a block.
5. Pumps, tanks, columns and vessels still use `STAND_OUT`: unify them on `NECK`?
6. Fixed-bed gasifier +2 m bay.
