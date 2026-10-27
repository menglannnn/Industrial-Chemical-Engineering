# 2026-09-01-v3-full-cdu-draw-wall

Purpose: Full CDU plan (side strippers + pumparounds), draw-rate convergence risk, Phase A0 ramp hardening, Phase W dry-tray/trace-pair experiments

Status: Superseded 2026-09-06 by 2026-09-06-v3-literature-cdu-pumparounds. Index: [../INDEX.md](../INDEX.md).

## Documents

- [V3_FULL_CDU_PLAN.md](V3_FULL_CDU_PLAN.md)
- [V3_CDU_CONVERGENCE_RISK.md](V3_CDU_CONVERGENCE_RISK.md)
- `V3_COLD_DOE_*.md`
- [V3_A0_WET_LANE_REVIEW.md](V3_A0_WET_LANE_REVIEW.md)
- [V3_FULL_CONVERGENCE_PLAN.md](V3_FULL_CONVERGENCE_PLAN.md)
- `V3_W*_*.md`
- [V3_TRACE_PAIR_ANCHOR_BENCHMARK.md](V3_TRACE_PAIR_ANCHOR_BENCHMARK.md)

## Code areas

branches `codex/v3-cdu-ramp-hardening`, `claude/wall-probes` (not merged)

## History

- Superseded 2026-09-06 by the literature CDU pumparound batch
- A0 and Phase W never reached `main`
- side strippers not implemented

## Notes

- A0 (`codex/v3-cdu-ramp-hardening` at `9d18bb9`) exists only on `claude/wall-probes`, not on `main`. The W-1 to W-4 candidates were rejected in their own benchmark files on 2026-09-02. On 2026-09-07 the draw wall was traced to a liquid-supply balance that pumparounds cure (`2026-09-06-v3-literature-cdu-pumparounds/V3_WALL_AND_40MW_BALANCE_ANALYSIS.md`), which superseded the dry-tray reading of this batch.
