# rack-slot-clearance

Purpose: a brute-force check that the pipe rack's five diagonal slots keep every pair of lanes apart, whatever faces they use. It is the evidence for the table in `PIPE_LAYING_PLAN.md`, section 5.1.

Batch: `documentation/2026-10-26-pipe-laying-system/` (plan `PIPE_LAYING_PLAN.md`). No code in a tracked path; W3's `PipeRackGeometryTest` (commit 68ceb242) replaces this check on the drawn mesh, as a gate.

## How to run

Java 21 or later, single-file source launch, from any directory:

```
java tools/rack-slot-clearance/RackCheck.java
java tools/rack-slot-clearance/BendCheck.java
```

## Entry points

- `RackCheck.java`: lane arms as capsules from the slot centre P = (c, c, c) to each joint face (c = 1/8 + 3k/16; a lane of m slots has OD = min(0.8, m · 3/16 − 1/64)). It checks every pair of disjoint slot ranges and every pair of faces: 2 520 arm pairs. It prints the smallest surface gap and the smallest margin to the block edge.
- `BendCheck.java`: whole lanes (two joints, elbows included, sampled every 2 mm), once with long-radius elbows (1.5 OD, as plain pipes have) and once with short elbows of radius (m + 1)/2 · 3/16 m.

## History

- 2026-10-26: written and run for the plan. Results: arms 0.0156 m (= 1/64 m) and 0.039 m to the edge; long-radius elbows −0.034 m (a clash); short elbows 0.0156 m.
- 2026-10-26: superseded as a gate by `PipeRackGeometryTest` (W3, 68ceb242), which checks the drawn arms and short elbows.
- 2026-10-26: obsolete. The layered rack (R1-R4, `RACK_LAYERS_PLAN.md`) replaced the five diagonal slots with one tier of four positions; `PipeRackGeometryTest` checks the new lanes (pair clearance, block bounds, steel).
