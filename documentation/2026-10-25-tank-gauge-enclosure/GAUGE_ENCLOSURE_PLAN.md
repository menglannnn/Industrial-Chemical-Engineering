# Tank level-gauge enclosure and connections

Status: In progress since 2026-10-25. Branch `codex/tank-gauge-enclosure`, based on `e5591c04` (the pending bubble-direction feature).

- Add a real clear glass sleeve to the floating-roof tank's integral liquid indicator, retaining the front-port location, neutral scale, actual fluid colours and hollow external marker.
- Fit the sleeve between the liquid column and marker opening: half widths .120 / .123 / .125 m. Preserve current rail/cap radius and the 5 mm liquid-to-marker clearance.
- Replace hidden square taps with visible round .09 m equalizing pipes into thickened upper/lower end manifolds. Add wall sockets and union fittings; retain vent, drain and intermediate brackets.
- Route static `Material.GLASS` polygons through cached translucent chunk quads. Draw dynamic liquid before terrain glass, following the existing pipeline sight-glass approach.
- Owner refinement: use a custom neutral clear-pane texture, never Minecraft glass. Map one full sprite per pane across the whole gauge height, avoiding repeated cyan/grid fragments. Keep an editable code-native source graphic in the batch tool folder.
- Keep the tank's hydraulic ports, datum, volume, process behavior, requested displays and persistence formats unchanged. Existing native layout refresh handles the static mesh.
- Verify the smallest relevant geometry/port/refresh gates, package build and native software views. No game check or old-world test.
- Record actual contracts and results; deliver on feature branch without automatic merge or version bump.
