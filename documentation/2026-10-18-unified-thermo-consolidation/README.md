# 2026-10-18-unified-thermo-consolidation

Purpose: Code consolidation of the unified thermo port (owner 2026-10-18: "this is becoming too massive"): dead code, duplication, one-caller types, comment bloat and duplicate tests, behaviour-free (every number bitwise, the campaign's COUNTERS identical); number-moving simplifications only proposed

Status: Implemented 2026-10-19 (0.9.0). Index: [../INDEX.md](../INDEX.md).

## Documents

- [CONSOLIDATION_PLAN.md](CONSOLIDATION_PLAN.md)
- [CONSOLIDATION_REVIEW.md](CONSOLIDATION_REVIEW.md)

## Code areas

src/main and src/test, first science/thermo, science/fluid, science/material, runtime/fluid; gates `lane.sh` (m11 against m10)

## History

- Implemented 2026-10-19 (main `5b49f940`, mod 0.9.0, release 0.9.0 by fast-forward of `claude/junction-seeding` with the pre-merge commits `9ce2553a`, `47391899`, `81bf147f`; not pushed; final gates t48 (m16 at `798bedd9`): full test 1482/0, G2 exact, fluid GameTests 31/31, campaign cold 944 / bundled 988 / defaults 1000; t49 (m17 at `81bf147f`): 1482/0, G2 exact; merge plan `2026-09-24-coolprop-low-temperature/MERGE_STACK_PLAN.md`; code guide refreshed at `5b49f940`).
- Open: the review's proposals P1 to P4 and its behaviour-free items (`CONSOLIDATION_REVIEW.md`).
- Next: the custom equipment rendering branch merges as its own batch with checkpoint format 12.
- History: in progress since 2026-10-18 (thread t35, Fable 5.1 xhigh, branch `claude/unified-thermo-consolidation` from `c9aea46e`; plan `CONSOLIDATION_PLAN.md`, code commits done 2026-10-18, HEAD `688f4cd7`, Opus final test m11 done 2026-10-18 (t36): full test, G2 and GameTests as expected, campaign classes equal, campaign COUNTERS of cold and bundled random differ (wall-deadline seeds), returned to the orchestrator; review `CONSOLIDATION_REVIEW.md` written)
