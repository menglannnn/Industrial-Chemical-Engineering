# M5 worse worlds: seeds 322 and 47 minimized and traced (2026-10-17)

Batch `2026-09-24-coolprop-low-temperature`, `claude/unified-thermo-port` at `7bd5fafc`, diagnosis only, nothing committed. Tools: `tools/unified-thermo-port-m5/trace/` (minimizer, solve log, row trace through a temporary patch that is now reverted, main contrast in a removed worktree at `ffeafe8a`). Logs: `build/m5-logs/08`-`15`. Minimized: 322 from 19 to 13 blocks (still E6), 47 from 27 to 18 blocks. For 47 the criterion is work, not wall time: one solve of at least 300 k cancellation checks with the budgets off. The costly interval stays at 334 427 checks.

## Seed 322 (E6): a trace of liquid at a tank's bottom port

**Physics.** A methane generator (431 kPa, 357 K) vents to two 1 atm voids, each line at its velocity cap (1.83 kg/s). The junctions and two 1 m3 tanks stand at 430 kPa. Tank T1 (z = 0, 369 K) connects from its bottom port through a 0.05 m line (2 L) to the top of tank T2 (z = -2, 430 K). T1 holds 1.8e-8 m3 (about 13 mg) of heavy crude condensate (pc05-pc08). It condensed at tick 785 from crude vapour that the methane carried in. In real life the 13 mg runs down into T2 at once, and the line holds gas. The gas column (about 2 kg/m3 over 2 m, 40 Pa) matches the 38 Pa between the tanks. Steady state: no flow in the drop line, both tanks at rest, the generator venting 2 x 1.83 kg/s.

**What the code did.** Island 19's interval [997, 1097] fails at every step size from 1 s to 2e-6 s, mostly at pass 0 (Newton iteration limit):
- **The column.** It is read from the leading phase of T1's bottom port (`endDensity`, D11). That phase is the 13 mg trace, so the line is stated as a liquid column of about 14 kPa. At the pass start the line's own row is the largest (r = 2.25).
- **The draw.** The segment is decided at the carried flow, which is about zero and slightly reverse. Nothing is pinned and the trace is the marginal phase, so every forward trial draws T1's liquid alone.
- **The Newton.** No root exists. At the last iterates T1's equilibrium rows (crude_pc01..pc07) reach |r| = 5 to 13 and the line's row reaches 22.5. The line's flow wanders between -0.04 and -0.38 kg/s, and one trial reports "Liquid root absent".
- **No relief.** D11's drained seed needs a pinned phase, and there is none. D25's phase correction keeps every populated phase. Halving the step changes neither the column nor the segment.
- **The TP answer.** At T1's own T, P and amounts it holds 3.0e-10 m3 of liquid, 1/60 of the state's trace.

**Why main is clean (log 14).** Main ran the same 13 blocks. It matches M5 to tick 897 (T1 holds 1.86e-8 m3), then ends the next interval with T1 dry. Main's phase correction may swap a populated layout for the TP flash's; D25 removed that. On main the trace comes and goes until tick 1797. After that T1 is dry and T2 holds 5.2e-6 m3. This meets D25's stop condition: a new E-class hold where a phase must vanish.

**Proposition (owner to choose; each rests on an assumption).**
- **A (recommended).** Fill a phase port's static column (column, sign test, start-flow cap) with the port's priority phases by volume over the connection. A 1.8e-8 m3 trace in a 2 L line then gives a gas column. The draw itself is unchanged: the trace still leaves first, at its capacity.
- **B.** Decide a port end's segment from the sign of its static drive, not the carried flow. The trace is then pinned at its capacity, and D11's drained seed removes it.
- **C.** Let D25 drop a populated trace phase that a port draws, at a failed iterate. This is main's behaviour for this case only and an exception to D25.

## Seed 47 (F): a newly admitted generator at the line's own pressure

**Physics.** A crude generator (281 kPa, 360 K, z = -3) feeds about 3 kg/s of crude through 0.02-0.05 m lines and junctions. The crude passes a tank at z = 3 that drains from its bottom, and flows on to a 1 atm void. Light ends flash in the junctions. A methane generator (175 kPa, 290 K) connects to three of those junctions. While they stand above 175 kPa (190-254 kPa at the hold), it delivers nothing. Settled, they stand at 171-175.5 kPa, and it feeds 0.12 kg/s into the junction beside the void. In real life a supply at the line's own pressure acts as a check valve at rest or gives a small steady flow; the steady state is stationary.

**What the code did.** There is no failure: the hold is cost.
- **Admission.** Main refused this generator at placement (methane below 293.15 K). M5's range rule admits it, and the island becomes 10 nodes with every junction two-phase in about 20 components.
- **Cost of the run.** 5.2-5.5 M checks (26-28 s), against 0.59 M (2.2 s) for the same world without the generator, which is main's world. The costly intervals (50-120 k checks) reject 6-12 substeps each:
  - The generator's connections reopen ("boundary reopened") and close again.
  - The crude junctions beside them switch between liquid and liquid plus gas (phase codes 1 and 3).
  - Pass sequences cycle.
- **The hold.** In the first interval where those junctions fall to 175 kPa, the step collapses from 2 s to 0.24 ms before it recovers: 53 attempts, 175 passes, 2148 Newton iterations, 334 k checks, 1.57-1.68 s alone. That is past the 1.5 s soft budget, and the fallback refuses (trust region), so the hold is F. The retry at the halved slice passes.
- **Not the extrapolation.**
  - Generator at 293.2 K on M5: no hold, first interval 0.77 s, 25.1 s in all.
  - Main's code with the generator at 293.2 K (log 15): 26.2 s and two range holds (methane at 293.1499999 K in a junction).

**Proposition (owner to choose).**
- **A.** Carry a generator connection's closure across steps and intervals, as pump modes and caps already are. Change it only on a converged point. A supply standing at the line's pressure then stops costing a reopen cycle per interval.
- **B.** Accept this as a new world of the range rule: one transient budget hold that passes on retry, and main's solver is as slow on the same world.

## The other new worse seeds (campaign lines only, not traced)

- **1000 (F).** Main refused its 292 K crude generator. It is a newly admitted world, as 47; its cost mechanism is not checked.
- **678 (F).** Main refused its 268 K methane generator and held E5 on crude_pc01 range rejections. It is a newly admitted world, as 47.
- **629 (E5, pass 1).** Main refused nothing, so the world is the same. Tanks fed by four crude generators make 322's mechanism plausible; this is not tested.

Code guide: no change. Tracked tree unchanged: the hooks are kept in `patches/` and were reverted.
