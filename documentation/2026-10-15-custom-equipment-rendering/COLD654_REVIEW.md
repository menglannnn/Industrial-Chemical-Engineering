# Cold fuzz seed 654 (Opus, 2026-10-20), `bde9013c` on `claude/stoic-bell-ae6019`, not merged or pushed
Owner request 2026-10-20: "Trace cold 654 on this branch (its B hold after the placement at 2629, from 37797661), physics first, fix it structurally, re-run only the affected parts." Follows `FINAL_LANE_37797661_REVIEW.md` section 3. Probes and prototypes: `tools/new-units/od4-holds/r6/`. Logs: `tools/new-units/logs/cold654-2026-10-20.tar.gz`. Code guide: no change (not merged; at the merge the network chapter records that the balance start closes one-way lines only off a converged balance).

## 1. Physics first (island 50, 15 nodes, tick 2629)
- A void (101.3 kPa), a compressor and a filter are placed. The void takes four lines, from junctions at 169, 211, 426 kPa and 99.0 kPa.
- Three junctions, #4, #32 and #48, stand at 99.0 kPa. Two choked lines feed them: 0.070 kg/s from #33 (426 kPa) and 0.031 kg/s from #24 (211 kPa). #48 is a dead end behind the compressor, refused for its two-phase suction (D6). It holds 0.077 kg of LNG and CO2 at 173.6 K.
- The run's only outlet is the void, 2.3 kPa above it. Expected: the run rises just over 101.3 kPa and discharges the 0.10 kg/s into the void, and #48 keeps its cold stock. That is a short, trivial step.
## 2. What the code did instead (`37797661`, held B at 2631 and onwards)
- The balance start (`balanceStart`) ended unconverged after 13 iterations and 13 sweeps. Its residual was 0.245 kg/s at #14, and 0.10 kg/s was left in the run, whose last iterate was still at 99.0 kPa.
- The one-way closure read off that iterate shut the void line for the whole solve (`boundaryClosed`).
- The run then had no outlet. Its inflows are deep in saturation: the cap's slope (1+r^8)^(-9/8) is 7e-4 and 2.4e-3. Every pass ran the run to 13 to 15 MPa, from 0.05 s down to 9.5e-8 s, ending in "Matrix values must be finite" or line-search stalls.
- The balance's outcome is round-off sensitive. A probe that changed only the loss inversion's warm start (`lastQ`, at 1e-12) turned 654 clean, so the hold is not specific to `37797661`'s rule. That rule only moved the path.
- A second symptom: #48's seed was the balance's −242.4 kJ/kg against its stock's −274.7. Form E (below) removed it, and the hold remained.
## 3. Fix (`bde9013c`; owning stage: the balance start's closures)
- A passive one-way connection is closed for the solve only off a balance that converged. An unconverged balance's last iterate is no state of the island, and that closure is the one decision the solve cannot revise. Pump modes, junction states and start flows stay starting guesses that the pass loop moves.
- The change is 1 code line plus javadoc. Forms tried (`r6/prototypes/`):
  - D3, always the own split: 654 clean on its path; 994 F from 3544; 157 E4 (the D10 trace dropped).
  - E, every junction seed is its mixture, with the D10 trace kept: 654 still B (pass 2), 994 F from 3544, 157 F.
  - F, this fix.
## 4. Results (javac lane; Gradle for chain-100)
- Alone, one worker:
  - 654: clean, 5 761 Newton iterations (on `2def39dc`: clean, 4 976).
  - 994: clean, FULL, 8.77 kg of crystal in 3 vessels, 113 213 iterations (112 584 on `37797661`).
  - Bundled 157: the same 14 wall-deadline holds on both trees, 14 590 iterations against 15 752.
- Quick check, 200 seeds, 4 workers, against `37797661`:
  - cold 194 = 194, the same six worlds held, Newton +0.3 %;
  - bundled 197 against 196 (151 cleared, was E5), nothing newly held, Newton +1.3 %;
  - defaults 200 = 200, Newton +0.6 % (+77).
- Named worlds: 13 cold and 7 bundled, all clean. Bundled is identical.
  - Cold Newton is +33 % (109 835 → 146 183), nearly all of it in 729 (9 152 → 21 840) and 743 (23 039 → 44 940).
  - 743's island 38 now ends FULL instead of STEADY. 729 ends FULL as before, with crystal in 2 vessels against 1.
- Targeted classes 148/149 (the known lane failure). No printed line moved. Chain-100 exact.
## 5. What the cost is (probe `r6/unconverged-closures.pl` on `37797661`)
- The balance is often unconverged in the hard worlds: 37 times in 729 and 140 times in 743, with residuals from 1e-4 to 0.4 kg/s.
- Closures read off those iterates on `37797661`: 10 in 729 and 25 in 743. Each was a line into a generator, from a junction standing 0.43 to 4.5 MPa above it. Those closures were right; the pass loop now finds them only after a converged pass, and 743 is no longer certified at rest.
- 654's wrong closure (29 of its 66) was the void line, from a junction 2.3 kPa under the void whose run had to rise.
## 6. Proposition for the owner
- (a) Recommended: keep `bde9013c`. Next, a task in the owning stage: the balance stops short where Newton meets the cap's flats and the one-way kink, with at most 12 sweeps and a sweep dropped when the merit rises. A balance that converges there gives every closure from balanced ends, which would bring 743's STEADY back. Then the final lane.
- (b) Keep `bde9013c` and go to the final lane now, with the named cost (729, 743) recorded.
- (c) Revert `bde9013c`. 654 stays a known worse world.
