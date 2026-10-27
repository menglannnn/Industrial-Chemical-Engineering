# ac-blender: air cooler parts, scripted in Blender

Purpose: low-poly parts and sprites for the forced-draft air cooler (ACHE) of batch
`documentation/2026-10-15-custom-equipment-rendering/`, owner decision "Blender parts, scripted". One Python script,
run inside Blender 5.2, builds the fan rotor, the fan ring with guard and the fan motor. It also paints their textures
and the three tileable sprites, exports OBJ files and checks them, and renders previews. The mod loads the outputs; this
folder changes no mod sources.

## Run

```
"C:/Program Files/Blender Foundation/Blender 5.2/blender.exe" -b --factory-startup --python ac_parts.py -- [--out DIR] [--no-render]
```

- Default output: `out/` next to the script. A run takes about 6 s (Eevee, 32 samples).
- `--no-render` stops after the export and the checks.
- The script is deterministic: no random module, and the textures use the integer hash and `plate()` noise of
  `../textures/VesselTextures.java`, bit for bit. Two consecutive runs gave byte-identical outputs, previews included.
- The script exits with an error when a part check fails. `out/report.json` holds the check counts.

## Outputs (`out/`)

| File | Contents |
|---|---|
| `ac_fan.obj` + `.mtl` | Fan rotor, material `ac_fan`, texture `ac_fan.png` (64×64) |
| `ac_fan_ring.obj` + `.mtl` | Fan ring and guard, material `ac_ring`, texture `ac_ring.png` (64×64) |
| `ac_motor.obj` + `.mtl` | Fan motor, material `ac_motor`, texture `ac_motor.png` (64×64) |
| `ac_fins.png`, `ac_header.png`, `ac_plenum.png` | Tileable sprites, 32×32 |
| `report.json` | Check results of each exported OBJ |
| `previews/*.png` | 1024×768 renders plus `contact_sheet.png` |

## Conventions (all parts)

- Units are metres, with +Y up. The script models in game coordinates and converts to Blender coordinates
  (x, −z, y) only when it creates a mesh. Export uses Blender's exporter with forward −Z and up Y, which maps
  the data back exactly.
- Each OBJ has one object, one `usemtl` and triangles only, with `v/vt/vn` on every corner. There are no modifiers.
- Blender writes `s 0`/`s 1` lines (its smooth flag). They are harmless: every corner has an explicit normal.
- Normals: round surfaces are smooth (hub, motor frame, flange edge, shaft, ring walls, and each blade surface along
  the span). Edges between different surfaces are split, and caps, boxes, fins and annuli are flat.
- `vt` uses the OBJ convention: v = 0 is the bottom row of the PNG. A loader that puts v = 0 at the top must flip v.
- Each face's UVs lie in a pixel rectangle of its texture, inset by 0.5 px (the rectangles are listed in
  `FAN_RECTS`, `RING_RECTS` and `MOTOR_RECTS` in the script). All part UVs lie in [0, 1].
- Every solid is a separate closed shell. Its winding is made outward from the sign of the shell's volume.

### Checks

The script reads each exported OBJ back and checks these properties (all results are in `report.json`):
- Every edge is shared by exactly two faces, and each directed edge occurs once (consistent winding).
- Every shell has a positive volume (outward).
- No vertex normal points against its face's winding.
- No triangle is degenerate.
- No UV lies outside 0..1.
- There is one material.

| Part | Triangles / budget | Shells | Non-manifold edges | Winding errors | Inward shells | UVs out of 0..1 |
|---|---|---|---|---|---|---|
| `ac_fan` | 268 / 300 | 5 | 0 | 0 | 0 | 0 |
| `ac_fan_ring` | 696 / 700 | 12 | 0 | 0 | 0 | 0 |
| `ac_motor` | 384 / 450 | 18 | 0 | 0 | 0 | 0 |

## Parts

### `ac_fan`: rotor, D = 1.0 m

- Origin at the rotor centre. Axis +Y. The mod scales it uniformly to the real fan diameter (0.8 to 2.2 m).
- Four blades sit at 0°, 90°, 180° and 270°. Each blade is a twisted box loft through 6 sections at r = 0.07, 0.15,
  0.24, 0.33, 0.41 and 0.49 m.
- Pitch is 25° at r = 0.10 and falls linearly to 12° at the tip. The root section is buried in the hub.
- Blade chord is 0.14 m and is wrapped on the cylinder of each radius. Thickness tapers from 22 mm to 16 mm.
- The blades occupy y ∈ [−0.037, 0.037]. The tip radius is 0.49, which leaves 25 mm of clearance to the ring.
- Hub: D 0.20, y ∈ [−0.06, 0.06], 16 sides, with a chamfered bottom that faces the motor.
- Turning direction: positive rotation about +Y (counter-clockwise seen from above) pushes air towards +Y (forced
  draft). The leading edges point along the motion and sit lower than the trailing edges. Spin it the other way and
  the blades push air down.
- Look: aluminium blades brushed along the span, and a dark hub with six clamp bolts on its cap.

### `ac_fan_ring`: ring and guard for the 1.0 m rotor

- Origin at the ring centre, on its top face (y = 0). The ring hangs down. It is scaled with the fan.
- Ring: a 24-sided solid of revolution. Inner radius 0.515 and outer radius 0.535, y from −0.30 to 0. Its top flange
  lip has an outer radius of 0.60, y from −0.02 to 0.
- Guard: eight radial bars with a 12 mm square section run from r = 0.11 to r = 0.528.
- The guard also has three rings at r = 0.12, 0.31 and 0.49, with 12, 16 and 24 segments. Their wire has a
  triangular section (14 mm wide, 12 mm high, flat side down) and rests on the bars.
- The guard is dished: y = −0.292 at the rim down to −0.35 at the centre, about −0.33 on average. At the rim the bar
  tops reach 2 mm into the ring wall, so the guard hangs from the ring.
- Suggested rotor position: 0.15 D below the ring top, as in the mock-up.
- Look: near-white grey with four bolted ring sections and flange bolt holes. The guard is a slightly darker
  galvanised grey. Ring and guard share the tinted `ac_ring` material.

### `ac_motor`: vertical flange motor (V1), not scaled

- Origin at the centre of the top face of the mounting flange.
- Flange: D 0.40, y from −0.03 to 0.
- Frame: D 0.32 from y = −0.02 down to −0.52. A chamfered fan-cowl end follows down to −0.58.
- 12 cooling fins (40 × 12 mm, from y −0.06 to −0.50), at 15° + 30°·k, so +X stays clear.
- Terminal box on +X: x 0.14 to 0.27, y −0.40 to −0.24, z ±0.08. Its lid carries a nameplate (x up to 0.285).
  A cable gland points down from the box.
- Shaft: D 0.05, from 0 to 0.12 above the origin.
- Look: medium blue-grey (86, 102, 122), a darker flange, a cowl grille on the bottom, and a bright steel shaft.

## Tileable sprites

| Sprite | Size | Tile | px/m | Tinted | Layout |
|---|---|---|---|---|---|
| `ac_fins.png` | 32×32 | 1 m × 1 m | 32 | no | Tubes run along V and repeat every 2 px (6.25 cm) along U: a lit fin crest, then the shadowed gap. Fins are fine lines across U (every other row). Light aluminium (198, 202, 204). |
| `ac_header.png` | 32×32 | 0.5 m × 0.5 m | 64 | yes | Steel plate (198, 200, 202). One row of hex plugs on rows 14 to 16 (v centre 0.516), one plug per tube: pitch 4 px = 6.25 cm, the fins sprite's tube pitch. Weld bead on row 0. |
| `ac_plenum.png` | 32×32 | 1 m × 1 m | 32 | yes | Sheet steel (194, 197, 200). Faint lap seams on column 0 and row 0. A vertical stiffener rib on columns 15 to 18. |

## Previews (`out/previews/`)

- Parts alone, two views each: `fan_below`, `fan_above`, `ring_below`, `ring_above`, `motor_side` and `motor_below`.
- Bay mock-up: `bay_below` (low camera looking up from the long side) and `bay_above_front`.
- `contact_sheet.png` has the eight views in that order (four per row), with the six textures enlarged underneath:
  fan, ring, motor, fins, header, plenum.
- The previews re-import the exported OBJ files, so the renders show what the mod loads. Backface culling is on, as
  in the game.
- Renderer: Eevee with the Standard view transform, closest-pixel texture sampling, a sky colour and a grey ground.
- The tinted sprites are multiplied by a preview paint colour of (0.86, 0.88, 0.90). The mod supplies the real one.
- Mock-up geometry is preview-only and is not exported:
  - The bay is 3.0 × 6.0 m. Headers are 0.6 m deep at both ends.
  - The fin bundle is 0.5 m thick, at 5.0 to 5.5 m.
  - The plenum tapers to a 2.4 × 4.6 m fan deck at 3.8 m.
  - Two 1.9 m fans with rings and guards sit at z = ±1.15, with the rotor 0.285 m below the deck.
  - Each motor's shaft reaches 6 cm into its hub. A cross beam under each motor hangs from the deck.
  - Four 0.2 m legs stand under the headers.

## Deviations from the brief

1. The shaft solid starts 1 cm inside the flange (y −0.01). The frame solid starts 1 cm inside the flange (y −0.02).
   The visible sizes match the brief, and nothing is coplanar.
2. The guard is dished (y −0.292 to −0.35) instead of flat at −0.33, so that the bars meet the ring.
   - It has a central opening of r = 0.11 D, where the bars start, for the motor.
   - The motor's fins (r 0.19 m) pass through that opening only for D ≥ about 1.75 m.
   - For smaller fans, or for the terminal box (r 0.285 m), the motor must sit below the guard. In the mock-up the
     box sits 3 cm below it. The integrating agent should decide.
3. The guard rings use a triangular wire section, not a square one, to stay within the 700-triangle budget. The ring
   is 24-sided for the same reason.
4. "One plug per tube row" was read as one plug per tube, in a single row with the fins sprite's tube pitch. A real
   header has one row of plugs per tube layer.
5. Extra files: an `.mtl` file next to each OBJ (`map_Kd` names the PNG) and `report.json`.
