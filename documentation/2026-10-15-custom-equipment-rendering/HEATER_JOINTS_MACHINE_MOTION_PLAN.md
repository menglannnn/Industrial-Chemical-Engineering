# Heater joints, compressor pistons, machine motion: plan

Batch `2026-10-15-custom-equipment-rendering` · branch `claude/custom-equipment-rendering-805e7d` on 9cadc501 · 2026-10-18 · owner notes after the dev client: "Red dots on the side of furnace convection section is weird"; "Furnaces are assembled from sections and have junction lines, check actual photo"; "Add Piston model to compressor"; "Give compressor and pump working animations". Owner answers the same day: remove the soot blowers; the pump's shaft and motor fan turn; piston rods in open distance-piece windows.

## Physical expectation (photo survey 2026-10-18)
- Soot blowers keep oil-fired convection tubes free of soot and ash. Gas firing leaves the tubes clean, and our burners burn fuel gas, so a real heater of ours has none. The red dots are their gearboxes, drawn in the actuator colour every metre up and every 4 m along.
- Heaters are shop-built in road-legal modules (about 4 m wide or tall at most) and bolted together on site. A vertical heater's radiant shell is curved full-height panels joined on vertical flanged seams at the casing columns (training slide "radiant section panel modules joined"; Farnikan photo with girth stiffeners). Convection modules, about 2 to 3 m tall, stack on heavy bolted perimeter flanges, with corner columns (Indus Metalworks lift photo; shop module photo). The convection section sits on the radiant roof and carries the breeching on flanged frames. Stack sections have flanged joints. Each joint shows as a flange ridge about 0.1 m proud with a row of nuts.
- Compressor: the pistons stay inside the cylinders. What moves in sight is each piston rod, through the open distance piece between the crosshead guide and the cylinder's packing case (illustration "Basics of distance pieces"). The rod diameter is about 0.15 to 0.3 bores, and an oil deflector collar on the rod moves with it. The crank turns at 300 to 870 rpm (`compressorSpeed`); a stroke of 0.14 to 0.4 m follows a slider-crank, and opposed throws move outward together (balanced opposed).
- Pump: the shaft turns at about 2,950 rpm, seen in the bracket windows with its seal collar. On a TEFC motor the same shaft turns the cooling fan behind the cowl's grille. The coupling stays under its solid guard.
- Shown speeds: real speeds alias at frame rates (as with the air cooler fans), so the display runs slower: the crank at 1 rev/s, the pump at 2 rev/s, the fans at 1.5 rev/s as now. 40 ticks hold whole turns of all three.

## What changes
1. Soot blowers: removed from `convection()` and from the heater docs.
2. Joints, one pair of helpers placed where each heater draws its casing:
   - `seam`: two flange bars with a split line and nuts every 0.5 m. Used at every vertical heater's casing column, and at the cabin's wall columns and roof bay lines.
   - `joint`: a flange frame round a box at a height, with nuts. Used at the roof under the convection section, between convection modules (equal modules of 3 m at most; header-box panels split with them) and at the breeching. Corner columns on the convection box.
   - Stack joints get a split line in their flange band.
3. Compressor: the distance piece becomes an open frame (top and bottom plates, posts, partition), with its windows open both sides. Inside: the piston rod, the packing case at the cylinder and the wiper case at the guide. The rod and its deflector collar are a moving part, long enough that both ends stay hidden in the guide and the cylinder over the whole stroke.
4. Moving parts, one path for every machine; this absorbs the air cooler's fan special case:
   - `Mesh` tags facets with a moving part's index, and `ProcessUnit` records each part's motion. A motion either turns about an axis or slides on a slider-crank, with a pivot, speed and phase. Fans, pump shafts and motor fans turn; piston rods slide.
   - `ShapeRasterizer` builds a cell's surface with or without moving facets, and gives each part's polygons cut at the cell grid, relative to its pivot. `ProcessUnit.rotors`/`rotor` and the `AC_FAN` exclusion go.
   - The client: `FanAnimation` becomes `MachineAnimation`, drawing any tracked part once a frame with its transform; the block entity tracks the parts whose pivots it holds.
   - The datasheet switch ("Moving parts: running/still") shows on every unit with moving parts. It is on by default and not saved.
5. Pump: a seal collar with set screws on the shaft in each bracket window. The motor's cowl end becomes a grille of spokes and rings over a dark bore, with a fan behind it (OH2 motor; the BB1 motor's cooler cowl). Each rotating assembly is one part.

## Gates and checks
- `ProcessUnitTest`: every moving part's polygons lie within its reach, and together they have the area of the still facets the chunk mesh draws (replaces `fanRotorsTurnAboutTheirHubs`). A piston rod's ends stay inside the guide and the cylinder at both stroke ends. No heater draws a soot blower. Pipe spacing and access gates unchanged.
- Quads per unit (`MachineCounts`) before and after; equipment tests and the determinism source gate.
- In game: the bridge lane (vanilla, fresh world: heaters, pumps, compressor, the switch), and one Iris lane for the moving parts.
