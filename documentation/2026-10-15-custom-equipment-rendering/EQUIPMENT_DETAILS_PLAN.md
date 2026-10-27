# Air cooler height and animation, colour config, adaptive connections, boot, skirt junction, manholes, flange bolts: plan

Batch `2026-10-15-custom-equipment-rendering` · branch `claude/custom-equipment-rendering-805e7d` · 2026-10-16 · follows `EQUIPMENT_POLISH_REVIEW.md` (ada9b340, 768e6fb6).

## Owner requests (2026-10-16, after the dev client on ada9b340)

1. The air cooler's lowest and default height are 2 m lower.
2. The joint between a drum and its skirt gets more detail.
3. A config sets the primary and secondary colours.
4. The default horizontal drum's connection 1 meets a saddle; connections adapt to the design.
5. A horizontal drum may have a boot, placed and supported as in real photos.
6. The air cooler's fans turn, can be switched off, and the FPS cost is checked and kept low.
7. Every flange has bolts.
8. The air cooler's underside looks like its top (grid).
9. Drums have manholes.

Owner answers: 8, the plenum shows the fin grid; 3, default colours in `createcheme-common.toml` and the Paint page says Primary and Secondary; 6, a switch in the air cooler's screen, for debugging.

## Physical expectation

- 1: a forced-draft fan takes its air from under its ring; half a fan diameter of free air there is a usual least. At a 3 m bundle every bay keeps its ring inlets at least 0.6 D up (the 3 × 5 m bay's single 2.48 m fan; 0.81 D for the 3 × 6 m bay's 2.05 m fans), with its motor beams 0.7 to 1.6 m up: no walkway under the fans, as at grade-mounted coolers. The default 4 m leaves 1.68 m or more under the motor beams.
- 4: the clash is in the screen, not the shape. Choosing Horizontal drum keeps the standing drum's connection: side 3 faces left on a standing drum, but down on a lying one, onto the saddle. A connection is an intent (a side, a place along the axis) that must stay clear of saddles, outlets and grade whatever the size.
- 6: a real fan of 2 m turns about 5 to 8 times a second (tip speed 30 to 50 m/s), which strobes at 60 frames per second with 4 to 6 blades; drawn at about 1.5 turns per second.
- 7: ASME B16.5 class 150 bolt counts by size (research below); a bolt end stands out about one bolt diameter from the flange back.
- 2 (survey B): the skirt is welded to the bottom head, flush with the shell; from 5 to 20 m one reads a band at the seam (weld or fireproofing flashing), the dark access hole and the base ring. Two vents of about 50 mm (four on a large skirt) sit as high as possible to let out gas caught under the head.
- 5 (survey A): free water settles to the low point at the outlet end, so the boot hangs under the far end, inboard of the far saddle and at least 0.15 m clear of it, with no support of its own. It is at most 0.5 D and at least 0.3, 0.4 or 0.5 m across (by D), 0.9 m or more deep, with a dished bottom and a draw-off at its lowest point. Boot drums stand higher: the drain needs room for its valve and pipe above grade.
- 8 and 6 (owner report with a photo from below, 2026-10-16: "two fans overlaps, and the metal sheets lacks a bottom casing"): a forced-draught plenum is a closed sheet-metal casing, one hopper per fan from the bundle's frame down to the fan ring; from below one sees the fins only through the rings, as from above through the bundle. Two fans in a bay sit side by side along the tubes, each ring with its inlet bellmouth about 1.1 D across, so the rings, not the blades, set the pitch. API 661 asks for 40 % fan coverage of the bundle face.
- 9 (survey D): a 24 in (0.6 m) manway, 18 in (0.45 m) on a small vessel, none below about 0.9 m (body flange). A standing drum has it on the shell about 0.76 m over the bottom tangent, facing the access side; a lying drum on an end head, its davit above. The cover weighs about 200 kg and swings on a davit.

## Reversible decisions (mine)

- 1: `ProcessUnit.minSupport(AIR_COOLER)` 3, default 4; the headroom gate becomes a fan-inlet gate (ring inlet at least 0.5 D above grade at the lowest bundle).
- 3: `equipment.primaryColour` and `equipment.secondaryColour` (#RRGGBB, defaults RAL 9006 and 7031) in the common config, read when a constructor's design is new.
- 4: `EquipmentShape.fit` keeps each connection where it is when it clashes with nothing; otherwise it moves it to the nearest station, on its own side first, then the form's other sides, that clashes with nothing. If no such place exists, it stays clamped and the status shows the clash. A new drum's connection and every size change go through it. A change between standing and lying gives the new form's default connection, because the sides mean other directions.
- 6: the rotors leave the chunk mesh while animation is on. A client renderer draws every tracked air cooler's rotors within 96 blocks and in view, turned about their shafts, lit at the fan. The switch on the air cooler's datasheet sets it for the client (not saved) and rebuilds the chunks. The FPS check compares on and off on a 6 × 4 bank, vanilla lane, 60 s + 60 s each.
- 7: bolts are drawn in one place for every flange (pipes, nozzles, devices, units), as hexagonal prisms on the flange's back on its bolt circle.
- 8: the plenum's panels and floor take the fin sprite (material `AC_PLENUM` removed). Superseded after the owner's report: a square-to-round hopper per fan (`ProcessUnit.plenum`, `AC_PLENUM` back, sheets on both faces) from the frame to its ring; a fan is the largest whose ring (`RING_OD` 1.12 D) fits its share, 0.02 m from the next (`fanDiameter`).
- 2: a bare steel band 0.1 m high and 0.02 m proud over the bottom tangent line, and two vent sleeves of 0.1 m (four from D 1.25 m) 0.25 m under it on the diagonals, with dark bores. No fireproofing (proposed to the owner as an option).
- 5: `EquipmentShape.Boot`: D/3, clamped to the survey's least sizes and 0.5 D; 0.9 m or 0.6 D deep; centred on a cell, inboard of the far saddle (just beyond it on a short drum, none where neither fits). Its drain is a fixed LIQUID nozzle facing down; the least saddle height keeps a block for its pipe above grade (4 m for a 3 × 9 m drum). Constructor toggle on the Nozzles page beside the drain it carries (raises the saddles; moved off the design page, whose bottom buttons it pushed out of a 480-pixel screen), NBT `Boot`, protocol `equipment-8`, command word `boot`.
- 9: one `Manway` record (neck, bolted flange, bolted cover, davit) shared by columns and drums. A lying drum's sits on the far head's centre (the front head carries the liquid outlet); a standing drum's faces the back, 0.76 m over the bottom tangent, and a shell too short to hold its cover there (1 m) has none. Connections fit around it (clash `MANWAY`, and `BOOT` for the boot).
- 4 (as built): `fit` and `place` take a template drum (`withConnections`), so every fixed part a drum carries (outlets, boot, manway, saddles, grade) is avoided by one rule.

## Steps

1. Air cooler height; colour config and Primary/Secondary; plenum grid; adaptive connections (gate: every default drum and every fitted connection clash-free).
2. Photo research (agent): boot, skirt junction, manholes, bolt table. Then the boot option, the junction detail and manholes, with gates.
3. Flange bolts everywhere.
4. Fan animation, its switch and the FPS check.
5. Gates, in-game check, review, changelog, docs.
