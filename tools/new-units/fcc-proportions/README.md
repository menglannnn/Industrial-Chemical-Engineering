# fcc-proportions (batch 2026-10-15-custom-equipment-rendering, 2026-10-18)
Prototype of the FCC converter proportions after the owner's remark "Regenerator is too big, reactor by itself does not have
appropriate proportions" (`FCC_PROPORTIONS_RESEARCH.md`, `FCC_PROPORTIONS_PLAN.md`). Nothing here was ever in a tracked path.
- `form-a/FccConverterLayout.java`: the research rules (regenerator 9 + 1.5 D, reactor 6 + 1.2 OD, stripper 0.35 D x (5 + 0.3 D),
  cone (rr - rs)·√3, stripper head rs / 2 in `fits`). `form-b/`: the same with the reactor 0.75 D.
- `FccProbe.java`: per size 4-17 m, the fitting ranges, the default form's elevations, box and quads (`probe-*.txt`).
- `UnitRenderFixedScale.java`: `tools/new-units/UnitRender.java` with `-Dscale=<px per m>`; `Montage.java` sets renders side by side,
  grade aligned.
Run (worktree root, after a Gradle build): `javac -cp "build/classes/java/main;build/resources/main" -d <out> form-a/FccConverterLayout.java
FccProbe.java UnitRenderFixedScale.java` (rename the renderer file to UnitRender.java first), then `java -cp "<out>;build/classes/java/main;
build/resources/main" com.wormzjl.createcheme.science.equipment.FccProbe q`, or `... UnitRender FCC_CONVERTER 10.5 32 9 <prefix> front,iso`
with `-Dscale=14`.
