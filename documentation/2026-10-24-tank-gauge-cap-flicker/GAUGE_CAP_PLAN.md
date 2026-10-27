# Tank gauge cap flicker

2026-10-24. Owner requests a direct fix on the current integration branch, claude/floating-roof-tanks-81d68b, after testing 81cabbe9. No new branch.

- At a full gauge the marker's top clamps to the liquid top. Both were solid box caps at that same height, producing overlapping same-facing polygons.
- Make the marker a genuine hollow collar with 5 mm clearance around the liquid column, using cached Mesh box faces. Share the existing draw transform for closed liquid columns and the hollow marker; preserve dimensions, tint, level and timing.
- Build/package and run an offline cap-area/end-stop sweep; no test suite while the owner's client runs. Preserve all ten unrelated local files and stage only the direct fix and changelog. Leave the client/world untouched; Java geometry changes require restart.
- Record review/index/code-guide facts and compressed evidence. No physics, format, main merge or version change.
