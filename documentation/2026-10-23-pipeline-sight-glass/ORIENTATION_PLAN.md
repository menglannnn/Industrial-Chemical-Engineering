# Sight-glass installation orientation

2026-10-23 follow-up, same batch and owned branch. Owner: "Make the sight glass transparent side facing the player when placed."

- Determine the straight pipe's run from its actual connections, using its existing facing only when unconnected.
- Choose the cardinal axis toward the installer's eye with the greatest perpendicular component. Paired windows are symmetric, so only the axis needs storage; never aim through a flow end.
- Persist `sight_glass_axis` as vanilla blockstate enum `x/y/z`, separately from hydraulic `FACING`. Choose it once on successful installation; duplicate use remains a no-op.
- Allow top/bottom viewing on horizontal pipes when the installer is above/below. This owner instruction supersedes the original horizontal-window-only default.
- Use the same immutable geometry/orientation key for baked housing/glass and dynamic liquid. Choose a perpendicular geometric fallback if later connections make the stored axis parallel to the run.
- Retain straight-only eligibility, resizing, consumption/recovery, pipe identity/settings and queued hydraulic scheduling. No new fluid formats or payload fields.
- Compile/package only, no tests or automated GUI checks. Produce an actual-mesh orientation sheet using the same production selector. Record the review and updated contracts/index/changelog.
