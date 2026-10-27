# skirt-render

Batch `2026-10-21-floating-roof-tanks`, 2026-10-25. The owner reported that the trickle-bed and ebullated-bed reactor skirts did not match the drums' and columns' skirts in design or material. Fix: `36e0f9fb`. A boot drain neck defect found on the way is fixed in `bae8620a`.

## Contents
- `UnitRender.java`: `tools/new-units/UnitRender.java` with the package renamed to `com.wormzjl.icheme` (mod rename 0.11.0) and a `DRUM` case (a standing drum on its skirt, for comparison).
- `bootdrop-table.js`: boot depth, minimum saddle height and drain neck per lying-drum diameter. It compares the STAND_OUT-only face rule (`7d53cef5`) with `bootDrop`, where the boot gives way (`bae8620a`).
- `renders/`:
  - `tb-before-low.png`, `eb-before-low.png`: the concrete skirts on their pedestal, before `36e0f9fb`.
  - `tb-after-low.png`, `eb-after-low.png`: the steel skirts, after `36e0f9fb`.
  - `drum-low.png`: the reference standing drum, D 3 m, T/T 9 m, skirt 3 m.

## Run (from a worktree, after `./gradlew compileJava`)
```bash
mkdir -p build/tmp/skirt-render/classes
javac -d build/tmp/skirt-render/classes -cp "build/classes/java/main;build/resources/main" <this folder>/UnitRender.java
CP="build/tmp/skirt-render/classes;build/classes/java/main;build/resources/main"
MSYS_NO_PATHCONV=1 java -Xshare:off -Xmx1536m -cp "$CP" com.wormzjl.icheme.science.equipment.UnitRender TRICKLE_BED_REACTOR 4 2 5 out/tb low
MSYS_NO_PATHCONV=1 java -Xshare:off -Xmx1536m -cp "$CP" com.wormzjl.icheme.science.equipment.UnitRender EBULLATED_BED_REACTOR 4 20 8 out/eb low
MSYS_NO_PATHCONV=1 java -Xshare:off -Xmx1536m -cp "$CP" com.wormzjl.icheme.science.equipment.UnitRender DRUM 3 9 3 out/drum low
node <this folder>/bootdrop-table.js
```
Arguments: kind, diameter, length (beds, or T/T in m), support, output stem, view. The renderer draws the unit's server mesh in software. No game client is needed.
