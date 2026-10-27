# d46: what the unbooked junction buffers create (thread t52, 2026-10-19)

Batch `documentation/2026-10-19-post-release-followups/`, review `D46_MINT_MASS_REVIEW.md`. Analysis only: the probe
runs in lane exports (`tools/unified-thermo-port-m5/d40/lane.sh`, `/tmp/d40-<name>`), never in a tracked tree.

## What it measures
D46 keeps a junction buffer at its mass m_J (the mass pin, `ConservativeTransport.pinnedFlows`), so the vessels gain
exactly what the boundaries book in total mass. Per component and for energy, the world gains, over every buffer life,
m_J (w_install - w_end). The probe measures that quantity as minus the sum of every committed change of the junction
buffers, per component and signed. It hooks `IslandCoordinator` in the export in three places: a solve's commit, a
STEADY replay's commit and a topology install. t44's "mint mass" (sum of m_J) is an upper bound, and it says nothing
about what reaches a vessel.

## Files
| File | Purpose |
|---|---|
| `probe/D46Probe.java` | Per-thread accumulator; one `D46SEED` line per seed. The line has `freshKg` (t44's metric), `createdAbsKg` (sum over components of the absolute kg), `createdNetKg` (fluid total, which is minus `solidCreatedKg`: crystals held in buffers), `waterKg`, `createdMJ` and `turnoverKg` (the boundaries' gross kg). With `-Dd46.trace=true` it also prints `D46INSTALL` and `D46COMMIT` lines. |
| `probe/apply-probe.pl` | Puts the probe into an export, edits `IslandCoordinator`, and writes probe copies of the two hold-fuzz harnesses (reset and flush per seed). |
| `run-d46.sh` | `bash run-d46.sh <export> <tree> <logdir> [seeds] [patch...]`: the 200-seed quick check over three distributions, 4 workers, counters on. |
| `summarize.pl` | Sums the `D46SEED` lines of one distribution (`D46SUM` line, the 8 largest seeds). |
| `patches/form-fitting-volume-tau.patch` | Form (d): `HOLDUP_TAU` 0.05 to 0.01 s, a buffer of one fitting (1 m of pipe) instead of 5 m. |
| `patches/rule-off-supplier-seeds.patch` | Reference: the mint's round 0 from the junctions' built state (the placeholder), as at `33455d4c`. |
| `logs/t52-logs.tar.gz` | q0 (first run), q1 (base, counters), q2 (tau form), q3 (rule-off), the six cold seeds with solids, seed 188 traced. |

## Re-run
`bash run-d46.sh t52q 5b49f940 <logdir> 1-200` (about 2 minutes on 4 workers); seed 188 traced:
`JAVA_OPTS="-Dd46.trace=true -DholdFuzz.verbose=true -DholdFuzz.seeds=188 -DholdFuzz.threads=1 -DholdFuzz.defaults=false" lane.sh run <export> com.wormzjl.createcheme.runtime.fluid.HoldFuzzTest`
after compiling the bundled harness copy into the export.
